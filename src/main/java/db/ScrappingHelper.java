package db;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class ScrappingHelper {

    public String stripAndConvert(String in, long currentTime) {
        return calcDueTime(stripTd(in), currentTime);
    }

    public String stripTd(String in) {
        String rVal = in;
        if (in != null) {
            rVal = in.substring(in.indexOf("<td>") + 4, in.indexOf("</td>")).trim();
        }
        return rVal;
    }

    public String calcDueTime(String in, long currentTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");

        String rVal = "0";
        if (in != null) {
            try {
            	Date inDate = sdf.parse(in);
            	GregorianCalendar inCal = new GregorianCalendar();
            	inCal.setTime(inDate);
            	GregorianCalendar currentCal = new GregorianCalendar();
            	currentCal.setTime(new Date(currentTime));
            	inCal.set(Calendar.YEAR, currentCal.get(Calendar.YEAR));
            	inCal.set(Calendar.MONTH, currentCal.get(Calendar.MONTH));
            	inCal.set(Calendar.DAY_OF_YEAR, currentCal.get(Calendar.DAY_OF_YEAR));

                rVal =  String.valueOf((inCal.getTimeInMillis()-currentTime)/(1000*60));
            } catch (ParseException pe) {
                System.out.println("problem parsing integer:" + in);
            }
        }
        return rVal;
    }
    
	public StopResults parseHtmlStops(String htmlIn) {
		List<Result> innerResults = new ArrayList<Result>();
		if (htmlIn != null) {
			try {
				Document doc = Jsoup.parse(htmlIn);
				Elements rtpiResults = doc.select("#rtpi-results"); // a with
																	// href
				if (rtpiResults.size() == 1) {
					Element table = rtpiResults.get(0);
					Elements rows = table.select("tr");
					int outterCount = 0;

					for (Element row : rows) {
						Elements cols = row.select("td");
						Result result = new Result();

						int count = 0;
						for (Element col : cols) {
							// System.out.println("col: " + count + " col.toString()" + col.toString());
							if (count == 0) {
								result.setRoute(stripTd(col.toString()));
							}
							else if (count == 1) {
								result.setDestination(stripTd(col.toString()));
							}
							else if (count == 2) {
								result.setDuetime(stripAndConvert(col.toString(), System.currentTimeMillis()));
							}
							count++;
						}
						
						if(outterCount > 0) {
							innerResults.add(result);
							System.out.println("route:" + result.getRoute() + " destination:" + result.getDestination() + " due:" + result.getDuetime());
						}
						outterCount++;
					}
				}
			} catch (Exception ioe) {
				System.out.println("IO exception " + ioe.getMessage());
			}
		}
		StopResults results = new StopResults();
		results.setNumberofresults(innerResults.size());
		results.setTimestamp("");
		results.setResults(innerResults);
		return results;
	}
}
