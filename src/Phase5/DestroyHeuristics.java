package Phase5;
import java.util.*;

import Tools.ContractGroup;
import Tools.Duty;
import Tools.Instance;
import Tools.ReserveDutyType;
import Tools.Schedule;

/**
 * This class stores the destroy heuristics.
 * @author Mette Wagenvoort
 *
 */
public class DestroyHeuristics {
	private FeasCheck feasCheck;
	private Instance instance;
	
	/**
	 * Constructor for the destroy heuristics.
	 * @param instance				the problem instance
	 */
	public DestroyHeuristics(Instance instance) {
		this.instance = instance;
		this.feasCheck = new FeasCheck(instance);
	}

	//---------------------- Random Removal -------------------------------------------------------------------------------------------------------
	/**
	 * This method exeuctes a random removal.
	 * @param solution				the current solution
	 * @param nRemove				the size of the destroy neighbourhood
	 * @param random				the random number generator
	 * @param instance				the problem instance
	 * @return						the new solution
	 */
	public Solution executeRandom(Solution solution, int nRemove, Random random, Instance instance) {
		Set<TimeSlot> slots = new HashSet<>();
		int counter = 0;
		while(counter != nRemove) { //continue till we have removed the size of the neighborhood amount of duties
			int contGroup = random.nextInt(solution.getNewSchedule().keySet().size()); //get a contract group
			//System.out.println(contGroup);
			for(ContractGroup group: solution.getNewSchedule().keySet()) {
				if(group.getNr()-1 == contGroup) {
					int dutyDay = random.nextInt(solution.getNewSchedule().get(group).getScheduleArray().length); //get a random day in the schedule of the contract group
					//if the duty is a reserve duty, make a request consisting of a reserve duty and the corresponding day
					if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay])){
						Request request = new Request(instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay]), group, dutyDay);	
						solution.removeRequest(request, solution, slots, dutyDay);
						counter++;
					}
					//if the duty is a normal duty, make a request consisting of a normal duty and the corresponding day
					else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay])) {
						Request request = new Request(instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay]), group, dutyDay);	
						solution.removeRequest(request, solution, slots, dutyDay);
						counter++;	 
					}
					//if the duty is an ATV duty, make a request consisting of an ATV duty and the corresponding day
					else if(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay] == 1) {
						Request request = new Request(1, group, dutyDay);
						solution.removeRequest(request, solution, slots, dutyDay);
						counter++;
					}
				} 
			}
		}
		//return the new solution with the ramdom removals
		return solution; 
	}
	
	//---------------------- Extreme Removal ------------------------------------------------------------------------------------------------------
	/**
	 * This method executes the extreme removal in which the largest duties are removed from the weeks with the most overtime and the smallest duties
	 * are removed from the weeks with the least overtime/most minus hours.
	 * @param solution				the current solution
	 * @param nRemove				the size of the destroy neighbourhood
	 * @param random				the random number generator
	 * @param instance				the problem instance
	 * @return						the new solution
	 */
	public Solution executeRandomOvertimeWithWeeks(Solution solution, int nRemove, Random random, Instance instance) {
		Set<TimeSlot> slots = new HashSet<TimeSlot>();
		Map<ContractGroup, List<Integer>> removalDuties = new HashMap<ContractGroup, List<Integer>>(); //list of the weeks which have the largest overtime to remove
		Map<ContractGroup,double[]> weeklyOvertimePGroup = new HashMap<ContractGroup,double[]>(); //the weekly overtime per contract group
		//for every contract group find the weekly overtime
		for(ContractGroup group: solution.getNewSchedule().keySet()) {
			double[] weeklyOvertime = solution.getNewSchedule().get(group).getWeeklyOvertime();
			weeklyOvertimePGroup.put(group, weeklyOvertime);
			removalDuties.put(group, new ArrayList<Integer>());	
		}
		//Find the biggest weeks
		for(int i =0; i < (nRemove*2)/3; i++) {
			double check = 0;
			ContractGroup groupToAdd = new ContractGroup(0,0,0,0,0,null);
			int index = -1;
			for(ContractGroup group: instance.getContractGroups()) {
				//find the week with the most overtime that are not contained in the removal duties yet
				for(int j = 0; j < weeklyOvertimePGroup.get(group).length; j++) {
					if(weeklyOvertimePGroup.get(group)[j] > check && !removalDuties.get(group).contains(j)) {
						check = weeklyOvertimePGroup.get(group)[j];
						index = j;
						groupToAdd = group;
					}
				}
			}
			if(index != -1) {
				removalDuties.get(groupToAdd).add(index);
			}
		}


		//Find the smallest weeks
		for(int i = (nRemove*2)/3; i < nRemove; i++) {
			double check = Double.POSITIVE_INFINITY;
			ContractGroup groupToAdd = new ContractGroup(0,0,0,0,0,null);
			int index = -1;
			for(ContractGroup group: instance.getContractGroups()) {
				//find the weeks with the least overtime/minus hours and if they are not contained yet to remove a duty from it
				for(int j = 0; j < weeklyOvertimePGroup.get(group).length; j++) {
					if(weeklyOvertimePGroup.get(group)[j] < check && !removalDuties.get(group).contains(j)) {
						check = weeklyOvertimePGroup.get(group)[j];
						index = j;
						groupToAdd = group;
					}
				}
			}
			removalDuties.get(groupToAdd).add(index);
		}
		//for every week that we found, remove it and add the requests  (only if it is a rest day, we leave it in the week)
		for(ContractGroup group: instance.getContractGroups()) {
			for(int index: removalDuties.get(group)) {
				for(int w = 7*index; w <= 7*index+6; w++) {
					if(solution.getNewSchedule().get(group).getScheduleArray()[w] == 1) {
						Request request = new Request(1, group, w);
						solution.removeRequest(request, solution, slots, w);
					}
					else if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[w])) {
						Duty duty = instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[w]);
						Request request = new Request(duty, group, w);
						solution.removeRequest(request, solution,slots, w);
					}
					else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[w])) {
						ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[w]);
						Request request = new Request(reserveDuty, group, w);
						solution.removeRequest(request, solution,slots, w);
					}
				}
			}
		}
		return solution;
	}
	
	//---------------------- Extreme Specific Removal ---------------------------------------------------------------------------------------------
	/**
	 * This method executes the extreme specific removal in which the largest duties are removed from the weeks with the most overtime and the 
	 * smallest duties are removed from the weeks with the least overtime/most minus hours.
	 * 
	 * DIFFERENCE WITH LAST ONE????
	 * @param solution				the current solution
	 * @param nRemove				the size of the destroy neighbourhood
	 * @param random				the random number generator
	 * @param instance				the problem instance
	 * @return						the new solution
	 */
	public Solution executeRandomOvertimeWithSpecificDuties(Solution solution, int nRemove, Random random, Instance instance) {
		Set<TimeSlot> slots = new HashSet<TimeSlot>();
		Map<ContractGroup, List<Integer>> removalDuties = new HashMap<ContractGroup, List<Integer>>(); //list of the weeks which have the largest overtime to remove
		Map<ContractGroup,double[]> weeklyOvertimePGroup = new HashMap<ContractGroup,double[]>(); //the weekly overtime per contract group
		//for every contract group find the weekly overtime
		for(ContractGroup group: solution.getNewSchedule().keySet()) {
			double[] weeklyOvertime = solution.getNewSchedule().get(group).getWeeklyOvertime();
			weeklyOvertimePGroup.put(group, weeklyOvertime);
			removalDuties.put(group, new ArrayList<Integer>());	
		}
		//Find the biggest weeks
		for(int i =0; i < (nRemove*2)/3; i++) {
			double check = 0;
			ContractGroup groupToAdd = new ContractGroup(0,0,0,0,0,null);
			int index = -1;
			for(ContractGroup group: instance.getContractGroups()) {
				//find the week with the most overtime that are not contained in the removal duties yet
				for(int j = 0; j < weeklyOvertimePGroup.get(group).length; j++) {
					if(weeklyOvertimePGroup.get(group)[j] > check && !removalDuties.get(group).contains(j)) {
						check = weeklyOvertimePGroup.get(group)[j];
						index = j;
						groupToAdd = group;
					}
				}
			}
			if(index != -1) {
				removalDuties.get(groupToAdd).add(index);
			}
		}

		//Find the smallest weeks
		for(int i = (nRemove*2)/3; i < nRemove; i++) {
			double check = Double.POSITIVE_INFINITY;
			ContractGroup groupToAdd = new ContractGroup(0,0,0,0,0,null);
			int index = -1;
			for(ContractGroup group: instance.getContractGroups()) {
				//find the weeks with the least overtime/minus hours and if they are not contained yet to remove a duty from it
				for(int j = 0; j < weeklyOvertimePGroup.get(group).length; j++) {
					if(weeklyOvertimePGroup.get(group)[j] < check && !removalDuties.get(group).contains(j)) {
						check = weeklyOvertimePGroup.get(group)[j];
						index = j;
						groupToAdd = group;
					}
				}
			}
			removalDuties.get(groupToAdd).add(index);
		}
		//for every group, find the largest duty in the week to remove
		for(ContractGroup group: instance.getContractGroups()) {
			double dutyTime = 0;
			int dutyNumber = 0;
			ReserveDutyType saveReserveDuty = new ReserveDutyType(null, null, 0,0,0,0);
			Duty saveDuty = new Duty(0,null,0,0,0,0, null,0);
			int dutyDay = -1;
			for(int index: removalDuties.get(group)) {			   
				//if we have overtime pick largest duty
				if(solution.getNewSchedule().get(group).getWeeklyOvertime()[index] > 0) {
					dutyTime = 0;
					dutyNumber = 0;
					for(int w = index*7; w <= index*7+6; w++) {
						if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[w])) {
							Duty duty = instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[w]);
							if(duty.getEndTime()- duty.getStartTime() > dutyTime) {
								dutyTime = duty.getEndTime()- duty.getStartTime();
								dutyNumber = duty.getNr();
								saveDuty = duty;
								dutyDay = w;
							}
						}
						else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[w])){
							ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[w]);
							//System.out.println(reserveDuty);
							if(reserveDuty.getEndTime()- reserveDuty.getStartTime() > dutyTime) {
								dutyTime = reserveDuty.getEndTime()- reserveDuty.getStartTime();
								dutyNumber = reserveDuty.getNr();
								saveReserveDuty = reserveDuty;
								dutyDay = w;
							}
						}
						else if(solution.getNewSchedule().get(group).getScheduleArray()[w] == 1) {
							if(group.getAvgHoursPerDay()*60 > dutyTime) {
								dutyTime = group.getAvgHoursPerDay()*60;
								dutyNumber = 1;
								dutyDay = w;
							}
						}
					}

					if(dutyNumber == 1) {
						Request request = new Request(dutyNumber, group, dutyDay);
						solution.removeRequest(request, solution, slots, dutyDay);
					}
					else if(dutyNumber<= 1000) {
						Request request = new Request(saveReserveDuty, group, dutyDay);
						solution.removeRequest(request, solution,slots, dutyDay);
					}
					else {
						Request request = new Request(saveDuty, group, dutyDay);
						solution.removeRequest(request, solution,slots, dutyDay);	
					}
				}
				// find the smallest duty to remove
				else {
					dutyTime = Double.POSITIVE_INFINITY;
					dutyNumber = 0;
					int numberOfDutiesInWeek = 0;
					for(int z = index*7;z <= index*7+6; z++) {
						if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[z])) {
							numberOfDutiesInWeek++;
						}
						else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[z])){
							numberOfDutiesInWeek++;
						}
						else if(solution.getNewSchedule().get(group).getScheduleArray()[z] == 1) {
							numberOfDutiesInWeek++;
						} 
					}
					if(numberOfDutiesInWeek >= 4 ) {
						for(int w = index*7; w <= index*7+6; w++) {
							if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[w])) {
								Duty duty = instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[w]);
								if(duty.getEndTime()- duty.getStartTime() < dutyTime) {
									dutyTime = duty.getEndTime()- duty.getStartTime();

									dutyNumber = duty.getNr();
									saveDuty = duty;
									//System.out.println(duty.getNr());
									dutyDay = w;
								}
							}
							else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[w])){
								ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[w]);
								//System.out.println(reserveDuty);
								if(reserveDuty.getEndTime()- reserveDuty.getStartTime() < dutyTime) {
									dutyTime = reserveDuty.getEndTime()- reserveDuty.getStartTime();
									dutyNumber = reserveDuty.getNr();
									saveReserveDuty = reserveDuty;
									dutyDay = w;
								}
							}
							else if(solution.getNewSchedule().get(group).getScheduleArray()[w] == 1) {
								if(group.getAvgHoursPerDay()*60 < dutyTime) {
									dutyTime = group.getAvgHoursPerDay()*60;
									dutyNumber = 1;
									dutyDay = w;
								}
							}


						}
						if(dutyDay == -1) {
							System.out.println("NOT");
						}
						if(dutyNumber == 0) {
							System.out.println("duty number is zero");
						}
						if(dutyNumber == 1) {
							Request request = new Request(dutyNumber, group, dutyDay);
							solution.removeRequest(request, solution, slots, dutyDay);
						}
						else if(dutyNumber <= 1000) {
							//System.out.println("reserve : " + saveReserveDuty.getNr());
							Request request = new Request(saveReserveDuty, group, dutyDay);
							solution.removeRequest(request, solution,slots, dutyDay);
						}
						else {
							//System.out.println("duty:  " + saveDuty.getNr());
							Request request = new Request(saveDuty, group, dutyDay);
							solution.removeRequest(request, solution,slots, dutyDay);	
						}
					}
				}
			}
		}
		return solution; 
	}

	//---------------------- Week Removal ---------------------------------------------------------------------------------------------------------
	/**
	 * This method executes the week removal in which the week with the least overtime/most minus hours is removed from the schedule.
	 * @param solution				the current solution
	 * @param random				the random number generator
	 * @param instance				the problem instance
	 * @return						the new solution
	 */
	public Solution executeRemoveWeek(Solution solution, Random random, Instance instance) {
		int counter = 0; //can use counter if we want to remove multiple weeks
		int number = random.nextInt(instance.getContractGroups().size());
		ContractGroup group = new ContractGroup(0,0, 0, 0, 0, null);
		Set<TimeSlot> emptyTimeSlots = new HashSet<TimeSlot>();
		for(ContractGroup g: instance.getContractGroups()) {
			if(g.getNr()-1 == number) {
				group = g;
			}
		}
		int count = 0;
		outer: while(count != 100) {
			int index = random.nextInt(solution.getNewSchedule().get(group).getScheduleArray().length/7-1);
			if(index == solution.getNewSchedule().get(group).getScheduleArray().length/7) {
				index--;
			}
			int numberOfRestDays = 0;
			for(int j = 7*index; j <= index*7+6; j++) {
				if(solution.getNewSchedule().get(group).getScheduleArray()[j] == 2) {
					numberOfRestDays++;
				}
			}
			if(numberOfRestDays >= 3) {
				int[] newSchedule = new int[solution.getNewSchedule().get(group).getScheduleArray().length-7];
				int[] temp = solution.getNewSchedule().get(group).getScheduleArray().clone();
				for(int l =0; l < index*7; l++) {
					newSchedule[l]= temp[l];
				}
				if(index + 1 < solution.getNewSchedule().get(group).getScheduleArray().length/7) {
				for(int m = (index+1)*7; m < solution.getNewSchedule().get(group).getScheduleArray().length; m++) {
					newSchedule[((m-7)%solution.getNewSchedule().get(group).getScheduleArray().length)%solution.getNewSchedule().get(group).getScheduleArray().length] = temp[m];
				 }
				}
				Schedule newschedule = new Schedule(group, newSchedule, (int) this.feasCheck.QuarterlyOvertime(newSchedule, group) );
				if(this.checkFeasibility(newschedule, (index*7)) && this.checkRelativeGroupSize(solution, newschedule)) {
				
				 for(int k = 7*index; k <= index*7+6; k++) {
					if(solution.getNewSchedule().get(group).getScheduleArray()[k] == 1) {
						Request request = new Request(1, group, k);
						solution.removeRequest(request, solution, emptyTimeSlots, k);
						
					}
					else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[k])) {	
						ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[k]);
						Request request = new Request(reserveDuty, group, k);
						solution.removeRequest(request, solution, emptyTimeSlots, k);
						
					}
					else if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[k])) {
						Duty duty = instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[k]);
						Request request =  new Request(duty, group, k);
						solution.removeRequest(request, solution, emptyTimeSlots, k);
					
					}
				}
				solution.getNewSchedule().get(group).setScheduleArray(newSchedule);
				
			}
				
				break outer;
			}
			else {
				count++;
			}
		}
		return solution;
	}
	
	public Solution executeSwapWeek(Solution solution, Random random, Instance instance) {
		int number = random.nextInt(instance.getContractGroups().size());
		int number2 = random.nextInt(instance.getContractGroups().size());
		while(number == number2) {
			number2 = random.nextInt(instance.getContractGroups().size());
		}
		ContractGroup group1 = new ContractGroup(0,0, 0, 0, 0, null);
		ContractGroup group2 = new ContractGroup(0,0, 0, 0, 0, null);
		Set<TimeSlot> emptyTimeSlots = new HashSet<TimeSlot>();
		for(ContractGroup g: instance.getContractGroups()) {
			if(number >= number2) {
				if(g.getNr()-1 == number) {
					group1 = g;
				}
				else if(g.getNr()-1 == number2) {
					group2 = g;
				}
			}
			else {
				if(g.getNr()-1 == number) {
					group2 = g;
				}
				else if(g.getNr()-1 == number2) {
					group1 = g;
				}
			}
		}
		int count = 0;
		outer: while(count != 100) {
			int index = random.nextInt(solution.getNewSchedule().get(group1).getScheduleArray().length/7-1);
			if(index == solution.getNewSchedule().get(group1).getScheduleArray().length/7) {
				index--;
			}
			int numberOfRestDays = 0;
			for(int j = 7*index; j <= index*7+6; j++) {
				if(solution.getNewSchedule().get(group1).getScheduleArray()[j] == 2) {
					numberOfRestDays++;
				}
			}
			if(numberOfRestDays >= 3) {
				int[] newSchedule = new int[solution.getNewSchedule().get(group1).getScheduleArray().length-7];
				int[] temp = solution.getNewSchedule().get(group1).getScheduleArray().clone();
				for(int l =0; l < index*7; l++) {
					newSchedule[l]= temp[l];
				}
				if(index + 1 < solution.getNewSchedule().get(group1).getScheduleArray().length/7) {
				for(int m = (index+1)*7; m < solution.getNewSchedule().get(group1).getScheduleArray().length; m++) {
					newSchedule[((m-7)%solution.getNewSchedule().get(group1).getScheduleArray().length)%solution.getNewSchedule().get(group1).getScheduleArray().length] = temp[m];
				 }
				}
				Schedule newschedule = new Schedule(group1, newSchedule, (int) this.feasCheck.QuarterlyOvertime(newSchedule, group1) );
				if(this.checkFeasibility(newschedule, (index*7)) && this.checkRelativeGroupSize(solution, newschedule)) {
				
				 for(int k = 7*index; k <= index*7+6; k++) {
					if(solution.getNewSchedule().get(group1).getScheduleArray()[k] == 1) {
						Request request = new Request(1, group1, k);
						solution.removeRequest(request, solution, emptyTimeSlots, k);
						
					}
					else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group1).getScheduleArray()[k])) {	
						ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group1).getScheduleArray()[k]);
						Request request = new Request(reserveDuty, group1, k);
						solution.removeRequest(request, solution, emptyTimeSlots, k);
						
					}
					else if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group1).getScheduleArray()[k])) {
						Duty duty = instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group1).getScheduleArray()[k]);
						Request request =  new Request(duty, group1, k);
						solution.removeRequest(request, solution, emptyTimeSlots, k);
					}
				}
				solution.getNewSchedule().get(group1).setScheduleArray(newSchedule);
				}
				break outer;
			}
			else {
				count++;
			}
		}
		if(count != 100) {
			int[] newSchedule = new int[solution.getNewSchedule().get(group2).getScheduleArray().length+7];
			int[] temp = solution.getNewSchedule().get(group2).getScheduleArray().clone();
			for(int i =0; i < temp.length; i++) {
				newSchedule[i] = temp[i];
			}
			for(int j = temp.length; j < newSchedule.length; j++) {
				newSchedule[j] =2;
			}
			//System.out.println(solution.getNewSchedule().get(group1).getScheduleArray().length + " " + newSchedule.length);
			Schedule schedule = new Schedule(group2, newSchedule, (int) this.feasCheck.QuarterlyOvertime(newSchedule, group2));
			System.out.println(newSchedule.length/7 + " " + schedule.getWeeklyOvertime().length);
				
			if(this.checkRelativeGroupSize(solution, schedule)) {
				solution.getNewSchedule().get(group2).setScheduleArray(newSchedule);
				solution.setWeeklyOvertime(newSchedule, group2);
				System.out.println("2: "+ solution.getNewSchedule().get(group2).getWeeklyOvertime().length);
				System.out.println("YES");
			}
		}

		return solution;
	}
	/**
	 * This method checks the feasibility of a schedule.
	 * @param schedule			the schedule
	 * @param i					????
	 * @return					a boolean denoting whether the shedule is feasible or not
	 */
	public boolean checkFeasibility(Schedule schedule, int i) {
		int[] check = schedule.getScheduleArray();
		if(check[i] == 1) {
			Request request = new Request(1, schedule.getC(), i);
			//System.out.println("atv:  " + this.feasCheck.isFeasible7(check, i-7, i+7) + " " + this.feasCheck.isFeasible14(check, i-14, i+14) + " " +  this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) + " " +  this.feasCheck.checkMax2SplitDuties(check));
			return  this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check); 
		}
		else if(instance.getFromRDutyNrToRDuty().containsKey(check[i])) {
			ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(check[i]);
			Request request = new Request(reserveDuty, schedule.getC(), i);
			//System.out.println("reserve: " + this.feasCheck.isFeasible7(check, i-7, i+7) + " " + this.feasCheck.isFeasible14(check, i-14, i+14) + " " +  this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) + " " +  this.feasCheck.checkMax2SplitDuties(check));
			return  this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check); 
		}
		else if(instance.getFromDutyNrToDuty().containsKey(check[i])) {
			Duty duty = instance.getFromDutyNrToDuty().get(check[i]);
			Request request = new Request(duty, schedule.getC(), i);
			//System.out.println("duty: " + this.feasCheck.isFeasible7(check, i-7, i+7) + " " + this.feasCheck.isFeasible14(check, i-14, i+14) + " " +  this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) + " " +  this.feasCheck.checkMax2SplitDuties(check));
			return  this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check); 
		}
		else {
			Request request = new Request(2, schedule.getC(), i);
			//System.out.println("rest :  " +this.feasCheck.isFeasible7(check, i-7, i+7) + " " + this.feasCheck.isFeasible14(check, i-14, i+14) + " " +  this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) + " " +  this.feasCheck.checkMax2SplitDuties(check));
			return this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check); 

		}
	}
	public boolean checkRelativeGroupSize(Solution solution, Schedule schedule) {
		double numberOfDrivers = 0;
		double numberOfDrivers1 = 0;
		double numberOfDrivers2 = 0;
		boolean check = false;
		for(ContractGroup group: solution.getNewSchedule().keySet()) {
			if(schedule.getC().equals(group)) {
				numberOfDrivers1 += schedule.getScheduleArray().length/7;
			}
			else {
				numberOfDrivers2 += solution.getNewSchedule().get(group).getScheduleArray().length/7;
			}
			
		}
		numberOfDrivers = numberOfDrivers1 + numberOfDrivers2;
		
		for(ContractGroup group: solution.getNewSchedule().keySet()) {
			if(schedule.getC().equals(group)) {
			 if( numberOfDrivers1/numberOfDrivers <= (group.getRelativeGroupSize()+0.05) && (group.getRelativeGroupSize()-0.05) <= numberOfDrivers1/numberOfDrivers) {
				 check = true;
				}
			}
			else {
				if( numberOfDrivers2/numberOfDrivers <= (group.getRelativeGroupSize() + 0.05) && (group.getRelativeGroupSize()- 0.05) <= numberOfDrivers2/numberOfDrivers) {
					check = true;
				}
			}
			
		}
		
		return check;
	}
}