package com.example.pdac_assignment.Utils;

import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class Utils {

    //Code sample taken from here: https://stackoverflow.com/questions/26060679/yuv420p-and-yv12-or-yv12-to-rgb888-conversion
    /**
    // data - input byte array in NV21 format
    // width - image width
    // height - image height
    // pixels - if null function will return null. If incorrect size new array allocated.
    // amountsOfRGBs - must be of size 3
    **/

    public static int[] convertYUV420_NV21toARGB8888(byte [] data, int width, int height, int[] pixels, float [] amountsOfRGB){
        int size = width*height;
        int offset = size;
        if(pixels != null && pixels.length != size)
            pixels = new int[size];
        int[] tmpPixels = new int[4];

        if(amountsOfRGB != null)
            Arrays.fill(amountsOfRGB, -1);

        int u, v, y1, y2, y3, y4;

// i along Y and the final pixels
// k along pixels U and V
        for(int i=0, k=0; i < size; i+=2, k+=2) {
            y1 = data[i  ]&0xff;
            y2 = data[i+1]&0xff;
            y3 = data[width+i  ]&0xff;
            y4 = data[width+i+1]&0xff;

            v = data[offset+k  ]&0xff;
            u = data[offset+k+1] & 0xff;
            v = v-128;
            u = u-128;

            tmpPixels[0] = convertYUVtoARGB(y1, u, v, amountsOfRGB);
            tmpPixels[1] = convertYUVtoARGB(y2, u, v, amountsOfRGB);
            tmpPixels[2] = convertYUVtoARGB(y3, u, v, amountsOfRGB);
            tmpPixels[3] = convertYUVtoARGB(y4, u, v, amountsOfRGB);

            if(pixels != null) {
                pixels[i] = tmpPixels[0];
                pixels[i + 1] = tmpPixels[1];
                pixels[width + i] = tmpPixels[2];
                pixels[width + i + 1] = tmpPixels[3];
            }

            if (i!=0 && (i+2)%width==0)
                i += width;
        }

        return pixels;
    }

    private static int convertYUVtoARGB(int y, int u, int v, float[] amountsOfRGB) {
        int []rgb = new int[3];
        rgb[0] = y + (int)(1.772f*v);
        rgb[1] = y - (int)(0.344f*v + 0.714f*u);
        rgb[2] = y + (int)(1.402f*u);

        rgb[0] = rgb[0]>255? 255 : Math.max(rgb[0], 0);
        rgb[1] = rgb[1]>255? 255 : Math.max(rgb[1], 0);
        rgb[2] = rgb[2]>255? 255 : Math.max(rgb[2], 0);
        if(amountsOfRGB != null){
            for (int i = 0; i < rgb.length && i < amountsOfRGB.length; i++)
                amountsOfRGB[i] = amountsOfRGB[i] < 0 ? rgb[i]/255F : (amountsOfRGB[i] + rgb[i]/255F)/2F;
        }

        return 0xff000000 | (rgb[0]<<16) | (rgb[1]<<8) | rgb[2];
    }

    @SuppressWarnings("deprecation")
    public static byte[] convertYuvToJpeg(byte[] data, int imageFormat, int width, int height) {

        YuvImage image = new YuvImage(data, imageFormat, width, height, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 60;
        image.compressToJpeg(new Rect(0, 0, width, height), quality, baos);

        return baos.toByteArray();
    }
}
