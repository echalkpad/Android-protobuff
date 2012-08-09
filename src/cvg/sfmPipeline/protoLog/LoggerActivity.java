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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.SlidingDrawer;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import cvg.sfmPipeline.protoLog.ProtoLog.MetadataProto.SensorType;
import cvg.sfmPipeline.protoLog.R;

// This very simple application will store the  sensors and images on a Date-labeled
// folder. Also it can log an UDP incoming stream from a hard-coded
// port and IP address. Check "UDPthread" class for details on the IP and port. Notice also
// that the sensors available on each phone will differ, sometimes it may provide
// post-processed values for better pose estimation. This can be checked on the
// logcat output under DEBUG tag

// Author: Federico Camposeco Paulsen

public class LoggerActivity extends Activity {
    public static final String TAG = "protoLogger::activity";
    
    //--   State
    private volatile Boolean mIsRecording;
    private boolean logGrav = true;
    private boolean logAccel = false;
    private boolean logOrien = false;
    private boolean logCame;
    private boolean logVICON;
    private List<Integer> logList;
    private UDPthread listenUDP;
    private TCPclient listenTCP;
    private CameraThread camThread;
    private int camNumber = 1;
    public long snapDelay;
    
    //--   GUI
    private CameraPreview mCamera;
    private TextView mAccelXTextView;
    private TextView mAccelYTextView;
    private TextView mAccelZTextView;
    private TextView mGyroXTextView;
    private TextView mGyroYTextView;
    private TextView mGyroZTextView;
    private TextView mMagXTextView;
    private TextView mMagYTextView;
    private TextView mMagZTextView;
    private ProgressBar mStorageBarImageView;
    private TextView mStorageTextView;
    private SlidingDrawer mDataDrawer;    
    private AlertDialog alertDialog;
    private RadioGroup mRadioGroup;
    private RadioGroup mRadioGroupRaw;
    private EditText mDelayText;
    private Button mRecordButton;
    
    //--   Sensors
    private List<Sensor> sensors;
    private SensorManager mSensorManager;
    private StatFs mStatFs;
    private int mFreeSpacePct;
       
