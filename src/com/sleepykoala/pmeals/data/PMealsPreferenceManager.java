package com.sleepykoala.pmeals.data;

import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_LOCATIONORDER;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

public class PMealsPreferenceManager {
	
	private static boolean isInitialized;
	private static SharedPreferences prefs;
	private static ArrayList<Integer> locIds; // cache it
	private static Context ctx;

	// can't be initialized
	private PMealsPreferenceManager() {};

	public static void initialize(Context c) {
		if (!isInitialized) {
			ctx = c;
			prefs = ctx.getSharedPreferences(PREFSFILENAME, 0);
			
			isInitialized = true;
		}
	}
	
	// get an arraylist of locations in the user's order
	// make sure LocationProvider is initialized!
	public static ArrayList<Integer> getLocIds() {
		if (!isInitialized)
			throw new IllegalStateException("Not initialized");
		
		if (locIds != null)
			return locIds;
		
		Set<String> locs = prefs.getStringSet(PREF_LOCATIONORDER, null);
		if (locs != null) { // yay exists
			// retrieve them in the correct order
			ArrayList<String> locIDsRaw = new ArrayList<String>(locs);
			Collections.sort(locIDsRaw);
			locIds = new ArrayList<Integer>(locs.size());
			for (String s : locIDsRaw)
				locIds.add(Integer.valueOf(s.substring(2)));
		} else { // new pref
			// initialize location provider
			try {
				LocationProviderFactory.initialize(ctx.getAssets().open(LOCATIONSXML));
			} catch (IOException e) {
				throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
			}
			LocationProvider lP = LocationProviderFactory.newLocationProvider();
			locIds = lP.getIDsForType(0, 1, 2);
			int numLocs = locIds.size();
			locs = new HashSet<String>(numLocs);
    		for (int i = 0; i < numLocs; ++i)
    			locs.add(String.valueOf(String.format("%02d%d", i, locIds.get(i))));
    		SharedPreferences.Editor editor = prefs.edit();
			editor.putStringSet(PREF_LOCATIONORDER, locs);
			editor.commit();
		}
		
		return locIds;
	}
	
	public static void storeLocIds(ArrayList<Integer> aLocIds) {
		if (!isInitialized)
			throw new IllegalStateException("Not initialized");
		
		Set<String> locs = new HashSet<String>(aLocIds.size());
		int numLocs = aLocIds.size();
		for (int i = 0; i < numLocs; ++i)
			locs.add(String.valueOf(String.format("%02d%d", i, aLocIds.get(i))));
		// store it
		SharedPreferences.Editor editor = prefs.edit();
		editor.putStringSet(PREF_LOCATIONORDER, locs);
		editor.commit();
		locIds = aLocIds;
	}
}
