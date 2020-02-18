import Tools.*;

import java.util.*;
import java.util.Random;

public class Phase5_ALNS {

	//private List<Map<ContractGroup, LSschedule>> listWithSol = new ArrayList<Map<ContractGroup, LSschedule>>();
	private double globalOptimum;
	private Solution globalSolution;
	private Random random;
	private int minSizeNeighbourhood;
	private int maxSizeNeighbourhood;
//	private Set<Request> dutiesStillAsRequest = new HashSet<Request>();
	private int nIterations;
	private Map<ContractGroup, Schedule> copySchedule;
	private Set<Request> copyRequest= new HashSet<Request>();
	private final Instance instance;
	private DestroyHeuristics destroyHeuristics;
	private RepairHeuristics repairHeuristics;
	
	private double[] weightsDestroy;
	private double[][] weightsDestroyAdj;
	private double[] weightsRepair;
	private double[][] weightsRepairAdj;
	private int nDestroy; 
	private int nRepair; 
	private double T;
	private double c;
	
	public Phase5_ALNS (int iterations, Instance instance, Map<ContractGroup, Schedule> startSchedule, long seed){
		this.minSizeNeighbourhood = 5;
		this.maxSizeNeighbourhood = 50;
		this.nIterations = iterations;
		this.instance = instance;
		this.random = new Random();
		this.nDestroy = 2;
		this.nRepair = 2;
		this.destroyHeuristics = new DestroyHeuristics();
		this.repairHeuristics = new RepairHeuristics(instance);
		System.out.println(startSchedule.toString()); 
		this.c = 0.5;
		
		
	}
	
