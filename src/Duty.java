
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
