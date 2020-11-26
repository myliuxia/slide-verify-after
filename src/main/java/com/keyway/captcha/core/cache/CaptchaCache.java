package com.keyway.captcha.core.cache;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.keyway.captcha.core.mode.SliderCaptcha;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class CaptchaCache {
    public static Snowflake ID_GENERATOR = IdUtil.getSnowflake(1, 1);
    private static Map<Long, CacheObj> map = new ConcurrentSkipListMap<>();
    public static volatile Boolean CLEAN_THREAD_IS_RUN = false;

    public static void add(SliderCaptcha sliderCaptcha, Long expire) {
        Long ttlTime = null;
        if (expire <= 0L) {
            if (expire == -1L) {
                ttlTime = -1L;
            } else {
                return;
            }
        }
        SliderCaptcha captcha = new SliderCaptcha() {{
            BeanUtil.copyProperties(sliderCaptcha, this, "bottomLayerImg", "sliderLayerImg");
        }};
        if (ttlTime == null) {
            ttlTime = System.currentTimeMillis() + expire;
        }
        CacheObj cacheObj = CacheObj.builder().sliderCaptcha(sliderCaptcha).expired(ttlTime).build();
        map.put(sliderCaptcha.getId(), cacheObj);
    }

    public static void add(SliderCaptcha sliderCaptcha) {
        add(sliderCaptcha, -1L);
    }

    public static SliderCaptcha get(Long id) {
        startCacheClean();
        CacheObj cacheObj = map.get(id);
        if (cacheObj == null) {
            return null;
        }
        if (cacheObj.getExpired() == -1L) {
            return cacheObj.getSliderCaptcha();
        }
        if (System.currentTimeMillis() > cacheObj.getExpired()) {
            del(id);
        }
        return cacheObj.getSliderCaptcha();
    }

    public static void del(Long id) {
        map.remove(id);
    }

    public static void deleteExpired() {
        for (Map.Entry<Long, CacheObj> entry : map.entrySet()) {
            Long expired = entry.getValue().getExpired();
            if (System.currentTimeMillis() > expired && expired != -1L) {
                map.remove(entry.getKey());
            }
        }
    }

    private static void startCacheClean() {
        if (!CaptchaCache.CLEAN_THREAD_IS_RUN) {
            CleanExpiredThread cleanExpiredThread = new CleanExpiredThread();
            Thread thread = new Thread(cleanExpiredThread);
            //设置为后台守护线程
            thread.setDaemon(true);
            thread.start();
        }
    }

    @Data
    @Builder
    static class CacheObj {
        private SliderCaptcha sliderCaptcha;
        private Long expired;
    }


    /**
     * 每一分钟清理一次过期缓存
     */
    static class CleanExpiredThread implements Runnable {

        @Override
        public void run() {
            CaptchaCache.CLEAN_THREAD_IS_RUN = Boolean.TRUE;
            while (true) {
                System.out.println("clean thread run ");
                CaptchaCache.deleteExpired();
                try {
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}


