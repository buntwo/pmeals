package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_DATES;
import static com.sleepykoala.pmeals.data.C.EXTRA_ITEMNAMES;
import static com.sleepykoala.pmeals.data.C.EXTRA_ITEMSPERLOC;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONIDS;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAME;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;

import java.io.IOException;
import java.util.ArrayList;

import android.app.ActionBar;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.adapters.AlertViewerListAdapter;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;

public class AlertViewerActivity extends ListActivity {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alertviewer);
        
        // get location provider
        try {
        	LocationProviderFactory.initialize(getAssets().open(LOCATIONSXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
		}
        LocationProvider lP = LocationProviderFactory.newLocationProvider();
        
        Bundle extras = getIntent().getExtras();
        // extract extras
        ArrayList<String> dates = extras.getStringArrayList(EXTRA_DATES);
        String mealName = extras.getString(EXTRA_MEALNAME);
        String query = extras.getString(EXTRA_ALERTQUERY);
        ArrayList<Integer> locIds = extras.getIntegerArrayList(EXTRA_LOCATIONIDS);
        ArrayList<Integer> itemsPerLoc = extras.getIntegerArrayList(EXTRA_ITEMSPERLOC);
        ArrayList<String> itemNames = extras.getStringArrayList(EXTRA_ITEMNAMES);
        
        // get location list
        ArrayList<Location> locs = new ArrayList<Location>(locIds.size());
        for (int id : locIds)
        	locs.add(lP.getById(id));
        
        // set adapter
        AlertViewerListAdapter adapter = new AlertViewerListAdapter(this,
        		locs, itemsPerLoc, itemNames, mealName, dates);
        setListAdapter(adapter);
        
        // set title
        ActionBar aB = getActionBar();
        aB.setTitle(query + ": " + itemNames.size() + " items");
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		Intent vbl = new Intent(this, ViewByLocation.class);
		AlertViewerListAdapter adapter = (AlertViewerListAdapter) l.getAdapter();
		int locId = (Integer) adapter.getItem(position);
		vbl.putExtra(EXTRA_LOCATIONID, locId);
		vbl.putExtra(EXTRA_DATE, adapter.getDate(locId));
		
		startActivity(vbl);
	}
	
}