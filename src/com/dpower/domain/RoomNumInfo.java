package com.dpower.domain;

import java.util.ArrayList;

import com.dpower.util.MyLog;

public class RoomNumInfo {

	private String mArea;
	private String mBuild;
	private String mUnilt;
	private String mRoom;
	private String mExtension;

	/**
	 * 获取区号
	 * @return
	 */
	public String getArea() {
		return mArea;
	}

	/**
	 * 设置区号
	 * @param area
	 *            长度2位，不足两位请用0补齐
	 * @return
	 */
	public boolean setArea(String area) {
		if (area.length() == 2) {
			mArea = area;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取栋号
	 * @return
	 */
	public String getBuilding() {
		return mBuild;
	}

	/**
	 * 设置栋号
	 * @param building
	 *            长度2位，不足两位请用0补齐
	 * @return
	 */
	public boolean setBuilding(String building) {
		if (building.length() == 2) {
			mBuild = building;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取单元号
	 * @return
	 */
	public String getUnit() {
		return mUnilt;
	}

	/**
	 * 设置单元号
	 * @param unit
	 *            长度2位，不足两位请用0补齐
	 * @return
	 */
	public boolean setUnit(String unit) {
		if (unit.length() == 2) {
			mUnilt = unit;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取房号
	 * @return
	 */
	public String getRoom() {
		return mRoom;
	}

	/**
	 * 设置房号
	 * @param room
	 *            长度4位，不足两位请用0补齐
	 * @return
	 */
	public boolean setRoom(String room) {
		if (room.length() == 4) {
			mRoom = room;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取分机号
	 * @return
	 */
	public String getExtension() {
		return mExtension;
	}

	/**
	 * 设置分机号
	 * @param extension
	 *            长度2位，不足请用0补齐
	 * @return
	 */
	public boolean setExtension(String extension) {
		if (extension.length() == 2) {
			mExtension = extension;
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取12位完整号码
	 * @return
	 */
	public String getString() {
		return mArea + mBuild + mUnilt + mRoom
				+ mExtension;
	}

	/**
	 * @静态方法名：parsingRoomNumInfo
	 * @描述：解析XML格式字符串转换成RoomNumInfo数组列表
	 * @param String
	 *            infoXml - XML格式字符串
	 * @return 参数为null则返回null,否则返回RoomNumInfo数组列表
	 */
	public static ArrayList<RoomNumInfo> parsingRoomNumInfo(String infoXml) {
		if (infoXml == null) {
			MyLog.print("parsingRoomNumInfo - > Parameter is not correct");
			return null;
		}
		ArrayList<RoomNumInfo> list = new ArrayList<RoomNumInfo>();
		while (true) {
			int start = infoXml.indexOf("<code>");
			int end = infoXml.indexOf("</code>");
			if (start == -1 || end == -1) {
				break;
			}
			String infoItem = new String(infoXml.substring(start + 6, end));
			if (infoItem != null) {
				if (infoItem.substring(0, 1).equals("1")) {
					RoomNumInfo addrinfo = new RoomNumInfo();
					addrinfo.setArea(infoItem.substring(1, 3));
					addrinfo.setBuilding(infoItem.substring(3, 5));
					addrinfo.setUnit(infoItem.substring(5, 7));
					addrinfo.setRoom(infoItem.substring(7, 11));
					addrinfo.setExtension(infoItem.substring(11, 13));
					list.add(addrinfo);
				}
			}
			infoXml = new String(infoXml.substring(end + 7));
		}
		return list;
	}
}
