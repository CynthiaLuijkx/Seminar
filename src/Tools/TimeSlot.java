package Tools;

public class TimeSlot {
	private final ContractGroup group;
	private final int day;
	
	public TimeSlot(ContractGroup c, int t) {
		this.group = c;
		this.day = t;
	}
}
