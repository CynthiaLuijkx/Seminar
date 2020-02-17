import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import Tools.ContractGroup;
import Tools.DetermineViolations;
import Tools.Duty;
import Tools.Instance;
import Tools.ReserveDutyType;
import Tools.Schedule;
import Tools.ScheduleVis;
import Tools.Violation;
import ilog.concert.IloException;

import java.util.Locale;

public class Main 
{
	public static void main(String[] args) throws FileNotFoundException, IloException {
		// ---------------------------- Variable Input ------------------------------------------------------------
		String depot = "Dirksland"; //adjust to "Dirksland" or "Heinenoord"
		int dailyRestMin = 11 * 60; //amount of daily rest in minutes
		int restDayMin = 36 * 60; //amount of rest days in minutes (at least 32 hours in a row in one week)
		int restDayMinCG = 32*60;
		int restTwoWeek = 72 * 60;
		double violationBound = 0.3;
		double violationBound3Days = 0.3;

		// ---------------------------- Initialise instance -------------------------------------------------------
		Set<String> dutyTypes = new HashSet<>(); //types of duties
		//add the duty types
		dutyTypes.add("V");	dutyTypes.add("G");	dutyTypes.add("D");	dutyTypes.add("L");	dutyTypes.add("P"); dutyTypes.add("ATV"); 
		dutyTypes.add("RV"); dutyTypes.add("RG"); dutyTypes.add("RD"); dutyTypes.add("RL");
		if (depot.equals("Dirksland") || depot.equals("DirkslandEasier")) {
			dutyTypes.add("M");	dutyTypes.add("GM"); 
		} else if (depot.equals("Heinenoord")) {
			dutyTypes.add("W");
		} else {
			throw new IllegalArgumentException("This is not a valid depot name, please enter: 'Dirksland' or 'Heinenoord' to construct rosters for "
					+ "one of these depots.");
		}
		//Input the files
		File dutiesFile = new File("Data/" + depot + ".txt"); //file that contains duties and their features
		File contractGroupsFile = new File("Data/ContractGroups" + depot + ".txt"); //file that contains all contract groups and their features
		File reserveDutyFile = new File("Data/ReserveDuties" + depot + ".txt"); //file that contains the reserve duties and their features

		//Get all starting information
		Instance instance = readInstance(dutiesFile, contractGroupsFile, reserveDutyFile, dutyTypes, dailyRestMin, restDayMin, violationBound);

		System.out.println("Instance " + depot + " initialised");


		DetermineViolations temp = new DetermineViolations(instance, dutyTypes, violationBound, violationBound3Days); 
		System.out.println("Violations Determined"); 

		System.out.println(temp.get11Violations().size()); 
		System.out.println(temp.get32Violations().size()); 
		System.out.println(temp.getViolations3Days().size());

		instance.setViol(temp.get11Violations(), temp.get32Violations(), temp.getViolations3Days());
		System.out.println("Instance " + depot + " initialised");
		
		int numberOfDrivers = instance.getLB() +15;
		instance.setNrDrivers(numberOfDrivers);

		Phase1_Penalties penalties = new Phase1_Penalties();
		Set<Schedule> schedules = new HashSet<>();
		int iteration = 1;
		int maxIt = 5;
		boolean scheduleForEveryGroup = false;
		MIP_Phase1 mip = new MIP_Phase1(instance, dutyTypes, penalties);
		mip.solve();
		if (mip.isFeasible()) {
			mip.populate(maxIt);
			while (scheduleForEveryGroup == false && iteration <= maxIt) {
				mip.makeSolution();
				//mip.makeSolution(iteration);
				instance.setBasicSchedules(mip.getSolution());
				
				for(ContractGroup c : instance.getContractGroups()) {
					new ScheduleVis(instance.getBasicSchedules().get(c), ""+c.getNr());
				}

				long phase3Start = System.nanoTime();
				Phase3 colGen = new Phase3(instance, dailyRestMin, restDayMinCG, restTwoWeek);
				HashMap<Schedule, Double> solution = colGen.executeColumnGeneration();
				long phase3End = System.nanoTime();
				System.out.println("Phase 3 runtime: " + (phase3End - phase3Start) / 1000000000.0);

				int treshold = 0; // bigger than or equal
				schedules = getSchedulesAboveTreshold(solution, treshold);
				scheduleForEveryGroup = true;
				for (ContractGroup c : instance.getContractGroups()) {
					int included = 0;
					for (Schedule schedule : schedules) {
						if(schedule.getC() == c) {
							included++;
						}
					}
					if(included < 1) {
						scheduleForEveryGroup = false;
					}
				}
				iteration++;
			}
			
			if(iteration == maxIt || schedules.size() == 0) {
				System.out.println("No feasible schedules found on all " + maxIt + " basic schedules");
			}
			else {
				Phase4 phase4 = new Phase4(schedules, instance);
				List<Schedule> newSchedules = phase4.runILP();
				for(Schedule schedule : newSchedules) {
					new ScheduleVis(schedule.getSchedule(), ""+schedule.getC().getNr() , instance);
				}
			}
		} else {
			System.out.println("Basic schedule cannot be made.");
		}
		

	}

