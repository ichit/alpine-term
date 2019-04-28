package xeffyr.alpine.term.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import xeffyr.alpine.term.R;
import xeffyr.alpine.term.terminal.EmulatorDebug;

public class TerminalStyleActivity extends Activity {

    private static final String DEFAULT_FILENAME = "Default";

    private static String capitalize(String str) {
        boolean lastWhitespace = true;
        char[] chars = str.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (Character.isLetter(chars[i])) {
                if (lastWhitespace) {
                    chars[i] = Character.toUpperCase(chars[i]);
                }

                lastWhitespace = false;
            } else {
                lastWhitespace = Character.isWhitespace(chars[i]);
            }
        }

        return new String(chars);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Avoid dim behind:
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        setContentView(R.layout.styling_layout);

        final Button colorSpinner = findViewById(R.id.color_spinner);
        final Button fontSpinner = findViewById(R.id.font_spinner);

        final ArrayAdapter<Selectable> colorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);

        colorSpinner.setOnClickListener(v -> {
            final AlertDialog dialog = new AlertDialog.Builder(TerminalStyleActivity.this).setAdapter(colorAdapter, (dialog1, which) -> copyFile(colorAdapter.getItem(which), true)).create();
            dialog.show();
        });

        final ArrayAdapter<Selectable> fontAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item);
        fontSpinner.setOnClickListener(v -> {
            final AlertDialog dialog = new AlertDialog.Builder(TerminalStyleActivity.this).setAdapter(fontAdapter, (dialog12, which) -> copyFile(fontAdapter.getItem(which), false)).create();
            dialog.show();
        });

        List<Selectable> colorList = new ArrayList<>();
        List<Selectable> fontList = new ArrayList<>();

        for (String assetType : new String[]{"color_schemes", "fonts"}) {
            boolean isColors = assetType.equals("color_schemes");

            String assetsFileExtension = isColors ? ".properties" : ".ttf";
            List<Selectable> currentList = isColors ? colorList : fontList;

            currentList.add(new Selectable(isColors ? DEFAULT_FILENAME : DEFAULT_FILENAME));

            try {
                for (String f : getAssets().list(assetType)) {
                    if (f.endsWith(assetsFileExtension)) currentList.add(new Selectable(f));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        Pair<List<Selectable>, List<Selectable>> result = Pair.create(colorList, fontList);

        colorAdapter.addAll(result.first);
        fontAdapter.addAll(result.second);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void copyFile(Selectable mCurrentSelectable, boolean colors) {
        final String outputFileName = colors ? "console_colors.prop" : "console_font.ttf";

        try {
            final String assetsFolder = colors ? "color_schemes" : "fonts";
            File destinationFile = new File(getApplicationContext().getFilesDir().getAbsolutePath() + "/" + outputFileName);

            // Fix for if the user has messed up with chmod:
            destinationFile.setWritable(true);
            destinationFile.getParentFile().setWritable(true);
            destinationFile.getParentFile().setExecutable(true);

            boolean defaultChoice = mCurrentSelectable.fileName.equals(DEFAULT_FILENAME);

            // Write to existing file to keep symlink if this is used.
            try (FileOutputStream out = new FileOutputStream(destinationFile)) {
                if (defaultChoice) {
                    if (colors) {
                        byte[] comment = "# Using default color theme.".getBytes(StandardCharsets.UTF_8);
                        out.write(comment);
                    }
                } else {
                    try (InputStream in = getAssets().open(assetsFolder + "/" + mCurrentSelectable.fileName)) {
                        byte[] buffer = new byte[4096];
                        int len;

                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                }
            }

            Intent executeIntent = new Intent(TerminalActivity.INTENT_ACTION_RELOAD);
            executeIntent.putExtra(TerminalActivity.INTENT_ACTION_RELOAD, "console_style");
            sendBroadcast(executeIntent);
        } catch (Exception e) {
            Log.w(EmulatorDebug.LOG_TAG, "Failed to write " + outputFileName, e);
            Toast.makeText(this, R.string.style_toast_install_failed, Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("WeakerAccess")
    private static class Selectable {
        final String displayName;
        final String fileName;

        public Selectable(final String fileName) {
            String name = fileName.replace('-', ' ');
            int dotIndex = name.lastIndexOf('.');
            if (dotIndex != -1) name = name.substring(0, dotIndex);

            this.displayName = capitalize(name);
            this.fileName = fileName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }
}
