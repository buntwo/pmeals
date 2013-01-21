package com.sleepykoala.pmeals.services;

import java.util.LinkedList;
import java.util.Queue;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

// Downloader service
// gets its arguments from an Intent
public class MenuDownloaderService extends Service {

	//private static final String TAG = "MenuDownloaderService";

	private static final Object lock = new Object();
	private static final int MAX_DOWNLOADTHREADS = 2;
	private static int availableThreads = MAX_DOWNLOADTHREADS;
	private static Queue<Thread> workerQueue = new LinkedList<Thread>();

	//------------------------------------------------------OVERRIDES------------------------------------------------

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Thread task = new Thread(
				new MenuDownloader(intent.getExtras(), getApplicationContext())
				);

		synchronized (lock) {
			if (availableThreads > 0) {
				--availableThreads;
				task.start();
			} else
				workerQueue.offer(task);
		}

		stopSelf(startId);

		return START_NOT_STICKY;
	}

	// start next task, if exists
	public static void startNextTask() {
		synchronized (lock) {
			++availableThreads;
			Thread next = workerQueue.poll();
			if (next != null) {
				--availableThreads;
				next.start();
			}
		}
	}

	// no binding
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
