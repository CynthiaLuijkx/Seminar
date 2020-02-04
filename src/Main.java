import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import ilog.concert.IloException;

import java.util.Locale;

public class Main 
{
	public static void main(String[] args) throws FileNotFoundException, IloException {
		// ---------------------------- Variable Input ------------------------------------------------------------
		String depot = "Heinenoord"; //adjust to "Dirksland" or "Heinenoord"
		int dailyRestMin = 11 * 60; //amount of daily rest in minutes
		int restDayMin = 32 * 60; //amount of rest days in minutes (at least 32 hours in a row in one week)
		double violationBound = 0.9; 
				
		// ---------------------------- Initialise instance -------------------------------------------------------
		Set<String> dutyTypes = new HashSet<>(); //types of duties
		//add the duty types
		dutyTypes.add("V");	dutyTypes.add("G");	dutyTypes.add("D");	dutyTypes.add("L");	dutyTypes.add("P"); dutyTypes.add("ATV"); 
		dutyTypes.add("RV"); dutyTypes.add("RG"); dutyTypes.add("RD"); dutyTypes.add("RL");
		if (depot.equals("Dirksland")) {
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

		MIP_Phase1 mip = new MIP_Phase1(instance, dutyTypes);
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
		Set<ContractGroup> contractGroups = new HashSet<>();
		Set<ReserveDutyType> reserveDutyTypes = new HashSet<>();
		Set<Violation> violations11 = new HashSet<>();
		Set<Violation> violations32 = new HashSet<>();
		
		Scanner scDuties = new Scanner(dutiesFile); //Read the file dutiesFile
		while (scDuties.hasNext()) { //till all information from the file is read
			Duty newDuty = new Duty(scDuties.nextInt(), scDuties.next(), scDuties.nextInt(), scDuties.nextInt(), scDuties.nextInt(), 
					scDuties.nextInt(), scDuties.next(), scDuties.nextInt()); //Create a new duty with the corresponding information
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
			reserveDutyTypes.add(new ReserveDutyType(scReserve.next(), scReserve.next(), scReserve.nextInt(), scReserve.nextInt(), scReserve.nextDouble(), 100));
		} 
		scReserve.close();

		// ---------------------------- Determine the violations --------------------------------------------------

		/**
		 * Counts will contain:
		 * 	0:	The number of times 11 hours is violated
		 * 	1:	The number of times 32 hours is violated
		 * 	2:	The total number of times the combination exists
		 */
		int[] counts = new int[3];
		// In ReserveDutyType[3]: 0: working day, 1: Saturday, 2: Sunday
		ReserveDutyType[] late = new ReserveDutyType[3]; 
		ReserveDutyType[] early = new ReserveDutyType[3];
		ReserveDutyType[] split = new ReserveDutyType[3]; 
		//for every possible reserve duty type, check which duty type it is and on which day type
		for (ReserveDutyType duty : reserveDutyTypes) {
			if (duty.getDayType().equals("Workingday")) {
				if (duty.getType().equals("V")) {
					early[0] = duty;
				} else if (duty.getType().equals("L")) {
					late[0] = duty;
				} else if (duty.getType().equals("G") || duty.getType().equals("GM")) { //both can be seen as split duties
					split[0] = duty;
				}
			} else if (duty.getDayType().equals("Saturday")) {
				if (duty.getType().equals("V")) {
					early[1] = duty;
				} else if (duty.getType().equals("L")) {
					late[1] = duty;
				} else if (duty.getType().equals("G") || duty.getType().equals("GM")) {
					split[1] = duty;
				}
			} else {
				if (duty.getType().equals("V")) {
					early[2] = duty;
				} else if (duty.getType().equals("L")) {
					late[2] = duty;
				} else if (duty.getType().equals("G") || duty.getType().equals("GM")) {
					split[2] = duty;
				}
			}
		}

		// for the combination: W - W
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeW.containsKey("L") && dutiesPerTypeW.containsKey("V")) {
			counts = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeW.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "V", "Workingday", false));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "V", "Workingday", false));
			}
		}
		if (dutiesPerTypeW.containsKey("L") && (dutiesPerTypeW.containsKey("G") || dutiesPerTypeW.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeW.containsKey("G")) {
				countsG = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeW.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeW.containsKey("GM")) {
				countsGM = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeW.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "G", "Workingday", false));
			}
			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "G", "Workingday", false));
			}
		}

		// for the combination: W - Sat
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeW.containsKey("L") && dutiesPerTypeSat.containsKey("V")) {
			counts = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeSat.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "V", "Saturday", false));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "V", "Saturday", false));
			}
		}
		if (dutiesPerTypeW.containsKey("L") && (dutiesPerTypeSat.containsKey("G") || dutiesPerTypeSat.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeSat.containsKey("G")) {
				countsG = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeSat.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeSat.containsKey("GM")) {
				countsGM = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeSat.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "G", "Saturday", false));
			}
			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "G", "Saturday", false));
			}
		}

		// for the combination: W - Sun
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeW.containsKey("L") && dutiesPerTypeSun.containsKey("V")) {
			counts = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeSun.get("V"), dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "V", "Sunday", false));
			}
		}
		if (dutiesPerTypeW.containsKey("L") && (dutiesPerTypeSun.containsKey("G") || dutiesPerTypeSun.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeSun.containsKey("G")) {
				countsG = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeSun.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeSun.containsKey("GM")) {
				countsGM = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeSun.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "G", "Sunday", false));
			}
		}

		// for the combination: W - RW
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeW.containsKey("L") && early[0] != null) {
			counts = getViolations(dutiesPerTypeW.get("L"), early[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "V", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "V", "Workingday", true));
			}
		}
		if (dutiesPerTypeW.containsKey("L") && split[0] != null) {
			counts = getViolations(dutiesPerTypeW.get("L"), split[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "G", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "G", "Workingday", true));
			}
		}

		// for the combination: W - RSat
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeW.containsKey("L") && early[1] != null) {
			counts = getViolations(dutiesPerTypeW.get("L"), early[1], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "V", "Saturday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "V", "Saturday", true));
			}
		}
		if (dutiesPerTypeW.containsKey("L") && split[1] != null) {
			counts = getViolations(dutiesPerTypeW.get("L"), split[1], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "G", "Saturday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "G", "Saturday", true));
			}
		}

		// for the combination: W - RSun
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeW.containsKey("L") && early[2] != null) {
			counts = getViolations(dutiesPerTypeW.get("L"), early[2], dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "V", "Sunday", true));
			}
		}
		if (dutiesPerTypeW.containsKey("L") && split[2] != null) {
			counts = getViolations(dutiesPerTypeW.get("L"), split[2], dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "G", "Sunday", true));
			}
		}

		//for the combination: Sat - Sun
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeSat.containsKey("L") && dutiesPerTypeSun.containsKey("V")) {
			counts = getViolations(dutiesPerTypeSat.get("L"), dutiesPerTypeSun.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", false, "V", "Sunday", false));
			}
		}
		if (dutiesPerTypeSat.containsKey("L") && (dutiesPerTypeSun.containsKey("G") || dutiesPerTypeSun.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeW.containsKey("G")) {
				countsG = getViolations(dutiesPerTypeSat.get("L"), dutiesPerTypeSun.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeW.containsKey("GM")) {
				countsGM = getViolations(dutiesPerTypeSat.get("L"), dutiesPerTypeSun.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", false, "G", "Sunday", false));
			}
		}

		//for the combination: Sat - W
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeSat.containsKey("L") && dutiesPerTypeW.containsKey("V")) {
			counts = getViolations(dutiesPerTypeSat.get("L"), dutiesPerTypeW.get("V"), dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Saturday", false, "V", "Workingday", false));
			}
		}
		if (dutiesPerTypeSat.containsKey("L") && (dutiesPerTypeW.containsKey("G") || dutiesPerTypeW.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeW.containsKey("G")) {
				countsG = getViolations(dutiesPerTypeSat.get("L"), dutiesPerTypeW.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeW.containsKey("GM")) {
				countsGM = getViolations(dutiesPerTypeSat.get("L"), dutiesPerTypeW.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Saturday", false, "G", "Workingday", false));
			}
		}

		// for the combination: Sat - RSun
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeSat.containsKey("L") && early[2] != null) {
			counts = getViolations(dutiesPerTypeSat.get("L"), early[2], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", false, "V", "Sunday", true));
			}
		}
		if (dutiesPerTypeSat.containsKey("L") && split[2] != null) {
			counts = getViolations(dutiesPerTypeSat.get("L"), split[2], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", false, "G", "Sunday", true));
			}
		}

		// for the combination: Sat - RW
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeSat.containsKey("L") && early[0] != null) {
			counts = getViolations(dutiesPerTypeSat.get("L"), early[0], dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Saturday", false, "V", "Workingday", true));
			}
		}
		if (dutiesPerTypeSat.containsKey("L") && split[0] != null) {
			counts = getViolations(dutiesPerTypeSat.get("L"), split[0], dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Saturday", false, "G", "Workingday", true));
			}
		}

		// for the combination: Sun - W
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeSun.containsKey("L") && dutiesPerTypeW.containsKey("V")) {
			counts = getViolations(dutiesPerTypeSun.get("L"), dutiesPerTypeW.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", false, "V", "Workingday", false));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", false, "V", "Workingday", false));
			}
		}
		if (dutiesPerTypeSun.containsKey("L") && (dutiesPerTypeW.containsKey("G") || dutiesPerTypeW.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeW.containsKey("G")) {
				countsG = getViolations(dutiesPerTypeSun.get("L"), dutiesPerTypeW.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeW.containsKey("GM")) {
				countsGM = getViolations(dutiesPerTypeSun.get("L"), dutiesPerTypeW.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", false, "G", "Workingday", false));
			}
			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", false, "G", "Workingday", false));
			}
		}

		// for the combination: Sun - RW
		//if we exceed the violation bound then we add it to the set of violations
		if (dutiesPerTypeSun.containsKey("L") && early[0] != null) {
			counts = getViolations(dutiesPerTypeSun.get("L"), early[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", false, "V", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", false, "V", "Workingday", true));
			}
		}
		if (dutiesPerTypeSun.containsKey("L") && split[0] != null) {
			counts = getViolations(dutiesPerTypeSun.get("L"), split[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", false, "G", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", false, "G", "Workingday", true));
			}
		}

		//for the combination: RW - W
		//if we exceed the violation bound then we add it to the set of violations
		if (late[0] != null && dutiesPerTypeW.containsKey("V")) {
			counts = getViolations(late[0], dutiesPerTypeW.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "V", "Workingday", false));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "V", "Workingday", false));
			}
		}
		if (late[0] != null && (dutiesPerTypeW.containsKey("G") || dutiesPerTypeW.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeW.containsKey("G")) {
				countsG = getViolations(late[0], dutiesPerTypeW.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeW.containsKey("GM")) {
				countsGM = getViolations(late[0], dutiesPerTypeW.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "G", "Workingday", false));
			}
			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "G", "Workingday", false));
			}
		}

		// for the combination: RW - Sat
		//if we exceed the violation bound then we add it to the set of violations
		if (late[0] != null && dutiesPerTypeSat.containsKey("V")) {
			counts = getViolations(late[0], dutiesPerTypeSat.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "V", "Saturday", false));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "V", "Saturday", false));
			}
		}
		if (late[0] != null && (dutiesPerTypeSat.containsKey("G") || dutiesPerTypeSat.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeSat.containsKey("G")) {
				countsG = getViolations(late[0], dutiesPerTypeSat.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeSat.containsKey("GM")) {
				countsGM = getViolations(late[0], dutiesPerTypeSat.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "G", "Saturday", false));
			}
			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "G", "Saturday", false));
			}
		}

		// for the combination: RW - Sun
		//if we exceed the violation bound then we add it to the set of violations
		if (late[0] != null && dutiesPerTypeSun.containsKey("V")) {
			counts = getViolations(late[0], dutiesPerTypeSun.get("V"), dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "V", "Sunday", false));
			}
		}
		if (late[0] != null && (dutiesPerTypeSun.containsKey("G") || dutiesPerTypeSun.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeSun.containsKey("G")) {
				countsG = getViolations(late[0], dutiesPerTypeSun.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeSun.containsKey("GM")) {
				countsGM = getViolations(late[0], dutiesPerTypeSun.get("GM"), dailyRestMin, restDayMin);
			}
			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "G", "Sunday", false));
			}
		}

		// for the combination: RW - RW
		//if we exceed the violation bound then we add it to the set of violations
		if (late[0] != null && early[0] != null) {
			counts = getViolations(late[0], early[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "V", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "V", "Workingday", true));
			}
		}
		if (late[0] != null && split[0] != null) {
			counts = getViolations(late[0], split[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "G", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "G", "Workingday", true));
			}
		}

		// for the combination: RW - RSat
		//if we exceed the violation bound then we add it to the set of violations
		if (late[0] != null && early[1] != null) {
			counts = getViolations(late[0], early[1], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "V", "Saturday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "V", "Saturday", true));
			}
		}
		if (late[0] != null && split[1] != null) {
			counts = getViolations(late[0], split[1], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "G", "Saturday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "G", "Saturday", true));
			}
		}

		// for the combination: RW - RSun
		//if we exceed the violation bound then we add it to the set of violations
		if (late[0] != null && early[2] != null) {
			counts = getViolations(late[0], early[2], dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "V", "Sunday", true));
			}
		}
		if (late[0] != null && split[2] != null) {
			counts = getViolations(late[0], split[2], dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "G", "Sunday", true));
			}
		}

		// for the combination: RSat - Sun
		//if we exceed the violation bound then we add it to the set of violations
		if (late[1] != null && dutiesPerTypeSun.containsKey("V")) {
			counts = getViolations(late[1], dutiesPerTypeSun.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", true, "V", "Sunday", false));
			}
		}
		if (late[1] != null && (dutiesPerTypeSun.containsKey("G") || dutiesPerTypeSun.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeSun.containsKey("G")) {
				countsG = getViolations(late[1], dutiesPerTypeSun.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeSun.containsKey("GM")) {
				countsGM = getViolations(late[1], dutiesPerTypeSun.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", true, "G", "Sunday", false));
			}
		}

		//// for the combination: RSat - W
		//if we exceed the violation bound then we add it to the set of violations
		if (late[1] != null && dutiesPerTypeW.containsKey("V")) {
			counts = getViolations(late[1], dutiesPerTypeW.get("V"), dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Saturday", true, "V", "Workingday", false));
			}
		}
		if (late[1] != null && (dutiesPerTypeW.containsKey("G") || dutiesPerTypeW.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeW.containsKey("G")) {
				countsG = getViolations(late[1], dutiesPerTypeW.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeW.containsKey("GM")) {
				countsGM = getViolations(late[1], dutiesPerTypeW.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Saturday", true, "G", "Workingday", false));
			}
		}

		// for the combination: RSat - Sun
		//if we exceed the violation bound then we add it to the set of violations
		if (late[1] != null && early[2] != null) {
			counts = getViolations(late[1], early[2], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", true, "V", "Sunday", true));
			}
		}
		if (late[1] != null && split[2] != null) {
			counts = getViolations(late[1], split[2], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", true, "G", "Sunday", true));
			}
		}

		// for the combination: RSat - RW
		//if we exceed the violation bound then we add it to the set of violations
		if (late[1] != null && early[0] != null) {
			counts = getViolations(late[1], early[0], dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Saturday", true, "V", "Workingday", true));
			}
		}
		if (late[1] != null && split[0] != null) {
			counts = getViolations(late[1], split[0], dailyRestMin, restDayMin);
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Saturday", true, "G", "Workingday", true));
			}
		}

		// for the combination: RSun - W 
		//if we exceed the violation bound then we add it to the set of violations
		if (late[2] != null && dutiesPerTypeW.containsKey("V")) {
			counts = getViolations(late[2], dutiesPerTypeW.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", true, "V", "Workingday", false));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", true, "V", "Workingday", false));
			}
		}
		if (late[2] != null && (dutiesPerTypeW.containsKey("G") || dutiesPerTypeW.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeW.containsKey("G")) {
				countsG = getViolations(late[2], dutiesPerTypeW.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeW.containsKey("GM")) {
				countsGM = getViolations(late[2], dutiesPerTypeW.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", true, "G", "Workingday", false));
			}
			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", true, "G", "Workingday", false));
			}
		}

		//for the combination: RSun - RW 
		//if we exceed the violation bound then we add it to the set of violations
		if (late[2] != null && early[0] != null) {
			counts = getViolations(late[2], early[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", true, "V", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", true, "V", "Workingday", true));
			}
		}
		if (late[2] != null && split[0] != null) {
			counts = getViolations(late[2], split[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", true, "G", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", true, "G", "Workingday", true));
			}
		}

		return new Instance(workingDays, saturday, sunday, dutiesPerType, dutiesPerTypeW, dutiesPerTypeSat, dutiesPerTypeSun, contractGroups, 
				reserveDutyTypes, violations11, violations32);
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
}
