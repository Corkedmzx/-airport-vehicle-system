package com.airport.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 华为云上「直连上报定位」的硬件设备 ID 集合（ESP 等），来自配置文件。
 * <p>
 * 支持逗号分隔多 ID；保留旧项 {@code hardware-gps-device-id} 单值兼容。
 * MQTT 额外订阅与坐标转换策略见 {@link com.airport.service.impl.MultiDeviceMqttServiceImpl}
 * 与 {@link com.airport.websocket.VehicleLocationServiceImpl}。
 */
@Component
public class HardwareGpsDeviceRegistry {

    private final Set<String> configuredIds;

    public HardwareGpsDeviceRegistry(
            @Value("${huawei.iot.mqtt.hardware-gps-device-ids:}") String commaSeparatedIds,
            @Value("${huawei.iot.mqtt.hardware-gps-device-id:}") String legacySingleId) {
        Set<String> ids = new LinkedHashSet<>();
        if (commaSeparatedIds != null && !commaSeparatedIds.isBlank()) {
            for (String part : commaSeparatedIds.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    ids.add(t);
                }
            }
        }
        if (ids.isEmpty() && legacySingleId != null && !legacySingleId.isBlank()) {
            ids.add(legacySingleId.trim());
        }
        this.configuredIds = Collections.unmodifiableSet(ids);
    }

    /** 配置文件中显式列出的硬件设备 ID（可为空） */
    public Set<String> getConfiguredDeviceIds() {
        return configuredIds;
    }

    public boolean isConfiguredDeviceId(String deviceId) {
        return deviceId != null && configuredIds.contains(deviceId);
    }
}
