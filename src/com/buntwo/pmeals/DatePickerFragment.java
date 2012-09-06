package com.buntwo.pmeals;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import com.buntwo.pmeals.data.Date;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Date today = new Date();
		
		return null;
	}
	
	public void onDateSet(DatePicker view, int year, int monthOfYear,
			int dayOfMonth) {
		// TODO Auto-generated method stub
		
	}

}
