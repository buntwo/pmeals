package com.sleepykoala.pmeals.services;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAME;
import android.content.Intent;
import android.widget.RemoteViewsService;

import com.sleepykoala.pmeals.adapters.MenuWidgetListFactory;

public class MenuWidgetAdapterService extends RemoteViewsService {
	
	//private static final String TAG = "MenuWidgetAdapterService";
	
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		return new MenuWidgetListFactory(this,
				intent.getIntExtra(EXTRA_LOCATIONID, -1),
				intent.getStringExtra(EXTRA_MEALNAME),
				intent.getStringExtra(EXTRA_DATE)); }
	
}