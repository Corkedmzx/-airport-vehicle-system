# 始终声明 WiFi/MQTT/netif 依赖，避免 CMake 条件与 #if CONFIG_AIRPORT_USE_WIFI_MQTT 不一致导致缺头文件。
# 未启用 WiFi 时 airport_connectivity.c 不会包含相关头，链接器会丢弃未用符号。
set(priv_req
    esp_driver_uart
    nvs_flash
    esp_event
    esp_timer
    mqtt
    esp_wifi
    esp_netif
    mbedtls
)

idf_component_register(
    SRCS
        "main.c"
        "gps_nmea.c"
        "airport_connectivity.c"
    INCLUDE_DIRS
        "."
    PRIV_REQUIRES
        ${priv_req}
)
