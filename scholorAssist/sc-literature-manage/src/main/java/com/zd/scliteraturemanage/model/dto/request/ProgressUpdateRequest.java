package com.zd.scliteraturemanage.model.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 进度更新请求DTO
 * 
 * <p>用于接收阅读进度更新请求</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "进度更新请求")
public class ProgressUpdateRequest {

    @NotNull(message = "阅读进度不能为空")
    @DecimalMin(value = "0.0", message = "阅读进度不能小于0")
    @DecimalMax(value = "1.0", message = "阅读进度不能大于1")
    @Schema(description = "阅读进度，范围0-1", example = "0.75", minimum = "0", maximum = "1")
    private BigDecimal progress;

    public BigDecimal getProgress() {
        return progress;
    }
}