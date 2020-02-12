import Tools.*;
import java.util.*;

public class Phase5_ALNS {

	private List<Map<ContractGroup, LSschedule>> listWithSol = new ArrayList<Map<ContractGroup, LSschedule>>();
	private double globalOptimum;
	private Map<ContractGroup, LSschedule> globalSchedule;
	private Random random;
	private int minSizeNeighborhood;
	private int maxSizeNeighborhood;
	private Set<Request> dutiesStillAsRequest = new HashSet<>();
	private int nIterations;
	private Map<ContractGroup, LSschedule> copySchedule;
	private Set<Request> copyRequest;
	
	public Phase5_ALNS (int iterations){
		this.minSizeNeighborhood = 2;
		this.maxSizeNeighborhood = 25;
		this.nIterations = iterations;
	}
	public Solution getInitialSol(Map<ContractGroup, Schedule> startSol, Instance instance) {
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
	
	public Solution copySolution(Solution solution, Instance instance) {
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
