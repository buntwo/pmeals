package com.sleepykoala.pmeals.data;

import static com.sleepykoala.pmeals.data.C.IS24HOURFORMAT;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import android.text.format.Time;

// get info about meal times
// should never return any MealTimes; all returned MealTimes should be DatedMealTimes
public class MealTimeProvider {
	
	//private static final String TAG = "MealTimeProvider";
    
	private ArrayList<ArrayList<MealTime>[]> mealTimes;
	
	/**
	 * Return a new MealTimeProvider.
	 * 
	 * @param aMealTimes ArrayList of ArrayList of MealTimes. First index
	 * is type, second is weekday, where 0 is Sunday
	 */
	public MealTimeProvider(ArrayList<ArrayList<MealTime>[]> aMealTimes) {
		mealTimes = aMealTimes;
	}
	
	/**
	 * Given a DatedMealTime, return another one of the same but different type.
	 * 
	 * @param m DatedMealTime to convert
	 * @param type Type to convert to
	 * @return A new DatedMealTime with the given type, or null if it does not exist
	 */
	public DatedMealTime swapType(DatedMealTime m, int type) {
		// loop through the day's meals in the requested type
		for (MealTime mt : mealTimes.get(type)[m.date.weekDay]) {
			if (mt.mealName.equals(m.mealName))
				return new DatedMealTime(mt, m.date);
		}
		return null;
	}
	
	/**
	 * Checks if the given DatedMealTime is the current meal for the given type.
	 * 
	 * @param dmt Meal to check
	 * @param type Type to check against
	 * @return If the given meal is the current meal of the given type
	 */
	public boolean isCurrentMeal(DatedMealTime dmt, int type) {
		return getCurrentMeal(type).equals(dmt);
	}
	
	/**
	 * Returns the current meal of the given type.
	 * 
	 * @param type Meal type to return
	 * @return A DatedMealTime of the current meal
	 */
	public DatedMealTime getCurrentMeal(int type) {
		Time tm = getCurTime();
		return getMealAtTime(type, tm);
	}
	
	/**
	 * Return the meal that is after or during the given Time.
	 * 
	 * @param type Meal type to return
	 * @param tm The time to start searching for the next meal
	 * @return A DatedMealTime of the next meal
	 */
	private DatedMealTime getMealAtTime(int type, Time tm) {
		int wkDay = tm.weekDay;
		// check the last meal of the day before us first
		// for checking next-day closing times
		Time tm_ = new Time(tm);
		--tm_.monthDay;
		ArrayList<MealTime> arr = mealTimes.get(type)[(wkDay + 6) % 7];
		try {
			MealTime mtmt = arr.get(arr.size() - 1);
			mtmt.setProperTimes(tm_);
			if (isBeforeMeal(tm, mtmt) || isDuringMeal(tm, mtmt))
				return new DatedMealTime(mtmt, tm_);
		} catch (ArrayIndexOutOfBoundsException e) { }
		// restore monthDay
		++tm_.monthDay;
		// now start checking from given date
		for (MealTime mt : mealTimes.get(type)[wkDay]) { // loop through our day
			mt.setProperTimes(tm);
			if (isBeforeMeal(tm, mt) || isDuringMeal(tm, mt)) // found meal!
				return new DatedMealTime(mt, tm);
		}
		// else return first one after this day
		MealTime meal = null;
		int daysRolled;
		for (daysRolled = 1; daysRolled <= 6; ++daysRolled) {
			ArrayList<MealTime> day = mealTimes.get(type)[(wkDay + daysRolled) % 7];
			if (day.size() != 0) {
				meal = day.get(0);
				break;
			}
		}
		// make new time with correct date
		tm_.monthDay += daysRolled;
		tm_.normalize(true);
		return new DatedMealTime(meal, tm_);
	}
	
	/**
	 * Return the meal that is before the given Time.
	 * 
	 * @param type Meal type to return
	 * @param tm The time to start searching for the previous meal
	 * @return A DatedMealTime of the previous meal
	 */
	private DatedMealTime getMealBeforeTime(int type, Time tm) {
		int wkDay = tm.weekDay;
		ArrayList<MealTime> daysMeals = mealTimes.get(type)[wkDay];
		for (int i = daysMeals.size() - 1; i >= 0; --i) { // loop through our day in reverse
			MealTime mt = daysMeals.get(i);
			mt.setProperTimes(tm);
			if (isAfterMeal(tm, mt)) // found
				return new DatedMealTime(mt, tm);
		}
		// else return first one before this day
		MealTime meal = null;
		int daysRolledBack;
		for (daysRolledBack = 1; daysRolledBack <= 6; ++daysRolledBack) {
			ArrayList<MealTime> day = mealTimes.get(type)[(wkDay - daysRolledBack + 7) % 7];
			int size = day.size();
			if (size != 0) {
				meal = day.get(size - 1);
				break;
			}
		}
		if (meal == null)
			throw new RuntimeException("Null meal, should never happen!");
		// make new time with correct date
		Time tm_ = new Time(tm);
		tm_.monthDay -= daysRolledBack;
		tm_.normalize(true);
		return new DatedMealTime(meal, tm_);
	}
	
	/**
	 * Find the next meal.
	 * 
	 * @param type Meal type to search for
	 * @param meal Starting meal
	 * @return The meal following the given one
	 */
	public DatedMealTime getNextMeal(int type, DatedMealTime meal) {
		Time tm = new Time();
		tm.set(meal.endTime);
		return getMealAtTime(type, tm);
	}
	
