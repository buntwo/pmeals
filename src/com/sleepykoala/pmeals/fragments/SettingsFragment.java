package com.sleepykoala.pmeals.fragments;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.C;

public class SettingsFragment extends PreferenceFragment {
	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set to our pref filename
        PreferenceManager prefMgr = getPreferenceManager();
        prefMgr.setSharedPreferencesName(C.PREFSFILENAME);
        prefMgr.setSharedPreferencesMode(0);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);
    }

}
