package Tools;

public class Penalties {
	private static double[] feasPenalties;
	private static double[] softPenalties;
	public final static double penaltyRequest = 10000;
	
	public Penalties() {
		feasPenalties = new double[2];
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
		softPenalties[0] = 1000; 
		softPenalties[1] = 500; 
		softPenalties[2] = 500; 
		softPenalties[3] = 500; 
		softPenalties[4] = 1000; 
		softPenalties[5] = 1000; 
		softPenalties[6] = 1000; 
		softPenalties[7] = 1000; 
		softPenalties[8] = 1000; 
		softPenalties[9] = 1000; 
		softPenalties[10] = 1000; 

	}

	public double[] getFeasPenalties() {
		return feasPenalties;
	}

	public double[] getSoftPenalties() {
		return softPenalties;
	}
}
