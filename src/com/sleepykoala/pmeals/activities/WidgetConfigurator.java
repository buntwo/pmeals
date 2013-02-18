package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_WIDGET_LOCID;
import static com.sleepykoala.pmeals.data.C.PREF_WIDGET_LOCNAME;
import static com.sleepykoala.pmeals.data.C.PREF_WIDGET_TYPE;

import java.io.IOException;
import java.util.ArrayList;

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.receivers.MenuWidgetProvider;

public class WidgetConfigurator extends ListActivity {
	
	//private static final String TAG = "WidgetConfigurator";
	
	private ArrayList<Location> locs;
	private int widgetId;
	private Intent resultValue;

	@Override
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_widgetconfigurator);
        
		resultValue = new Intent();
		setResult(RESULT_CANCELED, resultValue);
		
		Intent intent = getIntent();
		Bundle extras = intent.getExtras();
		if (extras != null) {
			widgetId = extras.getInt(
					AppWidgetManager.EXTRA_APPWIDGET_ID, 
					AppWidgetManager.INVALID_APPWIDGET_ID);
		} else
			finish();
		
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
		
        // get location provider
        try {
        	LocationProviderFactory.initialize(getAssets().open(LOCATIONSXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
		}
        LocationProvider lP = LocationProviderFactory.newLocationProvider();
        
        // get list of locations
		locs = lP.getAllLocations();
		int numLocs = locs.size();
		ArrayList<String> locNames = new ArrayList<String>(numLocs);
		for (Location l : locs)
			locNames.add(l.locName);
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
				R.layout.widgetconfig_row, R.id.widgetconfig_locname, locNames);
		setListAdapter(adapter);
	}
	
	// configure the widget
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Location loc = locs.get(position);
		
		// save widget config in prefs
		SharedPreferences prefs = getSharedPreferences(PREFSFILENAME, 0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(PREF_WIDGET_LOCID, loc.ID);
		editor.putString(PREF_WIDGET_LOCNAME, loc.nickname);
		editor.putInt(PREF_WIDGET_TYPE, loc.type);
		editor.commit();
		
		// update
		Intent update = new Intent(this, MenuWidgetProvider.class);
		update.setAction("android.appwidget.action.APPWIDGET_UPDATE");
		int[] ids = {widgetId};
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		sendBroadcast(update);
		
		setResult(RESULT_OK, resultValue);
		finish();
	}
	
}