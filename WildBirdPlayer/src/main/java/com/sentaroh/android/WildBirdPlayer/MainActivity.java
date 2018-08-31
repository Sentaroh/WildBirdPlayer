package com.sentaroh.android.WildBirdPlayer;

import static com.sentaroh.android.WildBirdPlayer.Constants.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.SerializeUtil;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.ProgressSpinDialogFragment;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

	public GlobalParameters mGp=null;
	
	private boolean mTerminateApplication=false;
	private int mRestartStatus=0;

	private FragmentManager mFragmentManager=null;
	private Context mContext;
	private float mDefaultBackLightLevel=0;
	
	@Override
	final public void onConfigurationChanged(final Configuration newConfig) {
	    // Ignore orientation change to keep activity from restarting
	    super.onConfigurationChanged(newConfig);
	    if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onConfigurationChanged Entered");
	    
	    
//		Bundle sb=new Bundle();
//		saveViewContents(sb);
//		
//		initViewWidget();
//		
//		restoreViewContents(sb);

	    refreshOptionMenu();
	};

	@Override  
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onSaveInstanceState entered");
		saveViewContents(outState);
	};  

	private void saveViewContents(Bundle outState) {
	};
	
	@Override  
	protected void onRestoreInstanceState(Bundle savedState) {  
		super.onRestoreInstanceState(savedState);
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onRestoreInstanceState entered");
		restoreViewContents(savedState);
		mRestartStatus=2;
	};

	private void restoreViewContents(Bundle savedState) {
	};
	
	private void initViewWidget() {
        getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        setContentView(R.layout.main_activity);
	};

	private UncaughtExceptionHandler defaultUEH=null;
	// handler listener
    @SuppressLint("SimpleDateFormat")
	private Thread.UncaughtExceptionHandler unCaughtExceptionHandler =
        new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
            	Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);
            	if (mGp.debugEnabled) {
                	ex.printStackTrace();
                	StackTraceElement[] st=ex.getStackTrace();
                	String st_msg="";
                	final SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                	st_msg="unCaughtExceptionHandler entered, "+sdfDate.format(System.currentTimeMillis())+"\n";
//                	if (ex.getCause()!=null) st_msg+=ex.getCause();
//                	if (ex.getLocalizedMessage()!=null) st_msg+=ex.getLocalizedMessage();
                	st_msg+=ex.getMessage();
                	for (int i=0;i<st.length;i++) {
                		st_msg+="\n at "+st[i].getClassName()+"."+
                				st[i].getMethodName()+"("+st[i].getFileName()+
                				":"+st[i].getLineNumber()+")";
                	}
                	st_msg+="\n\n";
                	String fd=Environment.getExternalStorageDirectory().toString()+
                			"/WildBirdPlayer";
                	File ld=new File(fd);
                	if (!ld.exists()) ld.mkdirs();
                	String fp=fd+"/exception_log.txt";
                	boolean append=true;
                	File lf=new File(fp);
                	if (lf.exists() && 
                		((System.currentTimeMillis()-lf.lastModified()>(1000*60*60*24*2))))
                		append=false; 
                	try {
                		FileWriter fw=new FileWriter(lf,append);
    					PrintWriter pw=new PrintWriter(fw);
    					pw.println(st_msg);
    					pw.close();
    				} catch (FileNotFoundException e) {
    					e.printStackTrace();
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
            	}
                // re-throw critical exception further to the os (important)
                defaultUEH.uncaughtException(thread, ex);
            }
    };

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this;
        mFragmentManager=getSupportFragmentManager();
        mRestartStatus=0;
       	mGp=GlobalWorkArea.getGlobalParameters(mContext);
    	mGp.mainResources=getResources();
    	mGp.appContext=this.getApplicationContext();
        
        if (defaultUEH==null) {
    		defaultUEH = Thread.currentThread().getUncaughtExceptionHandler();
            Thread.currentThread().setUncaughtExceptionHandler(unCaughtExceptionHandler);
        }

        mGp.initSettingParms(mContext);
        mGp.loadSettingParms(mContext);

        if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onCreate entered");
		
        mActivityWindow=getWindow();
        
        mGp.commonDlg=new CommonDialog(mContext, mFragmentManager);

        initViewWidget();
        
        mDefaultBackLightLevel=getWindow().getAttributes().screenBrightness;
        
