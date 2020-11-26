package com.keyway.captcha.service;

import com.keyway.captcha.core.mode.SliderCaptcha;
import com.keyway.captcha.dto.ValidateParam;

public interface ISliderCaptchaService {
    SliderCaptcha generatorImgCaptcha(String fileType, Long ttl);

    Boolean validateImgCaptcha(ValidateParam param);
}
