package com.bacter.bactercleaner;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.heinrichreimersoftware.androidissuereporter.IssueReporterLauncher;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate (savedInstanceState);
        setContentView (R.layout.activity_settings);
        getSupportFragmentManager ().beginTransaction ().replace (R.id.layout, new MyPreferenceFragment()).commit ();
    }
    public static class MyPreferenceFragment extends PreferenceFragmentCompat{
        @Override
        public void onCreate(final Bundle savedInstanceState){
            super.onCreate (savedInstanceState);
            this.setHasOptionsMenu (true);
        }
        @Override
        public void onCreatePreferences(Bundle savedInstanceState,String rootKey) {
            addPreferencesFromResource (R.xml.preferences);
        }
        @Override
        public boolean onPreferenceTreeClick(androidx.preference.Preference preference){
            String key = preference.getKey();
            if ("suggestion".equals (key)){
                reportIssue(getContext ());
            }else if ("privacyPolicy".equals (key)){
                startActivity (new Intent (Intent.ACTION_VIEW, Uri.parse (getString (R.string.privacy_policy_url))));
                return true;
            }
            return super.onPreferenceTreeClick (preference);
        }
        final void reportIssue(Context context){
            IssueReporterLauncher.forTarget ("Bacter777", "BacterCleaner")
                    .theme (R.style.CustomIssueReportTheme)
                    .guestEmailRequired (false)
                    .guestToken ("")
                    .minDescriptionLength (20)
                    .homeAsUpEnabled (true)
                    .launch(context);
        }
    }
}
