package com.rmthrt.camerabtremote;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


//TODO PROJECT ISSUES:
//TODO give modern appareance for remote_control ( white stripe at bottom for connection infos, then big button in the middle, then VolumeButton,headsetbutton And WifiButton) -> BE SURE HOW TO CODE THIS FUNCTION BEFORE
//TODO handle connection issue :should display "connecting"  during pairing
//TODO try to get shorter time between 2 pics ( avoid auto play pics )


public class MainActivity extends AppCompatActivity {

    private MainActivity.LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private ProgressBar progressBar;
    private ListView listView;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_LOCATION = 2;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;
    private static final String TAG = "MainActivity";

    public static SharedPreferences persistency;
    public static String PERSISTENCY_DEVICE_ADDRESS = "deviceaddress";
    public static String PERSISTENCY_USING_HEADSET = "usingheadset";
    public static String PERSISTENCY_USING_VOLUME_BUTTONS = "usingvolumebuttons";
    public static String PERSISTENCY_USING_VIBRATOR = "usingvvibrator";
    public static String PERSISTENCY_USING_PHONE_OR_BLUETOOTH_PAIRING = "usingphoneorbluetoothpairing";
    public static BluetoothDevice mdevice;


    private void IntentDeviceConnection(BluetoothDevice device)
    {
        final Intent intent = new Intent(getApplicationContext(), DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2  && device.getBondState() == BluetoothDevice.BOND_NONE) {

            device.createBond();
        }
        if(device.getBondState() != BluetoothDevice.BOND_NONE)
        {
            if (mScanning) {
                scanLeDevice(false);
            }

            mBluetoothAdapter.getBluetoothLeScanner().stopScan(mLeScanCallback);
            mdevice = device;
            startActivity(intent);
        }


    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_launcher);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setTitle("Camera Bt Remote");

        persistency = getSharedPreferences(PERSISTENCY_DEVICE_ADDRESS, Context.MODE_PRIVATE);
        persistency = getSharedPreferences(PERSISTENCY_USING_HEADSET, Context.MODE_PRIVATE);
        persistency = getSharedPreferences(PERSISTENCY_USING_PHONE_OR_BLUETOOTH_PAIRING,Context.MODE_PRIVATE);
        persistency = getSharedPreferences(PERSISTENCY_USING_VOLUME_BUTTONS, Context.MODE_PRIVATE);




        mHandler = new Handler();
        listView=findViewById(R.id.lv_device_list);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                IntentDeviceConnection(mLeDeviceListAdapter.getDevice(i));
            }


        });

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if(persistency.getString(PERSISTENCY_DEVICE_ADDRESS,null)=="")
        {
            final Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(intent);
        }

        String  alreadyPairedDevice = persistency.getString(PERSISTENCY_DEVICE_ADDRESS,null);

            if(alreadyPairedDevice==null)
            {
                SharedPreferences.Editor editor = MainActivity.persistency.edit();
                editor.putBoolean(MainActivity.PERSISTENCY_USING_PHONE_OR_BLUETOOTH_PAIRING, false);
                editor.commit();
                final Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
            }


    }





    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.device_scan, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.loading_progress);
            progressBar = menu.findItem(R.id.menu_refresh).getActionView().findViewById(R.id.progressBarCircle);
            progressBar.getIndeterminateDrawable().setColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
            case android.R.id.home:
                final Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return true;
    }



    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(broadcastReceiver,intentFilter);

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.

        if (BluetoothLeService.mConnectionState == BluetoothLeService.STATE_CONNECTED) {
            final Intent intent = new Intent(this, DeviceControlActivity.class);
            intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, BluetoothLeService.currentDeviceName);
            intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, BluetoothLeService.currentDeviceAddress);
            if (mScanning) {
                scanLeDevice(false);
            }
            startActivity(intent);

        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                if (!mBluetoothAdapter.isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
            }else{
                checkLocationPermission();
            }

            // Initializes list view adapter.
            mLeDeviceListAdapter = new MainActivity.LeDeviceListAdapter();

            listView.setAdapter(mLeDeviceListAdapter);
            scanLeDevice(true);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            scanLeDevice(false);
            mLeDeviceListAdapter.clear();
        } catch (Exception e) {

        }
        unregisterReceiver(broadcastReceiver);
    }



    private void scanLeDevice(final boolean enable) {

        final BluetoothLeScanner bluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.notifyDataSetChanged();
            mScanning = true;
            bluetoothLeScanner.startScan(mLeScanCallback);
        } else {
            mScanning = false;
            bluetoothLeScanner.stopScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }


    // Adapter for holding devices found through scanning.
    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            String  alreadyPairedDevice = persistency.getString(PERSISTENCY_DEVICE_ADDRESS,null);

            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);

                if(device.getAddress().equals(alreadyPairedDevice) ){
                    IntentDeviceConnection(device);
                }
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            MainActivity.ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_scan, null);
                viewHolder = new MainActivity.ViewHolder();
                viewHolder.deviceAddress = view.findViewById(R.id.device_address);
                viewHolder.deviceName = view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (MainActivity.ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    // Device scan callback.

    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            mLeDeviceListAdapter.addDevice(result.getDevice());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_LOCATION) {
            // Request for camera permission.
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.location_permission_granted, Toast.LENGTH_SHORT).show();
            } else {
                // Permission request was denied.
                Toast.makeText(this, R.string.location_permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.location_permission_title)
                        .setMessage(R.string.location_permission_message)
                        .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                        PERMISSION_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSION_REQUEST_LOCATION);
            }

        } else {
            enableLocation();
        }
    }

    public void enableLocation(){
        boolean gps_enabled=false, network_enabled=false;
        LocationManager lm = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }catch (Exception ex){}
        try{
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        }catch (Exception ex){}
        if(!gps_enabled && !network_enabled){
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(R.string.location_enable_title);
            dialog.setMessage(R.string.location_enable_message);
            dialog.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Intent myIntent = new Intent( Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    MainActivity.this.startActivity(myIntent);
                }
            });
            dialog.setNegativeButton(R.string.dialogs_cancel, new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            });
            dialog.show();
        }
    }

    private BroadcastReceiver broadcastReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        break;
                    case BluetoothAdapter.STATE_ON:
                        scanLeDevice(true);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        break;
                }
            }
        }
    };


}
