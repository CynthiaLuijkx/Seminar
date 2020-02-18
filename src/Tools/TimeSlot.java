package Tools;

public class TimeSlot {
	private final ContractGroup group;
	private final int day;
	
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