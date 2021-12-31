package com.example.pdac_assignment.Utils;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;

public class Histogram {

    public static class Config{
        public int max_height;
        public int max_width;

        public Config(){
            this(HISTOGRAM_DEFAULT_MAX_HEIGHT, HISTOGRAM_DEFAULT_MAX_WIDTH);
        }
        public Config(int max_height, int max_width) {
            this.max_height = max_height;
            this.max_width = max_width;
        }
    }
    public static class Color{
        public final int color;
        private int mCount = 1;


        public Color(int color) {
            this.color = color;
        }
        private void incrementCount(){
            mCount+=1;
        }
        public int getCount(){
            return mCount;
        }

        @NonNull
        @Override
        public String toString() {
            return "R:" + Integer.toString((color & 0x00ff0000) >> 16)
                    +" B:" + Integer.toString((color & 0x0000ff00) >> 8)
                    +" C:" + Integer.toString((color & 0x000000ff));
        }

        private static final int COLOR_DIFFERENCE_LIMIT = 2;
        @Override
        public boolean equals(@Nullable Object obj) {
            if(obj instanceof Color) {
                int color = ((Color) obj).color;
                return Math.abs((color & 0x00ff0000) - (this.color & 0x00ff0000) >> 16) < COLOR_DIFFERENCE_LIMIT
                        && Math.abs((color & 0x0000ff00) - (this.color & 0x0000ff00) >> 8) < COLOR_DIFFERENCE_LIMIT
                            && Math.abs(color & 0x000000ff) - (this.color & 0x000000ff) < COLOR_DIFFERENCE_LIMIT;

            }
            return false;
        }
    }
    private Histogram(){
    }

    private Histogram(Color[] colors, int itemsCount){
        mPaletteColors = colors;
        mItemsCount = itemsCount;
    }
    private Color[] mPaletteColors;
    private boolean mSorted = false;
    private int mItemsCount = 0;
    private static int HISTOGRAM_DEFAULT_MAX_HEIGHT = 64;
    private static int HISTOGRAM_DEFAULT_MAX_WIDTH = 64;


    public Color[] getSortedColors(){
        if(!mSorted){
            Arrays.sort(mPaletteColors, new Comparator<Color>() {
                @Override
                public int compare(Color color, Color color2) {
                    return color2.mCount - color.mCount;
                }
            });
            mSorted = true;
        }
        return mPaletteColors;
    }

    public int getItemCount() {
        return mItemsCount;
    }

    public static Histogram instantiateHistogram(byte[] bytes, int offset, int length){
       return instantiateHistogram(bytes, offset, length,null);
    }
    public static Histogram instantiateHistogram(byte[] bytes, int offset, int length, Config config) throws IllegalArgumentException{
        if(config == null)
            config = new Config();
        Bitmap bitmap = null;
        try {
            bitmap = prepareHistogramBitmap(bytes,offset, length, config);
        }catch (Exception e){
            throw new IllegalArgumentException("Unable to decode bytes", e);
        }
        Histogram histogram = new Histogram(prepareColorsFromHistogramBitmap(bitmap),bitmap.getHeight() * bitmap.getWidth());
        bitmap.recycle();
        return histogram;
    }
    public static Histogram instantiateHistogram(InputStream is){
       return instantiateHistogram(is,null);
    }
    public static Histogram instantiateHistogram(InputStream is, Config config) throws IllegalArgumentException{
        if(config == null)
            config = new Config();
        Bitmap bitmap = null;
        try {
            bitmap = prepareHistogramBitmap(is,config);
        }catch (Exception e){
            throw new IllegalArgumentException("Unable to decode stream", e);
        }
        Histogram histogram = new Histogram(prepareColorsFromHistogramBitmap(bitmap),bitmap.getHeight() * bitmap.getWidth());
        bitmap.recycle();
        return histogram;
    }
    private static Color[] prepareColorsFromHistogramBitmap(@NonNull Bitmap bitmap){
        HashMap<Integer,Color> map = new HashMap<>();
        for(int i = 0; i < bitmap.getWidth(); i++)
            for(int j = 0; j < bitmap.getHeight(); j++) {
                int pixel = bitmap.getPixel(i,j);
                if (map.containsKey(pixel))
                    map.get(pixel).incrementCount();
                else
                    map.put(pixel,new Color(pixel));
            }
       return map.values().toArray(new Color[map.size()]);
    }

    private static Bitmap prepareHistogramBitmap(byte[] bytes, int offset, int length, Config config) throws IllegalStateException{
        BitmapFactory.Options options = prepareOptionsForSampling();
        BitmapFactory.decodeByteArray(bytes,offset,length,options);
        updateOptionsForDecoding(options,config);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,offset,length,options);
        if(bitmap == null)
            throw new IllegalStateException("Unable to decode bitmap from array");
        return bitmap;

    }
    private static Bitmap prepareHistogramBitmap(InputStream is, Config config) throws IOException {
        BitmapFactory.Options options = prepareOptionsForSampling();
        BitmapFactory.decodeStream(is,null,options);
        is.reset();
        updateOptionsForDecoding(options,config);
        Bitmap bitmap =  BitmapFactory.decodeStream(is,null,options);
        if(bitmap == null)
            throw new IOException("Unable to decode bitmap");
        return bitmap;
    }

    private static BitmapFactory.Options prepareOptionsForSampling(){
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        options.inSampleSize = 1;
        return options;
    }
    private static void updateOptionsForDecoding(BitmapFactory.Options options, Config config){
        options.inJustDecodeBounds = false;
        options.inSampleSize = calcSampleSize(options,config);
    }
    private static int calcSampleSize(BitmapFactory.Options options, Config config){
        int sampleSize = 1;
        while (options.outHeight / sampleSize > config.max_height || options.outWidth / sampleSize > config.max_width)
            sampleSize *= 2;

        return sampleSize;
    }
}
