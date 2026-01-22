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
                                      String vehicleNo, String vehicleBrand, String vehicleModel,
                                      String dispatcherName, String dispatcherRole);

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
                                            String vehicleNo, String vehicleBrand, String vehicleModel,
                                            String dispatcherName, String dispatcherRole);

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

    // ==================== 角色特定邮件模板 ====================
    
    /**
     * 发送告警通知邮件（根据角色定制内容）
     * 
     * @param to 收件人邮箱
     * @param roleCode 角色代码（ADMIN, DISPATCHER, MAINTENANCE, MONITOR等）
     * @param alertTitle 告警标题
     * @param alertDescription 告警描述
     * @param severity 严重程度
     * @param category 告警类别
     * @param vehicleNo 关联车辆车牌号（可选）
     * @param taskNo 关联任务编号（可选）
     * @param alertTime 告警时间
     */
    void sendAlertNotificationEmail(String to, String roleCode, String alertTitle,
                                    String alertDescription, String severity, String category,
                                    String vehicleNo, String taskNo, java.time.LocalDateTime alertTime);

    /**
     * 发送调度员任务创建确认邮件
     * 
     * @param to 收件人邮箱
     * @param taskNo 任务编号
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param priority 优先级
     * @param startLocation 起始位置
     * @param endLocation 目标位置
     * @param startTime 开始时间
     * @param driverName 分配的司机姓名（可选）
     * @param vehicleNo 分配的车辆车牌号（可选）
     */
    void sendDispatcherTaskCreatedEmail(String to, String taskNo, String taskName,
                                        String taskType, Integer priority,
                                        String startLocation, String endLocation,
                                        java.time.LocalDateTime startTime,
                                        String driverName, String vehicleNo);

    /**
     * 发送调度员任务完成通知邮件
     * 
     * @param to 收件人邮箱
     * @param taskNo 任务编号
     * @param taskName 任务名称
     * @param completedTime 完成时间
     * @param driverName 执行司机
     * @param vehicleNo 执行车辆
     * @param duration 执行时长（分钟）
     */
    void sendDispatcherTaskCompletedEmail(String to, String taskNo, String taskName,
                                          java.time.LocalDateTime completedTime,
                                          String driverName, String vehicleNo, Long duration);

    /**
     * 发送维修员车辆故障告警邮件
     * 
     * @param to 收件人邮箱
     * @param vehicleNo 车牌号
     * @param vehicleType 车辆类型
     * @param faultType 故障类型
     * @param faultDescription 故障描述
     * @param severity 严重程度
     * @param location 故障位置
     * @param alertTime 告警时间
     */
    void sendMaintenanceVehicleFaultEmail(String to, String vehicleNo, String vehicleType,
                                          String faultType, String faultDescription,
                                          String severity, String location,
                                          java.time.LocalDateTime alertTime);

    /**
     * 发送维修员维修提醒邮件
     * 
     * @param to 收件人邮箱
     * @param vehicleNo 车牌号
     * @param maintenanceType 维修类型
     * @param maintenanceDate 计划维修日期
     * @param currentMileage 当前里程
     * @param nextMaintenanceMileage 下次维修里程
     * @param daysRemaining 剩余天数
     */
    void sendMaintenanceReminderEmail(String to, String vehicleNo, String maintenanceType,
                                     java.time.LocalDate maintenanceDate,
                                     java.math.BigDecimal currentMileage,
                                     java.math.BigDecimal nextMaintenanceMileage,
                                     Integer daysRemaining);

    /**
     * 发送监控员系统状态通知邮件
     * 
     * @param to 收件人邮箱
     * @param statusType 状态类型（normal-正常, warning-警告, error-错误）
     * @param statusTitle 状态标题
     * @param statusDescription 状态描述
     * @param affectedSystems 受影响系统（可选）
     * @param reportTime 报告时间
     */
    void sendMonitorSystemStatusEmail(String to, String statusType, String statusTitle,
                                     String statusDescription, String affectedSystems,
                                     java.time.LocalDateTime reportTime);

    /**
     * 发送管理员重要操作通知邮件
     * 
     * @param to 收件人邮箱
     * @param operationType 操作类型
     * @param operationTitle 操作标题
     * @param operationDescription 操作描述
     * @param operatorName 操作人
     * @param operationTime 操作时间
     * @param affectedResources 受影响资源（可选）
     */
    void sendAdminImportantOperationEmail(String to, String operationType, String operationTitle,
                                         String operationDescription, String operatorName,
                                         java.time.LocalDateTime operationTime,
                                         String affectedResources);

    /**
     * 发送操作员操作确认邮件
     * 
     * @param to 收件人邮箱
     * @param operationType 操作类型（vehicle_create, vehicle_update, user_create等）
     * @param resourceName 资源名称（车辆车牌号、用户名等）
     * @param operationDetails 操作详情
     * @param operatorName 操作人
     * @param operationTime 操作时间
     */
    void sendOperatorOperationConfirmationEmail(String to, String operationType,
                                                String resourceName, String operationDetails,
                                                String operatorName, java.time.LocalDateTime operationTime);

    /**
     * 发送任务完成确认邮件（给司机）
     * 
     * @param to 收件人邮箱
     * @param taskNo 任务编号
     * @param taskName 任务名称
     * @param completedTime 完成时间
     * @param duration 执行时长（分钟）
     * @param startLocation 起始位置
     * @param endLocation 目标位置
     */
    void sendDriverTaskCompletedConfirmationEmail(String to, String taskNo, String taskName,
                                                  java.time.LocalDateTime completedTime,
                                                  Long duration, String startLocation, String endLocation);

    /**
     * 异步发送维修员任务分配通知邮件（不阻塞主流程）
     * 
     * @param to 收件人邮箱
     * @param taskNo 任务编号
     * @param taskName 任务名称
     * @param taskType 任务类型
     * @param priority 优先级
     * @param startLocation 起始位置（车辆位置）
     * @param endLocation 目标位置（维修地点）
     * @param startTime 开始时间
     * @param vehicleNo 车牌号
     * @param vehicleBrand 车辆品牌
     * @param vehicleModel 车辆型号
     * @param description 任务描述（故障描述等）
     * @param dispatcherName 调度员姓名
     * @param dispatcherRole 调度员角色
     */
    void sendMaintenanceTaskAssignmentEmailAsync(String to, String taskNo, String taskName,
                                                 String taskType, Integer priority,
                                                 String startLocation, String endLocation,
                                                 java.time.LocalDateTime startTime,
                                                 String vehicleNo, String vehicleBrand, String vehicleModel,
                                                 String description, String dispatcherName, String dispatcherRole);

    /**
     * 发送维修员告警确认邮件给管理员
     * 
     * @param to 收件人邮箱（管理员）
     * @param alertTitle 告警标题
     * @param alertDescription 告警描述
     * @param severity 严重程度
     * @param vehicleNo 车辆车牌号
     * @param maintenanceName 维修员姓名
     * @param acknowledgeTime 确认时间
     */
    void sendMaintenanceAlertAcknowledgeEmail(String to, String alertTitle, String alertDescription,
                                              String severity, String vehicleNo, String maintenanceName,
                                              java.time.LocalDateTime acknowledgeTime);

    /**
     * 发送维修员告警完成邮件给管理员
     * 
     * @param to 收件人邮箱（管理员）
     * @param alertTitle 告警标题
     * @param alertDescription 告警描述
     * @param severity 严重程度
     * @param vehicleNo 车辆车牌号
     * @param maintenanceName 维修员姓名
     * @param resolutionNotes 处理说明
     * @param resolveTime 解决时间
     */
    void sendMaintenanceAlertResolveEmail(String to, String alertTitle, String alertDescription,
                                         String severity, String vehicleNo, String maintenanceName,
                                         String resolutionNotes, java.time.LocalDateTime resolveTime);

    /**
     * 发送维修员告警报告邮件给管理员
     * 
     * @param to 收件人邮箱（管理员）
     * @param alertTitle 告警标题
     * @param alertDescription 告警描述
     * @param severity 严重程度
     * @param vehicleNo 车辆车牌号
     * @param maintenanceName 维修员姓名
     * @param reportTime 报告时间
     */
    void sendMaintenanceAlertReportEmail(String to, String alertTitle, String alertDescription,
                                        String severity, String vehicleNo, String maintenanceName,
                                        java.time.LocalDateTime reportTime);
}
