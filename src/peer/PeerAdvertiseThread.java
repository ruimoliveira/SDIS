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
    private int broadcastPort;
    private int servicePort;
    private boolean running = true;

    public PeerAdvertiseThread(String name, int broadcastPort, int servicePort) throws IOException
    {
        super(name);
        advertiseSocket = new DatagramSocket(null);
        advertiseSocket.setReuseAddress(true);
        advertiseSocket.setBroadcast(true);
        advertiseSocket.bind(new InetSocketAddress(broadcastPort));        
        this.broadcastPort = broadcastPort;
        this.servicePort = servicePort;
        
        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface i : Collections.list(interfaces))
        {
        	/*System.out.println("Name: " + i.getName());
        	System.out.println("DisplayName: " + i.getDisplayName());
        	System.out.println("Virtual: " + i.isVirtual());
        	System.out.println("Loopback: " + i.isLoopback());
        	System.out.println("Up: " + i.isUp());*/
            
            if(!i.isLoopback() && !i.isVirtual() && i.isUp())
            {
            	Enumeration<InetAddress> addresses = i.getInetAddresses();
            	//System.out.println("Addresses:");
                for (InetAddress a : Collections.list(addresses))
                {
                	if (a instanceof Inet4Address){                		
                		//System.out.println(a.getHostAddress());
                	}
                }
                
            	List<InterfaceAddress> iAddresses = i.getInterfaceAddresses();
            	for (InterfaceAddress ia : iAddresses)
            	{
            		/*if (ia.getBroadcast() != null){
            			broadcastAddresses.add(ia.getBroadcast());
            			System.out.println("Broadcast Address: " + ia.getBroadcast().getHostAddress());
            		}*/

            		InetAddress host = ia.getAddress();
            		if (host instanceof Inet6Address)
            			continue;
            		int prefixLength = ia.getNetworkPrefixLength();
            		InetAddress broadcast = getBroadcast(host, prefixLength);
            		broadcastAddresses.add(broadcast);
            		//System.out.println("Host Address: " + ia.getAddress().getHostAddress());
            		//System.out.println("Network Prefix Length: " + ia.getNetworkPrefixLength());
            		//System.out.println("Broadcast Address calculated: " + broadcast.getHostAddress());
            	}
            }
        }        
    }
    
    private InetAddress getBroadcast(InetAddress host, int prefixLength)
    {
    	if (host instanceof Inet6Address)
    		return null;
    	
    	int shft = 0xffffffff>>>(prefixLength);
    	byte[] address = host.getAddress();
    	
    	address[0] = (byte) (((byte) ((shft&0xff000000)>>24)) | address[0]);
    	address[1] = (byte) (((byte) ((shft&0x00ff0000)>>16)) | address[1]);
    	address[2] = (byte) (((byte) ((shft&0x0000ff00)>>8)) | address[2]);
    	address[3] = (byte) (((byte) (shft&0x000000ff)) | address[3]);
    	
    	try {
			return InetAddress.getByAddress(address);
		} catch (UnknownHostException e) {
			return null;
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