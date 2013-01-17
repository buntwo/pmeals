package com.sleepykoala.pmeals;

import com.sleepykoala.pmeals.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

public class AboutFragment extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_Dialog_NoFrame);
    	
    	builder.setTitle("About")
    		   .setCancelable(true)
    		   .setView(getActivity().getLayoutInflater().inflate(R.layout.about, null))
    		   
    		   ;
    	
    	return builder.create();
	}
}
