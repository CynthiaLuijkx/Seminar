import Tools.*;

import java.util.*;
import java.util.Random;

import Phase5.DestroyHeuristics;
import Phase5.RepairHeuristics;
import Phase5.Request;
import Phase5.Solution;

/**
 * This class executes the Adaptive Large Neighbourhood Search
 * @author Mette Wagenvoort
 *
 */
public class Phase5_ALNS {

	private double globalOptimum; //best solution value found so far
	private Random random; //generates a random number
	private int minSizeNeighbourhood; //the minimum size the neighbourhood can be
	private int maxSizeNeighbourhood; //the maximum size the neighbourhood can be
	private int nIterations; //the number of iterations we are going to execute the ALNS
	//private Map<ContractGroup, Schedule> copySchedule;
	private Set<Integer> hashCodesSolutions = new HashSet<Integer>(); //set that contains the hashcodes of the solutions
	//private Set<Request> copyRequest= new HashSet<Request>();
	private final Instance instance; //use of the instance
	private DestroyHeuristics destroyHeuristics; //use of the destroy heuristics
	private RepairHeuristics repairHeuristics; //use of the repair heuristics

	private double[] weightUpdates = {33/(double)55, 9/(double)55, 13/(double)55}; //determine how to update the weights
	private final double rho = 0.1; //the proportion of the weights that is determined by the recent performance
	private final double c = 0.99975; //cooling rate of the simulated annealing aspect

	private double[] weightsDestroy; //weights of the destroy methods
	private double[][] weightsDestroyAdj; //adjusted weights of the destroy methods
	private double[] weightsRepair; //weights of the repair methods
	private double[][] weightsRepairAdj; //adjusted weights of the repair methods
	private int nDestroy = 3; //number of destroy methods
	private int nRepair = 3;  // number of repair methods
	private Solution globalBestSol;  //best solution found so far
	private double T; //temperature used for simulated annealing

	//Constructor of the class
	public Phase5_ALNS (int iterations, Instance instance, Map<ContractGroup, Schedule> startSchedule, long seed){
		this.minSizeNeighbourhood = 2;
		this.maxSizeNeighbourhood = 25;
		this.nIterations = iterations;
		this.instance = instance;
		this.random = new Random(seed);
		this.destroyHeuristics = new DestroyHeuristics(instance);
		this.repairHeuristics = new RepairHeuristics(instance);
		//System.out.println(startSchedule.toString()); 
		this.T = 1; //Starting temperature 
		//determine the starting values of the weights of the destroy and repair methods
		this.weightsDestroy = new double[this.nDestroy];
		for (int i = 0; i < this.nDestroy; i++) {
			this.weightsDestroy[i] = 1 / (double) this.nDestroy;
		}
		this.weightsDestroyAdj = new double[this.nDestroy][2];

		this.weightsRepair = new double[this.nRepair];
		for (int i = 0; i < this.nRepair; i++) {
			this.weightsRepair[i] = 1/  (double) this.nRepair;
		}
		this.weightsRepairAdj = new double[this.nRepair][2];

		//this.executeBasic(startSchedule);
	}

