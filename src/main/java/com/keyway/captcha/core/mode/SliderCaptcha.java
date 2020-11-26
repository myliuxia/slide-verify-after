package com.keyway.captcha.core.mode;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 滑动验证
 *
 * @author liuchunqing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SliderCaptcha {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String bottomLayerImg;
    private Integer originWidth;
    private Integer originHeight;
    private Integer blockWidth;
    private Integer blockHeight;
    private String sliderLayerImg;
    private Integer offsetX;
    private Integer offsetY;
}
