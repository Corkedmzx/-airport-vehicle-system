package com.airport.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统配置实体类
 * 
 * @author Corkedmzx
 */
@Data
@Entity
@Table(name = "system_config")
@EqualsAndHashCode(callSuper = true)
public class SystemConfigEntity extends BaseEntity {

    /**
     * 配置键
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /**
     * 配置值
     */
    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    /**
     * 配置类型:string,number,boolean,json
     */
    @Column(name = "config_type", length = 20)
    private String configType = "string";

    /**
     * 描述
     */
    @Column(name = "description", length = 200)
    private String description;
}

