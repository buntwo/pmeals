package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_DEFAULTLOC;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.sleepykoala.pmeals.data.Date;

public class Launcher extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		SharedPreferences prefs = getSharedPreferences(PREFSFILENAME, 0);
		// launch correct activity
		Intent intent;
		int defaultLoc = Integer.parseInt(prefs.getString(PREF_DEFAULTLOC, "-1"));
		if (defaultLoc == -1) {
			intent = new Intent(this, ViewByMeal.class);
		} else {
			intent = new Intent(this, ViewByLocation.class);
			intent.putExtra(EXTRA_LOCATIONID, defaultLoc);
			intent.putExtra(EXTRA_DATE, (new Date().toString()));
		}
		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
		finish();
	}
}
