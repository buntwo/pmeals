package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTLOC;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUM;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.app.TimePickerDialog.OnTimeSetListener;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.Time;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.C;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;
import com.sleepykoala.pmeals.services.AlertService;

public class SetupNewAlert extends Activity implements
		OnMultiChoiceClickListener, OnClickListener, OnCancelListener,
		OnItemSelectedListener, OnCheckedChangeListener {

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
	private static final int BREAKFAST_DEFAULTTIME = 540; // 9:00am
	private static final int LUNCH_DEFAULTTIME = 690; // 11:30am
	private static final int DINNER_DEFAULTTIME = 1020; // 5:00pm
	
	private ArrayList<Location> locs;
	private ArrayList<Boolean> checkedLocs;
	private ArrayList<Integer> times;
	
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
	// cache fadeout animation
	private Animation fadeout;
	
	private LayoutInflater mInflater;
	
	private LinearLayout locsContainer;
	private LinearLayout timeContainer;

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
        
        mInflater = getLayoutInflater();
		
		// cache days of week
		sun = (TextView) findViewById(R.id.sunday);
		mon = (TextView) findViewById(R.id.monday);
		tue = (TextView) findViewById(R.id.tuesday);
		wed = (TextView) findViewById(R.id.wednesday);
		thu = (TextView) findViewById(R.id.thursday);
		fri = (TextView) findViewById(R.id.friday);
		sat = (TextView) findViewById(R.id.saturday);
		// cache fadeout animation
		fadeout = AnimationUtils.loadAnimation(this, R.anim.fadeout);

		// set title back button
		ActionBar aB = getActionBar();
		aB.setDisplayHomeAsUpEnabled(true);
		
		Intent intent = getIntent();
		alertNum = intent.getIntExtra(EXTRA_ALERTNUM, -1);
		// set query
		String query = PMealsPreferenceManager.getAlertQuery(alertNum);
		if (query.equals(""))
			aB.setTitle("New alert");
		else
			aB.setTitle("Edit alert: " + query);
		((EditText) findViewById(R.id.alertquery)).setText(query);
		// set repeat
		int repeat = PMealsPreferenceManager.getAlertRepeat(alertNum);
		checkedDays = new boolean[7];
		checkedDaysBuff = new boolean[7];
		for (int i = 0; i < 7; ++i)
			checkedDays[i] = (((repeat >> i) & 1) == 1);
		drawDaysOfWeek();
		// set time
		timeContainer = (LinearLayout) findViewById(R.id.timepicker_container);
		ArrayList<String> mealNames = new ArrayList<String>();
		times = new ArrayList<Integer>();
		PMealsPreferenceManager.getAlertMeal_Times(alertNum, mealNames, times);
		int numTimes = times.size();
		if (numTimes == 0) {
			RelativeLayout alertTime = (RelativeLayout) mInflater.inflate(R.layout.newalert_timepicker, null);
			alertTime.setTag(0);
			timeContainer.addView(alertTime);
			// setup meal spinner
			Spinner alertMeals = (Spinner) alertTime.findViewById(R.id.alertmeal);
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.alert_meals, android.R.layout.simple_spinner_item);
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			alertMeals.setAdapter(adapter);
			alertMeals.setSelection(3);
			alertMeals.setOnItemSelectedListener(this);
			Time tm = new Time();
			tm.setToNow();
			setAlertTitle(0, tm.hour, tm.minute);
			times.add(tm.hour * 60 + tm.minute);
		} else {
			for (int i = 0; i < numTimes; ++i) {
				RelativeLayout alertTime = (RelativeLayout) mInflater.inflate(R.layout.newalert_timepicker, null);
				// set tag
				// 0-indexed
				alertTime.setTag(i);
				timeContainer.addView(alertTime);
				// set title
				int time = times.get(i);
				setAlertTitle(i, time / 60, time % 60);
				// setup meal spinner
				Spinner alertMeals = (Spinner) alertTime.findViewById(R.id.alertmeal);
				ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.alert_meals, android.R.layout.simple_spinner_item);
				adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
				alertMeals.setAdapter(adapter);
				String mealname = mealNames.get(i);
				if (mealname.equals(""))
					alertMeals.setSelection(3);
				else if (mealname.equals("Breakfast"))
					alertMeals.setSelection(0);
				else if (mealname.equals("Lunch"))
					alertMeals.setSelection(1);
				else if (mealname.equals("Dinner"))
					alertMeals.setSelection(2);
				alertMeals.setOnItemSelectedListener(this);
			}
		}
		// can't delete last one
		if (numTimes <= 1)
			timeContainer.getChildAt(0).findViewById(R.id.deletealerttime).setVisibility(View.GONE);
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
		// get query
		String query = String.valueOf(((TextView) findViewById(R.id.alertquery)).getText());
		query = query.trim();
		if (query.isEmpty()) {
			Toast.makeText(this, "Please enter an alert query", Toast.LENGTH_SHORT).show();
			return;
		}
		// get alert repeat
		int repeat = 0;
		for (int i = 0; i < 7; ++i)
			if (checkedDays[i])
				repeat |= 1 << i;
		if (repeat == 0) {
			Toast.makeText(this, "Please select at least one day to show alert",
					Toast.LENGTH_SHORT).show();
			return;
		}
		// get locations
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
		// get alert times
		int numTimes = timeContainer.getChildCount();
		ArrayList<String> meals = new ArrayList<String>(numTimes);
		for (int i = 0; i < numTimes; ++i) {
			RelativeLayout time = (RelativeLayout) timeContainer.getChildAt(i);
			int selected = ((Spinner) time.findViewById(R.id.alertmeal)).getSelectedItemPosition();
			switch (selected) {
			case 0:
				meals.add("Breakfast");
				break;
			case 1:
				meals.add("Lunch");
				break;
			case 2:
				meals.add("Dinner");
				break;
			case 3:
				meals.add("");
				break;
			}
		}
		
		result.putExtra(EXTRA_ALERTNUM, alertNum);
		// store alert
		PMealsPreferenceManager.storeAlert(alertNum, query, repeat, meals, times, locsSelected);
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
	
	public void onAddAlertTime(View v) {
		RelativeLayout alertTime = (RelativeLayout) mInflater.inflate(R.layout.newalert_timepicker, null);
		timeContainer.addView(alertTime);
		int numAlerts = timeContainer.getChildCount();
		alertTime.setTag(numAlerts - 1);
		// setup meal spinner
		Spinner alertMeals = (Spinner) alertTime.findViewById(R.id.alertmeal);
		ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.alert_meals, android.R.layout.simple_spinner_item);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		alertMeals.setAdapter(adapter);
		alertMeals.setSelection(3);
		alertMeals.setOnItemSelectedListener(this);
		Time tm = new Time();
		tm.setToNow();
		setAlertTitle(numAlerts - 1, tm.hour, tm.minute);
		times.add(tm.hour * 60 + tm.minute);
		// enable previous one's delete button if this is the 2nd alert time
		if (numAlerts == 2)
			timeContainer.getChildAt(0).findViewById(R.id.deletealerttime).setVisibility(View.VISIBLE);
	}
	
	public void onDeleteAlertTime(View v) {
		int numDeleted = (Integer) ((View) v.getParent()).getTag();
		times.remove(numDeleted);
		// shift tags down
		int totalTimes = timeContainer.getChildCount();
		for (int i = numDeleted + 1; i < totalTimes; ++i) {
			RelativeLayout alertTime = (RelativeLayout) timeContainer.getChildAt(i);
			alertTime.setTag(i - 1);
		}
		// can't delete last one
		if (totalTimes == 2)
			timeContainer.getChildAt((numDeleted == 1) ? 0 : 1).findViewById(R.id.deletealerttime).setVisibility(View.GONE);
		fadeout.setAnimationListener(new DeleteAlertAnimationListener(timeContainer, numDeleted));
		timeContainer.getChildAt(numDeleted).startAnimation(fadeout);
	}
	
	public void onSetAlertTimeTime(View v) {
		int timeNum = (Integer) ((View) v.getParent()).getTag();
		int time = times.get(timeNum);
		TimePickerDialog tpd = new TimePickerDialog(this,
				new AlertTimeTimeSetListener(timeNum),
				time / 60, time % 60, C.IS24HOURFORMAT);
		
		tpd.show();
	}

	public void onItemSelected(AdapterView<?> av, View v, int pos,
			long id) {
		int num = (Integer) ((View) av.getParent()).getTag();
		int newTime = 0;
		switch (pos) {
		case 0:
			newTime = BREAKFAST_DEFAULTTIME;
			break;
		case 1:
			newTime = LUNCH_DEFAULTTIME;
			break;
		case 2:
			newTime = DINNER_DEFAULTTIME;
			break;
		case 3:
			Time tm = new Time();
			tm.setToNow();
			newTime = tm.hour * 60 + tm.minute;
		}
		times.set(num, newTime);
		setAlertTitle(num, newTime / 60, newTime % 60);
	}

	public void onNothingSelected(AdapterView<?> arg0) { }
	
	private class DeleteAlertAnimationListener implements AnimationListener {
		private final ViewGroup parent;
		private final int num;
		
		public DeleteAlertAnimationListener(ViewGroup parent, int numDelete) {
			this.parent = parent;
			num = numDelete;
		}

		public void onAnimationEnd(Animation animation) {
			parent.post(new Runnable() {
				public void run() {
					parent.removeViewAt(num);
				}
			});
		}

		public void onAnimationRepeat(Animation animation) { }
		public void onAnimationStart(Animation animation) { }
		
	}
	
	//----------------------------------------------------------------------------------------
	
	private void setAlertTitle(int num, int hour, int min) {
		String timeStr = MealTimeProvider.getFormattedTime(hour, min);
		int len = timeStr.length();
		SpannableString title = new SpannableString(timeStr + " (tap to change)");
		title.setSpan(new StyleSpan(Typeface.BOLD), 0, len, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
		((TextView) timeContainer.getChildAt(num).findViewById(R.id.alerttimetitle))
			.setText(title);
	}
	
	private class AlertTimeTimeSetListener implements OnTimeSetListener {
		
		private final int num;
		
		public AlertTimeTimeSetListener(int num) {
			this.num = num;
		}
		
		public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
			// set data
			times.set(num, hourOfDay * 60 + minute);
			// set UI
			setAlertTitle(num, hourOfDay, minute);
		}

	}
	
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