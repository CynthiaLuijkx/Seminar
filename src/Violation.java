
public class Violation 
{
	private final String dayTypeFrom;
	private final String dayTypeTo;
	private final String typeFrom;
	private final String typeTo;
	private final boolean reserveFrom;
	private final boolean reserveTo;
	
	public Violation(String typeFrom, String dayTypeFrom, boolean reserveFrom, String typeTo, String dayTypeTo, boolean reserveTo) {
		this.dayTypeFrom = dayTypeFrom;
		this.dayTypeTo = dayTypeTo;
		this.typeFrom = typeFrom;
		this.typeTo = typeTo;
		this.reserveFrom = reserveFrom;
		this.reserveTo = reserveTo;
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
