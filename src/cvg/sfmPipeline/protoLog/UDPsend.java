package cvg.sfmPipeline.protoLog;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.lang.Long;

import android.os.AsyncTask;
import android.util.Log;

public class UDPsend extends AsyncTask<Integer, Void, Void> {

	//--   Constants
	private static final int UDP_SERVER_PORT = 50050; 
	private static final String IP = "192.168.0.255";// broadcast address cannot be used to obtain network info, so we need:
	private static final String IPnet = LoggerApplication.getLocalIpAddress(); 
	//--   Members
	private DatagramSocket socket;
	public boolean send = false;
	private String TAG = "UDPsend::ProtoLog";


	@Override
	protected Void doInBackground(Integer... a) {
		if(LoggerApplication.ready2send){
			LoggerApplication.ready2send = false;
			try {
				InetAddress thisAddress = InetAddress.getByName(IP);
				socket = new DatagramSocket();
				if(a[0] == 1){
					DatagramPacket endthis = new DatagramPacket("EXIT".getBytes(), "EXIT".getBytes().length, 
							thisAddress, UDP_SERVER_PORT);
					socket.send(endthis);
				}else{
					Log.i(TAG, "command: " + a[0]);
					NetworkInterface ni = NetworkInterface.getByInetAddress(InetAddress.getByName(IPnet));
					int chunkNo = (int) Math.ceil((double)ProtoWriter.data.length / (double)ni.getMTU());
					String headerStr = "ProtobuffMessage_"+chunkNo;
					DatagramPacket header = new DatagramPacket(headerStr.getBytes(), headerStr.length(), 
							thisAddress, UDP_SERVER_PORT);
					socket.send(header);
					for (int i = 0; i < chunkNo; i++){
						int start = Math.min(ProtoWriter.data.length,i * ni.getMTU());
						int end = Math.min(ProtoWriter.data.length, (i+1)*ni.getMTU());
						byte[] thisChunk = Arrays.copyOfRange(ProtoWriter.data, start, end);
						DatagramPacket dp = new DatagramPacket(thisChunk, thisChunk.length, 
								thisAddress, UDP_SERVER_PORT);
						socket.send(dp);
					}
					Checksum check = new CRC32();
					check.update(ProtoWriter.data, 0, ProtoWriter.data.length);
					long CHKSUM = check.getValue();
					DatagramPacket checkPack = new DatagramPacket(String.valueOf(CHKSUM).getBytes(), 
							String.valueOf(CHKSUM).getBytes().length, thisAddress, UDP_SERVER_PORT);
					socket.send(checkPack);
				}
				Log.i(TAG, "message sent at " + TimeStamp.getTime()/1000);
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

    @Override
	protected void onPostExecute(Void result) {
    	LoggerApplication.ready2send = true;
		super.onPostExecute(result);
	}

    
    @Override
    protected void onCancelled(){
    }


}