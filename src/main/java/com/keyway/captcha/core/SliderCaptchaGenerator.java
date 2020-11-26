package com.keyway.captcha.core;

import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.StrUtil;
import com.keyway.captcha.core.cache.CaptchaCache;
import com.keyway.captcha.core.mode.SliderCaptcha;
import org.springframework.util.ResourceUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.NumberFormat;
import java.util.Random;

import static com.keyway.captcha.core.cache.CaptchaCache.ID_GENERATOR;

/**
 * 滑块验证工具类
 *
 * @author : keyway-lcq
 */
public class SliderCaptchaGenerator {
    /**
     * 源文件宽度
     */
    private static final int ORI_WIDTH = 320;
    /**
     * 源文件高度
     */
    private static final int ORI_HEIGHT = 160;
    /**
     * 抠图坐标x
     */
    private static int X;
    /**
     * 抠图坐标y
     */
    private static int Y;
    /**
     * 模板图宽度
     */
    private static int WIDTH;
    /**
     * 模板图高度
     */
    private static int HEIGHT;

    /**
     * 根据模板切图
     *
     * @param templateFile 模板文件
     * @param targetFile   目标文件
     * @param bottomFile
     * @param templateType 模板文件类型
     * @param targetType   目标文件类型
     * @return 切图map集合
     * @throws Exception 异常
     */
    private static SliderCaptcha pictureTemplatesCut(File templateFile, File targetFile, File bottomFile,
                                                     String templateType, String targetType) throws Exception {
        if (StrUtil.isEmpty(templateType) || StrUtil.isEmpty(targetType)) {
            throw new RuntimeException("file type is empty");
        }
        // 模板图
        BufferedImage imageTemplate = ImageIO.read(templateFile);
        // 源图
        BufferedImage oriImage = ImageIO.read(targetFile);
        WIDTH = imageTemplate.getWidth();
        HEIGHT = imageTemplate.getHeight();
        // 随机生成抠图坐标
        generateCutoutCoordinates();
        // 最终图像
        BufferedImage sliderImg = new BufferedImage(WIDTH, HEIGHT, imageTemplate.getType());
        Graphics2D graphics = sliderImg.createGraphics();
        graphics.setBackground(Color.white);

        int bold = 5;
        // 根据模板图片抠图
        sliderImg = dealCutPictureByTemplate(oriImage, imageTemplate, X, Y);
        sliderImg = mergeImg(ImgUtil.read(bottomFile), sliderImg, 0, 0, 1.0f);
        // 设置“抗锯齿”的属性
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        graphics.drawImage(sliderImg, 0, 0, null);
        graphics.dispose();

        //新建流,将图片转换成二进制数组
        ByteArrayOutputStream sliderImgOs = new ByteArrayOutputStream();
        //利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
        ImageIO.write(sliderImg, targetType, sliderImgOs);
        byte[] sliderLayerImgBytes = sliderImgOs.toByteArray();
        //源图添加遮罩同时降低亮度
        BufferedImage bottomLayerImg = dealOriPictureByTemplate(imgColorBrightness(oriImage, -30), imageTemplate, X, Y);
        //新建流,将图片转换成二进制数组
        ByteArrayOutputStream bottomLayerOs = new ByteArrayOutputStream();
        //利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
        ImageIO.write(bottomLayerImg, targetType, bottomLayerOs);
        byte[] bottomLayerImgBytes = bottomLayerOs.toByteArray();
        System.out.println("X=" + X + ";y=" + Y);
        return SliderCaptcha.builder()
                .sliderLayerImg("data:image/png;base64," + Base64Encoder.encode(sliderLayerImgBytes))
                .bottomLayerImg("data:image/png;base64," + Base64Encoder.encode(bottomLayerImgBytes))
                .originWidth(ORI_WIDTH)
                .originHeight(ORI_HEIGHT)
                .blockWidth(WIDTH)
                .blockHeight(HEIGHT)
                .offsetX(X)
                .offsetY(Y)
                .id(ID_GENERATOR.nextId())
                .build();
    }

    /**
     * 抠图后原图生成
     *
     * @param oriImage      原始图片
     * @param templateImage 模板图片
     * @param x             坐标X
     * @param y             坐标Y
     * @return 添加遮罩层后的原始图片
     * @throws Exception 异常
     */
    private static BufferedImage dealOriPictureByTemplate(BufferedImage oriImage, BufferedImage templateImage, int x,
                                                          int y) throws Exception {
        // 源文件备份图像矩阵 支持alpha通道的rgb图像
        BufferedImage oriCopyImage = new BufferedImage(oriImage.getWidth(), oriImage.getHeight(),
                BufferedImage.TYPE_4BYTE_ABGR);
        //copy 源图做不透明处理
        for (int i = 0; i < oriImage.getWidth(); i++) {
            for (int j = 0; j < oriImage.getHeight(); j++) {
                int rgb = oriImage.getRGB(i, j);
                int r = (0xff & rgb);
                int g = (0xff & (rgb >> 8));
                int b = (0xff & (rgb >> 16));
                //无透明处理
                rgb = r + (g << 8) + (b << 16) + (255 << 24);
                oriCopyImage.setRGB(i, j, rgb);
            }
        }
        //透明化扣掉的部分
        for (int i = 0; i < templateImage.getWidth(); i++) {
            for (int j = 0; j < templateImage.getHeight(); j++) {
                int rgb = templateImage.getRGB(i, j);
                //对源文件备份图像(x+i,y+j)坐标点进行透明处理
                if (rgb != 16777215 && rgb <= 0) {
                    int rgb_ori = oriCopyImage.getRGB(x + i, y + j);
                    int r = (0xff & rgb_ori);
                    int g = (0xff & (rgb_ori >> 8));
                    int b = (0xff & (rgb_ori >> 16));
                    //（150<<24）控制透明度
                    rgb_ori = r + (g << 8) + (b << 16) + (50 << 24);
                    oriCopyImage.setRGB(x + i, y + j, rgb_ori);
                }
            }
        }
        return oriCopyImage;
    }

