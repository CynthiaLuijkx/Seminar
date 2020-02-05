import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PricingProblem_Phase3 
{
	/*
	 * 1: ATV
	 * 2: Rest
	 * 100 - ?: Reserve
	 * Really big: Normal
	 */
	
	private final Instance instance;

	private HashMap<ContractGroup, DirectedGraph<Node, Double>> graphs;

	public PricingProblem_Phase3(Instance instance) {
		this.instance = instance;
	}
	
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
						if (prevNode.getDutyNr() == 1 || prevNode.getDutyNr() == 2) {
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
							if (prevNode.getDutyNr() == 1 || prevNode.getDutyNr() == 2) {
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

	public Map<ContractGroup, ArrayList<Schedule>> executeLabelling() {
		Map<ContractGroup, ArrayList<Schedule>> negRedCostSchedules = new HashMap<ContractGroup, ArrayList<Schedule>>();

		for (ContractGroup c : instance.getContractGroups()) {
			Map<Node, Set<Label>> labelMap = new HashMap<Node, Set<Label>>();

			// Initialise the label at the source (t == -1)
			Set<Label> labels = new HashSet<>();
			for (Node source : graphs.get(c).getNodes().get(-1)) {
				labels.add(new Label(0, 0, 0, new int[instance.getBasicSchedules().get(c).length]));
				labelMap.put(source, labels);
			}

			// Execute labelling for all other nodes
			for (int t = 0; t <= instance.getBasicSchedules().get(c).length; t++) {
				for (Node curNode : graphs.get(c).getNodes().get(t)) {
					labels = new HashSet<>();
					for (DirectedGraphArc<Node, Double> curArc : graphs.get(c).getInArcs(curNode)) {
						for (Label prevLabel : labelMap.get(curArc.getFrom())) {
							// Construct the label if it is feasible
//							Label newLabel = this.getLabel(curNode.getDutyNr(), prevLabel, curArc, t);
//							if (newLabel != null) {
//								
//							}
						}
					}
				}
			}
		}

		return negRedCostSchedules;
	}
	
	public Label getLabel(Label prevLabel, DirectedGraphArc<Node, Double> curArc, int t, int dutyNr) {		
		int[] schedule = this.copyIntArray(prevLabel.getSchedule());
		schedule[t] = dutyNr;
		
		return null;
	}

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

	public void updateDualCosts() {

	}
	
	public int[] copyIntArray(int[] toCopy) {
		int[] copy = new int[toCopy.length];
		for (int i = 0; i < toCopy.length; i++) {
			copy[i] = toCopy[i];
		}
		return copy;
	}

	public HashMap<Integer, Integer> copyMap(HashMap<Integer, Integer> toCopy) {
		HashMap<Integer, Integer> copy = new HashMap<>();

		for (Integer key : toCopy.keySet()) {
			copy.put(key, toCopy.get(key));
		}

		return copy;
	}
}
