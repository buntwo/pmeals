package com.buntwo.pmeals;

import static com.buntwo.pmeals.data.C.EXTRA_DATE;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;

import com.buntwo.pmeals.data.Date;

public class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {
	
	private OnDateSelectedListener mListener;
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
			mListener = (OnDateSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(String.format("%s must implement OnDateSelectedListener", activity.toString()));
		}
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Holo_Dialog);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Date today = new Date(getArguments().getString(EXTRA_DATE));
		DatePickerDialog dialog = new DatePickerDialog(getActivity(), this, today.year, today.month, today.monthDay);
		dialog.getDatePicker().setCalendarViewShown(true);
		dialog.getDatePicker().setSpinnersShown(false);
		
		dialog.setTitle("Jump to date");
		return dialog;
	}
	
	public void onDateSet(DatePicker view, int year, int month,
			int dayOfMonth) {
		mListener.jumpToDate(new Date(month, dayOfMonth, year));
	}
	
	
	// activity interface, called in onDateSet
	public interface OnDateSelectedListener {
		public void jumpToDate(Date date);
	}

}
