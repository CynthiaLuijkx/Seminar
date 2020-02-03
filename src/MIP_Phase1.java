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
	private final HashMap<ContractGroup, Integer> driversPerGroup;
	private final HashMap<ContractGroup, Integer> weeksPerGroup;
	private final int nrContractGroups;
	
	//Decision Variables
	private HashMap<ContractGroup, IloNumVar[]> restDaysPerGroup;
	private HashMap<Combination, Set<IloNumVar>> dutyAssignmentCombis;
	private HashMap<ContractGroup, Set<IloNumVar>> dutyAssignmentGroups;
	private HashMap<ContractGroup, ArrayList<Set<IloNumVar>>> daysPerGroup;
	
	public MIP_Phase1(Instance instance, Set<String> dutyTypes) throws IloException {
		this.cplex = new IloCplex();
		
		this.instance = instance;
		this.dutyTypes = dutyTypes; 
		
		//Adjust these when we have the driver input 
		this.nrContractGroups = instance.getContractGroups().size();		
		
		this.driversPerGroup = new HashMap<>();
		this.daysPerGroup = new HashMap<>();
		this.weeksPerGroup = new HashMap<>();
		for(ContractGroup group : instance.getContractGroups()) {
			driversPerGroup.put(group, 4);
			ArrayList<Set<IloNumVar>> days = new ArrayList<>();
			for(int i = 0; i < driversPerGroup.get(group) * 7; i++) {
				Set<IloNumVar> emptySet = new HashSet<>();
				days.add(emptySet);
			}
			daysPerGroup.put(group, days);
			weeksPerGroup.put(group, driversPerGroup.get(group));
		}
		
		initVars();
		initConstraint1();
		initConstraint2();
		initConstraint3();
		initObjective();
		
		System.out.println(this.cplex.getModel());
		this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
		//this.cplex.exportModel(null);
		//this.cplex.exportModel("MIP_Phase1.lp");
		solve();
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
	
	public void initVars() throws IloException {
		this.dutyAssignmentCombis = new HashMap<>();
		for(Combination combination : this.instance.getM()) {
			this.dutyAssignmentCombis.put(combination, new HashSet<>());
			
			for(ContractGroup group : this.instance.getContractGroups()) {
				if(group.getDutyTypes().contains(combination.getType())) { //If this contract group can work this duty type 
					int weeks = this.weeksPerGroup.get(group); 
					for(int i = 0; i < weeks; i++) { //For every week the group can do it
						IloNumVar newOption = this.cplex.boolVar(); 
						this.dutyAssignmentCombis.get(combination).add(newOption); //Add it to the combination decision variables
						newOption.setName(group.groupNumberToString() + " " + combination.toString());
						//Add it to the right week
						if(combination.getDayType().equals("Sunday")) {
							this.daysPerGroup.get(group).get((i*7)).add(newOption);
						}
						if(combination.getDayType().equals("Monday")) {
							this.daysPerGroup.get(group).get((i*7 + 1)).add(newOption);
						}
						if(combination.getDayType().equals("Tuesday")) {
							this.daysPerGroup.get(group).get((i*7 + 2)).add(newOption);
						}
						if(combination.getDayType().equals("Wednesday")) {
							this.daysPerGroup.get(group).get((i*7 + 3)).add(newOption);
						}
						if(combination.getDayType().equals("Thurday")) { //NOT SHOWING UP. THERE'S A TYPO SOMEWHERE
							this.daysPerGroup.get(group).get((i*7 + 4)).add(newOption);
						}
						if(combination.getDayType().equals("Friday")) {
							this.daysPerGroup.get(group).get((i*7 + 5)).add(newOption);
						}
						if(combination.getDayType().equals("Saturday")) {
							this.daysPerGroup.get(group).get((i*7 + 6)).add(newOption);
						}
					}
				}
			}
		}
		
		this.restDaysPerGroup = new HashMap<>();
		for(ContractGroup group : this.instance.getContractGroups()) {
			IloNumVar[] restDays = this.cplex.boolVarArray(this.daysPerGroup.get(group).size());
			this.restDaysPerGroup.put(group, restDays);
		}
	}
	
	public void initConstraint1() throws IloException { //Max one duty per day
		for(ContractGroup group : this.instance.getContractGroups()) {
			this.daysPerGroup.get(group);
			for(int i = 0; i < this.daysPerGroup.get(group).size(); i++) {
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				Set<IloNumVar> currentDay = this.daysPerGroup.get(group).get(i);
				for(IloNumVar decisionVar : currentDay) {
					constraint.addTerm(decisionVar, 1);
				}
				constraint.addTerm(restDaysPerGroup.get(group)[i], 1);
				this.cplex.addLe(constraint, 1);
			}			
		}
	}
	
	public void initConstraint2() throws IloException { //Combinations
		for(Combination combination : this.instance.getM()) {
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			for(IloNumVar decisionVar : this.dutyAssignmentCombis.get(combination)) {
				constraint.addTerm(decisionVar, 1);
			}
			this.cplex.addGe(constraint, combination.getN());
		}
	}
	
	public void initConstraint3() throws IloException { // Meal duties, at most one per week
		for (Combination combination : this.instance.getM()) {
			if (combination.getType().equals("M")) { // If this is a meal duty
				for (ContractGroup group : this.instance.getContractGroups()) { // For all groups
					for (int w = 0; w <= this.weeksPerGroup.get(group) - 1; w++) { // For all weeks, minus 1
						IloLinearNumExpr constraint = this.cplex.linearNumExpr();
						for (int i = (7 * w); i <= ((7 * (w+1)) - 1); i++) { // From the first day to the last day of the week
							// If the decision variable is also contained in the set of decision variables
							// for this day
							for (IloNumVar decVar : this.dutyAssignmentCombis.get(combination)) { // For the decision
																									// variables of this
																									// combination
								if (this.daysPerGroup.get(group).get(i).contains(decVar)) {
									constraint.addTerm(decVar, 1);
								}
							}
						}
						this.cplex.addLe(constraint, 1);
					}
				}
			}
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
}
