package com.cjq.pojo.common;

import lombok.Data;

@Data
public class Result<T> {

    private Integer code;    // 状态码：200=成功，其他=失败
    private String message;  // 提示信息
    private T data;          // 具体业务数据（泛型）

    // ========== 私有构造方法（不允许直接 new，必须通过静态方法） ==========
    private Result() {}

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ========== 成功响应（静态工厂方法） ==========

    // 1. 只返回成功状态（无业务数据）
    public static <T> Result<T> success() {
        return new Result<>(200, "操作成功", null);
    }

    // 2. 返回成功状态 + 业务数据（最常用）
    public static <T> Result<T> success(T data) {
        return new Result<>(200, "操作成功", data);
    }

    // 3. 返回成功状态 + 自定义提示消息 + 业务数据
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    // ========== 失败响应（静态工厂方法） ==========

    // 4. 失败：只传错误信息（code 默认 500）
    public static <T> Result<T> error(String message) {
        return new Result<>(500, message, null);
    }

    // 5. 失败：自定义状态码 + 错误信息（给 BusinessException 用）
    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    // 6. 失败：自定义状态码 + 错误信息 + 额外数据（极少数情况）
    public static <T> Result<T> error(Integer code, String message, T data) {
        return new Result<>(code, message, data);
    }
}