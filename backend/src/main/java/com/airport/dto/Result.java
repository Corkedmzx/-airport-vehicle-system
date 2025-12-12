package com.airport.dto;

import lombok.Data;

/**
 * 统一响应结果
 * 
 * @author MiniMax Agent
 */
@Data
public class Result<T> {

    /**
     * 响应码
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    /**
     * 成功响应
     */
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功");
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    /**
     * 失败响应
     */
    public static <T> Result<T> error() {
        return new Result<>(500, "操作失败");
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(500, message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }

    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }

    /**
     * 业务错误响应
     */
    public static <T> Result<T> businessError(String message) {
        return new Result<>(400, message);
    }

    public static <T> Result<T> unauthorized(String message) {
        return new Result<>(401, message);
    }

    public static <T> Result<T> forbidden(String message) {
        return new Result<>(403, message);
    }

    public static <T> Result<T> notFound(String message) {
        return new Result<>(404, message);
    }
}