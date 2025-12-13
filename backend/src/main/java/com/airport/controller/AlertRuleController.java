package com.airport.controller;

import com.airport.dto.Result;
import com.airport.entity.AlertRule;
import com.airport.repository.AlertRuleRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 告警规则管理控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/alert-rules")
@RequiredArgsConstructor
@Tag(name = "告警规则管理", description = "告警规则配置相关接口")
public class AlertRuleController {

    private final AlertRuleRepository alertRuleRepository;

    @GetMapping
    @Operation(summary = "获取告警规则列表", description = "获取所有告警规则")
    public Result<List<AlertRule>> getAlertRules(
            @Parameter(description = "是否只获取启用的规则", required = false)
            @RequestParam(required = false) Boolean enabled) {
        try {
            List<AlertRule> rules;
            if (enabled != null && enabled) {
                rules = alertRuleRepository.findByEnabledTrue();
            } else {
                rules = alertRuleRepository.findAll();
            }
            return Result.success(rules);
        } catch (Exception e) {
            log.error("获取告警规则列表失败", e);
            return Result.error("获取告警规则列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取告警规则详情", description = "根据ID获取告警规则详细信息")
    public Result<AlertRule> getAlertRuleById(
            @Parameter(description = "规则ID", required = true)
            @PathVariable Long id) {
        try {
            AlertRule rule = alertRuleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警规则不存在"));
            return Result.success(rule);
        } catch (Exception e) {
            log.error("获取告警规则详情失败", e);
            return Result.error("获取告警规则详情失败: " + e.getMessage());
        }
    }

    @PostMapping
    @Operation(summary = "创建告警规则", description = "创建新告警规则")
    public Result<AlertRule> createAlertRule(@RequestBody AlertRule rule) {
        try {
            AlertRule createdRule = alertRuleRepository.save(rule);
            return Result.success("告警规则创建成功", createdRule);
        } catch (Exception e) {
            log.error("创建告警规则失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新告警规则", description = "更新告警规则信息")
    public Result<AlertRule> updateAlertRule(
            @Parameter(description = "规则ID", required = true)
            @PathVariable Long id,
            @RequestBody AlertRule rule) {
        try {
            AlertRule existingRule = alertRuleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警规则不存在"));
            
            existingRule.setRuleName(rule.getRuleName());
            existingRule.setRuleType(rule.getRuleType());
            existingRule.setConditionType(rule.getConditionType());
            existingRule.setConditionValue(rule.getConditionValue());
            existingRule.setSeverity(rule.getSeverity());
            existingRule.setEnabled(rule.getEnabled());
            existingRule.setDescription(rule.getDescription());
            
            AlertRule updatedRule = alertRuleRepository.save(existingRule);
            return Result.success("告警规则更新成功", updatedRule);
        } catch (Exception e) {
            log.error("更新告警规则失败", e);
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除告警规则", description = "删除指定告警规则")
    public Result<String> deleteAlertRule(
            @Parameter(description = "规则ID", required = true)
            @PathVariable Long id) {
        try {
            if (!alertRuleRepository.existsById(id)) {
                return Result.error("告警规则不存在");
            }
            alertRuleRepository.deleteById(id);
            return Result.success("告警规则删除成功");
        } catch (Exception e) {
            log.error("删除告警规则失败", e);
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "启用/禁用告警规则", description = "切换告警规则的启用状态")
    public Result<AlertRule> toggleAlertRule(
            @Parameter(description = "规则ID", required = true)
            @PathVariable Long id) {
        try {
            AlertRule rule = alertRuleRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("告警规则不存在"));
            rule.setEnabled(!rule.getEnabled());
            AlertRule updatedRule = alertRuleRepository.save(rule);
            return Result.success("告警规则状态更新成功", updatedRule);
        } catch (Exception e) {
            log.error("切换告警规则状态失败", e);
            return Result.error(e.getMessage());
        }
    }
}

