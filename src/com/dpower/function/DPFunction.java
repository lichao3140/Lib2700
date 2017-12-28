package com.dpower.function;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.android.api.UhomeApi;
import org.android.netcfgsdk.NetCfgManager;
import org.android.talkserversdk.LockParam;
import org.android.talkserversdk.TalkManager;
import org.android.talkserversdk.TalkerCallback;

import com.dpower.cloudintercom.Alarm;
import com.dpower.cloudintercom.CloudIntercom;
import com.dpower.cloudintercom.CloudIntercomCallback;
import com.dpower.cloudintercom.Constant;
import com.dpower.cloudintercom.Message;
import com.dpower.cloudintercom.Visit;
import com.dpower.domain.AddrInfo;
import com.dpower.domain.AlarmInfo;
import com.dpower.domain.AlarmLog;
import com.dpower.domain.AlarmNameInfo;
import com.dpower.domain.AlarmTypeInfo;
import com.dpower.domain.AlarmVideo;
import com.dpower.domain.CallInfo;
import com.dpower.domain.MessageInfo;
import com.dpower.domain.RoomNumInfo;
import com.dpower.domain.SafeModeInfo;
import com.dpower.util.CommonUT;
import com.dpower.util.ConstConf;
import com.dpower.util.DPDBHelper;
import com.dpower.util.MyLog;
import com.dpower.util.ProjectConfigure;
import com.example.dpservice.CallReceiver;
import com.example.dpservice.DPIntercomService;
import com.example.dpservice.DPSafeService;
import com.example.dpservice.JniPhoneClass;
import com.example.dpservice.PCToolService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.net.ethernet.EthernetDevInfo;
import android.os.Build;
import android.text.TextUtils;
import android.text.format.Time;

/**
 * sdk面对开发者的接口及配置信息，单例
 */
public class DPFunction {
	private static final String TAG = "DPFunction";
	
	private static Object mutex = new Object(); // 面向开发者的接口加锁

	/** 触发报警 ,附加消息ALARM_ID */
	public static final String ACTION_ALARMING = "action.intent.ALARMING";
	
	/** 正在报警的id */
	public static final String ALARM_ID = "alarmID";

	/** 解除报警 */
	public static final int DISALARMING = -1;

	/** 安防模式改变 */
	public static final String ACTION_SAFE_MODE = "action.intent.SAFE_MODE";
	
	/** 对讲通话id */
	public static final String SESSION_ID = "sessionID";
	
	/** 对讲对方房号 */
	public static final String REMOTE_CODE = "remoteCode";
	
	/** 对讲消息类型 */
	public static final String MSG_TYPE = "msgType";
	
	/** 对讲消息内容 */
	public static final String MSG_CONTENT = "msgContent";
	
	/** 智能家居模式改变 */
	public static final String ACTION_SMART_HOME_MODE = "action.intent.SMART_HOME_MODE";

	/** 更新网络配置表 */
	public static final String ACTION_UPDATE_NETCFG = "manager.update.netcfg";

	/** 管理中心升级apk */
	public static final String ACTION_UPDATE_APK = "manager.update.apk";

	/** 管理中心同步时间 */
	public static final String ACTION_UPDATE_TIME = "manager.update.time";

	/** 管理中心升级广告 */
	public static final String ACTION_UPDATE_ADVERTISEMENT = "manager.update.advertisement";

	/** 管理中心更新天气 */
	public static final String ACTION_UPDATE_WEATHER = "manager.update.weather";

	/** */
	public static final String ACTION_UPDATE_GUARD = "manager.update.guard";
	
	/** 云对讲登录状态改变 */
	public static final String ACTION_CLOUD_LOGIN_CHANGED = "action.intent.CLOUD_LOGIN_CHANGED";
	
	/** 手机绑定或者解绑设备 */
	public static final String ACTION_CLOUD_BIND_CHANGED = "action.intent.CLOUD_BIND_CHANGED";
	
	public static int[] videoAreaCallInUnit = { 0, 0, 600, 480 }; // 默认视频显示区域
	public static boolean phoneAccept = false;
	public static boolean isCallAccept = false;
	public static Class<?> alarmActivity = null;
	public static Class<?> callInFromDoorActivity = null;
	public static Class<?> callInActivity = null;
	
	private static final String DPSAFESERVICE_NULL = "DPSafeService is not runing";

	/** 与其他设备通信 */
	private static TalkManager mTalkManager = null;

	/** 对网络配置表信息操作 */
	private static NetCfgManager mNetCfgManager = null;

	/** 网络配置表版本号 */
	private static int mNetCfgVer = 0;

	/** 与其他设备通话 */
	private static JniPhoneClass mJniPhoneClass = null;
	private static UhomeApi mUhomeApi = null;
	private static CallInfo mSeeInfo = null;
	private static List<CallInfo> mCallOutSessionIDList = null;
	private static List<CallInfo> mCallInSessionIDList = null;
	private static boolean mIsAlarming = false; // 是否正在报警
	private static boolean mAlarmActivityState = false; // 报警界面是否已经运行
	private static boolean mIsCallPhone = false;
	private static SoundPool mSoundPool = null;
	private static int mStreamID = 0;
	private static File mFromFile;
	private static File mToFile;
	private static Context mContext = null;

	public static Context getContext() {
		return mContext;
	}

	/**
	 * 初始化后台服务，以及相应的配置,销毁的时候，需要执行deinit() 此方法需耗时10s左右，请在10s之后再进行其他操作
	 * 
	 * @param context
	 *            此上下文需要一直存在，不可被destroy掉
	 * @return int 返回0正常，其他则异常
	 */
	public static int init(Context context) {
		synchronized (mutex) {
			MyLog.setLogPrint(true);
			MyLog.print(TAG, "init开始");
			mContext = context;
			Time time = new Time();
			time.setToNow();
			if (time.year < 2016) {
				setTime("2016-01-01 00:00:00");
			}
			
			mUhomeApi = new UhomeApi(mContext);
			mSeeInfo = new CallInfo();
			mCallOutSessionIDList = Collections
					.synchronizedList(new ArrayList<CallInfo>());
			mCallInSessionIDList = Collections
					.synchronizedList(new ArrayList<CallInfo>());
			mContext.startService(new Intent(mContext, DPSafeService.class)); // 启动安防服务
			mContext.startService(new Intent(mContext, DPIntercomService.class)); // 启动对讲服务
			mContext.startService(new Intent(mContext, PCToolService.class)); // 启动PC工具服务
			mTalkManager = new TalkManager(mTalkerCallback);
			if (mTalkManager == null) {
				return -2;
			}
			mNetCfgManager = NetCfgManager.getInstance();
			if (mNetCfgManager == null) {
				MyLog.print("NetCfgManager init() fail");
				return -3;
			}
			mNetCfgVer = mNetCfgManager.InitNetcfgFile(ConstConf.NET_CFG_PATH);
			if (mNetCfgVer == 0) {
				MyLog.print("Init NetcfgFile fail");
				return -4;
			}
			mJniPhoneClass = JniPhoneClass.getInstance();
			if (mJniPhoneClass == null) {
				return -5;
			}
			String saveCode = DPDBHelper.getRoomCode();
			MyLog.print("saveCode - > " + saveCode);
			int result = changeRoomCode(saveCode);
			if (result != 0) {
				MyLog.print(MyLog.ERROR, "初始化房号失败");
				return -6;
//				String defaultCode = getDefaultCode(1);
//				if (defaultCode == null) {
//					MyLog.print("getDefaultCode error");
//					return -6;
//				}
//				result = changeRoomCode(defaultCode);
			}
			if (result == 0) {
				MyLog.print("start TalkManager service");
				TalkManager.StartManageClient();
//				TalkManager.StartPCServer();
				TalkManager.StartRoomServer();
				if (ProjectConfigure.needCloudIntercom) {
					MyLog.print(TAG, "启动云对讲");
					if (ProjectConfigure.project == 3) {
						setIp(ConstConf.LAN_NETWORK_CARD_ETH1);
						CloudIntercom.setEthernet(true);
					}
					CloudIntercom.setJNILogPrint(false);
					CloudIntercom.init(mContext, mCloudIntercomCallback);
				}
				return 0;
			}
			return -7;
		}
	}
	
	/**
	 * 获取默认设备的号码
	 * @param type 1：室内机、2：单元门口机、3：别墅门口机、6：警卫机、7:大门口机、8：管理中心
	 * @return String null-失败，非null-默认设备的号码
	 */
	public static String getDefaultCode(int type) {
		ArrayList<AddrInfo> list = getDefaultInfo(type);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list.get(0).getCode();
	}
	
	/**
	 * 获取默认设备的信息
	 * @param type 1：室内机、2：单元门口机、3：别墅门口机、6：警卫机、7:大门口机、8：管理中心
	 * @return null-失败，非null-默认设备的信息
	 */
	public static ArrayList<AddrInfo> getDefaultInfo(int type) {
		String defaultInfo = mNetCfgManager.GetDefaultTerm(type);
		if (defaultInfo == null) {
			return null;
		}
		ArrayList<AddrInfo> list = AddrInfo.parsingAddrInfo(defaultInfo);
		if (list == null || list.isEmpty()) {
			return null;
		}
		return list;
	}

	/**
	 * 结束服务，清理变量
	 */
	public static void deinit() {
		synchronized (mutex) {
			mContext.stopService(new Intent(mContext, DPSafeService.class)); // 结束安防服务
			mContext.stopService(new Intent(mContext, DPIntercomService.class)); // 结束对讲服务
			mContext.stopService(new Intent(mContext, PCToolService.class)); // 结束PC工具服务
			TalkManager.StopManageClient();
			TalkManager.StopRoomServer();
			if (ProjectConfigure.needCloudIntercom) {
				CloudIntercom.deinit();
			}
			mCallOutSessionIDList.clear();
			mCallInSessionIDList.clear();
			if (mNetCfgManager != null) {
				mNetCfgManager.ReleaseNetcfg();
				mNetCfgManager = null;
			}
			isCallAccept = false;
			mIsAlarming = false;
			mAlarmActivityState = false;
		}
	}
	
	// TODO-----------------------------通信相关-----------------------------

