package com.bang.bangapplication.service;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.R.integer;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
public class BleService extends Service implements BluetoothAdapter.LeScanCallback {
    public static final String TAG = "BleService";
    public static final int MSG_REGISTER = 1;
    public static final int MSG_UNREGISTER = 2;
    public static final int MSG_START_SCAN = 3;
    public static final int MSG_STATE_CHANGED = 4;
    public static final int MSG_DEVICE_FOUND = 5;
    public static final int MSG_DEVICE_CONNECT = 6;
    public static final int MSG_DEVICE_DISCONNECT = 7;
    public static final int MSG_READ_RSSI = 12;

    private static final long SCAN_PERIOD = 5000;

    public static final String KEY_MAC_ADDRESSES = "KEY_MAC_ADDRESSES";

    private static final String DEVICE_NAME = "RFDuino";
    private static final UUID UUID_BLE_GENERIC_ACCESS_PROFILE_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_BLE_GENERIC_ATTRIBUTE_PROFILE_UUID = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_BLUEINNO_PROFILE_SERVICE_UUID = UUID.fromString("00002220-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_BLUEINNO_PROFILE_SEND_UUID = UUID.fromString("00002222-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_BLUEINNO_PROFILE_RECEIVE_UUIDD = UUID.fromString("00002221-0000-1000-8000-00805f9b34fb");
    private static final UUID UUID_BLUEINNO_DISCONNECT_UUID = UUID.fromString("00002223-0000-1000-8000-00805f9b34fb");

    private boolean isReading;

    private final IncomingHandler mHandler;
    private final Messenger mMessenger;
    private final List<Messenger> mClients = new LinkedList<Messenger>();
    private final Map<String, BluetoothDevice> mDevices = new HashMap<String, BluetoothDevice>();
    private BluetoothGatt mGatt = null;

    public enum State {
        UNKNOWN,
        IDLE,
        SCANNING,
        BLUETOOTH_OFF,
        CONNECTING,
        CONNECTED,
        DISCONNECTING
    }

    private BluetoothAdapter mBluetooth = null;
    private State mState = State.UNKNOWN;

    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.v(TAG, "Connection State Changed: " + (newState == BluetoothProfile.STATE_CONNECTED ? "Connected" : "Disconnected"));
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                setState(State.CONNECTED);
                gatt.discoverServices();
            } else {
                setState(State.IDLE);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            Log.v(TAG, "onServicesDiscovered: " + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                subscribe(gatt);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            byte bytes[] = characteristic.getValue();
            if(bytes.length>0) {
                Log.e(TAG, "onCharacteristicRead: " + bytes.length);
            }
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            super.onReadRemoteRssi(gatt, rssi, status);
            Message msg = getStateMessage();
            msg.what = MSG_READ_RSSI;
            msg.arg1 = rssi;
            sendMessage(msg);
        }

    };

