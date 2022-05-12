
        /*
         * Copyright (C) 2013 The Android Open Source Project
         *
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *      http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         */

        package com.polkapolka.bluetooth.le;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;


        /**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {


    private Handler handler = new Handler();

    private TextView version;
    private final static String TAG = DeviceControlActivity.class.getSimpleName();
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private int[] RGBFrame = {0, 0, 0};
    private TextView isSerial;
    private TextView chargingText;
    private TextView TRQtxt;
    private TextView PWRtxt;
    private TextView SOCtxt;
    private TextView TMPtxt;
    private TextView IGBTtxt;
    private TextView BATTEMPtxt;
    private TextView SysAvailabilitytxt;
    private TextView mConnectionState;
    private TextView mDataField;
    private SeekBar mRed, mGreen, mBlue;
    private String mDeviceName;
    private String mDeviceAddress;
    private ImageView scale;
    private TextView SD_Logging_Status;
    private TextView Fault_Code_Status_RUN_HI;
    private TextView Fault_Code_Status_RUN_LO;
    private TextView Fault_Code_Status_POST_HI;
    private TextView Fault_Code_Status_POST_LO;
    private ProgressBar waitingForSerialService;
    private boolean serialServiceFound = true;
    public boolean fileTransferFailed = false;
    private boolean showValues = false;
    private LinearLayout buttonsDisplay, horizBarsDisplay;
    private RelativeLayout torqueDisplay;

    private ImageView logo;
    private ImageView getVersionImage;
    private Button Off, Street, Track, Overboost, readBin, sendBin, bootP1, bootP2, backup, update;
    //  private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;
    private boolean incomingDataRequested = false;
    private BluetoothGattCharacteristic characteristicTX;
    private BluetoothGattCharacteristic characteristicRX;
    private int SOC = 0;
    private int MAP = 0;
    private int eMotTemp = 0;
    private int BattTemp = 0;
    private int InvTemp = 0;
    private int inRecovery = 0;
    private int BLEPointer = 0;
    private int battTemp_Okay, SysAvail_BattTemp, BMS_CellTmin, Tmot_okay2Torq, SysAvail_MtrTemp, RMS_T_motor, SysAvail_InvTemp, maxIGBT = 0;
    private int counter1 = 0;
    int dataDelay = 20;
    private int counter2 = 0;
    private int fileProgress = 0;
    private int fileProgressPercent = 0;
    final ByteArrayOutputStream bo = new ByteArrayOutputStream();
    byte[] buffer = new byte[262144];
    int[] buffer2send = new int[262144];
    byte[] data = new byte[262144];

    int fileSize = 0;
    String constructedMessage;
//    public final static UUID HM_RX_TX = UUID.fromString(SampleGattAttributes.HM_RX_TX);
    boolean done = false;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    //GPS location
    private Double mlat, mlong;
    private TextView valCheck;
    private Location mCurrentLocation;
    private boolean requestingLocationUpdates;
    private ArrayList<LogData> logFile = new ArrayList<LogData>();
    private SimpleDateFormat sdf;
    private int gen, hp, torq;
    public boolean fileTransferSuccessful = false ;
    public int checkSumApp = 0;
    public int checkSumVCU = 0;
    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };


    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
//                int[] iii = intent.getIntArrayExtra(mBluetoothLeService.EXTRA_DATA);

                /*String s = intent.getStringExtra(mBluetoothLeService.EXTRA_DATA).replace("\r\n","");
                CharSequence ww = intent.getCharSequenceExtra(mBluetoothLeService.EXTRA_DATA);
                byte[] w = intent.getByteArrayExtra(mBluetoothLeService.EXTRA_DATA);
//                intent.getCharArrayExtra()
                int[] iss = intent.getIntArrayExtra(mBluetoothLeService.EXTRA_DATA);
                String idkman = mBluetoothLeService.EXTRA_DATA;
                byte q = intent.getByteExtra(mBluetoothLeService.EXTRA_DATA,(byte)5);*/
