package com.moon.util.image;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Author : moon
 * Date  : 2019/2/21 16:18
 * Description : Class for 利用Graphics2D绘制图片
 */
public class DrawPicUtil {


    //BufferedImage
    private static BufferedImage parse2D(String file) throws IOException {
        File pic = new File(file);
        return ImageIO.read(pic);
    }

    /**
     * x,y为图片左上角坐标
     *
     * @param file1
     * @param file2
     * @return
     * @throws IOException
     */
    public static void drawImage(String file1, String file2, String file3) throws IOException {
        BufferedImage backgroudImg = parse2D(file1);
        int backgroudImgHeight = backgroudImg.getHeight();
        System.out.println("backgroudImgHeight="+backgroudImgHeight);
        int backgroudImgWidth = backgroudImg.getWidth();
        System.out.println("backgroudImgWidth="+backgroudImgWidth);
        Graphics2D imgGraphics = backgroudImg.createGraphics();
        BufferedImage frontImg = parse2D(file2);
        //获取宽高,居中显示
        int height = frontImg.getHeight();
        int width = frontImg.getWidth();
        int x = (backgroudImgWidth - width) / 2;
        System.out.println("x="+x);
        int y = (backgroudImgHeight - height) / 2;
        System.out.println("y="+y);
        //居中显示
        imgGraphics.drawImage(frontImg, x, y, null, null);
        imgGraphics.dispose();
        File file = new File(file3);
        ImageIO.write(backgroudImg, "png", file);
    }

   public static void main(String[] agrs) {
        String file1 = "H:\\test\\44.jpg";
        String file2 = "H:\\test\\55.jpg";
        String file3 = "H:\\test\\66.png";
       try {
           drawImage(file1,file2,file3);
       } catch (IOException e) {
           e.printStackTrace();
       }
   }

}
