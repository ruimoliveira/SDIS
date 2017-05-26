package peer;
import java.net.InetAddress;

public class PeerInRange
{
	private String id;
	private InetAddress ip;
	private int port;
	private long lastAdvertiseTime;
	
	public PeerInRange(String id, InetAddress ip, int port){
		this.id = id;
		this.ip = ip;
		this.port = port;
		this.lastAdvertiseTime = System.currentTimeMillis();
	}
	
	public String getId() {
		return id;
	}
	
	public InetAddress getIp() {
		return ip;
	}
	
	public void setIp(InetAddress ip) {
		this.ip = ip;
	}
	
	public int getPort() {
		return port;
	}
	
	public void setPort(int port) {
		this.port = port;
	}
	
	public long getLastAdvertiseTime() {
		return lastAdvertiseTime;
	}
	
	public void setLastAdvertiseTime() {
		this.lastAdvertiseTime = System.currentTimeMillis();
	}
}
