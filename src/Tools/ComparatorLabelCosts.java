package Tools;

import java.util.Comparator;

/**
 * Sorts the removals in descending order based on the costs of the arcs adjacent to the request in the current solution.
 * @author Mette Wagenvoort
 *
 */
public class ComparatorLabelCosts implements Comparator<Label>
{
	@Override
	public int compare(Label l1,Label l2) {
		return l1.compareToArcCosts(l2);
	}
}
