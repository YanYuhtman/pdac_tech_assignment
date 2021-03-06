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

/**
 * Histogram factory class is responsible for popular colors evaluation
 */
public class Histogram {
    //Default scaling factor is empirically discovered (see androidTest)
    public static final int DEFAULT_SCALING_FACTOR = 4;

    //Config is a helper class for setting scaling factor or boundary size for image resizing
    private static class Config{
        public Integer max_boundary = null;
        public Integer scale_by = DEFAULT_SCALING_FACTOR;

        public boolean isPredefinedScaleBy(){
            return scale_by != null;
        }
    }
    //Builder helper class for setting Config params
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
    /**
     * Color class the resulting color item that involved in calculations
     * it includes the amount of this color represented in image
     */
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
    private Histogram(){
    }

    private Histogram(Color[] colors, int itemsCount){
        mColors = colors;
        mItemsCount = itemsCount;
    }
    
    //Resulting array of total unique colors of image
    private Color[] mColors;
    private boolean mSorted = false;
    //Total pixels count in image
    private int mItemsCount = 0;

    public Color[] getSortedColors(){
        if(!mSorted){
            Arrays.sort(mColors, new Comparator<Color>() {
                @Override
                public int compare(Color color, Color color2) {
                    return color2.mCount - color.mCount;
                }
            });
            mSorted = true;
        }
        return mColors;
    }

    public int getTotalColorsCount() {
        return mItemsCount;
    }
    public float getColorShare(@NonNull Color color){
        return (color.getCount()/(float) mItemsCount) * 100F;
    }

    public static Histogram instantiateHistogram(byte[] bytes, int offset, int length){
       return instantiateHistogram(bytes, offset, length,null);
    }

    /**
     * Factory function returns histogram of a byte array that includes bitmap
     * @param bytes
     * @param offset
     * @param length
     * @param config
     * @return Histogram
     * @throws IllegalArgumentException
     */
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

    /**
     * Factory function returns histogram calculated from input stream bitmap
     * @param is
     * @param config
     * @return
     * @throws IllegalArgumentException
     */
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

    /**
     * Calculates the histogram map out of Bitmap object
     * @param bitmap
     * @return
     */
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

    /**
     * Generates scaled bitmap out of byte array
     * @param bytes
     * @param offset
     * @param length
     * @param config
     * @return
     * @throws IllegalStateException
     */
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

    /**
     * Generates scaled bitmap out of input stream
     * @param is
     * @param config
     * @return
     * @throws IOException
     */
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
    // Helper function to resolve scaling factor upon boundary size
    private static BitmapFactory.Options prepareOptionsForSampling(Config config){
        BitmapFactory.Options options = new BitmapFactory.Options();
        if(config.max_boundary != null) {
            options.inJustDecodeBounds = true;
            options.inSampleSize = 1;
        }else
            options.inSampleSize = config.scale_by;
        return options;
    }
    // Helper function to prepare Bitmap.Options for bitmap generation
    private static void updateOptionsForDecoding(BitmapFactory.Options options, Config config){
        options.inJustDecodeBounds = false;
        options.inSampleSize = calcSampleSize(options,config);
    }
    // Helper function to calculate sample size
    private static int calcSampleSize(BitmapFactory.Options options, Config config){
        int sampleSize = 1;
        while (options.outHeight / sampleSize > config.max_boundary || options.outWidth / sampleSize > config.max_boundary)
            sampleSize *= 2;

        return sampleSize;
    }
}
