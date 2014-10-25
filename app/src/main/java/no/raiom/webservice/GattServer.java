package no.raiom.webservice;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import java.util.UUID;

import no.raiom.utils.ByteUtils;

public class GattServer {
    public final static UUID WEBSERVICE_SERV   = UUID.fromString("785f7d50-6736-4ebc-ad98-1bdc9306a622");
    public final static UUID WEBSERVICE_BODY   = UUID.fromString("785f7d51-6736-4ebc-ad98-1bdc9306a622");
    public final static UUID WEBSERVICE_STATUS = UUID.fromString("785f7d52-6736-4ebc-ad98-1bdc9306a622");
    public final static UUID CCCD              = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    private final BluetoothManager    btManager;
    private final BluetoothGattServer gattServer;

    public GattServer(Context context, BluetoothManager btManager) {
        this.btManager = btManager;
        this.gattServer = btManager.openGattServer(context, gattCallback);
    }

    public void stopGattServer() {
        gattServer.close();
        gattServer.clearServices();
    }

    public void setupGattServer() {
        Log.i("Fjase", "setupGattServer");
        BluetoothGattCharacteristic characteristic;
        BluetoothGattDescriptor descriptor;
        BluetoothGattService service = new BluetoothGattService(WEBSERVICE_SERV,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // WEBSERVICE BODY
        characteristic = new BluetoothGattCharacteristic(WEBSERVICE_BODY,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE);
        service.addCharacteristic(characteristic);

        // WEBSERVICE Response Status
        characteristic = new BluetoothGattCharacteristic(WEBSERVICE_STATUS,
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ);
        descriptor = new BluetoothGattDescriptor(CCCD,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE);
        descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
        characteristic.addDescriptor(descriptor);
        service.addCharacteristic(characteristic);

        gattServer.addService(service);
    }

    BluetoothGattServerCallback gattCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            Log.i("Fjase", "onConnectionStateChange: " + status + " - " + newState);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            characteristic.setValue(value);
            if (characteristic.getUuid().equals(WEBSERVICE_BODY)) {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
                HttpPutAsyncTask httpPut = new HttpPutAsyncTask(GattServer.this, new String(value));
                httpPut.execute();
            }
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            Log.i("Fjase", "CCCD set");
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
            descriptor.setValue(value);
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value);
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
        }
    };

    public void onPostExecute(int statusCode) {
        BluetoothGattCharacteristic characteristic = gattServer.getService(WEBSERVICE_SERV).getCharacteristic(WEBSERVICE_STATUS);
        characteristic.setValue(("" + statusCode).getBytes());
        gattServer.notifyCharacteristicChanged(btManager.getConnectedDevices(BluetoothGatt.GATT).get(0), characteristic, false);
    }
}
