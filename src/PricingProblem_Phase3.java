import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PricingProblem_Phase3 
{
	private final Instance instance;
	
	private Set<DirectedGraph<Node, ArcData>> graphs;
	
	public PricingProblem_Phase3(Instance instance) {
		this.instance = instance;
	}
	
	public void initGraphs() {
		this.graphs = new HashSet<DirectedGraph<Node, ArcData>>();
		int minPerDay = 24 * 60;
		int minBreakBetweenShifts = 11 * 60;
		// Initialise the graphs for each contract group
		for (ContractGroup c : instance.getContractGroups()) {
			
			DirectedGraph<Node, ArcData> newGraph = new DirectedGraph<>();
			
			int nWeeks = instance.getBasicSchedules().get(c).length / 7;
			
			Node source = new Node(-1);
			newGraph.addNode(source, -1);
			
			/*
			 * For each day in the time horizon of the basic schedule for contract group c
			 * 		Add all required nodes to the graph
			 * 		Create arcs to the node from the nodes of the previous day
			 */
			for (int t = 0; t < instance.getBasicSchedules().get(c).length; t++) {
				// Check if its a normal duty or a reserve duty
				String type = instance.getBasicSchedules().get(c)[t];
				if (type.equals("ATV")) {
					// This day is an ATV day
					Node newNode = new Node(t, true, false);
					newGraph.addNode(newNode, t);
					
					// All nodes on the day before an ATV day should get an arc as always feasible
					if (t - 1 == -1) {
						newGraph.addArc(source, newNode, new ArcData(0, minPerDay, false, false, 0, (int) c.getAvgHoursPerDay() * 60));
					} else {
						for (Node prevNode : newGraph.getNodes().get(t-1)) {
							if (prevNode.isATV() || prevNode.isRest()) {
								newGraph.addArc(prevNode, newNode, new ArcData(0, minPerDay, prevNode.isATV(), prevNode.isRest(), 0, 
										(int) c.getAvgHoursPerDay() * 60));
							} else if (prevNode.getDuty() != null) {
								newGraph.addArc(prevNode, newNode, new ArcData(Math.abs(minPerDay - prevNode.getDuty().getEndTime()), 
										minPerDay, false, false, 0, (int) c.getAvgHoursPerDay() * 60));
							} else {
								newGraph.addArc(prevNode, newNode, new ArcData(Math.abs(minPerDay - prevNode.getReserveDutyType().getEndTime()), 
										minPerDay, false, false, 0, (int) c.getAvgHoursPerDay() * 60));
							}
						}
					}
				} else if (type.equals("Rest")) {
					// This day is a rest day
					Node newNode = new Node(t, false, true);
					newGraph.addNode(newNode, t);
					
					// All nodes on the day before a rest day should get an arc as always feasible
					if (t - 1 == -1) {
						newGraph.addArc(source, newNode, new ArcData(0, minPerDay, false, false, 0, 0));
					} else {
						for (Node prevNode : newGraph.getNodes().get(t-1)) {
							if (prevNode.isATV() || prevNode.isRest()) {
								newGraph.addArc(prevNode, newNode, new ArcData(0, minPerDay, prevNode.isATV(), prevNode.isRest(), 0, 0));
							} else if (prevNode.getDuty() != null) {
								newGraph.addArc(prevNode, newNode, new ArcData(Math.abs(minPerDay - prevNode.getDuty().getEndTime()), 
										minPerDay, false, false, 0, 0));
							} else {
								newGraph.addArc(prevNode, newNode, new ArcData(Math.abs(minPerDay - prevNode.getReserveDutyType().getEndTime()), 
										minPerDay, false, false, 0, 0));
							}
						}
					}
				} else if (type.substring(0, 1).equals("R")) {
					// This day is a reserve duty
					Node newNode = new Node(t, this.getReserveDutyType(type.substring(1), t));
					newGraph.addNode(newNode, t);
					
					// For each previous node, add an arc if 11 hours is satisfied
					if (t - 1 == -1) {
						newGraph.addArc(source, newNode, new ArcData(0,	newNode.getReserveDutyType().getStartTime(), false, false, 
								(int) c.getAvgHoursPerDay() * 60, (int) c.getAvgHoursPerDay() * 60));
					}
					for (Node prevNode : newGraph.getNodes().get(t-1)) {
						if (prevNode.isATV() || prevNode.isRest()) {
							newGraph.addArc(prevNode, newNode, new ArcData(0, newNode.getReserveDutyType().getStartTime(), false, false, 
									(int) c.getAvgHoursPerDay() * 60, (int) c.getAvgHoursPerDay() * 60));
						} else if (prevNode.getDuty() != null) {
							if (Math.abs(minPerDay - prevNode.getDuty().getEndTime()) + newNode.getReserveDutyType().getStartTime() >= minBreakBetweenShifts) {
								newGraph.addArc(prevNode, newNode, new ArcData(Math.abs(minPerDay - prevNode.getDuty().getEndTime()), 
										newNode.getReserveDutyType().getStartTime(), false, false, (int) c.getAvgHoursPerDay() * 60, 
										(int) c.getAvgHoursPerDay() * 60));
							}
						} else {
							if (Math.abs(minPerDay - prevNode.getReserveDutyType().getEndTime()) + newNode.getReserveDutyType().getStartTime() >= minBreakBetweenShifts) {
								newGraph.addArc(prevNode, newNode, new ArcData(Math.abs(minPerDay - prevNode.getReserveDutyType().getEndTime()), 
										newNode.getReserveDutyType().getStartTime(), false, false, (int) c.getAvgHoursPerDay() * 60, 
										(int) c.getAvgHoursPerDay() * 60));
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
						Node newNode = new Node(t, duty);
						newGraph.addNode(newNode, t);
						
						// For each previous node, add an arc if 11 hours is satisfied
						if (t - 1 == -1) {
							newGraph.addArc(source, newNode, new ArcData(0,	newNode.getDuty().getStartTime(), false, false, 
									newNode.getDuty().getWorkingMin(), newNode.getDuty().getPaidMin()));
						} else {
							for (Node prevNode : newGraph.getNodes().get(t-1)) {
								if (prevNode.isATV() || prevNode.isRest()) {
									newGraph.addArc(prevNode, newNode, new ArcData(0, newNode.getDuty().getStartTime(), false, false, 
											newNode.getDuty().getWorkingMin(), newNode.getDuty().getPaidMin()));
								} else if (prevNode.getDuty() != null) {
									if (Math.abs(minPerDay - prevNode.getDuty().getEndTime()) + newNode.getDuty().getStartTime() >= minBreakBetweenShifts) {
										newGraph.addArc(prevNode, newNode, new ArcData(Math.abs(minPerDay - prevNode.getDuty().getEndTime()), 
												newNode.getDuty().getStartTime(), false, false, newNode.getDuty().getWorkingMin(), newNode.getDuty().getPaidMin()));
									}
								} else {
									if (Math.abs(minPerDay - prevNode.getReserveDutyType().getEndTime()) + newNode.getDuty().getStartTime() >= minBreakBetweenShifts) {
										newGraph.addArc(prevNode, newNode, new ArcData(Math.abs(minPerDay - prevNode.getReserveDutyType().getEndTime()), 
												newNode.getDuty().getStartTime(), false, false, newNode.getDuty().getWorkingMin(), newNode.getDuty().getPaidMin()));
									}
								}
							}
						}
					}
				}
			}
			
			Node sink = new Node(instance.getBasicSchedules().get(c).length);
			newGraph.addNode(sink, instance.getBasicSchedules().get(c).length);
			
			for (Node prevNode : newGraph.getNodes().get(instance.getBasicSchedules().get(c).length - 1)) {
				if (prevNode.isATV() || prevNode.isRest()) {
					newGraph.addArc(prevNode, sink, new ArcData(0, 0, prevNode.isATV(), prevNode.isRest(), 0, 0));
				} else if (prevNode.getDuty() != null) {
					newGraph.addArc(prevNode, sink, new ArcData(Math.abs(minPerDay - prevNode.getDuty().getEndTime()), 0, false, false, 0, 0));
				} else {
					newGraph.addArc(prevNode, sink, new ArcData(Math.abs(minPerDay - prevNode.getReserveDutyType().getEndTime()), 0, false, false, 0, 0));
				}
			}
			
			System.out.println(c);
			System.out.println("Number of nodes: " + newGraph.getNumberOfNodes());
			System.out.println("Number of arcs: " + newGraph.getNumberOfArcs());
			
			this.removeNodes(newGraph, c);
			
			System.out.println("After removal: ");
			System.out.println("Number of nodes: " + newGraph.getNumberOfNodes());
			System.out.println("Number of arcs: " + newGraph.getNumberOfArcs());
			
			this.graphs.add(newGraph);
		}
	}
	
	public void removeNodes(DirectedGraph<Node, ArcData> graph, ContractGroup c) {
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
	
	public List<Schedule> executeLabelling() {
		List<Schedule> negRedCostSchedules = new ArrayList<>();
		
		
		
		return negRedCostSchedules;
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
}
