public class Phase1_Penalties {

	private final int ATVSpreadPenaltyParam;
	private final int reservePenaltyParam;
	private final int tooManyConsecutiveDutiesParam;
	private final int consecutiveMaxPenaltyParam;
	private final int consecutiveMinPenaltyParam;
	private final int consecutiveRestParam;
	private final int earlyToLateParam;
	private final int partTimeParam;
	
	public Phase1_Penalties() {
		this.ATVSpreadPenaltyParam = 1;
		this.reservePenaltyParam = 1;
		this.tooManyConsecutiveDutiesParam = 1;
		this.consecutiveMaxPenaltyParam = 1;
		this.consecutiveMinPenaltyParam = 1;
		this.consecutiveRestParam = 1;
		this.earlyToLateParam = 1;
		this.partTimeParam = 1;
	}

	public int getATVSpreadPenaltyParam() {
		return ATVSpreadPenaltyParam;
	}

	public int getReservePenaltyParam() {
		return reservePenaltyParam;
	}

	public int getTooManyConsecutiveDutiesParam() {
		return tooManyConsecutiveDutiesParam;
	}

	public int getConsecutiveMaxPenaltyParam() {
		return consecutiveMaxPenaltyParam;
	}

	public int getConsecutiveMinPenaltyParam() {
		return consecutiveMinPenaltyParam;
	}

	public int getConsecutiveRestParam() {
		return consecutiveRestParam;
	}

	public int getEarlyToLateParam() {
		return earlyToLateParam;
	}

	public int getPartTimeParam() {
		return partTimeParam;
	}
}
