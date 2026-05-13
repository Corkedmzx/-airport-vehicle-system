#pragma once

#include <stdbool.h>

/** NMEA RMC/GNRMC 解析结果（与机场系统后端 MQTT JSON 字段对应） */
typedef struct {
    double latitude;
    double longitude;
    float speed_kmh;
    float course_deg;
    bool valid;
} nmea_fix_t;

/**
 * 解析一行 NMEA（支持 $xxRMC，如 GPRMC、GNRMC）。
 * @return true 表示解析且定位有效（状态 A）
 */
bool nmea_try_parse_rmc(const char *line, nmea_fix_t *out);

/** 是否为 RMC 且定位无效（状态 V，室内/无星常见） */
bool nmea_rmc_is_void(const char *line);

/**
 * 解析 $GNGGA/$GPGGA：定位质量≥1 时有有效经纬度（部分模块 RMC 仍为 V 时已可先出 GGA）。
 */
bool nmea_try_parse_gga(const char *line, nmea_fix_t *out);
