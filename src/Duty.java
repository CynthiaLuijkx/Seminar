
// Class of type Duty
 

public class Duty 
{
	private final int nr; //number of the duty
	private final String type; //type of the duty (for instance, Early/Day/Late etc.)
	private final int startTime; //start time of the duty
	private final int endTime; //end time of the duty
	private final int paidMin; //amount of minutes the drivers get paid
	private final int workingMin; //amount of minutes the drivers work
	private final String dayType; //day type on which the duty needs to be executed (Saturday, Sunday, Working day)
	private final int nightMin; //amount of minutes the driver works during the night
	
	//Constructor of the class Duty
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

	//Return methods
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
