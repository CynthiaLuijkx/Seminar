package Phase5;

/**
 * This class contains the penalties.
 *
 */
public class Penalties {
	private static double[] feasPenalties;
	private static double[] softPenalties;
	public final static double penaltyRequest = 10000;
	
	public Penalties() {
		feasPenalties = new double[3];
		softPenalties = new double[11];
		
		feasPenalties[0] = 1.35;//Quarterly overtime
		feasPenalties[1] = 1000; //number of ATV days
		
		
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
		softPenalties[0] = 5; 
		softPenalties[1] = 5; 
		softPenalties[2] = 5; 
		softPenalties[3] = 5; 
		softPenalties[4] = 10; 
		softPenalties[5] = 10; 
		softPenalties[6] = 10; 
		softPenalties[7] = 10; 
		softPenalties[8] = 10; 
		softPenalties[9] = 10; 
		softPenalties[10] = 10; 

	}

	public double[] getFeasPenalties() {
		return feasPenalties;
	}

	public double[] getSoftPenalties() {
		return softPenalties;
	}
}