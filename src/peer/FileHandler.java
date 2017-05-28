package peer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class FileHandler implements HttpHandler {

	Map<String, String> queryParams = new HashMap<String, String>();
	
	@Override
	public void handle(HttpExchange t)
	{
		String method = t.getRequestMethod();
		System.out.println("Request Method: " + method);

		//Request Headers
		Headers headers = t.getRequestHeaders();
		System.out.println("Request Headers:");
		for (Entry<String, List<String>> header : headers.entrySet()) {
			System.out.print("  " + header.getKey() + ": ");
			for (String str : header.getValue()) {
				System.out.print(str + " ");
			}
			System.out.println();
		}

		//Request query
		try {
			String query = t.getRequestURI().getQuery();
			if (query != null){
				query = java.net.URLDecoder.decode(query, "UTF-8");
				queryParams = HttpHandlerUtil.queryToMap(query);
				
				System.out.println("Request Query parameters:");
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
				//invalid args
				t.sendResponseHeaders(400, -1);
				return;
			}
			
			if(Peer.isMessageReceived(msgId))
			{
				//already received message
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
				//ask other peers for the file
				boolean foundFile = false; 
				ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
				for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
				{
					PeerInRange peer = entry.getValue();
					if(peerId != Peer.getId() && Requests.getFile(peer.getIp().getHostAddress(), peer.getPort(), fileId, msgId, is) == 200){
						foundFile = true;
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
	
	private void post(HttpExchange t)
	{

	}
	
	private void delete(HttpExchange t)
	{
		try {
			String fileId = queryParams.get("id");
			String msgId = queryParams.get("msg");
			String peerId = queryParams.get("peer");
			
			if(fileId == null || msgId == null || peerId == null)
			{
				//invalid args
				t.sendResponseHeaders(400, -1);
				return;
			}
			
			if(Peer.isMessageReceived(msgId))
			{
				//already received message
				t.sendResponseHeaders(205, -1);
				return;
			}
			
			Peer.addMessageReceived(msgId);
					
			//ask other peers to delete the file
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
			}
			
			t.sendResponseHeaders(200, -1);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return;
		}
	}
	
	@SuppressWarnings("resource")
	private void put(HttpExchange t)
	{
		Headers h = t.getRequestHeaders();
		String fileID = queryParams.get("id");
		String msgID = queryParams.get("msg");
		String senderID = queryParams.get("peer");
		String file_length = h.getFirst("File_Length");

		InputStream is = t.getRequestBody();

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
		for(Entry<String, Long> entry : Peer.messagesReceived.entrySet())
		{
			if (entry.getKey().compareTo(msgID) == 0) {
				try {
					t.sendResponseHeaders(205, 0);
				} catch (IOException e) {
					e.printStackTrace();
				}
				return;
			}
		}
		
		/* Create temporary file to share */
		File dir = new File("tempFiles" + Peer.getId());
		
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdir();

		File tempFile = new File(dir.getName() + "//" + fileID);
		try {
			tempFile.createNewFile();
		} catch (IOException e) {
			try { t.sendResponseHeaders(500, -1); } catch (IOException e1) {e1.printStackTrace();}
			e.printStackTrace();
			tempFile.delete();
			return;
		}
		
		OutputStream os = null;
	    try {
			os = new FileOutputStream(tempFile);
			byte[] body = new byte[(int) Long.parseLong(file_length)];
			int bytesRead;
			while ((bytesRead = is.read(body)) != -1) {
				os.write(body, 0, bytesRead);
			}
			is.read(body);
		} catch (FileNotFoundException e) {
			try {
				t.sendResponseHeaders(500, -1);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			tempFile.delete();
			return;
		} catch (IOException e) {
			try {
				t.sendResponseHeaders(400, -1);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
			tempFile.delete();
			return;
		}
		
		try { is.close(); } catch (IOException e) { }
		try { os.close(); } catch (IOException e) { }

		try {
			t.sendResponseHeaders(200, 0);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		os = t.getResponseBody();
		try {
			os.write(fileID.getBytes());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		try { os.close(); } catch (IOException e) { }

		/* Forward file with other peers */
		Peer.addMessageReceived(msgID);
		Peer.addStoredMessageReceived(msgID);
		try {
			Requests.forward(msgID, fileID, senderID, tempFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
