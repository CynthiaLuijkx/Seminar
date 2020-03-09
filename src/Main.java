import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import Phase5.Solution;
import Tools.ContractGroup;
import Tools.DetermineViolations;
import Tools.Duty;
import Tools.Instance;
import Tools.ReserveDutyType;
import Tools.Schedule;
import Tools.ScheduleVis;
import Tools.Violation;
import ilog.concert.IloException;

public class Main 
{
	private static Scanner sc;

	public static void main(String[] args) throws FileNotFoundException, IloException, IOException {
		// ---------------------------- Variable Input ------------------------------------------------------------
		String depot = "Dirksland"; //adjust to "Dirksland" or "Heinenoord"
		int paramCase = 421;
		int multiplierSoft = 500;
		int multiplierFair = 1000;
		int dailyRestMin = 11 * 60; //amount of daily rest in minutes
		int restDayMin = 36 * 60; //amount of rest days in minutes (at least 32 hours in a row in one week)
		int restDayMinCG = 32*60;
		int restTwoWeek = 72 * 60;
		int tabuLength = 5;
		int iterations_phase5 = 10000;
		double violationBound = 0.3;
		double violationBound3Days = 0.3;
		boolean phase123 = false;
		boolean ALNS = false;
		long[] seeds = new long[10];
		seeds[0] = 150659;
		seeds[1] = 332803;
		seeds[2] = 418219;
		seeds[3] = 415993;
		seeds[4] = 68371;
		seeds[5] = 186917;
		seeds[6] = 41;
		seeds[7] = 56081;
		seeds[8] = 609599;
		seeds[9] = 218527;
		long seedColGen = 1000;
		long seedInteger = 1000;
		boolean visualise = true;
		int scheduleNr = 7;

		// ---------------------------- Initialise instance -------------------------------------------------------
		long[] times = new long[6];
		times[0] = System.nanoTime();
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
		Instance instance = readInstance(dutiesFile, contractGroupsFile, reserveDutyFile, dutyTypes, dailyRestMin, restDayMin, violationBound, tabuLength, 
				multiplierSoft, multiplierFair);
		Schedule.setInstance(instance);
		System.out.println("Instance " + depot + " initialised");
		
		DetermineViolations temp = new DetermineViolations(instance, dutyTypes, violationBound, violationBound3Days); 
		System.out.println("Violations Determined"); 

		System.out.println(temp.get11Violations().size()); 
		System.out.println(temp.get32Violations().size()); 
		System.out.println(temp.getViolations3Days().size());

		instance.setViol(temp.get11Violations(), temp.get32Violations(), temp.getViolations3Days());
		System.out.println("Instance " + depot + " initialised");

		//Set based on bounds
		int numberOfDrivers = instance.getLB()+13;
		instance.setNrDrivers(numberOfDrivers);
		
		//Set manually
		/*
		int contractgroup1 = 37;
		int contractgroup2 = 25;
		int contractgroup3 = 0; //Heinenoord only
		int contractgroup4 = 0; //Heinenoord only
		for(ContractGroup group : instance.getContractGroups()) {
			if(group.getNr() == 1) {
				group.setTc(7*contractgroup1);
				group.setATVc((int) Math.floor(group.getATVPerYear() / 365.0 * group.getTc()));
			}
			if(group.getNr() == 2) {
				group.setTc(7*contractgroup2);
				group.setATVc((int) Math.floor(group.getATVPerYear() / 365.0 * group.getTc()));
			}
			if(group.getNr() == 3) {
				group.setTc(7*contractgroup3);
				group.setATVc((int) Math.floor(group.getATVPerYear() / 365.0 * group.getTc()));
			}
			if(group.getNr() == 4) {
				group.setTc(7*contractgroup4);
				group.setATVc((int) Math.floor(group.getATVPerYear() / 365.0 * group.getTc()));
			}
		}*/
		
		times[1] = System.nanoTime();
		
		if (phase123) {
			Set<Schedule> schedules = new HashSet<>();
			int iteration = 0;
			int maxIt = 5;
			boolean scheduleForEveryGroup = false;
			MIP_Phase1 mip = new MIP_Phase1(instance, dutyTypes);
			mip.solve();
			if (mip.isFeasible()) {
				//int nsol = mip.populate(maxIt); //When using populate
				int nsol = 1; //When not using populate 
				while (scheduleForEveryGroup == false && iteration < nsol) {
					mip.makeSolution(iteration); 
					instance.setBasicSchedules(mip.getSolution());
					
					for(ContractGroup c : instance.getContractGroups()) {
						//new ScheduleVis(instance.getBasicSchedules().get(c), ""+c.getNr(), depot);
					}
					
					times[2] = System.nanoTime();
	
					long phase3Start = System.nanoTime();
					Phase3 colGen = new Phase3(instance, dailyRestMin, restDayMinCG, restTwoWeek, seedColGen);
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
					
					times[3] = System.nanoTime();
				}
				
				if(iteration == maxIt || schedules.size() == 0) {
					System.out.println("No feasible schedules found on all " + maxIt + " basic schedules");
				}
				else {
					Phase4 phase4 = new Phase4(schedules, instance, seedInteger);
					List<Schedule> newSchedules = phase4.runILP();
					
					//Turn this off if you don't want to do the swaps
					Phase4_AddMissing addMissing = new Phase4_AddMissing(newSchedules, instance, seedInteger);
					newSchedules = addMissing.getNewSchedules();
					
					for(Schedule schedule : newSchedules) {
						new ScheduleVis(schedule.getSchedule(), ""+schedule.getC().getNr() , instance, depot);
						printSchedule(schedule, depot, numberOfDrivers, schedule.getC().getNr());
					}
					times[4] = System.nanoTime();
				}
			} else {
				System.out.println("Basic schedule cannot be made.");
			}
		}
		
		double[][] results = new double[seeds.length][instance.getContractGroups().size() + 7];
		if (ALNS) {
			Map<ContractGroup, Schedule> schedules = readSchedules(depot, numberOfDrivers, instance.getContractGroups());
			
			for (ContractGroup group : instance.getContractGroups()) {
				new ScheduleVis(schedules.get(group).getScheduleArray(), ""+ group.getNr() +"before", instance, depot);
			}
			FileWriter writer = new FileWriter("ResultsALNS_" + depot + "_C" + paramCase + "_" + multiplierSoft + "_" + multiplierFair + ".txt");
			for (int seedNr = 0; seedNr <seeds.length; seedNr++) {
				long startALNS = System.nanoTime();
				Phase5_ALNS alns= new Phase5_ALNS(iterations_phase5, instance, schedules, seeds[seedNr]); 
				Solution solutionALNS = alns.executeBasic(schedules);
				long endALNS = System.nanoTime();
				double obj = solutionALNS.getObj();
				double costs = solutionALNS.getCosts();
				double fairScore = solutionALNS.getFairScore();
				double softScore = solutionALNS.getSoftScore();
				double overTime = solutionALNS.getOvertime();
				double minus = solutionALNS.getMinusHours();
				System.out.println("----------------------------------------------------------");
				System.out.println("Objective values: " + obj);
				System.out.println("Contract + Overtime Costs: " + costs);
				System.out.println("Penalties Attractiveness: " + softScore);
				System.out.println("Penalties Fairness: " + fairScore);
				System.out.println("Total Overtime: " + overTime);
				System.out.println("Total Minus Hours: " + minus);
				System.out.println("----------------------------------------------------------");
				System.out.println("Violations Attractiveness: ");
				solutionALNS.printSoftViol();
				System.out.println("Violations Fairness: ");
				solutionALNS.printFairPen();
				results[seedNr][0] = obj;
				results[seedNr][1] = costs;
				results[seedNr][2] = softScore;
				results[seedNr][3] = fairScore;
				results[seedNr][4] = overTime;
				results[seedNr][5] = minus;
				results[seedNr][6] = (endALNS-startALNS)/1000000000.0;
				for (ContractGroup group : instance.getContractGroups()) {
					printSchedule(solutionALNS.getNewSchedule().get(group), depot, group.getNr(), seedNr, multiplierSoft, multiplierFair);
				}
				
				for (ContractGroup group : instance.getContractGroups()) {
					results[seedNr][6 + group.getNr()] = solutionALNS.getNewSchedule().get(group).getSchedule().length/7;
				}
				
				for (int i = 0; i < results[0].length - instance.getContractGroups().size(); i++) {
					writer.write(Double.toString(results[seedNr][i]) + ", ");
				}
				double[][] fairViolations = solutionALNS.getFeasCheck().getAllFairness(solutionALNS);
				for (int i = 1; i <= instance.getContractGroups().size(); i++) {
					if (i > 1) {
						writer.write(System.getProperty("line.separator"));
						for (int j = 0; j < results[0].length - instance.getContractGroups().size(); j++) {
							writer.write(", ");
						}
					}
					ContractGroup c = null;
					for (ContractGroup group : instance.getContractGroups()) {
						if (group.getNr() == i) {
							c = group;
							break;
						}
					}
					writer.write(Double.toString(results[seedNr][6 + i]) + ", ");
					int[] softViolations = solutionALNS.getFeasCheck().allViolations(solutionALNS.getNewSchedule().get(c).getSchedule(), c);
					for (int violNr = 0; violNr < softViolations.length; violNr++) {
						writer.write(Integer.toString(softViolations[violNr]) + ", ");
					}
					
					for (int violNr = 0; violNr < fairViolations.length; violNr++) {
						writer.write(Double.toString(fairViolations[violNr][c.getNr()-1]) + ", ");
					}
				}
				writer.write(System.getProperty("line.separator"));
				
//				for (ContractGroup group : instance.getContractGroups()) {
//					new ScheduleVis(solutionALNS.getNewSchedule().get(group).getScheduleArray(), ""+group.getNr()+"after" , instance, depot);
//				}
			}
			writer.close();
			times[5] = System.nanoTime();
		}
		
		if (visualise) {
			String[] names = new String[instance.getContractGroups().size()];
			for (int c = 0; c < instance.getContractGroups().size(); c++) {
				names[c] = "Schedule_" + depot + "_" + c + "_" + scheduleNr + "_" + multiplierSoft + "_" + multiplierFair + ".txt";
			}
			
			Map<ContractGroup, Schedule> schedules = readSchedules(names, instance.getContractGroups());
			
			for (ContractGroup group : instance.getContractGroups()) {
				new ScheduleVis(schedules.get(group).getScheduleArray(), ""+ group.getNr() +"before", instance, depot);
			}
		}
		
		System.out.println("----------------------------------------------------------");
		
		if(ALNS && phase123) {
			System.out.println("Total time elapsed: " + (times[5] - times[0])/1000000000.0);
			System.out.println("Initialisation: " + (times[1] - times[0])/1000000000.0);
			System.out.println("Phase 1: " + (times[2] - times[1])/1000000000.0);
			System.out.println("Phase 2: " + (times[3] - times[2])/1000000000.0);
			System.out.println("Phase 3: " + (times[4] - times[3])/1000000000.0);
			System.out.println("Phase 4: " + (times[5] - times[4])/1000000000.0);
		}
		else if(ALNS) {
			System.out.println("Total time elapsed: " + (times[5] - times[0])/1000000000.0);
			System.out.println("Initialisation: " + (times[1] - times[0])/1000000000.0);
			System.out.println("Phase 4: " + (times[5] - times[1])/1000000000.0);
		}
		else {
			System.out.println("Total time elapsed: " + (times[4] - times[0])/1000000000.0);
			System.out.println("Initialisation: " + (times[1] - times[0])/1000000000.0);
			System.out.println("Phase 1: " + (times[2] - times[1])/1000000000.0);
			System.out.println("Phase 2: " + (times[3] - times[2])/1000000000.0);
			System.out.println("Phase 3: " + (times[4] - times[3])/1000000000.0);
		}
		
		System.out.println("----------------------------------------------------------");
		System.out.print("ObjVal"); System.out.print("\t"); System.out.print("Contract+Overtime"); System.out.print("\t"); System.out.print("Attractiveness");
		System.out.print("\t"); System.out.print("Fairness"); System.out.print("\t"); System.out.print("Overtime"); System.out.print("\t"); System.out.println("Minus");
		System.out.print("\t"); System.out.print("ContractGroupSizes(OrderOfContractNumber)"); System.out.print("\t"); System.out.println("RunningTime(sec.)");
		for (int seedNr = 0; seedNr < seeds.length; seedNr++) {
			for (int i = 0; i < 6 + instance.getContractGroups().size(); i++) {
				System.out.print(results[seedNr][i]);
				System.out.print("\t");
			}
			System.out.println();
		}
	}

