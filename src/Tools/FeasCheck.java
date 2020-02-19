package Tools;

public class FeasCheck {

	public Instance instance; 

	public FeasCheck(Instance instance) {
		this.instance = instance; 
	}

	/**
	 * Checks the constraint for the 7 * 24 hours rest 
	 * @param schedule 
	 * @param startDay including startDay starting from 0
	 * @param endDay including endDay 
	 * @return
	 */
	public boolean isFeasible7(int[] schedule, int startDay, int endDay) {

		if( endDay - startDay < 6) {
			throw new IllegalArgumentException("not more than 14 days between bounds"); 
		}

		//Over the whole array (make changes here if you want it to loop over less)
		for (int k = startDay; k < endDay; k++) {// s is the starting day
			int s = (k + schedule.length) % schedule.length; 
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
						if (schedule[(s + i) % schedule.length] == 1 || schedule[(s + i) % schedule.length] == 2) {
							int consecRest = 24 * 60;

							// The day before
							if (instance.getFromDutyNrToDuty()
									.containsKey(schedule[(s + i - 1) % schedule.length])) {
								consecRest += 24 * 60 - instance.getFromDutyNrToDuty()
										.get(schedule[(s + i - 1) % schedule.length]).getEndTime();
							} else {
							/*System.out.println("day before ");
							for(int z = 0; z < schedule.length; z++) {
							 System.out.print(schedule[z] + " ");
							}*/
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
									//System.out.println(schedule[(s + i + 1) % schedule.length]);
									consecRest += instance.getFromRDutyNrToRDuty()
											.get(schedule[(s + i + 1) % schedule.length]).getStartTime();
								}
							}
							if (consecRest >= 32 * 60) {
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

	/**
	 * Checks the feasibility of the complete schedule for the 14 * 24 hour constraint
	 * @param schedule
	 * @return true if feasible
	 */
	public boolean isFeasible14(int[] schedule) {
		return isFeasible14(schedule, 0, schedule.length); 
	}

	/**
	 * Checks the constraint of minimum 2 weekly rest, returns true is correct 
	 * 
	 * @param schedule
	 * @param startDay check including startDay, starting from 0 
	 * @param endDay check including endDay
	 * @return
	 */
	public boolean isFeasible14(int [] schedule, int startDay, int endDay) {


		if( endDay - startDay < 13) {
			throw new IllegalArgumentException("not more than 14 days between bounds"); 
		}

		for (int k = startDay ; k <endDay; k++) {
			int s = (k + schedule.length) % schedule.length; 
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

							if (consec >= instance.getMinWeekBreak()) {
								consec14 += consec;
							}
						}
					}
					if (consec14 >= instance.getMin2WeekBreak()) {
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

	/**
	 * Checks whether adding a new duty at the current day is feasible with the min break between duties constraint
	 * @param scheduleArray
	 * @param current
	 * @param startTimeNewDuty
	 * @param endTimeNewDuty
	 * @return
	 */
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
			if (startTimeNewDuty + (24 * 60 - endTimePrevious) >= instance.getMinBreak()) {
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
			if (startTimeNext + (24 * 60 - endTimeNewDuty) >= instance.getMinBreak()) {
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

	/**
	 * Checks if the overtime is feasible 
	 * @param schedule
	 * @param c
	 * @return
	 */
	public boolean overTimeFeasible(int[] schedule, ContractGroup c) {
		int totMinWorkedOverSchedule = 0;
		//For every week
		for (int i = 0; i < schedule.length/7; i++) {
			//For all the days in that week 
			for (int j = 0; j < 7; j++) {
				if (schedule[i * 7 + j] == 1 || instance.getFromRDutyNrToRDuty().containsKey(schedule[i * 7 + j])) {
					totMinWorkedOverSchedule += c.getAvgHoursPerDay() * 60;
				} else if (instance.getFromDutyNrToDuty().containsKey(schedule[i * 7 + j])) {
					totMinWorkedOverSchedule += instance.getFromDutyNrToDuty().get(schedule[i * 7 + j]).getPaidMin();
				}
			}
		}

		if(totMinWorkedOverSchedule > schedule.length/7 * c.getAvgDaysPerWeek() * c.getAvgHoursPerDay() * 60) {
			return false;
		}
		else {
			return true;
		}
	}

	/**
	 * Checks if there are enough ATV days for that contract group
	 * @param schedule
	 * @param c
	 * @return
	 */
	public boolean checkATVDays(int[] schedule, ContractGroup c) {
		int nATVdays = 0; 

		for(int i = 0; i<schedule.length; i++) {
			if(schedule[i] == 1) {
				nATVdays++; 
			}
		}

		if(nATVdays >= c.getATVc()) {
			return true; 
		}else {
			return false;
		}
	}
	//Need 7 days before and after without ATV day
	public double ATVspread(int[] schedule, int startDay, int endDay,  ContractGroup c) {
		double addCost = 0;
		int counter = 0;
		for(int k = startDay; k < endDay; k++) {
			if(schedule[(k+schedule.length)%schedule.length] == 1) {
				counter++;
			}
		}
		//System.out.println(counter);
		if(counter > 1) {
		 addCost = (counter-1)*100000;
		}
		return addCost;
	}
	
//get the total quarterly overtime 	
	public double QuarterlyOvertime(Solution sol) {
		double overtime = 0;
		for(ContractGroup group: sol.getNewSchedule().keySet()) {
			overtime += QuarterlyOvertime(sol.getNewSchedule().get(group).getScheduleArray(), group); 
		}

		return overtime;
	}
//get the quarterly overtime per contract group
	public double QuarterlyOvertime(int[] solution, ContractGroup c) {
		double overtime = 0;
		double[] weeklyOvertime = this.setWeeklyOvertime(solution, c);
		for(int empl = 0; empl < solution.length/7; empl++) {	
			for(int i =0; i < 13; i++) { //need to loop over 13 weeks for overtime
					int remainder = (empl + i) % solution.length/7;
					if(weeklyOvertime[remainder] > 0) {
						overtime = overtime + weeklyOvertime[remainder];		
					}
				}
		}
		return overtime;
	}
	//determine the weekly overtime in a schedule of a certain contract group
	public double[] setWeeklyOvertime(int[] schedule, ContractGroup c) {
		int sum = 0;
		double[] weeklyOvertime = new double[schedule.length/7];
		for(int  k = 0; k < (schedule.length/7); k++) {
			sum = 0;
			for(int i = 7*k; i < (7*k+6); i++) {
				if(instance.getFromDutyNrToDuty().containsKey(schedule[i])) {
					sum += instance.getFromDutyNrToDuty().get(schedule[i]).getPaidMin();
				}
				else if(instance.getFromRDutyNrToRDuty().containsKey(schedule[i])) {
					sum += c.getAvgHoursPerDay()*60;
				}
				else if(schedule[i] == 1) {
					sum += c.getAvgHoursPerDay()*60;
				}
			}

			weeklyOvertime[k] = sum - (c.getAvgDaysPerWeek()*c.getAvgHoursPerDay()*60) ;
		}
		return weeklyOvertime;
	}
}
