/*
 * ESP32-S3 + GPS(E108-GN03D NMEA)
 *
 * 默认：关闭 WiFi/MQTT，解析后经 USB 串口打印 JSON（PC 上在设备管理器查看 USB 串口对应 COMx）。
 * 启用联网：menuconfig 打开「启用 WiFi + 华为云 MQTT 上报」。
 *
 * GPS 接线：TXD→GPIO10(RX)、RXD→GPIO11(TX)，波特率见 menuconfig。
 */

#include <inttypes.h>
#include <stdio.h>
#include <string.h>
#include <sys/time.h>

#include "airport_connectivity.h"
#include "driver/uart.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "gps_nmea.h"
#include "nvs_flash.h"

static const char *TAG = "airport_iot";

#if CONFIG_AIRPORT_GPS_UART_PORT == 0
#define GPS_UART UART_NUM_0
#elif CONFIG_AIRPORT_GPS_UART_PORT == 1
#define GPS_UART UART_NUM_1
#else
#define GPS_UART UART_NUM_2
#endif

static int64_t wall_clock_ms(void)
{
    struct timeval tv;
    gettimeofday(&tv, NULL);
    return (int64_t)tv.tv_sec * 1000LL + (int64_t)tv.tv_usec / 1000LL;
}

static void gps_uart_init(void)
{
    uart_config_t uart_config = {
        .baud_rate = CONFIG_AIRPORT_GPS_UART_BAUD,
        .data_bits = UART_DATA_8_BITS,
        .parity = UART_PARITY_DISABLE,
        .stop_bits = UART_STOP_BITS_1,
        .flow_ctrl = UART_HW_FLOWCTRL_DISABLE,
        .source_clk = UART_SCLK_DEFAULT,
    };

    ESP_ERROR_CHECK(uart_driver_install(GPS_UART, 4096, 0, 0, NULL, 0));
    ESP_ERROR_CHECK(uart_param_config(GPS_UART, &uart_config));
    ESP_ERROR_CHECK(uart_set_pin(GPS_UART,
                                 CONFIG_AIRPORT_GPS_UART_TX_PIN,
                                 CONFIG_AIRPORT_GPS_UART_RX_PIN,
                                 UART_PIN_NO_CHANGE,
                                 UART_PIN_NO_CHANGE));
}

