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
// https://www.luas.ie/index.php?id=346&get=Goldenbridge&direction=Outbound
// https://www.luas.ie/index.php?id=346&get=Goldenbridge&direction=Inbound

public class WsRequest {
	private final Logger log = LoggerFactory.getLogger(WsRequest.class);

	// private String SERVER = "http://localhost:8081/";
	// private String SERVER = "https://www.globoforce.net/";
	//"https://data.dublinked.ie/cgi-bin/rtpi/realtimebusinformation?format=json&stopid=";
	
//	public final static String SERVER = "https://data.smartdublin.ie/cgi-bin/rtpi/realtimebusinformation?format=json&stopid=";
	public final static String JSON_SERVER = "https://data.dublinked.ie/cgi-bin/rtpi/realtimebusinformation?format=json&stopid="; // stopped working 14 nov 2018
	public final static String HTML_SERVER = "https://www.dublinbus.ie/RTPI/Sources-of-Real-Time-Information/?searchtype=view&searchquery=";
	private Map<String, String> defaultHeaders = new HashMap<String, String>();
	private final static boolean USING_JSON = Boolean.FALSE;
	
	private static WsRequest wsRequest = new WsRequest();

	private Gson gson = new GsonBuilder().create();

	public static WsRequest getInstance() {
		return wsRequest;
	}

	public WsRequest init(String client, String username, String password) {
		return wsRequest;
	}

	public List<Result> getNextB(int stopNumber) {
		if (USING_JSON) {
			return getNextBusJson(stopNumber, 0);
		}
		else {
			return getNextBusHtml(stopNumber);
		}
	}
	
	public List<Result> getNextBusJson(int stopNumber, int retryNumber) {		
		if (retryNumber < 3) {
			String response = doRequest(WsRequest.JSON_SERVER + stopNumber);
			StopResults results = gson.fromJson(response, StopResults.class);
			if (results != null) {
				log.info("Got a result from dublin bus backend " + retryNumber);				
				return results.getResults();
			}
			else {
				log.info("Retrying request to dublin bus backend " + retryNumber);
				return getNextBusJson(stopNumber, retryNumber+1);
			}			
		}
		else {
			log.info("Gave up retrying dublin bus backend " + retryNumber);			
			return null;
		}
	}
	
	public List<Result> getNextBusHtml(int stopNumber) {		
			String response = doRequest(WsRequest.HTML_SERVER + stopNumber);
			ScrappingHelper sh = new ScrappingHelper();
			StopResults results = sh.parseHtmlStops(response);
			results.setStopid(String.valueOf(stopNumber));
			if (results != null) {
				log.info("Got a result from dublin bus backend ");				
				return results.getResults();
			}	
			return null;
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
