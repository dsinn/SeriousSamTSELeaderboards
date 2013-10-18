import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.xml.sax.SAXException;

public class Main {
	final public static String SITE = "http://steamcommunity.com/";
	final public static String STATS = "stats/SSHD:SecondEncounter/leaderboards/";
	final public static String APPENDIX = "?xml=1";
	final public static int MAX_PLAYERS = 50;

	final public static String CSS_AVATAR = "avatar";
	final public static String CSS_FRAC = "fraction";
	final public static String CSS_HACKED = "hacked";
	final public static String CSS_NUMBER = "number";
	final public static String CSS_TIME = "time";

	final public static String OUTPUT_FILE = "SeriousSamLeaders.html";
	final public static String ENCODING = "UTF8";

	final protected static String[] gametypes = { "SinglePlayer", "Cooperative", "CooperativeCoinOp", "Survival",
			"TeamSurvival" };
	final protected static String[] medalNames = { "gold", "silver", "bronze" };

	final public static SimpleDateFormat SDF = new SimpleDateFormat("EEE, MMM dd, yyyy, hh:mm aaa");
	final public static String TZ = new SimpleDateFormat("z").format(new Date());
	final public static Pattern LOAD_PATTERN = Pattern.compile("[^\\\\][:=]");

	final protected static HashMap<String, String> lbs = IndexHandler.getLeaderboards();
	final protected static Properties levelNames = loadPropertiesFromResource("levelNames.txt");
	final protected static Properties medalTimes = loadPropertiesFromResource("medalTimes.txt");

	public static void main(String[] args) throws IOException, SAXException {
		final Scanner sn = new Scanner(System.in);

		System.out.println("1. Generate the leaderboard table for a specific level.");
		System.out.println("2. Find rankings of one or more players for each level of a specific gametype.");
		System.out.print("Select: ");
		final int mode = getNumberFromUser(sn, System.out, 2);

		for (int i = 0; i < gametypes.length; i++) {
			System.out.printf("%d. %s%n", i + 1, getGametypeName(gametypes[i]));
		}
		System.out.print("Select gametype number: ");
		final int gameId = getNumberFromUser(sn, System.out, gametypes.length) - 1;
		final String gametype = gametypes[gameId];
		System.out.println();

		// Find out which levels belong to the selected gametype
		final List<String> levelIds = new ArrayList<String>();
		for (final Object key0 : lbs.keySet()) {
			final String key = key0.toString();
			if (lbs.get(key).toString().startsWith(gametype + "_")) {
				levelIds.add(key);
			}
		}

		if (mode == 1) {
			generateLeaderboard(sn, levelIds, gametype, gameId);
		} else if (mode == 2) {
			stalkPlayers(sn, levelIds, gametype, gameId);
		}
		sn.close();
	}

	private static void generateLeaderboard(Scanner sn, List<String> levelIds, String gametype, int gameId)
			throws IOException, SAXException {
		for (int i = 0; i < levelIds.size(); i++) {
			final String levelCode = lbs.get(levelIds.get(i)).toString().substring(gametype.length() + 1);
			String levelName = levelNames.getProperty(levelCode);
			if (levelName == null) {
				levelName = levelCode;
			}
			System.out.printf("%2d. %s%n", i + 1, levelName);
		}
		System.out.print("Select level ID: ");
		String lbChoice = levelIds.get(getNumberFromUser(sn, System.out, levelIds.size()) - 1);

		System.out.printf("Enter number of players (max %d): ", MAX_PLAYERS);
		final int numLeaders = getNumberFromUser(sn, System.out, MAX_PLAYERS);

		final List<Player> leaders = new LevelHandler(lbChoice, numLeaders).getLeaders();
		outputLBHtml(leaders, gametype, gameId, lbs.get(lbChoice));
	}

