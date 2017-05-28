package peer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.Random;
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
	private static ConcurrentHashMap<String, Long> messagesReceived = new ConcurrentHashMap<String, Long>();
			
	public static void main(String[] args) throws IOException
	{
		id = IdGenerator.nextId();
		int httpServerPort = Integer.parseInt(args[0]);
		
		//Initiate http server
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(httpServerPort), 0);
			server.createContext("/file", new FileHandler());
			server.createContext("/files", new FilesHandler());
			server.createContext("/fileStored", new StoredHandler());
			server.createContext("/test", new TestHandler());
			server.createContext("/client", new ClientHandler());
			server.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));
			server.start();
			System.out.println("HttpServer started");
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//Initiate server port advertise thread
        Thread advertiseThread = new PeerAdvertiseThread("advertiser", 7000, httpServerPort);
        advertiseThread.start();
        
        //Initiate peers in range listener thread
        Thread listenerThread = new PeerListenerThread("listener", 7000);
        listenerThread.start();
        
        //Initiate messages cleaner thread
    	Timer timer = new Timer();
    	timer.schedule(new MessagesCleanerTask(300000), 0, 30000);
            
    	//Tests
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
	
	protected static boolean isMessageReceived(String msgId)
	{
		return messagesReceived.containsKey(msgId);
	}
	
	protected static void addMessageReceived(String msgId)
	{
		messagesReceived.put(msgId, System.currentTimeMillis());
	}
	
    static class MessagesCleanerTask extends TimerTask
    {
    	long milisecs;
    	
    	public MessagesCleanerTask(long milisecs){
    		this.milisecs = milisecs;
    	}
    	
        public void run()
        {        	
    		for(Entry<String, Long> entry : messagesReceived.entrySet())
    		{
    			if (entry.getValue() + milisecs < System.currentTimeMillis())
    				messagesReceived.remove(entry.getKey());
    		}
        }
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

	protected static void share(String messageID, String fileID, File tempFile) throws MalformedURLException, IOException {
		/* TODO: Start Upload protocol/thread
		 * 2. Waits a random amount of time between 0 and 400 ms.
    			While counting the number of saved_file responses with same requestID (n_saved)
		 * 3. Decides on weather or not to save file based on a chance.
    			It has a cts% of saving the file, where cts = (n_pir-n_saved)/n_pir
    			if (n_pir == 2) {
					cts = (n_pir-n_saved+1)/(n_pir+1)
				}
		 * 4. Sends a saved_file to all peers in range
		 * 
		 * For now, it saves the file in /database/ folder
		 * 
		 * */
		
		/* New message */
		addMessageReceived(messageID);
		
		/* Sending save request */
		HttpURLConnection httpConnection = null;
		InputStream response = null;
		FileInputStream fis = null;
		fis = new FileInputStream(tempFile);
		String url;

		for (Entry<String, PeerInRange> entry : Peer.getPeersInRange().entrySet()) {
			PeerInRange peer = entry.getValue();
			url = "http://" + peer.getIp().toString() + ":" + peer.getPort() + "/file";
			httpConnection = (HttpURLConnection) new URL(url).openConnection();
			httpConnection.setChunkedStreamingMode(1024);

			httpConnection.setDoOutput(true);

			/* Create header */
			httpConnection.setRequestMethod("PUT");
			httpConnection.setRequestProperty("Message_ID", messageID);
			httpConnection.setRequestProperty("File_ID", tempFile.getName());
			httpConnection.setRequestProperty("File_Length", new Long(tempFile.length()).toString());

			OutputStream os = httpConnection.getOutputStream();
			
			byte[] bytes = new byte[1000];
	
			int n;
			while((n = fis.read(bytes)) != -1){
				os.write(bytes, 0, n);
			}
			
			try { fis.close(); } catch (IOException e) { }
			try { os.close(); } catch (IOException e) { }
	
			/* Response */
			response = httpConnection.getInputStream();
			int responseCode = httpConnection.getResponseCode();
	
			if(responseCode == 500){
				System.out.println("Server Response: " + responseCode + " Internal Server Error.");
			} else if(responseCode == 400){
				System.out.println("Server Response: " + responseCode + " Bad Request.");
			} else if(responseCode == 205){
				System.out.println("Server Response: " + responseCode + " Message " + messageID + " already received.");
			} else if(responseCode == 200){
				String responseBody;
				try (Scanner scanner = new Scanner(response)) {
					responseBody = scanner.useDelimiter("\\A").next();
				}
				System.out.println("Server Response: " + responseCode + " File for retransmition received.");
			} else {
				System.out.println("Unexpected Server Response:");
				for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
					System.out.println(header.getKey() + "=" + header.getValue());
				}
			}
		}

		/* Give a chance for other peers to save before deciding to save */
	    Random rng = new Random();
		int time = rng.nextInt(401);
		try { TimeUnit.MILLISECONDS.sleep(time); } catch (InterruptedException e) {}
		
		/* Roll dice to save or not */
		int saved_msgs = getSavedMessagesCounter();
		int pir = Peer.getPeersInRange().size();
		int cts = 0;
		if (pir < 2)
			cts = (pir - saved_msgs + 1) / (pir + 1) * 100;
		else
			cts = (pir - saved_msgs) / (pir) * 1000;

		int randomInt = rng.nextInt(1001);
		if (randomInt <= cts) {
			/* Save File */
			
			/* Send Save Message */
		}
		
		/* Delete temporary file */
		tempFile.delete();
	}
}
