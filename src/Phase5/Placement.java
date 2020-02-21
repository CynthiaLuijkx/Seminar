package Phase5;
import Phase5.TimeSlot;

/**
 * This class contains a placement of a duty on a specific time slot.
 * @author Mette Wagenvoort
 *
 */
public class Placement implements Comparable<Placement>{
	private final Request request; //a placement is of a certain request
	private final TimeSlot timeslot; //a placement can be put on a certain timeslot
	private double cost; //a placement has a cost involved
	
	public Placement(Request request, TimeSlot slot, double cost) {
		this.request = request;
		this.timeslot = slot;
		this.cost = cost; 
		
	}
	
	public double getCost() {
		return cost;
	}
	
	@Override
	public String toString() {
		return "Placement [request=" + request + ", timeslot=" + timeslot + ", cost=" + cost + "]";
	}

	public void setCost(double cost) {
		this.cost = cost;
	}

	public TimeSlot getTimeslot() {
		return timeslot;
	}

	public Request getRequest() {
		return request;
	}

	@Override
	public int compareTo(Placement o) {
		if(this.getCost() < o.getCost()) {
			return -1; 
		}else if (this.getCost() > o.getCost()) {
			return 1; 
		}else {
			return 0; 
		}
	}
	
	public Placement duplicate() {
		return new Placement(this.request, this.timeslot, this.cost); 
	}
}