package cvg.sfmPipeline.protoLog;

import android.hardware.Sensor;

public class SensorProperty {
	
	private String prefix;
	private String units;
	private int numFields = 3;
	
    @SuppressWarnings("deprecation")
	public SensorProperty(Sensor sensor){
    	int sensorInt = sensor.getType();
    	switch (sensorInt) {
    	case Sensor.TYPE_LINEAR_ACCELERATION:
    			prefix = "LinAcc";
    			units = "m_s2";
    			break;
    		case Sensor.TYPE_ACCELEROMETER:
				prefix = "Acc";
				units = "m_s2";
				break;
			case Sensor.TYPE_MAGNETIC_FIELD:
				prefix = "Magnt";
				units = "uT";
				break;
			case Sensor.TYPE_GYROSCOPE:
				prefix = "Gyro";
				units = "rads_S";
				break;
			case Sensor.TYPE_ROTATION_VECTOR:
				prefix = "RotV";
				units = "";
				numFields = 4;
				break;
			case Sensor.TYPE_ORIENTATION:
				prefix = "Orien";
				units = "deg";
				break;
			case Sensor.TYPE_GRAVITY:
				prefix = "Grav";
				units = "m_s2";
				break;
			case Sensor.TYPE_PRESSURE:
				prefix = "Press";
				units = "hPa";
				numFields = 1;
				break;
			default:
				prefix = "Unk";
				units = "";
				break;
		}
    }
    
    public String get(){
    	String str;
    	switch (numFields){
	    	case 3:
	    		str = String.format("t%s[uS], x%s[%s], y%s[%s], z%s[%s],\n", prefix, prefix, units, prefix, units, prefix, units );
	    		break;
	    	case 4:
	    		str = String.format("t%s[uS], xsin(th_2), ysin(th_2), zsin(th_2),\n", prefix );
	    		break;
	    	case 1:
	    		str = String.format("t%s[uS], %s[%s],unk,unk \n", prefix, prefix, units );
	    	default:
	    		str = String.format("t%s[uS], x%s[%s], y%s[%s], z%s[%s],\n", prefix, prefix, units, prefix, units, prefix, units );
	    		break;
    	}
    	return str;
    }
}
    

