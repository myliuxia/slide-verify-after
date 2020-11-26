package com.keyway.captcha.core;

import com.keyway.captcha.common.exception.CaptchaException;
import com.keyway.captcha.core.cache.CaptchaCache;
import com.keyway.captcha.core.mode.SliderCaptcha;
import com.keyway.captcha.dto.ValidateParam;


/**
 * 验证器
 *
 * @author liuchunqing
 */
public class SliderCaptchaValidator {
    /**
     * 图形验证
     *
     * @param validateParam
     */
    public static Boolean validate(ValidateParam validateParam) {

        if (validateParam.getId() == null) {
            throw new CaptchaException("验证码为空！");
        }
        SliderCaptcha sliderCaptcha = CaptchaCache.get(validateParam.getId());
        if (sliderCaptcha == null) {
            throw new CaptchaException("验证码已过期!");
        }
        //计算偏移量
        int deltaX = validateParam.getOffsetX() - sliderCaptcha.getOffsetX();
        int deltaY = validateParam.getOffsetX() - sliderCaptcha.getOffsetX();
        //误差自己调整
        if (!(Math.abs(deltaX) <= 10 && Math.abs(deltaY) <= 10)) {
            return Boolean.FALSE;
        }
        CaptchaCache.del(validateParam.getId());
        return Boolean.TRUE;
    }
}
