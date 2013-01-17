package com.sleepykoala.pmeals.data;

import static com.sleepykoala.pmeals.data.C.MEAL_NAMES;

public class MealTime {
	
	public final int[] startTime;
	public final int[] endTime;
	public final int mealNameIndex;
	public final String mealName;
	public final int type;
	
	// Constructor
	// takes int, which is index of name of meal in MEAL_NAMES
	// two two-int arrays, representing start and end times of this meal.
	// first element is hour, second is minute.
	public MealTime(int mni, int[] start, int[] end, int type) {
		startTime = new int[2];
		startTime[0] = start[0];
		startTime[1] = start[1];
		endTime = new int[2];
		endTime[0] = end[0];
		endTime[1] = end[1];
		mealNameIndex = mni;
		mealName = MEAL_NAMES[mealNameIndex];
		this.type = type;
	}
	
	// takes a meal name instead of an index
	public MealTime(String name, int[] start, int[] end, int type) {
		mealName = name;
		int index;
		for (index = 0; index < MEAL_NAMES.length; ++index) {
			if (name.equals(MEAL_NAMES[index]))
				break;
		}
		if (index == MEAL_NAMES.length)
			throw new IllegalArgumentException("Meal name not recognized!"); // bad name!!
		mealNameIndex = index;
		startTime = new int[2];
		startTime[0] = start[0];
		startTime[1] = start[1];
		endTime = new int[2];
		endTime[0] = end[0];
		endTime[1] = end[1];
		this.type = type;
	}
	
	// copy constructor
	public MealTime(MealTime mt) {
		startTime = new int[2];
		startTime[0] = mt.startTime[0];
		startTime[1] = mt.startTime[1];
		endTime = new int[2];
		endTime[0] = mt.endTime[0];
		endTime[1] = mt.endTime[1];
		mealNameIndex = mt.mealNameIndex;
		mealName = MEAL_NAMES[mealNameIndex];
		type = mt.type;
	}
}
