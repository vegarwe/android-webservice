package no.raiom.webservice;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.AdvertisementData;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class AdvertiserService extends Service {
    private BluetoothAdapter      btAdapter;
    private BluetoothLeAdvertiser advertiser;
    private GattServer            gattserver;

    // #################### Service section ####################

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("Fjase", "AdvertiserService.onCreate");

        BluetoothManager btManager = (BluetoothManager)getSystemService(BLUETOOTH_SERVICE);
        btAdapter                  = btManager.getAdapter();
        gattserver                 = new GattServer(this, btManager);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i("Fjase", "AdvertiserService.onDestroy");

        gattserver.stopGattServer();
        stopBroadcaster();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("Fjase", "AdvertiserService.onStartCommand");

        gattserver.setupGattServer();
        setupAndStartBroadcaster();

        return super.onStartCommand(intent, flags, startId);
    }

    // #################### BroadcastReceiver section ##########

    private final BroadcastReceiver bluetoothStatusChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.e("Fjase", "BluetoothStatusChange.onReceive " + action);
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                if (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_OFF) {
                    handleOnAdapterOff();
                } else if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1) == BluetoothAdapter.STATE_ON){
                    handleOnAdapterOn();
                }
            }
        }

    };

    private void handleOnAdapterOff() {
        Log.i("Fjase", "AdvertiserService.handleOnAdpaterOff");
    }

    private void handleOnAdapterOn() {
        Log.i("Fjase", "AdvertiserService.handleOnAdpaterOn");
    }

    // #################### Service implementation section #####
    private void setupAndStartBroadcaster() {
        Log.i("Fjase", "setupAndStartBroadcaster");
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setType(AdvertiseSettings.ADVERTISE_TYPE_CONNECTABLE);

        AdvertisementData.Builder dataBuilder = new AdvertisementData.Builder();
        dataBuilder.setServiceUuids(new ArrayList<ParcelUuid>() {
            {
                add(new ParcelUuid(GattServer.WEBSERVICE_SERV));
            }
        });

        registerReceiver(bluetoothStatusChangeReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        advertiser = btAdapter.getBluetoothLeAdvertiser();
        advertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), advertiseCallback);
    }

    private void stopBroadcaster() {
        Log.i("Fjase", "stopBroadcaster");
        advertiser.stopAdvertising(advertiseCallback);

        unregisterReceiver(bluetoothStatusChangeReceiver);
    }

    private final AdvertiseCallback advertiseCallback = new AdvertiseCallback() {
        @Override
        public void onSuccess(AdvertiseSettings advertiseSettings) {
            Log.i("Fjase", "AdvertiserService.advertiseCallback.onSuccess");
        }

        @Override
        public void onFailure(int i) {
            Log.i("Fjase", "AdvertiserService.advertiseCallback.onFailure: " + i);
        }
    };
}
