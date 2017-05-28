package peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

public class Requests {
	
	protected static int getFile(String peer, int port, String fileId, String msgId, InputStream inputStream)
	{
		String url = "http://" + peer + ":" + port + "/file";
		String charset = java.nio.charset.StandardCharsets.UTF_8.name();		
		try
		{
			url += "?" + String.format("id=%s&msg=%s&peer=%s", URLEncoder.encode(fileId, charset), URLEncoder.encode(msgId, charset), URLEncoder.encode(Peer.getId(), charset));			
			
			HttpURLConnection httpConnection = null;
			httpConnection = (HttpURLConnection) new URL(url).openConnection();
			
			int status = httpConnection.getResponseCode();
			if (status == 200)
				inputStream = httpConnection.getInputStream();
			
			return status;

		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 400;
	}
	
	protected static int deleteFile(String peer, int port, String fileId, String msgId)
	{
		String url = "http://" + peer + ":" + port + "/file";
		String charset = java.nio.charset.StandardCharsets.UTF_8.name();		
		try
		{
			url += "?" + String.format("id=%s&msg=%s&peer=%s", URLEncoder.encode(fileId, charset), URLEncoder.encode(msgId, charset), URLEncoder.encode(Peer.getId(), charset));
			
			HttpURLConnection httpConnection = null;
			httpConnection = (HttpURLConnection) new URL(url).openConnection();			
			httpConnection.setRequestMethod("PUT");
			
			int status = httpConnection.getResponseCode();
			return status;

		}
		catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return 400;
	}

	protected static void forward(String messageID, String fileID, String senderID, File tempFile) throws MalformedURLException, IOException {
		/* New message */
		Peer.addMessageReceived(messageID);
		String charset = java.nio.charset.StandardCharsets.UTF_8.name();	
		
		/* Sending save request */
		HttpURLConnection httpConnection = null;
		FileInputStream fis = null;
		String url;

		for (Entry<String, PeerInRange> entry : Peer.getPeersInRange().entrySet()) {
			fis = new FileInputStream(tempFile);
			
			if (entry.getKey().compareTo(senderID) == 0)
				continue;
			PeerInRange peer = entry.getValue();
			url = "http:/" + peer.getIp().toString() + ":" + peer.getPort() + "/file";
			url += "?" + String.format("id=%s&msg=%s&peer=%s", URLEncoder.encode(fileID, charset),
					URLEncoder.encode(messageID, charset), URLEncoder.encode(Peer.getId(), charset));
			System.out.println("URL: " + url);
			httpConnection = (HttpURLConnection) new URL(url).openConnection();
			httpConnection.setChunkedStreamingMode(1024);

			httpConnection.setDoOutput(true);

			/* Create header */
			httpConnection.setRequestMethod("PUT");
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
			int responseCode = httpConnection.getResponseCode();
			if(responseCode == 500){
				System.out.println("Server Response: " + responseCode + " Internal Server Error.");
			} else if(responseCode == 400){
				System.out.println("Server Response: " + responseCode + " Bad Request.");
			} else if(responseCode == 205){
				System.out.println("Server Response: " + responseCode + " Message " + messageID + " already received.");
			} if(responseCode == 200){
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
		int time = rng.nextInt(501);
		try { TimeUnit.MILLISECONDS.sleep(time); } catch (InterruptedException e) {}
		
		/* Roll dice to save or not */
		int saved_msgs = Peer.getSavedMessagesCounter(messageID);
		int pir = Peer.getPeersInRange().size();
		int cts;
		if (pir == 1) {
			cts = (pir - saved_msgs + 2) * 1000 / (pir + 2);
			System.out.println("cts = " + (pir - saved_msgs + 2) + "/" + (pir + 2));
		} else {
			cts = (pir - saved_msgs) * 1000 / (pir + 1);
			System.out.println("cts = " + (pir - saved_msgs + 1) + "/" + (pir + 1));
		}

		int randomInt = rng.nextInt(1001);
		System.out.println("pir: " + pir);
		System.out.println("saved_msgs: " + saved_msgs);
		System.out.println("cts: " + cts);
		System.out.println("randomInt: " + randomInt);
		
		if (randomInt <= cts) {
			/* Save File */
			File dbDir = new File("database" + Peer.getId());
			
			if (!dbDir.exists() || !dbDir.isDirectory())
				dbDir.mkdir();

			File savedFile = new File(dbDir.getName() + "//" + fileID);
			
		    FileInputStream is = null;
			FileOutputStream os = null;
			try {
		        is = new FileInputStream(tempFile);
		        os = new FileOutputStream(savedFile);
		        byte[] buffer = new byte[1024];
		        int length;
		        while ((length = is.read(buffer)) > 0) {
		            os.write(buffer, 0, length);
		        }
		    } finally {
		        is.close();
		        os.close();
		    }
			
			/* Send Save Message */
			sendSavedMessage(messageID, fileID);
		}
		
		/* Delete temporary file */
		tempFile.delete();
	}

	private static void sendSavedMessage(String messageID, String fileID) throws MalformedURLException, IOException {
		/* Sending save request */
		HttpURLConnection httpConnection = null;
		String url;

		for (Entry<String, PeerInRange> entry : Peer.getPeersInRange().entrySet()) {
			PeerInRange peer = entry.getValue();
			url = "http://" + peer.getIp().toString() + ":" + peer.getPort() + "/fileStored";
			httpConnection = (HttpURLConnection) new URL(url).openConnection();

			/* Create header */
			httpConnection.setRequestMethod("POST");
			httpConnection.setRequestProperty("Message_ID", messageID);
			httpConnection.setRequestProperty("File_ID", fileID);

			/* Get Response */
			int responseCode = httpConnection.getResponseCode();
			if(responseCode == 205){
				System.out.println("Server Response: " + responseCode + " Message " + messageID + " ignored.");
			} else if(responseCode == 200){
				System.out.println("Server Response: " + responseCode + " File save acknowledged ");
			} else {
				System.out.println("Unexpected Server Response:");
				for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
					System.out.println(header.getKey() + "=" + header.getValue());
				}
			}
		}
		
	}
}
