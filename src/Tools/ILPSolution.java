package Tools;

import java.util.List;
import java.util.ArrayList;

public class ILPSolution {
	private final List<Schedule> schedules;
	private final ArrayList<List<Duty>> unscheduledPerWeekday;
	
	public ILPSolution(List<Schedule> schedules, ArrayList<List<Duty>> unscheduledPerWeekday) {
		this.schedules = schedules;
		this.unscheduledPerWeekday = unscheduledPerWeekday;
	}

	public ArrayList<List<Duty>> getUnscheduledPerWeekday() {
		return unscheduledPerWeekday;
	}

	public List<Schedule>  getSchedules() {
		return schedules;
	}
}
