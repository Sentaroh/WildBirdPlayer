package com.sentaroh.android.WildBirdPlayer;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.view.PagerAdapter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

public class AdapterArtworkView extends PagerAdapter {

	final static private String APPLICATION_TAG="AdapterArtworkView";
	
	private GlobalParameters mGlblParms=null;
	
    private Context mContext;
    @SuppressWarnings("unused")
	private Activity mActivity;

    private ArrayList<ArtWorkImageListItem> mArtworkBinaryDataList;
    
	private DisplayMetrics mDisplayMetrics=null;
    
    private ArtWorkImageArrayItem[] mArtWorkImageArray=null;
    
    public AdapterArtworkView(Activity a, final ArrayList<ArtWorkImageListItem> al, 
    		final DisplayMetrics dm, 
    		GlobalParameters gp) {
        mContext = a.getApplicationContext();
        mActivity=a;
        mArtworkBinaryDataList = al;
        mDisplayMetrics=dm;
        mGlblParms=gp;
        mArtWorkImageArray=new ArtWorkImageArrayItem[al.size()];
        
        for (int i=0;i<al.size();i++) {
//        	Log.v("","i="+i+", t="+al.get(i).file_type);
        	mArtWorkImageArray[i]=new ArtWorkImageArrayItem();
    		mArtWorkImageArray[i].file_type=al.get(i).file_type;
        	mArtWorkImageArray[i].image_view=new CustomImageView(mContext);
        	mArtWorkImageArray[i].image_view.setDisplayType( CustomImageView.DisplayType.FIT_TO_SCREEN);
        	mArtWorkImageArray[i].exif_image_info=al.get(i).exif_image_info;
        }
    };

    final public ArtWorkImageArrayItem[] getImageViewArray() {
    	return mArtWorkImageArray;
    }
    
    @SuppressWarnings("deprecation")
	final private Bitmap convertBinaryToBitmap(byte[] data,  
    		DisplayMetrics disp_metrics) {
    	Bitmap bitmap=null;
		if (data!=null) {
			BitmapFactory.Options options = new BitmapFactory.Options();  
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, options);
			int d_h=0, d_w=0;
			boolean orientation_normal=false;
			if (disp_metrics.heightPixels>=disp_metrics.widthPixels) {
				d_h=disp_metrics.heightPixels;
				d_w=disp_metrics.widthPixels;
				orientation_normal=true;
			} else {
				orientation_normal=false;
				d_w=disp_metrics.heightPixels;
				d_h=disp_metrics.widthPixels;
			}
			int scale=0;
			int h_diff=options.outHeight-d_h;
			int w_diff=options.outWidth-d_w;
			if (h_diff>0 && w_diff>0) {
				if (h_diff>w_diff) {
					scale=options.outHeight/d_h+1;
				} else if (h_diff<w_diff) {
					scale=options.outWidth/d_w+1;
				} else {
					scale=1;
				}
			} else if (h_diff>0) {
				scale=options.outHeight/d_h+1;
			} else if (w_diff>0) {
				scale=options.outWidth/d_w+1;
			} else {
				scale=1;
			}

			options = new BitmapFactory.Options();  
			options.inPurgeable=true;
			options.inSampleSize=scale;
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
	    	if (mGlblParms.debugEnabled)  
	    		Log.v(APPLICATION_TAG,"Orientation normal="+orientation_normal+
	    			", Display h="+d_h+", w="+d_w+
	    			", bitmap h="+options.outHeight+", w="+options.outWidth+
	    			", scale="+scale+
	    			", data size="+data.length+
	    			", bitmap size="+bitmap.getByteCount());
		} else {
			bitmap = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.blank);
		}
		return bitmap;
    };

	@Override
	final public View instantiateItem(ViewGroup container, int position) {
    	if (mGlblParms.debugEnabled) 
    		Log.v(APPLICATION_TAG,"instantiateItem entered, pos="+position);
    	if (mArtWorkImageArray[position].image_bitmap==null) {
    		mArtWorkImageArray[position].image_bitmap=convertBinaryToBitmap(
    				mArtworkBinaryDataList.get(position).art_work_image, mDisplayMetrics);
    		mArtWorkImageArray[position].image_view.setImageBitmap(mArtWorkImageArray[position].image_bitmap);
    		mArtworkBinaryDataList.set(position,null);
    	}
		container.addView(mArtWorkImageArray[position].image_view, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        return mArtWorkImageArray[position].image_view;
	};

    @Override
    final public void destroyItem(ViewGroup container, int position, Object object) {
    	if (mGlblParms.debugEnabled) 
    		Log.v(APPLICATION_TAG,"destroyItem entered, pos="+position);
    	int before=container.getChildCount();
        container.removeView((View) object);
        int after=container.getChildCount();
        if (before==after) {
        	Log.v(APPLICATION_TAG,"destroyItem can not remove view pos="+position);
        }
        
    };

    @Override
    final public int getCount() {
        return mArtworkBinaryDataList.size();
    };

//    @Override
//    final public void finishUpdate (ViewGroup container){
//        super.finishUpdate(container);
//        Log.v("","finish");
//    };

    @Override
    final public boolean isViewFromObject(View view, Object object) {
        return view.equals(object);
    };
    
    final public void resetZoom() {
    	for (int i=0;i<mArtworkBinaryDataList.size();i++) {
    		if (mArtWorkImageArray[i].image_view!=null) mArtWorkImageArray[i].image_view.zoomTo(1.0f, 1);
    	}
    };
    
    final public void cleanup() {
    	if (mGlblParms.debugEnabled) 
    		Log.v(APPLICATION_TAG,"cleanup entered");
        for (int i=0;i<mArtworkBinaryDataList.size();i++) {
        	if (mArtWorkImageArray[i].image_bitmap!=null) mArtWorkImageArray[i].image_bitmap.recycle();
        	if (mArtWorkImageArray[i].image_view!=null) mArtWorkImageArray[i].image_view.setImageBitmap(null);
        	
        	mArtWorkImageArray[i].image_view=null;
        	mArtWorkImageArray[i].image_bitmap=null;
        	mArtWorkImageArray[i]=null;
        }
    };
}

class ArtWorkImageListItem {
	public String file_type="";
	public byte[] art_work_image=null;
	
	public String exif_image_info="";

}

class ArtWorkImageArrayItem {
	public String file_type="";
	public CustomImageView image_view=null;
	public Bitmap image_bitmap=null;
	public String exif_image_info="";
}
