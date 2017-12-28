package com.example.dpservice;

import com.dpower.function.DPFunction;
import com.dpower.util.MyLog;

import android.content.Context;
import android.content.Intent;

/**
 * @类名：JniPhoneClass
 * @描述：DP对讲类 该类方法均在JNI实现,该类为单例类并且该类包名必须固定为com.example.dpservice
 */
public class JniPhoneClass {
	
	private static JniPhoneClass mInstance = null;
	private static Context mContext = null;

	private JniPhoneClass() {
		   mContext= DPFunction.getContext();
	}

	public static JniPhoneClass getInstance() {
		if (mInstance == null) {
			mInstance = new JniPhoneClass();
		}
		return mInstance;
	}

	/**
	 * @方法名：Init
	 * @功能：开启对讲监听
	 * @参数1：String localCode - 本机当前房号
	 * @参数2：String localIpAddr - 本机当前IP地址
	 * @返回值：boolean ture-成功 false-失败
	 * @备注：与UnInit()搭配使用
	 */
	public native boolean Init(String localCode, String localIpAddr);

	/**
	 * @方法名：UnInit
	 * @功能：結束对讲监听
	 */
	public native void UnInit();

	/**
	 * @方法名：CallOut
	 * @功能：呼叫指定房号
	 * @参数1：String RemoteCode - 对方房号("1010101010101")
	 * @参数2：String RemoteipAddr - 对方IP地址("192.168.1.21")
	 * @返回值：0-失败,非0-CallSessionID
	 */
	public native int CallOut(String RemoteCode, String RemoteipAddr);

	/**
	 * @方法名：Monitor
	 * @功能：监控指定门口机
	 * @参数1：String RemoteCode - 对方房号("3010101010101")
	 * @参数2：String RemoteipAddr - 对方IP地址("192.168.1.20")
	 * @返回值：0-失败,非0-CallSessionID
	 */
	public native int Monitor(String RemoteCode, String RemoteipAddr);

	/**
	 * @方法名：Accept
	 * @功能：接听指定ID的会话
	 * @参数：int CallSessionID - ID
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean Accept(int CallSessionID);

	/**
	 * @方法名：HangUp
	 * @功能：挂断指定ID的会话
	 * @参数：int CallSessionID - ID
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean HangUp(int CallSessionID);

	/**
	 * @方法名：SetVideoDisplayArea
	 * @功能：设置对方视频在本机屏幕的显示区域
	 * @参数1：int CallSessionID - ID
	 * @参数2：int index - 保留，始终为0
	 * @参数3：int x - 显示区域左上角横坐标
	 * @参数4：int y - 显示区域左上角纵坐标
	 * @参数5：int w - 显示区域的宽
	 * @参数6：int h - 显示区域的高
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean SetVideoDisplayArea(int CallSessionID, int index,
			int x, int y, int w, int h);

	/**
	 * @方法名：CaptureVideoImage
	 * @功能：从指定ID会话中解码视频抓图一张，保存为JPG格式数据
	 * @参数1：int CallSessionID - ID
	 * @参数2：int index - 保留，始终为0
	 * @参数3：byte[] ImageBuffer - 保存后的数据，缓冲区ImageBuffer的大小应大于等于解码视频(图像宽*图像高/4)
	 * @返回值：0-失败,非0-成功
	 */
	public native int CaptureVideoImage(int CallSessionID, int index,
			byte[] ImageBuffer);

	/**
	 * @方法名：SetAudioVolume
	 * @功能：设置指定ID会话的通话音量
	 * @参数1：int CallSessionID - ID
	 * @参数2：int Percent - 音量百分比
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean SetAudioVolume(int CallSessionID, int Percent);

	/**
	 * @方法名：SetTVoutDisplayArea
	 * @功能：设置电视输出（模拟副分机）显示区域
	 * @参数1：int CallSessionID - ID
	 * @参数2：int index - 保留，始终为0
	 * @参数3：int x - 显示区域左上角横坐标
	 * @参数4：int y - 显示区域左上角纵坐标
	 * @参数5：int w - 显示区域的宽
	 * @参数6：int h - 显示区域的高
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean SetTVoutDisplayArea(int CallSessionID, int index,
			int x, int y, int w, int h);

	/**
	 * @方法名：Transfer
	 * @功能：呼叫转移 当新呼入进来时,如果不接听而需要转移时,调用此函数给出转移的号码.然后调用HangUp函数来挂断此会话
	 * @参数1：int CallSessionID - ID
	 * @参数2：String TransferCode - 转移房号("3010101010101")
	 * @备注：呼叫对方时,如果对方发来呼叫转移消息时,呼叫库会有回调消息MSG_CALLDISTRACT通知应用程序做相应处理
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean Transfer(int CallSessionID, String TransferCode);

	/**
	 * @方法名：SetLocalVideoVisable
	 * @功能：控制本地视频是否对方可见 请在调用SetVideoDisplayArea前一行调用它，来设置初始默认值。然后当用户按视频开关按钮时也调用
	 * @参数1：int CallSessionID - ID
	 * @参数2：int index - 保留，始终为0
	 * @参数3：boolean enable - 保留，始终为0
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean SetLocalVideoVisable(int CallSessionID, int index,
			boolean enable);

	/**
	 * @方法名：SetRelayMode
	 * @功能：设置转播 将收到的(音视频)数据转到指定IP和端口
	 * @参数1：int CallSessionID - ID
	 * @参数2：boolean enable - 保留，始终为true
	 * @参数3：int LocalPort - 本机当前接收数据的端口
	 * @参数4：String RemoteIP - 对方IP
	 * @参数5：int RemotePort - 对方端口号
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean SetRelayMode(int CallSessionID, boolean enable,
			int LocalPort, String RemoteIP, int RemotePort);

	/**
	 * @方法名：SetFilePlayMode
	 * @功能：播放应答留言 请在会话接听后调用
	 * @参数1：int CallSessionID - ID
	 * @参数2：boolean enable - 保留，始终为true
	 * @参数3：String FileName - 播放的音频文件绝对路径
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean SetFilePlayMode(int CallSessionID, boolean enable,
			String FileName);

	/**
	 * @方法名：SetFileRecordMode
	 * @功能：录制访客留言 请在会话接听后调用
	 * @参数1：int CallSessionID - ID
	 * @参数2：boolean enable - 保留，始终为true
	 * @参数3：String FileName - 录制的音频文件绝对路径
	 * @返回值：boolean false-失败,true-成功
	 */
	public native boolean SetFileRecordMode(int CallSessionID, boolean enable,
			String FileName);