//                boolean idk = Character.toString(s.charAt(0)).equals(Character.toString(s.charAt(1)));
//                boolean idk2 = Character.toString(s.charAt(4)).equals(Character.toString(s.charAt(5)));
//
//                int j = s.length();
//                byte [][] bytes = new byte[j][2];
//                int [] is = new int[j];
//                byte[] bs = new byte[j];
//                byte[] bs2 = new byte[j];
//                for (int i = 0;i<j;i++){
//                    bytes[i] = Character.toString(s.charAt(i)).getBytes();
//                    is[i] = (int) ww.charAt(i);
//                    bs[i] = (byte) ww.charAt(i);
//
//                }

                displayData(intent.getIntArrayExtra(mBluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private void clearUI() {
        mDataField.setText(R.string.no_data);
    }

    private void sendWholeFile() {


//        byte[] value = new byte[20];
//        for (fileProgress =  0; fileProgress < fileSize ; fileProgress = fileProgress + 20) {
//
//            for (int i = 0; i < 20; i++) {
//                value[i] = (byte) (data[fileProgress + i] & 0xFF);
//            }
//            mBluetoothLeService.writeCharacteristicTest(value);
//            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
//
//            try {
//                //set time in mili
//                Thread.sleep(15);
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
    }

    private void sendDataChunk() {


        byte[] value = new byte[20];

        for (int i = 0; i < 20; i++) {
            value[i] = (byte) (data[fileProgress] & 0xFF);
            fileProgress++;
        }
        mBluetoothLeService.writeCharacteristicTest(value);
        mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        Log.d(TAG, "File Progress in SendDataChunk function = " + fileProgress);


}


    private void send500DataChunks1() {


//        if (fileProgress < (fileSize - 1000)) {
//
//            byte[] value = new byte[20];
//
//            for (int j = 0; j < 50; j++) {
//                for (int i = 0; i < 20; i++) {
//                    value[i] = (byte) (data[fileProgress + ((j * 20) + i)] & 0xFF);
//
//                    mBluetoothLeService.writeCharacteristicTest(value);
//
//                    try {
//                        //set time in mili
//                        Thread.sleep(dataDelay);
//
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
//
//
//
//            try {
//                //set time in mili
//                Thread.sleep(25);
//
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//
//        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.gatt_services_characteristics);

        //GPS
        version = (TextView) findViewById(R.id.version);
        valCheck = (TextView) findViewById(R.id.testval);
        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssz");


        //        startLocationUpdates();
        //
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);


        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        // is serial present?
        isSerial = (TextView) findViewById(R.id.isSerial);

        SOCtxt = (TextView) findViewById(R.id.bmsSOC);
        TMPtxt = (TextView) findViewById(R.id.Emottmp);
        IGBTtxt = (TextView) findViewById(R.id.SysAvailTxt);
        BATTEMPtxt = (TextView) findViewById(R.id.battTmax);

        TRQtxt = (TextView) findViewById(R.id.Emottrq);
        PWRtxt = (TextView) findViewById(R.id.Emotpwr);
        SD_Logging_Status = (TextView) findViewById(R.id.SD_Status);
        SD_Logging_Status.setVisibility(View.INVISIBLE); //TODO: remove later
        Fault_Code_Status_POST_HI = (TextView) findViewById(R.id.POST_FAULT_HI);
        Fault_Code_Status_POST_LO = (TextView) findViewById(R.id.POST_FAULT_LO);
        Fault_Code_Status_RUN_HI = (TextView) findViewById(R.id.RUN_FAULT_HI);
        Fault_Code_Status_RUN_LO = (TextView) findViewById(R.id.RUN_FAULT_LO);
        chargingText = (TextView) findViewById(R.id.chargingTxt);

        waitingForSerialService = (ProgressBar) findViewById(R.id.waitingToBegin);
        torqueDisplay = (RelativeLayout) findViewById(R.id.torqueDial);
        buttonsDisplay = (LinearLayout) findViewById(R.id.buttonsPanel);
        horizBarsDisplay = (LinearLayout) findViewById(R.id.horizontalBars);

        chargingText.setVisibility(View.INVISIBLE);

        Fault_Code_Status_POST_HI.setVisibility(View.VISIBLE);
        Fault_Code_Status_POST_LO.setVisibility(View.INVISIBLE);
        Fault_Code_Status_RUN_HI.setVisibility(View.INVISIBLE);
        Fault_Code_Status_RUN_LO.setVisibility(View.INVISIBLE);

        Off = (Button) findViewById(R.id.buttonOFF);
        getVersionImage = (ImageView) findViewById(R.id.bigLogo);

        Street = (Button) findViewById(R.id.buttonSTREET);
        Track = (Button) findViewById(R.id.buttonTRACK);
        Overboost = (Button) findViewById(R.id.buttonOVERBOOST);

        readBin = (Button) findViewById(R.id.btngetBinaryFile);
        sendBin = (Button) findViewById(R.id.btnSendFile);
        bootP1 = (Button) findViewById(R.id.btnReqVersionNumber);
        bootP2 = (Button) findViewById(R.id.btnReserved2);
        backup = (Button) findViewById(R.id.btnBackUpZ1);
        update = (Button) findViewById(R.id.btnUpdateZ1);

        String currentString = "0,0,0,0,3";
        String[] separated = currentString.split(",");
//        SOCtxt.setText(separated[0].trim());
//        separated[1] = separated[1].trim();

        mDataField = (TextView) findViewById(R.id.data_value);
        mRed = (SeekBar) findViewById(R.id.seekRed);
        mGreen = (SeekBar) findViewById(R.id.seekGreen);
        mBlue = (SeekBar) findViewById(R.id.seekBlue);
        scale = (ImageView) findViewById(R.id.scale);

        scale.setVisibility(View.GONE);
        mRed.setVisibility(View.GONE);

        readSeek(mRed, 0);
        readSeek(mGreen, 1);
        readSeek(mBlue, 2);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Swipe actions
//        torqueDisplay.setOnTouchListener(new OnSwipeTouchListener(DeviceControlActivity.this) {
//
//            public void onSwipeTop() {
//                Toast.makeText(DeviceControlActivity.this, "swipe Top on dial test", Toast.LENGTH_SHORT).show();
//                readBinaryFromServer();
//
//            }
//
//            public void onSwipeRight() {
//                Toast.makeText(DeviceControlActivity.this, "swipe Right on dial test", Toast.LENGTH_SHORT).show();
//
//                if (mConnected) {
////            Toast.makeText(DeviceControlActivity.this, str, Toast.LENGTH_SHORT).show();
//                    characteristicTX.setValue("<//////>"); // ascii 47 to boot from P1
//                    mBluetoothLeService.writeCharacteristic(characteristicTX);
//                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
//                }
//
//            }
//
//            public void onSwipeLeft() {
//                Toast.makeText(DeviceControlActivity.this, "swipe Left / boot in P2", Toast.LENGTH_SHORT).show();
//                if (mConnected) {
////            Toast.makeText(DeviceControlActivity.this, str, Toast.LENGTH_SHORT).show();
//                    characteristicTX.setValue("<F0FFFE>");
//                    mBluetoothLeService.writeCharacteristic(characteristicTX);
//                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
//                }
//            }
//
//            public void onSwipeBottom() {//
//
////                int i = 0 ;
////                for (int i = 0 ; i < 10 ; i = i + 9) {
////                    buffer2send[i] = (int) (data[i] & 0xFF);
////                    buffer2send[i + 1] = (int) (data[i + 1] & 0xFF);
////                    buffer2send[i + 2] = (int) (data[i + 2] & 0xFF);
////                    buffer2send[i + 3] = (int) (data[i + 3] & 0xFF);
////                    buffer2send[i + 4] = (int) (data[i + 4] & 0xFF);
////                    buffer2send[i + 5] = (int) (data[i + 5] & 0xFF);
////                    buffer2send[i + 6] = (int) (data[i + 6] & 0xFF);
////                    buffer2send[i + 7] = (int) (data[i + 7] & 0xFF);
////                    buffer2send[i + 8] = (int) (data[i + 8] & 0xFF);
//
////                    constructedMessage = String.format("<%02X%02X%02X%02X%02X%02X%02X%02X%02X>", buffer2send[i], buffer2send[i + 1], buffer2send[i + 2], buffer2send[i + 3], buffer2send[i + 4], buffer2send[i + 5], buffer2send[i + 6], buffer2send[i + 7], buffer2send[i + 8]);
////                    if (mConnected) {
////                        characteristicTX.setValue(constructedMessage);
////                        mBluetoothLeService.writeCharacteristic(characteristicTX);
////                        mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
////                    }
//
////                characteristicTX.setValue(0x80,android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8,0);
////
////                        mBluetoothLeService.writeCharacteristic(characteristicTX);
////                        mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
//
////                characteristicTX.setValue(value);
////                mBluetoothLeService.writeCharacteristic(characteristicTX);
////                mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
//
//            }
//
//        });

        horizBarsDisplay.setOnTouchListener(new OnSwipeTouchListener(DeviceControlActivity.this) {

            public void onSwipeTop() {
                Toast.makeText(DeviceControlActivity.this, "swipe Top on bars test", Toast.LENGTH_SHORT).show();

                if (mConnected) {
//            Toast.makeText(DeviceControlActivity.this, str, Toast.LENGTH_SHORT).show();
                    characteristicTX.setValue("<::::::>");
                    mBluetoothLeService.writeCharacteristic(characteristicTX);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                }
            }

            public void onSwipeRight() {
                Toast.makeText(DeviceControlActivity.this, "swipe Right on bars test", Toast.LENGTH_SHORT).show();
                showValues = false;
            }

            public void onSwipeLeft() {
                Toast.makeText(DeviceControlActivity.this, "swipe Left on bars test", Toast.LENGTH_SHORT).show();
                showValues = true;
            }

            public void onSwipeBottom() {
                Toast.makeText(DeviceControlActivity.this, "swipe Bottom on bars test", Toast.LENGTH_SHORT).show();
                    readVersionFromServer();

            }
        });


        readBin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                Toast.makeText(DeviceControlActivity.this, "obtain file from github", Toast.LENGTH_SHORT).show();
                readBinaryFromServer();            }
        });


        sendBin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(DeviceControlActivity.this, "begin file transfer", Toast.LENGTH_SHORT).show();
                initiateFileTransfer();
