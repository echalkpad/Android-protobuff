package cvg.sfmPipeline.protoLog;

import java.io.IOException;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class CameraThread extends AsyncTask<Long, Integer, Void>{
	private static final String TAG = "ProtoLog::CamThread";
	
	private Camera mCamera;
    private long mDelay;
    
	public CameraThread(CameraPreview cam) {
		mCamera = cam.getCamera();
	}
	
	@Override
	protected void onPreExecute(){
	}
	
	@Override
	protected Void doInBackground(Long... params) {
		mDelay = params[0];
		if(mDelay > 0){// continuous mode
			mDelay = Math.max(params[0], 150l);
			while(!isCancelled()){
				try {
					
		            Thread.sleep(mDelay);
		            if(mCamera == null){
		            	Log.i(TAG, "camera is null");
		            	break;
		            }
		            if(LoggerApplication.ready2snap){
		            	LoggerApplication.ready2snap = false;
			            mCamera.takePicture(null, null, new PictureCallback() {
			                @Override
			                public void onPictureTaken(byte[] data, Camera camera) {
			                	// since this thread is the slowest and holds the most
			                	// valuable information, this will call the writeout at update
			                	LoggerApplication.setImage(data);
			                	publishProgress(0);
			                }
			            });
		            }
		        } catch (InterruptedException e) {
		            Log.w(TAG, "Thread cancelled");
		        }
			}
		}else{ // non auto mode
			if(mCamera == null){
				Log.i(TAG, "camera is null");
				return null;
			}
			if(LoggerApplication.ready2snap){
				LoggerApplication.ready2snap = false;
				mCamera.takePicture(null, null, new PictureCallback() {
					@Override
					public void onPictureTaken(byte[] data, Camera camera) {
						LoggerApplication.setImage(data);
						publishProgress(1);
					}
				});
			}
		}
		return null;
	}

	@Override
	protected void onProgressUpdate(Integer... command){
		// runs in UI thread
		if(command[0] == 1){
				playSound();
		}
		LoggerApplication.ready2snap = true;
		LoggerApplication.noFrames++;
		if (LoggerApplication.isStoreInSD())
			ProtoWriter.writeMessage();
		else{
			ProtoWriter.buildMessage();
			new UDPsend().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
		}
		mCamera.startPreview();
	}


	public void playSound(){
		final ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 60);
	     tg.startTone(ToneGenerator.TONE_PROP_BEEP);
	}
}
