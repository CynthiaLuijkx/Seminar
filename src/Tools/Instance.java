package Tools;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
	private final HashMap<Integer, Duty> fromDutyNrToDuty;
	
	private final Set<ContractGroup> contractGroups;
	private final Set<ReserveDutyType> reserveDutyTypes;
	private final HashMap<Integer, ReserveDutyType> fromRDutyNrToRDuty;
	
	private Set<Violation> violations11;
	private Set<Violation> violations32;
	private Set<Violation3Days> violations3Days;
	
	private Set<Combination> M;					// A Set with combinations of day type, duty type and the number of times this shift should be added
	
	private int UB = 0; 
	private int LB = 0;
	
	private int nDrivers;
	
	private Map<ContractGroup, String[]> basicSchedules;
	
	private final Map<String, Integer> avgMinW;
	private final Map<String, Integer> avgMinSat;
	private final Map<String, Integer> avgMinSun;
	
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
			HashMap<Integer, Duty> fromDutyNrToDuty, Set<ContractGroup> contractGroups, Set<ReserveDutyType> reserveDutyTypes, HashMap<Integer, ReserveDutyType> fromRDutyNrToRDuty, Set<Violation> violations11, Set<Violation> violations32) {
		this.workingDays = workingDays;
		this.saturday = saturday;
		this.sunday = sunday;
		this.dutiesPerType = dutiesPerType;
		this.dutiesPerTypeW = dutiesPerTypeW;
		this.dutiesPerTypeSat = dutiesPerTypeSat;
		this.dutiesPerTypeSun = dutiesPerTypeSun;
		this.fromDutyNrToDuty = fromDutyNrToDuty;
		this.fromRDutyNrToRDuty = fromRDutyNrToRDuty;
		this.contractGroups = contractGroups;
		this.reserveDutyTypes = reserveDutyTypes;
		this.violations11 = violations11;
		this.violations32 = violations32;
		this.avgMinW = new HashMap<>();
		this.avgMinSat = new HashMap<>();
		this.avgMinSun = new HashMap<>();
		this.calculateAverages();
		
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
		
		this.calculateBounds();
	}

	public Map<String, Integer> getAvgMinW() {
		return avgMinW;
	}

	public Map<String, Integer> getAvgMinSat() {
		return avgMinSat;
	}

	public Map<String, Integer> getAvgMinSun() {
		return avgMinSun;
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
	public HashMap<Integer, Duty> getFromDutyNrToDuty(){
		return fromDutyNrToDuty;
	}
	public HashMap<Integer, ReserveDutyType> getFromRDutyNrToRDuty(){
		return fromRDutyNrToRDuty;
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
	
	/**
	 * Calculates the lower and upper bound for the number of drivers
	 */
	public void calculateBounds() {
		int nDutiesW = this.workingDays.size(); 
		int nDutiesSat = this.saturday.size(); 
		int nDutiesSun = this.sunday.size(); 
		int nReserveDuties =0; 
		double scale = 4/3.0; 
		
		for(ReserveDutyType duty: this.reserveDutyTypes) {
			if(duty.getDayType().equals("Workingday")) {
				int temp = (int) Math.ceil(scale*duty.getApproximateSize()*nDutiesW);
				nReserveDuties += temp*5;
				String[] workDays = new String[] {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};
				for (String day : workDays) {
					M.add(new Combination(day, "R" + duty.getType(), temp));
					duty.setAmount(temp);
				}
			} else if(duty.getDayType().equals("Saturday")){
				int temp = (int) Math.ceil(scale* duty.getApproximateSize()*nDutiesSat);
				nReserveDuties += temp;
				M.add(new Combination("Saturday", "R" + duty.getType(), temp));
				duty.setAmount(temp);
			}else {
				int temp = (int) Math.ceil(scale* duty.getApproximateSize()*nDutiesSun);
				nReserveDuties +=  temp;
				M.add(new Combination("Sunday", "R" + duty.getType(), temp));
				duty.setAmount(temp);
			}
		}
		
		int totalnDuties = nDutiesW*5 + nDutiesSat + nDutiesSun + nReserveDuties; 
		this.UB =  (int) Math.ceil(totalnDuties/3.0); 
		this.LB = (int) Math.ceil(totalnDuties/6); 
	}
	
	public int getUB() {
		return this.UB; 
	}
	
	public int getLB() {
		return this.LB; 
	}
	
	public Map<ContractGroup, String[]> getBasicSchedules() {
		return this.basicSchedules;
	}
	
	public void setBasicSchedules(Map<ContractGroup, String[]> basicSchedules) {
		this.basicSchedules = basicSchedules;
	}
	
	/**
	 * This method initialises the number of drivers in the instance and sets for each contract group:
	 * 	- Tc = nDrivers * relativeGroupSize rounded up multiplied by 7
	 * 	- ATVc = ATV days per year / 365.0 * Tc
	 * @param nDrivers				the number of drivers
	 */
	public void setNrDrivers(int nDrivers) {
		this.nDrivers = nDrivers;
		
		for (ContractGroup c : this.contractGroups) {
			
			c.setTc((int) Math.floor(nDrivers * c.getRelativeGroupSize()) * 7);
			c.setATVc((int) Math.floor(c.getATVPerYear() / 365.0 * c.getTc()));
		}
	}
	
	public void setViol(Set<Violation> violations11, Set<Violation> violations32, Set<Violation3Days> violations3Days) {
		this.violations11 = violations11; 
		this.violations32 = violations32; 
		this.violations3Days = violations3Days;
	}

	public Set<Violation3Days> getViolations3Days() {
		return violations3Days;
	}
	
	public void calculateAverages() {
		for(String dutyType : this.dutiesPerTypeW.keySet()) {
			int totalMin = 0;
			for(Duty duty : this.dutiesPerTypeW.get(dutyType)) {
				totalMin = totalMin + duty.getPaidMin();
			}
			int averageMin = (int) Math.ceil(totalMin/this.dutiesPerTypeW.get(dutyType).size());
			this.avgMinW.put(dutyType, averageMin);
		}
		for(String dutyType : this.dutiesPerTypeSat.keySet()) {
			int totalMin = 0;
			for(Duty duty : this.dutiesPerTypeSat.get(dutyType)) {
				totalMin = totalMin + duty.getPaidMin();
			}
			int averageMin = (int) Math.ceil(totalMin/this.dutiesPerTypeSat.get(dutyType).size());
			this.avgMinSat.put(dutyType, averageMin);
		}
		
		for(String dutyType : this.dutiesPerTypeSun.keySet()) {
			int totalMin = 0;
			for(Duty duty : this.dutiesPerTypeSun.get(dutyType)) {
				totalMin = totalMin + duty.getPaidMin();
			}
			int averageMin = (int) Math.ceil(totalMin/this.dutiesPerTypeSun.get(dutyType).size());
			this.avgMinSun.put(dutyType, averageMin);
		}
	}
}
