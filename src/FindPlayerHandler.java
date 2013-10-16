import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class FindPlayerHandler extends DefaultHandler {
	String saxTemp;

	private String targetid, steamid;
	private int score, rank;
	private Player p = null;

	/**
	 * Returns the details of the specified player for the specified
	 * leaderboard.
	 * 
	 * @param id
	 *            the leaderboard's ID
	 * @param limit
	 *            the number of players from which to pick up data
	 * @return a list of the Leaders
	 */
	protected Player getRanking(String lbid, String targetid) {
		this.targetid = targetid;

		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser sp = factory.newSAXParser();
			sp.parse(Main.SITE + Main.STATS + lbid + Main.APPENDIX, this);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			System.out.println(se);
		}

		return p;
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		saxTemp = new String(ch, start, length);
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equalsIgnoreCase("steamid")) {
			steamid = saxTemp;
		} else if (qName.equalsIgnoreCase("score")) {
			score = Integer.parseInt(saxTemp);
		} else if (qName.equalsIgnoreCase("rank")) {
			rank = Integer.parseInt(saxTemp);
		} else if (qName.equalsIgnoreCase("details")
				&& targetid.equals(steamid)) {
			p = new Player(steamid, rank, score, saxTemp);
			throw new SAXException();
		}
	}
}