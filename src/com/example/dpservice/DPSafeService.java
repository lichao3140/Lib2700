package com.example.dpservice;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.android.talkserversdk.TalkManager;
import org.dpower.GenernalGpioSet.JniClassGpioSet;
import org.dpower.GenernalGpioSet.UtilConst;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.dpower.domain.AlarmInfo;
import com.dpower.domain.AlarmLog;
import com.dpower.domain.AlarmNameInfo;
import com.dpower.domain.AlarmTypeInfo;
import com.dpower.domain.SafeModeInfo;
import com.dpower.function.DPFunction;
import com.dpower.util.CommonUT;
import com.dpower.util.ConstConf;
import com.dpower.util.DPDBHelper;
import com.dpower.util.MyLog;
import com.dpower.util.ProjectConfigure;
import com.example.dpsafe.DPSafe;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;

/**
 * 本地安防服务 默认所有探头常开和禁用 0-禁用，1-启用 探头接法（高低电平）：0-常闭，1-常开
 * */
public class DPSafeService extends Service {
	private static final String TAG = "DPSafeService";
    
    /** 安防探头个数,从配置文件中获取 */
	public static int tanTouNum = ConstConf.DEFAULT_SAFE_TANTOU;

	/** 防区名称列表，从配置文件中获取 */
	public List<AlarmNameInfo> alarmAreaNameList;

	/** 报警类型名称列表，从配置文件中获取 */
	public List<AlarmTypeInfo> alarmTypeNameList;

	/** 所有安防探头的名称在防区名称列表的索引 */
	public int[] alarmArea = null;

	/** 所有安防探头的报警类型名称在报警类型名称列表的索引 */
	public int[] alarmType = null;
	
	/** 记录上一次检测到安防探头的状态, 对应state */
	public long oldState = 0;
	
	private static DPSafeService mInstance = null;
	private static int[] mDefaultAlarmArea = new int[ConstConf.DEFAULT_SAFE_TANTOU];
	private static int[] mDefaultAlarmType = new int[ConstConf.DEFAULT_SAFE_TANTOU];

	/** 当前安防探头的状态0-255 */
	private int mState;

	/** 当前安防模式 mode 0-测试，1-撤防，2-夜间，3-在家，4-离家 */
	private int mCurrentMode = 0;

	/** 当前各个探头的接法(常开常闭,用户设置的接法) */
	private long mConnection = 0;

	/** 当前各个探头的使能状态，禁用或启用 */
	private long mEnable = 0;
	
	/** 记录发生改变的安防状态 */
	private long mChange = 0;
	
	/** 记录正在报警的防区,针对按主键（HOME键）退出报警界面的情况 */
	private int[] mAlarmingArea; 
	
	private ScheduledThreadPoolExecutor mSafeScheduled;
	private SafeHandler mSafeHandler = null;
	private Handler mHandler = null;
	private HandlerThread mHandlerThread;
	private JniClassGpioSet mGpioSet;
	private int mDoorbell_H17;
	private int mH17_oldState = 0;
	private SoundPool mAlarmPlayer = null;
	private int mAlarmID = 0;
	private SoundPool mDoorbellPlayer = null;
	private int mDoorbellID = 0;
	private boolean mDelaying = false;// 报警铃声变量，ture-预警,false-报警,报警铃声开始后不能再次改变为预警铃声
	private boolean mSafeCheckSwitch = true;
	private int mCurrentDelayTime = 0; // 倒计时结束后有预警铃声转报警铃声
	private Timer mTimer;
	private DelayTimerTask mDelayTimerTask;
	private Intent mAlarmIntent;
	
