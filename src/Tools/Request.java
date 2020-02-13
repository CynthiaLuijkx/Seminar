package Tools;
import Tools.Duty;
import Tools.ContractGroup;
import Tools.ReserveDutyType;

public class Request {
	private Duty duty;
	private ReserveDutyType reserveDuty;
	private int dutyNumber;
	private ContractGroup group;
	
	public Request(Duty duty, ContractGroup group) {
		this.duty = duty;
		this.group = group;
	}
	public Request(ReserveDutyType rduty, ContractGroup group) {
		this.reserveDuty = rduty;
		this.group = group;
	}

	public Request(int dutyNumber, ContractGroup group) {
		this.dutyNumber = dutyNumber;
		this.group = group;
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
		
}

