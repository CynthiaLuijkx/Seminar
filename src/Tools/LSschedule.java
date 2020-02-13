package Tools;
import Tools.Schedule;
import Tools.Instance;

public class LSschedule {
	private final Schedule schedule;
	private double overtime;
	private double[] weeklyOvertime;
	//private final Instance instance;
	
	public LSschedule(Schedule schedule, double overtime, double[] weeklyOvertime) {
		this.schedule = schedule;
		this.overtime = overtime;
		this.weeklyOvertime = weeklyOvertime;
		this.weeklyOvertime = new double[schedule.getSchedule().length/7];
	}

	public Schedule getLSSchedule() {
		return schedule;
	}

	

	public double getOvertime() {
		return overtime;
	}

	public void setOvertime(double overtime) {
		this.overtime = overtime;
	}

	public double[] getWeeklyOvertime() {
		return weeklyOvertime;
	}

	public void setWeeklyOvertime(Schedule schedule, Instance instance) {
		int sum = 0;
		for(int  k = 0; k < (schedule.getSchedule().length/7); k++) {
			sum = 0;
			for(int i = 7*k; i < (7*k+6); i++) {
				if(instance.getFromDutyNrToDuty().containsKey(schedule.getSchedule()[i])) {
					sum += instance.getFromDutyNrToDuty().get(schedule.getSchedule()[i]).getPaidMin();
				}
				else if(instance.getFromRDutyNrToRDuty().containsKey(schedule.getSchedule()[i])) {
					sum += schedule.getC().getAvgHoursPerDay()*60;
				}
				else if(schedule.getSchedule()[i] == 1) {
					sum += schedule.getC().getAvgHoursPerDay()*60;
				}
			}
		
		this.weeklyOvertime[k] = sum;
	}
	}
}
