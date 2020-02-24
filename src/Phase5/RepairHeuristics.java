package Phase5;
import java.util.*;

import Phase5.Placement;
import Tools.ContractGroup;
import Tools.Instance;
import Tools.Schedule;

/**
 * This method stores the repair heuristics.
 * @author Mette Wagenvoort
 *
 */
public class RepairHeuristics {
	private FeasCheck feasCheck; 
	private double[] softFeas;
	private double[] feasPen; 

	public RepairHeuristics(Instance instance) {
		this.feasCheck = new FeasCheck(instance); 
		this.softFeas = new Penalties().getSoftPenalties();
		this.feasPen = new Penalties().getFeasPenalties(); 
	}

	//---------------------- Regret Repair --------------------------------------------------------------------------------------------------------

	/**
	 * This method executes a regret repair in which the best placement is executed for the request that has the highest regret.
	 * @param forwardCheck
	 * @param checkBestFirst
	 * @param solution
	 * @return
	 */
	public Solution regretRepair(int forwardCheck, int checkBestFirst, Solution solution) {

		while(solution.getRequests().size()!= 0) {

			ArrayList<Placement> allPlacements = new ArrayList<Placement>(); 
			for(Request request: solution.getRequests()) {
				allPlacements.addAll(request.getPlacements()); 
			}

			Collections.sort(allPlacements);
			double bestCosts = Double.MAX_VALUE; 
			Placement bestRegretPlacement = null; 
			Solution tempSol = solution.clone();

			for(int j = 0; j<checkBestFirst; j++) {
				double regret = 0;
				Placement firstPlaced = allPlacements.get(0); 
				tempSol.addRequest(firstPlaced);

				for(Placement placement: allPlacements) {
					if(placement.getTimeslot().getDay() == firstPlaced.getTimeslot().getDay() && placement.getTimeslot().getGroup().equals(firstPlaced.getTimeslot().getGroup())) {
						allPlacements.remove(placement); 
					}
				}

				List<Placement> copyAllPlacements = copyPlacementList(allPlacements); 

				for(int i= 0; i<forwardCheck; i++) {
					Placement bestPlacement = Collections.max(copyAllPlacements); 
					tempSol.addRequest(bestPlacement); //This method also deletes the request placed

					for(Placement placement: allPlacements) {
						if(placement.getTimeslot().getDay() == bestPlacement.getTimeslot().getDay() && placement.getTimeslot().getGroup().equals(bestPlacement.getTimeslot().getGroup())) {
							copyAllPlacements.remove(placement); 
						}
					}

					for(Request request: tempSol.getRequests()) {
						updatePlacements(request, tempSol, bestPlacement.getTimeslot().getGroup(), bestPlacement.getTimeslot().getDay());
					}
					regret += bestPlacement.getCost(); 

				}

				if(regret<bestCosts) {
					bestRegretPlacement = firstPlaced; 
					bestCosts = regret; 
				}

			}
			solution.addRequest(bestRegretPlacement);
		}
		return solution; 
	}

	/**
	 * This method makes a deep copy of a placement list.
	 * @param placements
	 * @return
	 */
	public List<Placement> copyPlacementList(List<Placement> placements){
		ArrayList<Placement> copyPlacements = new ArrayList<Placement>(); 

		for(Placement placement:placements) {
			copyPlacements.add(placement.duplicate()); 
		}
		return copyPlacements; 
	}

	/**
	 * This methods initialises the placements.
	 * @param solution
	 * @return
	 */
	public List<Placement> setAllPlacements(Solution solution){
		List<Placement> placements = new ArrayList<Placement>(); 
		for(Request request: solution.getRequests()) {
			placements.addAll(setPlacements(request, solution, solution.getNewSchedule().keySet())); 
		}
		return placements; 
	}

