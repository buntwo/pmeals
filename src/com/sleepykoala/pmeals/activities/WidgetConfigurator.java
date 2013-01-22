package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_TYPE;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;

import java.io.IOException;
import java.util.ArrayList;

import android.app.ListActivity;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RemoteViews;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.services.MenuWidgetAdapterService;

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
		locs = lP.getLocationsForType(0, 1, 2);
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
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
		
		RemoteViews views = new RemoteViews(this.getPackageName(), R.layout.widget_menu);
		Location loc = locs.get(position);
		views.setTextViewText(R.id.widget_locname, loc.nickname);
		Intent intent = new Intent(this, MenuWidgetAdapterService.class);
		intent.putExtra(EXTRA_LOCATIONID, loc.ID);
		intent.putExtra(EXTRA_TYPE, loc.type);
		views.setRemoteAdapter(R.id.widget_list, intent);
		
		appWidgetManager.updateAppWidget(widgetId, views);
		setResult(RESULT_OK, resultValue);
		finish();
	}
	
}
