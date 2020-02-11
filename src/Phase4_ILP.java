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
	
	private final HashMap<Schedule, Double> solution;
	

	public Phase4_ILP(Set<Schedule> inputSolution, Instance instance) throws IloException {
		this.cplex = new IloCplex();
		this.schedules = new HashSet<>();
		schedules.addAll(inputSolution);
		this.instance = instance;
		
		this.variables = new HashSet<>();
		this.scheduleToVar = new HashMap<>();
		this.varToSchedule = new HashMap<>();
		
		initVars();
		initConstraint1();
		initConstraint2();
		initObjective();
		
		this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
		this.cplex.exportModel("Phase4_ILP.lp");
		solve();
	
		System.out.println("Objective: " + this.cplex.getObjValue());
		
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
			this.cplex.addEq(constraint, 1, group.groupNumberToString());
		}
	}
	
	public void initConstraint2() throws IloException {
		for(Duty duty : instance.getSunday()) {
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			for(Schedule curSchedule : this.schedules) {
				for(int w = 0; w < curSchedule.getSchedule().length/7; w++) {//For every week
					if(curSchedule.getSchedule()[7*w] == duty.getNr()) {
						constraint.addTerm(this.scheduleToVar.get(curSchedule), 1);
						break;
					}
				}
			}
			this.cplex.addGe(constraint, 1, duty.getNr() + "_" + 0);
		}
		for(Duty duty : instance.getSaturday()) {
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			for(Schedule curSchedule : this.schedules) {
				for(int w = 0; w < curSchedule.getSchedule().length/7; w++) {//For every week
					if(curSchedule.getSchedule()[7*w + 6] == duty.getNr()) {
						constraint.addTerm(this.scheduleToVar.get(curSchedule), 1);
						break;
					}
				}
			}
			this.cplex.addGe(constraint, 1, duty.getNr() + "_" + 6);
		}
		for (Duty duty : instance.getWorkingDays()) {
			for (int s = 1; s <= 5; s++) {
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				for (Schedule curSchedule : this.schedules) {
					for (int w = 0; w < curSchedule.getSchedule().length / 7; w++) {// For every week
						if (curSchedule.getSchedule()[7 * w + s] == duty.getNr()) {
							constraint.addTerm(this.scheduleToVar.get(curSchedule), 1);
							break;
						}
					}
				}
				this.cplex.addGe(constraint, 1, duty.getNr() + "_" + s);
			}
		}
	}

	public void initObjective() throws IloException {
		IloLinearNumExpr objective = this.cplex.linearNumExpr();
		for(IloNumVar var : this.variables) {
			Schedule schedule = this.varToSchedule.get(var);
			objective.addTerm(Math.max(0, schedule.getPlusMin() - schedule.getMinMin()), var);
		}
		this.cplex.addMinimize(objective);
	}
	
	public void makeSolution() throws IloException {
		for(Schedule schedule : this.schedules) {
			this.solution.put(schedule, this.cplex.getValue(this.scheduleToVar.get(schedule)));
		}
	}
	
	public HashMap<Schedule, Double> getSolution(){
		return this.solution;
	}
}
