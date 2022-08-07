package com.jasonfoglia.universalrobotcontroller;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.RelativeLayout;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends LicenseCheckActivity {

    private static final int REQUEST_CONNECT_DEVICE = 1;

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter;
    // Member object for the chat services
    private BluetoothService mService;


    private final String TAG = "UniversalRobot";
    private CoordinatorLayout mainActivity;
    private RelativeLayout relativeLayout;
    private Button joyStickBtn;
    private Intent serverIntent;
    private FloatingActionButton blueToothBtn;

    private void setUpBluetoothConnection() {
        try {
            // Initialize the BluetoothService to perform bluetooth connections
            mService = new BluetoothService(mHandler, this.getBaseContext());
            Notice("Bluetooth is on!");// Initialize the buffer for outgoing
            // messages
            mOutStringBuffer = new StringBuffer();

        } catch (Exception e) {
            Log.e(TAG, e.getMessage() + " - " + e.getLocalizedMessage() + " - "
                    + e);
        }
    }

    private void sendMessage(String message) {
        // Check that there's actually something to send
        if (message.length() > 0) {
            message = "<" + message + ">";
            // Check that we're actually connected before trying anything
            if (mService.getState() != BluetoothService.STATE_CONNECTED) {
                Notice(getString(R.string.deviceNotConnected));
                return;
            }
            //tv.setText("");
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mService.write(send);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    private void connectDevice(Intent data) {
        try {
            // Get the device MAC address
            String address = data.getExtras().getString(
                    DeviceListActivity.EXTRA_DEVICE_ADDRESS);
            // Get the BLuetoothDevice object
            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            // Attempt to connect to the device
            mService.connect(device);
            // The Handler that gets information back from the BluetoothService
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    // This handler is pushed into BluetoothService and handles all events in the class
    // This is nice because it's different then callback method parameters
    private final Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            blueToothBtn.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.button_bluetooth_connected));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            blueToothBtn.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.button_bluetooth_connecting));
                            Notice(getString(R.string.connecting));
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            break;
                        case BluetoothService.STATE_DISCONNECTED:
                            blueToothBtn.setBackgroundTintList(ContextCompat.getColorStateList(MainActivity.this, R.color.button_bluetooth_disconnected));
                            break;
                    }
                    break;
                case MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    //String writeMessage = new String(writeBuf);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //outputterm.append(readMessage + "\n");
                    break;
                case MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    String mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    break;
                case MESSAGE_TOAST:
                    Log.e(TAG, "MESSAGE_TOAST : " + msg);
                    Notice(msg.getData().getString(TOAST));
                    break;
            }
        }
    };

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityResultLauncher<Intent> someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                            connectDevice(data);
                            Notice("ActivityResultCallback");
                        }
                    }
                });

        mainActivity = findViewById(R.id.coordinator_layout);
        relativeLayout = findViewById(R.id.frameLayout);
        joyStickBtn = findViewById(R.id.joy_stick_btn);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If BT is not on, request that it be enabled.
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {

            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            someActivityResultLauncher.launch(enableIntent);

            Notice("Looks like we started our bluetooth!");
        } else if (mService == null) {
            setUpBluetoothConnection();
        }

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            try {
                // Launch the DeviceListActivity to see devices and do scan
                sendMessage("-100:-100");
                Notice("sendMessage -100:-100");
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        });

        blueToothBtn = findViewById(R.id.bluetoothBtn);
        blueToothBtn.setOnClickListener(view -> {
            try {
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                someActivityResultLauncher.launch(serverIntent);
            } catch (Exception e) {
                Log.d(TAG, e.getMessage());
            }
        });

        mainActivity.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(joyStickBtn.getWidth(), joyStickBtn.getHeight());
                params.leftMargin = relativeLayout.getWidth()/2;
                params.topMargin = relativeLayout.getWidth()/2;
                joyStickBtn.setLayoutParams(params);
                mainActivity.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        joyStickBtn.setOnTouchListener((v, me) -> {

            int left = (int) (me.getRawX() - (v.getWidth() / 2));
            int top = (int) (me.getRawY() + (relativeLayout.getTop()/2) - v.getHeight());
            int X = (left - (relativeLayout.getWidth() /2));
            int Y = (top - (relativeLayout.getHeight() /2));

            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(v.getWidth(), v.getHeight());
            if (me.getAction() == MotionEvent.ACTION_MOVE){
                int topMargin = (relativeLayout.getTop()) + (relativeLayout.getTop()/2);
                boolean insideBounds =
                        (Math.pow(me.getRawX() - (relativeLayout.getWidth()/2 + relativeLayout.getLeft()), 2) +
                         Math.pow(me.getRawY() - (relativeLayout.getHeight()/2 + topMargin), 2)
                        <= Math.pow(relativeLayout.getWidth()/2, 2));

                if (insideBounds) {
                    int max = relativeLayout.getWidth() / 2;

                    sendMessage(Percentage(X, max) + ":" + Percentage(Y, max));

                    params.leftMargin = left;
                    params.topMargin = top;
                    v.setLayoutParams(params);
                }
            }
            if(me.getAction() == MotionEvent.ACTION_UP){
                params.leftMargin = relativeLayout.getWidth()/2;
                params.topMargin = relativeLayout.getWidth()/2;

                // Send message to Arduino to stop movement

                sendMessage("0:0");

                v.setLayoutParams(params);
            }
            return true;
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void Notice(String msg) {
        Snackbar.make(mainActivity, msg, Snackbar.LENGTH_LONG).setAction("Action", null).show();
        //Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
    }

    private int Percentage(float min, float max){
        return Math.round((min / max) * 100);
    }
}