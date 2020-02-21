import java.util.ArrayList;
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
		
		Map<ContractGroup, Set<Duty>> assignedDuties = randomAssign(allDuties);
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
			totalMax.put(group, 4 * this.newNrDrivers.get(group)); //Excluding reserve duties 
		}
		Set<Duty> added = new HashSet<>();
		while (added.size() < allDuties.size()) {
			int randomGroup = (int) (Math.random() * groups.size());
			ContractGroup group = groups.get(randomGroup);
			
			if (totalCount[randomGroup] < totalMax.get(group)) {
				int random = (int) (Math.random() * allDuties.size());
				Duty toAdd = allDuties.get(random);
				if (!added.contains(toAdd)) {
					if (!toAdd.getDayType().equals("Sunday") || (toAdd.getDayType().equals("Sunday")
							&& sundayCount[randomGroup] < sundayMax.get(group))) {
						assignedDuties.get(group).add(toAdd);
						added.add(toAdd);
						totalCount[randomGroup]++;
						if (toAdd.getDayType().equals("Sunday")) {
							sundayCount[randomGroup]++;
						}
					}
				}
			}
		}
		return assignedDuties;
	}
}
