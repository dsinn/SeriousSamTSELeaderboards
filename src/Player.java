import java.util.Date;

/**
 * A player of a Serious Sam leaderboard.
 */
public class Player {
	final public static int HACKED = 2988, LEGIT = 1494;
	final public String id, details, difficulty;
	final public int rank, score, legitimacy, kills, killsPossible, secrets,
			secretsPossible, seconds, parSeconds, multiplier, saves;
	final public Date date;

	final protected static String[] difficulties = { "Tourist", "Easy",
			"Normal", "Hard", "Serious", "Mental" };

	/**
	 * A player entry in a leaderboard.
	 * 
	 * @param id
	 *            the Steam ID 64 of the player
	 * @param rank
	 *            the player's ranking for the leaderboard
	 * @param score
	 *            the player's score
	 * @param details
	 *            the hex data associated with this entry
	 */
	public Player(String id, int rank, int score, String details) {
		this.id = id;
		this.rank = rank;
		this.score = score;
		this.details = details;

		final int[] values = new int[11];
		for (int i = 0; i < values.length; i++) {
			values[i] = parseHex(i);
		}

		legitimacy = values[0];
		if (values[2] >= 0) {
			difficulty = difficulties[values[2]];
		} else {
			difficulty = null;
		}
		kills = values[3];
		killsPossible = values[4];
		secrets = values[5];
		secretsPossible = values[6];
		seconds = values[7];
		parSeconds = values[8];
		multiplier = values[9];
		saves = values[10];

		date = new Date((long) values[1] * 1000); // sec to ms
	}

	/**
	 * Returns true if the legitimacy code of this entry indicates that the
	 * player is hacking.
	 * 
	 * @return true if the legitimacy code of this entry indicates that the
	 *         player is hacking
	 */
	public boolean isHacking() {
		return legitimacy == HACKED;
	}

	private int parseHex(int field) {
		String hex = "";
		int limit = (field * 2 + 1) * 8;
		if (limit > details.length()) {
			return -1;
		}
		for (int i = limit + 6; i >= limit; i -= 2) {
			hex += details.substring(i, i + 2);
		}
		return Integer.parseInt(hex, 16);
	}
}
