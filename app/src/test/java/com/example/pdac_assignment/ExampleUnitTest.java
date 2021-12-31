package com.example.pdac_assignment;

import org.junit.Test;

import static org.junit.Assert.*;

import com.example.pdac_assignment.Utils.HistogramFactory;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    @Test
    public void addition_isCorrect() {
        assertEquals(4, 2 + 2);
    }
    @Test
    public void testColor(){
        assertEquals(new HistogramFactory.Color(0xff0000).toString(), "R:255 B:0 C:0");
        assertEquals(new HistogramFactory.Color(0xffff00).toString(), "R:255 B:255 C:0");
        assertEquals(new HistogramFactory.Color(0xffffff).toString(), "R:255 B:255 C:255");
        assertEquals(new HistogramFactory.Color(0xffffff).toString(), "R:255 B:255 C:255");
        assertEquals(new HistogramFactory.Color(0x7f7f7f).toString(), "R:127 B:127 C:127");
    }
}