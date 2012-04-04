# EasyTracker

This is the EasyTracker library we use. It is based on [Google's version](http://code.google.com/apis/analytics/docs/mobile/android.html#eztracker).

All documentation they provide applies to this library.

## What's different

There are a few differences. Some arised from EasyTracker's unexpected behavior, others from necessity.

### Avoids exploding

Google's Analytics library throws a few exceptions from time to time. We were never able to reproduce this, and the lib is not open source which doesn't help.

Our version of EasyTracker just keeps going even if the Analytics lib blows off. Not force closing on the users face because of Analytics undoubtly makes up for a few missed events.

### Gentle with the UI thread

Google's EasyTracker tries hard to stay off of the UI thread and generally succeeds. The problem arises from the Analytics lib itself, which has the bad habit of dispatching events on the UI thread when auto-dispatching is enabled.

Our version of EasyTracker uses a `ScheduledExecutorService` to simulate this automatic behavior, and timely dispatches all events **on a separate, low-priority thread**.

### Flexible

All paremeters which are normally set in XML can also be set from Java code.

```java
EasyTracker.getTracker().setDebug(false);
EasyTracker.getTracker().setDryMode(true);
// …
```

And so on. It just works. We also guarantee that your XML options will never override these options.