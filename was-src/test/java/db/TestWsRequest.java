package db;

import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
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
      String response = request.doRequest(WsRequest.SERVER + 2188);
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
               System.out.println("got first ");
               Elements rows = table.select("tr");
               System.out.println("got  row " + rows.size());

               for (Element row : rows) {
                  Elements cols = row.select("td");
                  Result result = new Result();
                  int count = 0;
                  for (Element col : cols) {
                     if (count == 0) {
                        result.setRoute(stripTd(col.toString()));
                     }
                     else if (count == 1) {
                        result.setDestination(stripTd(col.toString()));
                     }
                     else if (count == 2) {
                        result.setDuetime(calcDueTime(stripTd(col.toString())));
                     }
                     count++;
                  }
//                result.setDestination(cols.get(1).toString());
//                result.setDuetime(cols.get(2).toString());
                  System.out.println("route:" + result.getRoute() + " destination:" + result.getDestination() + " due:" + result.getDuetime());
//                Elements tds = row.toString();
//                System.out.println("got td " + tds.size() );
//                String busNum = tds.get(0).toString();
//                System.out.println("got td 1");
//                String busDescription = tds.get(1).toString();
//                String busTime = tds.get(2).toString();
//                System.out.println("busNum:" + busNum + " busDescription:" + busDescription + " busTime:" + busTime);
//                System.out.println("row.. has  " + tds.size());
               }
            }
         } catch (Exception ioe) {
            System.out.println("IO exception " + ioe.getMessage());
         }
      }
      return results;
   }

   private String stripTd(String in) {
      String rVal = in;
      if (in != null) {
         rVal = in.substring(in.indexOf("<td>") + 4, in.indexOf("</td>")).trim();
      }
      return rVal;
   }

   private String calcDueTime(String in) {
      SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
      SimpleDateFormat sdfOut = new SimpleDateFormat("HH:mm");

      String rVal = "0";
      if (in != null) {
         try {
            rVal = sdfOut.format(new Date(sdf.parse(in).getTime() - System.currentTimeMillis()));
         } catch (ParseException pe) {
            System.out.println("problem parsing integer:" + in);
         }
      }
      return rVal;
   }
}