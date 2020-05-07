package com.bacter.bactercleaner;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.Color;
import android.widget.TextView;

import com.fxn.stash.Stash;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static com.bacter.bactercleaner.WhitelistActivity.getWhiteList;

class FileScanner {
    private File path;
    private Resources res;
    private MainActivity gui;
    private int filesRemoved = 0;
    private long kilobytesTotal = 0;
    private boolean delete = false;
    private boolean emptyDir = false;
    private boolean autoWhite = true;
    private static ArrayList<String> filters = new ArrayList<>();
    private static String[] protectedFileList = {
            "backup", "copy", "copies", "important", "do_not_edit"};

    FileScanner(File path) {
        this.path = path;
    }
    private List<File> getListFiles() {
        return getListFiles(path);
    }
    private synchronized List<File> getListFiles(File parentDirectory) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDirectory.listFiles();

        if (files != null) {
            for (File file : files) {
                if (!isWhiteListed(file)) { // won't touch if whitelisted
                    if (file.isDirectory()) { // folder

                        if (autoWhite) {
                            if (!autoWhiteList(file))
                                inFiles.add(file);
                        }
                        else inFiles.add(file); // add folder itself

                        inFiles.addAll(getListFiles(file)); // add contents to returned list

                    } else inFiles.add(file); // add file
                }
            }
        }

        return inFiles;
    }
    private synchronized boolean isWhiteListed(File file) {
        for (String path : getWhiteList())
            if (path.equalsIgnoreCase(file.getAbsolutePath())
                    || path.equalsIgnoreCase(file.getName()))
                return true;
        return false;
    }

    private synchronized boolean autoWhiteList(File file) {

        for (String protectedFile : protectedFileList) {
            if (file.getName().toLowerCase().contains(protectedFile) &&
                    !getWhiteList().contains(file.getAbsolutePath().toLowerCase())) {
                getWhiteList().add(file.getAbsolutePath().toLowerCase());
                Stash.put("whiteList", getWhiteList());
                return true;
            }
        }

        return false;
    }

    private synchronized boolean filter(File file) {
        if (file.isDirectory() && isDirectoryEmpty(file)
                && emptyDir) return true; // empty folder

        for (String filter : filters)
            if (file.getAbsolutePath().toLowerCase()
                    .matches(filter.toLowerCase()))
                return true; // file

        return false; // not empty folder or file in filter
    }

    private synchronized boolean isDirectoryEmpty(File directory) {

        return Objects.requireNonNull(directory.listFiles()).length == 0;
    }

    @SuppressLint("ResourceType")
    synchronized void setUpFilters(boolean generic, boolean aggressive, boolean apk) {
        List<String> folders = new ArrayList<>();
        List<String> files = new ArrayList<>();

        if (gui != null)
            setResources(gui.getResources());
        if (generic) {
            folders.addAll(Arrays.asList(res.getStringArray(R.array.generic_filter_folders)));
            files.addAll(Arrays.asList(res.getStringArray(R.array.generic_filter_files)));
        }
        if (aggressive) {
            folders.addAll(Arrays.asList(res.getStringArray(R.array.aggressive_filter_folders)));
            files.addAll(Arrays.asList(res.getStringArray(R.array.aggressive_filter_files)));
        }
        // filters
        filters.clear();
        for (String folder : folders)
            filters.add(getRegexForFolder(folder));
        for (String file : files)
            filters.add(getRegexForFile(file));
        // apk
        if (apk) filters.add(getRegexForFile(".apk"));
    }
    @SuppressLint("SetTextI18n")
    long startScan() {
        byte cycles = 0;
        byte maxCycles = 10;
        List<File> foundFiles;
        if (!delete) maxCycles = 1; // when nothing is being deleted. Stops duplicates from being found

        // removes the need to 'clean' multiple times to get everything
        while (cycles < maxCycles) {

            // find files
            foundFiles = getListFiles();
            if (gui != null) gui.scanPBar.setMax(gui.scanPBar.getMax() + foundFiles.size());

            // scan & delete
            for (File file : foundFiles) {
                if (filter(file)) { // filter
                    TextView tv = null;
                    if (gui != null)
                        tv = gui.displayPath(file);

                    if (delete) {
                        kilobytesTotal += file.length();
                        ++filesRemoved;
                        if (file.delete()) { // deletion

                        } else if (tv != null) tv.setTextColor(Color.GRAY); // error effect
                    } else {
                        kilobytesTotal += file.length();
                    }
                }

                if (gui != null) {
                    // progress
                    gui.runOnUiThread(() -> gui.scanPBar.setProgress(gui.scanPBar.getProgress() + 1));
                    double scanPercent = gui.scanPBar.getProgress() * 100.0 / gui.scanPBar.getMax();
                    gui.runOnUiThread(() -> gui.progressText.setText(String.format(Locale.US, "%.0f", scanPercent) + "%"));
                }
            }

            if (filesRemoved == 0) break; // nothing found this run, no need to run again

            filesRemoved = 0; // reset for next cycle
            ++cycles;
        }

        return kilobytesTotal;
    }

    private String getRegexForFolder(String folder) {
        return ".*(\\\\|/)" + folder + "(\\\\|/|$).*";
    }

    private String getRegexForFile(String file) {
        return ".+"+ file.replace(".", "\\.") + "$";
    }

    void setGUI(MainActivity gui) {
        this.gui = gui;
    }

    private void setResources(Resources res) {
        this.res = res;
    }

    void setEmptyDir(boolean emptyDir) {
        this.emptyDir = emptyDir;
    }

    void setDelete(boolean delete) {
        this.delete = delete;
    }

    void setAutoWhite(boolean autoWhite) {
        this.autoWhite = autoWhite;
    }
}
