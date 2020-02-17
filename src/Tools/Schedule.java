package Tools;
import java.util.Arrays;

public class Schedule implements Cloneable 
{
	private final ContractGroup c;
	private final int overTime;
	private final int[] schedule;
	private double[] weeklyOvertime;
	private static Instance instance; 
	
	
	
	public Schedule(ContractGroup c, int overTime, int[] schedule) {
		this.c = c;
		this.overTime = overTime;
		this.schedule = schedule;
		this.weeklyOvertime = new double[schedule.length]; 
		this.setWeeklyOvertime(this, instance);
	}

	public static void setInstance(Instance instance) {
		Schedule.instance = instance; 
	}
	
	public ContractGroup getC() {
		return c;
	}

	public int getOvertime() {
		return overTime;
	}

	/**
	 * Returns the array with the duty numbers
	 * @return
	 */
	public int[] getScheduleArray() {
		return schedule;
	}
	
	
	public void setWeeklyOvertime(Schedule schedule, Instance instance) {
		int sum = 0;
		for(int  k = 0; k < (schedule.getScheduleArray().length/7); k++) {
			sum = 0;
			for(int i = 7*k; i < (7*k+6); i++) {
				if(instance.getFromDutyNrToDuty().containsKey(schedule.getScheduleArray()[i])) {
					sum += instance.getFromDutyNrToDuty().get(schedule.getScheduleArray()[i]).getPaidMin();
				}
				else if(instance.getFromRDutyNrToRDuty().containsKey(schedule.getScheduleArray()[i])) {
					sum += schedule.getC().getAvgHoursPerDay()*60;
				}
				else if(schedule.getScheduleArray()[i] == 1) {
					sum += schedule.getC().getAvgHoursPerDay()*60;
				}
			}

			this.weeklyOvertime[k] = sum - (schedule.getC().getAvgDaysPerWeek()*schedule.getC().getAvgHoursPerDay()*60) ;
		}
		
	}
	
	public double[] getWeeklyOvertime() {
		return weeklyOvertime;
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
		for(int i = 0; i < schedule.length; i++) {
			if(schedule[i] != other.schedule[i]){
				return false;
			}
		}
		return true;
	}
	
	public Schedule clone() {
		return new Schedule(this.c, this.overTime, this.schedule.clone()); 
	}
}