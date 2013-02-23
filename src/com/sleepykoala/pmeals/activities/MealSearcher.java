package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAME;
import android.app.ActionBar;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.SearchRecentSuggestions;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.SearchView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.adapters.SearchResultsListAdapter;
import com.sleepykoala.pmeals.contentproviders.MenuProvider;
import com.sleepykoala.pmeals.contentproviders.SearchSuggestionsProvider;
import com.sleepykoala.pmeals.data.PMealsDatabase;

public class MealSearcher extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private SearchResultsListAdapter mAdapter;
	private String query;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mealsearcher);
		
		// Get the intent, verify the action and get the query
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			query = intent.getStringExtra(SearchManager.QUERY);
			// set title
			getActionBar().setTitle("Search results: " + query);
			// save query into recent searches
			SearchRecentSuggestions suggestions = new SearchRecentSuggestions(this,
					SearchSuggestionsProvider.AUTHORITY, SearchSuggestionsProvider.MODE);
			suggestions.saveRecentQuery(query, null);
			// do the search
			performSearch(query);
		}
        ActionBar aB = getActionBar();
        aB.setDisplayHomeAsUpEnabled(true);
	}
	
	private void performSearch(String query) {
		mAdapter = new SearchResultsListAdapter(this, null);
		setListAdapter(mAdapter);
		
		Bundle args = new Bundle();
		args.putString(EXTRA_MEALNAME, query);
		getLoaderManager().initLoader(0, args, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_meal_searcher, menu);
	    
		// Get the SearchView and set the searchable configuration
	    SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
	    SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
	    searchView.setSearchableInfo(searchManager.getSearchableInfo(
	    		new ComponentName(this, MealSearcher.class)
	    		));
	    searchView.setIconifiedByDefault(false); // Do not iconify the widget; expand it by default
	    
        return true;
	}
	
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
    	case android.R.id.home:
    		finish();
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		if (mAdapter.getItemViewType(position) == 1) {
			// start VBL
			Intent intent = new Intent(this, ViewByLocation.class);
			intent.putExtra(EXTRA_LOCATIONID, mAdapter.getLocId(position));
			intent.putExtra(EXTRA_DATE, mAdapter.getDateString(position));
			startActivity(intent);
		}
	}
	
	//------------------------------------------------LOADER CALLBACKS-----------------------------------
	
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		String query = args.getString(EXTRA_MEALNAME);
		query = "%" + query + "%";
		String[] projection = {
				PMealsDatabase._ID,
				PMealsDatabase.ITEMNAME,
				PMealsDatabase.DATE,
				PMealsDatabase.MEALNAME,
				PMealsDatabase.LOCATIONID
				};
		String selection = PMealsDatabase.ITEMNAME + " LIKE ?";
		return new CursorLoader(this, MenuProvider.CONTENT_URI, projection, selection, new String[]{query},
				PMealsDatabase.DATE + " desc, " + PMealsDatabase.LOCATIONID + " asc");
	}

	public void onLoadFinished(Loader<Cursor> l, Cursor c) {
		mAdapter.swapCursor(c);
	}

	public void onLoaderReset(Loader<Cursor> l) {
		mAdapter.swapCursor(null);
	}

}
