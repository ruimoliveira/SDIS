package client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Scanner;


public class Operation {
	public static long oneGB = (long)(1 * Math.pow(10, 9));
	
	private String server_address, server_port, operation, operative;
	
	Operation (String server_address, String server_port, String operation, String operative) {
		this.server_address = server_address;
		this.server_port = server_port;
		this.operation = operation;
		this.operative = operative;
		
		handleOperation();
	}

	private void handleOperation() {
		String url = null;
		//String charset = "UTF-8"; // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()

		try {

			HttpURLConnection httpConnection = null;
			InputStream response = null;

			switch (operation) {
			case "UPLOAD":
				upload(url, httpConnection, response);
				break;
			case "DOWNLOAD":
				download(url, httpConnection, response);
				break;
			case "DELETE":
				/* TODO: dar ordem de apagar o ficheiro */
				/*
				url = "http://" + server_address + ":" + server_port + "/client/" + operative;
				httpConnection = (HttpURLConnection) new URL(url).openConnection();
				httpConnection.setRequestMethod("DELETE");
				response = httpConnection.getInputStream();
*/
				break;
			case "LIST":
				/* TODO: pedir listagem de ficheiros */
				/*
				url = "http://" + server_address + ":" + server_port + "/client";
						URLEncoder.encode(args[4], charset));
				httpConnection = (HttpURLConnection) new URL(url).openConnection();

				response = httpConnection.getInputStream();
*/
				break;
			default:
				System.out.println("ERROR: Impossible state reached.");
				return;
			}


		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	private void download(String url, HttpURLConnection httpConnection, InputStream response) throws MalformedURLException, IOException {
		/* TODO: receber ficheiro do server */
		url = "http://" + server_address + ":" + server_port + "/client/file";
		httpConnection = (HttpURLConnection) new URL(url).openConnection();

		/* Create header */
		httpConnection.setRequestMethod("GET");
		httpConnection.setRequestProperty("File_ID", operative);

		/* Get Response */
		int responseCode = httpConnection.getResponseCode();
		
		if(responseCode == 400){
			System.out.println("Server Response: " + responseCode + " Bad Request - no FileID received");
		} else if(responseCode == 204){
			System.out.println("Server Response: " + responseCode + " File does not exist withing the network.");
		} else if(responseCode == 200){
			
			response = httpConnection.getInputStream();			
			FileOutputStream stream = null;
			try {		
				stream = new FileOutputStream(new File(operative));
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
			System.out.println("File received!");
			
		} else {
			System.out.println("Unexpected Server Response:");
			for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
				System.out.println(header.getKey() + "=" + header.getValue());
			}
		}
		
	}

	private void upload(String url, HttpURLConnection httpConnection, InputStream response) throws IOException {
		/* Load file */
		File file = new File(operative);
		if (!file.exists() || file.isDirectory()) {
			System.out.println("File does not exist. Exiting...");
			return;
		}
		
		if (file.length() > oneGB) {
			System.out.println("File exceeds size limit of 1 GB. Exiting...");
		}
		
		FileInputStream fis = null;
		fis = new FileInputStream(file);
		
		/* Create connection */
		url = "http://" + server_address + ":" + server_port + "/client";
		httpConnection = (HttpURLConnection) new URL(url).openConnection();
		
		httpConnection.setDoOutput(true);
		
		/* Create header */
		httpConnection.setRequestMethod("PUT");
		httpConnection.setRequestProperty("File_Name", file.getName());
		httpConnection.setRequestProperty("File_Length", new Long(file.length()).toString());

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
		} else if(responseCode == 200){
			String responseBody;
			try (Scanner scanner = new Scanner(response)) {
				responseBody = scanner.useDelimiter("\\A").next();
			}
			System.out.println("File Uploaded! Your file's shareID is: " + responseBody);
		} else {
			System.out.println("Unexpected Server Response:");
			for (java.util.Map.Entry<String, List<String>> header : httpConnection.getHeaderFields().entrySet()) {
				System.out.println(header.getKey() + "=" + header.getValue());
			}
		}
		
	}
}