//		Thread th=new Thread(){
//			@Override
//			public void run() {
//				ArtWorkUtil.listExifInfo("/sdcard/000.jpg");
//				ArtWorkUtil.listExifInfo("/sdcard/001.jpg");
//				ArtWorkUtil.listExifInfo("/sdcard/002.png");
//			}
//		};
//		th.start();
		
//		ArtWorkUtil.extractArtWorkImageFile("/mnt/sdcard/fh");

	};
    
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onNewIntent entered, restartStatus="+mRestartStatus);
		if (mRestartStatus==2) return;
//    	String fp="";
//		if (intent!=null) if (intent.getDataString()!=null) fp=intent.getDataString().replace("file://", "");
//		Log.v("","onNewIntent fp="+fp);
//		if (fp.equals("")) {
//		} else {
//		}
	};
	
	@Override
	public void onStart() {
		super.onStart();
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onStart entered");
	};

	@Override
	public void onRestart() {
		super.onStart();
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onRestart entered");
	};

	@Override
	public void onResume() {
		super.onResume();
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onResume entered, restartStatus="+mRestartStatus);

		setBackLightLevelToDefault();
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c, Object[] o) {
				if (mRestartStatus==0) {
					startPlayerFragment();
				} else if (mRestartStatus==1) {
				} else if (mRestartStatus==2) {
					FragmentTransaction ft=mFragmentManager.beginTransaction();
					if (mGp.fragmentPlayer!=null) ft.remove(mGp.fragmentPlayer);
					ft.setTransition(FragmentTransaction.TRANSIT_NONE);
					ft.commitAllowingStateLoss();
					startPlayerFragment();
				}
				mRestartStatus=1;
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
			
		});
		if (mRestartStatus==1) ntfy.notifyToListener(true, null);
		else buildFileList(ntfy);
	};

	private Window mActivityWindow=null;
	
	public void setBackLightLevelToMax() {
//		Thread.currentThread().dumpStack();
		LayoutParams lp = new LayoutParams(); 
		lp=mActivityWindow.getAttributes();
		lp.screenBrightness = 1.0f;
		mActivityWindow.setAttributes(lp); 
	};
	
	public void setBackLightLevelToDefault() {
//		Thread.currentThread().dumpStack();
		LayoutParams lp = new LayoutParams(); 
		lp=mActivityWindow.getAttributes();
		lp.screenBrightness = mDefaultBackLightLevel;
		mActivityWindow.setAttributes(lp); 
	};
	 
	public void startPlayerFragment() {
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"startPlayerFragment entered, restartStatus="+mRestartStatus);
		if (mGp.masterFileList.size()>0) {
			mGp.fragmentPlayer=FragmentPlayer.newInstance();
			FragmentTransaction ft=mFragmentManager.beginTransaction();
			ft.setTransition(FragmentTransaction.TRANSIT_NONE);
			ft.replace(R.id.main_player_fragment, mGp.fragmentPlayer);
//			ft.commit();
			ft.commitAllowingStateLoss();
		} else {
			mGp.commonDlg.showCommonDialog(false, "W",
					mContext.getString(R.string.msgs_main_no_music_file), "", null);
		}
	};

	@Override
	public void onPause() {
		super.onPause();
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onPause entered");
        // Application process is follow
	};

	@Override
	public void onStop() {
		super.onStop();
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onStop entered");
        // Application process is follow
	};

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onDestroy entered");
        // Application process is follow

		if (mTerminateApplication) {
//			deleteTaskData();
			if (mGp.settingExitCleanly) {
				Handler hndl=new Handler();
				hndl.postDelayed(new Runnable(){
					@Override
					public void run() {
						android.os.Process.killProcess(android.os.Process.myPid());
					}
				}, 200);
			} else {
				mGp.commonDlg=null;
				mFragmentManager=null;
				mGp.fragmentPlayer=null;
				mGp.masterFileList=null;
				mGp.masterFolderList=null;
				mGp.viewedFileList=null;
				mContext=null;
		    	mGp=null;
				System.gc();
			}
		} else {
			
		}
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_BACK:
			if (mGp.fragmentPlayer!=null && mGp.fragmentPlayer.processBackKey()) {
				return true;
			} else {
				confirmExit();
				return true;
			}
		default:
			return super.onKeyDown(keyCode, event);
		}
	};
	
	public void refreshOptionMenu() {
		invalidateOptionsMenu();
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onCreateOptionsMenu Entered");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	};
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onPrepareOptionsMenu Entered");
        super.onPrepareOptionsMenu(menu);
        if (!mIsReloadFileListFinished) {
        	menu.findItem(R.id.action_refresh).setVisible(false);
        } else {
        	menu.findItem(R.id.action_refresh).setVisible(true);
        }
        return true;
	};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) { 
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"onOptionsItemSelected Entered");
		if (item.getItemId()==R.id.action_settings) invokeSettings();
		else if (item.getItemId()==R.id.action_refresh) reloadFilelist();
		else if (item.getItemId()==R.id.action_clear_cache) reCreateCacheFile();
		else if (item.getItemId()==R.id.action_about) about();
		else if (item.getItemId()==R.id.action_uninstall) uninstallApplication();
		else if (item.getItemId()==R.id.action_exit) confirmExit();
		return false;
	};
	
	private void uninstallApplication() {
		Uri uri=Uri.fromParts("package",getPackageName(),null);
		Intent intent=new Intent(Intent.ACTION_DELETE,uri);
		startActivity(intent);
	};
	