	//Method that read the instance files and add the right information to the corresponding sets
	//Also used as constructor of the class
	public static Instance readInstance(File dutiesFile, File contractGroupsFile, File reserveDutiesFile, Set<String> dutyTypes, 
			int dailyRestMin, int restDayMin, double violationBound, int tabuLength, int multiplierSoft, int multiplierFair) throws FileNotFoundException {
		//Initialize all sets/maps
		LinkedHashSet<Duty> workingDays = new LinkedHashSet<>(); 
		LinkedHashSet<Duty> saturday = new LinkedHashSet<>();
		LinkedHashSet<Duty> sunday = new LinkedHashSet<>();
		HashMap<String, Set<Duty>> dutiesPerType = new HashMap<>();
		HashMap<String, Set<Duty>> dutiesPerTypeW = new HashMap<>();
		HashMap<String, Set<Duty>> dutiesPerTypeSat = new HashMap<>();
		HashMap<String, Set<Duty>> dutiesPerTypeSun = new HashMap<>();
		HashMap<Integer, Duty> fromDutyNrToDuty = new HashMap<>();
		LinkedHashSet<ContractGroup> contractGroups = new LinkedHashSet<>();
		LinkedHashSet<ReserveDutyType> reserveDutyTypes = new LinkedHashSet<>();
		HashMap<Integer, ReserveDutyType> fromRDutyNrToRDuty = new HashMap<>();
		Set<Violation> violations11 = new HashSet<>();
		Set<Violation> violations32 = new HashSet<>();
		
		LinkedHashSet<String> dutyTypesLinked = new LinkedHashSet<>();

		Scanner scDuties = new Scanner(dutiesFile); //Read the file dutiesFile
		while (scDuties.hasNext()) { //till all information from the file is read
			Duty newDuty = new Duty(scDuties.nextInt(), scDuties.next(), scDuties.nextInt(), scDuties.nextInt(), scDuties.nextInt(), 
					scDuties.nextInt(), scDuties.next(), scDuties.nextInt()); //Create a new duty with the corresponding information
			fromDutyNrToDuty.put(newDuty.getNr(),newDuty);
			dutyTypesLinked.add(newDuty.getType());
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
				reserveDutyTypes, fromRDutyNrToRDuty, violations11, violations32, tabuLength, multiplierSoft, multiplierFair, dutyTypesLinked);

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
	
	public static void printSchedule(Schedule schedule, String depot, int nDrivers, int contractGroupNr) throws IOException {
		FileWriter writer = new FileWriter("Schedule_" + depot + "_" + nDrivers + "_" + contractGroupNr + ".txt");
		
		writer.write(String.valueOf(contractGroupNr));
		writer.write(System.getProperty("line.separator"));
		writer.write(String.valueOf(schedule.getOvertime()));
		writer.write(System.getProperty("line.separator"));
		writer.write(String.valueOf(schedule.getSchedule().length));
		for (int i = 0; i < schedule.getSchedule().length; i++) {
			writer.write(System.getProperty("line.separator"));
			writer.write(String.valueOf(schedule.getSchedule()[i]));
		}
		
		writer.close();
	}
	
	public static void printSchedule(Schedule schedule, String depot, int contractGroupNr, 
			int seedNr, int multSoft, int multFair) throws IOException {
		FileWriter writer = new FileWriter("Schedule_" + depot + "_" + contractGroupNr + "_" + seedNr + "_" + multSoft + "_" + multFair + ".txt");
		
		writer.write(String.valueOf(contractGroupNr));
		writer.write(System.getProperty("line.separator"));
		writer.write(String.valueOf(schedule.getOvertime()));
		writer.write(System.getProperty("line.separator"));
		writer.write(String.valueOf(schedule.getSchedule().length));
		for (int i = 0; i < schedule.getSchedule().length; i++) {
			writer.write(System.getProperty("line.separator"));
			writer.write(String.valueOf(schedule.getSchedule()[i]));
		}
		
		writer.close();
	}
	
	public static Map<ContractGroup, Schedule> readSchedules(String[] names, Set<ContractGroup> groups) throws FileNotFoundException {
		Map<ContractGroup, Schedule> schedules = new HashMap<>();
		
		for (int c = 1; c <= groups.size(); c++) {
			sc = new Scanner(new File(names[c-1]));
			int contractGroupNr = sc.nextInt();
			int overtime = sc.nextInt();
			int[] schedule = new int[sc.nextInt()];
			for (int i = 0; i < schedule.length; i++) {
				schedule[i] = sc.nextInt();
			}
			
			ContractGroup group = null;
			for (ContractGroup temp : groups) {
				if (temp.getNr() == contractGroupNr) {
					group = temp;
				}
			}
			
			schedules.put(group, new Schedule(group, schedule, overtime));
		}
		
		return schedules;
	}
	
	public static Map<ContractGroup, Schedule> readSchedules(String depot, int nDrivers, Set<ContractGroup> groups) throws FileNotFoundException {
		Map<ContractGroup, Schedule> schedules = new HashMap<>();
		
		for (int c = 1; c <= groups.size(); c++) {
			sc = new Scanner(new File("Schedule_" + depot + "_" + nDrivers + "_" + c + ".txt"));
			int contractGroupNr = sc.nextInt();
			int overtime = sc.nextInt();
			int[] schedule = new int[sc.nextInt()];
			for (int i = 0; i < schedule.length; i++) {
				schedule[i] = sc.nextInt();
			}
			
			ContractGroup group = null;
			for (ContractGroup temp : groups) {
				if (temp.getNr() == contractGroupNr) {
					group = temp;
				}
			}
			
			schedules.put(group, new Schedule(group, schedule, overtime));
		}
		
		return schedules;
	}
	
	public static Map<ContractGroup, String[]> readBasicSchedule(String depot, int nDrivers, Set<ContractGroup> groups) throws FileNotFoundException {
		Map<ContractGroup, String[]> schedules = new HashMap<>();
		
		for (int c = 1; c <= groups.size(); c++) {
			sc = new Scanner(new File("BS_" + depot + "_" + nDrivers + "_" + c + ".txt"));
			int contractGroupNr = sc.nextInt();
			String[] schedule = new String[sc.nextInt()];
			for (int i = 0; i < schedule.length; i++) {
				schedule[i] = sc.next();
			}
			
			ContractGroup group = null;
			for (ContractGroup temp : groups) {
				if (temp.getNr() == contractGroupNr) {
					group = temp;
				}
			}
			
			schedules.put(group, schedule);
		}
		
		return schedules;
	}
}