	/**
	 * This method executes the Adaptive Large Neighbourhood Search.
	 * @param startSchedule				the initial solution
	 * @return							the best global solution found
	 */
	public Solution executeBasic(Map<ContractGroup, Schedule> startSchedule) {
		int n = 1; //start with the first iteration
		// find initial solution
		Solution initSol = this.getInitialSol(startSchedule);
		//System.out.println(initSol.toString()); 
		this.globalOptimum = initSol.getObj(); // set the best found solution equal to this one
		Solution currentSol = initSol.clone(); 
		boolean accepted = true;

		this.globalBestSol = currentSol.clone();
		System.out.println("Best Solution so far: " + this.globalOptimum);
		System.out.println("request bank size: " +this.globalBestSol.getRequests().size());

		for(ContractGroup group: instance.getContractGroups()) {
			System.out.println(group.getNr() + " " + this.globalBestSol.getNewSchedule().get(group).getScheduleArray().length);
		}
		//System.out.println(initSol);
		//till the number of iterations is reached
		while (n <= this.nIterations) {	

			// find the destroy and repair heuristic depending on the weights
			double UDestroy = this.random.nextDouble();
			int destroyHeuristicNr = 0;
			for (int i = 0; i < this.weightsDestroy.length; i++) {
				if (UDestroy < this.weightsDestroy[i]) {
					destroyHeuristicNr = i;
					break;
				}
				UDestroy -= this.weightsDestroy[i];
			}


			int repairHeuristicNr = 0;
			double URepair = this.random.nextDouble();
			for (int i = 0; i < this.weightsRepair.length; i++) {
				if (URepair < this.weightsRepair[i]) {
					repairHeuristicNr = i;
					break;
				}
				URepair -= this.weightsRepair[i];
			}

			Solution tempSol = currentSol.clone(); //get a temporary solution
			//			System.out.println("request bank contains: "+ tempSol.getRequests().size());
			//determine randomly the size of the neighborhood
			int sizeNeighbourhood = this.random.nextInt(this.maxSizeNeighbourhood - this.minSizeNeighbourhood) + this.minSizeNeighbourhood;

			//			System.out.println("-----------------------------------------------------------------------");
			//			System.out.println("ITERATION " + n + ":");

			// find new solution
			tempSol = this.executeDestroyAndRepair(tempSol, destroyHeuristicNr, repairHeuristicNr, sizeNeighbourhood);
			boolean unique = updateUniqueSol(tempSol); // determine whether it is unique

			// determine if accepted or not
			boolean globalOpt = false;
			accepted = false;
			//			System.out.println("Number of request in request bank: " + tempSol.getRequests().size()); 
			if (tempSol.getObj() < this.globalBestSol.getObj()) { //if we improve our global solution
				this.globalBestSol = tempSol.clone();
				globalOpt = true; //we found a new global optimum
				accepted = true; //we always accept the solution 
				currentSol = tempSol.clone(); //set the current solution to the temporary solution
				System.out.println("-----------------------------------------------------------------------");
				System.out.println("New global best solution (iteration " + n + "): " + this.globalBestSol.getObj());

			}
			//if we accept the solution by simulated annealing
			else if (this.random.nextDouble() < Math.exp(-(tempSol.getObj() - currentSol.getObj()) / this.T)) {
				accepted = true; //accept solution
				currentSol = tempSol.clone(); //set the current solution to the temporary solution
				//				System.out.println("We accepted the solution: " + currentSol.getObj());
			}


			// update weight adjustments
			this.updateWeightAdj(globalOpt, accepted, unique, destroyHeuristicNr, repairHeuristicNr);
			// update weights if multiple of 100
			if (n % 100 == 0 && n < nIterations) {
				this.updateWeights();
				System.out.println("-----------------------------------------------------------------------");
				System.out.println("-----------------------------------------------------------------------");
				System.out.println("ITERATION " + n + " - " + (n+99) + ":");
			}

			// update temperature
			this.T = this.T * this.c;
			n++;
		}
		System.out.println("end size"+ this.globalBestSol.getRequests().size());
		for(ContractGroup group: instance.getContractGroups()) {
			System.out.println("number of drivers of group " +group.getNr()+ " is: " + this.globalBestSol.getNewSchedule().get(group).getScheduleArray().length/7);
		}
		System.out.println("Nr. of request bank: " + this.globalBestSol.getRequests().size());
		return this.globalBestSol; //return our global solution
	}

	/**
	 * This method is used to get the initial solution.
	 * @param startSol			the initial solution
	 * @return					the initial solution
	 */
	public Solution getInitialSol(Map<ContractGroup, Schedule> startSol) {
		Set<Request> emptyRequestSet = new HashSet<Request>();
		List<Schedule> check = new ArrayList<>();
		for(ContractGroup group: instance.getContractGroups()) {
			check.add(startSol.get(group));
		}
		emptyRequestSet = this.missingDuties(check);
		Solution initSol = new Solution(emptyRequestSet, startSol, instance);
		return initSol;
	}

	/**
	 * This method executes the destroy and repair heuristic given the destroy and repair heuristic number.
	 * @param currentSol				the current solution
	 * @param destroyHeuristicNr		the destroy heuristic number
	 * @param repairHeuristicNr			the repair heuristic number
	 * @param sizeNeighbourhood			the size of the neighbourhood
	 * @return							the updated solution
	 */
	public Solution executeDestroyAndRepair(Solution currentSol, int destroyHeuristicNr, int repairHeuristicNr, 
			int sizeNeighbourhood) {
		//execute a destroy heuristic depending on the generated number
		//		currentSol = this.destroyHeuristics.executeRandom(currentSol, sizeNeighbourhood, random, instance);
		//		currentSol = this.destroyHeuristics.executeExtremeSpecificRemoval(currentSol, sizeNeighbourhood, random, instance);
		if (destroyHeuristicNr == 0) {
			currentSol = this.destroyHeuristics.executeRandom(currentSol, sizeNeighbourhood,  random,instance);
		}
		else if (destroyHeuristicNr == 1){
			currentSol = this.destroyHeuristics.executeRemoveWeek(currentSol, random, instance);
		} 
		else if (destroyHeuristicNr == 2) {
			currentSol = this.destroyHeuristics.executeSwapWeek(currentSol, random, instance);
		}
//		else if(destroyHeuristicNr == 3){
//			currentSol = this.destroyHeuristics.executeExtremeSpecificRemoval(currentSol, sizeNeighbourhood, random, instance);
//		}
//		else  if(destroyHeuristicNr == 4){
//			currentSol = this.destroyHeuristics.executeExtremeRemoval(currentSol, sizeNeighbourhood, random, instance);
//		}

		this.repairHeuristics.setAllPlacements(currentSol).toString();

		//execute a repair heuristic depending on the generated number
		//		currentSol = this.repairHeuristics.greedyRepair(currentSol);
		//		currentSol = this.repairHeuristics.regretRepair2(currentSol, 2);
		if (repairHeuristicNr == 0) {
			currentSol = this.repairHeuristics.greedyRepair(currentSol);
		} else if (repairHeuristicNr == 1){
			currentSol = this.repairHeuristics.regretRepair2(currentSol, 2);
		} else {
			currentSol = this.repairHeuristics.regretRepair2(currentSol, 3);
		}

		return currentSol;
	}

