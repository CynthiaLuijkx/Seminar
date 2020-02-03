import java.util.Set;

public class ContractGroup 
{
	private final int nr;
	private final int avgDaysPerWeek;
	private final double avgHoursPerDay;
	private final int ATVPerYear;
	private final double relativeGroupSize;
	private final Set<String> dutyTypes;
	
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
