package db;

import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TestWsRequest {
	
	@Test
	public void testDoGet() {
		WsRequest request = new WsRequest();
		Gson gson = new GsonBuilder().create();
		String response = request.doRequest(WsRequest.SERVER + 389);
		StopResults results = gson.fromJson(response, StopResults.class);
		List<Result> list = results.getResults();
		
		list.forEach((value) -> {
			System.out.println(value);			
		});

		
	}
}
