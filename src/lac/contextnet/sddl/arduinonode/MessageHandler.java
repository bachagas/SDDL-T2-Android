package lac.contextnet.sddl.arduinonode;

import java.io.Serializable;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;
import lac.contextnet.model.PingObject;
import lac.contextnet.sddl.arduinonode.R;

public class MessageHandler extends Handler {
	
	private Context context;
	
	public MessageHandler(Context context)
	{
		this.context = context;
	}
	
	@Override
	public void handleMessage(Message msg) 
	{
		super.handleMessage(msg);
		Log.d("SDDL", "Handling message=" + msg.getData().toString());
		//Toast.makeText(context, msg.getData().toString(), Toast.LENGTH_LONG).show();
		if (msg.getData().getString("status") != null) 
		{
			String status = msg.getData().getString("status");
			
			if (status.equals("connected")) 
				Log.d("SDDL", (String) context.getResources().getText(R.string.msg_d_connected));
			else if (status.equals("disconnected")) 
				Log.d("SDDL", (String) context.getResources().getText(R.string.msg_d_disconnected));
			else if (status.equals("package")) {
				Serializable s = msg.getData().getSerializable("package");
				Log.d("SDDL", "Package received=" + s.toString());
				Toast.makeText(context, s.toString(), Toast.LENGTH_LONG).show();
				
				if (s instanceof PingObject) {
					PingObject p = (PingObject) s;
					//Toast.makeText(context, p.toString(), Toast.LENGTH_LONG).show();
					Log.d("SDDL", "Ping object received=" + p.toString());
				}
				/* Here you can add different treatments to different types of 
				 * received data if you decide not to do that on the 
				 * NodeConnectionListener */
				if (s instanceof String) {
					String text = (String) s;
					Log.d("SDDL", "String object received=" + text);
					/* Calling the SendPingMsg action to the PingBroadcastReceiver */
					Intent i = new Intent(context, CommunicationService.class);
					i.setAction("lac.contextnet.sddl.arduinonode.broadcastmessage." + "ActionSendUsb");
					i.putExtra("lac.contextnet.sddl.arduinonode.broadcastmessage." + "ExtraUsbMsg", text);
					LocalBroadcastManager.getInstance(context).sendBroadcast(i);
				}
			} else {
				Log.d("SDDL", status);
			}
		}
	}
}
