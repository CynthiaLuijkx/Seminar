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
	public String toString() {
		return "Schedule [c=" + c + ", minMin=" + minMin + ", plusMin=" + plusMin + ", schedule="
				+ Arrays.toString(schedule) + "]";
	}
}
