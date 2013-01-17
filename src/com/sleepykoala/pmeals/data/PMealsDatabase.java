package com.sleepykoala.pmeals.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PMealsDatabase extends SQLiteOpenHelper {
	
	// V1 had _ID, locationid, date, mealname, itemname, itemerror
	// V2 had food info
	private static final int DB_VERSION = 2;
	private static final String DB_NAME = "pmeals";
	
	// table names
	public static final String TABLE_MEALS = "meals";
	// column names
	public static final String _ID = "_id";
	public static final String LOCATIONID = "locationid";
	public static final String DATE = "date"; // in MM/dd/yyyy format
	public static final String MEALNAME = "meal_name";
	public static final String ITEMNAME = "item_name"; // corresponds to itemName field
	public static final String ITEMERROR = "item_error"; // corresponds to error field
	// food info
	public static final String ITEMVEGAN = "item_vegan"; // is the item vegan?
	public static final String ITEMVEGETARIAN = "item_vegetarian"; // is the item vegetarian?
	public static final String ITEMPORK = "item_pork"; // does the item contain pork?
	public static final String ITEMNUTS = "item_nuts"; // does the item have nuts?
	public static final String ITEMEFRIENDLY = "item_efriendly"; // is the item earth-friendly?
	
	// SQL commands
	// Now with vegetarian, vegan, earth-friendly, pork, and nuts allergen info
	private static final String CREATE_TABLE_MEALS = "create table " + TABLE_MEALS +
			" (" + _ID + " integer primary key autoincrement, " +
			LOCATIONID + " text, " +
			DATE + " text, " +
			MEALNAME + " text, " +
			ITEMNAME + " text not null, " +
			ITEMERROR + " integer, " + 
			ITEMVEGAN + " integer default 0, " +
			ITEMVEGETARIAN + " integer default 0, " +
			ITEMPORK + " integer default 0, " +
			ITEMNUTS + " integer default 0, " +
			ITEMEFRIENDLY + " integer default 0" +
			");";
	
	// schema
	private static final String DB_SCHEMA = CREATE_TABLE_MEALS;
	
	public PMealsDatabase(Context c) {
		super(c, DB_NAME, null, DB_VERSION);
	}
	
	// create new table, and put in the "Closed" entry
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(DB_SCHEMA);
		// insert closed
		ContentValues closed = new ContentValues();
		closed.put(ITEMNAME, C.STRING_CLOSED);
		closed.put(ITEMERROR, true);
		db.insert(TABLE_MEALS, null, closed);
		// insert no meals today
		ContentValues nmt = new ContentValues();
		nmt.put(ITEMNAME, C.STRING_NOMEALSTODAY);
		nmt.put(ITEMERROR, true);
		db.insert(TABLE_MEALS, null, nmt);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (oldVersion < 2 && newVersion >= 2) {
			db.execSQL("alter table " + TABLE_MEALS + " add column " + ITEMVEGAN + " integer default 0");
			db.execSQL("alter table " + TABLE_MEALS + " add column " + ITEMVEGETARIAN + " integer default 0");
			db.execSQL("alter table " + TABLE_MEALS + " add column " + ITEMPORK + " integer default 0");
			db.execSQL("alter table " + TABLE_MEALS + " add column " + ITEMNUTS + " integer default 0");
			db.execSQL("alter table " + TABLE_MEALS + " add column " + ITEMEFRIENDLY + " integer default 0");
		}
	}

}