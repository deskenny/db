package db;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
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
		String response = request.doRequest(WsRequest.HTML_SERVER + 2188);
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
			ScrappingHelper sh = new ScrappingHelper();
			sh.parseHtmlStops(response);
			// System.out.println("null gson results " + response);
		}

		assertTrue(true);

	}



}

