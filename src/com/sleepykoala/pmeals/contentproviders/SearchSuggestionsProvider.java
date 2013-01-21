package com.sleepykoala.pmeals.contentproviders;

import android.content.SearchRecentSuggestionsProvider;

public class SearchSuggestionsProvider extends SearchRecentSuggestionsProvider {
	
	public static final String AUTHORITY = "com.sleepykoala.pmeals.contentproviders.SearchSuggestionsProvider";
	public static final int MODE = DATABASE_MODE_QUERIES;
	
	public SearchSuggestionsProvider() {
		setupSuggestions(AUTHORITY, MODE);
	}

}
