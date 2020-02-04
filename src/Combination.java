
/**
 * This class consists of a Combination of a day and duty type and contains the number of times it should be in the basic schedule.
 * @author Mette Wagenvoort
 *
 */
public class Combination 
{
	private final String dayType;
	private final String type;
	private final int n;
	
	/**
	 * Constructs a Combination.
	 * @param dayType		the day type : "Workingday" (all working days separately) , "Saturday", "Sunday"
	 * @param type			the duty type : "V", "D", "L", "P", "G", "M", "GM", "W"
	 * @param n				the number of times this dayType - dutyType combination should be in the basic schedule
	 */
	public Combination(String dayType, String type, int n) {
		this.dayType = dayType;
		this.type = type;
		this.n = n;
	}

	public String getDayType() {
		return dayType;
	}

	public String getType() {
		return type;
	}

	public int getN() {
		return n;
	}

	@Override
	public String toString() {
		return "Combination [dayType=" + dayType + ", type=" + type + ", n=" + n + "]";
	}
}