	public Solution executeBasic(Map<ContractGroup, Schedule> startSchedule) {
		int n = 1;
		// find initial solution
		Solution initSol = this.getInitialSol(startSchedule);
		this.globalSolution = initSol;
		System.out.println(initSol.toString()); 
		
		this.globalOptimum = initSol.getObj();
		System.out.println("Best Solution so far: " + this.globalOptimum);
		Solution currentSol = initSol.clone(); 
		this.copySchedule = currentSol.getNewSchedule(); 
		this.getTStart();
		boolean accepted = true;
		
		// Initialize weights
		System.out.println("nDestory: " + this.nDestroy +" nRepair: " + this.nRepair);
		this.weightsDestroy = new double[this.nDestroy];
		for (int i = 0; i < this.nDestroy; i++) {
			this.weightsDestroy[i] = 1 / (double) this.nDestroy;
		}
		this.weightsDestroyAdj = new double[this.nDestroy][2];
		this.weightsRepair = new double[this.nRepair];
		for (int i = 0; i < this.nRepair; i++) {
			this.weightsRepair[i] = 1/ (double) this.nRepair;
		}
		this.weightsRepairAdj = new double[this.nRepair][2];

		System.out.println(initSol.toString());
		
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
			//System.out.println("UDestroy: " + UDestroy + " destroy heuristic number: " + destroyHeuristicNr);
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
			//System.out.println(tempSol.toString());
			int sizeNeighbourhood = this.random.nextInt(this.maxSizeNeighbourhood - this.minSizeNeighbourhood) + this.minSizeNeighbourhood;
			
			System.out.println("-----------------------------------------------------------------------");
			System.out.println("ITERATION " + n + ":");
			
			// find new solution
			tempSol = this.executeDestroyAndRepair(tempSol, destroyHeuristicNr, repairHeuristicNr, sizeNeighbourhood);
			
			// determine if accepted or not
			boolean globalOpt = false;
			accepted  = false;
			if (tempSol.getObj() < this.globalOptimum) {
				
				this.globalSolution = tempSol.clone();
				this.globalOptimum = this.globalSolution.getObj();
				globalOpt = true;
				accepted = true;
				currentSol = tempSol.clone();
				System.out.println(tempSol.getRequests().size());
				//System.out.println("-----------------------------------------------------------------------");
				System.out.println("New global best solution (iteration " + n + "): " + this.globalOptimum);
				 
				
				} else if (this.random.nextDouble() < Math.exp(-(tempSol.getObj() - currentSol.getObj()) / this.T)) {
					accepted = true;
					currentSol = tempSol.clone();
					System.out.println("We accept the solution with value: " + currentSol.getObj());
					System.out.println(currentSol.getNewSchedule().toString());
			}

			/*	// update weight adjustments
						this.updateWeightAdj(globalOpt, accepted, unique, destroyHeuristicNr, repairHeuristicNr);

						// update weights if multiple of 100
						if (n % 100 == 0 && n < nIterations) {
							this.updateWeights();
							System.out.println("-----------------------------------------------------------------------");
							System.out.println("-----------------------------------------------------------------------");
							System.out.println("ITERATION " + n + " - " + (n+99) + ":");
						}
*/
						// update temperature
						this.T = this.T *this.c;
						//System.out.println(tempSol.toString());
						//System.out.println(currentSol.toString());
					//	System.out.println(this.globalSolution.toString());
						n++;

		}
		System.out.println(this.globalSolution.getObj());
		System.out.println(this.globalSolution.getNewSchedule().toString());
		return this.globalSolution;
	}
	
	public Solution getInitialSol(Map<ContractGroup, Schedule> startSol) {
		Set<Request> emptyRequestSet = new HashSet<Request>();
		Solution initSol = new Solution(emptyRequestSet, startSol, instance);
		return initSol;
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
		//if (destroyHeuristicNr == 0) {
			currentSol = this.destroyHeuristics.executeRandom(currentSol, sizeNeighbourhood,  random,instance);
		//} 
		//else {
			//currentSol = this.destroyHeuristics.executeRandomOvertime(currentSol, sizeNeighbourhood, random, instance);
		//}

		this.repairHeuristics.setAllPlacements(currentSol).toString();
		
	//if (repairHeuristicNr == 0) {
			currentSol = this.repairHeuristics.greedyRepair(currentSol);
		/*} else {
			currentSol = this.repairHeuristics.regretRepair2(currentSol, 2);
		} */

		return currentSol;
	}
	
	
	public double sumOfArray(double[] array) {
		double sum = 0;
		for(int i =0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}
	public void getTStart() {
		this.T = 1;
	}
	
	public Solution copySolution(Solution solution) {
		Map<ContractGroup, Schedule> schedule = solution.getNewSchedule();
		this.copySchedule = new HashMap<ContractGroup, Schedule>();
		for(ContractGroup group: schedule.keySet()) {
			this.copySchedule.put(group, schedule.get(group));
		}

		this.copyRequest = new HashSet<Request>(solution.getRequests());

		Solution copy = new Solution(this.copyRequest, this.copySchedule, instance);

		return copy;
	}
	public boolean overTimeFeasible(int[] schedule, ContractGroup c) {
		int totMinWorkedOverSchedule = 0;
		//For every week
		for (int i = 0; i < schedule.length/7; i++) {
			//For all the days in that week 
			for (int j = 0; j < 7; j++) {
				if (schedule[i * 7 + j] == 1 || instance.getFromRDutyNrToRDuty().containsKey(schedule[i * 7 + j])) {
					totMinWorkedOverSchedule += c.getAvgHoursPerDay() * 60;
				} else if (instance.getFromDutyNrToDuty().containsKey(schedule[i * 7 + j])) {
					totMinWorkedOverSchedule += instance.getFromDutyNrToDuty().get(schedule[i * 7 + j]).getPaidMin();
				}
			}
		}

		if(totMinWorkedOverSchedule > schedule.length/7 * c.getAvgDaysPerWeek() * c.getAvgHoursPerDay() * 60) {
			return false;
		}
		else {
			return true;
		}
	}
	public double QuaterlyOvertime(int[] solution, ContractGroup c) {
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
}

