package com.cliff.comichelper;

import java.io.File;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceFragment;

public class SettingsActivity extends Activity {
	final public static String KEY_DOWNLOAD_LOCATION = "DownloadLocation";
	final public static String KEY_ENABLE_JAVASCRIPT = "EnableJavascript";
	final public static String DIR_IBUKA = "/ibuka/my";
	final public static String DIR_DOWNLOAD = "/Download";
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);
        }
    }
    
    public static void initSettings(SharedPreferences pref) {
    	String downloadLocation = pref.getString(KEY_DOWNLOAD_LOCATION, null);
    	if (null != downloadLocation)
    		return;
    	
    	Editor editor = pref.edit();
    	editor.putBoolean(KEY_ENABLE_JAVASCRIPT, true);
    	File bukaDirectory = new File(Environment.getExternalStorageDirectory().getPath()+DIR_IBUKA);
    	if (bukaDirectory.exists()) {
    		editor.putString(KEY_DOWNLOAD_LOCATION, DIR_IBUKA);
    	}
    	else {
			editor.putString(KEY_DOWNLOAD_LOCATION, DIR_DOWNLOAD);
		}
    	editor.commit();
    }
}
