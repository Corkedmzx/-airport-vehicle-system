#include "airport_connectivity.h"

#include <stdio.h>
#include <string.h>
#include <time.h>

#include "esp_log.h"

#if CONFIG_AIRPORT_USE_WIFI_MQTT
#include "esp_check.h"
#include "esp_crt_bundle.h"
#include "esp_event.h"
#include "esp_netif.h"
#include "esp_sntp.h"
#include "esp_wifi.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "mbedtls/md.h"
#include "mqtt_client.h"
#endif

static const char *TAG = "airport_conn";

#if CONFIG_AIRPORT_USE_WIFI_MQTT

/*
 * 华为云 IoTDA 密钥鉴权（与控制台「MQTT 连接参数」、官方文档一致）：
 * - UserName = device_id
 * - ClientId = {device_id}_0_0_{UTC时间戳YYYYMMDDHH}
 * - Password = HMAC_SHA256( key=时间戳字符串, message=secret 的 ASCII 字节 ) → 64位十六进制小写
 * 说明：不可再把「设备密钥」明文当作 MQTT password（否则会报 bad username or password）。
 * 文档：https://support.huaweicloud.com/devg-iothub/iot_02_0203.html
 */

static esp_mqtt_client_handle_t s_mqtt;
static volatile bool s_mqtt_connected;

static char s_mqtt_client_id[192];
static char s_mqtt_password_hex[70];

static void mqtt_event_handler(void *handler_args, esp_event_base_t base, int32_t event_id, void *event_data);

static void wait_utc_time_for_huawei(void)
{
    int n = 0;
    while (esp_sntp_get_sync_status() != SNTP_SYNC_STATUS_COMPLETED && n < 120) {
        vTaskDelay(pdMS_TO_TICKS(500));
        n++;
    }
    n = 0;
    time_t now = time(NULL);
    while (now < (time_t)1700000000 && n < 120) {
        vTaskDelay(pdMS_TO_TICKS(500));
        now = time(NULL);
        n++;
    }
    if (now < (time_t)1700000000) {
        ESP_LOGW(TAG, "SNTP 可能未同步，华为云 HMAC 时间戳可能错误，鉴权易失败");
    }
}

static int build_utc_timestamp_yyyymmddhh(char *out11)
{
    time_t now = time(NULL);
    struct tm tm_utc;
    if (gmtime_r(&now, &tm_utc) == NULL) {
        return -1;
    }
    /* 华为要求共 10 位数字 + '\\0'；先写入足够大的缓冲区，避免 -Wformat-truncation */
    char buf[16];
    int n = snprintf(buf, sizeof(buf), "%04d%02d%02d%02d",
                     tm_utc.tm_year + 1900,
                     tm_utc.tm_mon + 1,
                     tm_utc.tm_mday,
                     tm_utc.tm_hour);
    if (n != 10) {
        return -1;
    }
    memcpy(out11, buf, 10);
    out11[10] = '\0';
    return 0;
}

static int huawei_hmac_password_hex(const char *ts10, const char *secret_ascii, char *out65)
{
    const mbedtls_md_info_t *md = mbedtls_md_info_from_type(MBEDTLS_MD_SHA256);
    if (md == NULL) {
        return -1;
    }
    unsigned char mac[32];
    int ret = mbedtls_md_hmac(md,
                              (const unsigned char *)ts10,
                              strlen(ts10),
                              (const unsigned char *)secret_ascii,
                              strlen(secret_ascii),
                              mac);
    if (ret != 0) {
        return ret;
    }
    for (int i = 0; i < 32; i++) {
        snprintf(out65 + i * 2, 3, "%02x", mac[i]);
    }
    out65[64] = '\0';
    return 0;
}

static esp_err_t huawei_prepare_mqtt_credentials(void)
{
    const char *device_id = CONFIG_AIRPORT_IOT_DEVICE_ID;
    const char *secret = CONFIG_AIRPORT_IOT_DEVICE_SECRET;

    char ts[11];
    if (build_utc_timestamp_yyyymmddhh(ts) != 0) {
        return ESP_FAIL;
    }

    if (huawei_hmac_password_hex(ts, secret, s_mqtt_password_hex) != 0) {
        ESP_LOGE(TAG, "HMAC 密码计算失败");
        return ESP_FAIL;
    }

    int n = snprintf(s_mqtt_client_id, sizeof(s_mqtt_client_id), "%s_0_0_%s", device_id, ts);
    if (n <= 0 || n >= (int)sizeof(s_mqtt_client_id)) {
        ESP_LOGE(TAG, "ClientId 过长");
        return ESP_ERR_INVALID_SIZE;
    }

    ESP_LOGI(TAG, "华为 MQTT 鉴权: ClientId=%s", s_mqtt_client_id);
    ESP_LOGI(TAG, "华为 MQTT 鉴权: UserName=%s (Password=HMAC，已隐藏)", device_id);
    return ESP_OK;
}

static void wifi_event_handler(void *arg, esp_event_base_t event_base, int32_t event_id, void *event_data)
{
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        ESP_LOGW(TAG, "WiFi 断开，重连中...");
        esp_wifi_connect();
    }
}

