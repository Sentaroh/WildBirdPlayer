package com.sentaroh.android.WildBirdPlayer;

import java.util.ArrayList;

import com.sentaroh.android.Utilities.Dialog.CommonDialog;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Environment;
import android.preference.PreferenceManager;

public class GlobalParameters extends Application{
	
//	public boolean initCompleted=false;
	
	public boolean debugEnabled=true;
	
	public CommonDialog commonDlg=null;
	
	public boolean settingExitCleanly=true;
	
	public ArrayList<MusicFileListItem> masterFileList=null;
	public ArrayList<MusicFileListItem> viewedFileList=null;
	public ArrayList<FolderListItem> masterFolderList=null;
	
	public FragmentPlayer fragmentPlayer=null;

	public Resources mainResources=null;
	public Context appContext=null;
	
	public String wildBirdPlayerHomeDir="";
	
	public String settingScanFolder="";
	public int settingImageQuality=30;
	public int settingImagesizeMax=512;
	public boolean settingPlaybackWhenPlayerFileSwitched=false;
	public boolean settingShowDescriptionWhenPlayerFileSwitched=false;
	public boolean settingShowFileNamePositionSelector=true;
	public boolean settingArtworkSlideShow=false;
	public int settingPlayBackVolume=100;
	public boolean settingMaxBrightnessWhenImageShowed=true;
	
	public final static String DISPLAY_ARTWORK_OPTION_SHOW_BEFORE_IMAGE="0";
	public final static String DISPLAY_ARTWORK_OPTION_SHOW_AFTER_IMAGE="1";
	public final static String DISPLAY_ARTWORK_OPTION_NOT_SHOW="2";
	public String settingDisplayArtworkOption=DISPLAY_ARTWORK_OPTION_SHOW_BEFORE_IMAGE;
	
	public boolean settingConfirmExit=false;
//	public boolean settingShowArtworkAndDescription=false;
	
	public boolean settingStartupPlayerPositionResumed=true;
	public boolean playerPositionResumedWhenReload=false;
	
	public boolean settingShowDescrptionByFullscreen=false;
	public int settingDescrptionTextSize=100;

//	@Override
//	public void  onConfigurationChanged(Configuration newConfig) {
//		Log.v("GlobalParms","onConfigurationChanged");
//	};
	
	@SuppressLint("SdCardPath")
	@Override
	public void  onCreate() {
		super.onCreate();
//		Log.v("GlobalParms","onCreate entered");
		loadSettingParms();
	};
	
	public void loadSettingParms() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		String ed=Environment.getExternalStorageDirectory().toString();
		settingPlaybackWhenPlayerFileSwitched=
				prefs.getBoolean(getString(R.string.settings_player_when_player_file_switched_playback),false);
		settingScanFolder=
				prefs.getString(getString(R.string.settings_scan_folder),ed+"/Music");
		debugEnabled=
				prefs.getBoolean(getString(R.string.settings_debug_enable),false);
		settingExitCleanly=
				prefs.getBoolean(getString(R.string.settings_exit_cleanly),false);
		settingPlayBackVolume=
				prefs.getInt(getString(R.string.settings_player_volume),100);
		settingMaxBrightnessWhenImageShowed=
				prefs.getBoolean(getString(R.string.settings_player_max_screen_brightness_when_image_showed),true);
		settingStartupPlayerPositionResumed=
				prefs.getBoolean(getString(R.string.settings_startup_player_position_resumed),true);
		wildBirdPlayerHomeDir=Environment.getExternalStorageDirectory().toString()+
				"/WildBirdPlayer";
		settingShowDescrptionByFullscreen=
				prefs.getBoolean(getString(R.string.settings_show_decription_full_screen),false);
		settingDescrptionTextSize=Integer.parseInt(
				prefs.getString(getString(R.string.settings_description_text_size),"100"));
		settingDisplayArtworkOption=
				prefs.getString(getString(R.string.settings_show_audio_file_artwork),"0");
	};
	
	public void initSettingParms() {
		SharedPreferences prefs = 
				PreferenceManager.getDefaultSharedPreferences(this);
		String ed=Environment.getExternalStorageDirectory().toString();
		if (prefs.getString(getString(R.string.settings_scan_folder),"").equals("")) {
			prefs.edit().putString(getString(R.string.settings_scan_folder),ed+"/Music").commit();
			
			prefs.edit().putBoolean(getString(R.string.settings_player_when_player_file_switched_playback),false).commit();
			prefs.edit().putBoolean(getString(R.string.settings_debug_enable),false).commit();
			prefs.edit().putBoolean(getString(R.string.settings_exit_cleanly),false).commit();
			prefs.edit().putInt(getString(R.string.settings_player_volume),100).commit();
			prefs.edit().putBoolean(getString(R.string.settings_player_max_screen_brightness_when_image_showed),true).commit();
			prefs.edit().putBoolean(getString(R.string.settings_startup_player_position_resumed),true).commit();
		}
	};
	
	
//	public void  onLowMemory() {
//		Log.v("GlobalParms","onLowMemory");
//	}
//	public void  onTerminate() {
//		Log.v("GlobalParms","onTerminate");
//	}
//	public void  onTrimMemory(int level) {
//		Log.v("GlobalParms","onTrimMemory");
//	}

}
