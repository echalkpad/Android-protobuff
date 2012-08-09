/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package cvg.sfmPipeline.protoLog;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;

import cvg.sfmPipeline.protoLog.ProtoLog.MetadataProto.SensorType;


public final class LoggerApplication {

    static private String filePathUniqueIdentifier = "01-01_01-01-000";
    static private int camera = 0;
    static private String ipAddress = "0";
    static private boolean storeInSD = true; 
    static public boolean ready2send = true;
    static public boolean ready2snap = true;
    static private boolean remoteOperated = false; 
    static private boolean continuousMode = true;
    
    
    public static boolean isRemoteOperated() {
		return remoteOperated;
	}
    
    public static void setDefaults() {
    	filePathUniqueIdentifier = "01-01_01-01-000";
    	camera = 0;
    	ipAddress = "0";
    	storeInSD = true; 
    	ready2send = true;
    	ready2snap = true;
    	remoteOperated = false; 
    	continuousMode = true;
    	noFrames = 0l;
	}
    
	public static void setRemoteOperated(boolean remoteOperated) {
		LoggerApplication.remoteOperated = remoteOperated;
	}

	public static boolean isContinuousMode() {
		return continuousMode;
	}

	public static void setContinuousMode(boolean continuousMode) {
		LoggerApplication.continuousMode = continuousMode;
	}

	// noninstantiability
    private LoggerApplication(){}
    
    static public int getCam(){
    	return camera;
    }
    
    static public void setCam(int c){
    	camera = c;
    }
    
    static public void generateNewFilePathUniqueIdentifier() {
        Date date = new Date();
        SimpleDateFormat fmt = new SimpleDateFormat("MM-dd_HH-mm-SS");
        filePathUniqueIdentifier = fmt.format(date);
    }

    static public void resetFilePathUniqueIdentifier() {
        filePathUniqueIdentifier = null;
    }

    static private String getFilePathUniqueIdentifier() {
        if (filePathUniqueIdentifier == null) {
            throw new IllegalStateException(
                    "filePathUniqueIdentifier has not been initialized for the app.");
        }
        return filePathUniqueIdentifier;
    }

    static public String getDataLoggerPath() {
    	return Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Log/" + getFilePathUniqueIdentifier() + "/";
    }

    static public String getLocalIpAddress() {
    	return ipAddress;
    }
    
    @SuppressWarnings("deprecation")
    static public void setLocalIpAddress(WifiManager wim){
    	ipAddress = Formatter.formatIpAddress(wim.getConnectionInfo().getIpAddress());
    }
    
    
    
    public static boolean isStoreInSD() {
		return storeInSD;
	}

	public static boolean setStoreInSD(Context c, boolean storeInSD) {
		if(!storeInSD && ipAddress.startsWith("0.0.0")){
			Toast.makeText(c,
	                "At the moment only one sensor output can be stored. Default is Gravity.",
	                Toast.LENGTH_LONG).show();
			LoggerApplication.storeInSD = true;
		}
		else
			LoggerApplication.storeInSD = storeInSD;
		return LoggerApplication.storeInSD;
	}



	//--   Data Fields ---------------------------------------------------------------
    static private float[]		lastSensor = {0,0,0}; 
    static private float[] 		lastGroundTr = {0,0,0,0,0,0};
    static private  byte[] 		image;	
    static private float[] 		cameraMatrix = {0,0,0,0,0,0,0,0,0};
    static private float[] 		camera2body = {0,0,0,0,0,0,0,0,0};
    static private SensorType 	sensorType = SensorType.GRAVITY;
    static public 	long		noFrames = 0l;
    
    public static float[] getLastGroundTr() {
		return lastGroundTr;
	}

	public static void setLastGroundTr(float[] lastGroundTr) {
		LoggerApplication.lastGroundTr = lastGroundTr;
	}
    
	public static float[] getCameraMatrix() {
		return cameraMatrix;
	}

	public static void setCameraMatrix(float[] cameraMatrix) {
		LoggerApplication.cameraMatrix = cameraMatrix;
	}

	public static float[] getCamera2body() {
		return camera2body;
	}

	public static void setCamera2body(float[] camera2body) {
		LoggerApplication.camera2body = camera2body;
	}

	public static SensorType getSensorType() {
		return sensorType;
	}

	public static void setSensorType(SensorType sensorType) {
		LoggerApplication.sensorType = sensorType;
	}

	public static float[] getLastSensor() {
		return lastSensor;
	}

	public static void setLastSensor(float[] lastSensor) {
		LoggerApplication.lastSensor = lastSensor;
	}


	public static byte[] getImage() {
		return image;
	}

	public static void setImage(byte[] image) {
		LoggerApplication.image = image;
	}

	

	
    
}

