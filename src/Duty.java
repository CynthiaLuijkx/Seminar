
/**
 * This class consists of information on a duty; the number of the duty, its day and duty type and information on the timing of the duty.
 * @author Mette Wagenvoort
 *
 */
public class Duty 
{
	private final int nr;
	private final String type;
	private final int startTime;
	private final int endTime;
	private final int paidMin;
	private final int workingMin;
	private final String dayType;
	private final int nightMin;
	
	/**
	 * Constructs a Duty.
	 * @param nr					the duty nr corresponding to the excel data sheet
	 * @param type					the duty type : "V", "D", "L", "P", "G" (and possibly "M" and "GM" or "W")
	 * @param startTime				the start time of the duty in minutes
	 * @param endTime				the end time of the duty in minutes (NOTE: a duty can end after midnight, so this can exceed 24 * 60)
	 * @param paidMin				the number of minutes paid for this shift
	 * @param workingMin			the number of minutes worked on this shift
	 * @param dayType				the day type of this shift : "Workingday", "Saturday", "Sunday"
	 * @param nightMin				the number of minutes worked between 00:00 and 06:00
	 */
	public Duty(int nr, String type, int startTime, int endTime, int paidMin, int workingMin, String dayType, int nightMin) {
		this.nr = nr;
		this.type = type;
		this.startTime = startTime;
		this.endTime = endTime;
		this.paidMin = paidMin;
		this.workingMin = workingMin;
		this.dayType = dayType;
		this.nightMin = nightMin;
	}

	public int getNr() {
		return nr;
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

	public int getPaidMin() {
		return paidMin;
	}

	public int getWorkingMin() {
		return workingMin;
	}

	public String getDayType() {
		return dayType;
	}

	public int getNightMin() {
		return nightMin;
	}

	@Override
	public String toString() {
		return "Duty [nr=" + nr + ", type=" + type + ", startTime=" + startTime + ", endTime=" + endTime + ", paidMin="
				+ paidMin + ", workingMin=" + workingMin + ", dayType=" + dayType + ", nightMin=" + nightMin + "]";
	}
}
