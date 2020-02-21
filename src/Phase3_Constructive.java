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
	private Map<ContractGroup, int[]> solutionHeur; 
	private Instance instance; 
	private Map<Integer, Duty> dutyNrToDuty; 
	private Set<Schedule> finalSchedules = new HashSet<Schedule>(); 
	private Map<Integer, Integer> prevOfThisType = new HashMap<Integer, Integer>(); 
	private int[] numberOfThisChecked ; 
	private String[] completeSolution; 
	private ArrayList<ContractGroup> groups; 
	private int totalDays = 0; 

	public Phase3_Constructive(Instance instance, HashMap<ContractGroup, String[]> solutionMIP ) {
		this.dutyNrToDuty = instance.getFromDutyNrToDuty(); 
		this.solutionHeur = new  HashMap<ContractGroup, int[]>(); 
		this.solutionMIP = instance.getBasicSchedules(); 
		this.toSchedule = new ArrayList<HashMap<String, ArrayList<Integer>>>(); 
		this.alrScheduled = new ArrayList<HashMap<String, ArrayList<Integer>>>(); 
		this.instance = instance; 

		this.groups = new ArrayList<ContractGroup>(instance.getContractGroups()); 
		Collections.sort(groups, (a,b) -> a.getTc() - b.getTc());

		for(ContractGroup group: groups) {
			totalDays += this.solutionMIP.get(group).length; 
		}

		this.completeSolution = new String[totalDays]; 
		int currentIndex = 0; 
		for(ContractGroup group: groups) {
			for(int i = 0; i< this.solutionMIP.get(group).length; i++) {
				this.completeSolution[i+currentIndex] = this.solutionMIP.get(group)[i]; 
			}
			currentIndex += this.solutionMIP.get(group).length; 
		}

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

		int[] finalSolution = solve(); 
		this.solutionHeur = getIndividualSolutions(finalSolution); 


		for(ContractGroup group: solutionHeur.keySet()) {
			this.finalSchedules.add(getSchedule(group)); 
			printSolution(group); 
			//System.out.println(Arrays.toString(solutionHeur.get(group))); 
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

	public int[] solve() {

		String[] solution = this.completeSolution; 
		Map<Integer, ArrayList<Integer>> otherPos = new HashMap<Integer, ArrayList<Integer>>(); 
		this.numberOfThisChecked = new int[solution.length]; 
		int[] sol = new int[solution.length]; 
		HashMap<Integer, Integer> prevOfThisType =  getPrevOfThisType(solution); 
		HashMap<Integer, String> skipped = new HashMap<Integer, String>(); 

		int change = 0; 
		boolean allDutiesPContractGroupPlanned = false; 
		boolean goBack = false; 
		int current = 0; 
		int maxDay = 0; 
		int[] maxSol = null; 
		while(!allDutiesPContractGroupPlanned) {

			if(current>maxDay) {
				maxDay = current; 
				maxSol = sol; 
			}
			ArrayList<Integer> possibleDuties = null; 
			boolean normalDuty = false; 

			/*
			 * Two possibilities here, either we are going back in time from point t to t_ because it was infeasible,
			 * or we continue from t to t+1. 
			 * 
			 * If we are going back, goBack is true. Then we are going to reschedule everything from t_ onwards. 
			 * In this case the possibleDuties at t_ decreases by one as the previous value of t_ was deemed infeasible.
			 * The possible duties are retrieved from a Map that stores the set of possible duties at point t_
			 * 
			 * In case we continue to t+1, we set the possible duties as all the possible duties that are not scheduled yet
			 */

			if(goBack) {
				goBack(change, current + change, solution, sol); 
				if(solution[current].equals("ATV") ||solution[current].contains("R") ) {
					current--; 
					normalDuty = false; 
				}else {
					possibleDuties = otherPos.get(current);  
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
					change = 1; 
					current -= change; 
					goBack = true;
				}else if(!normalDuty) {
					current++; 
				}

			}

			// If it is a normal duty (not a ATV, Rest or reserve duty) then we check if any of the duties can be placed inn the schedule 
			if(normalDuty) {
				boolean possDay = false; 
				for(int i = 0; i<possibleDuties.size(); i++) {
					sol[current] = possibleDuties.get(i); 
					if(checkFeasibility(sol, current)) {
						possibleDuties.remove(i); 
						otherPos.put(current, possibleDuties); 
						this.toSchedule.get(current%7).get(solution[current]).remove(i); 
						possDay = true; 
						current++; 
						goBack = false; 
						break; 
					}
					else {
						sol[current]= 0; 
					}
				}

				/*
				 * If it is not possible to place any duty from the possible duties in the schedule, then we need to go back in time 
				 * 
				 * We also keep track of how often we could not place a duty on this day. 
				 * If this number exceeds the number of possible duties of the previous day, 
				 * then we go back to the previous point t, which was on the same weekday and had to schedule the same duty type 
				 * 
				 */
				if(!possDay) {
					this.numberOfThisChecked[current]++; 
					int nPosPrev = otherPos.containsKey(current-1)? otherPos.get(current - 1).size() : 1; 

					if(nPosPrev == 0) {
						if(prevOfThisType.containsKey(current - 1)) {
							change = current - prevOfThisType.get(current - 1); 
						}else {
							change = 1; 
						}
					}
					else if(this.numberOfThisChecked[current] >= nPosPrev* possibleDuties.size()) {
						if(prevOfThisType.get(current) == null) {
							change = 1; 
						}
						else {
							change = current - prevOfThisType.get(current);
						}
						this.numberOfThisChecked[current] = 0; 
					}
					else {
						change = 1; 
					}
					current -= change; 
					goBack = true; 

				}
			}

			if(current == solution.length) {
				allDutiesPContractGroupPlanned = true; 
			}else if(current == -1) {
				System.out.println("No feasible Solution");
				break; 
			}
		}
		return sol;
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
	public boolean checkFeasibility(int[] sol, int current) {
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

			minbreak = startCur +  (24*60 -  endPrev)>= instance.getMinBreak(); 
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
	public boolean isFeasible7(int[] sol, int t) {

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
	public boolean isFeasible14(int[] sol, int t) {

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
		int[] solutionPGroup = this.solutionHeur.get(group); 
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

		return new Schedule(group, overTime, this.solutionHeur.get(group)); 
	}

	public HashMap<Integer, Integer> getPrevOfThisType(String[] solution){
		HashMap<Integer, Integer> prevOfThisType = new HashMap<Integer, Integer> (); 
		ArrayList<HashMap<String, Integer>> lastOfThisType = new ArrayList<HashMap<String, Integer>>(); 

		for(int i = 0; i<7; i++) {
			lastOfThisType.add(new HashMap<String, Integer>()); 
		}

		for(int i =0; i< solution.length; i++) {

			if(lastOfThisType.get(i%7).containsKey(solution[i])) {
				prevOfThisType.put(i,lastOfThisType.get(i%7).get(solution[i])); 
				lastOfThisType.get(i%7).put(solution[i], i); 
			}else {
				lastOfThisType.get(i%7).put(solution[i], i); 
			}
		}

		return prevOfThisType; 
	}

	public void goBack(int change, int current, String[] solution, int[] sol) {
		for(int i = current - 1; i>=current - change; i--) {
			if(!(solution[i].equals("ATV") ||solution[i].contains("R"))) {
				if(sol[i] == 0) {
					System.out.println("test2"); 
				}
				this.toSchedule.get(i%7).get(solution[i]).add(0, sol[i]); 
			}
			//sol[i] = null; 
		}
	}

	public Map<ContractGroup, int[]> getIndividualSolutions(int[] sol){
		Map<ContractGroup, int[]> result = new HashMap<ContractGroup, int[]>(); 
		int currentIndex = 0; 
		for(ContractGroup group: groups) {
			int[] solPGroup = new int[this.solutionMIP.get(group).length]; 
			for(int i=0; i<solPGroup.length; i++) {
				solPGroup[i] = sol[currentIndex + i]; 
			}
			currentIndex += solPGroup.length; 
			result.put(group, solPGroup); 
		}
		return result; 
	}

	public void printSolution(ContractGroup group) {
		System.out.println("Contract Group: " + group.getNr()); 
		for(int w = 0; w < this.solutionHeur.get(group).length/7 ; w++) {
			for(int j = 0; j< 7; j++) {
				System.out.print(this.solutionMIP.get(group)[w*7 + j] +" ");
			}
			System.out.println(""); 
			for(int j = 0; j< 7; j++) {
				System.out.print(this.solutionHeur.get(group)[w*7 + j]+" ");
			}
			System.out.println(""); 
		}

	}
}