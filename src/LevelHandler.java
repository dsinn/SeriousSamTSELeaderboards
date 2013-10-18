import java.util.LinkedList;
import java.util.List;

import org.xml.sax.SAXException;

public class LevelHandler extends LBHandler {
	private List<Player> leaders;
	private int count, limit;

	public LevelHandler(String id, int limit) {
		leaders = new LinkedList<Player>();
		this.limit = limit;
		execute(id);
	}

	protected List<Player> getLeaders() {
		return leaders;
	}

	protected void addPlayer(Player p) throws SAXException {
		leaders.add(p);
		count++;
		if (count > limit) {
			System.out.printf("%nParsed leaderboard XML in %d ms.%n", System.currentTimeMillis() - t1);
			throw new SAXException("Finished collecting leaderboard data from " + limit + " entries.");
		}
	}
}
