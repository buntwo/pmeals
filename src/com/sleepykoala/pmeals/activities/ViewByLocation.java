package com.sleepykoala.pmeals.activities;

import static com.sleepykoala.pmeals.data.C.ALERT_FADEIN_TIME;
import static com.sleepykoala.pmeals.data.C.ALPHA_DISABLED;
import static com.sleepykoala.pmeals.data.C.ALPHA_ENABLED;
import static com.sleepykoala.pmeals.data.C.END_ALERT_COLOR;
import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.IS24HOURFORMAT;
import static com.sleepykoala.pmeals.data.C.LOCATIONSXML;
import static com.sleepykoala.pmeals.data.C.MEALTIMESXML;
import static com.sleepykoala.pmeals.data.C.MEAL_PASSED_COLOR;
import static com.sleepykoala.pmeals.data.C.MINUTES_END_ALERT;
import static com.sleepykoala.pmeals.data.C.MINUTES_START_ALERT;
import static com.sleepykoala.pmeals.data.C.NO_ALERT_COLOR;
import static com.sleepykoala.pmeals.data.C.ONEHOUR_RADIUS;
import static com.sleepykoala.pmeals.data.C.START_ALERT_COLOR;
import static com.sleepykoala.pmeals.data.C.VBL_NUMLISTS_AFTER;
import static com.sleepykoala.pmeals.data.C.VBL_NUMLISTS_BEFORE;

import java.io.IOException;

import android.animation.ValueAnimator;
import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.TextView;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.adapters.LocationViewPagerAdapter;
import com.sleepykoala.pmeals.data.C;
import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.Location;
import com.sleepykoala.pmeals.data.LocationProvider;
import com.sleepykoala.pmeals.data.LocationProviderFactory;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.data.RgbEvaluator;
import com.sleepykoala.pmeals.fragments.DatePickerDialogFragment;
import com.sleepykoala.pmeals.fragments.DatePickerDialogFragment.OnDateSelectedListener;
import com.sleepykoala.pmeals.fragments.LegendFragment;

public class ViewByLocation extends FragmentActivity implements OnNavigationListener, OnDateSelectedListener {
	
	//private static final String TAG = "ViewByLocation";
	
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
	
    private ViewPager mPager;
    private LocationViewPagerAdapter mAdapter;

    // cached animations
    private Animation dropdown0;
    private Animation dropdown1;
					
    private LinearLayout pageIndicatorsLayout;
    private ImageView[] pageIndicators;
    private Drawable indic_notSelected;
    private Drawable indic_selected;
    private Animation fadeoutAnim;
    private static int TOTAL_NUMLISTS = VBL_NUMLISTS_BEFORE + VBL_NUMLISTS_AFTER + 1;
    public static boolean isWeird;
    
    private MealTimeProvider mTP;
    private LocationProvider lP;
    
    private DatedMealTime currentMeal;
    private Date dateDisplayed;
    private Date currentCenter;
    private Location displayedLoc;
    
    private Date today;
    
