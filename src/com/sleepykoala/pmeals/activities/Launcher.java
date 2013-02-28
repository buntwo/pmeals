package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.*;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.IS24HOURFORMAT;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_FIRSTTIME;
import static com.sleepykoala.pmeals.data.C.PREF_LASTVER;
import static com.sleepykoala.pmeals.data.C.PREF_STARTUPLOC;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.format.DateFormat;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.fragments.FirstTimeFragment;
import com.sleepykoala.pmeals.fragments.FirstTimeFragment.OnFirstTimeDismissListener;
import com.sleepykoala.pmeals.services.AlertService;
import com.sleepykoala.pmeals.services.DailyDownloadService;

public class Launcher extends Activity implements OnFirstTimeDismissListener {
	
	private static final int COOL_NUMBER = 2;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
		
        // set 24 hour status
        IS24HOURFORMAT = DateFormat.is24HourFormat(this);

        /*
		startActivity(new Intent(this, ManageAlertsActivity.class));
		finish();
		//*/
        /*
		Intent alert = new Intent(this, AlertService.class);
		ArrayList<Integer> alertNums = new ArrayList<Integer>();
		ArrayList<String> mealNames = new ArrayList<String>();
		alertNums.add(1);
		mealNames.add("Breakfast");
		alert.putExtra(EXTRA_ALERTNUMS, alertNums);
		alert.putExtra(EXTRA_MEALNAMES, mealNames);
		startService(alert);
		launch();
		//*/
		
		//*
        // set alert
        AlertService.setNextAlert(this);
		// set daily update alarm
		Intent dailyDownload = new Intent(this, DailyDownloadService.class);
		PendingIntent pI = PendingIntent.getService(this, 0, dailyDownload, PendingIntent.FLAG_CANCEL_CURRENT);
		((AlarmManager) getSystemService(Context.ALARM_SERVICE)).setRepeating(
				AlarmManager.RTC, (new Date()).toMillis(false) + COOL_NUMBER * 1000, AlarmManager.INTERVAL_DAY, pI);
		
        // upgrade code
        // show help dialog on first time or upgrade
		SharedPreferences prefs = getSharedPreferences(PREFSFILENAME, 0);
        int currentVer = 1;
        try {
			currentVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			// better not get here lol
		}
        if (prefs.getBoolean(PREF_FIRSTTIME, true) || (prefs.getInt(PREF_LASTVER, 0) < currentVer)) {
    		FirstTimeFragment ftf = new FirstTimeFragment();
    		ftf.show(getFragmentManager(), "firsttime");
    		
    		SharedPreferences.Editor editor = prefs.edit();
        	editor.putBoolean(PREF_FIRSTTIME, false);
        	editor.putInt(PREF_LASTVER, currentVer);
        	editor.commit();
        } else {
        	launch();
        }
        //*/
	}

	public void launch() {
		SharedPreferences prefs = getSharedPreferences(PREFSFILENAME, 0);
		// launch correct activity
		Intent intent;
		int defaultLoc = Integer.parseInt(prefs.getString(PREF_STARTUPLOC, "-1"));
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
