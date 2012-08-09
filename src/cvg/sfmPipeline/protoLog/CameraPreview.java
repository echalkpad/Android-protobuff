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
import android.hardware.Camera;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.IOException;

// New class to handle continous picture grabing 
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    public static final String TAG = "Logger::camera";
    
    private SurfaceHolder holder;
    private Camera mCamera;   
    public  Context contextP;
    
    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        contextP = context;
        holder = getHolder();
        holder.addCallback(this);
        
    }
    
    public Camera getCamera(){
    	return mCamera;

    }

    public void surfaceCreated(SurfaceHolder holder) {
        acquireCamera(holder);
//        TODO
//        Camera.Parameters params = mCamera.getParameters();
//        float height = (float)params.getPreviewSize().height;
//        float width = (float)params.getPreviewSize().width;
//        float ratio = width / height;
//        FrameLayout trueview = (FrameLayout)((View)getParent()).findViewById(R.id.videoCont);
//		ViewGroup.LayoutParams params2 = trueview.getLayoutParams();
//		params2.height = height;
//		params2.width = width;
//		trueview.setLayoutParams(params2);
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.startPreview();
    }


    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }
    
    public void acquireCamera(SurfaceHolder holder) {
        mCamera = Camera.open(LoggerApplication.getCam());
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.setDisplayOrientation(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void releaseCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
            holder.removeCallback(this);
            holder = null;
        }
    }
    
    public void changeCamera(){
    	mCamera.stopPreview();
    	mCamera.release();
    	mCamera = null;
    	acquireCamera(holder);
    	mCamera.startPreview();
    }

}
