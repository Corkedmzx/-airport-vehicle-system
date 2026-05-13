package com.airport.controller;

import com.airport.dto.Result;
import com.airport.service.MultiDeviceMqttService;
import com.airport.websocket.VehicleLocationService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MQTT 位置上传（PC 浏览器）
 * <p>
 * 使用 <b>web_001</b> 身份直连发布到 web 的 {@code user/location} 主题，由 web_001 连接消费并入库/WebSocket。
 * <b>vehicle_001</b> 仅用于小程序链路（mobile_001 → vehicle_001 → web_001）；硬件 GPS 以 MQTT 订阅为主，
 * 若华为侧仅走「数据转发」而不向应用 MQTT 扇出，可配置 {@code /mqtt/iot-forward-location} HTTP 投递闭合链路。
 *
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/mqtt")
@RequiredArgsConstructor
@Tag(name = "MQTT位置上传", description = "PC位置信息上传到华为云IoT平台")
public class MqttLocationController {

    private final MultiDeviceMqttService multiDeviceMqttService;
    private final VehicleLocationService vehicleLocationService;
    private final ObjectMapper objectMapper;

    @Value("${huawei.iot.mqtt.instance-id:a494d922-ff97-4873-bd0c-2d6b1a72086d}")
    private String instanceId;

    /**
     * 与华为云「数据转发」HTTP 投递共用的简单密钥；非空时开放 {@link #receiveIotForwardLocation}。
     */
    @Value("${huawei.iot.forward-webhook-secret:}")
    private String forwardWebhookSecret;

    /**
     * 上传 PC 浏览器位置（不经 vehicle_001，与小程序/GPS 链路解耦）
     */
    @PostMapping("/upload-pc-location")
    @Operation(summary = "上传PC位置信息", description = "使用 web_001 MQTT 连接发布到 web 的 user/location 主题")
    public Result<String> uploadPCLocation(@RequestBody Map<String, Object> locationData) {
        try {
            log.info("[PC位置上传] 收到PC位置上传统计请求: {}", locationData);

            if (!multiDeviceMqttService.isDeviceConnected("web_001")) {
                log.error("[PC位置上传] web_001 设备未连接，无法上传 PC 位置");
                return Result.error("web_001 设备未连接，请检查 MQTT 多设备初始化与密钥文件");
            }

            String webDeviceId = multiDeviceMqttService.getLoadedDeviceId("web_001");
            if (webDeviceId == null || webDeviceId.isBlank()) {
                log.error("[PC位置上传] 未加载 web_001 的 device_id");
                return Result.error("web_001 设备信息未加载");
            }

            // 提取位置信息
            Double latitude = getDoubleValue(locationData, "latitude");
            Double longitude = getDoubleValue(locationData, "longitude");
            Double accuracy = getDoubleValue(locationData, "accuracy");
            Long timestamp = getLongValue(locationData, "timestamp");

            if (latitude == null || longitude == null) {
                return Result.error("位置信息不完整：缺少latitude或longitude");
            }

            // 构建位置数据
            Map<String, Object> locationMessage = new HashMap<>();
            locationMessage.put("deviceId", webDeviceId);
            locationMessage.put("deviceName", "pc_location"); // 标识为PC位置
            locationMessage.put("vehicleId", null); // PC位置没有关联车辆
            locationMessage.put("vehicleNo", "PC位置"); // 显示名称
            locationMessage.put("latitude", latitude);
            locationMessage.put("longitude", longitude);
            if (accuracy != null) {
                locationMessage.put("accuracy", accuracy);
            }
            if (timestamp != null) {
                locationMessage.put("timestamp", timestamp);
            } else {
                locationMessage.put("timestamp", System.currentTimeMillis());
            }
            locationMessage.put("source", "pc_browser"); // 标识来源为PC浏览器

            // 构建主题：web_001订阅的主题
            // 格式：/{instanceId}/{web_001_deviceId}/user/location
            String topic = String.format("/%s/%s/user/location", instanceId, webDeviceId);

            // 转换为JSON字符串
            String payload = objectMapper.writeValueAsString(locationMessage);

            multiDeviceMqttService.publishToDevice("web_001", topic, payload, 1);

            log.info("[PC位置上传] PC 位置已由 web_001 发布，主题: {}, 位置: ({}, {}), 精度: {}米",
                    topic, latitude, longitude, accuracy != null ? accuracy : "未知");

            return Result.success("PC位置信息上传成功");
        } catch (Exception e) {
            log.error("[PC位置上传] 上传PC位置信息失败", e);
            // 记录详细的错误堆栈，便于排查问题
            log.error("[PC位置上传] 错误详情: {}", e.getClass().getName() + ": " + e.getMessage(), e);
            return Result.error("上传PC位置信息失败: " + e.getMessage());
        }
    }

