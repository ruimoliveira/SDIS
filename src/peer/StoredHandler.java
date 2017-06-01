package peer;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class StoredHandler implements HttpHandler
{
	private static String charset = java.nio.charset.StandardCharsets.UTF_8.name();
	
	@Override
	public void handle(HttpExchange t) throws IOException
	{
		/* Parse request query */
		Map<String, String> queryParams = new HashMap<String, String>();

		String query = t.getRequestURI().getQuery();
		if (query != null){
			query = java.net.URLDecoder.decode(query, charset);
			queryParams = HttpHandlerUtil.queryToMap(query);
		}

		String fileId = queryParams.get("fileid");
		String msgId = queryParams.get("msg");
		String putMsgId = queryParams.get("putmsg");
		String peerId = queryParams.get("peer");
		
		if(fileId == null || msgId == null || putMsgId == null || peerId == null)
		{
			/* invalid args */
			t.sendResponseHeaders(400, -1);
			return;
		}

		if(Peer.isMessageReceived(msgId))
		{
			/* already received message */
			t.sendResponseHeaders(205, -1);
			return;
		}
		
		System.out.println("Stored received: " + fileId);
		
		Peer.addMessageReceived(msgId);
		Peer.addStoredReceived(putMsgId);
		
		ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
		for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
		{
			PeerInRange peer = entry.getValue();
			if(!peer.getId().equals(peerId)){
				Thread thread = new Thread() {
					public void run(){
						Requests.stored(peer.getIp().getHostAddress(), peer.getPort(), fileId, msgId, putMsgId);					
					}  
				};
				thread.start();
			}
		}
	}
}
