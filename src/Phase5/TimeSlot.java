package Phase5;

import Tools.ContractGroup;

/**
 * This class contains a time slot for the placement of a duty.
 *
 */
public class TimeSlot {
	private final ContractGroup group; //a timeslot contains the contract group of the schedule it is in
	private final int day; //a timeslot has the day in the schedule
	
	public TimeSlot(ContractGroup c, int t) {
		this.group = c;
		this.day = t;
	}

	public ContractGroup getGroup() {
		return group;
	}

	public int getDay() {
		return day;
	}
	
	public boolean equals(TimeSlot other) {
		if(this.group.equals(other.group) && this.day == other.day) {
			return true; 
		}else {
			return false; 
		}
		
	}
}