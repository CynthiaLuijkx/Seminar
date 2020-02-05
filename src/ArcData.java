
public class ArcData 
{
	private final int minFromMidnightFrom;
	private final int minTillShiftTo;
	private final boolean ATV;
	private final boolean restDay;
	private final int minWorked;
	private final int minPaid;
	private double dualCosts;
	
	public ArcData(int minFromMidnightFrom, int minTillShiftTo, boolean ATV, boolean restDay, int minWorked, int minPaid) {
		this.minFromMidnightFrom = minFromMidnightFrom;
		this.minTillShiftTo = minTillShiftTo;
		this.ATV = ATV;
		this.restDay = restDay;
		this.minWorked = minWorked;
		this.minPaid = minPaid;
	}

	public double getDualCosts() {
		return dualCosts;
	}

	public void setDualCosts(double dualCosts) {
		this.dualCosts = dualCosts;
	}

	public int getMinFromMidnightFrom() {
		return minFromMidnightFrom;
	}

	public int getMinTillShiftTo() {
		return minTillShiftTo;
	}

	public boolean isATV() {
		return ATV;
	}

	public boolean isRestDay() {
		return restDay;
	}
	
	public int getMinWorked() {
		return minWorked;
	}
	
	public int getMinPaid() {
		return minPaid;
	}

	@Override
	public String toString() {
		return "ArcData [minFromMidnightFrom=" + minFromMidnightFrom + ", minTillShiftTo="
				+ minTillShiftTo + ", ATV=" + ATV + ", freeDay=" + restDay + ", dualCosts=" + dualCosts + "]";
	}
}
