package cvg.sfmPipeline.protoLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;

import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;

public class Preferences extends PreferenceActivity{
	
	// Non-fragment based deprecated implementation. FIXME
	
	private boolean calibOK = false;
	private static TextView mMat_1_1;
    private static TextView mMat_1_2;
    private static TextView mMat_1_3;
    private static TextView mMat_2_1;
    private static TextView mMat_2_2;
    private static TextView mMat_2_3;
    private static TextView mMat_3_1;
    private static TextView mMat_3_2;
    private static TextView mMat_3_3;
    
    private static TextView mMati_1_1;
    private static TextView mMati_1_2;
    private static TextView mMati_1_3;
    private static TextView mMati_2_1;
    private static TextView mMati_2_2;
    private static TextView mMati_2_3;
    private static TextView mMati_3_1;
    private static TextView mMati_3_2;
    private static TextView mMati_3_3;
	
    
    
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
	
		addPreferencesFromResource(R.layout.mainmenu);
		Preference buttonLoad = (Preference)findPreference("buttonLoad");
		Preference buttonCalib = (Preference)findPreference("buttonCalib");
		Preference buttonRunLocal = (Preference)findPreference("buttonRunLocal");
		Preference buttonRunRemote = (Preference)findPreference("buttonRunRemote");
		Preference buttonView = (Preference) findPreference("buttonViewCalibs");
		
