package Tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

/**
 * This class contains the penalties.
 *
 */
public class Penalties {
	private static double[] feasPenalties;
	private static double[] softPenalties;
	private static double[] fairPenalties; 
	public final static double penaltyRequest = 10000;
	
	public Penalties() throws FileNotFoundException {
		feasPenalties = new double[2];
		softPenalties = new double[12];
		fairPenalties = new double[7]; 
		
		feasPenalties[0] = 1.35;//Quarterly overtime
		feasPenalties[1] = 1000; //number of ATV days
		
		Scanner sc = new Scanner(new File("Data/Penalties.txt"));
		sc.useLocale(Locale.US);
		
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
		 * 10 check that at most 3 duties of the same type are in a row\
		 * 11: no more than 3 rest+ATV in a week
		 */
		softPenalties[0] = sc.nextDouble();
		softPenalties[1] = sc.nextDouble();
		softPenalties[2] = sc.nextDouble();
		softPenalties[3] = sc.nextDouble();
		softPenalties[4] = sc.nextDouble();
		softPenalties[5] = sc.nextDouble();
		softPenalties[6] = sc.nextDouble();
		softPenalties[7] = sc.nextDouble();
		softPenalties[8] = sc.nextDouble();
		softPenalties[9] = sc.nextDouble();
		softPenalties[10] = sc.nextDouble();
		softPenalties[11] = sc.nextDouble();
		
		/*
		 * 0:	ReserveDuties Distribution 
		 * 1:	Working Sundays Distribution 
		 * 2: 	Desirability Distribution 
		 * 3: 	Distribution split duties 
		 * 4: 	Distribution attractiveness
		 * 5:	Distribution Early duties
		 * 6:	Distribution Late duties
		 */
		
		fairPenalties[0] = sc.nextDouble();
		fairPenalties[1] = sc.nextDouble();
		fairPenalties[2] = sc.nextDouble();
		fairPenalties[3] = sc.nextDouble();
		fairPenalties[4] = sc.nextDouble();
		fairPenalties[5] = sc.nextDouble();
		fairPenalties[6] = sc.nextDouble();
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