package com.example.pdac_assignment.Utils;

import android.graphics.Rect;
import android.graphics.YuvImage;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class Utils {

    /**
     * Helper function for Yuv -> RGB conversion
     * @param data
     * @param imageFormat
     * @param width
     * @param height
     * @return
     */
    public static byte[] convertYuvToJpeg(byte[] data, int imageFormat, int width, int height) {

        YuvImage image = new YuvImage(data, imageFormat, width, height, null);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int quality = 60;
        image.compressToJpeg(new Rect(0, 0, width, height), quality, baos);

        return baos.toByteArray();
    }
}
