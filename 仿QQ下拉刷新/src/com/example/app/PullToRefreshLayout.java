package com.example.app;

import java.lang.reflect.Field;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * ��������ˢ�¾���һ�����֣��������������ӿؼ�������һ��������ͷ����һ���ǰ������ݵ�contentView��������AbsListView���κ����ࣩ
 * �����������ͣ�http://blog.csdn.net/zhongkejingwang/article/details/38340701
 * @author �¾�
 */
public class PullToRefreshLayout extends RelativeLayout implements OnTouchListener
{
	public static final String TAG = "PullToRefreshLayout";
	// ����ˢ��
	public static final int PULL_TO_REFRESH = 0;
	// �ͷ�ˢ��
	public static final int RELEASE_TO_REFRESH = 1;
	// ����ˢ��
	public static final int REFRESHING = 2;
	// ˢ�����
	public static final int DONE = 3;
	// ��ǰ״̬
	private int state = PULL_TO_REFRESH;
	// ˢ�»ص��ӿ�
	private OnRefreshListener mListener;
	// ˢ�³ɹ�
	public static final int REFRESH_SUCCEED = 0;
	// ˢ��ʧ��
	public static final int REFRESH_FAIL = 1;
	// ����ͷ
	private View headView;
	// ����
	private View contentView;
	// ����Y���꣬��һ���¼���Y����
	private float downY, lastY;
	// �����ľ���
	public float moveDeltaY = 0;
	// �ͷ�ˢ�µľ���
	private float refreshDist = 200;
	private Timer timer;
	private MyTimerTask mTask;
	// �ع��ٶ�
	public float MOVE_SPEED = 8;
	// ��һ��ִ�в���
	private boolean isLayout = false;
	// �Ƿ��������
	private boolean canPull = true;
	// ��ˢ�¹����л�������
	private boolean isTouchInRefreshing = false;
	// ��ָ��������������ͷ�Ļ�������ȣ��м�������к����仯
	private float radio = 2;
	// ������ͷ��ת180�㶯��
	private RotateAnimation rotateAnimation;
	// ������ת����
	private RotateAnimation refreshingAnimation;
	// �����ļ�ͷ
	private View pullView;
	// ����ˢ�µ�ͼ��
	private View refreshingView;
	// ˢ�½��ͼ��
	private View stateImageView;
	// ˢ�½�����ɹ���ʧ��
	private TextView stateTextView;
	/**
	 * ִ���Զ��ع���handler
	 */
	Handler updateHandler = new Handler()
	{

		@Override
		public void handleMessage(Message msg)
		{
			// �ص��ٶ�����������moveDeltaY���������
			MOVE_SPEED = (float) (8 + 5 * Math.tan(Math.PI / 2 / getMeasuredHeight() * moveDeltaY));
			if (state == REFRESHING && moveDeltaY <= refreshDist && !isTouchInRefreshing)
			{
				moveDeltaY = refreshDist;
				mTask.cancel();
			}
			if (canPull)
				moveDeltaY -= MOVE_SPEED;
			if (moveDeltaY <= 0)
			{
				// ����ɻص�
				moveDeltaY = 0;
				pullView.clearAnimation();
				if (state != REFRESHING)
					changeState(PULL_TO_REFRESH);
				mTask.cancel();
			}
			requestLayout();
		}

	};

	public void setOnRefreshListener(OnRefreshListener listener)
	{
		mListener = listener;
	}

	public PullToRefreshLayout(Context context)
	{
		super(context);
		initView(context);
	}

