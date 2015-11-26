package lac.contextnet.sddl.arduinonode;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.UUID;

import lac.cnclib.net.NodeConnection;
import lac.cnclib.net.mrudp.MrUdpNodeConnection;
import lac.cnclib.sddl.message.ApplicationMessage;
import lac.cnclib.sddl.message.Message;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class CommunicationService extends Service {

	private volatile Boolean keepRunning;
	private volatile Boolean isConnected;
	
	private ConnectionListener listener;
	private NodeConnection connection;
	private SocketAddress socket;
	private Thread t;
	private LocalBroadcastManager broadcastManager;

	private Handler handler;
	private Bundle extras;
	private UUID clientID;
	private ArrayList<Message> msgLst = new ArrayList<Message>();
	
	@Override
	public void onCreate() {
		
		/* Initialize the flags */
		t = null;
		keepRunning = true;
		isConnected = false;
		super.onCreate();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {

		Log.d("SDDL", "<< Service Started >>");
		handler = new MessageHandler(getBaseContext());
	    extras = intent.getExtras();
	    
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
	
	private void registerBroadcasts () {
	
		IntentFilter filter = new IntentFilter();
		filter.addAction("lac.contextnet.sddl.arduinonode.broadcastmessage.ActionSendMsg");
		broadcastManager.registerReceiver(mConnBroadcastReceiver, filter);
	}
	
	private void unregisterBroadcasts() {

		broadcastManager.unregisterReceiver(mConnBroadcastReceiver);
	}
	
	private void bootstrap()
	{
		/* Set the flags */
		keepRunning = true;
		isConnected = false;
		
		clientID = UUID.fromString(extras.getString("uuid"));
		
	    /* Instantiate the listener for this connection */
		listener = new ConnectionListener(handler, clientID);
	}
	
	private void startThread()
	{
		t = new Thread(new Runnable () {
			public void run () {
				try {
					/* Create a new MR-UDP connection */
					connection = new MrUdpNodeConnection(clientID);
					
					/* Assign the listener to the connection created */
					connection.addNodeConnectionListener(listener);
					
					/* Obtain the IP:PORT */
					String ip = extras.getString("ip");
					int port = extras.getInt("port");
					
					/* Create the socket and assign the IP:PORT */
					socket = new InetSocketAddress(ip, port);
					
					/* Assign the socket with the connection */
					connection.connect(socket);

					isConnected = true;
					while (keepRunning) 
					{
						/* Disconnect and set the thread to null */
						if (!isConnected) {
							keepRunning = false;
							connection.disconnect();
							stopThread(t);
						}
						else {
							while(msgLst.size() > 0) {
								connection.sendMessage(msgLst.get(0));
								msgLst.remove(0);
							}
						}
						synchronized (t) {
							Thread.sleep(1000);
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

	private BroadcastReceiver mConnBroadcastReceiver = new BroadcastReceiver () {

		@Override
		public void onReceive(Context c, Intent i) {
			
			String action = i.getAction();
			if (action.equals("lac.contextnet.sddl.arduinonode.broadcastmessage.ActionSendMsg")) {
				Serializable s = i.getSerializableExtra("lac.contextnet.sddl.arduinonode.broadcastmessage.ExtraMsg");
				
				ApplicationMessage am = new ApplicationMessage();
				am.setContentObject(s);
				am.setTagList(new ArrayList<String>());
				am.setSenderID(clientID);
				
				msgLst.add(am);
			}
		}
	};
	
	/* Stop the thread */
	private synchronized void stopThread (Thread t) {
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
		
		Log.d("SDDL", "<< Service Stopped >>");
		super.onDestroy();
	}
}
