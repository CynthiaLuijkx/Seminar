import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PricingProblem_Phase3 
{
	private final int consecFreeWeekly = 32 * 60;
	private final int freeTwoWeeks = 72 * 60;
	
	private final Instance instance;

	private HashMap<ContractGroup, DirectedGraph<Node, Double>> graphs;

	public PricingProblem_Phase3(Instance instance) {
		this.instance = instance;
		initGraphs();
	}
	
	/**
	 * This method intialises the directed graph for each contract group.
	 */
	public void initGraphs() {
		this.graphs = new HashMap<ContractGroup, DirectedGraph<Node, Double>>();
		int minPerDay = 24 * 60;
		int minBreakBetweenShifts = 11 * 60;
		
		// Initialise the graphs for each contract group
		for (ContractGroup c : instance.getContractGroups()) {
			DirectedGraph<Node, Double> newGraph = new DirectedGraph<>();
			
			Node source = new Node(-1, -1);
			newGraph.addNode(source, -1);
			
			/*
			 * For each day in the time horizon of the basic schedule for contract group c
			 * 		Add all required nodes to the graph
			 * 		Create arcs to the node from the nodes of the previous day
			 */
			for (int t = 0; t < instance.getBasicSchedules().get(c).length; t++) {
				// Check if its a normal duty, reserve duty, ATV or rest day
				String type = instance.getBasicSchedules().get(c)[t];
				if (type.equals("ATV")) {
					Node newNode = new Node(t, 1);
					newGraph.addNode(newNode, t);
					
					// All nodes on the day before an ATV day should get an arc as always feasible
					for (Node prevNode : newGraph.getNodes().get(t-1)) {
						newGraph.addArc(prevNode, newNode, 0.0);
					}
				} else if (type.equals("Rest")) {
					Node newNode = new Node(t, 2);
					newGraph.addNode(newNode, t);
					
					// All nodes on the day before a rest day should get an arc as always feasible
					for (Node prevNode : newGraph.getNodes().get(t-1)) {
						newGraph.addArc(prevNode, newNode, 0.0);
					}
				} else if (type.substring(0, 1).equals("R")) {
					Node newNode = new Node(t, this.getReserveDutyType(type.substring(1), t).getNr());
					newGraph.addNode(newNode, t);
					
					for (Node prevNode : newGraph.getNodes().get(t-1)) {
						// All nodes from ATV or rest day should get an arc as always feasible
						if (t== 0) {
							newGraph.addArc(source, newNode, 0.0);
						} else if (prevNode.getDutyNr() == 1 || prevNode.getDutyNr() == 2) {
							newGraph.addArc(prevNode, newNode, 0.0);
						} else if (instance.getFromRDutyNrToRDuty().containsKey(prevNode.getDutyNr())) {
							// Check the 11 hour constraint
							if (instance.getFromRDutyNrToRDuty().get(newNode.getDutyNr()).getStartTime() + 
									(minPerDay - instance.getFromRDutyNrToRDuty().get(prevNode.getDutyNr()).getEndTime()) >= minBreakBetweenShifts) {
								newGraph.addArc(prevNode, newNode, 0.0);
							}
						} else {
							// Check the 11 hour constraint
							if (instance.getFromRDutyNrToRDuty().get(newNode.getDutyNr()).getStartTime() + 
									(minPerDay - instance.getFromDutyNrToDuty().get(prevNode.getDutyNr()).getEndTime()) >= minBreakBetweenShifts) {
								newGraph.addArc(prevNode, newNode, 0.0);
							}
						}
					}
				} else {
					// This day is another duty
					Set<Duty> toCreate = new HashSet<>();
					int weekdayNr = t % 7;
					if (weekdayNr == 0) {
						toCreate = instance.getDutiesPerTypeSun().get(type);
					} else if (weekdayNr == 6) {
						toCreate = instance.getDutiesPerTypeSat().get(type);
					} else {
						toCreate = instance.getDutiesPerTypeW().get(type);
					}
					
					for (Duty duty : toCreate) {
						Node newNode = new Node(t, duty.getNr());
						newGraph.addNode(newNode, t);
						
						for (Node prevNode : newGraph.getNodes().get(t-1)) {
							// All nodes from ATV or rest day should get an arc as always feasible
							if (t == 0) {
								newGraph.addArc(source, newNode, 0.0);
							} else if (prevNode.getDutyNr() == 1 || prevNode.getDutyNr() == 2) {
								newGraph.addArc(prevNode, newNode, 0.0);
							} else if (instance.getFromRDutyNrToRDuty().containsKey(prevNode.getDutyNr())) {
								// Check the 11 hour constraint
								if (instance.getFromDutyNrToDuty().get(newNode.getDutyNr()).getStartTime() + 
										(minPerDay - instance.getFromRDutyNrToRDuty().get(prevNode.getDutyNr()).getEndTime()) >= minBreakBetweenShifts) {
									newGraph.addArc(prevNode, newNode, 0.0);
								}
							} else {
								// Check the 11 hour constraint
								if (instance.getFromDutyNrToDuty().get(newNode.getDutyNr()).getStartTime() + 
										(minPerDay - instance.getFromDutyNrToDuty().get(prevNode.getDutyNr()).getEndTime()) >= minBreakBetweenShifts) {
									newGraph.addArc(prevNode, newNode, 0.0);
								}
							}
						}
					}
				}
			}
			
			Node sink = new Node(instance.getBasicSchedules().get(c).length, instance.getBasicSchedules().get(c).length);
			newGraph.addNode(sink, instance.getBasicSchedules().get(c).length);

			for (Node prevNode : newGraph.getNodes().get(instance.getBasicSchedules().get(c).length - 1)) {
				newGraph.addArc(prevNode, sink, 0.0);
			}

			System.out.println(c);
			System.out.println("Number of nodes: " + newGraph.getNumberOfNodes());
			System.out.println("Number of arcs: " + newGraph.getNumberOfArcs());

			this.removeNodes(newGraph, c);

			System.out.println("After removal: ");
			System.out.println("Number of nodes: " + newGraph.getNumberOfNodes());
			System.out.println("Number of arcs: " + newGraph.getNumberOfArcs());

			this.graphs.put(c, newGraph);
		}
	}

	/**
	 * This method removes the nodes from the directed graph that do not have any in or outgoing arcs.
	 * @param graph			the graph for which the node removal has to be executed
	 * @param c				the contract group of consideration
	 */
	public void removeNodes(DirectedGraph<Node, Double> graph, ContractGroup c) {
		boolean check = true;
		while (check) {
			check = false;
			for (int t = instance.getBasicSchedules().get(c).length - 1; t >= 0; t--) {
				Set<Node> toRemove = new HashSet<>();
				for (Node curNode : graph.getNodes().get(t)) {
					if (graph.getOutDegree(curNode) == 0) {
						toRemove.add(curNode);
						check = true;
					} else if (graph.getInDegree(curNode) == 0) {
						toRemove.add(curNode);
						check = true;
					}
				}
				for (Node curNode : toRemove) {
					graph.removeNode(curNode, t);
				}
			}
		}
	}

	/**
	 * This method executes the labelling algorithm.
	 * @return				a set with schedules with negative reduced costs
	 */
	public Map<ContractGroup, Set<Schedule>> executeLabelling() {
		Map<ContractGroup, Set<Schedule>> negRedCostSchedules = new HashMap<ContractGroup, Set<Schedule>>();

		for (ContractGroup c : instance.getContractGroups()) {
			Map<Node, Set<Label>> labelMap = new HashMap<Node, Set<Label>>();

			// Initialise the label at the source (t == -1)
			Set<Label> labels = new HashSet<>();
			ArrayList<Set<Integer>> dutiesOn = new ArrayList<>();
			for(int i = 0; i < 7; i++) {
				Set<Integer> newSet = new HashSet<>();
				dutiesOn.add(newSet);
			}
			for (Node source : graphs.get(c).getNodes().get(-1)) {
				labels.add(new Label(0, 0, 0, new int[instance.getBasicSchedules().get(c).length], dutiesOn));
				labelMap.put(source, labels);
			}

			// Execute labelling for all other nodes
			for (int t = 0; t < instance.getBasicSchedules().get(c).length; t++) {
				for (Node curNode : graphs.get(c).getNodes().get(t)) {
					labels = new HashSet<>();
					for (DirectedGraphArc<Node, Double> curArc : graphs.get(c).getInArcs(curNode)) {
						for (Label prevLabel : labelMap.get(curArc.getFrom())) {
							// Construct the label if it is feasible
							Label newLabel = this.getLabel(prevLabel, curArc, t, curNode.getDutyNr(), c);
							if (newLabel != null) {
								Set<Label> toRemove = new HashSet<>();
								boolean dominated = false;
								for (Label other : labels) {
									if (newLabel.dominates(other)) {
										toRemove.add(other);
									} else if (other.dominates(newLabel)) {
										dominated = true;
										break;
									}
								}
								if (!dominated) {
									labels.add(newLabel);
								}
								
								for (Label label : toRemove) {
									labels.remove(label);
								}
							}
						}
					}
					labelMap.put(curNode, labels);
				}
			
				Set<Node> removeMap = new HashSet<>();
				for (Node curNode : labelMap.keySet()) {
					if (curNode.getDay() < t) {
						removeMap.add(curNode);
					}
				}
				for (Node remove : removeMap) {
					labelMap.remove(remove);
				}
			}
			
			// For all labels at the sink with negative reduced costs, check 7x24 and 14x24 constraints from end to start
			Set<Schedule> schedules = new HashSet<>();
			for (Node curNode : labelMap.keySet()) {
				for (Label curLabel : labelMap.get(curNode)) {
					if (curLabel.getRedCosts() < 0) {
						if (this.isFeasibleSchedule(curLabel)) {
							schedules.add(new Schedule(c, curLabel.getTotMinus(), curLabel.getTotOvertime(), curLabel.getSchedule()));
						}
					}
				}
			}
			negRedCostSchedules.put(c, schedules);
		}

		return negRedCostSchedules;
	}
	
	/**
	 * This method decides whether feasibility constraints are met at this point and if so, returns the label.
	 * @param prevLabel				the previous label
	 * @param curArc				the arc to be traversed
	 * @param t						the day number
	 * @param dutyNr				the duty number
	 * @param c						the contract group of consideration
	 * @return						null if the schedule is not feasible or the new label
	 */
	public Label getLabel(Label prevLabel, DirectedGraphArc<Node, Double> curArc, int t, int dutyNr, ContractGroup c) {
		int[] schedule = this.copyIntArray(prevLabel.getSchedule());
		ArrayList<Set<Integer>> dutiesOn = prevLabel.getDutiesOn();
		boolean dutyNotYetTaken = true;
		if (dutyNr > 1000) {
			
			int max = (int) t/7;
			for (int w = 0; w < max; w++) {
				if (schedule[(7*w) + t%7] == dutyNr) {
					dutyNotYetTaken = false;
					break;
				}
			}
			
			//Cynthia: I have no idea why this one doesn't work and the above does... Probably something stupid
			/*
			if(dutiesOn.get(t%7).contains(dutyNr)) {
				dutyNotYetTaken = false;
			}*/
		}
		if (dutyNotYetTaken) {
			schedule[t] = dutyNr;
			if(dutyNr > 1000) {
				dutiesOn.get(t%7).add(dutyNr);
			}

			if (t >= 7) {
				/*
				 * For a schedule to be feasible: - at least one period of 32 hours free in the
				 * past 7X24 hours - at least 72 hours free in the past 14x24 hours of which at
				 * least 32 hours per period free
				 */

				// 7 x 14
				boolean feas7 = this.isFeasible7(schedule, t - 7);

				if (!feas7) {
					return null;
				}

				boolean feas14 = true;
				if (t >= 14) {
					feas14 = this.isFeasible14(schedule, t - 14);
				}

				if (feas7 && feas14) {
					if (t % 7 == 6) {
						int totHours = 0;
						for (int i = 0; i < 7; i++) {
							if (instance.getFromDutyNrToDuty().containsKey(schedule[t - i])) {
								totHours += instance.getFromDutyNrToDuty().get(schedule[t - i]).getPaidMin();
							} else if (instance.getFromRDutyNrToRDuty().containsKey(schedule[t - i])) {
								totHours += c.getAvgHoursPerDay() * 60;
							}
						}
						return new Label(prevLabel.getRedCosts() + curArc.getData(),
								prevLabel.getTotOvertime() + Math.max(0,
										(int) (totHours - 60 * (c.getAvgDaysPerWeek() * c.getAvgHoursPerDay()))),
								prevLabel.getTotMinus() + Math.max(0,
										(int) (60 * (c.getAvgDaysPerWeek() * c.getAvgHoursPerDay()) - totHours)),
								schedule, dutiesOn);
					} else {
						return new Label(prevLabel.getRedCosts() + curArc.getData(), prevLabel.getTotOvertime(),
								prevLabel.getTotMinus(), schedule, dutiesOn);
					}
				}

				return null;
			} else {
				if (t % 7 == 6) {
					int totHours = 0;
					for (int i = 0; i < 7; i++) {
						if (instance.getFromDutyNrToDuty().containsKey(schedule[t - i])) {
							totHours += instance.getFromDutyNrToDuty().get(schedule[t - i]).getPaidMin();
						} else if (instance.getFromRDutyNrToRDuty().containsKey(schedule[t - i])) {
							totHours += c.getAvgHoursPerDay() * 60;
						}
					}
					return new Label(prevLabel.getRedCosts() + curArc.getData(),
							prevLabel.getTotOvertime() + Math.max(0,
									(int) (totHours - 60 * (c.getAvgDaysPerWeek() * c.getAvgHoursPerDay()))),
							prevLabel.getTotMinus() + Math.max(0,
									(int) (60 * (c.getAvgDaysPerWeek() * c.getAvgHoursPerDay()) - totHours)),
							schedule, dutiesOn);
				} else {
					return new Label(prevLabel.getRedCosts() + curArc.getData(), prevLabel.getTotOvertime(),
							prevLabel.getTotMinus(), schedule, dutiesOn);
				}
			}
		}
		else {
			return null;
		}
	}
	
	/**
	 * This method returns whether a schedule is feasible from the end to the start of the schedule.
	 * @param label					the label of the schedule of consideration
	 * @return						true if the 7x24 and 14x24 constraints are met from the end to the start of the schedule
	 */
	public boolean isFeasibleSchedule(Label label) {		
		for (int t = label.getSchedule().length - 6; t < label.getSchedule().length; t++) {
			if (!this.isFeasible7(label.getSchedule(), t)) {
				return false;
			} else if (!this.isFeasible14(label.getSchedule(), t)) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * This method tests whether the 7x24 hour constraint is satisfied starting at day t
	 * @param schedule			the schedule
	 * @param t					the starting day to check the 7x24 constraint
	 * @return					whether the 7x24 hour constraint is satisfied or not
	 */
	public boolean isFeasible7(int[] schedule, int t) {
		// The constraint has to be tested from the start of a duty, so ATV and rest days can be skipped
		if (schedule[t] != 1 && schedule[t] != 2) {
			int start = 0;
			if (instance.getFromDutyNrToDuty().containsKey(schedule[t])) {
				start = instance.getFromDutyNrToDuty().get(schedule[t]).getStartTime();
			} else {
				start = instance.getFromRDutyNrToRDuty().get(schedule[t]).getStartTime();
			}
			
			for (int i = 1; i < 7; i++) {
				if (schedule[(t+i)%schedule.length] == 1 || schedule[(t+i)%schedule.length] == 2) {
					int consec = 24 * 60;
					
					if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+i-1)%schedule.length])) {
						consec += instance.getFromDutyNrToDuty().get(schedule[(t+i-1)%schedule.length]).getEndTime();
					} else {
						consec += instance.getFromRDutyNrToRDuty().get(schedule[(t+i-1)%schedule.length]).getEndTime();
					}
					
					if (i == 6) {
						if (schedule[(t+7)%schedule.length] == 1 || schedule[(t+7)%schedule.length] == 2) {
							consec += start;
						} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+7)%schedule.length])) {
							consec += Math.min(start, instance.getFromDutyNrToDuty().get(schedule[(t+7)%schedule.length]).getStartTime());
						} else {
							consec += Math.min(start, instance.getFromRDutyNrToRDuty().get(schedule[(t+7)%schedule.length]).getStartTime());
						}
					} else {
						if (schedule[(t+i+1)%schedule.length] == 1 || schedule[(t+i+1)%schedule.length] == 2) {
							consec += 24 * 60;
						} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+i+1)%schedule.length])) {
							consec += instance.getFromDutyNrToDuty().get(schedule[(t+i+1)%schedule.length]).getStartTime();
						} else {
							consec += instance.getFromRDutyNrToRDuty().get(schedule[(t+i+1)%schedule.length]).getStartTime();
						}
					}
					
					if (consec >= 32 * 60) {
						return true;
					}
				}
			}
		} else {
			return true;
		}
		
		return false;
	}
	
	/**
	 * This method tests whether the 14x24 hour constraint is satisfied starting at day t
	 * @param schedule			the schedule
	 * @param t					the starting day to check the 14x24 constraint
	 * @return					whether the 14x24 hour constraint is satisfied or not
	 */
	public boolean isFeasible14(int[] schedule, int t) {
		// The constraint has to be tested from the start of a duty, so ATV and rest days can be skipped
		if (schedule[t] != 1 && schedule[t] != 2) {
			int start = 0;
			if (instance.getFromDutyNrToDuty().containsKey(schedule[t])) {
				start = instance.getFromDutyNrToDuty().get(schedule[t]).getStartTime();
			} else {
				start = instance.getFromRDutyNrToRDuty().get(schedule[t]).getStartTime();
			}
			
			int consec14 = 0;
			
			for (int i = 1; i < 14; i++) {
				if ((schedule[(t+i)%schedule.length] == 1 || schedule[(t+i)%schedule.length] == 2) &&
						(schedule[(t+i-1)%schedule.length] != 1 && schedule[(t+i-1)%schedule.length] != 2)) {
					int consec = 24 * 60;
					
					if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+i-1)%schedule.length])) {
						consec += instance.getFromDutyNrToDuty().get(schedule[(t+i-1)%schedule.length]).getEndTime();
					} else {
						consec += instance.getFromRDutyNrToRDuty().get(schedule[(t+i-1)%schedule.length]).getEndTime();
					}
					
					if (i == 13) {
						if (schedule[(t+14)%schedule.length] == 1 || schedule[(t+14)%schedule.length] == 2) {
							consec += start;
						} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+14)%schedule.length])) {
							consec += Math.min(start, instance.getFromDutyNrToDuty().get(schedule[(t+14)%schedule.length]).getStartTime());
						} else {
							consec += Math.min(start, instance.getFromRDutyNrToRDuty().get(schedule[(t+14)%schedule.length]).getStartTime());
						}
					} else {
						if (schedule[(t+i+1)%schedule.length] == 1 || schedule[(t+i+1)%schedule.length] == 2) {
							consec += 24 * 60;
						} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+i+1)%schedule.length])) {
							consec += instance.getFromDutyNrToDuty().get(schedule[(t+i+1)%schedule.length]).getStartTime();
						} else {
							consec += instance.getFromRDutyNrToRDuty().get(schedule[(t+i+1)%schedule.length]).getStartTime();
						}
					}
					
					if (consec >= 32 * 60) {
						consec14 += consec;
					}
					
					if (consec14 >= 72 * 60) {
						return true;
					}
				}
			}
		} else {
			return true;
		}
		
		return false;
	}

	/**
	 * This method returns the reserve duty type to be considered for a certain day number and duty type
	 * @param type				the duty type
	 * @param t					the day number
	 * @return					the reserve duty type to be scheduled on that day
	 */
	public ReserveDutyType getReserveDutyType(String type, int t) {
		// First, obtain the day type from the day number
		int weekdayNr = t % 7;
		String dayType = "";
		if (weekdayNr == 0) {
			dayType = "Sunday";
		} else if (weekdayNr == 6) {
			dayType = "Saturday";
		} else {
			dayType = "Workingday";
		}

		// Then, find the reserve duty type that corresponds to this day type and duty type
		for (ReserveDutyType rDuty : instance.getReserveDutyTypes()) {
			if (rDuty.getDayType().equals(dayType) && rDuty.getType().equals(type)) {
				return rDuty;
			}
		}

		throw new IllegalArgumentException("There is no reserve duty type " + type + " on day type " + dayType);
	}

	public void updateDualCosts(double[] dualsContractGroup, List<HashMap<Integer, Double>> dualsDuties) {
		// Put the duals costs of the contractgroup on the arc from source to the nodes of day 0
		// For each ingoing arc, put the dual costs of that ingoing duty
		for (ContractGroup c : instance.getContractGroups()) {
			Node source = null;
			for (Node curNode : graphs.get(c).getNodes().get(-1)) {
				source = curNode;
			}
			for (DirectedGraphArc<Node, Double> outArc : graphs.get(c).getOutArcs(source)) {
				if (outArc.getTo().getDutyNr() != 1 && outArc.getTo().getDutyNr() != 2) {
					outArc.setData(-dualsContractGroup[c.getNr() - 1] - dualsDuties.get(0).get(outArc.getTo().getDutyNr()));
				} else {
					outArc.setData(-dualsContractGroup[c.getNr() - 1]);
				}
			}
			
			for (int t = 1; t < instance.getBasicSchedules().get(c).length; t++) {
				for (Node curNode : graphs.get(c).getNodes().get(t)) {
					for (DirectedGraphArc<Node, Double> inArc : graphs.get(c).getInArcs(curNode)) {
						if (dualsDuties.get(t%7).containsKey(curNode.getDutyNr())) {
							inArc.setData(-dualsDuties.get(t%7).get(curNode.getDutyNr()));
						}
					}
				}
			}
		}
	}
	
	/**
	 * This method creates a deep copy of an array.
	 * @param toCopy			the array to be copied
	 * @return					the copy of the array
	 */
	public int[] copyIntArray(int[] toCopy) {
		int[] copy = new int[toCopy.length];
		for (int i = 0; i < toCopy.length; i++) {
			copy[i] = toCopy[i];
		}
		return copy;
	}

	/**
	 * This method creates a deep copy of a map
	 * @param toCopy			the map to be copied
	 * @return					the copy of the map
	 */
	public HashMap<Integer, Integer> copyMap(HashMap<Integer, Integer> toCopy) {
		HashMap<Integer, Integer> copy = new HashMap<>();

		for (Integer key : toCopy.keySet()) {
			copy.put(key, toCopy.get(key));
		}

		return copy;
	}
}