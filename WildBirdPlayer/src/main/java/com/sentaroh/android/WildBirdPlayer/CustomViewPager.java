package com.sentaroh.android.WildBirdPlayer;

import com.sentaroh.android.Utilities.Widget.OverScrollEffectViewPager;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.View;

class CustomViewPager extends OverScrollEffectViewPager {

	public CustomViewPager(Context context, AttributeSet attrs) 
			throws Exception {
		super(context, attrs);
		
		setPageTransformer(false, new ViewPager.PageTransformer() {
		    @Override
		    public void transformPage(View page, float position) {
		    	final float normalizedposition = Math.abs(Math.abs(position) - 1);

//		    	page.setAlpha(normalizedposition);
		        
		        page.setScaleX(normalizedposition / 2 + 0.5f);
		        page.setScaleY(normalizedposition / 2 + 0.5f);
		        
//		    	page.setRotationY(position * -30);
		    } 
		});

	}
}
