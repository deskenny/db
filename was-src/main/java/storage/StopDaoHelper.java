package storage;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;

public class StopDaoHelper {
	private final Logger log = LoggerFactory.getLogger(StopDaoHelper.class);

	public UserStopDataItem getUserStopDataItem(String userId, AmazonDynamoDB dynamoDB) {
		log.info("getUserStopDataItem for userId={}", userId);
		UserStopDataItem item = new UserStopDataItem();
		HashMap<String, AttributeValue> map = new HashMap<String, AttributeValue>();
		AttributeValue value = new AttributeValue(userId);
		log.info("getUserStopDataItem for value={}", value);
		map.put("userid", value);
		GetItemResult result = getStopItemWithRetry(map, 0, dynamoDB);
		extractStopNumber(item, result);
		return item;
	}

	private void extractStopNumber(UserStopDataItem item, GetItemResult result) {
		if (result != null) {
			Map<String, AttributeValue> resultItem = result.getItem();
			if (resultItem != null) {
				AttributeValue attributeValue = resultItem.get("stop");
				if (attributeValue != null) {
					log.info("attributeValue={}", attributeValue);
					extractStopNumber(item, attributeValue.getS());
				}
			}
		}
	}

	private void extractStopNumber(UserStopDataItem item, String stop) {
		try {
			if (stop != null) {
				log.info("getting stop number stop={}", stop);
				item.setStop(Integer.valueOf(stop.toString()));
			} else {
				log.info("did not find stop number ");
			}
		} catch (NumberFormatException nfe) {
			log.error("Number format problem " + nfe.getMessage());
		}
	}

	private GetItemResult getStopItemWithRetry(HashMap<String, AttributeValue> map, int retryNumber,
			AmazonDynamoDB dynamoDB) {
		if (retryNumber < 3 && dynamoDB != null) {
			GetItemResult result = dynamoDB.getItem("dbstops", map);
			if (result == null) {
				log.info("Retrying request to dynamo " + retryNumber);
				return getStopItemWithRetry(map, retryNumber + 1, dynamoDB);
			} else {
				log.info("Got a result on retry " + retryNumber);
				return result;
			}
		} else {
			log.info("Gave up retrying dynamo " + retryNumber);
			return null;
		}
	}

	public void saveUserStopDataItem(String userId, Integer stop, AmazonDynamoDB dynamoDB) {
		saveUserStopDataItem(userId, stop, dynamoDB, System.currentTimeMillis());
	}

	public void saveUserStopDataItem(String userId, Integer stop, AmazonDynamoDB dynamoDB, long time) {
		PutItemRequest putItem = new PutItemRequest();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		HashMap<String, AttributeValue> map = new HashMap<String, AttributeValue>();
		map.put("userid", new AttributeValue(userId));
		map.put("stop", new AttributeValue(String.valueOf(stop)));
		map.put("time", new AttributeValue(sdf.format(time)));
		putItem.setItem(map);
		putItem.setTableName("dbstops");
		PutItemResult result = dynamoDB.putItem(putItem);
		log.info("result was " + result);
	}

}