		buttonView.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if(calibOK){
					AlertDialog.Builder builder = new AlertDialog.Builder(preference.getContext());
					View newlay = Preferences.this.getLayoutInflater().inflate(R.layout.viewmats, null);
					mMat_1_1 = (TextView) newlay.findViewById(R.id.matrix_1_1);
					mMat_1_2 = (TextView) newlay.findViewById(R.id.matrix_1_2);
					mMat_1_3 = (TextView) newlay.findViewById(R.id.matrix_1_3);
					mMat_2_1 = (TextView) newlay.findViewById(R.id.matrix_2_1);
					mMat_2_2 = (TextView) newlay.findViewById(R.id.matrix_2_2);
					mMat_2_3 = (TextView) newlay.findViewById(R.id.matrix_2_3);
					mMat_3_1 = (TextView) newlay.findViewById(R.id.matrix_3_1);
					mMat_3_2 = (TextView) newlay.findViewById(R.id.matrix_3_2);
					mMat_3_3 = (TextView) newlay.findViewById(R.id.matrix_3_3);

					mMati_1_1 = (TextView) newlay.findViewById(R.id.imatrix_1_1);
					mMati_1_2 = (TextView) newlay.findViewById(R.id.imatrix_1_2);
					mMati_1_3 = (TextView) newlay.findViewById(R.id.imatrix_1_3);
					mMati_2_1 = (TextView) newlay.findViewById(R.id.imatrix_2_1);
					mMati_2_2 = (TextView) newlay.findViewById(R.id.imatrix_2_2);
					mMati_2_3 = (TextView) newlay.findViewById(R.id.imatrix_2_3);
					mMati_3_1 = (TextView) newlay.findViewById(R.id.imatrix_3_1);
					mMati_3_2 = (TextView) newlay.findViewById(R.id.imatrix_3_2);
					mMati_3_3 = (TextView) newlay.findViewById(R.id.imatrix_3_3);
					builder.setView(newlay);
					builder.create().show();
					putMatrixData(LoggerApplication.getCamera2body(), LoggerApplication.getCameraMatrix());
				}
				else
					Toast.makeText(getApplicationContext(),
			                "Calibration data not found. Load calibration matrix or calibrate first!",
			                Toast.LENGTH_LONG).show();
				return false;
			}
		});
		
		buttonLoad.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				calibOK = loadCalib();
				return false;
			}
		});
		
		buttonCalib.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				preference.getContext().getPackageManager();
				Intent intent = new Intent(Intent.ACTION_MAIN);
				PackageManager manager = preference.getContext().getPackageManager();
				intent = manager.getLaunchIntentForPackage("cvg.sfmPipeline.calibration");
				if(intent == null)
					Toast.makeText(getApplicationContext(),
			                "Please install Calibration app first!",
			                Toast.LENGTH_LONG).show();
				else{
					intent.addCategory("android.intent.category.LAUNCHER");
					startActivity(intent);
				}
				return false;
			}
		});
		
		buttonRunLocal.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if(calibOK){
					Intent newi = new Intent(preference.getContext(), LoggerActivity.class);
					newi.putExtra("remoteMode", false);
					startActivity(newi);
				}else{
					Toast.makeText(getApplicationContext(),
			                "Calibration data not found. Load calibration matrix or calibrate first!",
			                Toast.LENGTH_LONG).show();
				}
				return false;
			}
		});
		
		buttonRunRemote.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				if(calibOK){
					Intent newi = new Intent(preference.getContext(), LoggerActivity.class);
					newi.putExtra("remoteMode", true);
					startActivity(newi);
				}else{
					Toast.makeText(getApplicationContext(),
			                "Calibration data not found. Load calibration matrix or calibrate first!",
			                Toast.LENGTH_LONG).show();
				}
				return false;
			}
		});
		
	}
	
	
	protected void putMatrixData(float[] camera2body, float[] cameraMatrix) {
		putMatrixDatahelp(0, camera2body);
		putMatrixDatahelp(1, cameraMatrix);
	}
	
	public static void putMatrixDatahelp(int type, float[] vals){
		// type is 0 for rotation and 1 for intrisnics matrix
		if (type == 0){
			mMat_1_1.setText(numberDisplayFormatter(vals[0]));
			mMat_1_2.setText(numberDisplayFormatter(vals[1]));
			mMat_1_3.setText(numberDisplayFormatter(vals[2]));
			mMat_2_1.setText(numberDisplayFormatter(vals[3])); 
			mMat_2_2.setText(numberDisplayFormatter(vals[4]));
			mMat_2_3.setText(numberDisplayFormatter(vals[5]));
			mMat_3_1.setText(numberDisplayFormatter(vals[6]));
			mMat_3_2.setText(numberDisplayFormatter(vals[7]));
			mMat_3_3.setText(numberDisplayFormatter(vals[8]));
		}else{
			mMati_1_1.setText(numberDisplayFormatter(vals[0]));
			mMati_1_2.setText(numberDisplayFormatter(vals[1]));
			mMati_1_3.setText(numberDisplayFormatter(vals[2]));
			mMati_2_1.setText(numberDisplayFormatter(vals[3])); 
			mMati_2_2.setText(numberDisplayFormatter(vals[4]));
			mMati_2_3.setText(numberDisplayFormatter(vals[5]));
			mMati_3_1.setText(numberDisplayFormatter(vals[6]));
			mMati_3_2.setText(numberDisplayFormatter(vals[7]));
			mMati_3_3.setText(numberDisplayFormatter(vals[8]));
		}
	}
	
	private static String numberDisplayFormatter(float value) {
        String displayedText = String.format("%.3f", value);
        if (value >= 0) {
            displayedText = " " + displayedText;
        }
        if (displayedText.length() > 6) {
            displayedText = displayedText.substring(0, 6);
        }
        while (displayedText.length() < 6) {
            displayedText = displayedText + " ";
        }
        return displayedText;
    }

	@Override
	protected void onResume() {
		calibOK = loadCalib();
		LoggerApplication.setDefaults();
		super.onRestart();
	}


	private boolean loadCalib(){
		String path = Environment.getExternalStorageDirectory().getPath()
        		+ "/CalibrationData/";
		File fileK = new File(path + "camMatrix.dat");
		File fileC2B = new File(path + "rotCam2imu.dat");
		float[] K = {0,0,0,0,0,0,0,0,0};
		float[] C2B = {0,0,0,0,0,0,0,0,0};
		if (fileK.exists() && fileC2B.exists()){
			try {
				FileInputStream fisK = new FileInputStream(fileK);
				FileInputStream fisC2B = new FileInputStream(fileC2B);
				
				ObjectInputStream oiiK 	 = new ObjectInputStream(fisK);
				ObjectInputStream oiiC2B = new ObjectInputStream(fisC2B);
				for(int i = 0; i < 9; i ++)
					K[i] = (float)oiiK.readDouble();
				for(int i = 0; i < 9; i ++)
					C2B[i] = (float)oiiC2B.readDouble();
				fisK.close();
				fisC2B.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			LoggerApplication.setCameraMatrix(K);
			LoggerApplication.setCamera2body(C2B);
			Toast.makeText(getApplicationContext(),
	                "Calibration data successfully loaded!",
	                Toast.LENGTH_LONG).show();
			return true;
		}
		else{
			Toast.makeText(getApplicationContext(),
	                "Calibration data not found. Perform application first!",
	                Toast.LENGTH_LONG).show();
			return false;
		}
	}
	
	
	
	@Override
	public void onBackPressed() {
		Log.e("back", "tsback");
		android.os.Process.killProcess(android.os.Process.myPid());
		super.onBackPressed();
	}



//	@Override
//    public boolean onKeyDown(int keyCode, KeyEvent event) {
//        if (keyCode == KeyEvent.KEYCODE_BACK) {
//        	
//            finish();
//            return true;
//        }
//        
//        return super.onKeyDown(keyCode, event);
//    }
	
}
