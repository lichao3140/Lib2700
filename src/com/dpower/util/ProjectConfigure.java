package com.dpower.util;

/**
 * 工程配置
 */
public class ProjectConfigure {

	/** 
	 * 为了方便管理中心升级APK，需要在AndroidManifest.xml文件中对versionName进行相应的修改
	 * 公版：android:versionName="DP2700"
	 * 龙侨华7寸：android:versionName="90AC1" 10寸：android:versionName="90AC10"
	 * 克耐克7寸：android:versionName="CMT-1602" 10寸：android:versionName="CMT-1601"
	 * 易天元：android:versionName="ETY_X6"
	 * 韩昌：android:versionName="smacom"
	 */
	
	/** 公版、易天元 = 0, 龙侨华 = 1, 克耐克 = 2, 韩昌 = 3 */
	public static int project = 1;
	
	/** 不区分尺寸 = 0, 7寸 = 7, 10寸 = 10 */
	public static int size = 7;
	
	/** 克耐克区分简单版和完全版 */
	public static boolean isSimple = false;
	
	/** 调试 = true, 非调试 = false*/
	public static boolean isDebug = false;
	
	/** 需要云对讲 = true, 不需要 = false*/
	public static boolean needCloudIntercom = false;
	
	/** 需要智能家居 = true, 不需要 = false*/
	public static boolean needSmartHome = false;
	
	/** 有网络摄像头 = true, 没有 = false*/
	public static boolean webCamera = true;
	
	/** true = 启动第三方 */
	public static boolean smartHomeApk = false;

	/** true = 使用第三方服务器 */
	public static boolean smartHomeServer = false;
	
	/** 易天元：首页是否可以点击返回 , 香港项目可以增加返回键 */
	public static boolean isHomePageBack = false;
	
	/** 易天元：增值娱乐 */
	public static class Entertainment {
		/** 是否使用外部APK */
		public final static boolean isOutsideApk = false;
		public final static String packageName = "com.ken.webExplorery";
		public final static String classLaunchName = "com.ken.webExplorery.WebExplorerActivity";
	}
}
