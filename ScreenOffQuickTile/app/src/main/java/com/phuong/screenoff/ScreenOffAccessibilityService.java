package com.phuong.screenoff;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.TouchInteractionController;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.SystemClock;
import android.view.Display;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityEvent;
import android.accessibilityservice.AccessibilityServiceInfo;

public class ScreenOffAccessibilityService extends AccessibilityService {
    private static final String PREFS = "screenoff_settings";
    private static final String KEY_DOUBLE_TAP_HOME = "double_tap_home";
    private static final long MAX_TAP_DURATION_MS = 240L;
    private static final long MIN_DOUBLE_TAP_GAP_MS = 30L;
    private static final long MAX_DOUBLE_TAP_GAP_MS = 360L;

    private static volatile ScreenOffAccessibilityService instance;

    private SharedPreferences prefs;
    private String homePackage;
    private String currentPackage;

    private TouchInteractionController touchController;
    private TouchInteractionController.Callback touchCallback;
    private boolean callbackRegistered;

    private long currentDownTime;
    private float currentDownX;
    private float currentDownY;
    private boolean currentHomeInteraction;

    private long lastTapEndTime;
    private float lastTapX;
    private float lastTapY;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener =
            (sharedPreferences, key) -> {
                if (KEY_DOUBLE_TAP_HOME.equals(key)) {
                    configureTouchController(isDoubleTapHomeEnabled());
                }
            };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        homePackage = resolveHomePackage();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(prefListener);
        configureTouchController(isDoubleTapHomeEnabled());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;

        CharSequence packageName = event.getPackageName();
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && packageName != null) {
            currentPackage = packageName.toString();
            if (!isHomeScreen()) clearTapCandidate();
        }
    }

    private void configureTouchController(boolean enabled) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;
        }

        AccessibilityServiceInfo info = getServiceInfo();
        if (info == null) return;

        if (enabled) {
            info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
            setServiceInfo(info);

            if (touchController == null) {
                touchController = getTouchInteractionController(Display.DEFAULT_DISPLAY);
            }
            if (touchCallback == null) {
                touchCallback = new TouchInteractionController.Callback() {
                    @Override
                    public void onMotionEvent(MotionEvent event) {
                        handleMotionEvent(event);
                    }

                    @Override
                    public void onStateChanged(int state) {
                        handleTouchStateChanged(state);
                    }
                };
            }
            if (!callbackRegistered) {
                touchController.registerCallback(getMainExecutor(), touchCallback);
                callbackRegistered = true;
            }
        } else {
            if (touchController != null && touchCallback != null && callbackRegistered) {
                touchController.unregisterCallback(touchCallback);
                callbackRegistered = false;
            }
            info.flags &= ~AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE;
            setServiceInfo(info);
            resetTouchState();
        }
    }

    private void handleMotionEvent(MotionEvent event) {
        if (event == null || event.getActionMasked() != MotionEvent.ACTION_DOWN) return;

        if (!isDoubleTapHomeEnabled() || !isHomeScreen()) {
            clearTapCandidate();
            delegateCurrentInteraction();
            return;
        }

        long now = event.getEventTime();
        float x = event.getX();
        float y = event.getY();

        long gap = lastTapEndTime > 0L ? now - lastTapEndTime : Long.MAX_VALUE;
        boolean closeEnough = isWithinDoubleTapDistance(x, y);

        if (gap >= MIN_DOUBLE_TAP_GAP_MS && gap <= MAX_DOUBLE_TAP_GAP_MS && closeEnough) {
            clearTapCandidate();
            currentHomeInteraction = false;
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
            return;
        }

        currentDownTime = now;
        currentDownX = x;
        currentDownY = y;
        currentHomeInteraction = true;

        // Pass the first interaction through immediately. This keeps normal Home-screen
        // taps and swipes working. On the next interaction we get another ACTION_DOWN,
        // which lets us recognize a double tap without an overlay.
        delegateCurrentInteraction();
    }

    private void handleTouchStateChanged(int state) {
        if (state != TouchInteractionController.STATE_CLEAR || !currentHomeInteraction) return;

        long now = SystemClock.uptimeMillis();
        long duration = currentDownTime > 0L ? now - currentDownTime : Long.MAX_VALUE;

        if (duration >= 0L && duration <= MAX_TAP_DURATION_MS && isHomeScreen()) {
            lastTapEndTime = now;
            lastTapX = currentDownX;
            lastTapY = currentDownY;
        } else {
            clearTapCandidate();
        }

        currentHomeInteraction = false;
        currentDownTime = 0L;
    }

    private void delegateCurrentInteraction() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || touchController == null) return;
        try {
            if (touchController.getState() == TouchInteractionController.STATE_TOUCH_INTERACTING) {
                touchController.requestDelegating();
            }
        } catch (IllegalStateException ignored) {
            // If the framework already changed state, leave the interaction to the system.
        }
    }

    private boolean isWithinDoubleTapDistance(float x, float y) {
        if (lastTapEndTime <= 0L) return false;
        int slop = ViewConfiguration.get(this).getScaledDoubleTapSlop();
        float dx = x - lastTapX;
        float dy = y - lastTapY;
        return (dx * dx + dy * dy) <= (float) slop * slop;
    }

    private boolean isDoubleTapHomeEnabled() {
        SharedPreferences p = prefs != null ? prefs : getSharedPreferences(PREFS, MODE_PRIVATE);
        return p.getBoolean(KEY_DOUBLE_TAP_HOME, true);
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

    private void clearTapCandidate() {
        lastTapEndTime = 0L;
        lastTapX = 0f;
        lastTapY = 0f;
    }

    private void resetTouchState() {
        currentDownTime = 0L;
        currentDownX = 0f;
        currentDownY = 0f;
        currentHomeInteraction = false;
        clearTapCandidate();
    }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        if (prefs != null) prefs.unregisterOnSharedPreferenceChangeListener(prefListener);
        if (touchController != null && touchCallback != null && callbackRegistered) {
            touchController.unregisterCallback(touchCallback);
            callbackRegistered = false;
        }
        if (instance == this) instance = null;
        resetTouchState();
        super.onDestroy();
    }

    public static boolean lockScreenNow() {
        ScreenOffAccessibilityService service = instance;
        if (service == null) return false;
        return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }
}
