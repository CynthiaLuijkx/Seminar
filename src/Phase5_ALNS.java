import Tools.*;

import java.util.*;
import java.util.Random;

public class Phase5_ALNS {

	//private List<Map<ContractGroup, LSschedule>> listWithSol = new ArrayList<Map<ContractGroup, LSschedule>>();
	private double globalOptimum;
	private Random random;
	private int minSizeNeighbourhood;
	private int maxSizeNeighbourhood;
	private int nIterations;
	private Map<ContractGroup, Schedule> copySchedule;
	private Set<Integer> hashCodesSolutions = new HashSet<Integer>();
	private Set<Request> copyRequest= new HashSet<Request>();
	private final Instance instance;
	private DestroyHeuristics destroyHeuristics;
	private RepairHeuristics repairHeuristics;

	private double[] weightUpdates = {33/(double)55, 9/(double)55, 13/(double)55};
	private final double rho = 0.1;
	private final double c = 0.99975;

	private double[] weightsDestroy;
	private double[][] weightsDestroyAdj;
	private double[] weightsRepair;
	private double[][] weightsRepairAdj;
	private int nDestroy = 2; 
	private int nRepair = 2; 
	private Solution globalBestSol; 
	private double T;

	public Phase5_ALNS (int iterations, Instance instance, Map<ContractGroup, Schedule> startSchedule, long seed){
		this.minSizeNeighbourhood = 2;
		this.maxSizeNeighbourhood = 25;
		this.nIterations = iterations;
		this.instance = instance;
		this.random = new Random(0);
		this.destroyHeuristics = new DestroyHeuristics();
		this.repairHeuristics = new RepairHeuristics(instance);
		System.out.println(startSchedule.toString()); 
		this.T = 1; 
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

		Solution solutionALNS = this.executeBasic(startSchedule);


	}

	public Solution executeBasic(Map<ContractGroup, Schedule> startSchedule) {
		int n = 1;
		// find initial solution
		Solution initSol = this.getInitialSol(startSchedule);
		System.out.println(initSol.toString()); 
		this.globalOptimum = initSol.getObj(); 
		System.out.println("Best Solution so far: " + this.globalOptimum);
		Solution currentSol = initSol.clone(); 
		boolean accepted = true;

		globalBestSol = currentSol.clone(); 

		//System.out.println(initSol);
		while (n <= this.nIterations) {	

			// find the destroy and repair heuristic
			double UDestroy = this.random.nextDouble();
			int destroyHeuristicNr = 0;
			for (int i = 0; i < this.weightsDestroy.length; i++) {
				if (UDestroy < this.weightsDestroy[i]) {
					destroyHeuristicNr = i;
					break;
				}
				UDestroy -= this.weightsDestroy[i];
			}
			double URepair = this.random.nextDouble();
			int repairHeuristicNr = 0;
			for (int i = 0; i < this.weightsRepair.length; i++) {
				if (URepair < this.weightsRepair[i]) {
					repairHeuristicNr = i;
					break;
				}
				URepair -= this.weightsRepair[i];
			}


			Solution tempSol = currentSol.clone();
			int sizeNeighbourhood = this.random.nextInt(this.maxSizeNeighbourhood - this.minSizeNeighbourhood) + this.minSizeNeighbourhood;

			System.out.println("-----------------------------------------------------------------------");
			System.out.println("ITERATION " + n + ":");

			// find new solution
			tempSol = this.executeDestroyAndRepair(tempSol, destroyHeuristicNr, repairHeuristicNr, sizeNeighbourhood);
			for(ContractGroup group: tempSol.getNewSchedule().keySet()) {
				tempSol.getNewSchedule().get(group).setWeeklyOvertime();
			}
			boolean unique = updateUniqueSol(tempSol); // this.updateUniqueSol(tempSol);

			// determine if accepted or not
			boolean globalOpt = false;
			accepted = false;

			double obj1 = tempSol.getObj(); 
			double obj2 = this.globalBestSol.getObj(); 
			System.out.println("Number of request in request bank: " + tempSol.getRequests().size()); 
			if (tempSol.getObj() < this.globalBestSol.getObj()) {
				this.globalBestSol = tempSol.clone();
				globalOpt = true;
				accepted = true;
				currentSol = tempSol;
				System.out.println("-----------------------------------------------------------------------");
				System.out.println("New global best solution (iteration " + n + "): " + this.globalBestSol.getObj());
			} else if (this.random.nextDouble() < Math.exp(-(tempSol.getObj() - currentSol.getObj()) / this.T)) {
				accepted = true;
				currentSol = tempSol;
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
		return this.globalBestSol; 
	}

	public Solution getInitialSol(Map<ContractGroup, Schedule> startSol) {
		Set<Request> emptyRequestSet = new HashSet<Request>();
		Solution initSol = new Solution(emptyRequestSet, startSol, instance);
		return initSol;
	}

	public double[] QuaterlyOvertime(Solution sol) {
		double[] overtime = new double[sol.getNewSchedule().keySet().size()];
		for(ContractGroup group: sol.getNewSchedule().keySet()) {
			for(int empl = 0; empl < sol.getNewSchedule().get(group).getWeeklyOvertime().length; empl++) {
				for(int i =0; i < 13; i++) { //need to loop over 13 weeks for overtime

					if((empl + i) < sol.getNewSchedule().get(group).getWeeklyOvertime().length){
						if(sol.getNewSchedule().get(group).getWeeklyOvertime()[empl+ i] > 0) {
							overtime[group.getNr()-1] += sol.getNewSchedule().get(group).getWeeklyOvertime()[empl+ i];	
						}
					}
					else {
						int remainder = (empl + i) % sol.getNewSchedule().get(group).getWeeklyOvertime().length;

						if(sol.getNewSchedule().get(group).getWeeklyOvertime()[remainder] > 0) {
							overtime[group.getNr()-1] += + sol.getNewSchedule().get(group).getWeeklyOvertime()[remainder];		
						}
					}
				}
			}
		}
		return overtime;
	}

	/**
	 * This method executes the destroy and repair heuristic given the destroy and repair heuristic number.
	 * @param currentSol				the current solution
	 * @param destroyHeuristicNr		the destroy heuristic number
	 * @param repairHeuristicNr			the repair heuristic number
	 * @param sizeNeighbourhood			the size of the neighborhood
	 * @return							the updated solution
	 */
	public Solution executeDestroyAndRepair(Solution currentSol, int destroyHeuristicNr, int repairHeuristicNr, 
			int sizeNeighbourhood) {
		if (destroyHeuristicNr == 0) {
			currentSol = this.destroyHeuristics.executeRandom(currentSol, sizeNeighbourhood,  random,instance);
		}else {
			currentSol = this.destroyHeuristics.executeRandomOvertime(currentSol, sizeNeighbourhood, random, instance); 
		}

		this.repairHeuristics.setAllPlacements(currentSol).toString(); 

		if (repairHeuristicNr == 0) {
			currentSol = this.repairHeuristics.greedyRepair(currentSol);
		} else {
			currentSol = this.repairHeuristics.regretRepair2(currentSol, 2);
		} 

		return currentSol;
	}

	public double sumOfArray(double[] array) {
		double sum = 0;
		for(int i =0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}

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
			sum = this.weightsRepair[i];
		}
		for (int i = 0; i < this.weightsRepair.length; i++) {
			this.weightsRepair[i] = this.weightsRepair[i] / sum;
		}
	}

}