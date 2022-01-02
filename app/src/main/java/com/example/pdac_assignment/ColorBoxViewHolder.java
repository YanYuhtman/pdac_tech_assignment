package com.example.pdac_assignment;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.pdac_assignment.Utils.Histogram;

import java.util.Locale;

public class ColorBoxViewHolder{

    View colorBox = null;
    TextView tv_percent = null;
    TextView tv_colorstring = null;
    static final String DEFAULT_PERCENT = "0.0%";
    static final String DEFAULT_COLOR = "R: B: G:";

    ColorBoxViewHolder(@NonNull View view ) {
        colorBox = view.findViewById(R.id.color_component_colorbox);
        tv_percent = view.findViewById(R.id.color_component_tv_percent);
        tv_colorstring = view.findViewById(R.id.color_component_tv_colorstring);
        setDefaults();
    }
    void setDefaults(){
        ((GradientDrawable)colorBox.getBackground()).setColor(0x00000000);
        tv_percent.setText(DEFAULT_PERCENT);
        tv_percent.setTextColor(Color.WHITE);
        tv_colorstring.setText(DEFAULT_COLOR);
    }
    int calculateTextColor(int bgColor){
        return ((bgColor & 0xff0000) >> 24 < 0x7f
                && (bgColor & 0x00ff00) >> 16 < 0x7f
                && (bgColor & 0x0000ff)  < 0x7f) ? Color.WHITE
                : Color.BLACK;
    }
    void populateWith(Histogram.Color color, float rate){
        if(color != null){
            ((GradientDrawable)colorBox.getBackground()).setColor(color.color);
            tv_percent.setText(String.format(Locale.getDefault(),"%.2f%%",(rate)));
            tv_percent.setTextColor(calculateTextColor(color.color));
            tv_colorstring.setText(color.toString());
        }else
            setDefaults();
    }
}