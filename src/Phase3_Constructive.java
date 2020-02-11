
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import Tools.*; 

public class Phase3_Constructive {

	private Map<ContractGroup, String[]> solutionMIP; 
	private ArrayList<HashMap<String, ArrayList<Integer>>> toSchedule; 
	private ArrayList<HashMap<String, ArrayList<Integer>>> allDuties; 
	private ArrayList<HashMap<String, ArrayList<Integer>>> alrScheduled; 
	private Map<ContractGroup, Integer[]> solutionHeur; 
	private Instance instance; 
	private Map<Integer, Duty> dutyNrToDuty; 
	private Set<Schedule> finalSchedules = new HashSet<Schedule>(); 

	public Phase3_Constructive(Instance instance, HashMap<ContractGroup, String[]> solutionMIP ) {
		this.dutyNrToDuty = instance.getFromDutyNrToDuty(); 
		this.solutionHeur = new  HashMap<ContractGroup, Integer[]>(); 
		this.solutionMIP = instance.getBasicSchedules(); 
		this.toSchedule = new ArrayList<HashMap<String, ArrayList<Integer>>>(); 
		this.alrScheduled = new ArrayList<HashMap<String, ArrayList<Integer>>>(); 
		this.instance = instance; 
		
		for(int i= 0; i<7; i++) {
			this.alrScheduled.add(new HashMap<String, ArrayList<Integer>>()); 
			this.toSchedule.add(new HashMap<String, ArrayList<Integer>>()); 
			for(String dutyType : instance.getDutyTypes()) {
				if(i==0) {
					if(instance.getDutiesPerTypeSun().containsKey(dutyType)) {
						this.toSchedule.get(i).put(dutyType, getNumbersFromDuty(instance.getDutiesPerTypeSun().get(dutyType))); 
						this.alrScheduled.get(i).put(dutyType, new ArrayList<Integer>()); 	
						if(dutyType.substring(dutyType.length()-1, dutyType.length()).equals("L")) {
							Collections.sort(this.toSchedule.get(i).get(dutyType), (a,b) -> this.dutyNrToDuty.get(b).getEndTime() - this.dutyNrToDuty.get(a).getEndTime()); 
						}else {
							Collections.sort(this.toSchedule.get(i).get(dutyType), (a,b) -> this.dutyNrToDuty.get(a).getStartTime() - this.dutyNrToDuty.get(b).getStartTime()); 
						}
					}
				}else if(i==6) {
					if(instance.getDutiesPerTypeSat().containsKey(dutyType)) {
						this.toSchedule.get(i).put(dutyType, getNumbersFromDuty(instance.getDutiesPerTypeSat().get(dutyType))); 
						this.alrScheduled.get(i).put(dutyType, new ArrayList<Integer>()); 
						if(dutyType.substring(dutyType.length()-1, dutyType.length()).equals("L")) {
							Collections.sort(this.toSchedule.get(i).get(dutyType), (a,b) -> this.dutyNrToDuty.get(b).getEndTime() - this.dutyNrToDuty.get(a).getEndTime()); 
						}else {
							Collections.sort(this.toSchedule.get(i).get(dutyType), (a,b) -> this.dutyNrToDuty.get(a).getStartTime() - this.dutyNrToDuty.get(b).getStartTime()); 
						}
					}
				}else {
					if(instance.getDutiesPerTypeW().containsKey(dutyType)) {
						this.toSchedule.get(i).put(dutyType, getNumbersFromDuty(instance.getDutiesPerTypeW().get(dutyType))); 
						this.alrScheduled.get(i).put(dutyType, new ArrayList<Integer>()); 
						if(dutyType.substring(dutyType.length()-1, dutyType.length()).equals("L")) {
							Collections.sort(this.toSchedule.get(i).get(dutyType), (a,b) -> this.dutyNrToDuty.get(b).getEndTime() - this.dutyNrToDuty.get(a).getEndTime()); 
						}else {
							Collections.sort(this.toSchedule.get(i).get(dutyType), (a,b) -> this.dutyNrToDuty.get(a).getStartTime() - this.dutyNrToDuty.get(b).getStartTime()); 
						}
					}
				}
			}
		}
		this.allDuties = new ArrayList<HashMap<String, ArrayList<Integer>>>(this.toSchedule); 
		solve(); 

		for(ContractGroup group: solutionHeur.keySet()) {
			this.finalSchedules.add(getSchedule(group)); 
			System.out.println(Arrays.toString(solutionHeur.get(group))); 
			System.out.println(Arrays.toString(calculateOverTime(group))); 
			System.out.println(Arrays.toString(calculateAvOverTime(calculateOverTime(group)))); 
		}
	}

