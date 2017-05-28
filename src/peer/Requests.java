package peer;

import java.io.File;
import java.io.FileInputStream;
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
