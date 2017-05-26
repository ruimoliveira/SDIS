package client;

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

public class Client {

	public static void main(String[] args)
	{
        if (args.length < 5 || args.length > 6) {
            System.out.println("Usage: java Client <srv_addr> <srv_port> <oper> <opnd>*");
            return;
       }
		
		String url = "http://" + args[0] + ":" + args[1] + "/test";
		String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()

		try {
	
			HttpURLConnection httpConnection = null;
			OutputStreamWriter out = null;
			InputStream response = null;
			
			switch(args[2]){
				case "GET":
					if(args.length != 5){
			            System.out.println("GET must have 5 params");
			            return;
					}						
					url += "?" + String.format("plate=%s&type=%s", URLEncoder.encode(args[3], charset), URLEncoder.encode(args[4], charset));
					httpConnection = (HttpURLConnection) new URL(url).openConnection();
					
					response = httpConnection.getInputStream();
					
					break;
				case "PUT":
					if(args.length != 6){
			            System.out.println("PUT must have 5 or 6 params");
			            return;
					}
					url += "?" + String.format("type=%s", URLEncoder.encode(args[5], charset));
					httpConnection = (HttpURLConnection) new URL(url).openConnection();
					httpConnection.setDoOutput(true);
					httpConnection.setRequestMethod("PUT");
					
					out = new OutputStreamWriter(httpConnection.getOutputStream());
					if(args[5].equalsIgnoreCase("json"))
						out.write("json");
					else
						out.write("xml");
					out.close();
					
					response = httpConnection.getInputStream();
					
					break;
				case "POST":
					if(args.length != 6){
			            System.out.println("POST must have 6 params");
			            return;
					}
					url += "?" + String.format("type=%s", URLEncoder.encode(args[5], charset));				
					httpConnection = (HttpURLConnection) new URL(url).openConnection();
					httpConnection.setDoOutput(true);
					httpConnection.setRequestMethod("POST");
					
					out = new OutputStreamWriter(httpConnection.getOutputStream());
					if(args[5].equalsIgnoreCase("json"))
						out.write("json");
					else
						out.write("xml");
					out.close();
					
					response = httpConnection.getInputStream();
					
					break;
				case "DELETE":
					if(args.length != 5){
			            System.out.println("DELETE must have 5 params");
			            return;
					}
					url += "?" + String.format("plate=%s&type=%s", URLEncoder.encode(args[3], charset), URLEncoder.encode(args[4], charset));
					httpConnection = (HttpURLConnection) new URL(url).openConnection();
					httpConnection.setRequestMethod("DELETE");
					response = httpConnection.getInputStream();
					
					break;
					default:
						System.out.println(args[2] + " is not a valid method");
						break;
			}			
	
			
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
			
		} catch (UnsupportedEncodingException e) {
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