	/**
	 * @方法名：MessageCallback
	 * @描述：JNI回调函数 收到消息后请在服务类中处理消息(避免死锁,避免界面finish()后收不到)
	 * @功能：对讲消息通知
	 * @参数1：int CallSessionID - ID
	 * @参数2：int MsgType - 消息类型
	 * @参数3：String MsgContent - 消息参数
	 * @返回值：0
	 */
	private int MessageCallback(int CallSessionID, int MsgType,
			String MsgContent) {
		MyLog.print("MessageCallback SessionID = " + CallSessionID
				+ ",MsgType = " + MsgType + ",MsgContent = " + MsgContent);
		Intent intent = new Intent();
		intent.setAction(CallReceiver.CALL_ACTION);
		intent.putExtra(DPFunction.SESSION_ID, CallSessionID);
		intent.putExtra(DPFunction.MSG_TYPE, MsgType);
		intent.putExtra(DPFunction.MSG_CONTENT, MsgContent);
		if (mContext != null) {
			mContext.sendBroadcast(intent);
		} else {
			MyLog.print("JniPhoneClass", "mContext is null");
		}
		return 0;
	}

	/** @手机挂断 */
	public static final int MSG_PHONE_HANGUP = 1988;
	/** @手机接听 */
	public static final int MSG_PHONE_ACCEPT = 1989;
	
	/** @呼出振铃超时 */
	public static final int MSG_RING_TIMEOUT = 1990;
	/** @通话超时 */
	public static final int MSG_TALK_TIMEOUT = 1991;
	/** @监视超时 */
	public static final int MSG_MONITOR_TIMEOUT = 1992;

	/** @呼出结果 呼叫挂起 */
	public static final int CALLACK_HOLD = 1996;
	/** @呼出结果 没有媒体 */
	public static final int CALLACK_NOMEDIA = 1997;
	/** @呼出结果 对方占线 */
	public static final int CALLACK_BUSY = 1998;
	
	/** @JNI回调消息 对方振铃 */
	public static final int CALLACK_RING = 1999;
	/** @JNI回调消息 呼出结果返回 */
	public static final int MSG_CALLACK = 2000;
	/** @JNI回调消息 呼叫转移 */
	public static final int MSG_CALLDISTRACT = 2001;
	/** @JNI回调消息 新呼入 */
	public static final int MSG_CALLIN = 2002;
	/** @JNI回调消息 对方挂断 */
	public static final int MSG_REMOTE_HANGUP = 2003;
	/** @JNI回调消息 对方接听 */
	public static final int MSG_REMOTE_ACCEPT = 2004;
	/** @JNI回调消息 对方挂起 */
	public static final int MSG_REMOTE_HOLD = 2005;
	/** @JNI回调消息 对方唤醒 */
	public static final int MSG_REMOTEWAKE = 2006;
	/** @JNI回调消息 呼叫出错 */
	public static final int MSG_ERROR = 2007;
	/** @JNI回调消息 保留 */
	public static final int MSG_MESSAGE = 2008;
	/** @JNI回调消息 保留 */
	public static final int MSG_MESSAGE_ERROR = 2009;

	/** @呼出结果描述 占线 */
	public static final String CALL_BUSY = "busy";
	/** @呼出结果描述 没有媒体 */
	public static final String CALL_NOMEDIA = "nomedia";
	/** @JNI呼出结果描述 对方振铃 */
	public static final String CALL_RING = "ring";
	/** @JNI呼出结果描述 呼叫挂起 */
	public static final String CALL_HOLD = "hold";

	static {
		System.loadLibrary("PhoneCore");
	}
}
