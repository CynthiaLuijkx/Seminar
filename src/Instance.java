import java.util.HashMap;
import java.util.Set;

public class Instance 
{
	private final Set<Duty> workingDays;
	private final Set<Duty> saturday;
	private final Set<Duty> sunday;
	private final HashMap<String, Set<Duty>> dutiesPerType;
	private final Set<ContractGroup> contractGroups;
	private final Set<ReserveDuty> reserveDutyTypes;
	private final Set<Violation> violations11;
	private final Set<Violation> violations32;
	
	public Instance(Set<Duty> workingDays, Set<Duty> saturday, Set<Duty> sunday, HashMap<String, Set<Duty>> dutiesPerType, 
			Set<ContractGroup> contractGroups, Set<ReserveDuty> reserveDutyTypes, Set<Violation> violations11, Set<Violation> violations32) {
		this.workingDays = workingDays;
		this.saturday = saturday;
		this.sunday = sunday;
		this.dutiesPerType = dutiesPerType;
		this.contractGroups = contractGroups;
		this.reserveDutyTypes = reserveDutyTypes;
		this.violations11 = violations11;
		this.violations32 = violations32;
	}

	public Set<Duty> getWorkingDays() {
		return workingDays;
	}

	public Set<Duty> getSaturday() {
		return saturday;
	}

	public Set<Duty> getSunday() {
		return sunday;
	}

	public HashMap<String, Set<Duty>> getDutiesPerType() {
		return dutiesPerType;
	}

	public Set<ContractGroup> getContractGroups() {
		return contractGroups;
	}
	
	public Set<ReserveDuty> getReserveDutyTypes() {
		return reserveDutyTypes;
	}
	
	public Set<Violation> getViolations11() {
		return violations11;
	}
	
	public Set<Violation> getViolations32() {
		return violations32;
	}
}
