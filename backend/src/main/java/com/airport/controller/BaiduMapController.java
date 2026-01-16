package com.airport.controller;

import com.airport.dto.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 百度地图API代理控制器
 * 
 * 通过后端代理调用百度地图API，避免前端暴露AK，同时可以使用IP白名单（支持端口）
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping({"/api/baidu-map", "/baidu-map"})
@RequiredArgsConstructor
@Tag(name = "百度地图代理", description = "百度地图API代理接口")
public class BaiduMapController {

    private final RestTemplate restTemplate;

    @Value("${baidu.map.ak:}")
    private String baiduMapAk;

    @Value("${baidu.map.api.base-url:https://api.map.baidu.com}")
    private String baseUrl;

    /**
     * 获取百度地图JavaScript API脚本
     * 返回包含AK的完整API URL
     */
    @GetMapping("/api-script")
    @Operation(summary = "获取百度地图API脚本URL", description = "返回包含AK的百度地图JavaScript API脚本URL")
    public Result<Map<String, String>> getApiScript() {
        try {
            // 优先从环境变量读取（如果配置了）
            String ak = baiduMapAk;
            if (ak == null || ak.isEmpty()) {
                // 尝试从系统属性读取（.env文件加载的）
                ak = System.getProperty("BAIDU_MAP_AK");
            }
            if (ak == null || ak.isEmpty()) {
                // 尝试从环境变量读取
                ak = System.getenv("BAIDU_MAP_AK");
            }
            
            if (ak == null || ak.isEmpty()) {
                log.warn("百度地图AK未配置，请检查：");
                log.warn("  1. backend/.env 文件中是否配置了 BAIDU_MAP_AK");
                log.warn("  2. 系统环境变量中是否设置了 BAIDU_MAP_AK");
                log.warn("  3. application.yml 中是否配置了 baidu.map.ak");
                return Result.error("百度地图AK未配置，请在backend/.env文件中配置BAIDU_MAP_AK，或在application.yml中配置baidu.map.ak");
            }

            String apiUrl = String.format("%s/api?v=3.0&ak=%s", baseUrl, ak);
            
            log.debug("返回百度地图API URL，AK: {}...", ak.length() > 8 ? ak.substring(0, 8) + "***" : "***");
            
            Map<String, String> result = new HashMap<>();
            result.put("apiUrl", apiUrl);
            result.put("ak", ak); // 注意：这里返回了AK，但前端只用于加载脚本，不会暴露给用户
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("获取百度地图API脚本失败", e);
            return Result.error("获取百度地图API脚本失败: " + e.getMessage());
        }
    }

