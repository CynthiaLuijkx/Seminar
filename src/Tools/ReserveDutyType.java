package Tools;

/**
 * This class consists of information on a reserve duty.
 * @author Mette Wagenvoort
 *
 */
public class ReserveDutyType 
{
	private final String dayType;
	private final String type;
	private final int startTime;
	private final int endTime;
	private final double approximateSize;
	private final int nr;
	
	private int amount;
	
	/**
	 * Constructs a reserve duty type.
	 * @param dayType				the day type of the reserve duty : "Workingday", "Saturday", "Sunday"
	 * @param type					the duty type of the reserve duty : "V", "D", "L", "G", "P" (and possibly "M" and "GM" or "W")
	 * @param startTime				the start time of the reserve duty in minutes
	 * @param endTime				the end time of the reserve duty in minutes (NOTE: a duty can end after midnight so this can exceed 24 * 60)
	 * @param approximateSize		the approximate size of the reserve duty group of this type relative to the 
	 */
	public ReserveDutyType(String dayType, String type, int startTime, int endTime, double approximateSize, int nr) {
		this.dayType = dayType;
		this.type = type;
		this.startTime = startTime;
		this.endTime = endTime;
		this.approximateSize = approximateSize;
		this.nr = nr;
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
	
	public int getNr() {
		return nr;
	}
	
	public int getAmount() {
		return amount;
	}
	
	public void setAmount(int amount) {
		this.amount = amount;
	}

	@Override
	public String toString() {
		return "ReserveDutyType [dayType=" + dayType + ", type=" + type + ", startTime=" + startTime + ", endTime="
				+ endTime + ", approximateSize=" + approximateSize + ", nr=" + nr + "]";
	}
}
