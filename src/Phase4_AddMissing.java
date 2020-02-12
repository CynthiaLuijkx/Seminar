import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
		
		this.swapDuplicatesWithMissing();
		for(Schedule schedule : ilpSolution.getSchedules()) {
			System.out.println(schedule.toString());
		}
		determineDuplicates();
	}
	
	public ArrayList<Set<Duty>> determineDuplicates() {
		ArrayList<Set<Duty>> duplicates = new ArrayList<>();
		int counter = 0;
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
							//System.out.println(duty.getNr() + " " + schedule.getSchedule()[(7*w) + s]);
							if(schedule.getSchedule()[(7*w) + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included > 1) {
						duplicates.get(s).add(duty);
						counter++;
					}
				}
			}
			else if(s == 6) {
				for(Duty duty : instance.getSaturday()) {
					int included = 0;
					for(Schedule schedule : ilpSolution.getSchedules()) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							//System.out.println(duty.getNr() + " " +  schedule.getSchedule()[(7*w) + s]);
							if(schedule.getSchedule()[(7*w) + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included > 1) {
						duplicates.get(s).add(duty);
						counter++;
					}
				}
			}
			else {
				for(Duty duty : instance.getWorkingDays()) {
					int included = 0;
					for(Schedule schedule : ilpSolution.getSchedules()) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							//System.out.println(duty.getNr() + " " +  schedule.getSchedule()[(7*w) + s]);
							if(schedule.getSchedule()[(7*w)+ s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included > 1) {
						duplicates.get(s).add(duty);
						counter++;
					}
				}
			}
		}
		System.out.println("Number of duplicates: " + counter);
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

	public void swapDuplicatesWithMissing() {
		ArrayList<Set<Duty>> duplicatesCopy = this.createDuplicateCopy();
		ArrayList<Set<Duty>> duplicatesTaken = new ArrayList<>();
		for (int i = 0; i < 7; i++) {
			Set<Duty> duties = new HashSet<>();
			duplicatesTaken.add(duties);
		}
		int totalInserted = 0;
		for (int i = 0; i < 7; i++) {
			HashSet<Duty> insertedDuties = new HashSet<>();
			for (Duty toInsert : ilpSolution.getUnscheduledPerWeekday().get(i)) {
				if (!insertedDuties.contains(toInsert)) {
					
					for (Duty toDelete : duplicatesCopy.get(i)) {
						
						if (!insertedDuties.contains(toInsert) && toInsert.getType().equals(toDelete.getType())
								&& !duplicatesTaken.get(i).contains(toDelete)) {
							
							List<Schedule> schedulesChecked = new ArrayList<>();
							while (schedulesChecked.size() != ilpSolution.getSchedules().size()) {
								int random = (int) (Math.random() * ilpSolution.getSchedules().size());

								if (!schedulesChecked.contains(ilpSolution.getSchedules().get(random))) {
									schedulesChecked.add(ilpSolution.getSchedules().get(random));
									int[] scheduleArray = ilpSolution.getSchedules().get(random).getSchedule();
									
									for (int t = 0; t < scheduleArray.length / 7; t++) {//Could save time by saving where this duplicate is located
										if (!insertedDuties.contains(toInsert) && !duplicatesTaken.get(i).contains(toDelete)) {
											int current = (t * 7) + i;
											if (scheduleArray[current] == toDelete.getNr()) {

												if (restTimeFeasible(scheduleArray, current, toInsert.getStartTime(),
														toInsert.getEndTime())) {
													scheduleArray[t * 7 + i] = toInsert.getNr();
													//System.out.println("Trying to swap:" + " " + (i) + " " + random + " "
													//		+ toInsert.getNr() + " " + toDelete.getNr());
													if (scheduleIsFeasible(scheduleArray)) {
														System.out.println("Swapped:" + " " + (i) + " " + random + " "
																+ toInsert.getNr() + " " + toDelete.getNr());
														duplicatesTaken.get(i).add(toDelete);
														insertedDuties.add(toInsert);
													} else {
														scheduleArray[t * 7 + i] = toDelete.getNr();
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			totalInserted = totalInserted + insertedDuties.size();
		}

		System.out.println("Total inserted: " + totalInserted);
	}

	public boolean scheduleIsFeasible(int[] schedule) {
		// 7 feasibility
		//if(!isFeasible7(schedule)) {
		//	System.out.println("7 check failed");
		//	return false;
		//}
		//if(!isFeasible14(schedule)) {
		//	System.out.println("14 check failed");
		//	return false;
		//}
		return true;
	}
	
	public boolean isFeasible7(int[] schedule) {
		for (int s = 0; s <= schedule.length - 1; s++) {
			
			// Don't have to check if it's a rest/ATV day
			if (schedule[s] != 1 && schedule[s] != 2) {
				boolean rangeFeasible = false;
				int start = 0;
				// Get the start time of this duty. We are counting from this point onwards.
				if (instance.getFromDutyNrToDuty().containsKey(schedule[s])) {
					start = instance.getFromDutyNrToDuty().get(schedule[s]).getStartTime();
				} else {
					start = instance.getFromRDutyNrToRDuty().get(schedule[s]).getStartTime();
				}

				for (int i = 1; i <= 6; i++) {// For every rolling window of 7 days from this day on
					if (!rangeFeasible) {
						// If this day is a rest/ATV day
						if (schedule[(s + i) % schedule.length] == 1 || schedule[(s + i) % schedule.length] == 2) {
							int consecRest = 24 * 60;

							// The day before
							if (instance.getFromDutyNrToDuty()
									.containsKey(schedule[(s + i - 1) % schedule.length])) {
								consecRest += 24 * 60 - instance.getFromDutyNrToDuty()
										.get(schedule[(s + i - 1) % schedule.length]).getEndTime();
							} else {
								consecRest += 24 * 60 - instance.getFromRDutyNrToRDuty()
										.get(schedule[(s + i - 1) % schedule.length]).getEndTime();
							}

							// If the rest day we're on is the final day of the row, we need to jump one
							// further
							if (i == 6) {
								// We count up to max the start time of the duty
								if (schedule[(s + 7) % schedule.length] == 1
										|| schedule[(s + 7) % schedule.length] == 2) {
									consecRest += start;
								} else if (instance.getFromDutyNrToDuty()
										.containsKey(schedule[(s + 7) % schedule.length])) {
									consecRest += Math.min(start, instance.getFromDutyNrToDuty()
											.get(schedule[(s + 7) % schedule.length]).getStartTime());
								} else {
									consecRest += Math.min(start, instance.getFromRDutyNrToRDuty()
											.get(schedule[(s + 7) % schedule.length]).getStartTime());
								}
							}
							// The day after
							else {
								if (schedule[(s + i + 1) % schedule.length] == 1
										|| schedule[(s + i + 1) % schedule.length] == 2) {// ATV/rest day
									consecRest += 24 * 60;
									// Normal duty
								} else if (instance.getFromDutyNrToDuty()
										.containsKey(schedule[(s + i + 1) % schedule.length])) {
									consecRest += instance.getFromDutyNrToDuty()
											.get(schedule[(s + i + 1) % schedule.length]).getStartTime();
									// Reserve duty
								} else {
									consecRest += instance.getFromRDutyNrToRDuty()
											.get(schedule[(s + i + 1) % schedule.length]).getStartTime();
								}
							}
							if (consecRest >= 32 * 60) {
								System.out.println("reached for" + s);
								rangeFeasible = true;
							}
						}
					}
				}
				if(!rangeFeasible) {
					System.out.println(s);
					return false;
				}
			}
		}
		return true;
	}

	public boolean isFeasible14(int[] schedule) {
		for (int s = 0; s <= schedule.length - 1; s++) {
			// Don't have to check if this day is an ATV or Rest day
			if (schedule[s] != 1 && schedule[s] != 2) {
				boolean rangeFeasible = false;
				int start = 0;
				if (instance.getFromDutyNrToDuty().containsKey(schedule[s])) {
					start = instance.getFromDutyNrToDuty().get(schedule[s]).getStartTime();
				} else {
					start = instance.getFromRDutyNrToRDuty().get(schedule[s]).getStartTime();
				}

				int consec14 = 0;

				for (int i = 1; i <= 13; i++) {
					// If this day is an ATV/Rest day and the day before it isn't
					// (if that's the case, we shouldn't start counting today)
					if (!rangeFeasible) {
						if ((schedule[(s + i) % schedule.length] == 1 || schedule[(s + i) % schedule.length] == 2)
								&& (schedule[(s + i - 1) % schedule.length] != 1
										&& schedule[(s + i - 1) % schedule.length] != 2)) {
							int consec = 24 * 60;

							// Day before
							if (instance.getFromDutyNrToDuty()
									.containsKey(schedule[(s + i - 1) % schedule.length])) {
								consec += 24 * 60 - instance.getFromDutyNrToDuty()
										.get(schedule[(s + i - 1) % schedule.length]).getEndTime();
							} else {
								consec += 24 * 60 - instance.getFromRDutyNrToRDuty()
										.get(schedule[(s + i - 1) % schedule.length]).getEndTime();
							}

							// Day after, end of the period
							if (i == 13) {
								if (schedule[(s + 14) % schedule.length] == 1
										|| schedule[(s + 14) % schedule.length] == 2) {
									consec += start;
								} else if (instance.getFromDutyNrToDuty()
										.containsKey(schedule[(s + 14) % schedule.length])) {
									consec += Math.min(start, instance.getFromDutyNrToDuty()
											.get(schedule[(s + 14) % schedule.length]).getStartTime());
								} else {
									consec += Math.min(start, instance.getFromRDutyNrToRDuty()
											.get(schedule[(s + 14) % schedule.length]).getStartTime());
								}
							}
							// Day after, not the end of the period
							else {
								if (schedule[(s + i + 1) % schedule.length] == 1
										|| schedule[(s + i + 1) % schedule.length] == 2) {
									consec += 24 * 60;
								} else if (instance.getFromDutyNrToDuty()
										.containsKey(schedule[(s + i + 1) % schedule.length])) {
									consec += instance.getFromDutyNrToDuty()
											.get(schedule[(s + i + 1) % schedule.length]).getStartTime();
								} else {
									consec += instance.getFromRDutyNrToRDuty()
											.get(schedule[(s + i + 1) % schedule.length]).getStartTime();
								}
							}

							if (consec >= 32 * 60) {
								consec14 += consec;
							}
						}
					}
					if (consec14 >= 72 * 60) {
						rangeFeasible = true;
					}
				}
				if(!rangeFeasible) {
					return false;
				}
			}
		}
		return true;
	}
	
	public boolean restTimeFeasible(int[] scheduleArray, int current, int startTimeNewDuty, int endTimeNewDuty) {
		boolean feasibleWithPrevious = false;
		if(current == 0) {
			current = scheduleArray.length;
		}
		if (scheduleArray[(current - 1)%scheduleArray.length] == 1
				|| scheduleArray[(current - 1)%scheduleArray.length] == 2) {
			feasibleWithPrevious = true;
		} else {
			int endTimePrevious = 0;
			if (instance.getFromDutyNrToDuty()
					.containsKey(scheduleArray[(current - 1) % scheduleArray.length])) {
				endTimePrevious = instance.getFromDutyNrToDuty()
						.get(scheduleArray[(current - 1) % scheduleArray.length])
						.getEndTime();
			} else {
				endTimePrevious = instance.getFromRDutyNrToRDuty()
						.get(scheduleArray[(current - 1) % scheduleArray.length])
						.getEndTime();
			}
			if (startTimeNewDuty + (24 * 60 - endTimePrevious) >= 11 * 60) {
				feasibleWithPrevious = true;
			}
		}

		boolean feasibleWithNext = false;
		int startTimeNext = 0;
		if (scheduleArray[(current + 1) % scheduleArray.length] == 1
				|| scheduleArray[(current + 1) % scheduleArray.length] == 2) {
			feasibleWithNext = true;
		} else {
			if (instance.getFromDutyNrToDuty()
					.containsKey(scheduleArray[(current + 1) % scheduleArray.length])) {
				startTimeNext = instance.getFromDutyNrToDuty()
						.get(scheduleArray[(current + 1) % scheduleArray.length])
						.getEndTime();
			} else {
				startTimeNext = instance.getFromRDutyNrToRDuty()
						.get(scheduleArray[(current + 1) % scheduleArray.length])
						.getEndTime();
			}
			if (startTimeNext + (24 * 60 - endTimeNewDuty) >= 11 * 60) {
				feasibleWithNext = true;
			}
		}
		
		if(feasibleWithNext && feasibleWithPrevious) {
			return true;
		}
		else {
			return false;
		}
	}
}
