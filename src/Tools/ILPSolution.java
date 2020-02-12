package Tools;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class ILPSolution {
	private final List<Schedule> schedules;
	private final ArrayList<Set<Duty>> unscheduledPerWeekday;
	
	public ILPSolution(List<Schedule> schedules, ArrayList<Set<Duty>> unscheduledPerWeekday) {
		this.schedules = schedules;
		this.unscheduledPerWeekday = unscheduledPerWeekday;
	}

	public ArrayList<Set<Duty>> getUnscheduledPerWeekday() {
		return unscheduledPerWeekday;
	}

	public List<Schedule>  getSchedules() {
		return schedules;
	}
}
