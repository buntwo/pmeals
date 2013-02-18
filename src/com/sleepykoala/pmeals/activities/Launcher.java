package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_DEFAULTLOC;
import static com.sleepykoala.pmeals.data.C.PREF_FIRSTTIME;
import static com.sleepykoala.pmeals.data.C.PREF_LASTVER;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.fragments.FirstTimeFragment;
import com.sleepykoala.pmeals.fragments.FirstTimeFragment.OnFirstTimeDismissListener;

public class Launcher extends Activity implements OnFirstTimeDismissListener {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_launcher);
		
		SharedPreferences prefs = getSharedPreferences(PREFSFILENAME, 0);
        // upgrade code
        // show help dialog on first time or upgrade
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
        
	}

	public void launch() {
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
