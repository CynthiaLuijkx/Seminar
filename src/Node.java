
public class Node 
{
	private final int day;
	private final int dutyNr;
	
	public Node(int day, int dutyNr) {
		this.day = day;
		this.dutyNr = dutyNr;
	}

	public int getDay() {
		return day;
	}

	public int getDutyNr() {
		return dutyNr;
	}

	@Override
	public String toString() {
		return "Node [day=" + day + ", dutyNr=" + dutyNr + "]";
	}
}
