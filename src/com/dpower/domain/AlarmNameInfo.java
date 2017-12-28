package com.dpower.domain;

public class AlarmNameInfo {
	
	public String name;
	public int value;

	public AlarmNameInfo(String name, int value) {
		this.name = name;
		this.value = value;
	}

	@Override
	public String toString() {
		return "name:" + name + ",value:" + value;
	}
}
