import Tools.*;
import Tools.DestroyHeuristics;
import java.util.*;
import java.util.Random;

public class Phase5_ALNS {

	//private List<Map<ContractGroup, LSschedule>> listWithSol = new ArrayList<Map<ContractGroup, LSschedule>>();
	private double globalOptimum;
	private Map<ContractGroup, LSschedule> globalSchedule;
	private Random random;
	private int minSizeNeighbourhood;
	private int maxSizeNeighbourhood;
//	private Set<Request> dutiesStillAsRequest = new HashSet<Request>();
	private int nIterations;
	private Map<ContractGroup, LSschedule> copySchedule;
	private Set<Request> copyRequest= new HashSet<Request>();
	private final Instance instance;
	private DestroyHeuristics destroyHeuristics;
	private RepairHeuristics repairHeuristics;
	
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
		this.copySolution(initSol);
		this.globalSchedule = this.copySchedule;
		
		Solution currentSol = initSol;
		//System.out.println(initSol);
		while (n <= this.nIterations) {	
			Solution tempSol = this.copySolution(currentSol);
			int sizeNeighbourhood = this.random.nextInt(this.maxSizeNeighbourhood - this.minSizeNeighbourhood) + this.minSizeNeighbourhood;
			System.out.println("-----------------------------------------------------------------------");
			System.out.println("ITERATION " + n + ":");
			this.destroyHeuristics.executeRandom(tempSol, sizeNeighbourhood, this.random, instance);
			double[] newOvertime = this.QuaterlyOvertime(tempSol);
			List<List<Placement>> requestWithPlacements = new ArrayList<List<Placement>>();
			for(Request request: tempSol.getRequests()) {
				List<Placement> placements = new ArrayList<Placement>();
				placements = this.repairHeuristics.updatePlacements(request, tempSol, newOvertime);
				requestWithPlacements.add(placements);
				System.out.println(placements.toString());
			}
			//System.out.println(requestWithPlacements.toString());
			n++;
		}
		return new Solution(null, this.globalSchedule, instance);
	}
	public Solution getInitialSol(Map<ContractGroup, Schedule> startSol) {
		Set<Request> emptyRequestSet = new HashSet<Request>();
		Solution initSol = new Solution(emptyRequestSet, null, instance);
		Map<ContractGroup, LSschedule> need = new HashMap<>();
		for(ContractGroup group: startSol.keySet()) {
		LSschedule startLSsol = new LSschedule(startSol.get(group), (double) (startSol.get(group).getPlusMin() - startSol.get(group).getMinMin()), null);
		startLSsol.setWeeklyOvertime(startSol.get(group), instance);
		need.put(group, startLSsol);
		}
		initSol.setNewSchedule(need);
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
	public double sumOfArray(double[] array) {
		double sum = 0;
		for(int i =0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}
	
	public Solution copySolution(Solution solution) {
		Map<ContractGroup, LSschedule> schedule = solution.getNewSchedule();
		this.copySchedule = new HashMap<ContractGroup, LSschedule>();
		for(ContractGroup group: schedule.keySet()) {
			LSschedule newLSschedule = new LSschedule(schedule.get(group).getLSSchedule(), schedule.get(group).getOvertime(), null);
			newLSschedule.setWeeklyOvertime(schedule.get(group).getLSSchedule(), instance);
			this.copySchedule.put(group, newLSschedule);
		}

		this.copyRequest = new HashSet<Request>(solution.getRequests());

		Solution copy = new Solution(this.copyRequest, this.copySchedule, instance);

		return copy;
	}
}