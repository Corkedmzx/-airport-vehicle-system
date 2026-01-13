package com.airport.service.impl;

import com.airport.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

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
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送HTML邮件到: {}", to);
            return;
        }

        try {
            smtpEmailClient.sendHtmlEmail(to, subject, htmlContent);
            log.info("HTML邮件发送成功，收件人: {}, 主题: {}", to, subject);
        } catch (Exception e) {
            log.error("发送HTML邮件失败，收件人: {}, 主题: {}", to, subject, e);
            throw new RuntimeException("发送HTML邮件失败: " + e.getMessage(), e);
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
                                             String vehicleNo, String vehicleBrand, String vehicleModel) {
        if (!emailEnabled) {
            log.warn("邮件服务未启用，跳过发送司机任务分配邮件到: {}", to);
            return;
        }

        String subject = "任务分配通知 - " + taskNo;
        String priorityText = getPriorityText(priority);

        String htmlContent = buildDriverTaskAssignmentEmailHtml(taskNo, taskName, taskType, 
                                                                 priorityText, startLocation, 
                                                                 endLocation, startTime,
                                                                 vehicleNo, vehicleBrand, vehicleModel);

        sendHtmlEmail(to, subject, htmlContent);
    }

    private String buildDriverTaskAssignmentEmailHtml(String taskNo, String taskName,
                                                     String taskType, String priorityText,
                                                     String startLocation, String endLocation,
                                                     java.time.LocalDateTime startTime,
                                                     String vehicleNo, String vehicleBrand, String vehicleModel) {
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
                    <p><strong style="color: #2196F3;">请按时到达起始位置，开始执行任务。</strong></p>
                    <p>如有疑问或特殊情况，请及时联系调度中心。</p>
                    <p style="color: #999; font-size: 12px; margin-top: 30px;">此邮件由机场车辆监控与调度系统自动发送，请勿回复。</p>
                </div>
            </body>
            </html>
            """, taskNo, taskName, taskType, priorityText, startLocation, endLocation, startTimeStr, vehicleInfo);
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
                                                   String vehicleNo, String vehicleBrand, String vehicleModel) {
        log.info("开始异步发送任务分配邮件，收件人: {}", to);
        try {
            sendDriverTaskAssignmentEmail(to, taskNo, taskName, taskType, priority, 
                                         startLocation, endLocation, startTime, 
                                         vehicleNo, vehicleBrand, vehicleModel);
            log.info("异步任务分配邮件发送完成，收件人: {}", to);
        } catch (Exception e) {
            log.error("异步发送任务分配邮件失败，收件人: {}", to, e);
        }
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
}
