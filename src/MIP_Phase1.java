import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.concert.*;
import ilog.cplex.*;
import ilog.cplex.IloCplex;

public class MIP_Phase1 
{
	private IloCplex cplex;
	
	//Class input 
	private final Instance instance; 
	private final Set<String> dutyTypes;
	
	// Parameters
	private final HashMap<ContractGroup, Integer> weeksPerGroup;
	
	//Decision Variables
	private HashMap<ContractGroup, IloNumVar[]> restDaysPerGroup;
	private HashMap<Combination, Set<IloNumVar>> dutyAssignmentCombis;
	private HashMap<ContractGroup, ArrayList<Set<IloNumVar>>> daysPerGroup;

	private HashMap<IloNumVar, Combination> decVarToCombination;
	
	public MIP_Phase1(Instance instance, Set<String> dutyTypes) throws IloException {
		this.cplex = new IloCplex();
		
		this.instance = instance;
		this.dutyTypes = dutyTypes; 	
		
		this.instance.setNrDrivers(this.instance.getLB()); //Setting the number of drivers to the upper bound
		this.daysPerGroup = new HashMap<>(); //List of days for this group, 
		this.weeksPerGroup = new HashMap<>(); //Short map for the amount of weeks per group 
		for(ContractGroup group : instance.getContractGroups()) {
			System.out.println("Drivers: " + group.getTc()/7);
			
			//Create an empty set for every day. This is what the decision variables can go into
			ArrayList<Set<IloNumVar>> days = new ArrayList<>();
			for(int t = 0; t < group.getTc(); t++) { 
				Set<IloNumVar> emptySet = new HashSet<>();
				days.add(emptySet);
			}
			daysPerGroup.put(group, days);
			weeksPerGroup.put(group, group.getTc()/7);
		}
		
		initVars(); //Initialize variables
		initConstraint1(); //Max one duty per day
		initConstraint2(); //All combinations ticked off IS CURRENTLY THE CONSTRAINT THAT MAKES IT INFEASIBLE 
		initConstraint3(); //Max one meal duty per two weeks
		initConstraint4(); //ATV duties | SHOULD TRY TURNING THIS ONE OFF 
		initConstraint5(); //Rest of 11 hours
		initConstraint6(); //Minimum of one rest day per two weeks
		initConstraint7(); //Rest of 32 hours
		initConstraint8(); //Sunday maximum 
		initObjective();
		
		//System.out.println(this.cplex.getModel());
		this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
		this.cplex.exportModel("MIP_Phase1.lp");
		solve();
		System.out.println("Objective Value: " + this.cplex.getObjValue());
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
	
	public void initVars() throws IloException { //Initializing the variables
		//Create new Hashmaps
		this.dutyAssignmentCombis = new HashMap<>();
		this.decVarToCombination = new HashMap<>();
		
		//Creating a decision variable for every combination of dayType + dutyType
		for(Combination combination : this.instance.getM()) {
			this.dutyAssignmentCombis.put(combination, new HashSet<>());
			for(ContractGroup group : this.instance.getContractGroups()) { //For every contract group 
				
				if(group.getDutyTypes().contains(combination.getType())) { //If this contract group can work this duty type 
					
					for(int i = 0; i < this.weeksPerGroup.get(group); i++) { //For every week the group can do it
						
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
				this.cplex.addLe(constraint, 1, name);
			}			
		}
	}
	
	public void initConstraint2() throws IloException { //All combinations should be included the right number of times 
		for(Combination combination : this.instance.getM()) { //For all combinations
			IloLinearNumExpr constraint = this.cplex.linearNumExpr(); 
			for(IloNumVar decisionVar : this.dutyAssignmentCombis.get(combination)) { //All decision variables for that combination
				constraint.addTerm(decisionVar, 1);
			}
			this.cplex.addGe(constraint, combination.getN(), combination.toString());
		}
	}
	
	public void initConstraint3() throws IloException { // Meal duties, at most one per week
		for (ContractGroup group : this.instance.getContractGroups()) { // For all groups
			
			for (int w = 0; w <= this.weeksPerGroup.get(group) - 2; w++) { // For all weeks, minus 2
				
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				
				for (int t = (7 * w); t <= ((7 * (w + 2)) - 1); t++) { // From the Sunday of this week until the Saturday before two weeks later
					
					//If there is a duty of type M, add it 
					IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "M");
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
				this.cplex.addGe(constraint, group.getATVc(), "ATV" + group.groupNumberToString());
			}			
		}
	}

	public void initConstraint5() throws IloException { // Rest of 11 hours
		for (Violation violation : this.instance.getViolations11()) { // For all violations of more than 11 hours

			for (ContractGroup group : this.instance.getContractGroups()) { // For all contract groups

				// Get a set of all the days in this contract group the violation can apply to
				Set<Integer> daysToCover = this.daysToCover(violation.getDayTypeFrom(), this.weeksPerGroup.get(group));
				for (Integer t : daysToCover) {

					if ((t + 1) < this.daysPerGroup.get(group).size()) {
						// Find a decVar with the right type in the set
						IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t),
								violation.getTypeFrom());
						if (decVar != null) { // if we found one
							// Find another decVar with the right type in the set of the next day
							IloNumVar decVarNext = this.decVarOfThisType(this.daysPerGroup.get(group).get(t + 1),
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
	
	public void initConstraint6() throws IloException { //At least one rest or ATV day per week
		for(ContractGroup group : this.instance.getContractGroups()) {// For all contract groups
			for(int w = 0; w <= this.weeksPerGroup.get(group) - 1 ; w++) { //For every week
				
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				for(int t = 7*w; t <= (7*(w+1))-1; t++) {//Sum all days of the week
					constraint.addTerm(this.restDaysPerGroup.get(group)[t],1); //Add the rest day 
					
					
					if(group.getDutyTypes().contains("ATV")) { //If this group can work ATV
						IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "ATV"); 
						if(decVar != null) {//This check should be redundant but better safe than sorry
							constraint.addTerm(decVar, 1);;
						}
					}
				}
				this.cplex.addGe(constraint,1, "Rest per week" + w + "," + group.groupNumberToString());
			}
		}
	}
	
	public void initConstraint7() throws IloException { // Rest of 32 hours
		for (Violation violation : this.instance.getViolations32()) {//For all violations
			for (ContractGroup group : this.instance.getContractGroups()) { // For all contract groups
				
				// Get a set of all the days in this contract group the violation can apply to
				Set<Integer> daysToCover = this.daysToCover(violation.getDayTypeFrom(), this.weeksPerGroup.get(group));
				
				for (Integer t : daysToCover) { // Go over all days
					
					//Find a decVar with this type 
					IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t),
							violation.getTypeFrom());
					
						// If we found one
						if (decVar != null) {
							// Look for the right dutyTo type in two days from now
							if ((t + 2) < this.daysPerGroup.get(group).size()) { //So we don't go out of bounds on a Friday -> Sunday transition
								for (IloNumVar decVarNext : this.daysPerGroup.get(group).get(t + 2)) {
									if (this.decVarToCombination.get(decVarNext).getType()
											.equals(violation.getTypeTo())) {
										// If we have found it, add a constraint
										IloLinearNumExpr constraint = this.cplex.linearNumExpr();
										constraint.addTerm(decVar, 1); // The current day
										constraint.addTerm(this.restDaysPerGroup.get(group)[t + 1], 1); // The rest day
										// Find an ATV day, if there is one
										for (IloNumVar decVarATV : this.daysPerGroup.get(group).get(t + 1)) {
											if (this.decVarToCombination.get(decVarATV).getType().equals("ATV")) {
												constraint.addTerm(decVar, 1);
											}
										}
										constraint.addTerm(decVarNext, 1); // The day two days from now
										this.cplex.addLe(constraint, 2); // Add the constraint
									}
								}
							}
							// continue;
						}
				}
			}
		}
	}
	
	public void initConstraint8() throws IloException { //Maximum of sundays (3/4 of the time)
		for(ContractGroup group : this.instance.getContractGroups()) { //For every contract group
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			for(int w = 0; w < this.weeksPerGroup.get(group); w++) { //Summing all groups
				for(IloNumVar decVar : this.daysPerGroup.get(group).get(w*7)) {
					constraint.addTerm(decVar, 1);
				}				
			}
			int numberOfSundays = (int) (Math.floor((this.weeksPerGroup.get(group) * 3/4)));
			this.cplex.addLe(constraint, numberOfSundays);
		}
		
	}
	
	public void initConstraintX() throws IloException {
		
		
	}
	
	
	public void initObjective() throws IloException {
		//Edit this later 
		IloLinearNumExpr objective = this.cplex.linearNumExpr();
		for(ContractGroup group : this.instance.getContractGroups()) {
			IloNumVar[] variables = restDaysPerGroup.get(group);
			for(int i = 0; i < variables.length; i++) {
				objective.addTerm(variables[i], 1);
			}			
		}
		this.cplex.addMinimize(objective);
	}
	
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
	
	public IloNumVar decVarOfThisType(Set<IloNumVar> set, String string) {
		IloNumVar returnVar = null;
		for(IloNumVar decVar : set) {
			if(this.decVarToCombination.get(decVar).getType().equals(string)) {
				returnVar = decVar;
				break;
			}
		}
		return returnVar;
	}
}