	/**
	 * This methods sets a placement for a duty.
	 * @param request
	 * @param solution
	 * @param groups
	 * @return
	 */
	public List<Placement> setPlacements(Request request, Solution solution, Set<ContractGroup> groups){
		ArrayList<Placement> updatedPlacements = new ArrayList<Placement>(); 
		for(ContractGroup group: groups) {
			if(request.getDutyNumber() == 1) {
				if(group.getDutyTypes().contains("ATV")){
					request.deletePlacements(group);
					Schedule schedule = solution.getNewSchedule().get(group); 
					for(int i = 0; i < schedule.getScheduleArray().length; i++) {
						if(solution.getNewSchedule().get(group).getScheduleArray()[i] == 2) {
							if(this.checkFeasibility(schedule, i, request)) {
								double costOfPlacement = calculateCosts(schedule, i, request); 
								Placement newPlacement = new Placement(request, new TimeSlot(group,i), costOfPlacement); 
								request.addPlacement(newPlacement);
								updatedPlacements.add(newPlacement); 
							}
						}
					}
				}
			}
		   else if(group.getDutyTypes().contains("W")) {
				if(request.getDutyNumber() > 1000) {
					if(request.getDuty().getType().equals("W")) {
						request.deletePlacements(group);
						Schedule schedule = solution.getNewSchedule().get(group); 
						for(int i = 0; i < schedule.getScheduleArray().length; i++) {
							if(solution.getNewSchedule().get(group).getScheduleArray()[i] == 2) {
								if(this.checkFeasibility(schedule, i, request)) {
									double costOfPlacement = calculateCosts(schedule, i, request); 
									Placement newPlacement = new Placement(request, new TimeSlot(group,i), costOfPlacement); 
									request.addPlacement(newPlacement);
									updatedPlacements.add(newPlacement); 
								}
							}
						}
					 }
				else {
						request.deletePlacements(group);
						Schedule schedule = solution.getNewSchedule().get(group); 
						for(int i = request.getWeekday(); i < schedule.getScheduleArray().length; i+=7) {
							if(solution.getNewSchedule().get(group).getScheduleArray()[i] == 2) {
								if(this.checkFeasibility(schedule, i, request)) {
									double costOfPlacement = calculateCosts(schedule, i, request); 
									Placement newPlacement = new Placement(request, new TimeSlot(group,i), costOfPlacement); 
									request.addPlacement(newPlacement);
									updatedPlacements.add(newPlacement); 
								}
							}
						}
					}
				}
			}
			else {
				request.deletePlacements(group);
				Schedule schedule = solution.getNewSchedule().get(group); 
				for(int i = request.getWeekday(); i < schedule.getScheduleArray().length; i+=7) {
					if(solution.getNewSchedule().get(group).getScheduleArray()[i] == 2) {
						if(this.checkFeasibility(schedule, i, request)) {
							double costOfPlacement = calculateCosts(schedule, i, request); 
							Placement newPlacement = new Placement(request, new TimeSlot(group,i), costOfPlacement); 
							request.addPlacement(newPlacement);
							updatedPlacements.add(newPlacement); 
						}
					}
				}
			}
		}
		return updatedPlacements;
		
	}

	/**
	 * This method updates the placements when another placement is executed.
	 * @param request
	 * @param solution
	 * @param group
	 * @param changedDay
	 * @return
	 */
	public List<Placement> updatePlacements(Request request, Solution solution, ContractGroup group, int changedDay){
		Schedule schedule = solution.getNewSchedule().get(group);
		ArrayList<Placement> updatedPlacements = new ArrayList<Placement>(); 
		Set<Placement> toRemove = new HashSet<Placement>(); 
		for(Placement placement:request.getPlacements()) {
			if(placement.getTimeslot().getGroup().equals(group)) {
				int i = placement.getTimeslot().getDay();  
				if(i == changedDay) {
					toRemove.add(placement); 
				}else {
					if(checkFeasibility(solution.getNewSchedule().get(group), i, request)) {
						double costOfPlacement = calculateCosts(schedule, i, request); 
						placement.setCost(costOfPlacement);
						updatedPlacements.add(placement); 
					}
					else {
						toRemove.add(placement);
					}
				}
			}
		}

		for(Placement placement: toRemove) {
			request.deletePlacement(placement);
		}
		return updatedPlacements; 
	}

	//---------------------- Regret Repair 2 ------------------------------------------------------------------------------------------------------
	/**
	 * This method executes a regret repair in which the best placement is executed for the request that has the highest regret.
	 * @param solution
	 * @param q
	 * @return
	 */
	public Solution regretRepair2(Solution solution, int q) {

		while(solution.getRequests().size()!=0) {

			Request mostRegretRequest = null; 
			double maxRegret = - Double.MAX_VALUE; 
			for(Request request: solution.getRequests()) {
				List<Placement> placements = request.getPlacements(); 
				Collections.sort(placements);

				double regret = 0; 
				if(placements.size() != 0) {
					if(placements.size() == 1 ) {
						regret = Double.MAX_VALUE; 
					}else if(placements.size() <= q) {
						regret = Double.MAX_VALUE/2; 
					}else {
						regret = placements.get(q).getCost() - placements.get(0).getCost();	
					}

					if(regret > maxRegret) {
						maxRegret = regret; 
						mostRegretRequest = request; 
					}
				}

			}

			if(mostRegretRequest == null) {
				System.out.println("No more placements possible");
				break; 
			}

			Placement bestPlacement = mostRegretRequest.getPlacements().get(0); 
			//System.out.println(bestPlacement.getTimeslot().getDay() +  "  "+ bestPlacement.toString() );
			solution.addRequest(bestPlacement); //This method also deletes the request placed
			//System.out.println("Placed same spot as before: " +( bestPlacement.getTimeslot().getDay()== bestPlacement.getRequest().getDay() && bestPlacement.getTimeslot().getGroup().equals(bestPlacement.getRequest().getGroup()))); 
			//System.out.println("Added: " + bestPlacement.getRequest().toString()); 

			deleteInvalidPlacements(bestPlacement, solution); 

			for(Request request: solution.getRequests()) {
				updatePlacements(request, solution, bestPlacement.getTimeslot().getGroup(), bestPlacement.getTimeslot().getDay());
			}
		}
		return solution; 
	}

