package cvg.sfmPipeline.protoLog;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import com.google.protobuf.ByteString;

import cvg.sfmPipeline.protoLog.ProtoLog.CameraBodyTransProto;
import cvg.sfmPipeline.protoLog.ProtoLog.CameraMatrixProto;
import cvg.sfmPipeline.protoLog.ProtoLog.FrameProto;
import cvg.sfmPipeline.protoLog.ProtoLog.MetadataProto;
import cvg.sfmPipeline.protoLog.ProtoLog.cvMatProto;
import cvg.sfmPipeline.protoLog.ProtoLog.cvMatProto.ImageType;

import android.util.Log;

public class ProtoWriter {
	static private final String TAG = "ProtoLog::Writer";
	
	static private File directory;
	static private int	noMessages = 0;
	static public byte[] data;
	
	private ProtoWriter(){
	}
	
	static public void setupStorage(){
		/// file initialization
	    String directoryName = LoggerApplication.getDataLoggerPath();
	    directory = new File(directoryName);
	    if (!directory.exists() && !directory.mkdirs()) {
	        try {
	            throw new IOException(
	                    "Path to file could not be created. " + directory.getAbsolutePath());
	        } catch (IOException e) {
	            Log.e(TAG, "Directory could not be created. " + e.toString());
	        }
	    }
	}
	
	static public void buildMessage(){
		//-- Add image metadata
		MetadataProto.Builder meta = MetadataProto.newBuilder();
		
		meta.setAngX( LoggerApplication.getLastGroundTr()[0]);
		meta.setAngY( LoggerApplication.getLastGroundTr()[1]);
		meta.setAngZ( LoggerApplication.getLastGroundTr()[2]);
		meta.setPosX( LoggerApplication.getLastGroundTr()[3]);
		meta.setPosY( LoggerApplication.getLastGroundTr()[4]);
		meta.setPosZ( LoggerApplication.getLastGroundTr()[5]);
		
		meta.setVal0( LoggerApplication.getLastSensor()[0]);
		meta.setVal1( LoggerApplication.getLastSensor()[1]);
		meta.setVal2( LoggerApplication.getLastSensor()[2]);
		meta.setType(LoggerApplication.getSensorType());
		
		meta.setTimestamp(TimeStamp.getTime());
		
		//-- Add calibration data
		CameraMatrixProto.Builder K = CameraMatrixProto.newBuilder();
		
		float[] Kf = LoggerApplication.getCameraMatrix();
		for (int i = 0; i < Kf.length; i++)
			K.addData(Kf[i]);
		CameraBodyTransProto.Builder C2B = CameraBodyTransProto.newBuilder();
		float[] C2Bf = LoggerApplication.getCamera2body();
		for (int i = 0; i < C2Bf.length; i++)
			C2B.addData(C2Bf[i]);

		//-- Add image data
		cvMatProto.Builder image = cvMatProto.newBuilder();
		image.setBytedata(ByteString.copyFrom(LoggerApplication.getImage()));
		image.setFormat(ImageType.JPEG);
		
		//-- Bundle everything
		FrameProto.Builder frame = FrameProto.newBuilder();
		
		frame.addImages(image.build());
		frame.setMetadata(meta.build());
		frame.setCameraMatrix(K.build());
		frame.setCameraBodyTrans(C2B.build());
		frame.setId(3l); // should contain an identifier for the device! TODO
		frame.setSeq(LoggerApplication.noFrames);
		data = frame.build().toByteArray();
	}
	
	static public void writeMessage(){
		buildMessage();
		noMessages++;
		String str = String.format(
				"%s/%04d.frs",directory.getAbsoluteFile().toString(), noMessages);
		try {
			FileOutputStream outStream = new FileOutputStream(str);
			Log.i(TAG, "Data here " + data.length);
			outStream.write(data);
			outStream.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}

