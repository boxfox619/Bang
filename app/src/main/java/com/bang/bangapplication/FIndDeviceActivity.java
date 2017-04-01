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

import android.app.Activity;
import android.app.ProgressDialog;
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
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.bang.bangapplication.service.BleService;


public class FIndDeviceActivity extends Activity {
	private static final String[] KEYS = {"KEY_MAC_ADDRESS"};
	private static final int[] IDS = {android.R.id.text1};
	public static final String TAG = "FIndDeviceActivity";
	private ListView mMenuList = null;

	private ArrayAdapter<String> mAdapter = null;

	private final int ENABLE_BT = 1;

	private final Messenger mMessenger;
	private Intent mServiceIntent;
	private Messenger mService = null;
	private BleService.State mState = BleService.State.UNKNOWN;
	private ProgressDialog mProgressDialog;
	Thread progressThread; 
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
	
	private static class IncomingHandler extends Handler {
		private final WeakReference<FIndDeviceActivity> mActivity;

		public IncomingHandler(FIndDeviceActivity activity) {
			mActivity = new WeakReference<FIndDeviceActivity>(activity);
		}

		@Override
		public void handleMessage(Message msg) {
			FIndDeviceActivity activity = mActivity.get();
			activity.colseProgress();
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
	
	
	public FIndDeviceActivity() {
		super();
		mMessenger = new Messenger(new IncomingHandler(this));
	}


	@Override
	protected void onStart() {
		super.onStart();
		bindService(mServiceIntent, mConnection, BIND_AUTO_CREATE);
	}

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.find_device);
		mServiceIntent = new Intent(this, BleService.class);
		mMenuList = (ListView)findViewById(R.id.device_list);
		FIndDeviceActivity.fIndDeviceActivity = this;
		//startScan();
	}
	
	
	@Override
	protected void onResume() {
		//startScan();
		super.onResume();
	}


	private void openProgress(){
		mProgressDialog = new ProgressDialog(FIndDeviceActivity.this);
		mProgressDialog.setMessage("Loading...");
		mProgressDialog.show();
	}
	

	public void colseProgress(){
		if(mProgressDialog!=null&&mProgressDialog.isShowing()){
		}
	}
	
	private void stateChanged(BleService.State newState) {
		boolean disconnected = mState == BleService.State.CONNECTED;
		mState = newState;
		switch (mState) {
			case SCANNING:
				if( mProgressDialog != null ){
					mProgressDialog.setMessage("SCANNING");
				}
				break;
			case BLUETOOTH_OFF:
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, ENABLE_BT);
				if( mProgressDialog != null ){
					mProgressDialog.setMessage("BLUETOOTH_OFF");
				}
				break;
			case IDLE:
				if (disconnected) {
					if( mProgressDialog != null ){
						mProgressDialog.setMessage("IDLE");
					}
				}
				break;
			case CONNECTED:
				if( mProgressDialog != null ){
					mProgressDialog.setMessage("CONNECTED");
				}
				Toast.makeText(FIndDeviceActivity.this, "연결되었습니다.", Toast.LENGTH_SHORT).show();
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
				openProgress();
				mService.send(msg);
			} catch (RemoteException e) {
				Log.w(TAG, "Lost connection to service", e);
				unbindService(mConnection);
			}
		}
	}
	
	public void setDevices(Context context, String[] devices) {
		mProgressDialog.setMessage("setDevices");
		mProgressDialog.dismiss();
		mAdapter = new ArrayAdapter<String>(context, android.R.layout.simple_list_item_1);
		
		
		List<Map<String, String>> items = new ArrayList<Map<String, String>>();
		if (devices != null) {
			for (String device : devices) {
				mAdapter.add(device.toString());
			}
		}
		mMenuList.setAdapter(mAdapter);
		mMenuList.setOnItemClickListener( new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				onDeviceListFragmentInteraction(mAdapter.getItem(position).toString());
			}
		});
		
		mMenuList.setOnItemSelectedListener( new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				onDeviceListFragmentInteraction(mAdapter.getItem(arg2).toString());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});
	}

	public void onDeviceListFragmentInteraction(String macAddress) {
		Message msg = Message.obtain(null, BleService.MSG_DEVICE_CONNECT);
		Toast.makeText(FIndDeviceActivity.this, macAddress + "로 연결", Toast.LENGTH_SHORT);
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

}
