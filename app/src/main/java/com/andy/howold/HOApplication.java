package com.andy.howold;

import android.app.Application;

import com.andy.utils.toastMgr;

public class HOApplication extends Application
{
	@Override
	public void onCreate()
	{
		// TODO Auto-generated method stub
		super.onCreate();
		toastMgr.builder.init(getApplicationContext());
	}
}
