package storage;

import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;

/**
 * Contains the methods to interact with the persistence layer for ScoreKeeper
 * in DynamoDB.
 */
public class StopDao {
	private AmazonDynamoDB dynamoDB = AmazonDynamoDBClientBuilder.standard().build();
	// withRegion(Regions.US_EAST_1).

	// private AmazonDynamoDBClient dynamoDB = AmazonDynamoDB.defaultClient();
	// if (amazonDynamoDBClient == null) {
	// amazonDynamoDBClient =
	// new DynamoDB(new AmazonDynamoDBClient(new
	// EnvironmentVariableCredentialsProvider()));
	private final Logger log = LoggerFactory.getLogger(StopDao.class);

	public StopDao() {
	}

	/**
	 * Reads and returns the {@link UserStopDataItem} using user information from
	 * the session.
	 * <p>
	 * Returns null if the item could not be found in the database.
	 * 
	 * @param session
	 * @return
	 */
	public UserStopDataItem getUserStopDataItem(String userId) {
		return new StopDaoHelper().getUserStopDataItem(userId, dynamoDB);

	}

	public void saveUserStopDataItem(String userId, Integer stop) {
		PutItemRequest putItem = new PutItemRequest();
		SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
		HashMap<String, AttributeValue> map = new HashMap<String, AttributeValue>();
		map.put("userid", new AttributeValue(userId));
		map.put("stop", new AttributeValue(String.valueOf(stop)));
		map.put("time", new AttributeValue(sdf.format(System.currentTimeMillis())));
		putItem.setItem(map);
		putItem.setTableName("dbstops");
		PutItemResult result = dynamoDB.putItem(putItem);
		log.info("result was " + result);
	}
}
