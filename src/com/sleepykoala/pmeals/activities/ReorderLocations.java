package com.sleepykoala.pmeals.activities;

import java.util.ArrayList;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.DragShadowBuilder;
import android.view.View.OnDragListener;
import android.view.View.OnTouchListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;

public class ReorderLocations extends Activity {
	
	// data
	private ArrayList<Integer> locIDs;
	private ArrayList<Integer> origLocIDs;
	private ArrayList<String> locNames;
	private ArrayList<String> origLocNames;
	// views
	private ArrayList<TextView> views;
	// light gray color
	private int LIGHT_GRAY;
	private int WHITE;
	
	private int numLocs;

    @SuppressWarnings("unchecked")
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reorderlocations);
        
        LIGHT_GRAY = getResources().getColor(R.color.light_gray);
        WHITE = 0xffffffff;
        locIDs = PMealsPreferenceManager.getLocIds();
        
        locNames = new ArrayList<String>();
        numLocs = locIDs.size();
        LocationProvider lP = LocationProviderFactory.newLocationProvider();
		for (int id : locIDs)
			locNames.add(lP.getById(id).nickname);
		// save originals for resetting
		origLocIDs = (ArrayList<Integer>) locIDs.clone();
		origLocNames = (ArrayList<String>) locNames.clone();
        
        LinearLayout container = (LinearLayout) findViewById(R.id.reorder_container);
        
		// build TextViews
        views = new ArrayList<TextView>();
        LayoutInflater inflater = getLayoutInflater();
        ReorderTouchListener rTL = new ReorderTouchListener();
        ReorderDragListener rDL = new ReorderDragListener();
        for (int i = 0; i < numLocs; ++i) {
        	TextView tv = (TextView) inflater.inflate(R.layout.reorder_name, null);
        	container.addView(tv);
        	views.add(tv);
        	tv.setTag(i);
        	tv.setText(locNames.get(i));
        	tv.setOnTouchListener(rTL);
        	tv.setOnDragListener(rDL);
        }
        ((LinearLayout) findViewById(R.id.buttons)).setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        
        ActionBar aB = getActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case android.R.id.home:
    		finish();
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }
    //-------------------------------------------------BUTTON CALLBACKS-----------------------------------------------
    
    public void done(View v) {
    	PMealsPreferenceManager.storeLocIds(locIDs);
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
    	finish();
    }
    
    //-------------------------------------------------DRAG/TOUCH LISTENERS-------------------------------------------
    
    private class ReorderTouchListener implements OnTouchListener {
    	
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();
			
			switch (action) {
			case MotionEvent.ACTION_DOWN:
				DragShadowBuilder builder = new View.DragShadowBuilder();
				v.startDrag(null, builder, v.getTag(), 0);
				return true;
				
			default:
				return false;
			}
		}
    }
    
    private class ReorderDragListener implements OnDragListener {

		public boolean onDrag(View v, DragEvent event) {
			int action = event.getAction();
			int num = (Integer) v.getTag(); // the number we are dragging
			int dragging = (Integer) event.getLocalState();
			switch (action) {
			case DragEvent.ACTION_DRAG_STARTED:
				for (int i = 0; i < numLocs; ++i)
					if (dragging != i)
						views.get(i).setTextColor(LIGHT_GRAY);
				break;
			case DragEvent.ACTION_DRAG_ENTERED:
				// redraw data
				int offset = 0;
				if (dragging <= num) {
					for (int i = 0; i < numLocs; ++i) {
						if (dragging == i)
							++offset;
						TextView tv = views.get(i);
						if (i == num) {
							tv.setText(locNames.get(dragging));
							tv.setTextColor(WHITE);
							--offset;
							continue;
						} else {
							tv.setText(locNames.get(i + offset));
							tv.setTextColor(LIGHT_GRAY);
						}
					}
				} else {
					for (int i = 0; i < numLocs; ++i) {
						TextView tv = views.get(i);
						if (i == num) {
							tv.setText(locNames.get(dragging));
							tv.setTextColor(WHITE);
							--offset;
							continue;
						} else {
							tv.setText(locNames.get(i + offset));
							tv.setTextColor(LIGHT_GRAY);
							if (dragging == i)
								++offset;
						}
					}
				}
				break;
			case DragEvent.ACTION_DRAG_EXITED:
				break;
			case DragEvent.ACTION_DROP:
				dragging = (Integer) event.getLocalState();
				locNames.add(num, locNames.remove(dragging));
				locIDs.add(num, locIDs.remove(dragging));
				break;
			case DragEvent.ACTION_DRAG_ENDED:
				for (int i = 0; i < numLocs; ++i)
					views.get(i).setTextColor(WHITE);
				break;
			}
			return true;
		}
    	
    }
    
}