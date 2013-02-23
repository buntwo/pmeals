package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTHOUR;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTLOC;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTMINUTE;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTREPEAT;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;
import com.sleepykoala.pmeals.data.ScrollableTimePicker;
import com.sleepykoala.pmeals.services.AlertService;

public class SetupNewAlert extends Activity implements
		OnMultiChoiceClickListener, OnClickListener, OnCancelListener,
		OnCheckedChangeListener {

	//private static final String TAG = "SetupNewAlert";
	
	private static final String[] daysOfWeek = {
		"Sunday",
		"Monday",
		"Tuesday",
		"Wednesday",
		"Thursday",
		"Friday",
		"Saturday",
	};
	private static final int COLOR_DIM = 0xff5d5d5d;
	private static final int COLOR_ACTIVE = 0xffffffff;
	
	private ArrayList<Location> locs;
	private ArrayList<Boolean> checkedLocs;
	
	private int alertNum;
	private boolean[] checkedDays;
	private boolean[] checkedDaysBuff;
	
	// cache day of week views
	private TextView sun;
	private TextView mon;
	private TextView tue;
	private TextView wed;
	private TextView thu;
	private TextView fri;
	private TextView sat;
	
	private LinearLayout locsContainer;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setupnewalert);

		PMealsPreferenceManager.initialize(this);
        // get location provider
        try {
        	LocationProviderFactory.initialize(getAssets().open(LOCATIONSXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
		}
        LocationProvider lP = LocationProviderFactory.newLocationProvider();
		
		// cache days of week
		sun = (TextView) findViewById(R.id.sunday);
		mon = (TextView) findViewById(R.id.monday);
		tue = (TextView) findViewById(R.id.tuesday);
		wed = (TextView) findViewById(R.id.wednesday);
		thu = (TextView) findViewById(R.id.thursday);
		fri = (TextView) findViewById(R.id.friday);
		sat = (TextView) findViewById(R.id.saturday);

		// set title back button
		ActionBar aB = getActionBar();
		aB.setDisplayHomeAsUpEnabled(true);
		
		Intent intent = getIntent();
		alertNum = intent.getIntExtra(EXTRA_ALERTNUM, -1);
		// set query
		String query = intent.getStringExtra(EXTRA_ALERTQUERY);
		if (query == null)
			aB.setTitle("New alert");
		else
			aB.setTitle("Edit alert: " + query);
		((EditText) findViewById(R.id.alertquery)).setText(query);
		// set repeat
		int repeat = intent.getIntExtra(EXTRA_ALERTREPEAT, 0);
		checkedDays = new boolean[7];
		checkedDaysBuff = new boolean[7];
		for (int i = 0; i < 7; ++i)
			checkedDays[i] = (((repeat >> i) & 1) == 1);
		drawDaysOfWeek();
		// set time
		Time tm = new Time();
		tm.setToNow();
		int hour = intent.getIntExtra(EXTRA_ALERTHOUR, tm.hour);
		int minute = intent.getIntExtra(EXTRA_ALERTMINUTE, tm.minute);
		TimePicker tP = (TimePicker) findViewById(R.id.alerttime);
		tP.setCurrentHour(hour);
		tP.setCurrentMinute(minute);
		// populate and set locs container
		locsContainer = (LinearLayout) findViewById(R.id.alertlocs_container);
		locs = lP.getAllLocations();
		Set<String> alertLocs = PMealsPreferenceManager.getAlertLocations(alertNum);
		checkedLocs = new ArrayList<Boolean>(locs.size());
		for (int i = 0; i < locs.size(); ++i) {
			checkedLocs.add(false);
			CheckBox ch = new CheckBox(this);
			ch.setText(locs.get(i).nickname);
			ch.setOnCheckedChangeListener(this);
			ch.setTag(i);
			locsContainer.addView(ch);
		}
		if (alertLocs != null)
			for (String s : alertLocs) {
				int i = Integer.parseInt(s);
				checkedLocs.set(i, true);
				((CheckBox) locsContainer.getChildAt(i)).setChecked(true);
			}
		// check one location if given
		int checkLoc = intent.getIntExtra(EXTRA_ALERTLOC, -1);
		if (checkLoc != -1) {
			checkedLocs.set(checkLoc, true);
			((CheckBox) locsContainer.getChildAt(checkLoc)).setChecked(true);
		}
		// show dividers
		((LinearLayout) findViewById(R.id.buttons))
				.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
		((LinearLayout) findViewById(R.id.alertlocs_buttons))
				.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
	}
	
	//-----------------------------------CALLBACKS/LISTENERS--------------------------------
	
	public void selectDays(View v) {
		System.arraycopy(checkedDays, 0, checkedDaysBuff, 0, 7);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select days to repeat")
			   .setCancelable(true)
			   .setMultiChoiceItems(daysOfWeek, checkedDaysBuff, this)
			   .setPositiveButton("OK", this)
			   .setNegativeButton("Cancel", this);
		
		builder.create().show();
	}

	public void create(View v) {
		// build result
		Intent result = new Intent();
		String query = String.valueOf(((TextView) findViewById(R.id.alertquery)).getText());
		query = query.trim();
		if (query.isEmpty()) {
			Toast.makeText(this, "Please enter an alert query", Toast.LENGTH_SHORT).show();
			return;
		}
		int repeat = 0;
		for (int i = 0; i < 7; ++i)
			if (checkedDays[i])
				repeat |= 1 << i;
		if (repeat == 0) {
			Toast.makeText(this, "Please select at least one day to show alert",
					Toast.LENGTH_SHORT).show();
			return;
		}
		Set<String> locsSelected = new HashSet<String>();
		int size = locs.size();
		for (int i = 0; i < size; ++i)
			if (checkedLocs.get(i))
				locsSelected.add(String.valueOf(locs.get(i).ID));
		if (locsSelected.isEmpty()) {
			Toast.makeText(this, "Please select at least one location to query",
					Toast.LENGTH_SHORT).show();
			return;
		}
		ScrollableTimePicker tP = (ScrollableTimePicker) findViewById(R.id.alerttime);
		int hour = tP.getCurrentHour();
		int min = tP.getCurrentMinute();
		
		result.putExtra(EXTRA_ALERTNUM, alertNum);

		// store alert
		PMealsPreferenceManager.storeAlert(alertNum, query, repeat, hour, min, locsSelected);
		// set next alert
		AlertService.setNextAlert(this);
		
		setResult(RESULT_OK, result);
		finish();
	}

	public void cancel(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}
	
	public void selectDiningHalls(View v) {
		int numLocs = locs.size();
		for (int i = 0; i < numLocs; ++i) {
			Location l = locs.get(i);
			if (l.type == 0 || l.type == 1) { // dining halls or CJL
				checkedLocs.set(i, true);
				((CheckBox) locsContainer.getChildAt(i)).setChecked(true);
			} else {
				checkedLocs.set(i, false);
				((CheckBox) locsContainer.getChildAt(i)).setChecked(false);
			}
		}
	}
	
	public void selectAll(View v) {
		int numLocs = locs.size();
		for (int i = 0; i < numLocs; ++i) {
			checkedLocs.set(i, true);
			((CheckBox) locsContainer.getChildAt(i)).setChecked(true);
		}
	}
	
	public void selectNone(View v) {
		int numLocs = locs.size();
		for (int i = 0; i < numLocs; ++i) {
			checkedLocs.set(i, false);
			((CheckBox) locsContainer.getChildAt(i)).setChecked(false);
		}
	}
	
	// multiclick listener
	public void onClick(DialogInterface dialog, int which, boolean isChecked) {
	}
	
	// positive button listener
	public void onClick(DialogInterface dialog, int which) {
		dialog.dismiss();
		if (which == DialogInterface.BUTTON_POSITIVE) {
			System.arraycopy(checkedDaysBuff, 0, checkedDays, 0, 7);
			drawDaysOfWeek();
		}
	}

	// cancel button listener
	public void onCancel(DialogInterface dialog) {
		dialog.dismiss();
	}


	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		checkedLocs.set((Integer) buttonView.getTag(), isChecked);
	}

	//----------------------------------------------------------------------------------------
	
	// colors the selected days
	private void drawDaysOfWeek() {
		sun.setTextColor(getColor(checkedDays[0]));
		mon.setTextColor(getColor(checkedDays[1]));
		tue.setTextColor(getColor(checkedDays[2]));
		wed.setTextColor(getColor(checkedDays[3]));
		thu.setTextColor(getColor(checkedDays[4]));
		fri.setTextColor(getColor(checkedDays[5]));
		sat.setTextColor(getColor(checkedDays[6]));
	}
	
	private int getColor(boolean checked) {
		return checked ? COLOR_ACTIVE : COLOR_DIM;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case android.R.id.home:
    		cancel(null);
    		return true;
    	default:
    		return false;
    	}
    }

}