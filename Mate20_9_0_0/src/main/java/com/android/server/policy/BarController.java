package com.android.server.policy;

import android.app.StatusBarManager;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.server.LocalServices;
import com.android.server.policy.WindowManagerPolicy.WindowState;
import com.android.server.statusbar.StatusBarManagerInternal;
import com.android.server.wm.WindowManagerInternal;
import java.io.PrintWriter;

public class BarController {
    private static final boolean DEBUG = false;
    private static final int MSG_NAV_BAR_VISIBILITY_CHANGED = 1;
    private static final int TRANSIENT_BAR_HIDING = 3;
    private static final int TRANSIENT_BAR_NONE = 0;
    private static final int TRANSIENT_BAR_SHOWING = 2;
    private static final int TRANSIENT_BAR_SHOW_REQUESTED = 1;
    private static final int TRANSLUCENT_ANIMATION_DELAY_MS = 1000;
    private final Rect mContentFrame = new Rect();
    protected final Handler mHandler;
    private long mLastTranslucent;
    private boolean mNoAnimationOnNextShow;
    private boolean mPendingShow;
    private final Object mServiceAquireLock = new Object();
    private boolean mSetUnHideFlagWhenNextTransparent;
    private boolean mShowTransparent;
    private int mState = 0;
    protected StatusBarManagerInternal mStatusBarInternal;
    private final int mStatusBarManagerId;
    protected final String mTag;
    private int mTransientBarState;
    private final int mTransientFlag;
    private final int mTranslucentFlag;
    private final int mTranslucentWmFlag;
    private final int mTransparentFlag;
    private final int mUnhideFlag;
    private OnBarVisibilityChangedListener mVisibilityChangeListener;
    protected WindowState mWin;

    private class BarHandler extends Handler {
        private BarHandler() {
        }

        /* synthetic */ BarHandler(BarController x0, AnonymousClass1 x1) {
            this();
        }

        public void handleMessage(Message msg) {
            boolean z = true;
            if (msg.what == 1) {
                if (msg.arg1 == 0) {
                    z = false;
                }
                boolean visible = z;
                if (BarController.this.mVisibilityChangeListener != null) {
                    BarController.this.mVisibilityChangeListener.onBarVisibilityChanged(visible);
                }
            }
        }
    }

    interface OnBarVisibilityChangedListener {
        void onBarVisibilityChanged(boolean z);
    }

