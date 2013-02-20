package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.ALPHA_DISABLED;
import static com.sleepykoala.pmeals.data.C.ALPHA_ENABLED;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTHOUR;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTMINUTE;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTREPEAT;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;

public class ManageAlertsActivity extends Activity {
	
	//private static final String TAG = "ManageAlertsActivity";
	
	private LinearLayout container;
	private LayoutInflater mInflater;
	
	private static final String[] dayAbbrevs = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
	private static final int EVERY_DAY = 127;
	private static final int WEEKDAYS = 62;
	private static final int WEEKENDS = 65;
	private static final int COLOR_DIM = 0xff8a8a8a;
	private static final int COLOR_ACTIVE = 0xffffffff;
	private static final int REQ_NEW = 0;
	private static final int REQ_EDIT = 1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_managealerts);
        
        PMealsPreferenceManager.initialize(this);
        
        container = (LinearLayout) findViewById(R.id.alertcontainer);
        mInflater = getLayoutInflater();
        int numAlerts = PMealsPreferenceManager.getNumAlerts();
        if (numAlerts == 0) {
        	TextView tv = (TextView) mInflater.inflate(R.layout.no_alert, null);
        	container.addView(tv);
        } else {
        	for (int i = 1; i <= numAlerts; ++i)
        		newAlertView(
        				PMealsPreferenceManager.getAlertQuery(i),
        				PMealsPreferenceManager.getAlertOn(i),
        				PMealsPreferenceManager.getAlertRepeat(i),
        				PMealsPreferenceManager.getAlertHour(i),
        				PMealsPreferenceManager.getAlertMinute(i));
        }
	}

	public void addAlert(View v) {
		Intent add = new Intent(this, SetupNewAlert.class);
		add.putExtra(EXTRA_ALERTNUM, PMealsPreferenceManager.getNumAlerts() + 1);
		startActivityForResult(add, REQ_NEW);
	}
	
	public void onActivityResult(int reqCode, int resCode, Intent data) {
		if (resCode != RESULT_OK)
			return;
		
		if (reqCode == REQ_NEW) {
			newAlertView(
					data.getStringExtra(EXTRA_ALERTQUERY),
					true,
					data.getIntExtra(EXTRA_ALERTREPEAT, 0),
					data.getIntExtra(EXTRA_ALERTHOUR, 0),
					data.getIntExtra(EXTRA_ALERTMINUTE, 0));
		} else if (reqCode == REQ_EDIT) {
			int num = data.getIntExtra(EXTRA_ALERTNUM, 0);
			LinearLayout alert = (LinearLayout) container.getChildAt(num - 1);
			updateAlertView(alert, data.getStringExtra(EXTRA_ALERTQUERY),
					data.getIntExtra(EXTRA_ALERTREPEAT, 0),
					data.getIntExtra(EXTRA_ALERTHOUR, 0),
					data.getIntExtra(EXTRA_ALERTMINUTE, 0));
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
    
    private void newAlertView(String query, boolean isOn, int repeat,
    		int hour, int min) {
    	if (PMealsPreferenceManager.getNumAlerts() == 1)
    		container.removeAllViews();
    	int num = container.getChildCount() + 1;
    	
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
    	iv.setAlpha(isOn ? ALPHA_ENABLED : ALPHA_DISABLED);
    	iv.setTag(num);
    	iv.setOnTouchListener(new AlertStatusTouchListener());

    	container.addView(alert);
    	
    	updateAlertView(alert, query, repeat, hour, min);
    }
    
    private void updateAlertView(LinearLayout alert, String query,
    		int repeat, int hour, int min) {
    	// build text
    	((TextView) alert.findViewById(R.id.alertquery)).setText(query);
    	StringBuilder info;
    	if (repeat == EVERY_DAY)
    		info = new StringBuilder("Every day");
    	else if (repeat == WEEKDAYS)
    		info = new StringBuilder("Weekdays");
    	else if (repeat == WEEKENDS)
    		info = new StringBuilder("Weekends");
    	else {
    		info = new StringBuilder();
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
    }
    
    public void exitDeleteMode(View v) {
    	int numAlerts = PMealsPreferenceManager.getNumAlerts();
    	for (int i = 0; i < numAlerts; ++i) {
    		View alert = container.getChildAt(i);
    		alert.findViewById(R.id.deletealert).setVisibility(View.GONE);
    		((TextView) alert.findViewById(R.id.alertquery)).setTextColor(COLOR_ACTIVE);
    	}
    	findViewById(R.id.addalert).setVisibility(View.VISIBLE);
    	findViewById(R.id.donedeleting).setVisibility(View.GONE);
    }
    
	public void delete(View v) {
		deleteAlert((Integer) v.getTag());
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
				((ImageView) v).setAlpha( isOn ? ALPHA_ENABLED : ALPHA_DISABLED);
				PMealsPreferenceManager.setAlertOn(num, isOn);
				return true;
			default:
				return false;
			}
		}
		
	}
    
    private class DeleteListener implements OnLongClickListener {

    	// enter delete mode
    	// dim title, make trash cans visible
		public boolean onLongClick(View v) {
			int numAlerts = PMealsPreferenceManager.getNumAlerts();
			for (int i = 0; i < numAlerts; ++i) {
				View alert = container.getChildAt(i);
				alert.findViewById(R.id.deletealert).setVisibility(View.VISIBLE);
				((TextView) alert.findViewById(R.id.alertquery)).setTextColor(COLOR_DIM);
			}
			findViewById(R.id.addalert).setVisibility(View.GONE);
			findViewById(R.id.donedeleting).setVisibility(View.VISIBLE);
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
