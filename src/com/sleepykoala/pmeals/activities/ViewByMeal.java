package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.ALERT_FADEIN_TIME;
import static com.sleepykoala.pmeals.data.C.ALPHA_DISABLED;
import static com.sleepykoala.pmeals.data.C.ALPHA_ENABLED;
import static com.sleepykoala.pmeals.data.C.END_ALERT_COLOR;
import static com.sleepykoala.pmeals.data.C.IS24HOURFORMAT;
import static com.sleepykoala.pmeals.data.C.MEALTIMESXML;
import static com.sleepykoala.pmeals.data.C.MEAL_PASSED_COLOR;
import static com.sleepykoala.pmeals.data.C.MINUTES_END_ALERT;
import static com.sleepykoala.pmeals.data.C.MINUTES_START_ALERT;
import static com.sleepykoala.pmeals.data.C.NO_ALERT_COLOR;
import static com.sleepykoala.pmeals.data.C.ONEHOUR_RADIUS;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_FIRSTTIME;
import static com.sleepykoala.pmeals.data.C.PREF_LASTVER;
import static com.sleepykoala.pmeals.data.C.REQCODE_REORDER;
import static com.sleepykoala.pmeals.data.C.START_ALERT_COLOR;
import static com.sleepykoala.pmeals.data.C.VBM_NUMLISTS_AFTER;
import static com.sleepykoala.pmeals.data.C.VBM_NUMLISTS_BEFORE;

import java.io.IOException;
import java.util.ArrayList;

import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.SimpleOnPageChangeListener;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.adapters.MealViewPagerAdapter;
import com.sleepykoala.pmeals.data.C;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.data.PreferenceManager;
import com.sleepykoala.pmeals.data.RgbEvaluator;
import com.sleepykoala.pmeals.fragments.AboutFragment;
import com.sleepykoala.pmeals.fragments.DatePickerDialogFragment;
import com.sleepykoala.pmeals.fragments.DatePickerDialogFragment.OnDateSelectedListener;
import com.sleepykoala.pmeals.fragments.FirstTimeFragment;
import com.sleepykoala.pmeals.fragments.LegendFragment;
import com.sleepykoala.pmeals.fragments.MealPickerDialogFragment;
import com.sleepykoala.pmeals.fragments.MealPickerDialogFragment.OnMealSelectedListener;

public class ViewByMeal extends FragmentActivity implements OnDateSelectedListener, OnMealSelectedListener {
	
    //private static final String TAG = "ViewByMeal";
    