//                sendWholeFileThreadWithBreak();
//                send5DataChunks();
                sendDataChunk();
            }
        });


        bootP1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(DeviceControlActivity.this, "Boot in P1", Toast.LENGTH_SHORT).show();
                if (mConnected) {
                    characteristicTX.setValue("<//////>");
                    mBluetoothLeService.writeCharacteristic(characteristicTX);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                }
                mConnectionState.setText("REBOOTING");

            }
        });

        bootP2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(DeviceControlActivity.this, "Boot in P2", Toast.LENGTH_SHORT).show();
                if (mConnected) {
                    characteristicTX.setValue("<F0FFFE>");
                    mBluetoothLeService.writeCharacteristic(characteristicTX);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                }
                mConnectionState.setText("REBOOTING");

            }
        });


        backup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                if (!fileTransferSuccessful){
                    Toast.makeText(DeviceControlActivity.this, "You can not do this, until you complete a successful file transfer. Press get file, then send file.", Toast.LENGTH_LONG).show();

                }else {

                    Toast.makeText(DeviceControlActivity.this, "Backup Existing Partition", Toast.LENGTH_SHORT).show();
                    if (mConnected) {
                        characteristicTX.setValue("<<<<<<<>");
                        mBluetoothLeService.writeCharacteristic(characteristicTX);
                        mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                    }


                    try {
                        //set time in mili
                        Thread.sleep(1500);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                    Toast.makeText(DeviceControlActivity.this, "Copying OTA File to Target", Toast.LENGTH_SHORT).show();
                    if (mConnected) {
                        characteristicTX.setValue("<::::::>");
                        mBluetoothLeService.writeCharacteristic(characteristicTX);
                        mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                    }

                    try {
                        //set time in mili
                        Thread.sleep(1500);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        update.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                Toast.makeText(DeviceControlActivity.this, "Rolling Back", Toast.LENGTH_SHORT).show();
                if (mConnected) {
                    characteristicTX.setValue("<;;;;;;>");
                    mBluetoothLeService.writeCharacteristic(characteristicTX);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                }


                try {
                    //set time in mili
                    Thread.sleep(1500);

                }catch (Exception e){
                    e.printStackTrace();
                }

            }
        });

        backup.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                Toast.makeText(DeviceControlActivity.this, "Backup Existing Partition", Toast.LENGTH_SHORT).show();
                if (mConnected) {
                    characteristicTX.setValue("<<<<<<<>");
                    mBluetoothLeService.writeCharacteristic(characteristicTX);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                }


                try {
                    //set time in mili
                    Thread.sleep(1500);

                } catch (Exception e) {
                    e.printStackTrace();
                }


                Toast.makeText(DeviceControlActivity.this, "Copying OTA File to Target", Toast.LENGTH_SHORT).show();
                if (mConnected) {
                    characteristicTX.setValue("<::::::>");
                    mBluetoothLeService.writeCharacteristic(characteristicTX);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                }

                try {
                    //set time in mili
                    Thread.sleep(1500);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                return false;
            }
        });


        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        //  Runnable r = new Runnable() {
        //    @Override
        //  public void run(){

        alertDialog.setTitle("Welcome to the VONNEN app");
        alertDialog.setMessage("By proceeding, I agree to obey all local laws regarding cell phone usage while driving. Furthermore, I will not hold VONNEN responsible for any damage or consequences as a result of using this app.");
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "I ACCEPT",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                        if ((mConnected)) {
//                            characteristicTX.setValue("04");
                            waitingForSerialService.setVisibility(View.GONE);
                            incomingDataRequested = true;
//                            mBluetoothLeService.writeCharacteristic(characteristicTX);
                             mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                            resetLog();
                        }
                        dialog.dismiss();
                        torqueDisplay.setVisibility(View.VISIBLE);
                        buttonsDisplay.setVisibility(View.VISIBLE);
                        horizBarsDisplay.setVisibility(View.VISIBLE);
                    }
                });

//        do {} while (!serialServiceFound);

        alertDialog.show();

