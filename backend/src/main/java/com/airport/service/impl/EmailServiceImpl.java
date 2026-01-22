package com.airport.service.impl;

import com.airport.entity.SysUser;
import com.airport.repository.SysUserRepository;
import com.airport.service.EmailService;
import com.airport.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * 邮件服务实现类
 * 使用SMTP协议发送邮件，支持HTML格式
 * 系统会自动从数据库的sys_user表中读取用户邮箱地址并发送邮件
 * 
 * @author Corkedmzx
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final SmtpEmailClient smtpEmailClient;
    private final MessageService messageService;
    private final SysUserRepository userRepository;

    @Value("${spring.mail.enabled:true}")
    private boolean emailEnabled;

    @Override
    public void sendSimpleEmail(String to, String subject, String content) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送邮件到: {}", to);
            return;
        }

        try {
            smtpEmailClient.sendTextEmail(to, subject, content);
            log.info("邮件发送成功，收件人: {}, 主题: {}", to, subject);
        } catch (Exception e) {
            log.error("发送邮件失败，收件人: {}, 主题: {}", to, subject, e);
            throw new RuntimeException("发送邮件失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        // 先发送邮件
        if (emailEnabled) {
        try {
            smtpEmailClient.sendHtmlEmail(to, subject, htmlContent);
            log.info("HTML邮件发送成功，收件人: {}, 主题: {}", to, subject);
        } catch (Exception e) {
            log.error("发送HTML邮件失败，收件人: {}, 主题: {}", to, subject, e);
            throw new RuntimeException("发送HTML邮件失败: " + e.getMessage(), e);
            }
        } else {
            log.warn("邮件服务未启用，跳过发送HTML邮件到: {}", to);
        }
        
        // 检查是否需要自动创建站内信
        // 注意：任务分配邮件已经在业务逻辑中手动创建了更详细的站内信，这里跳过以避免重复
        // 告警相关的邮件（确认、完成、报告）也会自动创建站内信，但可能已经在业务逻辑中创建了，需要检查
        boolean shouldCreateMessage = true;
        if (subject != null && (subject.contains("任务分配通知") || subject.contains("任务分配") || 
            subject.contains("维修任务分配"))) {
            // 任务分配相关的邮件（包括司机和维修员）已经在 DispatchTaskServiceImpl 中手动创建了站内信
            // 这里跳过自动创建，避免重复
            shouldCreateMessage = false;
            log.debug("任务分配邮件跳过自动创建站内信，已在业务逻辑中手动创建");
        } else if (subject != null && (subject.contains("告警确认通知") || subject.contains("告警完成通知") || 
                   subject.contains("告警报告通知"))) {
            // 告警相关的邮件（确认、完成、报告）已经在 AlertController 中通过邮件发送自动创建了站内信
            // 这里跳过自动创建，避免重复
            shouldCreateMessage = false;
            log.debug("告警邮件跳过自动创建站内信，已在邮件发送时自动创建");
        }
        
        // 如果需要，创建站内信（确保用户能收到通知）
        if (shouldCreateMessage) {
            createMessageForEmail(to, subject, htmlContent, null, null, null, null, null);
        }
    }

    /**
     * 根据邮箱地址创建站内信的辅助方法
     * 提取邮件主题和内容，创建对应的站内信
     */
    private void createMessageForEmail(String email, String subject, String htmlContent,
                                       String messageType, String category, String priority,
                                       Long relatedId, String relatedType) {
        try {
            // 根据邮箱地址查找用户
            Optional<SysUser> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                log.warn("邮箱 {} 对应的用户不存在，跳过创建站内信", email);
                return;
            }

            SysUser user = userOpt.get();
            if (user.getStatus() != 1) {
                log.warn("用户 {} 已禁用，跳过创建站内信", user.getUsername());
                return;
            }

            // 从HTML内容中提取纯文本内容（移除所有HTML标签、CSS样式和脚本）
            String plainContent = "";
            if (htmlContent != null && !htmlContent.trim().isEmpty()) {
                // 使用多行模式（DOTALL）移除 <style> 标签及其内容（包括换行符）
                plainContent = htmlContent.replaceAll("(?is)<style[^>]*>.*?</style>", "")
                        // 移除 <script> 标签及其内容
                        .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                        // 移除 <head> 标签及其内容
                        .replaceAll("(?is)<head[^>]*>.*?</head>", "")
                        // 移除 <meta> 标签
                        .replaceAll("(?i)<meta[^>]*>", "")
                        // 移除所有HTML标签
                        .replaceAll("<[^>]+>", "")
                        // 解码HTML实体
                        .replaceAll("&nbsp;", " ")
                        .replaceAll("&lt;", "<")
                        .replaceAll("&gt;", ">")
                        .replaceAll("&amp;", "&")
                        .replaceAll("&quot;", "\"")
                        .replaceAll("&#39;", "'")
                        .replaceAll("&apos;", "'")
                        .replaceAll("&mdash;", "—")
                        .replaceAll("&ndash;", "–")
                        // 移除CSS代码块（如 "body { font-family: Arial; }"）
                        .replaceAll("[a-zA-Z\\-]+\\s*\\{[^}]*\\}", "")
                        // 移除单独的CSS属性（如 "font-family: Arial;"）
                        .replaceAll("[a-zA-Z\\-]+\\s*:\\s*[^;]+;", "")
                        // 将多个连续空白字符（包括换行符）替换为单个空格
                        .replaceAll("[\\s\\n\\r]+", " ")
                        // 移除首尾空白
                        .trim();
            }
            if (plainContent.isEmpty()) {
                plainContent = subject; // 如果没有内容，使用主题作为内容
            }
            
            // 如果内容仍然包含CSS代码片段，进一步清理
            if (plainContent.contains("{") || plainContent.contains("}") || plainContent.contains(":")) {
                // 移除所有包含大括号或冒号的代码片段
                plainContent = plainContent.replaceAll("[^\\u4e00-\\u9fa5a-zA-Z0-9\\s，。！？：；、（）【】《》]+", " ")
                        .replaceAll("\\s+", " ")
                        .trim();
            }

            // 如果消息类型未指定，根据主题推断
            if (messageType == null) {
                if (subject.contains("任务分配")) {
                    messageType = "task_assignment";
                } else if (subject.contains("任务完成")) {
                    messageType = "task_completion";
                } else if (subject.contains("告警")) {
                    messageType = "alert";
                } else if (subject.contains("维修")) {
                    messageType = "maintenance";
                } else {
                    messageType = "system";
                }
            }

            // 创建站内信
            com.airport.entity.Message message = new com.airport.entity.Message();
            message.setUserId(user.getId());
            message.setTitle(subject);
            message.setContent(plainContent.length() > 500 ? plainContent.substring(0, 500) + "..." : plainContent);
            message.setMessageType(messageType);
            message.setCategory(category);
            message.setPriority(priority != null ? priority : "normal");
            message.setRelatedId(relatedId);
            message.setRelatedType(relatedType);
            message.setRead(false);

            messageService.createMessage(message);
            log.info("已为用户 {} 创建站内信，消息类型: {}", user.getUsername(), messageType);
        } catch (Exception e) {
            log.error("创建站内信失败，邮箱: {}, 主题: {}", email, subject, e);
            // 不抛出异常，避免影响邮件发送流程
        }
    }

    @Override
    public void sendTaskAssignmentEmail(String to, String taskNo, String taskName,
                                         String taskType, Integer priority,
                                         String startLocation, String endLocation,
                                         java.time.LocalDateTime startTime) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送任务分配邮件到: {}", to);
            return;
        }

        String subject = "任务分配通知 - " + taskNo;
        String priorityText = getPriorityText(priority);

        String htmlContent = buildTaskAssignmentEmailHtml(taskNo, taskName, taskType, 
                                                           priorityText, startLocation, 
                                                           endLocation, startTime);

        sendHtmlEmail(to, subject, htmlContent);
    }

    private String buildTaskAssignmentEmailHtml(String taskNo, String taskName,
                                                 String taskType, String priorityText,
                                                 String startLocation, String endLocation,
                                                 java.time.LocalDateTime startTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime != null ? startTime.format(formatter) : "未指定";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #4CAF50;">任务分配通知</h2>
                        <p>您好，</p>
                        <p>您已被分配了一个新任务，详细信息如下：</p>
                    <div style="background: #f0f7ff; padding: 15px; margin: 15px 0; border-left: 4px solid #4CAF50;">
                        <h3 style="color: #4CAF50; margin-top: 0;">任务信息</h3>
                        <p><strong>任务编号：</strong>%s</p>
                        <p><strong>任务名称：</strong>%s</p>
                        <p><strong>任务类型：</strong>%s</p>
                        <p><strong>优先级：</strong>%s</p>
                        <p><strong>起始位置：</strong>%s</p>
                        <p><strong>目标位置：</strong>%s</p>
                        <p><strong>开始时间：</strong>%s</p>
                    </div>
                    <p>请及时查看任务详情并开始执行。</p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, taskNo, taskName, taskType, priorityText, startLocation, endLocation, startTimeStr);
    }

    @Override
    public void sendDriverTaskAssignmentEmail(String to, String taskNo, String taskName,
                                             String taskType, Integer priority,
                                             String startLocation, String endLocation,
                                             java.time.LocalDateTime startTime,
                                             String vehicleNo, String vehicleBrand, String vehicleModel,
                                             String dispatcherName, String dispatcherRole) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送司机任务分配邮件到: {}", to);
            return;
        }

        String subject = "任务分配通知 - " + taskNo;
        String priorityText = getPriorityText(priority);

        String htmlContent = buildDriverTaskAssignmentEmailHtml(taskNo, taskName, taskType, 
                                                                 priorityText, startLocation, 
                                                                 endLocation, startTime,
                                                                 vehicleNo, vehicleBrand, vehicleModel,
                                                                 dispatcherName, dispatcherRole);

        sendHtmlEmail(to, subject, htmlContent);
    }

    private String buildDriverTaskAssignmentEmailHtml(String taskNo, String taskName,
                                                     String taskType, String priorityText,
                                                     String startLocation, String endLocation,
                                                     java.time.LocalDateTime startTime,
                                                     String vehicleNo, String vehicleBrand, String vehicleModel,
                                                     String dispatcherName, String dispatcherRole) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime != null ? startTime.format(formatter) : "未指定";

        String vehicleInfo = "";
        if (vehicleNo != null && !vehicleNo.trim().isEmpty()) {
            vehicleInfo = String.format("""
                <div style="background: #fff7e6; padding: 15px; margin: 15px 0; border-left: 4px solid #FF9800;">
                    <h3 style="color: #FF9800; margin-top: 0;">分配车辆信息</h3>
                    <p><strong>车牌号：</strong>%s</p>
                    <p><strong>车辆品牌：</strong>%s</p>
                    <p><strong>车辆型号：</strong>%s</p>
                </div>
                """, 
                vehicleNo,
                vehicleBrand != null ? vehicleBrand : "未指定",
                vehicleModel != null ? vehicleModel : "未指定");
        }

        String dispatcherInfo = "";
        if (dispatcherName != null && !dispatcherName.trim().isEmpty()) {
            dispatcherInfo = String.format("""
                <div style="background: #f5f5f5; padding: 12px; margin: 15px 0; border-left: 3px solid #909399; border-radius: 4px;">
                    <p style="margin: 0; color: #606266; font-size: 13px;">
                        <strong>发送人：</strong>%s <span style="color: #909399;">（%s）</span>
                    </p>
                </div>
                """,
                dispatcherName,
                dispatcherRole != null && !dispatcherRole.trim().isEmpty() ? dispatcherRole : "调度员");
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #2196F3;">任务分配通知</h2>
                        <p>您好，</p>
                        <p>您已被分配了一个新的驾驶任务，请及时查看任务详情并前往指定地点执行任务。</p>
                    <div style="background: #f0f7ff; padding: 15px; margin: 15px 0; border-left: 4px solid #2196F3;">
                        <h3 style="color: #2196F3; margin-top: 0;">任务信息</h3>
                        <p><strong>任务编号：</strong>%s</p>
                        <p><strong>任务名称：</strong>%s</p>
                        <p><strong>任务类型：</strong>%s</p>
                        <p><strong>优先级：</strong>%s</p>
                        <p><strong>起始位置：</strong>%s</p>
                        <p><strong>目标位置：</strong>%s</p>
                        <p><strong>开始时间：</strong>%s</p>
                    </div>
                    %s
                    %s
                    <p><strong style="color: #2196F3;">请按时到达起始位置，开始执行任务。</strong></p>
                    <p>如有疑问或特殊情况，请及时联系调度中心。</p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, taskNo, taskName, taskType, priorityText, startLocation, endLocation, startTimeStr, vehicleInfo, dispatcherInfo);
    }

    @Override
    public void sendTaskUnassignmentEmail(String to, String taskNo, String taskName,
                                         String taskType, Integer priority,
                                         String startLocation, String endLocation,
                                         java.time.LocalDateTime startTime,
                                         String vehicleNo, String reason) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送任务取消分配邮件到: {}", to);
            return;
        }

        String subject = "任务取消分配通知 - " + taskNo;
        String priorityText = getPriorityText(priority);

        String htmlContent = buildTaskUnassignmentEmailHtml(taskNo, taskName, taskType, 
                                                            priorityText, startLocation, 
                                                            endLocation, startTime,
                                                            vehicleNo, reason);

        sendHtmlEmail(to, subject, htmlContent);
    }

    private String buildTaskUnassignmentEmailHtml(String taskNo, String taskName,
                                                  String taskType, String priorityText,
                                                  String startLocation, String endLocation,
                                                  java.time.LocalDateTime startTime,
                                                  String vehicleNo, String reason) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime != null ? startTime.format(formatter) : "未指定";
        String reasonText = reason != null && !reason.trim().isEmpty() ? reason : "管理员取消了任务分配";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #FF9800;">任务取消分配通知</h2>
                        <p>您好，</p>
                    <div style="background: #fff7e6; padding: 15px; margin: 15px 0; border: 1px solid #ffc107;">
                        <p><strong style="color: #fa8c16;">您已被分配的任务已被取消分配。</strong></p>
                            <p>原因：%s</p>
                    </div>
                    <div style="background: #f0f7ff; padding: 15px; margin: 15px 0; border-left: 4px solid #FF9800;">
                        <h3 style="color: #FF9800; margin-top: 0;">任务信息</h3>
                        <p><strong>任务编号：</strong>%s</p>
                        <p><strong>任务名称：</strong>%s</p>
                        <p><strong>任务类型：</strong>%s</p>
                        <p><strong>优先级：</strong>%s</p>
                        <p><strong>起始位置：</strong>%s</p>
                        <p><strong>目标位置：</strong>%s</p>
                        <p><strong>原计划开始时间：</strong>%s</p>
                        <p><strong>原分配车辆：</strong>%s</p>
                    </div>
                    <p><strong style="color: #FF9800;">任务已恢复为待分配状态，您无需执行此任务。</strong></p>
                    <p>如有疑问，请及时联系调度中心。</p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, reasonText, taskNo, taskName, taskType, priorityText, startLocation, endLocation, startTimeStr, vehicleNo);
    }

    @Override
    @Async("taskExecutor")
    public void sendDriverTaskAssignmentEmailAsync(String to, String taskNo, String taskName,
                                                   String taskType, Integer priority,
                                                   String startLocation, String endLocation,
                                                   java.time.LocalDateTime startTime,
                                                   String vehicleNo, String vehicleBrand, String vehicleModel,
                                                   String dispatcherName, String dispatcherRole) {
        log.info("开始异步发送任务分配邮件，收件人: {}", to);
        try {
            sendDriverTaskAssignmentEmail(to, taskNo, taskName, taskType, priority, 
                                         startLocation, endLocation, startTime, 
                                         vehicleNo, vehicleBrand, vehicleModel,
                                         dispatcherName, dispatcherRole);
            log.info("异步任务分配邮件发送完成，收件人: {}", to);
        } catch (Exception e) {
            log.error("异步发送任务分配邮件失败，收件人: {}", to, e);
        }
    }

    @Override
    @Async("taskExecutor")
    public void sendMaintenanceTaskAssignmentEmailAsync(String to, String taskNo, String taskName,
                                                        String taskType, Integer priority,
                                                        String startLocation, String endLocation,
                                                        java.time.LocalDateTime startTime,
                                                        String vehicleNo, String vehicleBrand, String vehicleModel,
                                                        String description, String dispatcherName, String dispatcherRole) {
        log.info("开始异步发送维修任务分配邮件，收件人: {}", to);
        try {
            sendMaintenanceTaskAssignmentEmail(to, taskNo, taskName, taskType, priority,
                                              startLocation, endLocation, startTime,
                                              vehicleNo, vehicleBrand, vehicleModel,
                                              description, dispatcherName, dispatcherRole);
            log.info("异步维修任务分配邮件发送完成，收件人: {}", to);
        } catch (Exception e) {
            log.error("异步发送维修任务分配邮件失败，收件人: {}", to, e);
        }
    }

    /**
     * 发送维修员任务分配邮件（同步方法，供异步方法调用）
     */
    private void sendMaintenanceTaskAssignmentEmail(String to, String taskNo, String taskName,
                                                     String taskType, Integer priority,
                                                     String startLocation, String endLocation,
                                                     java.time.LocalDateTime startTime,
                                                     String vehicleNo, String vehicleBrand, String vehicleModel,
                                                     String description, String dispatcherName, String dispatcherRole) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送维修任务分配邮件到: {}", to);
            return;
        }

        String subject = "维修任务分配通知 - " + taskNo;
        String priorityText = getPriorityText(priority);
        String htmlContent = buildMaintenanceTaskAssignmentEmailHtml(taskNo, taskName, taskType,
                                                                   priorityText, startLocation, endLocation,
                                                                   startTime, vehicleNo, vehicleBrand, vehicleModel,
                                                                   description, dispatcherName, dispatcherRole);

        sendHtmlEmail(to, subject, htmlContent);
    }

    /**
     * 构建维修员任务分配邮件HTML内容
     */
    private String buildMaintenanceTaskAssignmentEmailHtml(String taskNo, String taskName,
                                                           String taskType, String priorityText,
                                                           String startLocation, String endLocation,
                                                           java.time.LocalDateTime startTime,
                                                           String vehicleNo, String vehicleBrand, String vehicleModel,
                                                           String description, String dispatcherName, String dispatcherRole) {
        String startTimeStr = startTime != null ? startTime.toString() : "未指定";
        String vehicleInfo = (vehicleBrand != null ? vehicleBrand : "") + " " + (vehicleModel != null ? vehicleModel : "");
        vehicleInfo = vehicleInfo.trim();
        if (vehicleInfo.isEmpty()) {
            vehicleInfo = "未知车型";
        }
        
        String dispatcherInfo = "";
        if (dispatcherName != null && !dispatcherName.trim().isEmpty()) {
            dispatcherInfo = String.format("<p><strong>分配人：</strong>%s（%s）</p>", dispatcherName, dispatcherRole);
        }

        String descriptionHtml = "";
        if (description != null && !description.trim().isEmpty()) {
            descriptionHtml = String.format("<p><strong>任务描述：</strong>%s</p>", description);
        }

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #409EFF; color: white; padding: 20px; text-align: center; border-radius: 5px 5px 0 0; }
                    .content { background-color: #f9f9f9; padding: 20px; border-radius: 0 0 5px 5px; }
                    .info-box { background-color: white; padding: 15px; margin: 15px 0; border-left: 4px solid #409EFF; }
                    .priority-high { color: #F56C6C; font-weight: bold; }
                    .priority-medium { color: #E6A23C; font-weight: bold; }
                    .priority-low { color: #67C23A; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>维修任务分配通知</h2>
                    </div>
                    <div class="content">
                        <p>您好，</p>
                        <p>您已被分配了一个新的维修任务，请及时查看任务详情并前往指定地点执行维修工作。</p>
                        <div class="info-box">
                            <h3 style="margin-top: 0;">任务信息</h3>
                            <p><strong>任务编号：</strong>%s</p>
                            <p><strong>任务名称：</strong>%s</p>
                            <p><strong>任务类型：</strong>%s</p>
                            <p><strong>优先级：</strong><span class="priority-%s">%s</span></p>
                            <p><strong>车辆位置：</strong>%s</p>
                            <p><strong>维修地点：</strong>%s</p>
                            <p><strong>开始时间：</strong>%s</p>
                            <p><strong>车辆信息：</strong>%s (车牌号: %s)</p>
                            %s
                            %s
                        </div>
                        <p><strong style="color: #E6A23C;">请提前准备维修材料和工具，确保按时完成维修任务。</strong></p>
                        <p>如有疑问，请及时联系调度中心。</p>
                        <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                    </div>
                </div>
            </body>
            </html>
            """, taskNo, taskName, taskType, 
            priorityText.equals("高") || priorityText.equals("紧急") ? "high" : priorityText.equals("中") ? "medium" : "low",
            priorityText, startLocation, endLocation, startTimeStr, vehicleInfo, vehicleNo,
            descriptionHtml, dispatcherInfo);
    }

    @Override
    @Async("taskExecutor")
    public void sendTaskUnassignmentEmailAsync(String to, String taskNo, String taskName,
                                               String taskType, Integer priority,
                                               String startLocation, String endLocation,
                                               java.time.LocalDateTime startTime,
                                               String vehicleNo, String reason) {
        log.info("开始异步发送任务取消分配邮件，收件人: {}", to);
        try {
            sendTaskUnassignmentEmail(to, taskNo, taskName, taskType, priority, 
                                     startLocation, endLocation, startTime, vehicleNo, reason);
            log.info("异步任务取消分配邮件发送完成，收件人: {}", to);
        } catch (Exception e) {
            log.error("异步发送任务取消分配邮件失败，收件人: {}", to, e);
        }
    }

    private String getPriorityText(Integer priority) {
        if (priority == null) {
            return "未知";
        }
        return switch (priority) {
            case 1 -> "低";
            case 2 -> "中";
            case 3 -> "高";
            case 4 -> "紧急";
            default -> "未知";
        };
    }

    // ==================== 角色特定邮件模板实现 ====================

    @Override
    public void sendAlertNotificationEmail(String to, String roleCode, String alertTitle,
                                          String alertDescription, String severity, String category,
                                          String vehicleNo, String taskNo, java.time.LocalDateTime alertTime) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送告警通知邮件到: {}", to);
            return;
        }

        // 根据不同角色定制邮件内容
        String subject = buildAlertSubject(roleCode, severity, alertTitle);
        String htmlContent = buildAlertNotificationEmailHtml(roleCode, alertTitle, alertDescription,
                                                           severity, category, vehicleNo, taskNo, alertTime);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendDispatcherTaskCreatedEmail(String to, String taskNo, String taskName,
                                                String taskType, Integer priority,
                                                String startLocation, String endLocation,
                                                java.time.LocalDateTime startTime,
                                                String driverName, String vehicleNo) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送调度员任务创建邮件到: {}", to);
            return;
        }

        String subject = "任务创建确认 - " + taskNo;
        String priorityText = getPriorityText(priority);
        String htmlContent = buildDispatcherTaskCreatedEmailHtml(taskNo, taskName, taskType,
                                                                priorityText, startLocation, endLocation,
                                                                startTime, driverName, vehicleNo);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendDispatcherTaskCompletedEmail(String to, String taskNo, String taskName,
                                                 java.time.LocalDateTime completedTime,
                                                 String driverName, String vehicleNo, Long duration) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送调度员任务完成邮件到: {}", to);
            return;
        }

        String subject = "任务完成通知 - " + taskNo;
        String htmlContent = buildDispatcherTaskCompletedEmailHtml(taskNo, taskName, completedTime,
                                                                  driverName, vehicleNo, duration);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendMaintenanceVehicleFaultEmail(String to, String vehicleNo, String vehicleType,
                                                  String faultType, String faultDescription,
                                                  String severity, String location,
                                                  java.time.LocalDateTime alertTime) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送维修员车辆故障邮件到: {}", to);
            return;
        }

        String subject = "车辆故障告警 - " + vehicleNo;
        String htmlContent = buildMaintenanceVehicleFaultEmailHtml(vehicleNo, vehicleType, faultType,
                                                                  faultDescription, severity, location, alertTime);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendMaintenanceReminderEmail(String to, String vehicleNo, String maintenanceType,
                                             java.time.LocalDate maintenanceDate,
                                             java.math.BigDecimal currentMileage,
                                             java.math.BigDecimal nextMaintenanceMileage,
                                             Integer daysRemaining) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送维修提醒邮件到: {}", to);
            return;
        }

        String subject = "维修提醒 - " + vehicleNo + " - " + maintenanceType;
        String htmlContent = buildMaintenanceReminderEmailHtml(vehicleNo, maintenanceType, maintenanceDate,
                                                              currentMileage, nextMaintenanceMileage, daysRemaining);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendMonitorSystemStatusEmail(String to, String statusType, String statusTitle,
                                             String statusDescription, String affectedSystems,
                                             java.time.LocalDateTime reportTime) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送监控员系统状态邮件到: {}", to);
            return;
        }

        String subject = "系统状态通知 - " + statusTitle;
        String htmlContent = buildMonitorSystemStatusEmailHtml(statusType, statusTitle, statusDescription,
                                                              affectedSystems, reportTime);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendAdminImportantOperationEmail(String to, String operationType, String operationTitle,
                                                  String operationDescription, String operatorName,
                                                  java.time.LocalDateTime operationTime,
                                                  String affectedResources) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送管理员重要操作邮件到: {}", to);
            return;
        }

        String subject = "重要操作通知 - " + operationTitle;
        String htmlContent = buildAdminImportantOperationEmailHtml(operationType, operationTitle,
                                                                  operationDescription, operatorName,
                                                                  operationTime, affectedResources);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendOperatorOperationConfirmationEmail(String to, String operationType,
                                                        String resourceName, String operationDetails,
                                                        String operatorName, java.time.LocalDateTime operationTime) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送操作员操作确认邮件到: {}", to);
            return;
        }

        String subject = "操作确认 - " + getOperationTypeText(operationType) + " - " + resourceName;
        String htmlContent = buildOperatorOperationConfirmationEmailHtml(operationType, resourceName,
                                                                        operationDetails, operatorName, operationTime);

        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendDriverTaskCompletedConfirmationEmail(String to, String taskNo, String taskName,
                                                          java.time.LocalDateTime completedTime,
                                                          Long duration, String startLocation, String endLocation) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送司机任务完成确认邮件到: {}", to);
            return;
        }

        String subject = "任务完成确认 - " + taskNo;
        String htmlContent = buildDriverTaskCompletedConfirmationEmailHtml(taskNo, taskName, completedTime,
                                                                          duration, startLocation, endLocation);

        sendHtmlEmail(to, subject, htmlContent);
    }

    // ==================== 邮件模板构建方法 ====================

    private String buildAlertSubject(String roleCode, String severity, String alertTitle) {
        String severityText = switch (severity) {
            case "high" -> "【紧急】";
            case "medium" -> "【重要】";
            case "low" -> "【提示】";
            default -> "";
        };
        return severityText + "告警通知 - " + alertTitle;
    }

    private String buildAlertNotificationEmailHtml(String roleCode, String alertTitle, String alertDescription,
                                                   String severity, String category, String vehicleNo,
                                                   String taskNo, java.time.LocalDateTime alertTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String alertTimeStr = alertTime != null ? alertTime.format(formatter) : "未知";

        String severityText = switch (severity) {
            case "high" -> "紧急";
            case "medium" -> "重要";
            case "low" -> "提示";
            default -> "未知";
        };
        String severityColor = switch (severity) {
            case "high" -> "#F56C6C";
            case "medium" -> "#E6A23C";
            case "low" -> "#67C23A";
            default -> "#909399";
        };

        String categoryText = switch (category) {
            case "vehicle_fault" -> "车辆故障";
            case "task_timeout" -> "任务超时";
            case "system_error" -> "系统错误";
            case "safety_alert" -> "安全告警";
            case "fuel_low" -> "油量过低";
            case "speed_exceed" -> "速度超限";
            default -> category;
        };

        // 根据不同角色定制提示信息
        String roleSpecificInfo = switch (roleCode) {
            case "ADMIN" -> "<p><strong style='color: #F56C6C;'>【系统管理员】请及时处理此告警，确保系统正常运行。</strong></p>";
            case "DISPATCHER" -> "<p><strong style='color: #E6A23C;'>【调度员】请检查任务调度情况，必要时调整任务分配。</strong></p>";
            case "MAINTENANCE" -> "<p><strong style='color: #2196F3;'>【维修员】请及时前往处理车辆故障，确保车辆正常运行。</strong></p>";
            case "MONITOR" -> "<p><strong style='color: #909399;'>【监控员】请继续监控相关系统状态，如有异常请及时上报。</strong></p>";
            default -> "";
        };

        String vehicleInfo = vehicleNo != null && !vehicleNo.trim().isEmpty() 
            ? String.format("<p><strong>关联车辆：</strong>%s</p>", vehicleNo) : "";
        String taskInfo = taskNo != null && !taskNo.trim().isEmpty() 
            ? String.format("<p><strong>关联任务：</strong>%s</p>", taskNo) : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: %s;">告警通知</h2>
                    <p>您好，</p>
                    %s
                    <div style="background: %s; padding: 15px; margin: 15px 0; border-left: 4px solid %s; border-radius: 4px;">
                        <h3 style="color: %s; margin-top: 0;">告警信息</h3>
                        <p><strong>告警标题：</strong>%s</p>
                        <p><strong>告警描述：</strong>%s</p>
                        <p><strong>严重程度：</strong><span style="color: %s; font-weight: 600;">%s</span></p>
                        <p><strong>告警类别：</strong>%s</p>
                        %s
                        %s
                        <p><strong>告警时间：</strong>%s</p>
                    </div>
                    <p>请及时查看并处理此告警。</p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, severityColor, roleSpecificInfo,
            severityColor.equals("#F56C6C") ? "#FEF0F0" : severityColor.equals("#E6A23C") ? "#FDF6EC" : "#F0F9FF",
            severityColor, severityColor, alertTitle, alertDescription != null ? alertDescription : "无",
            severityColor, severityText, categoryText, vehicleInfo, taskInfo, alertTimeStr);
    }

    private String buildDispatcherTaskCreatedEmailHtml(String taskNo, String taskName, String taskType,
                                                       String priorityText, String startLocation, String endLocation,
                                                       java.time.LocalDateTime startTime, String driverName, String vehicleNo) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String startTimeStr = startTime != null ? startTime.format(formatter) : "未指定";

        String driverInfo = driverName != null && !driverName.trim().isEmpty()
            ? String.format("<p><strong>分配司机：</strong>%s</p>", driverName) : "<p><strong>分配司机：</strong>待分配</p>";
        String vehicleInfo = vehicleNo != null && !vehicleNo.trim().isEmpty()
            ? String.format("<p><strong>分配车辆：</strong>%s</p>", vehicleNo) : "<p><strong>分配车辆：</strong>待分配</p>";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #409EFF;">任务创建确认</h2>
                    <p>您好，</p>
                    <p>您已成功创建了一个新任务，系统将自动分配给合适的司机和车辆。</p>
                    <div style="background: #f0f7ff; padding: 15px; margin: 15px 0; border-left: 4px solid #409EFF; border-radius: 4px;">
                        <h3 style="color: #409EFF; margin-top: 0;">任务信息</h3>
                        <p><strong>任务编号：</strong>%s</p>
                        <p><strong>任务名称：</strong>%s</p>
                        <p><strong>任务类型：</strong>%s</p>
                        <p><strong>优先级：</strong>%s</p>
                        <p><strong>起始位置：</strong>%s</p>
                        <p><strong>目标位置：</strong>%s</p>
                        <p><strong>开始时间：</strong>%s</p>
                        %s
                        %s
                    </div>
                    <p><strong style="color: #409EFF;">请等待系统分配司机和车辆，分配完成后将通知相关人员。</strong></p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, taskNo, taskName, taskType, priorityText, startLocation, endLocation, startTimeStr, driverInfo, vehicleInfo);
    }

    private String buildDispatcherTaskCompletedEmailHtml(String taskNo, String taskName,
                                                         java.time.LocalDateTime completedTime,
                                                         String driverName, String vehicleNo, Long duration) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String completedTimeStr = completedTime != null ? completedTime.format(formatter) : "未知";
        String durationStr = duration != null ? String.format("%d分钟", duration) : "未知";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #67C23A;">任务完成通知</h2>
                    <p>您好，</p>
                    <p>您创建的任务已完成执行，详细信息如下：</p>
                    <div style="background: #f0f9ff; padding: 15px; margin: 15px 0; border-left: 4px solid #67C23A; border-radius: 4px;">
                        <h3 style="color: #67C23A; margin-top: 0;">任务信息</h3>
                        <p><strong>任务编号：</strong>%s</p>
                        <p><strong>任务名称：</strong>%s</p>
                        <p><strong>执行司机：</strong>%s</p>
                        <p><strong>执行车辆：</strong>%s</p>
                        <p><strong>完成时间：</strong>%s</p>
                        <p><strong>执行时长：</strong>%s</p>
                    </div>
                    <p><strong style="color: #67C23A;">任务已成功完成，感谢您的调度管理。</strong></p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, taskNo, taskName, driverName != null ? driverName : "未知",
            vehicleNo != null ? vehicleNo : "未知", completedTimeStr, durationStr);
    }

    private String buildMaintenanceVehicleFaultEmailHtml(String vehicleNo, String vehicleType, String faultType,
                                                         String faultDescription, String severity, String location,
                                                         java.time.LocalDateTime alertTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String alertTimeStr = alertTime != null ? alertTime.format(formatter) : "未知";

        String severityText = switch (severity) {
            case "high" -> "紧急";
            case "medium" -> "重要";
            case "low" -> "一般";
            default -> "未知";
        };
        String severityColor = switch (severity) {
            case "high" -> "#F56C6C";
            case "medium" -> "#E6A23C";
            case "low" -> "#909399";
            default -> "#909399";
        };

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: %s;">车辆故障告警</h2>
                    <p>您好，</p>
                    <p><strong style="color: %s;">检测到车辆故障，请及时前往处理。</strong></p>
                    <div style="background: %s; padding: 15px; margin: 15px 0; border-left: 4px solid %s; border-radius: 4px;">
                        <h3 style="color: %s; margin-top: 0;">车辆信息</h3>
                        <p><strong>车牌号：</strong>%s</p>
                        <p><strong>车辆类型：</strong>%s</p>
                        <p><strong>故障类型：</strong>%s</p>
                        <p><strong>故障描述：</strong>%s</p>
                        <p><strong>严重程度：</strong><span style="color: %s; font-weight: 600;">%s</span></p>
                        <p><strong>故障位置：</strong>%s</p>
                        <p><strong>告警时间：</strong>%s</p>
                    </div>
                    <p><strong style="color: %s;">请尽快前往故障地点进行维修，确保车辆正常运行。</strong></p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, severityColor, severityColor,
            severityColor.equals("#F56C6C") ? "#FEF0F0" : severityColor.equals("#E6A23C") ? "#FDF6EC" : "#F5F7FA",
            severityColor, severityColor, vehicleNo, vehicleType != null ? vehicleType : "未知",
            faultType != null ? faultType : "未知", faultDescription != null ? faultDescription : "无",
            severityColor, severityText, location != null ? location : "未知", alertTimeStr, severityColor);
    }

    private String buildMaintenanceReminderEmailHtml(String vehicleNo, String maintenanceType,
                                                     java.time.LocalDate maintenanceDate,
                                                     java.math.BigDecimal currentMileage,
                                                     java.math.BigDecimal nextMaintenanceMileage,
                                                     Integer daysRemaining) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String maintenanceDateStr = maintenanceDate != null ? maintenanceDate.format(formatter) : "未知";
        String currentMileageStr = currentMileage != null ? currentMileage.toString() : "未知";
        String nextMileageStr = nextMaintenanceMileage != null ? nextMaintenanceMileage.toString() : "未设定";
        String daysRemainingStr = daysRemaining != null ? daysRemaining.toString() : "未知";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #E6A23C;">维修提醒</h2>
                    <p>您好，</p>
                    <p>系统提醒您，以下车辆需要进行维修，请提前安排维修计划。</p>
                    <div style="background: #FDF6EC; padding: 15px; margin: 15px 0; border-left: 4px solid #E6A23C; border-radius: 4px;">
                        <h3 style="color: #E6A23C; margin-top: 0;">车辆信息</h3>
                        <p><strong>车牌号：</strong>%s</p>
                        <p><strong>维修类型：</strong>%s</p>
                        <p><strong>计划维修日期：</strong>%s</p>
                        <p><strong>当前里程：</strong>%s 公里</p>
                        <p><strong>下次维修里程：</strong>%s 公里</p>
                        <p><strong>剩余天数：</strong><span style="color: #E6A23C; font-weight: 600;">%s 天</span></p>
                    </div>
                    <p><strong style="color: #E6A23C;">请提前准备维修材料和工具，确保按时完成维修任务。</strong></p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, vehicleNo, maintenanceType != null ? maintenanceType : "定期保养",
            maintenanceDateStr, currentMileageStr, nextMileageStr, daysRemainingStr);
    }

    private String buildMonitorSystemStatusEmailHtml(String statusType, String statusTitle,
                                                     String statusDescription, String affectedSystems,
                                                     java.time.LocalDateTime reportTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String reportTimeStr = reportTime != null ? reportTime.format(formatter) : "未知";

        String statusText = switch (statusType) {
            case "normal" -> "正常";
            case "warning" -> "警告";
            case "error" -> "错误";
            default -> "未知";
        };
        String statusColor = switch (statusType) {
            case "normal" -> "#67C23A";
            case "warning" -> "#E6A23C";
            case "error" -> "#F56C6C";
            default -> "#909399";
        };

        String affectedSystemsInfo = affectedSystems != null && !affectedSystems.trim().isEmpty()
            ? String.format("<p><strong>受影响系统：</strong>%s</p>", affectedSystems) : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: %s;">系统状态通知</h2>
                    <p>您好，</p>
                    <p>系统状态发生变化，详细信息如下：</p>
                    <div style="background: %s; padding: 15px; margin: 15px 0; border-left: 4px solid %s; border-radius: 4px;">
                        <h3 style="color: %s; margin-top: 0;">状态信息</h3>
                        <p><strong>状态标题：</strong>%s</p>
                        <p><strong>状态类型：</strong><span style="color: %s; font-weight: 600;">%s</span></p>
                        <p><strong>状态描述：</strong>%s</p>
                        %s
                        <p><strong>报告时间：</strong>%s</p>
                    </div>
                    <p><strong style="color: %s;">请继续监控系统状态，如有异常请及时上报。</strong></p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, statusColor,
            statusColor.equals("#67C23A") ? "#F0F9FF" : statusColor.equals("#E6A23C") ? "#FDF6EC" : "#FEF0F0",
            statusColor, statusColor, statusTitle, statusColor, statusText,
            statusDescription != null ? statusDescription : "无", affectedSystemsInfo, reportTimeStr, statusColor);
    }

    private String buildAdminImportantOperationEmailHtml(String operationType, String operationTitle,
                                                         String operationDescription, String operatorName,
                                                         java.time.LocalDateTime operationTime, String affectedResources) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String operationTimeStr = operationTime != null ? operationTime.format(formatter) : "未知";

        String operationTypeText = switch (operationType) {
            case "user_create" -> "创建用户";
            case "user_update" -> "更新用户";
            case "user_delete" -> "删除用户";
            case "role_update" -> "角色变更";
            case "permission_update" -> "权限变更";
            case "system_config" -> "系统配置";
            default -> operationType;
        };

        String affectedResourcesInfo = affectedResources != null && !affectedResources.trim().isEmpty()
            ? String.format("<p><strong>受影响资源：</strong>%s</p>", affectedResources) : "";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #F56C6C;">重要操作通知</h2>
                    <p>您好，系统管理员</p>
                    <p><strong style="color: #F56C6C;">系统发生重要操作，请及时关注。</strong></p>
                    <div style="background: #FEF0F0; padding: 15px; margin: 15px 0; border-left: 4px solid #F56C6C; border-radius: 4px;">
                        <h3 style="color: #F56C6C; margin-top: 0;">操作信息</h3>
                        <p><strong>操作类型：</strong>%s</p>
                        <p><strong>操作标题：</strong>%s</p>
                        <p><strong>操作描述：</strong>%s</p>
                        <p><strong>操作人：</strong>%s</p>
                        %s
                        <p><strong>操作时间：</strong>%s</p>
                    </div>
                    <p><strong style="color: #F56C6C;">请确认此操作是否符合系统安全规范，如有疑问请及时处理。</strong></p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, operationTypeText, operationTitle, operationDescription != null ? operationDescription : "无",
            operatorName != null ? operatorName : "未知", affectedResourcesInfo, operationTimeStr);
    }

    private String buildOperatorOperationConfirmationEmailHtml(String operationType, String resourceName,
                                                               String operationDetails, String operatorName,
                                                               java.time.LocalDateTime operationTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String operationTimeStr = operationTime != null ? operationTime.format(formatter) : "未知";

        String operationTypeText = getOperationTypeText(operationType);

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #409EFF;">操作确认</h2>
                    <p>您好，</p>
                    <p>您的操作已成功执行，详细信息如下：</p>
                    <div style="background: #f0f7ff; padding: 15px; margin: 15px 0; border-left: 4px solid #409EFF; border-radius: 4px;">
                        <h3 style="color: #409EFF; margin-top: 0;">操作信息</h3>
                        <p><strong>操作类型：</strong>%s</p>
                        <p><strong>资源名称：</strong>%s</p>
                        <p><strong>操作详情：</strong>%s</p>
                        <p><strong>操作人：</strong>%s</p>
                        <p><strong>操作时间：</strong>%s</p>
                    </div>
                    <p><strong style="color: #409EFF;">操作已成功完成，请继续您的日常工作。</strong></p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, operationTypeText, resourceName, operationDetails != null ? operationDetails : "无",
            operatorName != null ? operatorName : "未知", operationTimeStr);
    }

    private String buildDriverTaskCompletedConfirmationEmailHtml(String taskNo, String taskName,
                                                                 java.time.LocalDateTime completedTime,
                                                                 Long duration, String startLocation, String endLocation) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String completedTimeStr = completedTime != null ? completedTime.format(formatter) : "未知";
        String durationStr = duration != null ? String.format("%d分钟", duration) : "未知";

        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
            </head>
            <body style="font-family: Arial, 'Microsoft YaHei', sans-serif; line-height: 1.6; color: #333333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px;">
                    <h2 style="color: #67C23A;">任务完成确认</h2>
                    <p>您好，</p>
                    <p>您的任务已完成执行，系统已记录完成信息。</p>
                    <div style="background: #f0f9ff; padding: 15px; margin: 15px 0; border-left: 4px solid #67C23A; border-radius: 4px;">
                        <h3 style="color: #67C23A; margin-top: 0;">任务信息</h3>
                        <p><strong>任务编号：</strong>%s</p>
                        <p><strong>任务名称：</strong>%s</p>
                        <p><strong>起始位置：</strong>%s</p>
                        <p><strong>目标位置：</strong>%s</p>
                        <p><strong>完成时间：</strong>%s</p>
                        <p><strong>执行时长：</strong>%s</p>
                    </div>
                    <p><strong style="color: #67C23A;">感谢您的辛勤工作，任务已完成并已通知调度中心。</strong></p>
                    <p>请等待下一个任务分配。</p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, taskNo, taskName, startLocation != null ? startLocation : "未知",
            endLocation != null ? endLocation : "未知", completedTimeStr, durationStr);
    }

    private String getOperationTypeText(String operationType) {
        return switch (operationType) {
            case "vehicle_create" -> "创建车辆";
            case "vehicle_update" -> "更新车辆";
            case "vehicle_delete" -> "删除车辆";
            case "user_create" -> "创建用户";
            case "user_update" -> "更新用户";
            case "user_delete" -> "删除用户";
            case "task_create" -> "创建任务";
            case "task_update" -> "更新任务";
            case "task_delete" -> "删除任务";
            default -> operationType;
        };
    }

    @Override
    public void sendMaintenanceAlertAcknowledgeEmail(String to, String alertTitle, String alertDescription,
                                                     String severity, String vehicleNo, String maintenanceName,
                                                     java.time.LocalDateTime acknowledgeTime) {
        String subject = String.format("告警确认通知 - %s", alertTitle);
        String htmlContent = buildMaintenanceAlertAcknowledgeEmailHtml(
                alertTitle, alertDescription, severity, vehicleNo, maintenanceName, acknowledgeTime);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendMaintenanceAlertResolveEmail(String to, String alertTitle, String alertDescription,
                                                String severity, String vehicleNo, String maintenanceName,
                                                String resolutionNotes, java.time.LocalDateTime resolveTime) {
        String subject = String.format("告警完成通知 - %s", alertTitle);
        String htmlContent = buildMaintenanceAlertResolveEmailHtml(
                alertTitle, alertDescription, severity, vehicleNo, maintenanceName, resolutionNotes, resolveTime);
        sendHtmlEmail(to, subject, htmlContent);
    }

    @Override
    public void sendMaintenanceAlertReportEmail(String to, String alertTitle, String alertDescription,
                                               String severity, String vehicleNo, String maintenanceName,
                                               java.time.LocalDateTime reportTime) {
        String subject = String.format("告警报告通知 - %s", alertTitle);
        String htmlContent = buildMaintenanceAlertReportEmailHtml(
                alertTitle, alertDescription, severity, vehicleNo, maintenanceName, reportTime);
        sendHtmlEmail(to, subject, htmlContent);
    }

    private String buildMaintenanceAlertAcknowledgeEmailHtml(String alertTitle, String alertDescription,
                                                             String severity, String vehicleNo, String maintenanceName,
                                                             java.time.LocalDateTime acknowledgeTime) {
        String severityText = switch (severity) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> severity;
        };
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #409EFF; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                    .info-row { margin: 10px 0; }
                    .label { font-weight: bold; color: #606266; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #909399; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>告警确认通知</h2>
                    </div>
                    <div class="content">
                        <p>尊敬的管理员，</p>
                        <p>维修员 <strong>%s</strong> 已确认处理以下告警：</p>
                        <div class="info-row">
                            <span class="label">告警标题：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">告警描述：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">严重程度：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">关联车辆：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">确认时间：</span>%s
                        </div>
                        <p style="margin-top: 20px;">维修员已开始处理该告警，请关注处理进度。</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复。</p>
                    </div>
                </div>
            </body>
            </html>
            """, maintenanceName, alertTitle, alertDescription, severityText, vehicleNo,
                acknowledgeTime != null ? acknowledgeTime.toString() : "未知");
    }

    private String buildMaintenanceAlertResolveEmailHtml(String alertTitle, String alertDescription,
                                                         String severity, String vehicleNo, String maintenanceName,
                                                         String resolutionNotes, java.time.LocalDateTime resolveTime) {
        String severityText = switch (severity) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> severity;
        };
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #67C23A; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                    .info-row { margin: 10px 0; }
                    .label { font-weight: bold; color: #606266; }
                    .notes { background-color: #fff; padding: 15px; border-left: 4px solid #67C23A; margin: 15px 0; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #909399; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>告警完成通知</h2>
                    </div>
                    <div class="content">
                        <p>尊敬的管理员，</p>
                        <p>维修员 <strong>%s</strong> 已完成以下告警的处理：</p>
                        <div class="info-row">
                            <span class="label">告警标题：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">告警描述：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">严重程度：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">关联车辆：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">完成时间：</span>%s
                        </div>
                        <div class="notes">
                            <strong>处理说明：</strong><br>%s
                        </div>
                        <p style="margin-top: 20px;">告警已标记为已解决，请确认处理结果。</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复。</p>
                    </div>
                </div>
            </body>
            </html>
            """, maintenanceName, alertTitle, alertDescription, severityText, vehicleNo,
                resolveTime != null ? resolveTime.toString() : "未知", resolutionNotes);
    }

    private String buildMaintenanceAlertReportEmailHtml(String alertTitle, String alertDescription,
                                                        String severity, String vehicleNo, String maintenanceName,
                                                        java.time.LocalDateTime reportTime) {
        String severityText = switch (severity) {
            case "high" -> "高";
            case "medium" -> "中";
            case "low" -> "低";
            default -> severity;
        };
        
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }
                    .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background-color: #E6A23C; color: white; padding: 20px; text-align: center; }
                    .content { background-color: #f9f9f9; padding: 20px; margin-top: 20px; }
                    .info-row { margin: 10px 0; }
                    .label { font-weight: bold; color: #606266; }
                    .footer { margin-top: 20px; padding-top: 20px; border-top: 1px solid #ddd; font-size: 12px; color: #909399; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>告警报告通知</h2>
                    </div>
                    <div class="content">
                        <p>尊敬的管理员，</p>
                        <p>维修员 <strong>%s</strong> 向您报告了以下告警：</p>
                        <div class="info-row">
                            <span class="label">告警标题：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">告警描述：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">严重程度：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">关联车辆：</span>%s
                        </div>
                        <div class="info-row">
                            <span class="label">报告时间：</span>%s
                        </div>
                        <p style="margin-top: 20px;">请及时查看告警详情并安排处理。</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由系统自动发送，请勿回复。</p>
                    </div>
                </div>
            </body>
            </html>
            """, maintenanceName, alertTitle, alertDescription, severityText, vehicleNo,
                reportTime != null ? reportTime.toString() : "未知");
    }
}
