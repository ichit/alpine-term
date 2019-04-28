package xeffyr.alpine.term.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.os.Build;
import android.system.Os;
import android.util.Log;
import android.util.Pair;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import xeffyr.alpine.term.BuildConfig;
import xeffyr.alpine.term.R;
import xeffyr.alpine.term.terminal.EmulatorDebug;

/**
 * Install the environment if necessary by following the below steps:
 * <p/>
 * (1) Check for version file. If specified version does not match application's version code, then delete
 * environment directory ($PREFIX) if it exist.
 * <p/>
 * (2) A progress dialog is shown with "Updating environment..." message and a spinner.
 * <p/>
 * (3) Extract file 'assets/environment/{device_architecture}.bin' into $STAGING_PREFIX.
 * </p>
 * (3.1) If the zip entry encountered is EXECUTABLES.txt, go through it and remember all files that should have executable bit.
 * </p>
 * (3.2) If the zip entry encountered is SYMLINKS.txt, go through it and remember all symlinks to setup.
 * <p/>
 * (4) Extract the OS image and place it into $STAGING_PREFIX.
 * </p>
 * (5) Extract the entry point script (assets/environment/entrypoint.bash) and place it into $STAGING_PREFIX.
 * </p>
 * (6) Set executable permissions according to EXECUTABLES.txt.
 * </p>
 * (7) Create symlinks according to SYMLINKS.txt.
 * </p>
 * (8) Rename $STAGING_PREFIX to $PREFIX.
 */

@SuppressWarnings("WeakerAccess")
final class Installer {

    public static String getEnvironmentPrefix(final Context context) {
        return context.getFilesDir().getAbsolutePath() + "/environment";
    }

    /**
     * Performs setup if necessary.
     */
    public static void setupIfNeeded(final Activity activity, final Runnable whenDone) {
        // This will cause installer to stop if CPU architecture is unsupported.
        if (!isDeviceSupported()) {
            throw new RuntimeException("Device CPU architecture is unsupported. Build.SUPPORTED_ABIS =  " +
                Arrays.toString(Build.SUPPORTED_ABIS));
        }

        final File PREFIX_DIR = new File(getEnvironmentPrefix(activity.getApplicationContext()));
        if (PREFIX_DIR.isDirectory()) {
            whenDone.run();
            return;
        }

        final ProgressDialog progress = ProgressDialog.show(activity, null,
            activity.getString(R.string.installer_progress_message), true, false);

        new Thread() {
            @Override
            public void run() {
                try {
                    final String STAGING_PREFIX_PATH = activity.getApplicationContext()
                        .getFilesDir().getAbsolutePath() + "/environment.staging";
                    final File STAGING_PREFIX_DIR = new File(STAGING_PREFIX_PATH);

                    if (STAGING_PREFIX_DIR.exists()) {
                        deleteFolder(STAGING_PREFIX_DIR);
                    }

                    final byte[] buffer = new byte[16384];
                    AssetManager assetManager = activity.getAssets();

                    try (ZipInputStream zipInput = new ZipInputStream(assetManager.open("environment/data.bin"))) {
                        ZipEntry zipEntry;
                        while ((zipEntry = zipInput.getNextEntry()) != null) {
                            String zipEntryName = zipEntry.getName();
                            File targetFile = new File(STAGING_PREFIX_PATH, zipEntryName);

                            if (zipEntry.isDirectory()) {
                                if (!targetFile.mkdirs())
                                    throw new RuntimeException("Failed to create directory: " + targetFile.getAbsolutePath());
                            } else {
                                try (FileOutputStream outStream = new FileOutputStream(targetFile)) {
                                    int readBytes;
                                    while ((readBytes = zipInput.read(buffer)) != -1) {
                                        outStream.write(buffer, 0, readBytes);
                                    }
                                    outStream.flush();
                                    Os.chmod(targetFile.getAbsolutePath(), 0x100);
                                }
                            }
                        }
                    }

                    // Extract HDD image for QEMU.
                    try (InputStream inStream = assetManager.open("environment/os_image.qcow2")) {
                        try (FileOutputStream outStream = new FileOutputStream(STAGING_PREFIX_PATH + "/os_image.qcow2")) {
                            int readBytes;
                            while ((readBytes = inStream.read(buffer)) != -1) {
                                outStream.write(buffer, 0, readBytes);
                            }
                            outStream.flush();
                            Os.chmod(STAGING_PREFIX_PATH + "/os_image.qcow2", 0x100);
                        }
                    }

                    // Extract CD-ROM image for QEMU.
                    try (InputStream inStream = assetManager.open("environment/os_cdrom.iso")) {
                        try (FileOutputStream outStream = new FileOutputStream(STAGING_PREFIX_PATH + "/os_cdrom.iso")) {
                            int readBytes;
                            while ((readBytes = inStream.read(buffer)) != -1) {
                                outStream.write(buffer, 0, readBytes);
                            }
                            outStream.flush();
                            Os.chmod(STAGING_PREFIX_PATH + "/os_cdrom.iso", 0x100);
                        }
                    }

                    // Extract entry point script.
                    try (InputStream inStream = assetManager.open("environment/entrypoint.bash")) {
                        try (FileOutputStream outStream = new FileOutputStream(STAGING_PREFIX_PATH + "/entrypoint.bash")) {
                            int readBytes;
                            while ((readBytes = inStream.read(buffer)) != -1) {
                                outStream.write(buffer, 0, readBytes);
                            }
                            outStream.flush();
                            Os.chmod(STAGING_PREFIX_PATH + "/entrypoint.bash", 0x140);
                        }
                    }

                    if (!STAGING_PREFIX_DIR.renameTo(PREFIX_DIR)) {
                        throw new RuntimeException("Unable to rename staging folder");
                    }

                    activity.runOnUiThread(whenDone);
                } catch (final Exception e) {
                    Log.e(EmulatorDebug.LOG_TAG, "Installation error", e);
                    activity.runOnUiThread(() -> {
                        try {
                            new AlertDialog.Builder(activity)
                                .setTitle(R.string.installer_error_title)
                                .setMessage(R.string.installer_error_body)
                                .setNegativeButton(R.string.exit_label, (dialog, which) -> {
                                    dialog.dismiss();
                                    activity.finish();
                                }).setPositiveButton(R.string.installer_error_try_again_button, (dialog, which) -> {
                                dialog.dismiss();
                                Installer.setupIfNeeded(activity, whenDone);
                            }).show();
                        } catch (WindowManager.BadTokenException e1) {
                            // Activity already dismissed - ignore.
                        }
                    });
                } finally {
                    activity.runOnUiThread(() -> {
                        try {
                            progress.dismiss();
                        } catch (RuntimeException e) {
                            // Activity already dismissed - ignore.
                        }
                    });
                }
            }
        }.start();
    }