//        torqueDisplay.setVisibility(View.VISIBLE);
//        buttonsDisplay.setVisibility(View.VISIBLE);
//        horizBarsDisplay.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
//        startLocationUpdates();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    //for GPS
    //
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }


    public void readBinaryFromServer(){
        new Thread() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                String path ="https://github.com/Vonnen/Release/raw/main/341.bin";
                URL u = null;

                try {
                    u = new URL(path);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
//                    c.setRequestMethod("GET");
//                    c.connect();
//                    InputStream in = c.getInputStream();
//                    in.read(buffer); // Read from Buffer.
//                    bo.write(buffer); // Write Into Buffer.

                    String contentType = c.getContentType();
                    int contentLength = c.getContentLength();
                    fileSize = contentLength ;

                    if (contentType.startsWith("text/") || contentLength == -1) {
                        throw new IOException("This is not a binary file.");
                    }

                    InputStream raw = c.getInputStream();
                    InputStream in = new BufferedInputStream(raw);
                    int bytesRead = 0;
                    int offset = 0;
                    while (offset < contentLength) {

                        bytesRead = in.read(data, offset, data.length - offset);
                        if (bytesRead == -1)
                            break;
                        offset += bytesRead;


//                        constructedMessage = String.format("%06x%02x>",i , buffer[i]);
//                        if (mConnected) {
//                            characteristicTX.setValue(constructedMessage);
//                            mBluetoothLeService.writeCharacteristic(characteristicTX);
//                            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
//                        }
                    }
                    in.close();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String boTostring = Integer.toString(fileSize);
                            Toast.makeText(DeviceControlActivity.this, boTostring, Toast.LENGTH_LONG).show();
                            System.out.println(boTostring);

                            TRQtxt.setText(boTostring);
                            try {
                                bo.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }


    private void sendWholeFileThread(){
        new Thread() {
            @Override
            public void run() {

//                byte[] value = new byte[20];
//                for (fileProgress =  0; fileProgress < fileSize ; fileProgress = fileProgress + 20) {
//
//                    for (int i = 0; i < 20; i++) {
//                        value[i] = (byte) (data[fileProgress + i] & 0xFF);
//                    }
//                    mBluetoothLeService.writeCharacteristicTest(value);
//                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
//
//                    try {
//                        //set time in mili
//                        Thread.sleep(15);
//
//                    }catch (Exception e){
//                        e.printStackTrace();
//                    }
//                }

                String path ="https://raw.githubusercontent.com/Vonnen-Dev/OTA_File/master/version.txt";
                URL u = null;
                try {
                    u = new URL(path);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }
    public void initiateFileTransfer() {
        fileTransferSuccessful = false ;
        checkSumApp = 0 ;
        for (int i = 0; i < fileSize; i++) {
            checkSumApp = checkSumApp +  ( data[i] & 0xFF ) ;
        }
        Fault_Code_Status_POST_HI.setText(Integer.toString(checkSumApp));

        fileProgress = 0 ;
        constructedMessage = String.format("<%06X>", fileSize );
        if (mConnected) {
            characteristicTX.setValue(constructedMessage);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }

        try {
            //set time in mili
            Thread.sleep(250);

        } catch (Exception e){
            e.printStackTrace();
        }
        constructedMessage = String.format("<%06X>", 0 );
        if (mConnected) {
            characteristicTX.setValue(constructedMessage);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }

        try {
            //set time in mili
            Thread.sleep(250);

        }catch (Exception e){
            e.printStackTrace();
        }

    }


    public void sendWholeFileThreadWithBreak(){
        fileTransferFailed = false ;
//        initiateFileTransfer();

        new Thread() {
            @Override
            public void run() {
                int msgCounter = 0 ;
                boolean neverDelayed = true ;
                byte[] value = new byte[20];
                for (fileProgress =  0; fileProgress < fileSize ; fileProgress = fileProgress + 20) {

                    for (int i = 0; i < 20; i++) {
                        value[i] = (byte) (data[fileProgress + i] & 0xFF);

                    }
                    msgCounter++ ;
                    mBluetoothLeService.writeCharacteristicTest(value);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);

                    try {
                        //set time in mili
                        Thread.sleep(20 );

                    }catch (Exception e){
                        e.printStackTrace();
                    }
                    if (msgCounter > 10){
                        msgCounter =  0 ;
                        try {
                            //set time in mili
                            Thread.sleep( 5 );

                        }catch (Exception e){
                            e.printStackTrace();
                        }}
                    if ((fileProgress > 65535) && (neverDelayed)) {
                        neverDelayed = false ;
                        try {
                            //set time in mili
                            Thread.sleep(1500);

                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }

                String path ="https://raw.githubusercontent.com/Vonnen-Dev/OTA_File/master/version.txt";
                URL u = null;
                try {
                    u = new URL(path);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
//                    c.setRequestMethod("GET");
//                    c.connect();
//                    InputStream in = c.getInputStream();
//                    final ByteArrayOutputStream bo = new ByteArrayOutputStream();
//                    byte[] buffer2 = new byte[1024];
//                    in.read(buffer2); // Read from Buffer.
//                    bo.write(buffer2); // Write Into Buffer.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    public void send5DataChunks(){

        new Thread() {
            @Override
            public void run() {



                if (fileProgress < (fileSize)) {

                    byte[] value = new byte[20];

                    for (int i = 0; i < 20; i++) {
                        value[i] = (byte) (data[fileProgress + i ] & 0xFF);
                        fileProgress++;

                    }

                    mBluetoothLeService.writeCharacteristicTest(value);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
                    try {
                        //set time in mili
                        Thread.sleep(99);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    for (int i = 0; i < 20; i++) {
                        value[i] = (byte) (data[fileProgress + i ] & 0xFF);
                        fileProgress++;

                    }

                    mBluetoothLeService.writeCharacteristicTest(value);
                    mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);


                }

                String path ="https://raw.githubusercontent.com/Vonnen-Dev/OTA_File/master/version.txt";
                URL u = null;
                try {
                    u = new URL(path);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }

    public void send5ButtonClick(View v) {
        //Toast.makeText(DeviceControlActivity.this, "test", Toast.LENGTH_SHORT).show();

        if (mConnected) {

            if (!incomingDataRequested) {
//                characteristicTX.setValue("04");
//                incomingDataRequested = true;
            } else {
//                characteristicTX.setValue("05");
//                incomingDataRequested = false;
            }
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }

    }

    public void getVersionInformation(View v) {

        if (mConnected) {
//            Toast.makeText(DeviceControlActivity.this, "Requesting Version", Toast.LENGTH_SHORT).show();
            characteristicTX.setValue("<F0FFFF>");
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }
        else{
            Toast.makeText(DeviceControlActivity.this, "Not connected to anything", Toast.LENGTH_SHORT).show();
        }

    }

    public void sendMAP1(View v) {
        //Toast.makeText(DeviceControlActivity.this, "test", Toast.LENGTH_SHORT).show();

        if (mConnected) {
            characteristicTX.setValue("<F00000>");
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }

    }

    public void sendMAP2(View v) {
        //Toast.makeText(DeviceControlActivity.this, "test", Toast.LENGTH_SHORT).show();

        if (mConnected) {
            characteristicTX.setValue("<F00001>");
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }

    }

    public void sendMAP3(View v) {
        //Toast.makeText(DeviceControlActivity.this, "test", Toast.LENGTH_SHORT).show();

        if (mConnected) {
            characteristicTX.setValue("<F00002>");
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }
    }

    public void sendMAP4(View v) {
        //Toast.makeText(DeviceControlActivity.this, "test", Toast.LENGTH_SHORT).show();

        if (mConnected) {
            characteristicTX.setValue("<F00003>");
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);

                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                writeKML();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void readVersionFromServer(){
        new Thread() {
            @Override
            public void run() {
                String path ="https://raw.githubusercontent.com/Vonnen-Dev/OTA_File/master/version.txt";
                URL u = null;
                try {
                    u = new URL(path);
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
                    c.setRequestMethod("GET");
                    c.connect();
                    InputStream in = c.getInputStream();
                    final ByteArrayOutputStream bo = new ByteArrayOutputStream();
                    byte[] buffer2 = new byte[1024];
                    in.read(buffer2); // Read from Buffer.
                    bo.write(buffer2); // Write Into Buffer.
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String boTostring = bo.toString();
                            Toast.makeText(DeviceControlActivity.this, boTostring, Toast.LENGTH_LONG).show();
                            System.out.println(boTostring);
                            String[] sp = boTostring.split(",");//.replaceAll("\\s","")
                            try {
                                bo.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });

                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }.start();
    }
    private void displayData(int[] data) {

//        if (data_not_zeros){
//
//        }
        if (data != null) {
//            if ((data_not_zeros) && (data.length == 20 )) {
            mDataField.setText(Arrays.toString(data));
            Log.i(TAG,Arrays.toString(data));

            if ( (data.length > 19 )) {
                final ProgressBar mProgressTorqueInner = (ProgressBar) findViewById(R.id.circularProgressbar);
                Resources res = getResources();
                Drawable drawable = res.getDrawable(R.drawable.circ_torque_inner);
                final ProgressBar mProgressTorqueOuter = (ProgressBar) findViewById(R.id.circularProgressbar2);
                Resources res2 = getResources();
                Drawable drawable2 = res2.getDrawable(R.drawable.circ_torque_outer);
                final ProgressBar mProgressRegenInner = (ProgressBar) findViewById(R.id.circularrRegenBarInner);
                Resources resRegenInner = getResources();
                Drawable drawableRegenInner = resRegenInner.getDrawable(R.drawable.circ_regen_inner);
                final ProgressBar mProgressRegenOuter = (ProgressBar) findViewById(R.id.circularRegenBarOuter);
                Resources resRegenOuter = getResources();
                Drawable drawableRegenOuter = resRegenOuter.getDrawable(R.drawable.circ_regen_outer);
                final ProgressBar mProgressSecondaryInner = (ProgressBar) findViewById(R.id.circularProgressbarBG);
                Resources res3 = getResources();
                Drawable drawable3 = res3.getDrawable(R.drawable.circbackgr);
                final ProgressBar mProgressSecondaryOuter = (ProgressBar) findViewById(R.id.circularProgressbar2BG);
                Resources res4 = getResources();
                Drawable drawable4 = res4.getDrawable(R.drawable.circ2backgr);

                final ProgressBar SOC_PROG = (ProgressBar) findViewById(R.id.SOC_BAR);
                Resources res_SOC_PROG = getResources();
                Drawable drawable_SOC_PROG = res_SOC_PROG.getDrawable(R.drawable.progress_bar);

                final ProgressBar BAT_TEMP_PROG = (ProgressBar) findViewById(R.id.BATT_TEMP_BAR);
                Resources res_BAT_TEMP_PROG = getResources();
                Drawable drawable_BAT_TEMP_PROG = res_BAT_TEMP_PROG.getDrawable(R.drawable.progress_bar);

                final ProgressBar eMOT_TEMP_PROG = (ProgressBar) findViewById(R.id.eMOT_TEMP_BAR);
                Resources res_eMOT_TEMP_PROG = getResources();
                Drawable drawable_eMOT_TEMP_PROG = res_eMOT_TEMP_PROG.getDrawable(R.drawable.progress_bar);

                final ProgressBar INV_TEMP_PROG = (ProgressBar) findViewById(R.id.INV_TEMP_BAR);
                Resources res_INV_TEMP_PROG = getResources();
                Drawable drawable_INV_TEMP_PROG = res_INV_TEMP_PROG.getDrawable(R.drawable.progress_bar);

//                mProgressTorqueInner.setProgress(0);   // Main Progress
//                mProgressTorqueInner.setSecondaryProgress(100); // Secondary Progress
//                mProgressTorqueInner.setMax(400); // Maximum Progress
//                mProgressTorqueInner.setProgressDrawable(drawable);
//                mProgressTorqueInner.setMax(400); // Maximum Progress
                //int torque = (int)data.charAt(3);
                int char0 = data[0];
                int sign = 1;
                if (char0%2==1){
                    sign = -1;
                }//least sig. digit
                MAP = (char0/2)%8;


                BLEPointer = char0/16;
                if(BLEPointer==0) {
                    mConnectionState.setText("");//                    sendWholeFileThreadWithBreak();

                    int SOC_Okay2Torq = data[1] % 2;
                    int SysAvail_SOC = data[1] / 2;
                    int SDworking = data[2] % 2;
                    SOC = data[2]/2;
                    torq = ((4*data[3])+(data[4]/64))*sign;
                    hp = (16*(data[4]%64)+(data[5]/16))*sign;

                    battTemp_Okay = data[8]%2;
                    SysAvail_BattTemp = data[8]/2;
                    BMS_CellTmin = data[9];
                    Tmot_okay2Torq = data[10]%2;
                    SysAvail_MtrTemp = data[10]/2;
                    RMS_T_motor = data[11]*10;
                    SysAvail_InvTemp = data[12]/2;
                    maxIGBT = data[13]*10;
//                    int [] ar ={SOC_Okay2Torq,SysAvail_SOC,SDworking,SOC,torq,hp,battTemp_Okay,SysAvail_BattTemp,BMS_CellTmin,Tmot_okay2Torq,SysAvail_MtrTemp,RMS_T_motor,SysAvail_InvTemp,maxIGBT};
//                    String s = Arrays.toString(ar);
//                    Log.i(TAG,s);
                }
                else if(BLEPointer == 1){
                    int POST_Fault_LO = data[1]*256+data[2];
                    int POST_Fault_HI = data[3]*256+data[4];
                    int RUN_Fault_LO = data[5]*256+data[6];
                    int RUN_Fault_HI = data[7]*256+data[8];
                    int VCU_Faults1 = data[13];
//                    int [] ar = {POST_Fault_HI,POST_Fault_LO,RUN_Fault_HI,RUN_Fault_LO,VCU_Faults1};
//                    String s = Arrays.toString(ar);
//                    Log.i(TAG,s);

                }
                else if(BLEPointer == 2){
//                    readVersionFromServer();

//                    version.setText(String.format("Version Number : %d.%d.%d.%d.%d",data[1],data[2],data[3],data[4],data[5]));
                    String VersionToShow = String.format("Version Number : %d.%d.%d.%d.%d.%d.%d.%d.%d.%d.%d.%d.%d.%d.%d.%d.%d",data[1],data[2],data[3],data[4],data[5],data[6],data[7],data[8],data[9],data[10],data[11],data[12],data[13],data[14],data[15],data[16],data[17]) ;
//
                    Toast.makeText(DeviceControlActivity.this, VersionToShow, Toast.LENGTH_LONG).show();
//
//                    final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
//
//                    alertDialog.setTitle("VERSION INFORMATION");
//                    alertDialog.setMessage(VersionToShow);
//                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "DISMISS",
//                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "DISMISS",
//                            new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//
//                                    dialog.dismiss();
//
//                                }
//                            });
//
//                    alertDialog.show();
//
                }
                else if(BLEPointer == 3){
//                    readVersionFromServer();
//                    if (( data[1] + (data[2]*256) + (data[3] * 256 *256) ) == fileProgress + 1000) {
                    int tempFileProg  = data[1] + (data[2]*256) + (data[3] * 256 *256) ;
                    if  (tempFileProg == fileProgress ) {
                        sendDataChunk();
//                            send5DataChunks();
                    }else if  (tempFileProg >= fileSize ) {
                        fileTransferSuccessful = true ;
                        //                            send5DataChunks();
                    } else {
                            Toast.makeText(DeviceControlActivity.this, "data lost", Toast.LENGTH_SHORT).show();
//                            fileProgress = tempFileProg ;
//                            sendDataChunk();

                        }

                    if (fileSize !=0) {
                        fileProgressPercent = (int) (100 * (data[1] + (data[2] * 256) + (data[3] * 256 * 256)) / fileSize);
                    }
//                    if (fileTransferFailed) {
//                        initiateFileTransfer();
//                        send500DataChunks();
//                    }
                    PWRtxt.setText(Integer.toString(fileProgressPercent));
//                    TRQtxt.setText(Integer.toString(fileSize));
//                    }
//                    else {
//                        sendDataChunk();
//                    }
                }
                else if (BLEPointer == 4 ) {
//                    readVersionFromServer();
                    checkSumVCU = ( (data[1]*(256 * 256 * 256)) + (data[2]*(256 * 256)) + (data[3]*256 ) + (data[4]) ) ;
                    mConnectionState.setText(Integer.toString(checkSumVCU));
                    if(checkSumVCU == checkSumApp){
                        Toast.makeText(DeviceControlActivity.this, "file transfer complete with matching checksum", Toast.LENGTH_SHORT).show();

                    }else{
                        Toast.makeText(DeviceControlActivity.this, "checksum did not match", Toast.LENGTH_SHORT).show();
                        fileTransferFailed = true ;
//                        initiateFileTransfer();
//                        send500DataChunks();
                    }
                }
                else if(BLEPointer == 5 ){
//                    readVersionFromServer();
                    fileTransferFailed = true ;
//                    sendWholeFileThreadWithBreak();
//                    initiateFileTransfer();
//                    sendDataChunk();
                }

                else if(BLEPointer == 6 ){
//                    readVersionFromServer();
                        mConnectionState.setText("running in bootloader");//                    sendWholeFileThreadWithBreak();
//                    initiateFileTransfer();
//                    sendDataChunk();
                }

                /* old codes*/

                String Field1 = "";//separated[1].trim();
                String RPM = "";//separated[2].trim();

                int eMotor_RPM = 0;

                try {
                    eMotor_RPM = hp;

                } catch (NumberFormatException nfe) {
                    System.out.println("Could not parse " + nfe);
                }

                int TORQUE = 0;

                try {
                    TORQUE = torq;

                } catch (NumberFormatException nfe) {
                    System.out.println("Could not parse " + nfe);
                }

//                int horsepower = TORQUE * ( eMotor_RPM / 5252 ) ;
                int horsepower = eMotor_RPM;
                hp = horsepower;//for logging
                if ((TORQUE >= 0) && (TORQUE < 200)) {
                    chargingText.setVisibility(View.INVISIBLE);
                    if ((TORQUE > 150)) {
                        mProgressSecondaryInner.setProgress(320);   // Main Progress
                        mProgressSecondaryInner.setProgressDrawable(drawable3);
                        mProgressSecondaryOuter.setProgress(320);   // Main Progress
                        mProgressSecondaryOuter.setProgressDrawable(drawable4);
                    } else {

                        mProgressSecondaryInner.setProgress(240);   // Main Progress
                        mProgressSecondaryInner.setProgressDrawable(drawable3);
                        mProgressSecondaryOuter.setProgress(240);   // Main Progress
                        mProgressSecondaryOuter.setProgressDrawable(drawable4);
                    }


                    mProgressRegenInner.setProgress(0);   // Main Progress
                    mProgressRegenInner.setProgressDrawable(drawableRegenInner);
                    mProgressRegenOuter.setProgress(0);   // Main Progress
                    mProgressRegenOuter.setProgressDrawable(drawableRegenOuter);

                    mProgressTorqueInner.setProgress(horsepower * 16 / 10);   // Main Progress
                    mProgressTorqueInner.setProgressDrawable(drawable);
                    mProgressTorqueOuter.setProgress(TORQUE * 16 / 10);   // Main Progress
                    mProgressTorqueOuter.setProgressDrawable(drawable2);

                } else if ((TORQUE < 0) && (TORQUE > -200)) {
                    chargingText.setVisibility(View.VISIBLE);

                    if ((TORQUE < -150)) {
                        mProgressSecondaryInner.setProgress(320);   // Main Progress
                        mProgressSecondaryInner.setProgressDrawable(drawable3);
                        mProgressSecondaryOuter.setProgress(320);   // Main Progress
                        mProgressSecondaryOuter.setProgressDrawable(drawable4);
                    } else {

                        mProgressSecondaryInner.setProgress(240);   // Main Progress
                        mProgressSecondaryInner.setProgressDrawable(drawable3);
                        mProgressSecondaryOuter.setProgress(240);   // Main Progress
                        mProgressSecondaryOuter.setProgressDrawable(drawable4);
                    }


                    mProgressRegenInner.setProgress(-horsepower * 16 / 10);   // Main Progress
                    mProgressRegenInner.setProgressDrawable(drawableRegenInner);
                    mProgressRegenOuter.setProgress(-TORQUE * 16 / 10);   // Main Progress
                    mProgressRegenOuter.setProgressDrawable(drawableRegenOuter);

                    mProgressTorqueInner.setProgress(0);   // Main Progress
                    mProgressTorqueInner.setProgressDrawable(drawable);
                    mProgressTorqueOuter.setProgress(0);   // Main Progress
                    mProgressTorqueOuter.setProgressDrawable(drawable2);

                }

//End of torque and HP
                //String temporary = separated[0].trim();
                BattTemp = SysAvail_BattTemp; //battery temp
                eMotTemp = SysAvail_MtrTemp; //Motor temp
                inRecovery = SysAvail_InvTemp; //system avail.
                InvTemp = maxIGBT;

                try {
                    gen = SOC;


                } catch (NumberFormatException nfe) {
                    System.out.println("Could not parse " + nfe);
                }
                /*


                if ((SOC > 100) && (SOC < 181)) {
                    BattTemp = SOC - 100;
                }
                if ((SOC > 180) && (SOC < 190)) {
                    MAP = SOC - 180;
                }

                if ((SOC > 190) && (SOC < 193)) {
                    if ((SOC - 191) == 0) {
                        SD_Logging_Status.setVisibility(View.VISIBLE);
                    } else if ((SOC - 191) == 1) {
                        SD_Logging_Status.setVisibility(View.GONE);
                    }
                }

                if ((SOC > 200) && (SOC < 401)) {
                    eMotTemp = SOC - 200;
                }

                if ((SOC > 400) && (SOC < 501)) {
                    InvTemp = SOC - 400;
                }

                if ((SOC > 500) && (SOC < 503)) {
                    inRecovery = SOC - 501;
                }

                if ((SOC >= 100000) && (SOC < 200000)) {
                    if (SOC == 100000) {
                        Fault_Code_Status_POST_LO.setVisibility(View.GONE);
                    } else {
                        Fault_Code_Status_POST_LO.setVisibility(View.VISIBLE);
                        Fault_Code_Status_POST_LO.setText("POST FAULTS LOW : ");
                        Fault_Code_Status_POST_LO.append(String.valueOf(SOC - 100000));
                    }
                }
                if ((SOC >= 200000) && (SOC < 300000)) {

                    if (SOC == 200000) {
                        Fault_Code_Status_POST_HI.setVisibility(View.GONE);
                    } else {
                        Fault_Code_Status_POST_HI.setVisibility(View.VISIBLE);
                        Fault_Code_Status_POST_HI.setText("POST FAULTS HIGH : ");
                        Fault_Code_Status_POST_HI.append(String.valueOf(SOC - 200000));
                    }

                }
                if ((SOC >= 300000) && (SOC < 400000)) {


                    if (SOC == 300000) {
                        Fault_Code_Status_RUN_LO.setVisibility(View.GONE);
                    } else {
                        Fault_Code_Status_RUN_LO.setVisibility(View.VISIBLE);
                        Fault_Code_Status_RUN_LO.setText("RUN FAULTS LOW : ");
                        Fault_Code_Status_RUN_LO.append(String.valueOf(SOC - 300000));
                    }
                }
                if ((SOC >= 400000) && (SOC < 500000)) {

                    if (SOC == 400000) {
                        Fault_Code_Status_RUN_HI.setVisibility(View.GONE);
                    } else {
                        Fault_Code_Status_RUN_HI.setVisibility(View.VISIBLE);
                        Fault_Code_Status_RUN_HI.setText("RUN FAULTS HIGH : ");
                        Fault_Code_Status_RUN_HI.append(String.valueOf(SOC - 400000));
                    }

                }
                */
//
//                Ranges, for 0-100% of bar's range:
//                SOC: 20%-100%
//                eMotor Temp: 150-100C
//                Batt Temp: 50-30C
//                Inverter Temp: 95-60C
//
                if (SOC == 100) {
                    SOC_PROG.setProgress(100);
                    SOC_PROG.setSecondaryProgress(0);
                    SOC_PROG.setProgressDrawable(drawable_SOC_PROG);
                    if (showValues) {
                        SOCtxt.setText("BATTERY CHARGE : ");
                        SOCtxt.append(String.valueOf(SOC));
                        SOCtxt.append("%");
                    }
                } else if (SOC < 100) {
//                    SOC_PROG.setProgress(0);
//                    SOC_PROG.setSecondaryProgress((SOC - 20) * 10 / 8);
                    SOC_PROG.setProgress((SOC - 20) * 10 / 8);
                    SOC_PROG.setProgressDrawable(drawable_SOC_PROG);
                    if (showValues) {
                        SOCtxt.setText("BATTERY CHARGE : ");
                        SOCtxt.append(String.valueOf(SOC));
                        SOCtxt.append("%");
                    }

                }
                if (eMotTemp < 100) {
                    eMOT_TEMP_PROG.setProgress(100);
                    eMOT_TEMP_PROG.setSecondaryProgress(0);
                    eMOT_TEMP_PROG.setProgressDrawable(drawable_eMOT_TEMP_PROG);
                } else {
//                    eMOT_TEMP_PROG.setProgress(0);
//                    eMOT_TEMP_PROG.setSecondaryProgress(100 - ((eMotTemp - 100) * 2));
                    eMOT_TEMP_PROG.setProgress(100 - ((eMotTemp - 100) * 100 / 55));
                    eMOT_TEMP_PROG.setProgressDrawable(drawable_eMOT_TEMP_PROG);
                }
                if (BattTemp < 30) {
                    BAT_TEMP_PROG.setProgress(100);
                    BAT_TEMP_PROG.setSecondaryProgress(0);
                    BAT_TEMP_PROG.setProgressDrawable(drawable_BAT_TEMP_PROG);
                } else {
//                    BAT_TEMP_PROG.setProgress(0);
//                    BAT_TEMP_PROG.setSecondaryProgress(100 - ((BattTemp - 30) * 5));
                    BAT_TEMP_PROG.setProgress(100 - ((BattTemp - 30) * 10 / 3));
                    BAT_TEMP_PROG.setProgressDrawable(drawable_BAT_TEMP_PROG);
                }
                if (inRecovery == 1) {
                    INV_TEMP_PROG.setProgress(0);
                    INV_TEMP_PROG.setSecondaryProgress(InvTemp);
                    INV_TEMP_PROG.setProgressDrawable(drawable_INV_TEMP_PROG);
                } else if (inRecovery == 0) {
//                    INV_TEMP_PROG.setProgress(0);
                    INV_TEMP_PROG.setSecondaryProgress(0);
                    INV_TEMP_PROG.setProgress(InvTemp);
                    INV_TEMP_PROG.setProgressDrawable(drawable_INV_TEMP_PROG);
                }


                if (showValues) {
                    BATTEMPtxt.setText("BATTERY TEMPERATURE : ");
                    BATTEMPtxt.append(String.valueOf(BattTemp));
                    BATTEMPtxt.append(" C");
                    TMPtxt.setText("eMOTOR TEMP : ");
                    TMPtxt.append(String.valueOf(eMotTemp));
                    TMPtxt.append(" C");
                    IGBTtxt.setText("SYSTEM AVAILABILITY : ");
                    IGBTtxt.append(String.valueOf(InvTemp));
                    IGBTtxt.append(" %");

                } else {
                    SOCtxt.setText("BATTERY CHARGE");
                    BATTEMPtxt.setText("BATTERY TEMPERATURE");
                    TMPtxt.setText("eMOTOR TEMP");
                    IGBTtxt.setText("SYSTEM AVAILABILITY");

                }

                if ((SOC == 0) && (eMotTemp == 0) && (BattTemp == 0) && (InvTemp == 0)) {
                    SOC_PROG.setProgress(0);
                    SOC_PROG.setSecondaryProgress(0);
                    SOC_PROG.setProgressDrawable(drawable_SOC_PROG);
                    eMOT_TEMP_PROG.setProgress(0);
                    eMOT_TEMP_PROG.setSecondaryProgress(0);
                    eMOT_TEMP_PROG.setProgressDrawable(drawable_eMOT_TEMP_PROG);
                    BAT_TEMP_PROG.setProgress(0);
                    BAT_TEMP_PROG.setSecondaryProgress(0);
                    BAT_TEMP_PROG.setProgressDrawable(drawable_BAT_TEMP_PROG);
                    INV_TEMP_PROG.setProgress(0);
                    INV_TEMP_PROG.setSecondaryProgress(0);
                    INV_TEMP_PROG.setProgressDrawable(drawable_INV_TEMP_PROG);
                }

                Field1 = Field1.replaceAll("-", "");
                RPM = RPM.replaceAll("-", "");
                TRQtxt.setText(Integer.toString(TORQUE));
                PWRtxt.setText(Integer.toString(hp));
                Log.i("Device Control Activity", "parsed int TORQUE is " + TORQUE);

                torq = TORQUE;//for logging
//                SOCtxt.setText(separated[0].trim() + " %");
//                if (TORQUE < 0 ){
//                    TORQUE = -1 * TORQUE ;
//                    horsepower = -1 * horsepower ;
//                TRQtxt.setText((TORQUE));
//                PWRtxt.setText(horsepower);
//                } else {
//                    TRQtxt.setText(TORQUE);
//                    PWRtxt.setText(horsepower);
//                }
//                BATTEMPtxt.setText("Batt Max Temp : " + separated[2].trim());
//                IGBTtxt.setText("Inverter Temp : " + separated[5].trim());
//                TMPtxt.setText("E-MOTOR TEMP : " + separated[3].trim());
//                String MapPos = separated[4].trim();
//

                if ((BLEPointer == 3 )){
                    PWRtxt.setText(Integer.toString(fileProgressPercent));

                    mProgressTorqueInner.setProgress(fileProgressPercent );   // Main Progress
                    mProgressTorqueInner.setProgressDrawable(drawable);

//                    final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
//
                }
                if (MAP == 0) {
                    Off.setBackgroundResource(R.drawable.button_illuminated);

                    Street.setBackgroundResource(R.drawable.button_not_selected);
                    Track.setBackgroundResource(R.drawable.button_not_selected);
                    Overboost.setBackgroundResource(R.drawable.button_not_selected);

                    Off.setTextColor(0xFF000000);
                    Street.setTextColor(0xFFCACACA);
                    Track.setTextColor(0xFFCACACA);
                    Overboost.setTextColor(0xFFCACACA);

                } else if (MAP == 1) {


                    Off.setBackgroundResource(R.drawable.button_not_selected);
                    Track.setBackgroundResource(R.drawable.button_not_selected);
                    Overboost.setBackgroundResource(R.drawable.button_not_selected);

                    Street.setBackgroundResource(R.drawable.button_illuminated);

                    Off.setTextColor(0xFFCACACA);
                    Street.setTextColor(0xFF000000);
                    Track.setTextColor(0xFFCACACA);
                    Overboost.setTextColor(0xFFCACACA);

                } else if (MAP == 2) {

                    Off.setBackgroundResource(R.drawable.button_not_selected);
                    Street.setBackgroundResource(R.drawable.button_not_selected);
                    Overboost.setBackgroundResource(R.drawable.button_not_selected);
                    Track.setBackgroundResource(R.drawable.button_illuminated);

                    Off.setTextColor(0xFFCACACA);
                    Street.setTextColor(0xFFCACACA);
                    Track.setTextColor(0xFF000000);
                    Overboost.setTextColor(0xFFCACACA);
                } else if (MAP == 3) {

                    Off.setBackgroundResource(R.drawable.button_not_selected);
                    Street.setBackgroundResource(R.drawable.button_not_selected);
                    Track.setBackgroundResource(R.drawable.button_not_selected);
                    Overboost.setBackgroundResource(R.drawable.button_illuminated);

                    Off.setTextColor(0xFFCACACA);
                    Street.setTextColor(0xFFCACACA);
                    Track.setTextColor(0xFFCACACA);
                    Overboost.setTextColor(0xFF000000);
                }

            }
        }
    }


    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            // If the service exists for HM 10 Serial, say so.
            if (SampleGattAttributes.lookup(uuid, unknownServiceString) == "HM 10 Serial") {
                isSerial.setText("Serial Service found");
                serialServiceFound = true ;

            } else {
                isSerial.setText("No serial Service");
                serialServiceFound = false ;

            }
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            // get characteristic when UUID matches RX/TX UUID
            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_TX);
//            characteristicTX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
            characteristicRX = gattService.getCharacteristic(BluetoothLeService.UUID_HM_RX_TX);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void readSeek(SeekBar seekBar, final int pos) {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                RGBFrame[pos] = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //Toast.makeText(DeviceControlActivity.this, "test", Toast.LENGTH_SHORT).show();
                // TODO Auto-generated method stub
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // TODO Auto-generated method stub
                makeChange();
            }
        });
    }
    //for logging kml

    class LogData{
        String time;
        Double lat,lon;
        int f,s,t;
        public LogData(String time, Double lat,Double lon, int gen, int hp, int torq) {
            this.time = time;
            this.lat=lat;
            this.lon=lon;
            this.f=gen;
            this.s=hp;
            this.t=torq;
        }
        public String getTime(){
            return this.time;
        }
        public String toString(){
            String fmt=
                    "   <Placemark>\n"+
                            "       <TimeStamp>\n"+
                            "           <when>%s</when>\n"+
                            "       </TimeStamp>\n"+
                            "       <ExtendedData>\n"+
                            "		    <Data name='gen'>\n"+
                            "			    <value>%d</value>\n"+
                            "		    </Data\n>"+
                            "		    <Data name='HP'>\n"+
                            "			    <value>%d</value>\n"+
                            "   		</Data>\n"+
                            "	    	<Data name='Torque'>\n"+
                            "		    	<value>%d</value>\n"+
                            "		    </Data>\n"+
                            "	    </ExtendedData>\n"+
                            "       <styleUrl>#seeadler-dot-icon</styleUrl>\n"+
                            "       <Point>\n"+
                            "           <coordinates>%f,%f</coordinates>\n"+
                            "       </Point>\n"+
                            "   </Placemark>\n";
            return String.format(fmt,this.time,this.f,this.s,this.t,this.lon,this.lat);

        }
    }

    // on change of bars write char
    private void makeChange() {
        String str = RGBFrame[0] + "";
        Log.d(TAG, "Sending result=" + str);
        final byte[] tx = str.getBytes();
        if (mConnected) {
            characteristicTX.setValue(tx);
            mBluetoothLeService.writeCharacteristic(characteristicTX);
            mBluetoothLeService.setCharacteristicNotification(characteristicRX, true);
        }
    }
    private void writeKML(){
        int length = logFile.size();
        String header = "<?xml version='1.0' encoding='UTF-8'?>\n"+
                "<kml xmlns='http://www.opengis.net/kml/2.2'>\n"+
                "  <Document>\n";
        String footer = "  </Document>\n"+
                "</kml>";
        if (length>0){
            String title = logFile.get(0).getTime().replace("-","").replace(":","_")+".kml";
            FileOutputStream fos = null;
            File file = new File(getExternalFilesDir(null),title);
            try {
                file.createNewFile();
                fos = new FileOutputStream(file,true);
                fos.write(header.getBytes());
                for (LogData l: logFile){
                    fos.write(l.toString().getBytes());
                }
                fos.write(footer.getBytes());
                Log.i(TAG,getExternalFilesDir(null)+"/"+title);
                Toast.makeText(this,"Saved to "+getExternalFilesDir(null)+"/"+title,Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally{
                if(fos!=null){
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        resetLog();
    }
    private void resetLog(){
        logFile.clear();
    }
}