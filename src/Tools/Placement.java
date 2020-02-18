package Tools;
import Tools.TimeSlot;

public class Placement implements Comparable<Placement>{
	private final Request request;
	private final TimeSlot timeslot;
	private double cost;
	
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
		if(this.getCost() > o.getCost()) {
			return -1; 
		}else if (this.getCost() < o.getCost()) {
			return 1; 
		}else {
			return 0; 
		}
	}
	
	public Placement duplicate() {
		return new Placement(this.request, this.timeslot, this.cost); 
	}
}