    /**
     * 华为 IoT「数据转发」HTTP 推送到本服务（当平台 MQTT 不向 Java 客户端投递设备上行时，用此闭合链路）。
     * <p>
     * 鉴权二选一（均须配置 {@code huawei.iot.forward-webhook-secret} 与控制台 Token 一致）：
     * <ul>
     *   <li>华为控制台「鉴权」默认：请求头 {@code timestamp}、{@code nonce}、{@code signature}（对 token+nonce+timestamp 字典序拼接后 SHA256，见华为文档）</li>
     *   <li>兼容：请求头 {@code X-IoT-Webhook-Token} 与密钥明文相同（便于 curl/ngrok 自测）</li>
     * </ul>
     * Body 可为设备直传 JSON，或华为常见封装（含 {@code notify_data}、{@code body}、{@code services} 等），服务端会展平后再取经纬度。
     * {@code deviceId} 优先使用查询参数；否则从 JSON 的 {@code device_id} / {@code deviceId} / {@code topic} 推断。
     */
    @PostMapping("/iot-forward-location")
    @Operation(summary = "华为数据转发 HTTP 投递（硬件 GPS）", description = "需配置 forward-webhook-secret；鉴权：华为 signature 或 X-IoT-Webhook-Token")
    public Result<String> receiveIotForwardLocation(
            HttpServletRequest request,
            @RequestHeader(value = "X-IoT-Webhook-Token", required = false) String token,
            @RequestParam(value = "deviceId", required = false) String deviceIdParam,
            @RequestBody(required = false) byte[] rawBody) {
        int bodyLen = rawBody == null ? 0 : rawBody.length;
        String sigHdr = headerFirstNonBlank(request, "signature");
        String tsHdr = headerFirstNonBlank(request, "timestamp");
        String nonceHdr = headerFirstNonBlank(request, "nonce");
        boolean legacyToken = token != null && !token.isBlank();
        boolean huaweiSig = sigHdr != null && tsHdr != null && nonceHdr != null;
        log.info("[IoT转发HTTP] 收到推送 bodyLen={} authMode={}", bodyLen,
                legacyToken ? "X-IoT-Webhook-Token" : (huaweiSig ? "huawei_signature" : "none"));
        if (forwardWebhookSecret == null || forwardWebhookSecret.isBlank()) {
            log.warn("[IoT转发HTTP] 未配置 huawei.iot.forward-webhook-secret（或环境变量 HUAWEI_IOT_FORWARD_WEBHOOK_SECRET），拒绝处理");
            return Result.error("未配置 huawei.iot.forward-webhook-secret，本接口已禁用。请在 backend/.env 或环境中设置 HUAWEI_IOT_FORWARD_WEBHOOK_SECRET=与华为控制台 Token 相同的值后重启。");
        }
        boolean authOk = forwardWebhookSecret.equals(token)
                || verifyHuaweiIotHttpPushSignature(forwardWebhookSecret, nonceHdr, tsHdr, sigHdr);
        if (!authOk) {
            log.warn("[IoT转发HTTP] 拒绝：鉴权失败（非华为 signature 且非 X-IoT-Webhook-Token 明文匹配）");
            return Result.error("禁止访问");
        }
        if (rawBody == null || rawBody.length == 0) {
            log.warn("[IoT转发HTTP] Body 为空");
            return Result.error("Body 为空");
        }
        try {
            JsonNode tree = objectMapper.readTree(rawBody);
            if (!tree.isObject()) {
                log.warn("[IoT转发HTTP] Body 非 JSON 对象，前200字符: {}", previewUtf8(rawBody, 200));
                return Result.error("Body 须为 JSON 对象");
            }
            Map<String, Object> root = objectMapper.convertValue(tree, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> payload = flattenHuaweiWebhookBody(root);
            payload = unwrapIotForwardPayload(payload);
            hoistDeviceMessageContent(payload);

            String deviceId = deviceIdParam;
            if (deviceId == null || deviceId.isBlank()) {
                deviceId = firstNonBlank(payload.get("device_id"), payload.get("deviceId"));
            }
            if (deviceId == null || deviceId.isBlank()) {
                Object topicObj = payload.get("topic");
                if (topicObj != null) {
                    deviceId = extractDeviceIdFromTopic(topicObj.toString());
                }
            }
            if (deviceId == null || deviceId.isBlank()) {
                log.warn("[IoT转发HTTP] 无法解析 deviceId，展平后键: {} 前300字符: {}",
                        payload.keySet(), previewUtf8(rawBody, 300));
                return Result.error("缺少 deviceId：请使用查询参数 deviceId=…，或在 JSON 中提供 device_id / topic");
            }
            Double lat = pickLatitude(payload);
            Double lon = pickLongitude(payload);
            if (lat == null || lon == null) {
                log.warn("[IoT转发HTTP] 缺少经纬度，展平后键: {} 前400字符: {}",
                        payload.keySet(), previewUtf8(rawBody, 400));
                return Result.error("展平后仍缺少 latitude 或 longitude，请对照华为转发实际 Body 调整规则或联系开发扩展解析");
            }
            payload.put("latitude", lat);
            payload.put("longitude", lon);
            boolean applied = vehicleLocationService.processLocationUpdate(deviceId, payload);
            if (!applied) {
                log.warn("[IoT转发HTTP] 未落库：deviceId={} 与 vehicle.gps_device_id 无匹配，或处理异常；地图不会出现该车绿点", deviceId);
                return Result.error("未关联车辆：请将绑定 GPS 的那条 vehicle 记录的 gps_device_id 设为与华为上报 deviceId 完全一致（当前上报 deviceId=" + deviceId + "）");
            }
            log.info("[IoT转发HTTP] 已入库/WebSocket deviceId={} ({}, {})", deviceId, lat, lon);
            return Result.success("ok");
        } catch (Exception e) {
            log.error("[IoT转发HTTP] 处理失败，Body前400字符: {}", previewUtf8(rawBody, 400), e);
            return Result.error("处理失败: " + e.getMessage());
        }
    }

    /**
     * 说明本服务如何接收华为「数据转发」与 MQTT 的关系（无需鉴权，便于在浏览器或 curl 自测联通）。
     */
    @GetMapping("/iot-forward-location")
    @Operation(summary = "华为 HTTP 转发接入说明", description = "返回本机 webhook 是否启用及接入要点")
    public Result<Map<String, Object>> iotForwardLocationInfo() {
        Map<String, Object> m = new LinkedHashMap<>();
        boolean enabled = forwardWebhookSecret != null && !forwardWebhookSecret.isBlank();
        m.put("httpWebhookEnabled", enabled);
        m.put("postPath", "/api/mqtt/iot-forward-location");
        m.put("method", "POST");
        m.put("authHuaweiDefault", "控制台勾选鉴权时：请求头 timestamp、nonce、signature（SHA256，规则见华为「HTTP/HTTPS推送基于Token认证」文档），密钥= huawei.iot.forward-webhook-secret");
        m.put("authLegacy", "可选：请求头 X-IoT-Webhook-Token 与 forward-webhook-secret 明文相同（调试用）");
        m.put("queryRecommended", "deviceId=华为完整设备ID（如 …_vehicle_002）");
        m.put("body", "华为「设备消息上报」规则 SQL 建议输出 device_id + content（见项目说明）；本服务会展平 notify_data、body、content 与物模型 services");
        m.put("mqttVsForward", "控制台「消息跟踪/数据转发成功」表示平台已收或已触发规则，不保证会向你们 Java 的 MQTT 客户端再投递 PUBLISH；收不到时请以本 HTTP 为主路径。");
        m.put("vehicle001Mqtt", "vehicle_001 跨设备订阅 vehicle_002 为辅助路径，与华为转发相互独立。");
        return Result.success(m);
    }

    private static String previewUtf8(byte[] raw, int maxChars) {
        String s = new String(raw, StandardCharsets.UTF_8);
        if (s.length() <= maxChars) {
            return s;
        }
        return s.substring(0, maxChars) + "...";
    }

    private static String headerFirstNonBlank(HttpServletRequest request, String name) {
        String v = request.getHeader(name);
        if (v == null) {
            return null;
        }
        v = v.trim();
        return v.isEmpty() ? null : v;
    }

    /**
     * 华为云文档「HTTP/HTTPS推送基于Token认证」：将 token、nonce、timestamp 中非空项加入列表、字典排序后拼接，再对拼接串做 SHA256 十六进制，与请求头 signature 比较。
     */
    private static boolean verifyHuaweiIotHttpPushSignature(String token, String nonce, String timestamp, String signature) {
        if (token == null || token.isEmpty() || signature == null || signature.isBlank()) {
            return false;
        }
        List<String> list = new ArrayList<>();
        list.add(token);
        if (nonce != null && !nonce.isEmpty()) {
            list.add(nonce);
        }
        if (timestamp != null && !timestamp.isEmpty()) {
            list.add(timestamp);
        }
        Collections.sort(list);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            sb.append(s);
        }
        String expected = sha256HexLower(sb.toString());
        return expected.equalsIgnoreCase(signature.trim());
    }

