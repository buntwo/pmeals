package com.sleepykoala.pmeals.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

import com.sleepykoala.pmeals.R;

public class FirstTimeFragment extends DialogFragment {
	
	private OnFirstTimeDismissListener mListener;

	public interface OnFirstTimeDismissListener {
		public void launch();
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
			mListener = (OnFirstTimeDismissListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(String.format("%s must implement OnFirstTimeDismissListener", activity.toString()));
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_Dialog_NoFrame);
    	
    	builder.setTitle("First Time")
    		   .setCancelable(true)
    		   .setView(getActivity().getLayoutInflater().inflate(R.layout.firsttime, null))
    		   .setNeutralButton("OK!", new DialogInterface.OnClickListener() {
    			   public void onClick(DialogInterface dialog, int item) {
    				   dialog.dismiss();
    				   mListener.launch();
    			   }
    		   });

    	return builder.create();
	}
}