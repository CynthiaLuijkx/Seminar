import ilog.concert.IloException;
import ilog.cplex.IloCplex;

public class MIP_Phase1 
{
	private IloCplex cplex;
	
	private final Instance instance; 
	
	// Variables
	
	
	public MIP_Phase1(Instance instance) throws IloException {
		this.cplex = new IloCplex();
		
		this.instance = instance;
		
		initVars();
		initConstraintX();
		initObjective();
		
		this.cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0);
		this.cplex.exportModel(null);
		this.cplex.exportModel("MIP_Phase1.lp");
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
		
	}
	
	public void initConstraintX() throws IloException {
		
	}
	
	public void initObjective() throws IloException {
		
	}
}