    private void startReadCharacteristicListener() {
        isReading = true;
        new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {
                Log.d(TAG, "Start Characteristic Reader");
                while (isReading) {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (mBluetooth == null || mGatt == null) continue;
                    BluetoothGattService blueService = mGatt.getService(UUID_BLUEINNO_PROFILE_SERVICE_UUID);
                    if (blueService == null) continue;
                    BluetoothGattCharacteristic characteristic = blueService.getCharacteristic(UUID_BLUEINNO_PROFILE_RECEIVE_UUIDD);
                    mGatt.readCharacteristic(characteristic);
                    mGatt.readRemoteRssi();
                }
                return null;
            }
        }.execute();
    }

    private void stopReadCharacteristicListener() {
        isReading = false;
    }

    public BleService() {
        mHandler = new IncomingHandler(this);
        mMessenger = new Messenger(mHandler);
    }


    @Override
    public IBinder onBind(Intent intent) {
        startReadCharacteristicListener();
        return mMessenger.getBinder();
    }

    private static class IncomingHandler extends Handler {
        private final WeakReference<BleService> mService;

        public IncomingHandler(BleService service) {
            mService = new WeakReference<BleService>(service);
        }

        @Override
        public void handleMessage(Message msg) {
            BleService service = mService.get();
            if (service != null) {
                switch (msg.what) {
                    case MSG_REGISTER:
                        service.mClients.add(msg.replyTo);
                        Log.d(TAG, "Registered");
                        break;
                    case MSG_UNREGISTER:
                        service.mClients.remove(msg.replyTo);
                        if (service.mState == State.CONNECTED && service.mGatt != null) {
                            service.mGatt.disconnect();
                        }
                        Log.d(TAG, "Unegistered");
                        break;
                    case MSG_START_SCAN:
                        service.startScan();
                        Log.d(TAG, "Start Scan");
                        break;
                    case MSG_DEVICE_CONNECT:
                        service.connect((String) msg.obj);
                        break;
                    case MSG_DEVICE_DISCONNECT:
                        if (service.mState == State.CONNECTED && service.mGatt != null) {
                            service.mGatt.disconnect();
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                }
            }
        }
    }

    private void startScan() {
        mDevices.clear();
        setState(State.SCANNING);
        if (mBluetooth == null) {
            BluetoothManager bluetoothMgr = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
            mBluetooth = bluetoothMgr.getAdapter();
        }
        if (mBluetooth == null || !mBluetooth.isEnabled()) {
            setState(State.BLUETOOTH_OFF);
        } else {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mState == State.SCANNING) {
                        mBluetooth.stopLeScan(BleService.this);
                        setState(State.IDLE);
                    }
                }
            }, SCAN_PERIOD);
            mBluetooth.startLeScan(this);
        }
    }

    @Override
    public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device != null && !mDevices.containsValue(device) && device.getName() != null) { // && device.getName().equals(DEVICE_NAME)) {
            mDevices.put(device.getAddress(), device);
            Message msg = Message.obtain(null, MSG_DEVICE_FOUND);
            if (msg != null) {
                Bundle bundle = new Bundle();
                String[] addresses = mDevices.keySet().toArray(new String[mDevices.size()]);
                bundle.putStringArray(KEY_MAC_ADDRESSES, addresses);
                msg.setData(bundle);
                sendMessage(msg);
            }
        }
    }

    public void connect(String macAddress) {
        BluetoothDevice device = mDevices.get(macAddress);
        if (device != null) {
            mGatt = device.connectGatt(this, true, mGattCallback);
        }
    }

    private void subscribe(BluetoothGatt gatt) {
        BluetoothGattService blueService = gatt.getService(UUID_BLUEINNO_PROFILE_SERVICE_UUID);
        if (blueService != null) {
            Log.i(TAG, "Find GATT Service!!!");
            BluetoothGattCharacteristic blueCharacteristic = blueService.getCharacteristic(UUID_BLUEINNO_PROFILE_SEND_UUID);
            if (blueCharacteristic != null) {
                Log.i(TAG, "Find GATT Characteristic!!!");
                byte[] color = new byte[]{(byte) Color.red(100), (byte) Color.green(100), (byte) Color.blue(100)};

                blueCharacteristic.setValue(color);
                mGatt.writeCharacteristic((BluetoothGattCharacteristic) blueCharacteristic);
            }
        }
    }

    private void setState(State newState) {
        if (mState != newState) {
            mState = newState;
            Message msg = getStateMessage();
            if (msg != null) {
                sendMessage(msg);
            }
        }
    }

    private Message getStateMessage() {
        Message msg = Message.obtain(null, MSG_STATE_CHANGED);
        if (msg != null) {
            msg.arg1 = mState.ordinal();
        }
        return msg;
    }

    private void sendMessage(Message msg) {
        for (int i = mClients.size() - 1; i >= 0; i--) {
            Messenger messenger = mClients.get(i);
            if (!sendMessage(messenger, msg)) {
                mClients.remove(messenger);
            }
        }
    }

    private boolean sendMessage(Messenger messenger, Message msg) {
        boolean success = true;
        try {
            messenger.send(msg);
        } catch (RemoteException e) {
            Log.w(TAG, "Lost connection to client", e);
            success = false;
        }
        return success;
    }

}
