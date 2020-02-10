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

public class Phase4_RelaxFix_LP {
	
	private IloCplex cplex; //Start cplex
	private final Set<Schedule> schedules;
	private final HashMap<Schedule, Integer> fixedSchedules;
	private final Set<Schedule> integerSchedules;
	private final Set<Schedule> relaxedSchedules;
	private final Instance instance;
	
	private final HashMap<IloNumVar, Schedule> varToSchedule;
	private final HashMap<Schedule, IloNumVar> scheduleToVar;
	private final Set<IloNumVar> integerVariables;
	private final Set<IloNumVar> relaxedVariables;
	
	private final HashMap<Schedule, Double> solution;
	

	public Phase4_RelaxFix_LP(HashMap<Schedule, Integer> fixedSchedules, Set<Schedule> integerSchedules, Set<Schedule> relaxedSchedules, Instance instance) throws IloException {
		this.cplex = new IloCplex();
		this.fixedSchedules = fixedSchedules;
		this.integerSchedules = integerSchedules;
		this.relaxedSchedules = relaxedSchedules;
		this.schedules = new HashSet<>();
		schedules.addAll(integerSchedules);
		schedules.addAll(relaxedSchedules);
		this.instance = instance;
		
		this.integerVariables = new HashSet<>();
		this.relaxedVariables = new HashSet<>(); 
		this.scheduleToVar = new HashMap<>();
		this.varToSchedule = new HashMap<>();
		
		initVars();
		initConstraint1();
		initConstraint2();
		initObjective();
		
		this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
		this.cplex.setOut(null);
		this.cplex.setWarning(null);
		this.cplex.exportModel("Phase4_ILP.lp");
		solve();
	
		//System.out.println(this.cplex.getObjValue());
		
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
		for(Schedule curSchedule : this.integerSchedules) {
			IloNumVar var = this.cplex.boolVar();
			var.setName(curSchedule.toString());
			this.varToSchedule.put(var, curSchedule);
			this.scheduleToVar.put(curSchedule,var);
			this.integerVariables.add(var);
		}
		for(Schedule curSchedule : this.relaxedSchedules) {
			IloNumVar var = this.cplex.numVar(0,  Double.MAX_VALUE);
			var.setName(curSchedule.toString());
			this.varToSchedule.put(var, curSchedule);
			this.scheduleToVar.put(curSchedule,var);
			this.relaxedVariables.add(var);
		}
	}
	
	public void initConstraint1() throws IloException { //Only one schedule per contract group
		for(ContractGroup group : instance.getContractGroups()) {
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			int alreadyCovered = 0; 
			for(Schedule curSchedule : this.schedules) {
				if(curSchedule.getC() == group) {
					constraint.addTerm(this.scheduleToVar.get(curSchedule), 1);
				}
			}
			for(Schedule fixedSchedule : this.fixedSchedules.keySet()) {
				if(fixedSchedule.getC() == group && this.fixedSchedules.get(fixedSchedule) == 1) {
					alreadyCovered++;
				}
			}
			this.cplex.addEq(constraint, 1 - alreadyCovered, group.groupNumberToString());
		}
	}
	
	public void initConstraint2() throws IloException {
		for(Duty duty : instance.getSunday()) {
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			int alreadyCovered = 0; 
			for(Schedule curSchedule : this.schedules) {
				for(int w = 0; w < curSchedule.getSchedule().length/7; w++) {//For every week
					if(curSchedule.getSchedule()[7*w] == duty.getNr()) {
						constraint.addTerm(this.scheduleToVar.get(curSchedule), 1);
						break;
					}
				}
			}
			for(Schedule fixedSchedule : this.fixedSchedules.keySet()) {
				if(this.fixedSchedules.get(fixedSchedule) == 1) {
					for(int w = 0; w < fixedSchedule.getSchedule().length/7; w++) {//For every week
						if(fixedSchedule.getSchedule()[7*w] == duty.getNr()) {
							this.cplex.sum(constraint, this.cplex.constant(1));
							alreadyCovered++;
							break;
						}
					}
				}
			}
			this.cplex.addEq(constraint, 1-alreadyCovered, duty.getNr() + "_" + 0);
		}
		for(Duty duty : instance.getSaturday()) {
			IloLinearNumExpr constraint = this.cplex.linearNumExpr();
			int alreadyCovered = 0;
			for(Schedule curSchedule : this.schedules) {
				for(int w = 0; w < curSchedule.getSchedule().length/7; w++) {//For every week
					if(curSchedule.getSchedule()[7*w + 6] == duty.getNr()) {
						constraint.addTerm(this.scheduleToVar.get(curSchedule), 1);
						break;
					}
				}
			}
			for(Schedule fixedSchedule : this.fixedSchedules.keySet()) {
				if(this.fixedSchedules.get(fixedSchedule) == 1) {
					for(int w = 0; w < fixedSchedule.getSchedule().length/7; w++) {//For every week
						if(fixedSchedule.getSchedule()[7*w + 6] == duty.getNr()) {
							this.cplex.sum(constraint, this.cplex.constant(1));
							alreadyCovered++;
							break;
						}
					}
				}
			}
			this.cplex.addEq(constraint, 1-alreadyCovered, duty.getNr() + "_" + 6);
		}
		for (Duty duty : instance.getWorkingDays()) {
			for (int s = 1; s <= 5; s++) {
				IloLinearNumExpr constraint = this.cplex.linearNumExpr();
				int alreadyCovered = 0;
				for (Schedule curSchedule : this.schedules) {
					for (int w = 0; w < curSchedule.getSchedule().length / 7; w++) {// For every week
						if (curSchedule.getSchedule()[7 * w + s] == duty.getNr()) {
							constraint.addTerm(this.scheduleToVar.get(curSchedule), 1);
							break;
						}
					}
				}
				
				for(Schedule fixedSchedule : this.fixedSchedules.keySet()) {
					if(this.fixedSchedules.get(fixedSchedule) == 1) {
						for(int w = 0; w < fixedSchedule.getSchedule().length/7; w++) {//For every week
							if(fixedSchedule.getSchedule()[7*w + s] == duty.getNr()) {
								this.cplex.sum(constraint, this.cplex.constant(1));
								alreadyCovered++;
								break;
							}
						}
					}
				}
				this.cplex.addEq(constraint, 1 - alreadyCovered, duty.getNr() + "_" + s);
			}
		}
	}

	public void initObjective() throws IloException {
		IloLinearNumExpr objective = this.cplex.linearNumExpr();
		for(Schedule curSchedule : this.schedules) {
			objective.addTerm(Math.max(0, curSchedule.getPlusMin() - curSchedule.getMinMin()), this.scheduleToVar.get(curSchedule));
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
