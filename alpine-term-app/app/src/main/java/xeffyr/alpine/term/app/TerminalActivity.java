package xeffyr.alpine.term.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import xeffyr.alpine.term.R;
import xeffyr.alpine.term.terminal.EmulatorDebug;
import xeffyr.alpine.term.terminal.TerminalColors;
import xeffyr.alpine.term.terminal.TerminalSession;
import xeffyr.alpine.term.terminal.TerminalSession.SessionChangedCallback;
import xeffyr.alpine.term.terminal.TextStyle;
import xeffyr.alpine.term.view.TerminalView;

/**
 * A terminal emulator activity.
 * <p/>
 * See
 * <ul>
 * <li>http://www.mongrel-phones.com.au/default/how_to_make_a_local_service_and_bind_to_it_in_android</li>
 * <li>https://code.google.com/p/android/issues/detail?id=6426</li>
 * </ul>
 * about memory leaks.
 */
public final class TerminalActivity extends Activity implements ServiceConnection {

    public static final String INTENT_ACTION_RELOAD = "terminal_reload";

    private static final int CONTEXTMENU_SHOW_HELP = 0;
    private static final int CONTEXTMENU_CONSOLE_STYLE = 1;
    private static final int CONTEXTMENU_SELECT_URL_ID = 2;
    private static final int CONTEXTMENU_SHARE_TRANSCRIPT_ID = 3;
    private static final int CONTEXTMENU_PASTE_ID = 4;
    private static final int CONTEXTMENU_RESET_TERMINAL_ID = 5;
    private static final int CONTEXTMENU_TOGGLE_BACK_IS_ESCAPE = 6;
    private static final int CONTEXTMENU_TOGGLE_IGNORE_BELL = 7;

    private static final int REQUESTCODE_PERMISSION_STORAGE = 1234;


    private final int MAX_FONTSIZE = 256;
    private int MIN_FONTSIZE;
    static int currentFontSize = -1;

    /**
     * The main view of the activity showing the terminal. Initialized in onCreate().
     */
    @SuppressWarnings("NullableProblems")
    @NonNull
    TerminalView mTerminalView;

    ExtraKeysView mExtraKeysView;

    TerminalPreferences mSettings;

    /**
     * The connection to the {@link TerminalService}. Requested in {@link #onCreate(Bundle)} with a call to
     * {@link #bindService(Intent, ServiceConnection, int)}, and obtained and stored in
     * {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    TerminalService mTermService;

    /**
     * Initialized in {@link #onServiceConnected(ComponentName, IBinder)}.
     */
    ArrayAdapter<TerminalSession> mListViewAdapter;

    /**
     * The last toast shown, used cancel current toast before showing new in {@link #showToast(String, boolean)}.
     */
    Toast mLastToast;

    /**
     * If between onResume() and onStop(). Note that only one session is in the foreground of the terminal view at the
     * time, so if the session causing a change is not in the foreground it should probably be treated as background.
     */
    boolean mIsVisible;


