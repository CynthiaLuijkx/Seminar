package Tools;
import Tools.TimeSlot;
import java.util.*;

public class Placement {
	private List<Map<TimeSlot, Double>> possibilities = new ArrayList<Map<TimeSlot, Double>>();
	private final Request request;
	private double changedOvertime;
	
	public Placement(List<Map<TimeSlot, Double>> pos, Request request) {
		this.possibilities = pos;
		this.request = request;
		this.changedOvertime = Double.MAX_VALUE;
	}
	public List<Map<TimeSlot, Double>> getPossibilities() {
		return possibilities;
	}

	public void setPossibilities(List<Map<TimeSlot, Double>> pos) {
		this.possibilities = pos;
	}

	public double getChangedOvertime() {
		return changedOvertime;
	}

	public void setChangedOvertime(double changedOvertime) {
		this.changedOvertime = changedOvertime;
	}

	public Request getRequest() {
		return request;
	}

	
	
}
