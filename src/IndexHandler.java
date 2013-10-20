import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class IndexHandler extends DefaultHandler {
	private static HashMap<String, String> lbs = null;
	private String lbid;
	private String saxTemp;

	public static HashMap<String, String> getLeaderboards() {
		if (lbs == null) {
			new IndexHandler().makeLeaderboards();
		}
		return lbs;
	}

	private void makeLeaderboards() {
		try {
			lbs = new LinkedHashMap<String, String>();
			saxTemp = null;
			lbid = null;

			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser sp = factory.newSAXParser();
			sp.parse(Main.SITE + Main.STATS + Main.APPENDIX, this);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			se.printStackTrace();
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		saxTemp = new String(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if (qName.equalsIgnoreCase("lbid")) {
			lbid = saxTemp;
		} else if (qName.equalsIgnoreCase("name")) {
			lbs.put(lbid, saxTemp);
		}
	}
}
