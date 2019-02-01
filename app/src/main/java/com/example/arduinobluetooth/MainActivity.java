package com.example.arduinobluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {
    private UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothSocket btSocket = null;
    static Handler mHandler;
    String value;
    private StringBuilder sb = new StringBuilder();
    final int RECEIVE_MESSAGE = 1;

    private static final String TAG = "Game Over";

    @Override
    public void onBackPressed() {
        finish();
        System.exit(0);
    }

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        final Button button = findViewById(R.id.button);
        final ImageView imageView = findViewById(R.id.imageView);

        checkBt();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AsyncTaskEx().execute();
            }
        });

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECEIVE_MESSAGE:                                           // if receive massage
                        byte[] readBuf = (byte[]) msg.obj;
                        String strIncom = new String(readBuf, 0, msg.arg1);  // create string from bytes array
                        sb.append(strIncom);                                        // append string
                        int endOfLineIndex = sb.indexOf("\r\n");                    // determine the end-of-line
                        if (endOfLineIndex > 0) {                                   // if end-of-line,
                            String sbprint = sb.substring(0, endOfLineIndex);       // extract string
                            sb.delete(0, sb.length());                              // and clear
                            value = sbprint;

                            button.setOnClickListener(new View.OnClickListener() {
                                  @Override
                                  public void onClick(View v) {
                                      Resources res = getResources();
                                      int val;
                                      try{
                                          val = Integer.parseInt(value);
                                      }
                                      catch (NumberFormatException e){
                                          val = 0;
                                      }
                                      switch (val) {
                                          case 1:
                                              imageView.setImageDrawable(res.getDrawable(R.drawable.image1));
                                              break;
                                          case 2:
                                              imageView.setImageDrawable(res.getDrawable(R.drawable.image2));
                                              break;
                                          case 3:
                                              imageView.setImageDrawable(res.getDrawable(R.drawable.image3));
                                              break;
                                      }
                                  }
                              }
                            );
                        }
                        //Log.d(TAG, "...String:"+ sb.toString() +  "Byte:" + msg.arg1 + "...");
                        break;
                }
            }
        };
    }

    public void checkBt(){
        if (mBluetoothAdapter == null) {
            Toast.makeText(getBaseContext(), "BT Not Supported", Toast.LENGTH_LONG).show();
            finish();
        }
        else if (!mBluetoothAdapter.isEnabled()) {
            int REQUEST_ENABLE_BT = 1;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, MY_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    private class ConnectedThread extends Thread {
        private InputStream mmInStream;
        //private OutputStream mmOutStream;

        private ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            //OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                //tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Exception in getting Input Stream", e);
            }

            mmInStream = tmpIn;
            //mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            while (true) {
                try {
                    // Read from the InputStream

                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    mHandler.obtainMessage(RECEIVE_MESSAGE, bytes, -1, buffer).sendToTarget();        // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }
    }

    private void errorExit(String message) {
        Toast.makeText(getBaseContext(), "Fatal Error" + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    @SuppressLint("StaticFieldLeak")
    private class AsyncTaskEx extends AsyncTask<Void, Void, Void>{
        private String address = "98:D3:32:30:5A:FA";
        private ConnectedThread mConnectedThread;
        Button button = findViewById(R.id.button);

        @SuppressLint("SetTextI18n")
        @Override
        protected Void doInBackground(Void... arg0) {
            button.setText("Connecting...");

            Log.d(TAG, "...onResume - try connect...");

            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);

            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                errorExit("In onResume() and socket create failed: " + e.getMessage() + ".");
            }

            Log.d(TAG, "...Connecting...");
            try {
                btSocket.connect();
                Log.d(TAG, "....Connection ok...");
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    errorExit("In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            }

            Log.d(TAG, "...Create Socket...");

            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();

            return null;
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPostExecute(Void result) {
            button.setText("Show Image");
            button.setTextColor(Color.BLACK);
            button.setEnabled(true);
        }

        @SuppressLint("SetTextI18n")
        @Override
        protected void onPreExecute() {
            button.setText("Connecting...");
            button.setTextColor(Color.GRAY);
            button.setEnabled(false);
        }
    }
}