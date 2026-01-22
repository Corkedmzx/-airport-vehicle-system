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
        List<Long> userIds = new ArrayList<>();
        
        for (String roleCode : roleCodes) {
            // 查找角色ID
            SysRole role = roleRepository.findByRoleCode(roleCode).orElse(null);
            if (role == null || role.getStatus() != 1) {
                log.warn("角色不存在或已禁用: {}", roleCode);
                continue;
            }

            // 查找所有具有该角色的用户（通过查询用户角色关联表）
            List<SysUserRole> allUserRoles = userRoleRepository.findAll();
            for (SysUserRole userRole : allUserRoles) {
                if (userRole.getRoleId().equals(role.getId())) {
                    SysUser user = userRepository.findById(userRole.getUserId()).orElse(null);
                    if (user != null && user.getStatus() == 1) {
                        if (!userIds.contains(user.getId())) {
                            userIds.add(user.getId());
                        }
                    }
                }
            }
        }

        if (userIds.isEmpty()) {
            log.warn("没有找到具有指定角色的用户: {}", roleCodes);
            return new ArrayList<>();
        }

        return createMessagesForUsers(userIds, title, content, messageType, category, priority, relatedId, relatedType);
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
}
