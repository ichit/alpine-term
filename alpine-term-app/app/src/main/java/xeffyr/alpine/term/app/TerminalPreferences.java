package xeffyr.alpine.term.app;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import xeffyr.alpine.term.BuildConfig;
import xeffyr.alpine.term.terminal.EmulatorDebug;
import xeffyr.alpine.term.terminal.TerminalSession;

@SuppressWarnings("WeakerAccess")
final class TerminalPreferences {

    private static final String CURRENT_SESSION_KEY = "current_session";
    private static final String SHOW_EXTRA_KEYS_KEY = "show_extra_keys";
    private static final String SCREEN_ALWAYS_ON_KEY = "screen_always_on";
    private static final String BACK_IS_ESCAPE = "back_is_escape";
    private static final String IGNORE_BELL = "ignore_bell";

    private boolean mShowExtraKeys;
    private boolean mScreenAlwaysOn;
    private boolean mBackIsEscape;
    private boolean mIgnoreBellCharacter;


    TerminalPreferences(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        mShowExtraKeys = prefs.getBoolean(SHOW_EXTRA_KEYS_KEY, true);
        mScreenAlwaysOn = prefs.getBoolean(SCREEN_ALWAYS_ON_KEY, false);
        mBackIsEscape = prefs.getBoolean(BACK_IS_ESCAPE, false);
        mIgnoreBellCharacter = prefs.getBoolean(IGNORE_BELL, false);
    }

    public static void storeCurrentSession(Context context, TerminalSession session) {
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(TerminalPreferences.CURRENT_SESSION_KEY, session.mHandle).apply();
    }

    public static TerminalSession getCurrentSession(TerminalActivity context) {
        String sessionHandle = PreferenceManager.getDefaultSharedPreferences(context).getString(TerminalPreferences.CURRENT_SESSION_KEY, "");

        for (int i = 0, len = context.mTermService.getSessions().size(); i < len; i++) {
            TerminalSession session = context.mTermService.getSessions().get(i);
            if (session.mHandle.equals(sessionHandle)) return session;
        }

        return null;
    }

    public boolean isShowExtraKeys() {
        return mShowExtraKeys;
    }

    public boolean toggleShowExtraKeys(Context context) {
        mShowExtraKeys = !mShowExtraKeys;
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(SHOW_EXTRA_KEYS_KEY, mShowExtraKeys).apply();
        return mShowExtraKeys;
    }

    public boolean isScreenAlwaysOn() {
        return mScreenAlwaysOn;
    }

    public void setScreenAlwaysOn(Context context, boolean newValue) {
        mScreenAlwaysOn = newValue;
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(SCREEN_ALWAYS_ON_KEY, newValue).apply();
    }

    public boolean isBackEscape() {
        return mBackIsEscape;
    }

    public void setBackIsEscape(Context context, boolean newValue) {
        mBackIsEscape = newValue;
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(BACK_IS_ESCAPE, newValue).apply();
    }

    public boolean isBellIgnored() {
        return mIgnoreBellCharacter;
    }

    public void setIgnoreBellCharacter(Context context, boolean newValue) {
        mIgnoreBellCharacter = newValue;
        PreferenceManager.getDefaultSharedPreferences(context).edit().putBoolean(IGNORE_BELL, newValue).apply();
    }
}
