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
import java.util.Objects;

public class HistogramFactory {
    public static final int DEFAULT_SCALING_FACTOR = 4;

    private static class Config{
        public Integer max_boundary = null;
        public Integer scale_by = DEFAULT_SCALING_FACTOR;

        public boolean isPredefinedScaleBy(){
            return scale_by != null;
        }
    }
    public static class ConfigBuilder{
        private Config config = new Config();
        public ConfigBuilder setMaxBoundary(int boundary){
            config.scale_by = null;
            config.max_boundary = boundary;
            return this;
        }

        /**
         * @param scaleBy Must be power of 2
         */
        public ConfigBuilder setScaleBy(int scaleBy){
            config.scale_by = scaleBy;
            config.max_boundary = null;

            return this;
        }
        public Config build(){
            return config;
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
//            return Integer.toHexString(color);
            return "R:" + Integer.toString((color & 0x00ff0000) >> 16)
                    +" B:" + Integer.toString((color & 0x0000ff00) >> 8)
                    +" C:" + Integer.toString((color & 0x000000ff));
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            if(obj instanceof Color) {
                int color = ((Color) obj).color;
                return color == this.color;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.color);
        }
    }
    private HistogramFactory(){
    }

    private HistogramFactory(Color[] colors, int itemsCount){
        mPaletteColors = colors;
        mItemsCount = itemsCount;
    }
    private HistogramFactory(HashMap<Integer,Integer> map, int itemsCount){
        mColorMap = map;
        mItemsCount = itemsCount;

    }
    private Color[] mPaletteColors;
    private HashMap<Integer,Integer> mColorMap;
    private boolean mSorted = false;
    private int mItemsCount = 0;

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

    public int getTotalColorsCount() {
        return mItemsCount;
    }
    public float getColorShare(@NonNull Color color){
        return (color.getCount()/(float) mItemsCount) * 100F;
    }


    public static HistogramFactory instantiateHistogram(byte[] bytes, int offset, int length){
       return instantiateHistogram(bytes, offset, length,null);
    }
    public static HistogramFactory instantiateHistogram(byte[] bytes, int offset, int length, Config config) throws IllegalArgumentException{
        if(config == null)
            config = new Config();
        Bitmap bitmap = null;
        try {
            bitmap = prepareHistogramBitmap(bytes,offset, length, config);
        }catch (Exception e){
            throw new IllegalArgumentException("Unable to decode bytes", e);
        }
        HistogramFactory histogram = new HistogramFactory(prepareColorsFromHistogramBitmap(bitmap),bitmap.getHeight() * bitmap.getWidth());
        bitmap.recycle();
        return histogram;
    }
    public static HistogramFactory instantiateHistogram(InputStream is){
       return instantiateHistogram(is,null);
    }
    public static HistogramFactory instantiateHistogram(InputStream is, Config config) throws IllegalArgumentException{
        if(config == null)
            config = new Config();
        Bitmap bitmap = null;
        try {
            bitmap = prepareHistogramBitmap(is,config);
        }catch (Exception e){
            throw new IllegalArgumentException("Unable to decode stream", e);
        }
        HistogramFactory histogram = new HistogramFactory(prepareColorsFromHistogramBitmap(bitmap),bitmap.getHeight() * bitmap.getWidth());
        bitmap.recycle();
        return histogram;
    }
    private static HashMap<Integer,Integer> prepareColorMapFromBitmap(@NonNull Bitmap bitmap){
        HashMap<Integer,Integer> map = new HashMap<>();
        for(int i = 0; i < bitmap.getWidth(); i++)
            for(int j = 0; j < bitmap.getHeight(); j++) {
                int pixel = bitmap.getPixel(i,j);
                if (!map.containsKey(pixel))
                    map.put(pixel,1);
                else
                    map.put(pixel,map.get(pixel) + 1);

            }
        return map;
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
        BitmapFactory.Options options = prepareOptionsForSampling(config);
        if(!config.isPredefinedScaleBy()) {
            BitmapFactory.decodeByteArray(bytes, offset, length, options);
            updateOptionsForDecoding(options, config);
        }
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,offset,length,options);
        if(bitmap == null)
            throw new IllegalStateException("Unable to decode bitmap from array");
        return bitmap;

    }
    private static Bitmap prepareHistogramBitmap(InputStream is, Config config) throws IOException {
        BitmapFactory.Options options = prepareOptionsForSampling(config);
        if(!config.isPredefinedScaleBy()) {
            BitmapFactory.decodeStream(is, null, options);
            is.reset();
            updateOptionsForDecoding(options, config);
        }
        Bitmap bitmap =  BitmapFactory.decodeStream(is,null,options);
        if(bitmap == null)
            throw new IOException("Unable to decode bitmap");
        return bitmap;
    }

    private static BitmapFactory.Options prepareOptionsForSampling(Config config){
        BitmapFactory.Options options = new BitmapFactory.Options();
        if(config.max_boundary != null) {
            options.inJustDecodeBounds = true;
            options.inSampleSize = 1;
        }else
            options.inSampleSize = config.scale_by;
        return options;
    }
    private static void updateOptionsForDecoding(BitmapFactory.Options options, Config config){
        options.inJustDecodeBounds = false;
        options.inSampleSize = calcSampleSize(options,config);
    }
    private static int calcSampleSize(BitmapFactory.Options options, Config config){
        int sampleSize = 1;
        while (options.outHeight / sampleSize > config.max_boundary || options.outWidth / sampleSize > config.max_boundary)
            sampleSize *= 2;

        return sampleSize;
    }
}
