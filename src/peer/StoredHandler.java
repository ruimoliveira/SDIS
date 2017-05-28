package peer;
import java.io.File;
import java.io.IOException;
import java.util.Map.Entry;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class StoredHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange t) throws IOException {

		String method = t.getRequestMethod();
		
		switch (method) {
		case "POST":
			stored(t);
			break;
		}
	}

	private void stored(HttpExchange t) {
		Headers h = t.getRequestHeaders();
		String fileID = IdGenerator.nextId();

		/* Check if file exists before continuing to creation */
		File auxFile = new File("database" + Peer.getId() + "//" + fileID);
		if (auxFile.exists() && !auxFile.isDirectory() && auxFile.length()>0) {
			try {
				t.sendResponseHeaders(205, 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		/* Check if already received this transmission from another peer */
		String msgID = h.getFirst("Message_ID");

		boolean hasMessage = false;
		for(Entry<String, Long> entry : Peer.messagesReceived.entrySet())
		{
			if (entry.getKey().compareTo(msgID) == 0) {
				hasMessage = true;
				break;
			}
		}

		if (!hasMessage) {
			try {
				t.sendResponseHeaders(205, 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

		for(Entry<String, Integer> entry : Peer.storedMSGSreceived.entrySet())
		{
			if (entry.getKey().compareTo(msgID) == 0) {
				entry.setValue( entry.getValue() + 1 );
			}
		}
		
		try {
			t.sendResponseHeaders(200, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return;
		
	}
}
