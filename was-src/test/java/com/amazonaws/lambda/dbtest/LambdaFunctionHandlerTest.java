package com.amazonaws.lambda.dbtest;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;

import db.DBRequestStreamHandler;

/**
 * A simple test harness for locally invoking your Lambda function handler.
 */
public class LambdaFunctionHandlerTest {

    private static final Logger log = LoggerFactory.getLogger(LambdaFunctionHandlerTest.class);

	@BeforeClass
	public static void createInput() throws IOException {
	}

	private Context createContext() {
		TestContext ctx = new TestContext();

		// TODO: customize your context here if needed.
		//ctx.setFunctionName("Your Function Name");

		return ctx;
	}

	@Test
	public void testLambdaFunctionHandler() {
		
		DBRequestStreamHandler handler = new DBRequestStreamHandler();
		Context ctx = createContext();
		ByteArrayOutputStream bao = new ByteArrayOutputStream();
		try {
			String xml = IOUtils.toString(this.getClass().getResourceAsStream("input.json"), "UTF-8");
			handler.handleRequest(this.getClass().getResourceAsStream("input.json"), bao, ctx);

			if (bao != null) {
				String output =bao.toString();
//				assertTrue(output.contains("Hi world"));
//				System.out.println(output);
			}
		} catch (IOException ioe) {
			log.error("ioe " + ioe.getMessage());
		}
	}
}
