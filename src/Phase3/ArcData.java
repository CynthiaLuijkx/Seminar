package Phase3;

/**
 * Data class for arcs in the directed graph containing the dual costs and the working time.
 * @author Mette Wagenvoort
 *
 */
public class ArcData 
{
	private double dualCosts;
	private final int paidMin;
	
	public ArcData(double dualCosts, int paidMin) {
		this.dualCosts = dualCosts;
		this.paidMin = paidMin;
	}

	public double getDualCosts() {
		return dualCosts;
	}

	public void setDualCosts(double dualCosts) {
		this.dualCosts = dualCosts;
	}

	public int getPaidMin() {
		return paidMin;
	}
	
	public double getData(boolean costs) {
		if (costs) {
			return this.dualCosts;
		} else {
			return this.paidMin;
		}
	}

	@Override
	public String toString() {
		return "ArcData [dualCosts=" + dualCosts + ", paidMin=" + paidMin + "]";
	}
}