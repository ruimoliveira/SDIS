package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;

public class Action
{
	private static long oneGB = (long)(1 * Math.pow(10, 9));	
	private String server_address, server_port, operation, operative;
	private static String charset = java.nio.charset.StandardCharsets.UTF_8.name();
	
	Action (String server_address, String server_port, String operation, String operative) {
		this.server_address = server_address;
		this.server_port = server_port;
		this.operation = operation;
		this.operative = operative;
	}

	public void run()
	{
		try {
			switch (operation) {
			case "UPLOAD":
				upload();
				break;
			case "DOWNLOAD":
				download();
				break;
			case "DELETE":
				delete();
				break;
			case "LIST":
				list();
				break;
			default:
				System.out.println("Not a valid operation.");
				return;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}

	private void download() throws MalformedURLException, IOException
	{
		String url = "http://" + server_address + ":" + server_port + "/client";
		url += "?" + String.format("fileid=%s", URLEncoder.encode(operative, charset));
		HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();

		/* Create header */
		//httpConnection.setRequestMethod("GET");

		/* Get Response */
		int responseCode = httpConnection.getResponseCode();
		if(responseCode == 400){
			System.out.println("Server Response: " + responseCode + " Bad Request - no FileId received.");
		}
		else if(responseCode == 204){
			System.out.println("Server Response: " + responseCode + " File does not exist within the network.");
		}
		else if(responseCode == 200){
			
			InputStream is = httpConnection.getInputStream();			
			FileOutputStream stream = null;
			try {		
				stream = new FileOutputStream(new File(operative));
				byte[] bytes = new byte[1000];				
				int n;
				while((n = is.read(bytes)) != -1){
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
			System.out.println("File received!");
			
		} else {
			System.out.println("Unexpected Server Response:");
			for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
				System.out.println(header.getKey() + "=" + header.getValue());
			}
		}		
	}

	private void upload() throws IOException
	{
		/* Load file */
		File file = new File(operative);
		if (!file.exists() || file.isDirectory()) {
			System.out.println("File does not exist. Exiting...");
			return;
		}
		
		if (file.length() > oneGB) {
			System.out.println("File exceeds size limit of 1 GB. Exiting...");
			return;
		}
		
		FileInputStream fis = null;
		fis = new FileInputStream(file);
		
		/* Create connection */
		String url = "http://" + server_address + ":" + server_port + "/client";
		url += "?" + String.format("fileid=%s", URLEncoder.encode(operative, charset));
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
			System.out.println("Unexpected Server Response:");
			for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
				System.out.println(header.getKey() + "=" + header.getValue());
			}
		}
	}
	
	private void delete() throws MalformedURLException, IOException
	{
		/* Create connection */
		String url = "http://" + server_address + ":" + server_port + "/client";
		url += "?" + String.format("fileid=%s", URLEncoder.encode(operative, charset));
		HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();

		/* Create header */
		httpConnection.setRequestMethod("DELETE");

		/* Get Response */
		int responseCode = httpConnection.getResponseCode();
		if(responseCode == 400){
			System.out.println("Server Response: " + responseCode + " Bad Request - no FileID received.");
		} else if(responseCode == 200){
			System.out.println("Server Response: " + responseCode + " Delete order has been issued.");
		} else {
			System.out.println("Unexpected Server Response:");
			for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
				System.out.println(header.getKey() + "=" + header.getValue());
			}
		}
	}
	
	private void list() throws IOException
	{		
		/* Create connection */
		String url = "http://" + server_address + ":" + server_port + "/client";
		HttpURLConnection httpConnection = (HttpURLConnection) new URL(url).openConnection();
		
		/* Response */		
		int responseCode = httpConnection.getResponseCode();

		if(responseCode == 500){
			System.out.println("Server Response: " + responseCode + " Internal Server Error.");
		} else if(responseCode == 400){
			System.out.println("Server Response: " + responseCode + " Bad Request.");
		} else if(responseCode == 200){
			InputStream response = httpConnection.getInputStream();
			System.out.println("Files found:");
			String responseBody;
			String[] filenames = null;
			try (Scanner scanner = new Scanner(response, charset)) {
				scanner.useDelimiter("\\A");
				responseBody = scanner.hasNext() ? scanner.next() : "";
				filenames = responseBody.split("\\n");
			}
			for(String filename : filenames){
				if (!filename.trim().equals(""))
					System.out.println(filename);
			}
			
		} else {
			System.out.println("Unexpected Server Response:");
			for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
				System.out.println(header.getKey() + "=" + header.getValue());
			}
		}		
	}
}