static void location_worker_task(void *pv)
{
    char line[256];
    size_t line_len = 0;
    nmea_fix_t fix = {0};
    bool have_fix = false;
    int64_t last_output_ms = 0;
    int64_t last_no_fix_hint_ms = 0;
    int64_t last_uart_diag_ms = 0;
    int64_t task_start_ms = 0;
    bool task_started = false;
    uint32_t uart_rx_total = 0;
    bool uart_got_any_byte = false;
    char topic[160];
    /*
     * 与后端 MultiDeviceMqttServiceImpl 订阅格式一致：/{实例ID}/{设备ID}/user/location
     * （华为亦支持 $oc/ 前缀，但若 Broker 对跨设备订阅拒绝 $oc，后端仅能订阅无 $oc 路径；
     *  发布与订阅必须使用同一字符串，否则 MQTT 收不到消息。）
     */
    snprintf(topic, sizeof(topic), "/%s/%s/user/location",
             CONFIG_AIRPORT_IOT_INSTANCE_ID,
             CONFIG_AIRPORT_IOT_DEVICE_ID);

    const int interval_ms = CONFIG_AIRPORT_LOCATION_PUBLISH_INTERVAL_MS;

    for (;;) {
        if (!task_started) {
            task_start_ms = wall_clock_ms();
            task_started = true;
        }

        uint8_t buf[128];
        int len = uart_read_bytes(GPS_UART, buf, sizeof(buf), pdMS_TO_TICKS(200));
        if (len > 0) {
            uart_rx_total += (uint32_t)len;
            if (!uart_got_any_byte) {
                uart_got_any_byte = true;
                ESP_LOGI(TAG, "GPS 模块串口已有数据输出(接线与波特率基本正常)");
            }
            for (int i = 0; i < len; i++) {
                char c = (char)buf[i];
                if (c == '\r') {
                    continue;
                }
                if (c == '\n') {
                    line[line_len] = '\0';
                    if (line_len > 0) {
                        if (nmea_rmc_is_void(line)) {
                            static int64_t last_void_log_ms;
                            int64_t t = wall_clock_ms();
                            if (last_void_log_ms == 0 || (t - last_void_log_ms) >= 30000) {
                                last_void_log_ms = t;
                                ESP_LOGW(TAG,
                                         "GPS 输出 RMC 状态 V：当前无有效定位(室内常见)，不上传 MQTT。");
                            }
                        }
                        nmea_fix_t parsed = {0};
                        if (nmea_try_parse_rmc(line, &parsed) && parsed.valid) {
                            fix = parsed;
                            have_fix = true;
                            ESP_LOGI(TAG,
                                     "定位有效(RMC): lat=%.7f lon=%.7f v=%.1fkm/h",
                                     fix.latitude,
                                     fix.longitude,
                                     (double)fix.speed_kmh);
                        } else if (nmea_try_parse_gga(line, &parsed) && parsed.valid) {
                            fix = parsed;
                            have_fix = true;
                            ESP_LOGI(TAG,
                                     "定位有效(GGA): lat=%.7f lon=%.7f (RMC 可能仍为 V，已用 GGA 上报)",
                                     fix.latitude,
                                     fix.longitude);
                        }
                    }
                    line_len = 0;
                } else if (line_len < sizeof(line) - 1) {
                    line[line_len++] = c;
                } else {
                    line_len = 0;
                }
            }
        }

        int64_t loop_now = wall_clock_ms();
        if (last_uart_diag_ms == 0 || (loop_now - last_uart_diag_ms) >= 40000) {
            last_uart_diag_ms = loop_now;
#if CONFIG_AIRPORT_USE_WIFI_MQTT
            bool mq = airport_mqtt_is_ready();
#else
            bool mq = false;
#endif
            ESP_LOGI(TAG,
                     "诊断(每40s): UART累计接收=%" PRIu32 " 字节 | have_fix=%d | MQTT就绪=%d",
                     uart_rx_total,
                     have_fix ? 1 : 0,
                     mq ? 1 : 0);
            if (!uart_got_any_byte && task_started &&
                (loop_now - task_start_ms) > 60000) {
                ESP_LOGW(TAG,
                         "超过60秒 GPS UART 仍无任意字节：请查接线 GPS.TXD→ESP GPIO%d(RX)、GND、供电与 menuconfig 波特率",
                         CONFIG_AIRPORT_GPS_UART_RX_PIN);
            }
        }

        if (!have_fix) {
            int64_t hint_now = wall_clock_ms();
            if (last_no_fix_hint_ms == 0 || (hint_now - last_no_fix_hint_ms) >= 25000) {
                last_no_fix_hint_ms = hint_now;
                ESP_LOGW(TAG,
                         "尚无有效 GPS 定位(RMC 状态≠A)，不会向 MQTT 上报经纬度，地图不会动。"
                         "室内通常搜不到星；请到室外或窗边，并确认天线朝向天空。");
#if CONFIG_AIRPORT_USE_WIFI_MQTT
                if (airport_mqtt_is_ready()) {
                    ESP_LOGI(TAG, "MQTT 已在线，仅缺 GPS 有效解；华为云侧无位置消息属正常。");
                }
#endif
            }
            continue;
        }

        int64_t now = wall_clock_ms();
        if (last_output_ms != 0 && (now - last_output_ms) < interval_ms) {
            continue;
        }

        char payload[320];
        snprintf(payload, sizeof(payload),
                 "{\"longitude\":%.8f,\"latitude\":%.8f,\"speed\":%.2f,\"direction\":%.2f,"
                 "\"timestamp\":%" PRId64 ",\"address\":\"GPS\"}",
                 fix.longitude,
                 fix.latitude,
                 (double)fix.speed_kmh,
                 (double)fix.course_deg,
                 now);

        if (airport_mqtt_is_ready()) {
            esp_err_t pr = airport_mqtt_publish_location(topic, payload);
            if (pr == ESP_OK) {
                last_output_ms = now;
                ESP_LOGI(TAG,
                         "MQTT 上报成功 lon=%.7f lat=%.7f | topic=%s",
                         fix.longitude,
                         fix.latitude,
                         topic);
                ESP_LOGD(TAG, "payload=%s", payload);
            } else {
                ESP_LOGE(TAG, "MQTT 上报失败: %s (请查华为云规则与网络)", esp_err_to_name(pr));
            }
        } else {
#if !CONFIG_AIRPORT_USE_WIFI_MQTT
            printf("%s\n", payload);
            fflush(stdout);
            last_output_ms = now;
#else
            static bool warned;
            if (!warned) {
                ESP_LOGW(TAG, "等待 MQTT 连接后再上报…");
                warned = true;
            }
#endif
        }
    }
}

void app_main(void)
{
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    ESP_LOGI(TAG, "机场车辆终端启动");
    airport_ble_reserve_interface();

#if !CONFIG_AIRPORT_USE_WIFI_MQTT
    ESP_LOGI(TAG, "模式：串口输出 JSON（设备管理器中的 USB 串口 COMx；波特率 = menuconfig「控制台」默认多为 115200）");
#endif

    gps_uart_init();
    ESP_ERROR_CHECK(airport_connectivity_start());

    xTaskCreate(location_worker_task, "gps_out", 8192, NULL, 5, NULL);
}
