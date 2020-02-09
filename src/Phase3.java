import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ilog.concert.IloException;

public class Phase3 
{
	private final Instance instance;
	
	public Phase3(Instance instance) {
		this.instance = instance;
	}
	
	public void executeColumnGeneration() throws IloException {
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
		PricingProblem_Phase3 pricing = new PricingProblem_Phase3(instance);		
		
		boolean negRedCosts = true;
		while (negRedCosts) {
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
		for(int i = 0; i < model.dummies2.length; i++) {
			System.out.println(model.cplex.getValue(model.dummies2[i]));
		}
		for (int s = 0; s < model.dummiesDuties.size(); s++) {
			// Day type: Sunday
			if (s == 0) {
				// add all duties that need to be schedules on Sundays
				for (Duty duty : instance.getSunday()) {
					System.out.println(duty.getNr() + " " + model.cplex.getValue(model.dummiesDuties.get(s).get(duty.getNr())));
				}
			}
			// Day type: Saturday
			else if (s == 6) {
				// add all duties that need to be scheduled on Saturday
				for (Duty duty : instance.getSaturday()) {
					System.out.println(duty.getNr() + " " + model.cplex.getValue(model.dummiesDuties.get(s).get(duty.getNr())));
				}

			}
			// Day type: Workingday (add for Mon, Tue, Wed, Thu, Fri)
			else {
				// add all duties that need to be scheduled on working days
				for (Duty duty : instance.getWorkingDays()) {
					System.out.println(duty.getNr() + " " + model.cplex.getValue(model.dummiesDuties.get(s).get(duty.getNr())));
				}
			}
	}
	}
}
