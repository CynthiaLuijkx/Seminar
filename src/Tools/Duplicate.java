package Tools;

public class Duplicate {
	private final Duty duty;
	private final Schedule schedule;
	private final int day;
	
	public Duplicate(Duty duty, Schedule schedule, int day) {
		this.duty = duty;
		this.schedule = schedule;
		this.day = day;
	}

	public Duty getDuty() {
		return duty;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public int getDay() {
		return day;
	}
}
