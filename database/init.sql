-- 机场车辆监控与调度系统 - 数据库初始化脚本
-- 作者: Corkedmzx
-- 日期: 2025-11-30

-- 创建数据库
CREATE DATABASE IF NOT EXISTS airport_vehicle_system 
CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE airport_vehicle_system;

-- ================================
-- 1. 用户管理相关表
-- ================================

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` varchar(50) NOT NULL COMMENT '用户名',
    `password` varchar(100) NOT NULL COMMENT '密码(加密)',
    `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
    `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
    `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
    `avatar` varchar(255) DEFAULT NULL COMMENT '头像',
    `status` tinyint DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `role_name` varchar(50) NOT NULL COMMENT '角色名称',
    `role_code` varchar(50) NOT NULL COMMENT '角色编码',
    `description` varchar(200) DEFAULT NULL COMMENT '描述',
    `status` tinyint DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色关联表';

-- 权限表
CREATE TABLE IF NOT EXISTS `sys_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限ID',
    `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
    `permission_code` varchar(100) NOT NULL COMMENT '权限编码',
    `permission_type` varchar(20) DEFAULT 'api' COMMENT '权限类型:menu-菜单,button-按钮,api-接口',
    `resource` varchar(200) DEFAULT NULL COMMENT '资源路径',
    `description` varchar(200) DEFAULT NULL COMMENT '描述',
    `status` tinyint DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_permission_code` (`permission_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权限表';