	public static DPSafeService getInstance() {
		return mInstance;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		MyLog.print(TAG, "onCreate开始");
		mInstance = this;
		loadAlarmFile();
		mAlarmingArea = new int[tanTouNum];
		Arrays.fill(mAlarmingArea, 1);
		alarmArea = DPDBHelper.getSafeArea();
		if (alarmArea == null) {
			MyLog.print("alarmArea = null");
			alarmArea = new int[tanTouNum];
			setAlarmArea(mDefaultAlarmArea);
		}
		alarmType = DPDBHelper.getSafeType();
		if (alarmType == null) {
			MyLog.print("alarmType = null");
			alarmType = new int[tanTouNum];
			setAlarmType(mDefaultAlarmType);
			// 初始化安防开关和报警延迟
			if (ProjectConfigure.project == 2) {
				changeSafeEnable(ConstConf.HOME_MODE, 128);
				changeSafeEnable(ConstConf.NIGHT_MODE, 255);
				changeSafeEnable(ConstConf.LEAVE_HOME_MODE, 127);
				int[] time = { 30, 0, 0, 0, 0, 0, 0, 0 };
				for (int i = 0; i < ConstConf.MODE_NUM; i++) {
					changeAlarmDelayTime(i, time);
				}
			}
		}
		mAlarmIntent = new Intent();
		DPSafe.initDPSafe();
		mCurrentMode = DPDBHelper.getSafeMode();
		if (mCurrentMode == ConstConf.TEST_MODE) {
			mCurrentMode = ConstConf.UNSAFE_MODE;
			DPDBHelper.setSafeMode(mCurrentMode);
		}
		mConnection = DPDBHelper.getSefeConnection();
		mEnable = DPDBHelper.getSafeModeEnable(mCurrentMode);
		oldState = mConnection; //初始默认上一次接法是正常状态，不会报警
		/**
		 * 开启定时器，每200ms检测安防探头一次
		 */
		MyLog.print("onCreate oldState - >" + oldState);
		mSafeScheduled = new ScheduledThreadPoolExecutor(5);
		mSafeScheduled.scheduleWithFixedDelay(mSafeRunnable, 200, 200,
				TimeUnit.MILLISECONDS);

		mHandlerThread = new HandlerThread("safeThread");
		mHandlerThread.start();
		mSafeHandler = new SafeHandler(mHandlerThread.getLooper());
		if (ProjectConfigure.project == 2) {
			mGpioSet = new JniClassGpioSet();
			mDoorbell_H17 = mGpioSet.InitGpio(UtilConst.GPIO_H17, 0);
		}
		mHandler = new Handler();
		MyLog.print(TAG, "onCreate完毕");
	}
	
	/**
	 * 导入安防配置文件(改变语言时需要重新导入)
	 */
	public void loadAlarmFile() {
		alarmAreaNameList = new ArrayList<AlarmNameInfo>();
		alarmTypeNameList = new ArrayList<AlarmTypeInfo>();
		String fileInfo = readTextFile(ConstConf.SAFE_ALARM_PATH + ConstConf.SAFE_ALARM_TXT);
		if (fileInfo.length() > 0) {
			printfStringtoJson(fileInfo);
		} else {
			MyLog.print(MyLog.ERROR, "读取安防配置文件失败");
		}
		MyLog.print(alarmAreaNameList.toString());
		MyLog.print(alarmTypeNameList.toString());
	}
	
