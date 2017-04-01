/* 
 * Copyright (C) Mtrust Systems, Inc - All Rights Reserved
 * Written by feelon2 <feelon2@gmail.com>, 2015-05-06
 * 
 * */

package com.bang.bangapplication;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.Toast;

import com.bang.bangapplication.service.BleService;


public abstract class FIndDeviceActivity extends ActionBarActivity {
	private static final String BEACON_ADDRESS = "C1:3A:A3:36:03:86";
	public static final String TAG = "FIndDeviceActivity";

	private final int ENABLE_BT = 1;

	private Messenger mMessenger;
	private Intent mServiceIntent;
	private Messenger mService = null;
	private BleService.State mState = BleService.State.UNKNOWN;
	public static FIndDeviceActivity fIndDeviceActivity = null;
	
	private ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mService = new Messenger(service);
			try {
				Message msg = Message.obtain(null, BleService.MSG_REGISTER);
				if (msg != null) {
					msg.replyTo = mMessenger;
					mService.send(msg);
					startScan();
				} else {
					mService = null;
				}
			} catch (Exception e) {
				Log.w(TAG, "Error connecting to BleService", e);
				mService = null;
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mService = null;
		}
	};
	
	protected void startService(){
		mMessenger = new Messenger(new IncomingHandler(this));
		try {
			bindService(mServiceIntent, mConnection, BIND_AUTO_CREATE);
		}catch(Exception e){}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.find_device);
		mServiceIntent = new Intent(this, BleService.class);
		FIndDeviceActivity.fIndDeviceActivity = this;
	}
	
	
	@Override
	protected void onResume() {
		//startScan();
		super.onResume();
	}

	private void stateChanged(BleService.State newState) {
		boolean disconnected = mState == BleService.State.CONNECTED;
		mState = newState;
		switch (mState) {
			case SCANNING:
				break;
			case BLUETOOTH_OFF:
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, ENABLE_BT);
				onDisconnected();
				break;
			case IDLE:
				onDisconnected();
				break;
			case CONNECTED:
				onConnected();
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == ENABLE_BT) {
			if (resultCode == RESULT_OK) {
				startScan();
			} else {
				finish();
			}
		} else {
			super.onActivityResult(requestCode, resultCode, data);
		}
	}
	
	private void startScan() {
		Message msg = Message.obtain(null, BleService.MSG_START_SCAN);
		if (msg != null) {
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				Log.w(TAG, "Lost connection to service", e);
				unbindService(mConnection);
			}
		}
	}
	
	public void setDevices(Context context, String[] devices) {
		List<Map<String, String>> items = new ArrayList<Map<String, String>>();
		if (devices != null) {
			for (String device : devices) {
				if(device.toString().equals(BEACON_ADDRESS)){
					onDeviceListFragmentInteraction(BEACON_ADDRESS);
				}
			}
		}
	}

	public void onDeviceListFragmentInteraction(String macAddress) {
		Message msg = Message.obtain(null, BleService.MSG_DEVICE_CONNECT);
		if (msg != null) {
			msg.obj = macAddress;
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				Log.w(TAG, "Lost connection to service", e);
				unbindService(mConnection);
			}
		}
	}

	private static class IncomingHandler extends Handler {
		private final WeakReference<FIndDeviceActivity> mActivity;

		public IncomingHandler(FIndDeviceActivity activity) {
			mActivity = new WeakReference<FIndDeviceActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			FIndDeviceActivity activity = mActivity.get();
			if (activity != null) {
				switch (msg.what) {
					case BleService.MSG_STATE_CHANGED:
						activity.stateChanged(BleService.State.values()[msg.arg1]);
						break;
					case BleService.MSG_DEVICE_FOUND:
						Bundle data = msg.getData();
						if (data != null && data.containsKey(BleService.KEY_MAC_ADDRESSES)) {
							activity.setDevices(activity, data.getStringArray(BleService.KEY_MAC_ADDRESSES));
						}
						break;
				}
			}
			super.handleMessage(msg);
		}
	}

	public abstract void onConnected();
	public abstract void onDisconnected();


}
