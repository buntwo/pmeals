package com.sleepykoala.pmeals.services;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_ISREFRESH;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNAME;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONNUMBER;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAMES;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.MEALTIMESXML;
import static com.sleepykoala.pmeals.data.C.STRING_DOWNLOADFAILED;

import java.io.IOException;
import java.util.ArrayList;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.util.SparseArray;

import com.sleepykoala.pmeals.contentproviders.MenuProvider;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.data.PMealsDB;

public class DailyDownloadService extends IntentService {
	
	//private static final String TAG = "DailyDownloadService";

	private MealTimeProvider mTP;
	private ArrayList<Location> locs;
	private static final String[] projection = { PMealsDB.ITEMNAME, PMealsDB.ITEMERROR };
	private static final String select = "((" + PMealsDB.LOCATIONID + "=?) and ("
			+ PMealsDB.DATE + "=?) and (" + PMealsDB.MEALNAME + "=?))";

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
		ContentResolver cr = getContentResolver();
		SparseArray<DatedMealTime> meals = new SparseArray<DatedMealTime>();
		String[] selectArgs = null;
		long today = (new Date()).toMillis(false);
		for (Location l : locs) {
			int type = l.type;
			DatedMealTime dmt = meals.get(type);
			if (dmt == null) {
				dmt = mTP.getCurrentMeal(type);
				while (dmt.date.toMillis(false) < today)
					dmt = mTP.getNextMeal(type, dmt);
				meals.put(type, dmt);
				selectArgs = new String[]{ "", dmt.date.toString(), dmt.mealName };
			}
			selectArgs[0] = String.valueOf(l.ID);
			Cursor c = cr.query(MenuProvider.MEALS_URI, projection, select, selectArgs, null);
			if (c.getCount() != 0) {
				c.moveToFirst();
				// refresh on dl fail
				if (c.getInt(c.getColumnIndexOrThrow(PMealsDB.ITEMERROR)) == 1
						&& c.getString(c.getColumnIndexOrThrow(PMealsDB.ITEMNAME)).equals(STRING_DOWNLOADFAILED)) {
					Date date = new Date();
					MenuProvider.startRefresh(String.valueOf(l.ID), date.toString());
					Intent dlService = new Intent(this, MenuDownloaderService.class);
					dlService.putExtra(EXTRA_LOCATIONID, String.valueOf(l.ID));
					dlService.putExtra(EXTRA_LOCATIONNAME, l.locName);
					dlService.putExtra(EXTRA_LOCATIONNUMBER, l.locNum);
					dlService.putExtra(EXTRA_DATE, date.toString());
					dlService.putExtra(EXTRA_ISREFRESH, true);
					dlService.putExtra(EXTRA_MEALNAMES, mTP.getDaysMealNames(l.type, dmt.date.weekDay));

					startService(dlService);
				}
			}
			c.close();
		}
	}

}