    /**
     * 获取百度地图第一个脚本的内容（用于解析getscript URL）
     * 通过后端代理获取，避免CORS问题和Referer白名单限制
     */
    @GetMapping({"/first-script", "/api/baidu-map/first-script", "/baidu-map/first-script"})
    @Operation(summary = "获取百度地图第一个脚本内容", description = "通过后端代理获取百度地图API第一个脚本的内容，用于解析getscript URL")
    public Result<String> getFirstScript() {
        try {
            // 获取AK
            String ak = baiduMapAk;
            if (ak == null || ak.isEmpty()) {
                ak = System.getProperty("BAIDU_MAP_AK");
            }
            if (ak == null || ak.isEmpty()) {
                ak = System.getenv("BAIDU_MAP_AK");
            }
            
            if (ak == null || ak.isEmpty()) {
                return Result.error("百度地图AK未配置");
            }
            
            // 构建第一个脚本的URL
            String apiUrl = String.format("%s/api?v=3.0&ak=%s", baseUrl, ak);
            log.debug("通过后端代理获取百度地图第一个脚本: {}", apiUrl);
            
            // 通过后端调用百度地图API（使用服务端AK，支持IP白名单）
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("User-Agent", "Mozilla/5.0");
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("成功获取百度地图第一个脚本内容，长度: {}", response.getBody().length());
                return Result.success(response.getBody());
            } else {
                log.error("获取百度地图第一个脚本失败，状态码: {}", response.getStatusCode());
                return Result.error("获取百度地图脚本失败，状态码: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("获取百度地图第一个脚本失败", e);
            return Result.error("获取百度地图脚本失败: " + e.getMessage());
        }
    }

    /**
     * 获取百度地图getscript脚本内容
     * 通过后端代理获取，检查返回内容是否为错误页面
     */
    @GetMapping("/getscript")
    @Operation(summary = "获取百度地图getscript脚本内容", description = "通过后端代理获取getscript脚本内容，检查是否为错误页面")
    public Result<String> getGetscriptScript(@RequestParam(required = false) String url) {
        try {
            // 获取AK
            String ak = baiduMapAk;
            if (ak == null || ak.isEmpty()) {
                ak = System.getProperty("BAIDU_MAP_AK");
            }
            if (ak == null || ak.isEmpty()) {
                ak = System.getenv("BAIDU_MAP_AK");
            }
            
            if (ak == null || ak.isEmpty()) {
                return Result.error("百度地图AK未配置");
            }
            
            // 如果提供了URL，使用提供的URL；否则构建URL
            String getscriptUrl;
            if (url != null && !url.isEmpty()) {
                getscriptUrl = url;
            } else {
                // 构建getscript URL
                long timestamp = System.currentTimeMillis();
                getscriptUrl = String.format("%s/getscript?v=3.0&ak=%s&services=&t=%d", baseUrl, ak, timestamp);
            }
            
            log.debug("通过后端代理获取百度地图getscript脚本: {}", getscriptUrl);
            
            // 通过后端调用百度地图API（使用服务端AK，支持IP白名单）
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("User-Agent", "Mozilla/5.0");
            httpHeaders.set("Accept", "application/javascript, text/javascript, */*");
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            
            ResponseEntity<String> response = restTemplate.exchange(
                    getscriptUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String content = response.getBody();
                
                // 检查返回内容是否是错误页面
                if (content.contains("APP被您禁用") || 
                    content.contains("APP服务被禁用") ||
                    content.contains("被禁用") ||
                    (content.contains("确定") && content.length() < 1000)) {
                    log.error("百度地图getscript返回错误页面，内容预览: {}", content.substring(0, Math.min(200, content.length())));
                    return Result.error("百度地图应用被禁用。请检查：1. 应用状态是否为已启用；2. JavaScript API服务是否已启用；3. AK是否正确配置。详情查看: http://lbsyun.baidu.com/apiconsole/key");
                }
                
                // 检查是否是有效的JavaScript代码
                if (!content.contains("function") && !content.contains("var") && !content.contains("BMap")) {
                    log.warn("百度地图getscript返回内容可能不是有效的JavaScript代码，内容预览: {}", content.substring(0, Math.min(200, content.length())));
                }
                
                log.debug("成功获取百度地图getscript脚本内容，长度: {}", content.length());
                return Result.success(content);
            } else {
                log.error("获取百度地图getscript脚本失败，状态码: {}", response.getStatusCode());
                return Result.error("获取百度地图getscript脚本失败，状态码: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("获取百度地图getscript脚本失败", e);
            return Result.error("获取百度地图getscript脚本失败: " + e.getMessage());
        }
    }

    /**
     * 代理调用百度地图API
     * 支持所有百度地图API接口的代理调用
     */
    @GetMapping("/proxy/**")
    @Operation(summary = "代理调用百度地图API", description = "代理调用百度地图API，自动添加AK参数")
    public ResponseEntity<String> proxyApi(
            @RequestParam Map<String, String> allParams,
            @RequestHeader(required = false) Map<String, String> headers,
            jakarta.servlet.http.HttpServletRequest request) {
        try {
            if (baiduMapAk == null || baiduMapAk.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("{\"error\":\"百度地图AK未配置\"}");
            }

            // 获取请求路径（去掉 /api/baidu-map/proxy 前缀）
            String requestPath = request.getRequestURI().replaceFirst("^/api/baidu-map/proxy", "");
            
            // 构建完整的API URL
            StringBuilder urlBuilder = new StringBuilder(baseUrl);
            urlBuilder.append(requestPath);
            
            // 添加AK参数
            urlBuilder.append(requestPath.contains("?") ? "&" : "?");
            urlBuilder.append("ak=").append(baiduMapAk);
            
            // 添加其他查询参数
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                if (!"ak".equals(entry.getKey())) { // 避免重复添加AK
                    urlBuilder.append("&").append(entry.getKey()).append("=")
                            .append(java.net.URLEncoder.encode(entry.getValue(), "UTF-8"));
                }
            }
            
            String fullUrl = urlBuilder.toString();
            log.debug("代理调用百度地图API: {}", fullUrl);
            
            // 设置请求头
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.set("User-Agent", "Mozilla/5.0");
            if (headers != null && headers.containsKey("Accept")) {
                httpHeaders.set("Accept", headers.get("Accept"));
            }
            
            HttpEntity<String> entity = new HttpEntity<>(httpHeaders);
            
            // 调用百度地图API
            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );
            
            return response;
        } catch (Exception e) {
            log.error("代理调用百度地图API失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

}
