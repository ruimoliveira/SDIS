package peer;
import java.io.*;
import java.net.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerListenerThread extends Thread
{
    private DatagramSocket advertiseSocket = null;
    private boolean running = true;

    public PeerListenerThread(String name, int listeningPort) throws IOException
    {
        super(name);
        
        // create an unbound socket
        advertiseSocket = new DatagramSocket(null);

        // make it possible to bind several sockets to the same port
        advertiseSocket.setReuseAddress(true);

        // might not be necessary, but for clarity
        advertiseSocket.setBroadcast(true);

        advertiseSocket.bind(new InetSocketAddress(listeningPort));        
    }

    public void run()
    {
        while (running)
        {
            try
            {
            	byte[] buf = new byte[128];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                advertiseSocket.receive(packet);
                String request = new String(buf);
                
                Thread t = new Thread()
                {
					public void run()
					{
						final String[] split = request.trim().split(":");
		                if(!split[0].equals(Peer.getId()))
		                {
		                	updatePeersInRange(split[0], packet.getAddress(), Integer.parseInt(split[1]));
		                }
					}  
				};
				t.start();                
            }
            catch (Exception e) {
                e.printStackTrace();
                running = false;
            }            
        }
        advertiseSocket.close();
    }
    
	protected static void updatePeersInRange(String id, InetAddress ip, int port)
	{
		ConcurrentHashMap<String, PeerInRange> peersInRange = Peer.getPeersInRange();
		if(peersInRange.containsKey(id))
		{
			PeerInRange peer = peersInRange.get(id);
			if(!peer.getIp().equals(ip))
			{
				peer.setIp(ip);
			}
			if(peer.getPort() != port)
			{
				peer.setPort(port);
			}
			peer.setLastAdvertiseTime();
			
		}
		else{
			peersInRange.put(id, new PeerInRange(id, ip, port));
		}
	}
}