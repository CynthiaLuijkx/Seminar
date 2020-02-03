import java.util.HashMap;
import java.util.Set;

public class Instance 
{
	private Set<Duty> workingDays;
	private Set<Duty> saturday;
	private Set<Duty> sunday;
	private HashMap<String, Set<Duty>> dutiesPerType;
	private Set<ContractGroup> contractGroups;
	private Set<ReserveDuty> reserveDutyTypes;
	
	public Instance(Set<Duty> workingDays, Set<Duty> saturday, Set<Duty> sunday, HashMap<String, Set<Duty>> dutiesPerType, 
			Set<ContractGroup> contractGroups, Set<ReserveDuty> reserveDutyTypes) {
		this.workingDays = workingDays;
		this.saturday = saturday;
		this.sunday = sunday;
		this.dutiesPerType = dutiesPerType;
		this.contractGroups = contractGroups;
		this.reserveDutyTypes = reserveDutyTypes;
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
}
