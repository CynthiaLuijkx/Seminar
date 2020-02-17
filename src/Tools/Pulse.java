package Tools;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Pulse class for the pulse pricing algorithm for Phase 3: Column generation
 * @author Mette Wagenvoort
 *
 */
public class Pulse 
{
	private final double redCosts;
	private final int totMinWorked;
	private final int[] schedule;
	private final List<Set<Integer>> duties;
	private final Pulse prevPulse;
	
	/**
	 * Constructor of a pulse, which stores the reduced costs, the time worked, the schedule, the duties served and the previous pulse.
	 * @param redCosts					the reduced costs of this pulse
	 * @param totMinWorked				the total time worked of this pulse
	 * @param schedule					the schedule
	 * @param duties					the duties included in the pulse schedule
	 * @param prevPulse					the previous pulse
	 */
	public Pulse(double redCosts, int totMinWorked, int[] schedule, List<Set<Integer>> duties, Pulse prevPulse) {
		this.redCosts = redCosts;
		this.totMinWorked = totMinWorked;
		this.schedule = schedule;
		this.duties = duties;
		this.prevPulse = prevPulse;
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
	
	public Pulse getPrevPulse() {
		return prevPulse;
	}

	@Override
	public String toString() {
		return "Label [redCosts=" + redCosts + ", totMinWorked=" + totMinWorked + ", schedule="
				+ Arrays.toString(schedule) + ", duties=" + duties + "]";
	}
}