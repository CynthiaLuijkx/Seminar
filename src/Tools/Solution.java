package Tools;
import Tools.Request;
import Tools.ContractGroup;

import java.util.*;

public class Solution {

	private Set<Request> requests;
	private Map<ContractGroup, Schedule> newSchedule = new HashMap<ContractGroup,Schedule>();
	private final Instance instance;
	private FeasCheck feasCheck; 
	private final double[] softPenalties;
	private final double[] feasPenalties; 

	public Solution(Set<Request> requests, Map<ContractGroup,Schedule> schedule, Instance instance) {
		this.requests = requests;
		this.newSchedule = schedule;
		this.instance = instance;
		this.feasCheck= new FeasCheck(instance);
		this.softPenalties = new Penalties().getSoftPenalties(); 
		this.feasPenalties = new Penalties().getFeasPenalties(); 
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
		currentSchedule.setWeeklyOvertime(); 
		this.requests.remove(placement.getRequest()); 
	}

	@Override
	public String toString() {
		return "Solution [ newSchedule=" + newSchedule + "]";
	}


	public Solution clone() {

		Map<ContractGroup, Schedule> copy = new HashMap<ContractGroup, Schedule>(); 

		for(ContractGroup group: this.newSchedule.keySet()) {
			copy.put(group, this.newSchedule.get(group).clone()); 
		}

		return new Solution(new HashSet<Request>(this.requests),copy, this.instance);
	}

	/**
	 * Calculates the objective of a solution
	 * @return
	 */
	public double getObj() {

		double objective = 0.0;
		
		//Soft constraints
		for(ContractGroup group: this.getNewSchedule().keySet()) {
			objective += this.QuarterlyOvertime(this.getNewSchedule().get(group).getScheduleArray(), group);
			int[] violations = this.feasCheck.allViolations(this.getNewSchedule().get(group).getScheduleArray(), group);

			for(int i =0; i < this.softPenalties.length; i++) {
				objective += violations[i]*this.softPenalties[i];
			}
		}

		//Overtime 
		double[] allEmployeesOvertime = new double[instance.getContractGroups().size()];
		for(ContractGroup group:this.getNewSchedule().keySet()) {
			allEmployeesOvertime[group.getNr()-1] = this.feasCheck.QuaterlyOvertime(this.getNewSchedule().get(group).getScheduleArray(), group);
		}

		for(int i =0; i < allEmployeesOvertime.length; i++) {
			objective += this.feasPenalties[0] * allEmployeesOvertime[i];
		}
		
		//Not solved request 
		objective += this.requests.size()* Penalties.penaltyRequest; 
		
		//Fixed costs number of employees 
		for(ContractGroup group: this.newSchedule.keySet()) {
			objective += this.newSchedule.get(group).getScheduleArray().length/7 * 13 *group.getAvgDaysPerWeek() * group.getAvgHoursPerDay(); 
		}

		return objective;
	}

	public double QuarterlyOvertime(int[] solution, ContractGroup c) {
		double overtime = 0;
		double[] weeklyOvertime = this.setWeeklyOvertime(solution, c);
		for(int empl = 0; empl < solution.length/7; empl++) {
			for(int i =0; i < 13; i++) { //need to loop over 13 weeks for overtime
				if((empl + i) < solution.length/7){
					if(weeklyOvertime[empl + i] > 0) {
						overtime = overtime + weeklyOvertime[empl +i];	
					}
				}
				else {
					int remainder = (empl + i) % solution.length/7;
					if(weeklyOvertime[remainder] > 0) {
						overtime = overtime + weeklyOvertime[remainder];		
					}
				}
			}
		}
		return overtime;
	}


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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Solution other = (Solution) obj;
		if (newSchedule == null) {
			if (other.newSchedule != null)
				return false;
		} else if (!newSchedule.equals(other.newSchedule))
			return false;
		return true;
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