package com.sleepykoala.pmeals.services;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUMS;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.EXTRA_DATES;
import static com.sleepykoala.pmeals.data.C.EXTRA_ITEMNAMES;
import static com.sleepykoala.pmeals.data.C.EXTRA_ITEMSPERLOC;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONIDS;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAME;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAMES;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.MEALTIMESXML;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.activities.AlertViewerActivity;
import com.sleepykoala.pmeals.contentproviders.MenuProvider;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.data.PMealsDatabase;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;

public class AlertService extends IntentService {
	
	//private static final String TAG = "AlertService";
	
	private MealTimeProvider mTP;
	private LocationProvider lP;
	private static final String[] projection = { PMealsDatabase.ITEMNAME };
	private static final String select = "((" + PMealsDatabase.LOCATIONID + "=?) and ("
			+ PMealsDatabase.DATE + "=?) and (" + PMealsDatabase.MEALNAME + "=?) and ("
			+ PMealsDatabase.ITEMNAME + " like ?" + "))";
	
	public AlertService() {
		super("AlertService");
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
        lP = LocationProviderFactory.newLocationProvider();
        // initalize prefman
        PMealsPreferenceManager.initialize(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ArrayList<Integer> nums = intent.getIntegerArrayListExtra(EXTRA_ALERTNUMS);
		ArrayList<String> meals = intent.getStringArrayListExtra(EXTRA_MEALNAMES);
		ContentResolver cr = getContentResolver();
		Date today = new Date();
		
		int numAlerts = nums.size();
		for (int i = 0; i < numAlerts; ++i) {
			int num  = nums.get(i);
			String meal = meals.get(i);
			// parallel-ish arrays of matched items (can have > 1 menu item per loc)
			String locName = null; // name of first matched loc
			ArrayList<Integer> locIds = new ArrayList<Integer>(); // ids of all matched locs
			ArrayList<Integer> itemsPerLoc = new ArrayList<Integer>(); // parallel to locIds, number of matched items per loc
																	   // sum of numbers here must be equal to size of menuItems
			ArrayList<String> menuItems = new ArrayList<String>();
			ArrayList<String> dates = new ArrayList<String>();
			String mealName = null;
			
			String query = PMealsPreferenceManager.getAlertQuery(num);
			String query_ = "%" + query + "%";
			Set<String> locs = PMealsPreferenceManager.getAlertLocations(num);
			for (String s : locs) {
				Location l = lP.getById(Integer.parseInt(s));
				// get correct meal
				DatedMealTime dmt = mTP.getCurrentMeal(l.type);
				if (!meal.equals(""))
					while (!dmt.mealName.equals(meal))
						dmt = mTP.getNextMeal(l.type, dmt);
				// if it's not today or tomorrow, skip
				if (!(dmt.date.equals(today) || dmt.date.isYesterday(today)))
					continue;
				String[] selectArgs =  { String.valueOf(l.ID), dmt.date.toString(),
						dmt.mealName, query_ };
				Cursor c = cr.query(MenuProvider.CONTENT_URI, projection, select, selectArgs, null);
				int numMatched = c.getCount();
				if (numMatched == 0) // no match
					continue;
				// yay matched, add to arrays
				if (locName == null) {
					locName = l.nickname;
					if (LocationProvider.isDiningHall(l))
						mealName = dmt.mealName;
				}
				locIds.add(l.ID);
				itemsPerLoc.add(numMatched);
				dates.add(dmt.date.toString());
				c.moveToFirst();
				while (!c.isAfterLast()) {
					menuItems.add(c.getString(c.getColumnIndexOrThrow(PMealsDatabase.ITEMNAME)));
					c.moveToNext();
				}
				c.close();
			}
			if (locName == null)
				continue;
			
			// build notification
			NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
			builder.setSmallIcon(R.drawable.notif_small);
			builder.setContentTitle(menuItems.get(0));
			// set detail text
			StringBuilder detail = new StringBuilder();
			if (mealName != null)
				detail.append(mealName).append(" at ");
			detail.append(locName);
			if (menuItems.size() > 1)
				detail.append(", and more");
			builder.setContentText(detail);
			// set ticker
			StringBuilder ticker = new StringBuilder();
			for (String s : menuItems) {
				ticker.append(s).append(", ");
			}
			ticker.setLength(ticker.length() - 2);
			ticker.append(" at " );
			int size = locIds.size();
			ticker.append(locName);
			if (size > 1) {
				ticker.append(" and ").append(size - 1).append(" more location");
				if (size > 2)
					ticker.append("s");
			}
			if (mealName != null)
				ticker.append(" for ").append(mealName.toLowerCase());
			builder.setTicker(ticker);
			// set number of matched items
			builder.setContentInfo(String.valueOf(menuItems.size()));
			// set alert type
			builder.setDefaults(Notification.DEFAULT_ALL);
			builder.setAutoCancel(true);
			
			// build action intent
			Intent action = new Intent(this, AlertViewerActivity.class);
			action.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
			action.putExtra(EXTRA_ALERTQUERY, query);
			if (mealName != null)
				action.putExtra(EXTRA_MEALNAME, mealName);
			action.putExtra(EXTRA_LOCATIONIDS, locIds);
			action.putExtra(EXTRA_ITEMSPERLOC, itemsPerLoc);
			action.putExtra(EXTRA_ITEMNAMES, menuItems);
			action.putExtra(EXTRA_DATES, dates);
			// set data to differentiate it
			int id = (int) System.currentTimeMillis();
			action.setData(Uri.fromParts("content", String.format("%s.%s.%d",
					query, meal, id), null));
			PendingIntent pI = PendingIntent.getActivity(this, 0, action, PendingIntent.FLAG_ONE_SHOT);
			builder.setContentIntent(pI);
			// send notification!
			((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, builder.build());
		}
	}

	/**
	 * Set the next alert
	 * 
	 * @param c Context to use for setting alarm
	 */
	public static void setNextAlert(Context c) {
		PMealsPreferenceManager.initialize(c);
		
		long nextTime = Long.MAX_VALUE;
		ArrayList<Integer> nextNums = new ArrayList<Integer>(); // alert numbers
		ArrayList<String> nextMeals = new ArrayList<String>(); // alert meal names
		int numAlerts = PMealsPreferenceManager.getNumAlerts();
		boolean isOn = false; // is at least one alert on
		Time tm = new Time();
		tm.setToNow();
		long now = tm.toMillis(false);

		for (int num = 1; num <= numAlerts; ++num) {
			if (!PMealsPreferenceManager.getAlertOn(num))
				continue;

			isOn = true;
			int repeat = PMealsPreferenceManager.getAlertRepeat(num);
			ArrayList<Integer> times = new ArrayList<Integer>();
			ArrayList<String> meals = new ArrayList<String>();
			PMealsPreferenceManager.getAlertMeal_Times(num, meals, times);
			for (int j = 0; j < 7; ++j) {
				if (((repeat >> j) & 1) == 0)
					continue;
				int numTimes = times.size();
				for (int i = 0; i < numTimes; ++i) {
					tm.setToNow();
					tm.second = 0;
					tm.monthDay += (j - tm.weekDay + 7) % 7;
					int time = times.get(i);
					tm.hour = time / 60;
					tm.minute = time % 60;
					long alertTime = tm.toMillis(true);
					// add a week if it's before current time
					if (now >= alertTime) {
						tm.monthDay += 7;
						alertTime = tm.toMillis(true);
					}
					if (alertTime < nextTime) {
						nextTime = alertTime;
						nextNums.clear();
						nextNums.add(num);
						nextMeals.clear();
						nextMeals.add(meals.get(i));
					} else if (alertTime == nextTime) {
						nextNums.add(num);
						nextMeals.add(meals.get(i));
					}
				}
			}
		}
		if (!isOn) {
			PendingIntent pI = PendingIntent.getService(c, 0, new Intent(c,
					AlertService.class), PendingIntent.FLAG_CANCEL_CURRENT);
			((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).cancel(pI);
			return;
		}
		
		// set alarm
		Intent alert = new Intent(c, AlertService.class);
		alert.putExtra(EXTRA_ALERTNUMS, nextNums);
		alert.putExtra(EXTRA_MEALNAMES, nextMeals);
		PendingIntent pI = PendingIntent.getService(c, 0, alert, PendingIntent.FLAG_CANCEL_CURRENT);
		((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).set(
				AlarmManager.RTC_WAKEUP, nextTime, pI);
	}

}
