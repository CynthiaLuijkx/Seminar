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
		this.penaltiesFeas = new double[6]; //feasibilty7, feasibility14, overTimeFeasible, restTimeFeasible, ATVfeasible, Quarterly
		this.penaltiesFeas[0] = 1000;
		this.penaltiesFeas[1] = 1000;
		this.penaltiesFeas[2] = 10000; //more strict
		this.penaltiesFeas[3] = 1000;
		this.penaltiesFeas[4] = 10000; //more strict
		this.penaltiesFeas[5] = 1;
	}
	public List<Placement> updatePlacements(Request request, Solution solution, double[] newOvertime){
		List<Placement> list = new ArrayList<Placement>();
		for(ContractGroup group: solution.getNewSchedule().keySet()) {
			
		for(int i = request.getWeekday(); i < solution.getNewSchedule().get(group).getLSSchedule().getSchedule().length; i+=7) {
			double costOfPlacement = 0;
			if(solution.getNewSchedule().get(group).getLSSchedule().getSchedule()[i] == 2) {
				int[] check = solution.getNewSchedule().get(group).getLSSchedule().getSchedule();
				check[i] = request.getDutyNumber();
				boolean[] checkFeasibility = new boolean[5]; //feasibilty7, feasibility14, overTimeFeasible, restTimeFeasible, ATVfeasible
				checkFeasibility[0] = this.feasCheck.isFeasible7(check, i-7, i+7);
				checkFeasibility[1] = this.feasCheck.isFeasible14(check, i-14, i+14);
				checkFeasibility[2] = this.feasCheck.overTimeFeasible(check, request.getGroup());
				checkFeasibility[3] = this.feasCheck.restTimeFeasible(check, i, request.getStartTime(), request.getEndTime());
				checkFeasibility[4] = this.feasCheck.checkATVDays(check, request.getGroup());
				//System.out.println(checkFeasibility);
				
				double[] changedOverTime = newOvertime;
				changedOverTime[group.getNr()-1] = this.feasCheck.QuaterlyOvertime(check, group);
				
				for(int j =0; j < checkFeasibility.length; j++) {
					if(checkFeasibility[j] == false) {
						costOfPlacement += this.penaltiesFeas[j];
					}
				}
				costOfPlacement += changedOverTime[group.getNr()-1];
				
			}
			list.add(new Placement(request, new TimeSlot(group,i), costOfPlacement));
			}
		}
		
		return list;
	}
	
	public double sumOfArray(double[] array) {
		double sum = 0;
		for(int i =0; i < array.length; i++) {
			sum += array[i];
		}
		return sum;
	}
}
