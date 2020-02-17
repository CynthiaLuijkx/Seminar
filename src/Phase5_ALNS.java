import Tools.*;

import java.util.*;
import java.util.Random;

public class Phase5_ALNS {

	//private List<Map<ContractGroup, LSschedule>> listWithSol = new ArrayList<Map<ContractGroup, LSschedule>>();
	private double globalOptimum;
	private Map<ContractGroup, Schedule> globalSchedule;
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
	
	public Phase5_ALNS (int iterations, Instance instance, Map<ContractGroup, Schedule> startSchedule, long seed){
		this.minSizeNeighbourhood = 2;
		this.maxSizeNeighbourhood = 25;
		this.nIterations = iterations;
		this.instance = instance;
		this.random = new Random(seed);
		this.destroyHeuristics = new DestroyHeuristics();
		this.repairHeuristics = new RepairHeuristics(instance);
		System.out.println(startSchedule.toString()); 
		Solution solutionALNS = this.executeBasic(startSchedule);
		
	}
	
	public Solution executeBasic(Map<ContractGroup, Schedule> startSchedule) {
		int n = 1;
		// find initial solution
		Solution initSol = this.getInitialSol(startSchedule);
		System.out.println(initSol.toString()); 
		double[] overtime = this.QuaterlyOvertime(initSol);
		this.globalOptimum = this.sumOfArray(overtime);
		System.out.println("Best Solution so far: " + this.globalOptimum);
		Solution currentSol = initSol.clone(); 
		this.copySchedule = currentSol.getNewSchedule(); 
		this.globalSchedule = this.copySchedule;
		
		// Initialize weights
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
			this.destroyHeuristics.executeRandom(tempSol, sizeNeighbourhood, this.random, instance);
			double[] newOvertime = this.QuaterlyOvertime(tempSol);
			List<List<Placement>> requestWithPlacements = new ArrayList<List<Placement>>();
			System.out.println(this.repairHeuristics.setAllPlacements(tempSol).toString()); 
			
			
			// find new solution
			tempSol = this.executeDestroyAndRepair(tempSol, destroyHeuristicNr, repairHeuristicNr, sizeNeighbourhood);
			this.repairHeuristics.greedyRepair(tempSol); 
			
//			for(Request request: tempSol.getRequests()) {
//				List<Placement> placements = new ArrayList<Placement>();
//				placements = this.repairHeuristics.setPlacements(request, tempSol);
//			
//				requestWithPlacements.add(placements);
//			}
			//System.out.println(requestWithPlacements.toString());
			
			n++;
		}
		return new Solution(null, this.globalSchedule, instance);
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
		//if (destroyHeuristicNr == 0) {
			currentSol = this.destroyHeuristics.executeRandom(currentSol, sizeNeighbourhood,  random,instance);
		//} 

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
	
//	public Solution copySolution(Solution solution) {
//		Map<ContractGroup, Schedule> schedule = solution.getNewSchedule();
//		this.copySchedule = new HashMap<ContractGroup, Schedule>();
//		for(ContractGroup group: schedule.keySet()) {
//			Schedule newLSschedule = new LSschedule(schedule.get(group).getSchedule(), null);
//			newLSschedule.setWeeklyOvertime(schedule.get(group).getSchedule(), instance);
//			this.copySchedule.put(group, newLSschedule);
//		}
//
//		this.copyRequest = new HashSet<Request>(solution.getRequests());
//
//		Solution copy = new Solution(this.copyRequest, this.copySchedule, instance);
//
//		return copy;
//	}
}