	private static final IntentFilter sIntentFilter;
	// create intent filter
	static {
		sIntentFilter = new IntentFilter();
		sIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
		sIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
		sIntentFilter.addAction(Intent.ACTION_TIME_TICK);
	}
	private final BroadcastReceiver timeChangedReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
			if (action.equals(Intent.ACTION_TIME_CHANGED) ||
					action.equals(Intent.ACTION_TIMEZONE_CHANGED) ||
					action.equals(Intent.ACTION_TIME_TICK)) {
				onTimeChanged();
			}
		}
	};
	
    private MealTimeProvider mTP;
    
    private DatedMealTime mealDisplayed;
    private DatedMealTime currentMeal;
    
    private Date today;
    private ArrayList<Integer> locIDsToShow;
    
    // infobar stuff
    private TextView mealInfoView0;
    private TextView mealInfoView1;
    private FrameLayout infoBar;
    private String mealInfo;
    private int infoBarColor;
    
    private ViewPager mPager;
    private MealViewPagerAdapter mAdapter;
    
    // meal selector cache
    private String selectedDate;
    
    // cached animations
    private Animation dropdown0;
    private Animation dropdown1;
					
    private LinearLayout pageIndicatorsLayout;
    private ImageView[] pageIndicators;
    private Drawable indic_notSelected;
    private Drawable indic_selected;
    private Animation fadeoutAnim;
    private static int TOTAL_NUMLISTS = VBM_NUMLISTS_BEFORE + VBM_NUMLISTS_AFTER + 1;
    
    private boolean isInfoBarMoving;
    
	// food indicator drawables
	public static Drawable vegan;
	public static Drawable vegetarian;
	public static Drawable pork;
	public static Drawable nuts;
	public static Drawable outline;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewbymeal);
        
        // set 24 hour status
        IS24HOURFORMAT = DateFormat.is24HourFormat(this);

        // get meal time provider
		try {
			MealTimeProviderFactory.initialize(getAssets().open(MEALTIMESXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + MEALTIMESXML + "!!");
		}
		mTP = MealTimeProviderFactory.newMealTimeProvider();
		/*
        // get location provider
        try {
        	LocationProviderFactory.initialize(getAssets().open(LOCATIONSXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
		}
        LocationProvider lP = LocationProviderFactory.newLocationProvider();
        */
        
        // cache animations
        dropdown0 = AnimationUtils.loadAnimation(this, R.anim.infobar_dropdown0);
        dropdown0.setAnimationListener(new ResetAnimationListener());
        dropdown1 = AnimationUtils.loadAnimation(this, R.anim.infobar_dropdown1);
        dropdown1.setAnimationListener(new AnimationListener() {
			public void onAnimationRepeat(Animation animation) {}
			public void onAnimationStart(Animation animation) {}
			public void onAnimationEnd(Animation animation) {
				// swap references
				TextView tmp = mealInfoView0;
				mealInfoView0 = mealInfoView1;
				mealInfoView1 = tmp;
			}
        });
        
        // set up page indicators
        pageIndicatorsLayout = (LinearLayout) findViewById(R.id.pageindicators);
        pageIndicators = new ImageView[TOTAL_NUMLISTS];
        for (int i = 0; i < TOTAL_NUMLISTS; ++i) {
        	ImageView iv =  (ImageView) getLayoutInflater().inflate(R.layout.pageindicator, null, false);
        	pageIndicatorsLayout.addView(iv);
        	pageIndicators[i] = iv;
        }
        // cache some drawables
        Resources res = getResources();
        indic_notSelected = res.getDrawable(R.drawable.pageindicator_notselected);
        indic_selected = res.getDrawable(R.drawable.pageindicator_selected);
        // load fadeout animation
        fadeoutAnim = AnimationUtils.loadAnimation(ViewByMeal.this, R.anim.pageindicator_fadeout);
        
        // cache food indicator drawables
		vegan = res.getDrawable(R.drawable.vegan);
		vegetarian = res.getDrawable(R.drawable.vegetarian);
		pork = res.getDrawable(R.drawable.pork);
		nuts = res.getDrawable(R.drawable.nuts);
		outline = res.getDrawable(R.drawable.foodinfo_outline);
		
        // not animating
        isInfoBarMoving = false;
        
        // cache views
		mealInfoView0 = (TextView) findViewById(R.id.infobar_mealinfo0);
		mealInfoView1 = (TextView) findViewById(R.id.infobar_mealinfo1);
		infoBar = (FrameLayout) findViewById(R.id.infobar);
		mPager = (ViewPager) findViewById(R.id.listview_pager);

		// retrieve location order from settings, if exists
		PreferenceManager.initialize(this);
		locIDsToShow = PreferenceManager.getLocIds();
		// setup pager adapter
		// HARD CODED meal type!!
		mAdapter = new MealViewPagerAdapter(locIDsToShow, mTP.getCurrentMeal(0), 0, getSupportFragmentManager());
		
		// set meals
		// HARD CODED meal type!!
		currentMeal = mTP.getCurrentMeal(0);
		mealDisplayed = currentMeal;
		
		// set today
		today = new Date();
		
		// set up viewpager
		mPager.setAdapter(mAdapter);
		mPager.setCurrentItem(mAdapter.getMiddleIndex());
		TitleChangeListener tCL = new TitleChangeListener();
		mPager.setOnPageChangeListener(tCL);
		tCL.onPageSelected(mPager.getCurrentItem());
		
        // set up action bar
        ActionBar aB = getActionBar();
        aB.setTitle("PMeals");
        aB.setDisplayShowTitleEnabled(true);
        
        
        // upgrade code
        // show help dialog on first time or upgrade
        int currentVer = 1;
        try {
			currentVer = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			// better not get here lol
		}
		SharedPreferences prefs = getSharedPreferences(PREFSFILENAME, 0);
        if (prefs.getBoolean(PREF_FIRSTTIME, true) || (prefs.getInt(PREF_LASTVER, 0) < currentVer)) {
    		FirstTimeFragment ftf = new FirstTimeFragment();
    		ftf.show(getFragmentManager(), "firsttime");
    		
    		SharedPreferences.Editor editor = prefs.edit();
        	editor.putBoolean(PREF_FIRSTTIME, false);
        	editor.putInt(PREF_LASTVER, currentVer);
        	editor.commit();
        }
    }
    
    // go to current meal
    public void gotoCurrentMeal() {
    	int currentMealIndex = mAdapter.findMealIndex(currentMeal);
    	if (currentMealIndex != -1) // else, current meal not found?!
    		mPager.setCurrentItem(currentMealIndex, true);
    	else {
    		mAdapter = new MealViewPagerAdapter(locIDsToShow, currentMeal, 0, getSupportFragmentManager());
    		mPager.setAdapter(mAdapter);
    		mPager.setOnPageChangeListener(new TitleChangeListener());
    		mPager.setCurrentItem(mAdapter.getMiddleIndex());
    		startPageIndicatorFadeout();
    	}
    		
    }
    
	// do housekeeping when time ticks
	private void onTimeChanged() {
		DatedMealTime d = mTP.getCurrentMeal(0);
		boolean newMeal = !d.equals(currentMeal);
		Date oldToday = today;
		today = new Date();
		if (newMeal) {
			Intent newMealIntent = new Intent();
			newMealIntent.setAction(C.ACTION_NEW_MEAL);
			LocalBroadcastManager.getInstance(ViewByMeal.this).sendBroadcast(newMealIntent);

			currentMeal = d;
		} else if (!today.equals(oldToday)) { // new day?
				Intent newDayIntent = new Intent();
				newDayIntent.setAction(C.ACTION_NEW_DAY);
				LocalBroadcastManager.getInstance(ViewByMeal.this).sendBroadcast(newDayIntent);
		} else { // only send if others haven't been sent
			// send time changed broadcast to receivers (fragments)
			Intent timeChangedIntent = new Intent();
			timeChangedIntent.setAction(C.ACTION_TIME_CHANGED);
			LocalBroadcastManager.getInstance(ViewByMeal.this).sendBroadcast(timeChangedIntent);
		}
		refreshTitle(newMeal);
	}
	
    // build meal time data text to show in title
    public void refreshTitle(boolean newMeal) {
    	// build mealinfo text
		final StringBuilder newTitleText = new StringBuilder(); 
		int mealStatus = MealTimeProvider.currentMealStatus(mealDisplayed);
		boolean inMeal = (mealStatus < 1) ? false : true;
		int[] timeTo;
		
		boolean nextMealIsNotToday = !today.equals(currentMeal.date);
		
		if (mealStatus == 0) { // meal already happened
			timeTo = MealTimeProvider.getTimeUntilMeal(mealDisplayed, false);
			newTitleText.append("Ended ");
			if ((-timeTo[0] == 1 && -timeTo[1] <= ONEHOUR_RADIUS) ||
					timeTo[0] == 0 && 60 + timeTo[1] <= ONEHOUR_RADIUS) {
				if (timeTo[1] != 0)
					newTitleText.append("about ");
				newTitleText.append("an hour ago");
			} else if (timeTo[0] == 0) {
				if (timeTo[1] == 0)
					newTitleText.append("just now");
				else {
					newTitleText.append(-timeTo[1]);
					newTitleText.append(" minute");
					if (timeTo[1] != -1) // plural
						newTitleText.append("s");
					newTitleText.append(" ago");
				}
			} else {
				newTitleText.append("at ");
				newTitleText.append(MealTimeProvider.getFormattedTime(mealDisplayed.endTime));
			}
		} else {
			timeTo = MealTimeProvider.getTimeUntilMeal(mealDisplayed, !inMeal);
			newTitleText.append((inMeal) ? "Ends " : "Starts ");
			if ((timeTo[0] == 1 && timeTo[1] <= ONEHOUR_RADIUS) ||
					timeTo[0] == 0 && 60 - timeTo[1] <= ONEHOUR_RADIUS) {
				newTitleText.append("in ");
				if (timeTo[1] != 0)
					newTitleText.append("about ");
				newTitleText.append("an hour");
			} else if (timeTo[0] == 0) {
				newTitleText.append("in ");
				newTitleText.append(timeTo[1]);
				newTitleText.append(" minute");
				if (timeTo[1] != 1) // plural
					newTitleText.append("s");
			} else {
				newTitleText.append("at ");
				int[] time;
				if (inMeal)
					time = mealDisplayed.endTime;
				else
					time = mealDisplayed.startTime;
				newTitleText.append(MealTimeProvider.getFormattedTime(time));
			}
			if (mealDisplayed.equals(currentMeal) && nextMealIsNotToday) {
				if (today.isTomorrow(currentMeal.date)) { // next meal is tomorrow
					newTitleText.append(" tomorrow");
				} else { // not tomorrow
					newTitleText.append(" on ");
					newTitleText.append(DateFormat.format("EEEE", currentMeal.date.toMillis(true)));
				}
			}
		}
		mealInfo = newTitleText.toString();
		
		// get new color
		if (mealStatus == 0) {
			infoBarColor = MEAL_PASSED_COLOR;
		} else {
			if (timeTo[0] == 0) {
				if (inMeal && timeTo[1] <= MINUTES_END_ALERT)
					infoBarColor = END_ALERT_COLOR;
				else if (!inMeal && timeTo[1] <= MINUTES_START_ALERT)
					infoBarColor = START_ALERT_COLOR;
				else
					infoBarColor = NO_ALERT_COLOR;
			} else 
				infoBarColor = NO_ALERT_COLOR;
		}
		
		if (newMeal) {
			if (!isInfoBarMoving) {
				// fancy dropdown animation
				mealInfoView0.startAnimation(dropdown0);
				mealInfoView1.startAnimation(dropdown1);
				updateInfoBar();
			} else
				updateInfoBar();
		} else
			updateInfoBar();
    }
    
    private void updateInfoBar() {
    	mealInfoView0.setText(mealInfo);
    	changeInfoBarColor();
    }
    
    private void changeInfoBarColor() {
    	int oldBgColor = ((ColorDrawable) infoBar.getBackground()).getColor();
    	if (!(infoBarColor == oldBgColor)) {
    		// color changing animation
    		ValueAnimator colorAnim = ValueAnimator.ofInt(oldBgColor, infoBarColor);
    		colorAnim.setEvaluator(new RgbEvaluator());
    		colorAnim.setDuration(ALERT_FADEIN_TIME);
    		colorAnim.addUpdateListener(new BackgroundColorUpdateListener());
    		colorAnim.start();
    	}
    }
    
    // start fadeout animation
    private void startPageIndicatorFadeout() {
    	pageIndicatorsLayout.clearAnimation();
    	pageIndicatorsLayout.startAnimation(fadeoutAnim);
    }
    
    //--------------------------------------------------LISTENERS-----------------------------------------------
    
    private class BackgroundColorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
    	public void onAnimationUpdate(ValueAnimator animation) {
    		int bgColor = (Integer) animation.getAnimatedValue();
    		infoBar.setBackgroundColor(bgColor);
    	}
    }
    
    // title animating set to false at animation end
    private class ResetAnimationListener implements AnimationListener {
		public void onAnimationEnd(Animation animation) { isInfoBarMoving = false; }
		public void onAnimationRepeat(Animation animation) {}
		public void onAnimationStart(Animation animation) { isInfoBarMoving = true; }
    }
    
    private class TitleChangeListener extends SimpleOnPageChangeListener {
    	public void onPageScrollStateChanged(int state) {
    		if (state == ViewPager.SCROLL_STATE_IDLE) {
    			// start fadeout animation
    			pageIndicatorsLayout.startAnimation(fadeoutAnim);
    		} else if (state == ViewPager.SCROLL_STATE_DRAGGING) {
    			pageIndicatorsLayout.clearAnimation();
    		}
    	}

    	public void onPageSelected(int pos) {
    		boolean oldAreMealsEqual = mealDisplayed.equals(currentMeal);
    		mealDisplayed = mAdapter.getMeal(pos);
    		refreshTitle(true);
    		// whether or not to disable/enable the goto current meal button
    		if (oldAreMealsEqual != mealDisplayed.equals(currentMeal))
    			invalidateOptionsMenu();
    		// refresh page indicators
    		for (int i = 0; i < TOTAL_NUMLISTS; ++i) {
    			ImageView iv = pageIndicators[i];
    			if (i == pos)
    				iv.setImageDrawable(indic_selected);
    			else
    				iv.setImageDrawable(indic_notSelected);
    		}
    	}
    }
    
    // date picker dialog callback
    // create another dialog for meal selection
    public void dateSelected(Date dt) {
    	selectedDate = dt.toString();
    	// hard coded meal type!!
    	MealPickerDialogFragment mealPicker = MealPickerDialogFragment.newInstance(
    			mTP.getDaysMealNames(0, dt.weekDay)
    			);
    	mealPicker.show(getFragmentManager(), "MealPicker");
    }
    
    // meal selecter dialog callback
    public void onMealSelected(String mealName) {
    	mAdapter = new MealViewPagerAdapter(locIDsToShow, mTP.constructMeal(mealName, selectedDate, 0),
    			0, getSupportFragmentManager());
    	int oldPosition = mPager.getCurrentItem();
    	mPager.setAdapter(mAdapter);
    	mPager.setOnPageChangeListener(new TitleChangeListener());
    	mPager.setCurrentItem(oldPosition);
    	startPageIndicatorFadeout();
    }

    //----------------------------------------------------------------------------------------------------------

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	MenuItem gotoCurrent = menu.findItem(R.id.gotocurrentmeal);
    	boolean enabled = !mealDisplayed.equals(currentMeal);
    	gotoCurrent.setEnabled(enabled);
    	if (enabled)
    		gotoCurrent.getIcon().setAlpha(ALPHA_ENABLED);
    	else
    		gotoCurrent.getIcon().setAlpha(ALPHA_DISABLED);
    	return true;
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_viewbymeal, menu);
        
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
    	case R.id.gotocurrentmeal:
    		gotoCurrentMeal();
    		return true;
    	case R.id.jumptodate:
    		DatePickerDialogFragment datePicker = DatePickerDialogFragment.newInstance(
    				mAdapter.getMeal(mPager.getCurrentItem()).date.toString()
    				);
    		datePicker.show(getFragmentManager(), "datePicker");
    		return true;
    	case R.id.about:
    		AboutFragment about = new AboutFragment();
    		about.show(getFragmentManager(), "about");
    		return true;
    	case R.id.legend:
    		LegendFragment legend = new LegendFragment();
    		legend.show(getFragmentManager(), "legend");
    		return true;
    	case R.id.reorder:
    		Intent intent = new Intent(this, ReorderLocations.class);
    		startActivityForResult(intent, REQCODE_REORDER);
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
    	}
    }

    //---------------------------------------------------------------------------------------------
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	if (requestCode == REQCODE_REORDER) {
    		if (resultCode == RESULT_CANCELED)
    			return;
    		// result was ok
    		locIDsToShow = PreferenceManager.getLocIds();
    		// update viewpager
    		int savePos = mPager.getCurrentItem();
    		mAdapter.newLocs(locIDsToShow);
    		mPager.setAdapter(mAdapter);
    		mPager.setCurrentItem(savePos);
    	}
    }

    @Override
    public void onPause() {
    	super.onPause();
    	// unregister receiver
    	unregisterReceiver(timeChangedReceiver);
    }

    @Override
    public void onResume() {
    	super.onResume();
    	// register receiver
        registerReceiver(timeChangedReceiver, sIntentFilter);
        startPageIndicatorFadeout();
        onTimeChanged();
    }
    
}