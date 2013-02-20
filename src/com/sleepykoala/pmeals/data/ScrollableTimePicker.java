package com.sleepykoala.pmeals.data;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.TimePicker;

public class ScrollableTimePicker extends TimePicker {

	public ScrollableTimePicker(Context context) {
		super(context);
	}

	public ScrollableTimePicker(Context context, AttributeSet attr) {
		super(context, attr);
	}

	public ScrollableTimePicker(Context context, AttributeSet attr, int defStyle) {
		super(context, attr, defStyle);
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {

		// Stop ScrollView from getting involved once you interact with the View
		if (ev.getActionMasked() == MotionEvent.ACTION_DOWN)
		{
			ViewParent p = getParent();
			if (p != null)
				p.requestDisallowInterceptTouchEvent(true);
		}
		return false;
	}

}
