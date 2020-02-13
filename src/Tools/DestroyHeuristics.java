package Tools;
import java.util.*;

public class DestroyHeuristics {
	public DestroyHeuristics() {
	}
	//---Random Removal
	public void executeRandom(Solution solution, int nRemove, Random random, Instance instance) {
		Set<TimeSlot> slots = new HashSet<>();
		for(int i =0; i < nRemove; i++) {
			int contGroup = random.nextInt(solution.getNewSchedule().keySet().size())+1;
			for(ContractGroup group: solution.getNewSchedule().keySet()) {
				if(group.getNr() == contGroup) {
				 int dutyDay = random.nextInt(group.getTc());
				 if(instance.getFromDutyNrToDuty().containsKey(solution.getNewSchedule().get(group).getLSSchedule().getSchedule()[dutyDay])){
					
					 Request request = new Request(instance.getFromDutyNrToDuty().get(solution.getNewSchedule().get(group).getLSSchedule().getSchedule()[dutyDay]), group, dutyDay, solution.getNewSchedule().get(group).getLSSchedule().getSchedule()[dutyDay]);	
					 //System.out.println(request.toString());
					 solution.removeRequest(request, solution, slots, dutyDay);
				 }
				 else if(instance.getFromRDutyNrToRDuty().containsKey(solution.getNewSchedule().get(group).getLSSchedule().getSchedule()[dutyDay])) {
					Request request = new Request(instance.getFromRDutyNrToRDuty().get(solution.getNewSchedule().get(group).getLSSchedule().getSchedule()[dutyDay]), group, dutyDay, solution.getNewSchedule().get(group).getLSSchedule().getSchedule()[dutyDay]);	
					// System.out.println(request.toString());
					solution.removeRequest(request, solution, slots, dutyDay);
						 
				 }
				 else {
					 Request request = new Request(solution.getNewSchedule().get(group).getLSSchedule().getSchedule()[dutyDay], group, dutyDay);
					// System.out.println(request.toString());
					 solution.removeRequest(request, solution, slots, dutyDay);
				 }
			}
		    
		  }
		}
	}

}
