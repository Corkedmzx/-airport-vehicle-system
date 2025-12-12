package com.airport.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 
 * @author MiniMax Agent
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(
            MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("参数验证失败: {}", ex.getMessage());
        
        Map<String, Object> result = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        result.put("code", 400);
        result.put("message", "参数验证失败");
        result.put("errors", errors);
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理数据绑定异常
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, Object>> handleBindException(
            BindException ex, WebRequest request) {
        log.warn("数据绑定失败: {}", ex.getMessage());
        
        Map<String, Object> result = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        result.put("code", 400);
        result.put("message", "数据绑定失败");
        result.put("errors", errors);
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.badRequest().body(result);
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        log.warn("业务异常: {}", ex.getMessage());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", ex.getCode());
        result.put("message", ex.getMessage());
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {
        log.error("运行时异常", ex);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", "系统内部错误");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    /**
     * 处理一般异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(
            Exception ex, WebRequest request) {
        log.error("系统异常", ex);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", "系统异常");
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
}