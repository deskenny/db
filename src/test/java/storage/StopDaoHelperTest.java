package storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.GetItemResult;

@RunWith(MockitoJUnitRunner.class)
public class StopDaoHelperTest {

	@Mock
	AmazonDynamoDB mockDynamo;
	
	@Test
	public void testGetUserStopDataItem() {
		StopDaoHelper stopDao = new StopDaoHelper();
		UserStopDataItem rVal = stopDao.getUserStopDataItem(null, null);
		assertNotNull(rVal);		
	}
	
	@Test
	public void testGetUserStopDataItemMock() {
		StopDaoHelper stopDao = new StopDaoHelper();			
		
		GetItemResult input = new GetItemResult();
		
		//setup the expected result
		UserStopDataItem expectedResult = new UserStopDataItem();
		expectedResult.setStop(1234);
		
		when(mockDynamo.getItem(anyString(), any())).thenReturn(input);
		
		// didn't find the stop
		UserStopDataItem rVal = stopDao.getUserStopDataItem(null, mockDynamo);				
		assertNotNull(rVal);
		assertEquals(rVal.getStop(), 0);	
				
		// found the stop
		Map<String, AttributeValue> value = new HashMap<String, AttributeValue>();
		AttributeValue attValue = new AttributeValue();
		attValue.setS("1234");
		value.put("stop", attValue);		
		input.setItem(value);
		rVal = stopDao.getUserStopDataItem(null, mockDynamo);				
		assertNotNull(rVal);
		assertEquals(rVal.getStop(), expectedResult.getStop());	
		
		// found the stop that wasn't a number
		attValue.setS("THIS IS NOT A NUMBER");
		rVal = stopDao.getUserStopDataItem(null, mockDynamo);				
		assertNotNull(rVal);
		assertEquals(rVal.getStop(), 0);			
		
		// found the stop value was null though
		attValue.setS(null);
		rVal = stopDao.getUserStopDataItem(null, mockDynamo);				
		assertNotNull(rVal);
		assertEquals(rVal.getStop(), 0);				
				
		// did not get any item
		when(mockDynamo.getItem(anyString(), any())).thenReturn(null);
		rVal = stopDao.getUserStopDataItem(null, mockDynamo);				
		assertNotNull(rVal);
		assertEquals(rVal.getStop(), 0);		
		
		// got a result but no item
		when(mockDynamo.getItem(anyString(), any())).thenReturn(new GetItemResult());
		rVal = stopDao.getUserStopDataItem(null, mockDynamo);				
		assertNotNull(rVal);
		assertEquals(rVal.getStop(), 0);	
	}
	

}
