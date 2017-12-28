package com.dpower.util;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.graphics.Bitmap;
import android.os.SystemClock;

public class CommonUT {
	private static final String TAG = "CommonUT";
	
	public static boolean isIp(String strIP) {
		if (strIP == null || strIP.length() == 0)
			return false;
		if (strIP.length() < 7)
			return false;
		boolean b = false;
		String IP = strIP.trim();
		if (IP.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")) {
			String s[] = IP.split("\\.");
			if (Integer.parseInt(s[0]) <= 255)
				if (Integer.parseInt(s[1]) <= 255)
					if (Integer.parseInt(s[2]) <= 255)
						if (Integer.parseInt(s[3]) <= 255)
							b = true;
		}
		return b;
	}
	
	/**
	 * 将数字的ip转成字符串的ip
	 * @param ip
	 * @return
	 */
	public static String intToString(int ip) {
		return Integer.toString(ip & 0xFF) + "."
			+ Integer.toString((ip >> 8) & 0xFF) + "."
			+ Integer.toString((ip >> 16) & 0xFF) + "."
			+ Integer.toString((ip >> 24) & 0xFF);
	}
	
	/**
	 * 下载文件
	 * @param httpUrl 下载地址
	 * @param saveFile 保存路径
	 * @return
	 */
	public static boolean httpDownload(String httpUrl, String saveFile) {
		// 下载网络文件
		boolean result = false;
		URL url = null;
		try {
			url = new URL(httpUrl);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			return result;
		}
		InputStream inputStream = null;
		FileOutputStream outputStream = null;
		try {
			URLConnection conn = url.openConnection();
			inputStream = conn.getInputStream();
			outputStream = new FileOutputStream(saveFile);
			byte[] buffer = new byte[1024];
			int len;
			while ((len = inputStream.read(buffer)) > 0) {
				outputStream.write(buffer, 0, len);
			}
			outputStream.flush();
			outputStream.getFD().sync();
			MyLog.print(TAG, "下载网络文件成功");
			result = true;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
	
	/** 对图片进行压缩 */
	public static byte[] compressBitmap(Bitmap bitmap) {
		if (bitmap == null) {
			return null;
		}
		byte[] newData = null;
	    ByteArrayOutputStream stream = null;
	    try {
	    	stream = new ByteArrayOutputStream();
	    	bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		    MyLog.print("对图片进行压缩before:" + stream.toByteArray().length); 
		    int options = 90;
		    while (stream.toByteArray().length / 1024 > 25) {
		        stream.reset();
		        bitmap.compress(Bitmap.CompressFormat.JPEG, options, stream);
		        options -= 5;
		    }
		    stream.flush();
		    newData = stream.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	    MyLog.print("对图片进行压缩after:" + newData.length);
	    return newData;
	}
	
	public static boolean getLanConnectState(String lanName) {
		Process process = null;
		InputStream inputStream = null;
		ByteArrayOutputStream outputStream = null;
		String cmd = "ifconfig " + lanName;
		String result = "";
		try {
			outputStream = new ByteArrayOutputStream();
			process = Runtime.getRuntime().exec(cmd);
			outputStream.write("/n".getBytes());
			inputStream = process.getInputStream();
			int len;
			while ((len = inputStream.read()) > 0) {
				outputStream.write(len);
			}
			outputStream.flush();
			byte[] data = outputStream.toByteArray();
			result = new String(data);
		} catch (IOException e) {
			e.printStackTrace();
			MyLog.print(MyLog.ERROR, "cmd", "error");
		} finally {
			try {
				if (inputStream != null) {
					inputStream.close();
				}
				if (outputStream != null) {
					outputStream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (result.contains("up broadcast running multicast")) {
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 将long类型时间 转换成String类型
	 * @param time - "yyyy-MM-dd HH:mm:ss" "2016-08-31 08:31:01"
	 * @return
	 */
	public static String formatTime(long time) {
		SimpleDateFormat format = new SimpleDateFormat(
				"yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		Date date = new Date(time);
		return format.format(date);
	}
	
	/**
	 * 设置系统时间
	 * @param time - "yyyy-MM-dd HH:mm:ss" "2016-08-31 08:31:01"
	 * @return
	 */
	public static boolean setSystemTime(String time) {
		try {
			requestPermission();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Calendar calendar = Calendar.getInstance();
		try {
			calendar.setTime(dateFormat.parse(time));
		} catch (ParseException e) {
			e.printStackTrace();
			return false;
		}
		long when = calendar.getTimeInMillis();
		if (when / 1000 < Integer.MAX_VALUE) {
			SystemClock.setCurrentTimeMillis(when);
			MyLog.print(TAG, "setTime success");
			MyLog.print(TAG, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
					Locale.getDefault()).format(calendar.getTime()));
			return true;
		} else {
			MyLog.print(MyLog.ERROR, "setTime error");
			return false;
		}
	}
	
	/**
	 * 设置系统时间
	 * @param calendar
	 * @return
	 */
	public static boolean setSystemTime(Calendar calendar) {
		try {
			requestPermission();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		long when = calendar.getTimeInMillis();
		if (when / 1000 < Integer.MAX_VALUE) {
			SystemClock.setCurrentTimeMillis(when);
			MyLog.print(TAG, "setTime success");
			MyLog.print(TAG, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
					Locale.getDefault()).format(calendar.getTime()));
			return true;
		} else {
			MyLog.print(MyLog.ERROR, "setTime error");
			return false;
		}
	}
	
	/**
	 * 设置系统时间
	 * @param hour
	 * @param minute
	 * @return
	 */
	public static boolean setSystemTime(int hour, int minute) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		try {
			requestPermission();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		long when = calendar.getTimeInMillis();
		if (when / 1000 < Integer.MAX_VALUE) {
			SystemClock.setCurrentTimeMillis(when);
			MyLog.print(TAG, "setTime success");
			MyLog.print(TAG, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
					Locale.getDefault()).format(calendar.getTime()));
			return true;
		} else {
			MyLog.print(MyLog.ERROR, "setTime error");
			return false;
		}
	}
	
	/**
	 * 设置系统时间
	 * @param year
	 * @param month
	 * @param date
	 * @return
	 */
	public static boolean setSystemTime(int year, int month, int date) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month - 1);
		calendar.set(Calendar.DATE, date);
		try {
			requestPermission();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		long when = calendar.getTimeInMillis();
		if (when / 1000 < Integer.MAX_VALUE) {
			SystemClock.setCurrentTimeMillis(when);
			MyLog.print(TAG, "setTime success");
			MyLog.print(TAG, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
					Locale.getDefault()).format(calendar.getTime()));
			return true;
		} else {
			MyLog.print(MyLog.ERROR, "setTime error");
			return false;
		}
	}
	
	/**
	 * 设置系统时间
	 * @param year
	 * @param month
	 * @param date
	 * @param hour
	 * @param minute
	 * @param second
	 * @return
	 */
	public static boolean setSystemTime(int year, int month, int date, 
			int hour, int minute, int second) {
		Calendar calendar = Calendar.getInstance();
		calendar.set(Calendar.YEAR, year);
		calendar.set(Calendar.MONTH, month - 1);
		calendar.set(Calendar.DATE, date);
		calendar.set(Calendar.HOUR_OF_DAY, hour);
		calendar.set(Calendar.MINUTE, minute);
		calendar.set(Calendar.SECOND, second);
		try {
			requestPermission();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		long when = calendar.getTimeInMillis();
		if (when / 1000 < Integer.MAX_VALUE) {
			SystemClock.setCurrentTimeMillis(when);
			MyLog.print(TAG, "setTime success");
			MyLog.print(TAG, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
					Locale.getDefault()).format(calendar.getTime()));
			return true;
		} else {
			MyLog.print(MyLog.ERROR, "setTime error");
			return false;
		}
	}
	
	/**
	 * 获取设置系统时间权限
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	private static void requestPermission() throws InterruptedException, IOException {
		createSuProcess("chmod 666 /dev/alarm").waitFor();
	}
	
	private static Process createSuProcess(String cmd) throws IOException {
		DataOutputStream os = null;
		Process process = createSuProcess();
		try {
			os = new DataOutputStream(process.getOutputStream());
			os.writeBytes(cmd + "\n");
			os.writeBytes("exit $?\n");
			os.flush();
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return process;
	}

	private static Process createSuProcess() throws IOException {
		File rootUser = new File("/system/xbin/ru");
		if (rootUser.exists()) {
			return Runtime.getRuntime().exec(rootUser.getAbsolutePath());
		} else {
			return Runtime.getRuntime().exec("su");
		}
	}
}
