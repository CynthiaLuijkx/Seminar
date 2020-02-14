import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Tools.ContractGroup;
import Tools.Instance;
import Tools.Schedule;
import ilog.concert.IloException;

/**
 * This class executes Phase 3: Column generation
 * @author Mette Wagenvoort
 *
 */
public class Phase3 
{
	private final Instance instance;
	private final int minBreakBetweenShifts;
	private final int consecFreeWeekly;
	private final int freeTwoWeeks;
	
	public Phase3(Instance instance, int minBreakBetweenShifts, int consecWeek, int twoWeek) {
		this.instance = instance;
		this.minBreakBetweenShifts = minBreakBetweenShifts;
		this.consecFreeWeekly = consecWeek;
		this.freeTwoWeeks = twoWeek;
	}
	
	/**
	 * This method executes the column generation algorithm.
	 * @throws IloException
	 */
	public HashMap<Schedule, Double> executeColumnGeneration() throws IloException {
		/*
		 * Until no more schedules with negative reduced costs:
		 * 		Solve RMP
		 * 		Update dual costs on the arcs
		 * 		Solve the pricing problem
		 */
		HashMap<Schedule, Double> solution = new HashMap<>();
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
		PricingProblem_Phase3 pricing = new PricingProblem_Phase3(instance, minBreakBetweenShifts, consecFreeWeekly, freeTwoWeeks);
		
		boolean negRedCosts = true;
		while (negRedCosts) {
			model.clean();
			
			long start = System.nanoTime();
			negRedCosts = false;
			System.out.println("-------------------------------------------");
			System.out.println("Iteration: " + iteration);
			System.out.println("-------------------------------------------");
			
			// Solve the pricing problem
			pricing.updateDualCosts(dualValuesContractGroup, dualsDuties);
			Map<ContractGroup, Set<Schedule>> newSchedules = pricing.executePulse();
//			Map<ContractGroup, Set<Schedule>> newSchedules = pricing.executeLabelling();
			
			// Check whether schedules were already included
			for (ContractGroup c : instance.getContractGroups()) {
				int count = 0;
				for (Schedule curSchedule : newSchedules.get(c)) {
					if (!addedSchedules.contains(curSchedule)) {
						addedSchedules.add(curSchedule);
//						model.addSchedule(curSchedule);
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
			
			model = new RMP_Phase3(instance);
			for (Schedule toAdd : addedSchedules) {
				model.addSchedule(toAdd);
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
			model.makeSolution();
			solution = model.getSolution();
			
			dualValuesContractGroup = model.getDuals2();
			dualsDuties = model.getDuals1();
			
			ArrayList<Double> solDummies1 = model.getSolutionDummies2();
			ArrayList<ArrayList<Double>> solDummiesDuties = model.getSolutionDummiesDuties();
			
			for (int i = 0; i < dualValuesContractGroup.length; i++) {
				System.out.println(dualValuesContractGroup[i]);
			}
			iteration++;
			
		}
		System.out.println("Terminated");
		return solution;
	}
}