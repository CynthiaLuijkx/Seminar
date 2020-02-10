package Tools;
import java.util.Arrays;

public class Schedule 
{
	private final ContractGroup c;
	private final int minMin;
	private final int plusMin;
	private final int[] schedule;
	
	public Schedule(ContractGroup c, int minMin, int plusMin, int[] schedule) {
		this.c = c;
		this.minMin = minMin;
		this.plusMin = plusMin;
		this.schedule = schedule;
	}

	public ContractGroup getC() {
		return c;
	}

	public int getMinMin() {
		return minMin;
	}

	public int getPlusMin() {
		return plusMin;
	}

	public int[] getSchedule() {
		return schedule;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((c == null) ? 0 : c.hashCode());
		result = prime * result + minMin;
		result = prime * result + plusMin;
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
		if (minMin != other.minMin)
			return false;
		if (plusMin != other.plusMin)
			return false;
		if (!Arrays.equals(schedule, other.schedule))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Schedule [c=" + c + ", minMin=" + minMin + ", plusMin=" + plusMin + ", schedule="
				+ Arrays.toString(schedule) + "]";
	}
}