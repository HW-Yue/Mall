package com.yue.opsagent.adapter.persistence.approval;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.yue.opsagent.domain.model.AlertEvent;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MyBatis TypeHandler：{@code alert_json} 列 ↔ {@link AlertEvent} 双向映射。
 *
 * <p>MyBatis 在加载 Mapper XML 时通过无参构造实例化 TypeHandler，所以 {@link ObjectMapper}
 * 在这里内部维护，不走 Spring 注入。注册了 JavaTimeModule 以处理 {@link java.time.OffsetDateTime}。</p>
 */
@MappedTypes(AlertEvent.class)
public class AlertEventJsonTypeHandler extends BaseTypeHandler<AlertEvent> {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, AlertEvent parameter, JdbcType jdbcType)
            throws SQLException {
        try {
            ps.setString(i, MAPPER.writeValueAsString(parameter));
        } catch (Exception e) {
            throw new SQLException("AlertEvent 序列化失败: " + e.getMessage(), e);
        }
    }

    @Override
    public AlertEvent getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public AlertEvent getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public AlertEvent getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private AlertEvent parse(String json) throws SQLException {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, AlertEvent.class);
        } catch (Exception e) {
            throw new SQLException("AlertEvent 反序列化失败: " + e.getMessage(), e);
        }
    }
}
