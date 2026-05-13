#include "gps_nmea.h"
#include <ctype.h>
#include <math.h>
#include <stdlib.h>
#include <string.h>

static double nmea_to_decimal_degrees(const char *field, char dir_ch)
{
    if (field == NULL || field[0] == '\0') {
        return NAN;
    }
    double raw = strtod(field, NULL);
    int deg = (int)(raw / 100);
    double minutes = raw - (double)deg * 100.0;
    double dec = (double)deg + minutes / 60.0;
    if (dir_ch == 'S' || dir_ch == 's' || dir_ch == 'W' || dir_ch == 'w') {
        dec = -dec;
    }
    return dec;
}

bool nmea_rmc_is_void(const char *line)
{
    if (line == NULL) {
        return false;
    }
    while (*line && isspace((unsigned char)*line)) {
        line++;
    }
    if (strncmp(line, "$GNRMC", 6) != 0 && strncmp(line, "$GPRMC", 6) != 0) {
        return false;
    }
    char buf[256];
    size_t n = strnlen(line, sizeof(buf) - 1);
    if (n >= sizeof(buf)) {
        return false;
    }
    memcpy(buf, line, n + 1);
    char *save = NULL;
    char *tok = strtok_r(buf, ",", &save);
    char status = '\0';
    for (int idx = 0; idx < 3 && tok != NULL; idx++) {
        if (idx == 2) {
            status = tok[0];
            break;
        }
        tok = strtok_r(NULL, ",", &save);
    }
    return status == 'V';
}

bool nmea_try_parse_gga(const char *line, nmea_fix_t *out)
{
    if (line == NULL || out == NULL) {
        return false;
    }
    while (*line && isspace((unsigned char)*line)) {
        line++;
    }
    if (strncmp(line, "$GNGGA", 6) != 0 && strncmp(line, "$GPGGA", 6) != 0) {
        return false;
    }

    char buf[256];
    size_t n = strnlen(line, sizeof(buf) - 1);
    if (n >= sizeof(buf)) {
        return false;
    }
    memcpy(buf, line, n + 1);

    char *save = NULL;
    char *tok = strtok_r(buf, ",", &save);
    int idx = 0;
    char lat_str[16] = {0};
    char ns = '\0';
    char lon_str[16] = {0};
    char ew = '\0';
    char qual_str[8] = {0};

    while (tok != NULL && idx < 16) {
        switch (idx) {
            case 2:
                strncpy(lat_str, tok, sizeof(lat_str) - 1);
                break;
            case 3:
                ns = tok[0];
                break;
            case 4:
                strncpy(lon_str, tok, sizeof(lon_str) - 1);
                break;
            case 5:
                ew = tok[0];
                break;
            case 6:
                strncpy(qual_str, tok, sizeof(qual_str) - 1);
                break;
            default:
                break;
        }
        tok = strtok_r(NULL, ",", &save);
        idx++;
    }

    int qual = atoi(qual_str);
    if (qual < 1) {
        out->valid = false;
        return false;
    }

    double lat = nmea_to_decimal_degrees(lat_str, ns);
    double lon = nmea_to_decimal_degrees(lon_str, ew);
    if (isnan(lat) || isnan(lon)) {
        out->valid = false;
        return false;
    }

    out->latitude = lat;
    out->longitude = lon;
    out->speed_kmh = 0.0f;
    out->course_deg = 0.0f;
    out->valid = true;
    return true;
}

bool nmea_try_parse_rmc(const char *line, nmea_fix_t *out)
{
    if (line == NULL || out == NULL) {
        return false;
    }

    while (*line && isspace((unsigned char)*line)) {
        line++;
    }
    if (strncmp(line, "$GNRMC", 6) != 0 && strncmp(line, "$GPRMC", 6) != 0) {
        return false;
    }

    char buf[256];
    size_t n = strnlen(line, sizeof(buf) - 1);
    if (n >= sizeof(buf)) {
        return false;
    }
    memcpy(buf, line, n + 1);

    char *save = NULL;
    char *tok = strtok_r(buf, ",", &save);
    int idx = 0;
    char status = '\0';
    char lat_str[16] = {0};
    char ns = '\0';
    char lon_str[16] = {0};
    char ew = '\0';
    char knot_str[16] = {0};
    char course_str[16] = {0};

    while (tok != NULL && idx < 16) {
        switch (idx) {
            case 2:
                status = tok[0];
                break;
            case 3:
                strncpy(lat_str, tok, sizeof(lat_str) - 1);
                break;
            case 4:
                ns = tok[0];
                break;
            case 5:
                strncpy(lon_str, tok, sizeof(lon_str) - 1);
                break;
            case 6:
                ew = tok[0];
                break;
            case 7:
                strncpy(knot_str, tok, sizeof(knot_str) - 1);
                break;
            case 8:
                strncpy(course_str, tok, sizeof(course_str) - 1);
                break;
            default:
                break;
        }
        tok = strtok_r(NULL, ",", &save);
        idx++;
    }

    if (status != 'A') {
        out->valid = false;
        return false;
    }

    double lat = nmea_to_decimal_degrees(lat_str, ns);
    double lon = nmea_to_decimal_degrees(lon_str, ew);
    if (isnan(lat) || isnan(lon)) {
        out->valid = false;
        return false;
    }

    float knots = knot_str[0] ? (float)strtod(knot_str, NULL) : 0.0f;
    float kmh = knots * 1.852f;
    float course = course_str[0] ? (float)strtod(course_str, NULL) : 0.0f;

    out->latitude = lat;
    out->longitude = lon;
    out->speed_kmh = kmh;
    out->course_deg = course;
    out->valid = true;
    return true;
}
