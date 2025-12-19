package com.zd.sccommon.model; // 假设在公共模块中

import cn.hutool.http.HttpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "返回结果封装实体")
public class Result<T> {

    public static final String REQUEST_OK = "OK";

    @Schema(description = "业务状态码，200-成功，其它-失败", example = "200")
    private int code;

    @Schema(description = "响应消息", example = "OK")
    private String msg;

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "请求id", example = "1af123c11412e")
    private String requestId; // 此字段将由“组件”注入，而非构造函数

    // 构造函数变为私有，强制使用静态方法
    private Result(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        // 移除了 RequestUtils.getValueFromHeader()
    }

    // --- 静态工厂方法 ---

    public static <T> Result<T> ok(T data) {
        return new Result<>(HttpStatus.HTTP_OK, REQUEST_OK, data);
    }

    public static Result<Void> ok() {
        return new Result<>(HttpStatus.HTTP_OK, REQUEST_OK, null);
    }

    public static <T> Result<T> error(int code, String msg) {
        return new Result<>(code, msg, null);
    }

    public static <T> Result<T> error(String msg) {
        // 沿用您原版的设计，默认为 400
        return new Result<>(HttpStatus.HTTP_BAD_REQUEST, msg, null);
    }

    public boolean success() {
        return code == HttpStatus.HTTP_OK;
    }
}