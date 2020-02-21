//Not sure yet if I need this

package Phase5;
import Phase5.Request;

/**
 * This method stores the removal of a duty from a schedule.
 * @author Mette Wagenvoort
 *
 */
public class Removal {
	private final Request request;
	private double changedVal;
	
	public Removal(Request request, double changed) {
		this.request = request;
		this.changedVal = changed;
	}

	public double getChangedVal() {
		return changedVal;
	}

	public void setChangedVal(double changedVal) {
		this.changedVal = changedVal;
	}

	public Request getRequest() {
		return request;
	}

}