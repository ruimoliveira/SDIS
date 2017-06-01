package peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class ClientHandler implements HttpHandler
{	
	Map<String, String> queryParams = new HashMap<String, String>();
	private static String charset = java.nio.charset.StandardCharsets.UTF_8.name();

	@Override
	public void handle(HttpExchange t)
	{
		String method = t.getRequestMethod();
		
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
		
		switch (method) {
		case "PUT":
			uploadFile(t);
			break;
		case "GET":
			if (queryParams.get("fileid") == null)
				listFiles(t);
			else
				downloadFile(t);			
			break;
		case "DELETE":
			deleteFile(t);
			break;		
		}
	}	

	private void downloadFile(HttpExchange t)
	{
		String fileId = queryParams.get("fileid");		

		InputStream is = null;
		OutputStream os = null;
		try {

			if (fileId == null) {
				t.sendResponseHeaders(400, -1);
				return;
			}

			/* Check for file locally */
			File dir = new File("Files");
			File file = new File(dir, fileId);

			if(file.exists() && !file.isDirectory())
			{
				is = new FileInputStream(file);
			}			
			else
			{
				/* Ask other peers for the file */
				boolean foundFile = false;
				String msgId = IdGenerator.nextId();
				Peer.addMessageReceived(msgId);
				InputStreamWrapper isw = new InputStreamWrapper();
				
				ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();				
				for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
				{					
					PeerInRange peer = entry.getValue();
					if(Requests.getFile(peer.getIp().getHostAddress(), peer.getPort(), fileId, msgId, isw) == 200){
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

		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			try { is.close(); } catch (IOException e) {}
			try { os.close(); } catch (IOException e) {}
		}
	}

	private void uploadFile(HttpExchange t)
	{
		String fileId = queryParams.get("fileid");
		if (fileId == null) {
			try {
				t.sendResponseHeaders(400, -1);
			} catch (IOException e) {
				e.printStackTrace();
			}
			return;
		}

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

		/* Forward file with other peers */
		String msgId = IdGenerator.nextId();
		Peer.addMessageReceived(msgId);
		
		ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
		for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
		{
			PeerInRange peer = entry.getValue();
			Thread thread = new Thread() {
				public void run(){
					Requests.putFile(peer.getIp().getHostAddress(), peer.getPort(), fileId, msgId);					
				}  
			};
			thread.start();
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

				Thread thread = new Thread() {
					public void run(){
						Requests.stored(peer.getIp().getHostAddress(), peer.getPort(), fileId, newStoredMsgId, msgId);
					}  
				};
				thread.start();				
			}
		}
		else{
			tempFile.delete();
			System.out.println("Deleted file: " + fileId);
		}
	}
		
	private void deleteFile(HttpExchange t)
	{
		try {
			String fileId = queryParams.get("fileid");
			
			if(fileId == null)
			{
				/* invalid args */
				t.sendResponseHeaders(400, -1);
				return;
			}
			
			/* Ask other peers to delete the file */
			String msgId = IdGenerator.nextId();
			Peer.addMessageReceived(msgId);					
			
			ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
			for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
			{
				PeerInRange peer = entry.getValue();
				Thread thread = new Thread() {
					public void run(){
						Requests.deleteFile(peer.getIp().getHostAddress(), peer.getPort(), fileId, msgId);
					}  
				};
				thread.start();
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

	private void listFiles(HttpExchange t)
	{
		/* Ask files to other peers */
		String msgId = IdGenerator.nextId();
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
		
		ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
		for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
		{
			PeerInRange peer = entry.getValue();
			ArrayList<String> filenames = Requests.getFilesList(peer.getIp().getHostAddress(), peer.getPort(), msgId);
			for(String filename : filenames){
				uniqueFiles.add(filename);
			}
		}
		
		OutputStream os = null;
		try {
			/* Send response headers */
			t.sendResponseHeaders(200, 0);			
			
			/* Send response body */
			os = t.getResponseBody();
			for(String file : uniqueFiles){
				file += "\n";
				os.write(file.getBytes());
			}
			//os.write("\n".getBytes());
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		finally{
			try { os.close(); }
			catch (IOException e) {	e.printStackTrace(); }
		}
	}
	
}
