import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import Tools.Combination;
import Tools.ContractGroup;
import Tools.Instance;
import Tools.Violation;
import Tools.Violation3Days;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import ilog.cplex.IloCplex.UnknownObjectException;

/**
 * This class contains the MIP used in Phase 1, as described in the report. 
 * @author Cynthia Luijkx
 *
 */

public class MIP_Phase1 
{
	private IloCplex cplex;
	
	//Class input 
	private final Instance instance; 
	private final Set<String> dutyTypes;
	
	//Decision Variables
	private HashMap<ContractGroup, IloNumVar[]> restDaysPerGroup;
	private HashMap<Combination, Set<IloNumVar>> dutyAssignmentCombis;
	private HashMap<ContractGroup, ArrayList<Set<IloNumVar>>> daysPerGroup;

	//Tools
	private HashMap<IloNumVar, Combination> decVarToCombination;
	
	//Penalty variables
	private Set<IloNumVar> ATVSpreadPenalty;
	private Set<IloNumVar> reservePenalty;
	private Set<IloNumVar> tooManyConsecutiveDuties;
	private Set<IloNumVar> consecutiveMaxPenalty;
	private Set<IloNumVar> consecutiveMinPenalty;
	private Set<IloNumVar> consecutiveRest;
	private Set<IloNumVar> earlyToLate;
	private Set<IloNumVar> fivePerWeek;
	private Set<IloNumVar> fivePerWeek40;
	private Set<IloNumVar> ATVonWeekend;
	private Set<IloNumVar> lonelyDuty;
	private Set<IloNumVar> splitDuties;
	private Set<IloNumVar> RestSpread;
	private Set<IloNumVar> reserveDivision;
	
	//Penalties not in the penalty
	private final double splitPenalty = 1;
	private final double reserveSpreadPenalty = 1;
	
	//Output
	private final HashMap<ContractGroup, String[]> solution;
	
	public MIP_Phase1(Instance instance, Set<String> dutyTypes) throws IloException {
		this.cplex = new IloCplex();
		
		this.instance = instance;
		this.dutyTypes = dutyTypes; 	
		
		this.daysPerGroup = new HashMap<>(); //List of days for this group, 
		for(ContractGroup group : instance.getContractGroups()) {
			System.out.println("Drivers: " + group.getTc()/7);
			
			//Create an empty set for every day. This is what the decision variables can go into
			ArrayList<Set<IloNumVar>> days = new ArrayList<>();
			for(int t = 0; t < group.getTc(); t++) { 
				Set<IloNumVar> emptySet = new HashSet<>();
				days.add(emptySet);
			}
			daysPerGroup.put(group, days);
		}
		
		initVars(); //Initialize variables
		
		//Hard constraints
		initConstraint1(); //Max one activity per day
		initConstraint2(); //All combinations ticked off 
		initConstraint3(); //Max one meal duty per two weeks
		initConstraint4(); //ATV duties 
		initConstraint5(); //Rest of 11 hours violations
		initConstraint6(); //Minimum of one rest day per week
		initConstraint7(); //Rest of 32 hours violations
		initConstraint8(); //Sunday maximum 
		initConstraint9(); //3 day violations
		initConstraint10(); //Average hours should not exceed the contract hours 
		
		//Soft constraints 
		initSoft1(); //Even spread of ATV days 
		initSoft2(); //Reserve per week 
		initSoft3(); //Maximum of 5 consecutive duties of all types
		initSoft4(); //Max consecutive similar duties
		initSoft5(); //Min consecutive similar duties
		initSoft6(); //Min consecutive rest + ATV 
		initSoft7(); //Early to late duty 
		initSoft9(); //Maximum of 5 duties per calendar week on average
		initSoft10(); //Penalize ATV days on weekends 
		initSoft11(); //Penalize lone duties
		initSoft12(); //Average of 2 split duties per week
		initSoft13(); //Even spread of rest (no more than 3 per week)
		initSoft14(); //Even spread of reserve duties between contract groups 
		
		initObjective();
		
		//System.out.println(this.cplex.getModel());
		this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
		this.cplex.exportModel("MIP_Phase1.lp");
		//this.cplex.setOut(null);	
		
		this.solution = new HashMap<>();
	}
	
	public void clearModel() throws IloException {
		this.cplex.clearModel();
		this.cplex.end();
		this.cplex.endModel();
	}
	
	public void solve() throws IloException {
		this.cplex.solve();
		if(this.isFeasible()) {
			System.out.println("Objective Value: " + this.cplex.getObjValue());
		}
	}
	
	//Source: https://orinanobworld.blogspot.com/2013/01/finding-all-mip-optima-cplex-solution.html
	//Source: https://www.ibm.com/support/knowledgecenter/SSSA5P_12.7.1/ilog.odms.cplex.help/CPLEX/OverviewAPIs/topics/Soln_pool.html
	public int populate(int nsol) throws IloException{
		this.cplex.setParam(IloCplex.IntParam.SolnPoolCapacity, nsol);
		this.cplex.setParam(IloCplex.IntParam.SolnPoolReplace, 1);
		//this.cplex.setParam(IloCplex.DoubleParam.SolnPoolGap, 0);
		this.cplex.setParam(IloCplex.DoubleParam.SolnPoolAGap, 0.5);
		//this.cplex.setParam(IloCplex.IntParam.SolnPoolIntensity, 1);
		this.cplex.setParam(IloCplex.IntParam.PopulateLim, nsol);
		this.cplex.setParam(IloCplex.DoubleParam.TimeLimit, 600);
		this.cplex.populate();
		System.out.println("Solution pool size: " + cplex.getSolnPoolNsolns());
		return cplex.getSolnPoolNsolns();
	}
	
