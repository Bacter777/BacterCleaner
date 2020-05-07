package com.bacter.bactercleaner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;
import androidx.transition.TransitionManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fxn.stash.Stash;

import java.io.File;
import java.text.DecimalFormat;

public class MainActivity extends AppCompatActivity {
    ConstraintSet constraintSet = new ConstraintSet();
    static boolean running = false;
    SharedPreferences prefs;
    LinearLayout fileListView;
    ScrollView fileScrollView;
    ProgressBar scanPBar;
    TextView progressText;
    TextView statusText;
    ConstraintLayout layout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Stash.init(getApplicationContext());
        fileListView = findViewById(R.id.fileListview);
        fileScrollView = findViewById(R.id.fileScrollView);
        scanPBar = findViewById(R.id.scanProgress);
        progressText = findViewById(R.id.ScanTextView);
        statusText = findViewById(R.id.statusTextView);
        layout = findViewById(R.id.main_layout);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        constraintSet.clone(layout);
        requestWriteExternalPermission();
    }
    public final void firstTime() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("firsttime", false);
        editor.apply();
        new AlertDialog.Builder(this, R.style.MyAlertDialogTheme)
                .setTitle("HOY!!!")
                .setMessage(
                        "Hello There! I made this app because I LOVE to Code." +
                                "Tripping pod...lol" +
                                "This App has no ADS.." + "If you like this App," +
                                "Please Donate Bacter for his future App Development")
                // TODO :
                // DIALOG : SAMPLE DISPLAY
                // FINALIZED: PLAYSTORE PUB
                // CHANGE POSITIVE, NEGATIVE BUTTON DIALOGS( "FOR PLAYSTORE PUB")
                .setPositiveButton("Buy BACTER A REDHORSE BEER!", (dialog, whichButton) -> {
                })
                .setNegativeButton("AHUK LANG...", (dialog, whichButton) -> {})
                .show();
    }
    public final void settings(View view) {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    public final void clean(View view) {
        if (!running) {
            if (!prefs.getBoolean("one_click", false)) // ONE-CLICK BUTTON DISABLED
                new AlertDialog.Builder(this,R.style.MyAlertDialogTheme)
                        .setTitle(R.string.select_task)
                        .setMessage(R.string.do_you_want_to)
                        .setPositiveButton(R.string.clean, (dialog, whichButton) -> { // CLEAN
                            new Thread(()-> scan(true)).start();
                            if (prefs.getBoolean("firsttime", true)) firstTime();
                        })
                        .setNegativeButton(R.string.analyze, (dialog, whichButton) -> { // ANALYZE
                            new Thread(()-> scan(false)).start();
                            if (prefs.getBoolean("firsttime", true)) firstTime();
                        }).show();
            else new Thread(()-> scan(true)).start(); // ONE-CLICK ENABLED
        }
    }

    public void animateBtn() {
        TransitionManager.beginDelayedTransition(layout);
        constraintSet.clear(R.id.cleanButton,ConstraintSet.TOP);
        constraintSet.clear(R.id.statusTextView,ConstraintSet.BOTTOM);
        constraintSet.setMargin(R.id.statusTextView,ConstraintSet.TOP,50);
        constraintSet.applyTo(layout);
    }
    @SuppressLint("SetTextI18n")
    private void scan(boolean delete) {
        Looper.prepare();
        running = true;
        reset();

        File path = Environment.getExternalStorageDirectory();

        // scanner setup
        FileScanner fs = new FileScanner(path);
        fs.setEmptyDir(prefs.getBoolean("empty", false));
        fs.setAutoWhite(prefs.getBoolean("auto_white", true));
        fs.setDelete(delete);
        fs.setGUI(this);

        // filters
        fs.setUpFilters(prefs.getBoolean("generic", true),
                prefs.getBoolean("aggressive", false),
                prefs.getBoolean("apk", false));

        // failed scan
        if (path.listFiles() == null) { // is this needed? yes.
            TextView textView = printTextView("Scan failed.", Color.RED);
            runOnUiThread(() -> fileListView.addView(textView));
        }
        runOnUiThread(() -> {
            animateBtn();
            statusText.setText(getString(R.string.status_running));
        });
        // start scanning
        long kilobytesTotal = fs.startScan();
        // crappy but working fix for percentage never reached 100
        runOnUiThread(() -> {
            scanPBar.setProgress(scanPBar.getMax());
            progressText.setText("100%");
        });
        // kilobytes found/freed text
        runOnUiThread(() -> {
            if (delete) {
                statusText.setText(getString(R.string.freed) + " " + convertSize(kilobytesTotal));
            } else {
                statusText.setText(getString(R.string.found) + " " + convertSize(kilobytesTotal));
            }
        });
        fileScrollView.post(() -> fileScrollView.fullScroll(ScrollView.FOCUS_DOWN));

        running = false;
        Looper.loop();
    }

    private synchronized TextView printTextView(String text, int color) {
        TextView textView = new TextView(MainActivity.this);
        textView.setTextColor(color);
        textView.setText(text);
        textView.setPadding(3,3,3,3);
        return textView;
    }

    private String convertSize(long length) {
        final DecimalFormat format = new DecimalFormat("#.##");
        final long MiB = 1024 * 1024;
        final long KiB = 1024;

        if (length > MiB) {
            return format.format(length / MiB) + " MB";
        }
        if (length > KiB) {
            return format.format(length / KiB) + " KB";
        }
        return format.format(length) + " B";
    }

    synchronized TextView displayPath(File file) {
        // creating and adding a text view to the scroll view with path to file
        TextView textView = printTextView(file.getAbsolutePath(), getResources().getColor(R.color.colorAccent));

        // adding to scroll view
        runOnUiThread(() -> fileListView.addView(textView));

        // scroll to bottom
        fileScrollView.post(() -> fileScrollView.fullScroll(ScrollView.FOCUS_DOWN));

        return textView;
    }
    private synchronized void reset() {
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        runOnUiThread(() -> {
            fileListView.removeAllViews();
            scanPBar.setProgress(0);
            scanPBar.setMax(1);
        });
    }
     // Request write permission

    public synchronized void requestWriteExternalPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
    }
     // Handles the whether the user grants permission. Launches new fragment asking the user to give file permission.
    @Override
    public void onRequestPermissionsResult(int requestCode,@NonNull String[] permissions,@NonNull int[] grantResults) {
        if (requestCode == 1 &&
                grantResults.length > 0 &&
                grantResults[0] != PackageManager.PERMISSION_GRANTED)
            prompt();
    }
     // Launches the prompt activity
    public final void prompt() {
        Intent intent = new Intent(this, PromptActivity.class);
        startActivity(intent);
    }
}
