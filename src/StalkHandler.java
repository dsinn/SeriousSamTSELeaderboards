import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public class StalkHandler extends LBHandler {
	private String saxTemp;

	final private Set<String> players;
	final private List<StalkRow> rows;
	private String steamid, level;
	private int score, rank, playersFound;

	public StalkHandler(Set<String> players, List<StalkRow> rows,
			List<String> levelIds) {
		this.players = players;
		this.rows = rows;

		for (final String lid : levelIds) {
			steamid = null;
			score = -1;
			playersFound = 0;

			try {
				final SAXParserFactory factory = SAXParserFactory.newInstance();
				final SAXParser sp = factory.newSAXParser();
				sp.parse(Main.SITE + Main.STATS + lid + Main.APPENDIX, this);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (ParserConfigurationException pce) {
				pce.printStackTrace();
			} catch (SAXException se) {
				// System.out.println(se);
			}
			System.out.printf("Found %d entries on %s.%n", playersFound,
					Main.getLevelName(Main.lbs.get(lid)));
		}
	}

	public List<StalkRow> getRows() {
		return rows;
	}

	protected void addPlayer(Player p) throws SAXException {
		if (players.contains(steamid)) {
			rows.add(new StalkRow(level, new Player(steamid, rank, score,
					saxTemp)));
			playersFound++;
			if (playersFound == players.size()) {
				throw new SAXException();
			}
		}
	}
}