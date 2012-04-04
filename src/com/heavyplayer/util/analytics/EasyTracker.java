// Copyright 2011 Google Inc. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.heavyplayer.util.analytics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.google.android.apps.analytics.Item;
import com.google.android.apps.analytics.Transaction;

public class EasyTracker {
	public static final String LOG_TAG = EasyTracker.class.getName();

	// EasyTracker is a singleton. Don't let other classes create one.
	private EasyTracker() { }

	private static EasyTracker instance;
	private static Object instanceLock = new Object();

	public static EasyTracker getTracker() {
		if(instance == null) {
			synchronized(instanceLock) {
				if(instance == null) {
					instance = new EasyTracker();
				}
			}
		}

		return instance;
	}

	// If true, tracking is turned on.
	private Boolean gaEnabled = true;

	// The account id to be used for tracking.  It should take the form of
	// 'UA-12345-6'.
	private String gaAccountId;

	// The dispatch period, in seconds.  A value of 0 will turn off automatic
	// dispatching.
	private Integer gaDispatchPeriod;

	// If true, debug mode will be set in GoogleAnalyticsTracker.  This will
	// cause debug messages to be logged in the Android log, viewable with the
	// 'adb logcat' command.
	private Boolean gaDebug;

	// If true, hits will be generated normally, but will not actually be sent to
	// Google Analytics.  Useful for testing tracking code.
	private Boolean gaDryRun;

	// The sample rate determines what precentage of application installs will
	// actually report to Google Analytics.  A value of 100 means that all
	// application installs will report while a value of 0 means none will report.
	private Integer gaSampleRate;

	// If true, the Google Analytics servers will be told to strip off the last
	// octet of the Ip Address of the sender prior to logging the hit.
	private Boolean gaAnonymizeIp;

	// If true, each Activity will be tracked with a Pageview when the Activity
	// is started (via the trackActivityStart method) and an empty event when the
	// Activity is stopped (via the trackActivityStop method).  If false, the
	// Pageview will not be tracked.
	private Boolean autoActivityTracking;

	// The number of Activities that have had their onStart method called but not
	// their corresponding onStop method.  This value is used in determining when
	// a new session should be started.
	private Integer activitiesActive = 0;

	// Controls whether the next call to trackActivityStart will start a new
	// session. We always start out needing a new session.
	private Boolean sessionNeeded = true;

	private Context gaContext;

	private Map<String, String> activityNameMap = new HashMap<String, String>();

	private GoogleAnalyticsTrackerDelegate tracker;

	private ParameterLoader parameterFetcher;

