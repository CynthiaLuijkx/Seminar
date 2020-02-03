//Class that contains a type Violation
public class Violation 
{
	private final String dayTypeFrom; //the day type (Saturday etc.) from which we check onwards 
	private final String dayTypeTo; //the day type (Saturday etc.) till which we check 
	private final String typeFrom; //the type of duty (Early, Late etc.) from which we check onwards
	private final String typeTo; //the type of duty (Early, Late etc.) till which we check
	
	//Constructor of the class
	public Violation(String typeFrom, String dayTypeFrom, String typeTo, String dayTypeTo) {
		this.dayTypeFrom = dayTypeFrom;
		this.dayTypeTo = dayTypeTo;
		this.typeFrom = typeFrom;
		this.typeTo = typeTo;
	}

	//Return methods
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