    //--   Sensor event handlers
    private SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
        	// publish global timestamp
        	TimeStamp.updateTime(event.timestamp);
            Sensor sensor = event.sensor;
            int type = sensor.getType();
            if (logList.contains(type) && !sensor.getVendor().equalsIgnoreCase("Google Inc.")){ 
	            synchronized (mIsRecording) {
//	                if (mIsRecording) 
	                	LoggerApplication.setLastSensor(event.values);
//	                else
	                	updateSensorUi(sensor.getType(), event.accuracy, event.values);
	            }
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LoggerApplication.setLocalIpAddress((WifiManager)getSystemService(WIFI_SERVICE));
        LoggerApplication.generateNewFilePathUniqueIdentifier();
        LoggerApplication.noFrames = 0l;
        Bundle extras = getIntent().getExtras();
        LoggerApplication.setRemoteOperated(extras.getBoolean("remoteMode"));
        
        ProtoWriter.setupStorage();
        
        // Keep the screen on to make sure the phone stays awake.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mIsRecording = false;
       
		setContentView(R.layout.main);
		mCamera = (CameraPreview) findViewById(R.id.camPreview);
		
		// Setup the initial available space
        mStatFs = new StatFs(Environment.getExternalStorageDirectory().toString());
        float percentage = (float) (mStatFs.getBlockCount() - mStatFs.getAvailableBlocks())
                / (float) mStatFs.getBlockCount();
        mFreeSpacePct = (int) (percentage * 100);

        mStorageBarImageView = (ProgressBar) findViewById(R.id.progressBar);
        mStorageBarImageView.setMax(100);
        mStorageBarImageView.setProgress(mFreeSpacePct);
        mStorageTextView = (TextView) findViewById(R.id.usedMem);
        mStorageTextView.setText(mFreeSpacePct + "%  ");
        
        mRadioGroup = (RadioGroup) findViewById(R.id.radioGroup);        
        
        mDelayText = (EditText) findViewById(R.id.pictureRate);
        snapDelay = Long.decode(mDelayText.getText().toString());
        mDelayText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) { 
                snapDelay = Long.decode(v.getText().toString());
                return false;
            }
        });
        
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			// will set the field of application::camnumber 
			public void onCheckedChanged(RadioGroup rg, int checkedId) {
				switch (checkedId) {
					case R.id.isBack:
						camNumber = 0;
						break;
					case R.id.isFront:
						camNumber = 1;
						break;
					default:
						camNumber = 1;
						break;
				}
				LoggerApplication.setCam(camNumber);
				mCamera.changeCamera();
			}
		});
        
        
        final CheckBox checkGravity = (CheckBox) findViewById(R.id.gravityFlag);
        checkGravity.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				logGrav = checkGravity.isChecked();
				getSensorList(logAccel, logGrav, logOrien);
				setupSensors();
			}
		});
        final CheckBox checkAccel = (CheckBox) findViewById(R.id.accelFlag);
        checkAccel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				logAccel = checkAccel.isChecked();
				getSensorList(logAccel, logGrav, logOrien);
				setupSensors();
			}
		});
        
        final CheckBox checkOrien = (CheckBox) findViewById(R.id.orienFlag);
        checkOrien.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				logOrien = checkOrien.isChecked();
				getSensorList(logAccel, logGrav, logOrien);
				setupSensors();
			}
		});
        
        
        mRadioGroupRaw = (RadioGroup) findViewById(R.id.radioGroup1);
        mRadioGroupRaw.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			// will change the log target
			public void onCheckedChanged(RadioGroup rg, int checkedId) {
				boolean retval, log2SD = true;
				switch (checkedId) {
					case R.id.logToSD:
						log2SD = true;
						break;
					case R.id.logUDP:
						log2SD = false;

				}
				retval = LoggerApplication.setStoreInSD(getApplicationContext(), log2SD);
			
			}
		});
        
        final Switch automode = (Switch)findViewById(R.id.switch1);
        automode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				LoggerApplication.setContinuousMode(isChecked);
				if(isChecked)
					mRecordButton.setText("Start Logging");
				else
					mRecordButton.setText("Take Picture");
			}
		});
        
        final CheckBox checkCam = (CheckBox) findViewById(R.id.camFlag);
        final CheckBox checkVicon = (CheckBox) findViewById(R.id.viconFlag);
        final TextView ipADD = (TextView) findViewById(R.id.viconADD);
        logList = new ArrayList<Integer>();
    	getSensorList(false, true, false);
        
        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle("Log saved in:");
        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL,"OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				cleanup();
				finish();
			}
		});
        alertDialog.setIcon(R.drawable.aloglogo);
        mDataDrawer = (SlidingDrawer) findViewById(R.id.slidingDrawer1);
        ipADD.setText(LoggerApplication.getLocalIpAddress() + ", port 50000" );
        
        listenUDP = new UDPthread(this);
        listenTCP = new TCPclient(this);
        mRecordButton = (Button) findViewById(R.id.loggFlag);
        if(LoggerApplication.isRemoteOperated()){
        	mRecordButton.setVisibility(View.INVISIBLE);
        	listenTCP.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
        mRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	LoggerApplication.setContinuousMode(automode.isChecked());
            	logAccel = checkAccel.isChecked();
            	logGrav = checkGravity.isChecked();
            	logOrien = checkOrien.isChecked();
            	logCame = checkCam.isChecked();
            	logVICON = checkVicon.isChecked();
            	
            	getSensorList(logAccel, logGrav, logOrien);
            	setupSensors();
                synchronized (mIsRecording) {
                    if (!mIsRecording) {
                    	camThread = new CameraThread(mCamera);
                    	if(LoggerApplication.isContinuousMode()){
                    		mRecordButton.setText("Stop Logging");
                    		mRecordButton.setBackgroundColor(Color.RED);
                    	}
                    	mRadioGroup.setEnabled(false);
                    	mRadioGroupRaw.setEnabled(false);
                    	automode.setEnabled(false);
                    	TimeStamp.restart();
                    	
                    	if (logAccel || logGrav || logOrien)
                    		mIsRecording = true;
                    	
                    	if (logVICON){
                    		mIsRecording = true;
                    		listenUDP.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    	}
                    	
                    	if (logCame){
						    try {
						    	mIsRecording = true;
						    	Log.i(TAG, "delay: " + snapDelay + " iscontin: " + String.valueOf(LoggerApplication.isContinuousMode()));
						    	if(LoggerApplication.isContinuousMode())
						    		camThread.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, snapDelay);
						    	else
							    	new CameraThread(mCamera).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0l);
						    } catch (Exception e) {
						        Log.e("ls", "Recording has failed...", e);
						        Toast.makeText(getApplicationContext(),
						                "Camera hardware error. Please restart the application.",
						                Toast.LENGTH_LONG).show();
						        finish();
						        return;
						    }
					    }else{
					    	mCamera = null;
					    }                   		
					} else {
						if(LoggerApplication.isContinuousMode()){
						    cleanup();
						    if(LoggerApplication.isStoreInSD())
						    	alertDialog.setMessage(LoggerApplication.getDataLoggerPath());
						    else
						    	alertDialog.setMessage("UDP port closed.");
						    alertDialog.show();
					    }else{                    	
	                    	if (logCame){
							    try {
							    	new CameraThread(mCamera).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, -1l);
							    } catch (Exception e) {
							        Log.e("ls", "Recording has failed...", e);
							        Toast.makeText(getApplicationContext(),
							                "Camera hardware error. Please restart the application.",
							                Toast.LENGTH_LONG).show();
							        finish();
							        return;
							    }
						    }else{
						    	mCamera = null;
						    } 
					    }
					}
                }
            }
        });

        setupSensors();
    }
    
    
    
    public void remoteSnap(){
    	Log.i(TAG, "triggered");
    	mRecordButton.callOnClick();
    }


	@Override
    public void onPause() {
        super.onPause();
        cleanup();
    }

	private void getSensorList(boolean acc, boolean grav, boolean orien){
    	logList.clear();
    	if (acc){
    		logList.add(Sensor.TYPE_ACCELEROMETER);
    		LoggerApplication.setSensorType(SensorType.ACCEL);
    	}
    	if (grav){
    		logList.add(Sensor.TYPE_GRAVITY);
    		LoggerApplication.setSensorType(SensorType.GRAVITY);
    	}
    	if (orien){
    		logList.add(Sensor.TYPE_ROTATION_VECTOR);
    		LoggerApplication.setSensorType(SensorType.ROTVECT);
    	}

    	if(logList.size() > 1){
    		Toast.makeText(getApplicationContext(),
	                "At the moment only one sensor output can be stored. Default is Gravity.",
	                Toast.LENGTH_LONG).show();
    		LoggerApplication.setSensorType(SensorType.GRAVITY);
    		logList.clear();
    		logList.add(Sensor.TYPE_GRAVITY);
    	}
    }
    
    private void setupSensors() {
        initSensorUi();
        if(mSensorManager != null)
        	mSensorManager.unregisterListener(mSensorEventListener);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        
        for (Sensor sensor : sensors) {
        	if (!logList.contains( sensor.getType() ) )
            	continue;
			mSensorManager.registerListener(
                    mSensorEventListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
		}
        
    }

    private void initSensorUi() {
    	if (mAccelXTextView == null){
	        mAccelXTextView = (TextView) findViewById(R.id.accelX);
	        mAccelYTextView = (TextView) findViewById(R.id.accelY);
	        mAccelZTextView = (TextView) findViewById(R.id.accelZ);
	
	        mGyroXTextView = (TextView) findViewById(R.id.gyroX);
	        mGyroYTextView = (TextView) findViewById(R.id.gyroY);
	        mGyroZTextView = (TextView) findViewById(R.id.gyroZ);
	
	        mMagXTextView = (TextView) findViewById(R.id.compX);
	        mMagYTextView = (TextView) findViewById(R.id.compY);
	        mMagZTextView = (TextView) findViewById(R.id.compZ);
    	}
    }

      
    private void updateSensorUi(int sensorType, int accuracy, float[] values) {
        TextView xTextView;
        TextView yTextView;
        TextView zTextView;
        if (sensorType == Sensor.TYPE_ACCELEROMETER) {
            xTextView = mAccelXTextView;
            yTextView = mAccelYTextView;
            zTextView = mAccelZTextView;
        } else if (sensorType == Sensor.TYPE_ROTATION_VECTOR) {
            xTextView = mGyroXTextView;
            yTextView = mGyroYTextView;
            zTextView = mGyroZTextView;
        } else if (sensorType == Sensor.TYPE_GRAVITY) {
            xTextView = mMagXTextView;
            yTextView = mMagYTextView;
            zTextView = mMagZTextView;
        } else {
            return;
        }

        int textColor = Color.WHITE;
        String prefix = "";
  
        xTextView.setTextColor(textColor);
        yTextView.setTextColor(textColor);
        zTextView.setTextColor(textColor);
        xTextView.setText(prefix + numberDisplayFormatter(values[0]));
        yTextView.setText(prefix + numberDisplayFormatter(values[1]));
        zTextView.setText(prefix + numberDisplayFormatter(values[2]));

    }


    private String numberDisplayFormatter(float value) {
        String displayedText = Float.toString(value);
        if (value >= 0) {
            displayedText = " " + displayedText;
        }
        if (displayedText.length() > 8) {
            displayedText = displayedText.substring(0, 8);
        }
        while (displayedText.length() < 8) {
            displayedText = displayedText + " ";
        }
        return displayedText;
    }

    public void cleanup() {
        try {
            synchronized (mIsRecording) {
                if(camThread != null){
                	if(!camThread.isCancelled()){
                		camThread.cancel(true);
                		camThread = null;
                	}
                }
                
                if (mCamera != null){
                	mCamera.releaseCamera();
                }
                
                if(listenUDP != null){
	                if(!listenUDP.isCancelled()){
	                	listenUDP.cancel(true);
	                	listenUDP = null;                	
	                }
                }
                
                if(!LoggerApplication.isStoreInSD()){
                	new UDPsend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 1);
                }
                if(listenTCP != null){
	                if(!listenTCP.isCancelled()){
	                	listenTCP.cancel(true);
	                	listenTCP = null;                	
	                }
                }
                if (!mIsRecording) {
                    cleanupEmptyFiles();
                }
                mIsRecording = false;
            }

        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void onRestart(){
    	super.onRestart();
    	onCreate(null);
    }

    private void cleanupEmptyFiles() {
        Log.i(TAG, "cleaning up empty dirs and zero byte files");
        String logPath = LoggerApplication.getDataLoggerPath();
        List<String> filesAndDirs = new FileListFetcher().getFilesAndDirectoriesInDir(logPath);
        List<String> allFilesAndDir = new ArrayList<String>(filesAndDirs.size() + 1);
        allFilesAndDir.addAll(filesAndDirs);

        // make sure that all files in this list are zero byte files
        for (String name : allFilesAndDir) {
            File f = new File(name);
            if (f.isFile() && f.length() == 0) {
                // encountered a non-zero length file, deletes
            	if (f.exists() && f.delete()) {
            		Log.i(TAG, "File: " + name + " removed");
            	}
            }
        }
    }
    
    @Override
    protected void onStop(){
    	super.onStop();
    	cleanup();
    	finish();
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            boolean shouldExitApp;
            synchronized (mIsRecording) {
                shouldExitApp = !mIsRecording;
            }
            
            if (shouldExitApp || !LoggerApplication.isContinuousMode()) {
            	cleanup();
            	cleanup();
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU){
        	mDataDrawer.toggle();
        	return super.onKeyDown(KeyEvent.KEYCODE_MENU, event);
        }
        
        return super.onKeyDown(keyCode, event);
    }
    
   
}

