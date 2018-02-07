package com.sentaroh.android.WildBirdPlayer;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log; 

@SuppressLint("NewApi")
public class SettingActivity extends PreferenceActivity{
	private static boolean DEBUG_ENABLE=true;
	private static final String APPLICATION_TAG="WildBirdPlayer";
	private static Context mContext=null;
	private static PreferenceFragment mPrefFrag=null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onCreate entered");
        if (Build.VERSION.SDK_INT>=11) return;
	};

    @Override
    public void onStart(){
        super.onStart();
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onStart entered");
    };
 
    @Override
    public void onResume(){
        super.onResume();
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onResume entered");
        setTitle(R.string.settings_main_title);
    };
 
    @Override
    public void onBuildHeaders(List<Header> target) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onBuildHeaders entered");
        loadHeadersFromResource(R.xml.settings_frag, target);
    };

//    @Override
//    public boolean isMultiPane () {
//    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity isMultiPane entered");
//        return true;
//    };

    @Override
    public boolean onIsMultiPane () {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onIsMultiPane entered");
        return true;
    };

	@Override  
	protected void onPause() {  
	    super.onPause();  
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onPause entered");
	};

	@Override
	final public void onStop() {
		super.onStop();
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onStop entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingActivity onDestroy entered");
	};

	private static void initSettingValueAfterHc(SharedPreferences shared_pref, String key_string) {
		initSettingValue(mPrefFrag.findPreference(key_string),shared_pref,key_string);
	};

	private static void initSettingValue(Preference pref_key, 
			SharedPreferences shared_pref, String key_string) {
		
		if (!checkUiSettings(pref_key,shared_pref, key_string,mContext))
		if (!checkScanSettings(pref_key,shared_pref, key_string,mContext))
		if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))				    	
		   	checkOtherSettings(pref_key,shared_pref, key_string,mContext);
	};

	private static SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =   
		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
		    public void onSharedPreferenceChanged(SharedPreferences shared_pref, 
		    		String key_string) {
		    	Preference pref_key=mPrefFrag.findPreference(key_string);
				if (!checkUiSettings(pref_key,shared_pref, key_string,mContext))
				if (!checkScanSettings(pref_key,shared_pref, key_string,mContext))
				if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))				    	
				  	checkOtherSettings(pref_key,shared_pref, key_string,mContext);
		    }
	};

	private static boolean checkUiSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_player_when_player_file_switched_playback))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_player_max_screen_brightness_when_image_showed))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_show_audio_file_artwork))) {
    		isChecked=true;
    		String ts=shared_pref.getString(key_string,"0");
    		String[] ts_label= c.getResources().getStringArray(R.array.settings_show_audio_file_artwork_list_entries);
    		if (ts.equals("0")) {
        		pref_key.setSummary(ts_label[0]);
    		} else if (ts.equals("1")) {
        		pref_key.setSummary(ts_label[1]);
    		} else if (ts.equals("2")) {
        		pref_key.setSummary(ts_label[2]);
    		}
    		
    	} else if (key_string.equals(c.getString(R.string.settings_startup_player_position_resumed))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_show_decription_full_screen))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_description_text_size))) {
    		isChecked=true;
    		String ts=shared_pref.getString(key_string,"100");
    		String[] ts_label= c.getResources().getStringArray(R.array.settings_description_text_size_list_entries);
    		if (ts.equals("100")) {
        		pref_key.setSummary(ts_label[0]);
    		} else if (ts.equals("120")) {
        		pref_key.setSummary(ts_label[1]);
    		} else if (ts.equals("150")) {
        		pref_key.setSummary(ts_label[2]);
    		} else if (ts.equals("200")) {
        		pref_key.setSummary(ts_label[3]);
    		}
    	} 

		return isChecked;
	};

	private static boolean checkMiscSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_debug_enable))) {
    		isChecked=true;
    	} else if (key_string.equals(c.getString(R.string.settings_exit_cleanly))) {
    		isChecked=true;
    	} 
		return isChecked;
	};


	private static boolean checkScanSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		

    	return isChecked;
	};

	private static boolean checkOtherSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
//		Log.v("","other key="+key_string);
		boolean isChecked = true;
    	if (pref_key!=null) {
    		pref_key.setSummary(
	    		c.getString(R.string.settings_default_current_setting)+
	    		shared_pref.getString(key_string, "0"));
    	} else {
    		Log.v("TextFileBrowserSettings","key not found. key="+key_string);
    	}
    	return isChecked;
	};

    public static class SettingsUi extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentUi onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_ui);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_player_when_player_file_switched_playback));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_player_max_screen_brightness_when_image_showed));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_show_audio_file_artwork));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_startup_player_position_resumed));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_show_decription_full_screen));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_description_text_size));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentUi onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentUi onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
	
    public static class SettingsScan extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentScan onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_scan);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

			SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_scan_folder));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentScan onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentScan onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

	
    public static class SettingsMisc extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMisc onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_misc);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);

    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_debug_enable));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_exit_cleanly));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMisc onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMisc onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

}