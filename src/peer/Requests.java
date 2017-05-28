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
import java.util.Scanner;
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

	protected static void forward(String messageID, String fileID, File tempFile) throws MalformedURLException, IOException {
		/* New message */
		Peer.addMessageReceived(messageID);
		
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
			httpConnection.setRequestProperty("File_ID", fileID);
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
		int saved_msgs = Peer.getSavedMessagesCounter(messageID);
		int pir = Peer.getPeersInRange().size();
		int cts = 0;
		if (pir < 2)
			cts = (pir - saved_msgs + 1) / (pir + 1) * 100;
		else
			cts = (pir - saved_msgs) / (pir) * 1000;

		int randomInt = rng.nextInt(1001);
		if (randomInt <= cts) {
			/* Save File */

			File dbDir = new File("database");
			
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
