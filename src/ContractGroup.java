import java.util.Set;

/**
 * This class consists of information on a contract group.
 * @author Mette Wagenvoort
 *
 */
public class ContractGroup 
{
	private final int nr;
	private final int avgDaysPerWeek;
	private final double avgHoursPerDay;
	private final int ATVPerYear;
	private final double relativeGroupSize;
	private final Set<String> dutyTypes;
	
	private int ATVc;								// Number of ATV days required for the planning horizon
	private int Tc;
	
	/**
	 * Constructs a ContractGroup
	 * @param nr						the contract group nr corresponding with the excel data sheet
	 * @param avgDaysPerWeek			the average number of days per week someone from this contract group works
	 * @param avgHoursPerDay			the average number of hours per week someone from this contract group works
	 * @param ATVPerYear				the number of ATV days per year someone from this contract group should receive
	 * @param relativeGroupSize			the relative group size of this contract group to all employees at this depot
	 * @param dutyTypes					the duty types that someone from this contract group can serve
	 */
	public ContractGroup(int nr, int avgDaysPerWeek, double avgHoursPerDay, int ATVPerYear, double relativeGroupSize, Set<String> dutyTypes) {
		this.nr = nr;
		this.avgDaysPerWeek = avgDaysPerWeek;
		this.avgHoursPerDay = avgHoursPerDay;
		this.ATVPerYear = ATVPerYear;
		this.relativeGroupSize = relativeGroupSize;
		this.dutyTypes = dutyTypes;
	}

	public int getNr() {
		return nr;
	}

	public int getAvgDaysPerWeek() {
		return avgDaysPerWeek;
	}

	public double getAvgHoursPerDay() {
		return avgHoursPerDay;
	}

	public int getATVPerYear() {
		return ATVPerYear;
	}
	
	public double getRelativeGroupSize() {
		return relativeGroupSize;
	}

	public Set<String> getDutyTypes() {
		return dutyTypes;
	}
	
	public int getATVc() {
		return ATVc;
	}
	
	public void setATVc(int ATVc) {
		this.ATVc = ATVc;
	}
	
	public int getTc() {
		return Tc;
	}
	
	public void setTc(int Tc) {
		this.Tc = Tc;
	}

	@Override
	public String toString() {
		return "ContractGroup [nr=" + nr + ", avgDaysPerWeek=" + avgDaysPerWeek + ", avgHoursPerDay=" + avgHoursPerDay
				+ ", ATVPerYear=" + ATVPerYear + ", dutyTypes=" + dutyTypes + "]";
	}
	
	public String groupNumberToString() {
		return "ContractGroup" + nr;
	}
}