	private static void stalkPlayers(Scanner sn, List<String> levelIds, String gametype, int gameId) throws IOException {
		System.out.print("Enter the number of players to track: ");
		final String[] pids = new String[getNumberFromUser(sn, System.out, 5000)];
		System.out.println("Enter the steam IDs for...");
		for (int i = 0; i < pids.length; i++) {
			System.out.print("Player " + (i + 1) + ": ");
			pids[i] = sn.nextLine().trim();
		}

		final Map<String, ProfileHandler> profiles = new HashMap<String, ProfileHandler>();
		for (final String pid : pids) {
			try {
				final ProfileHandler sp = new ProfileHandler(pid);
				profiles.put(sp.getSteamId64(), sp);
			} catch (SAXException e) {
				System.out.printf("Could not find profile for \"%s\"%n", pid);
			}
		}
		System.out.println("Looked up " + pids.length + " profile" + (pids.length == 1 ? "" : "s") + ".");
		if (profiles.size() == 0) {
			System.out.println("Failed to find any profiles. Terminating.");
			return;
		}

		final List<StalkRow> rows = new ArrayList<StalkRow>();
		final StalkHandler lh = new StalkHandler(profiles.keySet(), rows, levelIds);
		outputStalkerHtml(lh.getRows(), profiles, gameId);
	}

	/**
	 * Converts a number of seconds to hh:mm:ss format.
	 *
	 * @param totalSeconds
	 *            time in terms of seconds
	 * @return a String in hh:mm:ss format
	 */
	public static String formatSeconds(int totalSeconds) {
		final int seconds = totalSeconds % 60;
		int minutes = totalSeconds / 60;
		final int hours = minutes / 60;
		minutes %= 60;
		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
	}

	/**
	 * Converts a number of centiseconds to h?:mm:ss.cc format
	 *
	 * @param totalCentis
	 *            time in terms of centiseconds
	 * @return a String in h?:mm:ss.cc format
	 */
	protected static String formatCentis(int totalCentis) {
		final int centis = totalCentis % 100;
		int seconds = totalCentis / 100;
		int minutes = seconds / 60;
		final int hours = minutes / 60;
		seconds %= 60;
		if (hours > 0) {
			minutes %= 60;
			return String.format("%d:%02d:%02d.%02d", hours, minutes, seconds, centis);
		}
		return String.format("%02d:%02d.%02d", minutes, seconds, centis);
	}

	/**
	 * Returns all of the text in a file.
	 *
	 * @param path
	 *            the path of the file
	 * @return all of the text in a file
	 */
	protected static String copypaste(String path) {
		final StringBuffer sb = new StringBuffer();

		Scanner sn;
		try {
			sn = new Scanner(ClassLoader.getSystemClassLoader().getResourceAsStream(path));
		} catch (NullPointerException e) {
			try {
				sn = new Scanner(new File(path));
			} catch (FileNotFoundException e2) {
				return "";
			}
		}

		while (sn.hasNextLine()) {
			sb.append(String.format("%s%n", sn.nextLine()));
		}
		sn.close();
		return sb.toString();
	}

	/**
	 * Prompts the user to enter a number from 1 to limit
	 *
	 * @param sn
	 *            most likely a Scanner on System.in
	 * @param limit
	 *            the upper bound on the input
	 * @return a valid input
	 */
	public static int getNumberFromUser(Scanner sn, PrintStream ps, int limit) {
		while (true) {
			final String input = sn.nextLine();
			try {
				final int value = Integer.parseInt(input);
				if (0 < value && value <= limit) {
					return value;
				}
				ps.print("Number out of range.");
			} catch (NumberFormatException nfe) {
				ps.print("Not a number.");
			}
			ps.print(" Try again: ");
		}
	}