	//---------------------- Greedy Repair --------------------------------------------------------------------------------------------------------
	/**
	 * This method executes a greedy repair in which the placement is executed which leads to the lowest change in objective value.
	 * @param solution
	 * @param q
	 * @return
	 */
	public Solution greedyRepair(Solution solution){

		while(solution.getRequests().size()!=0) { //until all requests are placed

			Request bestRequest = null; 
			double minCosts = Double.MAX_VALUE; 
			for(Request request: solution.getRequests()) {
				List<Placement> placements = request.getPlacements(); 
				Collections.sort(placements);
				if(placements.size()!=0) {
					if(placements.get(0).getCost() < minCosts) {
						minCosts = placements.get(0).getCost(); 
						bestRequest = request; 
					}
				}
			}

			if(bestRequest == null) {
				System.out.println("No more placements possible");
				break; 
			}

			Placement bestPlacement = bestRequest.getPlacements().get(0); 
			solution.addRequest(bestPlacement); //This method also deletes the request placed

			//System.out.println("Added: " + bestPlacement.getRequest().toString()); 

			deleteInvalidPlacements(bestPlacement, solution); 

			for(Request request: solution.getRequests()) {
				updatePlacements(request, solution, bestPlacement.getTimeslot().getGroup(), bestPlacement.getTimeslot().getDay());
			}
		}
		return solution; 
	}


	/**
	 * This method deletes invalid placements.
	 * @param bestPlacement
	 * @param solution
	 */
	public void deleteInvalidPlacements(Placement bestPlacement, Solution solution) {
		for(Request request: solution.getRequests()) {
			for(Placement placement : request.getPlacements()) {
				if(placement.getTimeslot().equals(bestPlacement.getTimeslot())) {
					request.deletePlacement(placement);
					break; 
				}
			}
		}
	}


	/**
	 * Calculates the costs of a placement.
	 * @param lsschedule 	Stores the schedule and the contractgroup 
	 * @param i				The index at which the request would be placed
	 * @param request		The request to place
	 * @return
	 */
	public double calculateCosts(Schedule schedule, int i, Request request) {

		ContractGroup group = schedule.getC(); 
		int[] check = schedule.getScheduleArray().clone();
		schedule.setWeeklyOvertime(); 
		double[] curOvertime = schedule.getWeeklyOvertime();  
		double costOfPlacement = 0;

		int[] curSoftPenalties = this.feasCheck.allViolations(check, schedule.getC(), i); 
		
		check[i] = request.getDutyNumber();
		if (request.getDutyNumber() == 1) {
			int curMissingATV = this.feasCheck.checkATVDays(schedule.getScheduleArray(), schedule.getC());
			if (curMissingATV > 0) {
				costOfPlacement = -this.feasPen[1];
			}
		}

		double newOvertime[] = this.feasCheck.setWeeklyOvertime(check, group); 
		
		int curTotOvertime = 0;
		for (int j = 0; j < curOvertime.length; j++) {
			int overtime = 0;
			for (int n = 0; n < 13; n++) {
				overtime += curOvertime[(j+n)%curOvertime.length];
			}
			if (overtime > 0) {
				curTotOvertime += overtime;
			}
		}
		int newTotOvertime = 0;
		for (int j = 0; j < newOvertime.length; j++) {
			int overtime = 0;
			for (int n = 0; n < 13; n++) {
				overtime += newOvertime[(j+n)%newOvertime.length];
			}
			if (overtime > 0) {
				newTotOvertime += overtime;
			}
		}

		costOfPlacement += this.feasPen[0] * (newTotOvertime - curTotOvertime); 

		int[] softViol = this.feasCheck.allViolations(check, group, i); 
		for(int j = 0; j< this.softFeas.length; j++) {
			costOfPlacement += this.softFeas[j]* (softViol[j] - curSoftPenalties[j]); 
		}

		return costOfPlacement; 
	}

	/**
	 * Calculates the sum of an array
	 * @param array
	 * @return
	 */
	public double sumOfArray(double[] array) {
		double sum = 0;
		for(int i =0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}

	/**
	 * This method checks the feasibility of a schedule.
	 * @param schedule
	 * @param i
	 * @param request
	 * @return
	 */
	public boolean checkFeasibility(Schedule schedule, int i, Request request) {
		int[] check = schedule.getScheduleArray().clone(); 
		check[i] = request.getDutyNumber(); 
		return  this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check); 
	}
}