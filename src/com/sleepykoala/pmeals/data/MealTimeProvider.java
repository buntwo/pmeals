package com.sleepykoala.pmeals.data;

import static com.sleepykoala.pmeals.data.C.IS24HOURFORMAT;

import java.util.ArrayList;

import android.text.format.Time;

// get info about meal times
// should never return any MealTimes; all returned MealTimes should be DatedMealTimes
public class MealTimeProvider {
	
	//private static final String TAG = "MealTimeProvider";
    
	private ArrayList<ArrayList<MealTime>[]> mealTimes;
	
	public MealTimeProvider(ArrayList<ArrayList<MealTime>[]> aMealTimes) {
		mealTimes = aMealTimes;
	}
	
	// get meal of the same name, but different type
	// null if such meal does not exist
	// matches by meal name
	public DatedMealTime swapType(DatedMealTime m, int type) {
		// loop through the day's meals in the requested type
		for (MealTime mt : mealTimes.get(type)[m.date.weekDay]) {
			if (mt.mealName.equals(m.mealName))
				return new DatedMealTime(mt, m.date);
		}
		return null;
	}
	
	// is the given meal the next meal for that type?
	public boolean isCurrentMeal(DatedMealTime dmt, int type) {
		return getCurrentMeal(type).equals(dmt);
	}
	
	// return meal at current time
	public DatedMealTime getCurrentMeal(int type) {
		Time tm = getCurTime();
		return getMealAtTime(type, tm);
	}
	
	// return the meal that is after or during given Time
	// type is the type of location's meal times to use
	// it is specified in mealTimes.xml file
	private DatedMealTime getMealAtTime(int type, Time tm) {
		int wkDay = tm.weekDay;
		for (MealTime mt : mealTimes.get(type)[wkDay]) { // loop through our day
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
		Time tm_new = new Time(tm);
		tm_new.monthDay += daysRolled;
		tm_new.normalize(true);
		return new DatedMealTime(meal, tm_new);
	}
	
	// return the meal that is before given Time
	// type is the type of location's meal times to use
	// it is specified in mealTimes.xml file
	private DatedMealTime getMealBeforeTime(int type, Time tm) {
		int wkDay = tm.weekDay;
		ArrayList<MealTime> daysMeals = mealTimes.get(type)[wkDay];
		for (int i = daysMeals.size() - 1; i >= 0; --i) { // loop through our day in reverse
			MealTime mt = daysMeals.get(i);
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
			throw new RuntimeException("wtf null meal");
		// make new time with correct date
		Time tm_new = new Time(tm);
		tm_new.monthDay -= daysRolledBack;
		tm_new.normalize(true);
		return new DatedMealTime(meal, tm_new);
	}
	
	// given a meal, find the next one
	public DatedMealTime getNextMeal(int type, DatedMealTime meal) {
		Time tm = new Time(meal.date);
		tm.allDay = false;
		tm.hour = meal.endTime[0];
		tm.minute = meal.endTime[1];
		tm.normalize(true);
		return getMealAtTime(type, tm);
	}
	
	// given a meal, find the previous one
	public DatedMealTime getPreviousMeal(int type, DatedMealTime meal) {
		Time tm = new Time(meal.date);
		tm.allDay = false;
		tm.hour = meal.startTime[0];
		tm.minute = meal.startTime[1];
		tm.normalize(true);
		return getMealBeforeTime(type, tm);
	}
	
	// returns -1 if before a meal, 0 if after a meal, 1 if in a meal
	public static int currentMealStatus(DatedMealTime meal) {
		Time tm = getCurTime();
		if (afterDate (tm, meal) || sameDate(tm, meal) && isAfterMeal(tm, meal))
			return 0;
		else if (beforeDate(tm, meal) || sameDate(tm, meal) && isBeforeMeal(tm, meal))
			return -1;
		else
			return 1;
	}
	
	// return a list of that day's meals
	// empty list of there are none
	public ArrayList<DatedMealTime> getDaysMeals(int type, Date date) {
		ArrayList<DatedMealTime> daysMeals = new ArrayList<DatedMealTime>();
		for (MealTime m : mealTimes.get(type)[date.weekDay])
			daysMeals.add(new DatedMealTime(m, date));
		return daysMeals;
	}
	
	// given type and day of week, return that day's meal names
	// intended to be more efficient than calling getDaysMeals, and extracting
	// meal names afterward
	public ArrayList<String> getDaysMealNames(int type, int dow) {
		ArrayList<String> daysMealNames = new ArrayList<String>();
		for (MealTime  m : mealTimes.get(type)[dow])
			daysMealNames.add(m.mealName);
		return daysMealNames;
	}
	
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
	
	// check if given Time is same date as given DatedMealTime
	private static boolean sameDate(Time tm, DatedMealTime meal) {
		Time date = meal.date;
		return tm.year == date.year && tm.month == date.month && tm.monthDay == date.monthDay;
	}
	
	// check if given Time's date is before given DatedMealTime
	private static boolean beforeDate(Time tm, DatedMealTime meal) {
		Time date = meal.date;
		return tm.year < date.year || tm.year == date.year &&
				(tm.month < date.month || tm.month == date.month && tm.monthDay < date.monthDay);
	}
	
	// check if given Time's date is after given DatedMealTime
	private static boolean afterDate(Time tm, DatedMealTime meal) {
		Time date = meal.date;
		return tm.year > date.year || tm.year == date.year &&
				(tm.month > date.month || tm.month == date.month && tm.monthDay > date.monthDay);
	}
	
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
	
	// whether or not given Time is before given int[] time
	private static boolean isBefore(Time tm, int[] time) {
		return (tm.hour < time[0]) || ((tm.hour == time[0]) && (tm.minute < time[1]));
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
		int[] mealTime = (start) ? meal.startTime : meal.endTime;
		timeTill[0] = mealTime[0] - tm.hour;
		timeTill[1] = mealTime[1] - tm.minute;
		// correct for negative values
		if ((timeTill[1] < 0) && (timeTill[0] > 0)) {
			timeTill[1] += 60;
			--timeTill[0];
		}
		if (afterDate(tm, meal))
			timeTill[0] -= 24;
		else if (beforeDate(tm, meal))
			timeTill[0] += 24;
		return timeTill;
	}
	
	// get 12/24 hour setting-aware time
	public static String getFormattedTime(int[] time) {
		if (IS24HOURFORMAT)
			return String.format("%d:%02d", time[0], time[1]);
		else {
			boolean inAM = time[0] < 12;
			return String.format("%d:%02d%s", inAM ? time[0] : time[0]-12,
					time[1], inAM ? "am" : "pm");
		}
	}
	
}