	/**
	 * Set the various parameters of the GoogleAnaltyicsTracker instance.
	 */
	private void initializeTracker() {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				tracker.setDebug(gaDebug);
				tracker.setDryRun(gaDryRun);
				tracker.setSampleRate(gaSampleRate);
				tracker.setAnonymizeIp(gaAnonymizeIp);
			}  
		});
	}

	/**
	 * Lazily get and initialize the GoogleAnalyticsTracker object.
	 *
	 * @return the GoogleAnalyticsTrackerDelegate object, or null if tracking is
	 *     disabled
	 */
	private GoogleAnalyticsTrackerDelegate getGoogleAnalyticsTracker() {
		if(tracker == null && gaEnabled) {
			tracker = new GoogleAnalyticsTrackerDelegateImpl();
			initializeTracker();
		}

		return tracker;
	}

	/**
	 * For testing only.
	 *
	 * @param d the delegate class to use
	 */
	void setTrackerDelegate(GoogleAnalyticsTrackerDelegate d) {
		if(gaEnabled) {
			tracker = d;
			initializeTracker();
		}
	}

	/**
	 * For testing only.
	 */
	static void clearTracker() {
		instance = null;
	}

	/**
	 * Load the parameters to be used, starting the trackerThread if necessary.
	 */
	private void loadParameters() {
		gaAccountId = parameterFetcher.getString("ga_api_key");

		if(gaAccountId != null) {
			if(gaDebug == null)
				gaDebug = parameterFetcher.getBoolean("ga_debug");

			if(gaDryRun == null)
				gaDryRun = parameterFetcher.getBoolean("ga_dryRun");

			if(gaSampleRate == null)
				gaSampleRate = parameterFetcher.getInt("ga_sampleRate", 100);

			if(gaDispatchPeriod == null) {
				int dispatchPeriod = parameterFetcher.getInt("ga_dispatchPeriod", 20);
				if(gaDispatchPeriod == null || dispatchPeriod != gaDispatchPeriod) {
					gaDispatchPeriod = dispatchPeriod;
					restartSchedulerIfRunning();
				}
			}

			if(autoActivityTracking == null)
				autoActivityTracking = parameterFetcher.getBoolean("ga_auto_activity_tracking");

			if(gaAnonymizeIp == null)
				gaAnonymizeIp = parameterFetcher.getBoolean("ga_anonymizeIp");
		}
		else {
			gaEnabled = false;
		}
	}

	public EasyTracker setDebug(boolean debug) {
		if(gaDebug == null || gaDebug != debug) {
			gaDebug = debug;
			getGoogleAnalyticsTracker().setDebug(gaDebug);
		}
		return this;
	}
	public EasyTracker setDryRun(boolean dryRun) {
		if(gaDryRun == null || gaDryRun != dryRun) {
			gaDryRun = dryRun;
			getGoogleAnalyticsTracker().setDryRun(gaDryRun);
		}
		return this;
	}
	public EasyTracker setSampleRate(int sampleRate) {
		if(gaSampleRate == null || gaSampleRate != sampleRate) {
			gaSampleRate = sampleRate;
			getGoogleAnalyticsTracker().setSampleRate(gaSampleRate);
		}
		return this;
	}
	public EasyTracker setDispatchPeriod(int dispatchPeriod) {
		if(gaDispatchPeriod != dispatchPeriod) {
			gaDispatchPeriod = dispatchPeriod;
			restartSchedulerIfRunning();
		}

		return this;
	}
	public EasyTracker setAnonymizeIp(boolean anonymizeIp) {
		if(gaAnonymizeIp == null || gaAnonymizeIp != anonymizeIp) {
			gaAnonymizeIp = anonymizeIp;
			getGoogleAnalyticsTracker().setAnonymizeIp(gaAnonymizeIp);
		}
		return this;
	}
	public EasyTracker setAutoActivityTracking(boolean autoActivityTracking) {
		this.autoActivityTracking = autoActivityTracking;
		return this;
	}


	/**
	 * Sets the context to use to the applicationContext of the Context ctx.
	 * If the input is not null, this method will then go on to initialize the
	 * EasyTracker Class with parameters from the resource files.  If there is
	 * an accountId specified, this method will enable Google Analytics tracking
	 * and start up the database Thread.  If not, it will leave tracking disabled.
	 *
	 * @param ctx the Context to use to fetch the applicationContext
	 */
	public void setContext(Context context) {
		if(context == null)
			Log.e(LOG_TAG, "Context cannot be null");

		if(gaContext == null) {
			gaContext = context.getApplicationContext();
			parameterFetcher = new ParameterLoaderImpl(gaContext);
			loadParameters();
		}
	}

	/**
	 * Used in testing to allow injection of a mock ParameterLoader.
	 *
	 * @param ctx the Context to use to fetch the applicationContext
	 * @param parameterLoader the ParamterLoader to use
	 */
	void setContext(Context context, ParameterLoader parameterLoader) {
		if(context == null)
			Log.e(LOG_TAG, "Context cannot be null");

		if(gaContext == null) {
			gaContext = context.getApplicationContext();
			parameterFetcher = parameterLoader;
			loadParameters();
		}
	}

	/**
	 * Track the start of an Activity, but only if autoActivityTracking is true.
	 * This method will start a new session if necessary, and will send an empty
	 * event to Google Analytics if autoActivityTracking is false to ensure proper
	 * application-level tracking.  Developers should not call this method
	 * directly.  Extend TrackedActivity (or its other cousins) instead of
	 * Activity to use this method. Note that this method should be called from
	 * the Activity's onStart method.
	 *
	 * @param activity the Activity that is to be tracked
	 */
	public void trackActivityStart(final Activity activity) {
		activitiesActive++;
		final boolean startASession = sessionNeeded;
		sessionNeeded = false;
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				if (startASession) {
					getGoogleAnalyticsTracker().startNewSession(gaAccountId, gaContext);
					if (!autoActivityTracking) {
						// We send an empty event so we get accurate time-on-site info.
						getGoogleAnalyticsTracker().trackEvent("", "", "", 0);
					}
				}
				if (autoActivityTracking) {
					getGoogleAnalyticsTracker().trackPageView(getActivityName(activity));
				}
			}  
		});
	}

	/**
	 * Track Activity restarts due to configuration changes (i.e. orientation
	 * change).  There is no need to start a new session in this case. Note that
	 * this method should be called from the Activity's
	 * onRetainNonConfigurationInstance callback.
	 * <p>
	 * Note that the GoogleAnalytics SDK supports Android versions back to 1.5.
	 * The onRetainNonConfigurationInstance method is deprecated in Android 3.0,
	 * but its replacement is only supported in Android 2.1 and beyond.
	 */
	public void trackActivityRetainNonConfigurationInstance() {
		sessionNeeded = false;
	}

	/**
	 * Track the end of an Activity and/or application.  This is done by sending
	 * an empty event to Google Analytics. Note that this method should be called
	 * from the Activity's onStop callback.
	 *
	 * @param activity the Activity that is to be tracked
	 */
	public void trackActivityStop(final Activity activity) {
		activitiesActive--;
		sessionNeeded = activitiesActive == 0;
		final boolean sendEvent = sessionNeeded;
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				if (sendEvent) {
					// We send an empty event so we get accurate time-on-page/site info.
					getGoogleAnalyticsTracker().trackEvent("", "", "", 0);
				}
			}  
		});
	}

	/**
	 * Look up the Activity's display name (as defined in a String resource named
	 * for the Activity's canonicalName).
	 *
	 * @param activity the Activity Class to look up
	 * @return the defined display name or the canonicalName if the display name
	 *     is not found
	 */
	private String getActivityName(Activity activity) {
		String canonicalName = activity.getClass().getCanonicalName();

		if(activityNameMap.containsKey(canonicalName)) {
			return activityNameMap.get(canonicalName);
		}
		else {
			String name = parameterFetcher.getString(canonicalName);
			if (name == null) {
				name = canonicalName;
			}
			activityNameMap.put(canonicalName, name);
			return name;
		}
	}

	// The following methods are simple pass-through methods for
	// GoogleAnalyticsTracker with the exception that they all are run on a single
	// Thread created to keep database access off the UI Thread.

	/**
	 * Adds an Item to the Transaction identified by Item.orderId.  A new
	 * Transaction will be created if one doesn't already exist.  This call will
	 * overwrite an existing Item object with the same orderId and itemSKU.
	 *
	 * @param item the Item to add
	 */
	public void addItem(final Item item) {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				getGoogleAnalyticsTracker().addItem(item); 
			}
		});
	}

	/**
	 * Adds a Transaction to be sent to Google Analytics.  If a Transaction with
	 * the same orderId already exists, it will be replaced with this Transaction.
	 *
	 * @param transaction the Transaction to add
	 */
	public void addTransaction(final Transaction transaction) {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				getGoogleAnalyticsTracker().addTransaction(transaction); 
			}
		});
	}

	/**
	 * Clears all pending Transactions and Items from the internal queue.  This
	 * method will not affect Transactions and Items already sent with the
	 * trackTransactions() call.
	 */
	public void clearTransactions() {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				getGoogleAnalyticsTracker().clearTransactions(); 
			}
		});
	}

	/**
	 * Dispatch up to 30 queued hits to the Google Analytics servers, but only if
	 * another dispatch is not in progress.
	 */
	public void dispatch() {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				getGoogleAnalyticsTracker().dispatch(); 
			}
		});
	}

	/**
	 * Set the campaign referral to the values in the input.  If the input is
	 * not valid, the method fails.  If the input is valid, the referrer
	 * information is changed and a new session is started.  Each hit after
	 * a successful call to setReferrer will have the referrer information
	 * attached.
	 *
	 * @param referrer the campaign referral information to set
	 */
	public void setReferrer(final String referrer) {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				getGoogleAnalyticsTracker().setReferrer(referrer); 
			}
		});
	}

	/**
	 * Start a new session using the parameters stored in the EasyTracker Class.
	 * This method flags GoogleAnalyticsTracker to start a new session with the
	 * next hit it generates.  As such, calling this multiple times in a row will
	 * have the same effect of calling it once.
	 */
	public void startNewSession() {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				// If this gets run, we know that gaAccountId and gaContext are not null
				// as the method queueToDbThreadIfEnabled will check the gaEnabled flag
				// before queuing anything.  That flag, in turn is set to true only if
				// gaContext and getAccountId are both non-null.
				getGoogleAnalyticsTracker().startNewSession(gaAccountId, gaContext);
			}  
		});
	}

	/**
	 * Stops the automatic dispatch from continuing to run.
	 */
	public void stopSession() {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				getGoogleAnalyticsTracker().stopSession();
			}  
		});
	}

	/**
	 * Track an Event.
	 *
	 * @param category the category of the event
	 * @param action the action of the event
	 * @param label the label of the event, can be null
	 * @param value the value of the event
	 */
	public void trackEvent(final String category, final String action, final String label, final int value) {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				getGoogleAnalyticsTracker().trackEvent(category, action, label, value); 
			}
		});
	}

	/**
	 * Track a pageview, which is analogous to an Activity.  If null is passed
	 * in as input, no pageview will be tracked.
	 *
	 * @param name The name of the Activity or view to be tracked.
	 */
	public void trackPageView(final String name) {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				getGoogleAnalyticsTracker().trackPageView(name); 
			}
		});
	}

	/**
	 * Sends all the pending Transactions and Items to dispatch.  Once this method
	 * is called, all the Transactions and Items added previously will not be
	 * cleared by a clearTransactions call and will eventually be dispatched to
	 * the GoogleAnalytics servers. 
	 */
	public void trackTransactions() {
		addToScheduler(new Runnable() {
			@Override
			public void run() {
				getGoogleAnalyticsTracker().trackTransactions(); 
			}
		});
	}

	// This section defines classes, variables and methods used to manage 
	// threading for GoogleAnalyticsTracker calls.

	class LowPriorityThreadFactory implements ThreadFactory {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "Analytics");
			t.setPriority(Thread.MIN_PRIORITY);
			return t;
		}
	}

	/**
	 * Queue the GoogleAnalytics call to the database thread, but only if
	 * GoogleAnalytics has been enabled.
	 *
	 * @param r the Runnable to execute
	 */
	private ScheduledExecutorService scheduler;
	private Object schedulerLock = new Object();

	private void addToScheduler(Runnable r) {
		if(gaEnabled) {
			synchronized(schedulerLock) {
				initializeSchedulerIfNeeded();

				scheduler.submit(r);
			}
		}
	}

	private void restartSchedulerIfRunning() {
		// Restart the scheduler if we need to
		if(scheduler != null) {
			synchronized(schedulerLock) {
				if(scheduler != null) {
					if(!scheduler.isShutdown())
						scheduler.shutdown();
					scheduler = null;
					initializeSchedulerIfNeeded();
				}
			}
		}
	}

	private void initializeSchedulerIfNeeded() {
		if(scheduler == null || scheduler.isShutdown()) {
			scheduler = Executors.newScheduledThreadPool(1, new LowPriorityThreadFactory());

			int period = 20;
			if(gaDispatchPeriod != null)
				period = gaDispatchPeriod;

			// Schedule our dispatch
			scheduler.scheduleAtFixedRate(new Runnable() {
				@Override
				public void run() {
					dispatch();
				}
			}, period, period, TimeUnit.SECONDS);
		}
	}
}