	public boolean isFeasible() throws IloException {
		return this.cplex.isPrimalFeasible();
	}
	
	public HashMap<ContractGroup, String[]> getSolution() {
		return this.solution;
	}
	
	public void makeSolution(int i) throws UnknownObjectException, IloException{
		this.solution.clear();
		for(ContractGroup group : this.instance.getContractGroups()) {
			String[] solutionArray = new String[group.getTc()];
			//System.out.println(group.toString());
			for(int t = 0; t < solutionArray.length; t++) {
				/*
				if(t % 7 == 0 && t > 0) {
					System.out.println(" ");
				}*/
				for(IloNumVar decVar : this.daysPerGroup.get(group).get(t)) {
					if(this.cplex.getValue(decVar, i) > 0) {
						solutionArray[t] = this.decVarToCombination.get(decVar).getType();
				//		System.out.print(solutionArray[t] + " ");
					}
				}
				if(this.cplex.getValue(this.restDaysPerGroup.get(group)[t], i) > 0) {
					solutionArray[t] = "Rest";
				//	System.out.print(solutionArray[t] + " ");
				}
			}
			//System.out.println("");
			//System.out.println("--------------");
			this.solution.put(group, solutionArray);
		}
	}
	
	public void initVars() throws IloException { //Initializing the variables
		//Create new Hashmaps
		this.dutyAssignmentCombis = new HashMap<>();
		this.decVarToCombination = new HashMap<>();
		
		//Creating a decision variable for every combination of dayType + dutyType
		for(Combination combination : this.instance.getM()) {
			this.dutyAssignmentCombis.put(combination, new HashSet<>());
			for(ContractGroup group : this.instance.getContractGroups()) { //For every contract group 
				
				if(group.getDutyTypes().contains(combination.getType())) { //If this contract group can work this duty type 
					for(int i = 0; i < group.getTc()/7; i++) { //For every week the group can do it
						
						IloNumVar newOption = this.cplex.boolVar(); //0, 1 boolean variable
						
						this.dutyAssignmentCombis.get(combination).add(newOption); //Add it to the combination decision variables
						
						this.decVarToCombination.put(newOption, combination); //Map decVar to the combination so we can easily map it back later
						
						//Add it to the list of duties this day can take on 
						int dayNumber = this.dayNumber(combination.getDayType());
						
						this.daysPerGroup.get(group).get((i*7) + dayNumber).add(newOption);
						
						newOption.setName("Day" + ((i*7) + dayNumber) + "," + group.groupNumberToString() + " , " + combination.toString());
					}
				}
			}
		}
		
		//Create a decision variable for every rest day
		this.restDaysPerGroup = new HashMap<>();
		for(ContractGroup group : this.instance.getContractGroups()) {
			IloNumVar[] restDays = this.cplex.boolVarArray(this.daysPerGroup.get(group).size());
			for(int i = 0; i < restDays.length; i++) {
				restDays[i].setName("Restday" + i);
			}
			this.restDaysPerGroup.put(group, restDays);
		}
		
		//Penalty/reward variables
		this.ATVSpreadPenalty = new HashSet<>();
		this.reservePenalty = new HashSet<>();
		this.tooManyConsecutiveDuties = new HashSet<>();
		this.consecutiveMaxPenalty = new HashSet<>();
		this.consecutiveMinPenalty = new HashSet<>();
		this.consecutiveRest = new HashSet<>();
		this.earlyToLate = new HashSet<>();
		this.fivePerWeek = new HashSet<>();
		this.fivePerWeek40 = new HashSet<>();
		this.ATVonWeekend = new HashSet<>();
		this.lonelyDuty = new HashSet<>();
		this.splitDuties = new HashSet<>();
		this.RestSpread = new HashSet<>();
		this.reserveDivision = new HashSet<>();
	}
	
	public void initConstraint1() throws IloException { //Max one activity per day
		for(ContractGroup group : this.instance.getContractGroups()) { //For every contract group 
			
			for(int t = 0; t < this.daysPerGroup.get(group).size(); t++) { //For every day
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				
				//Add all decision variables of day t
				for(IloNumVar decisionVar : this.daysPerGroup.get(group).get(t)) {
					constraint.addTerm(decisionVar, 1);
				}
				//Add the rest option of day t
				constraint.addTerm(restDaysPerGroup.get(group)[t], 1);
				
				//Name the constraint
				String name = "Day" + t + group.groupNumberToString();
				
				//Add it
				this.cplex.addEq(constraint, 1, name);
			}			
		}
	}
	
