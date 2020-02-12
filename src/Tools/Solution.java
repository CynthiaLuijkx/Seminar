package Tools;
import Tools.Request;
import Tools.ContractGroup;
import Tools.LSschedule;

import java.util.*;

public class Solution {
	Set<Request> requests;
	Map<ContractGroup, LSschedule> newSchedule = new HashMap<ContractGroup, LSschedule>();
	
	public Solution(Set<Request> requests, Map<ContractGroup,LSschedule> schedule) {
		this.requests = requests;
		this.newSchedule = schedule;
	}

	public Set<Request> getRequests() {
		return requests;
	}

	public void setRequests(Set<Request> requests) {
		this.requests = requests;
	}

	public Map<ContractGroup, LSschedule> getNewSchedule() {
		return newSchedule;
	}

	public void setNewSchedule(Map<ContractGroup, LSschedule> schedule) {
		this.newSchedule = schedule;
	}
	
}
