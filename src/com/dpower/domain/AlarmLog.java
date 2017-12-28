package com.dpower.domain;

public class AlarmLog {
	
	private int mDB_id = 0;
	private int mMode = 0;
	private int mAreaNum = 0;
	private int mAreaName = 0;
	private int mAreaType = 0;
	private long mTime = 0;
	private boolean mIsSuccess = false; //是否成功上报管理中心
	private boolean mIsRead = false;

	public AlarmLog(int db_id, int mode, int areaNum, int areaName, int areaType, long time) {
		mDB_id = db_id;
		mMode = mode;
		mAreaNum = areaNum;
		mAreaName = areaName;
		mAreaType = areaType;
		mTime = time;
	}

	public AlarmLog(int mode, int areaNum, int areaName, int areaType, long time) {
		mMode = mode;
		mAreaNum = areaNum;
		mAreaName = areaName;
		mAreaType = areaType;
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
	
	public int getAreaNum() {
		return mAreaNum;
	}
	
	public void setAreaNum(int areaNum) {
		mAreaNum = areaNum;
	}

	public int getAreaName() {
		return mAreaName;
	}

	public void setAreaName(int areaName) {
		mAreaName = areaName;
	}

	public int getAreaType() {
		return mAreaType;
	}

	public void setAreaType(int areaType) {
		this.mAreaType = areaType;
	}

	public long getTime() {
		return mTime;
	}

	public void setTime(int time) {
		mTime = time;
	}
	
	public boolean getIsSuccess() {
		return mIsSuccess;
	}
	
	public void setIsSuccess(boolean isSuccess) {
		mIsSuccess = isSuccess;
	}
	
	public boolean getIsRead() {
		return mIsRead;
	}
	
	public void setIsRead(boolean isRead) {
		mIsRead = isRead;
	}
}
