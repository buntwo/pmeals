package com.buntwo.pmeals.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class PMealsDatabase extends SQLiteOpenHelper {
	
	private static final int DB_VERSION = 1;
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
	
	// SQL commands
	private static final String CREATE_TABLE_MEALS = "create table " + TABLE_MEALS +
			" (" + _ID + " integer primary key autoincrement, " +
			LOCATIONID + " text, " +
			DATE + " text, " +
			MEALNAME + " text, " +
			ITEMNAME + " text not null, " +
			ITEMERROR + " integer);";
	
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

	// drops all data on upgrade!!
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("drop table if exists " + TABLE_MEALS);
		onCreate(db);
	}

}
