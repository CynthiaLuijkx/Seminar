import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import Tools.*;
import ilog.concert.IloException;

public class Phase4 {
	private final Set<Schedule> inputSolution; 
	private final Instance instance;
	
	public Phase4(Set<Schedule> inputSolution, Instance instance) {
		this.inputSolution = inputSolution;
		this.instance = instance;
	}
	
	public void runILP() throws IloException {
		long start = System.nanoTime();
		Phase4_ILP ilp = new Phase4_ILP(this.inputSolution, this.instance);
		long end = System.nanoTime();
		System.out.println("ILP runTime: " + (end-start)/1000000000.0);
		List<Schedule> ilpSolution = ilp.getSolution();
		for(Schedule schedule : ilpSolution) {
			System.out.println(schedule.toString());
		}
		Phase4_AddMissing addMissing = new Phase4_AddMissing(ilpSolution, this.instance);
	}
	
	public void runRelaxFix() throws IloException {
		long start = System.nanoTime();
		int parameter = 10; //Amount of groups 
		HashMap<Schedule, Integer> fixedSchedules = new HashMap<>();
		Set<Schedule> integerSchedules = new HashSet<>();
		Set<Schedule> relaxedSchedules = new HashSet<>();
		
		Set<Schedule> added = new HashSet<>();
		for(int i = 0; i < Math.max(1,inputSolution.size()/(parameter)); i++) {
			for(Schedule schedule : inputSolution) {
				if(!added.contains(schedule)) {
					integerSchedules.add(schedule);
					added.add(schedule);
					break;
				}
			}
		}
		relaxedSchedules.addAll(inputSolution);
		relaxedSchedules.removeAll(integerSchedules);
		
		while(fixedSchedules.size() != inputSolution.size()) {
			/*
			System.out.println("Fixed Size: " + fixedSchedules.size());
			System.out.println("Integer Size: " + integerSchedules.size());
			System.out.println("Relaxed Size: " + relaxedSchedules.size());*/
			Phase4_RelaxFix_LP lp = new Phase4_RelaxFix_LP(fixedSchedules, integerSchedules, relaxedSchedules, instance);
			
			//Moving from Integer to Fixed
			Set<Schedule> toRemove = new HashSet<>();
			for (int i = 0; i < Math.max(1, inputSolution.size() / (parameter * 2)); i++) {
				for (Schedule schedule : integerSchedules) {
					if (!toRemove.contains(schedule)) {
						int solValue = 0;
						if (lp.getSolution().get(schedule) > 0.1) {
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
			for(int i = 0; i < Math.max(1,inputSolution.size()/(parameter*2)); i++) {
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
		
		List<Schedule> schedules = new ArrayList<>();
		int objValue = 0;
		for(Schedule schedule : fixedSchedules.keySet()) {
			if(fixedSchedules.get(schedule) == 1) {
				System.out.println(schedule.toString());
				schedules.add(schedule);
				objValue = objValue + schedule.getOvertime();
			}
		}
		long end = System.nanoTime();
		System.out.println("Relax&Fix runTime: " + (end-start)/1000000000.0);
		System.out.println("Objective: " + objValue);
		Phase4_AddMissing addMissing = new Phase4_AddMissing(schedules, this.instance);		
	}
	
	public void runAllCombinations(String depot) {
		long start = System.nanoTime();
		Set<ScheduleCombination> allCombinations = this.getAllCombinations(depot);
		System.out.println(allCombinations.size());
		int minimum = Integer.MAX_VALUE;
		ScheduleCombination minimumCombi = null;
		for (ScheduleCombination combi : allCombinations) {
			if (combi.getCost() < minimum) {
				minimum = combi.getCost();
				minimumCombi = combi;
			}
			if (minimum == 0) {
				break;
			}
		}
		System.out.println("Objective: " + minimum);
		List<Schedule> schedules = new ArrayList<>();
		for (Schedule schedule : minimumCombi.getSchedules()) {
			System.out.println(schedule.toString());
			schedules.add(schedule);
		}
		long end = System.nanoTime();
		System.out.println("All Combinations runTime: " + (end-start)/1000000000.0);
		Phase4_AddMissing addMissing = new Phase4_AddMissing(schedules, this.instance);	
	}
	
	public Set<ScheduleCombination> getAllCombinations(String depot){
		Set<ScheduleCombination> output = new HashSet<>();
		
		if(depot.equals("Dirksland") || depot.equals("DirkslandEasier")) {
			Set<Schedule> contractGroup1 = new HashSet<>();
			Set<Schedule> contractGroup2 = new HashSet<>();
			for(Schedule schedule : this.inputSolution) {
				if(schedule.getC().getNr() == 1) {
					contractGroup1.add(schedule);
				}
				else {
					contractGroup2.add(schedule);
				}
			}
		
			for(Schedule group1 : contractGroup1) {
				for(Schedule group2 : contractGroup2) {
					Set<Schedule> combiInput = new HashSet<>();
					combiInput.add(group1);
					combiInput.add(group2);
					ScheduleCombination combination = new ScheduleCombination(combiInput, instance);
					if(combination.isFeasible()) {
						output.add(combination);
					}
				}
			}
		}
		else if(depot.equals("Heinenoord")) {
			Set<Schedule> contractGroup1 = new HashSet<>();
			Set<Schedule> contractGroup2 = new HashSet<>();
			Set<Schedule> contractGroup3 = new HashSet<>();
			Set<Schedule> contractGroup4 = new HashSet<>();
			for(Schedule schedule : this.inputSolution) {
				if(schedule.getC().getNr() == 1) {
					contractGroup1.add(schedule);
				}
				else if(schedule.getC().getNr() == 2) {
					contractGroup2.add(schedule);
				}
				else if(schedule.getC().getNr() == 3) {
					contractGroup3.add(schedule);
				}
				else {
					contractGroup4.add(schedule);
				}
			}
		
			for(Schedule group1 : contractGroup1) {
				for(Schedule group2 : contractGroup2) {
					for(Schedule group3: contractGroup3) {
						for(Schedule group4 : contractGroup4) {
							Set<Schedule> combiInput = new HashSet<>();
							combiInput.add(group1);
							combiInput.add(group2);
							combiInput.add(group3);
							combiInput.add(group4);
							ScheduleCombination combination = new ScheduleCombination(combiInput, instance);
							if(combination.isFeasible()) {
								output.add(combination);
							}
						}
					}
				}
			}
		}
		
		return output;
			
	}
}


