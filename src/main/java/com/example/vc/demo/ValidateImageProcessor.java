package com.example.vc.demo;

import cn.hutool.core.codec.Base64Encoder;
import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.util.ResourceUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

/**
 * 滑块验证工具类
 *
 * @author : keyway-lcq
 */
public class ValidateImageProcessor {
    public static Long RESTORE_EXPIRE = 60000L;
    /**
     * 源文件宽度
     */
    //private static final int ORI_WIDTH = 590;
    private static final int ORI_WIDTH = 320;
    /**
     * 源文件高度
     */
    private static final int ORI_HEIGHT = 160;
    //private static final int ORI_HEIGHT = 360;
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

    public static int getX() {
        return X;
    }

    public static int getY() {
        return Y;
    }

    /**
     * 根据模板切图（入口方法）
     *
     * @param templateFile 模板文件
     * @param targetFile   目标文件
     * @param bottomFile
     * @param templateType 模板文件类型
     * @param targetType   目标文件类型
     * @return 切图map集合
     * @throws Exception 异常
     */
    public static Map<String, Object> pictureTemplatesCut(File templateFile, File targetFile, File bottomFile, String templateType, String targetType) throws Exception {
        Map<String, Object> pictureMap = new HashMap<>(8);
        if (StrUtil.isEmpty(templateType) || StrUtil.isEmpty(targetType)) {
            throw new RuntimeException("file type is empty");
        }
        InputStream targetIs = new FileInputStream(targetFile);
        // 模板图
        BufferedImage imageTemplate = ImageIO.read(templateFile);
        WIDTH = imageTemplate.getWidth();
        HEIGHT = imageTemplate.getHeight();
        // 随机生成抠图坐标
        generateCutoutCoordinates();
        // 最终图像
        BufferedImage newImage = new BufferedImage(WIDTH, HEIGHT, imageTemplate.getType());
        Graphics2D graphics = newImage.createGraphics();
        graphics.setBackground(Color.white);

        int bold = 5;
        // 获取感兴趣的目标区域
        BufferedImage targetImageNoDeal = getTargetArea(X, Y, WIDTH, HEIGHT, targetIs, targetType);

        // 根据模板图片抠图

        newImage = dealCutPictureByTemplate(targetImageNoDeal, imageTemplate, newImage);
        newImage = mergeImg(ImgUtil.read(bottomFile), newImage, 0, 0, 1.0f);
        // 设置“抗锯齿”的属性
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(bold, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL));
        graphics.drawImage(newImage, 0, 0, null);
        graphics.dispose();

