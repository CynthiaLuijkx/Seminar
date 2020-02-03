import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
		Set<Duty> duties = new HashSet<>();
		Set<Duty> workingDays = new HashSet<>();
		Set<Duty> saturday = new HashSet<>();
		Set<Duty> sunday = new HashSet<>();
		HashMap<String, Set<Duty>> dutiesPerType = new HashMap<>();
		for (String type : dutyTypes) {
			dutiesPerType.put(type, new HashSet<Duty>());
		}
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
			} else if (newDuty.getDayType().equals("Saturday")) {
				saturday.add(newDuty);
			} else if (newDuty.getDayType().equals("Sunday")) {
				sunday.add(newDuty);
			} else {
				throw new IllegalArgumentException("The duty cannot be assigned to one of the day types, please verify input to be 'Workingday', "
						+ "'Saturday', or 'Sunday'");
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

		// ---------------------------- Determine the violations --------------------------------------------------
	
		// W - W
		int[] counts = getViolationsNormalToNormal(workingDays, workingDays, dailyRestMin, restDayMin);
		if (counts[0]/((double) counts[2]) >= violationBound) {
			violations11.add(new Violation("L", "Workingday", false, "E", "Workingday", false));
		}
		if (counts[1]/((double) counts[2]) >= violationBound) {
			violations32.add(new Violation("L", "Workingday", false, "E", "Workingday", false));
		}
		
		// W - Sat
		counts = getViolationsNormalToNormal(workingDays, saturday, dailyRestMin, restDayMin);
		if (counts[0]/((double) counts[2]) >= violationBound) {
			violations11.add(new Violation("L", "Workingday", "E", "Saturday"));
		}
		
		// W - Sun
		counts = getViolationsNormalToNormal(workingDays, sunday, dailyRestMin, restDayMin);
		if (counts[1]/((double) counts[2]) >= violationBound) {
			violations32.add(new Violation("N", "Workingday", "N", "Sunday"));
		}
		

		return new Instance(workingDays, saturday, sunday, dutiesPerType, contractGroups, reserveDutyTypes, violations11, violations32);
	}

	public static int[] getViolationsNormalToNormal(Set<Duty> fromDuties, Set<Duty> toDuties, int dailyRestMin, int restDayMin) {
		int[] counts = new int[3];
		Map<String, Set<Violation>> violations = new HashMap<>();
		
		for (Duty from : fromDuties) {
			if (from.getType().equals("L")) {
				for (Duty to : toDuties) {
					if (to.getType().equals("V")) {
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
			}
		}
		
		return counts;
	}

	public static Map<String, Set<Violation>> getViolationsNormalToReserve(Set<Duty> fromDuties, ReserveDutyType late, int dailyRestMin, int restDayMin) {
		Map<String, Set<Violation>> violations = new HashMap<>();
		
		for (Duty from : )

		return violations;
	}
}
