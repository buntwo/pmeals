package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAME;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.app.SearchManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.adapters.SearchResultsListAdapter;
import com.sleepykoala.pmeals.contentprovider.MenuProvider;
import com.sleepykoala.pmeals.data.PMealsDatabase;

public class MealSearcher extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	
	private SearchResultsListAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_mealsearcher);
		
		// Get the intent, verify the action and get the query
		Intent intent = getIntent();
		if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
			String query = intent.getStringExtra(SearchManager.QUERY);
			// set title
			((TextView) findViewById(R.id.searchresults_title)).setText("Search Results: " + query);

			performSearch(query);
		}
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
	    
		return true;
	}
	
	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		// start VBL
		Intent intent = new Intent(this, ViewByLocation.class);
		intent.putExtra(EXTRA_LOCATIONID, (int) id);
		intent.putExtra(EXTRA_DATE, mAdapter.getDateString(position));
		startActivity(intent);
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