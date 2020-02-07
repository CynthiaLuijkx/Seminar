import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DetermineViolations {

	private List<String[]> combDutyType = new ArrayList<String[]>(); 
	private List<String[]> combDayType11 = new ArrayList<String[]>();
	private List<String[]> combDayType32 = new ArrayList<String[]>(); 
	private List<String[]> combType = new ArrayList<String[]>(); 
	private String[] typesDuty = new String[] {"L", "V", "D", "G", "GM"}; 
	private String[] weekDays = new String[] {"Workingday", "Saturday", "Sunday"}; 
	private Instance instance; 
	private Set<Violation> violations11; 
	private Set<Violation> violations32; 

	private static int dailyRestMin = 11*60; 
	private static int restDayMin = 32*60; 
	private double violationBound = 0.9; 

	private Map<String, HashMap<String, ReserveDutyType>> rDutyMap = new HashMap<String, HashMap<String, ReserveDutyType>>(); 

	private Map<String, HashMap<String, Set<Duty>>> dutySetMap = new HashMap<String, HashMap<String, Set<Duty>>>(); 

	public DetermineViolations(Instance instance, Set<String> dutyTypes) {
		this.instance = instance; 

		for(String dutyTypeFrom: dutyTypes) {
			for(String dutyTypeTo: dutyTypes) {
				combDutyType.add(new String[] {dutyTypeFrom, dutyTypeTo}); 
			}
		}

		combDayType11.add(new String[] {"Workingday", "Workingday"}); 
		combDayType11.add(new String[] {"Workingday", "Saturday"}); 
		combDayType11.add(new String[] {"Saturday", "Sunday"});
		combDayType11.add(new String[] {"Sunday", "Workingday"}); 

		combDayType32.add(new String[] {"Workingday", "Workingday"}); 
		combDayType32.add(new String[] {"Workingday", "Saturday"});
		combDayType32.add(new String[] {"Workingday", "Sunday"}); 
		combDayType32.add(new String[] {"Saturday", "Workingday"});
		combDayType32.add(new String[] {"Sunday", "Workingday"}); 

		dutySetMap.put("Workingday", instance.getDutiesPerTypeW()); 
		dutySetMap.put("Saturday", instance.getDutiesPerTypeSat()); 
		dutySetMap.put("Sunday", instance.getDutiesPerTypeSun()); 

		combType.add(new String[] {"N", "N"}); 
		combType.add(new String[] {"N", "R"}); 
		combType.add(new String[] {"R", "N"}); 
		combType.add(new String[] {"R", "R"}); 	

		rDutyMap = checkReserveDuties(); 
		violations11 = getViolations(combDayType11, 0);
		violations32 = getViolations(combDayType32, 1);
	}

	
	public Set<Violation> get11Violations(){
		return this.violations11; 
	}

	/**
	 * Return a set of violations of the 32 hours rest 
	 * @return
	 */
	public Set<Violation> get32Violations(){
		return this.violations32; 
	}
	
	/**
	 * Returns a set of all combinations that have a high chance of violating the 11 hours constraint
	 * @return
	 */
	public Set<Violation> getViolations(List<String[]> combDayType11, int viol){
		Set<Violation> violations = new HashSet<Violation>(); 

		for(String[] dutyTypes:combDutyType) {
			for(String[] dayTypes11 : combDayType11) {
				for(String [] dayTypes: combType) {

					boolean reserveFrom = dayTypes[0].equals("R");
					boolean reserveTo= dayTypes[1].equals("R"); 
					String RTo = reserveTo? "R" :"";
					String RFrom = reserveFrom? "R" :""; 
					int[] counts = null; 
					if(!reserveFrom && !reserveTo) {
						if(dutySetMap.get(dayTypes11[0]).containsKey(dutyTypes[0]) && dutySetMap.get(dayTypes11[1]).containsKey(dutyTypes[1])) {
							counts = getViolations(dutySetMap.get(dayTypes11[0]).get(dutyTypes[0]), dutySetMap.get(dayTypes11[1]).get(dutyTypes[1]));
						}
					}else if(!reserveFrom && reserveTo) {
						if(dutySetMap.get(dayTypes11[0]).containsKey(dutyTypes[0]) && rDutyMap.get(dayTypes11[1]).get(RTo+ dutyTypes[1]) != null) {
							counts = getViolations(dutySetMap.get(dayTypes11[0]).get(dutyTypes[0]), rDutyMap.get(dayTypes11[1]).get(RTo +dutyTypes[1]));
						}
					}else if(reserveFrom && reserveTo) {
						if(rDutyMap.get(dayTypes11[0]).get(RFrom + dutyTypes[0]) != null && rDutyMap.get(dayTypes11[1]).get(RTo +dutyTypes[1]) != null) {
							counts = getViolations( rDutyMap.get(dayTypes11[0]).get(RFrom +dutyTypes[0]), rDutyMap.get(dayTypes11[1]).get(RTo +dutyTypes[1]));
						}
					}else if(reserveFrom && !reserveTo) {
						if(rDutyMap.get(dayTypes11[0]).get(RFrom +dutyTypes[0]) != null && dutySetMap.get(dayTypes11[1]).containsKey(dutyTypes[1])) {
							counts = getViolations( rDutyMap.get(dayTypes11[0]).get(RFrom +dutyTypes[0]),  dutySetMap.get(dayTypes11[1]).get(dutyTypes[1]));
						}
					}

					if(counts!= null) {
						if (counts[viol]/((double) counts[2]) >= violationBound) {
							violations.add(new Violation( dutyTypes[0], dayTypes11[0], reserveFrom,  dutyTypes[1], dayTypes11[1], reserveTo));
						}
					}
				}
			}
		}
		return violations; 
	}


	/**
	 * Determines the number of violations between two sets of duties
	 * @param fromDuties			a set of duties
	 * @param toDuties				a set of duties
	 * @param dailyRestMin			the minimum nr of minutes daily rest
	 * @param restDayMin			the minimum nr of minutes for a rest day
	 * @return						an array with the number of daily rest hours violations, number of rest day hours violations, total number of combinations
	 */
	public static int[] getViolations(Set<Duty> fromDuties, Set<Duty> toDuties) {
		int[] counts = new int[3];

		for (Duty from : fromDuties) {
			for (Duty to : toDuties) {
				int gap = 0;
				if (from.getEndTime() <= 24 * 60) {
					gap += 24 * 60 - from.getEndTime();
				} else {
					gap -= from.getEndTime() - 24 * 60;
				}

				gap += to.getStartTime();

				if (gap < dailyRestMin) {
					counts[0]++;
				}
				if (gap + 24 * 60 < restDayMin) {
					counts[1]++;
				}
				counts[2]++;
			}
		}

		return counts;
	}

	/**
	 * Determines whether a violation exists between reserve duties
	 * @param from					the from shift
	 * @param to					the to shift
	 * @param dailyRestMin			the minimum nr of minutes daily rest
	 * @param restDayMin			the minimum nr of minutes for a rest day
	 * @return						an array with the number of daily rest hours violations, number of rest day hours violations, total number of combinations
	 */
	//similar idea
	public static int[] getViolations(ReserveDutyType from, ReserveDutyType to) {
		int[] counts = new int[3];

		int gap = 0;
		if (from.getEndTime() <= 24 * 60) {
			gap += 24 * 60 - from.getEndTime();
		} else {
			gap -= from.getEndTime() - 24 * 60;
		}

		gap += to.getStartTime();

		if (gap < dailyRestMin) {
			counts[0]++;
		}
		if (gap + 24 * 60 < restDayMin) {
			counts[1]++;
		}
		counts[2]++;

		return counts;
	}

	/**
	 * Determines the number of violations between a reserve duty and a set of duties
	 * @param from					the from shift (reserve)
	 * @param toDuties				the set of duties
	 * @param dailyRestMin			the minimum nr of minutes daily rest
	 * @param restDayMin			the minimum nr of minutes for a rest day
	 * @return						an array with the number of daily rest hours violations, number of rest day hours violations, total number of combinations
	 */
	//Similar idea
	public static int[] getViolations(ReserveDutyType from, Set<Duty> toDuties) {
		int[] counts = new int[3];

		for (Duty to : toDuties) {
			int gap = 0;
			if (from.getEndTime() <= 24 * 60) {
				gap += 24 * 60 - from.getEndTime();
			} else {
				gap -= from.getEndTime() - 24 * 60;
			}

			gap += to.getStartTime();

			if (gap < dailyRestMin) {
				counts[0]++;
			}
			if (gap + 24 * 60 < restDayMin) {
				counts[1]++;
			}
			counts[2]++;
		}

		return counts;
	}

	/**
	 * Determines the number of violations between a set of duties and a reserve shift
	 * @param fromDuties			a set of duties
	 * @param to					the to shift (reserve)
	 * @param dailyRestMin			the minimum nr of minutes daily rest
	 * @param restDayMin			the minimum nr of minutes for a rest day
	 * @return						an array with the number of daily rest hours violations, number of rest day hours violations, total number of combinations
	 */
	public static int[] getViolations(Set<Duty> fromDuties, ReserveDutyType to) {
		int[] counts = new int[3];

		for (Duty from : fromDuties) {
			int gap = 0;
			//if the end time of the from duty is before the 24:00, we can determine the rest time of that day
			//if the end time of the from duty is after the 24:00, the rest only start afterwards
			if (from.getEndTime() <= 24 * 60) {
				gap += 24 * 60 - from.getEndTime();
			} else {
				gap -= from.getEndTime() - 24 * 60;
			}
			//add the start time of the follow up duty to get the rest time
			gap += to.getStartTime();
			//if the gap is less than 11 hours, it violates the daily rest constraint
			if (gap < dailyRestMin) {
				counts[0]++;
			}
			//if the gap is less than 32 hours, it violates the 32 hours constraint
			if (gap + 24 * 60 < restDayMin) {
				counts[1]++;
			}
			counts[2]++;
		}

		return counts;
	}

	public Map<String, HashMap<String, ReserveDutyType>> checkReserveDuties(){
		Map<String, HashMap<String, ReserveDutyType>> checks = new HashMap<String, HashMap<String, ReserveDutyType>>(); 
		for(int i = 0; i < weekDays.length; i++) {
			HashMap<String, ReserveDutyType> currentCheck1 = new HashMap<String, ReserveDutyType>();
			for(int j = 0; j<typesDuty.length; j++) {
				ReserveDutyType check2 = null; 
				for(ReserveDutyType duty : instance.getReserveDutyTypes()) {
					if(duty.getDayType().equals(weekDays[i]) && duty.getType().equals(typesDuty[j])) {
						check2 = duty; 
					}
				}
				currentCheck1.put("R" + typesDuty[j], check2); 
			}
			checks.put(weekDays[i], currentCheck1); 
		}
		return checks; 
	}
}