	/** 读取JSON文本文件 */
	private String readTextFile(String filePath) {
		String str = "";
		// 此文件编码ANSI
		File file = new File(filePath);
		if (file.exists()) {
			InputStream in = null;
			try {
				in = new FileInputStream(file);
				int size = in.available();
				byte[] buffer = new byte[size];
				in.read(buffer);
				str = new String(buffer, "UTF-8");
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			MyLog.print("Safe file is not find.");
		}
		return str;
	}

	/** 解析JSON文本文件 */
	private void printfStringtoJson(String info) {
		MyLog.print("读取安防配置表：" + info);
		JSONTokener jsonParser = new JSONTokener(info);

		try {
			JSONObject person = (JSONObject) jsonParser.nextValue();
			tanTouNum = person.getInt("probenum");
			MyLog.print("tanTouNum:" + tanTouNum);
			JSONObject jsonObject;
			if(Locale.getDefault().getLanguage().equals("en")) {
				jsonObject = person.getJSONObject("english");
			} else if (Locale.getDefault().getLanguage().equals("ko")) {
				jsonObject = person.getJSONObject("korean");
			} else {
				jsonObject = person.getJSONObject("chinese");
			}
			JSONArray area = jsonObject.getJSONArray("area");
			for (int i = 0; i < area.length(); i++) {
				JSONObject obj = area.getJSONObject(i);
				String name = (String) obj.get("name");
				int value = Integer.valueOf((String) obj.get("value"));
				alarmAreaNameList.add(new AlarmNameInfo(name, value));
			}
			JSONArray type = jsonObject.getJSONArray("type");
			for (int i = 0; i < type.length(); i++) {
				JSONObject obj = type.getJSONObject(i);
				String name = (String) obj.get("name");
				int priority = Integer.valueOf((String) obj.get("priority"));
				int value = Integer.valueOf((String) obj.get("value"));
				alarmTypeNameList.add(new AlarmTypeInfo(name, priority, value));
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 设置安防防区
	 * @param alarmArea 安防防区列表
	 */
	public boolean setAlarmArea(int[] alarmArea) {
		if (this.alarmArea.length == alarmArea.length) {
			this.alarmArea = alarmArea;
			return DPDBHelper.setSafeArea(this.alarmArea);
		}
		return false;
	}
	
	/**
	 * @功能：修改所有的报警类型
	 * @备注：需判断类型是否紧急后修改延时时间和启用禁用
	 * @描述：紧急、烟感、煤气等类型必须启动并不延时报警时间
	 */
	public boolean setAlarmType(int[] alarmType) {
		if (this.alarmType.length == alarmType.length) {
			for (int i = 0; i < alarmType.length; i++) {
				setAlarmType(i, alarmType[i]);
			}
			return true;
		}
		return false;
	}
	
	/**
	 * @功能：修改单个报警类型
	 * @参数1：int i -第几个探头 从0开始
	 * @参数2：int alarmType 探头类型
	 * @备注：需判断类型是否紧急后修改延时时间和启用禁用
	 * @描述：紧急、烟感、煤气等类型必须启动并不延时报警时间
	 */
	public boolean setAlarmType(int i, int alarmType) {
		if (i >= 0 && i < tanTouNum) {
			this.alarmType[i] = alarmType;
			long modeEnable, temp;
			int[] time;
			if (this.alarmType[i] >= alarmTypeNameList.size())
				return false;
			if (isEmergency(i)) {
				/** 将各个模式下的报警延时设置为0，并启用所有模式下的该探头 */
				for (int j = 0; j < ConstConf.MODE_NUM; j++) {
					modeEnable = DPDBHelper.getSafeModeEnable(j);
					temp = modeEnable | (1 << i);
					if (temp != modeEnable) {
						changeSafeEnable(j, temp);
					}
					time = DPDBHelper.getAlarmDelayTime(j);
					if (time[i] != 0) {
						time[i] = 0;
						changeAlarmDelayTime(j, time);
					}
				}
			} else if (alarmTypeNameList.get(this.alarmType[i]).priority == 3) {
				// 允许延时报警、允许在模式设置中禁用
				modeEnable = DPDBHelper.getSafeModeEnable(ConstConf.UNSAFE_MODE);
				temp = modeEnable & (~(1 << i));
				if (temp != modeEnable) {
					changeSafeEnable(ConstConf.UNSAFE_MODE, temp);
				}
//				for (int j = 1; j < ConstConf.MODE_NUM; j++) {
//					modeenable = DPDBHelper.getSafeModeEnable(j);
//					temp = modeenable & (~(1 << i));
//					if (temp != modeenable) {
//						ChangeSafeEnable(j, temp);
//					}
//					int[] modetime = DPDBHelper.getAlarmDelayTime(j);
//					modetime[i] = 0;
//					DPDBHelper.setAlarmDelayTime(j, modetime);
//				}
			}
			return DPDBHelper.setSafeType(this.alarmType);
		}
		return false;
	}
	
	/**
	 * @功能：判断是否可以设置延时和启用
	 * @备注：priority 0-门铃、1-紧急、2-烟感煤气、3-门磁红外
	 */
	public boolean isEmergency(int i) {
		if (alarmType[i] < alarmTypeNameList.size()) {
			return alarmTypeNameList.get(alarmType[i]).priority < 3;
		}
		return false;
	}
	
	/**
	 * 修改用户模式下的 安防模式对应的开关状态
	 * @param model 模式 例如 在家 离家模式
	 * @param enable 8个开关的开闭状态
	 */
	public void changeSafeEnable(int model, long enable) {
		if (model >= ConstConf.MODE_NUM || model < 0) {
			MyLog.print("This mode crossing the line.mode = " + mCurrentMode);
			return;
		}
		DPDBHelper.setSafeModeEnable(model, enable);
		if (model == mCurrentMode) {
			mEnable = enable;
		}
	}
	
	/**
	 * 修改安防模式对应的报警延迟时间
	 * @param model 模式 例如 在家 离家模式
	 * @param time 报警延迟时间
	 */
	public void changeAlarmDelayTime(int model, int[] time) {
		DPDBHelper.setAlarmDelayTime(model, time);
	}
	
	private Runnable mSafeRunnable = new Runnable() {

		@Override
		public void run() {
			// 单独门铃IO
			if (ProjectConfigure.project == 2) {
				int h17_state = mGpioSet.GetGpioValue(mDoorbell_H17);
				if (h17_state != mH17_oldState) {
					mH17_oldState = h17_state;
					MyLog.print("mDoorbell_H17 = " + h17_state);
					if (h17_state == 0) {
						playDoorbell();
					}
				}
			}
			if (!mSafeCheckSwitch) {
				return;
			}
			mState = DPSafe.getSafeVal();
			if (mState > 256 || mState < 0) {
				MyLog.print("get safe val error+" + mState);
				return;
			}
			
			// 更改防区顺序
			if (ProjectConfigure.project == 1 && ProjectConfigure.size == 10) {
				String binaryState = Integer.toBinaryString(mState);
				int length = binaryState.length();
				for (; length < tanTouNum; length++) {
					binaryState = "0" + binaryState;
				}
				
				binaryState = new StringBuffer(binaryState).reverse().toString();
				mState = Integer.parseInt(binaryState, 2);
			} else if (ProjectConfigure.project == 2) {
				String binaryState = Integer.toBinaryString(mState);
				int length = binaryState.length();
				for (; length < tanTouNum; length++) {
					binaryState = "0" + binaryState;
				}
				
				String left = binaryState.substring(0, length / 2);
				String right = binaryState.substring(length / 2, length);
				binaryState = new StringBuffer(left).reverse().toString() 
						+ new StringBuffer(right).reverse().toString();
				mState = Integer.parseInt(binaryState, 2);
			}
			if (mState == oldState) {
//				if (mState != 255) {
//					MyLog.print("same state! mState = " + mState);
//				}
				return;
			}
			
			MyLog.print("tantou changed state = " + mState + " ="
					+ Integer.toBinaryString(mState) + ",oldstate = " + oldState
					+ "= " + Long.toBinaryString(oldState));
			/** 获取发生改变的探头 */
			mChange = oldState ^ mState;
			oldState = mState;
			mSafeHandler.obtainMessage().sendToTarget();
		}
	};
	
	/**
	 * 播放门铃
	 */
	private void playDoorbell() {
//		if (DPFunction.getAlarming()) return;
		// 播放门铃声3次
		if (mDoorbellPlayer != null) {
			if (mDoorbellID != 0) {
				mDoorbellPlayer.stop(mDoorbellID);
				mDoorbellID = 0;
			}
			mDoorbellPlayer.release();
			mDoorbellPlayer = null;
		}
		mDoorbellPlayer = new SoundPool(1, AudioManager.STREAM_ALARM, 5);
		mDoorbellPlayer.load(ConstConf.DOOR_RING_PATH, 1);
		mDoorbellPlayer.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
				mDoorbellID = soundPool.play(1, 1, 1, 0, 2, 1);
				if (mDoorbellID == 0) {
					MyLog.print(MyLog.ERROR, "paly ring error");
				}
			}
		});
	}
	
	/***
	 * 处理发生改变的安防状态,报警时发广播
	 */
	private class SafeHandler extends Handler {
		
		public SafeHandler(Looper looper) {
			super(looper);
		}
		
		@Override
		public void handleMessage(Message msg) {
			// 测试模式
			if (mCurrentMode == ConstConf.TEST_MODE) {
				MyLog.print("报警触发 Test mode");
				return;
			}
			if (mCurrentMode >= ConstConf.MODE_NUM || mCurrentMode < 0) {
				MyLog.print("This mode crossing the line.mode = "
						+ mCurrentMode);
				return;
			}
			MyLog.print("mChange = " + mChange + ";mEnable = " + mEnable);
			long enableChange = mChange & mEnable;
			long abnormal = mState ^ mConnection;
			long alarm = enableChange & abnormal;
			if (alarm == 0) {
				return;
			}

			for (int i = 0; i < tanTouNum; i++) {
				if ((alarm & (1 << i)) > 0) {
					MyLog.print("alarm i = " + i + ",  " + mState + " "
							+ oldState);
					if (alarmTypeNameList.get(alarmType[i]).priority == 0) { // 判断是门铃
						playDoorbell();
					} else {
						if (ProjectConfigure.project != 2) {
							if (alarmTypeNameList.get(alarmType[i]).priority == 1) {
								// 通知手机报警
								DPFunction.toPhoneAlarm(i + 1,
										alarmAreaNameList.get(alarmArea[i]).value,
										alarmTypeNameList.get(alarmType[i]).value,
										CommonUT.formatTime(System.currentTimeMillis()));
								// 紧急报警不启动界面，只通知管理中心报警
								toManageAndLog(i);
								continue;
							}
						}
						toManageAndLog(i);

						// 报警之前挂断所有通话
						DPFunction.callHangUp();
						mAlarmingArea[i] = 0;
						
						if (!DPFunction.getAlarmActivityState()) {
							if (DPFunction.alarmActivity != null) {
								DPFunction.setAlarmActivityState(true);
								mAlarmIntent.setClass(DPSafeService.this, DPFunction.alarmActivity);
								mAlarmIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
										| Intent.FLAG_ACTIVITY_SINGLE_TOP);
								mAlarmIntent.putExtra(DPFunction.ALARM_ID, i);
								startActivity(mAlarmIntent);
								Intent intent = new Intent();
								intent.setAction(DPFunction.ACTION_ALARMING);
								intent.putExtra(DPFunction.ALARM_ID, i);
								sendBroadcast(intent);
								MyLog.print("alarm activity start");
							}
							
							// 针对按主键（HOME键）退出报警界面的情况，重新启动报警界面时要恢复之前没有撤防的防区报警
							if (DPFunction.getAlarming()) {
								final int k = i;
								new Handler().postDelayed(new Runnable() {
									
									@Override
									public void run() {
										for (int j = 0; j < mAlarmingArea.length; j++) {
											if (j == k) continue;
											if (mAlarmingArea[j] == 0) {
												MyLog.print("alarm add j = " + j);
												Intent intent = new Intent();
												intent.setAction(DPFunction.ACTION_ALARMING);
												intent.putExtra(DPFunction.ALARM_ID, j);
												sendBroadcast(intent);
												MyLog.print("alarm add broadcast send");
											}
										}
									}
								}, 500);
							}
						} else {
							final int k = i;
							new Handler().postDelayed(new Runnable() {
								
								@Override
								public void run() {
									Intent intent = new Intent();
									intent.putExtra(DPFunction.ALARM_ID, k);
									intent.setAction(DPFunction.ACTION_ALARMING);
									sendBroadcast(intent);
								}
							}, 500);
						}
						MyLog.print("alarm broadcast sent " + i + "  area "
								+ alarmArea[i] + ", type " + alarmType[i]);
						DPFunction.toPhoneAlarm(i + 1,
								alarmAreaNameList.get(alarmArea[i]).value,
								alarmTypeNameList.get(alarmType[i]).value,
								CommonUT.formatTime(System.currentTimeMillis()));
						
						int delayTime[] = DPDBHelper.getAlarmDelayTime(mCurrentMode);
						if (!DPFunction.getAlarming()) {
							DPFunction.setAlarming();
							if (delayTime[i] > 0) {
								mCurrentDelayTime = delayTime[i];
								mDelaying = true;
								/** 创建定时器，1秒1次检测报警延时，倒计时结束后更换报警铃声 */
								mTimer = new Timer();
								mDelayTimerTask = new DelayTimerTask();
								mTimer.schedule(mDelayTimerTask, 1000, 1000);
							} else {
								mDelaying = false;
							}
							playAlarmRing();
						} else {
							if (mDelaying) {
								int delay = delayTime[i];
								if (delay == 0) {
									mDelaying = false;
									playAlarmRing();
								} else {
									if (mTimer != null) {
										if (mCurrentDelayTime > 0 && delay > 0) {
											mCurrentDelayTime = Math.min(delay, mCurrentDelayTime);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * 上报管理中心和添加报警记录
	 * @param i 报警防区
	 */
	private void toManageAndLog(int i) {
		long time = System.currentTimeMillis();
		String alarmRoom; //包括防区号和防区的名称
		int areaNum = i + 1;
		if (alarmAreaNameList.get(alarmArea[i]).value < 10) {
			alarmRoom = Integer.toString(areaNum) + "0" 
					+ alarmAreaNameList.get(alarmArea[i]).value;
		} else {
			alarmRoom = Integer.toString(areaNum) 
					+ alarmAreaNameList.get(alarmArea[i]).value;
		}
		int delayTime[] = DPDBHelper.getAlarmDelayTime(mCurrentMode);
		int result = TalkManager.toManageAlarm(CommonUT.formatTime(time), 
				Integer.parseInt(alarmRoom),
				alarmTypeNameList.get(alarmType[i]).value,
				delayTime[i]);
		AlarmLog alarmLog = new AlarmLog(mCurrentMode, areaNum, alarmArea[i],
				alarmType[i], time);
		if (result == 0) {
			alarmLog.setIsSuccess(true);
		}
		DPDBHelper.addAlarmLog(alarmLog);
	}
	
	private class DelayTimerTask extends TimerTask {
		
		@Override
		public void run() {
			if (mDelaying) {
				if (--mCurrentDelayTime <= 0) {
					mDelaying = false;
					playAlarmRing();
				} else {
					MyLog.print("currentDelayTime:" + mCurrentDelayTime);
				}
			}
		}
	};
	
	private void playAlarmRing() {
		if (!mDelaying) {
			if (mTimer != null) {
				mDelayTimerTask.cancel();
				mTimer.cancel();
				mDelayTimerTask = null;
				mTimer = null;
			}
		}
//		if (mDoorbellPlayer != null) {
//			if (mDoorbellID != 0) {
//				mDoorbellPlayer.stop(mDoorbellID);
//				mDoorbellID = 0;
//			}
//			mDoorbellPlayer.release();
//			mDoorbellPlayer = null;
//		}
		if (mAlarmPlayer != null) {
			if (mAlarmID != 0) {
				mAlarmPlayer.stop(mAlarmID);
				mAlarmID = 0;
			}
			mAlarmPlayer.release();
			mAlarmPlayer = null;
		}
		mAlarmPlayer = new SoundPool(1, AudioManager.STREAM_ALARM, 5);
		mAlarmPlayer.load(mDelaying ? ConstConf.DELAYALARM_RING_PATH
				: ConstConf.ALARM_RING_PATH, 1);
		mAlarmPlayer.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId,
					int status) {
				mAlarmID = soundPool.play(1, 1, 1, 0, -1, 1);
				if (mAlarmID == 0) {
					MyLog.print(MyLog.ERROR, "paly alarmRing error");
				}
			}
		});
	}
	
	public void releaseAlarmRing() {
		mSafeCheckSwitch = false;
		mHandler.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				mSafeCheckSwitch = true;
			}
		}, 2000);
		if (mAlarmPlayer != null) {
			if (mAlarmID != 0) {
				mAlarmPlayer.stop(mAlarmID);
				mAlarmID = 0;
			}
			mAlarmPlayer.release();
			mAlarmPlayer = null;
		}
		if (mTimer != null) {
			mDelayTimerTask.cancel();
			mTimer.cancel();
			mDelayTimerTask = null;
			mTimer = null;
		}
		Arrays.fill(mAlarmingArea, 1);
	}
	
	public static void setDefaultAlarmArea(int[] area) {
		if (area.length == mDefaultAlarmArea.length) {
			System.arraycopy(area, 0, mDefaultAlarmArea, 0,
					mDefaultAlarmArea.length);
		} else {
			Arrays.fill(mDefaultAlarmArea, 0);
		}
	}

	public static void setDefaultAlarmType(int[] type) {
		if (type.length == mDefaultAlarmType.length) {
			System.arraycopy(type, 0, mDefaultAlarmType, 0,
					mDefaultAlarmType.length);
		} else {
			Arrays.fill(mDefaultAlarmType, 0);
		}
	}
	
	/** 获取指定安防模式的所有状态 */
	public List<AlarmInfo> getAlarmInfoList(int model) {
		if (alarmAreaNameList == null || alarmTypeNameList == null) {
			return null;
		}
		List<AlarmInfo> list = new ArrayList<AlarmInfo>();
		long enable = DPDBHelper.getSafeModeEnable(model);
		int[] time = DPDBHelper.getAlarmDelayTime(model);
		for (int i = 0; i < tanTouNum; i++) {
			String areaName = alarmAreaNameList.get(alarmArea[i]).name;
			String typeName = alarmTypeNameList.get(alarmType[i]).name;
			boolean bEnable = (enable & (1 << i)) > 0;
			boolean bOpen = ((mConnection & (1 << i)) > 0);
			AlarmInfo info = new AlarmInfo(areaName, typeName, bEnable,
					bOpen, time[i]);
			MyLog.print(info.toString());
			list.add(info);
		}
		return list;
	}

	/** 设置特定防区 */
	public boolean setAlarmArea(int i, int alarmArea) {
		this.alarmArea[i] = alarmArea;
		return DPDBHelper.setSafeArea(this.alarmArea);
	}

	/**
	 * 修改室内机的安防模式（同时会写入数据库中）
	 * @param isAuto true-室内机自动同步安防模式, false-手动修改安防模式
	 */
	public void changeSafeMode(int model, boolean isAuto) {
		if (mCurrentMode >= ConstConf.MODE_NUM || mCurrentMode < 0) {
			MyLog.print("changeSafeMode This mode crossing the line.mode = "
					+ mCurrentMode);
			return;
		}
		if (model == mCurrentMode
				&& model != ConstConf.UNSAFE_MODE) {
			MyLog.print("cannot set the same safemode");
			return;
		}
		boolean addLog = (mCurrentMode != ConstConf.TEST_MODE 
				&& model != ConstConf.TEST_MODE);
		DPDBHelper.setSafeMode(model);
		mCurrentMode = DPDBHelper.getSafeMode();
		mEnable = DPDBHelper.getSafeModeEnable(mCurrentMode);
		// 由测试模式切换到其他模式或其他模式切换到测试模式都不需要安防模式记录和同步室内分机安防
		if (addLog) {
			sendBroadcast(new Intent(DPFunction.ACTION_SAFE_MODE));
			SafeModeInfo info = new SafeModeInfo(mCurrentMode, System.currentTimeMillis());
			if (ProjectConfigure.project == 2) {
				int result = TalkManager.toManageAlarm(
						CommonUT.formatTime(info.getTime()), 
						mCurrentMode + 90,
						98,
						0);
				if (result == 0) {
					info.setIsSuccess(true);
				}
			}
			DPDBHelper.addSafeModeLog(info);
			if (!isAuto) {
				new Thread() {
					@Override
					public void run() {
						DPFunction.synchPhoneSafeMode();
						DPFunction.synchRoomSafeMode();
					};
				}.start();
			}
		}
		oldState = mConnection; //初始默认上一次接法是默认状态
	}

	/**
	 * 设置安防各个探头接法-高低电平
	 * @param connection
	 */
	public void changeSafeConnection(long connection) {
		DPDBHelper.setSefeConnection(connection);
		mConnection = DPDBHelper.getSefeConnection();
		oldState = mConnection;
	}

	@Override
	public void onDestroy() {
		MyLog.print(TAG, "onDestroy");
		mInstance = null;
		DPSafe.unInitDPSafe();
		if (mGpioSet != null) {
			mGpioSet.UninitGpio(mDoorbell_H17);
			mGpioSet = null;
		}
		if (mSafeScheduled != null) {
			mSafeScheduled.shutdown();
			mSafeScheduled = null;
		}

		if (mAlarmPlayer != null) {
			if (mAlarmID != 0) {
				mAlarmPlayer.stop(mAlarmID);
				mAlarmID = 0;
			}
			mAlarmPlayer.release();
			mAlarmPlayer = null;
		}
		if (mSafeHandler != null) {
			mSafeHandler.getLooper().quit();
			mSafeHandler.removeCallbacksAndMessages(null);
			mHandlerThread.quit();
			mSafeHandler = null;
		}
		if (mHandler != null) {
			mHandler.removeCallbacksAndMessages(null);
			mHandler = null;
		}
		super.onDestroy();
	}
}
