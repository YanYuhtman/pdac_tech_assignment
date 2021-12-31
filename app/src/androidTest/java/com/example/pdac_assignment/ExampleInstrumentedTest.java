package com.example.pdac_assignment;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.pdac_assignment.Utils.Histogram;

import java.io.IOException;

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
    public void testPalette() throws IOException {
        Context appContext = InstrumentationRegistry.getInstrumentation().getContext();
        Histogram p = Histogram.instantiateHistogram(appContext.getAssets().open("test_palette.jpg"),new Histogram.Config(128,128));
        Histogram p2 = Histogram.instantiateHistogram(appContext.getAssets().open("test_palette.jpg"),new Histogram.Config(3000,3000));
        assertEquals(p.getSortedColors()[0],p2.getSortedColors()[0]);
        assertEquals(p.getSortedColors()[1],p2.getSortedColors()[1]);
        assertEquals(p.getSortedColors()[2],p2.getSortedColors()[2]);
        assertEquals(p.getSortedColors()[3],p2.getSortedColors()[3]);
        assertEquals(p.getSortedColors()[4],p2.getSortedColors()[4]);

    }
}