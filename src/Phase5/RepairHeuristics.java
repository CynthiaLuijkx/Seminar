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
	private Instance instance;

	private double[][] fairnessCounts;

	public RepairHeuristics(Instance instance) {
		this.instance = instance;
		this.feasCheck = new FeasCheck(instance); 
		/*
		 * 0:	ReserveDuties Distribution 
		 * 1:	Working Sundays Distribution 
		 * 2: 	Desirability Distribution
		 * 3:	Split Distribution
		 * 4: 	Attractiveness Distribution
		 */
		this.fairnessCounts = new double[instance.getPenalties().getFairPenalties().length][instance.getContractGroups().size()] ;
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
		this.fairnessCounts = solution.getFeasCounts();
		List<Placement> placements = new ArrayList<Placement>(); 
		for(Request request: solution.getRequests()) {
			placements.addAll(setPlacements(request, solution, instance.getContractGroups())); 
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
	public List<Placement> setPlacements(Request request, Solution solution, LinkedHashSet<ContractGroup> groups){
		ArrayList<Placement> updatedPlacements = new ArrayList<Placement>();
		
		for (ContractGroup group : groups) {
			int countRest = 0; 
			int countFeas = 0; 
			if (request.getDutyNumber() == 1 && group.getNr() == request.getGroup().getNr()) {
				request.deletePlacements(group);
				Schedule schedule = solution.getNewSchedule().get(group); 
				for(int i = 0; i < schedule.getScheduleArray().length; i++) {
					if(solution.getNewSchedule().get(group).getScheduleArray()[i] == 2) {
						countRest++; 
						if(this.checkFeasibility(schedule, i, request)) {
							countFeas++; 
							double costOfPlacement = calculateCosts(schedule, i, request); 
							Placement newPlacement = new Placement(request, new TimeSlot(group,i), costOfPlacement); 
							request.addPlacement(newPlacement);
							updatedPlacements.add(newPlacement); 
						}
					}
				}
			} else if (request.getDutyNumber() != 1) {
				String type = null;
				if (request.getDuty() != null) {
					type = request.getDuty().getType();
				} else {
					type = request.getReserveDuty().getType();
				}
				if (group.getDutyTypes().contains(type)) {
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
		this.fairnessCounts = solution.getFeasCounts();
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

	//---------------------- Regret Repair --------------------------------------------------------------------------------------------------------
	/**
	 * This method executes a regret repair in which the best placement is executed for the request that has the highest regret.
	 * @param solution
	 * @param q
	 * @return
	 */
	public Solution regretRepair2(Solution solution, int q, Random random) {
		q=q-1;
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
				//				System.out.println("No more placements possible");
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
	public Solution greedyRepair(Solution solution, Random random){
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
				//				System.out.println("No more placements possible");
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
				costOfPlacement = -this.instance.getPenalties().getFeasPenalties()[1];
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

		costOfPlacement += this.instance.getPenalties().getFeasPenalties()[0] * (newTotOvertime - curTotOvertime); 

		int[] softViol = this.feasCheck.allViolations(check, group, i); 
		for(int j = 0; j< this.instance.getPenalties().getSoftPenalties().length; j++) {
			costOfPlacement += this.instance.getPenalties().getSoftPenalties()[j]* (softViol[j] - curSoftPenalties[j]) * instance.getMultiplierSoft(); 
		}

		double[][] copyFairCounts = new double[this.fairnessCounts.length][]; 

		for(int j = 0; j < this.fairnessCounts.length; j++) {
			copyFairCounts[j] = this.fairnessCounts[j].clone();
		}

		double[] varFairBefore = new double[this.instance.getPenalties().getFairPenalties().length];
		double[] varFairAfter = new double[this.instance.getPenalties().getFairPenalties().length]; 

		double[] newFair = this.feasCheck.getAllFairness(check, group); 
		for(int j = 0; j<varFairBefore.length; j++) {
			varFairBefore[j]= this.feasCheck.getCoefVariance(this.fairnessCounts[j]); 
			//update 
			copyFairCounts[j][schedule.getC().getNr()-1] = newFair[j]; 
		}

		for(int j = 0; j<varFairAfter.length; j++) {
			varFairAfter[j]= this.feasCheck.getCoefVariance(copyFairCounts[j]); 
		}

		//Fairness Costs 
		for(int j = 0; j< this.instance.getPenalties().getFairPenalties().length; j++) {
			costOfPlacement += this.instance.getPenalties().getFairPenalties()[j]* (varFairAfter[j] - varFairBefore[j]) * instance.getMultiplierFair(); 
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
		return  this.feasCheck.isFeasible7(check, i-7, i+7) && this.feasCheck.isFeasible14(check, i-14, i+14) && this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime()) && this.feasCheck.checkMax2SplitDuties(check) && this.feasCheck.checkSundays(check); 
	}
}