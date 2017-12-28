package com.dpower.domain;

public class MessageInfo {
	
	public static final String ACTION_MESSAGE = "android.intent.action.MESSAGE";
	public static final int PERSONAL = 1;
	public static final int PUBLIC = 1 << 1;
	private int mDB_id = 0;
	private String mTime = null;
	private String mTitle = null;
	private String mBody = null;
	private String mResName = null;
	private boolean mIsJpg = true;
	private boolean mRead = false;
	private boolean mPersonal = true;

	public MessageInfo() {

	}
	
	public MessageInfo(int db_id, String time, String title, String body, String resName,
			boolean isJpg, boolean personal, boolean read) {
		mDB_id = db_id;
		mTime = time;
		mTitle = title;
		mBody = body;
		mResName = resName;
		mIsJpg = isJpg;
		mPersonal = personal;
		mRead = read;
	}

	public MessageInfo(String time, String title, String body, String resName, boolean isJpg,
			boolean personal, boolean read) {
		mTime = time;
		mTitle = title;
		mBody = body;
		mResName = resName;
		mIsJpg = isJpg;
		mPersonal = personal;
		mRead = read;
	}

	public boolean isRead() {
		return mRead;
	}

	public void setRead(boolean read) {
		mRead = read;
	}

	public boolean isPersonal() {
		return mPersonal;
	}

	public void setPersonal(boolean personal) {
		mPersonal = personal;
	}

	public String getTime() {
		return mTime;
	}

	public void setTime(String time) {
		mTime = time;
	}

	public String getTitle() {
		return mTitle;
	}

	public void setTitle(String title) {
		mTitle = title;
	}

	public String getBody() {
		return mBody;
	}

	public void setBody(String body) {
		mBody = body;
	}

	public String getResName() {
		return mResName;
	}

	public void setResName(String resName) {
		mResName = resName;
	}

	public boolean isJpg() {
		return mIsJpg;
	}

	public void setIsJpg(boolean isJpg) {
		mIsJpg = isJpg;
	}

	public int getDb_id() {
		return mDB_id;
	}

	@Override
	public String toString() {
		return "MessageInfo:db_id=" + getDb_id() + ",time=" + getTime()
				+ ",title=" + getTitle() + ",body=" + getBody() + ",resName="
				+ getResName() + ",isJpg=" + isJpg();
	}
}
