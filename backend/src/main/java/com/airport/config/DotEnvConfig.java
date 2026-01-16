package com.airport.config;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;

/**
 * DotEnv配置类
 * 在Spring Boot启动时自动加载 .env 文件并设置环境变量
 * 
 * @author Corkedmzx
 */
@Slf4j
@Configuration
public class DotEnvConfig {

    /**
     * 在Spring Boot启动时自动加载 .env 文件
     */
    @PostConstruct
    public void loadDotEnv() {
        try {
            // 获取项目根目录（backend目录）
            String projectRoot = System.getProperty("user.dir");
            String envFilePath = Paths.get(projectRoot, ".env").toString();
            
            File envFile = new File(envFilePath);
            
            if (!envFile.exists()) {
                log.warn(".env 文件不存在，跳过加载。路径: {}", envFilePath);
                log.warn("提示：如果使用环境变量配置，请确保已设置所有必需的环境变量");
                return;
            }

            log.info("正在加载 .env 文件: {}", envFilePath);

            // 加载 .env 文件
            Dotenv dotenv = Dotenv.configure()
                    .directory(projectRoot)
                    .filename(".env")
                    .ignoreIfMissing()
                    .load();

            // 将 .env 文件中的变量设置到系统属性中
            // Spring Boot 会从系统属性中读取 ${VAR} 占位符
            // 由于 dotenv-java 3.0.0 没有 keys() 方法，我们直接读取文件内容
            int loadedCount = loadEnvFileToSystemProperties(envFilePath);

            log.info("已从 .env 文件加载 {} 个环境变量", loadedCount);
            
            // 显示已加载的关键配置（隐藏敏感信息）
            logLoadedConfig(dotenv);

        } catch (Exception e) {
            log.error("加载 .env 文件失败", e);
            log.warn("将继续使用系统环境变量或默认配置");
        }
    }

    /**
     * 读取 .env 文件并设置到系统属性
     * 
     * @param envFilePath .env 文件路径
     * @return 加载的变量数量
     */
    private int loadEnvFileToSystemProperties(String envFilePath) {
        int loadedCount = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(envFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                
                // 跳过空行和注释
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // 解析 KEY=VALUE 格式
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    
                    // 移除引号（如果存在）
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    } else if (value.startsWith("'") && value.endsWith("'")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    
                    // 如果系统属性或环境变量已存在，不覆盖（优先级更高）
                    // 但为了确保配置生效，我们仍然设置系统属性（系统属性优先级低于环境变量）
                    if (!key.isEmpty() && !value.isEmpty()) {
                        String existingEnv = System.getenv(key);
                        String existingProp = System.getProperty(key);
                        
                        // 如果环境变量已存在，跳过（环境变量优先级最高）
                        if (existingEnv != null) {
                            log.trace("跳过已存在的环境变量: {} (已有值: {})", key, "***");
                            continue;
                        }
                        
                        // 设置系统属性（即使系统属性已存在，也更新以确保使用最新值）
                        System.setProperty(key, value);
                        loadedCount++;
                        log.trace("设置系统属性: {} = {}", key, 
                                key.toLowerCase().contains("password") || key.toLowerCase().contains("secret") 
                                        ? "***" : value);
                    }
                }
            }
        } catch (Exception e) {
            log.error("读取 .env 文件失败", e);
        }
        return loadedCount;
    }

    /**
     * 记录已加载的配置信息（隐藏敏感信息）
     */
    private void logLoadedConfig(Dotenv dotenv) {
        log.debug("=== 已加载的配置 ===");
        
        // 数据库配置
        if (dotenv.get("DB_HOST") != null) {
            log.debug("  数据库: {}:{}", dotenv.get("DB_HOST"), dotenv.get("DB_PORT", "3306"));
            log.debug("  数据库名: {}", dotenv.get("DB_NAME"));
            log.debug("  用户名: {}", dotenv.get("DB_USERNAME"));
            log.debug("  密码: {}", dotenv.get("DB_PASSWORD") != null ? "***" : "未设置");
        }

        // MQTT配置
        if (dotenv.get("HUAWEI_IOT_MQTT_ENABLED") != null) {
            log.debug("  MQTT: {}", dotenv.get("HUAWEI_IOT_MQTT_ENABLED"));
            if ("true".equalsIgnoreCase(dotenv.get("HUAWEI_IOT_MQTT_ENABLED"))) {
                log.debug("  设备ID: {}", dotenv.get("HUAWEI_IOT_MQTT_DEVICE_ID"));
                log.debug("  设备密钥: {}", dotenv.get("HUAWEI_IOT_MQTT_DEVICE_SECRET") != null ? "***" : "未设置");
            }
        }

        // 百度地图配置
        if (dotenv.get("BAIDU_MAP_AK") != null) {
            String ak = dotenv.get("BAIDU_MAP_AK");
            log.debug("  百度地图AK: {}...", ak.length() > 8 ? ak.substring(0, 8) + "***" : "***");
        } else {
            log.debug("  百度地图AK: 未设置");
        }

        // JWT配置
        if (dotenv.get("JWT_SECRET") != null) {
            log.debug("  JWT密钥: ***");
        }
    }
}