	//Method that read the instance files and add the right information to the corresponding sets
	//Also used as constructor of the class
	public static Instance readInstance(File dutiesFile, File contractGroupsFile, File reserveDutiesFile, Set<String> dutyTypes, 
			int dailyRestMin, int restDayMin, double violationBound) throws FileNotFoundException {
		//Initialize all sets/maps
		Set<Duty> workingDays = new HashSet<>(); 
		Set<Duty> saturday = new HashSet<>();
		Set<Duty> sunday = new HashSet<>();
		HashMap<String, Set<Duty>> dutiesPerType = new HashMap<>();
		HashMap<String, Set<Duty>> dutiesPerTypeW = new HashMap<>();
		HashMap<String, Set<Duty>> dutiesPerTypeSat = new HashMap<>();
		HashMap<String, Set<Duty>> dutiesPerTypeSun = new HashMap<>();
		HashMap<Integer, Duty> fromDutyNrToDuty = new HashMap<>();
		Set<ContractGroup> contractGroups = new HashSet<>();
		Set<ReserveDutyType> reserveDutyTypes = new HashSet<>();
		HashMap<Integer, ReserveDutyType> fromRDutyNrToRDuty = new HashMap<>();
		Set<Violation> violations11 = new HashSet<>();
		Set<Violation> violations32 = new HashSet<>();

		Scanner scDuties = new Scanner(dutiesFile); //Read the file dutiesFile
		while (scDuties.hasNext()) { //till all information from the file is read
			Duty newDuty = new Duty(scDuties.nextInt(), scDuties.next(), scDuties.nextInt(), scDuties.nextInt(), scDuties.nextInt(), 
					scDuties.nextInt(), scDuties.next(), scDuties.nextInt()); //Create a new duty with the corresponding information
			fromDutyNrToDuty.put(newDuty.getNr(),newDuty);
			if (newDuty.getDayType().equals("Workingday")) { //add all possible duty types that can be executed on a working day 
				workingDays.add(newDuty);
				if (!dutiesPerTypeW.containsKey(newDuty.getType())) {
					dutiesPerTypeW.put(newDuty.getType(), new HashSet<>());
				}
				dutiesPerTypeW.get(newDuty.getType()).add(newDuty);
			} else if (newDuty.getDayType().equals("Saturday")) {//add all possible duty types that can be executed on a Saturday 
				saturday.add(newDuty);
				if (!dutiesPerTypeSat.containsKey(newDuty.getType())) {
					dutiesPerTypeSat.put(newDuty.getType(), new HashSet<>());
				}
				dutiesPerTypeSat.get(newDuty.getType()).add(newDuty);
			} else if (newDuty.getDayType().equals("Sunday")) {//add all possible duty types that can be executed on a Sunday 
				sunday.add(newDuty);
				if (!dutiesPerTypeSun.containsKey(newDuty.getType())) {
					dutiesPerTypeSun.put(newDuty.getType(), new HashSet<>());
				}
				dutiesPerTypeSun.get(newDuty.getType()).add(newDuty);
			} else {
				throw new IllegalArgumentException("The duty cannot be assigned to one of the day types, please verify input to be 'Workingday', "
						+ "'Saturday', or 'Sunday'");
			}
			//if the type is not included yet, add it 
			if (!dutiesPerType.containsKey(newDuty.getType())) {
				dutiesPerType.put(newDuty.getType(), new HashSet<>());
			}
			//Add the duty to the corresponding duty type
			dutiesPerType.get(newDuty.getType()).add(newDuty);
		}
		scDuties.close();
		//Read the contract groups
		Scanner scGroups = new Scanner(contractGroupsFile);
		while (scGroups.hasNextLine()) {
			String nextLine = scGroups.nextLine();
			String[] entriesLine = nextLine.split("\t");
			Set<String> types = new HashSet<>();
			for (int i = 5; i < entriesLine.length; i++) {
				types.add(entriesLine[i]);
			}
			contractGroups.add(new ContractGroup(Integer.parseInt(entriesLine[0]), Integer.parseInt(entriesLine[1]), Double.parseDouble(entriesLine[2]), 
					Integer.parseInt(entriesLine[3]), Double.parseDouble(entriesLine[4]), types));
		}
		scGroups.close();
		//Read the reserve duties
		Scanner scReserve = new Scanner(reserveDutiesFile);
		scReserve.useLocale(Locale.ENGLISH); 
		int reserveCounter = 100;
		while (scReserve.hasNext()) { //continue till you have no more input
			//add for every reserve duty type it's features
			reserveDutyTypes.add(new ReserveDutyType(scReserve.next(), scReserve.next(), scReserve.nextInt(), scReserve.nextInt(), scReserve.nextDouble(), 
					reserveCounter));
			reserveCounter++;
		} 
		for(ReserveDutyType reserveDuties: reserveDutyTypes) {
			fromRDutyNrToRDuty.put(reserveDuties.getNr(), reserveDuties);
		}
		scReserve.close();

		return new Instance(workingDays, saturday, sunday, dutiesPerType, dutiesPerTypeW, dutiesPerTypeSat, dutiesPerTypeSun, fromDutyNrToDuty, contractGroups, 
				reserveDutyTypes, fromRDutyNrToRDuty, violations11, violations32);

	}