	public PullToRefreshLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initView(context);
	}

	public PullToRefreshLayout(Context context, AttributeSet attrs, int defStyle)
	{
		super(context, attrs, defStyle);
		initView(context);
	}

	private void initView(Context context)
	{
		timer = new Timer();
		mTask = new MyTimerTask(updateHandler);
		rotateAnimation = (RotateAnimation) AnimationUtils.loadAnimation(context, R.anim.reverse_anim);
		refreshingAnimation = (RotateAnimation) AnimationUtils.loadAnimation(context, R.anim.rotating);
		// �������ת������
		LinearInterpolator lir = new LinearInterpolator();
		rotateAnimation.setInterpolator(lir);
		refreshingAnimation.setInterpolator(lir);
	}

	private void hideHead()
	{
		if (mTask != null)
		{
			mTask.cancel();
			mTask = null;
		}
		mTask = new MyTimerTask(updateHandler);
		timer.schedule(mTask, 0, 5);
	}

	/**
	 * ���ˢ�²�������ʾˢ�½��
	 */
	public void refreshFinish(int refreshResult)
	{
		refreshingView.clearAnimation();
		refreshingView.setVisibility(View.GONE);
		switch (refreshResult)
		{
		case REFRESH_SUCCEED:
			// ˢ�³ɹ�
			stateImageView.setVisibility(View.VISIBLE);
			stateTextView.setText(R.string.refresh_succeed);
			stateImageView.setBackgroundResource(R.drawable.refresh_succeed);
			break;
		case REFRESH_FAIL:
			// ˢ��ʧ��
			stateImageView.setVisibility(View.VISIBLE);
			stateTextView.setText(R.string.refresh_fail);
			stateImageView.setBackgroundResource(R.drawable.refresh_failed);
			break;
		default:
			break;
		}
		// ˢ�½��ͣ��1��
		new Handler()
		{
			@Override
			public void handleMessage(Message msg)
			{
				state = PULL_TO_REFRESH;
				hideHead();
			}
		}.sendEmptyMessageDelayed(0, 1000);
	}

	private void changeState(int to)
	{
		state = to;
		switch (state)
		{
		case PULL_TO_REFRESH:
			// ����ˢ��
			stateImageView.setVisibility(View.GONE);
			stateTextView.setText(R.string.pull_to_refresh);
			pullView.clearAnimation();
			pullView.setVisibility(View.VISIBLE);
			break;
		case RELEASE_TO_REFRESH:
			// �ͷ�ˢ��
			stateTextView.setText(R.string.release_to_refresh);
			pullView.startAnimation(rotateAnimation);
			break;
		case REFRESHING:
			// ����ˢ��
			pullView.clearAnimation();
			refreshingView.setVisibility(View.VISIBLE);
			pullView.setVisibility(View.INVISIBLE);
			refreshingView.startAnimation(refreshingAnimation);
			stateTextView.setText(R.string.refreshing);
			break;
		default:
			break;
		}
	}

	/*
	 * ���� Javadoc���ɸ��ؼ������Ƿ�ַ��¼�����ֹ�¼���ͻ
	 * 
	 * @see android.view.ViewGroup#dispatchTouchEvent(android.view.MotionEvent)
	 */
	@Override
	public boolean dispatchTouchEvent(MotionEvent ev)
	{
		switch (ev.getActionMasked())
		{
		case MotionEvent.ACTION_DOWN:
			downY = ev.getY();
			lastY = downY;
			if (mTask != null)
			{
				mTask.cancel();
			}
			if (ev.getY() < moveDeltaY)
				return true;
			break;
		case MotionEvent.ACTION_MOVE:
			if (canPull)
			{
				// ��ʵ�ʻ�����������С������������ĸо�
				moveDeltaY = moveDeltaY + (ev.getY() - lastY) / radio;
				if (moveDeltaY < 0)
					moveDeltaY = 0;
				if (moveDeltaY > getMeasuredHeight())
					moveDeltaY = getMeasuredHeight();
				if (state == REFRESHING)
				{
					isTouchInRefreshing = true;
				}
			}
			lastY = ev.getY();
			// ������������ı����
			radio = (float) (2 + 2 * Math.tan(Math.PI / 2 / getMeasuredHeight() * moveDeltaY));
			requestLayout();
			if (moveDeltaY <= refreshDist && state == RELEASE_TO_REFRESH)
			{
				changeState(PULL_TO_REFRESH);
			}
			if (moveDeltaY >= refreshDist && state == PULL_TO_REFRESH)
			{
				changeState(RELEASE_TO_REFRESH);
			}
			if (moveDeltaY > 8)
			{
				// ��ֹ�����������󴥷������¼��͵���¼�
				clearContentViewEvents();
			}
			if (moveDeltaY > 0)
			{
				return true;
			}
			break;
		case MotionEvent.ACTION_UP:
			if (moveDeltaY > refreshDist)
				// ����ˢ��ʱ�������ͷź�����ͷ������
				isTouchInRefreshing = false;
			if (state == RELEASE_TO_REFRESH)
			{
				changeState(REFRESHING);
				// ˢ�²���
				if (mListener != null)
					mListener.onRefresh();
			} else
			{

			}
			hideHead();
		default:
			break;
		}
		// �¼��ַ���������
		return super.dispatchTouchEvent(ev);
	}

	/**
	 * ͨ�������޸��ֶ�ȥ�������¼��͵���¼�
	 */
	private void clearContentViewEvents()
	{
		try
		{
			Field[] fields = AbsListView.class.getDeclaredFields();
			for (int i = 0; i < fields.length; i++)
				if (fields[i].getName().equals("mPendingCheckForLongPress"))
				{
					// mPendingCheckForLongPress��AbsListView�е��ֶΣ�ͨ�������ȡ������Ϣ�б�ɾ����ȥ�������¼�
					fields[i].setAccessible(true);
					contentView.getHandler().removeCallbacks((Runnable) fields[i].get(contentView));
				} else if (fields[i].getName().equals("mTouchMode"))
				{
					// TOUCH_MODE_REST = -1�� �������ȥ������¼�
					fields[i].setAccessible(true);
					fields[i].set(contentView, -1);
				}
			// ȥ������
			((AbsListView) contentView).getSelector().setState(new int[]
			{ 0 });
		} catch (Exception e)
		{
			Log.d(TAG, "error : " + e.toString());
		}
	}

	/*
	 * ���� Javadoc��������ӰЧ������ɫֵ�����޸�
	 * 
	 * @see android.view.ViewGroup#dispatchDraw(android.graphics.Canvas)
	 */
	@Override
	protected void dispatchDraw(Canvas canvas)
	{
		super.dispatchDraw(canvas);
		if (moveDeltaY == 0)
			return;
		RectF rectF = new RectF(0, 0, getMeasuredWidth(), moveDeltaY);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		// ��Ӱ�ĸ߶�Ϊ26
		LinearGradient linearGradient = new LinearGradient(0, moveDeltaY, 0, moveDeltaY - 26, 0x66000000, 0x00000000, TileMode.CLAMP);
		paint.setShader(linearGradient);
		paint.setStyle(Style.FILL);
		canvas.drawRect(rectF, paint);
	}

	private void initView()
	{
		pullView = headView.findViewById(R.id.pull_icon);
		stateTextView = (TextView) headView.findViewById(R.id.state_tv);
		refreshingView = headView.findViewById(R.id.refreshing_icon);
		stateImageView = headView.findViewById(R.id.state_iv);
	}

	@Override
	protected void onLayout(boolean changed, int l, int t, int r, int b)
	{
		if (!isLayout)
		{
			headView = getChildAt(0);
			contentView = getChildAt(1);
			contentView.setOnTouchListener(this);
			isLayout = true;
			initView();
			refreshDist = ((ViewGroup) headView).getChildAt(0).getMeasuredHeight();
		}
		if (canPull)
		{
			// �ı��ӿؼ��Ĳ���
			headView.layout(0, (int) moveDeltaY - headView.getMeasuredHeight(), headView.getMeasuredWidth(), (int) moveDeltaY);
			contentView.layout(0, (int) moveDeltaY, contentView.getMeasuredWidth(), (int) moveDeltaY + contentView.getMeasuredHeight());
		} else
			super.onLayout(changed, l, t, r, b);
	}

	class MyTimerTask extends TimerTask
	{
		Handler handler;

		public MyTimerTask(Handler handler)
		{
			this.handler = handler;
		}

		@Override
		public void run()
		{
			handler.sendMessage(handler.obtainMessage());
		}

	}

	@Override
	public boolean onTouch(View v, MotionEvent event)
	{
		// ��һ��item�ɼ��һ���������
		AbsListView alv = null;
		try
		{
			alv = (AbsListView) v;
		} catch (Exception e)
		{
			Log.d(TAG, e.getMessage());
			return false;
		}
		if (alv.getCount() == 0)
		{
			canPull = true;
		} else if (alv.getFirstVisiblePosition() == 0 && alv.getChildAt(0).getTop() >= 0)
		{
			canPull = true;
		} else
			canPull = false;
		return false;
	}
}
