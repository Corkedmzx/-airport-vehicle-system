package com.airport.controller;

import com.airport.dto.Result;
import com.airport.entity.SystemConfigEntity;
import com.airport.repository.SystemConfigRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 系统配置控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/system/config")
@RequiredArgsConstructor
@Tag(name = "系统配置", description = "系统配置管理相关接口")
public class SystemConfigController {

    private final SystemConfigRepository configRepository;

    @GetMapping
    @Operation(summary = "获取所有系统配置", description = "获取所有系统配置项")
    public Result<Map<String, String>> getAllConfigs() {
        try {
            List<SystemConfigEntity> configs = configRepository.findAll();
            Map<String, String> configMap = new HashMap<>();
            for (SystemConfigEntity config : configs) {
                configMap.put(config.getConfigKey(), config.getConfigValue());
            }
            return Result.success(configMap);
        } catch (Exception e) {
            log.error("获取系统配置失败", e);
            return Result.error("获取系统配置失败: " + e.getMessage());
        }
    }

    @GetMapping("/{key}")
    @Operation(summary = "获取指定配置", description = "根据配置键获取配置值")
    public Result<String> getConfig(
            @Parameter(description = "配置键", required = true) 
            @PathVariable String key) {
        try {
            Optional<SystemConfigEntity> config = configRepository.findByConfigKey(key);
            if (config.isPresent()) {
                return Result.success(config.get().getConfigValue());
            } else {
                return Result.error("配置不存在: " + key);
            }
        } catch (Exception e) {
            log.error("获取配置失败", e);
            return Result.error("获取配置失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "保存系统配置", description = "保存或更新系统配置")
    public Result<String> saveConfig(@RequestBody Map<String, String> configs) {
        try {
            for (Map.Entry<String, String> entry : configs.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                Optional<SystemConfigEntity> existingConfig = configRepository.findByConfigKey(key);
                if (existingConfig.isPresent()) {
                    SystemConfigEntity config = existingConfig.get();
                    config.setConfigValue(value);
                    configRepository.save(config);
                } else {
                    SystemConfigEntity config = new SystemConfigEntity();
                    config.setConfigKey(key);
                    config.setConfigValue(value);
                    config.setConfigType("string");
                    configRepository.save(config);
                }
            }
            return Result.success("配置保存成功");
        } catch (Exception e) {
            log.error("保存系统配置失败", e);
            return Result.error("保存系统配置失败: " + e.getMessage());
        }
    }

    @PutMapping("/{key}")
    @Operation(summary = "更新指定配置", description = "更新指定配置键的值")
    public Result<String> updateConfig(
            @Parameter(description = "配置键", required = true) 
            @PathVariable String key,
            @RequestBody Map<String, String> data) {
        try {
            String value = data.get("value");
            if (value == null) {
                return Result.error("配置值不能为空");
            }
            
            Optional<SystemConfigEntity> existingConfig = configRepository.findByConfigKey(key);
            if (existingConfig.isPresent()) {
                SystemConfigEntity config = existingConfig.get();
                config.setConfigValue(value);
                configRepository.save(config);
                return Result.success("配置更新成功");
            } else {
                return Result.error("配置不存在: " + key);
            }
        } catch (Exception e) {
            log.error("更新配置失败", e);
            return Result.error("更新配置失败: " + e.getMessage());
        }
    }
}

