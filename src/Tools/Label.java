package Tools;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class Label 
{
	private final double redCosts;
	private final int totOvertime;
	private final int totMinus;
	private final int[] schedule;
	private final List<Set<Integer>> duties;
	
	public Label(double redCosts, int totOvertime, int totMinus, int[] schedule, List<Set<Integer>> duties) {
		this.redCosts = redCosts;
		this.totOvertime = totOvertime;
		this.totMinus = totMinus;
		this.schedule = schedule;
		this.duties = duties;
	}

	public double getRedCosts() {
		return redCosts;
	}

	public int getTotOvertime() {
		return totOvertime;
	}

	public int getTotMinus() {
		return totMinus;
	}

	public int[] getSchedule() {
		return schedule;
	}
	
	public List<Set<Integer>> getDuties() {
		return duties;
	}

	@Override
	public String toString() {
		return "Label [redCosts=" + redCosts + ", totOvertime=" + totOvertime + ", totMinus=" + totMinus + ", schedule="
				+ Arrays.toString(schedule) + "]";
	}
	
	/**
	 * This method returns true if this label dominates the other label.
	 * @param other			another label
	 * @return				a boolean denoting whether this label dominated the other label or not
	 */
	public boolean dominates(Label other) {
		if (this.redCosts <= other.getRedCosts()) {// && this.totOvertime <= other.getTotOvertime()){//&& this.totMinus <= other.getTotMinus()) {
				/*
				int counter = 0;
				int needed = 0;
				for (int i = 0; i < this.schedule.length; i++) {
					if (this.schedule[i] > 1000) {
						needed++;
						int weekday = i % 7;
						int max = (int) this.schedule.length/7;
						for(int j = 0; j < max; j++) {
							if(this.schedule[i] == other.schedule[(j*7) + weekday]) {
								counter++;
								break;
							}
						}							
					}
				}*/

				boolean containsAll = true;
				for(int i = 0; i < 7; i++) {
					for(Integer dutyNr : this.duties.get(i)) {
						//System.out.println(dutyNr);
						if(!other.duties.get(i).contains(dutyNr)) {
							containsAll = false;
							break;
						}
					}
				}
				if (containsAll) {
					//System.out.println("Met");
					return true;
				} 
			//if(counter == needed) {
			//	return true;
			//}
				else {
					return false;
				}
		}
		return false;
	}
	
	/**
	 * This method returns true if this label dominates the other label.
	 * @param other			another label
	 * @return				a boolean denoting whether this label dominated the other label or not
	 */
	public boolean dominates2(Label other) {
		if (this.redCosts <= other.getRedCosts() && this.totOvertime <= other.getTotOvertime() && 
				this.duties.containsAll(other.getDuties()) && other.getDuties().containsAll(this.duties) &&
				(this.redCosts < other.getRedCosts() || this.totOvertime < other.getTotOvertime())) {
			return true;
		}
		return false;
	}
}