	private static TalkerCallback mTalkerCallback = new TalkerCallback() {

		@Override
		public void SynchSuccessed() {
			MyLog.print("TalkerCallback SynchSuccessed");
			// 同步报警记录
			ArrayList<AlarmLog> alarmLogs = DPDBHelper.getAlarmLogList();
			if (alarmLogs != null) {
				for (int i = 0; i < alarmLogs.size(); i++) {
					if (!alarmLogs.get(i).getIsSuccess()) {
						int delayTime[] = DPDBHelper.getAlarmDelayTime(DPDBHelper.getSafeMode());
						String alarmRoom; //包括防区号和防区的名称
						int areaNameValue = getAlarmAreaNameList().get(
								alarmLogs.get(i).getAreaName()).value;
						if (areaNameValue < 10) {
							alarmRoom = alarmLogs.get(i).getAreaNum() + "0" + areaNameValue;
						} else {
							alarmRoom = Integer.toString(alarmLogs.get(i).getAreaNum()) + areaNameValue;
						}
						int result = TalkManager.toManageAlarm(
								CommonUT.formatTime(alarmLogs.get(i).getTime()), 
								Integer.parseInt(alarmRoom),
								getAlarmTypeNameList().get(alarmLogs.get(i).getAreaType()).value,
								delayTime[alarmLogs.get(i).getAreaNum() - 1]);
						if (result == 0) {
							alarmLogs.get(i).setIsSuccess(true);
							DPDBHelper.modifyAlarmLog(alarmLogs.get(i));
							MyLog.print("toManageAlarm Success");
						}
					}
				}
			}
			
			if (ProjectConfigure.project == 2) {
				// 同步安防记录
				ArrayList<SafeModeInfo> list = DPDBHelper.getSafeModeInfoList();
				if (list != null) {
					for (int i = 0; i < list.size(); i++) {
						if (!list.get(i).getIsSuccess()) {
							int result = TalkManager.toManageAlarm(
									CommonUT.formatTime(list.get(i).getTime()), 
									list.get(i).getMode() + 90,
									98,
									0);
							if (result == 0) {
								list.get(i).setIsSuccess(true);
								DPDBHelper.modifySafeModeLog(list.get(i));
								MyLog.print("toManageSafeMode Success");
							}
						}
					}
				}
				
				// 同步门口机呼叫记录
				ArrayList<CallInfo> infos = DPFunction.getCallLogList(
						CallInfo.CALL_IN_ACCEPT | CallInfo.CALL_IN_UNACCEPT);
				if (infos == null) {
					return;
				}
				for (int i = 0; i < infos.size(); i++) {
					if (!infos.get(i).getIsSuccess()) {
						boolean isDoor = false;
						int type = 0;
						if (infos.get(i).getType() == CallInfo.CALL_IN_UNACCEPT
								&& infos.get(i).isDoor()) {
							isDoor = true;
							type = 1;
						} else if (infos.get(i).isDoor()) {
							isDoor = true;
							type = 0;
						}
						if (isDoor) {
							int result = TalkManager.toManageDoorCall(infos.get(i).getRemoteCode(), 
									CommonUT.formatTime(infos.get(i).getStartTime()), type);
							if (result == 0) {
								infos.get(i).setIsSuccess(true);
								DPDBHelper.modifyCallLog(infos.get(i));
								MyLog.print("toManageDoorCall Success");
							}
						}
					}
				}
			}
		}
		
		@Override
		public void SetTime(String dateTime) {
			setTime(dateTime);
		}

		@Override
		public void UpdateInfoMsg(String info) {
			if (TextUtils.isEmpty(info)) {
				return;
			}
			String saveTime = null;
			boolean personal = true;
			String type = getXmlVal("type", info);
			if (type == null) {
				personal = false;
			}
			int count = 0;
			while (true) {
				int start = info.indexOf("<info>");
				int end = info.indexOf("</info>");
				if (start == -1 || end == -1) {
					break;
				}
				String infoItem = new String(info.substring(start + 6, end));
				MessageInfo msgInfo = new MessageInfo();
				msgInfo.setPersonal(personal);
				String val = getXmlVal("time", infoItem);
				if (val != null) {
					msgInfo.setTime(val);
					// 保存最后公共信息时间,有多条消息时，第1条为最后时间
					if (!personal && saveTime == null) {
						saveTime = val;
						DPDBHelper.setUpdateMsgTime(saveTime);
					}
				}
				val = getXmlVal("title", infoItem);
				if (TextUtils.isEmpty(val)) {
					MyLog.print(MyLog.ERROR, "信息标题为空");
					return;
				}
				msgInfo.setTitle(val);
				val = getXmlVal("body", infoItem);
				if (val != null) {
					msgInfo.setBody(val);
				}
				val = getXmlVal("url", infoItem);
				if (val != null) {
					val = val.replace("\\", File.separator);
					if (val.contains("htm")) {
						File destDir = new File(ConstConf.MESSAGE_PATH);
						if (!destDir.exists()) {
							destDir.mkdirs();
						}
						msgInfo.setIsJpg(false);
						String resName = msgInfo.getTime().replaceAll(":", "-") + ".htm";
						MyLog.print(TAG, "resName = " + resName);
						// 对下载地址包含中文的处理
						try {
							val = URLEncoder.encode(val, "UTF-8");
							val = val.replaceAll("%3A", ":").replaceAll("%2F", "/"); 
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						boolean result = CommonUT.httpDownload(val, ConstConf.MESSAGE_PATH 
								+ File.separator + resName);
						if (result) {
							msgInfo.setResName(resName);
						}
					}
				}
				val = getXmlVal("jpg", infoItem);
				if (val != null) {
					val = val.replace("\\", File.separator);
					if (val.contains("jpg")) {
						File destDir = new File(ConstConf.MESSAGE_PATH);
						if (!destDir.exists()) {
							destDir.mkdirs();
						}
						msgInfo.setIsJpg(true);
						String resName = msgInfo.getTime().replaceAll(":", "-") + ".jpg";
						MyLog.print(TAG, "resName = " + resName);
						// 对下载地址包含中文的处理
						try {
							val = URLEncoder.encode(val, "UTF-8");
							val = val.replaceAll("%3A", ":").replaceAll("%2F", "/"); 
						} catch (UnsupportedEncodingException e) {
							e.printStackTrace();
						}
						boolean result = CommonUT.httpDownload(val, ConstConf.MESSAGE_PATH 
								+ File.separator + resName);
						if (result) {
							msgInfo.setResName(resName);
							File file = new File(ConstConf.MESSAGE_LITTLE_PATH);
							if (!file.exists()) {
								file.mkdirs();
							}
							FileOutputStream stream = null;
							try {
								String littleImage = ConstConf.MESSAGE_LITTLE_PATH 
										+ File.separator + resName;
								BitmapFactory.Options options = new BitmapFactory.Options();
								options.inSampleSize = 2;
								Bitmap bitmap = BitmapFactory.decodeFile(ConstConf.MESSAGE_PATH 
										+ File.separator + resName, options);
								stream = new FileOutputStream(littleImage);
								bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
							} catch (FileNotFoundException e) {
								e.printStackTrace();
							} finally {
								try {
									if (stream != null) {
										stream.close();
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
				++count;
				DPDBHelper.addMessageLog(msgInfo);
				info = new String(info.substring(end + 7));
			}

			mContext.sendBroadcast(new Intent(MessageInfo.ACTION_MESSAGE));
			// 播放提示音
			playNewMsgRing();
			toPhoneMessage(personal, count);
		}

		@Override
		public void UpdateWeather(String info) {
			// 添加更新天气代码
			// 保存时间
			String time = getXmlVal("time", info);
			if (time != null) {
				DPDBHelper.setUpdateWeatherTime(time);
			}
			mContext.sendBroadcast(new Intent(ACTION_UPDATE_WEATHER));
		}

		@Override
		public void UpdateNetcfg(String info) {
			if (TextUtils.isEmpty(info)) {
				return;
			}
			String tempUrl = getXmlVal("netcfgurl", info);
			if (tempUrl == null) {
				return;
			}
			String url = tempUrl.replace("\\", File.separator);
			if (url == null) {
				return;
			}
			String fileMD5 = getXmlVal("md5", info);
			if (fileMD5 == null) {
				return;
			}
			File destDir = new File(ConstConf.TEMP_PATH);
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			if (!CommonUT.httpDownload(url, ConstConf.NEW_NET_CFG_PATH)) {
				MyLog.print(MyLog.ERROR, TAG, "下载网络配置表失败");
				return;
			}
			// 比较管理中心给MD5值和本机下载的文件的MD5值，不相同则下载的文件有问题
			if (!fileMD5.equals(getFileMD5(ConstConf.NEW_NET_CFG_PATH))) {
				MyLog.print(TAG, "升级网络配置表失败");
				MyLog.print(MyLog.ERROR, TAG, "MD5不相同");
				MyLog.print(MyLog.ERROR, TAG, fileMD5);
				MyLog.print(MyLog.ERROR, TAG, getFileMD5(ConstConf.NEW_NET_CFG_PATH));
			} else {
				from(ConstConf.NEW_NET_CFG_PATH);
				if (to(ConstConf.NET_CFG_PATH)) {
					mContext.sendBroadcast(new Intent(ACTION_UPDATE_NETCFG));
					MyLog.print(TAG, "升级网络配置表成功");
				} else {
					MyLog.print(TAG, "升级网络配置表失败");
				}
			}
			File file = new File(ConstConf.NEW_NET_CFG_PATH);
			if (file.exists()) {
				file.delete();
			}
		}

		@Override
		public void UpdateVersion(String info) {
			if (TextUtils.isEmpty(info)) {
				return;
			}
			String tempUrl = getXmlVal("url", info);
			if (tempUrl == null) {
				return;
			}
			String url = tempUrl.replace("\\", File.separator);
			if (url == null) {
				return;
			}
			String fileMD5 = getXmlVal("md5", info);
			if (fileMD5 == null) {
				return;
			}
			if (!CommonUT.httpDownload(url, ConstConf.UPDATE_PATH)) {
				MyLog.print(MyLog.ERROR, TAG, "下载APK失败");
				return;
			}
			// 比较管理中心给MD5值和本机下载的文件的MD5值，不相同则下载的文件有问题
			if (!fileMD5.equals(getFileMD5(ConstConf.UPDATE_PATH))) {
				MyLog.print(MyLog.ERROR, TAG, "升级APK失败");
				MyLog.print(MyLog.ERROR, TAG, "MD5不相同");
				MyLog.print(MyLog.ERROR, TAG, fileMD5);
				MyLog.print(MyLog.ERROR, TAG, getFileMD5(ConstConf.UPDATE_PATH));
			} else {
				int result = TalkManager.Unpack(ConstConf.UPDATE_PATH, ConstConf.SD_DIR);
				if (result == 0) {
					MyLog.print(MyLog.ERROR, TAG, "解压成功");
					// 发送广播，启动安装
					mContext.sendBroadcast(new Intent(ACTION_UPDATE_APK));
				} else {
					MyLog.print(MyLog.ERROR, TAG, "升级APK失败");
				}
			}
			File file = new File(ConstConf.UPDATE_PATH);
			if (file.exists()) {
				file.delete();
			}
		}

		@Override
		public void UpdateAdvs(String info) {
			Intent intent = new Intent(ACTION_UPDATE_ADVERTISEMENT);
			intent.putExtra(ACTION_UPDATE_ADVERTISEMENT, info);
			mContext.sendBroadcast(intent);
		}

		@Override
		public void UpdateGuard(String info) {
			Intent intent = new Intent(ACTION_UPDATE_GUARD);
			intent.putExtra(ACTION_UPDATE_GUARD, info);
			mContext.sendBroadcast(intent);
		}
		
		@Override
		public void UpdateSystem(String info) {
			if (TextUtils.isEmpty(info)) {
				return;
			}
			String tempUrl = getXmlVal("url", info);
			if (tempUrl == null) {
				return;
			}
			String url = tempUrl.replace("\\", File.separator);
			if (url == null) {
				return;
			}
			MyLog.print(MyLog.ERROR, TAG, "url = " + url);
			if (!CommonUT.httpDownload(url, ConstConf.UPDATE_SYSTEM_PATH)) {
				MyLog.print(MyLog.ERROR, TAG, "下载系统失败");
				return;
			}
			try {
				execCommand("su -c rm /cache/update.zip");
				int result = execCommand("su -c cp " + ConstConf.UPDATE_SYSTEM_PATH 
						+ " " + "/cache/update.zip");
				if (result == 2) {
					MyLog.print(MyLog.ERROR, TAG, "正在打开升级系统的程序");
					execCommand("su -c rm " + ConstConf.UPDATE_SYSTEM_PATH);
					Intent intent = new Intent();
					intent.setComponent(new ComponentName("com.example.hello",
							"com.example.hello.MainActivity"));
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					mContext.startActivity(intent);
				} else {
					MyLog.print(MyLog.ERROR, TAG, "升级系统失败 result = " + result);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void ChangeSafeMode(String info) {
			int model = 1;
			if (info.equals("UnSafe")) {
				model = 1;
			} else if (info.equals("Night")) {
				model = 2;
			} else if (info.equals("Home")) {
				model = 3;
			} else if (info.equals("Leave")) {
				model = 4;
			}
			changeSafeMode(model, true);
		}

		@Override
		public String GetSafeMode() {
			return getStringSafeMode();
		}

		@Override
		public boolean ChangeRoomNum(String newCode) {
			if (changeRoomCode(newCode) == 0) {
				DPDBHelper.clearAccount();
				return true;
			}
			return false;
		}

		@Override
		public String GetLastInfoTime() {
			return DPDBHelper.getUpdateMsgTime();
		}

		@Override
		public String GetWeatherLastTime() {
			return DPDBHelper.getUpdateWeatherTime();
		}

		@Override
		public String GetManageIP() {
			MyLog.print("GetManageIP");
			if (mNetCfgManager == null) {
				return null;
			}
			String managerInfo = mNetCfgManager.ManagerGet(null);
			if (managerInfo != null) {
				ArrayList<AddrInfo> list = AddrInfo
						.parsingAddrInfo(managerInfo);
				if (list == null || list.isEmpty()) {
					MyLog.print("This netcfg is not manager");
				} else {
					MyLog.print("GetManageIP - > " + list.get(0).getIp());
					return list.get(0).getIp();
				}
			}
			MyLog.print("GetManageIP - > " + null);
			return null;
		}

		@Override
		public String GetDevType() {
			PackageManager manager = mContext.getPackageManager();
			PackageInfo info;
			try {
				info = manager.getPackageInfo(mContext.getPackageName(), 0);
				return info.versionName;
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public String GetLoaclCode() {
			return getRoomCode();
		}

		@Override
		public String GetVersion() {
			PackageManager manager = mContext.getPackageManager();
			PackageInfo info;
			try {
				info = manager.getPackageInfo(mContext.getPackageName(), 0);
				return new String(info.versionCode + "");
			} catch (NameNotFoundException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public String GetSysVersion() {
			return Build.DISPLAY;
		}

		@Override
		public String GetNetcfgMD5() {
			return getFileMD5(ConstConf.NET_CFG_PATH);
		}

		@Override
		public void Restart() {
			try {
				Runtime.getRuntime().exec("su -c reboot");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void Reset() {
			
		}

		@Override
		public void OpenFtp() {
			
		}
	};
	
	public static int execCommand(String command) throws IOException {
		Runtime runtime = Runtime.getRuntime();
		Process proc = runtime.exec(command);
		MyLog.print(TAG, "cmd = " + command);

		InputStream inputstream = proc.getInputStream();
		InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
		BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

		String line = "";
		StringBuilder sb = new StringBuilder(line);
		while ((line = bufferedreader.readLine()) != null) {
			sb.append(line);
			sb.append('\n');
			MyLog.print(TAG, line);
		}

		try {
			if (proc.waitFor() != 0) {
				MyLog.print(TAG, "exit value = " + proc.exitValue());
				return 1;
			} else {
				MyLog.print(TAG,"exit value = " + proc.exitValue());
				return 2;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return 3;
		}
	};
	
	/** 增加信息记录到数据库中 */
	public static void addMessageLog(MessageInfo info) {
		DPDBHelper.addMessageLog(info);
	}

	/**
	 * @功能：修改数据库中的信息记录
	 */
	public static void modifyMessageLog(MessageInfo info) {
		DPDBHelper.modifyMessageLog(info);
	}

	/**
	 * @功能：删除数据库中指定ID的消息记录
	 * @参数：int db_id - 数据库中自动生成的_id
	 */
	public static void deleteMessageLog(int db_id) {
		DPDBHelper.deleteMessageLog(db_id);
	}

	/**
	 * @功能：删除对应类型的所有消息记录
	 * @参数：int type - 1-个人信息，2-公共信息，3-所有信息
	 */
	public static void deleteAllMessageLog(int type) {
		DPDBHelper.deleteAllMessageLog(type);
	}

	/**
	 * @功能：从数据库中获取指定类型的的未读消息个数
	 * @参数：int type PERSONAL、PUBLIC、PERSONAL|PUBLIC
	 */
	public static int getUnReadMessageLogNum(int type) {
		return DPDBHelper.getUnReadMessageLogNum(type);
	}

	/**
	 * @功能：从数据库中获取指定类型的消息记录
	 * @参数：int type PERSONAL、PUBLIC、PERSONAL|PUBLIC
	 */
	public static ArrayList<MessageInfo> getMessageLogList(int type) {
		return DPDBHelper.getMessageLogList(type);
	}
	
	// TODO-----------------------------设置相关-----------------------------
	
	/** 设置报警界面 */
	public static void setAlarmActivity(Class<?> cl) {
		alarmActivity = cl;
	}
	
	/** 设置门口机新呼入后启动的Activity界面 有时门口机和室内机的摄像头编码大小不一样，所以显示的界面不一样，需分开处理 */
	public static void setDoorCallInActivity(Class<?> cl) {
		callInFromDoorActivity = cl;
	}

	/** 设置室内机新呼入后启动的Activity界面 */
	public static void setRoomCallInActivity(Class<?> cl) {
		callInActivity = cl;
	}

	/** 获取警界面是否已经运行 */
	public static boolean getAlarmActivityState() {
		return mAlarmActivityState;
	}

	/** 设置警界面是否已经运行，必须在报警页面结束前还原为false */
	public static void setAlarmActivityState(boolean AlarmActivityState) {
		mAlarmActivityState = AlarmActivityState;
	}
	
	private static void playNewMsgRing() {
		if (mSoundPool != null) {
			if (mStreamID != 0) {
				mSoundPool.stop(mStreamID);
				mStreamID = 0;
			}
			mSoundPool.release();
			mSoundPool = null;
		}
		mSoundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM, 5);
		mSoundPool.load(ConstConf.MSG_RING_PATH, 1);
		mSoundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
			
			@Override
			public void onLoadComplete(SoundPool soundPool, int sampleId,
					int status) {
				int streamID = soundPool.play(1, 1, 1, 0, 0, 1);
				if (streamID == 0) {
					MyLog.print(MyLog.ERROR, "paly MsgRing error");
				}
			}
		});
	}
	
	/**
	 * @功能：查找源字符串中的子字符串所包含的字符串，XML解析键值对
	 * @参数1：String key - 子字符串
	 * @参数2：String src - 源字符串
	 * @返回值：String - key不存在返回null,否则返回key包含的字符串
	 */
	private static String getXmlVal(String key, String src) {
		String strstart = "<" + key + ">";
		String strend = "</" + key + ">";
		int start = src.indexOf(strstart);
		if (start == -1) {
			MyLog.print("getXmlVal -> start key does not exist");
			return null;
		}
		int end = src.indexOf(strend);
		if (end == -1) {
			MyLog.print("getXmlVal -> end key does not exist");
			return null;
		}
		String strval = new String(
				src.substring(start + strstart.length(), end));
		return strval;
	}

	/**
	 * @功能：获取指定路径文件的MD5值
	 * @参数：String filePath - 文件绝对路径
	 * @返回值：String - 文件不存在则返回null,否则返回该文件MD5值
	 */
	private static String getFileMD5(String filePath) {
		File file = new File(filePath);
		if (!file.isFile()) {
			MyLog.print("getFileMD5 -> File does not exist");
			return null;
		}
		MessageDigest digest = null;
		FileInputStream in = null;
		byte[] buffer = new byte[1024];
		int len;
		try {
			digest = MessageDigest.getInstance("MD5");
			in = new FileInputStream(file);
			while ((len = in.read(buffer)) > 0) {
				digest.update(buffer, 0, len);
			}
			in.close();
		} catch (Exception e) {
			MyLog.print("getFileMD5 -> " + e.toString());
			if (in != null) {
				try {
					in.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
			return null;
		}
		return bytesToHexString(digest.digest());
	}

	/**
	 * 将byte[]转换成16进制字符串
	 */
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}
	
	/**
	 * 将16进制字符串转换成byte[]
	 */
	public static byte[] hexStringToBytes(String hexString) {
	    if (TextUtils.isEmpty(hexString)) {
	        return null;
	    }
	    hexString = hexString.toUpperCase();
	    int length = hexString.length() / 2;
	    char[] hexChars = hexString.toCharArray(); 
	    byte[] data = new byte[length];  
	    for (int i = 0; i < length; i++) {
	        int pos = i * 2;
	        data[i] = (byte) (charToByte(hexChars[pos]) << 4 
	        		| charToByte(hexChars[pos + 1])); 
	    }
	    return data;
	}
	
	private static byte charToByte(char c) {   
		return (byte) "0123456789ABCDEF".indexOf(c);   
	} 
	
	/**
	 * 将int转换为占四个字节的byte[]
	 */
	public static byte[] intToBytes( int value ) {
	    byte[] src = new byte[4];
	    src[3] =  (byte) ((value >> 24) & 0xFF);
	    src[2] =  (byte) ((value >> 16) & 0xFF);
	    src[1] =  (byte) ((value >> 8) & 0xFF);
	    src[0] =  (byte) (value & 0xFF);
	    return src;
	}
	
	/**
	 * 将byte[]转换为int
	 */
	public static int bytesToInt(byte[] src, int offset) {
	    int value;
	    value = (int) ((src[offset] & 0xFF)
	            | ((src[offset + 1] & 0xFF) << 8) 
	            | ((src[offset + 2] & 0xFF) << 16) 
	            | ((src[offset + 3] & 0xFF) << 24));
	    return value;
	} 
	
	/**
	* 将文件转换为字节数组
	*/
	public static byte[] fileToBytes(File file) {
		if (file == null || (int)file.length() == 0) {
            return null;
        }
	    byte[] data = null;
	    FileInputStream inputStream = null;
	    ByteArrayOutputStream outputStream = null;
	    try{
	        inputStream = new FileInputStream(file);
	        outputStream = new ByteArrayOutputStream((int)file.length());
	        byte[] buffer = new byte[1024];
	        int len;
	        while ((len = inputStream.read(buffer)) > 0) {
	            outputStream.write(buffer, 0, len);
	        }
	        outputStream.flush();
	        data = outputStream.toByteArray();
	    } catch(Exception e) {
	        e.printStackTrace();
	    } finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    return data;
	}
	
	/**
	* 对字符串进行压缩----"ISO-8859-1"
	*/
	public static String compress(String data) {
		if (data == null) {
			return null;
		}
	    String newData = null;
	    ByteArrayOutputStream outputStream = null;
	    GZIPOutputStream gzip = null;
	    MyLog.print("对字符串进行压缩before:" + data.length());
	    try{
	        outputStream = new ByteArrayOutputStream();
	        gzip = new GZIPOutputStream(outputStream);
	        gzip.write(data.getBytes("ISO-8859-1"));
	        gzip.finish();
	        gzip.flush();
	        outputStream.flush();
	        newData = outputStream.toString("ISO-8859-1");
	        MyLog.print("对字符串进行压缩after:" + newData.length());
	    } catch(Exception e) {
	        e.printStackTrace();
	    } finally {
			try {
				if (gzip != null) {
					gzip.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	    return newData;
	}
	
	/** 
     * 对byte[]进行压缩
     */  
	public static byte[] compress(byte[] data) {  
		if (data == null || data.length == 0) {
            return null;
        }
		byte[] newData = null;
		ByteArrayOutputStream outputStream = null;
	    GZIPOutputStream gzip = null;
		MyLog.print("对byte[]进行压缩before:" + data.length);
        try {
        	outputStream = new ByteArrayOutputStream();
        	gzip = new GZIPOutputStream(outputStream);
            gzip.write(data);
            gzip.finish();
            gzip.flush();
            outputStream.flush();
            newData = outputStream.toByteArray();
            MyLog.print("对byte[]进行压缩after:" + newData.length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
			try {
				if (gzip != null) {
					gzip.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        return newData;
    }  
	
	/** 对byte[]进行解压缩 */
	public static byte[] uncompress(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        byte[] newData = null;
        MyLog.print("对byte[]进行解压缩before:" + data.length);
        ByteArrayOutputStream outputStream = null;
        ByteArrayInputStream inputStream = null;
        GZIPInputStream gzip = null;
        try {
        	outputStream = new ByteArrayOutputStream();
        	inputStream = new ByteArrayInputStream(data);
            gzip = new GZIPInputStream(inputStream);
            byte[] buffer = new byte[1024];
            int len = -1;
            while ((len = gzip.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }
            outputStream.flush();
            newData = outputStream.toByteArray();
            MyLog.print("对byte[]进行解压缩after:" + newData.length);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
				if (gzip != null) {
					gzip.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
        return newData;
    }

	/** 设置房号 */
	public static boolean setRoomCode(String roomCode) {
		return DPDBHelper.setRoomCode(roomCode);
	}
	
	/**
	 * @功能：获取本机房号
	 * @返回值：null或本机房号
	 * */
	public static String getRoomCode() {
		if (mNetCfgManager == null) {
			return null;
		}
		return mNetCfgManager.GetLocalCode();
	}

	/**
	 * @功能：修改房号和对应的IP地址
	 * @参数：String newCode - 新的房号（13位的字符串）
	 * @返回值：int 0-成功，非0失败
	 */
	public static int changeRoomCode(String newCode) {
		synchronized (mutex) {
			MyLog.print("changeRoomCode " + newCode);
			if (newCode == null) {
				MyLog.print(MyLog.ERROR, "changeRoomCode is null");
				return -1;
			}
			if (mNetCfgManager == null) {
				return -1;
			}
			// 判断房号不存在
			if (mNetCfgManager.TermGet(newCode) == null) {
				MyLog.print(MyLog.ERROR, "newCode " + newCode + " is not exist");
				return -1;
			}
			String newInfo = mNetCfgManager.InitTerm(newCode);
			String oldCode = DPDBHelper.getRoomCode();
			if (newInfo == null) {
				MyLog.print(MyLog.ERROR, "InitTerm " + newCode + " error");
				mNetCfgManager.InitTerm(oldCode);
				return -2;
			}
			ArrayList<AddrInfo> list = AddrInfo.parsingAddrInfo(newInfo);
			if (list == null || list.size() != 1) {
				MyLog.print(MyLog.ERROR, "Should not be the case!");
				mNetCfgManager.InitTerm(oldCode);
				return -3;
			}
			AddrInfo curInfo = list.get(0);
			if (!setIp(ConstConf.LAN_NETWORK_CARD, curInfo.getIp(),
					curInfo.getMask(), curInfo.getGw(), null)) {
				MyLog.print(MyLog.ERROR, "setIp - >" + curInfo.toString());
				mNetCfgManager.InitTerm(oldCode);
				return -4;
			}
			if (mJniPhoneClass != null) {
				mJniPhoneClass.UnInit();
			}
			if (mJniPhoneClass.Init(curInfo.getCode(), curInfo.getIp())) {
				// 只有主分机才启动内网功能
				DPDBHelper.setRoomCode(mNetCfgManager.GetLocalCode());
				toStartLogin();
				return 0;
			}
			MyLog.print(MyLog.ERROR, "JniPhoneClass Init error");
			String oldInfo = mNetCfgManager.InitTerm(oldCode);
			list = AddrInfo.parsingAddrInfo(oldInfo);
			if (list != null && list.size() == 1) {
				curInfo = list.get(0);
				setIp(ConstConf.LAN_NETWORK_CARD, curInfo.getIp(),
						curInfo.getMask(), curInfo.getGw(), null);
			}
			return -5;
		}
	}
	
	public static void setDefauleWanIP() {
		MyLog.print("setDefauleWanIP - > " + DPDBHelper.isWanDHCP());
		if (DPDBHelper.isWanDHCP()) {
			setIp(ConstConf.WAN_NETWORK_CARD);
		} else {
			setIp(ConstConf.WAN_NETWORK_CARD, DPDBHelper.getWanIP(),
					DPDBHelper.getWanMask(), DPDBHelper.getWanGw(),
					DPDBHelper.getWanDNS());
		}
	}

	/**
	 * @方法名：setIp
	 * @功能：设置指定网卡的动态IP地址
	 * @参数1：String netCardName - 网卡名称
	 */
	private static boolean setIp(String netCardName) {
		if (TextUtils.isEmpty(netCardName)) {
			return false;
		}
		EthernetDevInfo devInfo = new EthernetDevInfo();
		devInfo.setIfName(netCardName);
		devInfo.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_DHCP);
		mUhomeApi.setEthernetInfo(devInfo);
		if (netCardName.equals(ConstConf.WAN_NETWORK_CARD)) {
			DPDBHelper.setWanDHCP(true);
		}
		return true;
	}
	
	/**
	 * @功能：设置指定网卡的IP地址
	 * @参数1：String netCardName - 网卡名称
	 * @参数2：String ip - ip地址
	 * @参数3：String mask - 子网掩码
	 * @参数4：String gw - 网关
	 */
	private static boolean setIp(String netCardName, String ip, String mask,
			String gw, String dns) {
		if (TextUtils.isEmpty(ip)) {
			return false;
		}
		EthernetDevInfo devInfo = new EthernetDevInfo();
		if (!TextUtils.isEmpty(gw)) {
			devInfo.setGateWay(gw);
		}
		if (!TextUtils.isEmpty(mask)) {
			devInfo.setNetMask(mask);
		}
		if (!TextUtils.isEmpty(dns)) {
			devInfo.setDnsAddr(dns);
		} else {
			devInfo.setDnsAddr(gw);
		}

		devInfo.setIfName(netCardName);
		devInfo.setIpAddress(ip);
		devInfo.setConnectMode(EthernetDevInfo.ETHERNET_CONN_MODE_MANUAL);
		mUhomeApi.setEthernetInfo(devInfo);
		if (netCardName.equals(ConstConf.WAN_NETWORK_CARD)) {
			DPDBHelper.setWanDHCP(false);
			DPDBHelper.setWanIP(ip);
			DPDBHelper.setWanMask(mask);
			DPDBHelper.setWanGw(gw);
			if (!TextUtils.isEmpty(dns)) {
				DPDBHelper.setWanDNS(dns);
			}
		}
		MyLog.print("netCardName:" + netCardName + ",ip:" + ip + ",mask:"
				+ mask + ",gw:" + gw);
		return true;
	}

	/**
	 * 设置门口机用户开锁密码
	 * 
	 * @param doorIpAddr
	 *            门口机ip
	 * @param pwd
	 *            新密码
	 * @return
	 */
	public static int toDoorModifyPassWord(String doorIpAddr, String pwd) {
		long roomID = CodetoID(getRoomCode());
		int ret = TalkManager.toDoorModifyPassWord(doorIpAddr, roomID, pwd,
				true);
		return ret;
	}
	
	/** 根据房号获取对应id */
	private static long CodetoID(String code) {
		if (mNetCfgManager == null) {
			return 0;
		}
		return mNetCfgManager.Code2ID(code);
	}

	/**
	 * 静态方法名：getNetCfgVer
	 * 
	 * @功能：获取网络配置表版本号
	 * @返回值：0或网络配置表版本号
	 * */
	public static int getNetCfgVer() {
		return mNetCfgVer;
	}
	
	/**
	 * @功能：根据房号获取网络配置表中对应的IP地址信息
	 * @参数：String code - 房号
	 * @返回值：AddrInfo IP地址信息
	 */
	public static AddrInfo getAddrInfo(String code) {
		if (mNetCfgManager == null) {
			return null;
		}
		if (code == null) {
			return null;
		}
		if (getRoomCode() == null) {
			return null;
		}
		if (code.length() == getRoomCode().length()) {
			String info = mNetCfgManager.TermGet(code);
			if (info == null) {
				return null;
			}
			info = getXmlVal("netcfg", info);
			if (info == null) {
				return null;
			}
			String strnum = getXmlVal("num", info);
			if (strnum == null) {
				return null;
			}
			int num = Integer.valueOf(strnum).intValue();
			if (num == 1) {
				ArrayList<AddrInfo> list = AddrInfo
						.parsingAddrInfo(info);
				if (list != null && !list.isEmpty()) {
					return list.get(0);
				}
			}
		}
		return null;
	}

	/**
	 * @功能：根据房号获取网络配置表中对应的中文名称
	 * @参数1：String code -房号
	 * @参数2：boolean isAbsolute - true-完整名称 false-相对本机房号的名称
	 * @返回值：String 网络配置表中对应的中文名称
	 * @eg：code2Name("1010101010101",ture) - 1区1栋1单元0101室1号室内分机
	 * @备注：网络配置表中只有中文名称，故要获取到英文名称只能自己根据房号另写方法
	 */
	public static String code2Name(String code, boolean isAbsolute) {
		if (mNetCfgManager == null) {
			return null;
		}
		if (code != null) {
			if (getRoomCode() != null) {
				if (code.length() == getRoomCode().length()) {
					long id = mNetCfgManager.Code2ID(code);
					if (id != 0) {
						return mNetCfgManager.ID2Name(id, isAbsolute);
					}
				}
			}
		}
		return null;
	}

	/**
	 * @功能：根据房号获取设备类型
	 * @参数：String code -房号
	 * @返回值：int 1-室内机，2-单元门口机，3-别墅门口机（小门口机），4-保安机，6-警卫员机，7-围墙机，8-管理中心
	 */
	public static int code2Type(String code) {
		if (mNetCfgManager == null) {
			MyLog.print("mNetCfgManager is not init<>");
			return 0;
		}
		if (code == null) {
			MyLog.print("param is null");
			return 0;
		}
		return mNetCfgManager.Code2Type(code);
	}

	/**
	 * 获取解析配置表中的所有的区、栋、单元、房号、分机号。
	 * 
	 * @return 失败返回null;
	 */
	public static ArrayList<RoomNumInfo> getRoomNumInfos() {
		String string = mNetCfgManager.GetAllTerm();
		return RoomNumInfo.parsingRoomNumInfo(string);
	}

	/**
	 * 呼梯
	 * @param doorIp
	 *            门口机ip
	 * @return 0-成功,非0-失败
	 */
	public static int toDoorCallEvt(String doorIp) {
		return TalkManager.toDoorCallEvt(doorIp,
				mNetCfgManager.Code2ID(getRoomCode()));
	}

	/**
	 * 设置门口机开锁设置（别墅机号、开锁延时、开锁电平、门磁延时）
	 * 
	 * @param iNum
	 *            别墅机号
	 * @param LockDelay
	 *            开锁延时(默认为-1)
	 * @param LockLevel
	 *            开锁电平(默认为-1)
	 * @param MagicDelay
	 *            门磁延时(默认为-1)
	 * @return 0-成功,非0-失败
	 */
	public static int toDoorSetLockParam(int iNum, int LockDelay,
			int LockLevel, int MagicDelay) {
		LockParam lockParam = new LockParam();
		String localCode = getRoomCode();
		String doorInfo = mNetCfgManager.SecDoorGet(localCode);
		ArrayList<AddrInfo> list = AddrInfo.parsingAddrInfo(doorInfo);
		if (list == null || iNum > list.size()) {
			return -1;
		}
		// 获取小门口机IP
		String doorIp = list.get(iNum - 1).getIp();
		// 获取室内机当前CODE信息
		long roomID = mNetCfgManager.Code2ID(getRoomCode());
		MyLog.print("toDoorSetLockParam  doorIp " + doorIp + ", roomID "
				+ roomID);
		MyLog.print("toDoorSetLockParam  LockDelay " + LockDelay
				+ ", LockLevel " + LockLevel + ", MagicDelay " + MagicDelay);
		if (LockDelay != -1)
			lockParam.setLockDelay(LockDelay);// 开锁延时
		if (LockLevel != -1)
			lockParam.setLockLevel(LockLevel);// 开锁电平
		if (MagicDelay != -1)
			lockParam.setMagicDelay(MagicDelay);// 门磁延时
		int result = TalkManager.toDoorSetLockParam(doorIp, roomID, lockParam);
		if (result == 0) {
			return 0;
		} else {
			return -2;
		}
	}

	/**
	 * 修改门口机房号
	 * 
	 * @param doorCode
	 *            门口机房号
	 * @return 0-成功,非0-失败
	 */
	public static int toDoorSetNum(String doorCode) {
		String info = mNetCfgManager.TermGet(doorCode);
		ArrayList<AddrInfo> list = AddrInfo.parsingAddrInfo(info);
		if (list == null || list.size() != 1) {
			MyLog.print("room_code_not_exist");
			return -1;
		}
		// 获取小门口机默认CODE信息
		String defaultInfo = mNetCfgManager.GetDefaultTerm(3);
		MyLog.print("info:" + defaultInfo);
		list = AddrInfo.parsingAddrInfo(defaultInfo);
		if (list == null || list.size() != 1) {
			MyLog.print("GetDefaultTerm 3 error!");
			return -2;
		}
		String doorIp = list.get(0).getIp();
		MyLog.print("doorIp:" + doorIp);
		// 获取室内机当前CODE信息
		long roomID = mNetCfgManager.Code2ID(getRoomCode());
		MyLog.print("roomID:" + roomID);
		// 获取新的小门口机CODE并转换成ID
		if (!doorCode.startsWith("3") && doorCode.length() != 13) {
			MyLog.print("error 1");
			return -3;
		}
		MyLog.print("doorCode:" + doorCode);
		long doorID = mNetCfgManager.Code2ID(doorCode);
		MyLog.print("doorID:" + doorID);
		if (doorIp == null || roomID == 0 || doorID == 0) {
			MyLog.print("door code error");
			return -4;
		}
		int result = TalkManager.toDoorSetNum(doorIp, roomID, doorID);
		if (result == 0) {
			return 0;
		} else {
			return -5;
		}
	}
	
	/**
	 * @静态方法名：setTime
	 * @功能：设置时间
	 * @参数：String dateTime - "2013-10-23 11:28:02"
	 */
	private static void setTime(String dateTime) {
		boolean result = CommonUT.setSystemTime(dateTime);
		if (result) {
			mContext.sendBroadcast(new Intent(ACTION_UPDATE_TIME));
		}
	}

	/**
	 * @功能：设置留言模式
	 * @参数 int mode 0-不留言，1-默认留言，2-业主留言
	 */
	public static boolean setMessageMode(int mode) {
		return DPDBHelper.setMessageMode(mode);
	}

	/**
	 * @功能：获取留言模式
	 * @return 0-不留言，1-默认留言，2-业主留言
	 */
	public static int getMessageMode() {
		return DPDBHelper.getMessageMode();
	}
	
	/**
	 * @功能：设置智能家居模式
	 * @参数 int mode 1=在家，2=就寝，3=聚餐，4=影视，5=娱乐，6=全关
	 */
	public static void setSmartHomeMode(int mode) {
		DPDBHelper.setSmartHomeMode(mode);
		mContext.sendBroadcast(new Intent(ACTION_SMART_HOME_MODE));
		synchPhoneSmartHomeMode();
	}

	/**
	 * 获取智能家居模式
	 * @return 1=在家，2=就寝，3=聚餐，4=影视，5=娱乐，6=全关
	 */
	public static int getSmartHomeMode() {
		return DPDBHelper.getSmartHomeMode();
	}
	
	/**
	 * 获取智能家居模式
	 * @return Home, Night, Dining, Video, Disco, AllClose
	 */
	public static String getStringSmartHomeMode() {
		String smartHomeMode = null;
		switch (DPDBHelper.getSmartHomeMode()) {
			case 1:
				smartHomeMode = "Home";
				break;
			case 2:
				smartHomeMode = "Night";
				break;
			case 3:
				smartHomeMode = "Dining";
				break;
			case 4:
				smartHomeMode = "Video";
				break;
			case 5:
				smartHomeMode = "Disco";
				break;
			case 6:
				smartHomeMode = "AllClose";
				break;
			default:
				smartHomeMode = "Home";
				break;
		}
		return smartHomeMode;
	}

	/** 设置室内机工程设置密码 **/
	public static boolean setPsdProjectSetting(String projectPsd) {
		return DPDBHelper.setPsdProjectSetting(projectPsd);
	}

	/** 获取室内机工程设置密码 */
	public static String getPsdProjectSetting() {
		return DPDBHelper.getPsdProjectSetting();
	}

	/** 保存亮度 */
	public static boolean setBrightness(int value) {
		return DPDBHelper.setBrightness(value);
	}
	
	/** 获取亮度 */
	public static int getBrightness() {
		return DPDBHelper.getBrightness();
	}

	/** 保存对比度 */
	public static boolean setContrast(int value) {
		return DPDBHelper.setContrast(value);
	}
	
	/** 获取对比度 */
	public static int getContrast() {
		return DPDBHelper.getContrast();
	}

	/** 保存饱和度 */
	public static boolean setSaturability(int value) {
		return DPDBHelper.setSaturability(value);
	}
	
	/** 获取饱和度 */
	public static int getSaturability() {
		return DPDBHelper.getSaturability();
	}

	/** 保存色相 */
	public static boolean setHue(int value) {
		return DPDBHelper.setHue(value);
	}
	
	/** 获取色相 */
	public static int getHue() {
		return DPDBHelper.getHue();
	}
	
	/** 获取是否呼叫转移到手机 */
	public static boolean getCallToPhone() {
		return DPDBHelper.getCallToPhone();
	}
	
	/** 设置是否呼叫转移到手机 */
	public static void setCallToPhone(boolean isToPhone) {
		DPDBHelper.setCallToPhone(isToPhone);
	}
	
	private static void from(String path) {
		mFromFile = new File(path);
	}

	private static boolean to(String filePath) {
		boolean result = false;
		if (mFromFile == null || !mFromFile.exists()) {
			if (mFromFile != null) {
				MyLog.print(MyLog.ERROR, TAG, "文件拷贝失败：mFromFile不存在 " 
						+ mFromFile.getAbsolutePath());
			} else {
				MyLog.print(MyLog.ERROR, TAG, "文件拷贝失败：mFromFile null ");
			}
			return result;
		}
		InputStream inputStream = null;
		FileOutputStream outputStream = null;
		try {
			mToFile = new File(filePath);
			inputStream = new FileInputStream(mFromFile);
			outputStream = new FileOutputStream(mToFile);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, len);
			}
			outputStream.flush();
			outputStream.getFD().sync();
			MyLog.print(TAG, "文件拷贝成功：" + filePath);
			result = true;
		} catch (Exception e) {
			e.printStackTrace();
			MyLog.print(MyLog.ERROR, TAG, "文件拷贝失败：" + filePath);
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/**
	 * 添加联系地址
	 * @param address
	 * @return 0-添加成功, -1 已到帐号的最大数量, -2 帐号已存在
	 */
	public static int addAddress(String address) {
		return DPDBHelper.addAddress(address);
	}
	
	public static void clearAddress() {
		DPDBHelper.clearAddress();
	}
	
	public static void delAddress(String address) {
		DPDBHelper.delAddress(address);
	}
	
	public static List<String> getAddressList() {
		return DPDBHelper.getAddressList();
	}
	
	// TODO-----------------------------安防相关-----------------------------
	
	public static int getTanTouNum() {
		return DPSafeService.tanTouNum;
	}
	
	/**
	 * 导入安防配置文件(改变语言时需要重新导入)
	 */
	public static void loadAlarmFile() {
		if (DPSafeService.getInstance() != null) {
			DPSafeService.getInstance().loadAlarmFile();
		}
	}
	
	/**
	 * 设置默认防区位置(调用生效条件：1.首次启动APP；2.初始化DPFunction之前)
	 * @param area
	 */
	public static void setDefaultAlarmArea(int[] area) {
		DPSafeService.setDefaultAlarmArea(area);
	}
	
	/**
	 * 设置默认防区类型(调用生效条件：1.首次启动APP；2.初始化DPFunction之前)
	 * @param type
	 */
	public static void setDefaultAlarmType(int[] type) {
		DPSafeService.setDefaultAlarmType(type);
	}
	
	/**
	 * 获取当前安防模式
	 * @return UnSafe, Night, Home, Leave
	 */
	public static String getStringSafeMode() {
		String safeMode = null;
		switch (DPDBHelper.getSafeMode()) {
			case ConstConf.UNSAFE_MODE:
				safeMode = "UnSafe";
				break;
			case ConstConf.NIGHT_MODE:
				safeMode = "Night";
				break;
			case ConstConf.HOME_MODE:
				safeMode = "Home";
				break;
			case ConstConf.LEAVE_HOME_MODE:
				safeMode = "Leave";
				break;
			default:
				safeMode = "UnSafe";
				break;
		}
		return safeMode;
	}
	
	/**
	 * @功能：获取安防模式
	 * 0-测试，1-撤防，2-夜间，3-在家，4-离家
	 * @备注：默认为1-撤防模式
	 */
	public static int getSafeMode() {
		return DPDBHelper.getSafeMode();
	}

	/**
	 * @功能：设置安防模式
	 * @参数 int mode 0-测试，1-撤防，2-夜间，3-在家，4-离家
	 */
	public static boolean setSafeMode(int mode) {
		return DPDBHelper.setSafeMode(mode);
	}

	/** 获取当前安防位置 */
	public static int[] getAlarmArea() {
		if (DPSafeService.getInstance() != null) {
			return DPSafeService.getInstance().alarmArea;
		} else {
			MyLog.print(TAG, DPSAFESERVICE_NULL);
			return null;
		}
	}

	/** 获取当前安防类型 */
	public static int[] getAlarmType() {
		if (DPSafeService.getInstance() != null) {
			return DPSafeService.getInstance().alarmType;
		} else {
			MyLog.print(TAG, DPSAFESERVICE_NULL);
			return null;
		}
	}

	/** 获取安防配置文件中的防区名称 */
	public static List<AlarmNameInfo> getAlarmAreaNameList() {
		if (DPSafeService.getInstance() != null) {
			return DPSafeService.getInstance().alarmAreaNameList;
		} else {
			MyLog.print(TAG, DPSAFESERVICE_NULL);
			return null;
		}
	}

	/** 获取安防配置文件中的防区类型 */
	public static List<AlarmTypeInfo> getAlarmTypeNameList() {
		if (DPSafeService.getInstance() != null) {
			return DPSafeService.getInstance().alarmTypeNameList;
		} else {
			MyLog.print(TAG, DPSAFESERVICE_NULL);
			return null;
		}
	}

	/** 获取当前探头状态 */
	public static long getSafeState() {
		if (DPSafeService.getInstance() != null) {
			return DPSafeService.getInstance().oldState;
		} else {
			MyLog.print(TAG, DPSAFESERVICE_NULL);
			return 0;
		}
	}

	/** 获取指定安防模式的所有状态 */
	public static List<AlarmInfo> getAlarmInfoList(int model) {
		if (DPSafeService.getInstance() != null) {
			return DPSafeService.getInstance().getAlarmInfoList(model);
		} else {
			MyLog.print(TAG, DPSAFESERVICE_NULL);
			return null;
		}
	}

	/**
	 * @功能：判断是否可以设置延时和启用
	 * @备注：priority 0-门铃、1-紧急、2-烟感煤气、3-门磁红外
	 */
	public static boolean isEmergency(int i) {
		if (DPSafeService.getInstance() != null) {
			return DPSafeService.getInstance().isEmergency(i);
		} else {
			MyLog.print(TAG, DPSAFESERVICE_NULL);
			return false;
		}
	}

	/**
	 * 设置全部安防防区
	 * @param alarmArea 安防防区列表
	 */
	public static boolean setAlarmArea(int[] alarmArea) {
		synchronized (mutex) {
			if (DPSafeService.getInstance() != null) {
				return DPSafeService.getInstance().setAlarmArea(alarmArea);
			} else {
				MyLog.print(TAG, DPSAFESERVICE_NULL);
				return false;
			}
		}
	}

	/** 设置特定防区 */
	public static boolean setAlarmArea(int i, int alarmArea) {
		synchronized (mutex) {
			if (DPSafeService.getInstance() != null) {
				return DPSafeService.getInstance().setAlarmArea(i, alarmArea);
			} else {
				MyLog.print(TAG, DPSAFESERVICE_NULL);
				return false;
			}
		}
	}
	
	/**
	 * @功能：设置各个探头防区名称
	 * @param area
	 *            数字对应的名称从配置表中读取
	 * */
	public static boolean setSafeArea(int[] area) {
		return DPDBHelper.setSafeArea(area);
	}

	/** @功能：获取各个探头防区名称 */
	public static int[] getSafeArea() {
		return DPDBHelper.getSafeArea();
	}

	/**
	 * @功能：修改所有的防区类型
	 * @备注：修改后改为默认状态不延时报警和禁用此防区
	 * @描述：紧急、烟感、煤气等类型必须启用并不延时报警时间
	 */
	public static boolean setAlarmType(int[] alarmType) {
		synchronized (mutex) {
			if (DPSafeService.getInstance() != null) {
				return DPSafeService.getInstance().setAlarmType(alarmType);
			} else {
				MyLog.print(TAG, DPSAFESERVICE_NULL);
				return false;
			}
		}
	}

	/**
	 * @功能：修改单个防区类型
	 * @参数1：int i -第几个探头 从0开始
	 * @参数2：int alarmType 防区类型
	 * @备注：修改后改为默认状态不延时报警和禁用此防区
	 * @描述：紧急、烟感、煤气等类型必须启动并不延时报警时间
	 */
	public static boolean setAlarmType(int i, int alarmType) {
		synchronized (mutex) {
			if (DPSafeService.getInstance() != null) {
				return DPSafeService.getInstance().setAlarmType(i, alarmType);
			} else {
				MyLog.print(TAG, DPSAFESERVICE_NULL);
				return false;
			}
		}
	}
	
	/** 设置各个探头类型 */
	public static boolean setSafeType(int[] type) {
		return DPDBHelper.setSafeType(type);
	}

	/** 获取各个探头类型 */
	public static int[] getSafeType() {
		return DPDBHelper.getSafeType();
	}

	/**
	 * 修改室内机的安防模式（同时会写入数据库中）
	 * @param isAuto true-室内机自动同步安防模式, false-手动修改安防模式
	 */
	public static void changeSafeMode(int model, boolean isAuto) {
		synchronized (mutex) {
			if (DPSafeService.getInstance() != null) {
				DPSafeService.getInstance().changeSafeMode(model, isAuto);
			} else {
				MyLog.print(TAG, DPSAFESERVICE_NULL);
			}
		}
	}
	
	/**
	 * @功能：设置各个安防模式下的探头启用与禁用
	 * @参数1：mode - 安防模式
	 * @参数2：enable - 启用或禁用 2进制位为1启用
	 * @返回值：boolean false-失败，true-成功
	 */
	public static boolean setSafeModeEnable(int mode, long enable) {
		return DPDBHelper.setSafeModeEnable(mode, enable);
	}

	/**
	 * @功能：获取各个安防模式下的探头启用与禁用
	 * @参数：mode - 安防模式
	 * @返回值：long - 启用或禁用 2进制位为1启用
	 */
	public static long getSafeModeEnable(int mode) {
		return DPDBHelper.getSafeModeEnable(mode);
	}

	/**
	 * 修改用户模式下的 安防模式对应的开关状态
	 * 
	 * @param model
	 *            模式 例如 在家 离家模式
	 * @param enable
	 *            8个开关的开闭状态
	 */
	public static void changeSafeEnable(int model, long enable) {
		synchronized (mutex) {
			if (DPSafeService.getInstance() != null) {
				DPSafeService.getInstance().changeSafeEnable(model, enable);
			} else {
				MyLog.print(TAG, DPSAFESERVICE_NULL);
			}
		}
	}

	/**
	 * 设置防区的接法(常开或常闭)
	 * @param connection
	 */
	public static void changeSafeConnection(long connection) {
		synchronized (mutex) {
			if (DPSafeService.getInstance() != null) {
				DPSafeService.getInstance().changeSafeConnection(connection);
			} else {
				MyLog.print(TAG, DPSAFESERVICE_NULL);
			}
		}
	}
	
	/**
	 * @功能：设置安防各个探头接法-高低电平
	 * @参数：long connection
	 * @例如：long connection = 0x1F 即2进制的1111 那么探头1-5常开，闭合时报警，探头6-8常闭，开时报警
	 * @描述：探头接法（高低电平）：0-常闭，1-常开
	 */
	public static boolean setSefeConnection(long connection) {
		return DPDBHelper.setSefeConnection(connection);
	}

	/**
	 * @功能：获取安防各个探头接法-高低电平
	 * @返回值：long - 安防探头接法
	 * @备注：返回值为 0x1F 即2进制的1111 那么探头1-5常开，闭合时报警，探头6-8常闭，开时报警
	 * @描述：探头接法（高低电平）：0-常闭，1-常开
	 */
	public static long getSefeConnection() {
		return DPDBHelper.getSefeConnection();
	}

	/**
	 * 设置对应安防模式的各个防区延时时间
	 * 
	 * @param model
	 *            安防模式
	 * @param time
	 *            上报延时时间
	 */
	public static void changeAlarmDelayTime(int model, int[] time) {
		synchronized (mutex) {
			if (DPSafeService.getInstance() != null) {
				DPSafeService.getInstance().changeAlarmDelayTime(model, time);
			} else {
				MyLog.print(TAG, DPSAFESERVICE_NULL);
			}
		}
	}
	
	/**
	 * @功能：设置各个安防模式下的每个探头的报警延时时间
	 * @参数1：mode - 安防模式
	 * @参数2：int[] - 每个探头的报警延时时间
	 * @返回值：boolean false-失败，true-成功
	 */
	public static boolean setAlarmDelayTime(int mode, int[] time) {
		return DPDBHelper.setAlarmDelayTime(mode, time);
	}

	/**
	 * @功能：获取各个安防模式下的每个探头的报警延时时间
	 * @参数：mode - 安防模式
	 * @返回值：int[] - 每个探头的报警延时时间
	 */
	public static int[] getAlarmDelayTime(int mode) {
		return DPDBHelper.getAlarmDelayTime(mode);
	}

	/**
	 * @return 是否正在报警
	 */
	public static boolean getAlarming() {
		return mIsAlarming;
	}
	
	/**
	 * 设置正在报警
	 */
	public static void setAlarming() {
		mIsAlarming = true;
	}

	/**
	 * 解除报警
	 * @param isHolding
	 *            是不是挟持报警
	 */
	public static void disalarm(boolean isHolding) {
		mIsAlarming = false;
		DPSafeService.getInstance().releaseAlarmRing();
		DPSafeService.getInstance().changeSafeMode(ConstConf.UNSAFE_MODE, false);
		if (isHolding) {
			TalkManager.toManageAlarm(
					CommonUT.formatTime(System.currentTimeMillis()), 99, 19, 0);
		} else {
			TalkManager.toManageDisAlarm();
		}
	}

	/** 同步室内分机安防模式 */
	public static void synchRoomSafeMode() {
		synchronized (mutex) {
			if (mNetCfgManager == null) {
				return;
			}
			String roomCode = getRoomCode()
					.substring(1, getRoomCode().length() - 2);
			String roomInfo = mNetCfgManager.RoomGet(roomCode);
			ArrayList<AddrInfo> list = AddrInfo.parsingAddrInfo(roomInfo);
			if (list != null && !list.isEmpty()) {
				for (int i = 0; i < list.size(); i++) {
					if (getRoomCode().equals(list.get(i).getCode())) {
						continue;
					}
					int ret = TalkManager.synchRoomSafeMode(list.get(i).getIp());
					MyLog.print("synchRoomSafeMode = " + ret);
				}
			}
		}
	}
	
	/**
	 * @功能：设置布防时间
	 * @参数 int time 0-300秒
	 */
	public static boolean setProtectionDelayTime(int time) {
		return DPDBHelper.setProtectionDelayTime(time);
	}

	/** @功能：获取布防时间 */
	public static int getProtectionDelayTime() {
		return DPDBHelper.getProtectionDelayTime();
	}

	/**
	 * 保存安防密码
	 * 
	 * @param holding
	 *            是否是挟持密码
	 * @param pwd
	 *            新密码
	 **/
	public static boolean setSafePassword(boolean holding, String pwd) {
		return DPDBHelper.setSafePassword(holding, pwd);
	}
	
	/**
	 * 获取安防密码
	 * 
	 * @param holding
	 *            是否是挟持密码
	 **/
	public static String getSafePassword(boolean holding) {
		return DPDBHelper.getSafePassword(holding);
	}
	
	/**
	 * @功能：增加安防记录到数据库中
	 */
	public static void addSafeModeLog(SafeModeInfo info) {
		DPDBHelper.addSafeModeLog(info);
	}

	/**
	 * @功能：删除数据库中指定ID的安防记录
	 * @参数：int db_id - 数据库中自动生成的_id
	 */
	public static void deleteSafeModeLog(int db_id) {
		DPDBHelper.deleteSafeModeLog(db_id);
	}

	/**
	 * @功能：删除所有布防记录
	 */
	public static void deleteAllSafeModeLog() {
		DPDBHelper.deleteAllSafeModeLog();
	}

	/**
	 * 获取布防记录
	 */
	public static ArrayList<SafeModeInfo> getSafeModeInfoList() {
		return DPDBHelper.getSafeModeInfoList();
	}

	/**
	 * @功能：增加报警记录到数据库中
	 */
	public static void addAlarmLog(AlarmLog info) {
		DPDBHelper.addAlarmLog(info);
	}
	
	/**
	 * @功能：修改报警记录到数据库中
	 */
	public static void modifyAlarmLog(AlarmLog info) {
		DPDBHelper.modifyAlarmLog(info);
	}

	/**
	 * @功能：删除数据库中指定ID的报警记录
	 * @参数：int db_id - 数据库中自动生成的_id
	 */
	public static void deleteAlarmLog(int db_id) {
		DPDBHelper.deleteAlarmLog(db_id);
	}

	/**
	 * @功能：删除所有报警记录
	 */
	public static void deleteAllAlarmLog() {
		DPDBHelper.deleteAllAlarmLog();
	}

	/** 获取报警记录 */
	public static ArrayList<AlarmLog> getAlarmLogList() {
		return DPDBHelper.getAlarmLogList();
	}
	
	/**
	 * @功能：增加报警录像到数据库中
	 */
	public static void addAlarmVideo(AlarmVideo info) {
		DPDBHelper.addAlarmVideo(info);
	}
	
	/**
	 * @功能：删除数据库中指定ID的报警录像
	 * @参数：int db_id - 数据库中自动生成的_id
	 */
	public static void deleteAlarmVideo(int db_id) {
		DPDBHelper.deleteAlarmVideo(db_id);
	}

	/**
	 * @功能：删除所有报警录像
	 */
	public static void deleteAlarmVideo() {
		DPDBHelper.deleteAllAlarmVideo();
	}
	
	/** 获取报警录像 */
	public static ArrayList<AlarmVideo> getAlarmVideoList() {
		return DPDBHelper.getAlarmVideoList();
	}
	
	// TODO-----------------------------对讲相关-----------------------------
	
	public static JniPhoneClass getJniPhoneClass() {
		return mJniPhoneClass;
	}
	
	public static ArrayList<AddrInfo> getMonitorList() {
		if (mNetCfgManager == null) {
			return null;
		}
		String info = mNetCfgManager.MonitorGet(null);
		if (info == null) {
			MyLog.print("DPGetMonitorList error.");
			return null;
		}
		int num = 0;
		info = getXmlVal("netcfg", info);
		if (info == null) {
			MyLog.print("DPGetMonitorList error.Do not find <netcfg>");
			return null;
		}
		String strnum = getXmlVal("num", info);
		if (strnum == null) {
			MyLog.print("DPGetMonitorList error.Do not find <num>");
			return null;
		}
		num = Integer.valueOf(strnum).intValue();
		if (num == 0) {
			MyLog.print("DPGetMonitorList error.num is 0");
			return null;
		}
		return AddrInfo.parsingAddrInfo(info);
	}

	/**
	 * @功能：获取相对本机房号的所有单元门口机的IP信息列表
	 */
	public static ArrayList<AddrInfo> getCellSeeList() {
		if (mNetCfgManager == null) {
			return null;
		}
		String info = mNetCfgManager.CellDoorGet(null);
		if (info == null) {
			return null;
		}
		int num = 0;
		info = getXmlVal("netcfg", info);
		if (info == null) {
			MyLog.print("DPGetCellSeeList error.Do not find <netcfg>");
			return null;
		}
		String strnum = getXmlVal("num", info);
		if (strnum == null) {
			MyLog.print("DPGetCellSeeList error.Do not find <num>");
			return null;
		}
		num = Integer.valueOf(strnum).intValue();
		if (num == 0) {
			MyLog.print("DPGetCellSeeList error.num is 0");
			return null;
		}
		return AddrInfo.parsingAddrInfo(info);
	}

	/**
	 * @功能：获取相对本机房号的所有別墅门口机的IP信息列表
	 */
	public static ArrayList<AddrInfo> getSecSeeList() {
		String info = mNetCfgManager.SecDoorGet(null);
		if (info == null) {
			return null;
		}
		int num = 0;
		info = getXmlVal("netcfg", info);
		if (info == null) {
			MyLog.print("DPGetSecSeeList error.Do not find <netcfg>");
			return null;
		}
		String strnum = getXmlVal("num", info);
		if (strnum == null) {
			MyLog.print("DPGetSecSeeList error.Do not find <num>");
			return null;
		}
		num = Integer.valueOf(strnum).intValue();
		if (num == 0) {
			MyLog.print("DPGetSecSeeList error.num is 0");
			return null;
		}
		return AddrInfo.parsingAddrInfo(info);
	}

	/**
	 * @功能：获取相对本机房号的所有大口机的IP信息列表
	 */
	public static ArrayList<AddrInfo> getAreaDoorSeeList() {
		String info = mNetCfgManager.AreadoorGet(null);
		if (info == null) {
			return null;
		}
		int num = 0;
		info = getXmlVal("netcfg", info);
		if (info == null) {
			MyLog.print("DPGetSecSeeList error.Do not find <netcfg>");
			return null;
		}
		String strnum = getXmlVal("num", info);
		if (strnum == null) {
			MyLog.print("DPGetSecSeeList error.Do not find <num>");
			return null;
		}
		num = Integer.valueOf(strnum).intValue();
		if (num == 0) {
			MyLog.print("DPGetSecSeeList error.num is 0");
			return null;
		}
		return AddrInfo.parsingAddrInfo(info);
	}
	
	/**
	 * @方法名：isCanCallIn
	 * @描述：收到新呼入后调用
	 * @功能：判断是否可以呼入
	 * @参数1：int CallSessionID - ID
	 * @参数2：String code - 呼入的房号
	 * @返回值：null或呼叫信息
	 */
	public static CallInfo isCanCallIn(int CallSessionID, String code) {
		// change by ZhengZhiying
		CallInfo callInfo = new CallInfo();
		callInfo.setSessionID(CallSessionID);
		callInfo.setStartTime();
		callInfo.setIsHangUp(false);
		callInfo.setRemoteCode(code);
		callInfo.setType(CallInfo.CALL_IN_UNACCEPT);
		if (mCallOutSessionIDList.size() == 0 && mCallInSessionIDList.size() == 0
				&& CallSessionID > 0) {
			mIsCallPhone = DPDBHelper.getCallToPhone();
			mCallInSessionIDList.add(callInfo);
			return callInfo;
		}
		mCallInSessionIDList.add(callInfo);
		return null;
	}

	/**
	 * 呼叫
	 * @param roomCode 房号 <br>
	 * 房号长度1位到13位之间 <br>
	 * 1或01-呼叫1号室内分机，0101或101-呼叫本单元0101室所有分机,020101或20101-呼叫本栋2单元0101室的所有分机
	 * @return =0-成功、>0-有呼入或呼出未挂断不能呼叫 <br>
	 * =-1-呼叫出错、=-2-房号不存在<br>
	 * =-3-不能呼叫本机房号、=-4-房号不能为空
	 */
	public static int callOut(String roomCode) {
		synchronized (mutex) {
			if (mCallOutSessionIDList.size() > 0) {
				return 1;
			}
			if (mCallInSessionIDList.size() > 0) {
				return 2;
			}
			if (!TextUtils.isEmpty(roomCode)) {
				String localCode = getRoomCode();
				if (TextUtils.isEmpty(localCode) || mNetCfgManager == null) {
					MyLog.print(MyLog.ERROR, TAG, "-呼叫出错");
					return -1;
				}
				MyLog.print("本机房号 = " + localCode);
				String roomInfo;
				boolean isPortion;
				if (roomCode.length() == localCode.length()) {
					if (roomCode.equals(localCode)) {
						MyLog.print(MyLog.ERROR, TAG, "-不能呼叫本机房号");
						return -3;
					}
					roomInfo = mNetCfgManager.TermGet(roomCode);
					isPortion = false;
				} else if (roomCode.length() < 3) {
					roomCode = localCode.substring(0, 
							localCode.length() - roomCode.length()) + roomCode;
					if (roomCode.equals(localCode)) {
						MyLog.print(MyLog.ERROR, TAG, "--不能呼叫本机房号");
						return -3;
					}
					roomInfo = mNetCfgManager.TermGet(roomCode);
					isPortion = false;
				} else if (roomCode.length() == 11) {
					roomCode = "10" + roomCode;
					if (roomCode.equals(localCode)) {
						MyLog.print(MyLog.ERROR, TAG, "---不能呼叫本机房号");
						return -3;
					}
					roomInfo = mNetCfgManager.TermGet(roomCode);
					isPortion = false;
				} else if (roomCode.length() == 12) {
					roomCode = "1" + roomCode;
					if (roomCode.equals(localCode)) {
						MyLog.print(MyLog.ERROR, TAG, "----不能呼叫本机房号");
						return -3;
					}
					roomInfo = mNetCfgManager.TermGet(roomCode);
					isPortion = false;
				} else {
					roomInfo = mNetCfgManager.RoomGet(roomCode);
					isPortion = true;
				}
				if (TextUtils.isEmpty(roomInfo)) {
					MyLog.print(MyLog.ERROR, TAG, "-房号不存在");
					return -2;
				}
				ArrayList<AddrInfo> list = AddrInfo.parsingAddrInfo(roomInfo);
				if (list == null || list.isEmpty()) {
					MyLog.print(MyLog.ERROR, TAG, "--房号不存在");
					return -2;
				}
				if (isPortion) {
					if (localCode.startsWith(
							list.get(0).getCode().substring(0, localCode.length() - 2))) {
						MyLog.print(MyLog.ERROR, TAG, "---不能呼叫本机房号");
						return -3;
					}
				}
				if (!callOut(list)) {
					MyLog.print(MyLog.ERROR, TAG, "--呼叫出错");
					return -1;
				}
				return 0;
			} else {
				return -4;
			}
		}
	}
	
	/**
	 * @方法名：callOut
	 * @功能：呼叫多个设备
	 * @参数：ArrayList<AddrInfo> list - 多个设备的IP信息列表
	 * @返回值：boolean true-成功,false-失败
	 */
	private static boolean callOut(ArrayList<AddrInfo> list) {
		synchronized (mutex) {
			int num = 0;
			if (list != null && !list.isEmpty()) {
				int sessionID = 0;
				Iterator<AddrInfo> iterator = list.iterator();
				while (iterator.hasNext()) {
					AddrInfo addrInfo = iterator.next();
					sessionID = mJniPhoneClass.CallOut(addrInfo.getCode(),
							addrInfo.getIp());
					MyLog.print("DPCallOut sessionid = " + sessionID
							+ addrInfo.toString());
					if (sessionID > 0) {
						num++;
						CallInfo callInfo = new CallInfo();
						callInfo.setSessionID(sessionID);
						callInfo.setStartTime();
						callInfo.setIsHangUp(false);
						callInfo.setRemoteCode(addrInfo.getCode());
						callInfo.setType(CallInfo.CALL_OUT_UNACCEPT);
						mCallOutSessionIDList.add(callInfo);
						MyLog.print("calloutSessionIdList size="
								+ mCallOutSessionIDList.size());
					}
				}
			}
			return num > 0;
		}
	}

	/**
	 * 呼叫所有的管理中心 <br>
	 * 如果只需单独呼叫1号管理中心请使用int callOut(String roomCode)
	 * @return =0-成功、>0-有呼入或呼出未挂断不能呼叫 <br>
	 * =-1-呼叫出错、=-2-房号不存在<br>
	 */
	public static int callManager() {
		synchronized (mutex) {
			if (mCallOutSessionIDList.size() > 0) {
				return 1;
			}
			if (mCallInSessionIDList.size() > 0) {
				return 2;
			}
			if (mNetCfgManager == null) {
				MyLog.print(MyLog.ERROR, TAG, "-呼叫出错");
				return -1;
			}
			String managerInfo = mNetCfgManager.ManagerGet(null);
			if (TextUtils.isEmpty(managerInfo)) {
				MyLog.print(MyLog.ERROR, TAG, "-房号不存在");
				return -2;
			}
			ArrayList<AddrInfo> list = AddrInfo.parsingAddrInfo(managerInfo);
			if (list == null || list.isEmpty()) {
				MyLog.print(MyLog.ERROR, TAG, "--房号不存在");
				return -2;
			}
			if (!callOut(list)) {
				MyLog.print(MyLog.ERROR, TAG, "--呼叫出错");
				return -1;
			}
			return 0;
		}
	}
	
	/**
	 * 呼叫所有的保安分机 <br>
	 * 如果只需单独呼叫1号管理中心请使用int callOut(String roomCode)
	 * @return =0-成功、>0-有呼入或呼出未挂断不能呼叫 <br>
	 * =-1-呼叫出错、=-2-房号不存在<br>
	 */
	public static int callSecurity() {
		synchronized (mutex) {
			if (mCallOutSessionIDList.size() > 0) {
				return 1;
			}
			if (mCallInSessionIDList.size() > 0) {
				return 2;
			}
			if (mNetCfgManager == null) {
				MyLog.print(MyLog.ERROR, TAG, "-呼叫出错");
				return -1;
			}
			String info = mNetCfgManager.GuardGet(null);
			if (TextUtils.isEmpty(info)) {
				MyLog.print(MyLog.ERROR, TAG, "-房号不存在");
				return -2;
			}
			ArrayList<AddrInfo> list = AddrInfo.parsingAddrInfo(info);
			if (list == null || list.isEmpty()) {
				MyLog.print(MyLog.ERROR, TAG, "--房号不存在");
				return -2;
			}
			if (!callOut(list)) {
				MyLog.print(MyLog.ERROR, TAG, "--呼叫出错");
				return -1;
			}
			return 0;
		}
	}

	/**
	 * @方法名：seeDoor
	 * @功能：监视门口机
	 * @参数：AddrInfo info -门口机 信息
	 * @返回值：boolean true-成功,false-失败
	 */
	public static int seeDoor(AddrInfo info) {
		synchronized (mutex) {
			if (mSeeInfo == null) {
				return 0;
			}
			if (mSeeInfo.isHangUp()) {
				int sessionID = mJniPhoneClass.Monitor(info.getCode(),
						info.getIp());
				if (sessionID > 0) {
					mSeeInfo = new CallInfo();
					mSeeInfo.setIsHangUp(false);
					mSeeInfo.setRemoteCode(info.getCode());
					mSeeInfo.setType(CallInfo.SEE_UNACCEPT);
					mSeeInfo.setStartTime();
					mSeeInfo.setSessionID(sessionID);
				}
			} else {
				MyLog.print("have other see.seeSessionID = "
						+ mSeeInfo.getSessionID());
			}
			return mSeeInfo.getSessionID();
		}
	}

	/**
	 * @方法名：seeHangUp
	 * @功能：挂断当前监视的门口机
	 */
	public static void seeHangUp() {
		synchronized (mutex) {
			if (!mSeeInfo.isHangUp()) {
				mSeeInfo.setEndTime();
				mSeeInfo.setIsHangUp(true);
				DPDBHelper.addCallLog(mSeeInfo);
				mJniPhoneClass.HangUp(mSeeInfo.getSessionID());
			}
		}
	}

	/**
	 * @方法名：openLock
	 * @功能：通知门口机开锁
	 * @参数：String doorCode-门口机房号
	 * @返回值：boolean true-成功,false-失败
	 */
	public static boolean openLock(String doorCode) {
		synchronized (mutex) {
			// 判断是否门口机呼入
			if (doorCode == null) {
				return false;
			}
			Iterator<CallInfo> iterator = mCallInSessionIDList.iterator();
			while (iterator.hasNext()) {
				CallInfo callinfo = iterator.next();
				if (callinfo.getRemoteCode() != null
						&& doorCode.equals(callinfo.getRemoteCode())) {
					if (callinfo.isDoor()) {
						if (mNetCfgManager == null) {
							return false;
						}
						String info = mNetCfgManager.TermGet(doorCode);
						if (info != null) {
							ArrayList<AddrInfo> list = AddrInfo
									.parsingAddrInfo(info);
							if (list != null && list.size() == 1) {
								if (TalkManager.toDoorOpenLock(list.get(0)
										.getIp(), getRoomCode()) == 0) {
									callinfo.setIsOpenLock(true);
									return true;
								}
							}
						}
					}
				}
			}
			return false;
		}
	}

	/**
	 * @功能：检测会话超时
	 */
	public static void checkCallTime() {
		synchronized (mutex) {
			if (mCallInSessionIDList.size() > 0) {
				Iterator<CallInfo> iterator_in = mCallInSessionIDList.iterator();
				while (iterator_in.hasNext()) {
					CallInfo callInfo = iterator_in.next();
					if (callInfo.isDoor()) {
						if (Math.abs(System.currentTimeMillis()
								- callInfo.getStartTime()) > ConstConf.START_CALL_PHONE) {
							if (mIsCallPhone && !callInfo.getCalledPhone()
									&& callInfo.getType() == CallInfo.CALL_IN_UNACCEPT) {
								callInfo.setCalledPhone(true);
								callPhone();
							}
						}
					}
					if (callInfo.getType() == CallInfo.CALL_IN_UNACCEPT) {
						// 室内机呼叫室内机需做超时处理
						// 呼出的室内机会做超时处理,但是对方呼叫后断网，会导致室内机一直在振铃界面
						if (DPFunction.code2Type(callInfo.getRemoteCode()) == 1) {
							if (Math.abs(System.currentTimeMillis()
									- callInfo.getStartTime()) > ConstConf.RING_TIMEOUT) {
								Intent intent = new Intent();
								intent.putExtra(SESSION_ID, callInfo.getSessionID());
								intent.putExtra(MSG_TYPE, JniPhoneClass.MSG_RING_TIMEOUT);
								intent.putExtra(REMOTE_CODE, callInfo.getRemoteCode());
								intent.setAction(CallReceiver.CALL_IN_ACTION);
								mContext.sendBroadcast(intent);
							}
						}
						if (Math.abs(System.currentTimeMillis()
								- callInfo.getStartTime()) > ConstConf.RING_TIMEOUT) {
							/** 呼入振铃超时应该由对方检测，但如果本机设置留言模式后需自动接听并保存留言 */
							if (callInfo.isDoor()
									&& callInfo.getAcceptTime() == 0) {
								Intent intent = new Intent();
								intent.putExtra(SESSION_ID, callInfo.getSessionID());
								intent.putExtra(MSG_TYPE, JniPhoneClass.MSG_RING_TIMEOUT);
								intent.putExtra(REMOTE_CODE, callInfo.getRemoteCode());
								intent.setAction(CallReceiver.CALL_IN_ACTION);
								mContext.sendBroadcast(intent);
								MyLog.print(MyLog.ERROR, "振铃超时");
							}
						}
					} else if (callInfo.getType() == CallInfo.CALL_IN_ACCEPT) {
						if (Math.abs(System.currentTimeMillis()
								- callInfo.getAcceptTime()) > ConstConf.TALK_TIMEOUT) {
							Intent intent = new Intent();
							intent.putExtra(SESSION_ID, callInfo.getSessionID());
							intent.putExtra(MSG_TYPE, JniPhoneClass.MSG_TALK_TIMEOUT);
							intent.putExtra(REMOTE_CODE, callInfo.getRemoteCode());
							intent.setAction(CallReceiver.CALL_IN_ACTION);
							mContext.sendBroadcast(intent);
							callHangUp(callInfo.getSessionID());
							iterator_in = mCallInSessionIDList.iterator();
						}
					}
				}
			} else if (mCallOutSessionIDList.size() > 0) {
				Iterator<CallInfo> iterator_out = mCallOutSessionIDList
						.iterator();
				while (iterator_out.hasNext()) {
					CallInfo callInfo = iterator_out.next();
					if (callInfo.getType() == CallInfo.CALL_OUT_UNACCEPT) {
						if (Math.abs(System.currentTimeMillis()
								- callInfo.getStartTime()) > ConstConf.RING_TIMEOUT) {
							Intent intent = new Intent();
							intent.putExtra(SESSION_ID, callInfo.getSessionID());
							intent.putExtra(MSG_TYPE, JniPhoneClass.MSG_RING_TIMEOUT);
							intent.putExtra(REMOTE_CODE, callInfo.getRemoteCode());
							intent.setAction(CallReceiver.CALL_OUT_ACTION);
							mContext.sendBroadcast(intent);
							callHangUp(callInfo.getSessionID());
							iterator_out = mCallOutSessionIDList.iterator();
						}
					} else if (callInfo.getType() == CallInfo.CALL_OUT_ACCEPT) {
						if (Math.abs(System.currentTimeMillis()
								- callInfo.getAcceptTime()) > ConstConf.TALK_TIMEOUT) {
							Intent intent = new Intent();
							intent.putExtra(SESSION_ID, callInfo.getSessionID());
							intent.putExtra(MSG_TYPE, JniPhoneClass.MSG_TALK_TIMEOUT);
							intent.putExtra(REMOTE_CODE, callInfo.getRemoteCode());
							intent.setAction(CallReceiver.CALL_OUT_ACTION);
							mContext.sendBroadcast(intent);
							callHangUp(callInfo.getSessionID());
							iterator_out = mCallOutSessionIDList.iterator();
						}
					}
				}
			} else if (!getSeeInfo().isHangUp()
					&& getSeeInfo().getType() == CallInfo.SEE_ACCEPT) { // 监视超时
				long time = Math.abs(System.currentTimeMillis()
						- getSeeInfo().getAcceptTime());
				if (time > ConstConf.MONITOR_TIMEOUT) {
					Intent i = new Intent();
					i.putExtra(SESSION_ID, getSeeInfo().getSessionID());
					i.putExtra(MSG_TYPE, JniPhoneClass.MSG_MONITOR_TIMEOUT);
					i.putExtra(REMOTE_CODE, getSeeInfo().getRemoteCode());
					i.setAction(CallReceiver.SEE_ACTION);
					mContext.sendBroadcast(i);
					DPFunction.seeHangUp();
				}
			}
		}
	}
	
	/**
	 * @功能：获取本机当前监视信息
	 */
	public static CallInfo getSeeInfo() {
		return mSeeInfo;
	}
	
	/**
	 * @功能：获取本机当前呼出会话列表
	 */
	public static List<CallInfo> getCallOutList() {
		if (mCallOutSessionIDList == null) {
			return null;
		} else {
			return mCallOutSessionIDList;
		}
	}

	/**
	 * @功能：获取本机当前呼出会话的个数
	 */
	public static int getCallOutSize() {
		if (mCallOutSessionIDList == null) {
			return 0;
		} else {
			return mCallOutSessionIDList.size();
		}
	}
	
	/**
	 * @功能：获取本机当前呼入会话列表
	 */
	public static List<CallInfo> getCallInList() {
		if (mCallInSessionIDList == null) {
			return null;
		} else {
			return mCallInSessionIDList;
		}
	}

	/**
	 * @功能：获取本机当前呼入会话的个数
	 */
	public static int getCallInSize() {
		if (mCallInSessionIDList == null) {
			return 0;
		} else {
			return mCallInSessionIDList.size();
		}
	}
	
	/**
	 * @功能：设置对方图像在本机屏幕的显示区域
	 * @参数1：int CallSessionID - ID
	 * @参数2-5：int xywh - 起始位置和宽高
	 * @返回值：boolean true-成功，false-失败
	 */
	public static boolean setVideoDisplayArea(int CallSessionID, int x, int y,
			int w, int h) {
		synchronized (mutex) {
			if (mJniPhoneClass != null && CallSessionID > 0) {
				return mJniPhoneClass.SetVideoDisplayArea(CallSessionID, 0, x,
						y, w, h);
			}
			return false;
		}
	}

	/**
	 * @功能：设置对方图像在TV屏幕的显示区域
	 * @参数1：int CallSessionID - ID
	 * @参数2-5：int xywh - 起始位置和宽高
	 * @返回值：boolean true-成功，false-失败
	 */
	public static boolean setTVDisplayArea(int CallSessionID, int x, int y,
			int w, int h) {
		synchronized (mutex) {
			if (mJniPhoneClass != null && CallSessionID > 0) {
				return mJniPhoneClass.SetTVoutDisplayArea(CallSessionID, 0, x,
						y, w, h);
			}
			return false;
		}
	}

	/**
	 * @功能：设置通话音量
	 * @参数1：int CallSessionID - ID
	 * @参数2：int Percent - 音量百分比
	 * @返回值：boolean true-成功，false-失败
	 */
	public static boolean setAudioVolume(int CallSessionID, int Percent) {
		synchronized (mutex) {
			if (mJniPhoneClass != null && CallSessionID > 0) {
				return mJniPhoneClass.SetAudioVolume(CallSessionID, Percent);
			}
			return false;
		}
	}

	/**
	 * @功能：控制本地视频是否对方可见 请在调用SetVideoDisplayArea前一行调用它，来设置初始默认值。然后当用户按视频开关按钮时也调用
	 * @参数1：int CallSessionID - ID
	 * @参数2：boolean enable true-可见,false-不可见
	 * @返回值：boolean false-失败,true-成功
	 */
	public static boolean setLocalVideoVisable(int CallSessionID, boolean enable) {
		synchronized (mutex) {
			if (mJniPhoneClass != null && CallSessionID > 0) {
				return mJniPhoneClass.SetLocalVideoVisable(CallSessionID, 0,
						enable);
			}
			return false;
		}
	}

	/**
	 * @方法名：accept
	 * @功能：接听指定会话
	 * @参数1：int CallSessionID - ID
	 * @参数2：int acceptType 0 - 是手动接听,1 - 超时自动接听,2 - 手机接听
	 * @返回值：boolean true-接听成功，false-接听失败
	 * @备注：自动接听后会设置应答音频文件路径和留言文件路径
	 */
	public static boolean accept(int CallSessionID, int acceptType) {
		synchronized (mutex) {
			MyLog.print("accept acceptType = " + acceptType);
			if (CallSessionID > 0) {
				Iterator<CallInfo> iterator = mCallInSessionIDList.iterator();
				while (iterator.hasNext()) {
					CallInfo callInfo = iterator.next();
					if (CallSessionID == callInfo.getSessionID()
							&& callInfo.getAcceptTime() == 0) {
						if (mJniPhoneClass.Accept(CallSessionID)) {
							callInfo.setAcceptTime();
							if (!callInfo.isDoor()) {
								// 接听后不显示本机摄像头图像给对方
								setLocalVideoVisable(CallSessionID, false);
							}
							if (acceptType == 1) {
								callInfo.setType(CallInfo.CALL_IN_UNACCEPT);
								boolean ret = setMessageModeFilePath(callInfo);
								MyLog.print("atuo accept ret=" + ret);
								toPhoneHangUp();
								isCallAccept = true;
							} else if (acceptType == 0) {
								callInfo.setType(CallInfo.CALL_IN_ACCEPT);
								toPhoneHangUp();
								isCallAccept = true;
							} else {
								callInfo.setType(CallInfo.CALL_IN_ACCEPT);
							}
							return true;
						}
					}
				}
			}
			return false;
		}
	}
	
	/**
	 * @方法名：setMessageModeFilePath
	 * @功能：设置门口机呼叫本机超时后应答音频文件路径和留言文件路径
	 * @参数：CallInfo info - 会话信息
	 * @返回值：boolean false-失败,true-成功
	 */
	private static boolean setMessageModeFilePath(CallInfo info) {
		if (info != null) {
			String palyFile = null;
			int mode = DPDBHelper.getMessageMode();
			if (mode == 1) {
				/** 默认留言 */
				palyFile = new String(ConstConf.DEFAULT_LEAVE_PATH);
			} else if (mode == 2) {
				/** 业主留言 */
				palyFile = new String(ConstConf.USER_LEAVE_PATH);
			} else {
				return false;
			}
			File play = new File(palyFile);
			if (play.exists()) {
				/**
				 * 根据会话开始时间来命名保存的录音文件名 因文件名称中不能有：所有要替换成-
				 */
				File destDir = new File(ConstConf.VISIT_PATH);
				if (!destDir.exists()) {
					destDir.mkdirs();
				}
				String audioName = ConstConf.VISIT_PATH
						+ File.separator
						+ CommonUT.formatTime(info.getStartTime())
								.replaceAll(":", "-") + ".wav";
				return mJniPhoneClass.SetFilePlayMode(info.getSessionID(),
						true, palyFile)
						&& mJniPhoneClass.SetFileRecordMode(
								info.getSessionID(), true, audioName);
			} else {
				MyLog.print("leave message file miss");
			}
		}
		return false;
	}
	
	/**
	 * @功能：保存图像
	 * @参数：CallInfo callInfo - 会话信息
	 */
	private static void saveImage(CallInfo callInfo) {
		synchronized (mutex) {
			if (callInfo.getType() == CallInfo.CALL_IN_UNACCEPT
					&& callInfo.isDoor()) {
				byte[] buffer = new byte[1280 * 720 / 4];
				int temp = mJniPhoneClass.CaptureVideoImage(
						callInfo.getSessionID(), 0, buffer);
				if (temp != 0) {
					FileOutputStream outputStream = null;
					FileOutputStream stream = null;
					try {
						File destDir = new File(ConstConf.VISIT_PATH);
						if (!destDir.exists()) {
							destDir.mkdirs();
						}
						File file = new File(ConstConf.VISIT_LITTLE_PATH);
						if (!file.exists()) {
							file.mkdirs();
						}
						String imageName = new String(destDir.getAbsolutePath()
								+ File.separator
								+ CommonUT.formatTime(callInfo.getStartTime())
										.replaceAll(":", "-") + ".jpg");
						MyLog.print("saveImage name = " + imageName);
						String littleImage = new String(file.getAbsolutePath()
								+ File.separator
								+ CommonUT.formatTime(callInfo.getStartTime())
										.replaceAll(":", "-") + ".jpg");
						BitmapFactory.Options options = new BitmapFactory.Options();
						options.inSampleSize = 3;
						Bitmap bitmap = BitmapFactory.decodeByteArray(buffer,
								0, buffer.length, options);
						stream = new FileOutputStream(littleImage);
						bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
						outputStream = new FileOutputStream(imageName);
						outputStream.write(buffer);
						outputStream.flush();
						outputStream.getFD().sync();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					} finally {
						try {
							if (outputStream != null) {
								outputStream.close();
							}
							if (stream != null) {
								stream.close();
							}
							
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					MyLog.print("saveImage error:" + temp);
				}
			}
		}
	}

	/**
	 * @描述：收到对方接听后调用
	 * @功能：挂断除去指定会话外的会话
	 * @参数1：int CallSessionID - ID
	 * @参数2：boolean isAutoAccept - 是否超时自动接听，true-超时自动接听
	 * @返回值：boolean true-接听成功，false-接听失败
	 */
	public static void callOutOtherHangUp(int callSessionID) {
		synchronized (mutex) {
			MyLog.print("callOutOtherHangUp CallSessionID = " + callSessionID);
			if (callSessionID > 0 && mCallOutSessionIDList.size() > 0) {
				int sessionID[] = new int[mCallOutSessionIDList.size()];
				Arrays.fill(sessionID, 0);
				for (int i = 0; i < mCallOutSessionIDList.size(); i++) {
					CallInfo callInfo = mCallOutSessionIDList.get(i);
					if (callSessionID == callInfo.getSessionID()) {
						callInfo.setAcceptTime();
						callInfo.setType(CallInfo.CALL_OUT_ACCEPT);
					} else {
						sessionID[i] = callInfo.getSessionID();
					}
				}
				for (int i = 0; i < sessionID.length; i++) {
					if (sessionID[i] != 0) {
						callHangUp(sessionID[i]);
					}
				}
			}
		}
	}
	
	/**
	 * @方法名：callHangUp
	 * @功能：挂断指定会话
	 * @参数1：int CallSessionID - ID
	 * @返回值：String 指定会话的对方房号
	 * @备注：门口机呼入后在没有接听的情况下会保存一帧图像后挂断
	 */
	public static String callHangUp(int callSessionID) {
		synchronized (mutex) {
			MyLog.print("callHangUp callSessionID = " + callSessionID);
			phoneAccept = false;
			isCallAccept = false;
			String ret = null;
			if (callSessionID > 0) {
				Iterator<CallInfo> iterator_in = mCallInSessionIDList.iterator();
				while (iterator_in.hasNext()) {
					CallInfo callInfo = iterator_in.next();
					if (callSessionID == callInfo.getSessionID()) {
						callInfo.setEndTime();
						callInfo.setIsHangUp(true);
						if (callInfo.getRemoteCode() != null) {
							ret = new String(callInfo.getRemoteCode());
						}
						if (ProjectConfigure.project == 2) {
							boolean isDoor = false;
							int type = 0;
							if (callInfo.getType() == CallInfo.CALL_IN_UNACCEPT
									&& callInfo.isDoor()) {
								saveImage(callInfo);
								// 需通知有新的来访记录
								isDoor = true;
								type = 1;
							} else if (callInfo.isDoor()) {
								isDoor = true;
								type = 0;
							}
							if (isDoor) {
								int result = TalkManager.toManageDoorCall(callInfo.getRemoteCode(), 
										CommonUT.formatTime(callInfo.getStartTime()), type);
								if (result == 0) {
									callInfo.setIsSuccess(true);
									MyLog.print(TAG, "toManageDoorCall Success");
								}
							}
						} else {
							if (callInfo.getType() == CallInfo.CALL_IN_UNACCEPT
									&& callInfo.isDoor()) {
								saveImage(callInfo);
								// 需通知有新的来访记录
							}
						}
						DPDBHelper.addCallLog(callInfo);
						mJniPhoneClass.HangUp(callSessionID);
						toPhoneHangUp();
						callSessionID = 0;
						iterator_in.remove();
						iterator_in = mCallInSessionIDList.iterator();
						break;
					}
				}
				MyLog.print("mCallInSessionIDList size = "
						+ mCallInSessionIDList.size());
				Iterator<CallInfo> iterator_out = mCallOutSessionIDList.iterator();
				while (iterator_out.hasNext()) {
					CallInfo callInfo = iterator_out.next();
					if (callSessionID == callInfo.getSessionID()) {
						callInfo.setEndTime();
						callInfo.setIsHangUp(true);
						DPDBHelper.addCallLog(callInfo);
						if (callInfo.getRemoteCode() != null) {
							ret = new String(callInfo.getRemoteCode());
						}
						mJniPhoneClass.HangUp(callSessionID);
						callSessionID = 0;
						iterator_out.remove();
						iterator_out = mCallOutSessionIDList.iterator();
						break;
					}
				}
				MyLog.print("mCallOutSessionIDList size = "
						+ mCallOutSessionIDList.size());
			}
			return ret;
		}
	}

	/**
	 * @方法名：callOutHangUp
	 * @功能：挂断所有呼出会话
	 */
	public static void callOutHangUp() {
		synchronized (mutex) {
			Iterator<CallInfo> iterator_out = mCallOutSessionIDList.iterator();
			while (iterator_out.hasNext()) {
				CallInfo calloutinfo = iterator_out.next();
				calloutinfo.setEndTime();
				calloutinfo.setIsHangUp(true);
				DPDBHelper.addCallLog(calloutinfo);
				mJniPhoneClass.HangUp(calloutinfo.getSessionID());
				iterator_out.remove();
				iterator_out = mCallOutSessionIDList.iterator();
			}
			MyLog.print("callOutHangUp mCallOutSessionIDList size = "
					+ mCallOutSessionIDList.size());
		}
	}

	/**
	 * 挂断所有通话，包括手机和其他设备
	 * 
	 */
	public static String callHangUp() {
		synchronized (mutex) {
			toPhoneHangUp();
			phoneAccept = false;
			isCallAccept = false;
			String ret = null;
			Iterator<CallInfo> iterator_in = mCallInSessionIDList.iterator();
			while (iterator_in.hasNext()) {
				CallInfo callInfo = iterator_in.next();
				callInfo.setEndTime();
				callInfo.setIsHangUp(true);
				if (callInfo.getRemoteCode() != null) {
					ret = new String(callInfo.getRemoteCode());
				}
				if (ProjectConfigure.project == 2) {
					boolean isDoor = false;
					int type = 0;
					if (callInfo.getType() == CallInfo.CALL_IN_UNACCEPT
							&& callInfo.isDoor()) {
						saveImage(callInfo);
						// 需通知有新的来访记录
						isDoor = true;
						type = 1;
					} else if (callInfo.isDoor()) {
						isDoor = true;
						type = 0;
					}
					if (isDoor) {
						int result = TalkManager.toManageDoorCall(callInfo.getRemoteCode(), 
								CommonUT.formatTime(callInfo.getStartTime()), type);
						if (result == 0) {
							callInfo.setIsSuccess(true);
							MyLog.print(TAG, "toManageDoorCall Success");
						}
					}
				} else {
					if (callInfo.getType() == CallInfo.CALL_IN_UNACCEPT
							&& callInfo.isDoor()) {
						saveImage(callInfo);
						// 需通知有新的来访记录
					}
				}
				DPDBHelper.addCallLog(callInfo);
				mJniPhoneClass.HangUp(callInfo.getSessionID());
				iterator_in.remove();
				iterator_in = mCallInSessionIDList.iterator();
			}
			MyLog.print("mCallInSessionIDList size = "
					+ mCallInSessionIDList.size());
			Iterator<CallInfo> iterator_out = mCallOutSessionIDList.iterator();
			while (iterator_out.hasNext()) {
				CallInfo callInfo = iterator_out.next();
				callInfo.setEndTime();
				callInfo.setIsHangUp(true);
				DPDBHelper.addCallLog(callInfo);
				if (callInfo.getRemoteCode() != null) {
					ret = new String(callInfo.getRemoteCode());
				}
				
				mJniPhoneClass.HangUp(callInfo.getSessionID());
				iterator_out.remove();
				iterator_out = mCallOutSessionIDList.iterator();
			}
			MyLog.print("mCallOutSessionIDList size = "
					+ mCallOutSessionIDList.size());
			return ret;
		}
	}
	
	/**
	 * @功能：查找呼出列表中指定会话ID的呼叫信息
	 * @参数：int CallSessionID - ID
	 * @返回值：指定会话ID的呼叫信息
	 */
	public static CallInfo findCallOut(int callSessionID) {
		synchronized (mutex) {
			if (callSessionID > 0) {
				Iterator<CallInfo> iterator_out = mCallOutSessionIDList.iterator();
				while (iterator_out.hasNext()) {
					CallInfo callinfo = iterator_out.next();
					if (callSessionID == callinfo.getSessionID()) {
						return callinfo;
					}
				}
			}
			return null;
		}
	}

	/**
	 * @功能：查找呼出列表中指定索引号的呼叫信息
	 * @参数：int index 索引号
	 * @返回值：指定索引号的呼叫信息
	 */
	public static CallInfo findCallOutIndex(int index) {
		synchronized (mutex) {
			if (index >= 0) {
				int i = 0;
				Iterator<CallInfo> iterator_out = mCallOutSessionIDList
						.iterator();
				while (iterator_out.hasNext()) {
					CallInfo callinfo = iterator_out.next();
					if (i++ == index) {
						return callinfo;
					}
				}
			}
			return null;
		}
	}

	/**
	 * @方法名：findCallin
	 * @功能：查找呼入列表中指定会话ID的呼叫信息
	 * @参数：int CallSessionID - ID
	 * @返回值：指定会话ID的呼叫信息
	 */
	public static CallInfo findCallIn(int callSessionID) {
		synchronized (mutex) {
			if (callSessionID > 0) {
				Iterator<CallInfo> iterator_out = mCallInSessionIDList
						.iterator();
				while (iterator_out.hasNext()) {
					CallInfo callinfo = iterator_out.next();
					if (callSessionID == callinfo.getSessionID()) {
						return callinfo;
					}
				}
			}
			return null;
		}
	}
	
	/**
	 * @功能：增加呼叫记录到数据库中 呼叫开始到结束时间小于1.5s时不保存记录
	 */
	public static void addCallLog(CallInfo info) {
		DPDBHelper.addCallLog(info);
	}

	/**
	 * @功能：修改数据库中的呼叫记录
	 */
	public static void modifyCallLog(CallInfo info) {
		DPDBHelper.modifyCallLog(info);
	}

	/**
	 * @功能：删除数据库中指定ID的呼叫记录
	 * @参数：int db_id - 数据库中自动生成的_id
	 */
	public static void deleteCallLog(int db_id) {
		DPDBHelper.deleteCallLog(db_id);
	}

	/**
	 * @功能：删除对应类型的所有呼叫记录
	 * @参数：int type 见CallInfo类中的CALLOUT_ACCEPT等值
	 * @备注：参数可以组合使用，如CALLOUT_ACCEPT | CALLOUT_UNACCEPT
	 */
	public static void deleteAllCallLog(int type) {
		DPDBHelper.deleteAllCallLog(type);
	}

	/**
	 * @功能：从数据库中获取呼叫记录
	 * @参数：int type 见CallInfo类中的CALLOUT_ACCEPT等值
	 * @备注：参数可以组合使用，如CALLOUT_ACCEPT | CALLOUT_UNACCEPT
	 */
	public static ArrayList<CallInfo> getCallLogList(int type) {
		return DPDBHelper.getCallLogList(type);
	}
	
	// TODO-----------------------------云对讲相关-----------------------------
	
	private static CloudIntercomCallback mCloudIntercomCallback = 
			new CloudIntercomCallback() {

		@Override
		public String getRoomCode() {
			return DPDBHelper.getRoomCode();
		}
		
		@Override
		public int getSafeMode() {
			return DPDBHelper.getSafeMode();
		}
		
		@Override
		public void changeSafeMode(int model, boolean isAuto) {
			if (model == DPDBHelper.getSafeMode() 
					&& model != ConstConf.UNSAFE_MODE) {
				// 如果收到的模式与当前相同(非撤防)则同步手机端安防模式
				synchPhoneSafeMode();
			} else {
				if (model == ConstConf.UNSAFE_MODE) {
					disalarm(false);
					Intent intent = new Intent();
					intent.setAction(ACTION_ALARMING);
					intent.putExtra(ALARM_ID, DISALARMING);
					mContext.sendBroadcast(intent);
				} else {
					DPFunction.changeSafeMode(model, false);
				}
			}
		}
		
		@Override
		public ArrayList<Alarm> getAlarmLog() {
			List<AlarmNameInfo> nameList = getAlarmAreaNameList();
			List<AlarmTypeInfo> typeList = getAlarmTypeNameList();
			ArrayList<AlarmLog> alarmLogs = getAlarmLogList();
			ArrayList<Alarm> list = new ArrayList<Alarm>();
			if (nameList != null && typeList != null && alarmLogs != null) {
				for (int i = 0; i < alarmLogs.size(); i++) {
					AlarmLog alarmLog = alarmLogs.get(i);
					Alarm alarm = new Alarm();
					alarm.id = alarmLog.getDb_id();
					alarm.area = nameList.get(alarmLog.getAreaName()).value;
					alarm.type = typeList.get(alarmLog.getAreaType()).value;
					alarm.time = CommonUT.formatTime(alarmLog.getTime());
					list.add(alarm);
				}
			}
			return list;
		}
		
		@Override
		public int getSmartHomeMode() {
			return DPDBHelper.getSmartHomeMode();
		}
		
		@Override
		public void changeSmartHomeMode(int model) {
			if (model == DPDBHelper.getSmartHomeMode()) {
				// 如果收到的模式与当前相同则同步手机端智能家居模式
				synchPhoneSmartHomeMode();
			} else {
				DPFunction.setSmartHomeMode(model);
			}
		}

		@Override
		public ArrayList<Visit> getVisitRecord() {
			ArrayList<CallInfo> infos = DPDBHelper
					.getCallLogList(CallInfo.CALL_IN_UNACCEPT);
			ArrayList<Visit> list = new ArrayList<Visit>();
			if (infos != null) {
				for (int i = 0; i < infos.size(); i++) {
					if (infos.get(i).isDoor()) {
						CallInfo callInfo = infos.get(i);
						Visit visit = new Visit();
						visit.id = callInfo.getDb_id();
						visit.remoteCode = callInfo.getRemoteCode();
						visit.time = CommonUT.formatTime(callInfo.getStartTime());
						list.add(visit);
					}
				}
			}
			return list;
		}
		
		@Override
		public String getVisitPhoto(int id) {
			String path = "";
			ArrayList<CallInfo> infos = DPDBHelper.getCallLogList(
					 CallInfo.CALL_IN_UNACCEPT);
			if (infos == null) {
				return path;
			}
			for (CallInfo callInfo : infos) {
				if (callInfo.getDb_id() == id) {
					path = ConstConf.VISIT_LITTLE_PATH + File.separator 
							+ CommonUT.formatTime(callInfo.getStartTime())
							.replaceAll(":", "-") + ".jpg";
					break;
				}
			}
			return path;
		}

		@Override
		public String getVisitVoice(int id) {
			String path = "";
			ArrayList<CallInfo> infos = DPDBHelper.getCallLogList(
					 CallInfo.CALL_IN_UNACCEPT);
			if (infos == null) {
				return path;
			}
			for (CallInfo callInfo : infos) {
				if (callInfo.getDb_id() == id) {
					path = ConstConf.VISIT_PATH + File.separator 
							+ CommonUT.formatTime(callInfo.getStartTime())
							.replaceAll(":", "-") + ".wav";
					break;
				}
			}
			return path;
		};
		
		@Override
		public ArrayList<Message> getMessageLog() {
			ArrayList<MessageInfo> messageList = DPDBHelper.getMessageLogList(
					MessageInfo.PERSONAL | MessageInfo.PUBLIC);
			ArrayList<Message> list = new ArrayList<Message>();
			if (messageList != null) {
				for (int i = 0; i < messageList.size(); i++) {
					MessageInfo messageInfo = messageList.get(i);
					Message message = new Message();
					message.id = messageInfo.getDb_id();
					message.title = messageInfo.getTitle();
					message.time = messageInfo.getTime();
					list.add(message);
				}
			}
			return list;
		}
		
		@Override
		public String getMessagePhoto(int id) {
			String path = "";
			ArrayList<MessageInfo> messageList = DPDBHelper.getMessageLogList(
					MessageInfo.PERSONAL | MessageInfo.PUBLIC);
			if (messageList == null) {
				return path;
			}
			for (MessageInfo messageInfo : messageList) {
				if (messageInfo.getDb_id() == id) {
					String resName = messageInfo.getResName();
					if (!TextUtils.isEmpty(resName) && messageInfo.isJpg()) {
						path = ConstConf.MESSAGE_LITTLE_PATH + File.separator + resName;
					}
					break;
				}
			}
			return path;
		}
		
		@Override
		public int getCallInSize() {
			return DPFunction.getCallInSize();
		}

		@Override
		public int getCallOutSize() {
			return DPFunction.getCallOutSize();
		}

		@Override
		public boolean openLock() {
			int callInCount = getCallInSize();
			if (callInCount == 1) {
				CallInfo callInfo = getCallInList().get(0);
				if (callInfo != null && callInfo.isDoor()) {
					return DPFunction.openLock(callInfo.getRemoteCode());
				}
			}
			return false;
		}

		@Override
		public void accept() {
			int callInCount = getCallInSize();
			if (callInCount == 1) {
				CallInfo callInfo =  getCallInList().get(0);
				if (callInfo != null && callInfo.getAcceptTime() == 0) {
					if (DPFunction.accept(callInfo.getSessionID(), 2)) {
						setLocalVideoVisable(callInfo.getSessionID(), false);
						// 将收到的(音视频)数据转到指定IP和端口
						boolean result = getJniPhoneClass().SetRelayMode(
								callInfo.getSessionID(), true, Constant.LOCAL_PORT,
								Constant.REMOTE_IP, Constant.REMOTEPORT);
						if (result) {
							MyLog.print("将收到的(音视频)数据转到指定IP和端口成功");
						} else {
							MyLog.print("将收到的(音视频)数据转到指定IP和端口失败");
						}
						phoneAccept = true;
						Intent intent = new Intent();
						intent.putExtra(MSG_TYPE, JniPhoneClass.MSG_PHONE_ACCEPT);
						intent.setAction(CallReceiver.CALL_IN_ACTION);
						mContext.sendBroadcast(intent);
					}
				}
			}
		}

		@Override
		public void hangUp(int sessionID) {
			if (getCallInList() != null) {
				int callInCount = getCallInSize();
				if (callInCount == 1) {
					if (!DPFunction.isCallAccept) {
						CallInfo callInfo = getCallInList().get(0);
						MyLog.print("callInSession "
								+ callInfo.getSessionID());
						if (phoneAccept) {
							callHangUp();
						}
						Intent intent = new Intent();
						intent.putExtra(MSG_TYPE,
								JniPhoneClass.MSG_PHONE_HANGUP);
						intent.putExtra(SESSION_ID, sessionID);
						intent .setAction(CallReceiver.CALL_IN_ACTION);
						mContext.sendBroadcast(intent);
					}
				}
			}
		}

		@Override
		public void connectChange(boolean isConnected, String reason) {
			MyLog.print(MyLog.ERROR, TAG, 
					"收到登录状态的回调: isOnline = " + isConnected + " reason = " + reason);
			mContext.sendBroadcast(new Intent(ACTION_CLOUD_LOGIN_CHANGED));
		}

		@Override
		public void onBindTel(String tel, boolean isBind) {
			MyLog.print(MyLog.ERROR, TAG, 
					"收到绑定回调: isBind = " + isBind + " tel = " + tel);
			mContext.sendBroadcast(new Intent(ACTION_CLOUD_BIND_CHANGED));
		}
		
		@Override
		public List<String> getAccountList() {
			return DPDBHelper.getAccountList();
		}

		@Override
		public int addAccount(String account) {
			return DPDBHelper.addAccount(account);
		}

		@Override
		public void deleteAccount(String account) {
			DPDBHelper.delAccount(account);
		}

		@Override
		public void clearAccount() {
			DPDBHelper.clearAccount();
		}
	};
	
	/**
	 * 通知已登录的手机,有新的报警
	 * @param alarmArea
	 *            报警防区
	 * @param alarmType
	 *            报警类型
	 */
	public static void toPhoneAlarm(int areaNum, int alarmArea, int alarmType, String time) {
		CloudIntercom.toPhoneAlarm(areaNum, alarmArea, alarmType, time);
	}
	
	/**
	 * 同步安防模式到手机
	 */
	public static void synchPhoneSafeMode() {
		CloudIntercom.synchPhoneSafeMode(getSafeMode());
	}
	
	/**
	 * 同步智能家居模式到手机
	 */
	public static void synchPhoneSmartHomeMode() {
		CloudIntercom.synchPhoneSmartHomeMode(getSmartHomeMode());
	}
	
	/** 挂断手机 */
	public static void toPhoneHangUp() {
		CloudIntercom.toPhoneHangUp();
	}
	
	/** 呼叫所有手机 */
	public static void callPhone() {
		CloudIntercom.callPhone();
	}
	
	/**
	 * @功能：通知已登录的手机，网关收到了新信息
	 * @参数1：boolean isPersonal: true-个人信息 false-公共信息
	 * @参数2：收到新信息的条数
	 */
	public static void toPhoneMessage(boolean isPersonal, int count) {
		CloudIntercom.toPhoneMessage(isPersonal, count);
	}
	
	/** 登录到服务器 */
	public static void toStartLogin() {
		CloudIntercom.toStartLogin();
	}
	
	/** 帐号是否在线 */
	public static boolean isOnline() {
		return CloudIntercom.isOnline();
	}
	
	/**
	 * 获取室内机的帐号
	 */
	public static String getAccount() {
		return CloudIntercom.getAccount();
	}
	
	/**
	 * 设置网卡（根据设置的网卡的网络状态来进行登录和退出登录，默认是无线网卡）
	 * @param isEthernet true-以太网"eth1", false-无线 
	 */
	public static void setEthernet(boolean isEthernet) {
		CloudIntercom.setEthernet(isEthernet);
	}
	
	/**
	 * 获取绑定室内机的手机帐号列表
	 */
	public static List<String> getPhoneList() {
		return DPDBHelper.getAccountList();
	}
}
