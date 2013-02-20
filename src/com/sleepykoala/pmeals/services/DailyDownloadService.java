package com.sleepykoala.pmeals.services;

import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.MEALTIMESXML;

import java.io.IOException;
import java.util.ArrayList;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.util.Log;
import android.util.SparseArray;

import com.sleepykoala.pmeals.contentproviders.MenuProvider;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.data.PMealsDatabase;

public class DailyDownloadService extends IntentService {
	
	private static final String TAG = "DailyDownloadService";

	private MealTimeProvider mTP;
	private ArrayList<Location> locs;
	private static final String[] projection = { PMealsDatabase.ITEMNAME };
	private static final String select = "((" + PMealsDatabase.LOCATIONID + "=?) and ("
			+ PMealsDatabase.DATE + "=?) and (" + PMealsDatabase.MEALNAME + "=?))";

	public DailyDownloadService() {
		super("DailyDownloadService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
        // get meal time provider
		try {
			MealTimeProviderFactory.initialize(getAssets().open(MEALTIMESXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + MEALTIMESXML + "!!");
		}
		mTP = MealTimeProviderFactory.newMealTimeProvider();
        // get location provider
        try {
        	LocationProviderFactory.initialize(getAssets().open(LOCATIONSXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
		}
        LocationProvider lP = LocationProviderFactory.newLocationProvider();
        locs = lP.getAllLocations();
	}

	/**
	 * Check and downloads if needed, each location's next meal.
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.d(TAG, "downloading stuffs");
		
		ContentResolver cr = getContentResolver();
		SparseArray<DatedMealTime> meals = new SparseArray<DatedMealTime>();
		String[] selectArgs = null;
		long today = (new Date()).toMillis(false);
		for (Location l : locs) {
			int type = l.type;
			DatedMealTime dmt = meals.get(type);
			if (dmt == null) {
				dmt = mTP.getCurrentMeal(type);
				while (dmt.date.toMillis(false) < today) {
					dmt = mTP.getNextMeal(type, dmt);
				}
				meals.put(type, dmt);
				selectArgs =  new String[]{ "", dmt.date.toString(), dmt.mealName };
			}
			selectArgs[0] = String.valueOf(l.ID);
			cr.query(MenuProvider.CONTENT_URI, projection, select, selectArgs, null);
		}
	}

}