package com.keyway.captcha.controller;

import com.keyway.captcha.common.http.ResponseBuilder;
import com.keyway.captcha.core.mode.SliderCaptcha;
import com.keyway.captcha.dto.ValidateParam;
import com.keyway.captcha.service.ISliderCaptchaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("slider")
public class SliderCaptchaController {
    @Autowired
    private ISliderCaptchaService sliderCaptchaService;

    @GetMapping("/randomCaptcha")
    public ResponseEntity randomCaptcha() {
        SliderCaptcha sliderCaptcha = sliderCaptchaService.generatorImgCaptcha("png", 30 * 1000L);
        return ResponseBuilder.data(sliderCaptcha).build();
    }

    @PostMapping("/validateCaptcha")
    public ResponseEntity randomCaptcha(@RequestBody ValidateParam param) {
        Boolean result = sliderCaptchaService.validateImgCaptcha(param);
        return ResponseBuilder.data(result).build();
    }
}
