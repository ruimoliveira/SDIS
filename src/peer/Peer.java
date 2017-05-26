package peer;
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
		id = new SessionIdentifierGenerator().nextSessionId();
		int httpServerPort = Integer.parseInt(args[0]);
		
		//Initiate http server
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(httpServerPort), 0);
			server.createContext("/files", new FilesHandler());
			server.createContext("/test", new TestHandler());
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
        
        while(true){
        	//listPeersInRange();
        	testServer();
        	try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
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
}
