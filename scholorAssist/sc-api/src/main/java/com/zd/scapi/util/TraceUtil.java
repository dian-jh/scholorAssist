package com.zd.scapi.util;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.util.UUID;

/**
 * 链路追踪工具类
 * 
 * <p>提供分布式链路追踪功能，用于跟踪请求在微服务间的调用链路。</p>
 * 
 * <p>主要功能：</p>
 * <ul>
 *   <li>生成唯一的追踪ID</li>
 *   <li>管理MDC上下文</li>
 *   <li>传递追踪信息</li>
 *   <li>清理追踪上下文</li>
 * </ul>
 * 
 * @author system
 * @since 1.0.0
 */
@Slf4j
public class TraceUtil {

    /** 追踪ID的MDC键名 */
    public static final String TRACE_ID_KEY = "traceId";
    
    /** 跨度ID的MDC键名 */
    public static final String SPAN_ID_KEY = "spanId";
    
    /** 父跨度ID的MDC键名 */
    public static final String PARENT_SPAN_ID_KEY = "parentSpanId";
    
    /** 服务名称的MDC键名 */
    public static final String SERVICE_NAME_KEY = "serviceName";

    /** HTTP头中的追踪ID键名 */
    public static final String TRACE_ID_HEADER = "X-Trace-Id";
    
    /** HTTP头中的跨度ID键名 */
    public static final String SPAN_ID_HEADER = "X-Span-Id";
    
    /** HTTP头中的父跨度ID键名 */
    public static final String PARENT_SPAN_ID_HEADER = "X-Parent-Span-Id";

    /**
     * 生成新的追踪ID
     * 
     * @return 追踪ID
     */
    public static String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成新的跨度ID
     * 
     * @return 跨度ID
     */
    public static String generateSpanId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 获取当前追踪ID
     * 
     * @return 追踪ID，如果不存在则返回null
     */
    public static String getCurrentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    /**
     * 获取当前跨度ID
     * 
     * @return 跨度ID，如果不存在则返回null
     */
    public static String getCurrentSpanId() {
        return MDC.get(SPAN_ID_KEY);
    }

    /**
     * 获取当前父跨度ID
     * 
     * @return 父跨度ID，如果不存在则返回null
     */
    public static String getCurrentParentSpanId() {
        return MDC.get(PARENT_SPAN_ID_KEY);
    }

    /**
     * 获取当前服务名称
     * 
     * @return 服务名称，如果不存在则返回null
     */
    public static String getCurrentServiceName() {
        return MDC.get(SERVICE_NAME_KEY);
    }

    /**
     * 设置追踪ID
     * 
     * @param traceId 追踪ID
     */
    public static void setTraceId(String traceId) {
        if (traceId != null && !traceId.trim().isEmpty()) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }

    /**
     * 设置跨度ID
     * 
     * @param spanId 跨度ID
     */
    public static void setSpanId(String spanId) {
        if (spanId != null && !spanId.trim().isEmpty()) {
            MDC.put(SPAN_ID_KEY, spanId);
        }
    }

    /**
     * 设置父跨度ID
     * 
     * @param parentSpanId 父跨度ID
     */
    public static void setParentSpanId(String parentSpanId) {
        if (parentSpanId != null && !parentSpanId.trim().isEmpty()) {
            MDC.put(PARENT_SPAN_ID_KEY, parentSpanId);
        }
    }

    /**
     * 设置服务名称
     * 
     * @param serviceName 服务名称
     */
    public static void setServiceName(String serviceName) {
        if (serviceName != null && !serviceName.trim().isEmpty()) {
            MDC.put(SERVICE_NAME_KEY, serviceName);
        }
    }

    /**
     * 初始化新的追踪上下文
     * 
     * @param serviceName 服务名称
     * @return 追踪ID
     */
    public static String initTrace(String serviceName) {
        String traceId = generateTraceId();
        String spanId = generateSpanId();
        
        setTraceId(traceId);
        setSpanId(spanId);
        setServiceName(serviceName);
        
        log.debug("初始化追踪上下文 - 追踪ID: {}, 跨度ID: {}, 服务: {}", traceId, spanId, serviceName);
        return traceId;
    }

