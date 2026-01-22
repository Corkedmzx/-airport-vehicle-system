package com.airport.repository;

import com.airport.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 站内信数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 根据用户ID查找消息
     */
    Page<Message> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    /**
     * 根据用户ID和已读状态查找消息
     */
    Page<Message> findByUserIdAndReadOrderByCreateTimeDesc(Long userId, Boolean read, Pageable pageable);

    /**
     * 根据用户ID和消息类型查找消息
     */
    Page<Message> findByUserIdAndMessageTypeOrderByCreateTimeDesc(Long userId, String messageType, Pageable pageable);

    /**
     * 根据用户ID、消息类型和已读状态查找消息
     */
    Page<Message> findByUserIdAndMessageTypeAndReadOrderByCreateTimeDesc(Long userId, String messageType, Boolean read, Pageable pageable);

    /**
     * 统计用户未读消息数量
     */
    Long countByUserIdAndRead(Long userId, Boolean read);

    /**
     * 根据用户ID查找未读消息
     */
    List<Message> findByUserIdAndReadOrderByCreateTimeDesc(Long userId, Boolean read);

    /**
     * 标记消息为已读
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE message SET `read` = 1, read_time = :readTime WHERE id IN :ids AND user_id = :userId", nativeQuery = true)
    void markAsRead(@Param("ids") List<Long> ids, @Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);

    /**
     * 标记用户所有消息为已读
     */
    @Modifying
    @Transactional
    @Query(value = "UPDATE message SET `read` = 1, read_time = :readTime WHERE user_id = :userId AND `read` = 0", nativeQuery = true)
    void markAllAsRead(@Param("userId") Long userId, @Param("readTime") LocalDateTime readTime);

    /**
     * 删除用户的所有已读消息
     */
    void deleteByUserIdAndRead(Long userId, Boolean read);

    /**
     * 根据关联信息查找消息
     */
    List<Message> findByRelatedTypeAndRelatedId(String relatedType, Long relatedId);
}
