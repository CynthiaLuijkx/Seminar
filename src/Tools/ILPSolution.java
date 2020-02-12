package Tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class ILPSolution {
	private final Set<Schedule> schedules;
	private final ArrayList<Set<Duty>> unscheduledPerWeekday;
	
	public ILPSolution(Set<Schedule> schedules, ArrayList<Set<Duty>> unscheduledPerWeekday) {
		this.schedules = schedules;
		this.unscheduledPerWeekday = unscheduledPerWeekday;
	}

	public ArrayList<Set<Duty>> getUnscheduledPerWeekday() {
		return unscheduledPerWeekday;
	}

	public Set<Schedule>  getSchedules() {
		return schedules;
	}
}
