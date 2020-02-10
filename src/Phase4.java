import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import Tools.*;
import ilog.concert.IloException;

public class Phase4 {
	private final HashMap<Schedule, Double> inputSolution; 
	private final Instance instance;
	
	public Phase4(HashMap<Schedule, Double> inputSolution, Instance instance) {
		this.inputSolution = inputSolution;
		this.instance = instance;
	}
	
	public void runILP() throws IloException {
		Phase4_ILP ilp = new Phase4_ILP(this.inputSolution, this.instance);
		HashMap<Schedule, Double> intSolution = ilp.getSolution();
		for(Schedule schedule : intSolution.keySet()) {
			if(intSolution.get(schedule) > 0) {
				System.out.println(intSolution.get(schedule) + " " + schedule.toString());
			}
		}
	}
	
	public void runRelaxFix() throws IloException {
		int parameter = 10; //Amount of groups 
		HashMap<Schedule, Integer> fixedSchedules = new HashMap<>();
		Set<Schedule> integerSchedules = new HashSet<>();
		Set<Schedule> relaxedSchedules = new HashSet<>();
		
		Set<Schedule> added = new HashSet<>();
		for(int i = 0; i < inputSolution.keySet().size()/parameter; i++) {
			for(Schedule schedule : inputSolution.keySet()) {
				if(!added.contains(schedule)) {
					integerSchedules.add(schedule);
					added.add(schedule);
					break;
				}
			}
		}
		
		
		relaxedSchedules.addAll(inputSolution.keySet());
		while(fixedSchedules.size() != inputSolution.keySet().size()) {
			Phase4_RelaxFix_LP lp = new Phase4_RelaxFix_LP(fixedSchedules, integerSchedules, relaxedSchedules, instance);
			
			//Moving from Integer to Fixed
			Set<Schedule> toRemove = new HashSet<>();
			for(int i = 0; i < inputSolution.keySet().size()/(parameter*2); i++) {
				for(Schedule schedule : integerSchedules) {
					if(!toRemove.contains(schedule)) {
					int solValue = 0;
					if(lp.getSolution().get(schedule) > 0.1) {
						solValue = 1;
					}
					fixedSchedules.put(schedule, solValue);
					toRemove.add(schedule);
					}
					break;
				}
			}
			integerSchedules.removeAll(toRemove);
			
			//Moving from relaxed to Integer
			toRemove = new HashSet<>();
			for(int i = 0; i < inputSolution.keySet().size()/(parameter*2); i++) {
				for(Schedule schedule : relaxedSchedules) {
					if(!toRemove.contains(schedule)) {
						integerSchedules.add(schedule);
						toRemove.add(schedule);
						break;
					}
				}
			}
			relaxedSchedules.removeAll(toRemove);
			
		}
		
		int objValue = 0;
		for(Schedule schedule : fixedSchedules.keySet()) {
			if(fixedSchedules.get(schedule) == 1) {
				System.out.println(schedule.toString());
				objValue = objValue + Math.max(0, schedule.getPlusMin() - schedule.getMinMin());
			}
		}
		System.out.println("Objective: " + objValue);
	}
}
