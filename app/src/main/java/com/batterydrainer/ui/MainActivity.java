package com.batterydrainer.ui;

import android.app.Activity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
// import android.widget.Toast;

import com.batterydrainer.managers.BluetoothScanManager;
import com.batterydrainer.managers.DrainManager;
import com.batterydrainer.managers.WifiScanManager;
import com.batterydrainer.models.BatteryInfo;
import com.batterydrainer.services.DrainForegroundService;
import com.batterydrainer.utils.DialogUtils;
import com.batterydrainer.utils.PermissionUtils;
import com.batterydrainer.utils.SharedPrefsUtils;
import com.batterydrainer.R;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.Disposable;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = "+_ManAtv";
    private SwitchCompat      switchFlash;
    private SwitchCompat      switchCpu;
    private SwitchCompat      switchGpu;
    private SwitchCompat      switchScreen;
    private SwitchCompat      switchGps;
    private SwitchCompat      switchWifi;
    private SwitchCompat      switchBluetooth;
    private TextView          btnStart;
    private ImageView         ivAboutApp;
    private ImageView         ivSettings;
    private Disposable        isDrainingDisposable;
    private TextView          tvBattLevel;
    private TextView          tvVoltage;
    private TextView          tvBattTemp;
    private BroadcastReceiver batteryLevelReceiver;

    private AlertDialog aboutDialog;
    private AlertDialog openSettingsDialog;
    private AlertDialog permissionRationaleDialog;

    MainViewModel mainViewModel;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {

        private static final String TAG = "BatteryDrainer";
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent != null) {
                if ("com.batterydrainer.START_BATTERY_DRAIN".equals(intent.getAction())) {
                    Log.d(TAG, "Received START_BATTERY_DRAIN intent");
                    startDraining();
                } else if ("com.batterydrainer.STOP_BATTERY_DRAIN".equals(intent.getAction())) {
                    Log.d(TAG, "Received STOP_BATTERY_DRAIN intent");
                    stopDraining();
                } else if ("com.batterydrainer.ENABLE_GPU".equals(intent.getAction())) {

                    boolean enableGpu = intent.getBooleanExtra("enable", false);
                    Log.d(TAG, "Received ENABLE_GPU intent with parameter: " + enableGpu);
                    if (switchGpu != null) {
                        switchGpu.setChecked(enableGpu);
                    }

                } else if ("com.batterydrainer.ENABLE_FLASH_LIGHT".equals(intent.getAction())) {

                    boolean enableFlashLight = intent.getBooleanExtra("enable", false);
                    Log.d(TAG,
                        "Received ENABLE_FLASH_LIGHT intent with parameter: " + enableFlashLight);
                    if (switchFlash != null) {
                        switchFlash.setChecked(enableFlashLight);
                    }

                } else if ("com.batterydrainer.ENABLE_SCREEN_LOCK".equals(intent.getAction())) {
                    // Custom logic for handling the "ENABLE_FLASH_LIGHT" intent
                    boolean enableScreenLock = intent.getBooleanExtra("enable", false);
                    Log.d(TAG,
                        "Received ENABLE_SCREEN_LOCK intent with parameter: " + enableScreenLock);
                    if (switchScreen != null) {
                        switchScreen.setChecked(true);
                    }
                } else if ("com.batterydrainer.DRAIN_LIMIT".equals(intent.getAction())) {
                    int drainLimit = intent.getIntExtra("limit", 0);
                    Log.d(TAG, "Received DRAIN_LIMIT intent with parameter: " + drainLimit);
                    TextView     tvLevelValue = findViewById(R.id.tv_settings_battery_level_seekbar_value);
                    tvLevelValue.setText(String.valueOf(drainLimit));
                }
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainViewModel = ViewModelProviders.of(MainActivity.this).get(MainViewModel.class);
        setupViews();
        setupSwitchStates();
        setupObservables();

        IntentFilter filter = new IntentFilter();
        filter.addAction("com.batterydrainer.START_BATTERY_DRAIN");
        filter.addAction("com.batterydrainer.STOP_BATTERY_DRAIN");
        filter.addAction("com.batterydrainer.ENABLE_GPU");
        filter.addAction("com.batterydrainer.ENABLE_FLASH_LIGHT");
        filter.addAction("com.batterydrainer.ENABLE_SCREEN_LOCK");
        filter.addAction("com.batterydrainer.DRAIN_LIMIT");

        this.registerReceiver(broadcastReceiver,filter);
    }

    private void setupViews() {
        switchFlash = findViewById(R.id.switch_flash);
        switchScreen = findViewById(R.id.switch_screen);
        switchCpu = findViewById(R.id.switch_cpu);
        switchGpu = findViewById(R.id.switch_gpu);
        switchGps = findViewById(R.id.switch_gps);
        switchWifi = findViewById(R.id.switch_wifi);
        switchBluetooth = findViewById(R.id.switch_bluetooth);
        btnStart = findViewById(R.id.tv_start);
        ivAboutApp = findViewById(R.id.iv_about);
        ivSettings = findViewById(R.id.iv_settings);
        tvBattLevel = findViewById(R.id.tv_battery_percentage);
        tvBattTemp = findViewById(R.id.tv_battery_temp);
        tvVoltage = findViewById(R.id.tv_battery_voltage);

        switchFlash.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!PermissionUtils.hasCameraPermission(MainActivity.this)) {
                    switchFlash.setChecked(false);
                    showPermissionRationaleDialog(PermissionUtils.REQUEST_CODE_CAMERA);
                }
            }
        });

        switchScreen.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (isChecked) {
                if (!PermissionUtils.hasWriteSettingsPermission(MainActivity.this)) {
                    switchScreen.setChecked(false);
                    showPermissionRationaleDialog(PermissionUtils.REQUEST_CODE_WRITE_SETTINGS);
                }
            }
        }));

        switchGps.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (isChecked) {
                if (!PermissionUtils.hasLocationPermission(MainActivity.this)) {
                    switchGps.setChecked(false);
                    showPermissionRationaleDialog(PermissionUtils.REQUEST_CODE_FINE_LOCATION);
                }
            }
        }));

        switchWifi.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (isChecked) {
                if (!WifiScanManager.getInstance().isWifiScanEnabled(getApplication())) {
                    switchWifi.setChecked(false);
                    showPermissionRationaleDialog(PermissionUtils.REQUEST_CODE_WIFI);
                }
            }
        }));

        switchBluetooth.setOnCheckedChangeListener(((buttonView, isChecked) -> {
            if (isChecked) {
                if (!BluetoothScanManager.getInstance().isBluetoothEnabled()) {
                    switchBluetooth.setChecked(false);
//                    showPermissionRationaleDialog(PermissionUtils.REQUEST_CODE_BLUETOOTH);
                    BluetoothScanManager.getInstance().enableBluetooth(this);
                }
            }
        }));

        btnStart.setOnClickListener(v -> {
            DrainManager drainManager = DrainManager.getInstance(getApplication());
            if (drainManager.isDraining()) {
                stopDraining();
            } else {
                startDraining();
            }

        });

        ivAboutApp.setOnClickListener(v -> showAboutAppDialog());

        ivSettings.setOnClickListener(v -> showSettingsDialogFragment());
    }

    private void setupSwitchStates() {
        boolean[] switchStates = SharedPrefsUtils.getSwitchStates(this);
        boolean   flashOn      = switchStates[0];
        boolean   screenOn     = switchStates[1];
        boolean   cpuOn        = switchStates[2];
        boolean   gpuOn        = switchStates[3];
        boolean   locationOn   = switchStates[4];
        boolean   wifiOn       = switchStates[5];
        boolean   bluetoothOn  = switchStates[6];

        if (PermissionUtils.hasCameraPermission(this)) {
            switchFlash.setChecked(flashOn);
        }
        if (PermissionUtils.hasWriteSettingsPermission(this)) {
            switchScreen.setChecked(screenOn);
        }
        switchCpu.setChecked(cpuOn);
        switchGpu.setChecked(gpuOn);
        if (PermissionUtils.hasLocationPermission(this)) {
            switchGps.setChecked(locationOn);
        }
        if (WifiScanManager.getInstance().isWifiScanEnabled(getApplication())) {
            switchWifi.setChecked(wifiOn);
        }
        if (BluetoothScanManager.getInstance().isBluetoothEnabled()) {
            switchBluetooth.setChecked(bluetoothOn);
        }
    }

    private void setupObservables() {
        isDrainingDisposable = DrainManager.getInstance(getApplication())
                .getDrainBehaviorSubject()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(b -> {
                    if (b == null) {
                        return;
                    }
                    setStartDrainingButtonState(b);
                });

        Observer<String[]> batteryInfoObserver = batteryInfo -> {
            if(batteryInfo != null && batteryInfo.length == 3){
                tvBattLevel.setText(batteryInfo[0]);
                tvBattTemp.setText(batteryInfo[1]);
                tvVoltage.setText(batteryInfo[2]);
            }
        };
        mainViewModel.getBatteryInfoLiveData().observe(MainActivity.this, batteryInfoObserver);
    }

    private void setupBatteryLevelReceiver() {
        IntentFilter batteryLevelFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        batteryLevelReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level           = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale           = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                int tempDeciCelsius = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                int milliVoltage    = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);

                BatteryInfo batteryInfo = new BatteryInfo(level, milliVoltage, tempDeciCelsius, scale);
                mainViewModel.setBatteryInfo(batteryInfo, SharedPrefsUtils.getUsesFahrenheit(MainActivity.this));
            }
        };

        registerReceiver(batteryLevelReceiver, batteryLevelFilter);
    }

    private void setStartDrainingButtonState(boolean isNowDraining) {
        if (isNowDraining) {
            btnStart.setText(R.string.stop);
            btnStart.setBackgroundResource(R.drawable.rounded_shape_red);
            btnStart.setTextColor(ContextCompat.getColor(this, R.color.material_red_400));
        } else {
            btnStart.setText(R.string.start);
            btnStart.setBackgroundResource(R.drawable.rounded_shape_green);
            btnStart.setTextColor(ContextCompat.getColor(this, R.color.material_green_400));
        }
    }

    private void startDraining() {
        if (DrainManager.getInstance(getApplication()).isDraining()) {
            return;
        }
        Intent startIntent = new Intent(MainActivity.this, DrainForegroundService.class);
        startIntent.setAction(DrainForegroundService.ACTION_START);

        startIntent.putExtra(DrainForegroundService.KEY_FLASH, switchFlash.isChecked());
        startIntent.putExtra(DrainForegroundService.KEY_SCREEN, switchScreen.isChecked());
        startIntent.putExtra(DrainForegroundService.KEY_CPU, switchCpu.isChecked());
        startIntent.putExtra(DrainForegroundService.KEY_GPU, switchGpu.isChecked());
        startIntent.putExtra(DrainForegroundService.KEY_GPS, switchGps.isChecked());
        startIntent.putExtra(DrainForegroundService.KEY_WIFI, switchWifi.isChecked());
        startIntent.putExtra(DrainForegroundService.KEY_BLUETOOTH, switchBluetooth.isChecked());

        // Toast.makeText(this, "Drain Started!", Toast.LENGTH_SHORT).show();
        saveSwitchStates();
        startService(startIntent);
    }

    private void stopDraining() {
        Intent stopIntent = new Intent(MainActivity.this, DrainForegroundService.class);
        stopIntent.setAction(DrainForegroundService.ACTION_STOP);
        startService(stopIntent);

    }

    private void showSettingsDialogFragment() {
        SettingsDialogFragment fragment = SettingsDialogFragment.getNewInstance();
        fragment.show(getSupportFragmentManager(), "SETTINGS_FRAGMENT");
    }

    private void showOpenSettingsDialog(int requestCode) {
        if (openSettingsDialog != null) {
            openSettingsDialog.dismiss();
        }
        openSettingsDialog = DialogUtils.getOpenSettingsDialog(this, requestCode);
    }

    private void showAboutAppDialog() {
        if (aboutDialog != null) {
            aboutDialog.dismiss();
        }
        aboutDialog = DialogUtils.getAboutAppDialog(this);
    }

    private void showPermissionRationaleDialog(int requestCode) {
        if (permissionRationaleDialog != null) {
            permissionRationaleDialog.dismiss();
        }
        permissionRationaleDialog = DialogUtils.showPermissionRationaleDialog(this, requestCode);
    }

    private void saveSwitchStates() {
        SharedPrefsUtils.setSwitchStates(this,
                                         switchFlash.isChecked(),
                                         switchScreen.isChecked(),
                                         switchCpu.isChecked(),
                                         switchGpu.isChecked(),
                                         switchGps.isChecked(),
                                         switchWifi.isChecked(),
                                         switchBluetooth.isChecked());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        saveSwitchStates();
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PermissionUtils.REQUEST_CODE_CAMERA: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        switchFlash.setChecked(true);
                    } else {
                        if (PermissionUtils.canRequestCameraPermission(MainActivity.this)) {
                            showPermissionRationaleDialog(requestCode);
                        } else {
                            showOpenSettingsDialog(requestCode);
                        }
                    }
                }
                break;
            }
            case PermissionUtils.REQUEST_CODE_FINE_LOCATION: {
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        switchGps.setChecked(true);
                    } else {
                        if (PermissionUtils.canRequestLocationPermission(MainActivity.this)) {
                            showPermissionRationaleDialog(requestCode);
                        } else {
                            showOpenSettingsDialog(requestCode);
                        }
                    }
                }
                break;
            }
            default: {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case PermissionUtils.REQUEST_CODE_BLUETOOTH: {
                if (resultCode == Activity.RESULT_OK) {
                    switchBluetooth.setChecked(true);
                } else {
                    showPermissionRationaleDialog(requestCode);
                }
                break;
            }
            case PermissionUtils.REQUEST_CODE_WRITE_SETTINGS: {
                if (PermissionUtils.hasWriteSettingsPermission(this)) {
                    switchScreen.setChecked(true);
                }
                break;
            }
            default: {
                onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupBatteryLevelReceiver();
    }

    @Override
    protected void onPause() {
        if (batteryLevelReceiver != null) {
            unregisterReceiver(batteryLevelReceiver);
        }
        if (openSettingsDialog != null && openSettingsDialog.isShowing()) {
            openSettingsDialog.dismiss();
        }
        if (aboutDialog != null && aboutDialog.isShowing()) {
            aboutDialog.dismiss();
        }
        if (permissionRationaleDialog != null && permissionRationaleDialog.isShowing()) {
            permissionRationaleDialog.dismiss();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (isDrainingDisposable != null) {
            isDrainingDisposable.dispose();
            isDrainingDisposable = null;
        }
        super.onDestroy();
    }
}
