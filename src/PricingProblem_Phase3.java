import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import Phase3.ArcData;
import Phase3.DirectedGraph;
import Phase3.DirectedGraphArc;
import Phase3.Node;
import Phase3.Path;
import Phase3.Pulse;
import Tools.ContractGroup;
import Tools.Duty;
import Tools.Instance;
import Tools.ReserveDutyType;
import Tools.Schedule;

public class PricingProblem_Phase3 
{
	private final Instance instance;
	private final int minBreakBetweenShifts;
	private final int consecFreeWeekly;
	private final int freeTwoWeeks;

	private HashMap<ContractGroup, DirectedGraph<Node, ArcData>> graphs;
	private HashMap<ContractGroup, List<Double>> overtimeSP;
	private HashMap<ContractGroup, List<Double>> redCostsSP;

	private Set<Schedule> finalSchedules;

	private int maxSchedules = 1;

	private long startTime;

	private Random random;

	public PricingProblem_Phase3(Instance instance, int minBreakBetweenShifts, int consecWeek, int twoWeek, long seed) {
		this.instance = instance;
		this.minBreakBetweenShifts = minBreakBetweenShifts;
		this.consecFreeWeekly = consecWeek;
		this.freeTwoWeeks = twoWeek;
		initGraphs();
		this.random = new Random(seed); //set a random seed 
	}

	/**
	 * This method executes the pulsing algorithm to find negative reduced costs schedules.
	 * @return				negative reduced costs schedules
	 */
	public Map<ContractGroup, Set<Schedule>> executePulse() {
		Map<ContractGroup, Set<Schedule>> negRedCostsSchedules = new HashMap<ContractGroup, Set<Schedule>>();
		redCostsSP = new HashMap<ContractGroup, List<Double>>();
		
		//Calculate the reduced costs of the shortest paths from every node to the end 
		for (ContractGroup c : instance.getContractGroups()) {
			Map<Node, Path> distances = this.shortestPathBackward(graphs.get(c), graphs.get(c).getNumberOfNodes()-1, 0, true);
			List<Double> redCosts = new ArrayList<>();
			for (int i = 0; i < graphs.get(c).getNumberOfNodes(); i++) {
				redCosts.add(distances.get(graphs.get(c).getNodes().get(i)).getCosts());
			}
			redCostsSP.put(c, redCosts);
		}

		for (ContractGroup c : instance.getContractGroups()) {			
			//Initialisation
			List<Set<Integer>> initDuties = new ArrayList<>();
			this.startTime = System.nanoTime();

			finalSchedules = new HashSet<>();
			for (int i = 0; i < 7; i++) {
				initDuties.add(new HashSet<>());
			}
			
			Pulse initPulse = new Pulse(0, 0, new int[instance.getBasicSchedules().get(c).length], initDuties, null);
			
			Node cur = graphs.get(c).getNodes().get(0);
			
			//Initialise pulse again and again until we have found a day that isn't a rest or an ATV duty 
			while (graphs.get(c).getOutArcs(cur).size() == 1) {
				DirectedGraphArc<Node, ArcData> curArc = graphs.get(c).getOutArcs(cur).get(0);
				List<Set<Integer>> newDuties = new ArrayList<>();
				
				//Copy the array and add the next duty 
				int[] schedule = this.copyIntArray(initPulse.getSchedule());
				schedule[curArc.getTo().getDayNr()] = curArc.getTo().getDutyNr();
				
				//Copy the set and add the next duty 
				for (int i = 0; i < 7; i++) {
					newDuties.add(this.copySet(initPulse.getDuties().get(i)));
				}
				if (instance.getFromDutyNrToDuty().containsKey(curArc.getTo().getDutyNr())) {
					newDuties.get(curArc.getTo().getDayNr()%7).add(curArc.getTo().getDutyNr());
				}
				
				initPulse = new Pulse(initPulse.getRedCosts() + curArc.getData().getDualCosts(), initPulse.getTotMinWorked() + curArc.getData().getPaidMin(), 
						schedule, newDuties, initPulse);
				cur = curArc.getTo();
			}
			
			
			Set<Schedule> toAdd = new HashSet<>();
			
			//Pick a random arc to extend the pulse from 
			Set<DirectedGraphArc<Node, ArcData>> evaluated = new HashSet<>();
			int nArcs = graphs.get(c).getOutArcs(cur).size();
			while (evaluated.size() < nArcs) {
				DirectedGraphArc<Node, ArcData> outArc = graphs.get(c).getOutArcs(cur).get(random.nextInt(nArcs));
					if (!evaluated.contains(outArc)) {
						pulse(graphs.get(c), c, outArc, initPulse);
						for (Schedule sched : finalSchedules) {
							toAdd.add(sched);
						}
						evaluated.add(outArc);
						finalSchedules = new HashSet<>();
					}
			}
			
			negRedCostsSchedules.put(c, toAdd);
		}

		return negRedCostsSchedules;
	}