	/**
	 * Outputs the given leaderboard data into a file with the specified path.
	 *
	 * @param path
	 *            the location of the file to be created
	 * @param players
	 *            the player entries of the leaderboard
	 * @param gametype
	 *            the gametype name
	 * @param gameId
	 *            the gametype ID
	 * @param level
	 *            the map name
	 */
	public static void outputLBHtml(List<Player> players, String gametype, int gameId, String level)
			throws IOException, SAXException {
		final File file = new File(OUTPUT_FILE);
		final BufferedWriter bw = createBufferedWriter(file);

		bw.write(copypaste("start.txt"));
		bw.write("<table>");
		bw.write(String.format("<caption>%s: %s</caption>", getGametypeName(gametype), getLevelName(level)));
		bw.write("<tr>");
		printStandardHeadings(bw, gameId);
		bw.write("</tr>");

		System.out.println("Collecting profile data...");
		for (final Player player : players) {
			final ProfileHandler sp = new ProfileHandler(player.id);
			System.out.print('-');

			bw.write("<tr>");
			printStandardPlayerCells(bw, level, sp, player, gameId);
			bw.write("</tr>");
		}

		bw.write("</table>");
		bw.write(String.format("<div>Table generated on %s (%s)</div>", SDF.format(new Date()), TZ));
		bw.write(copypaste("end.txt"));
		bw.close();
		java.awt.Desktop.getDesktop().browse(file.toURI());
	}

	/**
	 * Outputs the given leaderboard data into a file with the specified path.
	 *
	 * @param path
	 *            the location of the file to be created
	 * @param rows
	 *            the entries of the leaderboard
	 * @param profiles
	 *            a map from Steam ID 64 to Profile objects
	 * @param gameId
	 *            the gametype ID
	 */
	public static void outputStalkerHtml(List<StalkRow> rows, Map<String, ProfileHandler> profiles, int gameId)
			throws IOException {
		final File file = new File(OUTPUT_FILE);
		final BufferedWriter bw = createBufferedWriter(file);
		bw.write(copypaste("start.txt"));
		bw.write("<table>");
		bw.write("<tr>");
		bw.write("<th>Level</th>");
		printStandardHeadings(bw, gameId);
		bw.write("</tr>");

		for (final StalkRow r : rows) {
			final ProfileHandler sp = profiles.get(r.player.id);
			final String level = lbs.get(r.levelId);
			bw.write("<tr>");
			bw.write("<td>" + getLevelName(level) + "</td>");
			printStandardPlayerCells(bw, level, sp, r.player, gameId);
			bw.write("</tr>");
		}

		bw.write("</table>");
		bw.write("<td>Table generated on " + SDF.format(new Date()) + " (" + TZ + ")</td>");
		bw.write(copypaste("end.txt"));
		bw.close();
		java.awt.Desktop.getDesktop().browse(file.toURI());
	}

	protected static void printStandardHeadings(BufferedWriter bw, int gameId) throws IOException {
		bw.write("<th>Rank</th>");
		bw.write("<th colspan=\"2\">Name</th>");
		bw.write("<th>Time and Date (" + TZ + ")</th>");

		if (gameId <= 2) {
			// SP, Coop, Coin-op
			bw.write("<th>Score</th>");
			bw.write("<th>Difficulty</th>");
			bw.write("<th>Kills</th>");
			bw.write("<th>Secrets</th>");
			bw.write("<th>Time played</th>");
			if (gameId == 0) {
				bw.write("<th>Saves</th>");
			}
		} else if (gameId <= 4) {
			// Survival, Team Survival
			bw.write("<th>Time</th>");
		} else {
			System.out.println("Unknown gameId " + gameId);
			return;
		}
	}