    /**
     * 继承追踪上下文（用于微服务间调用）
     * 
     * @param traceId 追踪ID
     * @param parentSpanId 父跨度ID
     * @param serviceName 当前服务名称
     * @return 新的跨度ID
     */
    public static String inheritTrace(String traceId, String parentSpanId, String serviceName) {
        String spanId = generateSpanId();
        
        setTraceId(traceId);
        setSpanId(spanId);
        setParentSpanId(parentSpanId);
        setServiceName(serviceName);
        
        log.debug("继承追踪上下文 - 追踪ID: {}, 跨度ID: {}, 父跨度ID: {}, 服务: {}", 
                 traceId, spanId, parentSpanId, serviceName);
        return spanId;
    }

    /**
     * 创建子跨度
     * 
     * @param operation 操作名称
     * @return 子跨度ID
     */
    public static String createChildSpan(String operation) {
        String currentSpanId = getCurrentSpanId();
        String childSpanId = generateSpanId();
        
        setParentSpanId(currentSpanId);
        setSpanId(childSpanId);
        
        log.debug("创建子跨度 - 操作: {}, 子跨度ID: {}, 父跨度ID: {}", operation, childSpanId, currentSpanId);
        return childSpanId;
    }

    /**
     * 清理追踪上下文
     */
    public static void clearTrace() {
        MDC.remove(TRACE_ID_KEY);
        MDC.remove(SPAN_ID_KEY);
        MDC.remove(PARENT_SPAN_ID_KEY);
        MDC.remove(SERVICE_NAME_KEY);
        log.debug("清理追踪上下文");
    }

    /**
     * 清理所有MDC上下文
     */
    public static void clearAll() {
        MDC.clear();
        log.debug("清理所有MDC上下文");
    }

    /**
     * 获取追踪信息摘要
     * 
     * @return 追踪信息摘要
     */
    public static String getTraceSummary() {
        String traceId = getCurrentTraceId();
        String spanId = getCurrentSpanId();
        String parentSpanId = getCurrentParentSpanId();
        String serviceName = getCurrentServiceName();
        
        return String.format("Trace[traceId=%s, spanId=%s, parentSpanId=%s, service=%s]", 
                           traceId, spanId, parentSpanId, serviceName);
    }

    /**
     * 检查是否存在追踪上下文
     * 
     * @return 是否存在追踪上下文
     */
    public static boolean hasTrace() {
        return getCurrentTraceId() != null;
    }

    /**
     * 执行带追踪的操作
     * 
     * @param serviceName 服务名称
     * @param operation 操作名称
     * @param runnable 要执行的操作
     */
    public static void executeWithTrace(String serviceName, String operation, Runnable runnable) {
        String originalTraceId = getCurrentTraceId();
        String originalSpanId = getCurrentSpanId();
        String originalParentSpanId = getCurrentParentSpanId();
        String originalServiceName = getCurrentServiceName();
        
        try {
            if (originalTraceId == null) {
                // 初始化新的追踪上下文
                initTrace(serviceName);
            } else {
                // 创建子跨度
                createChildSpan(operation);
            }
            
            log.info("开始执行操作 - 服务: {}, 操作: {}, 追踪信息: {}", 
                    serviceName, operation, getTraceSummary());
            
            runnable.run();
            
            log.info("完成执行操作 - 服务: {}, 操作: {}", serviceName, operation);
            
        } catch (Exception e) {
            log.error("执行操作异常 - 服务: {}, 操作: {}, 异常: {}", serviceName, operation, e.getMessage(), e);
            throw e;
        } finally {
            // 恢复原始上下文
            if (originalTraceId != null) {
                setTraceId(originalTraceId);
                setSpanId(originalSpanId);
                setParentSpanId(originalParentSpanId);
                setServiceName(originalServiceName);
            } else {
                clearTrace();
            }
        }
    }
}