	/**
	 * This method returns whether a solution is an unique solution.
	 * @param currentSol				the solution
	 * @return							a boolean denoting whether it is unique or not
	 */
	public boolean updateUniqueSol(Solution currentSol) {
		if(!this.hashCodesSolutions.contains(currentSol.getHashCode())) {
			this.hashCodesSolutions.add(currentSol.getHashCode()); 
			return true; 
		}
		return false; 
	}

	/**
	 * This method updates the weight adjustments given the status of the new solution.
	 * @param globalOpt					true if the new solution is a global optimum, false otherwise
	 * @param accepted					true if the solution is accepted, false otherwise
	 * @param unique					true if the solution is unique, false otherwise
	 * @param destroyHeuristicNr		the destroy heuristic number
	 * @param repairHeuristicNr			the repair heuristic number
	 */
	public void updateWeightAdj(boolean globalOpt, boolean accepted, boolean unique, int destroyHeuristicNr, 
			int repairHeuristicNr) {

		this.weightsDestroyAdj[destroyHeuristicNr][1] += 1.0;
		this.weightsRepairAdj[repairHeuristicNr][1] += 1.0;
		if (globalOpt) {
			this.weightsDestroyAdj[destroyHeuristicNr][0] += this.weightUpdates[0];
			this.weightsRepairAdj[repairHeuristicNr][0] += this.weightUpdates[0];
		} else if (accepted && unique) {
			this.weightsDestroyAdj[destroyHeuristicNr][0] += this.weightUpdates[1];
			this.weightsRepairAdj[repairHeuristicNr][0] += this.weightUpdates[1];
		} else if (unique) {
			this.weightsDestroyAdj[destroyHeuristicNr][0] += this.weightUpdates[2];
			this.weightsRepairAdj[repairHeuristicNr][0] += this.weightUpdates[2];
		}
	}

	/**
	 * This method updates the weights given the weights adjustments found over the past 100 iterations.
	 */
	public void updateWeights() {
		double sum = 0;
		for (int i = 0; i < this.weightsDestroy.length; i++) {
			this.weightsDestroy[i] = this.rho * (this.weightsDestroyAdj[i][0] / this.weightsDestroyAdj[i][1]) + (1 - this.rho) * this.weightsDestroy[i];
			sum += this.weightsDestroy[i];
		}
		for (int i = 0; i < this.weightsDestroy.length; i++) {
			this.weightsDestroy[i] = this.weightsDestroy[i] / sum;
		}
		sum = 0;
		for (int i = 0; i < this.weightsRepair.length; i++) {
			this.weightsRepair[i] = this.rho * (this.weightsRepairAdj[i][0] / this.weightsRepairAdj[i][1]) + (1 - this.rho) * this.weightsRepair[i];
			sum += this.weightsRepair[i];
		}
		for (int i = 0; i < this.weightsRepair.length; i++) {
			this.weightsRepair[i] = this.weightsRepair[i] / sum;
		}
	}

	/**
	 * This method creates a set of requests for the missing duties.
	 * @param schedules					the list with schedules
	 * @return							a set of requests for the missing duties
	 */
	public Set<Request> missingDuties(List<Schedule> schedules) {
		Set<Request> requests = new HashSet<Request>();
		int counter = 0;
		for (int s = 0; s < 7; s++) {
			if(s == 0) {
				for(Duty duty : instance.getSunday()) {
					int included = 0;
					for(Schedule schedule : schedules) {
						for(int w = 0; w < schedule.getScheduleArray().length/7; w++) {
							if(schedule.getScheduleArray()[(7*w) + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included < 1) {
						counter++;
						Request request = new Request(duty, null, s);
						requests.add(request);
						//	System.out.println(duty);
					}
				}
			}
			else if(s == 6) {
				for(Duty duty : instance.getSaturday()) {
					int included = 0;
					for(Schedule schedule : schedules) {
						for(int w = 0; w < schedule.getScheduleArray().length/7; w++) {
							if(schedule.getScheduleArray()[(7*w) + s] == duty.getNr()) {
								included++;

							}
						}
					}
					if(included < 1) {
						counter++;
						Request request = new Request(duty, null, s);
						requests.add(request);
						//	System.out.println(duty);
					}
				}
			}
			else {
				for(Duty duty : instance.getWorkingDays()) {
					int included = 0;
					for(Schedule schedule : schedules) {
						for(int w = 0; w < schedule.getScheduleArray().length/7; w++) {
							//System.out.println(duty.getNr() + " " +  schedule.getSchedule()[(7*w) + s]);
							if(schedule.getScheduleArray()[(7*w)+ s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included < 1) {
						counter++;
						Request request = new Request(duty, null, s);
						requests.add(request);
						//	System.out.println(duty);
					}
				}
			}
		}
		System.out.println("missing duties: " + counter);
		return requests;
	}
}