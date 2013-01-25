package com.sleepykoala.pmeals.receivers;

import static com.sleepykoala.pmeals.data.C.ACTION_WIDGET_BACKWARD;
import static com.sleepykoala.pmeals.data.C.ACTION_WIDGET_FORWARD;
import static com.sleepykoala.pmeals.data.C.EXTRA_DATE;
import static com.sleepykoala.pmeals.data.C.EXTRA_LOCATIONID;
import static com.sleepykoala.pmeals.data.C.EXTRA_MEALNAME;
import static com.sleepykoala.pmeals.data.C.MEALTIMESXML;
import static com.sleepykoala.pmeals.data.C.PREFSFILENAME;
import static com.sleepykoala.pmeals.data.C.PREF_WIDGET_LOCID;
import static com.sleepykoala.pmeals.data.C.PREF_WIDGET_LOCNAME;
import static com.sleepykoala.pmeals.data.C.PREF_WIDGET_TYPE;

import java.io.IOException;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.text.format.Time;
import android.widget.RemoteViews;

import com.sleepykoala.pmeals.R;
import com.sleepykoala.pmeals.data.DatedMealTime;
import com.sleepykoala.pmeals.data.MealTimeProvider;
import com.sleepykoala.pmeals.data.MealTimeProviderFactory;
import com.sleepykoala.pmeals.services.MenuWidgetAdapterService;
import com.sleepykoala.pmeals.services.WidgetSwitcherService;

public class MenuWidgetProvider extends AppWidgetProvider {
	
	//private static final String TAG = "MenuWidgetProvider";

	@Override
	public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
		SharedPreferences prefs = context.getSharedPreferences(PREFSFILENAME, 0);

		// get meal time provider
		try {
			MealTimeProviderFactory.initialize(context.getAssets().open(MEALTIMESXML));
		} catch (IOException e) {
			throw new RuntimeException("Cannot find asset " + MEALTIMESXML + "!!");
		}
		MealTimeProvider mTP = MealTimeProviderFactory.newMealTimeProvider();
		int type = prefs.getInt(PREF_WIDGET_TYPE, 0);
		DatedMealTime dmt = mTP.getCurrentMeal(type);
		// set update alarm
		Time nextTime = new Time();
		nextTime.set(0, dmt.endTime[1], dmt.endTime[0], dmt.date.monthDay, dmt.date.month, dmt.date.year);
		Intent update = new Intent(context, MenuWidgetProvider.class);
		update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		PendingIntent pIUpdate = PendingIntent.getBroadcast(context, 0, update, 0);
		((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).
			set(AlarmManager.RTC, nextTime.toMillis(false), pIUpdate);
		
		RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_menu);
		// meal name
		views.setTextViewText(R.id.widget_locname, prefs.getString(PREF_WIDGET_LOCNAME, ""));
		Intent clickUpdate = new Intent(context, MenuWidgetProvider.class);
		clickUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
		PendingIntent pIClick = PendingIntent.getBroadcast(context, 0, clickUpdate, 0);
		views.setOnClickPendingIntent(R.id.widget_locname, pIClick);
		
		// switcher buttons
		Intent switcherF = new Intent(context, WidgetSwitcherService.class);
		switcherF.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		switcherF.setAction(ACTION_WIDGET_FORWARD);
		PendingIntent pIF = PendingIntent.getService(context, 0, switcherF, 0);
		views.setOnClickPendingIntent(R.id.widget_next, pIF);
		Intent switcherB = new Intent(context, WidgetSwitcherService.class);
		switcherB.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
		switcherB.setAction(ACTION_WIDGET_BACKWARD);
		PendingIntent pIB = PendingIntent.getService(context, 0, switcherB, 0);
		views.setOnClickPendingIntent(R.id.widget_prev, pIB);

		// adapter
		Intent adapter = new Intent(context, MenuWidgetAdapterService.class);
		// glom everything as intent data to differentiate it from previous
		// intents (more extras does not differentiate, causing it to not update!)
		int locId = prefs.getInt(PREF_WIDGET_LOCID, -1);
		adapter.setData(Uri.fromParts("content", dmt.mealName + locId + dmt.date, null));
		adapter.putExtra(EXTRA_LOCATIONID, locId);
		adapter.putExtra(EXTRA_MEALNAME, dmt.mealName);
		adapter.putExtra(EXTRA_DATE, dmt.date.toString());
		views.setRemoteAdapter(R.id.widget_list, adapter);

		// update
		manager.updateAppWidget(appWidgetIds, views);

		super.onUpdate(context, manager, appWidgetIds);
	}

}