package com.dpower.domain;

/**
 * @类名：AlarmInfo
 * @描述：安防模式的信息类
 * @类私有成员：areaName-安防区域,areaType-安防类型,enable-是否启用,open-常开(true) or 常闭(false),delayTime-延时时间
 */
public class AlarmInfo {
	
	/** 安防防区 */
	public String areaName;
	
	/** 安防类型 */
	public String areaType;
	
	/** 是否启用 */
	public boolean enable;
	
	/** 常开(true) or 常闭(false) */
	public boolean open;
	
	/** 延时时间 */
	public int delayTime;
	
	public AlarmInfo(String areaName, String areaType,
			boolean enable, boolean open, int time) {
		this.areaName = areaName;
		this.areaType = areaType;
		this.enable = enable;
		this.open = open;
		this.delayTime = time;
	}

	@Override
	public String toString() {
		return "areaName:" + areaName + ",areaType:" + areaType
				+ ",enable:" + enable + ", open:" + open + ",delayTime:" + delayTime;
	}
}
