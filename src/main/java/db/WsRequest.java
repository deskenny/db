package db;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// http://www.jsonschema2pojo.org/
// https://data.dublinked.ie/cgi-bin/rtpi/realtimebusinformation?stopid=184&format=json
// https://github.com/google/gson

public class WsRequest {
	private final Logger log = LoggerFactory.getLogger(WsRequest.class);

	// private String SERVER = "http://localhost:8081/";
	// private String SERVER = "https://www.globoforce.net/";
	public final static String SERVER = "https://data.dublinked.ie/cgi-bin/rtpi/realtimebusinformation?format=json&stopid=";
	private Map<String, String> defaultHeaders = new HashMap<String, String>();

	private static WsRequest wsRequest = new WsRequest();

	private Gson gson = new GsonBuilder().create();

	public static WsRequest getInstance() {
		return wsRequest;
	}

	public WsRequest init(String client, String username, String password) {
		return wsRequest;
	}

	public List<Result> getNextB(int stopNumber) {
		String response = doRequest(WsRequest.SERVER + stopNumber);
		StopResults results = gson.fromJson(response, StopResults.class);
		List<Result> list = results.getResults();
		
//		list.forEach((value) -> {
//			System.out.println(value);			
//		});
		return list;
	}
	
	public String doRequest(String endpoint) {
		HttpURLConnection connection = null;
		try {
			URL url = new URL(endpoint);
			connection = (HttpURLConnection) url.openConnection();
			connection.setUseCaches(false);
			connection.setRequestMethod("GET");

			final HttpURLConnection finalConnection = connection;
			defaultHeaders.forEach((key, value) -> {
				finalConnection.setRequestProperty(key, value);
			});


			connection.connect();
			int status = connection.getResponseCode();

			switch (status) {
			case 200:
			case 201:
				BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				StringBuilder sb = new StringBuilder();
				String line;
				while ((line = br.readLine()) != null) {
					sb.append(line + "\n");
				}
				br.close();

				return sb.toString();
			}
		} catch (MalformedURLException ex) {
			log.info("WsRequest: MalformedURLException endpoint={}", endpoint);
		} catch (IOException ex) {
			log.info("WsRequest: IOException endpoint={}", endpoint);
		} finally {
			if (connection != null) {
				try {
					connection.disconnect();
				} catch (Exception ex) {
					log.info("WsRequest: Exception endpoint={}", endpoint);
				}
			}
		}

		return null;
	}
}