	public Set<Schedule> getSchedule(){
		return this.finalSchedules; 
	}
	
	/**
	 * converts a set of duties to a list of the duty numbers 
	 * @param duties
	 * @return
	 */
	public ArrayList<Integer> getNumbersFromDuty(Set<Duty> duties){
		ArrayList<Integer> newList = new ArrayList<Integer>(); 
		for(Duty duty:duties) {
			newList.add(duty.getNr()); 
		}
		return newList; 
	}

	public void solve() {
		//while(!allDutiesPlanned) {
		ArrayList<ContractGroup> groups = new ArrayList<ContractGroup>(instance.getContractGroups()); 
		Collections.sort(groups, (a,b) -> a.getTc() - b.getTc());
		outmostLoop:
			for(ContractGroup group:groups) {
				System.out.println("Checking contract group: " + group.getNr()); 
				Map<Integer, ArrayList<Integer>> otherPos = new HashMap<Integer, ArrayList<Integer>>(); 
				String[] solution = solutionMIP.get(group); 
				boolean allDutiesPContractGroupPlanned = false; 
				int current = 0; 
				Integer[] sol = new Integer[solution.length]; 
				this.solutionHeur.put(group, sol); 
				boolean goBack = false; 

				while(!allDutiesPContractGroupPlanned) {
					ArrayList<Integer> possibleDuties = null; 
					boolean normalDuty = false; 
					if(goBack) {
						if(solution[current].equals("ATV") ||solution[current].contains("R") ) {
							current--; 
							normalDuty = false; 
						}else {
							possibleDuties = otherPos.get(current); 
							this.toSchedule.get(current%7).get(solution[current]).add(sol[current]); 
							normalDuty = true; 
						}
					}else {
						boolean feas = true;
						if(solution[current].equals("ATV")) {	
							sol[current] = 1; 
						}else if(solution[current].equals("Rest")) {
							sol[current] = 2; 
						}else if(solution[current].substring(0, 1).equals("R")){
							sol[current] = this.getReserveDutyType(solution[current].substring(1), current).getNr(); 
							feas = checkFeasibility(sol, current); 
						}else {
							possibleDuties = new ArrayList<Integer>(this.toSchedule.get(current%7).get(solution[current]));
							normalDuty = true; 
						}

						if(!feas) {
							current--; 
							goBack = true;
						}else if(!normalDuty) {
							current++; 
						}
					}

					if(normalDuty) {
						boolean possDay = false; 
						for(int i = 0; i<possibleDuties.size(); i++) {
							sol[current] = possibleDuties.get(i); 
							if(checkFeasibility(sol, current)) {
								possibleDuties.remove(i); 
								otherPos.put(current, possibleDuties); 
								this.toSchedule.get(current%7).get(solution[current]).remove(sol[current]); 
								possDay = true; 
								current++; 
								goBack = false; 
								break; 
							}
							else {
								sol[current]= null; 
							}
						}
						if(!possDay) {
							current--; 
							goBack = true; 
						}
					}

					if(current == solution.length) {
						allDutiesPContractGroupPlanned = true; 
					}else if(current == -1) {
						System.out.println("No feasible Solution for contract group: " + group.getNr() );
						break outmostLoop; 
					}
				}
			}
		//}
	}

