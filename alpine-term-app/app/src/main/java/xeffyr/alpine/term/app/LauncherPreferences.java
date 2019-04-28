package xeffyr.alpine.term.app;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import xeffyr.alpine.term.R;
import xeffyr.alpine.term.terminal.EmulatorDebug;

public class LauncherPreferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeDefaults(getApplicationContext());
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();
    }

    public static void initializeDefaults(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor prefsEditor = prefs.edit();

        if (prefs.getString(context.getString(R.string.qemu_ram_key), "").isEmpty()) {
            prefsEditor.putString(context.getString(R.string.qemu_ram_key), "256");
        }

        if (prefs.getString(context.getString(R.string.qemu_hdd1_path_key), "").isEmpty()) {
            prefsEditor.putString(context.getString(R.string.qemu_hdd1_path_key),
                context.getExternalFilesDir(null) + "/os_snapshot.qcow2");
        }

        if (prefs.getString(context.getString(R.string.qemu_upstream_dns_key), "").isEmpty()) {
            prefsEditor.putString(context.getString(R.string.qemu_upstream_dns_key), "1.1.1.1");
        }

        prefsEditor.apply();
    }

    public static class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.launcher_preferences);

            findPreference(getString(R.string.qemu_hdd1_path_key)).setOnPreferenceChangeListener(this);
            findPreference(getString(R.string.qemu_hdd2_path_key)).setOnPreferenceChangeListener(this);
            findPreference(getString(R.string.qemu_cdrom_path_key)).setOnPreferenceChangeListener(this);
            findPreference(getString(R.string.qemu_upstream_dns_key)).setOnPreferenceChangeListener(this);
            findPreference(getString(R.string.qemu_exposed_ports_key)).setOnPreferenceChangeListener(this);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference.getKey().equals(getString(R.string.qemu_hdd1_path_key))) {
                return validateFilePath(value.toString(), false);
            } else if (preference.getKey().equals(getString(R.string.qemu_hdd2_path_key))) {
                return validateFilePath(value.toString(), true);
            } else if (preference.getKey().equals(getString(R.string.qemu_cdrom_path_key))) {
                return validateFilePath(value.toString(), true);
            } else if (preference.getKey().equals(getString(R.string.qemu_upstream_dns_key))) {
                return validateIp(value.toString());
            } else if (preference.getKey().equals(getString(R.string.qemu_exposed_ports_key))) {
                return validatePortForwardingRules(value.toString());
            }

            // Do not perform input validation in any other cases.
            return true;
        }

        private boolean validateFilePath(String path, boolean checkExistence) {
            // Allow empty path: depending on option, a default value will be set
            // or option will be disabled.
            if (path.isEmpty()) {
                return true;
            }

            File targetFile = new File(path);
            File parentDir = new File(path).getParentFile();

            // Verify that path won't cause access problems.
            if (path.startsWith("/") && parentDir != null) {
                if (parentDir.canWrite()) {
                    if (checkExistence) {
                        if (targetFile.canWrite() && !targetFile.isDirectory()) {
                            return true;
                        }
                    } else {
                        if (!targetFile.isDirectory()) {
                            return true;
                        }
                    }
                }
            }

            new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.pref_dialog_invalid_file_path_title)
                .setMessage(R.string.pref_dialog_invalid_file_path_body)
                .setPositiveButton(R.string.ok_label, (dialogInterface, i) -> dialogInterface.dismiss()).show();

            return false;
        }

        private boolean validateIp(String ip) {
            String regex = "^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

            try {
                Matcher ipv4Matcher = Pattern.compile(regex).matcher(ip);

                if (ipv4Matcher.matches()) {
                    return true;
                }
            } catch (PatternSyntaxException e) {
                Log.e(EmulatorDebug.LOG_TAG, "Failed to compile regex", e);
            }

            new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.pref_dialog_invalid_dns_ip_title)
                .setMessage(R.string.pref_dialog_invalid_dns_ip_body)
                .setPositiveButton(R.string.ok_label, (dialogInterface, i) -> dialogInterface.dismiss()).show();

            return false;
        }

        private boolean validatePortForwardingRules(String rules) {
            if (rules.isEmpty()) {
                return true;
            }

            for (String rule : rules.split(",")) {
                rule = rule.trim();

                if (rule.isEmpty()) {
                    continue;
                }

                if (rule.matches("(?:tcp|udp):\\d{1,5}:\\d{1,5}")) {
                    String[] ports = rule.split(":");
                    int external, internal;

                    try {
                        external = Integer.parseInt(ports[1]);
                        internal = Integer.parseInt(ports[2]);

                        if (external < 1024 || external > 65535) {
                            new AlertDialog.Builder(getActivity())
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(R.string.pref_dialog_fwd_invalid_port_title)
                                .setMessage(getString(R.string.pref_dialog_fwd_invalid_external_port_body, rule))
                                .setPositiveButton(R.string.ok_label, (dialogInterface, i) -> dialogInterface.dismiss()).show();

                            return false;
                        }

                        if (internal < 1 || internal > 65535) {
                            new AlertDialog.Builder(getActivity())
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle(R.string.pref_dialog_fwd_invalid_port_title)
                                .setMessage(getString(R.string.pref_dialog_fwd_invalid_internal_port_body, rule))
                                .setPositiveButton(R.string.ok_label, (dialogInterface, i) -> dialogInterface.dismiss()).show();

                            return false;
                        }
                    } catch (Exception e) {
                        Log.e(EmulatorDebug.LOG_TAG, "Error occurred while checking port forwarding rules", e);
                        return false;
                    }
                } else {
                    new AlertDialog.Builder(getActivity())
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setTitle(R.string.pref_dialog_fwd_invalid_port_title)
                        .setMessage(getString(R.string.pref_dialog_fwd_invalid_rule, rule))
                        .setPositiveButton(R.string.ok_label, (dialogInterface, i) -> dialogInterface.dismiss()).show();

                    return false;
                }
            }

            return true;
        }
    }
}
