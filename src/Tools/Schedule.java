package Tools;
import java.util.Arrays;

/**
 * This class corresponds to a feasible schedule for a specific contractgroup.
 * @author Mette Wagenvoort
 *
 */
public class Schedule 
{
	private final ContractGroup c;
	private final int overTime;
	private final int[] schedule;
	
	public Schedule(ContractGroup c, int overTime, int[] schedule) {
		this.c = c;
		this.overTime = overTime;
		this.schedule = schedule;
	}

	public ContractGroup getC() {
		return c;
	}

	public int getOvertime() {
		return overTime;
	}

	public int[] getSchedule() {
		return schedule;
	}
	
	public Schedule copy() {
		int[] copySchedule = new int[schedule.length];
		for (int i = 0; i < copySchedule.length; i++) {
			copySchedule[i] = schedule[i];
		}
		return new Schedule(c, overTime, copySchedule);
	}

	@Override
	public String toString() {
		return "Schedule [c=" + c + ", overTime=" + overTime + ", schedule=" + Arrays.toString(schedule) + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((c == null) ? 0 : c.hashCode());
		result = prime * result + overTime;
		result = prime * result + Arrays.hashCode(schedule);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Schedule other = (Schedule) obj;
		if (c == null) {
			if (other.c != null)
				return false;
		} else if (!c.equals(other.c))
			return false;
		if (overTime != other.overTime)
			return false;
		if (!Arrays.equals(schedule, other.schedule))
			return false;
		return true;
	}
}
