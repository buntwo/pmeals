package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.ALPHA_DISABLED;
import static com.sleepykoala.pmeals.data.C.ALPHA_ENABLED;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTHOUR;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTMINUTE;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTREPEAT;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;

import java.io.IOException;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;
import com.sleepykoala.pmeals.services.AlertService;

public class ManageAlertsActivity extends Activity {
	
	//private static final String TAG = "ManageAlertsActivity";
	
	private LinearLayout container;
	private LayoutInflater mInflater;
	private LocationProvider lP;
	
	// buttons
	private ImageView addAlert;
	private ImageView delete;
	private ImageView doneDeleting;
	
	private static final String[] dayAbbrevs = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	private static final int EVERY_DAY = 127;
	private static final int WEEKDAYS = 62;
	private static final int WEEKENDS = 65;
	private static final int COLOR_DIM = 0xff8a8a8a;
	private static final int COLOR_SUBACTIVE = 0xffb9b9b9;
	private static final int COLOR_ACTIVE = 0xffffffff;
	private static final int REQ_NEW = 0;
	private static final int REQ_EDIT = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_managealerts);
        
        PMealsPreferenceManager.initialize(this);
        
		PMealsPreferenceManager.initialize(this);
        // get location provider
        try {
        	LocationProviderFactory.initialize(getAssets().open(LOCATIONSXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
		}
        lP = LocationProviderFactory.newLocationProvider();
        
        // cache buttons
        addAlert = (ImageView) findViewById(R.id.addalert);
        delete = (ImageView) findViewById(R.id.delete);
        doneDeleting = (ImageView) findViewById(R.id.donedeleting);
        // set dividers
        ((LinearLayout) findViewById(R.id.buttons)).setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        
        // fill container with stored alerts
        container = (LinearLayout) findViewById(R.id.alertcontainer);
        mInflater = getLayoutInflater();
        int numAlerts = PMealsPreferenceManager.getNumAlerts();
        if (numAlerts == 0) {
        	TextView tv = (TextView) mInflater.inflate(R.layout.no_alert, null);
        	container.addView(tv);
        } else {
        	for (int i = 1; i <= numAlerts; ++i)
        		newAlertView(i);
        }
        
        ActionBar aB = getActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
	}

	public void onActivityResult(int reqCode, int resCode, Intent data) {
		if (resCode != RESULT_OK)
			return;
		
		if (reqCode == REQ_NEW) {
			newAlertView(data.getIntExtra(EXTRA_ALERTNUM, 0));
		} else if (reqCode == REQ_EDIT) {
			int num = data.getIntExtra(EXTRA_ALERTNUM, 0);
			LinearLayout alert = (LinearLayout) container.getChildAt(num - 1);
			updateAlertView(alert, num);
		}
	}
	
    private void deleteAlert(int num) {
    	int numAlerts = PMealsPreferenceManager.getNumAlerts();
    	container.removeViewAt(num - 1);
    	// shift numbers down
    	for (int i = num - 1; i < numAlerts - 1; ++i) {
    		container.getChildAt(i).findViewById(R.id.alertstatus).setTag(i + 1);
    		container.getChildAt(i).findViewById(R.id.deletealert).setTag(i + 1);
    		container.getChildAt(i).findViewById(R.id.alertinfo).setTag(i + 1);
    	}
    	PMealsPreferenceManager.deleteAlert(num);
    	if (numAlerts == 1) {
    		mInflater.inflate(R.layout.no_alert, container);
    		exitDeleteMode(null);
    	}
    }
    
    /**
     * Puts in a new alert view in the container. Call this after you store the new alert
     * 
     * @param num Alert number
     */
    private void newAlertView(int num) {
    	if (PMealsPreferenceManager.getNumAlerts() == 1)
    		container.removeAllViews();
    	
    	LinearLayout alert = (LinearLayout) mInflater.inflate(R.layout.alert, null);
    	
    	// setup alertInfo box
    	View alertInfo = alert.findViewById(R.id.alertinfo);
    	alertInfo.setTag(num);
    	EditListener eL = new EditListener();
    	DeleteListener dL = new DeleteListener();
    	alertInfo.setOnClickListener(eL);
    	alertInfo.setOnLongClickListener(dL);
    	
    	// setup toggle button
    	View deleteButton = alert.findViewById(R.id.deletealert);
    	deleteButton.setTag(num);
    	// set status indicator
    	ImageView iv = (ImageView) alert.findViewById(R.id.alertstatus);
    	iv.setAlpha(PMealsPreferenceManager.getAlertOn(num) ? ALPHA_ENABLED : ALPHA_DISABLED);
    	iv.setTag(num);
    	iv.setOnTouchListener(new AlertStatusTouchListener());

    	container.addView(alert);
    	
    	updateAlertView(alert, num);
    }
    
    /**
     * Updates the given alert LinerLayout with the appropriate info
     * 
     * @param alert Alert view
     * @param num Alert num
     */
    private void updateAlertView(LinearLayout alert, int num) {
    	// build text
    	String query = PMealsPreferenceManager.getAlertQuery(num);
    	int repeat = PMealsPreferenceManager.getAlertRepeat(num);
    	int hour = PMealsPreferenceManager.getAlertHour(num);
    	int min = PMealsPreferenceManager.getAlertMinute(num);
    	// set query
    	((TextView) alert.findViewById(R.id.alertquery)).setText(query);
    	// set info
    	StringBuilder info = new StringBuilder();;
    	if (repeat == EVERY_DAY)
    		info.append("Every day");
    	else if (repeat == WEEKDAYS)
    		info.append("Weekdays");
    	else if (repeat == WEEKENDS)
    		info.append("Weekends");
    	else {
    		for (int j = 0; j < 7; ++j)
    			if (((repeat >> j) & 1) == 1)
    				info.append(dayAbbrevs[j]).append(", ");
    		info.setLength(info.length() - 2);
    	}
    	info.append(" at ");
    	Time tm = new Time();
    	tm.hour = hour;
    	tm.minute = min;
    	info.append(MealTimeProvider.getFormattedTime(tm.toMillis(false)));
    	((TextView) alert.findViewById(R.id.alerttime)).setText(info);
    	// set locations
    	info = new StringBuilder();
    	Set<String> locs = PMealsPreferenceManager.getAlertLocations(num);
    	int diningHallCount = 0;
    	int count = 0;
    	int size = locs.size();
    	String locName = null;
    	String diningHallName = null;
    	if (size == lP.getCount()) {
    		info.append("All locations");
    	} else {
    		for (String s : locs) {
    			++count;
    			Location l = lP.getById(Integer.parseInt(s));
    			if (count == 1)
    				locName = l.nickname;
    			if (l.type == 0 || l.type == 1) { // dining hall or CJL
    				if (diningHallCount == 0)
    					diningHallName = l.nickname;
    				++diningHallCount;
    			}
    		}
    		if (diningHallCount != 0) {
    			if (diningHallCount == 1)
    				info.append(diningHallName);
    			else {
    				if (diningHallCount == lP.getCountByType(0) + lP.getCountByType(1))
    					info.append("All");
    				else
    					info.append(diningHallCount);
    				info.append(" dining halls");
    			}
    			if (diningHallCount != size)
    				info.append(" and ").append(size - diningHallCount).append(" more");
    		} else {
    			info.append(locName);
    			if (size > 1)
    				info.append(" and ").append(locs.size() - 1).append(" more");
    		}
    	}
    	((TextView) alert.findViewById(R.id.alertlocs)).setText(info);
    }
    
    //--------------------------------------BUTTON CALLBACKS-------------------------------
    
    public void addAlert(View v) {
    	Intent add = new Intent(this, SetupNewAlert.class);
    	add.putExtra(EXTRA_ALERTNUM, PMealsPreferenceManager.getNumAlerts() + 1);
    	add.putExtra(EXTRA_ALERTQUERY, "");
    	startActivityForResult(add, REQ_NEW);
    }
    
    /**
     * Enter delete mode, dim colors, turn on trash cans
     */
    public void enterDeleteMode(View v) {
    	int numAlerts = PMealsPreferenceManager.getNumAlerts();
    	if (numAlerts == 0)
    			return;
    	for (int i = 0; i < numAlerts; ++i) {
    		View alert = container.getChildAt(i);
    		alert.findViewById(R.id.deletealert).setVisibility(View.VISIBLE);
    		((TextView) alert.findViewById(R.id.alertquery)).setTextColor(COLOR_DIM);
    		((TextView) alert.findViewById(R.id.alerttime)).setTextColor(COLOR_DIM);
    		((TextView) alert.findViewById(R.id.alertlocs)).setTextColor(COLOR_DIM);
    	}
    	addAlert.setVisibility(View.GONE);
    	delete.setVisibility(View.GONE);
    	doneDeleting.setVisibility(View.VISIBLE);
    }

    /**
     * Exit delete mode, restore colors, change menu item icon back
     */
    public void exitDeleteMode(View v) {
    	int numAlerts = PMealsPreferenceManager.getNumAlerts();
    	for (int i = 0; i < numAlerts; ++i) {
    		View alert = container.getChildAt(i);
    		alert.findViewById(R.id.deletealert).setVisibility(View.GONE);
    		((TextView) alert.findViewById(R.id.alertquery)).setTextColor(COLOR_ACTIVE);
    		((TextView) alert.findViewById(R.id.alerttime)).setTextColor(COLOR_SUBACTIVE);
    		((TextView) alert.findViewById(R.id.alertlocs)).setTextColor(COLOR_SUBACTIVE);
    	}
    	addAlert.setVisibility(View.VISIBLE);
    	delete.setVisibility(View.VISIBLE);
    	doneDeleting.setVisibility(View.GONE);
    }
	
    /**
     * Delete the given alert, click callback
     * 
     * @param v View that fired the delete command
     */
	public void delete(View v) {
		deleteAlert((Integer) v.getTag());
	}
	
	//--------------------------------------------------------------------------------------------
    
	/**
	 * Activates/deactivates the given alert, and sets the next alert
	 * 
	 * @param num Alert number
	 * @param on true to turn it on, false to turn it off
	 */
	private void activateAlert(int num, boolean on) {
		((ImageView) container.getChildAt(num - 1).findViewById(R.id.alertstatus))
			.setAlpha(on ? ALPHA_ENABLED : ALPHA_DISABLED);
		PMealsPreferenceManager.setAlertOn(num, on);
		
		// set alarm
		AlertService.setNextAlert(this);
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

    //-----------------------------------------LISTENERS-----------------------------------

    private class AlertStatusTouchListener implements OnTouchListener {

		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();

			switch (action) {
			case MotionEvent.ACTION_DOWN:
				int num = (Integer) v.getTag();
				boolean isOn = PMealsPreferenceManager.getAlertOn(num);
				isOn ^= true;
				activateAlert(num, isOn);
				return true;
			default:
				return false;
			}
		}
		
	}
    
    private class DeleteListener implements OnLongClickListener {

		public boolean onLongClick(View v) {
			enterDeleteMode(null);
			
			return true;
		}
    	
    }
    
    private class EditListener implements OnClickListener {

		public void onClick(View v) {
			int num = (Integer) v.getTag();
			Intent edit = new Intent(ManageAlertsActivity.this, SetupNewAlert.class);
			edit.putExtra(EXTRA_ALERTNUM, num);
			edit.putExtra(EXTRA_ALERTQUERY, PMealsPreferenceManager.getAlertQuery(num));
			edit.putExtra(EXTRA_ALERTREPEAT, PMealsPreferenceManager.getAlertRepeat(num));
			edit.putExtra(EXTRA_ALERTHOUR, PMealsPreferenceManager.getAlertHour(num));
			edit.putExtra(EXTRA_ALERTMINUTE, PMealsPreferenceManager.getAlertMinute(num));
			
			startActivityForResult(edit, REQ_EDIT);
		}
    	
    }
    
}
