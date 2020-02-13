package Tools;
import Tools.Request;
import Tools.ContractGroup;
import Tools.LSschedule;

import java.util.*;

public class Solution {


	private Set<Request> requests;
	private Map<ContractGroup, LSschedule> newSchedule = new HashMap<ContractGroup, LSschedule>();
	private final Instance instance;
	
	public Solution(Set<Request> requests, Map<ContractGroup,LSschedule> schedule, Instance instance) {
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
	public void removeRequest(Request request, Solution solution, Set<TimeSlot> emptyTimeSlots) {
		if(!requests.contains(request)) {
			for(int i =0; i <solution.getNewSchedule().get(request.getGroup()).getLSSchedule().getSchedule().length; i++ ) {
			if (solution.getNewSchedule().get(request.getGroup()).getLSSchedule().getSchedule()[i] == (request.getDuty().getNr())) {
				TimeSlot slot = new TimeSlot(request.getGroup(), i);
				emptyTimeSlots.add(slot);
				int weekNumber = (int) Math.floor(i/7);
				solution.getNewSchedule().get(request.getGroup()).getLSSchedule().getSchedule()[i] = 0; //set it empty = 0
				int sum = 0;
				for(int k = 7*weekNumber; k < (7*weekNumber+6); k++) {
					if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(request.getGroup()).getLSSchedule().getSchedule()[k])) {
						sum += instance.getFromDutyNrToDuty().get(k).getPaidMin();
					}
					else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(request.getGroup()).getLSSchedule().getSchedule()[k])) {
						sum +=solution.getNewSchedule().get(request.getGroup()).getLSSchedule().getC().getAvgHoursPerDay()*60;
					}
					else if(solution.getNewSchedule().get(request.getGroup()).getLSSchedule().getSchedule()[k] == 1) {
						sum += solution.getNewSchedule().get(request.getGroup()).getLSSchedule().getC().getAvgHoursPerDay()*60;
					}
					
					
				}
			
			solution.getNewSchedule().get(request.getGroup()).getWeeklyOvertime()[weekNumber] = sum;				
				}
			}
		this.requests.add(request);
		}
		
		
	}
	public Set<Request> getRequests() {
		return requests;
	}

	public void setRequests(Set<Request> requests) {
		this.requests = requests;
	}

	public Map<ContractGroup, LSschedule> getNewSchedule() {
		return newSchedule;
	}

	public void setNewSchedule(Map<ContractGroup, LSschedule> schedule) {
		this.newSchedule = schedule;
	}
	
	@Override
	public String toString() {
		return "Solution [requests=" + requests + ", newSchedule=" + newSchedule + "]";
	}
	
}
