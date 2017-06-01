package peer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class FileHandler implements HttpHandler {

	Map<String, String> queryParams = new HashMap<String, String>();
	private static String charset = java.nio.charset.StandardCharsets.UTF_8.name();
	
	@Override
	public void handle(HttpExchange t)
	{
		String method = t.getRequestMethod();
		System.out.println("Request Method: " + method);
		
		/* Parse request query */
		try {
			String query = t.getRequestURI().getQuery();
			if (query != null){
				query = java.net.URLDecoder.decode(query, charset);
				queryParams = HttpHandlerUtil.queryToMap(query);
				
				System.out.println("Request query parameters:");
				for (Entry<String, String> param : queryParams.entrySet()){
					System.out.println("  " + param.getKey() + "=" + param.getValue());
				}
			}
		} catch (UnsupportedEncodingException e1) {
			e1.printStackTrace();
		};		

		switch(method)
		{
		case "GET": get(t);	break;
		case "PUT":	put(t);	break;
		case "POST": post(t); break;
		case "DELETE": delete(t); break;
		}
	}
	
	private void get(HttpExchange t)
	{
		InputStream is = null;
		OutputStream os = null;
		try {
			String fileId = queryParams.get("id");
			String msgId = queryParams.get("msg");
			String peerId = queryParams.get("peer");
			
			if(fileId == null || msgId == null || peerId == null)
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
					
			File dir = new File("Files");
			File file = new File(dir, fileId);
			
			if(file.exists() && !file.isDirectory())
			{
				is = new FileInputStream(file);
			}			
			else
			{
				/* ask other peers for the file */
				boolean foundFile = false; 
				InputStreamWrapper isw = new InputStreamWrapper();
				
				ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
				for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
				{
					PeerInRange peer = entry.getValue();
					if(!peer.getId().equals(peerId) && Requests.getFile(peer.getIp().getHostAddress(), peer.getPort(), fileId, msgId, isw) == 200){
						foundFile = true;
						is = isw.inputStream;
						break;					
					}
				}
				if(!foundFile){
					t.sendResponseHeaders(204, -1);
					return;
				}
			}

			t.sendResponseHeaders(200, 0);
			os = t.getResponseBody();

			byte[] bytes = new byte[1000];

			int n;
			while((n = is.read(bytes)) != -1){
				os.write(bytes, 0, n);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return;
		}
		finally{
			try { is.close(); } catch (IOException e) { }
			try { os.close(); } catch (IOException e) { }
		}
	}
	
	private void put(HttpExchange t)
	{
		String fileId = queryParams.get("id");
		String msgId = queryParams.get("msg");
		String peerId = queryParams.get("peer");
		
		if(fileId == null || msgId == null || peerId == null)
		{
			/* invalid args */
			try {
				t.sendResponseHeaders(400, -1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		if(Peer.isMessageReceived(msgId))
		{
			/* already received message */
			try {
				t.sendResponseHeaders(205, -1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}
		
		Peer.addMessageReceived(msgId);

		InputStream is = t.getRequestBody();
		
		/* Create file to share */
		File tempDir = new File("Temp");
	    if (! tempDir.exists()){
	    	tempDir.mkdir();
	    }
		File tempFile = new File(tempDir, fileId);

		/* Get file from message */
		OutputStream os = null;
	    try {
			os = new FileOutputStream(tempFile);
			byte[] body = new byte[1000];
			int bytesRead;
			while ((bytesRead = is.read(body)) != -1) {
				os.write(body, 0, bytesRead);
			}
		} catch (IOException e) {
			try {
				t.sendResponseHeaders(500, -1);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			tempFile.delete();
			return;
		}
	    finally{
			try { is.close(); } catch (IOException e) { }
			try { os.close(); } catch (IOException e) { }
	    }

		try {
			t.sendResponseHeaders(200, 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
		for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
		{
			PeerInRange peer = entry.getValue();
			if(!peer.getId().equals(peerId)){
				Thread thread = new Thread() {
					public void run(){
						Requests.putFile(peer.getIp().getHostAddress(), peer.getPort(), fileId, msgId);
					}  
				};
				thread.start();
			}
		}

		RandomGenerator.waitUpTo(5000);
		
		int nPeers = peersInRange.size();
		int nStored = Peer.getStoredCounter(msgId);
		System.out.println("Stored received: " + nStored);
		double odds = (double)(nPeers - nStored + 1)/(nPeers + 1);
		System.out.println("Odds to save file: " + odds);
		
		if(RandomGenerator.newFraction() < odds)
		{
			File dir = new File("Files");
		    if (!dir.exists()){
		    	dir.mkdir();
		    }
			tempFile.renameTo(new File(dir, fileId));
			System.out.println("Saved file: " + fileId);
			
			String newStoredMsgId = IdGenerator.nextId();
			Peer.addMessageReceived(newStoredMsgId);

			for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
			{
				PeerInRange peer = entry.getValue();
				if(!peer.getId().equals(peerId)){
					Thread thread = new Thread() {
						public void run(){
							Requests.stored(peer.getIp().getHostAddress(), peer.getPort(), fileId, newStoredMsgId, msgId);
						}  
					};
					thread.start();
				}
			}
		}
		else{
			tempFile.delete();
			System.out.println("Deleted file: " + fileId);
		}
	}
	
	private void post(HttpExchange t)
	{
		try {
			t.sendResponseHeaders(400, -1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void delete(HttpExchange t)
	{
		try {
			String fileId = queryParams.get("id");
			String msgId = queryParams.get("msg");
			String peerId = queryParams.get("peer");
			
			if(fileId == null || msgId == null || peerId == null)
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
					
			/* ask other peers to delete the file */
			ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
			for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
			{
				PeerInRange peer = entry.getValue();
				if(peerId != Peer.getId()){
					Requests.deleteFile(peer.getIp().getHostAddress(), peer.getPort(), fileId, msgId);
				}
			}
			
			File dir = new File("Files");
			File file = new File(dir, fileId);
			
			if(file.exists() && !file.isDirectory())
			{
				file.delete();
				System.out.println("Deleted file: " + fileId);
			}
			else{
				System.out.println("File \"" + fileId + "\" wasn't stored");
			}
			
			t.sendResponseHeaders(200, -1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return;
		}
	}	
}