static void on_got_ip(void *arg, esp_event_base_t event_base, int32_t event_id, void *event_data)
{
    ip_event_got_ip_t *event = (ip_event_got_ip_t *)event_data;
    ESP_LOGI(TAG, "已获取 IP: " IPSTR, IP2STR(&event->ip_info.ip));

    esp_sntp_setoperatingmode(SNTP_OPMODE_POLL);
    esp_sntp_setservername(0, "pool.ntp.org");
    esp_sntp_set_sync_mode(SNTP_SYNC_MODE_IMMED);
    esp_sntp_init();

    wait_utc_time_for_huawei();

    if (s_mqtt == NULL) {
        if (huawei_prepare_mqtt_credentials() != ESP_OK) {
            ESP_LOGE(TAG, "华为 MQTT 凭据生成失败");
            return;
        }

        esp_mqtt_client_config_t mqtt_cfg = {
            .broker.address.uri = CONFIG_AIRPORT_IOT_MQTT_URI,
            .broker.verification.crt_bundle_attach = esp_crt_bundle_attach,
            .credentials.username = CONFIG_AIRPORT_IOT_DEVICE_ID,
            .credentials.authentication.password = s_mqtt_password_hex,
            .credentials.client_id = s_mqtt_client_id,
            .session.keepalive = 60,
        };

        s_mqtt = esp_mqtt_client_init(&mqtt_cfg);
        esp_mqtt_client_register_event(s_mqtt, ESP_EVENT_ANY_ID, mqtt_event_handler, NULL);
        esp_err_t err = esp_mqtt_client_start(s_mqtt);
        if (err != ESP_OK) {
            ESP_LOGE(TAG, "MQTT 启动失败: %s", esp_err_to_name(err));
        }
    }
}

static void mqtt_event_handler(void *handler_args, esp_event_base_t base, int32_t event_id, void *event_data)
{
    esp_mqtt_event_handle_t event = event_data;

    switch ((esp_mqtt_event_id_t)event_id) {
    case MQTT_EVENT_CONNECTED:
        ESP_LOGI(TAG, "MQTT 已连接华为云 Broker");
        s_mqtt_connected = true;
        break;
    case MQTT_EVENT_DISCONNECTED:
        ESP_LOGW(TAG, "MQTT 断开");
        s_mqtt_connected = false;
        break;
    case MQTT_EVENT_ERROR:
        if (event->error_handle != NULL &&
            event->error_handle->error_type == MQTT_ERROR_TYPE_TCP_TRANSPORT) {
            ESP_LOGE(TAG, "MQTT 传输错误: esp_tls_last_esp_err=%x",
                     event->error_handle->esp_tls_last_esp_err);
        }
        break;
    default:
        break;
    }
}

static esp_err_t wifi_init_sta(void)
{
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    esp_netif_create_default_wifi_sta();

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));

    esp_event_handler_instance_t instance_any_id;
    esp_event_handler_instance_t instance_got_ip;
    ESP_ERROR_CHECK(esp_event_handler_instance_register(WIFI_EVENT,
                                                        ESP_EVENT_ANY_ID,
                                                        &wifi_event_handler,
                                                        NULL,
                                                        &instance_any_id));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(IP_EVENT,
                                                        IP_EVENT_STA_GOT_IP,
                                                        &on_got_ip,
                                                        NULL,
                                                        &instance_got_ip));

    wifi_config_t wifi_config = {0};
    strncpy((char *)wifi_config.sta.ssid, CONFIG_AIRPORT_WIFI_SSID, sizeof(wifi_config.sta.ssid) - 1);
    strncpy((char *)wifi_config.sta.password, CONFIG_AIRPORT_WIFI_PASSWORD, sizeof(wifi_config.sta.password) - 1);
    wifi_config.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;

    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config));
    ESP_ERROR_CHECK(esp_wifi_start());

    ESP_LOGI(TAG, "WiFi 初始化完成，SSID=%s", CONFIG_AIRPORT_WIFI_SSID);
    return ESP_OK;
}

esp_err_t airport_connectivity_start(void)
{
    ESP_RETURN_ON_ERROR(wifi_init_sta(), TAG, "WiFi 启动失败");
    return ESP_OK;
}

bool airport_mqtt_is_ready(void)
{
    return s_mqtt_connected && (s_mqtt != NULL);
}

esp_err_t airport_mqtt_publish_location(const char *topic, const char *payload)
{
    if (s_mqtt == NULL || topic == NULL || payload == NULL) {
        return ESP_ERR_INVALID_ARG;
    }
    int id = esp_mqtt_client_publish(s_mqtt, topic, payload, 0, 1, 0);
    if (id < 0) {
        ESP_LOGW(TAG, "esp_mqtt_client_publish 失败, msg_id=%d (常见: 未连接或 outbox 满)", id);
    }
    return (id >= 0) ? ESP_OK : ESP_FAIL;
}

#else /* !CONFIG_AIRPORT_USE_WIFI_MQTT */

esp_err_t airport_connectivity_start(void)
{
    ESP_LOGW(TAG, "WiFi / 华为云 MQTT 已关闭（menuconfig: 启用 WiFi + 华为云 MQTT 上报）");
    ESP_LOGI(TAG, "当前模式：仅 GPS → 解析后经 USB 串口输出（PC 上一般为 COMx，波特率见 menuconfig 控制台）");
    return ESP_OK;
}

bool airport_mqtt_is_ready(void)
{
    return false;
}

esp_err_t airport_mqtt_publish_location(const char *topic, const char *payload)
{
    (void)topic;
    (void)payload;
    return ESP_ERR_NOT_SUPPORTED;
}

#endif /* CONFIG_AIRPORT_USE_WIFI_MQTT */

void airport_ble_reserve_interface(void)
{
    ESP_LOGI(TAG, "蓝牙：预留接口（未初始化协议栈，后续可在此接入 NimBLE 配网或透传）");
}
