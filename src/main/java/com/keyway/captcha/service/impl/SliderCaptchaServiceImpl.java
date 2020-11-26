package com.keyway.captcha.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.keyway.captcha.common.exception.CaptchaException;
import com.keyway.captcha.core.SliderCaptchaGenerator;
import com.keyway.captcha.core.SliderCaptchaValidator;
import com.keyway.captcha.core.mode.SliderCaptcha;
import com.keyway.captcha.dto.ValidateParam;
import com.keyway.captcha.service.ISliderCaptchaService;
import org.springframework.stereotype.Service;

/**
 * @author liuchunqing
 */
@Service
public class SliderCaptchaServiceImpl implements ISliderCaptchaService {
    @Override
    public SliderCaptcha generatorImgCaptcha(String fileType, Long ttl) {
        try {
            SliderCaptcha sliderCaptcha = SliderCaptchaGenerator.generateRandomValidationImg(fileType, ttl);
            return new SliderCaptcha() {{
                BeanUtil.copyProperties(sliderCaptcha, this, "offsetX", "offsetY");
            }};
        } catch (Exception e) {
            throw new CaptchaException("文件读取异常！");
        }
    }

    @Override
    public Boolean validateImgCaptcha(ValidateParam param) {
        return SliderCaptchaValidator.validate(param);
    }
}
