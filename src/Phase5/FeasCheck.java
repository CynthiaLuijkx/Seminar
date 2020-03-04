package Phase5;

import Tools.ContractGroup;
import Tools.Instance;

/**
 * This method contains the feasibility check for a method.
 *
 */
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

		//Over the whole array (make changes here if you want it to loop over less)
		for (int k = startDay; k < endDay; k++) {// s is the starting day
			int s = (k+schedule.length) % schedule.length; 
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
								if (instance.getFromDutyNrToDuty()
										.containsKey(schedule[(s + i + 1) % schedule.length])) {
									consec += instance.getFromDutyNrToDuty()
											.get(schedule[(s + i + 1) % schedule.length]).getStartTime();
								} else if (instance.getFromRDutyNrToRDuty().containsKey(schedule[(s+i+1)%schedule.length])) {
									consec += instance.getFromRDutyNrToRDuty()
											.get(schedule[(s + i + 1) % schedule.length]).getStartTime();
								} else {
									int j = 1;
									while (schedule[(s+i+j)%schedule.length] == 1 || schedule[(s+i+j)%schedule.length] == 2) {
										if (i + j == 14) {
											consec += start;
											break;
										}
										consec += 24 * 60;
										j++;
									}
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
		if(current == 0) {
			current = scheduleArray.length;
		}
		if (scheduleArray[(current - 1)%scheduleArray.length] != 1 && scheduleArray[(current - 1)%scheduleArray.length] != 2) {
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
			if (startTimeNewDuty + (24 * 60 - endTimePrevious) < instance.getMinBreak()) {
				return false;
			}
		}

		int startTimeNext = 0;
		if (scheduleArray[(current + 1)%scheduleArray.length] != 1 && scheduleArray[(current + 1)%scheduleArray.length] != 2) {
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
			if (startTimeNext + (24 * 60 - endTimeNewDuty) < instance.getMinBreak()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Checks if the overtime is feasible 
	 * @param schedule
	 * @param c
	 * @return
	 */
	public boolean overTimeFeasible(int[] schedule, ContractGroup c) {
		int totMinWorkedOverSchedule = 0;
		for (int i = 0; i < schedule.length; i++) {
			if (schedule[i] == 1 || instance.getFromRDutyNrToRDuty().containsKey(schedule[i])) {
				totMinWorkedOverSchedule += c.getAvgHoursPerDay() * 60;
			} else if (instance.getFromDutyNrToDuty().containsKey(schedule[i])) {
				totMinWorkedOverSchedule += instance.getFromDutyNrToDuty().get(schedule[i]).getPaidMin();
			}
		}

		if(totMinWorkedOverSchedule > schedule.length/7 * c.getAvgDaysPerWeek() * c.getAvgHoursPerDay() * 60) {
			return false;
		} else {
			return true;
		}
	}

	public boolean checkSundays(int[] schedule) {
		int count = 0;
		for (int i = 0; i < schedule.length; i+=7) {
			if (schedule[i] != 1 && schedule[i] != 2) {
				count++;
			}
		}

		if (count <= 0.75 * schedule.length/7) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks if there are enough ATV days for that contract group
	 * @param schedule
	 * @param c
	 * @return
	 */
	public int checkATVDays(int[] schedule, ContractGroup c) {
		int nATVdays = 0; 

		if ((int) Math.floor((schedule.length/7)/52.0 * c.getATVPerYear()) > 0) {
			for(int i = 0; i<schedule.length; i++) {
				if(schedule[i] == 1) {
					nATVdays++; 
				}
			}
			return (int) Math.floor((schedule.length/7)/52 * c.getATVPerYear() ) - nATVdays;
		} else {
			return 0;
		}
	}

	/**
	 * This method gets the total number of violations for every soft constraints.
	 * @param schedule
	 * @param c
	 * @return
	 */
	public int[] allViolations(int[] schedule, ContractGroup c) {
		int[] violations = new int[12]; //INCREASE IF YOU HAVE ALL VIOLATIONS;
		/*
		 * 0: ATV spread
		 * 1: no more than 2 reserve duties
		 * 2: max of 5 consecutive duties
		 * 3: no more than 5 duties + ATV days in a week
		 * 4: ATV days are not preferred in the weekends
		 * 5: if the contract group has part timers, gives back the number of not part time duties
		 * 6: do not want early followed by late duties
		 * 7: want ATV and rest after one another (check how often a rest/atv stands alone)
		 * 8: loose duties are not preferred
		 * 9: check at least 2 duties of the same type in a row
		 * 10: check that at most 3 duties of the same type are in a row
		 * 11: no more than 3 rest and ATV in a week
		 */

		for (int i = 0; i < schedule.length; i++) {
			if (schedule[i] == 1) {	// To avoid counting the same 'mistake' twice
				violations[0] += this.ATVspread(schedule, i - 7, i + 7, c);
			}
		}
		//		violations[0] = this.ATVspread(schedule, 0, schedule.length,c);
		violations[6] = this.checkEarlyFollowedByLate(schedule, 0, schedule.length);
		violations[7] = this.checkConsecutiveRestATV(schedule,0, schedule.length);
		violations[8] = this.checkLooseDuties(schedule, 0, schedule.length);
		int[] temp = this.checkSameDuties(schedule);
		violations[9] = temp[0]; 
		violations[10] = temp[1];
		violations[11] = this.check3RestATV(schedule);
		violations[2] = this.maxConsecutive(schedule); 
		for(int i = 0; i < schedule.length; i+=7) {
			violations[1] += this.reserveDuties(schedule, i, c);
			violations[3] += this.maxDuties(schedule, i, c);
		}
		int day = 0;
		while(day < schedule.length) {
			if(day%7 == 0) {
				violations[4]+= this.weekendATV(schedule, day,c);
				day += 6;
			}
			else if(day%7 == 6) {
				violations[4]+= this.weekendATV(schedule, day,c);
				day += 1;
			}
		}
		return violations;
	}

	/**
	 * This method gets the number of violations for a value i?????
	 * @param schedule
	 * @param c
	 * @param i
	 * @return
	 */
	public int[] allViolations(int[] schedule, ContractGroup c, int i) {
		int[] violations = new int[12]; //INCREASE IF YOU HAVE ALL VIOLATIONS;
		/*
		 * 0: ATV spread
		 * 1: no more than 2 reserve duties
		 * 2: max of 5 consecutive duties
		 * 3: no more than 5 duties + ATV days in a week
		 * 4: ATV days are not preferred in the weekends
		 * 5: if the contract group has part timers, gives back the number of not part time duties
		 * 6: do not want early followed by late duties
		 * 7: want ATV and rest after one another (check how often a rest/atv stands alone)
		 * 8: loose duties are not preferred
		 * 9: check at least 2 duties of the same type in a row
		 * 10: check that at most 3 duties of the same type are in a row
		 * 11: no more than 3 rest and ATV in a week
		 */

		violations[0] = this.ATVspread(schedule, i-7, i+7,c);
		violations[1] = this.reserveDuties(schedule, i, c);
		violations[2] = this.maxConsecutive(schedule, i, c);
		violations[3] = this.maxDuties(schedule, i, c);
		violations[4] = this.weekendATV(schedule, i,c);
		violations[6] = this.checkEarlyFollowedByLate(schedule, i-1, i+1);
		violations[7] = this.checkConsecutiveRestATV(schedule,i);
		violations[8] = this.checkLooseDuties(schedule, i);
		violations[9] = this.check2SameDuties(schedule,i);
		violations[10] = this.check3SameDuties(schedule,i);

		violations[11] = this.check3RestATV(schedule, i);
		return violations;
	}

	//Need 7 days before and after without ATV day
	public int ATVspread(int[] schedule, int startDay, int endDay, ContractGroup c) {
		int counter = 0;
		int violations = 0; //how many violations do we have
		for(int k = startDay; k < endDay; k++) {
			if(schedule[(k+schedule.length)%schedule.length] == 1) {
				counter++;
			}
		}
		//System.out.println(counter);
		if(counter -1 > 0) {
			violations  = counter -1;
		}
		return violations; //only want 1 ATV day every period of weeks
	}

	//No more than 2 reserve duties per week
	public int reserveDuties(int[] schedule, int index, ContractGroup c) {
		int remainder = index%7; //get on which day the request falls
		int counter = 0; //counter for the number of reserve duties in the week
		int violations  = 0;
		for(int i = index - remainder; i <=(index - remainder + 6); i++) { //check the whole week
			if(instance.getFromRDutyNrToRDuty().containsKey(schedule[i])) {
				counter++;
			}
		}
		if(counter -2 > 0) {
			violations = counter -2;
		}
		return violations;
	}

	/**
	 * This method checks the number of violations of 5 consecutive duties for the whole schedule of a contractgroup
	 * @param schedule 	Whole schedule of a contract group
	 * @return
	 */
	public int maxConsecutive(int[] schedule) {
		int counter = 0; 
		int nViolations = 0; 

		int j = 0; 
		while(true) {
			if(schedule[j] ==1|| schedule[j] == 2) {
				break; 
			}
			j++;
		}

		for(int i = 0; i<schedule.length; i++) {
			int k = (i+j) % schedule.length; 
			if(schedule[k]==1 || schedule[k] == 2) {
				counter = 0; 
			}else {
				counter++; 
			}
			if(counter>5) {
				nViolations++; 
			}
		}

		return nViolations; 
	}

	/**
	 * This method checks the number of violations of 5 consecutive duties around a duty on an index.
	 * @param schedule
	 * @param index
	 * @param c
	 * @return
	 */
	public int maxConsecutive(int[] schedule, int index, ContractGroup c) {
		// check nr of consecutive forward
		int counter = 1;
		if (schedule[index] != 1 && schedule[index]  != 2) {
			int loc = index;
			while (true) {
				loc = (loc + 1)%schedule.length;
				if (schedule[loc] != 1 && schedule[loc] != 2) {
					counter++;
				} else {
					break;
				}
			}
			loc = index;
			while (true) {
				loc = (loc - 1 + schedule.length)%schedule.length;
				if (schedule[loc] != 1 && schedule[loc] != 2) {
					counter++;
				} else {
					break;
				}
			}
		}

		return Math.max(0, counter - 5);

		//		int remainder = index%7; //get on which day the request falls
		//		int counter = 0; //counter for the number of reserve duties in the week
		//		int violations = 0; //number of violations in a week
		//		int save = 0;
		//		for(int i = index - remainder; i <=(index - remainder + 6); i++) { //check the whole week
		//			if(schedule[i] != 2 && schedule[i] != 1) {
		//				counter++;
		//				if(counter > 5) {
		//					save = 1;
		//				}
		//			}
		//			else {
		//				counter = 0;
		//			}
		//		}
		//		if(counter - 5 > 0) {
		//			violations = counter - 5;
		//		}
		//		else if(save == 1) {
		//			violations = 1;
		//		}
		//		return violations;
	}

	/**
	 * This method checks for the number of violations for no more than 5 duties or ATV days for the week in which an index falls.
	 * @param schedule
	 * @param index
	 * @param c
	 * @return
	 */
	public int maxDuties(int[] schedule, int index, ContractGroup c) {
		int remainder = index%7; //get on which day the request falls
		int counter = 0; //counter for the number of duties/ATV in the week
		int violations = 0; //number of violations in a week
		for(int i = index - remainder; i <=(index - remainder + 6); i++) { //check the whole week
			if(schedule[i] != 2) {
				counter++;
			}
		}
		if(counter - 5 > 0) {
			violations = counter - 5;
		}
		return violations;
	}

	/**
	 * This method checks for the number of ATV days on weekend days for the week in which an index falls.
	 * @param schedule
	 * @param index
	 * @param c
	 * @return
	 */
	public int weekendATV(int[] schedule, int index, ContractGroup c) {
		int remainder = index%7; //get on which day the request falls
		int violations = 0;
		if((remainder == 0 || remainder == 6) && schedule[index] == 1) {
			violations++;
		}
		return violations;
	}

	/**
	 * This method determines the quarterly overtime of a solution.
	 * @param sol
	 * @return
	 */
	public double QuarterlyOvertime(Solution sol) {
		double overtime = 0;
		for(ContractGroup group: sol.getNewSchedule().keySet()) {
			overtime += QuarterlyOvertime(sol.getNewSchedule().get(group).getScheduleArray(), group); 
		}

		return overtime;
	}

	/**
	 * This method determines the quarterly overtime of a schedule for contract group c.
	 * @param solution
	 * @param c
	 * @return
	 */
	public double QuarterlyOvertime(int[] solution, ContractGroup c) {
		double totOvertime = 0;

		double[] weeklyOvertime = this.setWeeklyOvertime(solution, c);
		for(int empl = 0; empl < solution.length/7; empl++) {
			double overtime = 0;
			for(int i = 0; i < 13; i++) { //need to loop over 13 weeks for overtime
				overtime += weeklyOvertime[(empl+i)%weeklyOvertime.length];
			}
			totOvertime += Math.max(0, overtime);
		}
		return totOvertime;
	}

	/**
	 * This method determines the quarterly minus hours of a schedule for contract group c.
	 * @param solution
	 * @param c
	 * @return
	 */
	public double QuarterlyMinus(int[] solution, ContractGroup c) {
		double totMinus = 0;

		double[] weeklyOvertime = this.setWeeklyOvertime(solution, c);
		for (int empl = 0; empl < solution.length/7; empl++) {
			double minus = 0;
			for (int i = 0; i < 13; i++) {
				minus += weeklyOvertime[(empl+i)%weeklyOvertime.length];
			}
			totMinus += Math.max(0, -minus);
		}

		return totMinus;
	}

	/**
	 * This method sets the weekly overtime of a schedule for contract group c.
	 * @param schedule
	 * @param c
	 * @return
	 */
	public double[] setWeeklyOvertime(int[] schedule, ContractGroup c) {
		double[] weeklyOvertime = new double[schedule.length/7];

		for(int  k = 0; k < (schedule.length/7); k++) {
			int sum = 0;
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

	/**
	 * Method calculates the penalty for part timers not getting a parttime duty 
	 * @param schedule
	 * @param c
	 * @return
	 */
	public double checkPartTime(int[] schedule, ContractGroup c, int startDate, int endDate ) {

		//If the group is not a parttime contractgroup, the check is not needed 
		if(c.getATVPerYear()!=0) {
			return 0.0;
		}

		int nPTduties = 0; 
		int nOduties = 0; 
		int nRduties = 0; 
		for(int i = startDate; i<endDate; i++) {
			int dutyNr = schedule[i]; 
			if(dutyNr>= 100 ) {
				if(dutyNr<1000) {
					nRduties++; 
				}
				nOduties ++; 
				//How to count reserve duties for parttimers 
				//				if(instance.getFromDutyNrToDuty().get(dutyNr).getType().equals("P")) {
				//					nPTduties++; 
				//				}else {
				//					nOduties++;
				//				}
			}
		}
		return nRduties; 

	}

	/**
	 * Methods checks the number of times an early duty is followed by a late duty
	 * @param schedule
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public int checkEarlyFollowedByLate(int[] schedule, int startDate, int endDate) {
		int nOccurances = 0;
		for (int i = startDate; i < endDate; i++) {
			if (schedule[(i+schedule.length)%schedule.length] != 1 && schedule[(i+schedule.length)%schedule.length] != 2 && schedule[((i+1)%schedule.length)%schedule.length] != 1 && schedule[((i+1)%schedule.length)%schedule.length] != 2) {
				String dutyType1 = this.instance.getDutyTypeFromDutyNR(schedule[(i+schedule.length)%schedule.length]); 
				String dutyType2 = this.instance.getDutyTypeFromDutyNR(schedule[((i+1)%schedule.length) %schedule.length]);

				if(dutyType1.length()== 2) {
					dutyType1 = dutyType1.substring(1); 
				}

				if(dutyType2.length()== 2) {
					dutyType2 = dutyType2.substring(1); 
				}

				if(dutyType1.equals("V") && dutyType2.equals("L")) {
					nOccurances++; 
				}
			}
		}
		return nOccurances;

		//		int nOccurances = 0; 
		//		for(int i = startDate; i<endDate; i++) {
		//			int dutyNr1 = schedule[i]; 
		//			int dutyNr2 = schedule[i]; 
		//			String dutyType1 = this.instance.getDutyTypeFromDutyNR(dutyNr1); 
		//			String dutyType2 = this.instance.getDutyTypeFromDutyNR(dutyNr2); 
		//
		//			if(dutyType1.length()== 2) {
		//				dutyType1 = dutyType1.substring(1); 
		//			}
		//
		//			if(dutyType2.length()== 2) {
		//				dutyType2 = dutyType2.substring(1); 
		//			}
		//
		//			if(dutyType1.equals("V") && dutyType2.equals("E")) {
		//				nOccurances++; 
		//			}
		//		}
		//		return nOccurances; 
	}

	/**
	 * Method checks the number of times a rest day is stand alone
	 * @param schedule
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public int checkConsecutiveRestATV(int[] schedule, int startDate, int endDate) {
		int nViolations = 0; 
		for(int i = startDate; i<endDate; i++) {
			String currentType = this.instance.getDutyTypeFromDutyNR(schedule[i]); 
			boolean checkRest = currentType.equals("Rest") || currentType.equals("ATV"); 

			if(checkRest) {
				String prevDay = this.instance.getDutyTypeFromDutyNR(schedule[(i - 1 + schedule.length)%schedule.length]); 
				String nextDay = this.instance.getDutyTypeFromDutyNR(schedule[(i +1 )%schedule.length]); 

				boolean restPrevDay = prevDay.equals("Rest") || prevDay.equals("ATV"); 
				boolean restNextDay = nextDay.equals("Rest") || nextDay.equals("ATV"); 
				if(!restPrevDay && ! restNextDay) {
					nViolations++; 
				}
			}
		}
		return nViolations; 
	}

	/**
	 * This method checks for the amount of consecutive rest of ATV days for a given index.
	 * @param schedule
	 * @param i
	 * @return
	 */
	public int checkConsecutiveRestATV(int[] schedule, int i) {
		String currentType = this.instance.getDutyTypeFromDutyNR(schedule[i]); 

		if(currentType.equals("ATV")) {
			String prevDay = this.instance.getDutyTypeFromDutyNR(schedule[(i - 1 + schedule.length)%schedule.length]); 
			String nextDay = this.instance.getDutyTypeFromDutyNR(schedule[(i + 1)%schedule.length]); 

			boolean restPrevDay = prevDay.equals("Rest") || prevDay.equals("ATV"); 
			boolean restNextDay = nextDay.equals("Rest") || nextDay.equals("ATV"); 
			if(!restPrevDay && ! restNextDay) {
				return 1; 
			}
		}else {
			String prevDay = this.instance.getDutyTypeFromDutyNR(schedule[(i - 1 + schedule.length)%schedule.length]); 
			String nextDay = this.instance.getDutyTypeFromDutyNR(schedule[(i + 1)%schedule.length]); 
			boolean restPrevDay = prevDay.equals("Rest") || prevDay.equals("ATV"); 
			boolean restNextDay = nextDay.equals("Rest") || nextDay.equals("ATV"); 

			if(restPrevDay) {
				String prev2Day = this.instance.getDutyTypeFromDutyNR(schedule[(i - 2 + schedule.length)%schedule.length]); 
				boolean restPrev2Day = prev2Day.equals("Rest") || prevDay.equals("ATV"); 
				if(!restPrev2Day) {
					return 1; 
				}
			}

			if(restNextDay) {
				String next2Day = this.instance.getDutyTypeFromDutyNR(schedule[(i + 2 + schedule.length)%schedule.length]); 
				boolean restNext2Day = next2Day.equals("Rest") || prevDay.equals("ATV"); 
				if(!restNext2Day) {
					return 1; 
				}
			}
		}
		return 0; 
	}

	/**
	 * Method checks the number of violations due to the consecutive same duties 
	 * @param schedule
	 * @param startDate
	 * @param endDate
	 * @return array of 		
	 *		index 0: number of times a duty is alone
	 *	  	index 1: number of times a duty is scheduled more than 3 times consecutively
	 */
	public int[] checkSameDuties(int[] schedule) {
		//Assume checking only early, day and late duties 
		int[] nViolations = new int[2]; 
		int j = 1;
		boolean checkBack  = true; 
		String currentType = this.instance.getDutyTypeFromDutyNR(schedule[0]); 
		boolean checkDuty = currentType.equals("V") || currentType.equals("D") || currentType.equals("L"); 
		//If it is a reserve duty get rid of the R 
		if(currentType.length()== 2){
			currentType = currentType.substring(1); 
		}

		while(checkBack && checkDuty) {
			String prevType = this.instance.getDutyTypeFromDutyNR(schedule[(- j + schedule.length) % schedule.length]); 
			//If it is a reserve duty get rid of the R 
			if(prevType.length() == 2) {
				prevType = prevType.substring(1); 
			}

			if(prevType.equals(currentType)) {
				j++; 
			}else {
				checkBack = false; 
			}
		}

		int count = 0; 
		if(j!=1) {
			count = j; 
		}

		for(int i = 0 ; i<=schedule.length - (j-1); i++){
			//Check if the duty need to be checked
			checkDuty = currentType.equals("V") || currentType.equals("D") || currentType.equals("L"); 

			if(checkDuty) {
				String nextType = this.instance.getDutyTypeFromDutyNR(schedule[(i+1) % schedule.length]); 

				//If it is a reserve duty get rid of the R 
				if(nextType.length() == 2) {
					nextType = nextType.substring(1); 
				}

				if(!nextType.equals(currentType)) {
					if(count < 2) {
						nViolations[0]++; 
					}
					if(count > 3) {
						nViolations[1]++; 
					}
					count = 1; 
					currentType = nextType; 
				}else {
					count++; 
				}
			} else {
				currentType = this.instance.getDutyTypeFromDutyNR(schedule[(i+1)%schedule.length]);
				if (currentType.length() == 2) {
					currentType = currentType.substring(1);
				}
			}
		}
		return nViolations; 
	}

	/**
	 * Checks the number of times a duty is loose (between two rest/ATV days
	 * @param schedule
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public int checkLooseDuties(int[] schedule, int startDate, int endDate) {
		int nViolations = 0; 
		for(int i = startDate; i<endDate; i++) {
			nViolations+= checkLooseDuties(schedule, i); 
		}
		return nViolations; 
	}

	/**
	 * This method checks for loose duties for index i.
	 * @param schedule
	 * @param i
	 * @return
	 */
	public int checkLooseDuties(int[]schedule, int i) {
		String currentType = this.instance.getDutyTypeFromDutyNR(schedule[i]); 

		boolean checkRest = !currentType.equals("Rest") && !currentType.equals("ATV"); 

		if(checkRest) {
			String prevDay = this.instance.getDutyTypeFromDutyNR(schedule[(i - 1 + schedule.length)%schedule.length]); 
			String nextDay = this.instance.getDutyTypeFromDutyNR(schedule[(i +1 )%schedule.length]); 

			boolean restPrevDay = prevDay.equals("Rest") || prevDay.equals("ATV"); 
			boolean restNextDay = nextDay.equals("Rest") || nextDay.equals("ATV"); 
			if(restPrevDay && restNextDay) {
				return 1; 
			}
		}
		return 0; 
	}

	/**
	 * Feasibility check to check the average number of split duties
	 * @param schedule
	 * @return
	 */
	public boolean checkMax2SplitDuties(int[] schedule) {
		int count = 0; //amount of split duties in whole schedule
		for(int i = 0; i<schedule.length; i++) {
			String dutyType = this.instance.getDutyTypeFromDutyNR(schedule[i]); 

			if(dutyType.equals("G") || dutyType.equals("GM")) {
				count++; 
			}
		}
		return count/(schedule.length/7) <= 2; 
	}

	/**
	 * This method checks for at least 2 the same duties consecutively.
	 * @param schedule
	 * @param i
	 * @return
	 */
	public int check2SameDuties(int[] schedule, int i) {

		String currentType = this.instance.getDutyTypeFromDutyNR(schedule[i]); 
		//If it is a reserve duty get rid of the R 
		if(currentType.length()== 2){
			currentType = currentType.substring(1); 
		}

		//Check if the duty need to be checked
		boolean checkDuty = currentType.equals("V") || currentType.equals("D") || currentType.equals("L"); 

		if(checkDuty) {
			String prevDay = this.instance.getDutyTypeFromDutyNR(schedule[(i - 1 + schedule.length)%schedule.length]);  
			prevDay = prevDay.substring(prevDay.length() - 1); 
			String nextDay = this.instance.getDutyTypeFromDutyNR(schedule[(i +1 )%schedule.length]); 
			nextDay = nextDay.substring(nextDay.length() - 1); 

			boolean restPrevDay = prevDay.equals(currentType); 
			boolean restNextDay = nextDay.equals(currentType); 
			if(!restPrevDay && ! restNextDay) {
				return 1; 
			}
		}
		return 0; 
	}

	/**
	 * This method checks for at most 3 the same duties consecutively.
	 * @param schedule
	 * @param i
	 * @return
	 */
	public int check3SameDuties(int[] schedule, int i) {
		String currentType = this.instance.getDutyTypeFromDutyNR(schedule[i]); 
		//If it is a reserve duty get rid of the R 
		if(currentType.length()== 2){
			currentType = currentType.substring(1); 
		}

		//Check if the duty need to be checked
		boolean checkDuty = currentType.equals("V") || currentType.equals("D") || currentType.equals("L"); 
		if(checkDuty) {
			boolean sameLeft = false; 
			int iLeft = 1; 
			while(sameLeft) {
				String prevDay = this.instance.getDutyTypeFromDutyNR(schedule[(i - iLeft + schedule.length)%schedule.length]);
				prevDay = prevDay.substring(prevDay.length() - 1); 
				if(prevDay.equals(currentType)) {
					iLeft++; 
				}else {
					sameLeft = true; 
				}
			}

			boolean sameRight = false; 
			int iRight = 1; 
			while(sameRight) {
				String nextDay = this.instance.getDutyTypeFromDutyNR(schedule[(i + iRight + schedule.length)%schedule.length]);
				nextDay = nextDay.substring(nextDay.length() - 1); 
				if(nextDay.equals(currentType)) {
					iRight++; 
				}else {
					sameRight = true; 
				}
			}

			int nConsecutiveDays = iLeft + iRight -1; 
			if(nConsecutiveDays >3) {
				return  nConsecutiveDays -3; 
			}else {
				return 0; 
			}
		}
		return 0; 
	}
	
	public int check3RestATV(int[] schedule) {
		int count = 0;
		
		for (int i = 0; i < schedule.length/7; i++) {
			int tempCount = 0;
			for (int j = 0; j < 7; j++) {
				if (schedule[7*i+j] == 1 || schedule[7*i+j] == 2) {
					tempCount++;
				}
			}
			if (tempCount > 3) {
				count++;
			}
		}
		
		return count;
	}
	
	public int check3RestATV(int[] schedule, int i) {
		int weekNr = i/7;
		int count = 0;
		for (int j = 0; j < 7; j++) {
			if (schedule[7*weekNr + j] == 1 || schedule[7*weekNr +j] == 2) {
				count++;
			}
		}
		if (count > 3) {
			return 1;
		} else {
			return 0;
		}
	}

	public double[] getAllFairness(int[] schedule, ContractGroup c) {
		double[] fairCount = new double[instance.getPenalties().getFairPenalties().length]; 

		/*
		 * 0:	ReserveDuties Distribution 
		 * 1:	Working Sundays Distribution 
		 * 2: 	Desirability Distribution 
		 * 3: 	Distribution split duties 
		 * 4: 	Distribution attractiveness
		 * 5:	Distribution Early duties
		 * 6:	Distribution Late duties
		 */

		fairCount[0] = this.getNReserveDuties(schedule); 
		fairCount[1] = this.getSundayProp(schedule); 
		fairCount[2] = this.getDesirability(schedule); 
		fairCount[3] = this.getPropDuty(schedule,"G"); 
		fairCount[4] = this.getAttractiveness(schedule, c);
		fairCount[5] = this.getPropDuty(schedule, "V");
		fairCount[6] = this.getPropDuty(schedule, "L");

		return fairCount; 

	}

	public double[][] getAllFairness(Solution solution){
		double[][] result = new double[instance.getPenalties().getFairPenalties().length][solution.getNewSchedule().keySet().size()]; 

		for(ContractGroup group :solution.getNewSchedule().keySet()) {
			double[] temp = getAllFairness(solution.getNewSchedule().get(group).getScheduleArray(), group); 
			for(int i = 0 ; i<temp.length; i++) {
				result[i][group.getNr()-1]  = temp[i]; 
			}
		}
		return result;
	}

	public double getSunDist(Solution solution) {
		double[] nSunDriver = new double[solution.getNewSchedule().keySet().size()]; 
		int i = 0; 
		for(ContractGroup group: solution.getNewSchedule().keySet()) {
			int[] schedule = solution.getNewSchedule().get(group).getScheduleArray(); 
			nSunDriver[i] = getNReserveDuties(schedule) / (double) schedule.length/7; 
			i++; 
		}

		return getCoefVariance(nSunDriver); 
	}

	public double getSundayProp(int[] schedule) {
		int count = 0; 

		for(int i = 0 ; i<schedule.length; i+= 7 ) {
			if(schedule[i] != 2) {
				count++; 
			}
		}

		return count / (double) (schedule.length/7); 
	}

	public double getResDutDist(Solution solution) {

		double[] nResDutPDriver = new double[solution.getNewSchedule().keySet().size()]; 
		int i = 0; 
		for(ContractGroup group: solution.getNewSchedule().keySet()) {
			int[] schedule = solution.getNewSchedule().get(group).getScheduleArray(); 
			nResDutPDriver[i] = getNReserveDuties(schedule); 
			i++; 
		}

		return getCoefVariance(nResDutPDriver); 
	}

	public double getNReserveDuties(int[] schedule) {
		int counter=0; 

		for(int i = 0; i<schedule.length; i++) {
			if(schedule[i]> 2 && schedule[i] <1000) {
				counter++; 
			}
		}

		return counter / (double) (schedule.length/7); 
	}

	public double getDesirDist(Solution solution) {
		double [] desirPDriver = new double[solution.getNewSchedule().keySet().size()]; 
		int i = 0; 
		for(ContractGroup group: solution.getNewSchedule().keySet()) {
			int[] schedule = solution.getNewSchedule().get(group).getScheduleArray(); 
			desirPDriver[i] = getDesirability(schedule) ; 
			i++; 
		}

		return getCoefVariance(desirPDriver); 
	}

	public double getDesirability(int[] schedule) {
		double sum  = 0; 
		for(int i =0; i<schedule.length; i++) {
			sum+= this.instance.getDesirability(schedule[i], i/7); 
		}
		return sum / (double) (schedule.length/7); 
	}

	public double getCoefVariance(double[] array) {
		double sumSquared = 0; 
		double sum = 0; 
		double count = array.length; 

		for(int i = 0; i< array.length; i++) {
			sumSquared += array[i] * array[i]; 
			sum += array[i]; 
		}

		double variance = (sumSquared - (double) (sum*sum / count))/ (count - 1); 
		double std = Math.sqrt(variance); 
		double mean = sum/count; 
		return std/mean; 
	}

	public double getAttractiveness(int[] schedule, ContractGroup c){

		double result = 0; 
		int[] violations = this.allViolations(schedule, c); 
		double[] penalties = instance.getPenalties().getSoftPenalties();
		for(int i = 0 ; i< violations.length; i++) {
			result+= violations[i] * penalties[i]; 
		}

		return result/ (double) schedule.length; 
	}
	
	public double getPropDuty(int[] schedule, String dutyType) {
		int count = 0; 
		
		for(int i = 0; i<schedule.length; i++) {
			if(this.instance.getSimpleDutyTypeFromDutyNR(schedule[i]).equals(dutyType)) {
				count++; 
			}
		}
		return count / (double) schedule.length; 
	}
}
