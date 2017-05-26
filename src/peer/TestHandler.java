package peer;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class TestHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange t) throws IOException {
		//Request Method
		String method = t.getRequestMethod();
		System.out.println("Request Method: " + method);
		System.out.println();

		//Request Headers
		Headers headers = t.getRequestHeaders();
		System.out.println("Request Headers:");
		for (Entry<String, List<String>> header : headers.entrySet()) {
			System.out.print(header.getKey() + ": ");
			for (String str : header.getValue()) {
				System.out.print(str + " ");
			}
			System.out.println();
		}
		System.out.println();

		//Request query
		String query = t.getRequestURI().getQuery();
		Map<String, String> params = null;
		if (query != null){
			params = queryToMap(query);
			System.out.println("Request Query:");
			for (Entry<String, String> param : params.entrySet()){
				System.out.println(param.getKey() + "=" + param.getValue());
			}
			System.out.println();
		}

		//Request Body
		InputStream is = t.getRequestBody();        
		InputStreamReader isr = new InputStreamReader(is,"utf-8");
		BufferedReader br = new BufferedReader(isr);

		int b;
		StringBuilder buf = new StringBuilder(512);
		while ((b = br.read()) != -1) {
			buf.append((char) b);
		}

		br.close();
		isr.close();
		System.out.println("Request Body:\n" + buf.toString());
		System.out.println();

		String response = null;

		switch(method){
		case "GET":
			response = "This is a GET response";
			break;
		case "PUT":
			response = "This is a PUT response";
			break;
		case "POST":
			response = "This is a POST response";
			break;
		case "DELETE":
			response = "This is a DELETE response";
			break;		
		}

		//Response
		System.out.println(response);
		System.out.println();

		//getResponseHeaders
		//Headers responseHeaders = t.getResponseHeaders();

		//sendResponseHeaders
		t.sendResponseHeaders(200, response.length());

		//getResponseBody
		OutputStream os = t.getResponseBody();
		os.write(response.getBytes());
		os.close();
	}
	
    public Map<String, String> queryToMap(String query)
    {
        Map<String, String> result = new HashMap<String, String>();
        
        for (String param : query.split("&")) {
            String pair[] = param.split("=");
            if (pair.length > 1) {
                result.put(pair[0], pair[1]);
            }else{
                result.put(pair[0], "");
            }
        }
        return result;
    }
}
