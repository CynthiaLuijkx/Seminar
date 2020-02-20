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
	//method that gets the total number of violations for every soft constraint
	public int[] allViolations(int[] schedule, ContractGroup c) {
		int[] violations = new int[11]; //INCREASE IF YOU HAVE ALL VIOLATIONS;
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
		 * 10 check that at most 3 duties of the same type are in a row
		 */
		
		violations[0] = this.ATVspread(schedule, 0, schedule.length,c);
		violations[6] = this.checkEarlyFollowedByLate(schedule, 0, schedule.length);
		violations[7] = this.checkConsecutiveRestATV(schedule,0, schedule.length);
		violations[8] = this.checkLooseDuties(schedule, 0, schedule.length);
		int[] temp = this.checkSameDuties(schedule, 0, schedule.length);
		violations[9] = temp[0]; 
		violations[10] = temp[1];
		for(int i = 0; i < schedule.length; i+=7) {
			violations[1] += this.reserveDuties(schedule, i, c);
			violations[2] += this.maxConsecutive(schedule, i, c);
			violations[3] += this.maxDuties(schedule, i, c);
		}
		int day = 0;
		while(day < schedule.length) {
			if(day%7 == 0) {
				this.weekendATV(schedule, day,c);
				day += 6;
			}
			else if(day%7 == 6) {
				this.weekendATV(schedule, day,c);
				day += 1;
			}
		}
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
	//No more than 5 consecutive duties per week
	public int maxConsecutive(int[] schedule, int index, ContractGroup c) {
		int remainder = index%7; //get on which day the request falls
		int counter = 0; //counter for the number of reserve duties in the week
		int violations = 0; //number of violations in a week
		int save = 0;
		for(int i = index - remainder; i <=(index - remainder + 6); i++) { //check the whole week
			if(schedule[i] != 2 && schedule[i] != 1) {
				counter++;
				if(counter > 5) {
					save = 1;
				}
			}
			else {
				counter = 0;
			}
		}
		if(counter - 5 > 0) {
			violations = counter - 5;
		}
		else if(save == 1) {
			violations = 1;
		}
		return violations;
	}
	//no more than 5 duties or ATV days in a week
	public int maxDuties(int[] schedule, int index, ContractGroup c) {
		int remainder = index%7; //get on which day the request falls
		int counter = 0; //counter for the number of reserve duties in the week
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
	
	//ATV days preferred not to be in the weekend
	public int weekendATV(int[] schedule, int index, ContractGroup c) {
		int remainder = index%7; //get on which day the request falls
		int violations = 0;
		if((remainder == 0 || remainder == 6) && schedule[index] == 1) {
				violations++;
			}
		return violations;
	}
	/**
	 * Method calculates the penalty for parttimers not getting a parttime duty 
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
		for(int i = startDate; i<endDate; i++) {
			int dutyNr = schedule[i]; 
			if(dutyNr>100) {
				//How to count reserve duties for parttimers 
				if(instance.getFromDutyNrToDuty().get(dutyNr).getType().equals("P")) {
					nPTduties++; 
				}else {
					nOduties++;
				}
			}
		}

		return nOduties; 

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
		for(int i = startDate; i<endDate; i++) {
			int dutyNr1 = schedule[i]; 
			int dutyNr2 = schedule[(i+1) % schedule.length]; 
			String dutyType1 = this.instance.getDutyTypeFromDutyNR(dutyNr1); 
			String dutyType2 = this.instance.getDutyTypeFromDutyNR(dutyNr2); 

			if(dutyType1.length()== 2) {
				dutyType1 = dutyType1.substring(1); 
			}

			if(dutyType2.length()== 2) {
				dutyType2 = dutyType2.substring(1); 
			}

			if(dutyType1.equals("V") && dutyType2.equals("E")) {
				nOccurances++; 
			}
		}
		return nOccurances; 
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
	 * Method checks the number of violations due to the consecutive same duties 
	 * @param schedule
	 * @param startDate
	 * @param endDate
	 * @return array of 		
	 *		index 0: number of times a duty is alone
	 *	  	index 1: number of times a duty is scheduled more than 3 times consecutively
	 */
	public int[] checkSameDuties(int[] schedule, int startDate, int endDate) {
		//Assume checking only early, day and late duties 
		int[] nViolations = new int[2]; 

		int count = 1; 
		for(int i = startDate + 1; i<endDate; i++){

			String currentType = this.instance.getDutyTypeFromDutyNR(schedule[i]); 

			//If it is a reserve duty get rid of the R 
			if(currentType.length()== 2){
				currentType = currentType.substring(1); 
			}

			//Check if the duty need to be checked
			boolean checkDuty = currentType.equals("V") || currentType.equals("D") || currentType.equals("L"); 

			if(checkDuty) {
				String nextType = this.instance.getDutyTypeFromDutyNR(schedule[(i+1)%schedule.length]); 

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
				}else {
					count++; 
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

			String currentType = this.instance.getDutyTypeFromDutyNR(schedule[i]); 

			boolean checkRest = !currentType.equals("Rest") && !currentType.equals("ATV"); 

			if(checkRest) {
				String prevDay = this.instance.getDutyTypeFromDutyNR(schedule[(i - 1 + schedule.length)%schedule.length]); 
				String nextDay = this.instance.getDutyTypeFromDutyNR(schedule[(i +1 )%schedule.length]); 

				boolean restPrevDay = prevDay.equals("Rest") || prevDay.equals("ATV"); 
				boolean restNextDay = nextDay.equals("Rest") || nextDay.equals("ATV"); 
				if(restPrevDay && restNextDay) {
					nViolations++; 
				}
			}
		}
		return nViolations; 
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