	/**
	 * This method returns the reserve duty type to be considered for a certain day number and duty type
	 * @param type				the duty type
	 * @param t					the day number
	 * @return					the reserve duty type to be scheduled on that day
	 */
	public ReserveDutyType getReserveDutyType(String type, int t) {
		// First, obtain the day type from the day number
		int weekdayNr = t % 7;
		String dayType = "";
		if (weekdayNr == 0) {
			dayType = "Sunday";
		} else if (weekdayNr == 6) {
			dayType = "Saturday";
		} else {
			dayType = "Workingday";
		}

		// Then, find the reserve duty type that corresponds to this day type and duty type
		for (ReserveDutyType rDuty : instance.getReserveDutyTypes()) {
			if (rDuty.getDayType().equals(dayType) && rDuty.getType().equals(type)) {
				return rDuty;
			}
		}

		throw new IllegalArgumentException("There is no reserve duty type " + type + " on day type " + dayType);
	}

	/**
	 * Check the feasibility of the roster up till the current day (only the last 14 days) 
	 * @param sol
	 * @param current
	 * @return
	 */
	public boolean checkFeasibility(Integer[] sol, int current) {
		boolean minbreak = true; 
		boolean minWeekBreak = true; 
		boolean min2WeekBreak = true; 

		if(current == 0) {
			return true; 
		}

		if(current > 0) {
			int endPrev = 0; 
			int startCur = 0; 
			if(sol[current-1] <=2) {
				endPrev = -24*60;  
			}else if(sol[current-1] <= 1000) {
				ReserveDutyType prevDuty = this.instance.getFromRDutyNrToRDuty().get(sol[current-1]); 
				endPrev = prevDuty.getEndTime(); 
			}else {
				Duty prevDuty = this.dutyNrToDuty.get(sol[current-1]); 
				endPrev = prevDuty.getEndTime(); 
			}

			if(sol[current] <=2) {
				startCur = 24*60;  
			}else if(sol[current] <= 1000) {
				ReserveDutyType curDuty = this.instance.getFromRDutyNrToRDuty().get(sol[current]); 
				startCur = curDuty.getStartTime(); 
			}else {
				Duty curDuty = this.dutyNrToDuty.get(sol[current]); 
				startCur = curDuty.getStartTime(); 
			}

			minbreak = startCur +  (24*60 -  endPrev)> instance.getMinBreak(); 
		}

		if(current >=6) {
			minWeekBreak = isFeasible7(sol, current); 
		}

		if(current >=13) {
			min2WeekBreak = isFeasible14(sol, current); 
		}

		return minbreak &&  minWeekBreak  && min2WeekBreak; 
	}

	/**
	 * This method tests whether the 7x24 hour constraint is satisfied ending at day t
	 * @param sol			the schedule
	 * @param t					the end day until which the constraint has to be checked. 
	 * @return					whether the 7x24 hour constraint is satisfied or not
	 */
	public boolean isFeasible7(Integer[] sol, int t) {

		for(int j = 0; j<7; j++) {
			int index = t - 6 + j; 
			if(sol[index]== 1|| sol[index] ==2) {
				int consec = 24*60; 
				if(j != 0) {	
					int prevEndTime = 0; 

					if(sol[(index)%sol.length]==1 || sol[(index)%sol.length] ==2) {
						prevEndTime = 0; 
					}else if(this.dutyNrToDuty.containsKey(sol[(index)%sol.length])) {
						prevEndTime =this.dutyNrToDuty.get(sol[(index)%sol.length]).getEndTime(); 
					}else {
						prevEndTime =instance.getFromRDutyNrToRDuty().get(sol[(index)%sol.length]).getEndTime(); 
					}
					consec += 24*60 - prevEndTime; 
				}

				if(j!=7) {
					int nextStartTime = 0; 

					if(sol[(index)%sol.length]==1 || sol[(index)%sol.length] ==2) {
						nextStartTime = 24*60; 
					}else if(this.dutyNrToDuty.containsKey(sol[(index)%sol.length])) {
						nextStartTime =this.dutyNrToDuty.get(sol[(index)%sol.length]).getStartTime(); 
					}else {
						nextStartTime =instance.getFromRDutyNrToRDuty().get(sol[(index)%sol.length]).getStartTime(); 
					}
					consec += nextStartTime; 
				}
				if(consec>instance.getMinBreak()) {
					return true; 
				}
			}
		}
		return false; 
	}

