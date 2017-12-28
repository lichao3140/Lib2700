package com.dpower.callback;

/**
 * 库的全部回调
 * @author Administrator
 *
 */
public interface IntercomCallback {
	
	/** 呼出振铃超时 */
	public void onRingTimeOut(int CallSessionID, int MsgType, String MsgContent);
	/** 通话超时 */
	public void onTalkTimeOut(int CallSessionID, int MsgType, String MsgContent);
	/** 监视超时 */
	public void onMonitorTimeOut(int CallSessionID, int MsgType, String MsgContent);
	/** 对方振铃 */
	public void onAckRing(int CallSessionID, int MsgType, String MsgContent);
	/** 对方占线 */
	public void onAckBusy(int CallSessionID, int MsgType, String MsgContent);
	/** 没有媒体 */
	public void onAckNoMeia(int CallSessionID, int MsgType, String MsgContent);
	/** 呼叫挂起 */
	public void onAckHold(int CallSessionID, int MsgType, String MsgContent);
	/** 呼出结果返回 */
	public void onCallOutAck(int CallSessionID, int MsgType, String MsgContent);
	/** 新呼入 */
	public void onNewCallIn(int CallSessionID, int MsgType, String MsgContent);
	/** 对方挂断 */
	public void onRemoteHangUp(int CallSessionID, int MsgType, String MsgContent);
	/** 对方接听 */
	public void onRemoteAccept(int CallSessionID, int MsgType, String MsgContent);
	/** 对方挂起 */
	public void onRemoteHold(int CallSessionID, int MsgType, String MsgContent);
	/** 对方唤醒 */
	public void onRemoteWake(int CallSessionID, int MsgType, String MsgContent);
	/** 呼叫出错 */
	public void onError(int CallSessionID, int MsgType, String MsgContent);
	/** 保留 */
	public void onMessage(int CallSessionID, int MsgType, String MsgContent);
	/** 保留 */
	public void onMessageError(int CallSessionID, int MsgType, String MsgContent);

	/** 手机接听 */
	public void onPhoneAccept();
	/** 手机挂断 */
	public void onPhoneHangUp();
}
