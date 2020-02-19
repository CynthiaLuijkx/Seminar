package Tools;
//The class time slot consists of a certain day in a certain schedule of the corresponding contract group
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