    // infobar stuff
    private TextView mealInfoView0;
    private TextView mealInfoView1;
    private FrameLayout infoBar;
    private String mealInfo;
    private int infoBarColor;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewbylocation);

        // set 24 hour status
        IS24HOURFORMAT = DateFormat.is24HourFormat(this);

        // get meal time provider
		try {
			MealTimeProviderFactory.initialize(getAssets().open(MEALTIMESXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + MEALTIMESXML + "!!");
		}
		mTP = MealTimeProviderFactory.newMealTimeProvider();
        // get location provider
        try {
        	LocationProviderFactory.initialize(getAssets().open(LOCATIONSXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + LOCATIONSXML + "!!");
		}
        lP = LocationProviderFactory.newLocationProvider();
        
        // cache animations
        dropdown0 = AnimationUtils.loadAnimation(this, R.anim.infobar_dropdown0);
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
        Resources res = getResources();
        indic_notSelected = res.getDrawable(R.drawable.pageindicator_notselected);
        indic_selected = res.getDrawable(R.drawable.pageindicator_selected);
        // load fadeout animation
        fadeoutAnim = AnimationUtils.loadAnimation(ViewByLocation.this, R.anim.pageindicator_fadeout);

        // cache food indicator drawables if not already cached by VBM
        // (e.g., entry from widget)
        if (ViewByMeal.vegan == null) {
        	ViewByMeal.vegan = res.getDrawable(R.drawable.vegan);
        	ViewByMeal.vegetarian = res.getDrawable(R.drawable.vegetarian);
        	ViewByMeal.pork = res.getDrawable(R.drawable.pork);
        	ViewByMeal.nuts = res.getDrawable(R.drawable.nuts);
        	ViewByMeal.outline = res.getDrawable(R.drawable.foodinfo_outline);
        }
		
		// set location
		Intent intent = getIntent();
		mPager = (ViewPager) findViewById(R.id.listview_pager);
		
		// cache views
		mealInfoView0 = (TextView) findViewById(R.id.infobar_mealinfo0);
		mealInfoView1 = (TextView) findViewById(R.id.infobar_mealinfo1);
		infoBar = (FrameLayout) findViewById(R.id.infobar);
		
		// set today
		today = new Date();
		
		dateDisplayed = currentCenter = new Date(intent.getStringExtra(EXTRA_DATE));
		
        // setup action bar
        ActionBar aB = getActionBar();
        aB.setTitle("PMeals");
        aB.setDisplayShowTitleEnabled(true);
        aB.setDisplayHomeAsUpEnabled(true);
        aB.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        // hard coded info type!! (nickname)
        /*
        PreferenceManager.initialize(this);
        ArrayList<Integer> locIds = PreferenceManager.getLocIds();
        ArrayList<String> locNames = new ArrayList<String>(locIds.size());
        for (Integer i : locIds)
        	locNames.add(lP.getById(i).nickname);
         */
        aB.setListNavigationCallbacks(new ArrayAdapter<String>(this, R.layout.actionbar_viewbylocation_spinner,
        		R.id.action_viewbylocation_spinnertext, lP.getInfoArray(1)), this);
        int index = LocationProvider.idToIndex(intent.getIntExtra(EXTRA_LOCATIONID, -1));
        
        // setup displayedLoc and mPager
        displayedLoc = lP.getByIndex(index);
        currentMeal = mTP.getCurrentMeal(displayedLoc.type);
        aB.setSelectedNavigationItem(index);
		mAdapter = new LocationViewPagerAdapter(displayedLoc, currentCenter, getSupportFragmentManager());
		mPager.setAdapter(mAdapter);
		mPager.setOffscreenPageLimit(1);
		mPager.setOnPageChangeListener(new TitleChangeListener());
        mPager.setCurrentItem(mAdapter.getMiddleIndex());
    }

    // go to today
    public void gotoToday() {
    	int todayIndex = mAdapter.getDateIndex(currentMeal.date);
    	if (todayIndex != -1) // -1 means not found, eg, different date picked out of range
    		mPager.setCurrentItem(todayIndex, true);
    	else
    		dateSelected(currentMeal.date);
    }

	private void onTimeChanged() {
		DatedMealTime d = mTP.getCurrentMeal(displayedLoc.type);
		boolean newMeal = !d.equals(currentMeal);
		Date oldToday = today;
		today = new Date();
		if (newMeal) {
    		Intent newMealIntent = new Intent();
    		newMealIntent.setAction(C.ACTION_NEW_MEAL);
    		LocalBroadcastManager.getInstance(this).sendBroadcast(newMealIntent);
    		currentMeal = d;
		}
		if (!today.equals(oldToday)) {
			Intent newDayIntent = new Intent();
			newDayIntent.setAction(C.ACTION_NEW_DAY);
			LocalBroadcastManager.getInstance(this).sendBroadcast(newDayIntent);
		}
		refreshTitle(newMeal);
	}
    
    // build meal time data text to show in title
    public void refreshTitle(boolean newMeal) {
    	// build mealinfo text
		final StringBuilder newTitleText = new StringBuilder(); 
		int mealStatus = MealTimeProvider.currentMealStatus(currentMeal);
		boolean inMeal = (mealStatus < 1) ? false : true;

		int[] timeTo = MealTimeProvider.getTimeUntilMeal(currentMeal, !inMeal);
		if (LocationProvider.isDiningHall(displayedLoc)) {
			newTitleText.append(currentMeal.mealName);
			newTitleText.append((inMeal) ? " ends " : " starts ");
		} else {
			newTitleText.append((inMeal) ? "Closes " : "Opens ");
		}
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
			long time;
			if (inMeal)
				time = currentMeal.endTime;
			else
				time = currentMeal.startTime;
			newTitleText.append(MealTimeProvider.getFormattedTime(time));
		}
		Date today = new Date();
		if (!today.equals(currentMeal.date)) { // next meal is not today
			if (today.isTomorrow(currentMeal.date)) { // next meal is tomorrow
				newTitleText.append(" tomorrow");
			} else if (today.isYesterday(currentMeal.date)) {
				// do nothing, this means we're on the next day of a next-day closing time
			} else { // not tomorrow
				newTitleText.append(" on ");
				newTitleText.append(DateFormat.format("EEEE", currentMeal.date.toMillis(true)));
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
			// fancy dropdown animation
			mealInfoView0.startAnimation(dropdown0);
			mealInfoView1.startAnimation(dropdown1);
		}
		updateInfoBar();
    }
    
    private void updateInfoBar() {
    	mealInfoView0.setText(mealInfo);
    	changeInfoBarColor(infoBarColor);
    }
    
    private void changeInfoBarColor(int newBgColor) {
    	int oldBgColor = ((ColorDrawable) infoBar.getBackground()).getColor();
    	if (!(newBgColor == oldBgColor)) {
    		// color changing animation
    		ValueAnimator colorAnim = new ValueAnimator();
    		colorAnim.setIntValues(oldBgColor, newBgColor);
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
    		boolean oldAreDatesEqual = dateDisplayed.equals(currentMeal.date);
    		dateDisplayed = mAdapter.getDate(pos);
    		// whether or not to disable/enable the goto today button
    		if (oldAreDatesEqual != dateDisplayed.equals(currentMeal.date))
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

    // DatePickerDialog callback
    public void dateSelected(Date date) {
    	currentCenter = date;
		mAdapter = new LocationViewPagerAdapter(displayedLoc, currentCenter, getSupportFragmentManager());
		mPager.setAdapter(mAdapter);
		mPager.setOnPageChangeListener(new TitleChangeListener());
		mPager.setCurrentItem(mAdapter.getMiddleIndex());
		startPageIndicatorFadeout();
		refreshTitle(false);
    }

    //----------------------------------------------------------------------------------------------------------

    // new location selected -> new adapter
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		int oldType;
		int oldPosition;
		if (displayedLoc == null) { // the beginning case
			oldType = -1;
			displayedLoc = lP.getByIndex(itemPosition);
			currentMeal = mTP.getCurrentMeal(displayedLoc.type);
			oldPosition = VBL_NUMLISTS_BEFORE;
		} else {
			oldType = displayedLoc.type;
			displayedLoc = lP.getByIndex(itemPosition);
			currentMeal = mTP.getCurrentMeal(displayedLoc.type);
			oldPosition = mPager.getCurrentItem();
		}
		isWeird = (MealTimeProvider.currentMealStatus(currentMeal) == 1) && !today.equals(currentMeal.date);
		mAdapter = new LocationViewPagerAdapter(displayedLoc, currentCenter, getSupportFragmentManager());
		mPager.setAdapter(mAdapter);
		mPager.setOnPageChangeListener(new TitleChangeListener());
		mPager.setCurrentItem(oldPosition);
		startPageIndicatorFadeout();
		refreshTitle(!(displayedLoc.type == oldType));
		return true;
	}
	
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	MenuItem gotoCurrent = menu.findItem(R.id.gototoday);
    	boolean enabled;
    	try {
    		enabled = !dateDisplayed.equals(currentMeal.date);
    	} catch (NullPointerException e) {
    		enabled = false;
    	}
    	gotoCurrent.setEnabled(enabled);
    	if (enabled)
    		gotoCurrent.getIcon().setAlpha(ALPHA_ENABLED);
    	else
    		gotoCurrent.getIcon().setAlpha(ALPHA_DISABLED);
    	return true;
    }
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_viewbylocation, menu);
        
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
    	case R.id.gototoday:
    		gotoToday();
    		return true;
    	case android.R.id.home:
    		Intent intent = new Intent(this, ViewByMeal.class);
    		startActivity(intent);
    		return true;
    	case R.id.jumptodate:
    		DatePickerDialogFragment datePicker = DatePickerDialogFragment.newInstance(
    				mAdapter.getDate(mPager.getCurrentItem()).toString()
    				);
    		datePicker.show(getFragmentManager(), "DatePicker");
    		return true;
    	case R.id.legend:
    		LegendFragment legend = new LegendFragment();
    		legend.show(getFragmentManager(), "legend");
    		return true;
    	case R.id.settings:
    		Intent settings = new Intent(this, SettingsActivity.class);
    		startActivity(settings);
    		return true;
    	default:
    		return super.onOptionsItemSelected(item);
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