//	private boolean isApplicationTerminating() {return mTerminateApplication;}
//	
	private void confirmExit() {
//		NotifyEvent ntfy=new NotifyEvent(mContext);
//		ntfy.setListener(new NotifyEventListener(){
//			@Override
//			public void positiveResponse(Context c, Object[] o) {
//				mTerminateApplication=true;
//				finish();
//			}
//			@Override
//			public void negativeResponse(Context c, Object[] o) {
//			}
//		});
//		mGp.commonDlg.showCommonDialog(true, "W",
//				mContext.getString(R.string.msgs_main_confirm_termination), "", ntfy);
		mTerminateApplication=true;
		finish();
	};

	
	private void about() {
		// common カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.about_dialog);
		((TextView)dialog.findViewById(R.id.about_dialog_title)).setText(
			getString(R.string.msgs_about_wild_bird_player)+" Ver "+getApplVersionName());
		final WebView func_view=(WebView)dialog.findViewById(R.id.about_dialog_function);
//	    func_view.setWebViewClient(new WebViewClient());
//	    func_view.getSettings().setJavaScriptEnabled(true); 
		func_view.getSettings().setSupportZoom(true);
//		func_view.setVerticalScrollbarOverlay(true);
		func_view.setBackgroundColor(Color.LTGRAY);
//		func_view.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
		func_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_INSET); 
		func_view.setVerticalScrollBarEnabled(true);
		func_view.setScrollbarFadingEnabled(false);
		if (Build.VERSION.SDK_INT>10) {
			func_view.getSettings().setDisplayZoomControls(true); 
			func_view.getSettings().setBuiltInZoomControls(true);
		} else {
			func_view.getSettings().setBuiltInZoomControls(true);
		}
		func_view.loadUrl("file:///android_asset/"+
				getString(R.string.msgs_about_dlg_func_html));

		func_view.getSettings().setTextZoom(120);
		func_view.getSettings().setDisplayZoomControls(true);
		func_view.getSettings().setBuiltInZoomControls(true);
		
		final Button btnOk = (Button) dialog.findViewById(R.id.about_dialog_btn_ok);
		
		func_view.setVisibility(TextView.VISIBLE);
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		
		// OKボタンの指定
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnOk.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
				
	};

	private String getApplVersionName() {
		try {
		    String packegeName = getPackageName();
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	};

	private void invokeSettings() {
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"invokeSettings Entered");
		Intent intent = new Intent(this,SettingActivity.class);
		startActivityForResult(intent,0);
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"Return from settings");
		if (requestCode==0) applySettingParms();
	};
	
	private void reCreateCacheFile() {
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
//				mGp.masterFileList.clear();
//				mGp.masterFileList=null;
				deleteSavedFileList(mGp);
				ArtWorkUtil.deleteAllImageByteArray(mGp);
				
				finish();
				Intent i = getBaseContext().getPackageManager()
						 .getLaunchIntentForPackage(getBaseContext().getPackageName() );
						 
				i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK );
				startActivity(i);
