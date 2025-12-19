// sc-common/src/main/java/com/zd/sccommon/advice/GlobalResponseAdvice.java
package com.zd.sccommon.advice;

import com.zd.sccommon.model.Result;
import jakarta.servlet.http.HttpServletRequest; // 2. 导入
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest; // 3. 导入
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.RequestContextHolder; // 4. 导入
import org.springframework.web.context.request.ServletRequestAttributes; // 5. 导入
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.zd")
@Order(2) // 确保在异常处理组件之后执行
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalResponseAdvice implements ResponseBodyAdvice<Object> {
    /**
     * 内部服务调用的统一路径前缀
     */
    private static final String INTERNAL_API_PREFIX = "/inner/";

    /**
     * 判断此组件是否要执行
     * @param returnType 返回类型
     * @param converterType 转换器类型
     * @return true = 执行包装, false = 跳过包装
     */
    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {

        // 1. 检查是否已经是 Result 类型，如果是，则不需要再包装
        // (这种情况通常是 GlobalExceptionHandler 已经处理过的异常)
        if (returnType.getParameterType().isAssignableFrom(Result.class)) {
            // 虽然已经是 Result，但我们仍需进入 beforeBodyWrite 为其注入 requestId
            // 因此这里返回 true
            return true;
        }
//        检查是否是ai模块的聊天chat接口，此接口不封装
        if (returnType.getMethod().getName().equals("chat")) {
            return false; // chat接口不包装
        }

        // 2. 检查是否是内部接口
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                if (request.getRequestURI().contains(INTERNAL_API_PREFIX)) {
                    // 是内部接口，并且返回的不是Result (例如返回一个 UserResDTO)
                    // 我们不应该包装它，Feign客户端期望的是裸DTO
                    return false;
                }
            }
        } catch (Exception e) {
            // 无法获取请求属性（例如非Web上下文），按默认（外部）逻辑处理，执行包装
        }

        // 3. 是外部接口，且不是Result，进行包装
        return true;
    }

    /**
     * 在响应体写入前的最后时机执行
     * @param body Controller 或 异常处理器 返回的对象
     * @param request 当前请求
     * @return 最终写回给前端的对象
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                  MediaType selectedContentType, Class converterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {

        // 1. 从请求头中获取 requestId
        String requestId = request.getHeaders().getFirst("X-Trace-Id");

        Result<?> result;

        // 2. 判断 body 是什么？
        if (body instanceof Result) {
            //情况A: body 已经是 Result 类型 (说明是 GlobalExceptionHandler 处理过的异常)
            result = (Result<?>) body;
        } else {
            //情况B: body 是 Controller 正常返回的 DTO/VO (说明是成功请求)
            // (supports 已经帮我们排除了内部调用, 所以这里一定是外部调用)
            result = Result.ok(body);
        }

        // 3. (核心) 注入 requestId 到最终的 Result 对象中
        // 无论成功失败，都在这里统一注入
        //result.setRequestId(requestId);

        return result;
    }
}