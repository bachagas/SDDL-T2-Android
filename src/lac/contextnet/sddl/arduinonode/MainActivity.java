package lac.contextnet.sddl.arduinonode;

import java.io.IOException;
import java.util.UUID;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import lac.contextnet.model.EventObject;
import lac.contextnet.model.PingObject;
import lac.contextnet.sddl.usernode.R;

/**
 * MainActivity: This is our application's MainActivity. It consists in 
 * 				 a UUID randomly generated and shown in txt_uuid, a text 
 * 				 field for the IP:PORT in et_ip, a "Ping!" button 
 * 				 (btn_ping) to send a Ping object message, a "Start 
 * 				 Service!" button (btn_startservice) to start the 
 * 				 communication service and a "Stop Service!" button 
 * 				 (btn_stopservice) to stop it.
 * 
 * @author andremd
 * 
 */
public class MainActivity extends Activity {

	/* Shared Preferences */
	private static String uniqueID = "550547b6-dcea-4aa6-8279-04332f1e251e";
	private static final String PREF_UNIQUE_ID = "USB_NODE_UNIQUE_ID";
	private static final String PREF_SDDL_SERVER = "PREF_SDDL_SERVER";
	
	/* Static Elements */
	private TextView txt_uuid;
	private EditText et_ip;
	private Button btn_ping;
	private Button btn_startservice;
	private Button btn_stopservice;
	private RadioGroup toggleGroupContexts;
	private Button btnComputer;
	private Button btnLamp;
	private Button btnTv;
	private Button btnEvent1;
	private Button btnEvent2;
	private TextView mUsbDeviceText;
	private TextView mOutputText;
	private ScrollView mOutputScrollView;
    private EditText mSendText;
    private Button mSendButton;
    private CheckBox mAutoScrollOutput;
	
	/* USB Elements */
    private UsbManager mUsbManager; //the system's USB service
    private UsbSerialDriver mSerialDevice; //the device currently in use, or {@code null}
    private UsbHandler mUsbIOHandler; //a handler for the received data
	
	/* App data */
	private String context = "computer"; //initial context
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* GUI Elements */
		txt_uuid = (TextView) findViewById(R.id.txt_uuid);
		et_ip = (EditText) findViewById(R.id.et_ip);
		//sets initial "default" ip address
		et_ip.setText(getServerAddress(getBaseContext()));
		btn_ping = (Button) findViewById(R.id.btn_ping);
		btn_startservice = (Button) findViewById(R.id.btn_startservice);
		btn_stopservice = (Button) findViewById(R.id.btn_stopservice);
		mUsbDeviceText = (TextView) findViewById(R.id.usbDeviceText);
		mOutputText = (TextView) findViewById(R.id.outputText);
		mOutputScrollView = (ScrollView) findViewById(R.id.outputScroller);
        mSendButton = (Button) findViewById(R.id.sendButton);
        mSendText = (EditText) findViewById(R.id.sendText);
        mAutoScrollOutput = (CheckBox) findViewById(R.id.checkBoxAutoScrollOutput);

		txt_uuid.setText(getUUID(getBaseContext()));
		
