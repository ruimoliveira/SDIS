package peer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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


		//Request Body
		InputStream is = t.getRequestBody();        
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			isr = new InputStreamReader(is,"utf-8");
			br = new BufferedReader(isr);

			int b;
			StringBuilder buf = new StringBuilder(512);
			while ((b = br.read()) != -1) {
				buf.append((char) b);
			}
			
			br.close();
			isr.close();
			
			System.out.println("Request Body:\n" + buf.toString());
			System.out.println();		
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

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
	
	private void put(HttpExchange t)
	{

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
}
