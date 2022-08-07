package com.jasonfoglia.universalrobotcontroller;

/*
 * Copyright (C) 2009 The Android Open Source Project
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

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * This class does all the work for setting up and managing Bluetooth
 * connections with other devices. It has a thread that listens for incoming
 * connections, a thread for connecting with a device, and a thread for
 * performing data transmissions when connected.
 */
class BluetoothService {
    // Debugging

    private static final String TAG = "BluetoothService_BoeBot";
    private static final boolean D = true;
    // Name for the SDP record when creating server socket
    private static final String NAME_SECURE = "BluetoothSecure";
    private static final String NAME_INSECURE = "BluetoothInsecure";
    // Unique UUID for this application
    private static final UUID MY_UUID_SECURE = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID MY_UUID_INSECURE = UUID
            .fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Member fields
    private final BluetoothAdapter mAdapter;
    private final Handler mHandler;
    private static Context ctx;
    private AcceptThread mSecureAcceptThread;
    private AcceptThread mInsecureAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;

    // Constants that indicate the current connection state
    static final int STATE_NONE = 0; // we're doing nothing
    static final int STATE_LISTEN = 1; // now listening for incoming connections
    static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    static final int STATE_CONNECTED = 3; // now connected to a remote
    static final int STATE_DISCONNECTED = 4;

    /**
     * Constructor. Prepares a new BluetoothChat session.
     *
     * @param handler
     *            A Handler to send messages back to the UI Activity
     */
    BluetoothService(Handler handler, Context contextForResource) {
        ctx = contextForResource;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mHandler = handler;
    }

    /**
     * Set the current state of the chat connection
     *
     * @param state
     *            An integer defining the current connection state
     */
    private synchronized void setState(int state) {
        mState = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1)
                .sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *  @param device
     *            The BluetoothDevice to connect
     *
     */
    synchronized void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {

            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        try {
            // Start the thread to connect with the given device
            mConnectThread = new ConnectThread(device, true);
            mConnectThread.start();
            setState(STATE_CONNECTING);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket
     *            The BluetoothSocket on which the connection was made
     * @param device
     *            The BluetoothDevice that has been connected
     */
    private synchronized void connected(BluetoothSocket socket, BluetoothDevice device, final String socketType) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one
        // device
        if (mSecureAcceptThread != null) {
            mSecureAcceptThread.cancel();
            mSecureAcceptThread = null;
        }
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket, socketType);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        }
        msg.setData(bundle);

        msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Device connected successfully");
        msg.setData(bundle);

        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Write to the ConnectedThread in an un-synchronized manner
     *
     * @param out
     *            The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                return;
            }
            r = mConnectedThread;
        }
        r.write(out);
        // Perform the write un-synchronized
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, ctx.getString(R.string.unableToConnect));
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_DISCONNECTED);
        // Start the service over to restart listening mode
        //BluetoothService.this.start();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, ctx.getString(R.string.connectionLost));
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_DISCONNECTED);
        // Start the service over to restart listening mode
        //BluetoothService.this.start();
    }

    /**
     * This thread runs while listening for incoming connections. It behaves
     * like a server-side client. It runs until a connection is accepted (or
     * until cancelled).
     */
    private class AcceptThread extends Thread {
        // The local server socket

        private BluetoothServerSocket mmServerSocket = null;
        private String mSocketType;

        AcceptThread(boolean secure) {

            BluetoothServerSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                // Create a new listening server socket
                try {
                    if (secure) {
                        tmp = mAdapter.listenUsingRfcommWithServiceRecord(
                                NAME_SECURE, MY_UUID_SECURE);
                    } else {
                        // tmp =
                        // mAdapter.listenUsingInsecureRfcommWithServiceRecord(
                        // NAME_INSECURE, MY_UUID_INSECURE);
                        tmp = mAdapter.listenUsingRfcommWithServiceRecord(
                                NAME_INSECURE, MY_UUID_INSECURE);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "listen() failed", e);
                }
                mmServerSocket = tmp;
            }
        }

        @Override
        public void run() {
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket;

            // Listen to the server socket if we're not connected
            while (mState != STATE_CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType
                            + "accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BluetoothService.this) {
                        switch (mState) {
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice(),
                                        mSocketType);
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate
                                // new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }

        }

        void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Socket Type" + mSocketType
                        + "close() of server failed", e);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection with a
     * device. It runs straight through; the connection either succeeds or
     * fails.
     */
    private class ConnectThread extends Thread {

        private BluetoothSocket mmSocket = null;
        private final BluetoothDevice mmDevice;
        private final String mSocketType;

        ConnectThread(BluetoothDevice device, boolean secure) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            mSocketType = secure ? "Secure" : "Insecure";

            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {

                // Get a BluetoothSocket for a connection with the
                // given BluetoothDevice
                try {
                    if (secure) {
                        tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                    } else {
                        tmp = device.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Socket Type: " + mSocketType + "create() failed", e);
                }
                mmSocket = tmp;
            }
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread SocketType:" + mSocketType);
            setName("ConnectThread" + mSocketType);

            // Always cancel discovery because it will slow down a connection
            if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                mAdapter.cancelDiscovery();

                // Make a connection to the BluetoothSocket
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmSocket.connect();
                } catch (IOException e) {
                    // Close the socket
                    try {
                        mmSocket.close();
                    } catch (IOException e2) {
                        Log.e(TAG, "unable to close() " + mSocketType
                                + " socket during connection failure", e2);
                    }
                    connectionFailed();
                    return;
                }
            }

            // Reset the ConnectThread because we're done
            synchronized (BluetoothService.this) {
                mConnectThread = null;
            }

            try {
                // Start the connected thread
                connected(mmSocket, mmDevice, mSocketType);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect " + mSocketType
                        + " socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device. It handles all
     * incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {

        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket, String socketType) {
            Log.d(TAG, "create ConnectedThread: " + socketType);
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1,
                            buffer).sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer
         *            The bytes to write
         */
        void write(byte[] buffer) {
            try {
                Log.v(TAG, buffer[0] + "");
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}

