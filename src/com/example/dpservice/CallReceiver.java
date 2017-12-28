package com.example.dpservice;

import com.dpower.callback.IntercomCallback;
import com.dpower.function.DPFunction;
import com.dpower.util.MyLog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class CallReceiver extends BroadcastReceiver {
	
	/** 请在服务中接收此广播,界面中不要注册此广播 */
	public static final String CALL_ACTION = "android.intent.action.DPCALL";
	/** 请在呼入界面中接收此广播 */
	public static final String CALL_IN_ACTION = "android.intent.action.CALLIN";
	/** 请在呼出界面中接收此广播 */
	public static final String CALL_OUT_ACTION = "android.intent.action.CALLOUT";
	/** 请在监视界面中接收此广播 */
	public static final String SEE_ACTION = "android.intent.action.SEE";
	private IntercomCallback mCallback = null;
	private IntentFilter mFilter = null;

	public CallReceiver(IntercomCallback back, String action) {
		mCallback = back;
		mFilter = new IntentFilter(action);
	}

	public CallReceiver(IntercomCallback back) {
		mCallback = back;
		mFilter = new IntentFilter(CALL_ACTION);
	}

	public IntentFilter getFilter() {
		return mFilter;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		int CallSessionID = intent.getIntExtra(DPFunction.SESSION_ID, 0);
		int MsgType = intent.getIntExtra(DPFunction.MSG_TYPE, 0);
		String MsgContent = intent.getStringExtra(DPFunction.MSG_CONTENT);
		String roomCode = intent.getStringExtra(DPFunction.REMOTE_CODE);
		MyLog.print("CallReceiver", "" + MsgType + ", " + MsgContent);
		switch (MsgType) {
			case JniPhoneClass.MSG_PHONE_ACCEPT:
				mCallback.onPhoneAccept();
				break;
			case JniPhoneClass.MSG_PHONE_HANGUP:
				mCallback.onPhoneHangUp();
				break;
			case JniPhoneClass.MSG_RING_TIMEOUT:
				mCallback.onRingTimeOut(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_TALK_TIMEOUT:
				mCallback.onTalkTimeOut(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_MONITOR_TIMEOUT:
				mCallback.onMonitorTimeOut(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.CALLACK_RING:
				mCallback.onAckRing(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.CALLACK_BUSY:
				MsgContent = roomCode;
				mCallback.onAckBusy(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.CALLACK_NOMEDIA:
				mCallback.onAckNoMeia(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.CALLACK_HOLD:
				mCallback.onAckHold(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_CALLACK: // 2000
				mCallback.onCallOutAck(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_CALLIN:
				mCallback.onNewCallIn(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_REMOTE_HANGUP:// 2003
				mCallback.onRemoteHangUp(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_REMOTE_ACCEPT:
				mCallback.onRemoteAccept(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_REMOTE_HOLD:
				mCallback.onRemoteHold(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_REMOTEWAKE:
				mCallback.onRemoteWake(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_ERROR: // 2007
				MsgContent = roomCode;
				mCallback.onError(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_MESSAGE:
				mCallback.onMessage(CallSessionID, MsgType, MsgContent);
				break;
			case JniPhoneClass.MSG_MESSAGE_ERROR:
				mCallback.onMessageError(CallSessionID, MsgType, MsgContent);
				break;
			default:
				break;
		}
	}
}