	protected static void printStandardPlayerCells(BufferedWriter bw, String level, ProfileHandler sp, Player player,
			int gameId) throws IOException {
		bw.write(String.format("<td class=\"%s\">%s</td>", CSS_NUMBER, player.rank));
		bw.write(String.format("<td class=\"avatar\"><img src=\"%s\" alt=\"\"></td>", sp.getImgsrc()));
		// Profile and stats if accessible
		bw.write("<td class=\"name\">");
		final String profile = Main.SITE + "profiles/" + player.id;
		bw.write(String.format("<a class=\"%s\" href=\"%s\">%s</a>", sp.getPrivacystate(), profile, sp.getName()));
		if (sp.getPrivacystate() != null && sp.getPrivacystate().matches("public|friendsonly")) {
			bw.write(String.format(" (<a class=\"%s\" href=\"%s\">stats</a>)", sp.getPrivacystate(), profile
					+ "/stats/SSHD:SecondEncounter"));
		}
		bw.write("</td>");

		bw.write("<td>" + SDF.format(player.date) + "</td>");
		if (gameId <= 2) {
			// SP, Coop, Coin-Op
			if (player.isHacking()) {
				bw.write(String.format("<td class=\"%s\" style=\"%s\">%,d</td>", "hacked", "text-align: right",
						player.score));
			} else {
				if (player.legitimacy != Player.LEGIT) {
					System.out.printf("%s has unknown legitimacy code %d.%n", sp.getName(), player.legitimacy);
				}
				bw.write(String.format("<td class=\"%s\">%,d</td>", CSS_NUMBER, player.score));
			}
			bw.write(String.format("<td>%s (x%d)</td>", player.difficulty, player.multiplier));
			bw.write(String
					.format("<td class=\"%s\">%,d&nbsp;/ %,d</td>", CSS_FRAC, player.kills, player.killsPossible));
			bw.write(String.format("<td class=\"%s\">%,d&nbsp;/ %,d</td>", CSS_FRAC, player.secrets,
					player.secretsPossible));
			bw.write(String.format("<td class=\"%s\">%s&nbsp;/ %s</td>", CSS_FRAC, formatSeconds(player.seconds),
					formatSeconds(player.parSeconds)));
			if (gameId == 0) {
				bw.write(String.format("<td class=\"%s\">%,d</td>", CSS_NUMBER, player.saves));
			}
		} else if (gameId <= 4) {
			// Survival, Team Survival
			final String score = formatCentis(player.score);
			if (player.isHacking()) {
				bw.write(String.format("<td class=\"%s %s\">%s</td>", CSS_TIME, CSS_HACKED, score));
			} else {
				// (Team) Survival medal times
				final int[] par = getParTimes(level);
				final String medal = getMedalName(par, player.score);
				bw.write(String.format("<td class=\"%s %s\">%s</td>", CSS_TIME, medal, score));
			}
		}
	}

	/**
	 * Returns a Properties object from the JAR file with the specified resource
	 * name.
	 *
	 * @param resource
	 *            The name of the serialized Properties file.
	 * @return a Properties object.
	 */
	public static Properties loadPropertiesFromResource(String resource) {
		final Properties ppt = new Properties();

		try {
			ppt.load(ClassLoader.getSystemClassLoader().getResourceAsStream(resource));
		} catch (IOException e) {
		} catch (NullPointerException e) {
		}
		try {
			ppt.load(new FileReader(resource));
		} catch (IOException e) {
		}

		return ppt;
	}

	protected static BufferedWriter createBufferedWriter(File file) throws IOException {
		final FileOutputStream fso = new FileOutputStream(file);
		final OutputStreamWriter osw = new OutputStreamWriter(fso, ENCODING);
		return new BufferedWriter(osw);
	}

	/**
	 * Returns the final title of the level with the given ID.
	 *
	 * @param level
	 *            the ID of the level
	 * @return the final title of the level
	 */
	public static String getLevelName(String level) {
		final String key = level.replaceFirst("^[^_]*_", "");
		if (levelNames.containsKey(key)) {
			return levelNames.getProperty(key);
		}
		return level;
	}

	private static int[] getParTimes(String level) {
		if (!medalTimes.containsKey(level)) {
			return new int[] {};
		}

		final String[] par0 = medalTimes.get(level).toString().split(" ");
		final int[] par = new int[par0.length];
		for (int i = 0; i < par.length; i++) {
			par[i] = Integer.parseInt(par0[i]);
		}
		return par;
	}

	public static String getMedalName(int[] par, int time) {
		for (int i = 0; i < par.length; i++) {
			if (time >= par[i]) {
				return medalNames[i];
			}
		}
		return "";
	}

	protected static String getGametypeName(String gametype) {
		return gametype.replaceAll("(?<=.)([A-Z])", " $1");
	}
}