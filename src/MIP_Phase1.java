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
import ilog.cplex.IloCplex.UnknownObjectException;

public class MIP_Phase1 
{
	private IloCplex cplex;
	
	//Class input 
	private final Instance instance; 
	private final Set<String> dutyTypes;
	
	// Parameters
	private final HashMap<ContractGroup, Integer> weeksPerGroup; //CAN BE REMOVED FOR getTc()/7
	
	//Decision Variables
	private HashMap<ContractGroup, IloNumVar[]> restDaysPerGroup;
	private HashMap<Combination, Set<IloNumVar>> dutyAssignmentCombis;
	private HashMap<ContractGroup, ArrayList<Set<IloNumVar>>> daysPerGroup;

	//Tools
	private HashMap<IloNumVar, Combination> decVarToCombination;
	
	//Penalty variables
	private Set<IloNumVar> ATVSpreadPenalty;
	private Set<IloNumVar> reservePenalty;
	private Set<IloNumVar> tooManyDutiesPenalty;
	private Set<IloNumVar> consecutivePenalty;
	private Set<IloNumVar> consecutiveReward;
	private Set<IloNumVar> consecutiveRest;
	
	//Output
	private final HashMap<ContractGroup, String[]> solution;
	
	public MIP_Phase1(Instance instance, Set<String> dutyTypes) throws IloException {
		this.cplex = new IloCplex();
		
		this.instance = instance;
		this.dutyTypes = dutyTypes; 	
		
		this.instance.setNrDrivers(this.instance.getLB()+10); //Setting the number of drivers to the upper bound
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
		
		//Hard constraints
		initConstraint1(); //Max one duty per day
		initConstraint2(); //All combinations ticked off IS CURRENTLY THE CONSTRAINT THAT MAKES IT INFEASIBLE 
		initConstraint3(); //Max one meal duty per two weeks
		initConstraint4(); //ATV duties | SHOULD TRY TURNING THIS ONE OFF 
		initConstraint5(); //Rest of 11 hours
		initConstraint6(); //Minimum of one rest day per two weeks
		initConstraint7(); //Rest of 32 hours
		initConstraint8(); //Sunday maximum 
		
		//Soft constraints 
		initSoft1();
		initSoft2();
		initSoft3();
		initSoft4();
		initSoft5();
		initSoft6();
		
		initObjective();
		
		//System.out.println(this.cplex.getModel());
		this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
		this.cplex.exportModel("MIP_Phase1.lp");
		solve();
		System.out.println("Objective Value: " + this.cplex.getObjValue());
		
		this.solution = new HashMap<>();
		getSolution();
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
	
	public void getSolution() throws UnknownObjectException, IloException {
		Set<String[]> solutionPerGroup = new HashSet<>();
		for(ContractGroup group : this.instance.getContractGroups()) {
			String[] solutionArray = new String[group.getTc()];
			for(int t = 0; t < solutionArray.length; t++) {
				if(t % 7 == 0) {
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
		
		//Penalty/reward variables
		
		this.ATVSpreadPenalty = new HashSet<>();
		this.reservePenalty = new HashSet<>();
		this.tooManyDutiesPenalty = new HashSet<>();
		this.consecutivePenalty = new HashSet<>();
		this.consecutiveReward = new HashSet<>();
		this.consecutiveRest = new HashSet<>();
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
				this.cplex.addEq(constraint, group.getATVc(), "ATV" + group.groupNumberToString());
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
		for (Violation violation : this.instance.getViolations32()) {// For all violations
			for (ContractGroup group : this.instance.getContractGroups()) { // For all contract groups

				// Get a set of all the days in this contract group the violation can apply to
				Set<Integer> daysToCover = this.daysToCover(violation.getDayTypeFrom(), this.weeksPerGroup.get(group));

				for (Integer t : daysToCover) { // Go over all days
					if ((t + 2) < this.daysPerGroup.get(group).size()) {// So we don't go out of bounds on a Friday -> Sunday transition
						// Find a decVar with this type
						IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t),
								violation.getTypeFrom());

						// If we found one
						if (decVar != null) {
							// Look for the right dutyTo type in two days from now
							
							IloNumVar decVarNext = this.decVarOfThisType(this.daysPerGroup.get(group).get(t+2), violation.getTypeTo());
								if (decVarNext != null) {
									// If we have found it, add a constraint
									IloLinearNumExpr constraint = this.cplex.linearNumExpr();
									constraint.addTerm(decVar, 1); // The current day
									constraint.addTerm(this.restDaysPerGroup.get(group)[t + 1], 1); // The rest day
									
									// Find an ATV day, if there is one
									
									if(group.getDutyTypes().contains("ATV")) {
										IloNumVar decVarATV = this.decVarOfThisType(this.daysPerGroup.get(group).get(t+1), "ATV");
										if (decVarATV != null) { //Should be redundant
											constraint.addTerm(decVarATV, 1);
										}
									}
									constraint.addTerm(decVarNext, 1); // The day two days from now
									// Add the constraint
									this.cplex.addLe(constraint, 2, group.groupNumberToString() + " " + violation.toString()); 
								}
						}
					}
				}
			}
		}
	}
	
	public void initConstraint8() throws IloException { //Maximum of sundays (3/4 of the time)
		for(ContractGroup group : this.instance.getContractGroups()) { //For every contract group
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			for(int w = 0; w < this.weeksPerGroup.get(group); w++) { //Summing all weeks
				for(IloNumVar decVar : this.daysPerGroup.get(group).get(w*7)) {//All decision variables on sunday
					constraint.addTerm(decVar, 1);
				}				
			}
			int numberOfSundays = (int) (Math.floor((this.weeksPerGroup.get(group) * 3/4))); //Rounding down 
			this.cplex.addLe(constraint, numberOfSundays, "Sundays" + group.groupNumberToString());
		}
	}
	
	public void initSoft1() throws IloException { // ATV spread
		//Initialize penalty set 
		this.ATVSpreadPenalty = new HashSet<>();
		for (ContractGroup group : this.instance.getContractGroups()) {
			if (group.getDutyTypes().contains("ATV")) {
				for (int w = 0; w < group.getTc() / 7; w++) {
					IloLinearNumExpr constraint = this.cplex.linearNumExpr();
					for (int t = (7 * w); t <= (7 * (w + 1) - 1); t++) {
						IloNumVar decVarATV = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "ATV");
						if (decVarATV != null) {
							constraint.addTerm(decVarATV, 1);
						}
					}
					IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int);
					penalty.setName(group.groupNumberToString() + "w" + w);
					this.ATVSpreadPenalty.add(penalty);
					constraint.addTerm(penalty, -1);
					this.cplex.addLe(constraint, 1, group.groupNumberToString() + "ATV spread" + w);
				}
			}
		}
	}
	