		/* Ping Button Listener*/
		btn_ping.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!isMyServiceRunning(CommunicationService.class))
					Toast.makeText(getBaseContext(), getResources().getText(R.string.msg_e_servicenotrunning), Toast.LENGTH_SHORT).show();
				else
				{
					PingObject ping = new PingObject();
					
					/* Calling the SendPingMsg action to the PingBroadcastReceiver */
					Intent i = new Intent(MainActivity.this, CommunicationService.class);
					i.setAction("lac.contextnet.sddl.arduinonode.broadcastmessage." + "ActionSendMsg");
					i.putExtra("lac.contextnet.sddl.arduinonode.broadcastmessage." + "ExtraMsg", ping);
					LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
				}
			}
		});

		/* Start Service Button Listener*/
		btn_startservice.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {

				String ipPort = et_ip.getText().toString();
				
				if(!IPPort.IPRegexChecker(ipPort))
				{
					Toast.makeText(getBaseContext(), getResources().getText(R.string.msg_e_invalid_ip), Toast.LENGTH_LONG).show();
					return;
				}
				setServerAddress(getBaseContext(), ipPort);
				IPPort ipPortObj = new IPPort(ipPort);
				
				/* Starting the communication service */
				Intent intent = new Intent(MainActivity.this, CommunicationService.class);
				intent.putExtra("ip", ipPortObj.getIP());
				intent.putExtra("port", Integer.valueOf(ipPortObj.getPort()));
				intent.putExtra("uuid", txt_uuid.getText().toString());
				startService(intent); 
			}
		});

		/* Stop Service Button Listener*/
		btn_stopservice.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				/* Stops the service and finalizes the connection */
				stopService(new Intent(getBaseContext(), CommunicationService.class));
			}
		});
		
		/* Ping Button Listener*/
		mSendButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(!isMyServiceRunning(UsbService.class))
					Toast.makeText(getBaseContext(), getResources().getText(R.string.msg_e_usbnotrunning), Toast.LENGTH_SHORT).show();
				else
				{
					String text = mSendText.getText().toString();
					
					/* Calling the SendPingMsg action to the PingBroadcastReceiver */
					Intent i = new Intent(MainActivity.this, CommunicationService.class);
					i.setAction("lac.contextnet.sddl.arduinonode.broadcastmessage." + "ActionSendUsb");
					i.putExtra("lac.contextnet.sddl.arduinonode.broadcastmessage." + "ExtraUsbMsg", text);
					LocalBroadcastManager.getInstance(getBaseContext()).sendBroadcast(i);
				}
			}
		});
		
		//Forces to hide the soft keyboard
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		
		/* USB management */
		mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
		mUsbIOHandler = new UsbHandler();
	}
	
	private class UsbHandler extends Handler {
		@Override
		public void handleMessage(Message msg) 
		{
			super.handleMessage(msg);
			
			if (msg.getData().getString("data") != null) {
				mOutputText.append(msg.getData().getString("data").toString() + "\n");
	            if (mAutoScrollOutput.isChecked()) mOutputScrollView.smoothScrollTo(0, mOutputText.getBottom());
			}
		}
	}
	
	@Override
    protected void onPause() {
        super.onPause();
        /* Stops the service and finalizes the USB connection */
        if (isMyServiceRunning(UsbService.class)) {
    		stopService(new Intent(getBaseContext(), UsbService.class));        	
        }
    }
	
    @Override
    protected void onResume() {
        super.onResume();
        
        /* Starting the USB serial service */
		Intent intent = new Intent(MainActivity.this, UsbService.class);
		mSerialDevice = UsbSerialProber.acquire(mUsbManager);
		if (mSerialDevice == null) {
            Log.d("MAIN", "No serial device.");
            mUsbDeviceText.setText(getResources().getText(R.string.no_usb_device));
        } else {
            mUsbDeviceText.setText(mSerialDevice.toString());
			intent.putExtra("device", mSerialDevice.toString());
			startService(intent);
			UsbService.setUsbHandler(mUsbIOHandler);
        }
    }
	
	//See http://stackoverflow.com/questions/600207/how-to-check-if-a-service-is-running-in-android
	private boolean isMyServiceRunning(Class<?> serviceClass) {
	    ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if (serviceClass.getName().equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
	
	//See http://androidsnippets.com/generate-random-uuid-and-store-it
	public synchronized static String getUUID(Context context) {
	    if (uniqueID == null) {
	        SharedPreferences sharedPrefs = context.getSharedPreferences(
	                PREF_UNIQUE_ID, Context.MODE_PRIVATE);
	        uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
	        if (uniqueID == null) {
	            uniqueID = UUID.randomUUID().toString();
	            Editor editor = sharedPrefs.edit();
	            editor.putString(PREF_UNIQUE_ID, uniqueID);
	            editor.commit();
	        }
	    }
	    return uniqueID;
	}
	
	public synchronized static String getServerAddress(Context context) {
    	String addr;
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_SDDL_SERVER, Context.MODE_PRIVATE);
        addr = sharedPrefs.getString(PREF_SDDL_SERVER, null);
        if (addr == null) {
            addr = "192.168.1.104:5555";
            Editor editor = sharedPrefs.edit();
            editor.putString(PREF_SDDL_SERVER, addr);
            editor.commit();
        }
	    return addr;
	}
	
	public synchronized static void setServerAddress(Context context, String addr) {
        SharedPreferences sharedPrefs = context.getSharedPreferences(
                PREF_SDDL_SERVER, Context.MODE_PRIVATE);
        Editor editor = sharedPrefs.edit();
        editor.putString(PREF_SDDL_SERVER, addr);
        editor.commit();
	}
}
