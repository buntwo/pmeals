package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUM;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;

public class ManageAlertsActivity extends Activity {
	
	private LinearLayout container;
	private LayoutInflater mInflater;
	
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
        	mInflater.inflate(R.layout.no_alert, container);
        } else {
        	String[] dayAbbrevs = { "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat" };
        	//RelativeLayout.LayoutParams lparams = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        	for (int i = 1; i <= numAlerts; ++i) {
        		RelativeLayout alertInfo = (RelativeLayout) mInflater.inflate(R.layout.alert, null);
        		//alertInfo.setLayoutParams(lparams);
        		((TextView) alertInfo.findViewById(R.id.alertquery)).setText(PMealsPreferenceManager.getAlertQuery(i));
        		int repeat = PMealsPreferenceManager.getAlertRepeat(i);
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
        		tm.hour = PMealsPreferenceManager.getAlertHour(i);
        		tm.minute = PMealsPreferenceManager.getAlertMinute(i);
        		info.append(MealTimeProvider.getFormattedTime(tm.toMillis(false)));
        		((TextView) alertInfo.findViewById(R.id.alertinfo)).setText(info);
        		container.addView(alertInfo);
        	}
        }
	}
	
	public void addAlert(View v) {
		Intent add = new Intent(this, SetupNewAlert.class);
		add.putExtra(EXTRA_ALERTNUM, PMealsPreferenceManager.getNumAlerts() + 1);
		startActivityForResult(add, 0);
	}
}
