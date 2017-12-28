package com.dpower.domain;

public class SafeModeInfo {

	private int mDB_id = 0;
	private int mMode = 0;
	private long mTime = 0;
	private boolean mIsSuccess = false; //是否成功上报管理中心
	
	public SafeModeInfo(int mode, long time) {
		mMode = mode;
		mTime = time;
	}

	public SafeModeInfo(int db_id, int mode, long time) {
		mDB_id = db_id;
		mMode = mode;
		mTime = time;
	}

	public int getDb_id() {
		return mDB_id;
	}

	public int getMode() {
		return mMode;
	}

	public void setMode(int mode) {
		mMode = mode;
	}

	public long getTime() {
		return mTime;
	}

	public void setTime(long time) {
		mTime = time;
	}
	
	public boolean getIsSuccess() {
		return mIsSuccess;
	}
	
	public void setIsSuccess(boolean isSuccess) {
		mIsSuccess = isSuccess;
	}
}
