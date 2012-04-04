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

import android.content.Context;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;
import com.google.android.apps.analytics.Item;
import com.google.android.apps.analytics.Transaction;

public class GoogleAnalyticsTrackerDelegateImpl implements GoogleAnalyticsTrackerDelegate {
	@Override
	public void startNewSession(String accountId, Context ctx) {
		try {
			getTracker().startNewSession(accountId, ctx);
		}
		catch(Exception ignore) {}
	}

	@Override
	public void trackEvent(String category, String action, String label, int value) {
		try {
			getTracker().trackEvent(category, action, label, value);
		}
		catch(Exception ignore) {}
	}

	@Override
	public void trackPageView(String pageUrl) {
		try {
			getTracker().trackPageView(pageUrl);
		}
		catch(Exception ignore) {}
	}

	@Override
	public boolean dispatch() {
		try {
			return getTracker().dispatch();
		}
		catch(Exception ignore) {
			return false;
		}
	}

	@Override
	public void stopSession() {
		try {
			getTracker().stopSession();
		}
		catch(Exception ignore) {}
	}

	@Override
	public boolean setCustomVar(int index, String name, String value, int scope) {
		try {
			return getTracker().setCustomVar(index, name, value, scope);
		}
		catch(Exception ignore) {
			return false;
		}
	}

	@Override
	public boolean setCustomVar(int index, String name, String value) {
		try {
			return getTracker().setCustomVar(index, name, value);
		}
		catch(Exception ignore) {
			return false;
		}
	}

	@Override
	public void addTransaction(Transaction transaction) {
		try {
			getTracker().addTransaction(transaction);
		}
		catch(Exception ignore) {}
	}

	@Override
	public void addItem(Item item) {
		try {
			getTracker().addItem(item);
		}
		catch(Exception ignore) {}
	}

	@Override
	public void trackTransactions() {
		try {
			getTracker().trackTransactions();
		}
		catch(Exception ignore) {}
	}

	@Override
	public void clearTransactions() {
		try {
			getTracker().clearTransactions();
		}
		catch(Exception ignore) {}
	}

	@Override
	public void setAnonymizeIp(boolean anonymizeIp) {
		try {
			getTracker().setAnonymizeIp(anonymizeIp);
		}
		catch(Exception ignore) {}
	}

	@Override
	public void setSampleRate(int sampleRate) {
		try {
			getTracker().setSampleRate(sampleRate);
		}
		catch(Exception ignore) {}
	}

	@Override
	public boolean setReferrer(String referrer) {
		try {
			return getTracker().setReferrer(referrer);
		}
		catch(Exception ignore) {
			return false;
		}
	}

	@Override
	public void setDebug(boolean debug) {
		try {
			getTracker().setDebug(debug);
		}
		catch(Exception ignore) {}
	}

	@Override
	public void setDryRun(boolean dryRun) {
		try {
			getTracker().setDryRun(dryRun);
		}
		catch(Exception ignore) {}
	}
	
	private GoogleAnalyticsTracker tracker;
	private Object trackerLock = new Object();
	
	private GoogleAnalyticsTracker getTracker() {
		if(tracker == null) {
			synchronized(trackerLock) {
				if(tracker == null) {
					tracker = GoogleAnalyticsTracker.getInstance();
				}
			}
		}
		
		return tracker;
	}
}
