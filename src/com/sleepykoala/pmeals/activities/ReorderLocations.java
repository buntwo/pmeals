package com.sleepykoala.pmeals.activities;

import java.util.ArrayList;

import android.app.Activity;
import android.os.Bundle;
import android.view.DragEvent;
import android.view.LayoutInflater;
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
        for (int i = 0; i < numLocs; ++i) {
        	LinearLayout ll = (LinearLayout) inflater.inflate(R.layout.reorder_name, null);
        	TextView tv = (TextView) ll.findViewById(R.id.reorder_name);
        	container.addView(ll);
        	views.add(tv);
        	tv.setText(locNames.get(i));
        	ll.setOnTouchListener(new ReorderTouchListener(i));
        	ll.setOnDragListener(new ReorderDragListener(i));
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
    	private final int num;
    	private final TextView tv;
    	
    	public ReorderDragListener(int num) {
    		this.num = num;
    		tv = views.get(num);
    	}

		public boolean onDrag(View v, DragEvent event) {
			int action = event.getAction();
			int dragging = (Integer) event.getLocalState();
			switch (action) {
			case DragEvent.ACTION_DRAG_STARTED:
				if (dragging != num) // not selected one
					tv.setTextColor(LIGHT_GRAY);
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
							tv.setText(locNames.get(dragging));
							tv.setTextColor(WHITE);
							--offset;
							continue;
						} else {
							TextView text = views.get(i);
							text.setText(locNames.get(i + offset));
							text.setTextColor(LIGHT_GRAY);
						}
					}
				} else {
					for (int i = 0; i < numLocs; ++i) {
						if (i == num) {
							tv.setText(locNames.get(dragging));
							tv.setTextColor(WHITE);
							--offset;
							continue;
						} else {
							TextView text = views.get(i);
							text.setText(locNames.get(i + offset));
							text.setTextColor(LIGHT_GRAY);
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
				tv.setTextColor(WHITE);
				break;
				
			}
			return true;
		}
    	
    }
    
}