//				reloadFilelist();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		mGp.commonDlg.showCommonDialog(true, "W",
				mContext.getString(R.string.msgs_main_confirm_recreate_cache_file), "", ntfy);
	};

	private boolean mIsReloadFileListFinished=true;
	private void reloadFilelist() {
		mIsReloadFileListFinished=false;
		invalidateOptionsMenu();
		if (mGp.fragmentPlayer!=null) mGp.fragmentPlayer.savePlayerSettings();
		
		final Handler hndl=new Handler();
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				hndl.post(new Runnable(){
					@Override
					public void run() {
						if (mGp.fragmentPlayer!=null) {
							mFragmentManager.beginTransaction()
							.remove(mGp.fragmentPlayer)
							.commitAllowingStateLoss();
//							.commit();
							mGp.fragmentPlayer=null;
						}

						mGp.playerPositionResumedWhenReload=true;
						startPlayerFragment();
						mIsReloadFileListFinished=true;
						invalidateOptionsMenu();
					}
				});
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		buildFileList(ntfy);
	};
	
	private void applySettingParms() {
		String p_sf=mGp.settingScanFolder;
		String p_awo=mGp.settingDisplayArtworkOption;
		mGp.loadSettingParms(mContext);
		if (mGp.fragmentPlayer!=null) {
			mGp.fragmentPlayer.setDescriptionTextSize();
			mGp.fragmentPlayer.setDescriptionDisplayMode();
		}
		if (!p_sf.equals(mGp.settingScanFolder)) reloadFilelist();
		else {
			if (!p_awo.equals(mGp.settingDisplayArtworkOption))
				mGp.fragmentPlayer.reshowBirdImage();
		}
	};

	private void buildFileList(final NotifyEvent p_ntfy) {
		final ThreadCtrl tc=new ThreadCtrl();
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				if (mGp.debugEnabled) 
					Log.v(APPLICATION_TAG,"buildFileList was cancelled");
				tc.setDisabled();
			}
		});
		final ProgressSpinDialogFragment pdf =ProgressSpinDialogFragment.newInstance(
				getString(R.string.msgs_main_building_file_list_title),
				getString(R.string.msgs_main_building_file_list_msg_main),
				getString(R.string.msgs_main_building_file_list_caninit),
				getString(R.string.msgs_main_building_file_list_canpressed));
