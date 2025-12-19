package com.zd.scliteraturemanage.model.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 进度更新响应DTO
 * 
 * <p>用于返回阅读进度更新结果</p>
 * 
 * @author System
 * @since 2024-01-21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "进度更新响应")
public class ProgressUpdateResponse {

    @Schema(description = "阅读进度", example = "0.75", minimum = "0", maximum = "1")
    private BigDecimal progress;

    public static ProgressUpdateResponseBuilder builder() {
        return new ProgressUpdateResponseBuilder();
    }

    public static class ProgressUpdateResponseBuilder {
        private BigDecimal progress;

        public ProgressUpdateResponseBuilder progress(BigDecimal progress) {
            this.progress = progress;
            return this;
        }

        public ProgressUpdateResponse build() {
            ProgressUpdateResponse response = new ProgressUpdateResponse();
            response.progress = this.progress;
            return response;
        }
    }
}