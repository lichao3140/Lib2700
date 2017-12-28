package com.dpower.domain;

public class AlarmTypeInfo {
	
	public String name;
	/** 0-门铃、1-紧急、2-烟感煤气、3-门磁红外 */
	public int priority;
	public int value;

	public AlarmTypeInfo(String name, int priority, int value) {
		this.name = name;
		this.priority = priority;
		this.value = value;
	}

	@Override
	public String toString() {
		return "name:" + name + ",priority:" + priority + ",value:" + value;
	}
}
