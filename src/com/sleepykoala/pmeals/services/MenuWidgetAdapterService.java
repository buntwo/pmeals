package com.sleepykoala.pmeals.services;

import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_TYPE;
import static com.sleepykoala.pmeals.data.C.MEALTIMESXML;

import java.io.IOException;

import android.content.Intent;
import android.widget.RemoteViewsService;

import com.sleepykoala.pmeals.adapters.MenuWidgetListFactory;
import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;

public class MenuWidgetAdapterService extends RemoteViewsService {
	
	@Override
	public RemoteViewsFactory onGetViewFactory(Intent intent) {
		int type = intent.getIntExtra(EXTRA_TYPE, 0);
        // get meal time provider
		try {
			MealTimeProviderFactory.initialize(getAssets().open(MEALTIMESXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + MEALTIMESXML + "!!");
		}
		MealTimeProvider mTP = MealTimeProviderFactory.newMealTimeProvider();
		DatedMealTime dmt = mTP.getCurrentMeal(type);
		
		return new MenuWidgetListFactory(this,
				intent.getIntExtra(EXTRA_LOCATIONID, -1), dmt);
	}
	
}