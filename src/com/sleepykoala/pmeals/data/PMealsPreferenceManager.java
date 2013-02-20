package com.sleepykoala.pmeals.data;

import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_ALERTHOUR;
import static com.sleepykoala.pmeals.data.C.PREF_ALERTMINUTE;
import static com.sleepykoala.pmeals.data.C.PREF_ALERTON;
import static com.sleepykoala.pmeals.data.C.PREF_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.PREF_ALERTREPEAT;
import static com.sleepykoala.pmeals.data.C.PREF_LOCATIONORDER;
import static com.sleepykoala.pmeals.data.C.PREF_NUMALERTS;

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
	private PMealsPreferenceManager() {
	}

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
		assertInitialized();

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
				LocationProviderFactory.initialize(ctx.getAssets().open(
						LOCATIONSXML));
			} catch (IOException e) {
				throw new RuntimeException("Cannot find asset " + LOCATIONSXML
						+ "!!");
			}
			LocationProvider lP = LocationProviderFactory.newLocationProvider();
			locIds = lP.getIDsForType(0, 1, 2);
			int numLocs = locIds.size();
			locs = new HashSet<String>(numLocs);
			for (int i = 0; i < numLocs; ++i)
				locs.add(String.valueOf(String.format("%02d%d", i,
						locIds.get(i))));
			SharedPreferences.Editor editor = prefs.edit();
			editor.putStringSet(PREF_LOCATIONORDER, locs);
			editor.commit();
		}

		return locIds;
	}

	public static void storeLocIds(ArrayList<Integer> aLocIds) {
		assertInitialized();

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

	/**
	 * Get the number of alerts.
	 * 
	 * @return Number of alerts
	 */
	public static int getNumAlerts() {
		assertInitialized();

		return prefs.getInt(PREF_NUMALERTS, 0);
	}
	
	/**
	 * Get the alert query.
	 * 
	 * @param num Alert number
	 * @return The alert query
	 */
	public static String getAlertQuery(int num) {
		assertInitialized();
		
		return prefs.getString(num + PREF_ALERTQUERY, "invalid alert number");
	}
	
	/**
	 * Get a 7-bit mask of which days this alert repeats on
	 * 
	 * @param num Alert number
	 * @return 7-bit mask of which days it repeats on, lowest bit is Sunday
	 */
	public static int getAlertRepeat(int num) {
		assertInitialized();
		
		/*
		int repeat = prefs.getInt(num + PREF_ALERTREPEAT, 0);
		boolean[] ret = new boolean[7];
		for (int i = 0; i < 7; ++i)
			if (((repeat >> i) & 1) == 1)
				ret[i] = true;
		
		return ret;
		*/
		return prefs.getInt(num + PREF_ALERTREPEAT, 0);
	}
	
	/**
	 * Get the hour at which this alert goes off.
	 * 
	 * @param num Alert number
	 * @return The hour at which this alert goes off
	 */
	public static int getAlertHour(int num) {
		assertInitialized();
		
		return prefs.getInt(num + PREF_ALERTHOUR, 0);
	}

	/**
	 * Get the minute at which this alert goes off.
	 * 
	 * @param num Alert number
	 * @return The minute at which this alert goes off
	 */
	public static int getAlertMinute(int num) {
		assertInitialized();
		
		return prefs.getInt(num + PREF_ALERTMINUTE, 0);
	}
	
	/**
	 * Gets the status of the alert.
	 * 
	 * @param num Alert number
	 * @return True if it's on, false if it's off
	 */
	public static boolean getAlertOn(int num) {
		assertInitialized();
		
		return prefs.getBoolean(num + PREF_ALERTON, false);
	}
	
	/**
	 * Set status of alert.
	 * 
	 * @param num Alert number
	 * @param isOn Status to store
	 */
	public static void setAlertOn(int num, boolean isOn) {
		assertInitialized();
		
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean(num + PREF_ALERTON, isOn);
		editor.commit();
	}

	/**
	 * Stores a new alert, incrementing PREF_NUMALERTS if necessary.
	 * 
	 * Assumes that if num > number of alerts, then num is the next number in
	 * line
	 * 
	 * @param num Alert number
	 * @param query Query string
	 * @param repeat 7-bit mask for repeat days
	 * @param hour Repeat hour
	 * @param min Repeat minute
	 */
	public static void storeAlert(int num, String query, int repeat, int hour,
			int min) {
		assertInitialized();
		
		SharedPreferences.Editor editor = prefs.edit();
		if (num > prefs.getInt(PREF_NUMALERTS, 0))
			editor.putInt(PREF_NUMALERTS, num);
		editor.putString(num + PREF_ALERTQUERY, query);
		editor.putInt(num + PREF_ALERTREPEAT, repeat);
		editor.putInt(num + PREF_ALERTHOUR, hour);
		editor.putInt(num + PREF_ALERTMINUTE, min);
		editor.putBoolean(num + PREF_ALERTON, true);
		editor.commit();
	}

	/**
	 * Deletes an alert.
	 * 
	 * @param num
	 *            Alert number to delete (1-indexed)
	 */
	public static void deleteAlert(int num) {
		assertInitialized();
		
		SharedPreferences.Editor editor = prefs.edit();
		int numAlerts = prefs.getInt(PREF_NUMALERTS, 0);
		// shift alerts down
		for (int i = num + 1; i <= numAlerts; ++i) {
			editor.putString((i-1) + PREF_ALERTQUERY, prefs.getString(i + PREF_ALERTQUERY, "invalid alert number"));
			editor.putInt((i-1) + PREF_ALERTREPEAT, prefs.getInt(i + PREF_ALERTREPEAT, 0));
			editor.putInt((i-1) + PREF_ALERTHOUR, prefs.getInt(i + PREF_ALERTHOUR, 0));
			editor.putInt((i-1) + PREF_ALERTMINUTE, prefs.getInt(i + PREF_ALERTMINUTE, 0));
			editor.putBoolean((i-1) + PREF_ALERTON, prefs.getBoolean(i + PREF_ALERTON, false));
		}
		// delete last one 
		editor.remove(numAlerts + PREF_ALERTQUERY);
		editor.remove(numAlerts + PREF_ALERTREPEAT);
		editor.remove(numAlerts + PREF_ALERTHOUR);
		editor.remove(numAlerts + PREF_ALERTMINUTE);
		editor.remove(numAlerts + PREF_ALERTON);
		// reduce number
		editor.putInt(PREF_NUMALERTS, numAlerts - 1);
		editor.commit();
	}

	// -----------------------------------------------------------------------------------

	private static void assertInitialized() {
		if (!isInitialized)
			throw new IllegalStateException("Not initialized");
	}
}
