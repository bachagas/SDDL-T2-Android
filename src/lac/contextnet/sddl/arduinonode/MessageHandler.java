package lac.contextnet.sddl.arduinonode;

import java.io.Serializable;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import lac.contextnet.model.PingObject;
import lac.contextnet.sddl.usernode.R;

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
		
		if (msg.getData().getString("status") != null) 
		{
			String status = msg.getData().getString("status");
			
			if (status.equals("connected")) 
				Log.d("SDDL", (String) context.getResources().getText(R.string.msg_d_connected));
			else if (status.equals("disconnected")) 
				Log.d("SDDL", (String) context.getResources().getText(R.string.msg_d_disconnected));
			else if (status.equals("package")) {
				Serializable s = msg.getData().getSerializable("package");
				Log.d("SDDL", s.toString());
				Toast.makeText(context, s.toString(), Toast.LENGTH_LONG).show();
				
				if (s instanceof PingObject) {
					//Toast.makeText(context, ((PingObject) s).toString(), Toast.LENGTH_LONG).show();
				}
				if (s instanceof String) {
					//Toast.makeText(context, (String) s, Toast.LENGTH_LONG).show();
					//TODO: save received messages to a local log
				}
				/* Here you can add different treatments to different types of 
				 * received data if you decide not to do that on the 
				 * NodeConnectionListener */
			}
			else
				Log.d("SDDL", status);
		}
	}
}
