package peer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

@SuppressWarnings("restriction")
public class ClientHandler implements HttpHandler {

	@Override
	public void handle(HttpExchange t) {

		String method = t.getRequestMethod();
		String path = t.getRequestURI().getPath();
		String[] pathContent = path.split("/");
		
		if (pathContent.length == 2)
			switch (method) {
			case "PUT":
				try {
					System.out.println("ClientHandler: Received upload request from client.");
					upload(t);
				} catch (IOException e) {
					e.printStackTrace();
				}
				break;
			case "GET":
				//list(t);
				break;
			}
		else if(pathContent.length == 3 && pathContent[2].compareTo("file") == 0){
			switch (method) {
			case "GET":
				download(t);
				break;
			case "DELETE":
				delete(t);
				break;
			}
		}
	}

	private void delete(HttpExchange t) {
		Headers h = t.getRequestHeaders();
		String fileID = h.getFirst("File_ID");
		
		try {
			if (fileID == null) {
				t.sendResponseHeaders(400, -1);
				return;
			}

			/* Check for file locally */
			File dbDir = new File("database" + Peer.getId());
			File file2delete = new File(dbDir.getName() + "//" + fileID);
			
			if (file2delete.exists() && !file2delete.isDirectory())
				file2delete.delete();
			
			/* TODO: send delete command to other peers */

			if (!file2delete.exists()) {
				t.sendResponseHeaders(200, 0);
				return;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
	}

	private void download(HttpExchange t) {
		Headers h = t.getRequestHeaders();
		String fileID = h.getFirst("File_ID");

		FileInputStream fs = null;
		OutputStream os = null;
		try {

			if (fileID == null) {
				t.sendResponseHeaders(400, -1);
				return;
			}

			/* Check for file locally */
			File dbDir = new File("database" + Peer.getId());
			File fileRequested = new File(dbDir.getName() + "//" + fileID);

			if (!fileRequested.exists() || fileRequested.isDirectory()) {
				/* Check for file abroad */
				File dir = new File("tempFiles" + Peer.getId());
				if (!dir.exists() || !dir.isDirectory())
					dir.mkdir();
				fileRequested = new File(dir.getName() + "//" + fileID);

				/*
				 * TODO: Start getFile protocol between Servers
				 * 
				 * boolean hasFile = new Thread().getFile(tempFile);
				 *
				 * that returns if the file is in the network and was saved in
				 * tempFiles.
				 * 
				 * For now it simulates waiting for file response
				 */

				try { TimeUnit.SECONDS.sleep(4); } catch (InterruptedException e) {}
			}
			

			if (!fileRequested.exists() || fileRequested.isDirectory()) {
				t.sendResponseHeaders(204, -1);
				return;
			}

			t.sendResponseHeaders(200, 0);
			os = t.getResponseBody();

			fs = new FileInputStream(fileRequested);
			byte[] bytes = new byte[1000];

			int n;
			while((n = fs.read(bytes)) != -1){
				os.write(bytes, 0, n);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return;
		} finally {
			try { fs.close(); } catch (IOException e) {}
			try { os.close(); } catch (IOException e) {}
		}
	}

	@SuppressWarnings("resource")
	private void upload(HttpExchange t) throws IOException {
		Headers h = t.getRequestHeaders();
		String file_length = h.getFirst("File_Length");
		String fileID = IdGenerator.nextId();

		InputStream is = t.getRequestBody();
		
		/* Create file to share */
		File dir = new File("tempFiles" + Peer.getId());
		
		if (!dir.exists() || !dir.isDirectory())
			dir.mkdir();
		
		File tempFile = new File(dir.getName() + "//" + fileID);
		
		try {
			tempFile.createNewFile();
		} catch (IOException e) {
			t.sendResponseHeaders(500, -1);
			e.printStackTrace();
			tempFile.delete();
			return;
		}
		
		/* Get file from message */
		OutputStream os = null;
	    try {
			os = new FileOutputStream(tempFile);
			byte[] body = new byte[(int) Long.parseLong(file_length)];
			int bytesRead;
			while ((bytesRead = is.read(body)) != -1) {
				os.write(body, 0, bytesRead);
			}
			is.read(body);
		} catch (FileNotFoundException e) {
			t.sendResponseHeaders(500, -1);
			e.printStackTrace();
			tempFile.delete();
			return;
		} catch (IOException e) {
			t.sendResponseHeaders(400, -1);
			e.printStackTrace();
			tempFile.delete();
			return;
		}
		
		try { is.close(); } catch (IOException e) { }
		try { os.close(); } catch (IOException e) { }

		t.sendResponseHeaders(200, 0);
		os = t.getResponseBody();
		os.write(fileID.getBytes());
		
		try { os.close(); } catch (IOException e) { }

		/* Forward file with other peers */
		String msgID = IdGenerator.nextId();
		Peer.addMessageReceived(msgID);
		Peer.addStoredMessageReceived(msgID);
		String senderID = Peer.getId();
		Requests.forward(msgID, fileID, senderID, tempFile);
		
	}

}
