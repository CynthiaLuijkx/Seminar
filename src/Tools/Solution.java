package Tools;
import Tools.Request;
import Tools.ContractGroup;

import java.util.*;

//This class is the solution information we want to have in every iteration of the adaptive large neighborhood search
public class Solution {

	private Set<Request> requests; //list with requests 
	private Map<ContractGroup, Schedule> newSchedule = new HashMap<ContractGroup,Schedule>(); //the new schedule of the solution
	private final Instance instance; //the instance information
	
	//constructor of the solution
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
		//if the request is not in there yet
		if(!requests.contains(request)) {
				TimeSlot slot = new TimeSlot(request.getGroup(), dutyDay);
				emptyTimeSlots.add(slot);
				//determine in which week the request needs to be removed from
				int weekNumber = (int) Math.floor(dutyDay/7);
				int sum = 0;
				//determine the total worked hours in that week
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
				//set the weekly overtime/minustime
				solution.getNewSchedule().get(request.getGroup()).getWeeklyOvertime()[weekNumber] = sum- request.getGroup().getAvgHoursPerDay()*request.getGroup().getAvgDaysPerWeek()*60 ;				
				//set the request to a rest day
				solution.getNewSchedule().get(request.getGroup()).getScheduleArray()[dutyDay] = 2;
			
		this.requests.add(request); //add the request to the list of requests
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
	//add a request to the schedule
	public void addRequest(Placement placement) {
		Schedule currentSchedule = this.getNewSchedule().get(placement.getTimeslot().getGroup());  //get the previous schedule
		//System.out.println("duty number: "+ placement.getRequest().getDutyNumber());
		currentSchedule.getScheduleArray()[placement.getTimeslot().getDay()] = placement.getRequest().getDutyNumber();  //add the placement on the right day with the new duty number
		currentSchedule.setWeeklyOvertime(currentSchedule, instance); //determine the weekly overtime 
		this.requests.remove(placement.getRequest());  //remove the request from the set of requests
	}
	
	
	@Override
	public String toString() {
		return "Solution [ newSchedule=" + newSchedule + "]";
	}
	
	//Determine the objective
	public double getObj() {
		double objective = 0.0;
		for(ContractGroup group: this.getNewSchedule().keySet()) {
			objective += this.QuarterlyOvertime(this.getNewSchedule().get(group).getScheduleArray(), group);
		}

		return objective;
	}
	//determine the quarterly overtime
	public double QuarterlyOvertime(int[] solution, ContractGroup c) {
		double overtime = 0;
		double[] weeklyOvertime = this.setWeeklyOvertime(solution, c);
		for(int empl = 0; empl < solution.length/7; empl++) {
			
			for(int i =0; i < 13; i++) { //need to loop over 13 weeks for overtime
					int remainder = (empl + i) % solution.length/7;
					if(weeklyOvertime[remainder] > 0) {
						overtime = overtime + weeklyOvertime[remainder];		
					}
				}
			
		}
		return overtime;
	}
	//determine the weekly overtime
	public double[] setWeeklyOvertime(int[] schedule, ContractGroup c) {
		int sum = 0;
		double[] weeklyOvertime = new double[schedule.length/7];
		for(int  k = 0; k < (schedule.length/7); k++) {
			sum = 0;
			for(int i = 7*k; i < (7*k+6); i++) {
				if(instance.getFromDutyNrToDuty().containsKey(schedule[i])) {
					sum += instance.getFromDutyNrToDuty().get(schedule[i]).getPaidMin();
				}
				else if(instance.getFromRDutyNrToRDuty().containsKey(schedule[i])) {
					sum += c.getAvgHoursPerDay()*60;
				}
				else if(schedule[i] == 1) {
					sum += c.getAvgHoursPerDay()*60;
				}
			}

			weeklyOvertime[k] = sum - (c.getAvgDaysPerWeek()*c.getAvgHoursPerDay()*60) ;
		}
		return weeklyOvertime;
	}
	//to clone a solution
	public Solution clone() {
		
		Map<ContractGroup, Schedule> copy = new HashMap<ContractGroup, Schedule>(); 
		
		for(ContractGroup group: this.newSchedule.keySet()) {
			copy.put(group, this.newSchedule.get(group).clone()); 
		}
		
		return new Solution(new HashSet<Request>(this.requests),copy, this.instance);
	}
	/**
	 * This method determines the hashcode of a solution.
	 * @param sol						the solution
	 * @return							its hashcode
	 */
	public int getHashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(this.getObj());
		result = prime * result + (int) (temp ^ (temp >>> 32));

		for(ContractGroup group: this.getNewSchedule().keySet()) {
			Schedule schedule = this.getNewSchedule().get(group); 
			result = prime*result + schedule.getScheduleArray().hashCode();  
		}
		return result;
	}
}