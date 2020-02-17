package Tools;

import java.util.Set;

public class ScheduleCombination {
	
	private final Set<Schedule> schedules;
	private final Instance instance;
	private final int cost;
	
	public ScheduleCombination(Set<Schedule> input, Instance instance) {
		this.schedules = input;
		this.instance = instance;
		this.cost = calculateCost();
	}
	
	public boolean isFeasible() {
		for(Duty duty : instance.getSunday()) {
			int found = 0;
			for(Schedule schedule : schedules) {
				for(int w = 0; w < schedule.getScheduleArray().length/7; w++) {
					if(schedule.getScheduleArray()[w*7] == duty.getNr()) {
						found++;
						break;
					}
				}
			}
			if(found < 1) {
				return false;
			}
		}
		
		for(Duty duty : instance.getSaturday()) {
			int found = 0; 
			for(Schedule schedule : schedules) {
				for(int w = 0; w < schedule.getScheduleArray().length/7; w++) {
					if(schedule.getScheduleArray()[w*7 +6] == duty.getNr()) {
						found++;
						break;
					}
				}
			}
			if(found < 1) {
				return false;
			}
		}
		for (int s = 1; s <= 5; s++) {
			for (Duty duty : instance.getWorkingDays()) {
				int found = 0;
				for (Schedule schedule : schedules) {
					for (int w = 0; w < schedule.getScheduleArray().length / 7; w++) {
						if (schedule.getScheduleArray()[w * 7 + s] == duty.getNr()) {
							found++;
							break;
						}
					}
				}
				if (found < 1) {
					return false;
				}
			}
		}		
		return true;
	}
	
	public int calculateCost() {
		int cost = 0;
		for(Schedule schedule : schedules) {
			cost = cost + Math.max(0,  schedule.getPlusMin()-schedule.getMinMin());
		}
		return cost;
	}
	
	public int getCost() {
		return this.cost;
	}
	
	public Set<Schedule> getSchedules() {
		return schedules;
	}

	public Instance getInstance() {
		return instance;
	}
	
}
