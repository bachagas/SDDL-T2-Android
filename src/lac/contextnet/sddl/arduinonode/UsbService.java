package lac.contextnet.sddl.arduinonode;

import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

//USB Android library
import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

public class UsbService extends Service {

	private static Handler serialHandler;
	public static void setUsbHandler(Handler handler) {
		serialHandler = handler;
	}

	//Serial write thread control
	private volatile Boolean keepRunning;
	private volatile Boolean isConnected;
	private Thread t;
	
	private LocalBroadcastManager broadcastManager;
	private Bundle extras;
	private ArrayList<String> msgLst = new ArrayList<String>();
	
	/* USB control */
    private UsbSerialDriver mSerialDevice; //the device currently in use, or {@code null}
    private UsbManager mUsbManager; //the system's USB service
    private static final int USB_BAUD_RATE = 9600;
	private static final int USB_TIMEOUT = 3000;
	private static final String TAG = UsbService.class.getSimpleName();
	private volatile String mBuffer = "";
	private String mDeviceDescription = "";
    
    private void addToBuffer(String text) {
        synchronized(mBuffer) {
            mBuffer += text;
        }
    }
    
    private String readFromBufferUntil(char chr) {
        String res = "";
        int i = 0;
        synchronized(mBuffer) {
            while (i < mBuffer.length()) {
                if (mBuffer.charAt(i) == chr) break;
                res += mBuffer.charAt(i++); 
            }
            if (i < mBuffer.length()) { //found
                mBuffer = mBuffer.substring(++i);
                return res;                
            } else { //not found
                return null;
            }
        }
    }
    
    private String readFromBuffer(int chars) {
        String res = null;
        int last = Math.min(chars, mBuffer.length());
        synchronized(mBuffer) {
            res = mBuffer.substring(0, last);
            mBuffer = mBuffer.substring(last);
            return res;
        }
    }
    
    private void sendText(String text) {
        byte[] buf = text.getBytes();
        if (mSerialDevice != null) {
            try {
                if (buf.length > 0) mSerialDevice.write(buf, USB_TIMEOUT);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
	
    //Serial read will run on a separate thread
	private volatile SerialInputOutputManager mSerialIoManager;
	private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();
	
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

        @Override
        public void onRunError(Exception e) {
            Log.d(TAG, "Runner stopped.");
        }

        @Override
        public void onNewData(final byte[] data) {
        	String text = new String(data);
            addToBuffer(text);
            Log.d(TAG, text);
            text = readFromBufferUntil((char) 0x0A); //LF
            if (text != null && text.length() > 0) {
            	Message msg = new Message().obtain();
            	Bundle content = new Bundle();
            	content.putString("data", text);
            	msg.setData(content);
            	msg.setTarget(serialHandler);
            	msg.sendToTarget();
            }
        }
    };
    
	@Override
	public void onCreate() {
		/* Initialize the flags */
		t = null;
		keepRunning = true;
		isConnected = false;
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "<< Service Started >>");
	    extras = intent.getExtras();
	    
	    mDeviceDescription = (String) extras.get("device");
	    
	    /* Broadcast Receiver */
	    broadcastManager = LocalBroadcastManager.getInstance(getBaseContext());
		registerBroadcasts();

		if (!isConnected) {
			/* Initialize variables */
			bootstrap();
			/* Start the thread */
			startThread();
		}
		
		return START_STICKY;
	}
	
    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager...");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (mSerialDevice != null) {
            Log.i(TAG, "Starting io manager...");
            mSerialIoManager = new SerialInputOutputManager(mSerialDevice, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }
		
    private void stopUsb() {
        stopIoManager();
        if (mSerialDevice != null) {
            try {
                mSerialDevice.close();
            } catch (IOException e) {
                // Ignore.
            }
            mSerialDevice = null;
        }
    }

    private void startUsb() {
    	mSerialDevice = UsbSerialProber.acquire(mUsbManager);
        Log.d(TAG, "Starting, mSerialDevice=" + mSerialDevice);
        if (mDeviceDescription != mSerialDevice.toString()) {
        	Log.w(TAG, "*** WARNING: DEVICES MISMATCH " + mDeviceDescription + " != " + mSerialDevice + " ***");
        }
        if (mSerialDevice == null) {
            Log.i(TAG, "No serial device.");
        } else {
            try {
                mSerialDevice.open();
                mSerialDevice.setBaudRate(USB_BAUD_RATE);
            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                try {
                    mSerialDevice.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                mSerialDevice = null;
                return;
            }
        }
        onDeviceStateChange();
    }
	
	private void bootstrap() {
		/* Set the flags */
		keepRunning = true;
		isConnected = false;
	}
	
	private void registerBroadcasts () {
		IntentFilter filter = new IntentFilter();
		filter.addAction("lac.contextnet.sddl.arduinonode.broadcastmessage.ActionSendUsb");
		broadcastManager.registerReceiver(mUsbBroadcastReceiver, filter);
	}
	
	private void unregisterBroadcasts() {
		broadcastManager.unregisterReceiver(mUsbBroadcastReceiver);
	}
	
	private void startThread() {
		t = new Thread(new Runnable () {
			public void run () {
				try {
					startUsb();

					isConnected = true;
					while (keepRunning) {
						/* Disconnect and set the thread to null */
						if (!isConnected) {
							keepRunning = false;
							//connection.disconnect();
							stopThread(t);
						}
						else {
							while (msgLst.size() > 0) {
								//connection.sendMessage(msgLst.get(0));
								sendText(msgLst.get(0));
								msgLst.remove(0);
							}
						}
						synchronized (t) {
							Thread.sleep(100);
						}
					}
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		t.start();
	}

	private BroadcastReceiver mUsbBroadcastReceiver = new BroadcastReceiver () {
		@Override
		public void onReceive(Context c, Intent i) {
			
			String action = i.getAction();
			if (action.equals("lac.contextnet.sddl.arduinonode.broadcastmessage.ActionSendUsb")) {
				Serializable s = i.getSerializableExtra("lac.contextnet.sddl.arduinonode.broadcastmessage.ExtraUsbMsg");
				//add message to queue
				msgLst.add((String) s);
			}
		}
	};
	
	/* Stop the thread */
	private synchronized void stopThread (Thread t) {
		stopUsb();
		if (t != null) {
			t = null;
		}
	}
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/* When the service is killed, disconnect */
	@Override
	public void onDestroy() {
		isConnected = false;
		
		/* Notify thread to disconnect */
		synchronized(t) {
			t.notify();
		}
		
		/* Unregister broadcast */
		unregisterBroadcasts();
		mDeviceDescription = null;
		Log.d(TAG, "<< Service Stopped >>");
		super.onDestroy();
	}
}
