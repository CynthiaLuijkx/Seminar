package Tools;
import java.util.*;

public class DestroyHeuristics {
	public DestroyHeuristics() {
	}
	
	//---Random Removal
	public Solution executeRandom(Solution solution, int nRemove, Random random, Instance instance) {
		Set<TimeSlot> slots = new HashSet<>();
		for(int i =0; i < nRemove; i++) {
			int contGroup = random.nextInt(solution.getNewSchedule().keySet().size()-1)+1;
			for(ContractGroup group: solution.getNewSchedule().keySet()) {
				if(group.getNr() == contGroup) {
				 int dutyDay = random.nextInt(group.getTc()-1);
				 if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay])){
					Request request = new Request(instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay]), group, dutyDay);	
					solution.removeRequest(request, solution, slots, dutyDay);
				 }
				 else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay])) {
					 Request request = new Request(instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[dutyDay]), group, dutyDay);	
					solution.removeRequest(request, solution, slots, dutyDay);
						 
				 }
			}
		    
		  }
		}
		
		return solution; 
	}
	
	//---Remove largest duties in weeks with most overtime and remove smallest duties in weeks with least overtime
		public Solution executeRandomOvertime(Solution solution, int nRemove, Random random, Instance instance) {
			Set<TimeSlot> slots = new HashSet<TimeSlot>();
			Map<ContractGroup, List<Integer>> removalDuties = new HashMap<ContractGroup, List<Integer>>(); 
			Map<ContractGroup,double[]> weeklyOvertimePGroup = new HashMap<ContractGroup,double[]>();
			for(ContractGroup group: solution.getNewSchedule().keySet()) {
				double[] weeklyOvertime = solution.getNewSchedule().get(group).getWeeklyOvertime();
				weeklyOvertimePGroup.put(group, weeklyOvertime);
				removalDuties.put(group, new ArrayList<Integer>());
				
			}
			//Find the biggest weeks
			for(int i =0; i < nRemove/2; i++) {
				double check = 0;
				ContractGroup groupToAdd = new ContractGroup(0,0,0,0,0,null);
				int index = -1;
				for(ContractGroup group: instance.getContractGroups()) {
					for(int j = 0; j < weeklyOvertimePGroup.get(group).length; j++) {
						if(weeklyOvertimePGroup.get(group)[j] > check && !removalDuties.get(group).contains(j)) {
							check = weeklyOvertimePGroup.get(group)[j];
							index = j;
							groupToAdd = group;
						}
					}
				}
				removalDuties.get(groupToAdd).add(index);
			}

			
			//Find the smallest weeks
			for(int i = nRemove/2; i < nRemove; i++) {
				double check = Double.POSITIVE_INFINITY;
				ContractGroup groupToAdd = new ContractGroup(0,0,0,0,0,null);
				int index = -1;
				for(ContractGroup group: instance.getContractGroups()) {
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
			for(ContractGroup group: instance.getContractGroups()) {
				double dutyTime = 0;
				int dutyNumber = 0;
				ReserveDutyType saveReserveDuty = new ReserveDutyType(null, null, 0,0,0,0);
				Duty saveDuty = new Duty(0,null,0,0,0,0, null,0);
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
								 }
							}
							else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[w])){
								ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[w]);
							 	System.out.println(reserveDuty);
								if(reserveDuty.getEndTime()- reserveDuty.getStartTime() > dutyTime) {
								 dutyTime = reserveDuty.getEndTime()- reserveDuty.getStartTime();
								 dutyNumber = reserveDuty.getNr();
								 saveReserveDuty = reserveDuty;
							 }
							}
							else if(solution.getNewSchedule().get(group).getScheduleArray()[index] == 1) {
								if(group.getAvgHoursPerDay()*60 > dutyTime) {
									dutyTime = group.getAvgHoursPerDay()*60;
									dutyNumber = 1;
								}
							}
							//If we have a rest duty
							else{
								if(dutyTime == 0) {
									dutyTime = 0;
									dutyNumber = 2;
								}
							}
						   
						}
						if(dutyNumber == 1 || dutyNumber == 2) {
						Request request = new Request(dutyNumber, group, index);
						solution.removeRequest(request, solution, slots, index);
						}
						else if(dutyNumber<= 1000) {
							Request request = new Request(saveReserveDuty, group, index);
							solution.removeRequest(request, solution,slots, index);
						}
						else {
							Request request = new Request(saveDuty, group, index);
							solution.removeRequest(request, solution,slots, index);	
						}
				    }
					// find the second smallest duty
					else {
						dutyTime = Double.POSITIVE_INFINITY;
						dutyNumber = 0;
						double secondSmallest = Double.POSITIVE_INFINITY;
						int secondSmallestNumber = 0;
						for(int w = index*7; w <= index*7+6; w++) {
							if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[w])) {
								Duty duty = instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[w]);
								 if(duty.getEndTime()- duty.getStartTime() < dutyTime) {
									 dutyTime = duty.getEndTime()- duty.getStartTime();
									 dutyNumber = duty.getNr();
								 }
								 else if(duty.getEndTime()- duty.getStartTime() < secondSmallest) {
									 secondSmallest = duty.getEndTime()- duty.getStartTime();
									 secondSmallestNumber = duty.getNr();
									 saveDuty = duty;
								 }
							}
							else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getScheduleArray()[w])){
								ReserveDutyType reserveDuty = instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getScheduleArray()[w]);
							 	System.out.println(reserveDuty);
								if(reserveDuty.getEndTime()- reserveDuty.getStartTime() < dutyTime) {
								 dutyTime = reserveDuty.getEndTime()- reserveDuty.getStartTime();
								 dutyNumber = reserveDuty.getNr();
							 }
							 	else if(reserveDuty.getEndTime()- reserveDuty.getStartTime() < secondSmallest) {
									 secondSmallest = reserveDuty.getEndTime()- reserveDuty.getStartTime();
									 secondSmallestNumber = reserveDuty.getNr();
									 saveReserveDuty = reserveDuty;
								 }
							}
							else if(solution.getNewSchedule().get(group).getScheduleArray()[index] == 1) {
								if(group.getAvgHoursPerDay()*60 < dutyTime) {
									dutyTime = group.getAvgHoursPerDay()*60;
									dutyNumber = 1;
								}
								else if(group.getAvgHoursPerDay()*60 < secondSmallest) {
									secondSmallest = group.getAvgHoursPerDay()*60;
									secondSmallestNumber = 1;
								}
							}
							//If we have a rest duty
							else{
								if(dutyTime == 0) {
									dutyTime = 0;
									dutyNumber = 2;
								}
								else if(secondSmallest ==0) {
									secondSmallest = 0;
									secondSmallestNumber = 2;
								}
							}
						   
						}
						if(secondSmallestNumber == 1 || secondSmallestNumber == 2) {
							Request request = new Request(secondSmallestNumber, group, index);
							solution.removeRequest(request, solution, slots, index);
							}
							else if(secondSmallestNumber<= 1000) {
								Request request = new Request(saveReserveDuty, group, index);
								solution.removeRequest(request, solution,slots, index);
							}
							else {
								Request request = new Request(saveDuty, group, index);
								solution.removeRequest(request, solution,slots, index);	
							}
				    }
				}
			}
			return solution; 
		}

}