    /**
     * @param oriImage      根据模块抠取滑块图案
     * @param templateImage
     * @param x
     * @param y
     * @return
     * @throws Exception
     */
    private static BufferedImage dealCutPictureByTemplate(BufferedImage oriImage, BufferedImage templateImage,
                                                          int x, int y) throws Exception {
        BufferedImage targetImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_4BYTE_ABGR);
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                int tempRgb = templateImage.getRGB(i, j);
                if (tempRgb != 16777215 && tempRgb <= 0) {
                    int rgb = oriImage.getRGB(i + x, j + y);
                    targetImage.setRGB(i, j, rgb);
                }
            }
        }
        return targetImage;
    }


    /**
     * 随机生成抠图坐标
     */
    private static void generateCutoutCoordinates() {
        Random random = new Random();
        // ORI_WIDTH：590  ORI_HEIGHT：360
        // WIDTH：93 HEIGHT：360
        int widthDifference = ORI_WIDTH - WIDTH;
        int heightDifference = ORI_HEIGHT - HEIGHT;

        if (widthDifference <= 0) {
            X = 5;
        } else {
            X = random.nextInt(ORI_WIDTH - 3 * WIDTH) + 2 * WIDTH + 5;
        }
        //fixme 目前均heightDifference==0，后续可扩展二维滑动
        if (heightDifference <= 0) {
            Y = 0;
            //Y = 5;
        } else {
            Y = random.nextInt(ORI_HEIGHT - HEIGHT) + 5;
        }

        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
    }

    /**
     * 图片色阶调整，调整rgb的分量
     */
    private static BufferedImage imgColorBrightness(BufferedImage imgsrc, int brightness) {
        try {
            //创建一个不带透明度的图片
            BufferedImage back = new BufferedImage(imgsrc.getWidth(), imgsrc.getHeight(), BufferedImage.TYPE_INT_RGB);
            int width = imgsrc.getWidth();
            int height = imgsrc.getHeight();
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    int pixel = imgsrc.getRGB(j, i);
                    Color color = new Color(pixel);
                    int red = color.getRed() + brightness;
                    if (red > 255) {
                        red = 255;
                    }
                    if (red < 0) {
                        red = 0;
                    }
                    int green = color.getGreen() + brightness;
                    if (green > 255) {
                        green = 255;
                    }
                    if (green < 0) {
                        green = 0;
                    }
                    int blue = color.getBlue() + brightness;
                    if (blue > 255) {
                        blue = 255;
                    }
                    if (blue < 0) {
                        blue = 0;
                    }
                    color = new Color(red, green, blue);
                    int x = color.getRGB();
                    back.setRGB(j, i, x);
                }
            }
            return back;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param origin    原始图片（下层）
     * @param waterMark 叠加图片（上层）
     * @param x         x坐标修正值。 默认在中间，偏移量相对于中间偏移
     * @param y         y坐标修正值。 默认在中间，偏移量相对于中间偏移
     * @param alpha     透明度
     */
    private static BufferedImage mergeImg(BufferedImage origin, BufferedImage waterMark, int x, int y, float alpha) {
        Image img = ImgUtil.pressImage(
                origin,
                waterMark,
                x,
                y,
                alpha
        );
        return ImgUtil.toBufferedImage(img);
    }

    /**
     * 获取随机验证码（入口方法）
     */
    public static SliderCaptcha generateRandomValidationImg(String targetType, Long timeOutMs) throws Exception {
        Integer templateNum = new Random().nextInt(10) + 1;
        Integer targetNum = new Random().nextInt(20) + 21;
        System.out.println("模版文件编号：" + templateNum + "，底图文件编号：" + targetNum);
        File templateFile =
                ResourceUtils.getFile(System.getProperty("user.dir") + "/view/static/validate/template/" + templateNum + "a.png");
        File sourceFile =
                ResourceUtils.getFile(System.getProperty("user.dir") + "/view/static/validate/source/" + targetNum +
                        ".png");
        File bottomFile =
                ResourceUtils.getFile(System.getProperty("user.dir") + "/view/static/validate/template/" + templateNum + "b.png");
        SliderCaptcha sliderCaptcha = SliderCaptchaGenerator.pictureTemplatesCut(templateFile, sourceFile, bottomFile,
                "png", targetType);
        CaptchaCache.add(sliderCaptcha, timeOutMs);
        return sliderCaptcha;
    }

}


