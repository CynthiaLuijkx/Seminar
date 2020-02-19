package Tools;
import java.util.*;

public class DestroyHeuristics {
	public DestroyHeuristics() {
	}
	
	//---Random Removal
	public Solution executeRandom(Solution solution, int nRemove, Random random, Instance instance) {
		Set<TimeSlot> slots = new HashSet<>();
		int counter = 0;
		while(counter != nRemove) { //continue till we have removed the size of the neighborhood amount of duties
			int contGroup = random.nextInt(solution.getNewSchedule().keySet().size()); //get a contract group
			//System.out.println(contGroup);
			for(ContractGroup group: solution.getNewSchedule().keySet()) {
				if(group.getNr()-1 == contGroup) {
				 int dutyDay = random.nextInt(group.getTc()-1); //get a random day in the schedule of the contract group
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
	
	//---Remove largest duties in weeks with most overtime and remove smallest duties in weeks with least overtime
		public Solution executeRandomOvertime(Solution solution, int nRemove, Random random, Instance instance) {
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
			
			/*//for every group, find the largest duty in the week to remove
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
			}*/
			return solution; 
		}

}