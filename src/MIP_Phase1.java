import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

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
	private final Phase1_Penalties penalties;
	
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
	private Set<IloNumVar> partTime;
	
	//Output
	private final HashMap<ContractGroup, String[]> solution;
	
	public MIP_Phase1(Instance instance, Set<String> dutyTypes, Phase1_Penalties penalties) throws IloException {
		this.cplex = new IloCplex();
		
		this.instance = instance;
		this.dutyTypes = dutyTypes; 	
		this.penalties = penalties;
		
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
		initConstraint1(); //Max one duty per day
		initConstraint2(); //All combinations ticked off 
		initConstraint3(); //Max one meal duty per two weeks
		initConstraint4(); //ATV duties 
		initConstraint5(); //Rest of 11 hours
		initConstraint6(); //Minimum of one rest day per two weeks
		initConstraint7(); //Rest of 32 hours
		initConstraint8(); //Sunday maximum 
		
		//Soft constraints 
		initSoft1(); //Even spread of ATV days 
		initSoft2(); //Reserve per week 
		initSoft3(); //Maximum of 5 consecutive duties of all types
		initSoft4(); //Max consecutive similar duties
		initSoft5(); //Min consecutive similar duties
		initSoft6(); //Min consecutive rest + ATV 
		initSoft7(); //Early to late duty 
		initSoft8(); //Parttimers should work as little other duties as possible 
		
		initObjective();
		
		//System.out.println(this.cplex.getModel());
		this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
		this.cplex.exportModel("MIP_Phase1.lp");
		//this.cplex.setOut(null);
		solve();
		System.out.println("Objective Value: " + this.cplex.getObjValue());
		
		this.solution = new HashMap<>();
		makeSolution();
	}
	
	public void clearModel() throws IloException {
		this.cplex.clearModel();
		this.cplex.end();
		this.cplex.endModel();
	}
	
	public void solve() throws IloException {
		this.cplex.solve();
	}
	
	public boolean isFeasible() throws IloException {
		return this.cplex.isPrimalFeasible();
	}
	
	public HashMap<ContractGroup, String[]> getSolution() {
		return this.solution;
	}
	
	public void makeSolution() throws UnknownObjectException, IloException {
		for(ContractGroup group : this.instance.getContractGroups()) {
			String[] solutionArray = new String[group.getTc()];
			System.out.println(group.toString());
			for(int t = 0; t < solutionArray.length; t++) {
				if(t % 7 == 0 && t > 0) {
					System.out.println(" ");
				}
				for(IloNumVar decVar : this.daysPerGroup.get(group).get(t)) {
					if(this.cplex.getValue(decVar) > 0) {
						solutionArray[t] = this.decVarToCombination.get(decVar).getType();
						System.out.print(solutionArray[t] + " ");
					}
				}
				if(this.cplex.getValue(this.restDaysPerGroup.get(group)[t]) > 0) {
					solutionArray[t] = "Rest";
					System.out.print(solutionArray[t] + " ");
				}
			}
			System.out.println("");
			System.out.println("--------------");
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
		this.partTime = new HashSet<>();
	}
	
	public void initConstraint1() throws IloException { //Max one duty per day
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
			if (!combination.getType().equals("ATV")) { //Exclude ATV 
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
				Set<Integer> daysToCover = new HashSet<>();
				if(w + 2 < group.getTc()/7) { //We don't need to go back to the beginning
					for(int t = (7 * w); t <= ((7 * (w + 2)) - 1); t++) {
						daysToCover.add(t);
					}
				}
				else { //We need to go back to the beginning for all the overflow days
					int overflow = w + 2 - group.getTc()/7;
					for(int t = (7 * w); t < group.getTc(); t++) {
						daysToCover.add(t);
					}
					for(int t = 0; t <= (7*overflow); t++) {
						daysToCover.add(t);
					}
				}
				
				for(Integer day : daysToCover) { // From the Sunday of this week until the Saturday before two weeks later
					
					//If there is a duty of type M, add it 
					IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(day), "M");
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

				for (int t = 0; t < this.daysPerGroup.get(group).size(); t++) { // Summing over all days

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
				Set<Integer> daysToCover = this.daysToCover(violation.getDayTypeFrom(), group.getTc()/7);
				for (Integer t : daysToCover) {
					int nextDay = t+1;
					//If the nextDay is the end, we want to go back to the beginning 
					if (nextDay == this.daysPerGroup.get(group).size()) {
						nextDay = 0;
					}
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
	
	public void initConstraint6() throws IloException { //At least one rest or ATV day per 7days
		for(ContractGroup group : this.instance.getContractGroups()) {// For all contract groups
			for(int i = 0; i < group.getTc(); i++) { //For every period of 7 days
				
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				Set<Integer> daysToCover = new HashSet<>();
				if(i + 6 < group.getTc()) { //We don't need to go back to the beginning
					for(int t = i; t <= i+6; t++) {
						daysToCover.add(t);
					}
				}
				else { //We need to go back to the beginning for all the overflow days
					int overflow = i + 6  - group.getTc();
					for(int t = i; t < group.getTc(); t++) {
						daysToCover.add(t);
					}
					for(int t = 0; t <= overflow; t++) {
						daysToCover.add(t);
					}
				}

				for(Integer day : daysToCover) {//Sum all days of the week
					
					constraint.addTerm(this.restDaysPerGroup.get(group)[day],1); //Add the rest day 
					
					
					if(group.getDutyTypes().contains("ATV")) { //If this group can work ATV
						IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(day), "ATV"); 
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
					
					//Ensuring the connection from end to start 
					boolean feasible = true;
					int nextDay = t + 1;
					int twoDays = t + 2;
					if (twoDays == this.daysPerGroup.get(group).size()) {
						if (violation.getDayTypeTo().equals("Sunday")) { // Otherwise, it's not feasible
							twoDays = 0;
						} else {
							feasible = false;
						}
					} else if (twoDays == this.daysPerGroup.get(group).size() + 1) {
						if (violation.getDayTypeTo().equals("Workingday")) {
							nextDay = 0;
							twoDays = 1;
						} else {
							feasible = false;
						}
					}
					if (feasible) {
						// Find a decVar with this type
						IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t),
								violation.getTypeFrom());

						// If we found one
						if (decVar != null) {
							// Look for the right dutyTo type in two days from now

							IloNumVar decVarNext = this.decVarOfThisType(this.daysPerGroup.get(group).get(twoDays),
									violation.getTypeTo());
							if (decVarNext != null) {
								// If we have found it, add a constraint
								IloLinearNumExpr constraint = this.cplex.linearNumExpr();
								constraint.addTerm(decVar, 1); // The current day
								constraint.addTerm(this.restDaysPerGroup.get(group)[nextDay], 1); // The rest day

								// Find an ATV day, if there is one

								if (group.getDutyTypes().contains("ATV")) {
									IloNumVar decVarATV = this
											.decVarOfThisType(this.daysPerGroup.get(group).get(nextDay), "ATV");
									if (decVarATV != null) { // Should be redundant
										constraint.addTerm(decVarATV, 1);
									}
								}
								constraint.addTerm(decVarNext, 1); // The day two days from now
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
					constraint.addTerm(decVar, 1);
				}				
			}
			int numberOfSundays = (int) (Math.floor((group.getTc()/7 * 3/4))); //Rounding down 
			this.cplex.addLe(constraint, numberOfSundays, "Sundays" + group.groupNumberToString());
		}
	}
	
	public void initSoft1() throws IloException { // ATV spread
		for (ContractGroup group : this.instance.getContractGroups()) { //For all contract groups
			if (group.getDutyTypes().contains("ATV")) { //If this group can work ATV duties
				for (int s = 0; s < group.getTc(); s++) {//For all weeks
					IloLinearNumExpr constraint = this.cplex.linearNumExpr();
					
					//Get all the days we need
					Set<Integer> daysToCover = new HashSet<>();
					if (s + 6 < group.getTc()) { // We don't need to go back to the beginning
						for (int t = s; t <= s + 6; t++) {
							daysToCover.add(t);
						}
					} else { // We need to go back to the beginning for all the overflow days
						int overflow = s + 6 - group.getTc();
						for (int t = s; t < group.getTc(); t++) {
							daysToCover.add(t);
						}
						for (int t = 0; t <= overflow; t++) {
							daysToCover.add(t);
						}
					}

					for (Integer day : daysToCover) { //Sum over all days in the week
						//Find ATV duty and add it
						IloNumVar decVarATV = this.decVarOfThisType(this.daysPerGroup.get(group).get(day), "ATV");
						constraint.addTerm(decVarATV, 1);
					}
					//Add a penalty 
					IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int);
					penalty.setName(group.groupNumberToString() + "ATVSpread_S" + s);
					this.ATVSpreadPenalty.add(penalty);
					constraint.addTerm(penalty, -1);
					
					this.cplex.addLe(constraint, 1, group.groupNumberToString() + "ATVSpread_S" + s);
				}
			}
		}
	}
	
	public void initSoft2() throws IloException { // Reserve per period of 7 days 
		for (ContractGroup group : this.instance.getContractGroups()) { //For all contract groups

			for (int s = 0; s < group.getTc(); s++) { //Starting on every day
				
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				//Find all the days up to 6 ahead of this one
				Set<Integer> daysToCover = new HashSet<>();
				if (s + 6 < group.getTc()) { // We don't need to go back to the beginning
					for (int t = s; t <= s + 6; t++) {
						daysToCover.add(t);
					}
				} else { // We need to go back to the beginning for all the overflow days
					int overflow = s + 6 - group.getTc();
					for (int t = s; t < group.getTc(); t++) {
						daysToCover.add(t);
					}
					for (int t = 0; t <= overflow; t++) {
						daysToCover.add(t);
					}
				}

				for (Integer day : daysToCover) {//Summing all days
					//If a decision variable's duty type starts with an R, add it 
					for(IloNumVar decVar : this.daysPerGroup.get(group).get(day)) {
						String type = this.decVarToCombination.get(decVar).getType();
						Character ch = type.charAt(0);
						if(ch.equals('R')) {
							constraint.addTerm(decVar, 1);
						}
					}
				}
				//Add the penalty
				IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int);
				penalty.setName(group.groupNumberToString() + "Reserve_S" + s);
				this.reservePenalty.add(penalty);
				
				constraint.addTerm(penalty, -1);
				this.cplex.addLe(constraint, 2, group.groupNumberToString() + "Reserve_S" + s);
			}
		}
	}
	
	public void initSoft3() throws IloException { // Max 5 duties of all types consecutively
		for (ContractGroup group : this.instance.getContractGroups()) {// For all contract groups
			for (int s = 0; s < group.getTc(); s++) { // For all days 

				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				//Get all the days we need to cover in the sum 
				Set<Integer> daysToCover = new HashSet<>();
				if (s + 5 < group.getTc()) { // We don't need to go back to the beginning
					for (int t = s; t <= s + 5; t++) {
						daysToCover.add(t);
					}
				} else { // We need to go back to the beginning for all the overflow days
					int overflow = s + 5 - group.getTc();
					for (int t = s; t < group.getTc(); t++) {
						daysToCover.add(t);
					}
					for (int t = 0; t <= overflow; t++) {
						daysToCover.add(t);
					}
				}

				for (Integer day : daysToCover) { // Sum over all days
					for (IloNumVar decVar : this.daysPerGroup.get(group).get(day)) {
						if (!this.decVarToCombination.get(decVar).getType().equals("ATV")) { // Exclude ATV days
							constraint.addTerm(decVar, 1);
						}
					}
				}
				// Add the penalty
				IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int);
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
					
					//if they can work this duty and it's not an ATV duty 
					if(group.getDutyTypes().contains(dutyType) && !dutyType.equals("ATV")) { 
						
						IloLinearNumExpr constraint = this.cplex.linearNumExpr();
						
						//Determine what duties we need to count
						Set<String> possibilities = new HashSet<>();
						possibilities.add(dutyType);
						
						Character firstChar = dutyType.charAt(0);
						
						if(firstChar.equals('R')) {//If it's a reserve duty, we also want the regular one
							possibilities.add(Character.toString(dutyType.charAt(1)));
						}
						else {//If it's a regular duty, we also want the reserve
							possibilities.add("R" + dutyType);
						}
						
						Set<Integer> daysToCover = new HashSet<>();
						if(s + 3 < group.getTc()) { //We don't need to go back to the beginning
							for(int t = s; t <= s+3; t++) {
								daysToCover.add(t);
							}
						}
						else { //We need to go back to the beginning for all the overflow days
							int overflow = s + 3  - group.getTc();
							for(int t = s; t < group.getTc(); t++) {
								daysToCover.add(t);
							}
							for(int t = 0; t <= overflow; t++) {
								daysToCover.add(t);
							}
						}
						//Add a feasibility check so we don't add unnecessary constraints
						//A constraint would be unnecessary if we can't even have that many consecutive duties of the same type
						boolean feasible = true;
						for(Integer day : daysToCover) { //Sum over these elements
							boolean added = false;
							for (String option : possibilities) {//See if the options are included
								IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(day), option);
								if (decVar != null) {
									constraint.addTerm(decVar, 1);
									added = true;
								}
							}
							if(!added) {
								feasible = false;
							}
						}
						//Add the penalty
						if (feasible) {
							IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int);
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
	
	public void initSoft5() throws IloException { //Min consecutive similar duties 
		for(ContractGroup group: this.instance.getContractGroups()) {//For all contract groups
			
			for(int s = 0; s < group.getTc(); s++){ //for all days
				
				for(String dutyType : dutyTypes) { //for all duty types
					
					//if they can work this duty and it's not an ATV duty 
					if(group.getDutyTypes().contains(dutyType) && !dutyType.equals("ATV")) {
						
						IloLinearNumExpr constraint = this.cplex.linearNumExpr();
						
						//Determine what duties we need to count
						Set<String> possibilities = new HashSet<>();
						possibilities.add(dutyType);
						
						Character firstChar = dutyType.charAt(0);
						
						if(firstChar.equals('R')) {//If it's a reserve duty, we also want the regular one
							possibilities.add(Character.toString(dutyType.charAt(1)));
						}
						else {//If it's a regular duty, we also want the reserve
							possibilities.add("R" + dutyType);
						}
						
						//Set of days to count over
						Set<Integer> daysToCover = new HashSet<>();
						if(s + 1 < group.getTc()) { //We don't need to go back to the beginning
							for(int t = s; t <= s+1; t++) {
								daysToCover.add(t);
							}
						}
						else { //We need to go back to the beginning for all the overflow days
							int overflow = s + 1 - group.getTc();
							for(int t = s; t < group.getTc(); t++) {
								daysToCover.add(t);
							}
							for(int t = 0; t <= overflow; t++) {
								daysToCover.add(t);
							}
						}
						
						//Add a feasibility check so we don't add unnecessary constraints
						//A constraint would be unnecessary if we can't even have that many consecutive duties of the same type
						boolean feasible = true;
						for(Integer day : daysToCover) { //Sum over these elements
							for (String option : possibilities) {//See if the options are included
								IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(day), option);
								if (decVar != null) {
									constraint.addTerm(decVar, 1);
									feasible = false;
								}
							}
							//Add a possible ATV day 
							if(group.getDutyTypes().contains("ATV")) {
								IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(day), "ATV");
								constraint.addTerm(decVar, 1);
							}
							//Add a possible rest day
							constraint.addTerm(this.restDaysPerGroup.get(group)[day], 1);
						}

						if (feasible) {
							IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE);
							penalty.setName(group.groupNumberToString() + "MinConsecutiveDuties_I" + dutyType + "_S" + s);
							this.consecutiveMinPenalty.add(penalty);
							
							constraint.addTerm(penalty, 1);
							this.cplex.addGe(constraint, 2, group.groupNumberToString() + "MinConsecutiveDuties_I" + dutyType + "_S" + s);
						}
					}
				}
			}
		}
	}
	
	public void initSoft6() throws IloException { //Min consecutive rest + ATV  
		for(ContractGroup group: this.instance.getContractGroups()) {//For all contract groups
			
			for(int s= 0; s < group.getTc(); s++){ //for all days
						IloLinearNumExpr constraint = this.cplex.linearNumExpr();
						
						Set<Integer> daysToCover = new HashSet<>();
						if(s + 1 < group.getTc()) { //We don't need to go back to the beginning
							for(int t = s; t <= s+1; t++) {
								daysToCover.add(t);
							}
						}
						else { //We need to go back to the beginning for all the overflow days
							int overflow = s + 1 - group.getTc();
							for(int t = s; t < group.getTc(); t++) {
								daysToCover.add(t);
							}
							for(int t = 0; t <= overflow; t++) {
								daysToCover.add(t);
							}
						}

						for(Integer day : daysToCover) {//Sum over these elements
							constraint.addTerm(this.restDaysPerGroup.get(group)[day], 1); //Add the rest day
							//Add an ATV day if we have one
							IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(day), "ATV");
							if(decVar !=null) {
								constraint.addTerm(decVar, 1);
							}
						}
						//Add the reward
							IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE);
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
				IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(i), "RV");
				if (decVar != null) {
					constraint.addTerm(decVar, 1);
					dutyExists[0] = true;
				}
				decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(i), "V");
				if (decVar != null) {
					constraint.addTerm(decVar, 1);
					dutyExists[0] = true;
				}
				
				//In case we go over the amount of days, we go back to 0
				int nextDay = i+1;
				if (nextDay >= group.getTc()) {
					nextDay = 0;
				}
				
				//Next day
				decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(nextDay), "RL");
				if (decVar != null) {
					constraint.addTerm(decVar, 1);
					dutyExists[1] = true;
				}

				decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(nextDay), "L");
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
					IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE);
					this.earlyToLate.add(penalty);
					constraint.addTerm(penalty, -1);
					this.cplex.addLe(constraint, 1, group.groupNumberToString() + "EarlyToLate_T" + i);
				}
			}
		}
	}
	
	public void initSoft8() throws IloException { //Assign as little duties other than part-time to part-timers
		for(ContractGroup group : this.instance.getContractGroups()) {
			if(group.getNr() == 4) {
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				
				for(int t = 0; t < this.daysPerGroup.get(group).size(); t++) { //Sum over all days
					for(IloNumVar decVar : this.daysPerGroup.get(group).get(t)) {
						if(!this.decVarToCombination.get(decVar).getType().equals("P")){//if it's not a P duty
							constraint.addTerm(decVar, 1);
						}
					}
				}
				//Add the penalty
				IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE);
				this.partTime.add(penalty);
				constraint.addTerm(penalty, -1);
				this.cplex.addLe(constraint, 0);
			}
		}
	}
	
	public void initObjective() throws IloException {
		//Edit this later 
		IloLinearNumExpr objective = this.cplex.linearNumExpr();
		
		for(IloNumVar ATVPenalty : this.ATVSpreadPenalty) {
			objective.addTerm(ATVPenalty, -penalties.getATVSpreadPenaltyParam());
		}
		for(IloNumVar reserveP : this.reservePenalty) {
			objective.addTerm(reserveP, -penalties.getReservePenaltyParam());
		}
		for(IloNumVar decVar : this.tooManyConsecutiveDuties) {
			objective.addTerm(decVar, -penalties.getTooManyConsecutiveDutiesParam());
		}
		for(IloNumVar consecutiveMaxP : this.consecutiveMaxPenalty) {
			objective.addTerm(consecutiveMaxP, -penalties.getConsecutiveMaxPenaltyParam());
		}
		
		for(IloNumVar consecutiveMinP : this.consecutiveMinPenalty) {
			objective.addTerm(consecutiveMinP, -penalties.getConsecutiveMinPenaltyParam());
		}
		
		for(IloNumVar rest : this.consecutiveRest) {
			objective.addTerm(rest, -penalties.getConsecutiveRestParam());
		}
		
		for(IloNumVar etl : this.earlyToLate) {
			objective.addTerm(etl, -penalties.getEarlyToLateParam());
		}
		
		for(IloNumVar pt : this.partTime) {
			objective.addTerm(pt, -penalties.getPartTimeParam());
		}
		
		this.cplex.addMaximize(objective);
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
