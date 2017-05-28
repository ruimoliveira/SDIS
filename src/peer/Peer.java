package peer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.HttpServer;

@SuppressWarnings("restriction")
public class Peer
{
	private static String id = null;
	private static ConcurrentHashMap<String, PeerInRange> peersInRange = new ConcurrentHashMap<String, PeerInRange>();
			
	public static void main(String[] args) throws IOException
	{
		id = IdGenerator.nextId();
		int httpServerPort = Integer.parseInt(args[0]);
		
		//Initiate http server
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(httpServerPort), 0);
			server.createContext("/file", new FileHandler());
			server.createContext("/files", new FilesHandler());
			server.createContext("/test", new TestHandler());
			server.createContext("/client", new ClientHandler());
			server.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));
			server.start();
			System.out.println("HttpServer started");
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Initiate advertise thread
        Thread advertiseThread = new PeerAdvertiseThread("advertiser", 7000, httpServerPort);
        advertiseThread.start();
        
        //Initiate advertise thread
        Thread listenerThread = new PeerListenerThread("listener", 7000);
        listenerThread.start();
                
        while(peersInRange.isEmpty()){
        	try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
        testGetFile();
        
        while(true){
        	listPeersInRange();
        	//testServer();
        	try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
        }
    }
	
	protected static String getId(){
		return id;
	}
	
	protected static ConcurrentHashMap<String, PeerInRange> getPeersInRange()
	{
		return peersInRange;
	}
	
	private static void listPeersInRange()
	{
		System.out.println("Peers in range:");
		for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
		{
			PeerInRange peer = entry.getValue();
			System.out.println("Id: " + peer.getId() + ", Ip: " + peer.getIp().getHostAddress() + ", Port: " + peer.getPort());
		}
		System.out.println();
	}
	
	private static void testServer()
	{
		String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()

		for(Entry<String, PeerInRange> entry : peersInRange.entrySet())
		{
			PeerInRange peer = entry.getValue();
			String url = "http://" + peer.getIp().getHostAddress() + ":" + peer.getPort() + "/test";		

			try {

				HttpURLConnection httpConnection = null;
				OutputStreamWriter out = null;
				InputStream response = null;

				url += "?" + String.format("param1=%s&param2=%s", URLEncoder.encode("a", charset), URLEncoder.encode("b", charset));
				httpConnection = (HttpURLConnection) new URL(url).openConnection();
				httpConnection.setRequestProperty("Accept-Charset", charset);
				response = httpConnection.getInputStream();
				
				int status = httpConnection.getResponseCode();
				System.out.println("Response Code: " + status);

				//HTTP response headers:
				System.out.println("Response Headers:");
				for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
					System.out.println(header.getKey() + "=" + header.getValue());
				}
				
				try (Scanner scanner = new Scanner(response)) {
				    String responseBody = scanner.useDelimiter("\\A").next();
				    System.out.println("Response Body:\n" + responseBody);
				}
				System.out.println();
			}
			catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}

	private static void testGetFile()
	{
		PeerInRange peer = peersInRange.entrySet().iterator().next().getValue();

		String url = "http://" + peer.getIp().getHostAddress() + ":" + peer.getPort() + "/file";
		String charset = java.nio.charset.StandardCharsets.UTF_8.name();		
		try {
			String testFile = "test.pdf";
			String msgId = IdGenerator.nextId();
			url += "?" + String.format("id=%s&msg=%s&peer=%s", URLEncoder.encode(testFile, charset), URLEncoder.encode(msgId, charset), URLEncoder.encode(id, charset));
			
			HttpURLConnection httpConnection = null;
			InputStream response = null;

			httpConnection = (HttpURLConnection) new URL(url).openConnection();
			int status = httpConnection.getResponseCode();
			System.out.println("Response Code: " + status);

			//HTTP response headers:
			System.out.println("Response Headers:");
			for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
				System.out.println(header.getKey() + "=" + header.getValue());
			}
			
			response = httpConnection.getInputStream();			
			FileOutputStream stream = null;
			try {		
				stream = new FileOutputStream(new File(testFile));
				byte[] bytes = new byte[1000];
				
				int n;
				while((n = response.read(bytes)) != -1){
					stream.write(bytes, 0, n);
				}
			}
			catch(Exception e)
			{
				System.out.println(e);
				return;
			}
			finally{
				try { stream.close(); } catch (IOException e) { }
			}
			System.out.println("Received file");
		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
}
