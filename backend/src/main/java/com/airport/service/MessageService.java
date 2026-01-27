package com.airport.service;

import com.airport.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 站内信服务接口
 * 
 * @author Corkedmzx
 */
public interface MessageService {

    /**
     * 创建站内信
     * 
     * @param message 消息实体
     * @return 创建的消息
     */
    Message createMessage(Message message);

    /**
     * 批量创建站内信（发送给多个用户）
     * 
     * @param userIds 用户ID列表
     * @param title 消息标题
     * @param content 消息内容
     * @param messageType 消息类型
     * @param category 消息类别
     * @param priority 优先级
     * @param relatedId 关联ID
     * @param relatedType 关联类型
     * @return 创建的消息列表
     */
    List<Message> createMessagesForUsers(List<Long> userIds, String title, String content,
                                         String messageType, String category, String priority,
                                         Long relatedId, String relatedType);

    /**
     * 根据角色代码批量创建站内信
     * 
     * @param roleCodes 角色代码列表
     * @param title 消息标题
     * @param content 消息内容
     * @param messageType 消息类型
     * @param category 消息类别
     * @param priority 优先级
     * @param relatedId 关联ID
     * @param relatedType 关联类型
     * @return 创建的消息列表
     */
    List<Message> createMessagesForRoles(List<String> roleCodes, String title, String content,
                                         String messageType, String category, String priority,
                                         Long relatedId, String relatedType);

    /**
     * 获取用户的消息列表（分页）
     * 
     * @param userId 用户ID
     * @param read 是否已读（null表示全部）
     * @param messageType 消息类型（null表示全部）
     * @param pageable 分页参数
     * @return 消息分页数据
     */
    Page<Message> getUserMessages(Long userId, Boolean read, String messageType, Pageable pageable);

    /**
     * 获取用户未读消息数量
     * 
     * @param userId 用户ID
     * @return 未读消息数量
     */
    Long getUnreadCount(Long userId);

    /**
     * 标记消息为已读
     * 
     * @param messageIds 消息ID列表
     * @param userId 用户ID
     */
    void markAsRead(List<Long> messageIds, Long userId);

    /**
     * 标记用户所有消息为已读
     * 
     * @param userId 用户ID
     */
    void markAllAsRead(Long userId);

    /**
     * 删除消息
     * 
     * @param messageId 消息ID
     * @param userId 用户ID（验证权限）
     */
    void deleteMessage(Long messageId, Long userId);

    /**
     * 删除用户所有已读消息
     * 
     * @param userId 用户ID
     */
    void deleteAllReadMessages(Long userId);

    /**
     * 批量删除消息
     * 
     * @param messageIds 消息ID列表
     * @param userId 用户ID（验证权限）
     */
    void deleteMessages(List<Long> messageIds, Long userId);

    /**
     * 清空当前用户的所有已读消息
     * 
     * @param userId 用户ID
     * @return 删除的消息数量
     */
    Long clearReadMessages(Long userId);

    /**
     * 根据权限代码批量创建站内信（发送给拥有指定权限的用户）
     * 
     * @param permissionCode 权限代码
     * @param title 消息标题
     * @param content 消息内容
     * @param messageType 消息类型
     * @param category 消息类别
     * @param priority 优先级
     * @param relatedId 关联ID
     * @param relatedType 关联类型
     * @return 创建的消息列表
     */
    List<Message> createMessagesForPermission(String permissionCode, String title, String content,
                                              String messageType, String category, String priority,
                                              Long relatedId, String relatedType);
}
