import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import Tools.ContractGroup;
import Tools.DetermineViolations;
import Tools.Duty;
import Tools.Instance;
import Tools.ReserveDutyType;
import Tools.Violation;
import ilog.concert.IloException;

import java.util.Locale;

public class Main 
{
	public static void main(String[] args) throws FileNotFoundException, IloException {
		// ---------------------------- Variable Input ------------------------------------------------------------
		String depot = "Dirksland"; //adjust to "Dirksland" or "Heinenoord"
		int dailyRestMin = 11 * 60; //amount of daily rest in minutes
		int restDayMin = 32 * 60; //amount of rest days in minutes (at least 32 hours in a row in one week)
		double violationBound = 0.8; 

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

		DetermineViolations temp = new DetermineViolations(instance, dutyTypes, violationBound); 
		System.out.println("Violations Determined"); 

		System.out.println(temp.get11Violations().size()); 
		System.out.println(temp.get32Violations().size()); 
		System.out.println(temp.getViolations3Days().size()); 

		instance.setViol(temp.get11Violations(), temp.get32Violations());
		System.out.println("Instance " + depot + " initialised");
		
		int numberOfDrivers = instance.getLB() + 15;
		instance.setNrDrivers(numberOfDrivers);

		Phase1_Penalties penalties = new Phase1_Penalties();
		MIP_Phase1 mip = new MIP_Phase1(instance, dutyTypes, penalties);
		instance.setBasicSchedules(mip.getSolution());
		
		Phase3_Constructive conHeur = new Phase3_Constructive(instance, mip.getSolution()); 
		
//		Phase3 colGen = new Phase3(instance, dailyRestMin, restDayMin);
//		colGen.executeColumnGeneration();
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
				reserveDutyTypes, fromRDutyNrToRDuty, dutyTypes);

	}
}