-- 角色权限关联表
CREATE TABLE IF NOT EXISTS `sys_role_permission` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `role_id` bigint NOT NULL COMMENT '角色ID',
    `permission_id` bigint NOT NULL COMMENT '权限ID',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_permission` (`role_id`, `permission_id`),
    KEY `idx_role_id` (`role_id`),
    KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色权限关联表';

-- ================================
-- 2. 车辆管理相关表
-- ================================

-- 车辆类型表
CREATE TABLE IF NOT EXISTS `vehicle_type` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '类型ID',
    `type_name` varchar(50) NOT NULL COMMENT '类型名称',
    `type_code` varchar(20) NOT NULL COMMENT '类型编码',
    `description` varchar(200) DEFAULT NULL COMMENT '描述',
    `max_speed` decimal(5,2) DEFAULT NULL COMMENT '最大速度(km/h)',
    `fuel_type` varchar(20) DEFAULT NULL COMMENT '燃料类型',
    `status` tinyint DEFAULT 1 COMMENT '状态:0-禁用,1-启用',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆类型表';

-- 车辆表
CREATE TABLE IF NOT EXISTS `vehicle` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '车辆ID',
    `vehicle_no` varchar(20) NOT NULL COMMENT '车牌号',
    `vehicle_type_id` bigint NOT NULL COMMENT '车辆类型ID',
    `brand` varchar(50) DEFAULT NULL COMMENT '品牌',
    `model` varchar(50) DEFAULT NULL COMMENT '型号',
    `color` varchar(20) DEFAULT NULL COMMENT '颜色',
    `engine_no` varchar(50) DEFAULT NULL COMMENT '发动机号',
    `vin` varchar(50) DEFAULT NULL COMMENT '车架号',
    `purchase_date` date DEFAULT NULL COMMENT '购买日期',
    `mileage` decimal(10,2) DEFAULT '0.00' COMMENT '里程数(km)',
    `fuel_capacity` decimal(5,2) DEFAULT NULL COMMENT '油箱容量(L)',
    `current_fuel` decimal(5,2) DEFAULT NULL COMMENT '当前油量(L)',
    `gps_device_id` varchar(50) DEFAULT NULL COMMENT 'GPS设备ID',
    `status` tinyint DEFAULT 1 COMMENT '状态:0-停用,1-正常,2-维修中,3-故障',
    `location_longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
    `location_latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
    `location_address` varchar(255) DEFAULT NULL COMMENT '当前位置',
    `last_update_time` datetime DEFAULT NULL COMMENT '最后位置更新时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_vehicle_no` (`vehicle_no`),
    UNIQUE KEY `uk_gps_device_id` (`gps_device_id`),
    KEY `idx_vehicle_type_id` (`vehicle_type_id`),
    KEY `idx_status` (`status`),
    KEY `idx_location` (`location_longitude`, `location_latitude`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆表';

-- 车辆实时位置表
CREATE TABLE IF NOT EXISTS `vehicle_location` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `vehicle_id` bigint NOT NULL COMMENT '车辆ID',
    `longitude` decimal(10,7) NOT NULL COMMENT '经度',
    `latitude` decimal(10,7) NOT NULL COMMENT '纬度',
    `altitude` decimal(8,2) DEFAULT NULL COMMENT '海拔(m)',
    `speed` decimal(5,2) DEFAULT NULL COMMENT '速度(km/h)',
    `direction` decimal(5,2) DEFAULT NULL COMMENT '方向角(度)',
    `accuracy` decimal(5,2) DEFAULT NULL COMMENT '精度(m)',
    `location_time` datetime NOT NULL COMMENT '定位时间',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_id` (`vehicle_id`),
    KEY `idx_location_time` (`location_time`),
    KEY `idx_vehicle_time` (`vehicle_id`, `location_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆实时位置表';

-- ================================
-- 3. 调度管理相关表
-- ================================

-- 调度任务表
CREATE TABLE IF NOT EXISTS `dispatch_task` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
    `task_no` varchar(20) NOT NULL COMMENT '任务编号',
    `task_name` varchar(100) NOT NULL COMMENT '任务名称',
    `task_type` varchar(20) NOT NULL COMMENT '任务类型:常规调度,紧急调度,维护调度',
    `priority` tinyint DEFAULT 2 COMMENT '优先级:1-低,2-中,3-高,4-紧急',
    `description` text COMMENT '任务描述',
    `start_location` varchar(200) NOT NULL COMMENT '起始位置',
    `end_location` varchar(200) NOT NULL COMMENT '目标位置',
    `start_time` datetime NOT NULL COMMENT '开始时间',
    `end_time` datetime DEFAULT NULL COMMENT '预计结束时间',
    `actual_start_time` datetime DEFAULT NULL COMMENT '实际开始时间',
    `actual_end_time` datetime DEFAULT NULL COMMENT '实际结束时间',
    `assigned_vehicle_id` bigint DEFAULT NULL COMMENT '分配车辆ID',
    `assigned_driver_id` bigint DEFAULT NULL COMMENT '分配司机ID',
    `status` tinyint DEFAULT 1 COMMENT '状态:1-待分配,2-已分配,3-执行中,4-已完成,5-已取消,6-异常',
    `progress` decimal(5,2) DEFAULT '0.00' COMMENT '完成进度(%)',
    `remark` text COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_no` (`task_no`),
    KEY `idx_assigned_vehicle_id` (`assigned_vehicle_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='调度任务表';

-- 任务执行记录表
CREATE TABLE IF NOT EXISTS `task_execution` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `task_id` bigint NOT NULL COMMENT '任务ID',
    `vehicle_id` bigint NOT NULL COMMENT '车辆ID',
    `driver_name` varchar(50) DEFAULT NULL COMMENT '司机姓名',
    `execution_status` varchar(20) NOT NULL COMMENT '执行状态:开始执行,到达起点,开始运输,到达终点,任务完成',
    `execution_time` datetime NOT NULL COMMENT '执行时间',
    `location_longitude` decimal(10,7) DEFAULT NULL COMMENT '执行位置经度',
    `location_latitude` decimal(10,7) DEFAULT NULL COMMENT '执行位置纬度',
    `description` varchar(500) DEFAULT NULL COMMENT '执行描述',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_vehicle_id` (`vehicle_id`),
    KEY `idx_execution_time` (`execution_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行记录表';

-- ================================
-- 4. 维护管理相关表
-- ================================

-- 告警信息表
CREATE TABLE IF NOT EXISTS `alert` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '告警ID',
    `title` varchar(200) NOT NULL COMMENT '告警标题',
    `description` text COMMENT '告警描述',
    `severity` varchar(20) NOT NULL COMMENT '严重程度:high,medium,low',
    `category` varchar(50) NOT NULL COMMENT '告警类别:vehicle_fault,task_timeout,system_error,safety_alert',
    `vehicle_id` bigint DEFAULT NULL COMMENT '关联车辆ID',
    `task_id` bigint DEFAULT NULL COMMENT '关联任务ID',
    `status` varchar(20) DEFAULT 'unprocessed' COMMENT '状态:unprocessed-未处理,processing-处理中,resolved-已解决',
    `assignee` varchar(50) DEFAULT NULL COMMENT '处理人',
    `acknowledged` tinyint DEFAULT 0 COMMENT '是否已确认:0-未确认,1-已确认',
    `acknowledged_time` datetime DEFAULT NULL COMMENT '确认时间',
    `resolved_time` datetime DEFAULT NULL COMMENT '解决时间',
    `resolution_notes` text COMMENT '解决备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_id` (`vehicle_id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_status` (`status`),
    KEY `idx_severity` (`severity`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警信息表';

-- 告警规则表
CREATE TABLE IF NOT EXISTS `alert_rule` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '规则ID',
    `rule_name` varchar(100) NOT NULL COMMENT '规则名称',
    `rule_type` varchar(50) NOT NULL COMMENT '规则类型:vehicle_fault,task_timeout,system_error,safety_alert,fuel_low,speed_exceed',
    `condition_type` varchar(50) NOT NULL COMMENT '条件类型:大于,小于,等于,范围',
    `condition_value` varchar(200) NOT NULL COMMENT '条件值',
    `severity` varchar(20) NOT NULL COMMENT '告警严重程度:high,medium,low',
    `enabled` tinyint DEFAULT 1 COMMENT '是否启用:0-禁用,1-启用',
    `description` text COMMENT '规则描述',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_rule_type` (`rule_type`),
    KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警规则表';

-- 插入默认告警规则
INSERT INTO `alert_rule` (`rule_name`, `rule_type`, `condition_type`, `condition_value`, `severity`, `description`) VALUES
('车辆引擎温度过高', 'vehicle_fault', '大于', '100', 'high', '当车辆引擎温度超过100度时触发告警'),
('车辆油量过低', 'fuel_low', '小于', '20', 'medium', '当车辆油量低于20%时触发告警'),
('任务执行超时', 'task_timeout', '大于', '3600', 'medium', '当任务执行时间超过1小时时触发告警'),
('车辆速度超限', 'speed_exceed', '大于', '80', 'high', '当车辆速度超过80km/h时触发告警'),
('系统错误', 'system_error', '等于', 'error', 'high', '当系统发生错误时触发告警'),
('安全告警', 'safety_alert', '等于', 'safety', 'high', '当发生安全相关问题时触发告警') AS new
ON DUPLICATE KEY UPDATE `rule_name` = new.`rule_name`;

-- 维修记录表
CREATE TABLE IF NOT EXISTS `maintenance_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `vehicle_id` bigint NOT NULL COMMENT '车辆ID',
    `maintenance_type` varchar(20) NOT NULL COMMENT '维修类型:定期保养,故障维修,预防性维护',
    `maintenance_date` date NOT NULL COMMENT '维修日期',
    `mileage` decimal(10,2) DEFAULT NULL COMMENT '维修时里程',
    `description` text NOT NULL COMMENT '维修内容',
    `parts_replaced` text COMMENT '更换零件',
    `cost` decimal(10,2) DEFAULT '0.00' COMMENT '维修费用',
    `maintenance_company` varchar(100) DEFAULT NULL COMMENT '维修公司',
    `responsible_person` varchar(50) DEFAULT NULL COMMENT '负责人',
    `next_maintenance_date` date DEFAULT NULL COMMENT '下次维修日期',
    `next_maintenance_mileage` decimal(10,2) DEFAULT NULL COMMENT '下次维修里程',
    `status` tinyint DEFAULT 1 COMMENT '状态:1-计划中,2-进行中,3-已完成,4-已取消',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_id` (`vehicle_id`),
    KEY `idx_maintenance_date` (`maintenance_date`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='维修记录表';

-- ================================
-- 5. 运行统计相关表
-- ================================

-- 车辆运行记录表
CREATE TABLE IF NOT EXISTS `vehicle_operation` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `vehicle_id` bigint NOT NULL COMMENT '车辆ID',
    `task_id` bigint DEFAULT NULL COMMENT '关联任务ID',
    `operation_date` date NOT NULL COMMENT '运行日期',
    `start_time` time NOT NULL COMMENT '开始时间',
    `end_time` time DEFAULT NULL COMMENT '结束时间',
    `start_mileage` decimal(10,2) DEFAULT NULL COMMENT '开始里程',
    `end_mileage` decimal(10,2) DEFAULT NULL COMMENT '结束里程',
    `distance` decimal(8,2) DEFAULT '0.00' COMMENT '运行距离(km)',
    `fuel_consumption` decimal(5,2) DEFAULT '0.00' COMMENT '油耗(L)',
    `operation_type` varchar(20) DEFAULT NULL COMMENT '运行类型',
    `driver_name` varchar(50) DEFAULT NULL COMMENT '司机姓名',
    `start_location` varchar(200) DEFAULT NULL COMMENT '起点',
    `end_location` varchar(200) DEFAULT NULL COMMENT '终点',
    `remark` varchar(500) DEFAULT NULL COMMENT '备注',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_vehicle_id` (`vehicle_id`),
    KEY `idx_operation_date` (`operation_date`),
    KEY `idx_task_id` (`task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='车辆运行记录表';

-- ================================
-- 6. 系统配置相关表
-- ================================

-- 系统配置表
CREATE TABLE IF NOT EXISTS `system_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `config_key` varchar(100) NOT NULL COMMENT '配置键',
    `config_value` text NOT NULL COMMENT '配置值',
    `config_type` varchar(20) DEFAULT 'string' COMMENT '配置类型:string,number,boolean,json',
    `description` varchar(200) DEFAULT NULL COMMENT '描述',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
    `username` varchar(50) DEFAULT NULL COMMENT '操作用户名',
    `operation` varchar(100) NOT NULL COMMENT '操作描述',
    `method` varchar(200) DEFAULT NULL COMMENT '请求方法',
    `params` text COMMENT '请求参数',
    `ip` varchar(50) DEFAULT NULL COMMENT 'IP地址',
    `location` varchar(200) DEFAULT NULL COMMENT '操作地点',
    `user_agent` varchar(500) DEFAULT NULL COMMENT '用户代理',
    `status` tinyint DEFAULT 1 COMMENT '操作状态:0-失败,1-成功',
    `error_msg` text COMMENT '错误信息',
    `operation_time` datetime NOT NULL COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ================================
-- 插入初始数据
-- ================================

-- 插入默认角色
INSERT INTO `sys_role` (`role_name`, `role_code`, `description`) VALUES
('系统管理员', 'ADMIN', '系统管理员，拥有所有权限'),
('调度员', 'DISPATCHER', '调度员，负责车辆调度管理'),
('司机', 'DRIVER', '司机，负责车辆驾驶任务'),
('维修员', 'MAINTENANCE', '维修员，负责车辆维护管理'),
('监控员', 'MONITOR', '监控员，负责车辆监控'),
('查看者', 'VIEWER', '查看者，只能查看信息，不能编辑'),
('操作员', 'OPERATOR', '操作员，可以管理用户和车辆信息') AS new
ON DUPLICATE KEY UPDATE `role_name` = new.`role_name`;

-- 插入车辆类型
INSERT INTO `vehicle_type` (`type_name`, `type_code`, `description`, `max_speed`, `fuel_type`) VALUES
('行李车', 'LUGGAGE_CAR', '机场行李运输车辆', 30.00, '汽油'),
('摆渡车', 'SHUTTLE_BUS', '乘客摆渡车辆', 40.00, '柴油'),
('货运车', 'CARGO_TRUCK', '货物运输车辆', 50.00, '柴油'),
('清洁车', 'CLEANING_VEHICLE', '机场清洁车辆', 25.00, '电动'),
('维修车', 'MAINTENANCE_VEHICLE', '机场维修车辆', 35.00, '汽油'),
('巡逻车', 'PATROL_CAR', '机场巡逻车辆', 60.00, '汽油') AS new
ON DUPLICATE KEY UPDATE `type_name` = new.`type_name`;

-- 插入系统配置
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`) VALUES
('vehicle.location.update.interval', '30', 'number', '车辆位置更新间隔(秒)'),
('task.auto.assign.enabled', 'true', 'boolean', '是否启用自动任务分配'),
('system.maintenance.reminder.days', '7', 'number', '维护提醒提前天数'),
('map.provider', 'baidu', 'string', '地图服务提供商'),
('location.tracking.enabled', 'true', 'boolean', '是否启用位置追踪') AS new
ON DUPLICATE KEY UPDATE `config_value` = new.`config_value`;

-- 创建默认管理员用户 (密码: admin123)
INSERT INTO `sys_user` (`username`, `password`, `email`, `real_name`, `status`) VALUES
('admin', '$2a$10$Leq3vhN2WBx4hSVOrqReteVcfl1Pnav14OQN/cgL13TUxQiXqFw9O', 'admin@airport.com', '系统管理员', 1) AS new
ON DUPLICATE KEY UPDATE `password` = new.`password`;

-- 分配管理员角色
INSERT INTO `sys_user_role` (`user_id`, `role_id`) VALUES (1, 1) AS new
ON DUPLICATE KEY UPDATE `user_id` = new.`user_id`;

-- 插入权限数据
INSERT INTO `sys_permission` (`permission_name`, `permission_code`, `permission_type`, `description`, `status`) VALUES
('用户管理', 'user:view', 'menu', '查看用户列表', 1),
('创建用户', 'user:create', 'button', '创建新用户', 1),
('编辑用户', 'user:update', 'button', '编辑用户信息', 1),
('删除用户', 'user:delete', 'button', '删除用户', 1),
('车辆管理', 'vehicle:view', 'menu', '查看车辆列表', 1),
('创建车辆', 'vehicle:create', 'button', '创建新车辆', 1),
('编辑车辆', 'vehicle:update', 'button', '编辑车辆信息', 1),
('删除车辆', 'vehicle:delete', 'button', '删除车辆', 1),
('任务管理', 'task:view', 'menu', '查看任务列表', 1),
('创建任务', 'task:create', 'button', '创建新任务', 1),
('编辑任务', 'task:update', 'button', '编辑任务信息', 1),
('删除任务', 'task:delete', 'button', '删除任务', 1),
('分配任务', 'task:assign', 'button', '分配任务给车辆', 1),
('调度中心', 'dispatch:view', 'menu', '查看调度中心', 1),
('实时监控', 'monitoring:view', 'menu', '查看实时监控', 1),
('统计分析', 'statistics:view', 'menu', '查看统计分析', 1),
('告警管理', 'alert:view', 'menu', '查看告警信息', 1),
('系统设置', 'system:view', 'menu', '查看系统设置', 1),
('权限管理', 'permission:manage', 'button', '管理角色权限', 1) AS new
ON DUPLICATE KEY UPDATE `permission_name` = new.`permission_name`;

-- 为ADMIN角色分配所有权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 1, id FROM `sys_permission`
ON DUPLICATE KEY UPDATE `role_id` = 1;

-- 为DISPATCHER角色分配权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 2, id FROM `sys_permission` WHERE `permission_code` IN ('task:view', 'task:create', 'task:update', 'task:assign', 'dispatch:view', 'monitoring:view')
ON DUPLICATE KEY UPDATE `role_id` = 2;

-- 为OPERATOR角色分配权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 7, id FROM `sys_permission` WHERE `permission_code` IN ('task:view', 'task:create', 'task:update', 'vehicle:view', 'vehicle:create', 'vehicle:update', 'monitoring:view')
ON DUPLICATE KEY UPDATE `role_id` = 7;

-- 为MAINTENANCE角色分配权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 4, id FROM `sys_permission` WHERE `permission_code` IN ('alert:view', 'monitoring:view', 'vehicle:view')
ON DUPLICATE KEY UPDATE `role_id` = 4;

-- 为MONITOR角色分配权限
INSERT INTO `sys_role_permission` (`role_id`, `permission_id`)
SELECT 5, id FROM `sys_permission` WHERE `permission_code` IN ('monitoring:view', 'statistics:view')
ON DUPLICATE KEY UPDATE `role_id` = 5;

-- VIEWER角色不分配任何权限（只读）

-- 创建示例车辆
INSERT INTO `vehicle` (`vehicle_no`, `vehicle_type_id`, `brand`, `model`, `color`, `purchase_date`, `status`, `location_longitude`, `location_latitude`, `location_address`) VALUES
('京A12345', 1, '福田', 'BJ5040XYZ', '白色', '2023-01-15', 1, 116.4074, 39.9042, '首都机场T3航站楼'),
('京B67890', 2, '金龙', 'XMQ6127G', '蓝色', '2023-03-20', 1, 116.4134, 39.9109, '首都机场T2航站楼'),
('京C11111', 3, '解放', 'CA1165PK2L2T4E', '黄色', '2022-11-10', 1, 116.4200, 39.8968, '货运区'),
('京D22222', 4, '比亚迪', 'T4A', '绿色', '2024-01-08', 1, 116.4334, 39.9156, '清洁区') AS new
ON DUPLICATE KEY UPDATE `vehicle_no` = new.`vehicle_no`;

-- 创建示例调度任务
INSERT INTO `dispatch_task` (`task_no`, `task_name`, `task_type`, `priority`, `description`, `start_location`, `end_location`, `start_time`) VALUES
('TASK20251130001', 'T3航站楼行李运输', '常规调度', 2, '将行李从T3航站楼运至货运区', 'T3航站楼', '货运区', '2025-11-30 14:00:00'),
('TASK20251130002', '乘客摆渡任务', '常规调度', 3, '接送乘客从T2航站楼至登机口', 'T2航站楼', 'A12登机口', '2025-11-30 15:00:00'),
('TASK20251130003', '紧急货物运输', '紧急调度', 4, '紧急运输重要货物至货运区', 'T1航站楼', '货运区', '2025-11-30 16:00:00') AS new
ON DUPLICATE KEY UPDATE `task_name` = new.`task_name`;

-- ================================
-- 创建索引优化
-- ================================

-- 创建存储过程：安全创建索引（如果不存在）
DELIMITER $$

-- 先删除存储过程（如果存在），避免警告
DROP PROCEDURE IF EXISTS create_index_if_not_exists$$

CREATE PROCEDURE create_index_if_not_exists(
    IN p_table_name VARCHAR(128),
    IN p_index_name VARCHAR(128),
    IN p_index_columns VARCHAR(512)
)
BEGIN
    DECLARE index_exists INT DEFAULT 0;
    
    SELECT COUNT(*) INTO index_exists
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = p_table_name
      AND index_name = p_index_name;
    
    IF index_exists = 0 THEN
        SET @sql = CONCAT('CREATE INDEX ', p_index_name, ' ON ', p_table_name, ' (', p_index_columns, ')');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$

DELIMITER ;

-- 为经常查询的字段创建复合索引
CALL create_index_if_not_exists('vehicle', 'idx_vehicle_status_location', 'status, last_update_time');
CALL create_index_if_not_exists('dispatch_task', 'idx_task_status_start_time', 'status, start_time');
CALL create_index_if_not_exists('vehicle_location', 'idx_vehicle_location_time', 'vehicle_id, location_time');
CALL create_index_if_not_exists('vehicle_operation', 'idx_operation_vehicle_date', 'vehicle_id, operation_date');

-- 为日志表创建索引
CALL create_index_if_not_exists('operation_log', 'idx_operation_log_user_time', 'user_id, operation_time');
CALL create_index_if_not_exists('operation_log', 'idx_operation_log_operation_time', 'operation_time');

-- 删除临时存储过程
-- 注意：第一次执行时如果存储过程不存在会产生警告，这是正常的，不影响功能
-- 使用变量抑制警告（MySQL 5.7+）
SET @suppress_warning = 0;
DROP PROCEDURE IF EXISTS create_index_if_not_exists;

COMMIT;