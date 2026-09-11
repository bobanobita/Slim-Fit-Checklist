package com.phuong.screenoff;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;

public class ScreenOffAccessibilityService extends AccessibilityService {
    private static volatile ScreenOffAccessibilityService instance;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    public static boolean lockScreenNow() {
        ScreenOffAccessibilityService service = instance;
        if (service == null) return false;
        return service.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
    }
}
