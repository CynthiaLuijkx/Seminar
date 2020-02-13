package Tools;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This method contains a path in the directed graph (duplicates allowed).
 * @author Mette Wagenvoort
 *
 */
public class Path 
{
	private final double costs;
	private final List<Node> schedule;
	
	public Path(double costs, List<Node> schedule) {
		this.costs = costs;
		this.schedule = schedule;
	}

	public double getCosts() {
		return costs;
	}

	public List<Node> getSchedule() {
		return schedule;
	}

	@Override
	public String toString() {
		return "Path [costs=" + costs + ", schedule=" + schedule + "]";
	}
}