    private final BroadcastReceiver mBroadcastReceiever = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mIsVisible) {
                String whatToReload = intent.getStringExtra(INTENT_ACTION_RELOAD);

                if (whatToReload.equals("console_style")) {
                    checkForFontAndColors();
                }
            }
        }
    };

    @SuppressWarnings("StringBufferReplaceableByString")
    private static LinkedHashSet<CharSequence> extractUrls(String text) {

        StringBuilder regex_sb = new StringBuilder();

        regex_sb.append("(");                       // Begin first matching group.
        regex_sb.append("(?:");                     // Begin scheme group.
        regex_sb.append("dav|");                    // The DAV proto.
        regex_sb.append("dict|");                   // The DICT proto.
        regex_sb.append("dns|");                    // The DNS proto.
        regex_sb.append("file|");                   // File path.
        regex_sb.append("finger|");                 // The Finger proto.
        regex_sb.append("ftp(?:s?)|");              // The FTP proto.
        regex_sb.append("git|");                    // The Git proto.
        regex_sb.append("gopher|");                 // The Gopher proto.
        regex_sb.append("http(?:s?)|");             // The HTTP proto.
        regex_sb.append("imap(?:s?)|");             // The IMAP proto.
        regex_sb.append("irc(?:[6s]?)|");           // The IRC proto.
        regex_sb.append("ip[fn]s|");                // The IPFS proto.
        regex_sb.append("ldap(?:s?)|");             // The LDAP proto.
        regex_sb.append("pop3(?:s?)|");             // The POP3 proto.
        regex_sb.append("redis(?:s?)|");            // The Redis proto.
        regex_sb.append("rsync|");                  // The Rsync proto.
        regex_sb.append("rtsp(?:[su]?)|");          // The RTSP proto.
        regex_sb.append("sftp|");                   // The SFTP proto.
        regex_sb.append("smb(?:s?)|");              // The SAMBA proto.
        regex_sb.append("smtp(?:s?)|");             // The SMTP proto.
        regex_sb.append("svn(?:(?:\\+ssh)?)|");     // The Subversion proto.
        regex_sb.append("tcp|");                    // The TCP proto.
        regex_sb.append("telnet|");                 // The Telnet proto.
        regex_sb.append("tftp|");                   // The TFTP proto.
        regex_sb.append("udp|");                    // The UDP proto.
        regex_sb.append("vnc|");                    // The VNC proto.
        regex_sb.append("ws(?:s?)");                // The Websocket proto.
        regex_sb.append(")://");                    // End scheme group.
        regex_sb.append(")");                       // End first matching group.


        // Begin second matching group.
        regex_sb.append("(");

        // User name and/or password in format 'user:pass@'.
        regex_sb.append("(?:\\S+(?::\\S*)?@)?");

        // Begin host group.
        regex_sb.append("(?:");

        // IP address (from http://www.regular-expressions.info/examples.html).
        regex_sb.append("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)|");

        // Host name or domain.
        regex_sb.append("(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)(?:(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*(?:\\.(?:[a-z\\u00a1-\\uffff]{2,})))?|");

        // Just path. Used in case of 'file://' scheme.
        regex_sb.append("/(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)");

        // End host group.
        regex_sb.append(")");

        // Port number.
        regex_sb.append("(?::\\d{1,5})?");

        // Resource path with optional query string.
        regex_sb.append("(?:/[a-zA-Z0-9:@%\\-._~!$&()*+,;=?/]*)?");

        // End second matching group.
        regex_sb.append(")");

        final Pattern urlPattern = Pattern.compile(
            regex_sb.toString(),
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

        LinkedHashSet<CharSequence> urlSet = new LinkedHashSet<>();
        Matcher matcher = urlPattern.matcher(text);

        while (matcher.find()) {
            int matchStart = matcher.start(1);
            int matchEnd = matcher.end();
            String url = text.substring(matchStart, matchEnd);
            urlSet.add(url);
        }

        return urlSet;
    }

    private void checkForFontAndColors() {
        try {
            File fontFile = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/console_font.ttf");
            File colorsFile = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/console_colors.prop");

            final Properties props = new Properties();
            if (colorsFile.isFile()) {
                try (InputStream in = new FileInputStream(colorsFile)) {
                    props.load(in);
                }
            }

            TerminalColors.COLOR_SCHEME.updateWith(props);

            if (mTermService != null) {
                for (TerminalSession session : mTermService.getSessions()) {
                    if (session != null && session.getEmulator() != null) {
                        session.getEmulator().mColors.reset();
                    }
                }
            }

            updateBackgroundColor();

            final Typeface newTypeface = (fontFile.exists() && fontFile.length() > 0) ? Typeface.createFromFile(fontFile) : Typeface.MONOSPACE;
            mTerminalView.setTypeface(newTypeface);
        } catch (Exception e) {
            Log.e(EmulatorDebug.LOG_TAG, "An error occurred in checkForFontAndColors()", e);
        }
    }

    private void updateBackgroundColor() {
        TerminalSession session = getCurrentTermSession();
        if (session != null && session.getEmulator() != null) {
            getWindow().getDecorView().setBackgroundColor(session.getEmulator().mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]);
        }
    }

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        mSettings = new TerminalPreferences(this);

        setContentView(R.layout.drawer_layout);
        mTerminalView = findViewById(R.id.terminal_view);
        mTerminalView.setOnKeyListener(new InputDispatcher(this));

        float dipInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        int defaultFontSize = Math.round(7.5f * dipInPixels);

        // Make it divisible by 2 since that is the minimal adjustment step:
        if (defaultFontSize % 2 == 1) defaultFontSize--;

        if (TerminalActivity.currentFontSize == -1) {
            TerminalActivity.currentFontSize = defaultFontSize;
        }

        // This is a bit arbitrary and sub-optimal. We want to give a sensible default for minimum
        // font size to prevent invisible text due to zoom be mistake:
        MIN_FONTSIZE = (int) (4f * dipInPixels);

        TerminalActivity.currentFontSize = Math.max(MIN_FONTSIZE, Math.min(TerminalActivity.currentFontSize, MAX_FONTSIZE));
        mTerminalView.setTextSize(TerminalActivity.currentFontSize);

        mTerminalView.setKeepScreenOn(true);
        mTerminalView.requestFocus();

        final ViewPager viewPager = findViewById(R.id.viewpager);
        if (mSettings.isShowExtraKeys()) viewPager.setVisibility(View.VISIBLE);

        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 2;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, Object object) {
                return view == object;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup collection, int position) {
                LayoutInflater inflater = LayoutInflater.from(TerminalActivity.this);
                View layout;

                if (position == 0) {
                    layout = mExtraKeysView = (ExtraKeysView) inflater.inflate(R.layout.extra_keys_main, collection, false);
                } else {
                    layout = inflater.inflate(R.layout.extra_keys_right, collection, false);
                    final EditText editText = layout.findViewById(R.id.text_input);

                    editText.setOnEditorActionListener((v, actionId, event) -> {
                        TerminalSession session = getCurrentTermSession();

                        if (session != null) {
                            if (session.isRunning()) {
                                String textToSend = editText.getText().toString();

                                if (textToSend.length() == 0) {
                                    textToSend = "\r";
                                }

                                session.write(textToSend);
                            } else {
                                removeFinishedSession(session);
                            }

                            editText.setText("");
                        }

                        return true;
                    });
                }

                collection.addView(layout);

                return layout;
            }

            @Override
            public void destroyItem(@NonNull ViewGroup collection, int position, Object view) {
                collection.removeView((View) view);
            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    mTerminalView.requestFocus();
                } else {
                    final EditText editText = viewPager.findViewById(R.id.text_input);

                    if (editText != null) {
                        editText.requestFocus();
                    }
                }
            }
        });

        findViewById(R.id.toggle_keyboard_button).setOnClickListener(v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            getDrawer().closeDrawers();
        });
        findViewById(R.id.toggle_keyboard_button).setOnLongClickListener(v -> {
            toggleShowExtraKeys();
            return true;
        });

        registerForContextMenu(mTerminalView);

        Intent serviceIntent = new Intent(this, TerminalService.class);
        // Start the service and make it run regardless of who is bound to it:
        startService(serviceIntent);
        if (!bindService(serviceIntent, this, 0)) {
            throw new RuntimeException("bindService() failed");
        }

        LauncherPreferences.initializeDefaults(getApplicationContext());
        checkForFontAndColors();
    }

    @Override
    public void onStart() {
        super.onStart();
        mIsVisible = true;

        if (mTermService != null) {
            // The service has connected, but data may have changed since we were last in the foreground.
            switchToSession(getStoredCurrentSessionOrLast());
            mListViewAdapter.notifyDataSetChanged();
        }

        registerReceiver(mBroadcastReceiever, new IntentFilter(INTENT_ACTION_RELOAD));

        // The current terminal session may have changed while being away, force
        // a refresh of the displayed terminal:
        mTerminalView.onScreenUpdated();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mIsVisible = false;
        TerminalSession currentSession = getCurrentTermSession();
        if (currentSession != null) TerminalPreferences.storeCurrentSession(this, currentSession);
        getDrawer().closeDrawers();
        unregisterReceiver(mBroadcastReceiever);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mTermService != null) {
            // Do not leave service with references to activity.
            mTermService.mSessionChangeCallback = null;
            mTermService = null;
        }
        unbindService(this);
    }

    /**
     * Part of the {@link ServiceConnection} interface. The service is bound with
     * {@link #bindService(Intent, ServiceConnection, int)} in {@link #onCreate(Bundle)} which will cause a call to this
     * callback method.
     */
    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service) {
        mTermService = ((TerminalService.LocalBinder) service).service;

        mTermService.mSessionChangeCallback = new SessionChangedCallback() {
            @Override
            public void onTextChanged(TerminalSession changedSession) {
                if (!mIsVisible) return;
                if (getCurrentTermSession() == changedSession) mTerminalView.onScreenUpdated();
            }

            @Override
            public void onTitleChanged(TerminalSession updatedSession) {
                if (!mIsVisible) return;
                if (updatedSession != getCurrentTermSession()) {
                    // Only show toast for other sessions than the current one, since the user
                    // probably consciously caused the title change to change in the current session
                    // and don't want an annoying toast for that.
                    showToast(toToastTitle(updatedSession), false);
                }
                mListViewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onSessionFinished(final TerminalSession finishedSession) {
                // Needed for resetting font size on next application launch
                // otherwise it will be reset only after force-closing.
                TerminalActivity.currentFontSize = -1;

                if (mTermService.mWantsToStop) {
                    // The service wants to stop as soon as possible.
                    finish();
                    return;
                }

                removeFinishedSession(finishedSession);
                mListViewAdapter.notifyDataSetChanged();
            }

            @Override
            public void onClipboardText(TerminalSession session, String text) {
                if (!mIsVisible) return;
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (clipboard != null) clipboard.setPrimaryClip(new ClipData(null, new String[]{"text/plain"}, new ClipData.Item(text)));
            }

            @Override
            public void onBell(TerminalSession session) {
                if (!mIsVisible) return;

                if (!mSettings.isBellIgnored()) {
                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    if (vibrator != null) vibrator.vibrate(50);
                }
            }

            @Override
            public void onColorsChanged(TerminalSession changedSession) {
                if (getCurrentTermSession() == changedSession) updateBackgroundColor();
            }
        };

        ListView listView = findViewById(R.id.left_drawer_list);
        mListViewAdapter = new ArrayAdapter<TerminalSession>(getApplicationContext(), R.layout.line_in_drawer, mTermService.getSessions()) {
            final StyleSpan boldSpan = new StyleSpan(Typeface.BOLD);
            final StyleSpan italicSpan = new StyleSpan(Typeface.ITALIC);

            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                View row = convertView;
                if (row == null) {
                    LayoutInflater inflater = getLayoutInflater();
                    row = inflater.inflate(R.layout.line_in_drawer, parent, false);
                }

                TerminalSession sessionAtRow = getItem(position);
                boolean sessionRunning = sessionAtRow.isRunning();

                TextView firstLineView = row.findViewById(R.id.row_line);

                String name = sessionAtRow.mSessionName;
                String sessionTitle = sessionAtRow.getTitle();

                String numberPart = "[" + (position + 1) + "] ";
                String sessionNamePart = (TextUtils.isEmpty(name) ? "" : name);
                String sessionTitlePart = (TextUtils.isEmpty(sessionTitle) ? "" : ((sessionNamePart.isEmpty() ? "" : "\n") + sessionTitle));

                String text = numberPart + sessionNamePart + sessionTitlePart;
                SpannableString styledText = new SpannableString(text);
                styledText.setSpan(boldSpan, 0, numberPart.length() + sessionNamePart.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                styledText.setSpan(italicSpan, numberPart.length() + sessionNamePart.length(), text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                firstLineView.setText(styledText);

                if (sessionRunning) {
                    firstLineView.setPaintFlags(firstLineView.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    firstLineView.setPaintFlags(firstLineView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                }
                int color = sessionRunning || sessionAtRow.getExitStatus() == 0 ? Color.BLACK : Color.RED;
                firstLineView.setTextColor(color);
                return row;
            }
        };

        listView.setAdapter(mListViewAdapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            TerminalSession clickedSession = mListViewAdapter.getItem(position);
            switchToSession(clickedSession);
            getDrawer().closeDrawers();
        });

        listView.setOnItemLongClickListener((parent, view, position, id) -> {
            final TerminalSession selectedSession = mListViewAdapter.getItem(position);
            if (selectedSession != null) {
                renameSession(selectedSession);
            }
            return true;
        });

        if (mTermService.getSessions().isEmpty()) {
            if (mIsVisible) {
                Installer.setupIfNeeded(TerminalActivity.this, () -> {
                    if (mTermService == null) return; // Activity might have been destroyed.
                    try {
                        String[] menuEntries = {
                            getString(R.string.launcher_start_default),
                            getString(R.string.launcher_start_sandbox),
                            getString(R.string.launcher_open_settings),
                            getString(R.string.launcher_open_help),
                            getString(R.string.exit_label)
                        };

                        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUESTCODE_PERMISSION_STORAGE);
                        }

                        AlertDialog.Builder dialog = new AlertDialog.Builder(TerminalActivity.this);
                        dialog.setCancelable(false);
                        dialog.setIcon(R.mipmap.ic_launcher);
                        dialog.setTitle(R.string.launcher_title);
                        dialog.setItems(menuEntries, (dialog1, which) -> {
                            switch (which) {
                                case 0:
                                    // Default QEMU session.
                                    for (int i=0; i<4; i++) {
                                        addNewSession(String.format(Locale.US, "/dev/ttyS%d", i), TerminalService.SESSION_TYPE_SERIAL, i);
                                    }
                                    addNewSession("QEMU Monitor", TerminalService.SESSION_TYPE_QEMU, -1);
                                    switchToSession(mTermService.getSessions().get(0));
                                    break;
                                case 1:
                                    // Sandbox QEMU session.
                                    for (int i=0; i<4; i++) {
                                        addNewSession(String.format(Locale.US, "/dev/ttyS%d", i), TerminalService.SESSION_TYPE_SERIAL, i);
                                    }
                                    addNewSession("QEMU Monitor", TerminalService.SESSION_TYPE_QEMU_SANDBOX, -1);
                                    switchToSession(mTermService.getSessions().get(0));
                                    break;
                                case 2:
                                    // Settings activity.
                                    startActivity(new Intent(TerminalActivity.this, LauncherPreferences.class));
                                    TerminalActivity.this.recreate();
                                    break;
                                case 3:
                                    // Help activity.
                                    startActivity(new Intent(TerminalActivity.this, HelpActivity.class));
                                    TerminalActivity.this.recreate();
                                    break;
                                case 4:
                                    // Exit application.
                                    mTermService.stopSelf();
                                    TerminalActivity.this.finish();
                                    break;
                                default:
                                    break;
                            }
                        });
                        dialog.show();
                    } catch (WindowManager.BadTokenException e) {
                        // Activity finished - ignore.
                    }
                });
            } else {
                // The service connected while not in foreground - just bail out.
                finish();
            }
        } else {
            switchToSession(getStoredCurrentSessionOrLast());
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // Respect being stopped from the TerminalService notification action.
        finish();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        TerminalSession currentSession = getCurrentTermSession();

        if (currentSession == null) {
            return;
        }

        menu.add(Menu.NONE, CONTEXTMENU_SHOW_HELP, Menu.NONE, R.string.menu_show_help);
        menu.add(Menu.NONE, CONTEXTMENU_CONSOLE_STYLE, Menu.NONE, R.string.menu_console_style);
        menu.add(Menu.NONE, CONTEXTMENU_SELECT_URL_ID, Menu.NONE, R.string.menu_select_url);
        menu.add(Menu.NONE, CONTEXTMENU_SHARE_TRANSCRIPT_ID, Menu.NONE, R.string.menu_share_transcript);
        menu.add(Menu.NONE, CONTEXTMENU_RESET_TERMINAL_ID, Menu.NONE, R.string.menu_reset_terminal);
        menu.add(Menu.NONE, CONTEXTMENU_TOGGLE_BACK_IS_ESCAPE, Menu.NONE, R.string.menu_toggle_back_is_escape).setCheckable(true).setChecked(mSettings.isBackEscape());
        menu.add(Menu.NONE, CONTEXTMENU_TOGGLE_IGNORE_BELL, Menu.NONE, R.string.menu_toggle_ignore_bell).setCheckable(true).setChecked(mSettings.isBellIgnored());
    }

    /**
     * Hook system menu to show context menu instead.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mTerminalView.showContextMenu();
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        TerminalSession session = getCurrentTermSession();

        switch (item.getItemId()) {
            case CONTEXTMENU_SHOW_HELP:
                startActivity(new Intent(this, HelpActivity.class));
                return true;
            case CONTEXTMENU_CONSOLE_STYLE:
                startActivity(new Intent(this, TerminalStyleActivity.class));
                return true;
            case CONTEXTMENU_SELECT_URL_ID:
                showUrlSelection();
                return true;
            case CONTEXTMENU_SHARE_TRANSCRIPT_ID:
                if (session != null) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, session.getEmulator().getScreen().getTranscriptText().trim());
                    intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_transcript_file_name));
                    startActivity(Intent.createChooser(intent, getString(R.string.share_transcript_chooser_title)));
                }
                return true;
            case CONTEXTMENU_PASTE_ID:
                doPaste();
                return true;
            case CONTEXTMENU_RESET_TERMINAL_ID: {
                if (session != null) {
                    session.reset();
                    showToast(getResources().getString(R.string.reset_toast_notification), true);
                }
                return true;
            }
            case CONTEXTMENU_TOGGLE_BACK_IS_ESCAPE: {
                if (mSettings.isBackEscape()) {
                    mSettings.setBackIsEscape(this, false);
                } else {
                    mSettings.setBackIsEscape(this, true);
                }
                return true;
            }
            case CONTEXTMENU_TOGGLE_IGNORE_BELL: {
                if (mSettings.isBellIgnored()) {
                    mSettings.setIgnoreBellCharacter(this, false);
                } else {
                    mSettings.setIgnoreBellCharacter(this, true);
                }
                return true;
            }

            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        if (getDrawer().isDrawerOpen(Gravity.LEFT)) {
            getDrawer().closeDrawers();
        } else {
            finish();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == REQUESTCODE_PERMISSION_STORAGE && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, PendingIntent.getActivity(this.getBaseContext(), 0, new Intent(getIntent()), getIntent().getFlags()));
                android.os.Process.killProcess(android.os.Process.myPid());
            } else {
                new AlertDialog.Builder(this).setTitle(R.string.storage_permission_denied_title).setMessage(R.string.storage_permission_denied_body).setCancelable(false).setPositiveButton(R.string.exit_label, (dialogInterface, i) -> {
                    mTermService.stopSelf();
                    TerminalActivity.this.finish();
                }).show();
            }
        }
    }

    public DrawerLayout getDrawer() {
        return (DrawerLayout) findViewById(R.id.drawer_layout);
    }

    public void toggleShowExtraKeys() {
        final ViewPager viewPager = findViewById(R.id.viewpager);
        final boolean showNow = mSettings.toggleShowExtraKeys(TerminalActivity.this);

        viewPager.setVisibility(showNow ? View.VISIBLE : View.GONE);

        if (showNow && viewPager.getCurrentItem() == 1) {
            // Focus the text input view if just revealed.
            findViewById(R.id.text_input).requestFocus();
        }
    }

    public void changeFontSize(boolean increase) {
        TerminalActivity.currentFontSize += (increase ? 1 : -1) * 2;
        TerminalActivity.currentFontSize = Math.max(MIN_FONTSIZE, Math.min(TerminalActivity.currentFontSize, MAX_FONTSIZE));
        mTerminalView.setTextSize(TerminalActivity.currentFontSize);

        TerminalSession currentSession = getCurrentTermSession();

        if (currentSession != null) {
            currentSession.reset();
        }
    }

    public void doPaste() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard != null) {
            ClipData clipData = clipboard.getPrimaryClip();

            if (clipData == null) {
                return;
            }

            CharSequence paste = clipData.getItemAt(0).coerceToText(this);
            if (!TextUtils.isEmpty(paste))
                getCurrentTermSession().getEmulator().paste(paste.toString());
        }
    }

    public void addNewSession(String sessionName, int sessionType, int sessionNumber) {
        if (mTermService.getSessions().size() == 0 && !mTermService.isWakelockEnabled()) {
            File envTmpDir = new File(Installer.getEnvironmentPrefix(getApplicationContext()) + "/tmp");
            if (envTmpDir.exists()) {
                try {
                    Installer.deleteFolder(envTmpDir);
                } catch (Exception e) {
                    // Ignore.
                }

                try {
                    envTmpDir.mkdirs();
                } catch (Exception e) {
                    Log.e(EmulatorDebug.LOG_TAG, "error while creating directory for temporary files", e);
                }
            }
        }

        TerminalSession newSession = mTermService.createTermSession(sessionType, sessionNumber);

        if (sessionName != null) {
            newSession.mSessionName = sessionName;
        }

        switchToSession(newSession);
    }

    /**
     * The current session as stored or the last one if that does not exist.
     */
    public TerminalSession getStoredCurrentSessionOrLast() {
        TerminalSession stored = TerminalPreferences.getCurrentSession(this);

        if (stored != null) {
            return stored;
        }

        List<TerminalSession> sessions = mTermService.getSessions();

        return sessions.isEmpty() ? null : sessions.get(sessions.size() - 1);
    }

    @Nullable
    public TerminalSession getCurrentTermSession() {
        return mTerminalView.getCurrentSession();
    }

    /**
     * Try switching to session and note about it, but do nothing if already displaying the session.
     */
    public void switchToSession(TerminalSession session) {
        if (mTerminalView.attachSession(session)) {
            if (mIsVisible) {
                final int indexOfSession = mTermService.getSessions().indexOf(session);
                mListViewAdapter.notifyDataSetChanged();

                final ListView lv = findViewById(R.id.left_drawer_list);
                lv.setItemChecked(indexOfSession, true);
                lv.smoothScrollToPosition(indexOfSession);

                showToast(toToastTitle(session), false);
            }

            updateBackgroundColor();
        }
    }

    public void switchToSession(boolean forward) {
        TerminalSession currentSession = getCurrentTermSession();
        int index = mTermService.getSessions().indexOf(currentSession);
        if (forward) {
            if (++index >= mTermService.getSessions().size()) index = 0;
        } else {
            if (--index < 0) index = mTermService.getSessions().size() - 1;
        }
        switchToSession(mTermService.getSessions().get(index));
    }

    @SuppressLint("InflateParams")
    public void renameSession(final TerminalSession sessionToRename) {
        DialogUtils.textInput(this, R.string.session_rename_title, sessionToRename.mSessionName, R.string.session_rename_positive_button, text -> {
            sessionToRename.mSessionName = text;
            mListViewAdapter.notifyDataSetChanged();
        }, -1, null, R.string.cancel_label, null);
    }

    public void removeFinishedSession(TerminalSession finishedSession) {
        // Return pressed with finished session - remove it.
        TerminalService service = mTermService;

        int index = service.removeTermSession(finishedSession);
        mListViewAdapter.notifyDataSetChanged();

        if (!mTermService.getSessions().isEmpty()) {
            if (index >= service.getSessions().size()) {
                index = service.getSessions().size() - 1;
            }

            switchToSession(service.getSessions().get(index));
        }
    }

    public void showUrlSelection() {
        String text = getCurrentTermSession().getEmulator().getScreen().getTranscriptText();
        LinkedHashSet<CharSequence> urlSet = extractUrls(text);
        if (urlSet.isEmpty()) {
            showToast(getResources().getString(R.string.select_url_toast_no_found), true);
            return;
        }

        final CharSequence[] urls = urlSet.toArray(new CharSequence[urlSet.size()]);
        Collections.reverse(Arrays.asList(urls)); // Latest first.

        // Click to copy url to clipboard:
        final AlertDialog dialog = new AlertDialog.Builder(TerminalActivity.this).setItems(urls, (di, which) -> {
            String url = (String) urls[which];
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                clipboard.setPrimaryClip(new ClipData(null, new String[]{"text/plain"}, new ClipData.Item(url)));
                showToast(getResources().getString(R.string.select_url_toast_copied_to_clipboard), true);
            }
        }).setTitle(R.string.select_url_dialog_title).create();

        // Long press to open URL:
        dialog.setOnShowListener(di -> {
            ListView lv = dialog.getListView(); // this is a ListView with your "buds" in it
            lv.setOnItemLongClickListener((parent, view, position, id) -> {
                dialog.dismiss();
                String url = (String) urls[position];

                // Disable handling of 'file://' urls since this may
                // produce android.os.FileUriExposedException.
                if (!url.startsWith("file://")) {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    try {
                        startActivity(i, null);
                    } catch (ActivityNotFoundException e) {
                        // If no applications match, Android displays a system message.
                        startActivity(Intent.createChooser(i, null));
                    }
                } else {
                    showToast(getResources().getString(R.string.select_url_toast_cannot_open), false);
                }

                return true;
            });
        });

        dialog.show();
    }

    private String toToastTitle(TerminalSession session) {
        final int indexOfSession = mTermService.getSessions().indexOf(session);

        StringBuilder toastTitle = new StringBuilder("[" + (indexOfSession + 1) + "]");

        if (!TextUtils.isEmpty(session.mSessionName)) {
            toastTitle.append(" ").append(session.mSessionName);
        }

        String title = session.getTitle();

        if (!TextUtils.isEmpty(title)) {
            // Space to "[${NR}] or newline after session name:
            toastTitle.append(session.mSessionName == null ? " " : "\n");
            toastTitle.append(title);
        }

        return toastTitle.toString();
    }

    /**
     * Show a toast and dismiss the last one if still visible.
     */
    private void showToast(String text, boolean longDuration) {
        if (mLastToast != null) mLastToast.cancel();
        mLastToast = Toast.makeText(TerminalActivity.this, text, longDuration ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        mLastToast.setGravity(Gravity.TOP, 0, 0);
        mLastToast.show();
    }
}
