import java.util.HashMap;
import java.util.Set;

//Class that contains all information we have as input or information we have determined
public class Instance 
{
	private final Set<Duty> workingDays; //set that contains all duties that need to be scheduled on a working day
	private final Set<Duty> saturday; //set that contains all duties that need to be scheduled on a Saturday
	private final Set<Duty> sunday; //set that contains all duties that need to be scheduled on a Sunday
	private final HashMap<String, Set<Duty>> dutiesPerType; //for every duty type, we have a set that contains all duties of that type
	private final Set<ContractGroup> contractGroups; //set that contains all contract groups
	private final Set<ReserveDuty> reserveDutyTypes; // set that contains all reserve duties
	private final Set<Violation> violations11; //set that contains all combinations of day types where a late duty followed by an early duty violates the 11 hours rest constraint
	private final Set<Violation> violations32; //set that contains all combinations of day types where a late duty followed by a rest day and an early duty violates the 32 hours rest constraint

	//Constructor of class
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
	//Return methods
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
