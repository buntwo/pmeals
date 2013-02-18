package com.sleepykoala.pmeals.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.Toast;

import com.sleepykoala.pmeals.data.C;
import com.sleepykoala.pmeals.fragments.AboutFragment;
import com.sleepykoala.pmeals.fragments.SettingsFragment;

public class SettingsActivity extends Activity {

	private static final IntentFilter sIntentFilter;
	// create intent filter
	static {
		sIntentFilter = new IntentFilter();
		sIntentFilter.addAction(C.ACTION_ABOUT);
	}
	private final BroadcastReceiver preferenceReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Toast.makeText(SettingsActivity.this, "lol", Toast.LENGTH_SHORT).show();
			if (action.equals(C.ACTION_ABOUT)) {
				AboutFragment about = new AboutFragment();
				about.show(getFragmentManager(), "about");
			}
		}
	};
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// Display the fragment as the main content.
		getFragmentManager().beginTransaction()
		.replace(android.R.id.content, new SettingsFragment())
		.commit();
	}

    @Override
    public void onPause() {
    	super.onPause();
    	// unregister receiver
    	unregisterReceiver(preferenceReceiver);
    }

    @Override
    public void onResume() {
    	super.onResume();
    	// register receiver
        registerReceiver(preferenceReceiver, sIntentFilter);
    }
}
