package Tools;
import Tools.Placement;
import java.util.*;


public class RepairHeuristics {
	private List<Placement> placements ;
	private FeasCheck feasCheck; 
	private double[] penaltiesFeas;
	public RepairHeuristics(Instance instance) {
		this.placements = new ArrayList<Placement>();
		this.feasCheck = new FeasCheck(instance); 

		/*
		 *	Order of the penalties
		 *	0: Feasibility 7*24 hours rest 
		 *	1: Feasibility 14*24 hours rest 
		 *	2: Check if overtime over the whole schedule is 0
		 *	3: Check if the 11 hours between breaks is feasible 
		 *	4: Check if the number of ATV days is satisfied 
		 *	5: Quaterly overtime paid out
		 */

		this.penaltiesFeas = new double[6]; 
		this.penaltiesFeas[0] = 10000;
		this.penaltiesFeas[1] = 10000;
		this.penaltiesFeas[2] = 10000; //more strict
		this.penaltiesFeas[3] = 10000;
		this.penaltiesFeas[4] = 10000; //more strict
		this.penaltiesFeas[5] = 1;
	}

	/**
	 * 
	 * @param forwardCheck
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

	public List<Placement> copyPlacementList(List<Placement> placements){
		ArrayList<Placement> copyPlacements = new ArrayList<Placement>(); 

		for(Placement placement:placements) {
			copyPlacements.add(placement.duplicate()); 
		}
		return copyPlacements; 
	}

	public List<Placement> setAllPlacements(Solution solution){
		List<Placement> placements = new ArrayList<Placement>(); 
		for(Request request: solution.getRequests()) {
			placements.addAll(setPlacements(request, solution, solution.getNewSchedule().keySet())); 
		}
		return placements; 
	}

	public List<Placement> setPlacements(Request request, Solution solution, Set<ContractGroup> groups){
		ArrayList<Placement> updatedPlacements = new ArrayList<Placement>(); 
		for(ContractGroup group: groups) {
			request.deletePlacements(group);
			
			Schedule schedule = solution.getNewSchedule().get(group); 
			for(int i = request.getWeekday(); i < schedule.getScheduleArray().length; i+=7) {
				if(solution.getNewSchedule().get(group).getScheduleArray()[i] == 2) {
					double costOfPlacement = calculateCosts(schedule, i, request); 
					Placement newPlacement = new Placement(request, new TimeSlot(group,i), costOfPlacement); 
					request.addPlacement(newPlacement);
					updatedPlacements.add(newPlacement); 
				}
			}
		}
		return updatedPlacements;
	}

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
					double costOfPlacement = calculateCosts(schedule, i, request); 
					placement.setCost(costOfPlacement);
					updatedPlacements.add(placement); 
				}
			}
		}

		for(Placement placement: toRemove) {
			request.deletePlacement(placement);
		}
		return updatedPlacements; 
	}

	
public Solution regretRepair2(Solution solution, int q) {
		
		while(solution.getRequests().size()!=0) {

			Request mostRegretRequest = null; 
			double maxRegret = - Double.MAX_VALUE; 
			for(Request request: solution.getRequests()) {
				List<Placement> placements = request.getPlacements(); 
				Collections.sort(placements);

				if(placements.size() != 0) {
					double regret = placements.get(q).getCost() - placements.get(0).getCost(); 
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
			solution.addRequest(bestPlacement); //This method also deletes the request placed

			System.out.println("Added: " + bestPlacement.getRequest().toString()); 

			deleteInvalidPlacements(bestPlacement, solution); 

			for(Request request: solution.getRequests()) {
				updatePlacements(request, solution, bestPlacement.getTimeslot().getGroup(), bestPlacement.getTimeslot().getDay());
			}
		}
		return solution; 
	}
	/**
	 * Greedy repair 
	 * 
	 * Each iteration the best placement is added, until all possible placements are done or all request are satisfied 
	 * 
	 * @param solution
	 * @return
	 */
	public Solution greedyRepair(Solution solution){
		ArrayList<Placement> allPlacements = new ArrayList<Placement>(); 

		for(Request request: solution.getRequests()) {
			allPlacements.addAll(request.getPlacements()); 
		}
		//System.out.println("Here " + allPlacements.size());

		while(solution.getRequests().size()!=0) { //until all requests are placed
			if(allPlacements.size()== 0) {
				System.out.println("Not all requests satisfied"); 
				break; 
			}
			
			Placement bestPlacement = Collections.min(allPlacements); 
			solution.addRequest(bestPlacement); //This method also deletes the request placed

			Set<Placement> toRemove = new HashSet<Placement>(); 
			for(Placement placement: allPlacements) {
				if(placement.getRequest().equals(bestPlacement.getRequest())) {
					toRemove.add(placement); 
				}
				if(placement.getTimeslot().getDay() == bestPlacement.getTimeslot().getDay() && placement.getTimeslot().getGroup().equals(bestPlacement.getTimeslot().getGroup())) {		
					toRemove.add(placement); 
				}
			}
			allPlacements.removeAll(toRemove); 
			for(Request request: solution.getRequests()) {
				request.deleteAllPlacements();
				List<Placement> newPlacements = updatePlacements(request, solution, bestPlacement.getTimeslot().getGroup(), bestPlacement.getTimeslot().getDay());
				for(int i =0; i <newPlacements.size(); i++) {
					request.addPlacement(newPlacements.get(i));
				}
			}
		}
		return solution; 
	}

	/**
	 * 
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
	 * Calculates the costs of a placement 
	 * @param lsschedule 	Stores the schedule and the contractgroup 
	 * @param i				The index at which the request would be placed
	 * @param request		The request to place
	 * @return
	 */
	public double calculateCosts(Schedule schedule, int i, Request request) {

		ContractGroup group = schedule.getC(); 
		double[] newOvertime = schedule.getWeeklyOvertime(); 
		double costOfPlacement = 0;

		int[] check = schedule.getScheduleArray();
		check[i] = request.getDutyNumber();
		boolean[] checkFeasibility = new boolean[5]; //feasibilty7, feasibility14, overTimeFeasible, restTimeFeasible, ATVfeasible
		checkFeasibility[0] = this.feasCheck.isFeasible7(check, i-7, i+7);
		checkFeasibility[1] = this.feasCheck.isFeasible14(check, i-14, i+14);
		checkFeasibility[2] = this.feasCheck.overTimeFeasible(check, request.getGroup());
		checkFeasibility[3] = this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime());
		checkFeasibility[4] = this.feasCheck.checkATVDays(check, request.getGroup());

		check[i] = 2; 
		int counter = 0;
		for(int j =0; j < checkFeasibility.length-1; j++) {
			if(checkFeasibility[j] == false) {
				counter++;
				
				
			}
		}
		if(counter == 4) {
			costOfPlacement = Double.POSITIVE_INFINITY;
		}
		else {
			if(checkFeasibility[4] == false) {
		costOfPlacement += this.penaltiesFeas[4];
		costOfPlacement += newOvertime[group.getNr() - 1] - this.feasCheck.QuaterlyOvertime(check, group);
	
		}
		else {
			costOfPlacement += newOvertime[group.getNr() - 1] - this.feasCheck.QuaterlyOvertime(check, group);

		}
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
}