package xeffyr.alpine.term.app;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import xeffyr.alpine.term.R;
import xeffyr.alpine.term.terminal.EmulatorDebug;
import xeffyr.alpine.term.terminal.TerminalSession;
import xeffyr.alpine.term.terminal.TerminalSession.SessionChangedCallback;

/**
 * A service holding a list of terminal sessions, {@link #mTerminalSessions}, showing a foreground notification while
 * running so that it is not terminated. The user interacts with the session through {@link TerminalActivity}, but this
 * service may outlive the activity when the user or the system disposes of the activity. In that case the user may
 * restart {@link TerminalActivity} later to yet again access the sessions.
 * <p/>
 * In order to keep both terminal sessions and spawned processes (who may outlive the terminal sessions) alive as long
 * as wanted by the user this service is a foreground service, {@link Service#startForeground(int, Notification)}.
 * <p/>
 * Optionally may hold a wake and a wifi lock, in which case that is shown in the notification - see
 * {@link #buildNotification()}.
 */
public final class TerminalService extends Service implements SessionChangedCallback {

    public static final int SESSION_TYPE_QEMU = 0;
    public static final int SESSION_TYPE_QEMU_SANDBOX = 1;
    public static final int SESSION_TYPE_SERIAL = 2;

    private static final String NOTIFICATION_CHANNEL_ID = "alpine_term_notification_channel";
    private static final int NOTIFICATION_ID = 1338;

    private static final String ACTION_STOP_SERVICE = "xeffyr.alpine.term.service_stop";
    private static final String ACTION_LOCK_WAKE = "xeffyr.alpine.term.service_wake_lock";
    private static final String ACTION_UNLOCK_WAKE = "xeffyr.alpine.term.service_wake_unlock";

    /**
     * The terminal sessions which this service manages.
     * <p/>
     * Note that this list is observed by {@link TerminalActivity#mListViewAdapter}, so any changes must be made on the UI
     * thread and followed by a call to {@link ArrayAdapter#notifyDataSetChanged()} }.
     */
    final List<TerminalSession> mTerminalSessions = new ArrayList<>();

    private final IBinder mBinder = new LocalBinder();

    /**
     * Note that the service may often outlive the activity, so need to clear this reference.
     */
    SessionChangedCallback mSessionChangeCallback;

    /**
     * If the user has executed the {@link #ACTION_STOP_SERVICE} intent.
     */
    boolean mWantsToStop = false;

    /**
     * The wake lock and wifi lock are always acquired and released together.
     */
    private PowerManager.WakeLock mWakeLock;
    private WifiManager.WifiLock mWifiLock;

