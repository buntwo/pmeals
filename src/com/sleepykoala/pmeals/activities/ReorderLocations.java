package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONIDS;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_LOCATIONORDER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;

public class ReorderLocations extends Activity {
	
	// data
	private ArrayList<Integer> locIDs;
	private ArrayList<Integer> origLocIDs;
	private ArrayList<String> locNames;
	private ArrayList<String> origLocNames;
	// views
	private ArrayList<TextView> views;
	
	private int numLocs;

    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reorderlocations);
        
        // build data
		SharedPreferences prefs = getSharedPreferences(PREFSFILENAME, 0);
		Set<String> locs = prefs.getStringSet(PREF_LOCATIONORDER, null);
		// retrieve them in the correct order
		ArrayList<String> locIDsRaw = new ArrayList<String>(locs);
		Collections.sort(locIDsRaw);
		locIDs = new ArrayList<Integer>();
		for (String s : locIDsRaw)
			locIDs.add(Integer.valueOf(s.substring(2)));
        
        locNames = new ArrayList<String>();
        numLocs = locIDs.size();
        LocationProvider lP = LocationProviderFactory.newLocationProvider();
		for (int id : locIDs)
			locNames.add(lP.getById(id).locName);
		// save originals for resetting
		origLocIDs = (ArrayList<Integer>) locIDs.clone();
		origLocNames = (ArrayList<String>) locNames.clone();
        
        LinearLayout container = (LinearLayout) findViewById(R.id.reorder_container);
        container.setShowDividers(LinearLayout.SHOW_DIVIDER_BEGINNING |
        		LinearLayout.SHOW_DIVIDER_MIDDLE |
        		LinearLayout.SHOW_DIVIDER_END);
        
		// build TextViews
        views = new ArrayList<TextView>();
        for (int i = 0; i < numLocs; ++i)
        	locNames.add("Number " + (i + 1));
        LayoutInflater inflater = getLayoutInflater();
        for (int i = 0; i < numLocs; ++i) {
        	LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.reorder_name, null);
        	TextView tv = (TextView) ll.findViewById(R.id.reorder_name);
        	ImageView iv = (ImageView) ll.findViewById(R.id.reorder_selector);
        	tv.setText(locNames.get(i));
        	ll.setOnTouchListener(new ReorderTouchListener(i));
        	ll.setOnDragListener(new ReorderDragListener(i));
        	tv.setTag(iv);
        	
        	container.addView(ll);
        	views.add(tv);
        }
        
    }
    
    //-------------------------------------------------BUTTON CALLBACKS-----------------------------------------------
    
    public void done(View v) {
    	Bundle result = new Bundle();
    	result.putIntegerArrayList(EXTRA_LOCATIONIDS, locIDs);
    	Intent intent = new Intent();
    	intent.putExtras(result);
    	setResult(RESULT_OK, intent);
    	finish();
    }
    
    public void reset(View v) {
    	locIDs.clear();
    	locNames.clear();
    	locIDs.addAll(origLocIDs);
    	locNames.addAll(origLocNames);
    	// redraw
    	for (int i = 0; i < numLocs; ++i)
    		views.get(i).setText(locNames.get(i));
    }
    
    public void cancel(View v) {
    	setResult(RESULT_CANCELED);
    	finish();
    }
    
    //-------------------------------------------------DRAG/TOUCH LISTENERS-------------------------------------------
    
    private class ReorderTouchListener implements OnTouchListener {
    	int num;
    	
    	public ReorderTouchListener(int num) {
    		this.num = num;
    	}

		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				DragShadowBuilder builder = new View.DragShadowBuilder();
				v.startDrag(null, builder, num, 0);
				return true;
				
			default:
				return false;
			}
		}
    }
    
    private class ReorderDragListener implements OnDragListener {
    	int num;
    	
    	public ReorderDragListener(int num) {
    		this.num = num;
    	}

		public boolean onDrag(View v, DragEvent event) {
			int action = event.getAction();
			int dragging = (Integer)event.getLocalState();
			switch (action) {
			case DragEvent.ACTION_DRAG_STARTED:
				if (dragging != num) // clear icon
					v.findViewById(R.id.reorder_selector).setVisibility(View.INVISIBLE);
				else // bold text
					views.get(num).setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
				break;
			case DragEvent.ACTION_DRAG_ENTERED:
				// redraw data
				dragging = (Integer)event.getLocalState();
				int offset = 0;
				if (dragging <= num) {
					for (int i = 0; i < numLocs; ++i) {
						if (dragging == i)
							++offset;
						if (i == num) {
							views.get(num).setText(locNames.get(dragging));
							views.get(num).setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
							v.findViewById(R.id.reorder_selector).setVisibility(View.VISIBLE);
							--offset;
							continue;
						}
						views.get(i).setText(locNames.get(i + offset));
					}
				} else {
					for (int i = 0; i < numLocs; ++i) {
						if (i == num) {
							views.get(num).setText(locNames.get(dragging));
							views.get(num).setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
							v.findViewById(R.id.reorder_selector).setVisibility(View.VISIBLE);
							--offset;
							continue;
						}
						views.get(i).setText(locNames.get(i + offset));
						if (dragging == i)
							++offset;
					}
				}
				break;
			case DragEvent.ACTION_DRAG_EXITED:
				v.findViewById(R.id.reorder_selector).setVisibility(View.INVISIBLE);
				views.get(num).setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				break;
			case DragEvent.ACTION_DROP:
				dragging = (Integer) event.getLocalState();
				locNames.add(num, locNames.remove(dragging));
				locIDs.add(num, locIDs.remove(dragging));
				break;
			case DragEvent.ACTION_DRAG_ENDED:
				// clear icon
				v.findViewById(R.id.reorder_selector).setVisibility(View.VISIBLE);
				// unbold
				views.get(num).setTypeface(Typeface.defaultFromStyle(Typeface.NORMAL));
				break;
				
			}
			return true;
		}
    	
    }
    
}
