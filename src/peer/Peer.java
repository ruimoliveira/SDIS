package peer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;
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
	static ConcurrentHashMap<String, Long> messagesReceived = new ConcurrentHashMap<String, Long>();
	static ConcurrentHashMap<String, Integer> storedReceived = new ConcurrentHashMap<String, Integer>();
			
	public static void main(String[] args) throws IOException
	{
		id = IdGenerator.nextId();
		int httpServerPort = Integer.parseInt(args[0]);
		
		/*Initiate http server*/
		try {
			HttpServer server = HttpServer.create(new InetSocketAddress(httpServerPort), 0);
			server.createContext("/file", new FileHandler());
			server.createContext("/files", new FilesHandler());
			server.createContext("/stored", new StoredHandler());
			server.createContext("/client", new ClientHandler());
			server.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(100)));
			server.start();
			System.out.println("HttpServer started");
			System.out.println();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		/*Initiate server port advertise thread*/
        Thread advertiseThread = new PeerAdvertiseThread("advertiser", 7000, httpServerPort);
        advertiseThread.start();
        
        /*Initiate peers in range listener thread*/
        Thread listenerThread = new PeerListenerThread("listener", 7000);
        listenerThread.start();
        
        /*Initiate messages cleaner thread*/
    	Timer timer = new Timer();
    	timer.schedule(new MessagesCleanerTask(300000), 0, 30000);
    	
    	//Timer testTimer = new Timer();
    	//testTimer.schedule(new Tests.TestTask(), 0, 5000);
    }
	
	protected static String getId(){
		return id;
	}
	
	protected static ConcurrentHashMap<String, PeerInRange> getPeersInRange()
	{
		return peersInRange;
	}
	
	protected static boolean isMessageReceived(String msgId)
	{
		return messagesReceived.containsKey(msgId);
	}
	
	protected static void addMessageReceived(String msgId)
	{
		messagesReceived.put(msgId, System.currentTimeMillis());
	}
	
	protected static void addStoredReceived(String msgId)
	{
		if (storedReceived.containsKey(msgId))
			storedReceived.put(msgId, storedReceived.get(msgId));
		else storedReceived.put(msgId, 1);
	}

	public static int getStoredCounter(String msgId)
	{
		if (storedReceived.containsKey(msgId))
			return storedReceived.get(msgId);
		return 0;
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
    			{
    				String key = entry.getKey();
    				messagesReceived.remove(key);
    				if (storedReceived.containsKey(key))
    					messagesReceived.remove(key);
    			}
    		}
        }
    }
}