    @Override
    public void onCreate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelName = getString(R.string.application_name);
            String channelDescription = "Notifications from " + getString(R.string.application_name);
            int importance = NotificationManager.IMPORTANCE_LOW;

            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance);
            channel.setDescription(channelDescription);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @Override
    public void onDestroy() {
        if (mWakeLock != null) mWakeLock.release();
        if (mWifiLock != null) mWifiLock.release();

        stopForeground(true);

        for (int i = 0; i < mTerminalSessions.size(); i++) {
            mTerminalSessions.get(i).finishIfRunning();
        }
    }

    @SuppressLint("Wakelock")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (ACTION_STOP_SERVICE.equals(action)) {
            mWantsToStop = true;
            for (int i = 0; i < mTerminalSessions.size(); i++) {
                mTerminalSessions.get(i).finishIfRunning();
            }
            stopSelf();
        } else if (ACTION_LOCK_WAKE.equals(action)) {
            if (mWakeLock == null) {
                PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
                mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, EmulatorDebug.LOG_TAG);
                mWakeLock.acquire();

                // http://tools.android.com/tech-docs/lint-in-studio-2-3#TOC-WifiManager-Leak
                WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                mWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, EmulatorDebug.LOG_TAG);
                mWifiLock.acquire();

                updateNotification();
            }
        } else if (ACTION_UNLOCK_WAKE.equals(action)) {
            if (mWakeLock != null) {
                mWakeLock.release();
                mWakeLock = null;

                mWifiLock.release();
                mWifiLock = null;

                updateNotification();
            }
        } else if (action != null) {
            Log.e(EmulatorDebug.LOG_TAG, "Received an unknown action for TerminalService: '" + action + "'");
        }

        // If this service really do get killed, there is no point restarting it automatically - let the user do on next
        // start of {@link Term):
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onTitleChanged(TerminalSession changedSession) {
        if (mSessionChangeCallback != null) {
            mSessionChangeCallback.onTitleChanged(changedSession);
        }
    }

    @Override
    public void onSessionFinished(final TerminalSession finishedSession) {
        if (mSessionChangeCallback != null) {
            mSessionChangeCallback.onSessionFinished(finishedSession);
        }
    }

    @Override
    public void onTextChanged(TerminalSession changedSession) {
        if (mSessionChangeCallback != null) {
            mSessionChangeCallback.onTextChanged(changedSession);
        }
    }

    @Override
    public void onClipboardText(TerminalSession session, String text) {
        if (mSessionChangeCallback != null) {
            mSessionChangeCallback.onClipboardText(session, text);
        }
    }

    @Override
    public void onBell(TerminalSession session) {
        if (mSessionChangeCallback != null) {
            mSessionChangeCallback.onBell(session);
        }
    }

    @Override
    public void onColorsChanged(TerminalSession session) {
        if (mSessionChangeCallback != null) {
            mSessionChangeCallback.onColorsChanged(session);
        }
    }

    public boolean isWakelockEnabled() {
        if (mWakeLock == null) {
            return false;
        } else {
            return mWakeLock.isHeld();
        }
    }

    public List<TerminalSession> getSessions() {
        return mTerminalSessions;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public TerminalSession createTermSession(int sessionType, int sessionNumber) {
        ArrayList<String> environment = new ArrayList<>();
        Context appContext = getApplicationContext();

        LauncherPreferences.initializeDefaults(appContext);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(appContext);

        // ${ANDROID_ROOT} and ${ANDROID_DATA} variables are not needed
        // but set them anyway to prevent Bionic warnings about timezone.
        environment.add("ANDROID_ROOT=" + System.getenv("ANDROID_ROOT"));
        environment.add("ANDROID_DATA=" + System.getenv("ANDROID_DATA"));

        // Entrypoint script is configured via environment variables.
        // Default values should be same as in LauncherPreferences.initializeDefaults().
        environment.add("CONFIG_QEMU_RAM=" + prefs.getString(getResources().getString(R.string.qemu_ram_key), "256"));
        environment.add("CONFIG_QEMU_HDD1_PATH=" + prefs.getString(getResources().getString(R.string.qemu_hdd1_path_key),
            getExternalFilesDir(null) + "/os_snapshot.qcow2"));
        environment.add("CONFIG_QEMU_HDD2_PATH=" + prefs.getString(getResources().getString(R.string.qemu_hdd2_path_key), ""));
        environment.add("CONFIG_QEMU_CDROM_PATH=" + prefs.getString(getResources().getString(R.string.qemu_cdrom_path_key), ""));
        environment.add("CONFIG_QEMU_DNS=" + prefs.getString(getResources().getString(R.string.qemu_upstream_dns_key), "1.1.1.1"));
        environment.add("CONFIG_QEMU_EXPOSED_PORTS=" + prefs.getString(getResources().getString(R.string.qemu_exposed_ports_key), ""));

        String prefix = Installer.getEnvironmentPrefix(appContext);
        String home = appContext.getFilesDir().getAbsolutePath();
        String execPath = appContext.getApplicationInfo().nativeLibraryDir;

        environment.add("PREFIX=" + prefix);
        environment.add("LANG=en_US.UTF-8");
        environment.add("TERM=xterm-256color");
        environment.add("HOME=" + home);
        environment.add("PWD=" + home);
        environment.add("PATH=" + execPath);
        environment.add("TERMINFO=" + prefix + "/share/terminfo");
        environment.add("TMPDIR=" + prefix + "/tmp");
        environment.add("EXTERNAL_STORAGE=" + Environment.getExternalStorageDirectory().getAbsolutePath());

        if (sessionType == SESSION_TYPE_SERIAL) {
            environment.add("SERIAL_CONSOLE_NUMBER=" + sessionNumber);
        }

        String processArgs[] = {execPath + "/libbash.so", prefix + "/entrypoint.bash", String.valueOf(sessionType)};
        TerminalSession session = new TerminalSession(execPath + "/libbash.so", processArgs, environment.toArray(new String[0]), home, this);
        mTerminalSessions.add(session);
        updateNotification();

        return session;
    }

    public int removeTermSession(TerminalSession sessionToRemove) {
        int indexOfRemoved = mTerminalSessions.indexOf(sessionToRemove);
        mTerminalSessions.remove(indexOfRemoved);

        if (mTerminalSessions.isEmpty()) {
            // Finish if there are no sessions left.
            stopSelf();
        } else {
            updateNotification();
        }

        return indexOfRemoved;
    }

    private Notification buildNotification() {
        Intent notifyIntent = new Intent(this, TerminalActivity.class);
        // PendingIntent#getActivity(): "Note that the activity will be started outside of the context of an existing
        // activity, so you must use the Intent.FLAG_ACTIVITY_NEW_TASK launch flag in the Intent":
        notifyIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notifyIntent, 0);


        StringBuilder contentText = new StringBuilder();

        if (!mTerminalSessions.isEmpty()) {
            contentText.append("Virtual machine is running.");
        } else {
            contentText.append("Virtual machine is not initialized.");
        }

        final boolean wakeLockHeld = mWakeLock != null;

        if (wakeLockHeld) {
            contentText.append(" Wake lock held.");
        }

        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle(getText(R.string.application_name));
        builder.setContentText(contentText.toString());
        builder.setSmallIcon(R.drawable.ic_service_notification);
        builder.setContentIntent(pendingIntent);
        builder.setOngoing(true);

        // If holding a wake or wifi lock consider the notification of high priority since it's using power,
        // otherwise use a low priority
        builder.setPriority((wakeLockHeld) ? Notification.PRIORITY_HIGH : Notification.PRIORITY_LOW);

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Background color for small notification icon:
        builder.setColor(0xFF000000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }

        Resources res = getResources();
        Intent exitIntent = new Intent(this, TerminalService.class).setAction(ACTION_STOP_SERVICE);
        builder.addAction(android.R.drawable.ic_delete, res.getString(R.string.exit_label), PendingIntent.getService(this, 0, exitIntent, 0));

        String newWakeAction = wakeLockHeld ? ACTION_UNLOCK_WAKE : ACTION_LOCK_WAKE;
        Intent toggleWakeLockIntent = new Intent(this, TerminalService.class).setAction(newWakeAction);
        String actionTitle = res.getString(wakeLockHeld ?
            R.string.notification_action_wake_unlock :
            R.string.notification_action_wake_lock);
        int actionIcon = wakeLockHeld ? android.R.drawable.ic_lock_idle_lock : android.R.drawable.ic_lock_lock;
        builder.addAction(actionIcon, actionTitle, PendingIntent.getService(this, 0, toggleWakeLockIntent, 0));

        return builder.build();
    }

    /**
     * Update the shown foreground service notification after making any changes that affect it.
     */
    private void updateNotification() {
        if (mWakeLock == null && mTerminalSessions.isEmpty()) {
            // Exit if we are updating after the user disabled all locks with no sessions or tasks running.
            stopSelf();
        } else {
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, buildNotification());
        }
    }

    /**
     * This service is only bound from inside the same process and never uses IPC.
     */
    class LocalBinder extends Binder {
        public final TerminalService service = TerminalService.this;
    }
}
