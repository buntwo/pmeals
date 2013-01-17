package com.sleepykoala.pmeals;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class FirstTimeFragment extends DialogFragment {

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.Theme_Dialog_NoFrame);
    	
    	builder.setTitle("First Time")
    		   .setCancelable(true)
    		   .setView(getActivity().getLayoutInflater().inflate(R.layout.firsttime, null))
    		   .setNeutralButton("OK!", new DialogInterface.OnClickListener() {
    			   public void onClick(DialogInterface dialog, int which) {
    				   dialog.cancel();
    			   }
    		   })
    		   ;
    	
    	return builder.create();
	}
}