	/**
	 * Find the previous meal.
	 * 
	 * @param type Meal type to search for
	 * @param meal Starting meal
	 * @return The meal preceding the given one
	 */
	public DatedMealTime getPreviousMeal(int type, DatedMealTime meal) {
		Time tm = new Time();
		tm.set(meal.startTime);
		return getMealBeforeTime(type, tm);
	}
	
	/**
	 * Check to see what the current time is relative to the given meal.
	 * 
	 * @param meal Meal to check against
	 * @return 1 if we are in the given meal<br>
	 * 0 if we are after the meal<br>
	 * -1 if we are before the meal
	 */
	public static int currentMealStatus(DatedMealTime meal) {
		Time tm = getCurTime();
		if (tm.toMillis(false) >= meal.endTime)
			return 0;
		else if (tm.toMillis(false) < meal.startTime)
			return -1;
		else
			return 1;
	}
	
	/**
	 * Return a list of the day's meals.
	 * 
	 * @param type Meal type
	 * @param date Which day's meals to return
	 * @return An ArrayList of the day's DatedMealTimes, which could be empty
	 */
	public ArrayList<DatedMealTime> getDaysMeals(int type, Date date) {
		ArrayList<DatedMealTime> daysMeals = new ArrayList<DatedMealTime>();
		for (MealTime m : mealTimes.get(type)[date.weekDay])
			daysMeals.add(new DatedMealTime(m, date));
		return daysMeals;
	}
	
	/**
	 * Return a list of the day's meal names. It is intended to be more efficient
	 * than calling {@link getDaysMeals} and extracting the meal names manually.
	 * 
	 * @param type Meal type
	 * @param dow Day of week's list to return (0 = Sunday)
	 * @return An ArrayList of the day's meal names, which could be empty
	 */
	public ArrayList<String> getDaysMealNames(int type, int dow) {
		ArrayList<String> daysMealNames = new ArrayList<String>();
		for (MealTime  m : mealTimes.get(type)[dow])
			daysMealNames.add(m.mealName);
		return daysMealNames;
	}
	
	/**
	 * Given a meal name, a date, and a type, return the unique DatedMealTime
	 * that has those properties, or null.
	 * 
	 * @param mealName Meal name
	 * @param date Date
	 * @param type Meal type
	 * @return The unique DatedMealTime that has the given properties, or null
	 */
	// given a mealname, a date string, and a type, get a DatedMealTime
	// if not found, return null
	public DatedMealTime constructMeal(String mealName, String date, int type) {
		Date dt = new Date(date);
		for (MealTime mt : mealTimes.get(type)[dt.weekDay]) {
			if (mt.mealName.equals(mealName)) {
				return new DatedMealTime(mt, dt);
			}
		}
		return null;
	}
	
	//-----------------------------------------PRIVATE STATIC METHODS--------------------------------
	
	// whether or not the given Time is before the given meal's starting time
	private static boolean isBeforeMeal(Time tm, MealTime meal) {
		return isBefore(tm, meal.startTime);
	}
	
	// whether or not the given Time is after given meal's ending time
	private static boolean isAfterMeal(Time tm, MealTime meal) {
		return !isBefore(tm, meal.endTime);
	}
	
	// whether or not given Time is during given MealTime
	private static boolean isDuringMeal(Time tm, MealTime meal) {
		return (!isBeforeMeal(tm, meal) && !isAfterMeal(tm, meal));
	}
	
	// whether or not given Time is before given long time
	private static boolean isBefore(Time tm, long time) {
		return TimeUnit.MILLISECONDS.toMinutes(tm.toMillis(false))
				< TimeUnit.MILLISECONDS.toMinutes(time);
	}
	
	private static Time getCurTime() {
		Time tm = new Time();
		tm.setToNow();
		return tm;
	}
	
	
	//--------------------------------------PUBLIC STATIC METHODS--------------------------------
	
	// get time until given meal as a 2-int array: {hour, min}
	// start is whether or not to use meal's start time
	// if the date of the meal is not the same as the current date, 24 hours
	// will be added/subtracted. if the date is off by more than 1 day, only 24
	// hours will be added/subtracted!
	public static int[] getTimeUntilMeal(DatedMealTime meal, boolean start) {
		Time tm = getCurTime();
		int[] timeTill = new int[2];
		long mealTime = (start) ? meal.startTime : meal.endTime;
		long till = TimeUnit.MILLISECONDS.toMinutes(mealTime)
				- TimeUnit.MILLISECONDS.toMinutes(tm.toMillis(false));
		timeTill[0] = (int) (till / 60);
		timeTill[1] = (int) (till % 60);
		return timeTill;
	}
	
	// get 12/24 hour setting-aware time
	public static String getFormattedTime(long time) {
		Time tm = new Time();
		tm.set(time);
		if (IS24HOURFORMAT)
			return String.format("%d:%02d", tm.hour, tm.minute);
		else {
			boolean inAM = (tm.hour < 12);
			int hour;
			if (inAM)
				hour = (tm.hour == 0) ? 12 : tm.hour;
			else
				hour = (tm.hour == 12) ? 12 : tm.hour - 12;
			return String.format("%d:%02d%s", hour, tm.minute, inAM ? "am" : "pm");
		}
	}
	
	// get 12/24 hour setting-aware time
	public static String getFormattedTime(int hour, int minute) {
		if (IS24HOURFORMAT)
			return String.format("%d:%02d", hour, minute);
		else {
			boolean inAM = (hour < 12);
			if (inAM)
				hour = (hour == 0) ? 12 : hour;
			else
				hour = (hour == 12) ? 12 : hour - 12;
			return String.format("%d:%02d%s", hour, minute, inAM ? "am" : "pm");
		}
	}
}