	/**
	 * This method tests whether the 7x24 hour constraint is satisfied ending at day t
	 * @param sol			the schedule
	 * @param t					the end day until which the constraint has to be checked. 
	 * @return					whether the 7x24 hour constraint is satisfied or not
	 */
	public boolean isFeasible14(Integer[] sol, int t) {

		int consec14 = 0;

		for(int j = 0; j<14; j++) {
			int index = t - 13 + j; 
			if(sol[index]== 1|| sol[index] ==2) {
				int consec = 24*60; 
				if(j != 0) {	
					int prevEndTime = 0; 

					if(sol[(index - 1)%sol.length]!=1 && sol[(index - 1)%sol.length] !=2) {
						if(this.dutyNrToDuty.containsKey(sol[(index - 1)%sol.length])) {
							prevEndTime =this.dutyNrToDuty.get(sol[(index - 1)%sol.length]).getEndTime(); 
						}else {
							prevEndTime =instance.getFromRDutyNrToRDuty().get(sol[(index - 1)%sol.length]).getEndTime(); 
						}
					}
					consec += 24*60 - prevEndTime; 
				}

				if(j!=7) {
					int nextStartTime = 0; 

					if(sol[(index+1)%sol.length]==1 || sol[(index+1)%sol.length] ==2) {
						nextStartTime = 24*60; 
						j++; 
						if(sol[(index+2)%sol.length]==1 || sol[(index+2)%sol.length] ==2) {
							j++; 
						}
					}else if(this.dutyNrToDuty.containsKey(sol[(index+1)%sol.length])) {
						nextStartTime =this.dutyNrToDuty.get(sol[(index+1)%sol.length]).getStartTime(); 
					}else {
						nextStartTime =instance.getFromRDutyNrToRDuty().get(sol[(index+1)%sol.length]).getStartTime(); 
					}
					consec += nextStartTime; 
				}
				if(consec>instance.getMinBreak()) {
					consec14 += consec; 
				}

				if(consec14 > instance.getMin2WeekBreak()) {
					return true; 
				}
			}
		}
		return false; 
	}

	public int[] calculateOverTime(ContractGroup group) {
		Integer[] solutionPGroup = this.solutionHeur.get(group); 
		int[] timeWorkPWeek = new int[solutionPGroup.length/7]; 

		for(int w=0;w<solutionPGroup.length/7; w++) {
			int hourspWeek = 0; 
			for(int d=0; d< 7; d++) {
				if(this.instance.getFromDutyNrToDuty().containsKey(solutionPGroup[w*7+d])) {
					hourspWeek += this.dutyNrToDuty.get(solutionPGroup[w*7+d]).getPaidMin(); 
				}else if(this.instance.getFromRDutyNrToRDuty().containsKey(solutionPGroup[w*7+d])) {
					hourspWeek += group.getAvgHoursPerDay()*60; 
				}
			}
			timeWorkPWeek[w] = (int) (hourspWeek - group.getAvgDaysPerWeek()*group.getAvgHoursPerDay()*60); 
		}

		return timeWorkPWeek; 
	}

	public int[] calculateAvOverTime(int[] timeWorkPWeek) {
		int[] timePQuarter = new int[timeWorkPWeek.length/13 + 1]; 

		for(int q= 0; q<timePQuarter.length + 1; q++) {
			for(int w =0; w<13; w++) {
				if(q*13+w< timeWorkPWeek.length) {
					timePQuarter[q] += timeWorkPWeek[q*13+w]; 
				}
			}
		}
		
		return timePQuarter; 
	}
	
	public Schedule getSchedule(ContractGroup group) {
		int overTime = 0; 
		int minHours = 0; 
		int[] time = calculateOverTime(group); 

		for(int i = 0; i<time.length; i++) {
			if(time[i]>0) {
				overTime += time[i]; 
			}else {
				minHours += -1*time[i]; 
			}
		}

		return new Schedule(group, overTime, minHours, Arrays.stream(this.solutionHeur.get(group)).mapToInt(Integer::intValue).toArray()); 
	}
}