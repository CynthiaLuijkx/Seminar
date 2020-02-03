
public class ReserveDutyType 
{
	private final String dayType;
	private final String type;
	private final int startTime;
	private final int endTime;
	private final double approximateSize;
	
	public ReserveDutyType(String dayType, String type, int startTime, int endTime, double approximateSize) {
		this.dayType = dayType;
		this.type = type;
		this.startTime = startTime;
		this.endTime = endTime;
		this.approximateSize = approximateSize;
	}

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
