package com.sleepykoala.pmeals.services;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUMS;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.MEALTIMESXML;

import java.io.IOException;
import java.util.ArrayList;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.text.format.Time;

import com.sleepykoala.pmeals.R;
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
	private ArrayList<Location> locs;
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
        LocationProvider lP = LocationProviderFactory.newLocationProvider();
        // get all locs
        locs = lP.getAllLocations();
        // initalize prefman
        PMealsPreferenceManager.initialize(this);
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		ArrayList<Integer> nums = intent.getIntegerArrayListExtra(EXTRA_ALERTNUMS);
		ContentResolver cr = getContentResolver();
		Date today = new Date();
		
		for (int num : nums) {
			// parallel-ish arrays of matched items (can have > 1 menu item per loc)
			String locName = null;
			ArrayList<String> menuItems = new ArrayList<String>();
			String mealName = null;
			
			String query = PMealsPreferenceManager.getAlertQuery(num);
			query = "%" + query + "%";
			for (Location l : locs) {
				DatedMealTime dmt = mTP.getCurrentMeal(l.type);
				// if it's not today or tomorrow, skip
				if (!(dmt.date.equals(today) || dmt.date.isTomorrow(today)))
					continue;
				String[] selectArgs =  { String.valueOf(l.ID), dmt.date.toString(),
						dmt.mealName, query };
				Cursor c = cr.query(MenuProvider.CONTENT_URI, projection, select, selectArgs, null);
				if (c.getCount() == 0)
					continue;
				// yay matched, add to arrays
				if (locName == null) {
					locName = l.nickname;
					if (LocationProvider.isDiningHall(l))
						mealName = dmt.mealName;
				}
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
			//Bitmap large = BitmapFactory.decodeResource(getResources(), R.drawable.launcher);
			//builder.setLargeIcon(large);
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
			ticker.append(" at " ).append(locName);
			if (mealName != null)
				ticker.append(" for ").append(mealName);
			builder.setTicker(ticker);
			// set other properties
			builder.setContentInfo(String.valueOf(menuItems.size()));
			builder.setDefaults(Notification.DEFAULT_ALL);
			// send notification!
			NotificationManager notifMan =
				    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
			notifMan.notify(num, builder.build());
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
		ArrayList<Integer> nextNums = new ArrayList<Integer>();
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
			tm.setToNow();
			tm.second = 0;
			int currentDay = tm.monthDay;
			tm.hour = PMealsPreferenceManager.getAlertHour(num);
			tm.minute = PMealsPreferenceManager.getAlertMinute(num);
			for (int j = 0; j < 7; ++j) {
				if (((repeat >> j) & 1) == 0)
					continue;
				tm.monthDay = currentDay;
				tm.monthDay += (j - tm.weekDay + 7) % 7;
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
				} else if (alertTime == nextTime)
					nextNums.add(num);
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
		PendingIntent pI = PendingIntent.getService(c, 0, alert, PendingIntent.FLAG_CANCEL_CURRENT);
		((AlarmManager) c.getSystemService(Context.ALARM_SERVICE)).set(
				AlarmManager.RTC_WAKEUP, nextTime, pI);
	}

}
