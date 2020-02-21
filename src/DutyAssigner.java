import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import Tools.*;

public class DutyAssigner {

	private final Instance oldInstance;
	private final Map<ContractGroup, Integer> newNrDrivers;
	
	public DutyAssigner(Instance oldInstance, Map<ContractGroup, Integer> newNrDrivers) {
		this.oldInstance = oldInstance;
		this.newNrDrivers = newNrDrivers;
	}
	
	public Set<Instance> assignDuties() {
		List<Duty> allDuties = new ArrayList<>();
		allDuties.addAll(oldInstance.getWorkingDays());
		allDuties.addAll(oldInstance.getSaturday());
		allDuties.addAll(oldInstance.getSunday());
		
		Map<ContractGroup, Set<Duty>> assignedDuties = sortedAssign(allDuties);
		Set<Instance> newInstances = new HashSet<>();
		
		for(ContractGroup group : assignedDuties.keySet()) {
			Instance soloInstance = createSoloInstance(group, assignedDuties.get(group));
			newInstances.add(soloInstance);
		}
		
		return newInstances;
	}
	
	public Set<Duty> dutySetCopier(Set<Duty> toCopy, Set<Duty> assignedDuties){
		Set<Duty> copy = new HashSet<>();
		for(Duty duty : toCopy) {
			if(assignedDuties.contains(duty)) {
				copy.add(duty);
			}
		}
		return copy;
	}
	
	public HashMap<String, Set<Duty>> hashMapDutyCopier(HashMap<String, Set<Duty>> toCopy, Set<Duty> assignedDuties){
		HashMap<String, Set<Duty>> copy = new HashMap<>();
		
		for(String type : toCopy.keySet()) {
			int count = 0; 
			Set<Duty> duties = new HashSet<>();
			for(Duty duty : toCopy.get(type)) {
				if(assignedDuties.contains(duty)) {
					duties.add(duty);
					count++;
				}
			}
			if(count > 0) {
				copy.put(type, duties);
			}
		}
		
		return copy;
	}
	
	public HashMap<Integer, Duty> hashMapDutyNrCopier(HashMap<Integer, Duty> toCopy, Set<Duty> assignedDuties){
		HashMap<Integer, Duty> copy = new HashMap<>();
		for(Integer number : toCopy.keySet()) {
			if(assignedDuties.contains(toCopy.get(number))) {
				copy.put(number, toCopy.get(number));
			}
		}
		
		return copy;
	}
	
	public Instance createSoloInstance(ContractGroup group, Set<Duty> assignedDuties) {
		Set<Duty> workingDays = dutySetCopier(oldInstance.getWorkingDays(), assignedDuties);
		Set<Duty> saturday = dutySetCopier(oldInstance.getSaturday(), assignedDuties);
		Set<Duty> sunday = dutySetCopier(oldInstance.getSunday(), assignedDuties); 
		HashMap<String, Set<Duty>> dutiesPerType = hashMapDutyCopier(oldInstance.getDutiesPerType(), assignedDuties);
		HashMap<String, Set<Duty>> dutiesPerTypeW = hashMapDutyCopier(oldInstance.getDutiesPerTypeW(), assignedDuties);
		HashMap<String, Set<Duty>> dutiesPerTypeSat = hashMapDutyCopier(oldInstance.getDutiesPerTypeSat(), assignedDuties);
		HashMap<String, Set<Duty>> dutiesPerTypeSun = hashMapDutyCopier(oldInstance.getDutiesPerTypeSun(), assignedDuties);
		HashMap<Integer, Duty> fromDutyNrToDuty = hashMapDutyNrCopier(oldInstance.getFromDutyNrToDuty(), assignedDuties);
		
		Set<ContractGroup> contractGroups = new HashSet<>();
		int nr = 1;
		int avgDaysPerWeek = group.getAvgDaysPerWeek(); 
		double avgHoursPerDay = group.getAvgHoursPerDay();
		int ATVPerYear = group.getATVPerYear();
		double relativeGroupSize = 1;
		Set<String> dutyTypes = group.getDutyTypes();
		ContractGroup newGroup = new ContractGroup(nr, avgDaysPerWeek, avgHoursPerDay, ATVPerYear, relativeGroupSize, dutyTypes);

		contractGroups.add(newGroup);
		
		Set<ReserveDutyType> reserveDutyTypes = oldInstance.getReserveDutyTypes(); 
		HashMap<Integer, ReserveDutyType> fromRDutyNrToRDuty = oldInstance.getFromRDutyNrToRDuty(); 
		Set<Violation> violations11 = new HashSet<>(); 
		Set<Violation> violations32 = new HashSet<>();
		
		return new Instance(workingDays, saturday, sunday, dutiesPerType, dutiesPerTypeW, dutiesPerTypeSat, dutiesPerTypeSun, fromDutyNrToDuty, contractGroups, reserveDutyTypes, fromRDutyNrToRDuty, violations11, violations32);
	}
	
