package Phase3;

/**
 * This method corresponds to a node in the directed graph containing the day number and the duty number.
 * @author Mette Wagenvoort
 *
 */
public class Node 
{
	private final int dayNr;
	private final int dutyNr;
	
	public Node(int dayNr, int dutyNr) {
		this.dayNr = dayNr;
		this.dutyNr = dutyNr;
	}

	public int getDayNr() {
		return dayNr;
	}

	public int getDutyNr() {
		return dutyNr;
	}

	@Override
	public String toString() {
		return "Node [dayNr=" + dayNr + ", dutyNr=" + dutyNr + "]";
	}
}