//		pdf.setCancelable(false);
		pdf.showDialog(getSupportFragmentManager(),pdf,ntfy,false);

		Thread th=new Thread() {
			@Override
			public void run() {
				@SuppressWarnings("deprecation")
				WakeLock wl=((PowerManager)getSystemService(Context.POWER_SERVICE))
		    			.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
		    				| PowerManager.ACQUIRE_CAUSES_WAKEUP
//		   	    				| PowerManager.ON_AFTER_RELEASE
		    				, "WildBirdPlayer-ScreenOn");
				try {
					wl.acquire();

					String path=mGp.settingScanFolder;
					String t_path="", base_path="";
					if (path.endsWith("/")) t_path=path.substring(0,path.length()-1);
					else t_path=path;
					
					if (t_path.lastIndexOf("/")>=0) base_path=path.substring(0,t_path.lastIndexOf("/")+1);
					else base_path=t_path;
//					Log.v("","path="+path+", t_path="+t_path+", base="+base_path);
					
					ArrayList<MusicFileListItem> masterFileList=new ArrayList<MusicFileListItem>();
					ArrayList<MusicFileListItem> newFileList=new ArrayList<MusicFileListItem>();
					ArrayList<MusicFileListItem> viewedFileList=new ArrayList<MusicFileListItem>();
					ArrayList<FolderListItem> masterFolderList=new ArrayList<FolderListItem>();
//					ArrayList<ImageFileListItem> imageFileList=new ArrayList<ImageFileListItem>();
//					long begin=System.currentTimeMillis();
					if (mGp.masterFileList==null) loadSavedFileList(mGp, masterFileList);
					else {
						masterFileList.addAll(mGp.masterFileList);
						for (int i=0;i<masterFileList.size();i++) {
							masterFileList.get(i).fileListItemUpdated=false;
							masterFileList.get(i).fileListItemRefered=false;
						}
					}
					
					mByteBuf=new byte[DESCRIPTION_FILE_MAX_SIZE];
					readFileList(mGp, masterFileList, newFileList, path, base_path);
					mByteBuf=null;
					
					int m_sz=masterFileList.size();
					boolean file_list_save_required=false;
					for (int i=m_sz-1;i>=0;i--) {
						if (!masterFileList.get(i).fileListItemRefered) {
							masterFileList.remove(i);
							file_list_save_required=true;
						}
					}
					masterFileList.addAll(newFileList);

					Collections.sort(masterFileList, new Comparator<MusicFileListItem>(){
						@Override
						public int compare(MusicFileListItem lhs, MusicFileListItem rhs) {
							if (!lhs.musicFolderName.equals(rhs.musicFolderName)) return lhs.musicFolderName.compareToIgnoreCase(rhs.musicFolderName);
							else if (!lhs.musicFileName.equals(rhs.musicFileName)) return lhs.musicFileName.compareToIgnoreCase(rhs.musicFileName);
							return 0;
						}
					});
					
					buildFolderList(masterFileList, masterFolderList);

					ArtWorkUtil.UpdateImageFileInfo(mGp, tc, pdf, 
							masterFileList, path);

					mGp.masterFileList=masterFileList;
					mGp.viewedFileList=viewedFileList;
					mGp.masterFolderList=masterFolderList;
//					createThumbnail(mGp.masterFileList);
					
					for (int i=0;i<mGp.masterFileList.size();i++)
						if (mGp.masterFileList.get(i).fileListItemUpdated) {
							file_list_save_required=true;
							break;
						}

					if (file_list_save_required) saveFileList(mGp, mGp.masterFileList);
//					Log.v("","save elapsed="+(System.currentTimeMillis()-begin));
//					begin=System.currentTimeMillis();

//					pdf.dismiss();
					pdf.dismissAllowingStateLoss();
					p_ntfy.notifyToListener(true, null);
					if (tc.isEnabled()) {
						if (mGp.debugEnabled) 
							Log.v(APPLICATION_TAG,"buildFileList was completed");
					}
				} finally {
					wl.release();
				}
			}
		};
		th.start();
	};

	private void buildFolderList(ArrayList<MusicFileListItem> fileList, ArrayList<FolderListItem> folderList) {
		String folder_name="";
		for (int i=0;i<fileList.size();i++) {
			if (!fileList.get(i).musicFolderName.equals(folder_name)) {
				FolderListItem flci=new FolderListItem();
				flci.folderName=fileList.get(i).musicFolderName;
				folder_name=fileList.get(i).musicFolderName;
				folderList.add(flci);
			}
		}
		
//		for (int i=0;i<folderList.size();i++) {
//			Log.v(APPLICATION_TAG,"index="+i+", folder="+folderList.get(i).folderName);
//		}
		
		if (folderList.size()<1) {
			FolderListItem flci=new FolderListItem();
			flci.folderName="*** Folder not available ***";
			folderList.add(flci);
		}
	}
	
	@SuppressWarnings("unused")
	private static byte[] mByteBuf=null;
	@SuppressLint("DefaultLocale")
	final private void readFileList(GlobalParameters mGp,ArrayList<MusicFileListItem> masterFileList,ArrayList<MusicFileListItem> newFileList,
			String path, String basePath) {
		File lf=new File(path);
		if (lf.exists()) {
			if (lf.isDirectory()) {
				File[] fa=lf.listFiles();
				if (fa!=null) {
					for (int i=0;i<fa.length;i++) {
						if (fa[i].isDirectory()) {
							readFileList(mGp,
									masterFileList, newFileList,  
									(path+"/"+fa[i].getName()).replaceAll("//", "/"),
									basePath);
						} else {
							String folderName=fa[i].getParent().replace(basePath, "");
							String fileName=fa[i].getName();
							String ft="";
							int ft_pos=fileName.lastIndexOf(".");
							if (ft_pos>0) {
								ft=fileName.substring(ft_pos+1);
								String mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ft.toLowerCase());
//								Log.v("","mt1="+mt+",type="+ft);
								if (mt!=null && mt.startsWith(ELIGIBLE_FILE_MIME_TYPE)) {
									MusicFileListItem c_flci=
										getMasterFileList(mGp,masterFileList,folderName,fileName);
									
									String description_file_name=fileName.replace("."+ft, "")+".htm";
									String desc_fp=(path+"/"+description_file_name).replaceAll("//", "/");
									
//									Log.v("","desc fn="+description_file_name+",type="+ft);
									
									if (c_flci!=null) {
										c_flci.fileListItemRefered=true;
										loadBirdDescription(desc_fp, c_flci);
									} else {
										MusicFileListItem new_flci=new MusicFileListItem();
										new_flci.musicFileLastModified=fa[i].lastModified();
										new_flci.musicFileSize=fa[i].length();
										new_flci.musicFolderName=folderName;
										new_flci.musicFileName=fileName;
										
										String mfp=path.replaceAll("//", "/");
//										Log.v("","mfp="+mfp);
										if (mfp.endsWith("/")) new_flci.musicFilePath=mfp.substring(0,mfp.length()-1); 
										else new_flci.musicFilePath=mfp;
										
										new_flci.fileListItemUpdated=true;
										loadBirdDescription(desc_fp, new_flci);
										newFileList.add(new_flci);
									}
								}
							}
						}
					}
				} else {
					//Nothing
				}
			} else {
				String fileName=lf.getName();
				String ft="",description_file_name="";
				int ft_pos=fileName.lastIndexOf(".");
				ft=fileName.substring(ft_pos+1).toLowerCase();
				description_file_name=fileName.replace("."+ft, "")+".htm";
				String desc_fp=(path+"/"+description_file_name).replaceAll("//", "/");
				String mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(ft.toLowerCase());
//				Log.v("","mt1="+mt+",type="+ft);
				if (mt!=null && mt.startsWith(ELIGIBLE_FILE_MIME_TYPE)) {
					MusicFileListItem flci=new MusicFileListItem();
					flci.musicFolderName=lf.getParent().replace(basePath, "");
					flci.musicFileLastModified=lf.lastModified();
					flci.musicFileName=lf.getName();
					
					String mfp=path.replaceAll("//", "/");
//					Log.v("","mfp2="+mfp);
					if (mfp.endsWith("/")) flci.musicFilePath=mfp.substring(0,mfp.length()-1); 
					else flci.musicFilePath=mfp;
					
					loadBirdDescription(desc_fp, flci);
					masterFileList.add(flci);
				}
			}
		} else {
			//Nothing
		}
	};
	
	static private void loadBirdDescription(String fp, MusicFileListItem new_flci) {
		File desc_fl=new File(fp);
		Log.v("","desc fp="+fp);
		if (desc_fl.exists()) {
			new_flci.descriptionIsAvailable=true;
			if (new_flci.descriptionFileLastModified!=desc_fl.lastModified() || 
					new_flci.descriptionFileSize!=desc_fl.length()	) {
//					FileInputStream is = new FileInputStream(desc_fl);
//					int rc=is.read(mByteBuf);
//					Log.v("","fp="+desc_fl.getName()+", rc="+mByteBufReadCnt);
//					new_flci.descriptionString=new String(mByteBuf,0,rc,"UTF-8");
				new_flci.descriptionFileName=fp;
//					is.close();
//					is=null;
				new_flci.descriptionFileLastModified=desc_fl.lastModified();
				new_flci.descriptionFileSize=desc_fl.length();
				new_flci.fileListItemUpdated=true;
			} 
		} else {
			if (new_flci.descriptionIsAvailable) new_flci.fileListItemUpdated=true;
			new_flci.descriptionIsAvailable=false;
//			new_flci.descriptionString=null;
			new_flci.descriptionFileName=null;
			new_flci.descriptionFileLastModified=0;
			new_flci.descriptionFileSize=0;
		}

	}
	
	static private void deleteSavedFileList(GlobalParameters mGp) {
		File lf=new File(mGp.wildBirdPlayerHomeDir+"/file_list_cache");
		lf.delete();
	};
	
	@SuppressWarnings("unchecked")
	static private void loadSavedFileList(GlobalParameters mGp, ArrayList<MusicFileListItem> mfl) {
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"loadSavedFileList entered");
		long bt=System.currentTimeMillis();
		try {
			File lf=new File(mGp.wildBirdPlayerHomeDir+"/file_list_cache");
			FileInputStream fis=new FileInputStream(lf);
			BufferedInputStream bis=new BufferedInputStream(fis,FILE_LIST_BUFFER_SIZE);
			ZipInputStream zis=new ZipInputStream(bis);
			zis.getNextEntry();
			ObjectInputStream ois=new ObjectInputStream(zis);
			long sid=ois.readLong();
			if (sid==CACHE_LIST_SID) {
				ArrayList<MusicFileListItem> tfl=(ArrayList<MusicFileListItem>) SerializeUtil.readArrayList(ois);
				mfl.clear();
				mfl.addAll(tfl);
			} else {
				if (mGp.debugEnabled) 
					Log.v(APPLICATION_TAG,"Saved SID is not matched. Saved SID="+sid+", expected SID="+CACHE_LIST_SID);
			}
//			for(int i=0;i<mfl.size();i++) {
//				if (mfl.get(i).artWorkList!=null) {
//					Log.v("","name="+mfl.get(i).musicFileName+", size="+mfl.get(i).artWorkList.size());
//				} else {
//					Log.v("","name="+mfl.get(i).musicFileName+", size="+mfl.get(i).artWorkList);
//				}
//			}
			
//			int fl_cnt=ois.readInt();
//			for (int i=0;i<fl_cnt;i++) {
//				FileListItem fli=new FileListItem();
//				fli.readExternal(ois);
//				mfl.add(fli);
//			}
			ois.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"loadSavedFileList exited, elapsed="+(System.currentTimeMillis()-bt));
	};

	private static final long CACHE_LIST_SID=3L;
	static private void saveFileList(final GlobalParameters mGp, 
			final ArrayList<MusicFileListItem> mfl) {
		Thread th=new Thread() {
			@Override
			public void run() {
				if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"saveFileList entered");		
				long bt=System.currentTimeMillis();
				try {
					File lfd=new File(mGp.wildBirdPlayerHomeDir+"/");
					lfd.mkdirs();
					File lf_tmp=new File(mGp.wildBirdPlayerHomeDir+"/file_list_cache.tmp");
					File lf_save=new File(mGp.wildBirdPlayerHomeDir+"/file_list_cache");
					FileOutputStream fos=new FileOutputStream(lf_tmp,false);
					BufferedOutputStream bos=new BufferedOutputStream(fos,FILE_LIST_BUFFER_SIZE);
					ZipOutputStream zos=new ZipOutputStream(bos);
					ZipEntry ze = new ZipEntry("list");
					zos.putNextEntry(ze);
					ObjectOutputStream oos=new ObjectOutputStream(zos);
					oos.writeLong(CACHE_LIST_SID);
					SerializeUtil.writeArrayList(oos,mfl);
					
//					oos.writeInt(mfl.size());
//					for (int i=0;i<mfl.size();i++) {
//						mfl.get(i).writeExternal(oos);
//					}
					oos.close();
					lf_save.delete();
					lf_tmp.renameTo(lf_save);
					
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (mGp.debugEnabled) Log.v(APPLICATION_TAG,"saveFileList exited, elapsed="+(System.currentTimeMillis()-bt));
			}
		};
		th.setPriority(Thread.MIN_PRIORITY);
		th.start();
	};

