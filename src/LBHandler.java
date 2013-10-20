import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class LBHandler extends DefaultHandler {
	private String saxTemp;
	private String steamid;
	private int score, rank;
	protected long t1;

	public LBHandler() {
	}

	/**
	 * Parses a leaderboard.
	 * 
	 * @param id
	 *            the leaderboard's ID
	 * @param limit
	 *            the number of players from which to pick up data
	 * @return a list of the Leaders
	 */
	public LBHandler(String id) {
		execute(id);
	}

	public void execute(String id) {
		steamid = null;
		score = -1;
		t1 = System.currentTimeMillis();
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser sp = factory.newSAXParser();
			sp.parse(Main.SITE + Main.STATS + id + Main.APPENDIX, this);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			System.out.println(se);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		saxTemp = new String(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("steamid")) {
			steamid = saxTemp;
		} else if (qName.equalsIgnoreCase("score")) {
			score = Integer.parseInt(saxTemp);
		} else if (qName.equalsIgnoreCase("rank")) {
			rank = Integer.parseInt(saxTemp);
		} else if (qName.equalsIgnoreCase("details")) {
			addPlayer(new Player(steamid, rank, score, saxTemp));
		}
	}

	protected abstract void addPlayer(Player p) throws SAXException;
}
