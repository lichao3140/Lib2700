package com.example.dpservice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.dpower.callback.PCToolCallback;
import com.dpower.function.DPFunction;
import com.dpower.pcproject.PcJniBack;
import com.dpower.pcproject.PcJniClass;
import com.dpower.util.MyLog;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class PCToolService extends Service {
	private static final String TAG = "PCToolService";
	
	private static PCToolService mInstance = null;
	private List<PCToolCallback> mCallbacks;
	
	public static PCToolService getInstance() {
		return mInstance;
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		MyLog.print(TAG, "onCreate");
		PcJniClass.getIns(); //初始化类
        PcJniClass.getIns().initPcLoadLibs(); //初始化库
        PcJniClass.getIns().setShowLog(true);
        registerJniCallback();
		PcJniClass.getIns().SystemInit(); //初始化线程
		mCallbacks = new ArrayList<PCToolCallback>();
		mInstance = this;
	}

	private void registerJniCallback() {
		PcJniClass.getIns().initPcJniBack(new PcJniBack() {

			@Override
			public void rebootBack() {
				MyLog.print(TAG, "rebootBack");
				try {
					String cmd = "su -c reboot";
					Runtime.getRuntime().exec(cmd);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			@Override
			public void resetBack() {
				MyLog.print(TAG, "resetBack");
				for (PCToolCallback callback : mCallbacks) {
					callback.reset();
				}
			}

			@Override
			public void roomBack(String room) {
				MyLog.print(TAG, "roomBack room = " + room);
				DPFunction.changeRoomCode(room);
			}

			@Override
			public void updateAppSuccessBack(String filePath) {
				MyLog.print(TAG, "updateAppSuccessBack filePath = " + filePath);
				for (PCToolCallback callback : mCallbacks) {
					callback.updateApp(filePath);
				}
			}

			@Override
			public void updateAppFailBack(String filePath) {
				MyLog.print(TAG, "updateAppFailBack filePath = " + filePath);
			}

			@Override
			public void updateSystemSuccessBack(String filePath) {
				MyLog.print(TAG, "updateSystemSuccessBack filePath = " + filePath);
				for (PCToolCallback callback : mCallbacks) {
					callback.updateSystem(filePath);
				}
			}

			@Override
			public void updateSystemFailBack(String filePath) {
				MyLog.print(TAG, "updateSystemFailBack filePath = " + filePath);
			}

			@Override
			public void updateNetcfgSuccessBack(String filePath) {
				MyLog.print(TAG, "updateNetcfgSuccessBack filePath = " + filePath);
				for (PCToolCallback callback : mCallbacks) {
					callback.updateNetCfg(filePath);
				}
			}

			@Override
			public void updateNetcfgFailBack(String filePath) {
				MyLog.print(TAG, "updateNetcfgFailBack filePath = " + filePath);
			}

			@Override
			public void updateFileSuccessBack(String filePath) {
				MyLog.print(TAG, "updateFileSuccessBack filePath = " + filePath);
			}

			@Override
			public void updateFileFailBack(String filePath) {
				MyLog.print(TAG, "updateFileFailBack filePath = " + filePath);
			}
        });
	}
	
	public void registerCallback(PCToolCallback callback) {
		if (!mCallbacks.contains(callback)) {
			mCallbacks.add(callback);
			MyLog.print(TAG, "registerCallback");
		}
	}
	
	public void unregisterCallback(PCToolCallback callback) {
		if (mCallbacks.contains(callback)) {
			mCallbacks.remove(callback);
			MyLog.print(MyLog.ERROR, TAG, "unregisterCallback");
		}
	}
	
	@Override
	public void onDestroy() {
		PcJniClass.getIns().SystemUninit(); //释放线程
		super.onDestroy();
	}
}
