//Not sure yet if I need this

package Tools;
import Tools.Request;

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
