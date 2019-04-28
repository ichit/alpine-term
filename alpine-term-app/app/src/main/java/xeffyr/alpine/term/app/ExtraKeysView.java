package xeffyr.alpine.term.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.PopupWindow;
import android.widget.ToggleButton;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import xeffyr.alpine.term.R;
import xeffyr.alpine.term.terminal.TerminalSession;
import xeffyr.alpine.term.view.TerminalView;

/**
 * A view showing extra keys (such as Escape, Ctrl, Alt) not normally available on an Android soft
 * keyboard.
 */
public final class ExtraKeysView extends GridLayout {

    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int BUTTON_COLOR = 0x00000000;
    private static final int BUTTON_PRESSED_COLOR = 0x7FFFFFFF;

    private ToggleButton controlButton;
    private ToggleButton altButton;
    private ScheduledExecutorService scheduledExecutor;
    private PopupWindow popupWindow;
    private int longPressCount;


    public ExtraKeysView(Context context, AttributeSet attrs) {
        super(context, attrs);
        reload();
    }

    private static void sendKey(View view, String keyName) {
        int keyCode = 0;
        String chars = null;
        switch (keyName) {
            case "ESC":
                keyCode = KeyEvent.KEYCODE_ESCAPE;
                break;
            case "TAB":
                keyCode = KeyEvent.KEYCODE_TAB;
                break;
            case "HOME":
                keyCode = KeyEvent.KEYCODE_MOVE_HOME;
                break;
            case "END":
                keyCode = KeyEvent.KEYCODE_MOVE_END;
                break;
            case "PGUP":
                keyCode = KeyEvent.KEYCODE_PAGE_UP;
                break;
            case "PGDN":
                keyCode = KeyEvent.KEYCODE_PAGE_DOWN;
                break;
            case "↑":
                keyCode = KeyEvent.KEYCODE_DPAD_UP;
                break;
            case "←":
                keyCode = KeyEvent.KEYCODE_DPAD_LEFT;
                break;
            case "→":
                keyCode = KeyEvent.KEYCODE_DPAD_RIGHT;
                break;
            case "↓":
                keyCode = KeyEvent.KEYCODE_DPAD_DOWN;
                break;
            case "―":
                chars = "-";
                break;
            default:
                chars = keyName;
        }

        TerminalView terminalView = view.findViewById(R.id.terminal_view);
        if (keyCode > 0) {
            terminalView.onKeyDown(keyCode, new KeyEvent(KeyEvent.ACTION_UP, keyCode));
        } else {
            TerminalSession session = terminalView.getCurrentSession();
            if (session != null) session.write(chars);
        }
    }

    public boolean readControlButton() {
        if (controlButton.isPressed()) {
            return true;
        }

        boolean result = controlButton.isChecked();

        if (result) {
            controlButton.setChecked(false);
            controlButton.setTextColor(TEXT_COLOR);
        }

        return result;
    }

    public boolean readAltButton() {
        if (altButton.isPressed()) {
            return true;
        }

        boolean result = altButton.isChecked();

        if (result) {
            altButton.setChecked(false);
            altButton.setTextColor(TEXT_COLOR);
        }

        return result;
    }