	public void initConstraint2() throws IloException { // All combinations should be included the right number of times
		for (Combination combination : this.instance.getM()) { // For all combinations
			if (!combination.getType().equals("ATV")) { //Exclude ATV, this is covered in another constraint
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				for (IloNumVar decisionVar : this.dutyAssignmentCombis.get(combination)) { // All decision variables for
																							// that combination
					constraint.addTerm(decisionVar, 1);
				}
				this.cplex.addEq(constraint, combination.getN(), combination.toString());
			}
		}
	}
	
	public void initConstraint3() throws IloException { // Meal duties, at most one per week
		for (ContractGroup group : this.instance.getContractGroups()) { // For all groups
			
			for (int w = 0; w < group.getTc()/7; w++) { // For all weeks
				
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				
				for(int t = 0; t < 14; t++) {//For this week and the week afterwards
					IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(((7*w)+t)%group.getTc()), "M");
					if(decVar != null) {
						constraint.addTerm(decVar, 1);
					}
				}
				
				this.cplex.addLe(constraint, 1, "Meal" + w + "," + (w+1));
			}
		}
	}
	
	public void initConstraint4() throws IloException { // ATV days requirement

		for (ContractGroup group : this.instance.getContractGroups()) { // For all contract groups
			if (group.getDutyTypes().contains("ATV")) { //If the group can work ATV 
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();

				for (int t = 0; t < group.getTc(); t++) { // Summing over all days

					// Get the variable of type ATV
					IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "ATV");
					if (decVar != null) {
						constraint.addTerm(decVar, 1); //Add it 
					}
				}
				this.cplex.addEq(constraint, group.getATVc(), "ATV" + group.groupNumberToString());
			}			
		}
	}

	public void initConstraint5() throws IloException { // Rest of 11 hours
		for (Violation violation : this.instance.getViolations11()) { // For all violations of more than 11 hours
			for (ContractGroup group : this.instance.getContractGroups()) { // For all contract groups
				// Get a set of all the days in this contract group the violation can apply to
				Set<Integer> daysToCover = this.daysToCover(violation.getDayTypeFrom(), group.getTc() / 7);
				for (Integer t : daysToCover) {
					int nextDay = (t + 1) % group.getTc();
					
					//Check whether the next dayType is correct 
					boolean feasible = true;
					if (violation.getDayTypeTo().equals("Sunday") && nextDay%7 != 0) {
						feasible = false;
					}
					if (violation.getDayTypeTo().equals("Saturday") && nextDay%7 != 6) {
						feasible = false;
					}
					if (violation.getDayTypeTo().equals("Workingday") && (nextDay%7 == 0 || nextDay%7 == 6)) {
						feasible = false;
					}
					if (feasible) {
						// Find a decVar with the right type in the set
						IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t),
								violation.getTypeFrom());
						if (decVar != null) { // if we found one
							// Find another decVar with the right type in the set of the next day
							IloNumVar decVarNext = this.decVarOfThisType(this.daysPerGroup.get(group).get(nextDay),
									violation.getTypeTo());
							if (decVarNext != null) {
								// If we have found it, add a constraint
								IloLinearNumExpr constraint = this.cplex.linearNumExpr();
								constraint.addTerm(decVar, 1);
								constraint.addTerm(decVarNext, 1);
								this.cplex.addLe(constraint, 1,
										group.groupNumberToString() + " " + violation.toString());
							}
						}
					}
				}
			}
		}
	}
	
	public void initConstraint6() throws IloException { //At least one rest or ATV day per 7days
		for(ContractGroup group : this.instance.getContractGroups()) {// For all contract groups
			for(int i = 0; i < group.getTc(); i++) { //For every period of 7 days
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				for(int t = 0; t < 7; t++) {
					constraint.addTerm(this.restDaysPerGroup.get(group)[(i+t)%group.getTc()],1); //Add the rest day 	
					if(group.getDutyTypes().contains("ATV")) { //If this group can work ATV
						IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get((i+t)%group.getTc()), "ATV"); 
						if(decVar != null) {//This check should be redundant but better safe than sorry
							constraint.addTerm(decVar, 1);;
						}
					}
				}
				this.cplex.addGe(constraint,1, "Rest per week" + i + "," + group.groupNumberToString());
			}
		}
	}
	
	public void initConstraint7() throws IloException { // Rest of 32 hours
		for (Violation violation : this.instance.getViolations32()) {// For all violations
			for (ContractGroup group : this.instance.getContractGroups()) { // For all contract groups

				// Get a set of all the days in this contract group the violation can apply to
				Set<Integer> daysToCover = this.daysToCover(violation.getDayTypeFrom(), group.getTc() / 7);

				for (Integer t : daysToCover) { // Go over all days				

					boolean feasible = true;
					//Check whether the dayType is correct
					if (violation.getDayTypeTo().equals("Sunday") && (t+2)%7 != 0) {
						feasible = false;
					}
					if (violation.getDayTypeTo().equals("Saturday") && (t+2)%7 != 6) {
						feasible = false;
					}
					if (violation.getDayTypeTo().equals("Workingday") && ((t+2)%7 == 0 || (t+2)%7 == 6)) {
						feasible = false;
					}
					
					if (feasible) {
						// Find a decVar with this type
						IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t),
								violation.getTypeFrom());

						// If we found one
						if (decVar != null) {
							// Look for the right dutyTo type in two days from now
							IloNumVar decVarTwoDays = this.decVarOfThisType(this.daysPerGroup.get(group).get((t+2)%group.getTc()),
									violation.getTypeTo());
							if (decVarTwoDays != null) {
								// If we have found it, add a constraint
								IloLinearNumExpr constraint = this.cplex.linearNumExpr();
								
								constraint.addTerm(decVar, 1); // The current day
								
								constraint.addTerm(this.restDaysPerGroup.get(group)[(t+1)%group.getTc()], 1); // The rest day

								// Find an ATV day, if there is one
								if (group.getDutyTypes().contains("ATV")) {
									IloNumVar decVarATV = this
											.decVarOfThisType(this.daysPerGroup.get(group).get((t+1)%group.getTc()), "ATV");
									if (decVarATV != null) { // Should be redundant
										constraint.addTerm(decVarATV, 1);
									}
								}
								constraint.addTerm(decVarTwoDays, 1); // The day two days from now
								// Add the constraint
								this.cplex.addLe(constraint, 2,
										group.groupNumberToString() + " " + violation.toString());
							}
						}
					}
				}
			}
		}
	}
	
	public void initConstraint8() throws IloException { //Maximum of Sundays (3/4 of the time)
		for(ContractGroup group : this.instance.getContractGroups()) { //For every contract group
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			for(int w = 0; w < group.getTc()/7; w++) { //Summing all weeks
				for(IloNumVar decVar : this.daysPerGroup.get(group).get(w*7)) {//All decision variables on sunday
					if(!this.decVarToCombination.get(decVar).getType().equals("ATV")) {
						constraint.addTerm(decVar, 1);
					}
				}				
			}
			int numberOfSundays = (int) (Math.floor((group.getTc()/7 * 3/4))); //Rounding down 
			this.cplex.addLe(constraint, numberOfSundays, "Sundays" + group.groupNumberToString());
		}
	}
	
	public void initConstraint9() throws IloException {// 3 day violations 
		for (Violation3Days violation : this.instance.getViolations3Days()) {// For all violations
			String[] dayTypes = violation.getDayTypes();
			String[] dutyTypes = violation.getDutyTypes();
			for (ContractGroup group : this.instance.getContractGroups()) { // For all contract groups

				// Get a set of all the days in this contract group the violation can apply to
				Set<Integer> daysToCover = this.daysToCover(dayTypes[0], group.getTc() / 7);

				for (Integer t : daysToCover) { // Go over all days

					// Ensuring the connection from end to start
					boolean feasible = true;
					
					if(dayTypes[1].equals("Sunday") && (t+1)%7 !=0) {
						feasible = false;
					}
					if(dayTypes[1].equals("Saturday") && (t+1)%7 != 6) {
						feasible = false;
					}
					if(dayTypes[1].equals("Workingday") && ((t+1)%7 == 0 || (t+1)%7 ==6)){
						feasible = false;
					}
					if(dayTypes[2].equals("Sunday") && (t+2)%7 !=0) {
						feasible = false;
					}
					if(dayTypes[2].equals("Saturday") && (t+2)%7 != 6) {
						feasible = false;
					}

					if(dayTypes[2].equals("Workingday") && ((t+2)%7 == 0 || (t+2)%7 ==6)){
						feasible = false;
					}

					if (feasible) {
						// Find a decVar with this type
						IloNumVar decVar0 = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), dutyTypes[0]);
						IloNumVar decVar1 = this.decVarOfThisType(this.daysPerGroup.get(group).get((t+1)%group.getTc()),
								dutyTypes[1]);
						IloNumVar decVar2 = this.decVarOfThisType(this.daysPerGroup.get(group).get((t+2)%group.getTc()),
								dutyTypes[2]);

						// If we found them all 
						if (decVar0 != null && decVar1 != null && decVar2 != null) {
							IloLinearNumExpr constraint = this.cplex.linearNumExpr();
							constraint.addTerm(decVar0, 1); // The First day
							constraint.addTerm(decVar1, 1); // The second day
							constraint.addTerm(decVar2, 1); // The third day

							this.cplex.addLe(constraint, 2, group.groupNumberToString() + " " + violation.toString());
						}
					}
				}
			}
		}
	}
	
	public void initConstraint10() throws IloException { //Don't exceed contract hours over the entire period 
		for (ContractGroup group : this.instance.getContractGroups()) {
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			for (int t = 0; t < group.getTc(); t++) {

				for (IloNumVar decVar : this.daysPerGroup.get(group).get(t)) {// Loop over all decVars
					// ATV days
					String type = this.decVarToCombination.get(decVar).getType();
					if (type.equals("ATV")) {
						constraint.addTerm(decVar, (int) group.getAvgHoursPerDay()*60);
					}

					// Normal duties
					else {
						Character ch = type.charAt(0);
						// Normal duties
						if (!ch.equals('R')) {
							if (t % 7 == 0) {// Sunday
								constraint.addTerm(decVar, this.instance.getAvgMinSun().get(type));
							} else if (t % 7 == 6) {// Saturday
								constraint.addTerm(decVar, this.instance.getAvgMinSat().get(type));
							} else {// Working days
								constraint.addTerm(decVar, this.instance.getAvgMinW().get(type));
							}
						}

						// Reserve duties
						if (ch.equals('R')) {
							constraint.addTerm(decVar, (int) group.getAvgHoursPerDay()*60);
						}
					}
				}
			}
			int rhs = (int) (60*group.getAvgHoursPerDay() * group.getAvgDaysPerWeek() * ((group.getTc() / 7)));
			cplex.addLe(constraint, rhs);
		}
	}
	
	public void initSoft1() throws IloException { // ATV spread
		for (ContractGroup group : this.instance.getContractGroups()) { //For all contract groups
			if (group.getDutyTypes().contains("ATV")) { //If this group can work ATV duties
				for (int w = 0; w < group.getTc()/7; w++) {//For all weeks
					IloLinearNumExpr constraint = this.cplex.linearNumExpr();
					
					for(int t = 0; t < 14; t++) {
						IloNumVar decVarATV = this.decVarOfThisType(this.daysPerGroup.get(group).get(((7*w)+t)%group.getTc()), "ATV");
						constraint.addTerm(decVarATV, 1);
					}

					//Add a penalty 
					IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
					penalty.setName(group.groupNumberToString() + "ATVSpread_W" + w);
					this.ATVSpreadPenalty.add(penalty);
					constraint.addTerm(penalty, -1);
					
					this.cplex.addLe(constraint, 1, group.groupNumberToString() + "ATVSpread_W" + w);
				}
			}
		}
	}
	
	public void initSoft2() throws IloException { // Reserve per period of 7 days 
		for (ContractGroup group : this.instance.getContractGroups()) { //For all contract groups

			for (int w = 0; w < group.getTc()/7; w++) { //Starting on every day
				
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				
				for(int t = 0; t < 7; t++) {
					for(IloNumVar decVar : this.daysPerGroup.get(group).get(((7*w)+t)%group.getTc())) {
						String type = this.decVarToCombination.get(decVar).getType();
						Character ch = type.charAt(0);
						if(ch.equals('R')) {
							constraint.addTerm(decVar, 1);
						}
					}
				}
				//Add the penalty
				IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
				penalty.setName(group.groupNumberToString() + "Reserve_W" + w);
				this.reservePenalty.add(penalty);
				
				constraint.addTerm(penalty, -1);
				this.cplex.addLe(constraint, 2, group.groupNumberToString() + "Reserve_W" + w);
			}
		}
	}
	
	public void initSoft3() throws IloException { // Max 5 duties of all types consecutively
		for (ContractGroup group : this.instance.getContractGroups()) {// For all contract groups
			for (int s = 0; s < group.getTc(); s++) { // For all days 

				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				for(int t = 0; t < 6; t++) {//For 6 days from now
					for (IloNumVar decVar : this.daysPerGroup.get(group).get((s+t)%group.getTc())) {
						if (!this.decVarToCombination.get(decVar).getType().equals("ATV")) { // Exclude ATV days
							constraint.addTerm(decVar, 1);
						}
					}
				}

				// Add the penalty
				IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
				penalty.setName(group.groupNumberToString() + "MaxDuties_S" + s);
				this.tooManyConsecutiveDuties.add(penalty);

				constraint.addTerm(penalty, -1);
				this.cplex.addLe(constraint, 5, group.groupNumberToString() + "MaxDuties_S" + s);
			}
		}
	}
	
	public void initSoft4() throws IloException { //Max consecutive similar duties 
		for(ContractGroup group: this.instance.getContractGroups()) {//For all contract groups
			for(int s = 0; s < group.getTc(); s++){ //for all days
				for(String dutyType : dutyTypes) { //for all duty types
					if (dutyType.equals("L") || dutyType.equals("D") || dutyType.equals("V")) {	
						
						IloLinearNumExpr constraint = this.cplex.linearNumExpr();
						
						//Determine what duties we need to count
						Set<String> possibilities = new HashSet<>();
						possibilities.add(dutyType);
						//If it's a regular duty, we also want the reserve
						possibilities.add("R" + dutyType);					
						
						//Add a feasibility check so we don't add unnecessary constraints
						//A constraint would be unnecessary if we can't even have that many consecutive duties of the same type
						boolean feasible = true;
						for(int t = 0; t < 3; t++) {
							boolean added = false;
							for (String option : possibilities) {//See if the options are included
								IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get((s+t)%group.getTc()), option);
								if (decVar != null) {
									constraint.addTerm(decVar, 1);
									added = true;
								}
							}
							if(!added) {
								feasible = false;
							}
						}
						//Add the penalty and initialise the constraint
						if (feasible) {
							IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
							penalty.setName(
									group.groupNumberToString() + "MaxConsecutiveDuties_I" + dutyType + "_S" + s);
							this.consecutiveMaxPenalty.add(penalty);

							constraint.addTerm(penalty, -1);
							this.cplex.addLe(constraint, 3,
									group.groupNumberToString() + "MaxConsecutiveDuties_I" + dutyType + "_S" + s);
						}
					}
				}
			}
		}
	}
	
	public void initSoft5() throws IloException { // Min consecutive similar duties
		for (ContractGroup group : this.instance.getContractGroups()) {// For all contract groups

			for (int s = 0; s < group.getTc(); s++) { // for all days

				for (String dutyType : dutyTypes) { // for all duty types
					if (dutyType.equals("L") || dutyType.equals("D") || dutyType.equals("V")) {

						IloLinearNumExpr constraint = this.cplex.linearNumExpr();

						// Determine what duties we need to count
						Set<String> possibilities = new HashSet<>();
						possibilities.add(dutyType);
						possibilities.add("R" + dutyType);

						// Add a feasibility check so we don't add unnecessary constraints
						// A constraint would be unnecessary if we can't even have that many consecutive
						// duties of the same type
						boolean feasible = true;
						for (int t = 0; t < 2; t++) {
							boolean added = false;
							for (String option : possibilities) {// See if the options are included
								IloNumVar decVar = this.decVarOfThisType(
										this.daysPerGroup.get(group).get((s + t) % group.getTc()), option);
								if (decVar != null) {
									constraint.addTerm(decVar, 1);
									added = true;
								}
							}
							// Add a possible ATV day
							if (group.getDutyTypes().contains("ATV")) {
								IloNumVar decVar = this.decVarOfThisType(
										this.daysPerGroup.get(group).get((s + t) % group.getTc()), "ATV");
								constraint.addTerm(decVar, 1);
							}
							// Add a possible rest day
							constraint.addTerm(this.restDaysPerGroup.get(group)[(s + t) % group.getTc()], 1);
							if (!added) {
								feasible = false;
							}
						}

						if (feasible) {
							IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
							penalty.setName(
									group.groupNumberToString() + "MinConsecutiveDuties_I" + dutyType + "_S" + s);
							this.consecutiveMinPenalty.add(penalty);

							constraint.addTerm(penalty, 1);
							this.cplex.addGe(constraint, 2,
									group.groupNumberToString() + "MinConsecutiveDuties_I" + dutyType + "_S" + s);
						}
					}
				}
			}
		}
	}
	
	public void initSoft6() throws IloException { // Min consecutive rest + ATV
		for (ContractGroup group : this.instance.getContractGroups()) {// For all contract groups

			for (int s = 0; s < group.getTc(); s++) { // for all days
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();

				for (int t = 0; t < 2; t++) {
					//Add the rest day
					constraint.addTerm(this.restDaysPerGroup.get(group)[(s + t) % group.getTc()], 1); 
					// Add an ATV day if we have one
					IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get((s + t) % group.getTc()),
							"ATV");
					if (decVar != null) {
						constraint.addTerm(decVar, 1);
					}
				}

				// Add the reward
				IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
				this.consecutiveRest.add(penalty);
				constraint.addTerm(penalty, 1);
				this.cplex.addGe(constraint, 2, group.groupNumberToString() + "ConsecutiveRest_S" + s);
			}
		}
	}
	
	public void initSoft7() throws IloException { //Penalty for early to late duty
		for (ContractGroup group : this.instance.getContractGroups()) {// For all contract groups

			for (int i = 0; i < group.getTc(); i++) { // for all days
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();

				//Use this array to keep track of whether we actually have the opportunity to add that type of duty on that day
				boolean[] dutyExists = new boolean[2];
				
				//First day
				//Reserve duty
				IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(i), "RV");
				if (decVar != null) {
					constraint.addTerm(decVar, 1);
					dutyExists[0] = true;
				}
				//Actual duty
				decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(i), "V");
				if (decVar != null) {
					constraint.addTerm(decVar, 1);
					dutyExists[0] = true;
				}
				
				
				//Next day
				//Reserve duty
				decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get((i+1)%group.getTc()), "RL");
				if (decVar != null) {
					constraint.addTerm(decVar, 1);
					dutyExists[1] = true;
				}
				//Actual duty
				decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get((i+1)%group.getTc()), "L");
				if (decVar != null) {
					constraint.addTerm(decVar, 1);
					dutyExists[1] = true;
				}
				
				boolean feasible = true;
				for(int a = 0; a < dutyExists.length; a++) {
					if(dutyExists[a] == false) {
						feasible = false;
					}
				}
				if (feasible) {
					IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
					this.earlyToLate.add(penalty);
					constraint.addTerm(penalty, -1);
					this.cplex.addLe(constraint, 1, group.groupNumberToString() + "EarlyToLate_T" + i);
				}
			}
		}
	}

	public void initSoft9() throws IloException { // Max 5 duties per calendar week on average
		for (ContractGroup group : this.instance.getContractGroups()) {// For all contract groups
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			
			for (int t = 0; t < group.getTc(); t++) { // For all days				
					for (IloNumVar decVar : this.daysPerGroup.get(group).get(t)) {
						if(this.decVarToCombination.get(decVar).getType().equals("P")) {
							constraint.addTerm(decVar, 0.6);
						}
						else {
							constraint.addTerm(decVar, 1);
						}
					}				
			}
			if(group.getAvgHoursPerDay() * group.getAvgDaysPerWeek() < 40) {
				// Add the penalty
				IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE);
				penalty.setName(group.groupNumberToString() + "Average5<40");
				this.fivePerWeek.add(penalty);

				constraint.addTerm(penalty, -1);
				this.cplex.addLe(constraint, 5*group.getTc()/7, group.groupNumberToString() + "Average5<40");
			}
			else {
				IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE);
				penalty.setName(group.groupNumberToString() + "Average5=40");
				this.fivePerWeek40.add(penalty);

				constraint.addTerm(penalty, -1);
				this.cplex.addLe(constraint, 5*group.getTc()/7, group.groupNumberToString() + "Average5=40");
			}
		}
	}

	public void initSoft10() throws IloException { //Penalize ATV days on Saturdays and Sundays
		IloLinearNumExpr constraint = this.cplex.linearNumExpr();
		for(ContractGroup group : this.instance.getContractGroups()) {
			if(group.getDutyTypes().contains("ATV")) {
				
				for(int t = 0; t < group.getTc(); t++) {
					if(t%7 == 0 || t%7 == 6) { //If it's a Saturday or a Sunday
						IloNumVar decVarATV = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "ATV");
						constraint.addTerm(decVarATV, 1);
					}
				}
			}
		}
		IloNumVar penalty = this.cplex.intVar(0,  Integer.MAX_VALUE);
		this.ATVonWeekend.add(penalty);
		
		constraint.addTerm(penalty, -1);
		this.cplex.addLe(constraint, 0, "ATV_On_Weekends");
	}
	
	public void initSoft11() throws IloException { //Penalize lonely duties
		for(ContractGroup group : this.instance.getContractGroups()) {
			for(int s = 0; s < group.getTc(); s++) { //For every day 
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				
				//This day a rest day or ATV day
				constraint.addTerm(this.restDaysPerGroup.get(group)[s], 1);
				IloNumVar decVarATV1 = this.decVarOfThisType(this.daysPerGroup.get(group).get(s), "ATV");
				if(decVarATV1 != null) {
					constraint.addTerm(decVarATV1,  1);
				}
				
				//The next day has a duty on it 
				for(IloNumVar decVar : this.daysPerGroup.get(group).get((s+1)%group.getTc())) {
					if(!this.decVarToCombination.get(decVar).getType().equals("ATV")) {
						constraint.addTerm(decVar, 1);
					}
				}
				
				//The day after that is ATV or rest
				constraint.addTerm(this.restDaysPerGroup.get(group)[(s+2)%group.getTc()], 1);
				IloNumVar decVarATV2 = this.decVarOfThisType(this.daysPerGroup.get(group).get((s+2)%group.getTc()), "ATV");
				if(decVarATV2 != null) {
					constraint.addTerm(decVarATV2,  1);
				}
				
				//Penalty
				IloNumVar penalty = this.cplex.intVar(0,  Integer.MAX_VALUE);
				this.lonelyDuty.add(penalty);
				
				constraint.addTerm(penalty,  -1);
				this.cplex.addLe(constraint, 2, "Lonely Duty" + (s+1));
			}
		}
	}
	
	public void initSoft12() throws IloException { //Penalize more than 2 split duties per week on average
		for (ContractGroup group : this.instance.getContractGroups()) {// For all contract groups
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			
			Set<String> possibilities = new HashSet<>();
			possibilities.add("G");
			possibilities.add("RG");
			possibilities.add("GM");
			
			for (int t = 0; t < group.getTc(); t++) { // For all days
				for(String option : possibilities) {
					IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), option);
					if(decVar != null) {
						constraint.addTerm(decVar, 1);
					}
				}
			}
			// Add the penalty
			IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
			penalty.setName(group.groupNumberToString() + "Average2Split");
			this.splitDuties.add(penalty);
			constraint.addTerm(penalty, -1);
			this.cplex.addLe(constraint, (2*group.getTc()/7));
		}
	}
	
	public void initSoft13() throws IloException { // Spread the rest
		for (ContractGroup group : this.instance.getContractGroups()) { // For all contract groups
			for (int w = 0; w < group.getTc()/7; w++) {// For all days
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();

				//The week from that day onwards 
				for (int t = 0; t < 7; t++) {
					IloNumVar decVarATV = this
							.decVarOfThisType(this.daysPerGroup.get(group).get(((7*w) + t) % group.getTc()), "ATV");
					if(decVarATV != null) {
						constraint.addTerm(decVarATV, 1);
					}
					constraint.addTerm(this.restDaysPerGroup.get(group)[((7*w) + t) % group.getTc()], 1);
				}

				// Add a penalty
				IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
				penalty.setName(group.groupNumberToString() + "RestSpread_W" + w);
				this.RestSpread.add(penalty);
				constraint.addTerm(penalty, -1);

				this.cplex.addLe(constraint, 3, group.groupNumberToString() + "RestSpread_W" + w);
			}
		}
	}
	
	public void initSoft14() throws IloException { //Spread the number of reserve duties across groups 
		for(ContractGroup group : instance.getContractGroups()) {
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			for(int t = 0; t < group.getTc(); t++) {
				for(IloNumVar decVar : this.daysPerGroup.get(group).get(t)) {
					String type = this.decVarToCombination.get(decVar).getType();
					Character firstChar = type.charAt(0);
					if(firstChar.equals('R')) {
						constraint.addTerm(decVar, 1);
					}
				}
			}
			int rhs = (int) Math.ceil(instance.getNrReserveDuties() * group.getRelativeGroupSize());
			IloNumVar penalty = this.cplex.intVar(0, Integer.MAX_VALUE);
			penalty.setName(group.groupNumberToString() + "ReserveDivision");
			this.reserveDivision.add(penalty);
			constraint.addTerm(penalty, -1);
			this.cplex.addLe(constraint, rhs);
		}
	}
	
	public void initObjective() throws IloException {
		//Edit this later 
		IloLinearNumExpr objective = this.cplex.linearNumExpr();
		
		for(IloNumVar ATVPenalty : this.ATVSpreadPenalty) {
			objective.addTerm(ATVPenalty, instance.getPenalties().getSoftPenalties()[0]);
		}
		for(IloNumVar reserveP : this.reservePenalty) {
			objective.addTerm(reserveP, instance.getPenalties().getSoftPenalties()[1]);
		}
		for(IloNumVar decVar : this.tooManyConsecutiveDuties) {
			objective.addTerm(decVar, instance.getPenalties().getSoftPenalties()[2]);
		}
		for(IloNumVar five : this.fivePerWeek) {
			objective.addTerm(five,  instance.getPenalties().getSoftPenalties()[3]);
		}
		for(IloNumVar five40 : this.fivePerWeek40) {
			objective.addTerm(five40,  instance.getPenalties().getSoftPenalties()[3]);
		}
		for(IloNumVar atvWeekend : this.ATVonWeekend) {
			objective.addTerm(atvWeekend, instance.getPenalties().getSoftPenalties()[4]);
		}
		for(IloNumVar etl : this.earlyToLate) {
			objective.addTerm(etl, instance.getPenalties().getSoftPenalties()[6]);
		}
		for(IloNumVar rest : this.consecutiveRest) {
			objective.addTerm(rest, instance.getPenalties().getSoftPenalties()[7]);
		}
		for(IloNumVar lonelyDuty : this.lonelyDuty) {
			objective.addTerm(lonelyDuty,  instance.getPenalties().getSoftPenalties()[8]);
		}
		
		for(IloNumVar consecutiveMinP : this.consecutiveMinPenalty) {
			objective.addTerm(consecutiveMinP, instance.getPenalties().getSoftPenalties()[9]);
		}
		
		for(IloNumVar consecutiveMaxP : this.consecutiveMaxPenalty) {
			objective.addTerm(consecutiveMaxP, instance.getPenalties().getSoftPenalties()[10]);
		}
		for(IloNumVar restSpread : this.RestSpread) {
			objective.addTerm(restSpread,  instance.getPenalties().getSoftPenalties()[11]);
		}
		
		for(IloNumVar splitDuty : this.splitDuties) {
			objective.addTerm(splitDuty, this.splitPenalty);
		}
		
		for(IloNumVar reserveDiv : this.reserveDivision) {
			objective.addTerm(reserveDiv, this.reserveSpreadPenalty);
		}
		
		this.cplex.addMinimize(objective);
	}
	
	//Method that returns the number of a day in a week 
	public Integer dayNumber(String string) {
		if(string.equals("Monday")) {
			return 1;
		}
		else if(string.equals("Tuesday")) {
			return 2;
		}
		else if(string.equals("Wednesday")) {
			return 3;
		}
		else if(string.equals("Thursday")) {
			return 4;
		}
		else if(string.equals("Friday")) {
			return 5;
		}
		else if(string.equals("Saturday")) {
			return 6;
		}
		return 0;
	}
	
	//Method that returns all the day numbers of that type 
	public Set<Integer> daysToCover(String string, int weeks) {
		Set<Integer> daysToCover = new HashSet<>();
		if(string.equals("Workingday")){
			for(int w = 0; w < weeks; w++) {
				daysToCover.add((w*7)+1); //Monday
				daysToCover.add((w*7)+2); //Tuesday
				daysToCover.add((w*7)+3); //Wednesday
				daysToCover.add((w*7)+4); //Thursday
				daysToCover.add((w*7)+5); //Friday
			}
		}
		else if(string.equals("Saturday")) {
			for(int w = 0; w < weeks; w++) { 
				daysToCover.add((w*7)+6);
			}			
		}
		else if(string.equals("Sunday")) {
			for(int w = 0; w < weeks; w++) {
				daysToCover.add((w*7));
			}
		}
		return daysToCover;
	}
	
	//Method that returns an IloNumVar out of a set if it the duty type matches. Think about whether we can do this smarter, e.g. with a map
	public IloNumVar decVarOfThisType(Set<IloNumVar> set, String string) {
		for(IloNumVar decVar : set) {
			if(this.decVarToCombination.get(decVar).getType().equals(string)) {
				return decVar;
			}
		}
		return null;
	}
}