        //新建流,将图片转换成二进制数组
        ByteArrayOutputStream os1 = new ByteArrayOutputStream();
        //利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
        ImageIO.write(newImage, "png", os1);
        byte[] newImages = os1.toByteArray();
        pictureMap.put("newImage", "data:image/png;base64," + Base64Encoder.encode(newImages));
        //源图生成遮罩
        BufferedImage oriImage = ImageIO.read(targetFile);
        //添加遮罩同时降低亮度
        BufferedImage oriCopyImage = dealOriPictureByTemplate(imgColorBrightness(oriImage, -30), imageTemplate, X, Y);
        //新建流,将图片转换成二进制数组
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        //利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流。
        ImageIO.write(oriCopyImage, "png", os2);
        byte[] oriCopyImages = os2.toByteArray();
        pictureMap.put("oriCopyImage", "data:image/png;base64," + Base64Encoder.encode(oriCopyImages));
        pictureMap.put("originWidth", ORI_WIDTH);
        pictureMap.put("originHeight", ORI_HEIGHT);
        pictureMap.put("blockWidth", WIDTH);
        pictureMap.put("blockHeight", HEIGHT);
        System.out.println("X=" + X + ";y=" + Y);
        return pictureMap;
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
        BufferedImage oriCopyImage = new BufferedImage(oriImage.getWidth(), oriImage.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        // 源文件图像矩阵
        int[][] oriImageData = getData(oriImage);
        // 模板图像矩阵
        int[][] templateImageData = getData(templateImage);

        //copy 源图做不透明处理
        for (int i = 0; i < oriImageData.length; i++) {
            for (int j = 0; j < oriImageData[0].length; j++) {
                int rgb = oriImage.getRGB(i, j);
                int r = (0xff & rgb);
                int g = (0xff & (rgb >> 8));
                int b = (0xff & (rgb >> 16));
                //无透明处理
                rgb = r + (g << 8) + (b << 16) + (255 << 24);
                oriCopyImage.setRGB(i, j, rgb);
            }
        }
        //蒙层
        for (int i = 0; i < templateImageData.length; i++) {
            for (int j = 0; j < templateImageData[0].length - 5; j++) {
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
                } else {
                    //do nothing
                }
            }
        }
        //新建流
        //ByteArrayOutputStream os = new ByteArrayOutputStream();
        //利用ImageIO类提供的write方法，将bi以png图片的数据模式写入流
        //ImageIO.write(oriCopyImage, "png", os);
        //从流中获取数据数组
        return oriCopyImage;
    }


    /**
     * 根据模板图片抠图
     *
     * @param oriImage      原始图片
     * @param templateImage 模板图片
     * @return 扣了图片之后的原始图片
     */
    private static BufferedImage dealCutPictureByTemplate(BufferedImage oriImage, BufferedImage templateImage, BufferedImage targetImage) throws Exception {
        // 源文件图像矩阵
        int[][] oriImageData = getData(imgColorBrightness(oriImage, 30));
        // 模板图像矩阵
        int[][] templateImageData = getData(templateImage);
        // 模板图像宽度

        for (int i = 0; i < templateImageData.length; i++) {
            // 模板图片高度
            for (int j = 0; j < templateImageData[0].length; j++) {
                // 如果模板图像当前像素点不是白色 copy源文件信息到目标图片中
                int rgb = templateImageData[i][j];
                /*rgb==-16731905*//*rgb == -16506510*/
                if (rgb != 16777215 && rgb <= 0) {
                    targetImage.setRGB(i, j, oriImageData[i][j]);
                }
            }
        }
        return targetImage;
    }

    /**
     * 获取目标区域
     *
     * @param x            随机切图坐标x轴位置
     * @param y            随机切图坐标y轴位置
     * @param targetWidth  切图后目标宽度
     * @param targetHeight 切图后目标高度
     * @param ois          源文件输入流
     * @return 返回目标区域
     * @throws Exception 异常
     */
    private static BufferedImage getTargetArea(int x, int y, int targetWidth, int targetHeight, InputStream ois,
                                               String fileType) throws Exception {
        Iterator<ImageReader> imageReaderList = ImageIO.getImageReadersByFormatName(fileType);
        ImageReader imageReader = imageReaderList.next();
        // 获取图片流
        ImageInputStream iis = ImageIO.createImageInputStream(ois);
        // 输入源中的图像将只按顺序读取
        imageReader.setInput(iis, true);

        ImageReadParam param = imageReader.getDefaultReadParam();
        Rectangle rec = new Rectangle(x, y, targetWidth, targetHeight);
        param.setSourceRegion(rec);
        return imageReader.read(0, param);
    }

    /**
     * 生成图像矩阵
     *
     * @param bufferedImage 图片流
     * @return 图像矩阵
     */
    private static int[][] getData(BufferedImage bufferedImage) {
        int[][] data = new int[bufferedImage.getWidth()][bufferedImage.getHeight()];
        for (int i = 0; i < bufferedImage.getWidth(); i++) {
            for (int j = 0; j < bufferedImage.getHeight(); j++) {
                data[i][j] = bufferedImage.getRGB(i, j);
            }
        }
        return data;
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
     * 创建滑块验证图
     *
     * @return
     * @throws Exception
     */
    public static Map<String, Object> createSlipperImage() throws Exception {
        Integer templateNum = new Random().nextInt(10) + 1;
        Integer targetNum = new Random().nextInt(20) + 21;
        System.out.println("模版文件编号：" + templateNum + "，底图文件编号：" + targetNum);
        File templateFile = ResourceUtils.getFile(System.getProperty("user.dir") + "/view/static/validate/template/" + templateNum + "a.png");
        File targetFile = ResourceUtils.getFile(System.getProperty("user.dir") + "/view/static/validate/source/" + targetNum + ".png");
        File bottomFile = ResourceUtils.getFile(System.getProperty("user.dir") + "/view/static/validate/template/" + templateNum + "b.png");

        return ValidateImageProcessor.pictureTemplatesCut(templateFile, targetFile, bottomFile,
                "png", "png");
    }

    /**
     * 图片色阶调整，调整rgb的分量
     */
    public static BufferedImage imgColorBrightness(BufferedImage imgsrc, int brightness) {
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
     * 图形验证
     *
     * @param request
     */
    public static void validate(HttpServletRequest request) {

        String validateCode = request.getParameter("validateCode");
        if (StrUtil.isEmpty(validateCode)) {
            throw new BusinessException("验证码为空！");
        }
        String username = request.getParameter("username");
        String companyName = request.getParameter("companyName");
        String validateKey = request.getParameter("validateKey");
        //根据上述参数取出存储的坐标 todo 根据自己项目定义取值方式
        String restoreVc = "";
        if (StrUtil.isEmpty(restoreVc)) {
            throw new BusinessException("验证码已过期!");
        }
        //自动化测试跳过验证
        /*if (168 == (Integer.parseInt(validateCode)) && "autotest".equals(username) && "四川科瑞".equals(companyName)) {
            return;
        }*/
        //计算偏移量，67是混淆偏移，增强安全性，需要前端相应加偏移（加密）
        int valueDifference = Integer.parseInt(restoreVc) - Integer.parseInt(validateCode) /*- 67*/;
        //误差自己调整
        if (!(-10 <= valueDifference && valueDifference <= 10)) {
            throw new BusinessException("验证失败！");
        }
    }

    /**
     * @param origin    原始图片（下层）
     * @param waterMark 叠加图片（上层）
     * @param x         x坐标修正值。 默认在中间，偏移量相对于中间偏移
     * @param y         y坐标修正值。 默认在中间，偏移量相对于中间偏移
     * @param alpha     透明度
     */
    public static BufferedImage mergeImg(BufferedImage origin, BufferedImage waterMark, int x, int y, float alpha) {
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
     * 获取随机验证码
     */
    public static Map<String, Object> randomVc() throws Exception {
        Integer templateNum = new Random().nextInt(10) + 1;
        Integer targetNum = new Random().nextInt(20) + 21;
        System.out.println("模版文件编号：" + templateNum + "，底图文件编号：" + targetNum);
        File templateFile = ResourceUtils.getFile(System.getProperty("user.dir") + "/view/static/validate/template/" + templateNum + "a.png");
        File targetFile = ResourceUtils.getFile(System.getProperty("user.dir") + "/view/static/validate/source/" + targetNum + ".png");
        File bottomFile = ResourceUtils.getFile(System.getProperty("user.dir") + "/view/static/validate/template/" + templateNum + "b.png");
        return ValidateImageProcessor.pictureTemplatesCut(templateFile, targetFile, bottomFile,
                "png", "png");
    }


    public static void main(String[] args) throws Exception {
        Object newImage = randomVc().get("newImage");
        System.out.println(JSONUtil.toJsonStr(newImage));
    }

}