    private void popup(View view, String text) {
        int width = view.getMeasuredWidth();
        int height = view.getMeasuredHeight();

        Button button = new Button(getContext(), null, android.R.attr.buttonBarButtonStyle);
        button.setText(text);
        button.setTextColor(TEXT_COLOR);
        button.setPadding(0, 0, 0, 0);
        button.setMinHeight(0);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinimumHeight(0);
        button.setWidth(width);
        button.setHeight(height);
        button.setBackgroundColor(BUTTON_PRESSED_COLOR);

        popupWindow = new PopupWindow(this);
        popupWindow.setWidth(LayoutParams.WRAP_CONTENT);
        popupWindow.setHeight(LayoutParams.WRAP_CONTENT);
        popupWindow.setContentView(button);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setFocusable(false);
        popupWindow.showAsDropDown(view, 0, -2 * height);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void reload() {
        altButton = controlButton = null;
        removeAllViews();

        String[][] buttons = {
            {"―",   "/",   "|",    ">",   "HOME", "↑", "END", "PGUP"},
            {"ESC", "TAB", "CTRL", "ALT", "←",    "↓", "→",   "PGDN"}
        };

        final int rows = buttons.length;
        final int[] cols = {buttons[0].length, buttons[1].length};

        setRowCount(rows);
        setColumnCount(cols[0]);

        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols[row]; col++) {
                final String buttonText = buttons[row][col];
                Button button;

                switch (buttonText) {
                    case "CTRL":
                        button = controlButton = new ToggleButton(getContext(), null,
                            android.R.attr.buttonBarButtonStyle);
                        button.setClickable(true);
                        break;
                    case "ALT":
                        button = altButton = new ToggleButton(getContext(), null,
                            android.R.attr.buttonBarButtonStyle);
                        button.setClickable(true);
                        break;
                    default:
                        button = new Button(getContext(), null, android.R.attr.buttonBarButtonStyle);
                        break;
                }

                button.setText(buttonText);
                button.setTextColor(TEXT_COLOR);
                button.setPadding(0, 0, 0, 0);

                if ("↑←↓→".contains(buttonText)) {
                    button.setTypeface(button.getTypeface(), Typeface.BOLD);
                }

                final Button finalButton = button;
                button.setOnClickListener(v -> {
                    finalButton.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    View root = getRootView();

                    switch (buttonText) {
                        case "CTRL":
                        case "ALT":
                            ToggleButton self = (ToggleButton) finalButton;
                            self.setChecked(self.isChecked());
                            self.setTextColor(self.isChecked() ? 0xFF80DEEA : TEXT_COLOR);
                            break;
                        default:
                            sendKey(root, buttonText);
                            break;
                    }
                });

                button.setOnTouchListener((v, event) -> {
                    final View root = getRootView();
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            longPressCount = 0;
                            v.setBackgroundColor(BUTTON_PRESSED_COLOR);
                            if ("↑↓←→".contains(buttonText)) {
                                scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
                                scheduledExecutor.scheduleWithFixedDelay(() -> {
                                    longPressCount++;
                                    sendKey(root, buttonText);
                                }, 400, 80, TimeUnit.MILLISECONDS);
                            }
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            if ("―/|>".contains(buttonText)) {
                                if (popupWindow == null && event.getY() < 0) {
                                    v.setBackgroundColor(BUTTON_COLOR);

                                    switch (buttonText) {
                                        case "―":
                                            popup(v, "_");
                                            break;
                                        case "/":
                                            popup(v, "\\");
                                            break;
                                        case "|":
                                            popup(v, "&");
                                            break;
                                        case ">":
                                            popup(v, "<");
                                            break;
                                    }
                                }
                                if (popupWindow != null && event.getY() > 0) {
                                    v.setBackgroundColor(BUTTON_PRESSED_COLOR);
                                    popupWindow.dismiss();
                                    popupWindow = null;
                                }
                            }
                            return true;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.setBackgroundColor(BUTTON_COLOR);
                            if (scheduledExecutor != null) {
                                scheduledExecutor.shutdownNow();
                                scheduledExecutor = null;
                            }
                            if (longPressCount == 0) {
                                if (popupWindow != null && "―/|>".contains(buttonText)) {
                                    popupWindow.setContentView(null);
                                    popupWindow.dismiss();
                                    popupWindow = null;

                                    switch (buttonText) {
                                        case "―":
                                            sendKey(root, "_");
                                            break;
                                        case "/":
                                            sendKey(root, "\\");
                                            break;
                                        case "|":
                                            sendKey(root, "&");
                                            break;
                                        case ">":
                                            sendKey(root, "<");
                                            break;
                                    }
                                } else {
                                    v.performClick();
                                }
                            }
                            return true;
                        default:
                            return true;
                    }

                });

                LayoutParams param = new GridLayout.LayoutParams();
                param.width = 0;
                param.height = 0;

                param.setMargins(0, 0, 0, 0);
                param.columnSpec = GridLayout.spec(col, GridLayout.FILL, 1.f);
                param.rowSpec = GridLayout.spec(row, GridLayout.FILL, 1.f);
                button.setLayoutParams(param);

                addView(button);
            }
        }
    }
}
