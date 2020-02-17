package Tools;
import Tools.Request;
import Tools.ContractGroup;

import java.util.*;

public class Solution {

	private Set<Request> requests;
	private Map<ContractGroup, Schedule> newSchedule = new HashMap<ContractGroup,Schedule>();
	private final Instance instance;
	
	public Solution(Set<Request> requests, Map<ContractGroup,Schedule> schedule, Instance instance) {
		this.requests = requests;
		this.newSchedule = schedule;
		this.instance = instance;
	}
	
	
	/**
	 * This method executes a removal, i.e., it removes the request form its route, adds it to the request bank, 
	 * removes it from the list of served requests, and updates the total distance driven in this solution.
	 * @param request					the request to remove from the solution
	 * @param problem					the rich pick-up and delivery vehicle routing problem with time windows
	 */
	public void removeRequest(Request request, Solution solution, Set<TimeSlot> emptyTimeSlots, int dutyDay) {
		if(!requests.contains(request)) {
				TimeSlot slot = new TimeSlot(request.getGroup(), dutyDay);
				emptyTimeSlots.add(slot);
				int weekNumber = (int) Math.floor(dutyDay/7);
				int sum = 0;
				for(int k = 7*weekNumber; k < (7*weekNumber+6); k++) {
					if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(request.getGroup()).getScheduleArray()[k])) {
						sum += instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(request.getGroup()).getScheduleArray()[k]).getPaidMin();
					}
					else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(request.getGroup()).getScheduleArray()[k])) {
						sum +=solution.getNewSchedule().get(request.getGroup()).getC().getAvgHoursPerDay()*60;
					}
					else if(solution.getNewSchedule().get(request.getGroup()).getScheduleArray()[k] == 1) {
						sum += solution.getNewSchedule().get(request.getGroup()).getC().getAvgHoursPerDay()*60;
					}
					
					
				}
				
				solution.getNewSchedule().get(request.getGroup()).getWeeklyOvertime()[weekNumber] = sum;				
				
			solution.getNewSchedule().get(request.getGroup()).getScheduleArray()[dutyDay] = 2;
			
		this.requests.add(request);
		}
		
		
	}
	public Set<Request> getRequests() {
		return requests;
	}

	public void setRequests(Set<Request> requests) {
		this.requests = requests;
	}

	public Map<ContractGroup, Schedule> getNewSchedule() {
		return newSchedule;
	}

	public void setNewSchedule(Map<ContractGroup, Schedule> schedule) {
		this.newSchedule = schedule;
	}
	
	public void addRequest(Placement placement) {
		Schedule currentSchedule = this.getNewSchedule().get(placement.getTimeslot().getGroup()); 
		currentSchedule.getScheduleArray()[placement.getTimeslot().getDay()] = placement.getRequest().getDutyNumber(); 
		currentSchedule.setWeeklyOvertime(currentSchedule, instance); 
		this.requests.remove(placement.getRequest()); 
	}
	
	
	@Override
	public String toString() {
		return "Solution [ newSchedule=" + newSchedule + "]";
	}
	
	
	public double getObj() {
		//Define Objective value! 
		return 0.0; 
	}
	
	public Solution clone() {
		
		Map<ContractGroup, Schedule> copy = new HashMap<ContractGroup, Schedule>(); 
		
		for(ContractGroup group: this.newSchedule.keySet()) {
			copy.put(group, this.newSchedule.get(group).clone()); 
		}
		
		return new Solution(new HashSet<Request>(this.requests),copy, this.instance);
	}
}