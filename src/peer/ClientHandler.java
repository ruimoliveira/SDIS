package peer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class ClientHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange t) {

		String method = t.getRequestMethod();
		System.out.println("Request Method: " + method);

		String path = t.getRequestURI().getPath();
		System.out.println("Request path: " + path);
		
		switch (method) {
		case "PUT":
			put(t);
			break;
		case "GET":
			get(t);
			break;
		}
	}

	private void put(HttpExchange t) {
		Headers h = t.getRequestHeaders();
		String file_length = h.getFirst("File_Length");
		String file_name = h.getFirst("File_Name");

		System.out.println("File name: " + file_name);
		System.out.println("File length: " + file_length);

		InputStream is = t.getRequestBody();
		
		/* 1. Retransmits save_request to all peers in range (number of peers in rage = n_pir)
		 * 2. Waits a random amount of time between 0 and 400 ms.
    			While counting the number of saved_file responses with same requestID (n_saved)
		 * 3. Decides on weather or not to save file based on a chance.
    			It has a cts% of saving the file, where cts = (n_pir-n_saved)/n_pir
		 * 4. Sends a saved_file to all peers in range
		 * 
		 * */
		try {
			byte[] buffer = new byte[is.available()];
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		File dir = new File("FilesToSave");
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdir();
		File file = new File(dir.getName() + "//" + file_name);
		try {
			file.createNewFile();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("File exists? " + file.exists());
		OutputStream os = null;
	    try {
			os = new FileOutputStream(file);
			byte[] body = new byte[(int) Long.parseLong(file_length)];
			int bytesRead;
			while ((bytesRead = is.read(body)) != -1) {
				os.write(body, 0, bytesRead);
			}
			is.read(body);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try { is.close(); } catch (IOException e) { }
		try { os.close(); } catch (IOException e) { }

		System.out.println("File saved successfully");
		
	}

	private void get(HttpExchange t) {
		
	}

}
