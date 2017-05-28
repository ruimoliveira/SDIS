package peer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;
import java.util.Map.Entry;

public class Tests
{
	private static void listPeersInRange()
	{
		System.out.println("Peers in range:");
		for(Entry<String, PeerInRange> entry : Peer.getPeersInRange().entrySet())
		{
			PeerInRange peer = entry.getValue();
			System.out.println("Id: " + peer.getId() + ", Ip: " + peer.getIp().getHostAddress() + ", Port: " + peer.getPort());
		}
		System.out.println();
	}
	
	private static void testServer()
	{
		String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()

		for(Entry<String, PeerInRange> entry : Peer.getPeersInRange().entrySet())
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
		PeerInRange peer = Peer.getPeersInRange().entrySet().iterator().next().getValue();

		String url = "http://" + peer.getIp().getHostAddress() + ":" + peer.getPort() + "/file";
		String charset = java.nio.charset.StandardCharsets.UTF_8.name();		
		try {
			String testFile = "test.pdf";
			String msgId = IdGenerator.nextId();
			url += "?" + String.format("id=%s&msg=%s&peer=%s", URLEncoder.encode(testFile, charset), URLEncoder.encode(msgId, charset), URLEncoder.encode(Peer.getId(), charset));
			
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
