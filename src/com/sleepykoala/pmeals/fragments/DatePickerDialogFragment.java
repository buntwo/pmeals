package com.sleepykoala.pmeals.fragments;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.widget.DatePicker;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.Date;

public class DatePickerDialogFragment extends DialogFragment {
	
	private OnDateSelectedListener mListener;
	
	// activity interface, called in onDateSet
	public interface OnDateSelectedListener {
		public void dateSelected(Date date);
	}
	
	public static DatePickerDialogFragment newInstance(String date) {
		DatePickerDialogFragment f = new DatePickerDialogFragment();
		
		// supply date as arg
		Bundle args = new Bundle();
		args.putString(EXTRA_DATE, date);
		f.setArguments(args);
		
		return f;
	}
	
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
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Date today = new Date(getArguments().getString(EXTRA_DATE));
		DatePickerDialog dialog = new DatePickerDialog(getActivity(),
				R.style.Theme_Dialog_NoFrame, null,
				today.year, today.month, today.monthDay);
		dialog.getDatePicker().setCalendarViewShown(true);
		dialog.getDatePicker().setSpinnersShown(false);
		final DatePicker picker = dialog.getDatePicker();
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(true);
		dialog.setButton(DialogInterface.BUTTON_POSITIVE, "Select", new OnClickListener() {
			
			public void onClick(DialogInterface dialog, int which) {
				picker.clearFocus();
				mListener.dateSelected(new Date(picker.getMonth(), picker.getDayOfMonth(), picker.getYear()));
			}
		});
		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (OnClickListener) null);
		
		dialog.setTitle("Pick a date");
		return dialog;
	}
	
}
