import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Tools.ContractGroup;
import Tools.Instance;
import Tools.Schedule;
import ilog.concert.IloException;

public class Phase3 
{
	private final Instance instance;
	private final int consecWeek;
	private final int twoWeek;
	
	public Phase3(Instance instance, int consecWeek, int twoWeek) {
		this.instance = instance;
		this.consecWeek = 32*60;
		this.twoWeek = 72*60;
	}
	
	public HashMap<Schedule, Double> executeColumnGeneration() throws IloException {
		/*
		 * Until no more schedules with negative reduced costs:
		 * 		Solve RMP
		 * 		Update dual costs on the arcs
		 * 		Solve the pricing problem
		 */
		
		Set<Schedule> addedSchedules = new HashSet<>();
		
		RMP_Phase3 model = new RMP_Phase3(instance);
		model.solve();
		System.out.println("-------------------------------------------");
		System.out.println("Restricted Model: ");
		System.out.println("-------------------------------------------");
		System.out.println("Objective value: " + model.getObjective());
		
		double[] dualValuesContractGroup = model.getDuals2();
		List<HashMap<Integer, Double>> dualsDuties = model.getDuals1();
		
		int iteration = 1;
		PricingProblem_Phase3 pricing = new PricingProblem_Phase3(instance, consecWeek, twoWeek);
		
		boolean negRedCosts = true;
		while (negRedCosts && model.getObjective() > 0) {
			long start = System.nanoTime();
			negRedCosts = false;
			System.out.println("-------------------------------------------");
			System.out.println("Iteration: " + iteration);
			System.out.println("-------------------------------------------");
			
			// Solve the pricing problem
			pricing.updateDualCosts(dualValuesContractGroup, dualsDuties);
			Map<ContractGroup, Set<Schedule>> newSchedules = pricing.executeLabelling();
			
			for (ContractGroup c : instance.getContractGroups()) {
				int count = 0;
				for (Schedule curSchedule : newSchedules.get(c)) {
					if (!addedSchedules.contains(curSchedule)) {
						addedSchedules.add(curSchedule);
						model.addSchedule(curSchedule);
						count++;
					}
				}
				
				if (count > 0) {
					negRedCosts = true;
				}
				System.out.println("Number of schedules added for contractgroup " + c.getNr() +": " + count);
			}
			
			if (!negRedCosts) {
				break;
			}
			
			long intermediate = System.nanoTime();
			
			// Solve the RMP again
			model.solve();
			long end = System.nanoTime();
			System.out.println("-------------------------------------------");
			System.out.println("Restricted Model:");
			System.out.println("-------------------------------------------");
			System.out.println("Is feasible? " + model.isFeasible());
			System.out.println("Objective value: " + model.getObjective());
			System.out.println("Running time: " + (end-start)/1000000000.0);
			System.out.println("Of which pricing: " + (intermediate-start)/1000000000.0);
			System.out.println("Of which RMP: " + (end-intermediate)/1000000000.0);
			
			dualValuesContractGroup = model.getDuals2();
			dualsDuties = model.getDuals1();

			iteration++;
		}
		model.makeSolution();
		return model.getSolution();
	}
}
