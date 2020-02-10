import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import Tools.ContractGroup;
import Tools.Duty;
import Tools.Instance;
import Tools.Schedule;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;

public class Phase4_ILP {
	
	private IloCplex cplex; //Start cplex
	private final Set<Schedule> schedules;
	private final Instance instance;
	
	private final HashMap<IloNumVar, Schedule> varToSchedule;
	private final HashMap<Schedule, IloNumVar> scheduleToVar;
	private final Set<IloNumVar> variables;
	

	public Phase4_ILP(HashMap<Schedule, Double> inputSolution, Instance instance) throws IloException {
		this.schedules = new HashSet<>();
		schedules.addAll(inputSolution.keySet());
		this.instance = instance;
		
		this.variables = new HashSet<>();
		this.scheduleToVar = new HashMap<>();
		this.varToSchedule = new HashMap<>();
		
		initVars();
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
		for(Schedule curSchedule : this.schedules) {
			IloNumVar var = this.cplex.boolVar();
			var.setName(curSchedule.toString());
			this.varToSchedule.put(var, curSchedule);
			this.scheduleToVar.put(curSchedule,var);
			this.variables.add(var);
		}
	}
	
	public void initConstraint1() throws IloException { //Only one schedule per contract group
		for(ContractGroup group : instance.getContractGroups()) {
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			for(Schedule curSchedule : this.schedules) {
				if(curSchedule.getC() == group) {
					constraint.addTerm(this.scheduleToVar.get(curSchedule), 1);
				}
			}
			this.cplex.addEq(constraint, 1);
		}
	}
	
	public void initConstraint2() throws IloException {
		for(Duty duty : instance.getSunday()) {
			
		}
	}
}