	/**
	 * Determines the number of violations between a set of duties and a reserve shift
	 * @param fromDuties			a set of duties
	 * @param to					the to shift (reserve)
	 * @param dailyRestMin			the minimum nr of minutes daily rest
	 * @param restDayMin			the minimum nr of minutes for a rest day
	 * @return						an array with the number of daily rest hours violations, number of rest day hours violations, total number of combinations
	 */
	public static int[] getViolations(Set<Duty> fromDuties, ReserveDutyType to, int dailyRestMin, int restDayMin) {
		int[] counts = new int[3];

		for (Duty from : fromDuties) {
			int gap = 0;
			//if the end time of the from duty is before the 24:00, we can determine the rest time of that day
			//if the end time of the from duty is after the 24:00, the rest only start afterwards
			if (from.getEndTime() <= 24 * 60) {
				gap += 24 * 60 - from.getEndTime();
			} else {
				gap -= from.getEndTime() - 24 * 60;
			}
			//add the start time of the follow up duty to get the rest time
			gap += to.getStartTime();
			//if the gap is less than 11 hours, it violates the daily rest constraint
			if (gap < dailyRestMin) {
				counts[0]++;
			}
			//if the gap is less than 32 hours, it violates the 32 hours constraint
			if (gap + 24 * 60 < restDayMin) {
				counts[1]++;
			}
			counts[2]++;
		}

		return counts;
	}

	/**
	 * Determines whether a violation exists between reserve duties
	 * @param from					the from shift
	 * @param to					the to shift
	 * @param dailyRestMin			the minimum nr of minutes daily rest
	 * @param restDayMin			the minimum nr of minutes for a rest day
	 * @return						an array with the number of daily rest hours violations, number of rest day hours violations, total number of combinations
	 */
	//similar idea
	public static int[] getViolations(ReserveDutyType from, ReserveDutyType to, int dailyRestMin, int restDayMin) {
		int[] counts = new int[3];

		int gap = 0;
		if (from.getEndTime() <= 24 * 60) {
			gap += 24 * 60 - from.getEndTime();
		} else {
			gap -= from.getEndTime() - 24 * 60;
		}

		gap += to.getStartTime();

		if (gap < dailyRestMin) {
			counts[0]++;
		}
		if (gap + 24 * 60 < restDayMin) {
			counts[1]++;
		}
		counts[2]++;

		return counts;
	}

	/**
	 * Determines the number of violations between a reserve duty and a set of duties
	 * @param from					the from shift (reserve)
	 * @param toDuties				the set of duties
	 * @param dailyRestMin			the minimum nr of minutes daily rest
	 * @param restDayMin			the minimum nr of minutes for a rest day
	 * @return						an array with the number of daily rest hours violations, number of rest day hours violations, total number of combinations
	 */
	//Similar idea
	public static int[] getViolations(ReserveDutyType from, Set<Duty> toDuties, int dailyRestMin, int restDayMin) {
		int[] counts = new int[3];

		for (Duty to : toDuties) {
			int gap = 0;
			if (from.getEndTime() <= 24 * 60) {
				gap += 24 * 60 - from.getEndTime();
			} else {
				gap -= from.getEndTime() - 24 * 60;
			}

			gap += to.getStartTime();

			if (gap < dailyRestMin) {
				counts[0]++;
			}
			if (gap + 24 * 60 < restDayMin) {
				counts[1]++;
			}
			counts[2]++;
		}

		return counts;
	}

	/**
	 * Determines the number of violations between two sets of duties
	 * @param fromDuties			a set of duties
	 * @param toDuties				a set of duties
	 * @param dailyRestMin			the minimum nr of minutes daily rest
	 * @param restDayMin			the minimum nr of minutes for a rest day
	 * @return						an array with the number of daily rest hours violations, number of rest day hours violations, total number of combinations
	 */
	//Similar idea
	public static int[] getViolations(Set<Duty> fromDuties, Set<Duty> toDuties, int dailyRestMin, int restDayMin) {
		int[] counts = new int[3];

		for (Duty from : fromDuties) {
			for (Duty to : toDuties) {
				int gap = 0;
				if (from.getEndTime() <= 24 * 60) {
					gap += 24 * 60 - from.getEndTime();
				} else {
					gap -= from.getEndTime() - 24 * 60;
				}

				gap += to.getStartTime();

				if (gap < dailyRestMin) {
					counts[0]++;
				}
				if (gap + 24 * 60 < restDayMin) {
					counts[1]++;
				}
				counts[2]++;
			}
		}

		return counts;
	}
	
	public static Set<Schedule> getSchedulesAboveTreshold(HashMap<Schedule, Double> solution, double treshold){
		Set<Schedule> output = new HashSet<>();
		for(Schedule schedule : solution.keySet()) {
			if(solution.get(schedule) >= treshold) {
				output.add(schedule);
			}
		}
		return output;
	}
}
