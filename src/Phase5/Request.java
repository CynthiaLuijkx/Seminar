package Phase5;
import Tools.Duty;
import java.util.*;
import Tools.ContractGroup;
import Tools.ReserveDutyType;
 
/**
 * This class stores a duty that should be placed.
 * @author Mette Wagenvoort
 *
 */
public class Request {
	private Duty duty; //a request can have a duty 
	private ReserveDutyType reserveDuty; //a request can have a reserve duty
	private int dutyNumber; //a request can be an ATV duty
	private ContractGroup group; //a request contains the contract group the duty is removed from
	private int day; //a request has the day on which the duty is removed
	private List<Placement> listOfPlacements = new ArrayList<Placement>(); //contains all the placements on which the request can be put
	private final int weekday; //the weekday the duty should be executed on
	
	public int getDay() {
		return day;
	}
	
	public void setDay(int newDay) {
		this.day = newDay;
	}
	
	public void setGroup(ContractGroup newGroup) {
		this.group = newGroup;
	}
	
	public Request(Duty duty, ContractGroup group, int day) {
		this.duty = duty;
		this.group = group;
		this.day = day;
		this.dutyNumber = duty.getNr(); 
		this.weekday = day%7;
	}
	
	public Request(ReserveDutyType rduty, ContractGroup group, int day ) {
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
	
	//Determine the startime of the duty
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
	
	/**
	 * This method sets the end time of a duty.
	 * @return
	 */
	public int getEndTime() {
		int end = 0;
		if(this.getDutyNumber() == 1 ||this.getDutyNumber() == 2) {
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
	
	/**
	 * This method adds a placement to the list of placements.
	 * @param request
	 * @param slot
	 * @param cost
	 */
	public void addPlacement(Request request, TimeSlot slot, double cost) {
		//System.out.println("Duty number: "  +request.getDutyNumber());
		Placement placement = new Placement(request, slot, cost);
		this.listOfPlacements.add(placement);
	}
	
	public void addPlacement(Placement placement) {
		this.listOfPlacements.add(placement); 
	}
	
	public List<Placement> getPlacements(){
		return this.listOfPlacements; 
	}
		
	public void deletePlacement(Placement placement) {
		this.listOfPlacements.remove(placement); 
	}
	
	//delete a placement from the list
	/**
	 * This method deletes a placement from the list.
	 * @param group
	 */
	public void deletePlacements(ContractGroup group) {
		Set<Placement> toDelete = new HashSet<Placement>(); 
		for(Placement placement: this.listOfPlacements) {
			if(placement.getTimeslot().getGroup().equals(group)) {
				toDelete.add(placement); 
			}
		}
		
		this.listOfPlacements.removeAll(toDelete); 
	}
	
	public void deleteAllPlacements() {
		this.listOfPlacements.clear();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + day;
		result = prime * result + ((duty == null) ? 0 : duty.hashCode());
		result = prime * result + dutyNumber;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((reserveDuty == null) ? 0 : reserveDuty.hashCode());
		result = prime * result + weekday;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Request other = (Request) obj;
		if (day != other.day)
			return false;
		if (duty == null) {
			if (other.duty != null)
				return false;
		} else if (!duty.equals(other.duty))
			return false;
		if (dutyNumber != other.dutyNumber)
			return false;
		if (group == null) {
			if (other.group != null)
				return false;
		} else if (!group.equals(other.group))
			return false;
		if (reserveDuty == null) {
			if (other.reserveDuty != null)
				return false;
		} else if (!reserveDuty.equals(other.reserveDuty))
			return false;
		if (weekday != other.weekday)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Request [dutyNr=" + this.dutyNumber + ", group=" + group + "]";
	}		
}