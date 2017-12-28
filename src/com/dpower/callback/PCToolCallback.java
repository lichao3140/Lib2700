package com.dpower.callback;

/**
 * PC工具回调
 */
public interface PCToolCallback {
	
	/** 恢复出厂设置 */
	public void reset();
	/** 升级程序 */
	public void updateApp(String filePath);
	/** 升级系统 */
	public void updateSystem(String filePath);
	/** 升级网络配置表 */
	public void updateNetCfg(String filePath);
}
