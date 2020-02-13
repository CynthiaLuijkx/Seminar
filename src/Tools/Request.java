package Tools;
import Tools.Duty;
import java.util.*;
import Tools.ContractGroup;
import Tools.ReserveDutyType;

public class Request {
	private Duty duty;
	private ReserveDutyType reserveDuty;
	private int dutyNumber;
	private ContractGroup group;
	private final int day;
	private List<Placement> listOfPlacements = new ArrayList<Placement>();
	private final int weekday;
	
	public int getDay() {
		return day;
	}
	public Request(Duty duty, ContractGroup group, int day) {
		this.duty = duty;
		this.group = group;
		this.day = day;
		this.dutyNumber = duty.getNr();
		this.weekday = day%7;
	}
	public Request(ReserveDutyType rduty, ContractGroup group, int day) {
		this.reserveDuty = rduty;
		this.group = group;
		this.day = day;
		this.dutyNumber = rduty.getNr();
		this.weekday = day%7;
	}

	public Request(int dutyNumber, ContractGroup group, int day) {
		this.dutyNumber = dutyNumber;
		this.group = group;
		this.day = day;
		this.weekday = day%7;
	}
	public int getStartTime() {
		int start = 0;
		if(this.getDutyNumber() == 1 || this.getDutyNumber() == 2) {
			start = 24*60;
		}
		else if(this.getDutyNumber() < 1000) {
			start = this.getReserveDuty().getStartTime();
		}
		else {
			start = this.getDuty().getStartTime();
		}
		return start;
	}
	public int getEndTime() {
		int end = 0;
		if(this.getDutyNumber() == 1 || this.getDutyNumber() == 2) {
			end = 0;
		}
		else if(this.getDutyNumber() < 1000) {
			end = this.getReserveDuty().getEndTime();
		}
		else {
			end = this.getDuty().getEndTime();
		}
		return end;
	}

	public int getWeekday() {
		return weekday;
	}
	public Duty getDuty() {
		return duty;
	}

	public ContractGroup getGroup() {
		return group;
	}
	public ReserveDutyType getReserveDuty() {
		return reserveDuty;
	}
	
	public int getDutyNumber() {
		return dutyNumber;
	}
	public void addPlacement(Request request, TimeSlot slot, double cost) {
		Placement placement = new Placement(request, slot, cost);
		this.listOfPlacements.add(placement);
	}
	@Override
	public String toString() {
		return "Request [duty=" + duty + ", group=" + group + "]";
	}
	
		
}

