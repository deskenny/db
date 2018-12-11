package db;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

public class TestWsRequest {

	@Test
	public void testDoGet() {
		WsRequest request = new WsRequest();
		Gson gson = new GsonBuilder().create();
		String response = request.doRequest(WsRequest.SERVER + 389);
		List<Result> list = null;
		StopResults results = null;
		try {
			results = gson.fromJson(response, StopResults.class);
		} catch (JsonSyntaxException jse) {
			System.out.println("Didnt get JSON, will try HTML parse now");
		}
		if (results != null) {
			list = results.getResults();
			list.forEach((value) -> {
				System.out.println(value);
			});
		} else {
			parseHtmlStops(response);
			// System.out.println("null gson results " + response);
		}

		assertTrue(true);

	}

	public StopResults parseHtmlStops(String stopsIn) {
		StopResults results = null;
		if (stopsIn != null) {
			try {
				Document doc = Jsoup.parse(stopsIn);
				Elements rtpiResults = doc.select("#rtpi-results"); // a with
																	// href
				if (rtpiResults.size() == 1) {
					System.out.println("Size " + rtpiResults.size());
					Element table = rtpiResults.get(0);
					Elements rows = table.getAllElements();
					for (Element row : rows) {
						Elements tds = row.getAllElements();
						System.out.println("row.. has  " + tds.size());
						for (Element td : tds) {
							System.out.println("td " + td);
						}
					}
				}
			} catch (Exception ioe) {
				System.out.println("IO exception " + ioe.getMessage());
			}
		}
		return results;
	}

}
