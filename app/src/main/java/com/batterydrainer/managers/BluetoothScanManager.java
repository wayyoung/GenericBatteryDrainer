package com.batterydrainer.managers;

import android.Manifest.permission;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import com.batterydrainer.utils.Logg;
import com.batterydrainer.utils.PermissionUtils;

/**
 * BluetoothScanManager
 * Created by jason on 3/7/18.
 */
public class BluetoothScanManager {

    private static final String LOG_TAG = "+_BthMgr";
    private static BluetoothScanManager instance;
    private BroadcastReceiver bluetoothReceiver;
    private boolean isRegistered = false;


    public static BluetoothScanManager getInstance() {
        if (instance == null) {
            instance = new BluetoothScanManager();
        }
        return instance;
    }

    private BluetoothScanManager() {
    }

    public void setBluetoothScan(Application application, boolean setOn) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (!isBluetoothEnabled()) {
            return;
        }
        if (setOn) {
            if (isRegistered) {
                return;
            }
            isRegistered = true;
            bluetoothReceiver = new BroadcastReceiver() {
                public void onReceive(Context context, Intent intent) {
                    Logg.d(LOG_TAG, "Starting new scan!");
                    if (ActivityCompat.checkSelfPermission(application, permission.BLUETOOTH_SCAN)
                        != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    adapter.startDiscovery();
                }
            };
            IntentFilter intentFilter = new IntentFilter(
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            application.registerReceiver(bluetoothReceiver, intentFilter);
            adapter.startDiscovery();
            Logg.d(LOG_TAG, "Started Bluetooth Discovery");
        } else {
            if (isRegistered && bluetoothReceiver != null) {
                application.unregisterReceiver(bluetoothReceiver);
                isRegistered = false;
                bluetoothReceiver = null;
                Logg.d(LOG_TAG, "Stopped Bluetooth Discovery");
            }
        }
    }

    public void enableBluetooth(AppCompatActivity activity) {
//        if(hasBluetooth()){
//            BluetoothAdapter.getDefaultAdapter().enable();
//        }
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        if (ActivityCompat.checkSelfPermission(activity, permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        activity.startActivityForResult(intent, PermissionUtils.REQUEST_CODE_BLUETOOTH);
    }

    public boolean isBluetoothEnabled(){
        return hasBluetooth() && BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    private boolean hasBluetooth(){
        return BluetoothAdapter.getDefaultAdapter() != null;
    }
}
