package com.sentaroh.android.WildBirdPlayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;

import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class FragmentPlayer extends Fragment{

	private final String APPLICATION_TAG="FragmentPlayer";
	private final String SAVED_PLAYED_FOLDER_NAME="saved_played_folder_name";
	private final String SAVED_PLAYED_FILE_NAME="saved_played_file_name";
	
	private GlobalParameters mGlblParms=null;
	
//	private boolean mTerminateApplication=false;
	private int mRestartStatus=0;

	@SuppressWarnings("unused")
	private FragmentManager mFragmentManager=null;
	@SuppressWarnings("unused")
	private Resources mResources=null;
	private Context mContext;
	private MainActivity mMainActivity=null; 
	
	private View mMainView=null;

	private MediaPlayer mPlayer=null;
	private ThreadCtrl mTcPlayer=null;
//	private String mPlayedFilePath=null;
//	private String mPlayedFileName=null;
	private int mPlayedFolderListPos=0;
	private int mPlayedFileListPos=0;
	private SeekBar mSongPlayedPos=null;
	private boolean mDisableSpinnerSelection=true;
	private boolean mDisablePlayBack=false;
	
	private AdapterArtworkView mAdapterArtwork=null;
	
	private CustomViewPager mCustomPagerView=null;
	private WebView mDescriptionView=null;
	private RelativeLayout mArtworkView=null;
	private boolean mPlayerRepeat=false;
	private boolean mIsDescriptionShowed=false;

	private ImageButton mMusicFilePrevBtn=null;
	private ImageButton mMusicFileNextBtn=null;
	private ImageView mArtworkImagePrevBtn=null;
	private ImageView mArtworkImageNextBtn=null;
	private TextView mArtworkImageInfo=null;
	private ImageButton mMusicFilePlayBtn=null;
	private ImageButton mDescriptionShowBtn=null;
	private SeekBar mMusciFileVolume=null;
	private ImageButton mMusicFIleRepeatBtn=null;

	private Handler mUiHandler=null;

	private Spinner mFolderSpinner=null;
	private Spinner mFileSpinner=null;

	public static FragmentPlayer newInstance() {
		FragmentPlayer frag = new FragmentPlayer();
        Bundle bundle = new Bundle(); 
        frag.setArguments(bundle);
        return frag;
    };
    
	public FragmentPlayer() {
//		Log.v(APPLICATION_TAG,"Constructor(Default)");
	};

	@Override
	public void onConfigurationChanged(Configuration newConfig){
		super.onConfigurationChanged(newConfig);
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"onConfigurationChanged entered, restartStatus="+mRestartStatus);

		final ViewContentSaveArea vcsa=new ViewContentSaveArea ();
		saveViewContents(vcsa);
		final boolean c_spinner_sel=isDisableSpinnerSelection();
		setDisableSpinnerSelection(true);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup vg=(ViewGroup)getView();
        vg.removeAllViewsInLayout();
        mMainView = inflater.inflate(R.layout.player_view, vg);
        initViewWidget();
        restoreViewContents(vcsa);

		mUiHandler.post(new Runnable() {
			@Override
			public void run() {
				//Scroll値がResetされるのを防止
				mCustomPagerView.setCurrentItem(vcsa.view_pager_current_pos);
				
				if (vcsa.view_image_view_scale!=null) {
					ArtWorkImageArrayItem[] ivt=mAdapterArtwork.getImageViewArray();
					for (int i=0;i<ivt.length;i++) {
						if (ivt[i].image_view!=null) ivt[i].image_view.zoomTo(vcsa.view_image_view_scale[i],50);
//						ivt[i].setScaleX(vcsa.view_image_view_scale[i]);
//						ivt[i].setScaleY(vcsa.view_image_view_scale[i]);
					}
				}
			}
		});

		mUiHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
//				mAdapterArtwork.playGifAnimation(vcsa.view_pager_current_pos);
				setDisableSpinnerSelection(c_spinner_sel);
			}
		},100);

	};
	
	class ViewContentSaveArea {
		public int view_pager_current_pos=0;
		public String artwork_count="0";
		public int folder_spinner_pos=0;
		public int file_spinner_pos=0;
		public float[] view_image_view_scale=null;
	};
	
	private void saveViewContents(ViewContentSaveArea vcsa) {
		vcsa.view_pager_current_pos=mCustomPagerView.getCurrentItem();
		mCustomPagerView.setAdapter(null);
		vcsa.folder_spinner_pos=mFolderSpinner.getSelectedItemPosition();
		vcsa.file_spinner_pos=mFileSpinner.getSelectedItemPosition();
		if (mAdapterArtwork!=null) {
			ArtWorkImageArrayItem[] ivt=mAdapterArtwork.getImageViewArray();
			vcsa.view_image_view_scale=new float[ivt.length];
			for (int i=0;i<ivt.length;i++) {
				if (ivt[i].image_view!=null) vcsa.view_image_view_scale[i]=ivt[i].image_view.getScale();
			}
		}
	};
	
	private void restoreViewContents(ViewContentSaveArea vcsa) {
		createFolderSpinner();
		createFileSpinner();
    	initFolderSpinner();
    	initFileSpinner(mPlayedFolderListPos);
    	setFolderSpinnerListener();
    	setFileSpinnerListener();
		mFolderSpinner.setSelection(vcsa.folder_spinner_pos);
		mFileSpinner.setSelection(vcsa.file_spinner_pos);
		showBirdImage(vcsa.file_spinner_pos);
		mPlayedFileListPos=vcsa.file_spinner_pos;
		
		setDescriptionDisplayMode();
		setScreenBrightness();
		setPlayerButton(vcsa.file_spinner_pos);
	};
	
	@Override
	public void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"onSaveInstanceState entered");
		if(outState.isEmpty()){
	        outState.putBoolean("WORKAROUND_FOR_BUG_19917_KEY", true);
	    }
	};  

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMainActivity=(MainActivity) getActivity();
        mContext=getActivity().getApplicationContext();
        mGlblParms=GlobalWorkArea.getGlobalParameters(mContext);
        if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"onCreate entered, savedInstanceState="+savedInstanceState);
