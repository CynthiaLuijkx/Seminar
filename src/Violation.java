
public class Violation 
{
	private final String dayTypeFrom;
	private final String dayTypeTo;
	private final String typeFrom;
	private final String typeTo;
	
	public Violation(String typeFrom, String dayTypeFrom, String typeTo, String dayTypeTo) {
		this.dayTypeFrom = dayTypeFrom;
		this.dayTypeTo = dayTypeTo;
		this.typeFrom = typeFrom;
		this.typeTo = typeTo;
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

	@Override
	public String toString() {
		return "Violation [dayTypeFrom=" + dayTypeFrom + ", typeFrom=" + typeFrom + ", dayTypeTo=" + dayTypeTo + ", typeFrom=" + typeFrom
				+ ", typeTo=" + typeTo + "]";
	}
}
