package Phase5;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import Tools.ContractGroup;
import Tools.Instance;
import Tools.Schedule;

/**
 * This class stores a solution.
 * @author Mette Wagenvoort
 *
 */
public class Solution {

	private Set<Request> requests;
	private Map<ContractGroup, Schedule> newSchedule = new HashMap<ContractGroup,Schedule>();
	private final Instance instance;
	private FeasCheck feasCheck; 
	private final double[] softPenalties;
	private final double[] feasPenalties;
	private double[][] fairCounts;
	private Set<Placement> executedPlacements;

	public Solution(Set<Request> requests, Map<ContractGroup,Schedule> schedule, Instance instance) {
		this.requests = requests;
		this.newSchedule = schedule;
		this.instance = instance;
		this.feasCheck= new FeasCheck(instance);
		this.softPenalties = new Penalties().getSoftPenalties(); 
		this.feasPenalties = new Penalties().getFeasPenalties();
		this.fairCounts = this.feasCheck.getAllFairness(this);
		this.executedPlacements = new HashSet<>();
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

	/**
	 * This method executes a placement.
	 * @param placement
	 */
	public void addRequest(Placement placement) {
		this.executedPlacements.add(placement);
		Schedule currentSchedule = this.getNewSchedule().get(placement.getTimeslot().getGroup());  //get the previous schedule
		//System.out.println("duty number: "+ placement.getRequest().getDutyNumber());
		if(placement.getRequest().getDutyNumber() == 1) {
			int numberOfATVdays = 0;
			for(int i =0; i < currentSchedule.getScheduleArray().length; i++) {
				if(currentSchedule.getScheduleArray()[i] ==1) {
					numberOfATVdays++;
				}
			}
			if(numberOfATVdays >= (int) Math.floor((currentSchedule.getScheduleArray().length/7)/52.0 *placement.getRequest().getGroup().getATVPerYear())){
				this.requests.remove(placement.getRequest());
			}
			else {

				currentSchedule.getScheduleArray()[placement.getTimeslot().getDay()] = placement.getRequest().getDutyNumber();  //add the placement on the right day with the new duty number
				currentSchedule.setWeeklyOvertime(); //determine the weekly overtime 
				this.requests.remove(placement.getRequest());  //remove the request from the set of requests
			}
		}
		else {
			currentSchedule.getScheduleArray()[placement.getTimeslot().getDay()] = placement.getRequest().getDutyNumber();  //add the placement on the right day with the new duty number
			currentSchedule.setWeeklyOvertime(); //determine the weekly overtime 
			this.requests.remove(placement.getRequest());  //remove the request from the set of requests
		}
		this.fairCounts = this.feasCheck.getAllFairness(this);
	}

	@Override
	public String toString() {
		return "Solution [ newSchedule=" + newSchedule + "]";
	}

	public double getCosts() {
		double costs = 0;
		//Fixed costs number of employees 
		for(ContractGroup group: this.newSchedule.keySet()) {
			costs += this.newSchedule.get(group).getScheduleArray().length/7 * 13 *group.getAvgDaysPerWeek() * group.getAvgHoursPerDay() * 60; 
		}

		//Overtime 
		double[] allEmployeesOvertime = new double[instance.getContractGroups().size()];
		for(ContractGroup group:this.getNewSchedule().keySet()) {
			allEmployeesOvertime[group.getNr()-1] = this.feasCheck.QuarterlyOvertime(this.getNewSchedule().get(group).getScheduleArray(), group);
		}
		for(int i =0; i < allEmployeesOvertime.length; i++) {
			costs += this.feasPenalties[0] * allEmployeesOvertime[i];
		}

		return costs;
	}
	
	public double getFair() {
		double fairPen = 0;
		for(int i = 0; i<new Penalties().getFairPenalties().length; i++) {
			fairPen += this.feasCheck.getCoefVariance(this.fairCounts[i])* new Penalties().getFairPenalties()[i]; 
		}
		return fairPen;
	}

	/**
	 * Calculates the objective of a solution
	 * @return
	 */
	public double getObj() {
		double objective = 0.0;

		//Soft constraints
		for(ContractGroup group: this.getNewSchedule().keySet()) {
			int[] violations = this.feasCheck.allViolations(this.getNewSchedule().get(group).getScheduleArray(), group);

			for(int i =0; i < this.softPenalties.length; i++) {
				objective += violations[i]*this.softPenalties[i];
			}
		}

		//Overtime 
		double[] allEmployeesOvertime = new double[instance.getContractGroups().size()];
		for(ContractGroup group:this.getNewSchedule().keySet()) {
			allEmployeesOvertime[group.getNr()-1] = this.feasCheck.QuarterlyOvertime(this.getNewSchedule().get(group).getScheduleArray(), group);
		}

		for(int i =0; i < allEmployeesOvertime.length; i++) {
			objective += this.feasPenalties[0] * allEmployeesOvertime[i];
		}

		//Not solved request 
		objective += this.requests.size()* Penalties.penaltyRequest; 

		//Fixed costs number of employees 
		for(ContractGroup group: this.newSchedule.keySet()) {
			objective += this.newSchedule.get(group).getScheduleArray().length/7 * 13 *group.getAvgDaysPerWeek() * group.getAvgHoursPerDay() * 60; 
		}

		// Fairness 
		for(int i = 0; i<new Penalties().getFairPenalties().length; i++) {
			objective+= this.feasCheck.getCoefVariance(this.fairCounts[i])* new Penalties().getFairPenalties()[i]; 
		}
		return objective;
	}

	public double[][] getFeasCounts() {
		return this.fairCounts; 
	}

	public double getOvertime() {
		double totOvertime = 0;

		double[] allEmployeesOvertime = new double[instance.getContractGroups().size()];
		for(ContractGroup group:this.getNewSchedule().keySet()) {
			allEmployeesOvertime[group.getNr()-1] = this.feasCheck.QuarterlyOvertime(this.getNewSchedule().get(group).getScheduleArray(), group);
		}

		for(int i =0; i < allEmployeesOvertime.length; i++) {
			totOvertime += allEmployeesOvertime[i];
		}

		return totOvertime;
	}

	/**
	 * This method calculates the total number of minus hours.
	 * @return
	 */
	public double getMinusHours() {
		double totMinus = 0;

		double[] allEmployeesMinus = new double[instance.getContractGroups().size()];
		for (ContractGroup group : this.getNewSchedule().keySet()) {
			allEmployeesMinus[group.getNr()-1] = this.feasCheck.QuarterlyMinus(this.getNewSchedule().get(group).getScheduleArray(), group);
		}
		for (int i = 0; i < allEmployeesMinus.length; i++) {
			totMinus += allEmployeesMinus[i];
		}

		return totMinus;
	}

	/**
	 * This method determines the quarterly overtime for a contract group.
	 * @param solution
	 * @param c
	 * @return
	 */
	public double QuarterlyOvertime(int[] solution, ContractGroup c) {
		double totOvertime = 0;

		double[] weeklyOvertime = this.setWeeklyOvertime(solution, c);
		for(int empl = 0; empl < solution.length/7; empl++) {
			double overtime = 0;
			for(int i = 0; i < 13; i++) { //need to loop over 13 weeks for overtime
				overtime += weeklyOvertime[(empl+i)%weeklyOvertime.length];
			}
			totOvertime += Math.max(0, overtime);
		}
		return totOvertime;
	}

	/**
	 * This method sets the weekly overtime.
	 * @param schedule
	 * @param c
	 * @return
	 */
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

	/**
	 * This method creates a deep copy of this solution.
	 */
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

	public Set<Placement> getPlacements() {
		return executedPlacements;
	}
	
	public void printSoftViol() {
		for(ContractGroup group: this.getNewSchedule().keySet()) {
			int[] violations = this.feasCheck.allViolations(this.getNewSchedule().get(group).getScheduleArray(), group);
			System.out.println(group.getNr() + ": " + Arrays.toString(violations)); 
		}
	}
	
	public void printFairPen() {
		for(int i = 0; i<this.fairCounts.length; i++) {
			System.out.println("Fairness " + i +" :" + Arrays.toString(this.fairCounts[i])); 
		}
	}
}