package com.gathin.tvhome;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;

/**
 * Maps the remote's branded IR buttons that the firmware leaves unmapped (the YouTube key arrives
 * as KEY_FN_F4, kernel code 469 — no .kl entry claims it, so nothing acts on it).
 *
 * Works like Button Mapper: the service asks to filter key events and consumes the button,
 * launching the app the label promises. Everything else passes through untouched.
 */
public class ButtonHookService extends AccessibilityService {

    private static final String TAG = "ButtonHook";
    /** IR scancode 0x4fb49 → kernel KEY_FN_F4 (469): the YouTube button. */
    private static final int SCAN_YOUTUBE = 469;
    /** IR scancode 0x4fb4e → the Netflix button, once we confirm it from getevent. */
    private static final int SCAN_NETFLIX = 0x4fb4e;
    private static final long MIN_INTERVAL_MS = 800;

    private long lastLaunchMs;

    @Override
    protected void onServiceConnected() {
        AccessibilityServiceInfo info = getServiceInfo();
        info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS;
        setServiceInfo(info);
        Log.i(TAG, "connected");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
        int scan = event.getScanCode();
        Log.d(TAG, "key code=" + event.getKeyCode() + " scan=" + scan);
        String pkg = null;
        if (scan == SCAN_YOUTUBE) {
            // The classic YouTube app first; "tvunplugged" is the YouTube TV live service.
            pkg = "com.google.android.youtube.tv";
        } else if (scan == SCAN_NETFLIX) {
            pkg = "com.netflix.ninja";
        }
        if (pkg == null) return false;

        long now = SystemClock.uptimeMillis();
        if (now - lastLaunchMs < MIN_INTERVAL_MS) return true;   // IR sends a burst; launch once
        lastLaunchMs = now;
        launch(pkg);
        return true;   // consume: nothing else should see this button
    }

    private void launch(String pkg) {
        Intent i = getLeanbackIntent(pkg);
        if (i == null && pkg.equals("com.google.android.youtube.tv")) {
            i = getLeanbackIntent("com.google.android.youtube.tvunplugged");   // fallback
        }
        if (i == null) return;
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
        } catch (Exception e) {
            Log.w(TAG, "launch failed", e);
        }
    }

    private Intent getLeanbackIntent(String pkg) {
        try {
            return getPackageManager().getLeanbackLaunchIntentForPackage(pkg);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }
}