    private static String sha256HexLower(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(String.format("%02x", b & 0xff));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    /** 与 {@link com.airport.service.impl.MultiDeviceMqttServiceImpl#extractDeviceId} 同构，供 HTTP topic 推断 deviceId */
    private static String extractDeviceIdFromTopic(String topic) {
        if (topic == null || !topic.contains("/")) {
            return null;
        }
        String[] parts = topic.split("/");
        List<String> segs = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isEmpty()) {
                segs.add(p);
            }
        }
        if (segs.size() >= 3 && "$oc".equals(segs.get(0))) {
            return segs.get(2);
        }
        if (segs.size() >= 2) {
            return segs.get(1);
        }
        return null;
    }

    /**
     * 展平华为 HTTP 转发常见嵌套（notify_data 为 JSON 字符串、body、物模型 services 等）。
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> flattenHuaweiWebhookBody(Map<String, Object> body) throws Exception {
        if (body == null) {
            return new HashMap<>();
        }
        LinkedHashMap<String, Object> m = new LinkedHashMap<>(body);
        Object nd = m.get("notify_data");
        if (nd instanceof String str) {
            String t = str.trim();
            if (t.startsWith("{")) {
                Map<String, Object> inner = objectMapper.readValue(t, new TypeReference<Map<String, Object>>() {});
                m.remove("notify_data");
                Map<String, Object> innerFlat = flattenHuaweiWebhookBody(inner);
                innerFlat.forEach(m::putIfAbsent);
            }
        }
        Object b = m.get("body");
        if (b instanceof Map<?, ?> bm) {
            ((Map<String, Object>) bm).forEach(m::putIfAbsent);
        }
        Object services = m.get("services");
        if (services instanceof Map<?, ?> sm) {
            for (Object v : sm.values()) {
                if (!(v instanceof Map<?, ?> svc)) {
                    continue;
                }
                Object props = ((Map<?, ?>) svc).get("properties");
                if (props instanceof Map<?, ?> pm) {
                    ((Map<String, Object>) pm).forEach(m::putIfAbsent);
                    break;
                }
                ((Map<String, Object>) svc).forEach(m::putIfAbsent);
            }
        }
        // 设备消息上报：notify_data.body.content 为业务 JSON（如 GPS 的 latitude/longitude）
        hoistDeviceMessageContent(m);
        return m;
    }

    /**
     * 将华为规则里 {@code body.content}（或已合并到根上的 {@code content}）展到顶层，便于取 latitude/longitude。
     */
    @SuppressWarnings("unchecked")
    private static void hoistDeviceMessageContent(Map<String, Object> m) {
        if (m == null) {
            return;
        }
        Object c = m.get("content");
        if (c instanceof Map<?, ?> cm) {
            ((Map<String, Object>) cm).forEach(m::putIfAbsent);
        }
    }

    /**
     * 将 {@code content}/{@code payload}/{@code data} 等嵌套对象<strong>合并进根 Map</strong>，避免仅返回子 Map 时丢失根上的
     * {@code device_id}、{@code topic} 等字段（华为常见格式会导致无法解析 deviceId、地图无绿点）。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> unwrapIotForwardPayload(Map<String, Object> body) {
        if (body == null) {
            return new HashMap<>();
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<>(body);
        mergeNestedMapInto(out, "content");
        mergeNestedMapInto(out, "payload");
        mergeNestedMapInto(out, "data");
        return out;
    }

    @SuppressWarnings("unchecked")
    private static void mergeNestedMapInto(Map<String, Object> out, String key) {
        Object child = out.get(key);
        if (child instanceof Map<?, ?> cm) {
            ((Map<String, Object>) cm).forEach(out::putIfAbsent);
        }
    }

    private static String firstNonBlank(Object a, Object b) {
        String s1 = a != null ? a.toString().trim() : "";
        if (!s1.isEmpty()) {
            return s1;
        }
        String s2 = b != null ? b.toString().trim() : "";
        return s2.isEmpty() ? null : s2;
    }

    private static Double pickLatitude(Map<String, Object> map) {
        return firstNonNullDouble(map, "latitude", "Latitude", "lat", "Lat", "GPS_latitude");
    }

    private static Double pickLongitude(Map<String, Object> map) {
        return firstNonNullDouble(map, "longitude", "Longitude", "lng", "lon", "Lng", "GPS_longitude");
    }

    private static Double firstNonNullDouble(Map<String, Object> map, String... keys) {
        for (String k : keys) {
            Double v = getDoubleValueStatic(map, k);
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    private static Double getDoubleValueStatic(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取double值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取long值
     */
    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