	/**
	 * Recursive method for the pulsing algorithm: it decides whether the extension over the arc is possible and used pruning by feasibility and bound
	 * to terminate pulses.
	 * @param graph					the current graph
	 * @param c						the contract group
	 * @param curArc				the arc for which the traversal should be verified
	 * @param curPulse				the current pulse
	 */
	public void pulse(DirectedGraph<Node, ArcData> graph, ContractGroup c, DirectedGraphArc<Node, ArcData> curArc, Pulse curPulse) {
		//If we are at the final node
		if (curArc.getTo().getDayNr() == curPulse.getSchedule().length) {
			int totOvertime = this.isFeasibleSchedule(curPulse.getSchedule(), c);
			//This check isn't met if the schedule isn't feasible 
			if (totOvertime != Integer.MAX_VALUE) {
				this.finalSchedules.add(new Schedule(c, totOvertime, curPulse.getSchedule()));
			}
		} else if (finalSchedules.size() != maxSchedules && (System.nanoTime() - startTime)/1000000000.0 < 15) {
			// Check whether the new schedule would be feasible in terms of 7x24 and 14x24 hours
			int[] schedule = this.copyIntArray(curPulse.getSchedule());
			schedule[curArc.getTo().getDayNr()] = curArc.getTo().getDutyNr();
			int maxMin = (int) (schedule.length/7 * c.getAvgDaysPerWeek() * c.getAvgHoursPerDay() * 60);
			if (curArc.getTo().getDayNr() < 7 || this.isFeasible7(schedule, curArc.getTo().getDayNr() - 7)) {
				if (curArc.getTo().getDayNr() < 14 || this.isFeasible14(schedule, curArc.getTo().getDayNr() - 14)) {
					
					// Check whether the overtime will not exceed using basic SP
					if (curPulse.getTotMinWorked() + curArc.getData().getPaidMin() + this.overtimeSP.get(c).get(graph.getNodes().indexOf(curArc.getTo())) <= maxMin) {
						// Check whether the reduced cost will be negative using the basic SP 
						if (curPulse.getRedCosts() + curArc.getData().getDualCosts() + this.redCostsSP.get(c).get(graph.getNodes().indexOf(curArc.getTo())) < 0) {
							
							//Check whether the overtime will not exceed using the advanced SP 
							Path SP = this.shortestPath(graph, graph.getNodes().indexOf(curArc.getTo()), graph.getNumberOfNodes()-1, false, curPulse.getDuties());
							if (SP != null && (curPulse.getTotMinWorked() + curArc.getData().getPaidMin() + SP.getCosts()) <= maxMin) {
								
								// Check whether the reduced costs will be negative using the advanced SP
								SP = this.shortestPath(graph, graph.getNodes().indexOf(curArc.getTo()), graph.getNumberOfNodes()-1, true, curPulse.getDuties());
								if (SP != null && (curPulse.getRedCosts() + curArc.getData().getDualCosts() + SP.getCosts()) < -0.000001) {
									List<Set<Integer>> newDuties = new ArrayList<>();
									for (int i = 0; i < 7; i++) {
										newDuties.add(this.copySet(curPulse.getDuties().get(i)));
									}
									if (instance.getFromDutyNrToDuty().containsKey(curArc.getTo().getDutyNr())) {
										newDuties.get(curArc.getTo().getDayNr()%7).add(curArc.getTo().getDutyNr());
									}
									Pulse newPulse = new Pulse(curPulse.getRedCosts() + curArc.getData().getDualCosts(), curPulse.getTotMinWorked() + curArc.getData().getPaidMin(), 
											schedule, newDuties, curPulse);
									// If final node, store the pulse that is (potentially) feasible
									//Pick a random arc to extend the pulse from 
									Set<DirectedGraphArc<Node, ArcData>> evaluated = new HashSet<>();
									int nArcs = graph.getOutArcs(curArc.getTo()).size();
									while (evaluated.size() < nArcs) {
										DirectedGraphArc<Node, ArcData> outArc = graph.getOutArcs(curArc.getTo()).get(random.nextInt(nArcs));
										if (!newPulse.getDuties().get(outArc.getTo().getDayNr()%7).contains(outArc.getTo().getDutyNr())) {
											if (!evaluated.contains(outArc)) {
												this.pulse(graph, c, outArc, newPulse);
												evaluated.add(outArc);
											}
										} else {
											evaluated.add(outArc);
										}
									}
								}
							}
							else {
						//		System.out.println(c.getNr() + "  " + curArc.getTo().getDayNr() + " Specific overtime violated");
							}
						}
					}
					else {
					//	System.out.println(c.getNr() + "  " + curArc.getTo().getDayNr() + " Overtime violated");
					}
				}
				else {
				//	System.out.println(c.getNr() + "  " + curArc.getTo().getDayNr() + " Feasible 14");
					for(int i = 14; i > 0; i--) {
				//		System.out.print(curPulse.getSchedule()[curArc.getTo().getDayNr()-i] + " ");
					}
				//	System.out.println(instance.getBasicSchedules().get(c)[curArc.getTo().getDayNr()]);
				}
			}
			else {
			//	System.out.println(c.getNr() + "  " + curArc.getTo().getDayNr() + "Feasible 7");
			}
		}
	}
	
