package Phase5;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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
	
	private final double addWeek = 0;

	/**
	 * Constructor for the destroy heuristics.
	 * @param instance				the problem instance
	 */
	public DestroyHeuristics(Instance instance) {
		this.instance = instance;
		this.feasCheck = new FeasCheck(instance);
	}

	/**
	 * Check whether the relative groupsizes are still satisfied
	 * @param solution
	 * @param schedule
	 * @return
	 */
	public boolean checkRelativeGroupSize(Solution solution, Schedule schedule) {
		Map<Integer, Integer> numberOfDriversPCG = new HashMap<Integer, Integer>(); 
		
		for(ContractGroup group : instance.getContractGroups()) {
			int temp = solution.getNewSchedule().get(group).getSchedule().length/7; 
			if(group.equals(schedule.getC())){
				temp = schedule.getScheduleArray().length/7; 
			}
			if(numberOfDriversPCG.containsKey(group.getOriginalNr())) {
				numberOfDriversPCG.put(group.getOriginalNr(), numberOfDriversPCG.get(group.getOriginalNr() + temp)); 
			}else {
				numberOfDriversPCG.put(group.getOriginalNr(), temp); 
			}
		}
		
		double totalNDrivers = 0; 
		
		for(Integer groupNr: numberOfDriversPCG.keySet()) {
			totalNDrivers += numberOfDriversPCG.get(groupNr); 
		}
		
		for(Integer groupNr: numberOfDriversPCG.keySet() ) {
			double ratio = numberOfDriversPCG.get(groupNr)/totalNDrivers; 
			ContractGroup group = this.instance.getOGGroupsFromNr().get(groupNr); 
			if(!(ratio<=(group.getRelativeGroupSize()*1.05) &&(group.getRelativeGroupSize()*0.95) <= ratio )){
				return false; 
			}
		}
		return true; 
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
	public Solution executeRandom(Solution solution, int nRemove, Random random, Instance instancem, int n) {
		Set<TimeSlot> slots = new HashSet<>();
		int counter = 0;
		Set<Request> tabu = new HashSet<>();
		while(counter != nRemove) { //continue till we have removed the size of the neighborhood amount of duties
			int contGroup = random.nextInt(instance.getContractGroups().size()); //get a contract group
			//System.out.println(contGroup);
			for(ContractGroup group : instance.getContractGroups()) {
				if(group.getNr()-1 == contGroup) {
					int dutyDay = random.nextInt(solution.getNewSchedule().get(group).getScheduleArray().length); //get a random day in the schedule of the contract group
					//if the duty is a reserve duty, make a request consisting of a reserve duty and the corresponding day
					if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay])){
						Request request = new Request(instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay]), group, dutyDay);	
						if (!instance.isTabu(request)) {
							solution.removeRequest(request, solution, slots, dutyDay);
							counter++;
							tabu.add(request);
						}
					}
					//if the duty is a normal duty, make a request consisting of a normal duty and the corresponding day
					else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay])) {
						Request request = new Request(instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay]), group, dutyDay);	
						if (!instance.isTabu(request)) {
							solution.removeRequest(request, solution, slots, dutyDay);
							counter++;
							tabu.add(request);
						}
					}
					//if the duty is an ATV duty, make a request consisting of an ATV duty and the corresponding day
					else if(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay] == 1) {
						Request request = new Request(1, group, dutyDay);
						if (!instance.isTabu(request)) {
							solution.removeRequest(request, solution, slots, dutyDay);
							counter++;
							tabu.add(request);
						} 
					}
				} 
			}
		}
		//return the new solution with the ramdom removals
		instance.addTabuRequests(tabu, n);
		
		if (random.nextDouble() < this.addWeek) {
			int groupNr = random.nextInt(instance.getContractGroups().size());
			ContractGroup group = null;
			for (ContractGroup curGroup : instance.getContractGroups()) {
				if (curGroup.getNr() - 1 == groupNr) {
					group = curGroup;
					break;
				}
			}
			
			int[] newSchedule = new int[solution.getNewSchedule().get(group).getSchedule().length + 7];
			for (int i = 0; i < solution.getNewSchedule().get(group).getSchedule().length; i++) {
				newSchedule[i] = solution.getNewSchedule().get(group).getSchedule()[i];
			}
			for (int i = solution.getNewSchedule().get(group).getSchedule().length; i < newSchedule.length; i++) {
				newSchedule[i] = 2;
			}
			Schedule newschedule = new Schedule(group, newSchedule, (int) this.feasCheck.QuarterlyOvertime(newSchedule, group) );
			if (this.checkRelativeGroupSize(solution, newschedule)) {
				solution.getNewSchedule().get(group).setScheduleArray(newSchedule);
			}
			int nEmployees = newSchedule.length/7; 
			int ornATV = (int) Math.floor((nEmployees-1)/52.0*group.getATVPerYear()); 
			int newATV = (int) Math.floor((nEmployees)/52.0*group.getATVPerYear()); 
			 
			if(newATV> ornATV) {
				Request request = new Request(1, group, 0);
				solution.removeRequest(request, solution, slots, 0);
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
	public Solution executeRemoveWeek(Solution solution, Random random, Instance instance, int n) {
		int counter = 0; //can use counter if we want to remove multiple weeks
		int number = random.nextInt(instance.getContractGroups().size());
		ContractGroup group = new ContractGroup(0,0, 0, 0, 0, null);
		Set<TimeSlot> emptyTimeSlots = new HashSet<TimeSlot>();
		Set<Request> tabu = new HashSet<>();
		for(ContractGroup g: instance.getContractGroups()) {
			if(g.getNr()-1 == number) {
				group = g;
			}
		}
		int count = 0;
		outer: while(count != 100) {
			int index = random.nextInt(solution.getNewSchedule().get(group).getScheduleArray().length/7-1);
			//			System.out.println("index:  " +index);
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
				LinkedHashSet<Request> toRemove = new LinkedHashSet<>();
				boolean tabuFeasible = true;
				if(this.checkFeasibility(newschedule, (index*7)) && this.checkRelativeGroupSize(solution, newschedule)) {
					for(int k = 7*index; k <= index*7+6; k++) {
						if(solution.getNewSchedule().get(group).getScheduleArray()[k] == 1) {
							Request request = new Request(1, group, k);
							if (!instance.isTabu(request)) {
								toRemove.add(request);
							} else {
								tabuFeasible = false;
								break;
							}
						}
						else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[k])) {	
							ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[k]);
							Request request = new Request(reserveDuty, group, k);
							if (!instance.isTabu(request)) {
								toRemove.add(request);
							} else {
								tabuFeasible = false;
								break;
							}
						}
						else if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[k])) {
							Duty duty = instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[k]);
							Request request =  new Request(duty, group, k);
							if (!instance.isTabu(request)) {
								toRemove.add(request);
							} else {
								tabuFeasible = false;
								break;
							}
						}
					}

					if (tabuFeasible) {
						for (Request req : toRemove) {
							solution.removeRequest(req, solution, emptyTimeSlots, req.getDay());
							tabu.add(req);
						}
						solution.getNewSchedule().get(group).setScheduleArray(newSchedule);
						break outer;
					} else {
						count++;
					}
				}
			}
			else {
				count++;
			}
		}
		instance.addTabuRequests(tabu, n);
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
			//			System.out.println("atv:  " + this.feasCheck.isFeasible7(check, i-7, i+7) + " " + this.feasCheck.isFeasible14(check, i-14, i+14) + " " +  this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) + " " +  this.feasCheck.checkMax2SplitDuties(check));
			return this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check) && this.feasCheck.checkSundays(check) && this.feasCheck.numDutiesFeasible(check) && this.feasCheck.checkATVWeekends(check); 
		}
		else if(instance.getFromRDutyNrToRDuty().containsKey(check[i])) {
			ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(check[i]);
			Request request = new Request(reserveDuty, schedule.getC(), i);
			//			System.out.println("reserve: " + this.feasCheck.isFeasible7(check, i-7, i+7) + " " + this.feasCheck.isFeasible14(check, i-14, i+14) + " " +  this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) + " " +  this.feasCheck.checkMax2SplitDuties(check));
			return this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check) && this.feasCheck.checkSundays(check) && this.feasCheck.numDutiesFeasible(check) && this.feasCheck.checkATVWeekends(check); 
		}
		else if(instance.getFromDutyNrToDuty().containsKey(check[i])) {
			Duty duty = instance.getFromDutyNrToDuty().get(check[i]);
			Request request = new Request(duty, schedule.getC(), i);
			//			System.out.println("duty: " + this.feasCheck.isFeasible7(check, i-7, i+7) + " " + this.feasCheck.isFeasible14(check, i-14, i+14) + " " +  this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) + " " +  this.feasCheck.checkMax2SplitDuties(check));
			return this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check) && this.feasCheck.checkSundays(check) && this.feasCheck.numDutiesFeasible(check) && this.feasCheck.checkATVWeekends(check); 
		}
		else {
			Request request = new Request(2, schedule.getC(), i);
			//			System.out.println("rest :  " +this.feasCheck.isFeasible7(check, i-7, i+7) + " " + this.feasCheck.isFeasible14(check, i-14, i+14) + " " +  this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) + " " +  this.feasCheck.checkMax2SplitDuties(check));
			return this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check) && this.feasCheck.checkSundays(check) && this.feasCheck.numDutiesFeasible(check) && this.feasCheck.checkATVWeekends(check); 
		}
	}

	//---------------------- Swap Removal ---------------------------------------------------------------------------------------------------------
	/**
	 * This method removes a week from a contract group and extends the planning horizon of another contract group.
	 * @param solution
	 * @param random
	 * @param instance
	 * @return
	 */
	public Solution executeSwapWeek(Solution solution, Random random, Instance instance, int n) {
		int number = random.nextInt(instance.getContractGroups().size());
		int number2 = random.nextInt(instance.getContractGroups().size());
		while(number == number2) {
			number2 = random.nextInt(instance.getContractGroups().size());
		}
		ContractGroup group1 = new ContractGroup(0,0, 0, 0, 0, null);
		ContractGroup group2 = new ContractGroup(0,0, 0, 0, 0, null);
		Set<TimeSlot> emptyTimeSlots = new HashSet<TimeSlot>();
		Set<Request> tabu = new HashSet<>();
		for(ContractGroup g: instance.getContractGroups()) {
			if(g.getNr()-1 == number) {
				group1 = g;
			} else if (g.getNr() - 1 == number2) {
				group2 = g;
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
					boolean tabuFeasible = true;
					LinkedHashSet<Request> toRemove = new LinkedHashSet<>();
					for(int k = 7*index; k <= index*7+6; k++) {
						if(solution.getNewSchedule().get(group1).getScheduleArray()[k] == 1) {
							Request request = new Request(1, group1, k);
							if (!instance.isTabu(request)) {
								toRemove.add(request);
							} else {
								tabuFeasible = false;
								break;
							}
						}
						else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group1).getScheduleArray()[k])) {	
							ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group1).getScheduleArray()[k]);
							Request request = new Request(reserveDuty, group1, k);
							if (!instance.isTabu(request)) {
								toRemove.add(request);
							} else {
								tabuFeasible = false;
								break;
							}
						}
						else if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group1).getScheduleArray()[k])) {
							Duty duty = instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group1).getScheduleArray()[k]);
							Request request =  new Request(duty, group1, k);
							if (!instance.isTabu(request)) {
								toRemove.add(request);
							} else {
								tabuFeasible = false;
								break;
							}
						}
					}

					if (tabuFeasible) {
						for (Request req : toRemove) {
							solution.removeRequest(req, solution, emptyTimeSlots, req.getDay());
							tabu.add(req);
						}
						solution.getNewSchedule().get(group1).setScheduleArray(newSchedule);
						break outer;
					} else {
						count++;
					}
				}
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
			//			System.out.println(newSchedule.length/7 + " " + schedule.getWeeklyOvertime().length);

			if(this.checkRelativeGroupSize(solution, schedule)) {
				solution.getNewSchedule().get(group2).setScheduleArray(newSchedule);
				solution.setWeeklyOvertime(newSchedule, group2);
				//				System.out.println("2: "+ solution.getNewSchedule().get(group2).getWeeklyOvertime().length);
				//				System.out.println("YES");
			}
		}
		instance.addTabuRequests(tabu, n);
		return solution;
	}

	//---------------------- Random Day Duty Removal ----------------------------------------------------------------------------------------------
	/**
	 * This method randomly chooses a weekday and removes all duties on that day of a particular duty type in all contract groups.
	 * @param solution
	 * @param random
	 * @param n
	 * @return
	 */
	public Solution executeRandomDayDuty(Solution solution, Random random, int n) {
		Set<TimeSlot> slots = new HashSet<>();
		int day = random.nextInt(7); 
		String dutyType = randomElement(instance.getDutyTypesLinked(), random);
		for(ContractGroup group: instance.getContractGroups()) {
			int[] scheduleArray = solution.getNewSchedule().get(group).getScheduleArray(); 
			for(int i = day; i< solution.getNewSchedule().get(group).getScheduleArray().length; i+=7) {
				if(instance.getDutyTypeFromDutyNR(scheduleArray[i]).equals(dutyType)) {
					if(instance.getFromDutyNrToDuty().containsKey(scheduleArray[i])){
						Request request = new Request(instance.getFromDutyNrToDuty().get(scheduleArray[i]), group, i);	
						solution.removeRequest(request, solution, slots,i);
					}
					//if the duty is a normal duty, make a request consisting of a normal duty and the corresponding day
					else if(instance.getFromRDutyNrToRDuty().containsKey(scheduleArray[i])) {
						Request request = new Request(instance.getFromRDutyNrToRDuty().get(scheduleArray[i]), group, i);	
						solution.removeRequest(request, solution, slots, i);
					}
					//if the duty is an ATV duty, make a request consisting of an ATV duty and the corresponding day
					else if(scheduleArray[i] == 1) {
						Request request = new Request(1, group, i);
						solution.removeRequest(request, solution, slots, i);
					}
				}
			}
		}
		Set<Request> tabu = new HashSet<>();
		instance.addTabuRequests(tabu, n);
		return solution; 
	}
	
	public static <E> E randomElement(LinkedHashSet<E> set, Random random) {
		int size = set.size();
		int item = random.nextInt(size); // In real life, the Random object should be rather more shared than this
		int i = 0;
		for(E obj : set)
		{
			if (i == item)
				return obj;
			i++;
		}
		return null;
	}
	
	public Solution removeATV(Solution solution) {
		Set<TimeSlot> slots = new HashSet<>();
		for(ContractGroup group : instance.getContractGroups()) {
				if(group.getATVPerYear()>0) {
					for(int i =0; i < solution.getNewSchedule().get(group).getScheduleArray().length; i++) {
						if(solution.getNewSchedule().get(group).getScheduleArray()[i] == 1 && (i%7 == 0 || i%7==6)) {
							Request request = new Request(1, group, i);
							solution.removeRequest(request, solution, slots, i);
						}
					}
				}
		}
		return solution; 
	}
}