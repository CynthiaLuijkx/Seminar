import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class Main 
{
	public static void main(String[] args) throws FileNotFoundException {
		// ---------------------------- Variable Input ------------------------------------------------------------
		String depot = "Heinenoord";
		int dailyRestMin = 11 * 60;
		int restDayMin = 32 * 60;
		double violationBound = 0.9;
		// ---------------------------- Initialise instance -------------------------------------------------------
		Set<String> dutyTypes = new HashSet<>();
		dutyTypes.add("V");	dutyTypes.add("G");	dutyTypes.add("D");	dutyTypes.add("L");	dutyTypes.add("P");
		if (depot.equals("Dirksland")) {
			dutyTypes.add("M");	dutyTypes.add("GM");
		} else if (depot.equals("Heinenoord")) {
			dutyTypes.add("W");
		} else {
			throw new IllegalArgumentException("This is not a valid depot name, please enter: 'Dirksland' or 'Heinenoord' to construct rosters for "
					+ "one of these depots.");
		}
		File dutiesFile = new File("Data/" + depot + ".txt");
		File contractGroupsFile = new File("Data/ContractGroups" + depot + ".txt");
		File reserveDutyFile = new File("Data/ReserveDuties" + depot + ".txt");

		Instance instance = readInstance(dutiesFile, contractGroupsFile, reserveDutyFile, dutyTypes, dailyRestMin, restDayMin, violationBound);

		System.out.println("Instance " + depot + " initialised");


	}

	public static Instance readInstance(File dutiesFile, File contractGroupsFile, File reserveDutiesFile, Set<String> dutyTypes, 
			int dailyRestMin, int restDayMin, double violationBound) throws FileNotFoundException {
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

		Scanner scDuties = new Scanner(dutiesFile);
		while (scDuties.hasNext()) {
			Duty newDuty = new Duty(scDuties.nextInt(), scDuties.next(), scDuties.nextInt(), scDuties.nextInt(), scDuties.nextInt(), 
					scDuties.nextInt(), scDuties.next(), scDuties.nextInt());
			if (newDuty.getDayType().equals("Workingday")) {
				workingDays.add(newDuty);
				if (!dutiesPerTypeW.containsKey(newDuty.getType())) {
					dutiesPerTypeW.put(newDuty.getType(), new HashSet<>());
				}
				dutiesPerTypeW.get(newDuty.getType()).add(newDuty);
			} else if (newDuty.getDayType().equals("Saturday")) {
				saturday.add(newDuty);
				if (!dutiesPerTypeSat.containsKey(newDuty.getType())) {
					dutiesPerTypeSat.put(newDuty.getType(), new HashSet<>());
				}
				dutiesPerTypeSat.get(newDuty.getType()).add(newDuty);
			} else if (newDuty.getDayType().equals("Sunday")) {
				sunday.add(newDuty);
				if (!dutiesPerTypeSun.containsKey(newDuty.getType())) {
					dutiesPerTypeSun.put(newDuty.getType(), new HashSet<>());
				}
				dutiesPerTypeSun.get(newDuty.getType()).add(newDuty);
			} else {
				throw new IllegalArgumentException("The duty cannot be assigned to one of the day types, please verify input to be 'Workingday', "
						+ "'Saturday', or 'Sunday'");
			}
			if (!dutiesPerType.containsKey(newDuty.getType())) {
				dutiesPerType.put(newDuty.getType(), new HashSet<>());
			}
			dutiesPerType.get(newDuty.getType()).add(newDuty);
		}
		scDuties.close();

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

		Scanner scReserve = new Scanner(reserveDutiesFile);
		while (scReserve.hasNext()) {
			reserveDutyTypes.add(new ReserveDutyType(scReserve.next(), scReserve.next(), scReserve.nextInt(), scReserve.nextInt(), scReserve.nextDouble()));
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
		ReserveDutyType[] late = new ReserveDutyType[3];
		ReserveDutyType[] early = new ReserveDutyType[3];
		ReserveDutyType[] split = new ReserveDutyType[3];
		for (ReserveDutyType duty : reserveDutyTypes) {
			if (duty.getDayType().equals("Workingday")) {
				if (duty.getType().equals("V")) {
					early[0] = duty;
				} else if (duty.getType().equals("L")) {
					late[0] = duty;
				} else if (duty.getType().equals("G") || duty.getType().equals("GM")) {
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

		// W - W
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

		// W - Sat
		if (dutiesPerTypeW.containsKey("L") && dutiesPerTypeSat.containsKey("V")) {
			counts = getViolations(dutiesPerTypeW.get("L"), dutiesPerTypeSat.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "V", "Saturday", false));
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

		// W - Sun
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

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", false, "G", "Sunday", false));
			}
			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", false, "G", "Sunday", false));
			}
		}

		// W - RW
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

		// W - RSat
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

		// W - RSun
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

		// Sat - Sun
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

		// Sat - W
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

		// Sat - RSun
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

		// Sat - RW
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

		// Sun - W
		if (dutiesPerTypeSun.containsKey("L") && dutiesPerTypeW.containsKey("V")) {
			counts = getViolations(dutiesPerTypeSun.get("L"), dutiesPerTypeW.get("V"), dailyRestMin, restDayMin);
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

		// Sun - RW
		if (dutiesPerTypeSun.containsKey("L") && early[0] != null) {
			counts = getViolations(dutiesPerTypeSun.get("L"), early[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", false, "V", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", false, "V", "Workingday", true));
			}
		}
		if (dutiesPerTypeW.containsKey("L") && split[0] != null) {
			counts = getViolations(dutiesPerTypeW.get("L"), split[0], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Sunday", false, "G", "Workingday", true));
			}
			if (counts[1]/((double) counts[2]) >= violationBound) {
				violations32.add(new Violation("L", "Sunday", false, "G", "Workingday", true));
			}
		}

		// RW - W
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

		// RW - Sat
		if (late[0] != null && dutiesPerTypeSat.containsKey("V")) {
			counts = getViolations(late[0], dutiesPerTypeSat.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "V", "Saturday", false));
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

		// RW - Sun
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

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Workingday", true, "G", "Sunday", false));
			}
			if ((countsG[1] + countsGM[1])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations32.add(new Violation("L", "Workingday", true, "G", "Sunday", false));
			}
		}

		// RW - RW
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

		// RW - RSat
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

		// RW - RSun
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

		// RSat - Sun
		if (late[1] != null && dutiesPerTypeSun.containsKey("V")) {
			counts = getViolations(late[1], dutiesPerTypeSun.get("V"), dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", true, "V", "Sunday", false));
			}
		}
		if (late[1] != null && (dutiesPerTypeSun.containsKey("G") || dutiesPerTypeSun.containsKey("GM"))) {
			int[] countsG = new int[3];
			int[] countsGM = new int[3];
			if (dutiesPerTypeW.containsKey("G")) {
				countsG = getViolations(late[1], dutiesPerTypeSun.get("G"), dailyRestMin, restDayMin);
			}
			if (dutiesPerTypeW.containsKey("GM")) {
				countsGM = getViolations(late[1], dutiesPerTypeSun.get("GM"), dailyRestMin, restDayMin);
			}

			if ((countsG[0] + countsGM[0])/((double) (countsG[2] + countsGM[2])) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", true, "G", "Sunday", false));
			}
		}

		// RSat - W
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

		// RSat - RSun
		if (late[1] != null && early[2] != null) {
			counts = getViolations(late[1], early[2], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", true, "V", "Sunday", true));
			}
		}
		if (late[1] != null && split[1] != null) {
			counts = getViolations(late[1], split[2], dailyRestMin, restDayMin);
			if (counts[0]/((double) counts[2]) >= violationBound) {
				violations11.add(new Violation("L", "Saturday", true, "G", "Sunday", true));
			}
		}

		// RSat - RW
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

		// RSun - W
		if (late[2] != null && dutiesPerTypeW.containsKey("V")) {
			counts = getViolations(late[2], dutiesPerTypeW.get("V"), dailyRestMin, restDayMin);
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

		// RSun - RW
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
	 * Determines whether a violation exists between reserve duties
	 * @param from					the from shift
	 * @param to					the to shift
	 * @param dailyRestMin			the minimum nr of minutes daily rest
	 * @param restDayMin			the minimum nr of minutes for a rest day
	 * @return						an array with the number of daily rest hours violations, number of rest day hours violations, total number of combinations
	 */
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