//      setRetainInstance(true);
        mResources=getResources();
        mFragmentManager=getFragmentManager();

        mUiHandler=new Handler();
        
        mGlblParms.fragmentPlayer=this;

        if (savedInstanceState==null) mRestartStatus=0;
        else mRestartStatus=2;

		if (mPlayer==null) {
			mPlayer=new MediaPlayer();
			mTcPlayer=new ThreadCtrl();
		}
	};

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"onCreateView entered, restartStatus="+mRestartStatus);
		mMainView = inflater.inflate(R.layout.player_view, container, false);

        if (mRestartStatus!=2) {
        	initViewWidget();
    		createFolderSpinner();
    		createFileSpinner();
    		
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            String saved_folder_name=prefs.getString(SAVED_PLAYED_FOLDER_NAME, "");
            String saved_file_name=prefs.getString(SAVED_PLAYED_FILE_NAME, "");
    		if (mGlblParms.settingStartupPlayerPositionResumed || mGlblParms.playerPositionResumedWhenReload) {
                for (int i=0;i<mGlblParms.masterFolderList.size();i++) {
                	if (mGlblParms.masterFolderList.get(i).folderName.equals(saved_folder_name)) {
                		mPlayedFolderListPos=i;
                		break;
                	}
                }
    		}
        	initFolderSpinner();
    		initFileSpinner(mPlayedFolderListPos);

    		if (mGlblParms.settingStartupPlayerPositionResumed || mGlblParms.playerPositionResumedWhenReload) {
                for (int i=0;i<mGlblParms.viewedFileList.size();i++) {
                	if (mGlblParms.viewedFileList.get(i).musicFileName.equals(saved_file_name)) {
                		mPlayedFileListPos=i;
                		break;
                	}
                }
    		}
            mFileSpinner.setSelection(mPlayedFileListPos);
            
        	setFolderSpinnerListener();
        	setFileSpinnerListener();
        	
			mGlblParms.playerPositionResumedWhenReload=false;
        }

		return mMainView;
	}
    
	@Override
	public void onStart() {
		super.onStart();
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"onStart entered, restartStatus="+mRestartStatus);
	};

	@Override
	public void onResume() {
		super.onResume();
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"onResume entered, restartStatus="+mRestartStatus);
		mUiHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				setDisableSpinnerSelection(false);
			}
		},500);
		if (mRestartStatus==0) {
			showBirdImage(mPlayedFileListPos);
			setPlayerButton(mPlayedFileListPos);
//			executeTest();
		} else if (mRestartStatus==1) {
			setScreenBrightness();
		} else if (mRestartStatus==2) {
//			showBirdImage(mPlayedFileListPos);
		}
		mRestartStatus=1;
		
	};
	
	@Override
	public void onPause() {
		super.onPause();
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"onPause entered, restartStatus="+mRestartStatus);
        // Application process is follow
		setDisableSpinnerSelection(true);
		savePlayerSettings();
	};

	@Override
	public void onStop() {
		super.onStop();
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"onStop entered, restartStatus="+mRestartStatus);
        // Application process is follow
	};

	public void savePlayerSettings() {
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"savePlayerSetting entered, restartStatus="+mRestartStatus);
//		Thread.currentThread().dumpStack();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		int vol=prefs.getInt(getString(R.string.settings_player_volume), 100);
		if (mGlblParms.settingPlayBackVolume!=vol) {
			prefs.edit().putInt(getString(R.string.settings_player_volume),
					mGlblParms.settingPlayBackVolume).commit();
		}
		if (mGlblParms.masterFolderList!=null && mGlblParms.viewedFileList!=null &&
				mGlblParms.masterFolderList.size()!=0 && mGlblParms.viewedFileList.size()!=0) {
			String folder_name=prefs.getString(SAVED_PLAYED_FOLDER_NAME, "");
			String file_name=prefs.getString(SAVED_PLAYED_FILE_NAME, "");
			String c_folder_name=mGlblParms.masterFolderList.get(mPlayedFolderListPos).folderName;
			String c_file_name=mGlblParms.viewedFileList.get(mPlayedFileListPos).musicFileName;

			if (!c_folder_name.equals(folder_name)) {
				prefs.edit().putString(SAVED_PLAYED_FOLDER_NAME, c_folder_name).commit();
			}
			if (!c_file_name.equals(file_name)) {
				prefs.edit().putString(SAVED_PLAYED_FILE_NAME, c_file_name).commit();
			}
			if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"Player setting was saved, restartStatus="+mRestartStatus);
		}
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"onDestroy entered, restartStatus="+mRestartStatus);
        // Application process is follow
		stopMusicPlayBack(mGlblParms);
		if (mCustomPagerView!=null) mCustomPagerView.setAdapter(null);
		if (mAdapterArtwork!=null) mAdapterArtwork.cleanup();

		mMainActivity.setBackLightLevelToDefault();
		
		mFragmentManager=null;
		mResources=null;
		mContext=null;
		mMainActivity=null; 
		
		mMainView=null;
		if (mPlayer!=null) {
			while(mPlayer.isPlaying()) {
				mTcPlayer.setDisabled();
				synchronized(mPlayer) {
					try {
						mPlayer.wait(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			mPlayer.release();
			mPlayer=null;
			mTcPlayer=null;
		}

		mAdapterArtwork=null;
		mCustomPagerView=null;

		System.gc();
	};

	public void reshowBirdImage() {
		showBirdImage(mFileSpinner.getSelectedItemPosition());
	}
	
	private void createFolderSpinner() {
		mFolderSpinner=(Spinner)mMainView.findViewById(R.id.player_view_music_folder_spinner);
	};
	
	private void initFolderSpinner() {
		CustomSpinnerAdapter mAdapterFolderSpinner = new CustomSpinnerAdapter(mContext, R.layout.custom_simple_spinner_item);
		for (int i=0;i<mGlblParms.masterFolderList.size();i++) {
			FolderListItem fli=mGlblParms.masterFolderList.get(i);
			if (fli.folderName.lastIndexOf("/")>=0) {
				String fn=fli.folderName.substring(fli.folderName.lastIndexOf("/"));
				mAdapterFolderSpinner.add(fn.replace("/", ""));
			} else {
				mAdapterFolderSpinner.add(fli.folderName);
			}
		}
		mAdapterFolderSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mFolderSpinner.setPrompt(mContext.getString(R.string.msgs_main_player_folder_spinner_title));
		mFolderSpinner.setAdapter(mAdapterFolderSpinner);
		mFolderSpinner.setSelected(true);
		mFolderSpinner.setSelection(mPlayedFolderListPos);
	};
	
	private boolean isDisableSpinnerSelection() {
//		Log.v("","isDisableSpinnerSelection="+mDisableSpinnerSelection);
		return mDisableSpinnerSelection;
	};

	private void setDisableSpinnerSelection(boolean p) {
//		Log.v("","setDisableSpinnerSelection="+p);
//		Thread.currentThread().dumpStack();
		mDisableSpinnerSelection=p;
	};

	private void setFolderSpinnerListener() {
		if (mGlblParms.debugEnabled) 
			Log.v(APPLICATION_TAG,"setFolderSpinnerListener entered");
		mFolderSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int sel_pos, long arg3) {
//				Log.v("","folder pos="+sel_pos+", mDisableSpinnerSelection="+isDisableSpinnerSelection());
				if (!isDisableSpinnerSelection()) {
					mDisablePlayBack=true;
					initFileSpinner(sel_pos);
					mPlayedFolderListPos=sel_pos;
					savePlayerSettings();
					mUiHandler.postDelayed(new Runnable() {
						@Override
						public void run() {
							mDisablePlayBack=false;
						}
					},100);
				}
//				setPlayerNewPos(false, 0);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	};
	
	private void createFileSpinner() {
		mFileSpinner=(Spinner)mMainView.findViewById(R.id.player_view_music_file_spinner);
	};
	
	private void initFileSpinner(int folder_pos) {
		CustomSpinnerAdapter mAdapterFileSpinner = new CustomSpinnerAdapter(mContext, R.layout.custom_simple_spinner_item);
		String folder=mGlblParms.masterFolderList.get(folder_pos).folderName;
		mGlblParms.viewedFileList=new ArrayList<MusicFileListItem>();
		for (int i=0;i<mGlblParms.masterFileList.size();i++) {
			if (folder.equals(mGlblParms.masterFileList.get(i).musicFolderName)) {
				mGlblParms.viewedFileList.add(mGlblParms.masterFileList.get(i));
				mAdapterFileSpinner.add(mGlblParms.masterFileList.get(i).musicFileName);
			}
		}
		mAdapterFileSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		mFileSpinner.setPrompt(mContext.getString(R.string.msgs_main_player_file_spinner_title));
		mFileSpinner.setAdapter(mAdapterFileSpinner);
		mFileSpinner.setSelected(true);
		mFileSpinner.setSelection(0);
		mPlayedFileListPos=0;
	};
	
	private void setFileSpinnerListener() {
		if (mGlblParms.debugEnabled) 
			Log.v(APPLICATION_TAG,"setFileSpinnerListener entered");
		mFileSpinner.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int sel_pos, long arg3) {
//				Log.v("","mDisableSpinnerSelection="+isDisableSpinnerSelection()+", file pos="+sel_pos);
				if (!isDisableSpinnerSelection()) {
					setPlayerNewPos(!mDisablePlayBack, sel_pos);
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	};
	
	private void setPlayerNewPos(boolean play_back_required, int sel_pos) {
		mPlayedFileListPos=sel_pos;
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"setPlayerNewPos entered, pos="+sel_pos);
		
		stopMusicPlayBack(mGlblParms);
		mMusicFilePlayBtn.setImageResource(R.drawable.player_play_disabled);
		mMusicFilePlayBtn.setEnabled(false);
		mMusicFilePrevBtn.setImageResource(R.drawable.prev_file_disabled);
		mMusicFilePrevBtn.setEnabled(false);
		mMusicFileNextBtn.setImageResource(R.drawable.next_file_disabled);
		mMusicFileNextBtn.setEnabled(false);

		showBirdImage(sel_pos);
		
		setPlayerButton(sel_pos);
		
		if (mGlblParms.settingPlaybackWhenPlayerFileSwitched && play_back_required &&
				mGlblParms.viewedFileList.get(sel_pos).musicFileTrackLength>0) 
			prepareMusicPlayBack();
//		if (mGlblParms.settingMaxBrightnessWhenImageShowed) 
//			mMainActivity.setBackLightLevelToMax();
	};

	public boolean processBackKey() {
    	boolean result=false;
    	if (!mGlblParms.settingShowDescrptionByFullscreen) {
            if (!mArtworkView.isShown()) {
  		      mDescriptionShowBtn.performClick();
  		      result=true;
            } 
    	} else {
            if (!mArtworkView.isShown()) {
  		      mDescriptionShowBtn.performClick();
  		      result=true;
            } else {
            	
            }
    	}
        return result;
	};
	
	private void initViewWidget() {
		if (mGlblParms.settingMaxBrightnessWhenImageShowed) mMainActivity.setBackLightLevelToMax();
		mDescriptionView=(WebView)mMainView.findViewById(R.id.player_view_file_description);
		mArtworkView=(RelativeLayout)mMainView.findViewById(R.id.player_view_artwork_image_view);
        mCustomPagerView=(CustomViewPager)mMainView.findViewById(R.id.player_view_artwork_image);
        
    	mMusicFilePrevBtn=(ImageButton)mMainView.findViewById(R.id.player_view_music_file_prev);
    	mMusicFileNextBtn=(ImageButton)mMainView.findViewById(R.id.player_view_music_file_next);
    	mArtworkImagePrevBtn=(ImageView)mMainView.findViewById(R.id.player_view_artwork_image_prev);
    	mArtworkImageNextBtn=(ImageView)mMainView.findViewById(R.id.player_view_artwork_image_next);
    	
    	mArtworkImageInfo=(TextView)mMainView.findViewById(R.id.player_view_artwork_image_info);

		mMusciFileVolume=(SeekBar)mMainView.findViewById(R.id.player_view_volume);
		mMusicFIleRepeatBtn=(ImageButton)mMainView.findViewById(R.id.player_view_repeat);
		mMusicFilePlayBtn=(ImageButton)mMainView.findViewById(R.id.player_view_music_file_play_stop);
		mDescriptionShowBtn=(ImageButton)mMainView.findViewById(R.id.player_view_show_description);
		mSongPlayedPos=(SeekBar)mMainView.findViewById(R.id.player_view_played_pos);
		
		
		mMusciFileVolume.setProgress(mGlblParms.settingPlayBackVolume);
		mMusciFileVolume.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar arg0, int vol, boolean arg2) {
				final float n_vol = (float) (1 - (Math.log(100 - vol) / Math.log(100)));
				mPlayer.setVolume(n_vol, n_vol);
				mGlblParms.settingPlayBackVolume=vol;
			}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {}
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {}
		});

		mArtworkView.setVisibility(RelativeLayout.VISIBLE);
		if (mIsDescriptionShowed) mDescriptionView.setVisibility(WebView.VISIBLE);
		else mDescriptionView.setVisibility(WebView.GONE);

		mDescriptionShowBtn.setOnClickListener(new OnClickListener() {
			@Override
			final public void onClick(View arg0) {
//				if (!mGlblParms.settingShowDescrptionByFullscreen) {
//					if (mIsDescriptionShowed) {
//						mDescriptionView.setVisibility(WebView.GONE);
//					} else {
//						mDescriptionView.setVisibility(WebView.VISIBLE);
//					}
//				} else {
//					if (mIsDescriptionShowed) {
//						if (mGlblParms.settingMaxBrightnessWhenImageShowed) 
//							mMainActivity.setBackLightLevelToMax();
//						mArtworkView.setVisibility(RelativeLayout.VISIBLE);
//						mDescriptionView.setVisibility(WebView.GONE);
//					} else {
//						mMainActivity.setBackLightLevelToDefault();
//						mArtworkView.setVisibility(RelativeLayout.GONE);
//						mDescriptionView.setVisibility(WebView.VISIBLE);
//					}
//				}
				mIsDescriptionShowed=!mIsDescriptionShowed;
				setDescriptionDisplayMode();
				setScreenBrightness();
			}
		});

		if (mPlayerRepeat) {
			mMusicFIleRepeatBtn.setImageResource(R.drawable.repeat_enabled);					
		} else {
			mMusicFIleRepeatBtn.setImageResource(R.drawable.repeat_disabled);
		}
		mMusicFIleRepeatBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				if (mPlayerRepeat) {
					mPlayerRepeat=false;
					if (mPlayer.isPlaying()) {
						mPlayer.setLooping(false);
					}
				} else {
					mPlayerRepeat=true;
					if (mPlayer.isPlaying()) {
						mPlayer.setLooping(true);
					}
				}
				if (mPlayerRepeat) {
					mMusicFIleRepeatBtn.setImageResource(R.drawable.repeat_enabled);					
				} else {
					mMusicFIleRepeatBtn.setImageResource(R.drawable.repeat_disabled);
				}
			}
		});

		mMusicFileNextBtn.setOnClickListener(new OnClickListener() {
			@Override
			final public void onClick(View v) {
				int c_pos=mFileSpinner.getSelectedItemPosition();
				if ((c_pos+1)<mGlblParms.viewedFileList.size()) {
					stopMusicPlayBack(mGlblParms);
					c_pos++;
					mPlayedFileListPos=c_pos;
					mFileSpinner.setSelection(c_pos);
				}
			}
		});

		mMusicFilePrevBtn.setOnClickListener(new OnClickListener() {
			@Override
			final public void onClick(View v) {
				int c_pos=mFileSpinner.getSelectedItemPosition();
				if (c_pos>0) {
					stopMusicPlayBack(mGlblParms);
					c_pos--;
					mPlayedFileListPos=c_pos;
					mFileSpinner.setSelection(c_pos);
				}
			}
		});

		
		mSongPlayedPos.setProgress(0);
		if (mPlayer.isPlaying()) mSongPlayedPos.setEnabled(true);
		else mSongPlayedPos.setEnabled(false);
		mSongPlayedPos.setOnSeekBarChangeListener(new OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(SeekBar arg0, int progress, boolean arg2) {
				if (mPlayer!=null && mPlayer.isPlaying()) {
					if (mSongPlayedPosIsTouched) {
						int n_pos=(mPlayer.getDuration()*progress)/100;
						mPlayer.seekTo(n_pos);
					}
				} 
			}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
				mSongPlayedPosIsTouched=true;
			}
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
				mSongPlayedPosIsTouched=false;
			}
		});

		mPlayer.setOnErrorListener(new OnErrorListener() {
			final public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				String p_fn=mGlblParms.viewedFileList.get(mPlayedFileListPos).musicFileName;
				String msg="error="+arg1+", extra="+arg2+", file="+p_fn;
				mGlblParms.commonDlg.showCommonDialog(false, "E", "Media player error error", msg, null);
				if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,msg);
				mMusicFilePlayBtn.setImageResource(R.drawable.player_play_enabled);
				return true;
			}
		});
		mPlayer.setOnCompletionListener(new OnCompletionListener(){
			@Override
			final public void onCompletion(MediaPlayer arg0) {
				String p_fn=mGlblParms.viewedFileList.get(mPlayedFileListPos).musicFileName;
				if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"setMusicPlayBack completed, file="+
						p_fn);
				mMusicFilePlayBtn.setImageResource(R.drawable.player_play_enabled);
			}
		});
		mPlayer.setOnPreparedListener(new OnPreparedListener(){
			@Override
			final public void onPrepared(MediaPlayer mp) {
				String p_fn=mGlblParms.viewedFileList.get(mPlayedFileListPos).musicFileName;
				if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"setMusicPlayBack prepared, file="+
						p_fn);
				mTcPlayer.setEnabled();
				startMusicPlayBack(mGlblParms);
			}
		});

		if (mPlayer.isPlaying()) mMusicFilePlayBtn.setImageResource(R.drawable.player_stop);
		else mMusicFilePlayBtn.setImageResource(R.drawable.player_play_enabled);
		mMusicFilePlayBtn.setOnClickListener(new OnClickListener() {
			@Override
			final public void onClick(View arg0) {
				prepareMusicPlayBack();
			}
		});
	};

	private boolean mSongPlayedPosIsTouched=false;

	private void prepareMusicPlayBack() {
		String p_path=mGlblParms.viewedFileList.get(mPlayedFileListPos).musicFilePath;
		String p_fn=mGlblParms.viewedFileList.get(mPlayedFileListPos).musicFileName;
		if (mPlayer.isPlaying()) {//Stop play back
			stopMusicPlayBack(mGlblParms);
			mMusicFilePlayBtn.setImageResource(R.drawable.player_play_enabled);
		} else {//Start play back
			File lf=new File(p_path+"/"+p_fn);
//			Log.v("","fp="+mPlayedFilePath+mPlayedFileName);
			if (lf.exists()) {
				mPlayer.reset();
				try {
					mPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
					mPlayer.setDataSource(p_path+"/"+p_fn);
					mPlayer.prepareAsync();
					mMusicFilePlayBtn.setImageResource(R.drawable.player_stop);
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				String msg="file="+p_fn;
				mGlblParms.commonDlg.showCommonDialog(false, "E", "Specified file can not be found",msg,null);
				if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG, msg);
			}
		}
	};

	private void setPlayerButton(int c_pos) {
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG, "setPlayerButton entered, pos="+c_pos);
		if (mGlblParms.viewedFileList.get(c_pos).musicFileTrackLength>0) {
			mMusicFilePlayBtn.setImageResource(R.drawable.player_play_enabled);
			mMusicFilePlayBtn.setEnabled(true);
		} else {
			mMusicFilePlayBtn.setImageResource(R.drawable.player_play_disabled);
			mMusicFilePlayBtn.setEnabled(false);
		}
		if (mGlblParms.viewedFileList.size()>1) {
			if (c_pos>0) {
				if ((c_pos+1)==mGlblParms.viewedFileList.size()) {
					mMusicFileNextBtn.setEnabled(false);
					mMusicFilePrevBtn.setEnabled(true);
				} else {
					mMusicFileNextBtn.setEnabled(true);
					mMusicFilePrevBtn.setEnabled(true);
				}
			} else {
				mMusicFileNextBtn.setEnabled(true);
				mMusicFilePrevBtn.setEnabled(false);
			}
			
		} else {
			mMusicFileNextBtn.setEnabled(false);
			mMusicFilePrevBtn.setEnabled(false);
		}

		if (mMusicFileNextBtn.isEnabled()) mMusicFileNextBtn.setImageResource(R.drawable.next_file_enabled);
		else mMusicFileNextBtn.setImageResource(R.drawable.next_file_disabled);
		
		if (mMusicFilePrevBtn.isEnabled()) mMusicFilePrevBtn.setImageResource(R.drawable.prev_file_enabled);
		else mMusicFilePrevBtn.setImageResource(R.drawable.prev_file_disabled);
		
		if (mGlblParms.viewedFileList.get(c_pos).descriptionIsAvailable) {
			mDescriptionShowBtn.setEnabled(true);
		} else {
			mDescriptionShowBtn.setEnabled(false);
		}
	};

	final private  void startMusicPlayBack(final GlobalParameters mGlblParms) {
		final int duration=mPlayer.getDuration();
//		Log.v("","duration="+mPlayer.getDuration());
		mSongPlayedPos.setEnabled(true);
		mSongPlayedPos.setProgress(0);
		mSongPlayedPosIsTouched=false;
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"startMusicPlayBack started, duration="+duration);
		new Thread() {
			@Override
			public void run() {
				final float volume = (float) (1 - (Math.log(100 - mGlblParms.settingPlayBackVolume) / Math.log(100)));
//				float n_vol=(float)mGlblParms.settingPlayBackVolume/100f;
				mPlayer.setVolume(volume, volume);
				if (mPlayerRepeat) mPlayer.setLooping(true);
				mPlayer.start();
				while (mPlayer!=null && mPlayer.isPlaying()) {
					if (!mPlayerRepeat && mPlayer.getCurrentPosition()>=duration) break;
					else {
						try {
							mUiHandler.post(new Runnable(){
								@Override
								public void run() {
									if (!mSongPlayedPosIsTouched && mPlayer!=null) {
										int prog=(mPlayer.getCurrentPosition()*100)/duration;
										mSongPlayedPos.setProgress(prog);
									}
								}
							});
							synchronized(mTcPlayer) {
								mTcPlayer.wait(50);
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						if (!mTcPlayer.isEnabled()) {
							if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG, "startMusicPlayBack cancelled");
							mPlayer.stop();
//							mPlayer.reset();
							break;
						}
					}
				}
				mPlayer.stop();
				mUiHandler.post(new Runnable(){
					@Override
					public void run() {
						mMusicFilePlayBtn.setImageResource(R.drawable.player_play_enabled);
						mSongPlayedPos.setProgress(0);
						mSongPlayedPos.setEnabled(false);
					}
				});
				if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"startMusicPlayBack expired, current position="+mPlayer.getCurrentPosition());
			}
		}.start();
	};
	
	final public void stopMusicPlayBack(GlobalParameters mGlblParms) {
		if (mGlblParms.debugEnabled) Log.v(APPLICATION_TAG,"stopMusicPlayBack enterd");
		if (mPlayer!=null && mPlayer.isPlaying()) {//Stop play back
			synchronized(mTcPlayer) {
				mTcPlayer.setDisabled();
				mTcPlayer.notify();
			}
//			mPlayer.reset();
			while(mPlayer.isPlaying()) {
				synchronized(mPlayer) {
					try {
						mPlayer.wait(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	};

	
	final private void showBirdImage(int pos) {
		if (mGlblParms.debugEnabled) 
			Log.v(APPLICATION_TAG,"showBirdImage enterd, pos="+pos);
//		if (!mGlblParms.settingShowDescrptionByFullscreen) {
//			mArtworkView.setVisibility(RelativeLayout.VISIBLE);
//		} else {
//			mArtworkView.setVisibility(RelativeLayout.VISIBLE);
//			mDescriptionView.setVisibility(WebView.GONE);
//		}
		mIsDescriptionShowed=mDescriptionView.isShown();
		
		if (mIsDescriptionShowed && mGlblParms.settingShowDescrptionByFullscreen) {
			mMainActivity.setBackLightLevelToDefault();
		}
		
		MusicFileListItem fli=mGlblParms.viewedFileList.get(pos);
		String fp_current=fli.musicFilePath+"/"+fli.musicFileName;
//		long b_t=System.currentTimeMillis();
		AudioFile f_current = null;
		try {
			f_current = AudioFileIO.read(new File(fp_current));
		} catch (CannotReadException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TagException e) {
			e.printStackTrace();
		} catch (ReadOnlyFileException e) {
			e.printStackTrace();
		} catch (InvalidAudioFrameException e) {
			e.printStackTrace(); 
		}
		if (f_current!=null) {
			Tag tag_current = f_current.getTag();
//			AudioHeader ah= f.getAudioHeader();
//			Log.v("","tl="+f_current.getAudioHeader().getTrackLength());
//			Log.v("","ARTIST="+tag.getFirst(FieldKey.ARTIST));
//			Log.v("","ALBUM="+tag.getFirst(FieldKey.ALBUM));
//			Log.v("","TITLE="+tag.getFirst(FieldKey.TITLE));
//			Log.v("","COMMENT="+tag.getFirst(FieldKey.COMMENT));
//			Log.v("","YEAR="+tag.getFirst(FieldKey.YEAR));
//			Log.v("","TRACK="+tag.getFirst(FieldKey.TRACK));
//			Log.v("","DISC_NO="+tag.getFirst(FieldKey.DISC_NO));
//			Log.v("","COMPOSER="+tag.getFirst(FieldKey.COMPOSER));
//			Log.v("","ARTIST_SORT="+tag.getFirst(FieldKey.ARTIST_SORT));
			
			fli.musicFileTrackLength=f_current.getAudioHeader().getTrackLength();
			
			ArrayList<ArtWorkImageListItem> bd_list_artwork=new ArrayList<ArtWorkImageListItem>();
			if (!mGlblParms.settingDisplayArtworkOption.equals(GlobalParameters.DISPLAY_ARTWORK_OPTION_NOT_SHOW)) {
				List<Artwork> t_list=tag_current.getArtworkList();
				if (t_list!=null) {
					for (int i=0;i<t_list.size();i++) {
						ArtWorkImageListItem awil=new ArtWorkImageListItem();
						awil.art_work_image=t_list.get(i).getBinaryData();
						bd_list_artwork.add(awil);
					}
				}
			}
			
			ArrayList<ArtWorkImageListItem> bd_list_image_file=new ArrayList<ArtWorkImageListItem>();
			String ft="";
			int ft_pos=fli.musicFileName.lastIndexOf(".");
			ft=fli.musicFileName.substring(ft_pos+1);
			String m_name=fli.musicFileName.replace("."+ft, "");
			String cache_path=ArtWorkUtil.getCacheDir(mGlblParms)+"/"+
					fli.musicFolderName.replace("/", "_")+"_"+m_name;

			ArrayList<ImageByteArrayListItem>awbal=
					 ArtWorkUtil.loadArtWorkImageByteArrayFile(mGlblParms, cache_path);
			if (awbal!=null) {
				for (int i=0;i<awbal.size();i++) {
					ArtWorkImageListItem awil=new ArtWorkImageListItem();
					awil.file_type=awbal.get(i).file_type;
					awil.art_work_image=awbal.get(i).imageByteArray;
					awil.exif_image_info="";
					String image_size=String.format("サイズ %s x %s", awbal.get(i).exif_image_length, awbal.get(i).exif_image_width)+" ";
					
					String aperture=awbal.get(i).exif_image_aperture.equals("")?"":"F="+awbal.get(i).exif_image_aperture+" ";
					
					String exposure=awbal.get(i).exif_image_exposure.equals("")?"":"SS="+awbal.get(i).exif_image_exposure+" ";
					
					String focal_length=awbal.get(i).exif_image_focal_length.equals("")?"":"焦点距離="+awbal.get(i).exif_image_focal_length+" mm ";
					
					String iso=awbal.get(i).exif_image_iso.equals("")?"":String.format("ISO感度=%s", awbal.get(i).exif_image_iso);
					
					String date_time=awbal.get(i).exif_image_date_time.equals("")?"":"撮影日時 "+awbal.get(i).exif_image_date_time+" ";
					
					awil.exif_image_info=String.format("%s %s %s %s %s %s", image_size, 
							date_time, aperture, exposure, focal_length, iso);
					
//					exif_image_make
					
					
//					exif_image_model

					bd_list_image_file.add(awil);
				}
			}
			
			ArrayList<ArtWorkImageListItem> bd_list=new ArrayList<ArtWorkImageListItem>();
			if (mGlblParms.settingDisplayArtworkOption.equals(GlobalParameters.DISPLAY_ARTWORK_OPTION_SHOW_BEFORE_IMAGE)) {
				bd_list.addAll(bd_list_artwork);
				bd_list.addAll(bd_list_image_file);
			} else if (mGlblParms.settingDisplayArtworkOption.equals(GlobalParameters.DISPLAY_ARTWORK_OPTION_SHOW_AFTER_IMAGE)) {
				bd_list.addAll(bd_list_image_file);
				bd_list.addAll(bd_list_artwork);
			} else if (mGlblParms.settingDisplayArtworkOption.equals(GlobalParameters.DISPLAY_ARTWORK_OPTION_NOT_SHOW)) {
				bd_list.addAll(bd_list_image_file);
			}
				
			if (bd_list.size()==0) {
				Artwork aw=new StandardArtwork();
				ArtWorkImageListItem awil=new ArtWorkImageListItem();
				awil.art_work_image=aw.getBinaryData();
				bd_list.add(awil);
			}
			
			createArtworkImageAdapter(bd_list);

			loadDescription(fli);
		} else {
			mGlblParms.commonDlg.showCommonDialog(false, "E",  
					mContext.getString(R.string.msgs_main_filelist_and_folder_was_not_sync),"",null);
		}
	};

	final private void createArtworkImageAdapter(final ArrayList<ArtWorkImageListItem> bd_list) {
		if (mAdapterArtwork!=null) {
			mAdapterArtwork.cleanup();
			mAdapterArtwork=null;
		}
		mAdapterArtwork = new AdapterArtworkView(getActivity(),bd_list,mMainActivity.getResources().getDisplayMetrics(),mGlblParms);
		
		mCustomPagerView.setAdapter(mAdapterArtwork);
		mCustomPagerView.setOverScrollMode(ViewCompat.OVER_SCROLL_ALWAYS);
		setArtworkImageScrollIndicator(0);
		
		mArtworkImageInfo.setText(mAdapterArtwork.getImageViewArray()[0].exif_image_info);
//		if (bd_list.get(0)!=null) mArtworkImageInfo.setText(bd_list.get(0).exif_image_info);
//		else mArtworkImageInfo.setText("");;
		mCustomPagerView.setOnPageChangeListener(new OnPageChangeListener(){
			@Override
			public void onPageScrollStateChanged(int arg0) {
//				Log.v("","onPageScrollStateChanged ="+arg0);
			}
			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
//				Log.v("","onPageScrolled ="+arg0);
			}
			@Override
			public void onPageSelected(int page_no) {
//				Log.v("","onPageSelected ="+page_no);
				setArtworkImageScrollIndicator(page_no);
				mAdapterArtwork.resetZoom();
				
				mArtworkImageInfo.setText(mAdapterArtwork.getImageViewArray()[page_no].exif_image_info);
//				mAdapterArtwork.playGifAnimation(page_no);
//				for (int i=0;i<mCustomPagerView.getChildCount();i++) {
//					ExtendImageViewTouch pv=(ExtendImageViewTouch)mCustomPagerView.getChildAt(page_no);
//					if (pv!=null) {
//						pv.zoomTo(1.0f,1);
//						pv.setScaleX(1.0f);
//						pv.setScaleY(1.0f);
//					}
//				}
			}
		});
	};
	
	private void setArtworkImageScrollIndicator(final int page_no) {
		mArtworkImageNextBtn.setClickable(true);
		mArtworkImageNextBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (mArtworkImageNextBtn.isEnabled()) {
					mCustomPagerView.setCurrentItem(page_no+1);
				}
			}
		});
		mArtworkImagePrevBtn.setClickable(true);
		mArtworkImagePrevBtn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (mArtworkImagePrevBtn.isEnabled()) {
					mCustomPagerView.setCurrentItem(page_no-1);
				}
			}
		});
		int list_size=mAdapterArtwork.getCount();
		if (list_size==1) {
			mArtworkImagePrevBtn.setVisibility(ImageView.GONE);
			mArtworkImageNextBtn.setVisibility(ImageView.GONE);
		} else {
			mArtworkImagePrevBtn.setVisibility(ImageView.VISIBLE);
			mArtworkImageNextBtn.setVisibility(ImageView.VISIBLE);
			int n_pn=page_no+1;
			if (n_pn<list_size) {
				mArtworkImageNextBtn.setImageResource(R.drawable.next_image_enabled);
				mArtworkImageNextBtn.setEnabled(true);
			} else {
				mArtworkImageNextBtn.setImageResource(R.drawable.next_image_disabled);
				mArtworkImageNextBtn.setEnabled(false);
			}
			if (n_pn==1) {
				mArtworkImagePrevBtn.setImageResource(R.drawable.prev_image_disabled);
			} else {
				mArtworkImagePrevBtn.setImageResource(R.drawable.prev_image_enabled);
			}
		}
	};
	
	@SuppressWarnings("unused")
	private void executeTest() {
		final Handler hndl=new Handler();
		Thread th=new Thread() {
			@Override
			public void run() {
				boolean forever=true;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				while(forever) {
					if (mMusicFileNextBtn.isEnabled()) {
						while(mMusicFileNextBtn.isEnabled()) {
//							Log.v("","next enabled="+ib_image_next.isEnabled()+", shown="+ib_image_next.isShown());
							while(mArtworkImageNextBtn.isEnabled() && mArtworkImageNextBtn.isShown()) {
								hndl.post(new Runnable(){
									@Override
									public void run() {
										mCustomPagerView.setCurrentItem(mCustomPagerView.getCurrentItem()+1);
									}
								});
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							hndl.post(new Runnable(){
								@Override
								public void run() {
									mMusicFileNextBtn.performClick();
								}
							});
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}

					if (mMusicFilePrevBtn.isEnabled()) {
						while(mMusicFilePrevBtn.isEnabled()) {
							while(mArtworkImageNextBtn.isEnabled() && mArtworkImageNextBtn.isShown()) {
								hndl.post(new Runnable(){
									@Override
									public void run() {
										mCustomPagerView.setCurrentItem(mCustomPagerView.getCurrentItem()+1);
									}
								});
								try {
									Thread.sleep(500);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							hndl.post(new Runnable(){
								@Override
								public void run() {
									mMusicFilePrevBtn.performClick();
								}
							});
							try {
								Thread.sleep(1000);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		};
		th.start();
	};

	final public void setScreenBrightness() {
		if (mGlblParms.settingShowDescrptionByFullscreen) {
			if (mIsDescriptionShowed) {
				mMainActivity.setBackLightLevelToDefault();
			} else {
				if (mGlblParms.settingMaxBrightnessWhenImageShowed) 
					mMainActivity.setBackLightLevelToMax();
				else mMainActivity.setBackLightLevelToDefault();
			}
		} else {
			if (mGlblParms.settingMaxBrightnessWhenImageShowed) 
				mMainActivity.setBackLightLevelToMax();
			else mMainActivity.setBackLightLevelToDefault();
		}
	};
	
	final public void setDescriptionDisplayMode() {
//		Log.v("","d="+mIsDescriptionShowed+
//				", f="+mGlblParms.settingShowDescrptionByFullscreen);
		if (!mGlblParms.settingShowDescrptionByFullscreen) {
//			if (mGlblParms.settingMaxBrightnessWhenImageShowed) 
//				mMainActivity.setBackLightLevelToMax();
			mArtworkView.setVisibility(RelativeLayout.VISIBLE);
			if (mIsDescriptionShowed) {
				mDescriptionView.setVisibility(WebView.VISIBLE);
			} else {
				mDescriptionView.setVisibility(WebView.GONE);
			}
		} else {
			if (mIsDescriptionShowed) {
//				mMainActivity.setBackLightLevelToDefault();
				mArtworkView.setVisibility(RelativeLayout.GONE);
				mDescriptionView.setVisibility(WebView.VISIBLE);
			} else {
//				if (mGlblParms.settingMaxBrightnessWhenImageShowed) 
//					mMainActivity.setBackLightLevelToMax();
				mArtworkView.setVisibility(RelativeLayout.VISIBLE);
				mDescriptionView.setVisibility(WebView.GONE);
			}
		}
	};
	
	final public void setDescriptionTextSize() {
		mDescriptionView.getSettings().setTextZoom(mGlblParms.settingDescrptionTextSize);
	};
	
	final private void loadDescription(MusicFileListItem fli) {
//		final WebView desc_view=(WebView)mMainView.findViewById(R.id.player_view_bird_description);
		
		mDescriptionView.getSettings().setSupportZoom(true);
		mDescriptionView.setBackgroundColor(Color.LTGRAY);
		mDescriptionView.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET); 
		mDescriptionView.setVerticalScrollBarEnabled(true);
		mDescriptionView.setScrollbarFadingEnabled(false);
		mDescriptionView.getSettings().setDisplayZoomControls(true); 
		mDescriptionView.getSettings().setBuiltInZoomControls(true);
		setDescriptionTextSize();
//		mDescriptionView.getSettings().setUseWideViewPort(true);
//		mDescriptionView.getSettings().setLoadWithOverviewMode(true);
		if (fli.descriptionIsAvailable) {
//			mDescriptionView.loadDataWithBaseURL(null, fli.descriptionString, 
//					"text/html", "UTF-8", null);
			if (fli.descriptionFileName!=null)
				mDescriptionView.loadUrl("file://"+fli.descriptionFileName);
			mDescriptionShowBtn.setEnabled(true);
			mDescriptionShowBtn.setImageResource(R.drawable.bird_question_enabled);
		} else {
			mDescriptionShowBtn.setEnabled(false);
			mDescriptionShowBtn.setImageResource(R.drawable.bird_question_disabled);
		}
	};

	
	
}
