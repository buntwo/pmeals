package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTHOUR;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTMINUTE;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.EXTRA_REPEATINGDAYS;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.PMealsPreferenceManager;
import com.sleepykoala.pmeals.data.ScrollableTimePicker;

public class SetupNewAlert extends Activity {

	 private static final String TAG = "SetupNewAlert";
	private int alertNum;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_setupnewalert);

		PMealsPreferenceManager.initialize(this);
		
		Intent intent = getIntent();
		alertNum = intent.getIntExtra(EXTRA_ALERTNUM, -1);
		// setup the views
		((CheckBox) findViewById(R.id.sunday)).setChecked(false);
		((CheckBox) findViewById(R.id.monday)).setChecked(false);
		((CheckBox) findViewById(R.id.tuesday)).setChecked(false);
		((CheckBox) findViewById(R.id.wednesday)).setChecked(false);
		((CheckBox) findViewById(R.id.thursday)).setChecked(false);
		((CheckBox) findViewById(R.id.friday)).setChecked(false);
		((CheckBox) findViewById(R.id.saturday)).setChecked(false);
		((LinearLayout) findViewById(R.id.buttons))
				.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
	}

	public void create(View v) {
		// build result
		Intent result = new Intent();
		String query = String.valueOf(((TextView) findViewById(R.id.alertquery)).getText());
		result.putExtra(EXTRA_ALERTQUERY, query);
		boolean[] repeatDays = { 
				((CheckBox) findViewById(R.id.sunday)).isChecked(),
				((CheckBox) findViewById(R.id.monday)).isChecked(),
				((CheckBox) findViewById(R.id.tuesday)).isChecked(),
				((CheckBox) findViewById(R.id.wednesday)).isChecked(),
				((CheckBox) findViewById(R.id.thursday)).isChecked(),
				((CheckBox) findViewById(R.id.friday)).isChecked(),
				((CheckBox) findViewById(R.id.saturday)).isChecked()
		};
		result.putExtra(EXTRA_REPEATINGDAYS, repeatDays);
		int repeat = 0;
		for (int i = 0; i < 7; ++i) {
			repeat += (repeatDays[i]) ? 1 << i : 0;
		}
		if (repeat == 0)
			cancel(null);
		ScrollableTimePicker tP = (ScrollableTimePicker) findViewById(R.id.alerttime);
		int hour = tP.getCurrentHour();
		int min = tP.getCurrentMinute();
		result.putExtra(EXTRA_ALERTHOUR, hour);
		result.putExtra(EXTRA_ALERTMINUTE, min);
		
		Log.d(TAG, "stored " + query + " " + repeat + " " + hour + ":" + min);
		// store alert
		PMealsPreferenceManager.storeAlert(alertNum, query, repeat, hour, min);

		setResult(RESULT_OK, result);
		finish();
	}

	public void cancel(View v) {
		setResult(RESULT_CANCELED);
		finish();
	}

}