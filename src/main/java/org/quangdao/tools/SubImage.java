package org.quangdao.tools;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;

@Getter
@Log4j2
public class SubImage {
    private final short[] datas;
    private final int width;
    private final int height;
    public SubImage(BufferedImage image, Rectangle rect) {
        width = rect.width;
        height = rect.height;
        int[] datas = image.getRGB(rect.x, rect.y, rect.width, rect.height, null, 0, rect.width);
        final ColorModel colorModel = image.getColorModel();
        datas = Arrays.stream(datas).parallel().map(i-> {
            int r = colorModel.getRed(i);
            int g = colorModel.getGreen(i);
            int b = colorModel.getBlue(i);
           return (short)((b>>2)<<10) + ((g>>3)<<5) + (r>>3);
        }).toArray();
        this.datas = new short[datas.length];
        for(int i = 0;i<datas.length;++i) {
            this.datas[i] = (short) datas[i];
        }
    }

    public Point findIn(SubImage other, Rectangle rrect) {
        Rectangle rect = rrect.intersection(new Rectangle(0,0,other.width-width,other.height-height));


        float mx = 0;
        for(int i=rect.x;i<rect.x+rect.width;++i)
            for(int j=rect.y;j<rect.y+rect.height;++j) {
                int jj=0;
                for(;jj<height;++jj)
                    if(!Arrays.equals(
                            this.datas, jj*width, (jj+1)*width,
                            other.datas, (j+jj)*other.width+i, (j+jj)*other.width+i+width))
                        break;
                if(jj==height) return new Point(i,j);
            }
        return null;
    }

}
