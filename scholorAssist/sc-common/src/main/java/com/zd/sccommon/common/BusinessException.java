package com.zd.sccommon.common;

import lombok.Getter;

/**
 * 自定义业务异常
 * 使用 HttpStatus 状态码
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code; // 存储 HttpStatus 状态码

    /**
     * 唯一的构造函数
     * @param code 传入 cn.hutool.http.HttpStatus 中的常量，例如 HttpStatus.HTTP_NOT_FOUND
     * @param message 异常信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    // (删除了那个错误的、以 HttpStatus 为参数的构造函数)
}