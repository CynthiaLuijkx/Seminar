package Tools;

/**
 * This class stores information on a violation of a constraint, i.e. it denotes which duties are included in this violation.
 * @author Mette Wagenvoort
 *
 */
public class Violation 
{
	private final String dayTypeFrom;
	private final String dayTypeTo;
	private final String typeFrom;
	private final String typeTo;
	private final boolean reserveFrom;
	private final boolean reserveTo;
	
	/**
	 * Constructs a Violation.
	 * @param typeFrom			duty type of the from duty
	 * @param dayTypeFrom		day type of the from duty
	 * @param reserveFrom		a boolean denoting whether the duty is a reserve duty or not
	 * @param typeTo			duty type of the to duty
	 * @param dayTypeTo			day type of the to duty
	 * @param reserveTo			a boolean denoting whether the duty is a reserve duty or not
	 */
	public Violation(String typeFrom, String dayTypeFrom, boolean reserveFrom, String typeTo, String dayTypeTo, boolean reserveTo) {
		this.dayTypeFrom = dayTypeFrom;
		this.dayTypeTo = dayTypeTo;
		this.reserveFrom = reserveFrom;
		this.reserveTo = reserveTo;
		if(this.reserveFrom) {
			String newTypeFrom = "R" + typeFrom;
			this.typeFrom = newTypeFrom;
		}
		else {
			this.typeFrom = typeFrom;
		}
		if(this.reserveTo) {
			this.typeTo = "R" + typeTo;
		}
		else {
			this.typeTo = typeTo;
		}
	}

	public String getDayTypeFrom() {
		return dayTypeFrom;
	}

	public String getDayTypeTo() {
		return dayTypeTo;
	}

	public String getTypeFrom() {
		return typeFrom;
	}

	public String getTypeTo() {
		return typeTo;
	}

	public boolean isReserveFrom() {
		return reserveFrom;
	}

	public boolean isReserveTo() {
		return reserveTo;
	}

	@Override
	public String toString() {
		return "Violation [dayTypeFrom=" + dayTypeFrom + ", dayTypeTo=" + dayTypeTo + ", typeFrom=" + typeFrom
				+ ", typeTo=" + typeTo + ", reserveFrom=" + reserveFrom + ", reserveTo=" + reserveTo + "]";
	}
}
