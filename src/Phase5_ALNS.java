import Tools.*;
import java.util.*;
import java.util.Random;

public class Phase5_ALNS {

	private List<Map<ContractGroup, LSschedule>> listWithSol = new ArrayList<Map<ContractGroup, LSschedule>>();
	private double globalOptimum;
	private Map<ContractGroup, LSschedule> globalSchedule;
	private Random random;
	private int minSizeNeighbourhood;
	private int maxSizeNeighbourhood;
	private Set<Request> dutiesStillAsRequest = new HashSet<>();
	private int nIterations;
	private Map<ContractGroup, LSschedule> copySchedule;
	private Set<Request> copyRequest;
	private final Instance instance;
	
	public Phase5_ALNS (int iterations, Instance instance, Map<ContractGroup, Schedule> startSchedule, long seed){
		this.minSizeNeighbourhood = 2;
		this.maxSizeNeighbourhood = 25;
		this.nIterations = iterations;
		this.instance = instance;
		this.random = new Random(seed);
		Solution solutionALNS = this.executeBasic(startSchedule);
	}
	public Solution executeBasic(Map<ContractGroup, Schedule> startSchedule) {
		int n = 1;
		// find initial solution
		Solution initSol = this.getInitialSol(startSchedule);
		this.globalOptimum = this.QuaterlyOvertime(initSol);
		this.copySolution(initSol);
		this.globalSchedule = this.copySchedule;
		Solution currentSol = initSol;

		while (n <= this.nIterations) {	
			int sizeNeighbourhood = this.random.nextInt(this.maxSizeNeighbourhood - this.minSizeNeighbourhood) + this.minSizeNeighbourhood;
			System.out.println("-----------------------------------------------------------------------");
			System.out.println("ITERATION " + n + ":");
			
			n++;
		}

		return new Solution(null, this.globalSchedule);
	}
	public Solution getInitialSol(Map<ContractGroup, Schedule> startSol) {
		Solution initSol = new Solution(null, null);
		Map<ContractGroup, LSschedule> need = new HashMap<>();
		for(ContractGroup group: startSol.keySet()) {
		LSschedule startLSsol = new LSschedule(startSol.get(group), (double) (startSol.get(group).getPlusMin() - startSol.get(group).getMinMin()), null);
		startLSsol.setWeeklyOvertime(startSol.get(group), instance);
		need.put(group, startLSsol);
		}
		initSol.setNewSchedule(need);
		return initSol;
	}
	public double QuaterlyOvertime(Solution sol) {
		double overtime = 0;
		for(ContractGroup group: sol.getNewSchedule().keySet()) {
			for(int empl = 0; empl < sol.getNewSchedule().get(group).getWeeklyOvertime().length; empl++) {
				for(int i =0; i < 13; i++) { //need to loop over 13 weeks for overtime
					if((empl + i) < sol.getNewSchedule().get(group).getWeeklyOvertime().length){
						overtime = overtime + sol.getNewSchedule().get(group).getWeeklyOvertime()[empl+ i];	
					}
					else {
						int remainder = (empl + i) % sol.getNewSchedule().get(group).getWeeklyOvertime().length;
						overtime = overtime + sol.getNewSchedule().get(group).getWeeklyOvertime()[remainder];		
						}
					}
			}
		}
		return overtime;
	}
	
	public Solution copySolution(Solution solution) {
		Map<ContractGroup, LSschedule> schedule = solution.getNewSchedule();
		this.copySchedule = new HashMap<ContractGroup, LSschedule>();
		for(ContractGroup group: schedule.keySet()) {
			LSschedule newLSschedule = new LSschedule(schedule.get(group).getSchedule(), schedule.get(group).getOvertime(), null);
			newLSschedule.setWeeklyOvertime(schedule.get(group).getSchedule(), instance);
			this.copySchedule.put(group, newLSschedule);
		}

		this.copyRequest = new HashSet<>(solution.getRequests());

		Solution copy = new Solution(this.copyRequest, this.copySchedule);

		return copy;
	}
}
