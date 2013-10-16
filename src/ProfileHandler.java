import java.io.IOException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A Steam profile.
 */
public class ProfileHandler extends DefaultHandler {
	final public static FriendsListHandler FHL = new FriendsListHandler();
	final public static SAXException NOT_FOUND = new SAXException(
			"Profile not found.");
	private String saxTemp;
	final public String link;
	private String name, imgsrc, privacystate, steam64id = null;

	/**
	 * A Steam profile.
	 * 
	 * @param id
	 *            the id of the profile, which follows "id/" or "profiles/" in
	 *            the URL.
	 */
	public ProfileHandler(String id) throws SAXException {
		name = "<i>No profile</i>";
		imgsrc = "http://media.steampowered.com/steamcommunity/public/images/avatars/fe/fef49e7fa7e1997310d705b2a6158ff8dc1cdfeb.jpg";

		final String directory;
		if (id.matches("7656119\\d{10}")) {
			directory = "profiles";
		} else {
			directory = "id";
		}
		link = directory + "/" + id;

		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			final SAXParser sp = factory.newSAXParser();
			sp.parse(Main.SITE + link + Main.APPENDIX, this);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (ParserConfigurationException pce) {
			pce.printStackTrace();
		} catch (SAXException se) {
			if (se == NOT_FOUND) {
				throw NOT_FOUND;
			}
			// System.out.println(se);
		}
	}

	public void characters(char[] ch, int start, int length)
			throws SAXException {
		saxTemp = new String(ch, start, length);
	}

	public void endElement(String uri, String localName, String qName)
			throws SAXException {
		if (qName.equalsIgnoreCase("steamID") && saxTemp.trim().length() > 0) {
			name = saxTemp;
		} else if (qName.equalsIgnoreCase("privacystate")) {
			privacystate = saxTemp;
		} else if (qName.equalsIgnoreCase("avatarIcon")) {
			imgsrc = saxTemp;
			// System.out.print('.');
			throw new SAXException("Collected data from " + name);
		} else if (qName.equalsIgnoreCase("steamID64")) {
			steam64id = saxTemp;
		} else if (qName.equalsIgnoreCase("error")) {
			throw NOT_FOUND;
		}
	}

	public List<String> getFriends() {
		return FHL.get(Main.SITE + link);
	}

	/**
	 * Returns the current alias of the Steam account.
	 * 
	 * @return the current alias of the Steam account
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the current avatar image location of the Steam account.
	 * 
	 * @return the current avatar image location of the Steam account
	 */
	public String getImgsrc() {
		return imgsrc;
	}

	/**
	 * Returns the privacy state of the Steam profile.
	 * 
	 * @return
	 */
	public String getPrivacystate() {
		return privacystate;
	}

	public String getSteamId64() {
		return steam64id;
	}
}