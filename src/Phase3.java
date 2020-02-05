
public class Phase3 
{
	private final Instance instance;
	
	public Phase3(Instance instance) {
		this.instance = instance;
	}
	
	public void executeColumnGeneration() {
		/*
		 * Until no more schedules with negative reduced costs:
		 * 		Solve RMP
		 * 		Update dual costs on the arcs
		 * 		Solve the pricing problem
		 */
	}
}
