package com.airport.controller;

import com.airport.dto.Result;
import com.airport.entity.Message;
import com.airport.service.MessageService;
import com.airport.utils.JwtUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 站内信管理控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
@Tag(name = "站内信管理", description = "站内信管理相关接口")
public class MessageController {

    private final MessageService messageService;
    private final JwtUtils jwtUtils;

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        String username = jwtUtils.getUsernameFromToken(token);
        if (username == null) {
            throw new RuntimeException("未认证或认证已过期");
        }
        // 从Token中获取用户ID
        return jwtUtils.getUserIdFromToken(token);
    }

    @GetMapping
    @Operation(summary = "获取用户消息列表", description = "分页获取当前用户的消息列表")
    public Result<Page<Message>> getMessages(
            HttpServletRequest request,
            @Parameter(description = "页码", required = false) 
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "每页数量", required = false) 
            @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "是否已读", required = false) 
            @RequestParam(required = false) Boolean read,
            @Parameter(description = "消息类型", required = false) 
            @RequestParam(required = false) String messageType) {
        try {
            Long userId = getCurrentUserId(request);
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
            Page<Message> messages = messageService.getUserMessages(userId, read, messageType, pageable);
            return Result.success(messages);
        } catch (Exception e) {
            log.error("获取消息列表失败", e);
            return Result.error("获取消息列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读消息数量", description = "获取当前用户的未读消息数量")
    public Result<Long> getUnreadCount(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Long count = messageService.getUnreadCount(userId);
            return Result.success(count);
        } catch (Exception e) {
            log.error("获取未读消息数量失败", e);
            return Result.error("获取未读消息数量失败: " + e.getMessage());
        }
    }

    @PutMapping("/mark-read")
    @Operation(summary = "标记消息为已读", description = "批量标记消息为已读")
    public Result<Void> markAsRead(
            HttpServletRequest request,
            @Parameter(description = "消息ID列表", required = true) 
            @RequestBody List<Long> messageIds) {
        try {
            Long userId = getCurrentUserId(request);
            messageService.markAsRead(messageIds, userId);
            Result<Void> result = Result.success();
            result.setMessage("标记成功");
            return result;
        } catch (Exception e) {
            log.error("标记消息为已读失败", e);
            return Result.error("标记消息为已读失败: " + e.getMessage());
        }
    }

    @PutMapping("/mark-all-read")
    @Operation(summary = "标记所有消息为已读", description = "将当前用户的所有消息标记为已读")
    public Result<Void> markAllAsRead(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            messageService.markAllAsRead(userId);
            Result<Void> result = Result.success();
            result.setMessage("全部标记成功");
            return result;
        } catch (Exception e) {
            log.error("标记所有消息为已读失败", e);
            return Result.error("标记所有消息为已读失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除消息", description = "删除指定消息")
    public Result<Void> deleteMessage(
            HttpServletRequest request,
            @Parameter(description = "消息ID", required = true) 
            @PathVariable Long id) {
        try {
            Long userId = getCurrentUserId(request);
            messageService.deleteMessage(id, userId);
            Result<Void> result = Result.success();
            result.setMessage("删除成功");
            return result;
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return Result.error("删除消息失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/read-all")
    @Operation(summary = "删除所有已读消息", description = "删除当前用户的所有已读消息")
    public Result<Void> deleteAllReadMessages(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            messageService.deleteAllReadMessages(userId);
            Result<Void> result = Result.success();
            result.setMessage("删除成功");
            return result;
        } catch (Exception e) {
            log.error("删除所有已读消息失败", e);
            return Result.error("删除所有已读消息失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除消息", description = "批量删除选中的消息")
    public Result<Void> deleteMessages(
            HttpServletRequest request,
            @Parameter(description = "消息ID列表", required = true) 
            @RequestBody List<Long> messageIds) {
        try {
            Long userId = getCurrentUserId(request);
            messageService.deleteMessages(messageIds, userId);
            Result<Void> result = Result.success();
            result.setMessage("删除成功");
            return result;
        } catch (Exception e) {
            log.error("批量删除消息失败", e);
            return Result.error("批量删除消息失败: " + e.getMessage());
        }
    }
}
