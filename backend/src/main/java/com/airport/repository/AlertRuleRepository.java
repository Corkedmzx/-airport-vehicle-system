package com.airport.repository;

import com.airport.entity.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 告警规则数据访问层
 * 
 * @author Corkedmzx
 */
@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /**
     * 查找启用的规则
     */
    List<AlertRule> findByEnabledTrue();

    /**
     * 根据规则类型查找
     */
    List<AlertRule> findByRuleType(String ruleType);

    /**
     * 根据规则类型和启用状态查找
     */
    List<AlertRule> findByRuleTypeAndEnabledTrue(String ruleType);
}

