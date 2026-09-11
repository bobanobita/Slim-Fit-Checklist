package com.phuong.screenoff;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.view.accessibility.AccessibilityEvent;

public class ScreenOffAccessibilityService extends AccessibilityService {
    private static final String PREFS = "screenoff_settings";
    private static final String KEY_DOUBLE_TAP_HOME = "double_tap_home";
    private static final long MAX_TAP_DURATION_MS = 220L;
    private static final long MIN_DOUBLE_TAP_GAP_MS = 40L;
    private static final long MAX_DOUBLE_TAP_GAP_MS = 360L;

    private static volatile ScreenOffAccessibilityService instance;

    private String homePackage;
    private String currentPackage;
    private long touchStartTime;
    private long lastTapEndTime;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        homePackage = resolveHomePackage();
        resetTapState();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        int type = event.getEventType();
        CharSequence packageName = event.getPackageName();

        if (type == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && packageName != null) {
            currentPackage = packageName.toString();
            if (!isHomeScreen()) resetTapState();
            return;
        }

        if (!isDoubleTapHomeEnabled() || !isHomeScreen()) {
            resetTapState();
            return;
        }

        if (type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_START) {
            touchStartTime = event.getEventTime();
            return;
        }

        if (type == AccessibilityEvent.TYPE_TOUCH_INTERACTION_END) {
            long endTime = event.getEventTime();
            long duration = touchStartTime > 0L ? endTime - touchStartTime : Long.MAX_VALUE;
            touchStartTime = 0L;

            // Ignore long presses and most swipes. We only want two short taps.
            if (duration < 0L || duration > MAX_TAP_DURATION_MS) {
                lastTapEndTime = 0L;
                return;
            }

            long gap = lastTapEndTime > 0L ? endTime - lastTapEndTime : Long.MAX_VALUE;
            if (gap >= MIN_DOUBLE_TAP_GAP_MS && gap <= MAX_DOUBLE_TAP_GAP_MS) {
                lastTapEndTime = 0L;
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
            } else {
                lastTapEndTime = endTime;
            }
        }
    }

    private boolean isDoubleTapHomeEnabled() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        return prefs.getBoolean(KEY_DOUBLE_TAP_HOME, true);
    }

    private boolean isHomeScreen() {
        if (homePackage == null) homePackage = resolveHomePackage();
        return homePackage != null && homePackage.equals(currentPackage);
    }

    private String resolveHomePackage() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        ResolveInfo info = getPackageManager().resolveActivity(intent, 0);
        if (info == null || info.activityInfo == null) return null;
        return info.activityInfo.packageName;
    }

    private void resetTapState() {
        touchStartTime = 0L;
        lastTapEndTime = 0L;
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        resetTapState();
        super.onDestroy();
    }

    public static boolean lockScreenNow() {
        ScreenOffAccessibilityService service = instance;
        if (service == null) return false;
        return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }
}
