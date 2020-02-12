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

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Violation other = (Violation) obj;
		if (dayTypeFrom == null) {
			if (other.dayTypeFrom != null)
				return false;
		} else if (!dayTypeFrom.equals(other.dayTypeFrom))
			return false;
		if (dayTypeTo == null) {
			if (other.dayTypeTo != null)
				return false;
		} else if (!dayTypeTo.equals(other.dayTypeTo))
			return false;
		if (reserveFrom != other.reserveFrom)
			return false;
		if (reserveTo != other.reserveTo)
			return false;
		if (typeFrom == null) {
			if (other.typeFrom != null)
				return false;
		} else if (!typeFrom.equals(other.typeFrom))
			return false;
		if (typeTo == null) {
			if (other.typeTo != null)
				return false;
		} else if (!typeTo.equals(other.typeTo))
			return false;
		return true;
	}
	
	
}