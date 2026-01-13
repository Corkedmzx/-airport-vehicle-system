package com.airport.service;

/**
 * 邮件服务接口
 * 
 * @author Corkedmzx
 */
public interface EmailService {

    /**
     * 发送简单文本邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件内容
     */
    void sendSimpleEmail(String to, String subject, String content);

    /**
     * 发送HTML格式邮件
     * 
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML格式的邮件内容
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);

    /**
     * 发送任务分配通知邮件
     * 
     * @param to 收件人邮箱
     * @param taskNo 任务编号
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param priority 优先级
     * @param startLocation 起始位置
     * @param endLocation 目标位置
     * @param startTime 开始时间
     */
    void sendTaskAssignmentEmail(String to, String taskNo, String taskName, 
                                   String taskType, Integer priority,
                                   String startLocation, String endLocation,
                                   java.time.LocalDateTime startTime);

    /**
     * 发送司机任务分配通知邮件（包含车辆信息）
     * 
     * @param to 收件人邮箱
     * @param taskNo 任务编号
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param priority 优先级
     * @param startLocation 起始位置
     * @param endLocation 目标位置
     * @param startTime 开始时间
     * @param vehicleNo 车牌号
     * @param vehicleBrand 车辆品牌
     * @param vehicleModel 车辆型号
     */
    void sendDriverTaskAssignmentEmail(String to, String taskNo, String taskName,
                                       String taskType, Integer priority,
                                       String startLocation, String endLocation,
                                       java.time.LocalDateTime startTime,
                                       String vehicleNo, String vehicleBrand, String vehicleModel);

    /**
     * 异步发送司机任务分配通知邮件（不阻塞主流程）
     * 
     * @param to 收件人邮箱
     * @param taskNo 任务编号
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param priority 优先级
     * @param startLocation 起始位置
     * @param endLocation 目标位置
     * @param startTime 开始时间
     * @param vehicleNo 车牌号
     * @param vehicleBrand 车辆品牌
     * @param vehicleModel 车辆型号
     */
    void sendDriverTaskAssignmentEmailAsync(String to, String taskNo, String taskName,
                                            String taskType, Integer priority,
                                            String startLocation, String endLocation,
                                            java.time.LocalDateTime startTime,
                                            String vehicleNo, String vehicleBrand, String vehicleModel);

    /**
     * 发送任务取消分配通知邮件
     * 
     * @param to 收件人邮箱
     * @param taskNo 任务编号
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param priority 优先级
     * @param startLocation 起始位置
     * @param endLocation 目标位置
     * @param startTime 开始时间
     * @param vehicleNo 车牌号
     * @param reason 取消原因（可选）
     */
    void sendTaskUnassignmentEmail(String to, String taskNo, String taskName,
                                   String taskType, Integer priority,
                                   String startLocation, String endLocation,
                                   java.time.LocalDateTime startTime,
                                   String vehicleNo, String reason);

    /**
     * 异步发送任务取消分配通知邮件（不阻塞主流程）
     * 
     * @param to 收件人邮箱
     * @param taskNo 任务编号
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param priority 优先级
     * @param startLocation 起始位置
     * @param endLocation 目标位置
     * @param startTime 开始时间
     * @param vehicleNo 车牌号
     * @param reason 取消原因（可选）
     */
    void sendTaskUnassignmentEmailAsync(String to, String taskNo, String taskName,
                                        String taskType, Integer priority,
                                        String startLocation, String endLocation,
                                        java.time.LocalDateTime startTime,
                                        String vehicleNo, String reason);
}
