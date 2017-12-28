package com.dpower.util;

import java.io.File;

import android.os.Environment;

/**
 * 主要存放一些常量
 * @author Arlene
 */
public class ConstConf {
	
	/** 系统配置文件路径 system/etc/ */
	public static final String SYSTEM_CONF = "system" + File.separator + "etc" + File.separator;
	
	/** 配置文件存放目录 system/media/backup/ */
	public static final String ROM_DIR = "system/media/backup" + File.separator;
	
	/** /mnt/sdcard/ */
	public static final String SD_DIR = Environment.getExternalStorageDirectory().getPath() 
			+ File.separator;
	
	/** 临时文件夹路径 /mnt/sdcard/temp */
	public static final String TEMP_PATH = SD_DIR + "temp";
	
	/** 门铃铃声路径 system/media/backup/Music/ringin.mp3 */
	public static final String DOOR_RING_PATH = ROM_DIR + "Music/ringin.mp3";
	
	/** 电话铃声路径 /mnt/sdcard/Ringtones/ */
	public static final String RING_PATH = SD_DIR + "Ringtones" + File.separator;
	
	/** 电话铃声文件 ring_you.mp3 */
	public static final String RING_MP3 = "ring_you.mp3";
	
	/** 网络配置表路径 /mnt/sdcard/netcfg.dat */
	public static final String NET_CFG_PATH = SD_DIR + "netcfg.dat";
	
	/** 下载网络配置表路径 /mnt/sdcard/temp/netcfg.dat */
	public static final String NEW_NET_CFG_PATH = SD_DIR + "temp/netcfg.dat";
	
	/** 网络配置表 netcfg.dat */
	public static final String NET_CFG_DAT = "netcfg.dat";
	
	/** 访客相册路径 /mnt/sdcard/visit */
	public static final String VISIT_PATH = SD_DIR + "visit";
	
	/** 访客相册的小图路径 /mnt/sdcard/visit/little */
	public static final String VISIT_LITTLE_PATH = VISIT_PATH + File.separator + "little";
	
	/** 小区信息路径 /mnt/sdcard/message */
	public static final String MESSAGE_PATH = SD_DIR + "message";
	
	/** 小区信息的小图路径 /mnt/sdcard/message/little */
	public static final String MESSAGE_LITTLE_PATH = MESSAGE_PATH + File.separator + "little";
	
	/** 默认留言路径 system/media/backup/default_leave.wav */
	public static final String DEFAULT_LEAVE_PATH = ROM_DIR + "default_leave.wav";
	
	/** 业主留言路径(用户自己录音并保存) system/media/backup/userleave.wav */
	public static final String USER_LEAVE_PATH = ROM_DIR + "userleave.wav";
	
	/** 管理中心升级apk路径 /mnt/sdcard/update.rar */
	public static final String UPDATE_PATH = SD_DIR + "update.rar";
	
	/** 管理中心升级系统路径 /mnt/sdcard/update.zip */
	public static final String UPDATE_SYSTEM_PATH = SD_DIR + "update.zip";
	
	/** 外置SD卡系统升级包  /mnt/extsd/update.zip */
	public static final String UPDATE_SYSTEM_EXT_SD = "/mnt/extsd/update.zip";
	
	/** 消息铃声路径 system/media/backup/Music/MSGRING.WAV */
	public static final String MSG_RING_PATH = ROM_DIR + "Music/MSGRING.WAV";
	
	/** 报警延迟铃声路径 system/media/backup/Music/DelayAlarm.wav */
	public static final String DELAYALARM_RING_PATH = ROM_DIR + "Music/DelayAlarm.wav";
	
	/** 报警铃声路径 system/media/backup/Music/Alarm.wav */
	public static final String ALARM_RING_PATH = ROM_DIR + "Music/Alarm.wav";
	
	/** 报警录像路径  /mnt/sdcard/AlarmVideo/ */
	public static final String ALARM_VIDEO_PATH = SD_DIR + "AlarmVideo" + File.separator;
	
	/** 声音配置文件 AECpara.inf */
	public static final String VOLUME_INF = "AECpara.inf";
	
	/** 防区配置文件路径 /mnt/sdcard/Alarms/ */
	public static final String SAFE_ALARM_PATH = SD_DIR + "Alarms" + File.separator;
	
	/** 防区配置文件 alarm.txt */
    public static final String SAFE_ALARM_TXT = "alarm.txt";
    
    public static final String SAFE_MODE = "safeMode";
    /** 测试模式 */
	public final static int TEST_MODE = 0;
	/** 撤防模式 */
	public final static int UNSAFE_MODE = 1;
	/** 夜间模式 */
	public final static int NIGHT_MODE = 2;
	/** 在家模式 */
	public final static int HOME_MODE = 3;
	/** 离家模式 */
	public final static int LEAVE_HOME_MODE = 4;
	/** 安防模式个数 */
	public final static int MODE_NUM = 5;
	/** 默认8个探头 */
	public static final int DEFAULT_SAFE_TANTOU = 8;
	
	/** 门口机呼入超过这个时间开始转呼手机(振铃超时与这个时间的时间差应该在20秒内，否则留言功能会失效) */
	public static final int START_CALL_PHONE = 30 * 1000;
	/** 振铃超时 */
	public static final int RING_TIMEOUT = 50 * 1000;
	/** 通话超时 */
	public static final int TALK_TIMEOUT = 300 * 1000;
	/** 监视超时 */
	public static final int MONITOR_TIMEOUT = 120 * 1000;
	
	public static final String LAN_NETWORK_CARD = "eth0";
	public static final String LAN_NETWORK_CARD_ETH1 = "eth1";
	public static final String WAN_NETWORK_CARD = "wlan0";
	public static final String VOLUME_CHANGED_ACTION = "android.media.VOLUME_CHANGED_ACTION";
}
