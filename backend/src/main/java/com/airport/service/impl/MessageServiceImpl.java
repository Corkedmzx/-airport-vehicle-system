package com.airport.service.impl;

import com.airport.entity.Message;
import com.airport.entity.SysUser;
import com.airport.entity.SysUserRole;
import com.airport.entity.SysRole;
import com.airport.repository.MessageRepository;
import com.airport.repository.SysUserRepository;
import com.airport.repository.SysUserRoleRepository;
import com.airport.repository.SysRoleRepository;
import com.airport.service.MessageService;
import com.airport.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 站内信服务实现类
 * 
 * @author Corkedmzx
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;
    private final SysUserService userService;

    @Override
    public Message createMessage(Message message) {
        if (message.getRead() == null) {
            message.setRead(false);
        }
        if (message.getPriority() == null) {
            message.setPriority("normal");
        }
        return messageRepository.save(message);
    }

    @Override
    public List<Message> createMessagesForUsers(List<Long> userIds, String title, String content,
                                                 String messageType, String category, String priority,
                                                 Long relatedId, String relatedType) {
        List<Message> messages = new ArrayList<>();
        for (Long userId : userIds) {
            // 检查用户是否存在且启用
            SysUser user = userRepository.findById(userId).orElse(null);
            if (user == null || user.getStatus() != 1) {
                log.warn("用户不存在或已禁用，跳过发送站内信: userId={}", userId);
                continue;
            }

            Message message = new Message();
            message.setUserId(userId);
            message.setTitle(title);
            message.setContent(content);
            message.setMessageType(messageType);
            message.setCategory(category);
            message.setPriority(priority != null ? priority : "normal");
            message.setRelatedId(relatedId);
            message.setRelatedType(relatedType);
            message.setRead(false);

            messages.add(messageRepository.save(message));
        }
        log.info("已为{}个用户创建站内信，消息类型: {}", messages.size(), messageType);
        return messages;
    }

    @Override
    public List<Message> createMessagesForRoles(List<String> roleCodes, String title, String content,
                                                 String messageType, String category, String priority,
                                                 Long relatedId, String relatedType) {
        // 根据角色代码查找所有具有该角色的用户ID
        List<Long> roleIds = new ArrayList<>();
        
        // 先查找所有角色ID
        for (String roleCode : roleCodes) {
            SysRole role = roleRepository.findByRoleCode(roleCode).orElse(null);
            if (role == null || role.getStatus() != 1) {
                log.warn("角色不存在或已禁用: {}", roleCode);
                continue;
            }
            roleIds.add(role.getId());
        }

        if (roleIds.isEmpty()) {
            log.warn("没有找到有效的角色: {}", roleCodes);
            return new ArrayList<>();
        }

        // 使用findByRoleIdIn高效查询用户角色关联
        List<SysUserRole> userRoles = userRoleRepository.findByRoleIdIn(roleIds);
        
        // 提取用户ID并去重
        List<Long> userIds = userRoles.stream()
                .map(SysUserRole::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // 验证用户是否存在且启用
        List<Long> validUserIds = new ArrayList<>();
        for (Long userId : userIds) {
            SysUser user = userRepository.findById(userId).orElse(null);
            if (user != null && user.getStatus() == 1) {
                validUserIds.add(userId);
            }
        }

        if (validUserIds.isEmpty()) {
            log.warn("没有找到具有指定角色的启用用户: {}", roleCodes);
            return new ArrayList<>();
        }

        log.info("找到{}个具有角色{}的用户，准备发送站内信", validUserIds.size(), roleCodes);
        return createMessagesForUsers(validUserIds, title, content, messageType, category, priority, relatedId, relatedType);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Message> getUserMessages(Long userId, Boolean read, String messageType, Pageable pageable) {
        if (read != null && messageType != null) {
            return messageRepository.findByUserIdAndMessageTypeAndReadOrderByCreateTimeDesc(userId, messageType, read, pageable);
        } else if (read != null) {
            return messageRepository.findByUserIdAndReadOrderByCreateTimeDesc(userId, read, pageable);
        } else if (messageType != null) {
            return messageRepository.findByUserIdAndMessageTypeOrderByCreateTimeDesc(userId, messageType, pageable);
        } else {
            return messageRepository.findByUserIdOrderByCreateTimeDesc(userId, pageable);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount(Long userId) {
        return messageRepository.countByUserIdAndRead(userId, false);
    }

    @Override
    public void markAsRead(List<Long> messageIds, Long userId) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        messageRepository.markAsRead(messageIds, userId, LocalDateTime.now());
        log.info("用户{}标记了{}条消息为已读", userId, messageIds.size());
    }

    @Override
    public void markAllAsRead(Long userId) {
        messageRepository.markAllAsRead(userId, LocalDateTime.now());
        log.info("用户{}标记所有消息为已读", userId);
    }

    @Override
    public void deleteMessage(Long messageId, Long userId) {
        Message message = messageRepository.findById(messageId).orElse(null);
        if (message == null) {
            throw new RuntimeException("消息不存在");
        }
        if (!message.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除此消息");
        }
        messageRepository.delete(message);
        log.info("用户{}删除了消息{}", userId, messageId);
    }

    @Override
    public void deleteAllReadMessages(Long userId) {
        messageRepository.deleteByUserIdAndRead(userId, true);
        log.info("用户{}删除了所有已读消息", userId);
    }

    @Override
    public void deleteMessages(List<Long> messageIds, Long userId) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        // 验证所有消息都属于当前用户
        List<Message> messages = messageRepository.findAllById(messageIds);
        for (Message message : messages) {
            if (!message.getUserId().equals(userId)) {
                throw new RuntimeException("无权删除消息: " + message.getId());
            }
        }
        messageRepository.deleteAll(messages);
        log.info("用户{}批量删除了{}条消息", userId, messages.size());
    }

    @Override
    public Long clearReadMessages(Long userId) {
        // 先统计要删除的消息数量
        Long count = messageRepository.countByUserIdAndRead(userId, true);
        if (count > 0) {
            // 删除所有已读消息
            messageRepository.deleteByUserIdAndRead(userId, true);
            log.info("用户{}清空了{}条已读消息", userId, count);
        }
        return count;
    }

    @Override
    public List<Message> createMessagesForPermission(String permissionCode, String title, String content,
                                                     String messageType, String category, String priority,
                                                     Long relatedId, String relatedType) {
        // 使用SysUserService查找拥有该权限的用户
        List<SysUser> users = userService.findUsersByPermission(permissionCode);
        
        if (users.isEmpty()) {
            log.warn("没有找到拥有权限{}的用户", permissionCode);
            return new ArrayList<>();
        }

        List<Long> userIds = users.stream()
                .map(SysUser::getId)
                .collect(Collectors.toList());

        return createMessagesForUsers(userIds, title, content, messageType, category, priority, relatedId, relatedType);
    }
}
