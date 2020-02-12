import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import Tools.*;

public class Phase4_AddMissing {
	private final ILPSolution ilpSolution;
	private final Instance instance;
	private final ArrayList<Set<Duty>> originalDuplicates;
	
	public Phase4_AddMissing(ILPSolution ilpSolution, Instance instance) {
		this.ilpSolution = ilpSolution;
		this.instance = instance;
		this.originalDuplicates = determineDuplicates();
	}
	
	public ArrayList<Set<Duty>> determineDuplicates() {
		ArrayList<Set<Duty>> duplicates = new ArrayList<>();
		for(int i = 0; i <= 6; i++) {
			Set<Duty> duties = new HashSet<>();
			duplicates.add(duties);
		}
		for (int s = 0; s <= 6; s++) {
			if(s == 0) {
				for(Duty duty : instance.getSunday()) {
					int included = 0;
					for(Schedule schedule : ilpSolution.getSchedules()) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[7*w + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included > 1) {
						duplicates.get(s).add(duty);
					}
				}
			}
			else if(s == 6) {
				for(Duty duty : instance.getSunday()) {
					int included = 0;
					for(Schedule schedule : ilpSolution.getSchedules()) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[7*w + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included > 1) {
						duplicates.get(s).add(duty);
					}
				}
			}
			else {
				for(Duty duty : instance.getSunday()) {
					int included = 0;
					for(Schedule schedule : ilpSolution.getSchedules()) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[7*w + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included > 1) {
						duplicates.get(s).add(duty);
					}
				}
			}
		}
		return duplicates;
	}
	
	public ArrayList<Set<Duty>> createDuplicateCopy(){
		ArrayList<Set<Duty>> copy = new ArrayList<>();
		for(int i = 0; i <= 6; i++) {
			Set<Duty> duties = new HashSet<>();
			for(Duty duty : this.originalDuplicates.get(i)) {
				duties.add(duty);
			}
			copy.add(duties);
		}
		
		return copy;
	}

	public void swapDuplicateWithMissing() {
		
	}
	
	public boolean scheduleIsFeasible(int[] schedule) {
		// 7 feasibility
		if(!isFeasible7(schedule)) {
			return false;
		}
		return true;
	}
	
	public boolean isFeasible7(int[] schedule) {
		for (int s = 0; s <= schedule.length - 1; s++) {
			// Don't have to check if it's a rest/ATV day
			if (schedule[s] != 1 && schedule[s] != 2) {
				int start = 0;
				// Get the start time of this duty. We are counting from this point onwards.
				if (instance.getFromDutyNrToDuty().containsKey(schedule[s])) {
					start = instance.getFromDutyNrToDuty().get(schedule[s]).getStartTime();
				} else {
					start = instance.getFromRDutyNrToRDuty().get(schedule[s]).getStartTime();
				}
				// Fix later to go back to the beginning too!
				for (int i = 1; i <= 6; s++) {// For every rolling window of 7 days from this day on
					// If this day is a rest/ATV day
					if (schedule[s+i] == 1 || schedule[s+i] == 2) {
						int consecRest = 24 * 60;

						// The day before
						if (instance.getFromDutyNrToDuty().containsKey(schedule[s+i - 1])) {
							consecRest += 24 * 60 - instance.getFromDutyNrToDuty().get(schedule[s+i - 1]).getEndTime();
						} else {
							consecRest += 24 * 60 - instance.getFromRDutyNrToRDuty().get(schedule[s+i- 1]).getEndTime();
						}

						// If the rest day we're on is the final day of the row, we need to jump one
						// further
						if (i == 6) {
							// We count up to max the start time of the duty
							if (schedule[s + 7] == 1 || schedule[s + 7] == 2) {
								consecRest += start;
							} else if (instance.getFromDutyNrToDuty().containsKey(schedule[s + 7])) {
								consecRest += Math.min(start,
										instance.getFromDutyNrToDuty().get(schedule[s + 7]).getStartTime());
							} else {
								consecRest += Math.min(start,
										instance.getFromRDutyNrToRDuty().get(schedule[s + 7]).getStartTime());
							}
						}
						// The day after
						else {
							if (schedule[s+i + 1] == 1 || schedule[s+i+ 1] == 2) {// ATV/rest day
								consecRest += 24 * 60;
								// Normal duty
							} else if (instance.getFromDutyNrToDuty().containsKey(schedule[s+i + 1])) {
								consecRest += instance.getFromDutyNrToDuty().get(schedule[s+i + 1]).getStartTime();
								// Reserve duty
							} else {
								consecRest += instance.getFromRDutyNrToRDuty().get(schedule[s+i + 1]).getStartTime();
							}
						}
						if (consecRest < 32*60) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public boolean isFeasibe14(int[] schedule) {
		for(int s = 0; s <= schedule.length-1;s++) {
			//Don't have to check if this day is an ATV or Rest day
			if (schedule[s] != 1 && schedule[s] != 2) {
				int start = 0;
				if (instance.getFromDutyNrToDuty().containsKey(schedule[s])) {
					start = instance.getFromDutyNrToDuty().get(schedule[s]).getStartTime();
				} else {
					start = instance.getFromRDutyNrToRDuty().get(schedule[s]).getStartTime();
				}
				
				int consec14 = 0;
				
				for (int i = 1; i <= 13; i++) {
					//If this day is an ATV/Rest day and the day before it isn't 
					//(if that's the case, we shouldn't start counting today)
					if ((schedule[(s+i)%schedule.length] == 1 || schedule[(s+i)%schedule.length] == 2) &&
							(schedule[(s+i-1)%schedule.length] != 1 && schedule[(s+i-1)%schedule.length] != 2)) {
						int consec = 24 * 60;
						
						//Day before
						if (instance.getFromDutyNrToDuty().containsKey(schedule[(s+i-1)%schedule.length])) {
							consec += 24 * 60 - instance.getFromDutyNrToDuty().get(schedule[(s+i-1)%schedule.length]).getEndTime();
						} else {
							consec += 24 * 60 - instance.getFromRDutyNrToRDuty().get(schedule[(s+i-1)%schedule.length]).getEndTime();
						}
						
						//Day after, end of the period
						if (i == 13) {
							if (schedule[(s+14)%schedule.length] == 1 || schedule[(s+14)%schedule.length] == 2) {
								consec += start;
							} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(s+14)%schedule.length])) {
								consec += Math.min(start, instance.getFromDutyNrToDuty().get(schedule[(s+14)%schedule.length]).getStartTime());
							} else {
								consec += Math.min(start, instance.getFromRDutyNrToRDuty().get(schedule[(s+14)%schedule.length]).getStartTime());
							}
						} 
						//Day after, not the end of the period
						else {
							if (schedule[(s+i+1)%schedule.length] == 1 || schedule[(s+i+1)%schedule.length] == 2) {
								consec += 24 * 60;
							} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(s+i+1)%schedule.length])) {
								consec += instance.getFromDutyNrToDuty().get(schedule[(s+i+1)%schedule.length]).getStartTime();
							} else {
								consec += instance.getFromRDutyNrToRDuty().get(schedule[(s+i+1)%schedule.length]).getStartTime();
							}
						}
						
						if (consec >= 32*60) {
							consec14 += consec;
						}
						//System.out.println(consec14 + " " + this.freeTwoWeeks);
					}
				}
				if (consec14 < 72*60) {
					return false;
				}
			} 
		}
		return true;
	}
}