    /**
     * Check whether device's CPU architecture is supported.
     */
    private static boolean isDeviceSupported() {
        // Note that we cannot use System.getProperty("os.arch") since that may give e.g. "aarch64"
        // while a 64-bit runtime may not be installed (like on the Samsung Galaxy S5 Neo).
        // Instead we search through the supported abi:s on the device, see:
        // http://developer.android.com/ndk/guides/abis.html
        // Note that we search for abi:s in preferred order (the ordering of the
        // Build.SUPPORTED_ABIS list) to avoid e.g. installing arm on an x86 system where arm
        // emulation is available.
        for (String androidArch : Build.SUPPORTED_ABIS) {
            switch (androidArch) {
                case "arm64-v8a":
                    return true;
            }
        }

        return false;
    }

    /**
     * Delete a folder and all its content or throw. Don't follow symlinks.
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void deleteFolder(File fileOrDirectory) throws IOException {
        if (!isSymlink(fileOrDirectory) && fileOrDirectory.isDirectory()) {
            // Make sure that we can access file or directory before deletion.
            fileOrDirectory.setReadable(true);
            fileOrDirectory.setWritable(true);
            fileOrDirectory.setExecutable(true);

            File[] children = fileOrDirectory.listFiles();

            if (children != null) {
                for (File child : children) {
                    deleteFolder(child);
                }
            }
        }

        if (!fileOrDirectory.delete()) {
            throw new RuntimeException("Unable to delete " +
                (fileOrDirectory.isDirectory() ? "directory " : "file ") + fileOrDirectory.getAbsolutePath());
        }
    }

    private static boolean isSymlink(File file) throws IOException {
        File canon;

        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = file.getParentFile().getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }

        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }
}
