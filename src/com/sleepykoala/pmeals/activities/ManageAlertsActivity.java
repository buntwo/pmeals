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
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
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
		startActivityForResult(add, 0);
	}
	
	public void onActivityResult(int reqCode, int resCode, Intent data) {
		if (resCode == RESULT_OK) {
			newAlertView(
					data.getStringExtra(EXTRA_ALERTQUERY),
					true,
					data.getIntExtra(EXTRA_ALERTREPEAT, 0),
					data.getIntExtra(EXTRA_ALERTHOUR, 0),
					data.getIntExtra(EXTRA_ALERTMINUTE, 0));
		}
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		
		menu.add(ContextMenu.NONE, (Integer) v.findViewById(R.id.alertstatus).getTag(),
				ContextMenu.NONE, "Delete alert");
	}

    @Override
    public boolean onContextItemSelected(MenuItem item) {
    	int num = item.getItemId();
    	deleteAlert(num);
    	
    	return true;
    }
    
    private void deleteAlert(int num) {
    	int numAlerts = PMealsPreferenceManager.getNumAlerts();
    	container.removeViewAt(num - 1);
    	for (int i = num - 1; i < numAlerts - 1; ++i)
    		container.getChildAt(i).findViewById(R.id.alertstatus).setTag(i);
    	PMealsPreferenceManager.deleteAlert(num);
    	if (numAlerts == 1)
    		mInflater.inflate(R.layout.no_alert, container);
    }
    
    private void newAlertView(String query, boolean isOn, int repeat,
    		int hour, int min) {
    	if (PMealsPreferenceManager.getNumAlerts() == 1)
    		container.removeAllViews();
    	int num = container.getChildCount() + 1;
    	
    	LinearLayout alertInfo = (LinearLayout) mInflater.inflate(R.layout.alert, null);
    	// set status indicator
    	ImageView iv = (ImageView) alertInfo.findViewById(R.id.alertstatus);
    	iv.setAlpha(isOn ? ALPHA_ENABLED : ALPHA_DISABLED);
    	iv.setTag(num);
    	iv.setOnTouchListener(new AlertStatusTouchListener());

    	// build text
    	((TextView) alertInfo.findViewById(R.id.alertquery)).setText(query);
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
    	((TextView) alertInfo.findViewById(R.id.alertinfo)).setText(info);

    	registerForContextMenu(alertInfo);
    	container.addView(alertInfo);
    }

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
    
}
