package com.example.pdac_assignment;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.pdac_assignment.Utils.Histogram;

import java.io.IOException;
import java.util.HashSet;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.example.pdac_assignment", appContext.getPackageName());
    }

    @Test
    public void testHistogram_SolidColor() throws IOException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getContext();
        Histogram p = Histogram.instantiateHistogram(appContext.getAssets().open("test_solid_color.jpg")
                , new Histogram.ConfigBuilder()
                        .setMaxBoundary(128)
                .build()
        );
        assertEquals(p.getSortedColors().length,1);

    }

    @Test
    public void testSignificantColors() throws IOException {
        testHistogram_accuracy("test_redish_image.jpg");
    }

    @Test
    public void testHistogram_landscape1() throws IOException {
        testHistogram_accuracy("test_landscape.jpg");
    }

    @Test
    public void testHistogram_landscape2() throws IOException {
        testHistogram_accuracy("test_landscape2.jpg");
    }
    public void testHistogram_accuracy(String fileName) throws IOException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getContext();
        Histogram p = Histogram.instantiateHistogram(appContext.getAssets().open(fileName)
                ,new Histogram.ConfigBuilder()
                    .setScaleBy(1)
                .build()
        );
        Histogram p2 = Histogram.instantiateHistogram(appContext.getAssets().open(fileName)
                ,new Histogram.ConfigBuilder()
                        .setScaleBy(4)
                        .build());
        HashSet<Histogram.Color> hashSet = new HashSet<>();
        for(int i = 0; i < 6; i++)
            hashSet.add(p.getSortedColors()[i]);
        for(int i = 0; i < 5; i++)
            assertTrue(hashSet.contains(p2.getSortedColors()[i]));
    }

}