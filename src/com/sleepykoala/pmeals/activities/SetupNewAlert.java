package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTHOUR;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTMINUTE;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTNUM;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTQUERY;
import static com.sleepykoala.pmeals.data.C.EXTRA_ALERTREPEAT;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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
		query = query.trim();
		if (query.isEmpty()) {
			Toast.makeText(this, "Please enter an alert query", Toast.LENGTH_SHORT).show();
			return;
		}
		result.putExtra(EXTRA_ALERTQUERY, query);
		int repeat =
				(((CheckBox) findViewById(R.id.sunday)).isChecked()    ? 1 << 0 : 0) |
				(((CheckBox) findViewById(R.id.monday)).isChecked()    ? 1 << 1 : 0) |
				(((CheckBox) findViewById(R.id.tuesday)).isChecked()   ? 1 << 2 : 0) |
				(((CheckBox) findViewById(R.id.wednesday)).isChecked() ? 1 << 3 : 0) |
				(((CheckBox) findViewById(R.id.thursday)).isChecked()  ? 1 << 4 : 0) |
				(((CheckBox) findViewById(R.id.friday)).isChecked()    ? 1 << 5 : 0) |
				(((CheckBox) findViewById(R.id.saturday)).isChecked()  ? 1 << 6 : 0)
		;
		result.putExtra(EXTRA_ALERTREPEAT, repeat);
		if (repeat == 0) {
			Toast.makeText(this, "Please select at least one day to show alert", Toast.LENGTH_SHORT).show();
			return;
		}
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