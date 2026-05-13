#pragma once

#include <stdbool.h>

#include "esp_err.h"

#ifdef __cplusplus
extern "C" {
#endif

esp_err_t airport_connectivity_start(void);

void airport_ble_reserve_interface(void);

bool airport_mqtt_is_ready(void);

esp_err_t airport_mqtt_publish_location(const char *topic, const char *payload);

#ifdef __cplusplus
}
#endif
