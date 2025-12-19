// sc-common/src/main/java/com/zd/sccommon/advice/GlobalExceptionHandler.java
package com.zd.sccommon.advice;

import cn.hutool.http.HttpStatus;
import com.zd.sccommon.common.BusinessException;
import com.zd.sccommon.model.Result;
import jakarta.servlet.http.HttpServletRequest; // 1. 导入
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder; // 2. 导入
import org.springframework.web.context.request.ServletRequestAttributes; // 3. 导入
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@Slf4j
@RestControllerAdvice(basePackages = "com.zd")
@Order(1)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandler {

    /**
     * 内部服务调用的统一路径前缀
     * (请确保您的内部Controller都映射在此路径下, 例如 /api/users/inner/...)
     */
    private static final String INTERNAL_API_PREFIX = "/inner/";

    /**
     * 检查当前请求是否为内部服务调用 (Feign 调用)
     * @return true 如果是内部调用, false 否则
     */
    private boolean isInnerRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 检查URI是否包含内部调用标记
                return request.getRequestURI().contains(INTERNAL_API_PREFIX);
            }
        } catch (Exception e) {
            // 无法获取请求（例如非Web上下文），默认为非内部请求
            log.warn("无法获取HttpServletRequest, 默认为外部请求");
        }
        // 默认为外部请求
        return false;
    }

    /**
     * 处理我们自定义的业务异常
     * @param e 业务异常
     * @return Result 错误封装
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        // 1. 检查是否为内部调用
        if (isInnerRequest()) {
            log.warn("内部服务业务异常 (将重新抛出): code={}, msg={}", e.getCode(), e.getMessage());
            // 2. 重新抛出, 让 Feign ErrorDecoder 捕获
            throw e;
        }

        // 3. 否则, 认为是外部调用, 包装后返回
        log.warn("外部服务业务异常: code={}, msg={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理异步请求在客户端主动中断时的异常（如浏览器插件拦截、用户取消）
     * 该场景不应视为服务器错误，统一返回一个语义化的提示码
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public Result<Void> handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        // 客户端中断连接, 无论是内部还是外部, 都不需要重新抛出, 仅记录
        log.warn("客户端中断连接: {}", e.getMessage());
        // 使用 499 (Client Closed Request) 语义化标识；前端不展示错误提示
        return Result.error(499, "客户端已中断请求");
    }

    /**
     * 处理所有未被捕获的系统异常
     * @param e 异常
     * @return Result 错误封装
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleOtherException(Exception e) {
        // 1. 检查是否为内部调用
        if (isInnerRequest()) {
            log.error("内部服务未知异常 (将重新抛出): ", e);
            // 2. 重新抛出, 让 Feign ErrorDecoder 捕获
            // 包装成 RuntimeException 以确保 Spring 能正确处理
            throw new RuntimeException("内部服务发生未知异常: " + e.getMessage(), e);
        }

        // 3. 否则, 认为是外部调用, 包装后返回
        log.error("系统发生未知异常: ", e);
        // 统一返回 500 错误
        return Result.error(HttpStatus.HTTP_INTERNAL_ERROR, "系统异常，请稍后再试");
    }
}