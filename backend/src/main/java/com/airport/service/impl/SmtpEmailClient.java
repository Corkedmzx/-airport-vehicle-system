package com.airport.service.impl;

import com.airport.entity.SysUser;
import com.airport.repository.SysUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import jakarta.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * SMTP邮件客户端
 * 从数据库读取用户邮箱地址和授权码，使用各自的邮箱账号发送邮件
 * 
 * @author Corkedmzx
 */
@Slf4j
@Component
public class SmtpEmailClient {

    private final SysUserRepository userRepository;

    @Value("${spring.mail.host:smtp.163.com}")
    private String defaultSmtpHost;

    @Value("${spring.mail.port:465}")
    private Integer defaultSmtpPort;

    @Value("${spring.mail.from-name:机场车辆系统}")
    private String defaultFromName;

    @Value("${spring.mail.properties.mail.smtp.ssl.enable:true}")
    private Boolean defaultSslEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:false}")
    private Boolean defaultStarttlsEnable;

    public SmtpEmailClient(SysUserRepository userRepository) {
        this.userRepository = userRepository;
        log.info("SMTP邮件客户端已初始化，支持从数据库读取用户邮箱和授权码");
    }

    /**
     * 根据邮箱地址从数据库获取用户信息
     */
    private SysUser getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return null;
        }
        return userRepository.findByEmail(email).orElse(null);
    }

    /**
     * 根据邮箱域名推断SMTP服务器地址
     */
    private String getSmtpHostByEmail(String email) {
        if (email == null || !email.contains("@")) {
            return defaultSmtpHost;
        }
        
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        
        // 常见邮箱SMTP服务器映射
        if (domain.contains("163.com")) {
            return "smtp.163.com";
        } else if (domain.contains("qq.com")) {
            return "smtp.qq.com";
        } else if (domain.contains("gmail.com")) {
            return "smtp.gmail.com";
        } else if (domain.contains("126.com")) {
            return "smtp.126.com";
        } else if (domain.contains("sina.com")) {
            return "smtp.sina.com";
        } else if (domain.contains("outlook.com") || domain.contains("hotmail.com")) {
            return "smtp-mail.outlook.com";
        }
        
        // 默认使用配置的SMTP服务器
        return defaultSmtpHost;
    }

    /**
     * 根据邮箱域名推断SMTP端口
     */
    private Integer getSmtpPortByEmail(String email) {
        if (email == null || !email.contains("@")) {
            return defaultSmtpPort;
        }
        
        String domain = email.substring(email.indexOf("@") + 1).toLowerCase();
        
        // 大多数邮箱使用465（SSL）或587（TLS）
        // 默认使用465
        return defaultSmtpPort;
    }

    /**
     * 创建JavaMailSender实例（使用用户自己的邮箱配置）
     */
    private JavaMailSender createMailSender(String email, String authCode) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        String smtpHost = getSmtpHostByEmail(email);
        Integer smtpPort = getSmtpPortByEmail(email);
        
        mailSender.setHost(smtpHost);
        mailSender.setPort(smtpPort);
        mailSender.setUsername(email);
        mailSender.setPassword(authCode);
        
        // 配置SMTP属性
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        
        // 根据端口判断使用SSL还是TLS
        if (smtpPort == 465) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.starttls.enable", "false");
        } else if (smtpPort == 587) {
            props.put("mail.smtp.ssl.enable", "false");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
        } else {
            // 使用默认配置
            props.put("mail.smtp.ssl.enable", defaultSslEnable.toString());
            props.put("mail.smtp.starttls.enable", defaultStarttlsEnable.toString());
        }
        
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        
        return mailSender;
    }

    /**
     * 发送文本格式邮件
     * 从数据库读取收件人的邮箱和授权码，使用收件人的邮箱账号发送
     */
    public void sendTextEmail(String to, String subject, String content) {
        try {
            // 从数据库获取收件人信息
            SysUser recipient = getUserByEmail(to);
            if (recipient == null || recipient.getEmail() == null) {
                throw new RuntimeException("收件人邮箱不存在于数据库中: " + to);
            }
            
            if (recipient.getEmailAuthCode() == null || recipient.getEmailAuthCode().trim().isEmpty()) {
                throw new RuntimeException("收件人邮箱未配置授权码: " + to);
            }
            
            // 使用收件人的邮箱和授权码创建邮件发送器
            JavaMailSender mailSender = createMailSender(recipient.getEmail(), recipient.getEmailAuthCode());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(recipient.getEmail(), defaultFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, false);
            
            mailSender.send(message);
            log.info("SMTP文本邮件发送成功，发信人: {}, 收件人: {}, 主题: {}", recipient.getEmail(), to, subject);
        } catch (Exception e) {
            log.error("SMTP文本邮件发送失败，收件人: {}, 主题: {}", to, subject, e);
            throw new RuntimeException("SMTP邮件发送失败: " + e.getMessage(), e);
        }
    }

    /**
     * 发送HTML格式邮件
     * 从数据库读取收件人的邮箱和授权码，使用收件人的邮箱账号发送
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            // 从数据库获取收件人信息
            SysUser recipient = getUserByEmail(to);
            if (recipient == null || recipient.getEmail() == null) {
                throw new RuntimeException("收件人邮箱不存在于数据库中: " + to);
            }
            
            if (recipient.getEmailAuthCode() == null || recipient.getEmailAuthCode().trim().isEmpty()) {
                throw new RuntimeException("收件人邮箱未配置授权码: " + to);
            }
            
            // 使用收件人的邮箱和授权码创建邮件发送器
            JavaMailSender mailSender = createMailSender(recipient.getEmail(), recipient.getEmailAuthCode());
            
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(recipient.getEmail(), defaultFromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            
            mailSender.send(message);
            log.info("SMTP HTML邮件发送成功，发信人: {}, 收件人: {}, 主题: {}, HTML长度: {} 字符", 
                    recipient.getEmail(), to, subject, htmlContent.length());
        } catch (Exception e) {
            log.error("SMTP HTML邮件发送失败，收件人: {}, 主题: {}", to, subject, e);
            throw new RuntimeException("SMTP HTML邮件发送失败: " + e.getMessage(), e);
        }
    }
}
