package com.sleepykoala.pmeals.fragments;

import android.content.Context;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;

import com.sleepykoala.pmeals.R;

public class AboutPreference extends DialogPreference {

	public AboutPreference(Context context, AttributeSet attrs) {
		super(new ContextThemeWrapper(context, R.style.Theme_Dialog_NoFrame), attrs);
		
		setDialogLayoutResource(R.layout.about);
		setDialogTitle("About");
		setDialogIcon(R.drawable.launcher);
	}

}
