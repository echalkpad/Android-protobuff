package cvg.sfmPipeline.protoLog;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class TCPclient extends AsyncTask<Void, String, Void>{
	
	private final String IPadd = "192.168.0.30";
	private final int	 port  =  60000;
	private LoggerActivity parent;
	
	public TCPclient(LoggerActivity ac){
		parent = ac;
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		Socket socket = null;
		DataInputStream dataInputStream = null;
		try {
			socket = new Socket(IPadd, port);
			String line = "";
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			while(!isCancelled()){
				line = in.readLine();
				if(line != null)
					publishProgress(line);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// the server is probably not available yet
			publishProgress("host_not_found_123456");
			e.printStackTrace();
		}
		finally{
			if (socket != null){
				try {
					socket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (dataInputStream != null){
				try {
					dataInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		if(values[0].startsWith("snap"))
			parent.remoteSnap();
		if(values[0].contentEquals("q")){
			cancel(true);
			Toast.makeText(parent.getApplicationContext(),
	                "Connection terminated.",
	                Toast.LENGTH_LONG).show();
			parent.cleanup();
			parent.finish();
		}
		if(values[0].contentEquals("host_not_found_123456")){
			Toast.makeText(parent.getApplicationContext(),
	                "Connection failed. TCP server at " + IPadd + " : " + port +" is not found.",
	                Toast.LENGTH_LONG).show();
			parent.cleanup();
			parent.finish();
		}
		super.onProgressUpdate(values);
	}

}
