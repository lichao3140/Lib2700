package com.dpower.domain;

import com.dpower.util.MyLog;

/**
 * @类名：CallInfo
 * @描述：记录DP楼宇会话的信息
 * @备注：会话ID、会话对方房号、会话呼入或呼出的时间、接听时间、挂断时间、是否开锁等
 */
public class CallInfo {
	
	/** 呼出已接听 */
	public static final int CALL_OUT_ACCEPT = 1 << 0;
	/** 呼出未接听 */
	public static final int CALL_OUT_UNACCEPT = 1 << 1;
	/** 呼入已接听 */
	public static final int CALL_IN_ACCEPT = 1 << 2;
	/** 呼入未接听 */
	public static final int CALL_IN_UNACCEPT = 1 << 3;
	/** 监视已接听 */
	public static final int SEE_ACCEPT = 1 << 4;
	/** 监视未接听 */
	public static final int SEE_UNACCEPT = 1 << 5;
	
	private int mDB_id = 0;
	private int mSessionID = 0;
	private String mRemoteCode = null;
	private long mStartTime = 0;
	private long mAcceptTime = 0;
	private long mEndTime = 0;
	private int mType = 0;
	private boolean mIsRead = false;
	private boolean mIsOpenLock = false;
	private boolean mIsHangUp = true;
	private boolean mIsSuccess = false; //是否成功上报管理中心
	private boolean mCalledPhone = false;
	private boolean mAutoAccepted = false;

	public CallInfo() {
		
	}

	public CallInfo(int db_id, String remoteCode, int type, long startTime,
			long acceptTime, long endTime, boolean isOpenLock, boolean isRead) {
		mDB_id = db_id;
		mSessionID = 0;
		mRemoteCode = remoteCode;
		mStartTime = startTime;
		mAcceptTime = acceptTime;
		mEndTime = endTime;
		mType = type;
		mIsRead = isRead;
		mIsOpenLock = isOpenLock;
		mIsHangUp = true;
	}

	/** 判断是否为门口机 */
	public boolean isDoor() {
		if (mRemoteCode != null) {
			int type = Integer.valueOf(new String(mRemoteCode.substring(0, 1)));
			return type == 2 || type == 3 || type == 7;
		}
		MyLog.print("isDoor error, remotecode is null");
		return false;
	}
	
	/**判断是不是管理中心*/
	public boolean isManageCenter(){
		return (mRemoteCode.substring(0, 1).equals("8"));
	}

	public int getSessionID() {
		return mSessionID;
	}

	public void setSessionID(int sessionID) {
		mSessionID = sessionID;
	}

	public long getStartTime() {
		return mStartTime;
	}

	public void setStartTime() {
		mStartTime = System.currentTimeMillis();
	}

	public long getAcceptTime() {
		return mAcceptTime;
	}

	public void setAcceptTime() {
		mAcceptTime = System.currentTimeMillis();
	}

	public long getEndTime() {
		return mEndTime;
	}

	public void setEndTime() {
		mEndTime = System.currentTimeMillis();
	}

	public int getType() {
		return mType;
	}

	public void setType(int type) {
		mType = type;
	}

	public boolean isRead() {
		return mIsRead;
	}

	public void setIsRead(boolean isRead) {
		mIsRead = isRead;
	}

	public String getRemoteCode() {
		return mRemoteCode;
	}

	public void setRemoteCode(String remoteCode) {
		mRemoteCode = remoteCode;
	}

	public boolean isOpenLock() {
		return mIsOpenLock;
	}

	public void setIsOpenLock(boolean isOpenLock) {
		mIsOpenLock = isOpenLock;
	}

	public boolean isHangUp() {
		return mIsHangUp;
	}

	public void setIsHangUp(boolean isHangUp) {
		mIsHangUp = isHangUp;
	}

	public int getDb_id() {
		return mDB_id;
	}

	public void setDb_id(int db_id) {
		mDB_id = db_id;
	}
	
	public boolean getIsSuccess() {
		return mIsSuccess;
	}
	
	public void setIsSuccess(boolean isSuccess) {
		mIsSuccess = isSuccess;
	}
	
	public boolean getCalledPhone() {
		return mCalledPhone;
	}
	
	public void setCalledPhone(boolean called) {
		mCalledPhone = called;
	}
	
	public boolean getAutoAccepted() {
		return mAutoAccepted;
	}
	
	public void setAutoAccepted(boolean accepted) {
		mAutoAccepted = accepted;
	}

	public String toString() {
		return "CallInfo:db_id=" + getDb_id() + ",sessionID=" + getSessionID()
				+ ",code=" + getRemoteCode() + ",type=" + getType()
				+ ",startTime=" + getStartTime() + ",acceptTime="
				+ getAcceptTime() + ",endTime=" + getEndTime() + ",isRead="
				+ isRead() + ",isOpenLock=" + isOpenLock() + ",isHangUp="
				+ isHangUp();
	}
}
