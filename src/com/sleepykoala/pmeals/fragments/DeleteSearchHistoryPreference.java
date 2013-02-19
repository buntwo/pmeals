package com.sleepykoala.pmeals.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.provider.SearchRecentSuggestions;
import android.util.AttributeSet;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.contentproviders.SearchSuggestionsProvider;

public class DeleteSearchHistoryPreference extends DialogPreference {
	
	private final Context ctx;
	
	public DeleteSearchHistoryPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		ctx = context;
		setDialogLayoutResource(R.layout.deletesearchhistory);
		setDialogIcon(R.drawable.ic_alert);
		setNegativeButtonText("Cancel");
		setPositiveButtonText("OK");
	}
	
	public void onClick(DialogInterface dialog, int which) {
		dialog.dismiss();
		if (which == DialogInterface.BUTTON_POSITIVE) {
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(ctx,
					SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
			suggestions.clearHistory();
		}
	}

}
