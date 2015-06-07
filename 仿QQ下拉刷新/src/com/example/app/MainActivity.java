package com.example.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnRefreshListener{

	private ScrollView alv;
	private PullToRefreshLayout refreshLayout;
	private View loading;
	private RotateAnimation loadingAnimation;
	private TextView loadTextView;
	private boolean isLoading = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
	}
	
	private void init()
	{
		alv = (ScrollView) findViewById(R.id.content_view);
		refreshLayout = (PullToRefreshLayout) findViewById(R.id.refreshview);
		refreshLayout.setOnRefreshListener(this);
		//initExpandableListView();
		loadingAnimation = (RotateAnimation) AnimationUtils.loadAnimation(this, R.anim.rotating);
		// 添加匀速转动动画
		LinearInterpolator lir = new LinearInterpolator();
		loadingAnimation.setInterpolator(lir);
	}

	@Override
	public void onRefresh()
	{
		// 下拉刷新操作
		new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				refreshLayout.refreshFinish(PullToRefreshLayout.REFRESH_SUCCEED);
			}
		}.sendEmptyMessageDelayed(0, 2000);
	}

	
}
