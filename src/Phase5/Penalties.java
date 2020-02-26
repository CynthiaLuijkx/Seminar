package Phase5;

/**
 * This class contains the penalties.
 *
 */
public class Penalties {
	private static double[] feasPenalties;
	private static double[] softPenalties;
	private static double[] fairPenalties; 
	public final static double penaltyRequest = 10000;
	
	public Penalties() {
		feasPenalties = new double[2];
		softPenalties = new double[11];
		fairPenalties = new double[3]; 
		
		feasPenalties[0] = 1.35;//Quarterly overtime
		feasPenalties[1] = 1000; //number of ATV days
		
		/*
		 * 0	:	ReserveDuties Distribution 
		 * 1:	Working Sundays Distribution 
		 * 2: 	Desirability Distribution 
		 */
		
		fairPenalties[0] = 1000; 
		fairPenalties[1] = 1000; 
		fairPenalties[2] = 1000; 
		
		/*
		 * 0: ATV spread
		 * 1: no more than 2 reserve duties
		 * 2: max of 5 consecutive duties
		 * 3: no more than 5 duties + ATV days in a week
		 * 4: ATV days are not preferred in the weekends
		 * 5: if the contract group has part timers, gives back the number of not part time duties
		 * 6: do not want early followed by late duties
		 * 7: want ATV and rest after one another (check how often a rest/atv stands alone)
		 * 8: loose duties are not preferred
		 * 9: check at least 2 duties of the same type in a row
		 * 10 check that at most 3 duties of the same type are in a row
		 */
		softPenalties[0] = 100; 
		softPenalties[1] = 100; 
		softPenalties[2] = 100; 
		softPenalties[3] = 100; 
		softPenalties[4] = 100; 
		softPenalties[5] = 100; 
		softPenalties[6] = 100; 
		softPenalties[7] = 100; 
		softPenalties[8] = 100; 
		softPenalties[9] = 100; 
		softPenalties[10] = 100; 
	}

	
	public double[] getFairPenalties() {
		return fairPenalties; 
	}
	
	public double[] getFeasPenalties() {
		return feasPenalties;
	}

	public double[] getSoftPenalties() {
		return softPenalties;
	}
}