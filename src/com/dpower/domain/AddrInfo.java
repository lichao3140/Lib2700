package com.dpower.domain;

import java.util.ArrayList;

import com.dpower.util.MyLog;

/**
 * @类名：AddrInfo
 * @描述：网络配置表中CODE对应的IP信息类
 * @类私有成员：mIp-IP地址,mMask-子网掩码,mGW-网关,mCode-房号,mName-中文名称
 */
public class AddrInfo {
	
	private String mIp;
	private String mMask;
	private String mGW;
	private String mCode;
	private String mName;

	public String getIp() {
		return mIp;
	}

	public void setIp(String ip) {
		mIp = ip;
	}
	
	public String getCode() {
		return mCode;
	}

	public void setCode(String code) {
		mCode = code;
	}

	public String getMask() {
		return mMask;
	}

	public void setMask(String mask) {
		mMask = mask;
	}

	public String getGw() {
		return mGW;
	}

	public void setGw(String gw) {
		mGW = gw;
	}

	public String getName() {
		return mName;
	}

	public void setName(String name) {
		mName = name;
	}

	public String toString() {
		return " AddrInfo code:" + mCode + ", ip:" + mIp + ", mask:" + mMask
				+ ", gw:" + mGW + ", name:" + mName;
	}

	/**
	 * @静态方法名：parsingAddrInfo
	 * @描述：解析XML格式字符串转换成AddrInfo数组列表
	 * @参数：String info - XML格式字符串
	 * @返回值：参数为null则返回null,否则返回AddrInfo数组列表
	 */
	public static ArrayList<AddrInfo> parsingAddrInfo(String info) {
		if (info == null) {
			MyLog.print("parsingAddrInfo - > Parameter is not correct");
			return null;
		}
		ArrayList<AddrInfo> list = new ArrayList<AddrInfo>();
		while (true) {
			int start = info.indexOf("<info>");
			int end = info.indexOf("</info>");
			if (start == -1 || end == -1) {
				break;
			}
			String infoitem = new String(info.substring(start + 6, end));
			AddrInfo addrinfo = new AddrInfo();
			String val = getXmlVal("ip", infoitem);
			if (val != null) {
				addrinfo.setIp(val);
			}
			val = getXmlVal("mask", infoitem);
			if (val != null) {
				addrinfo.setMask(val);
			}
			val = getXmlVal("gw", infoitem);
			if (val != null) {
				addrinfo.setGw(val);
			}
			val = getXmlVal("code", infoitem);
			if (val != null) {
				addrinfo.setCode(val);
			}
			val = getXmlVal("name", infoitem);
			if (val != null) {
				addrinfo.setName(val);
			}
			list.add(addrinfo);
			info = new String(info.substring(end + 7));
		}
		return list;
	}
	
	/**
	 * @静态方法名：getXmlVal
	 * @功能：查找源字符串中的子字符串所包含的字符串，XML解析键值对
	 * @参数1：String key - 子字符串
	 * @参数2：String src - 源字符串
	 * @返回值：String - key不存在返回null,否则返回key包含的字符串
	 */
	private static String getXmlVal(String key, String src) {
		String strStart = "<" + key + ">";
		String strEnd = "</" + key + ">";
		int start = src.indexOf(strStart);
		if (start == -1) {
			MyLog.print("getXmlVal -> start key does not exist");
			return null;
		}
		int end = src.indexOf(strEnd);
		if (end == -1) {
			MyLog.print("getXmlVal -> end key does not exist");
			return null;
		}
		String strval = new String(
				src.substring(start + strStart.length(), end));
		return strval;
	}
}
