package Tools;

import java.util.Arrays;

public class Violation3Days {

	public String[] dayTypes; 
	public String[] dutyTypes; 
	
	public Violation3Days(String []dayTypes, String []dutyTypes){
		this.dayTypes = dayTypes; 
		this.dutyTypes = dutyTypes; 
	}
	
	public String[] getDayTypes() {
		return dayTypes;
	}

	public void setDayTypes(String[] dayTypes) {
		this.dayTypes = dayTypes;
	}

	public String[] getDutyTypes() {
		return dutyTypes;
	}

	public void setDutyTypes(String[] dutyTypes) {
		this.dutyTypes = dutyTypes;
	}

	@Override
	public String toString() {
		return "Violation3Days [dayTypes=" + Arrays.toString(dayTypes) + ", dutyTypes=" + Arrays.toString(dutyTypes)
				+ "]";
	}
}
