package peer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class FileHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange t)
	{
		String method = t.getRequestMethod();
		System.out.println("Request Method: " + method);

		//Request Headers
		Headers headers = t.getRequestHeaders();
		System.out.println("Request Headers:");
		for (Entry<String, List<String>> header : headers.entrySet()) {
			System.out.print("  " + header.getKey() + ": ");
			for (String str : header.getValue()) {
				System.out.print(str + " ");
			}
			System.out.println();
		}

		//Request query
		String query = t.getRequestURI().getQuery();
		Map<String, String> params = null;
		if (query != null){
			params = HttpHandlerUtil.queryToMap(query);
			System.out.println("Request Query:");
			for (Entry<String, String> param : params.entrySet()){
				System.out.println("  " + param.getKey() + "=" + param.getValue());
			}
		}

		//Request Body
		InputStream is = t.getRequestBody();        
		InputStreamReader isr = null;
		BufferedReader br = null;
		try {
			isr = new InputStreamReader(is,"utf-8");
			br = new BufferedReader(isr);

			int b;
			StringBuilder buf = new StringBuilder(512);
			while ((b = br.read()) != -1) {
				buf.append((char) b);
			}
			
			br.close();
			isr.close();
			
			System.out.println("Request Body:\n" + buf.toString());
			System.out.println();		
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		switch(method)
		{
		case "GET":					
			get(t);
			break;
		case "PUT":
			put(t);
			break;
		case "POST":
			post(t);
			break;
		case "DELETE":
			delete(t);
			break;
		}
	}
	
	private void get(HttpExchange t)
	{
		FileInputStream fs = null;
		OutputStream os = null;
		try {
			t.sendResponseHeaders(200, 0);
			os = t.getResponseBody();
			
			File dir = new File("Files");			
			fs = new FileInputStream(new File(dir, "test.pdf"));
			byte[] bytes = new byte[1000];

			int n;
			while((n = fs.read(bytes)) != -1){
				System.out.println(n);
				os.write(bytes, 0, n);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return;
		}
		finally{
			try { fs.close(); } catch (IOException e) { }
			try { os.close(); } catch (IOException e) { }
		}
	}
	
	private void put(HttpExchange t)
	{

	}
	
	private void post(HttpExchange t)
	{

	}
	
	private void delete(HttpExchange t)
	{

	}
}
