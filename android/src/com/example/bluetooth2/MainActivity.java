/**
 *  @version 1.1 (28.01.2013)
 *  http://english.cxem.net/arduino/arduino5.php
 *  @author Koltykov A.V. (�������� �.�.)
 * 
 */

package com.example.bluetooth2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
 
public class MainActivity extends Activity {
  private static final String TAG = "bluetooth2";
  

  
  private NumberPicker shiftPicker;
  private NumberPicker exposurePicker;
  private NumberPicker stepsPicker;
  private NumberPicker pausePicker;
  
  private Button applybtn;
  
  
  Button moveToLeft, moveToRight;
  TextView txtArduino;
  Handler h;
   
  final int RECIEVE_MESSAGE = 1;		// Status  for Handler
  private BluetoothAdapter btAdapter = null;
  private BluetoothSocket btSocket = null;
  private StringBuilder sb = new StringBuilder();
  
  private ConnectedThread mConnectedThread;
   
  // SPP UUID service
  private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
 
  // MAC-address of Bluetooth module (you must edit this line)
//  private static String address = "00:15:FF:F2:19:5F";
		//20-14-04-14-10-45
  private static String address = "20:14:04:14:10:45";
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
 
    setContentView(R.layout.activity_main);
	Log.i(TAG, "String:"  );

    h = new Handler() {
    	public void handleMessage(android.os.Message msg) {
    		switch (msg.what) {
            case RECIEVE_MESSAGE:													// if receive massage
            	byte[] readBuf = (byte[]) msg.obj;
            	String strIncom = new String(readBuf, 0, msg.arg1);					// create string from bytes array
            	sb.append(strIncom);												// append string
            	int endOfLineIndex = sb.indexOf("\r\n");							// determine the end-of-line
            	if (endOfLineIndex > 0) { 											// if end-of-line,
            		String sbprint = sb.substring(0, endOfLineIndex);				// extract string
                    sb.delete(0, sb.length());										// and clear
                	
                	if(!sbprint.equals("flush\r\n") || !sbprint.equals("steps\r\n")){
//                	Log.d(TAG, "String:"+ sbprint+" "+bytesToHex(readBuf) );
                	}else{
                	
                	}
                	Log.i(TAG, "String:"+ sbprint+" "+(int)readBuf[3] );
                }
            	break;
    		}
        };
	};
     
    btAdapter = BluetoothAdapter.getDefaultAdapter();		// get Bluetooth adapter
    checkBTState();
    
    
    moveToLeft = (Button) findViewById(R.id.moveToLeft);					// button LED ON
    moveToRight = (Button) findViewById(R.id.moveToRight);				// button LED OFF
    //txtArduino = (TextView) findViewById(R.id.txtArduino);		// for display the received data from the Arduino
    
    shiftPicker = (NumberPicker) findViewById(R.id.shiftPicker);
    exposurePicker = (NumberPicker) findViewById(R.id.exposurePicker);
    stepsPicker = (NumberPicker) findViewById(R.id.stepsPicker);
    pausePicker = (NumberPicker) findViewById(R.id.pausePicker);
    
    shiftPicker.setMaxValue(126);
    exposurePicker.setMaxValue(126);
    stepsPicker.setMaxValue(126);
    pausePicker.setMaxValue(126);
    
    shiftPicker.setValue(2);
    exposurePicker.setValue(20);
    stepsPicker.setValue(30);
    pausePicker.setValue(1);    
    


    moveToLeft.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        //Toast.makeText(getBaseContext(), "Turn on LED", Toast.LENGTH_SHORT).show();
		byte[] config = {(byte) 5, (byte) 255};
    	mConnectedThread.write(config);	// Send "0" via Bluetooth
    	Log.i(TAG, "moveToLeft" );
     }
    });
 
    moveToRight.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
  		byte[] config = {(byte) 5, (byte) 0};
    	mConnectedThread.write(config);	// Send "0" via Bluetooth
    	Log.i(TAG, "moveToRight" );
      }
    });
    
    
    
    applybtn = (Button) findViewById(R.id.applay);

    applybtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				
				byte[] config = {
						(byte) 1, (byte) shiftPicker.getValue(),
						(byte) 2, (byte) exposurePicker.getValue(),
						(byte) 3, (byte) pausePicker.getValue(),
						(byte) 6, (byte) stepsPicker.getValue()
				};
		    	mConnectedThread.write(config);	// Send "0" via Bluetooth
		    	Log.i(TAG, "shift "+shiftPicker.getValue()+" exposure "+exposurePicker.getValue()+" pause "+pausePicker.getValue()+" steps "+stepsPicker.getValue() );
			}
     });

      
    
  }
  
  final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();
  public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    for ( int j = 0; j < bytes.length; j++ ) {
	        int v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
  
  public static int unsignedToBytes(byte b) {
	    return b & 0xFF;
	  }
  
  private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
      if(Build.VERSION.SDK_INT >= 10){
          try {
              final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
              return (BluetoothSocket) m.invoke(device, MY_UUID);
          } catch (Exception e) {
              Log.e(TAG, "Could not create Insecure RFComm Connection",e);
          }
      }
      return  device.createRfcommSocketToServiceRecord(MY_UUID);
  }
   
  @Override
  public void onResume() {
    super.onResume();
 
    Log.d(TAG, "...onResume - try connect...");
   
    // Set up a pointer to the remote node using it's address.
    BluetoothDevice device = btAdapter.getRemoteDevice(address);
   
    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
    
	try {
		btSocket = createBluetoothSocket(device);
	} catch (IOException e) {
		errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
	}
    
    /*try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
    }*/
   
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
   
    // Establish the connection.  This will block until it connects.
    Log.d(TAG, "...Connecting...");
    try {
      btSocket.connect();
      Log.d(TAG, "....Connection ok...");
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
     
    // Create a data stream so we can talk to server.
    Log.d(TAG, "...Create Socket...");
   
    mConnectedThread = new ConnectedThread(btSocket);
    mConnectedThread.start();
  }
 
  @Override
  public void onPause() {
    super.onPause();
 
    Log.d(TAG, "...In onPause()...");
  
    try     {
      btSocket.close();
    } catch (IOException e2) {
      errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
    }
  }
   
  private void checkBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on
    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) { 
      errorExit("Fatal Error", "Bluetooth not support");
    } else {
      if (btAdapter.isEnabled()) {
        Log.d(TAG, "...Bluetooth ON...");
      } else {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
      }
    }
  }
 
  private void errorExit(String title, String message){
    Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
    finish();
  }
 
  private class ConnectedThread extends Thread {
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer = new byte[256];  // buffer store for the stream
	        int bytes; // bytes returned from read()

	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	        	try {
	                // Read from the InputStream
	                bytes = mmInStream.read(buffer);		// Get number of bytes and message in "buffer"
                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();		// Send to message queue Handler
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] msgBuffer ) {
	    	try {
	            mmOutStream.write(msgBuffer);
	        } catch (IOException e) {
	            Log.d(TAG, "...Error data send: " + e.getMessage() + "...");     
	          }
	    }
	}
}