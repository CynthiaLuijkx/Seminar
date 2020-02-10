import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public class Label 
{
	private final double redCosts;
	private final int totOvertime;
	private final int totMinus;
	private final int[] schedule;
	private final ArrayList<Set<Integer>> dutiesOn;
	
	public Label(double redCosts, int totOvertime, int totMinus, int[] schedule, ArrayList<Set<Integer>> dutiesOn) {
		this.redCosts = redCosts;
		this.totOvertime = totOvertime;
		this.totMinus = totMinus;
		this.schedule = schedule;
		this.dutiesOn = dutiesOn;
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
	
	public ArrayList<Set<Integer>> getDutiesOn(){
		return dutiesOn;
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
		if (this.totOvertime <= other.getTotOvertime() && this.redCosts <= other.getRedCosts()) {// && this.totOvertime <= other.getTotOvertime()){//&& this.totMinus <= other.getTotMinus()) {
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
			/*
				boolean containsAll = true;
				for(int i = 0; i < 7; i++) {
					for(Integer dutyNr : this.dutiesOn.get(i)) {
						//System.out.println(dutyNr);
						if(!other.dutiesOn.get(i).contains(dutyNr)) {
							containsAll = false;
							break;
						}
					}
				}
				if (containsAll) {
					//System.out.println("Met");
					return true;
				} */
			return true;
			//if(counter == needed) {
			//	return true;
			//}
			//	else {
			//		return false;
			//	}
		}
		return false;
	}
	
	/**
	 * This method returns true if this label dominates the other label.
	 * @param other			another label
	 * @return				a boolean denoting whether this label dominated the other label or not
	 */
	public boolean dominates2(Label other) {
		if (this.redCosts <= other.getRedCosts() && this.totOvertime <= other.getTotOvertime() && this.totMinus <= other.getTotMinus() && 
				(this.redCosts < other.getRedCosts() || this.totOvertime < other.getTotOvertime() || this.totMinus < other.getTotMinus() || (this.redCosts == other.getRedCosts() && this.totOvertime == other.getTotOvertime() && this.totMinus == other.getTotMinus()))) {
			return true;
		}
		return false;
	}
}
