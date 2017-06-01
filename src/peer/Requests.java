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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Requests
{
	private static long oneGB = (long)(1 * Math.pow(10, 9));
	private static String charset = java.nio.charset.StandardCharsets.UTF_8.name();
	
	protected static int getFile(String peer, int port, String fileId, String msgId, InputStreamWrapper isw)
	{
		String url = "http://" + peer + ":" + port + "/file";				
		try
		{
			url += "?" + String.format("id=%s&msg=%s&peer=%s", URLEncoder.encode(fileId, charset), URLEncoder.encode(msgId, charset), URLEncoder.encode(Peer.getId(), charset));			
			
			HttpURLConnection httpConnection = null;
			httpConnection = (HttpURLConnection) new URL(url).openConnection();
			
			int status = httpConnection.getResponseCode();
			if (status == 200)
				isw.inputStream = httpConnection.getInputStream();
			
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
		try
		{
			url += "?" + String.format("id=%s&msg=%s&peer=%s", URLEncoder.encode(fileId, charset), URLEncoder.encode(msgId, charset), URLEncoder.encode(Peer.getId(), charset));
			
			HttpURLConnection httpConnection = null;
			httpConnection = (HttpURLConnection) new URL(url).openConnection();			
			httpConnection.setRequestMethod("DELETE");
			
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

	protected static int putFile(String peer, int port, String fileId, String msgId)
	{
		/* Load file */
		File file = new File("Temp", fileId);
		if (!file.exists() || file.isDirectory()) {
			System.out.println("File does not exist. Exiting...");
			return 400;
		}

		if (file.length() > oneGB) {
			System.out.println("File exceeds size limit of 1 GB. Exiting...");
			return 400;
		}

		try
		{
			FileInputStream fis = null;
			fis = new FileInputStream(file);

			/* Create connection */
			String url = "http://" + peer + ":" + port + "/file";
			url += "?" + String.format("id=%s&msg=%s&peer=%s", URLEncoder.encode(fileId, charset), URLEncoder.encode(msgId, charset), URLEncoder.encode(Peer.getId(), charset));
			HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
			httpConnection.setDoOutput(true);
			httpConnection.setChunkedStreamingMode(1024);

			/* Create header */
			httpConnection.setRequestMethod("PUT");

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
			} else if(responseCode == 200){
				System.out.println("File Uploaded!");
			} else {
				System.out.println("Unexpected Server Response (" + responseCode + "):");
				for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
					System.out.println(header.getKey() + "=" + header.getValue());
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return 400;
	}

	protected static void stored(String peer, int port, String fileId, String msgId, String putMsgId)
	{
		/* Sending save request */
		HttpURLConnection httpConnection = null;
		String url = "http://" + peer + ":" + port + "/stored";		

		try {
			url += "?" + String.format("fileid=%s&msg=%s&putmsg=%s&peer=%s", URLEncoder.encode(fileId, charset), URLEncoder.encode(msgId, charset), URLEncoder.encode(putMsgId, charset), URLEncoder.encode(Peer.getId(), charset));
			httpConnection = (HttpURLConnection) new URL(url).openConnection();

			/* Create header */
			httpConnection.setRequestMethod("PUT");

			/* Get Response */
			int responseCode = httpConnection.getResponseCode();
			if(responseCode == 205){
				System.out.println("Server Response: " + responseCode + " Message " + msgId + " ignored.");
			} else if(responseCode == 200){
				System.out.println("Server Response: " + responseCode + " File save acknowledged ");
			} else {
				System.out.println("Unexpected Server Response (" + responseCode + "):");
				for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
					System.out.println(header.getKey() + "=" + header.getValue());
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	protected static ArrayList<String> getFilesList(String peer, int port, String msgId)
	{
		String url = "http://" + peer + ":" + port + "/files";
		ArrayList<String> filesList = new ArrayList<String>();
		try
		{
			url += "?" + String.format("msg=%s&peer=%s", URLEncoder.encode(msgId, charset), URLEncoder.encode(Peer.getId(), charset));			
			
			HttpURLConnection httpConnection = null;
			httpConnection = (HttpURLConnection) new URL(url).openConnection();
			
			/* Response */
			int responseCode = httpConnection.getResponseCode();
			if (responseCode == 200){
				InputStream response = httpConnection.getInputStream();
				String[] filenames = null;
				try (Scanner scanner = new Scanner(response, charset)) {
					scanner.useDelimiter("\\A");
					String responseBody = scanner.hasNext() ? scanner.next() : "";
					filenames = responseBody.split("\\n");
				}
				for(String filename : filenames){
					filesList.add(filename);
				}
			}
			else if(responseCode == 500){
				System.out.println("Server Response: " + responseCode + " Internal Server Error.");
			}
			else if(responseCode == 400){
				System.out.println("Server Response: " + responseCode + " Bad Request.");
			}
			else {
				System.out.println("Unexpected Server Response (" + responseCode + "):");
				for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
					System.out.println(header.getKey() + "=" + header.getValue());
				}
			}
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return filesList;
	}
}
