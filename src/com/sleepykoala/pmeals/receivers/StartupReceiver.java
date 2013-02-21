package com.sleepykoala.pmeals.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sleepykoala.pmeals.data.Date;
import com.sleepykoala.pmeals.services.AlertService;
import com.sleepykoala.pmeals.services.DailyDownloadService;

public class StartupReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
			// set daily update alarm
			Intent dailyDownload = new Intent(context, DailyDownloadService.class);
			PendingIntent pI = PendingIntent.getService(context, 0, dailyDownload, PendingIntent.FLAG_CANCEL_CURRENT);
			((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setInexactRepeating(
					AlarmManager.RTC, (new Date()).toMillis(false) + 2000, AlarmManager.INTERVAL_DAY, pI);
			// set alerts
			AlertService.setNextAlert(context);
		}
	}

}
