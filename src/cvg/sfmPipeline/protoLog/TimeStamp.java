package cvg.sfmPipeline.protoLog;


// static class to keep track of the timestamp across all the logs

public final class TimeStamp {
	private static long timestamp;
	private static long startime;
	private static boolean started = false;
	
	private TimeStamp(){}
	
	/**Get the latest sensor timestamp in microseconds
	 * 
	 * */
	public static long getTime(){
		
		return timestamp/1000; // return microseconds
	}
	
	/**
	 * Update the timestamp field using the latest sensor timestamp acquired. 
	 * */
	public static void updateTime(long t){
		if (started){
			timestamp = t - startime;
		}else{
			startime = t;
			timestamp = 0;
			started = true;
		}
	}
	
	public static void restart(){
		started = false;
	}
}
