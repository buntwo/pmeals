package com.sleepykoala.pmeals.services;

import static com.sleepykoala.pmeals.data.C.ACTION_WIDGET_FORWARD;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_WIDGET_LOCID;
import static com.sleepykoala.pmeals.data.C.PREF_WIDGET_LOCNAME;
import static com.sleepykoala.pmeals.data.C.PREF_WIDGET_TYPE;

import java.io.IOException;
import java.util.ArrayList;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;

import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.receivers.MenuWidgetProvider;

public class WidgetSwitcherService extends IntentService {
	
	LocationProvider lP;
	
	public WidgetSwitcherService() {
		super("WidgetSwitcherService");
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		
        // get location provider
        try {
        	LocationProviderFactory.initialize(getAssets().open(LOCATIONSXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
		}
        lP = LocationProviderFactory.newLocationProvider();
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		SharedPreferences prefs = getSharedPreferences(PREFSFILENAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
        // get list of locations
		ArrayList<Location> locs = lP.getAllLocations();
		int size = locs.size();
		ArrayList<Integer> locIDs = new ArrayList<Integer>(size);
		for (Location l : locs)
			locIDs.add(l.ID);
		// select new location
		Location newLoc;
		if (intent.getAction().equals(ACTION_WIDGET_FORWARD)) {
			// select next one
			newLoc = lP.getById(locIDs.get(
					(locIDs.indexOf(prefs.getInt(PREF_WIDGET_LOCID, -1)) + 1) % size
					));
		} else {
			// select previous one
			newLoc = lP.getById(locIDs.get(
					(locIDs.indexOf(prefs.getInt(PREF_WIDGET_LOCID, -1)) + size - 1) % size
					));
		}
		// save in prefs
		editor.putInt(PREF_WIDGET_LOCID, newLoc.ID);
		editor.putString(PREF_WIDGET_LOCNAME, newLoc.nickname);
		editor.putInt(PREF_WIDGET_TYPE, newLoc.type);
		editor.commit();
		
		// update
		Intent update = new Intent(this, MenuWidgetProvider.class);
		update.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS,
				intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS));
		sendBroadcast(update);
	}

}