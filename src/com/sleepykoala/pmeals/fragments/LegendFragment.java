package com.sleepykoala.pmeals.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

import com.sleepykoala.pmeals.R;

public class LegendFragment extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_Dialog_NoFrame);
    	
    	builder.setTitle("Legend")
    		   .setCancelable(true)
    		   .setView(getActivity().getLayoutInflater().inflate(R.layout.legend, null))
    		   ;
    	
    	return builder.create();
	}
}