package com.airport.controller;

import com.airport.dto.Result;
import com.airport.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 邮件管理控制器
 * 
 * @author Corkedmzx
 */
@Slf4j
@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
@Tag(name = "邮件管理", description = "邮件发送相关接口")
public class EmailController {

    private final EmailService emailService;
    private final com.airport.utils.JwtUtils jwtUtils;

    /**
     * 从请求头中获取当前用户名
     */
    private String getCurrentUsername(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtUtils.validateToken(token, jwtUtils.getUsernameFromToken(token))) {
                    return jwtUtils.getUsernameFromToken(token);
                }
            }
        } catch (Exception e) {
            log.error("获取当前用户失败", e);
        }
        return null;
    }

    @PostMapping("/test")
    @Operation(summary = "发送测试邮件", description = "向指定邮箱发送测试邮件")
    public Result<String> sendTestEmail(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        try {
            String email = request.get("email");
            String subject = request.get("subject");
            
            if (email == null || email.trim().isEmpty()) {
                return Result.error("邮箱地址不能为空");
            }
            
            if (subject == null || subject.trim().isEmpty()) {
                subject = "测试邮件 - 机场车辆监控与调度系统";
            }
            
            String currentUsername = getCurrentUsername(httpRequest);
            if (currentUsername == null) {
                return Result.unauthorized("未认证或认证已过期");
            }
            
            // 构建测试邮件内容
            String htmlContent = buildTestEmailHtml(currentUsername);
            
            // 发送测试邮件
            emailService.sendHtmlEmail(email, subject, htmlContent);
            
            log.info("用户 {} 向 {} 发送了测试邮件", currentUsername, email);
            return Result.success("测试邮件发送成功");
        } catch (Exception e) {
            log.error("发送测试邮件失败", e);
            return Result.error("发送测试邮件失败: " + e.getMessage());
        }
    }

    /**
     * 构建测试邮件HTML内容
     */
    private String buildTestEmailHtml(String username) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 20px; border-radius: 8px 8px 0 0; }" +
                ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; }" +
                ".success-icon { font-size: 48px; text-align: center; margin: 20px 0; }" +
                ".message { background: white; padding: 20px; border-radius: 8px; margin: 20px 0; border-left: 4px solid #67c23a; }" +
                ".footer { text-align: center; margin-top: 30px; color: #909399; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'>" +
                "<h1 style='margin: 0;'>机场车辆监控与调度系统</h1>" +
                "</div>" +
                "<div class='content'>" +
                "<div class='success-icon'>✅</div>" +
                "<div class='message'>" +
                "<h2 style='margin-top: 0; color: #67c23a;'>测试邮件发送成功！</h2>" +
                "<p>尊敬的用户 <strong>" + username + "</strong>，</p>" +
                "<p>这是一封来自机场车辆监控与调度系统的测试邮件。</p>" +
                "<p>如果您收到了这封邮件，说明您的邮箱配置正确，系统可以正常向您发送邮件通知。</p>" +
                "<p>系统将自动向您发送以下类型的通知：</p>" +
                "<ul>" +
                "<li>任务分配和完成通知</li>" +
                "<li>车辆故障和维修提醒</li>" +
                "<li>系统告警和重要操作通知</li>" +
                "</ul>" +
                "<p>感谢您使用机场车辆监控与调度系统！</p>" +
                "</div>" +
                "<div class='footer'>" +
                "<p>此邮件由系统自动发送，请勿回复。</p>" +
                "<p>© 2026 机场车辆监控与调度系统</p>" +
                "</div>" +
                "</div>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}
