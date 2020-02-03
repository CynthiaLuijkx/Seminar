import java.util.Set;

//Class that uses a type Contract Group
public class ContractGroup 
{
	private final int nr; //number of the contract group type
	private final int avgDaysPerWeek; //the average number of days per week for the contract group
	private final double avgHoursPerDay; //the average of hours per day for the contract group
	private final int ATVPerYear; //the number of ATV days per year for the contract group
	private final double relativeGroupSize; //the relative group size of the contract group
	private final Set<String> dutyTypes; //duty types the contract group can execute
	
	//Constructor of the class
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

	@Override
	public String toString() {
		return "ContractGroup [nr=" + nr + ", avgDaysPerWeek=" + avgDaysPerWeek + ", avgHoursPerDay=" + avgHoursPerDay
				+ ", ATVPerYear=" + ATVPerYear + ", dutyTypes=" + dutyTypes + "]";
	}
}
