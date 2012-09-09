package com.buntwo.pmeals.data;

import android.animation.TypeEvaluator;
import android.graphics.Color;

public class RgbEvaluator implements TypeEvaluator<Integer> {

	public Integer evaluate(float fraction, Integer startValue, Integer endValue) {
		int color0 = startValue;
		int color1 = endValue;
		int red0 = Color.red(color0);
		int green0 = Color.green(color0);
		int blue0 = Color.blue(color0);
		return Color.argb(0xff,
				red0 + Math.round((fraction*(Color.red(color1)-red0))),
				green0 + Math.round((fraction*(Color.green(color1)-green0))),
				blue0 + Math.round(fraction*(Color.blue(color1)-blue0)));
	}
}
