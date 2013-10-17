import java.io.IOException;
import java.util.List;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;

public class StalkHandler extends LBHandler {
	final private Set<String> players;
	final private List<StalkRow> rows;
	private String levelId;
	private int playersFound;

	public StalkHandler(Set<String> players, List<StalkRow> rows, List<String> levelIds) {
		this.players = players;
		this.rows = rows;

		for (final String levelId : levelIds) {
			playersFound = 0;

			try {
				final SAXParserFactory factory = SAXParserFactory.newInstance();
				final SAXParser sp = factory.newSAXParser();
				this.levelId = levelId;
				sp.parse(Main.SITE + Main.STATS + levelId + Main.APPENDIX, this);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			} catch (ParserConfigurationException pce) {
				pce.printStackTrace();
			} catch (SAXException se) {
			}
			System.out.printf("Found %d %s on %s.%n", playersFound, playersFound == 1 ? "entry" : "entries",
					Main.getLevelName(Main.lbs.get(levelId)));
		}
	}

	public List<StalkRow> getRows() {
		return rows;
	}

	protected void addPlayer(Player p) throws SAXException {
		if (players.contains(p.id)) {
			rows.add(new StalkRow(levelId, p));
			playersFound++;
			if (playersFound == players.size()) {
				throw new SAXException();
			}
		}
	}
}