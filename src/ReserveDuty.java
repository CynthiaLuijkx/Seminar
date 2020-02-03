//Class that has a type Reserve Duty
public class ReserveDuty 
{
	private final String dayType; //day type on which the reserve duty should be scheduled
	private final String type; //type of reserve duty (for instance, Early/Day/Late etc.)
	private final int startTime; //start time of the reserve duty
	private final int endTime; //end time of the reserve duty
	private final double approximateSize; //the approximate size of the reserve duties of a certain type on a certain day type
	
	//Constructor of the class
	public ReserveDuty(String dayType, String type, int startTime, int endTime, double approximateSize) {
		this.dayType = dayType;
		this.type = type;
		this.startTime = startTime;
		this.endTime = endTime;
		this.approximateSize = approximateSize;
	}
	//return methods
	public String getDayType() {
		return dayType;
	}

	public String getType() {
		return type;
	}

	public int getStartTime() {
		return startTime;
	}

	public int getEndTime() {
		return endTime;
	}

	public double getApproximateSize() {
		return approximateSize;
	}

	@Override
	public String toString() {
		return "ReserveDuty [dayType=" + dayType + ", type=" + type + ", startTime=" + startTime + ", endTime="
				+ endTime + ", approximateSize=" + approximateSize + "]";
	}
}
