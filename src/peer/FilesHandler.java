package peer;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class FilesHandler implements HttpHandler {

	private static String charset = java.nio.charset.StandardCharsets.UTF_8.name();

	@Override
	public void handle(HttpExchange t) throws IOException
	{
		/* Parse request query */
		Map<String, String> queryParams = new HashMap<String, String>();
		try {
			String query = t.getRequestURI().getQuery();
			if (query != null){
				query = java.net.URLDecoder.decode(query, charset);
				queryParams = HttpHandlerUtil.queryToMap(query);
			}
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		};	
		
		String msgId = queryParams.get("msg");
		String peerId = queryParams.get("peer");
		
		if(msgId == null || peerId == null)
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
		
		Peer.addMessageReceived(msgId);
		
		HashSet<String> uniqueFiles = new HashSet<String>();
		
		/* View local files */
		File dir = new File("Files");
		if(dir.exists() && dir.isDirectory())
		{
			File[] listOfFiles = dir.listFiles();

			for (File file : listOfFiles) {
				if (file.isFile()) {
					uniqueFiles.add(file.getName());
					System.out.println("File found: " + file.getName());
				} else if (file.isDirectory()) {
					System.out.println("Directory found: " + file.getName());
				}
			}
		}
		
		/* Ask files to other peers */
		ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
		for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
		{
			PeerInRange peer = entry.getValue();
			if(!peer.getId().equals(peerId)){
				ArrayList<String> filenames = Requests.getFilesList(peer.getIp().getHostAddress(), peer.getPort(), msgId);
				for(String filename : filenames){
					uniqueFiles.add(filename);
				}
			}
		}
		
		OutputStream os = null;
		try {
			/* Send response headers */
			t.sendResponseHeaders(200, 0);			
			
			/* Send response body */
			os = t.getResponseBody();
			for(String filename : uniqueFiles){
				filename += "\n";
				os.write(filename.getBytes());
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try { os.close(); }
			catch (IOException e) {	e.printStackTrace(); }
		}
	}
}
