import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * This class stores the instance of the crew scheduling problem.
 * It stores the duties, the reserve duty types, contract groups, violations of some constraints and the set M used in the MIP of Phase 1.
 * @author Mette Wagenvoort
 *
 */
public class Instance 
{
	private final Set<Duty> workingDays;
	private final Set<Duty> saturday;
	private final Set<Duty> sunday;
	
	private final HashMap<String, Set<Duty>> dutiesPerType;
	private final HashMap<String, Set<Duty>> dutiesPerTypeW;
	private final HashMap<String, Set<Duty>> dutiesPerTypeSat;
	private final HashMap<String, Set<Duty>> dutiesPerTypeSun;
	
	private final Set<ContractGroup> contractGroups;
	private final Set<ReserveDutyType> reserveDutyTypes;
	
	private final Set<Violation> violations11;
	private final Set<Violation> violations32;
	
	private final Set<Combination> M;					// A Set with combinations of day type, duty type and the number of times this shift should be added
	
	/**
	 * Constructs an Instance.
	 * @param workingDays			a Set containing all the duties on a working day
	 * @param saturday				a Set containing all the duties on a saturday
	 * @param sunday				a Set containing all the duties on a sunday
	 * @param dutiesPerType			a Map that links the different duty types to the duties of that type
	 * @param dutiesPerTypeW		a Map that links the different duty types to the duties of that type for a workingday
	 * @param dutiesPerTypeSat		a Map that links the different duty types to the duties of that type for a saturday
	 * @param dutiesPerTypeSun		a Map that links the different duty types to the duties of that type for a sunday
	 * @param contractGroups		a Set of contract groups
	 * @param reserveDutyTypes		a Set of reserve duty types
	 * @param violations11			a Set containing all violations of the daily rest of 11 hours
	 * @param violations32			a Set containing all violations of the rest day of 32 hours
	 */
	public Instance(Set<Duty> workingDays, Set<Duty> saturday, Set<Duty> sunday, HashMap<String, Set<Duty>> dutiesPerType, 
			HashMap<String, Set<Duty>> dutiesPerTypeW,  HashMap<String, Set<Duty>> dutiesPerTypeSat,  HashMap<String, Set<Duty>> dutiesPerTypeSun,
			Set<ContractGroup> contractGroups, Set<ReserveDutyType> reserveDutyTypes, Set<Violation> violations11, Set<Violation> violations32) {
		this.workingDays = workingDays;
		this.saturday = saturday;
		this.sunday = sunday;
		this.dutiesPerType = dutiesPerType;
		this.dutiesPerTypeW = dutiesPerTypeW;
		this.dutiesPerTypeSat = dutiesPerTypeSat;
		this.dutiesPerTypeSun = dutiesPerTypeSun;
		this.contractGroups = contractGroups;
		this.reserveDutyTypes = reserveDutyTypes;
		this.violations11 = violations11;
		this.violations32 = violations32;
		
		this.M = new HashSet<>();
		String[] workDays = new String[] {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
		for (String day : workDays) {
			for (String dutyType : this.dutiesPerTypeW.keySet()) {
				M.add(new Combination(day, dutyType, this.dutiesPerTypeW.get(dutyType).size()));
			}
			M.add(new Combination(day, "ATV", 0));
		}
		for (String dutyType : this.dutiesPerTypeSat.keySet()) {
			M.add(new Combination("Saturday", dutyType, this.dutiesPerTypeSat.get(dutyType).size()));
		}
		M.add(new Combination("Saturday", "ATV", 0));
		for (String dutyType : this.dutiesPerTypeSun.keySet()) {
			M.add(new Combination("Sunday", dutyType, this.dutiesPerTypeSun.get(dutyType).size()));
		}
		M.add(new Combination("Sunday", "ATV", 0));
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

	public HashMap<String, Set<Duty>> getDutiesPerTypeW() {
		return dutiesPerTypeW;
	}

	public HashMap<String, Set<Duty>> getDutiesPerTypeSat() {
		return dutiesPerTypeSat;
	}

	public HashMap<String, Set<Duty>> getDutiesPerTypeSun() {
		return dutiesPerTypeSun;
	}

	public Set<ContractGroup> getContractGroups() {
		return contractGroups;
	}
	
	public Set<ReserveDutyType> getReserveDutyTypes() {
		return reserveDutyTypes;
	}
	
	public Set<Violation> getViolations11() {
		return violations11;
	}
	
	public Set<Violation> getViolations32() {
		return violations32;
	}
	
	public Set<Combination> getM() {
		return M;
	}
}
