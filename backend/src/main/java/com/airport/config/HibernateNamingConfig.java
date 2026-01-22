package com.airport.config;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

import java.util.Locale;

/**
 * Hibernate命名策略配置
 * 组合策略：先转换为下划线命名，然后为保留关键字添加反引号
 * 
 * @author Corkedmzx
 */
public class HibernateNamingConfig extends PhysicalNamingStrategyStandardImpl {

    // MySQL保留关键字列表
    private static final String[] MYSQL_RESERVED_KEYWORDS = {
        "read", "order", "group", "select", "table", "key", "index", "user"
    };

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
        // 先使用标准策略转换为下划线命名
        Identifier identifier = super.toPhysicalColumnName(name, context);
        
        // 如果是保留关键字，添加反引号
        String text = identifier.getText();
        if (isReservedKeyword(text)) {
            return Identifier.quote(identifier);
        }
        
        return identifier;
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
        // 先使用标准策略转换为下划线命名
        Identifier identifier = super.toPhysicalTableName(name, context);
        
        // 如果是保留关键字，添加反引号
        String text = identifier.getText();
        if (isReservedKeyword(text)) {
            return Identifier.quote(identifier);
        }
        
        return identifier;
    }

    /**
     * 检查是否是MySQL保留关键字
     */
    private boolean isReservedKeyword(String name) {
        if (name == null) {
            return false;
        }
        String lowerName = name.toLowerCase(Locale.ROOT);
        for (String keyword : MYSQL_RESERVED_KEYWORDS) {
            if (keyword.equals(lowerName)) {
                return true;
            }
        }
        return false;
    }
}
