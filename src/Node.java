
public class Node 
{
	private final int day;
	private Duty duty;
	private ReserveDutyType reserveDuty;
	private boolean ATV;
	private boolean rest;
	
	public Node(int day) {
		this.day = day;
	}
	
	public Node(int day, Duty duty) {
		this.day = day;
		this.duty = duty;
	}
	
	public Node(int day, ReserveDutyType reserveDuty) {
		this.day = day;
		this.reserveDuty = reserveDuty;
	}
	
	public Node(int day, boolean ATV, boolean rest) {
		this.day = day;
		this.ATV = ATV;
		this.rest = rest;
	}

	public int getDay() {
		return day;
	}

	public Duty getDuty() {
		return duty;
	}
	
	public ReserveDutyType getReserveDutyType() {
		return reserveDuty;
	}
	
	public boolean isATV() {
		return ATV;
	}
	
	public boolean isRest() {
		return rest;
	}

	@Override
	public String toString() {
		return "Node [day=" + day + ", duty=" + duty + ", reserveDuty=" + reserveDuty + ", ATV=" + ATV + ", rest="
				+ rest + "]";
	}
}
