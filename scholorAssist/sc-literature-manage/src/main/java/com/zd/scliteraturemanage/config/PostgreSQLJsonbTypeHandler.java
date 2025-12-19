package com.zd.scliteraturemanage.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * PostgreSQL JSONB类型处理器
 * 
 * <p>专门处理Java对象与PostgreSQL jsonb类型之间的转换</p>
 * <p>解决MyBatis默认TypeHandler无法正确处理PostgreSQL jsonb类型的问题</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@MappedTypes({String[].class, Object.class})
@MappedJdbcTypes({JdbcType.OTHER})
public class PostgreSQLJsonbTypeHandler extends BaseTypeHandler<Object> {

    private static final Logger log = LoggerFactory.getLogger(PostgreSQLJsonbTypeHandler.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 设置参数到PreparedStatement
     * 
     * @param ps PreparedStatement对象
     * @param i 参数索引
     * @param parameter 参数值
     * @param jdbcType JDBC类型
     * @throws SQLException SQL异常
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, Object parameter, JdbcType jdbcType) throws SQLException {
        try {
            // 将Java对象转换为JSON字符串
            String jsonString = OBJECT_MAPPER.writeValueAsString(parameter);
            
            // 使用PostgreSQL的CAST语法将字符串转换为jsonb
            // 这样避免了直接依赖PostgreSQL驱动的特定类
            ps.setObject(i, jsonString, java.sql.Types.OTHER);
            
            log.debug("设置JSONB参数，索引: {}, 值: {}", i, jsonString);
        } catch (JsonProcessingException e) {
            log.error("转换对象为JSON失败，参数: {}", parameter, e);
            throw new SQLException("转换对象为JSON失败", e);
        }
    }

    /**
     * 从ResultSet获取可空结果
     * 
     * @param rs ResultSet对象
     * @param columnName 列名
     * @return 解析后的对象
     * @throws SQLException SQL异常
     */
    @Override
    public Object getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String jsonString = rs.getString(columnName);
        return parseJsonString(jsonString);
    }

    /**
     * 从ResultSet获取可空结果（按索引）
     * 
     * @param rs ResultSet对象
     * @param columnIndex 列索引
     * @return 解析后的对象
     * @throws SQLException SQL异常
     */
    @Override
    public Object getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String jsonString = rs.getString(columnIndex);
        return parseJsonString(jsonString);
    }

    /**
     * 从CallableStatement获取可空结果
     * 
     * @param cs CallableStatement对象
     * @param columnIndex 列索引
     * @return 解析后的对象
     * @throws SQLException SQL异常
     */
    @Override
    public Object getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String jsonString = cs.getString(columnIndex);
        return parseJsonString(jsonString);
    }

    /**
     * 解析JSON字符串为Java对象
     * 
     * @param jsonString JSON字符串
     * @return 解析后的对象
     */
    private Object parseJsonString(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // 尝试解析为字符串数组（tags字段的情况）
            if (jsonString.startsWith("[") && jsonString.endsWith("]")) {
                return OBJECT_MAPPER.readValue(jsonString, String[].class);
            }
            
            // 其他情况返回原始JSON字符串
            return jsonString;
            
        } catch (JsonProcessingException e) {
            log.warn("解析JSON字符串失败，返回原始字符串，JSON: {}", jsonString, e);
            return jsonString;
        }
    }
}