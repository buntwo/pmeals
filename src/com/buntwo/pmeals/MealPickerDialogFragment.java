package com.buntwo.pmeals;

import static com.buntwo.pmeals.data.C.EXTRA_MEALNAMES;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

public class MealPickerDialogFragment extends DialogFragment {
	
	private static final String TAG = "MealPickerDialogFragment";
	
	private OnMealSelectedListener mListener;
	private String[] mMealNames;
	
	public interface OnMealSelectedListener {
		public void onMealSelected(String mealName);
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		try {
			mListener = (OnMealSelectedListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(String.format("%s must implement OnMealSelectedListener", activity.toString()));
		}
	}
	
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mMealNames = getArguments().getStringArray(EXTRA_MEALNAMES);
		Log.d(TAG, String.format("%s, %s", mMealNames[0], mMealNames[1]));
    	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
    	
    	builder.setTitle("Select meal")
    		   .setCancelable(true)
    		   .setItems(mMealNames, new DialogInterface.OnClickListener() {
    			   public void onClick(DialogInterface dialog, int item) {
    				   mListener.onMealSelected(mMealNames[item]);
    			   }
    		   })
    		   ;
    	
    	return builder.create();
	}
	
	
}
