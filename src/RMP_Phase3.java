import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import Tools.Duty;
import Tools.Instance;
import Tools.Schedule;
import ilog.concert.IloColumn;
import ilog.concert.IloException;
import ilog.concert.IloNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloObjective;
import ilog.concert.IloRange;
import ilog.cplex.IloCplex;

public class RMP_Phase3 {

	private IloCplex cplex; //Start cplex
	private List<HashMap<Integer, IloRange>> constraints1 = new ArrayList<HashMap<Integer, IloRange>>(); //constraints such that we need to have all duties of a certain day type scheduled
	private IloRange[] constraints2; //constraints such that each duty need to be rostered onces

	private IloObjective obj; //objective 
	private List<IloNumVar> variables; //variables that are schedules
	private List<HashMap<Integer, IloNumVar>> dummiesDuties = new ArrayList<HashMap<Integer,IloNumVar>>(); //dummy variables for the duties
	private IloNumVar[] dummies2; //set of dummies for constraint 2
	private final Instance instance; //use the instance
	private final int penaltyOver; //penalty for overtime
	private final int penaltyMinus; //penalty for minus hours

	//Constructor of the class
	public RMP_Phase3(Instance instance) throws IloException{
		this.cplex = new IloCplex();
		this.instance = instance;
		this.penaltyMinus = 1; //penalty = 1 is basic case
		this.penaltyOver = 1; //penalty = 1 is basic case

		//for every day of the week and for every duty at that day type
		for(int i =0; i < 7; i++) {
			this.constraints1.add(new HashMap<Integer, IloRange>());
			this.dummiesDuties.add(new HashMap<Integer, IloNumVar>());
		}
		//for every contract group
		this.constraints2 = new IloRange[instance.getContractGroups().size()];
		this.dummies2 = new IloNumVar[instance.getContractGroups().size()];

		this.variables = new ArrayList<>();

		//add variables, constraints and objective
		addVariables();
		addConstraints1();
		addConstraints2();
		addObjective();

		this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0); //the gap should be closed till it is 0%
		this.cplex.setOut(null);
		//this.cplex.exportModel("model.lp");
	}

	//Method to solve the RMP
	public void solve() throws IloException {
		//this.cplex.exportModel("model.lp");
		this.cplex.solve();
		//Some printing
		//		System.out.println(cplex.getModel());
		//		System.out.println(cplex.getObjValue());
		/*ArrayList<ArrayList<Double>> solution = getSolutionDummiesDuties();
		ArrayList<ArrayList<Double>> solutionReserve = getSolutionDummiesReserve();
		ArrayList<Double> solutionATV = getSolutionDummiesATV();
		ArrayList<Double> solution2 = getSolutionDummies2();*/

	}

	//Method to clean cplex
	public void clean() throws IloException {
		this.cplex.endModel();
		this.cplex.clearModel();
	}
	//Method to get the objective value
	public double getObjective() throws IloException {
		return this.cplex.getObjValue();
	}
	//Method to check whether the problem is feasible
	public boolean isFeasible() throws IloException {
		return this.cplex.isPrimalFeasible();
	}	

	//Method that add the decision variables we need
	public void addVariables() throws IloException {
		/*for (int d = 0; d < this.variables.size(); d++) {
			this.variables.add(this.cplex.numVar(0, Double.POSITIVE_INFINITY));
		}*/

		//for the initialization we only include dummy variables 
		for(int m = 0; m < this.dummies2.length; m++) {
			this.dummies2[m] = this.cplex.numVar(0, Double.POSITIVE_INFINITY);
		}
		//for every day type, there will be a dummy for every possible duty that has that day type
		for(int s = 0; s < this.dummiesDuties.size(); s++) {
			//Day type: Sunday
			if(s == 0) {
				//add all duties that need to be schedules on Sundays
				for(Duty duty: instance.getSunday()) {
					this.dummiesDuties.get(s).put(duty.getNr(), this.cplex.numVar(0, Double.POSITIVE_INFINITY));
				}	
			}
			//Day type: Saturday
			else if(s==6){
				//add all duties that need to be scheduled on Saturday
				for(Duty duty: instance.getSaturday()) {
					this.dummiesDuties.get(s).put(duty.getNr(), this.cplex.numVar(0, Double.POSITIVE_INFINITY));
				}	

			}
			//Day type: Workingday (add for Mon, Tue, Wed, Thu, Fri)
			else {
				//add all duties that need to be scheduled on working days
				for(Duty duty: instance.getWorkingDays()) {
					this.dummiesDuties.get(s).put(duty.getNr(), this.cplex.numVar(0, Double.POSITIVE_INFINITY));
				}	
			}

		}
	}
	//Method that creates the objective
	public void addObjective() throws IloException {
		double bigNumber = 100;
		IloNumExpr obj = this.cplex.constant(0);
		//add all dummies with a high cost
		for (int t = 0; t < this.dummiesDuties.size(); t++) {
			for(Integer dutyNumber: this.dummiesDuties.get(t).keySet()) {
				obj = this.cplex.sum(obj, this.cplex.prod(this.dummiesDuties.get(t).get(dutyNumber), bigNumber));
			}
		}

		for (int c = 0; c < this.dummies2.length; c++) {
			obj = this.cplex.sum(obj, this.cplex.prod(this.dummies2[c], bigNumber));	
		}
		//minimize objective
		this.obj = this.cplex.addMinimize(obj);
	}
	//Method that creates the first type of constraints: all duties should be satisfied
	public void addConstraints1() throws IloException{
		for (int i = 0; i < this.dummiesDuties.size(); i++) {
			for(Integer dutyNumber: this.dummiesDuties.get(i).keySet()) {
				IloNumExpr lhs = cplex.constant(0); 
				lhs = cplex.sum(lhs, this.dummiesDuties.get(i).get(dutyNumber));
				this.constraints1.get(i).put(dutyNumber, cplex.addEq(1, lhs, "Duty " + dutyNumber + " on day " + i));
			}
		}
	}
	//Method that creates the second type of constraints: every contract group should have 1 schedule
	public void addConstraints2() throws IloException{
		for (int c = 0; c < this.dummies2.length; c++) {
			IloNumExpr lhs = cplex.constant(0); 
			lhs = cplex.sum(lhs, this.dummies2[c]);
			this.constraints2[c] = cplex.addEq(lhs, 1, "Contractgroup " + c);	
		}
	}
	//Method that returns the dual values of the first type of constraints;
	public List<HashMap<Integer, Double>> getDuals1() throws IloException{
		List<HashMap<Integer, Double>> duals1 = new ArrayList<HashMap<Integer, Double>>();
		//for every day, for every possible duty on that day
		for (int i = 0; i < this.constraints1.size(); i++) {
			duals1.add(new HashMap<Integer,Double>());
			for(Integer dutyNumber: this.constraints1.get(i).keySet()) {
				// get dual values
				duals1.get(i).put(dutyNumber, cplex.getDual(this.constraints1.get(i).get(dutyNumber)));
			}
		}
		//Return list
		return duals1;
	}
	//Method that returns the dual values of the second type of constraints;
	public double[] getDuals2() throws IloException{
		double[] duals2 = new double[this.constraints2.length];
		//for every contract group
		for(int i =0; i < duals2.length; i++) {
			//get dual values
			duals2[i] = cplex.getDual(this.constraints2[i]);
		}
		return duals2;
	}

	//Method that add schedules as columns to the RMP
	public void addSchedule(Schedule schedule) throws IloException {
		//add to the objective the cost corresponding to the schedule
		//we add cost of overtime + minus time + contract time
		IloColumn column = this.cplex.column(this.obj, schedule.getOvertime() * this.penaltyOver - 1);

		//for every day in the schedule, add the coefficient to the corresponding constraint of the duty that is schedules on that day
		for(int t = 0; t < schedule.getSchedule().length; t++) {
			if (instance.getFromDutyNrToDuty().containsKey(schedule.getSchedule()[t])) {
				//if the day is a Sunday:
				if (t % 7 == 0) {
					IloColumn coefficient1sun = this.cplex.column(this.constraints1.get(0).get(schedule.getSchedule()[t]), 1);
					column = column.and(coefficient1sun);
				}
				//if the day is a Monday:
				if (t % 7 == 1) {
					IloColumn coefficient1mon = this.cplex.column(this.constraints1.get(1).get(schedule.getSchedule()[t]), 1);
					column = column.and(coefficient1mon);
				}
				//if the day is a Tuesday:
				if (t % 7 == 2) {
					IloColumn coefficient1tue = this.cplex.column(this.constraints1.get(2).get(schedule.getSchedule()[t]), 1);
					column = column.and(coefficient1tue);
				}
				//if the day is a Wednesday:
				if (t % 7 == 3) {
					IloColumn coefficient1wed = this.cplex.column(this.constraints1.get(3).get(schedule.getSchedule()[t]), 1);
					column = column.and(coefficient1wed);
				}
				//if the day is a Thursday:
				if (t % 7 == 4) {
					IloColumn coefficient1thu = this.cplex.column(this.constraints1.get(4).get(schedule.getSchedule()[t]), 1);
					column = column.and(coefficient1thu);
				}
				//if the day is a Friday:
				if (t % 7 == 5) {
					IloColumn coefficient1fri = this.cplex.column(this.constraints1.get(5).get(schedule.getSchedule()[t]), 1);
					column = column.and(coefficient1fri);
				}
				//if the day is a Saturday:
				if (t % 7 == 6) {
					IloColumn coefficient1sat = this.cplex.column(this.constraints1.get(6).get(schedule.getSchedule()[t]), 1);
					column = column.and(coefficient1sat);
				}
			}
		}
		//if the schedule is for a certain contract group, add a coefficient for this particular group
		IloColumn coefficient2 = this.cplex.column(this.constraints2[schedule.getC().getNr()-1], 1);
		column = column.and(coefficient2);

		//add the schedule as a variable
		IloNumVar var = this.cplex.numVar(column, 0, Double.POSITIVE_INFINITY);
		this.variables.add(var);
		this.cplex.exportModel("model.lp");
	}

	public ArrayList<ArrayList<Double>> getSolutionDummiesDuties() throws IloException {

		ArrayList<ArrayList<Double>> solution1 = new ArrayList<ArrayList<Double>>();

		for (int t = 0; t < this.dummiesDuties.size(); t++) {
			solution1.add(new ArrayList<Double>());
			for(Integer dutyNumber: this.dummiesDuties.get(t).keySet()) {
				solution1.get(t).add(this.cplex.getValue(this.dummiesDuties.get(t).get(dutyNumber)));
			}
		}
		return solution1;
	}


	public ArrayList<Double> getSolutionDummies2() throws IloException {

		ArrayList<Double> solution1 = new ArrayList<Double>();
		for (int t = 0; t < this.dummies2.length; t++) {
			solution1.add(this.cplex.getValue(this.dummies2[t]));
		}

		return solution1;
	}
}