    public BarController(String tag, int transientFlag, int unhideFlag, int translucentFlag, int statusBarManagerId, int translucentWmFlag, int transparentFlag) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("BarController.");
        stringBuilder.append(tag);
        this.mTag = stringBuilder.toString();
        this.mTransientFlag = transientFlag;
        this.mUnhideFlag = unhideFlag;
        this.mTranslucentFlag = translucentFlag;
        this.mStatusBarManagerId = statusBarManagerId;
        this.mTranslucentWmFlag = translucentWmFlag;
        this.mTransparentFlag = transparentFlag;
        this.mHandler = new BarHandler(this, null);
    }

    public void setWindow(WindowState win) {
        this.mWin = win;
    }

    public void setContentFrame(Rect frame) {
        this.mContentFrame.set(frame);
    }

    public void setShowTransparent(boolean transparent) {
        if (transparent != this.mShowTransparent) {
            this.mShowTransparent = transparent;
            this.mSetUnHideFlagWhenNextTransparent = transparent;
            this.mNoAnimationOnNextShow = true;
        }
    }

    public void showTransient() {
        if (this.mWin != null) {
            setTransientBarState(1);
        }
    }

    public boolean isTransientShowing() {
        return this.mTransientBarState == 2;
    }

    public boolean isTransientShowRequested() {
        return this.mTransientBarState == 1;
    }

    public boolean wasRecentlyTranslucent() {
        return SystemClock.uptimeMillis() - this.mLastTranslucent < 1000;
    }

    public void adjustSystemUiVisibilityLw(int oldVis, int vis) {
        if (this.mWin != null && this.mTransientBarState == 2 && (this.mTransientFlag & vis) == 0) {
            setTransientBarState(3);
            setBarShowingLw(false);
        } else if (this.mWin != null && (this.mUnhideFlag & oldVis) != 0 && (this.mUnhideFlag & vis) == 0) {
            setBarShowingLw(true);
        }
    }

    public int applyTranslucentFlagLw(WindowState win, int vis, int oldVis) {
        if (this.mWin == null) {
            return vis;
        }
        if (win == null || (win.getAttrs().privateFlags & 512) != 0) {
            return ((~this.mTransparentFlag) & (((~this.mTranslucentFlag) & vis) | (this.mTranslucentFlag & oldVis))) | (this.mTransparentFlag & oldVis);
        }
        int fl = PolicyControl.getWindowFlags(win, 0);
        if ((this.mTranslucentWmFlag & fl) != 0) {
            vis |= this.mTranslucentFlag;
        } else {
            vis &= ~this.mTranslucentFlag;
        }
        if ((Integer.MIN_VALUE & fl) == 0 || !isTransparentAllowed(win)) {
            return vis & (~this.mTransparentFlag);
        }
        boolean isEmui = (win.getAttrs() == null || win.getAttrs().isEmuiStyle == 0) ? false : true;
        if (isEmui) {
            return vis | 1073741824;
        }
        return vis | this.mTransparentFlag;
    }

    boolean isTransparentAllowed(WindowState win) {
        return win == null || !win.isLetterboxedOverlappingWith(this.mContentFrame);
    }

    public boolean setBarShowingLw(boolean show) {
        if (this.mWin == null) {
            return false;
        }
        boolean z = true;
        if (show && this.mTransientBarState == 3) {
            this.mPendingShow = true;
            return false;
        }
        boolean change;
        int state;
        WindowManagerInternal wmi = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        boolean isCoverOpen = wmi.isCoverOpen();
        boolean wasVis = this.mWin.isVisibleLw();
        boolean wasAnim = this.mWin.isAnimatingLw();
        if (isCoverOpen && wmi.isKeyguardLocked()) {
            this.mNoAnimationOnNextShow = true;
        }
        boolean z2;
        if (show) {
            WindowState windowState = this.mWin;
            z2 = (this.mNoAnimationOnNextShow || skipAnimation()) ? false : true;
            change = windowState.showLw(z2);
        } else {
            change = this.mWin;
            z2 = (this.mNoAnimationOnNextShow || skipAnimation()) ? false : true;
            change = change.hideLw(z2);
        }
        this.mNoAnimationOnNextShow = false;
        if (isCoverOpen) {
            state = computeStateLw(wasVis, wasAnim, this.mWin, change);
        } else {
            state = computeStateNoAnimLw(wasVis, wasAnim, this.mWin, change);
        }
        boolean stateChanged = updateStateLw(state);
        if (change && this.mVisibilityChangeListener != null) {
            this.mHandler.obtainMessage(1, show, 0).sendToTarget();
        } else if (PhoneWindowManager.NAV_TAG == this.mTag) {
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("show:");
            stringBuilder.append(show);
            stringBuilder.append(",change:");
            stringBuilder.append(change);
            stringBuilder.append(",mVisibilityChangeListener:");
            stringBuilder.append(this.mVisibilityChangeListener);
            stringBuilder.append(",mTransientBarState:");
            stringBuilder.append(this.mTransientBarState);
            Slog.i(str, stringBuilder.toString());
        }
        if (!(change || stateChanged)) {
            z = false;
        }
        return z;
    }

    private int computeStateNoAnimLw(boolean wasVis, boolean wasAnim, WindowState win, boolean change) {
        if (win.hasDrawnLw()) {
            boolean vis = win.isVisibleLw();
            boolean anim = win.isAnimatingLw();
            if (this.mState == 0 && !vis) {
                return 2;
            }
            if (this.mState == 2 && vis) {
                return 0;
            }
        }
        return this.mState;
    }

    void setOnBarVisibilityChangedListener(OnBarVisibilityChangedListener listener, boolean invokeWithState) {
        this.mVisibilityChangeListener = listener;
        if (invokeWithState) {
            this.mHandler.obtainMessage(1, this.mState == 0 ? 1 : 0, 0).sendToTarget();
        }
    }

    protected boolean skipAnimation() {
        return false;
    }

    private int computeStateLw(boolean wasVis, boolean wasAnim, WindowState win, boolean change) {
        if (win.isDrawnLw()) {
            boolean vis = win.isVisibleLw();
            boolean anim = win.isAnimatingLw();
            if (this.mState == 1 && !change && !vis) {
                return 2;
            }
            if (this.mState == 2 && vis) {
                return 0;
            }
            if (change) {
                if (wasVis && vis && !wasAnim && anim) {
                    return 1;
                }
                return 0;
            }
        }
        return this.mState;
    }

    private boolean updateStateLw(final int state) {
        if (state == this.mState) {
            return false;
        }
        this.mState = state;
        this.mHandler.post(new Runnable() {
            public void run() {
                StatusBarManagerInternal statusbar = BarController.this.getStatusBarInternal();
                if (statusbar != null) {
                    statusbar.setWindowState(BarController.this.mStatusBarManagerId, state);
                }
            }
        });
        return true;
    }

    public boolean checkHiddenLw() {
        if (this.mWin != null && this.mWin.isDrawnLw()) {
            if (!(this.mWin.isVisibleLw() || this.mWin.isAnimatingLw())) {
                updateStateLw(2);
            }
            if (this.mTransientBarState == 3 && !this.mWin.isVisibleLw()) {
                setTransientBarState(0);
                if (this.mPendingShow) {
                    setBarShowingLw(true);
                    this.mPendingShow = false;
                }
                return true;
            }
        }
        return false;
    }

    public boolean checkShowTransientBarLw() {
        if (this.mTransientBarState == 2 || this.mTransientBarState == 1 || this.mWin == null || this.mWin.isDisplayedLw()) {
            return false;
        }
        return true;
    }

    public int updateVisibilityLw(boolean transientAllowed, int oldVis, int vis) {
        if (this.mWin == null) {
            return vis;
        }
        if (isTransientShowing() || isTransientShowRequested()) {
            if (transientAllowed) {
                vis |= this.mTransientFlag;
                if ((this.mTransientFlag & oldVis) == 0) {
                    vis |= this.mUnhideFlag;
                }
                setTransientBarState(2);
            } else {
                setTransientBarState(0);
            }
        }
        if (this.mShowTransparent) {
            vis |= this.mTransparentFlag;
            if (this.mSetUnHideFlagWhenNextTransparent) {
                vis |= this.mUnhideFlag;
                this.mSetUnHideFlagWhenNextTransparent = false;
            }
        }
        if (this.mTransientBarState != 0) {
            vis = (vis | this.mTransientFlag) & -2;
        }
        if (!((this.mTranslucentFlag & vis) == 0 && (this.mTranslucentFlag & oldVis) == 0 && ((vis | oldVis) & this.mTransparentFlag) == 0)) {
            this.mLastTranslucent = SystemClock.uptimeMillis();
        }
        return vis;
    }

    private void setTransientBarState(int state) {
        if (PhoneWindowManager.NAV_TAG == this.mTag) {
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setTransientBarState mWin:");
            stringBuilder.append(this.mWin);
            stringBuilder.append(",state:");
            stringBuilder.append(state);
            stringBuilder.append(",mTransientBarState:");
            stringBuilder.append(this.mTransientBarState);
            Slog.i(str, stringBuilder.toString());
        }
        if (this.mWin != null && state != this.mTransientBarState) {
            if (this.mTransientBarState == 2 || state == 2) {
                this.mLastTranslucent = SystemClock.uptimeMillis();
            }
            this.mTransientBarState = state;
        }
    }

    protected StatusBarManagerInternal getStatusBarInternal() {
        StatusBarManagerInternal statusBarManagerInternal;
        synchronized (this.mServiceAquireLock) {
            if (this.mStatusBarInternal == null) {
                this.mStatusBarInternal = (StatusBarManagerInternal) LocalServices.getService(StatusBarManagerInternal.class);
            }
            statusBarManagerInternal = this.mStatusBarInternal;
        }
        return statusBarManagerInternal;
    }

    private static String transientBarStateToString(int state) {
        if (state == 3) {
            return "TRANSIENT_BAR_HIDING";
        }
        if (state == 2) {
            return "TRANSIENT_BAR_SHOWING";
        }
        if (state == 1) {
            return "TRANSIENT_BAR_SHOW_REQUESTED";
        }
        if (state == 0) {
            return "TRANSIENT_BAR_NONE";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown state ");
        stringBuilder.append(state);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        proto.write(1159641169921L, this.mState);
        proto.write(1159641169922L, this.mTransientBarState);
        proto.end(token);
    }

    public void dump(PrintWriter pw, String prefix) {
        if (this.mWin != null) {
            pw.print(prefix);
            pw.println(this.mTag);
            pw.print(prefix);
            pw.print("  ");
            pw.print("mState");
            pw.print('=');
            pw.println(StatusBarManager.windowStateToString(this.mState));
            pw.print(prefix);
            pw.print("  ");
            pw.print("mTransientBar");
            pw.print('=');
            pw.println(transientBarStateToString(this.mTransientBarState));
            pw.print(prefix);
            pw.print("  mContentFrame=");
            pw.println(this.mContentFrame);
        }
    }

    public boolean isTransientHiding() {
        return this.mTransientBarState == 3;
    }

    public void sethwTransientBarState(int state) {
        setTransientBarState(state);
    }
}
