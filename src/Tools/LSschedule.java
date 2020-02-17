package Tools;
import Tools.Schedule;
import Tools.Instance;

public class LSschedule {
	private final Schedule schedule;
	private double[] weeklyOvertime;
	
	//private final Instance instance;

	public LSschedule(Schedule schedule, double[] weeklyOvertime) {
		this.schedule = schedule;
		this.weeklyOvertime = weeklyOvertime;
		this.weeklyOvertime = new double[schedule.getScheduleArray().length/7];
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public double[] getWeeklyOvertime() {
		return weeklyOvertime;
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
	
	public LSschedule clone() {
		return new LSschedule(this.schedule.clone(), this.weeklyOvertime.clone()); 
	}
}
