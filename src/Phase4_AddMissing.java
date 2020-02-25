import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Tools.*;

public class Phase4_AddMissing {
	private final List<Schedule> ilpSolution;
	private final Instance instance;
	private final ArrayList<List<Duplicate>> originalDuplicates;
	private final ArrayList<Map<Duty, Set<Duplicate>>> duplicatesAvailable;
	private final ArrayList<List<Duty>> originalMissing;
	private final List<Schedule> newSchedules;
	
	public List<Schedule> getNewSchedules() {
		return newSchedules;
	}

	public Phase4_AddMissing(List<Schedule> schedules, Instance instance) {
		this.ilpSolution = schedules ;
		this.instance = instance;
		this.duplicatesAvailable = new ArrayList<>();
		for(int i = 0; i < 7; i++) {
			Map<Duty, Set<Duplicate>> map = new HashMap<>();
			duplicatesAvailable.add(map);
		}
		this.originalDuplicates = determineDuplicates(ilpSolution);
		this.originalMissing = determineMissingDuties(ilpSolution);
		
		this.newSchedules = this.swapDuplicatesWithMissing();
		for(Schedule schedule : newSchedules) {
			System.out.println(schedule.toString());
		}
	}
	
	public ArrayList<List<Duplicate>> determineDuplicates(List<Schedule> schedules) {
		ArrayList<List<Duplicate>> duplicates = new ArrayList<>();
		int counter = 0;
		for(int i = 0; i < 7; i++) {
			List<Duplicate> duties = new ArrayList<>();
			duplicates.add(duties);
		}
		
		for (int s = 0; s < 7; s++) {//For every weekday
			if(s == 0) {
				for(Duty duty : instance.getSunday()) {
					Set<Duplicate> duplicateThisDuty = new HashSet<>();
					for(Schedule schedule :  schedules) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[(7*w) + s] == duty.getNr()) {
								Duplicate newDuplicate = new Duplicate(duty, schedule, ((7*w) + s));
								duplicateThisDuty.add(newDuplicate);
							}
						}
					}
					if(duplicateThisDuty.size() > 1) {
						duplicates.get(s).addAll(duplicateThisDuty);
						duplicatesAvailable.get(s).put(duty, duplicateThisDuty);
						counter = counter + (duplicateThisDuty.size()-1);
					}
				}
			}
			else if(s == 6) {
				for(Duty duty : instance.getSaturday()) {
					Set<Duplicate> duplicateThisDuty = new HashSet<>();
					for(Schedule schedule :  schedules) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[(7*w) + s] == duty.getNr()) {
								Duplicate newDuplicate = new Duplicate(duty, schedule, ((7*w) + s));
								duplicateThisDuty.add(newDuplicate);
							}
						}
					}
					if(duplicateThisDuty.size() > 1) {
						duplicates.get(s).addAll(duplicateThisDuty);
						duplicatesAvailable.get(s).put(duty, duplicateThisDuty);
						counter = counter + (duplicateThisDuty.size()-1);
					}
				}
			}
			else {
				for(Duty duty : instance.getWorkingDays()) {
					Set<Duplicate> duplicateThisDuty = new HashSet<>();
					for(Schedule schedule :  schedules) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[(7*w) + s] == duty.getNr()) {
								Duplicate newDuplicate = new Duplicate(duty, schedule, ((7*w) + s));
								duplicateThisDuty.add(newDuplicate);
							}
						}
					}
					if(duplicateThisDuty.size() > 1) {
						duplicates.get(s).addAll(duplicateThisDuty);
						duplicatesAvailable.get(s).put(duty, duplicateThisDuty);
						counter = counter + (duplicateThisDuty.size()-1);
					}
				}
			}
		}
		System.out.println("Number of duplicates: " + counter);
		return duplicates;
	}
	
	public ArrayList<List<Duplicate>> createDuplicateCopy(){
		ArrayList<List<Duplicate>> copy = new ArrayList<>();
		for(int i = 0; i <= 6; i++) {
			List<Duplicate> duties = new ArrayList<>();
			for(Duplicate duty : this.originalDuplicates.get(i)) {
				duties.add(duty);
			}
			copy.add(duties);
		}
		
		return copy;
	}
	
	public List<Schedule> createSchedulesCopies(){
		List<Schedule> copies = new ArrayList<>();
		for(Schedule schedule : ilpSolution) {
			Schedule copy = schedule.copy();
			copies.add(copy);
		}
		return copies; 
	}

	public List<Schedule> swapDuplicatesWithMissing() {
		List<Schedule> bestSchedules = new ArrayList<>();
		int minimumMissed = missingDuties(ilpSolution);

		for (int n = 0; n < 100; n++) {// Trials
			ArrayList<List<Duplicate>> duplicatesCopy = this.createDuplicateCopy();
			List<Schedule> schedulesCopy = this.createSchedulesCopies();
			ArrayList<Map<Duty, Set<Duplicate>>> duplicatesTaken = new ArrayList<>();
			ArrayList<Set<Duty>> insertedDuties = new ArrayList<>();
			for (int i = 0; i < 7; i++) {
				Set<Duty> dutiesInserted = new HashSet<>();
				insertedDuties.add(dutiesInserted);
				
				Map<Duty, Set<Duplicate>> map = new HashMap<>();
				duplicatesTaken.add(map);
				for (Duty duty : duplicatesAvailable.get(i).keySet()) {
					Set<Duplicate> taken = new HashSet<>();
					duplicatesTaken.get(i).put(duty, taken);
				}
			}

			int tries = 0;
			int totalMissing = missingDuties(schedulesCopy);
			int currentMissing = totalMissing;
			while (currentMissing > 0 && tries < 10000) {
				int i = (int) (Math.random() * 7); // Pick a random weekday
				if (originalMissing.get(i).size() > 0) {
					//Pick a random missing duty 
					int insertNr = (int) (Math.random() * originalMissing.get(i).size());
					Duty toInsert = originalMissing.get(i).get(insertNr);
					// If it hasn't been inserted yet
					if (!insertedDuties.get(i).contains(toInsert)) {
						// Get a random duplicate
						int deleteNr = (int) (Math.random() * duplicatesCopy.get(i).size());
						Duplicate toDelete = duplicatesCopy.get(i).get(deleteNr);
						// If this duplicate hasn't been taken yet
						// AND the duty is still included at least once
						if (!duplicatesTaken.get(i).get(toDelete.getDuty()).contains(toDelete)
								&& duplicatesTaken.get(i).get(toDelete.getDuty()).size() < duplicatesAvailable.get(i).get(toDelete.getDuty()).size()) {
							//Get the corresponding schedulesCopy schedule
							int[] scheduleArray = null;
							for(Schedule schedule : schedulesCopy) {
								if(schedule.getC() == toDelete.getSchedule().getC()) {
									scheduleArray = schedule.getSchedule();
								}
							}
							int day = toDelete.getDay();

							if (scheduleArray[day] == toDelete.getDuty().getNr()) {
								//If it's feasible wrt resttime
								if (restTimeFeasible(scheduleArray, day, toInsert.getStartTime(),
										toInsert.getEndTime())) {
									scheduleArray[day] = toInsert.getNr();
									//If the schedule is feasible 
									if (scheduleIsFeasible(scheduleArray, toDelete.getSchedule().getC())) {
										duplicatesTaken.get(i).get(toDelete.getDuty()).add(toDelete);
										insertedDuties.get(i).add(toInsert);
										currentMissing = missingDuties(schedulesCopy);
										if(duplicatesTaken.get(i).get(toDelete.getDuty()).size() == duplicatesAvailable.get(i).get(toDelete.getDuty()).size()-1) {
											duplicatesCopy.get(i).removeAll(duplicatesAvailable.get(i).get(toDelete.getDuty()));
										}
									} else {
										scheduleArray[day] = toDelete.getDuty().getNr();
									}
								}
							}
							
						}
						
					}
					tries++;
				}
			}
			if (currentMissing < minimumMissed) {
				minimumMissed = currentMissing;
				bestSchedules.clear();
				bestSchedules.addAll(schedulesCopy);
			}
			if (minimumMissed == 0) {
				break;
			}
		}
		System.out.println("Final missed duties: " + minimumMissed);
		return bestSchedules;
	}

	public boolean scheduleIsFeasible(int[] schedule, ContractGroup c) {
		// 7 feasibility
		if(!isFeasible7(schedule)) {
		//	System.out.println("7 check failed");
			return false;
		}
		if(!isFeasible14(schedule)) {
		//	System.out.println("14 check failed");
			return false;
		}
		if(!overTimeFeasible(schedule,c)) {
		//	System.out.println("Overtime failed");
			return false;
		}
		return true;
	}
	
	public boolean isFeasible7(int[] schedule) {
		//Over the whole array (make changes here if you want it to loop over less)
		for (int s = 0; s <= schedule.length - 1; s++) {// s is the starting day
			
			// Don't have to check if it's a rest/ATV day
			if (schedule[s] != 1 && schedule[s] != 2) {
				boolean rangeFeasible = false; //My addition, rest is from the method found in the pricing problem
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
						if (schedule[(s+i)%schedule.length] == 1 || schedule[(s+i)%schedule.length] == 2) {
							int consec = 24 * 60;
							
							//Check the day before 
							if (instance.getFromDutyNrToDuty().containsKey(schedule[(s+i-1)%schedule.length])) {//Normal duty
								consec += 24 * 60 - instance.getFromDutyNrToDuty().get(schedule[(s+i-1)%schedule.length]).getEndTime();
							} else {//Reserve duty
								consec += 24 * 60 - instance.getFromRDutyNrToRDuty().get(schedule[(s+i-1)%schedule.length]).getEndTime();
							}
							
							//The day after
							
							//If it's the last day 
							if (i == 6) {
								//We only count up until the start of the previous duty, or the new duty if it starts earlier 
								if (schedule[(s+7)%schedule.length] == 1 || schedule[(s+7)%schedule.length] == 2) {
									consec += start;
								} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(s+7)%schedule.length])) {
									consec += Math.min(start, instance.getFromDutyNrToDuty().get(schedule[(s+7)%schedule.length]).getStartTime());
								} else {
									consec += Math.min(start, instance.getFromRDutyNrToRDuty().get(schedule[(s+7)%schedule.length]).getStartTime());
								}
							} 
							//If it's any other day 
							else {
								if (schedule[(s+i+1)%schedule.length] == 1 || schedule[(s+i+1)%schedule.length] == 2) {
									consec += 24 * 60;
								} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(s+i+1)%schedule.length])) {
									consec += instance.getFromDutyNrToDuty().get(schedule[(s+i+1)%schedule.length]).getStartTime();
								} else {
									consec += instance.getFromRDutyNrToRDuty().get(schedule[(s+i+1)%schedule.length]).getStartTime();
								}
							}
							if (consec >= 32 * 60) {
								rangeFeasible = true;
							}
						}
					}
				}
				if(!rangeFeasible) {
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
								if (instance.getFromDutyNrToDuty().containsKey(schedule[(s+i+1)%schedule.length])) {
									consec += instance.getFromDutyNrToDuty().get(schedule[(s+i+1)%schedule.length]).getStartTime();
								} else if (instance.getFromRDutyNrToRDuty().containsKey(schedule[(s+i+1)%schedule.length])) {
									consec += instance.getFromRDutyNrToRDuty().get(schedule[(s+i+1)%schedule.length]).getStartTime();
								} else {
									int j = 1;
									while (schedule[(s+i+j)%schedule.length] == 1 || schedule[(s+i+j)%schedule.length] == 2) {
										if (i+j == 14) {
											consec += start;
											break;
										}
										consec += 24 * 60;
										j++;
									}
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
						.getStartTime();
			} else {
				startTimeNext = instance.getFromRDutyNrToRDuty()
						.get(scheduleArray[(current + 1) % scheduleArray.length])
						.getStartTime();
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

	public boolean overTimeFeasible(int[] schedule, ContractGroup c) {
		int totMinWorkedOverSchedule = 0;
		//For every week
		for (int i = 0; i < schedule.length; i++) {
			//For all the days in that week 
				if (schedule[i] == 1 || instance.getFromRDutyNrToRDuty().containsKey(schedule[i])) {
					totMinWorkedOverSchedule += c.getAvgHoursPerDay() * 60;
				} else if (instance.getFromDutyNrToDuty().containsKey(schedule[i])) {
					totMinWorkedOverSchedule += instance.getFromDutyNrToDuty().get(schedule[i]).getPaidMin();
				}
		}
		
		if(totMinWorkedOverSchedule > (schedule.length/7 * c.getAvgDaysPerWeek() * c.getAvgHoursPerDay() * 60)) {
			//System.out.println(totMinWorkedOverSchedule + ">" + schedule.length/7 * c.getAvgDaysPerWeek() * c.getAvgHoursPerDay() * 60);
			return false;
		}
		else {
			return true;
		}
	}

	
	public int missingDuties(List<Schedule> schedules) {
		int counter = 0;
		for (int s = 0; s < 7; s++) {
			if(s == 0) {
				for(Duty duty : instance.getSunday()) {
					int included = 0;
					for(Schedule schedule : schedules) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[(7*w) + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if (included < 1) {
						counter++;
					}
				}
			}
			else if(s == 6) {
				for(Duty duty : instance.getSaturday()) {
					int included = 0;
					for(Schedule schedule : schedules) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[(7*w) + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included < 1) {
						counter++;
					}
				}
			}
			else {
				for(Duty duty : instance.getWorkingDays()) {
					int included = 0;
					for(Schedule schedule : schedules) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							//System.out.println(duty.getNr() + " " +  schedule.getSchedule()[(7*w) + s]);
							if(schedule.getSchedule()[(7*w)+ s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included < 1) {
						counter++;
					}
				}
			}
		}
		return counter;
	}
	
	public ArrayList<List<Duty>> determineMissingDuties(List<Schedule> schedules) {
		ArrayList<List<Duty>> missingDuties = new ArrayList<>();
		int counter = 0; 
		for(int i = 0; i < 7; i++) {
			List<Duty> duties = new ArrayList<>();
			missingDuties.add(duties);
		}
		for (int s = 0; s < 7; s++) {
			if(s == 0) {
				for(Duty duty : instance.getSunday()) {
					int included = 0;
					for(Schedule schedule : schedules) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[(7*w) + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included < 1) {
						counter++;
						missingDuties.get(s).add(duty);
					}
				}
			}
			else if(s == 6) {
				for(Duty duty : instance.getSaturday()) {
					int included = 0;
					for(Schedule schedule : schedules) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[(7*w) + s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included < 1) {
						counter++;
						missingDuties.get(s).add(duty);
					}
				}
			}
			else {
				for(Duty duty : instance.getWorkingDays()) {
					int included = 0;
					for(Schedule schedule : schedules) {
						for(int w = 0; w < schedule.getSchedule().length/7; w++) {
							if(schedule.getSchedule()[(7*w)+ s] == duty.getNr()) {
								included++;
							}
						}
					}
					if(included < 1) {
						counter++;
						missingDuties.get(s).add(duty);
					}
				}
			}
		}
		System.out.println("Missing duties: " + counter);
		return missingDuties;
	}
	
	public void countVDutiesWorkingday(List<Schedule> schedules) {
		for(int s = 0; s <= 6; s++) {
			int vDuties = 0; 
			int lDuties = 0;
			int dDuties = 0;
			int gDuties = 0;
			for(Schedule schedule : schedules) {
				for(int w = 0; w < schedule.getSchedule().length/7; w++) {
					if(instance.getFromDutyNrToDuty().containsKey(schedule.getSchedule()[(7*w)+ s])) {
						if(instance.getFromDutyNrToDuty().get(schedule.getSchedule()[(7*w)+ s]).getType().equals("V")) {
							vDuties++;
						}
						if(instance.getFromDutyNrToDuty().get(schedule.getSchedule()[(7*w)+ s]).getType().equals("L")) {
							lDuties++;
						}
						if(instance.getFromDutyNrToDuty().get(schedule.getSchedule()[(7*w)+ s]).getType().equals("D")) {
							dDuties++;
						}
						if(instance.getFromDutyNrToDuty().get(schedule.getSchedule()[(7*w)+ s]).getType().equals("G")) {
							gDuties++;
						}
					}
				}
			}
			System.out.println(s + " " + vDuties + " " + lDuties + " " + dDuties + " " + gDuties);
		}
	}
}
	
	



