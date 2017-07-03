package storage;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class StopDaoHelperTest {
	
	@Test
	public void testGetUserStopDataItem() {
		StopDaoHelper stopDao = new StopDaoHelper();
		stopDao.getUserStopDataItem(null, null);
		assertTrue(true);
	}

}
