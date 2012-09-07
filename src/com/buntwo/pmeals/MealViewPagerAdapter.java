package com.buntwo.pmeals;

import static com.buntwo.pmeals.data.C.EXTRA_DATE;
import static com.buntwo.pmeals.data.C.EXTRA_LOCATIONIDS;
import static com.buntwo.pmeals.data.C.EXTRA_MEALNAME;
import static com.buntwo.pmeals.data.C.VBM_NUMLISTS_AFTER;
import static com.buntwo.pmeals.data.C.VBM_NUMLISTS_BEFORE;

import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import com.buntwo.pmeals.data.DatedMealTime;
import com.buntwo.pmeals.data.MealTimeProvider;
import com.buntwo.pmeals.data.MealTimeProviderFactory;

public class MealViewPagerAdapter extends FragmentPagerAdapter {
	
	//private static final String TAG = "MealViewPagerAdapter";
	
	private ArrayList<Integer> locIDsToShow;
	
	private ArrayList<DatedMealTime> meals;
	private final int mPagerId;
	
	public MealViewPagerAdapter(ArrayList<Integer> locIDs, DatedMealTime centerMeal,
			int mainType, FragmentManager fm) {
		super(fm);
		locIDsToShow = locIDs;
		meals = new ArrayList<DatedMealTime>();
		mPagerId = centerMeal.hashCode();
		
		// should be initialized already
		MealTimeProvider mTP = MealTimeProviderFactory.newMealTimeProvider();
		
		int mealType = mainType;
		meals.add(centerMeal);
		
		// add later and previous ones
		DatedMealTime prevMeal = centerMeal;
		for (int i = 0; i < VBM_NUMLISTS_BEFORE; ++i) {
			prevMeal = mTP.getPreviousMeal(mealType, prevMeal);
			meals.add(0, prevMeal);
		}
		DatedMealTime nextMeal = centerMeal;
		for (int i = 0; i < VBM_NUMLISTS_AFTER; ++i) {
			// ** hardcoded meal type!! **
			nextMeal = mTP.getNextMeal(mealType, nextMeal);
			meals.add(nextMeal);
		}
	}
	
	// return the index of the given meal in the our list
	// if not found, return -1
	public int findMealIndex(DatedMealTime dmt) {
		int index = 0;
		for (DatedMealTime m : meals) {
			if (m.equals(dmt))
				return index;
			++index;
		}
		return -1;
	}
	
	// returns the middle index
	public int getMiddleIndex() {
		return VBM_NUMLISTS_BEFORE;
	}
	
	public DatedMealTime getMeal(int pos) {
		return meals.get(pos);
	}
	
	@Override
	public Fragment getItem(int pos) {
		MealViewListFragment lf = new MealViewListFragment();
		Bundle args = new Bundle();
		DatedMealTime meal = meals.get(pos);
		args.putIntegerArrayList(EXTRA_LOCATIONIDS, locIDsToShow);
		args.putString(EXTRA_DATE, meal.date.toString());
		args.putString(EXTRA_MEALNAME, meal.mealName);
		lf.setArguments(args);
		
		return lf;
	}
	
	@Override
	public int getCount() {
		return meals.size();
	}

	public void refreshList(int pos) {
		
	}
	
	@Override
	public long getItemId(int position) {
		return mPagerId*100 + position;
	}

}