//	static private void createThumbnail(GlobalParameters mGp, ArrayList<FileListItem> mfl) {
//		DisplayMetrics dm=mGp.mainResources.getDisplayMetrics();
//		for (int i=0;i<mfl.size();i++) {
//			FileListItem fli=mfl.get(i);
//			File lf=new File(fli.filePath+"/"+fli.fileName);
//			
//			AudioFile f = null;
//			try {
//				f = AudioFileIO.read(lf);
//			} catch (CannotReadException e) {
//				e.printStackTrace();
//			} catch (IOException e) {
//				e.printStackTrace();
//			} catch (TagException e) {
//				e.printStackTrace();
//			} catch (ReadOnlyFileException e) {
//				e.printStackTrace();
//			} catch (InvalidAudioFrameException e) {
//				e.printStackTrace();
//			}
//			
//			List<Artwork> al=f.getTag().getArtworkList();
//			if (al!=null && al.size()!=0) {
//				fli.thumbnaiBitmap = createThumbnaiBitmap(mGp,
//						al.get(0).getBinaryData(),dm);
//			} else {
//				Drawable dw=mGp.mainResources.getDrawable(R.drawable.blank);
//				Bitmap c_bm = ((BitmapDrawable)dw).getBitmap();
//				fli.thumbnaiBitmap=Bitmap.createScaledBitmap(c_bm, 64, 64, false);
//				c_bm.recycle();
//			}
//		}
//	};
//	
//	static private Bitmap createThumbnaiBitmap(GlobalParameters mGp, 
//			byte[] data, DisplayMetrics dm) {
//		BitmapFactory.Options options_sz = new BitmapFactory.Options();  
//		options_sz.inJustDecodeBounds = true;
//		BitmapFactory.decodeByteArray(data, 0, data.length, options_sz);
//		int d_h=dm.heightPixels;
//		int d_w=dm.widthPixels;
//		int scale=0;
//		int h_diff=options_sz.outHeight-d_h;
//		int w_diff=options_sz.outWidth-d_w;
//		if (h_diff>0 && w_diff>0) {
//			if (h_diff>w_diff) {
//				scale=options_sz.outHeight/d_h+1;
//			} else if (h_diff<w_diff) {
//				scale=options_sz.outWidth/d_w+1;
//			} else {
//				scale=1;
//			}
//		} else if (h_diff>0) {
//			scale=options_sz.outHeight/d_h+1;
//		} else if (w_diff>0) {
//			scale=options_sz.outWidth/d_w+1;
//		} else {
//			scale=1;
//		}
//		
//		BitmapFactory.Options options = new BitmapFactory.Options();  
//		options.inPurgeable=true;
//		options.inSampleSize=scale;
//		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
//		Bitmap th_bm=ThumbnailUtils.extractThumbnail(bitmap, 64, 64);
//		bitmap.recycle();
//
//		return th_bm;
//	};
	
	static private MusicFileListItem getMasterFileList(GlobalParameters mGp,
			ArrayList<MusicFileListItem> masterFileList,
			final String folderName, final String fileName) {
		MusicFileListItem flci=null;
		int idx=Collections.binarySearch(masterFileList, null, new Comparator<MusicFileListItem>(){
			@Override
			public int compare(MusicFileListItem arg0, MusicFileListItem arg1) {
				if (!arg0.musicFolderName.equals(folderName)) return arg0.musicFolderName.compareToIgnoreCase(folderName); 
				else if (!arg0.musicFileName.equals(fileName)) return arg0.musicFileName.compareToIgnoreCase(fileName);
				return 0;
			}
		});
		if (idx>=0) flci=masterFileList.get(idx);
//		Log.v("","f="+folderName+", n="+fileName+", result="+flci);
		return flci;
	};
	
}