import java.util.Arrays;

public class Label 
{
	private final double redCosts;
	private final int totOvertime;
	private final int totMinus;
	private final int[] schedule;
	
	public Label(double redCosts, int totOvertime, int totMinus, int[] schedule) {
		this.redCosts = redCosts;
		this.totOvertime = totOvertime;
		this.totMinus = totMinus;
		this.schedule = schedule;
	}

	public double getRedCosts() {
		return redCosts;
	}

	public int getTotOvertime() {
		return totOvertime;
	}

	public int getTotMinus() {
		return totMinus;
	}

	public int[] getSchedule() {
		return schedule;
	}

	@Override
	public String toString() {
		return "Label [redCosts=" + redCosts + ", totOvertime=" + totOvertime + ", totMinus=" + totMinus + ", schedule="
				+ Arrays.toString(schedule) + "]";
	}
	
	/**
	 * This method returns true if this label dominates the other label.
	 * @param other			another label
	 * @return				a boolean denoting whether this label dominated the other label or not
	 */
	public boolean dominates(Label other) {
		if (this.redCosts <= other.getRedCosts() && this.totOvertime <= other.getTotOvertime() && this.totMinus <= other.getTotMinus()) {
			return true;
		}
		return false;
	}
	
	/**
	 * This method returns true if this label dominates the other label.
	 * @param other			another label
	 * @return				a boolean denoting whether this label dominated the other label or not
	 */
	public boolean dominates2(Label other) {
		if (this.redCosts <= other.getRedCosts() && this.totOvertime <= other.getTotOvertime() && this.totMinus <= other.getTotMinus() && 
				(this.redCosts < other.getRedCosts() || this.totOvertime < other.getTotOvertime() || this.totMinus < other.getTotMinus())) {
			return true;
		}
		return false;
	}
}
