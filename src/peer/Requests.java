package peer;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

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

}