	public void initSoft2() throws IloException { // Reserve per week
		// Initialize penalty set
		for (ContractGroup group : this.instance.getContractGroups()) {

			for (int w = 0; w < group.getTc() / 7; w++) {
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				for (int t = (7 * w); t <= (7 * (w + 1) - 1); t++) {
					
					//Might want to just loop over all decVars in this case and check
					IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "RD");
					if (decVar != null) {
						constraint.addTerm(decVar, 1);
					}
					decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "RL");
					if (decVar != null) {
						constraint.addTerm(decVar, 1);
					}
					decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "RV");
					if (decVar != null) {
						constraint.addTerm(decVar, 1);
					}
					decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "RG");
					if (decVar != null) {
						constraint.addTerm(decVar, 1);
					}
				}
				IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int);
				penalty.setName(group.groupNumberToString() + "Reserve w" + w);
				this.reservePenalty.add(penalty);
				constraint.addTerm(penalty, -1);
				this.cplex.addLe(constraint, 2, group.groupNumberToString() + "Reserve w" + w);
			}
		}
	}
	
	public void initSoft3() throws IloException { // 5 duties per week
		for(ContractGroup group: this.instance.getContractGroups()) {
			for (int w = 0; w < group.getTc() / 7; w++) {
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				for (int t = (7 * w); t <= (7 * (w + 1) - 1); t++) {
					for(IloNumVar decVar : this.daysPerGroup.get(group).get(t)) {
						if(!this.decVarToCombination.get(decVar).getType().equals("ATV")) {
						constraint.addTerm(decVar, 1);
						}
					}
				}
				IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int);
				penalty.setName(group.groupNumberToString() + "MaxDuties w" + w);
				this.tooManyDutiesPenalty.add(penalty);
				constraint.addTerm(penalty, -1);
				this.cplex.addLe(constraint, 5, group.groupNumberToString() + "MaxDuties w" + w);
			}			
		}
	}
	
	public void initSoft4() throws IloException { //Max consecutive similar duties 
		for(ContractGroup group: this.instance.getContractGroups()) {//For all contract groups
			
			for(int i = 0; i < group.getTc() - 4; i++){ //for all days
				
				for(String dutyType : dutyTypes) { //for all duty types
					
					if(group.getDutyTypes().contains(dutyType)) { //if they can work this duty
						
						IloLinearNumExpr constraint = this.cplex.linearNumExpr();
						
						for(int t = i; t <= i+4; t++) { //Sum over these elements
							IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), dutyType);
							if(decVar !=null) {
								constraint.addTerm(decVar, 1);
							}
						}
						//Add the penalty
						IloNumVar penalty = this.cplex.numVar(0, Integer.MAX_VALUE, IloNumVarType.Int);
						this.consecutivePenalty.add(penalty);
						constraint.addTerm(penalty, -1);
						this.cplex.addLe(constraint, 3);
					}
				}
			}
		}
	}
	
	public void initSoft5() throws IloException { //Min consecutive similar duties 
		for(ContractGroup group: this.instance.getContractGroups()) {//For all contract groups
			
			for(int i = 0; i < group.getTc() - 1; i++){ //for all days
				
				for(String dutyType : dutyTypes) { //for all duty types
					
					if(group.getDutyTypes().contains(dutyType)) { //if they can work this duty
						
						IloLinearNumExpr constraint = this.cplex.linearNumExpr();
						
						int terms = 0; //Need to count because if we don't have duties we could add free rewards
						for(int t = i; t <= i+1; t++) { //Sum over these elements
							IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), dutyType);
							if(decVar !=null) {
								constraint.addTerm(decVar, 1);
								terms = terms +1;
							}
						}
						//Add the reward
						if (terms > 1) {
							IloNumVar reward = this.cplex.numVar(0, 1);
							this.consecutiveReward.add(reward);
							constraint.addTerm(reward, -1);
							this.cplex.addLe(constraint, 1);
						}
					}
				}
			}
		}
	}
	
	public void initSoft6() throws IloException { //Min consecutive rest + ATV  
		for(ContractGroup group: this.instance.getContractGroups()) {//For all contract groups
			
			for(int i = 0; i < group.getTc() - 1; i++){ //for all days
						IloLinearNumExpr constraint = this.cplex.linearNumExpr();
						
						for(int t = i; t <= i+1; t++) { //Sum over these elements
							constraint.addTerm(this.restDaysPerGroup.get(group)[t], 1); //Add the rest day
							//Add an ATV day if we have one
							IloNumVar decVar = this.decVarOfThisType(this.daysPerGroup.get(group).get(t), "ATV");
							if(decVar !=null) {
								constraint.addTerm(decVar, 1);
							}
						}
						//Add the reward
							IloNumVar reward = this.cplex.numVar(0, 1);
							this.consecutiveRest.add(reward);
							constraint.addTerm(reward, -1);
							this.cplex.addLe(constraint, 1);
			}
		}
	}
	
	public void initObjective() throws IloException {
		//Edit this later 
		IloLinearNumExpr objective = this.cplex.linearNumExpr();
		
		for(IloNumVar ATVPenalty : this.ATVSpreadPenalty) {
			objective.addTerm(ATVPenalty, -1);
		}
		for(IloNumVar reserveP : this.reservePenalty) {
			objective.addTerm(reserveP, -1);
		}
		for(IloNumVar tooMany : this.tooManyDutiesPenalty) {
			objective.addTerm(tooMany, -1);
		}
		for(IloNumVar consecutiveP : this.consecutivePenalty) {
			objective.addTerm(consecutiveP, -1);
		}
		
		for(IloNumVar consecutiveR : this.consecutiveReward) {
			objective.addTerm(consecutiveR, 1);
		}
		
		for(IloNumVar rest : this.consecutiveRest) {
			objective.addTerm(rest, 1);
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
