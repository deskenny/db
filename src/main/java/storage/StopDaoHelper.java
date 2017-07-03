package storage;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;

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
		if (result != null) {
			Map<String, AttributeValue> resultItem = result.getItem();
			if (resultItem != null) {
				AttributeValue attributeValue = resultItem.get("stop");
				if (attributeValue != null) {
					log.info("attributeValue={}", attributeValue);
					String stop = attributeValue.getS();

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
			}
		}
		return item;
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
}
