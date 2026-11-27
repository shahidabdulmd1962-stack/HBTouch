package com.hb.touch;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class DoubleTapService extends AccessibilityService {

    static volatile String lastReason = "No tap checked yet";

    private WindowManager wm;
    private View overlay;
    private long lastTap = 0;
    private int sw, sh;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        overlay = new View(this);
        overlay.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_OUTSIDE) {
                handleTap();
            }
            return false;
        });

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                1, 1,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        wm.addView(overlay, lp);
    }

    private void handleTap() {
        sw = getResources().getDisplayMetrics().widthPixels;
        sh = getResources().getDisplayMetrics().heightPixels;

        String why = blockReason();
        if ("SKIP".equals(why)) return;
        if (why != null) {
            lastReason = "Blocked: " + why;
            lastTap = 0;
            return;
        }

        long now = SystemClock.uptimeMillis();
        long gap = now - lastTap;
        if (lastTap != 0 && gap > 40 && gap <= ViewConfiguration.getDoubleTapTimeout()) {
            lastTap = 0;
            lastReason = "Plain home: screen locked";
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
        } else {
            lastTap = now;
            lastReason = "Plain home: first tap";
        }
    }

    /** null = plain home, otherwise the reason it is blocked */
    private String blockReason() {
        KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        if (km != null && km.isKeyguardLocked()) return "lock screen";

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "no active window";
        try {
            CharSequence pkg = root.getPackageName();
            if (pkg == null) return "no package";
            if (pkg.toString().equals(getPackageName())) return "SKIP";
            if (!pkg.toString().equals(homePackage())) {
                return "not home launcher (" + pkg + ")";
            }
            return scan(root, 0);
        } finally {
            root.recycle();
        }
    }

    private String scan(AccessibilityNodeInfo n, int depth) {
        if (depth > 30) return null;

        if (n.isVisibleToUser()) {
            String cls = n.getClassName() == null ? "" : n.getClassName().toString();
            String id = n.getViewIdResourceName() == null
                    ? "" : n.getViewIdResourceName().toLowerCase();

            Rect r = new Rect();
            n.getBoundsInScreen(r);
            int vw = Math.max(0, Math.min(r.right, sw) - Math.max(r.left, 0));
            int vh = Math.max(0, Math.min(r.bottom, sh) - Math.max(r.top, 0));

            boolean recents = id.contains("overview") || id.contains("recents")
                    || id.contains("task_view") || id.contains("snapshot")
                    || cls.contains("RecentsView") || cls.contains("TaskView");
            boolean drawer = cls.contains("RecyclerView") || cls.contains("ListView")
                    || id.contains("all_apps") || id.contains("apps_list")
                    || id.contains("apps_view") || id.contains("widgets");
            boolean folder = id.contains("folder");

            String hit = null;
            if (recents && vw >= sw * 0.2 && vh >= sh * 0.2) hit = "recents";
            else if (drawer && vw >= sw * 0.5 && vh >= sh * 0.4) hit = "drawer/list";
            else if (folder && vw >= sw * 0.3 && vh >= sh * 0.2) hit = "folder";

            if (hit != null) {
                String shortCls = cls.substring(cls.lastIndexOf('.') + 1);
                return hit + " | id=" + id + " | class=" + shortCls
                        + " | visible=" + vw + "x" + vh;
            }
        }

        for (int i = 0; i < n.getChildCount(); i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c == null) continue;
            String found = scan(c, depth + 1);
            c.recycle();
            if (found != null) return found;
        }
        return null;
    }

    private String homePackage() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        ResolveInfo ri = getPackageManager().resolveActivity(home, 0);
        return (ri != null && ri.activityInfo != null) ? ri.activityInfo.packageName : "";
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) { }

    @Override
    public void onInterrupt() { }

    @Override
    public void onDestroy() {
        try { if (wm != null && overlay != null) wm.removeView(overlay); } catch (Exception ignored) { }
        super.onDestroy();
    }
}