	public Map<ContractGroup, Set<Duty>> randomAssign(List<Duty> allDuties) {
		List<ContractGroup> groups = new ArrayList<>();
		groups.addAll(oldInstance.getContractGroups());
		int[] totalCount = new int[groups.size()];
		int[] sundayCount = new int[groups.size()];
		for(int i = 0; i < groups.size(); i++) {
			totalCount[i] = 0;
			sundayCount[i] = 0;
		}
		Map<ContractGroup, Set<Duty>> assignedDuties = new HashMap<>();
		Map<ContractGroup, Integer> sundayMax = new HashMap<>();
		Map<ContractGroup, Integer> totalMax = new HashMap<>();
		for (ContractGroup group : oldInstance.getContractGroups()) {
			Set<Duty> duties = new HashSet<>();
			assignedDuties.put(group, duties);
			sundayMax.put(group, (int) Math.floor(0.75 * this.newNrDrivers.get(group)));
			totalMax.put(group, (int) (3.5 * this.newNrDrivers.get(group))); //Excluding reserve duties 
		}
		Set<Duty> added = new HashSet<>();
		while (added.size() < allDuties.size()) {
			double randomGroup = (Math.random());
			ContractGroup group = null;
			if(randomGroup <= 0.4) {
				group = groups.get(0);
			}
			else {
				group = groups.get(1);
			}
			
			int random = (int) (Math.random() * allDuties.size());
			Duty toAdd = allDuties.get(random);
			int countToAdd = 1;
			if(toAdd.getDayType().equals("Workingday")) {
				countToAdd = 5;
			}
			if (totalCount[groups.indexOf(group)] + countToAdd <= totalMax.get(group)) {
				if (!added.contains(toAdd)) {
					if (!toAdd.getDayType().equals("Sunday") || (toAdd.getDayType().equals("Sunday")
							&& sundayCount[groups.indexOf(group)] < sundayMax.get(group))) {
						assignedDuties.get(group).add(toAdd);
						added.add(toAdd);
						totalCount[groups.indexOf(group)] = totalCount[groups.indexOf(group)] + countToAdd;
						if (toAdd.getDayType().equals("Sunday")) {
							sundayCount[groups.indexOf(group)]++;
						}
					}
				}
			}
		}
		for(ContractGroup group : assignedDuties.keySet()) {
			System.out.println(group + " " + totalCount[groups.indexOf(group)] + " out of " + totalMax.get(group));
		}
		return assignedDuties;
	}

	public Map<ContractGroup, Set<Duty>> sortedAssign(List<Duty> allDuties) {
		Collections.sort(allDuties, new SortByPaidMins());
		
		List<ContractGroup> contractGroups = new ArrayList<>();
		contractGroups.addAll(oldInstance.getContractGroups());
		Collections.sort(contractGroups, new SortByContractHours());
		
		Map<ContractGroup, Set<Duty>> assignedDuties = new HashMap<>();
		for(ContractGroup group : contractGroups) {
			int totalMax = (int) (3 * this.newNrDrivers.get(group));
			int sundayMax = (int) Math.floor(0.75 * this.newNrDrivers.get(group));
			int totalCounter = 0;
			int sundayCounter = 0; 
			Set<Duty> duties = new HashSet<>();
			for(Duty duty : allDuties) {
				if(duty.getDayType().equals("Sunday") && sundayCounter < sundayMax) {
					duties.add(duty);
					totalCounter++;
					sundayCounter++;
				}
				else {
					if(duty.getDayType().equals("Workingday") && totalCounter+5 < totalMax) {
						duties.add(duty);
						totalCounter = totalCounter + 5;
					}
					else {
						duties.add(duty);
						totalCounter++;
					}
				}
				
				if(totalCounter >= totalMax) {
					break;
				}
			}
			allDuties.removeAll(duties);
			assignedDuties.put(group, duties);
			System.out.println("Assigned");
			System.out.println(group + " " + totalCounter + " out of " + totalMax);
		}
		
		return assignedDuties;
	}
	
}
