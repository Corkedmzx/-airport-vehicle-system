package com.airport;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 机场车辆监控与调度系统 - 主应用程序
 * 
 * 系统会自动从 .env 文件加载环境变量，无需手动设置
 * 只需在 backend 目录下创建 .env 文件，配置数据库和MQTT连接信息
 * 
 * @author Corkedmzx
 * @version 1.0.0
 */
@SpringBootApplication
@EnableJpaRepositories
@EnableJpaAuditing
@EnableTransactionManagement
@EnableCaching
@EnableAsync
@EnableScheduling
public class AirportVehicleSystemApplication {

    public static void main(String[] args) {
        // 在Spring Boot启动前加载 .env 文件
        loadDotEnvFile();
        
        SpringApplication.run(AirportVehicleSystemApplication.class, args);
        String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        System.out.println("""
                
                ========================================
                   机场车辆监控与调度系统启动成功！
                ========================================
                
                🌟 系统特色：
                • Java 17 + SpringBoot 3.2
                • MySQL 8.0 + Redis 缓存
                • JWT 认证 + Spring Security
                • WebSocket 实时监控
                • RESTful API 设计
                • 自动加载 .env 配置
                
                📊 管理端点：
                • 健康检查: http://localhost:8080/api/actuator/health
                • API文档: http://localhost:8080/api/doc.html
                • 监控面板: http://localhost:8080/api/druid/
                
                🕒 启动时间: {} 

                """.formatted(currentTime));
    }

    /**
     * 在Spring Boot启动前加载 .env 文件
     * 确保环境变量在配置读取前就已设置
     */
    private static void loadDotEnvFile() {
        try {
            // 获取项目根目录（backend目录）
            String projectRoot = System.getProperty("user.dir");
            String envFilePath = Paths.get(projectRoot, ".env").toString();
            
            File envFile = new File(envFilePath);
            
            if (!envFile.exists()) {
                System.out.println("⚠️  警告: .env 文件不存在，跳过加载。路径: " + envFilePath);
                System.out.println("   提示: 如果使用环境变量配置，请确保已设置所有必需的环境变量");
                return;
            }

            System.out.println("📄 正在加载 .env 文件: " + envFilePath);

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

            System.out.println("已从 .env 文件加载 " + loadedCount + " 个环境变量");
            
        } catch (Exception e) {
            System.err.println("加载 .env 文件失败: " + e.getMessage());
            System.out.println("   将继续使用系统环境变量或默认配置");
        }
    }

    /**
     * 读取 .env 文件并设置到系统属性
     * 
     * @param envFilePath .env 文件路径
     * @return 加载的变量数量
     */
    private static int loadEnvFileToSystemProperties(String envFilePath) {
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
                    // 但为了确保配置生效，我们仍然设置系统属性
                    if (!key.isEmpty() && !value.isEmpty()) {
                        String existingEnv = System.getenv(key);
                        
                        // 如果环境变量已存在，跳过（环境变量优先级最高）
                        if (existingEnv != null) {
                            continue;
                        }
                        
                        // 设置系统属性
                        System.setProperty(key, value);
                        loadedCount++;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("读取 .env 文件失败: " + e.getMessage());
        }
        return loadedCount;
    }
}