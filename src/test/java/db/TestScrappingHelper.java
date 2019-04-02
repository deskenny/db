package db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestScrappingHelper {

	@Test
	public void testCalcDueTime() throws ParseException {
		ScrappingHelper sh = new ScrappingHelper();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		long time = sdf.parse("01-01-1970 14:34").getTime();
		assertEquals("4", sh.calcDueTime("14:38", time));

		time = sdf.parse("01-01-1970 14:24").getTime();
		assertEquals("14", sh.calcDueTime("14:38", time));

		time = sdf.parse("01-01-1970 13:24").getTime();
		assertEquals("74", sh.calcDueTime("14:38", time));

		time = sdf.parse("01-01-1970 15:24").getTime();
		assertEquals("-46", sh.calcDueTime("14:38", time));

	}

	@Test
	public void testCalcDueTimeDaylightSavings() throws ParseException {
		ScrappingHelper sh = new ScrappingHelper();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
		long time = sdf.parse("01-01-1970 14:34").getTime() + (1000*60*60);
		assertEquals("4", sh.calcDueTime("15:38", time));

		time = sdf.parse("01-01-1970 14:24").getTime() + (1000*60*60);
		assertEquals("14", sh.calcDueTime("15:38", time));

		time = sdf.parse("01-01-1970 13:24").getTime() + (1000*60*60);
		assertEquals("74", sh.calcDueTime("15:38", time));

		time = sdf.parse("01-01-1970 15:24").getTime() + (1000*60*60);
		assertEquals("-46", sh.calcDueTime("15:38", time));

	}


}

