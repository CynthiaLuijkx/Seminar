package Tools;
import Tools.Duty;
import Tools.ContractGroup;

import java.util.*;

public class Request {
	private final Duty duty;
	private final ContractGroup group;
	
	public Request(Duty duty, ContractGroup group) {
		this.duty = duty;
		this.group = group;
	}

	public Duty getDuty() {
		return duty;
	}

	public ContractGroup getGroup() {
		return group;
	}
	
}

