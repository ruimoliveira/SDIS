package peer;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class PeerAdvertiseThread extends Thread
{
    private DatagramSocket advertiseSocket = null;
    ArrayList<InetAddress> broadcastAddresses = new ArrayList<InetAddress>();
    private int broadcastPort = 7000;
    private int servicePort = 8000;
    private boolean running = true;

    public PeerAdvertiseThread(String name, int broadcastPort, int servicePort) throws IOException
    {
        super(name);
        
        // create an unbound socket
        advertiseSocket = new DatagramSocket(null);

        // make it possible to bind several sockets to the same port
        advertiseSocket.setReuseAddress(true);

        // might not be necessary, but for clarity
        advertiseSocket.setBroadcast(true);

        advertiseSocket.bind(new InetSocketAddress(broadcastPort));
        
        this.broadcastPort = broadcastPort;
        //this.serviceAddr = InetAddress.getLocalHost();
        this.servicePort = servicePort;
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface i : Collections.list(interfaces))
        {
        	/*System.out.println("Name: " + i.getName());
        	System.out.println("DisplayName: " + i.getDisplayName());
        	System.out.println("Virtual: " + i.isVirtual());
        	System.out.println("Loopback: " + i.isLoopback());
        	
        	Enumeration<InetAddress> addresses = i.getInetAddresses();
        	System.out.println("Addresses:");
            for (InetAddress a : Collections.list(addresses))
            {
            	System.out.println("  " + a.getHostAddress());
            }*/
            
            if(!i.isLoopback() && !i.isVirtual())
            {
            	List<InterfaceAddress> iAddresses = i.getInterfaceAddresses();
            	//System.out.println("Broadcast Addresses:");
            	for (InterfaceAddress ia : iAddresses)
            	{
            		if (ia.getBroadcast() != null){
            			broadcastAddresses.add(ia.getBroadcast());
            			//System.out.println("  " + ia.getBroadcast().getHostAddress());
            		}
            	}
            }
        }        
    }

    public void run()
    {
        while (running) {
            try {                
            	String msg = Peer.getId() + ":" + servicePort;
            	for(InetAddress broadcastAddress : broadcastAddresses)
            	{
            		DatagramPacket msgPacket = new DatagramPacket(msg.getBytes(), msg.getBytes().length, broadcastAddress, broadcastPort);
            		advertiseSocket.send(msgPacket);
            		//System.out.println("broadcast to " + broadcastAddress.getHostAddress() + ":" + broadcastPort + "\nbroadcast message " + msg);
            	}
                Thread.sleep(10000);
            }
            catch (Exception e) {
                e.printStackTrace();
                running = false;
            }            
        }
        advertiseSocket.close();
    }
}