	/**
	 * This method returns whether a schedule is feasible from the end to the start of the schedule.
	 * @param label					the label of the schedule of consideration
	 * @return						the total number of hours overtime that have to be paid
	 */
	public int isFeasibleSchedule(int[] schedule, ContractGroup c) {
		//Check if the schedule is feasible w.r.t. the 7 and 14 day constraints 
		for (int t = schedule.length - 6; t < schedule.length; t++) {
			if (!this.isFeasible7(schedule, t)) {
				return Integer.MAX_VALUE;
			} else if (!this.isFeasible14(schedule, t)) {
				return Integer.MAX_VALUE;
			}
		}
		//Count the hours per week 
		int[] weeklyHours = new int[instance.getBasicSchedules().get(c).length / 7];
		for (int i = 0; i < weeklyHours.length; i++) {
			int totMin = 0;
			for (int j = 0; j < 7; j++) {
				//Reserve duty or ATV day 
				if (schedule[i * 7 + j] == 1 || instance.getFromRDutyNrToRDuty().containsKey(schedule[i * 7 + j])) {
					totMin += c.getAvgHoursPerDay() * 60;
				} else if (instance.getFromDutyNrToDuty().containsKey(schedule[i * 7 + j])) {
					totMin += instance.getFromDutyNrToDuty().get(schedule[i * 7 + j]).getPaidMin();
				}
			}
			weeklyHours[i] = totMin;
		}
		
		//Counter the total overtime
		int totMinWorked = 0;		
		int totOvertime = 0;
		for (int i = 0; i < weeklyHours.length; i++) {
			totMinWorked += weeklyHours[i];
			if (totMinWorked > weeklyHours.length * c.getAvgDaysPerWeek() * c.getAvgHoursPerDay() * 60) {
				//Abort early 
				return Integer.MAX_VALUE;
			}
			int totMin = 0;
			for (int j = 0; j < 13; j++) {
				totMin = weeklyHours[(i+j)%weeklyHours.length];
			}

			totOvertime += Math.max(0, totMin - 13 * c.getAvgDaysPerWeek() * c.getAvgHoursPerDay() * 60);
		}

		return totOvertime;
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
			//For all days from this day on
			for (int i = 1; i < 7; i++) {
				//Only check if this day is an ATV or Rest day
				if (schedule[(t+i)%schedule.length] == 1 || schedule[(t+i)%schedule.length] == 2) {
					int consec = 24 * 60;
					//Check the day before 
					if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+i-1)%schedule.length])) {//Normal duty
						consec += 24 * 60 - instance.getFromDutyNrToDuty().get(schedule[(t+i-1)%schedule.length]).getEndTime();
					} else {//Reserve duty
						consec += 24 * 60 - instance.getFromRDutyNrToRDuty().get(schedule[(t+i-1)%schedule.length]).getEndTime();
					}
					//The day after
					//If it's the last day 
					if (i == 6) {
						//We only count up until the start of the previous duty, or the new duty if it starts earlier 
						if (schedule[(t+7)%schedule.length] == 1 || schedule[(t+7)%schedule.length] == 2) {
							consec += start;
						} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+7)%schedule.length])) {
							consec += Math.min(start, instance.getFromDutyNrToDuty().get(schedule[(t+7)%schedule.length]).getStartTime());
						} else {
							consec += Math.min(start, instance.getFromRDutyNrToRDuty().get(schedule[(t+7)%schedule.length]).getStartTime());
						}
					} 
					//If it's any other day 
					else {
						if (schedule[(t+i+1)%schedule.length] == 1 || schedule[(t+i+1)%schedule.length] == 2) {
							consec += 24 * 60;
						} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+i+1)%schedule.length])) {
							consec += instance.getFromDutyNrToDuty().get(schedule[(t+i+1)%schedule.length]).getStartTime();
						} else {
							consec += instance.getFromRDutyNrToRDuty().get(schedule[(t+i+1)%schedule.length]).getStartTime();
						}
					}

					if (consec >= this.consecFreeWeekly) {
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
			//Get the start time
			int start = 0;
			if (instance.getFromDutyNrToDuty().containsKey(schedule[t])) {
				start = instance.getFromDutyNrToDuty().get(schedule[t]).getStartTime();
			} else {
				start = instance.getFromRDutyNrToRDuty().get(schedule[t]).getStartTime();
			}

			int consec14 = 0;
			//Count onwards 
			for (int i = 1; i < 14; i++) {
				if ((schedule[(t+i)%schedule.length] == 1 || schedule[(t+i)%schedule.length] == 2) &&
						(schedule[(t+i-1)%schedule.length] != 1 && schedule[(t+i-1)%schedule.length] != 2)) {
					int consec = 24 * 60;	

					//The previous day 
					if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+i-1)%schedule.length])) {
						consec += 24 * 60 - instance.getFromDutyNrToDuty().get(schedule[(t+i-1)%schedule.length]).getEndTime();
					} else {
						consec += 24 * 60 - instance.getFromRDutyNrToRDuty().get(schedule[(t+i-1)%schedule.length]).getEndTime();
					}
					//The next day 
					//If it's the last day of the period 
					if (i == 13) {
						if (schedule[(t+14)%schedule.length] == 1 || schedule[(t+14)%schedule.length] == 2) {
							consec += start;
						} else if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+14)%schedule.length])) {
							consec += Math.min(start, instance.getFromDutyNrToDuty().get(schedule[(t+14)%schedule.length]).getStartTime());
						} else {
							consec += Math.min(start, instance.getFromRDutyNrToRDuty().get(schedule[(t+14)%schedule.length]).getStartTime());
						}
					}
					//For all other days 
					else {
						if (instance.getFromDutyNrToDuty().containsKey(schedule[(t+i+1)%schedule.length])) {
							consec += instance.getFromDutyNrToDuty().get(schedule[(t+i+1)%schedule.length]).getStartTime();
						} else if (instance.getFromRDutyNrToRDuty().containsKey(schedule[(t+i+1)%schedule.length])) {
							consec += instance.getFromRDutyNrToRDuty().get(schedule[(t+i+1)%schedule.length]).getStartTime();
						} else {
							int j = 1;
							while (schedule[(t+i+j)%schedule.length] == 1 || schedule[(t+i+j)%schedule.length] == 2) {
								if (i+j == 14) {
									consec += start;
									break;
								}
								consec += 24 * 60;
								j++;
							}
						}
					}
					//If this period is bigger than the required amount per week, count it 
					if (consec >= this.consecFreeWeekly) {
						consec14 += consec;
					}

					if (consec14 >= this.freeTwoWeeks) {
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
	 * This method determines the shortest path, moving backwards through the graph from one node to the end. Also tracks costs found along the way 
	 * @param graph				the directed acyclic graph
	 * @param from				the starting node
	 * @param to				the final node
	 * @param costs				whether the source is the costs or the working hours. True = costs, False = working hours. 
	 * @return					Map of nodes to their shortest paths 
	 */
	public Map<Node, Path> shortestPathBackward(DirectedGraph<Node, ArcData> graph, int from, int to, boolean costs) {
		Map<Node, Path> distances = new HashMap<>();

		// Initialise for the from node
		List<Node> initPath = new ArrayList<>();
		initPath.add(graph.getNodes().get(from));
		distances.put(graph.getNodes().get(from), new Path(0, initPath));

		// For all nodes, starting from the from node, update the paths
		for (int i = from; i >= to; i--) {
			for (DirectedGraphArc<Node, ArcData> inArc : graph.getInArcs(graph.getNodes().get(i))) {
				if (distances.containsKey(inArc.getTo())) {
					double newCosts = distances.get(inArc.getTo()).getCosts();
					newCosts += inArc.getData().getDualCosts();
					if (!distances.containsKey(inArc.getFrom()) || newCosts < distances.get(inArc.getFrom()).getCosts()) {
						List<Node> newPath = this.copyNodeList(distances.get(inArc.getTo()).getSchedule());
						newPath.add(inArc.getFrom());
						distances.put(inArc.getFrom(), new Path(newCosts, newPath));
					}
				}
			}
		}

		return distances;
	}

	/**
	 * This method determines the shortest path between two nodes based on either the costs on the arc or the working hours.
	 * @param graph				the directed acyclic graph
	 * @param from				the starting node
	 * @param to				the final node
	 * @param costs				whether the source is the costs or the working hours. True = costs, False = working hours. 
	 * @return					the shortest Path
	 */
	public Path shortestPath(DirectedGraph<Node, ArcData> graph, int from, int to, boolean costs) {
		Map<Node, Path> distances = new HashMap<>();

		// Initialise for the from node
		List<Node> initPath = new ArrayList<>();
		initPath.add(graph.getNodes().get(from));
		distances.put(graph.getNodes().get(from), new Path(0, initPath));

		// For all nodes, starting from the from node, update the paths
		for (int i = from; i < to; i++) {
			for (DirectedGraphArc<Node, ArcData> outArc : graph.getOutArcs(graph.getNodes().get(i))) {
				if (distances.containsKey(outArc.getFrom())) {
					double newCosts = distances.get(outArc.getFrom()).getCosts();
					newCosts += outArc.getData().getData(costs);
					//Updates the costs if they're better, or if they haven't been calculated yet 
					if (!distances.containsKey(outArc.getTo()) || newCosts < distances.get(outArc.getTo()).getCosts()) {
						List<Node> newPath = this.copyNodeList(distances.get(outArc.getFrom()).getSchedule());
						newPath.add(outArc.getTo());
						distances.put(outArc.getTo(), new Path(newCosts, newPath));
					}
				}
			}
		}

		return distances.get(graph.getNodes().get(to));
	}
	
	/**
	 * This method determines the shortest path between two nodes based on either the costs on the arc or the working hours.
	 * @param graph				the directed acyclic graph
	 * @param from				the starting node
	 * @param to				the final node
	 * @param costs				whether the source is the costs or the working hours. True = costs, False = working hours. 
	 * @param forbidden			the list of for every day of the week, the duties the shortest path is not allowed to take
	 * @return					the shortest Path
	 */
	public Path shortestPath(DirectedGraph<Node, ArcData> graph, int from, int to, boolean costs, List<Set<Integer>> forbidden) {
		Map<Node, Path> distances = new HashMap<>();

		// Initialise for the from node
		List<Node> initPath = new ArrayList<>();
		initPath.add(graph.getNodes().get(from));
		distances.put(graph.getNodes().get(from), new Path(0, initPath));

		// For all nodes, starting from the from node, update the paths
		for (int i = from; i < to; i++) {
			for (DirectedGraphArc<Node, ArcData> outArc : graph.getOutArcs(graph.getNodes().get(i))) {
				//If this duty hasn't been taken on this day already 
				if (!forbidden.get(outArc.getTo().getDayNr()%7).contains(outArc.getTo().getDutyNr())) {
					if (distances.containsKey(outArc.getFrom())) {
						double newCosts = distances.get(outArc.getFrom()).getCosts();
						newCosts += outArc.getData().getData(costs);
						//Updates the costs if they're better, or if they haven't been calculated yet 
						if (!distances.containsKey(outArc.getTo()) || newCosts < distances.get(outArc.getTo()).getCosts()) {
							List<Node> newPath = this.copyNodeList(distances.get(outArc.getFrom()).getSchedule());
							newPath.add(outArc.getTo());
							distances.put(outArc.getTo(), new Path(newCosts, newPath));
						}
					}
				}
			}
		}

		return distances.get(graph.getNodes().get(to));
	}

	/**
	 * This method creates the graphs for the contract groups.
	 */
	public void initGraphs() {
		this.graphs = new HashMap<ContractGroup, DirectedGraph<Node, ArcData>>();
		this.overtimeSP = new HashMap<ContractGroup, List<Double>>();
		int minPerDay = 24 * 60;

		// Initialise the graphs for each contract group
		for (ContractGroup c : instance.getContractGroups()) {
			DirectedGraph<Node, ArcData> newGraph = new DirectedGraph<>();

			Node source = new Node(-1, -1);
			newGraph.addNode(source);

			/*
			 * For each day in the time horizon of the basic schedule for contract group c
			 * 			Add all required nodes to the graph
			 * 			Create arcs to the node from the nodes of the previous day
			 */
			for (int t = 0; t < instance.getBasicSchedules().get(c).length; t++) {
				// Check if its a normal duty, reserve duty, ATV or rest day
				String type = instance.getBasicSchedules().get(c)[t];
				
				//ATV days
				if (type.equals("ATV")) {
					Node newNode = new Node(t, 1); //ATV days get duty number 1 
					newGraph.addNode(newNode);

					// All nodes on the day before an ATV day should get an arc as always feasible, duty duration equals the average length of a shift
					for (Node prevNode : newGraph.getNodes()) {
						if (prevNode.getDayNr() == t - 1) {
							newGraph.addArc(prevNode, newNode, new ArcData(0, (int) Math.ceil(c.getAvgHoursPerDay() * 60)));
						}
					}
				}
				//Rest days
				else if (type.equals("Rest")) {
					Node newNode = new Node(t, 2); //Rest days get duty number 2
					newGraph.addNode(newNode);

					// All nodes on the day before a rest day should get an arc as always feasible, no working hours
					for (Node prevNode : newGraph.getNodes()) {
						if (prevNode.getDayNr() == t - 1) {
							newGraph.addArc(prevNode, newNode, new ArcData(0, 0));
						}
					}
				} 
				//Reserve duties
				else if (type.substring(0, 1).equals("R")) {
					Node newNode = new Node(t, this.getReserveDutyType(type.substring(1), t).getNr());
					newGraph.addNode(newNode);

					for (Node prevNode : newGraph.getNodes()) {
						//If we come from the source
						if (prevNode.equals(source) && t == 0) {
							newGraph.addArc(prevNode, newNode, new ArcData(0, (int) Math.ceil(c.getAvgHoursPerDay() * 60)));
						} 
						//If we don't come from the source
						else if (prevNode.getDayNr() == t - 1) {
							//If we come from an ATV day
							if (prevNode.getDutyNr() == 1) {
								newGraph.addArc(prevNode, newNode, new ArcData(0, (int) Math.ceil(c.getAvgHoursPerDay() * 60)));
							} 
							//Coming from a rest day
							else if (prevNode.getDutyNr() == 2) {
								newGraph.addArc(prevNode, newNode, new ArcData(0, (int) Math.ceil(c.getAvgHoursPerDay() * 60)));
							} 
							//Coming from another reserve duty
							else if (instance.getFromRDutyNrToRDuty().containsKey(prevNode.getDutyNr())) {
								// Check the 11 hour constraint
								if (instance.getFromRDutyNrToRDuty().get(newNode.getDutyNr()).getStartTime() + 
										(minPerDay - instance.getFromRDutyNrToRDuty().get(prevNode.getDutyNr()).getEndTime()) >= minBreakBetweenShifts) {
									newGraph.addArc(prevNode, newNode, new ArcData(0, (int) Math.ceil(c.getAvgHoursPerDay() * 60)));
								}
							} else {
								// Check the 11 hour constraint
								if (instance.getFromRDutyNrToRDuty().get(newNode.getDutyNr()).getStartTime() + 
										(minPerDay - instance.getFromDutyNrToDuty().get(prevNode.getDutyNr()).getEndTime()) >= minBreakBetweenShifts) {
									newGraph.addArc(prevNode, newNode, new ArcData(0, (int) Math.ceil(c.getAvgHoursPerDay() * 60)));
								}
							}
						}
					}
				} else {
					// This day is a normal duty
					Set<Duty> toCreate = new HashSet<>();
					int weekdayNr = t % 7;
					
					//Get the list of duties to create
					if (weekdayNr == 0) {
						toCreate = instance.getDutiesPerTypeSun().get(type);
					} else if (weekdayNr == 6) {
						toCreate = instance.getDutiesPerTypeSat().get(type);
					} else {
						toCreate = instance.getDutiesPerTypeW().get(type);
					}
					//For all of these duties
					for (Duty duty : toCreate) {
						Node newNode = new Node(t, duty.getNr());
						newGraph.addNode(newNode);

						for (Node prevNode : newGraph.getNodes()) {
							//From source
							if (prevNode.equals(source) && t == 0) {
								newGraph.addArc(prevNode, newNode, new ArcData(0, duty.getPaidMin()));
							} 
							else if (prevNode.getDayNr() == t - 1) {
								//From ATV or rest day 
								if (prevNode.getDutyNr() == 1 || prevNode.getDutyNr() == 2) {
									// Always add from an ATV or rest day
									newGraph.addArc(prevNode,  newNode, new ArcData(0, duty.getPaidMin()));
									
								//From a reserve duty
								} else if (instance.getFromRDutyNrToRDuty().containsKey(prevNode.getDutyNr())) {
									// Check the 11 hour constraint
									if (instance.getFromDutyNrToDuty().get(newNode.getDutyNr()).getStartTime() + 
											(minPerDay - instance.getFromRDutyNrToRDuty().get(prevNode.getDutyNr()).getEndTime()) >= minBreakBetweenShifts) {
										newGraph.addArc(prevNode,  newNode, new ArcData(0, duty.getPaidMin()));
									}
								//From a normal duty
								} else {
									// Check the 11 hour constraint
									if (instance.getFromDutyNrToDuty().get(newNode.getDutyNr()).getStartTime() + 
											(minPerDay - instance.getFromDutyNrToDuty().get(prevNode.getDutyNr()).getEndTime()) >= minBreakBetweenShifts) {
										newGraph.addArc(prevNode,  newNode, new ArcData(0, duty.getPaidMin()));
									}
								}
							}
						}
					}
				}
			}

			// Add the sink
			Node sink = new Node(instance.getBasicSchedules().get(c).length, 0);
			newGraph.addNode(sink);
			
			//Connect all arcs to the sink
			for (Node prevNode : newGraph.getNodes()) {
				if (prevNode.getDayNr() == instance.getBasicSchedules().get(c).length - 1) {
					newGraph.addArc(prevNode, sink, new ArcData(0, 0));
				}
			}

			System.out.println(c);
			System.out.println("Number of Nodes: " + newGraph.getNumberOfNodes());
			System.out.println("Number of Arcs: " + newGraph.getNumberOfArcs());

			this.removeNodes(newGraph, c, source, sink);

			System.out.println("Remove 'loose' Nodes:");
			System.out.println("Number of Nodes: " + newGraph.getNumberOfNodes());
			System.out.println("Number of Arcs: " + newGraph.getNumberOfArcs());

			//			this.shrinkGraphShortestPath(newGraph, c);
			//			
			//			System.out.println("Shortest Path Node and Arc Removal: ");
			//			System.out.println("Number of Nodes: " + newGraph.getNumberOfNodes());
			//			System.out.println("Number of Arcs: " + newGraph.getNumberOfArcs());

			graphs.put(c, newGraph);

			Map<Node, Path> distances = this.shortestPathBackward(graphs.get(c), graphs.get(c).getNumberOfNodes()-1, 0, false);
			List<Double> overtimes = new ArrayList<>();
			for (int i = 0; i < graphs.get(c).getNumberOfNodes(); i++) {
				overtimes.add(distances.get(graphs.get(c).getNodes().get(i)).getCosts());
			}
			overtimeSP.put(c, overtimes);
		}
	}

	/**
	 * This method checks if nodes or arcs can be removed based on the overtime shortest path.
	 * @param graph				the graph
	 * @param c					the contract group
	 */
	public void shrinkGraphShortestPath(DirectedGraph<Node, ArcData> graph, ContractGroup c) {
		boolean check = true;

		double maxMin = c.getAvgHoursPerDay() * 60 * c.getAvgDaysPerWeek() * instance.getBasicSchedules().get(c).length/7;

		while (check) {
			check = false;

			Set<Node> toRemoveNodes = new HashSet<>();
			// Check if a node can be removed based on shortest paths
			for (int i = 1; i < graph.getNumberOfNodes() - 1; i++) {
				Path toPath = this.shortestPath(graph, 0, i, false);
				Path fromPath = this.shortestPath(graph, i, graph.getNumberOfNodes() - 1, false);

				if (toPath.getCosts() + fromPath.getCosts() > maxMin) {
					toRemoveNodes.add(graph.getNodes().get(i));
					check = true;
				}
			}

			for (Node remove : toRemoveNodes) {
				graph.removeNode(remove);
			}

			Set<DirectedGraphArc<Node, ArcData>> toRemoveArcs = new HashSet<>();
			// Check if an arc can be removed based on shortest paths
			for (int i = 1; i < instance.getBasicSchedules().get(c).length - 1; i++) {
				for (DirectedGraphArc<Node, ArcData> outArc : graph.getOutArcs(graph.getNodes().get(i))) {
					Path toPath = this.shortestPath(graph, 0, i, false);
					Path fromPath = this.shortestPath(graph, graph.getNodes().indexOf(outArc.getTo()), graph.getNumberOfNodes() - 1, false);
					if (toPath.getCosts() + outArc.getData().getData(false) + fromPath.getCosts() > maxMin) {
						toRemoveArcs.add(outArc);
						check = true;
					}
				}
			}

			for (DirectedGraphArc<Node, ArcData> remove : toRemoveArcs) {
				graph.removeArc(remove);
			}
		}
	}

	/**
	 * This method removes 'loose' nodes (i.e. nodes that have no in or out degree and that are not the source or sink) from the graph 
	 * and the corresponding arcs. 
	 * @param graph					the directed acyclic graph
	 * @param c						the contract group
	 * @param source				the source
	 * @param sink					the sink
	 */
	public void removeNodes(DirectedGraph<Node, ArcData> graph, ContractGroup c, Node source, Node sink) {
		boolean check = true; //Whether anything has to be removed 
		while (check) {//Loop until we have no more removals 
			check = false;
			Set<Node> toRemove = new HashSet<>();
			for (Node curNode : graph.getNodes()) {
				if (graph.getOutDegree(curNode) == 0 && !curNode.equals(sink)) {//If there are no outgoing arcs
					toRemove.add(curNode);
					check = true;
				} else if (graph.getInDegree(curNode) == 0 && !curNode.equals(source)) {//If there are no incoming arcs 
					toRemove.add(curNode);
					check = true;
				}
			}
			//Remove everything
			for (Node curNode : toRemove) {
				graph.removeNode(curNode);
			}
		}
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
		// Put the dual costs of the contractgroup on the arc from source to the nodes of day 0
		// For each ingoing arc, put the dual costs on that ingoing duty
		for (ContractGroup c : instance.getContractGroups()) {
			for (DirectedGraphArc<Node, ArcData> curArc : graphs.get(c).getArcs()) {
				if (curArc.getFrom().getDayNr() == -1) { //If this node is the source
					if (instance.getFromDutyNrToDuty().containsKey(curArc.getTo().getDutyNr())) {
						curArc.getData().setDualCosts(-dualsContractGroup[c.getNr() - 1] - dualsDuties.get(0).get(curArc.getTo().getDutyNr()));
					} else {
						curArc.getData().setDualCosts(-dualsContractGroup[c.getNr() - 1]);
					}
				//For all nodes that aren't the source or the sink, and if it's a normal duty 
				} else if (curArc.getTo().getDayNr() != instance.getBasicSchedules().get(c).length && instance.getFromDutyNrToDuty().containsKey(curArc.getTo().getDutyNr())) {
					curArc.getData().setDualCosts(-dualsDuties.get(curArc.getTo().getDayNr()%7).get(curArc.getTo().getDutyNr()));
				}
			}
		}
	}

	/**
	 * This method creates a copy of a list with nodes.
	 * @param toCopy				the list to be copied
	 * @return						the copy of the list
	 */
	public List<Node> copyNodeList(List<Node> toCopy) {
		List<Node> copy = new ArrayList<>();
		for (Node curNode : toCopy) {
			copy.add(curNode);
		}
		return copy;
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
	 * This method creates a deep copy of a set.
	 * @param toCopy			the set to be copied
	 * @return					the copy of the set
	 */
	public Set<Integer> copySet(Set<Integer> toCopy) {
		Set<Integer> copy = new HashSet<>();
		for (Integer entry : toCopy) {
			copy.add(entry);
		}

		return copy;
	}
}