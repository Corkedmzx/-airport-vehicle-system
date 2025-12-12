package com.airport.dto;

import lombok.Data;

/**
 * 调度任务统计数据
 * 
 * @author Corkedmzx
 */
@Data
public class TaskStatistics {
    
    /**
     * 任务总数
     */
    private Long totalCount;
    
    /**
     * 待分配任务数
     */
    private Long pendingCount;
    
    /**
     * 已分配任务数
     */
    private Long assignedCount;
    
    /**
     * 执行中任务数
     */
    private Long inProgressCount;
    
    /**
     * 已完成任务数
     */
    private Long completedCount;
    
    /**
     * 已取消任务数
     */
    private Long cancelledCount;
    
    /**
     * 异常任务数
     */
    private Long exceptionCount;
    
    /**
     * 今日任务数
     */
    private Long todayCount;
}