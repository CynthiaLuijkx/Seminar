package Tools;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * This class contains a label for the labelling pricing algorithm.
 * @author Mette Wagenvoort
 *
 */
public class Label 
{
	private final double redCosts;
	private final int totMinWorked;
	private final int[] schedule;
	private final List<Set<Integer>> duties;
	
	public Label(double redCosts, int totMinWorked, int[] schedule, List<Set<Integer>> duties) {
		this.redCosts = redCosts;
		this.totMinWorked = totMinWorked;
		this.schedule = schedule;
		this.duties = duties;
	}

	public double getRedCosts() {
		return redCosts;
	}

	public int getTotMinWorked() {
		return totMinWorked;
	}

	public int[] getSchedule() {
		return schedule;
	}

	public List<Set<Integer>> getDuties() {
		return duties;
	}

	@Override
	public String toString() {
		return "Label [redCosts=" + redCosts + ", totMinWorked=" + totMinWorked + ", schedule="
				+ Arrays.toString(schedule) + ", duties=" + duties + "]";
	}
}