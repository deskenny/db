/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package db;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

import storage.StopDao;
import storage.UserStopDataItem;

public class DBSpeechlet implements Speechlet {
	private static final Logger log = LoggerFactory.getLogger(DBSpeechlet.class);
	private final static String CURRENT_RESULT_INDEX = "CURRENT_RESULT_INDEX";
	private final static String MODE = "MODE";
	private final static String STOP_NUMBER = "STOP_NUMBER";
	private final static String STOP_NUMBER_IN_PROGRESS = "STOP_NUMBER_IN_PROGRESS";
	private final static int THREE_BUS_MODE = 1;
	private final static int STOP_NUMBER_MODE = 2;
	private final static int DETAILED_LIST_MODE = 3;	
	private final static String SLOT_STOP_NUMBER = "StopNumber";
	
	private StopDao stopDao = null;	
	
	private StopDao getStopDao() {
		if (stopDao == null) {
			stopDao = new StopDao();
		}
		return stopDao;
	}
	
	@Override
	public void onSessionStarted(final SessionStartedRequest request, final Session session) throws SpeechletException {
		log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
		// any initialization logic goes here
	}

	@Override
	public SpeechletResponse onLaunch(final LaunchRequest request, final Session session) throws SpeechletException {
		log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
		return getWelcomeResponse(session);
	}

	@Override
	public SpeechletResponse onIntent(final IntentRequest request, final Session session) throws SpeechletException {		
		log.info("onIntent requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());

		Intent intent = request.getIntent();
		String intentName = (intent != null) ? intent.getName() : null;

		if ("WelcomeIntent".equals(intentName)) {
			return getWelcomeResponse(session);
		} else if ("MoreDetailsIntent".equals(intentName)) {
			return getMoreDetails(session);
		} else if ("YesIntent".equals(intentName)) {
			return doYes(session);
		} else if ("NoIntent".equals(intentName)) {
			return doNo(session);
		} else if ("NextBusIntent".equals(intentName)) {
			return getNextBus(session);
		} else if ("SetStopIntent".equals(intentName)) {
			return doSetStop(session, intent);
		} else if ("AMAZON.HelpIntent".equals(intentName)) {
			return getHelpResponse(session);
		} else if ("AMAZON.StopIntent".equals(intentName)) {
			return doExit(session, "Goodbye.");
		} else if ("AMAZON.CancelIntent".equals(intentName)) {
			return doExit(session, "Goodbye.");
		} else {
			return getWelcomeResponse(session);
		}
	}

	private int getStopNumber(Session session) {
		int stop = getAttributeSafe(session, STOP_NUMBER);
		if (stop == 0) {
			log.info("Getting stop for " + session.getUser().getUserId());
			UserStopDataItem userStop = getStopDao().getUserStopDataItem(session.getUser().getUserId());
			log.info("userStop is " + userStop);
			if (userStop != null) {
				stop = userStop.getStop();
			}
		}
		return stop;
	}
	
	private List<Result> getBuses(Session session) {
		return new WsRequest().getNextB(getStopNumber(session));
	}

	private int getAttributeSafe(Session session, String name) {
		int questionNumber = 0;
		if (session.getAttribute(name) != null) {
			questionNumber = (Integer) session.getAttribute(name);
		}
		return questionNumber;
	}

	private SpeechletResponse doYes(Session session) {
		int mode = getAttributeSafe(session, MODE);
		if (mode == THREE_BUS_MODE || mode == DETAILED_LIST_MODE) {
			return doYesNextBus(session);
		} else if (mode == STOP_NUMBER_MODE) {
			return doYesSetStop(session);
		} else {
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
			speech.setText("I wasn't expecting a yes there. Please try again");
			return SpeechletResponse.newTellResponse(speech);
		}
	}

	private SpeechletResponse doNo(Session session) {
		int mode = getAttributeSafe(session, MODE);
		if (mode == THREE_BUS_MODE || mode == DETAILED_LIST_MODE) {
			return doNoNextBus(session);
		} else if (mode == STOP_NUMBER_MODE) {
			return doNoSetStop(session);
		} else {
			PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
			speech.setText("I wasn't expecting a no there. Please try again");
			return SpeechletResponse.newTellResponse(speech);
		}
	}

	private SpeechletResponse doYesNextBus(Session session) {
		int mode = getAttributeSafe(session, MODE);

		if (mode == THREE_BUS_MODE) {
			return getNextBus(session);
		}
		else {
			return getDetailedBusDetailsSpeech(session, getAttributeSafe(session, CURRENT_RESULT_INDEX),  getBuses(session));
		}
	}

	private SpeechletResponse doNoNextBus(Session session) {
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		if (Math.random() > 0.75) {
			speech.setText("Goodbye.");
		} else if (Math.random() > 0.5) {
			speech.setText("Slawwn.");
		} else if (Math.random() > 0.25) {
			speech.setText("Safe trip.");
		} else {
			speech.setText("Enjoy.");
		}
			
		return SpeechletResponse.newTellResponse(speech);
	}

	private SpeechletResponse doSetStop(final Session session, Intent intent) {
		try {
			log.info("slot stop number named " + intent.getSlot(SLOT_STOP_NUMBER).getName() + " was " + intent.getSlot(SLOT_STOP_NUMBER).getValue());
			int stopNumber = Integer.parseInt(intent.getSlot(SLOT_STOP_NUMBER).getValue());
			session.setAttribute(STOP_NUMBER_IN_PROGRESS, stopNumber);
			session.setAttribute(MODE, STOP_NUMBER_MODE);
			log.info("setting stop number to " + stopNumber);
			return getAskSpeechletResponse("I think you want to set the stop number to " + stopNumber + ". Is this correct?");

		} catch (NumberFormatException e) {
			log.error("problem setting stop number to  " + intent.getSlot(SLOT_STOP_NUMBER).getValue());
			String speechText = "I had a problem with that stop number. Please say again?";
			return getAskSpeechletResponse(speechText);
		}
	}

	private SpeechletResponse doYesSetStop(final Session session) {
		session.setAttribute(STOP_NUMBER, getAttributeSafe(session, STOP_NUMBER_IN_PROGRESS));
		session.setAttribute(MODE, STOP_NUMBER_MODE);
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		try {

			int stop = getStopNumber(session);
			//session.getUser().getUserI
			// new UploadObjectSingleOperation().uploadFile("gftestdbbucket", session.getUser().getUserId(), "" + stop);
			getStopDao().saveUserStopDataItem(session.getUser().getUserId(), stop);
			String speechText = "Great, I've updated your stop number to " + getStopNumber(session) + ". If you would like to hear the time of the next buses, just say next buses. Otherwise just say exit."; 
			speech.setText(speechText);
			session.setAttribute(MODE, THREE_BUS_MODE);

			SimpleCard card = new SimpleCard();
			card.setTitle("Setting stop");
			card.setContent(speechText);			
			
			Reprompt reprompt = new Reprompt();
			PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
			repromptSpeech.setText("Would you like to hear about the next bus?");
			reprompt.setOutputSpeech(repromptSpeech);			
			return SpeechletResponse.newAskResponse(speech, reprompt, card);

		}
		catch (Exception ioe) {
			log.error("Had a problem saving the stop " + ioe.getMessage());
			speech.setText("Had a problem saving the stop " + getStopNumber(session) + ", sorry. Please try again later.");
			return SpeechletResponse.newTellResponse(speech);
		}
	}

	private SpeechletResponse doNoSetStop(Session session) {
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		String speechText = "O.K. I've left the stop number the way it was. If you would like to try again, say set stop followed by the number. Alternatively say exit.";
		speech.setText(speechText);
		SimpleCard card = new SimpleCard();
		card.setTitle("Setting stop");
		card.setContent(speechText);
		
		Reprompt reprompt = new Reprompt();
		PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
		repromptSpeech.setText("Say set stop followed by the number, or exit.");
		
		return SpeechletResponse.newAskResponse(speech, reprompt, card);
	}

	private SpeechletResponse getMoreDetails(final Session session) {
		SpeechletResponse response = null;
		int currentResultIndex = getAttributeSafe(session, CURRENT_RESULT_INDEX);

		if (getStopNumber(session) == 0) {
			response = promptToSetStopNumberResponse();
		} else {
			response = getDetailedBusDetailsSpeech(session, currentResultIndex, getBuses(session));
		}

		return response ;
	}

	private SpeechletResponse getNextBus(final Session session) {
		SpeechletResponse response = null;

		int currentResultIndex = getAttributeSafe(session, CURRENT_RESULT_INDEX);
		session.setAttribute(MODE, THREE_BUS_MODE);

		if (getStopNumber(session) == 0) {
			response = promptToSetStopNumberResponse();
		} else {
			response = getThreeBusDetailsSpeech(session, currentResultIndex, getBuses(session));
		}

		return response;
	}
	private SpeechletResponse promptToSetStopNumberResponse() {
		SpeechletResponse response;
		String speechText = "First you need to tell me the stop number by saying \"set stop\" and then the number.";
		SimpleCard card = getCard(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);
		Reprompt reprompt = new Reprompt();
		PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
		repromptSpeech.setText("Would you like to hear about the next bus?");
		reprompt.setOutputSpeech(repromptSpeech);
		response = SpeechletResponse.newAskResponse(speech, reprompt, card);
		return response;
	}

	private SimpleCard getCard(String speechText) {
		SimpleCard card = new SimpleCard();
		card.setTitle("Detailed Dublin Bus view");
		card.setContent(speechText);
		return card;
	}
		
	private SpeechletResponse getThreeBusDetailsSpeech(final Session session, int currentResultIndex, List<Result> inBuses) {
		String speechText = "";
		if (inBuses != null && inBuses.size() >= 0) {
			
			if (currentResultIndex + 3 < inBuses.size()) {
				speechText = getMoreThanThreeBusString(inBuses, currentResultIndex);
				session.setAttribute(CURRENT_RESULT_INDEX, currentResultIndex + 3);
				return newAskResponse(speechText, "Would you like to hear about the next set of buses?", speechText);
			}
			else if (currentResultIndex + 2 < inBuses.size()) {
				speechText = getThreeBusString(inBuses, currentResultIndex);
				session.setAttribute(CURRENT_RESULT_INDEX, currentResultIndex + 3);
				return newTellResponse(speechText);
			}
			else if (currentResultIndex + 1 < inBuses.size()) {
				speechText = getTwoBusString(inBuses, currentResultIndex);
				session.setAttribute(CURRENT_RESULT_INDEX, currentResultIndex + 2);
				return newTellResponse(speechText);
			}
			else if (currentResultIndex < inBuses.size()) {
				speechText = getOneBusString(inBuses, currentResultIndex);
				session.setAttribute(CURRENT_RESULT_INDEX, currentResultIndex + 1);
				return newTellResponse(speechText);		
			} else {
				return newTellResponse("There are no more details to tell you about stop number " + getStopNumber(session) + " at this time.");
			}
		} else {
			return newTellResponse("The National Transport Authority and Smart Dublin, have temporarily suspended data services. Check back in 1 or 2 weeks.");
		}	
	}

	private String getDueTime(Result bus) {
		if (bus != null && bus.getDuetime().equalsIgnoreCase("due")) {
			return bus.getRoute() + " which is due now";
		}
		else if (bus != null && bus.getDuetime().equalsIgnoreCase("1")) {
			return bus.getRoute() + " in 1 minute";			
		} 
		else {
			return bus.getRoute() + " in " + bus.getDuetime() + " minutes";
		}
	}
	
	private String getMoreThanThreeBusString(List<Result> inBuses, int currentResultIndex) {
		return "The next three buses are " 
				+  getDueTime(inBuses.get(currentResultIndex)) + ", " 
				+  getDueTime(inBuses.get(currentResultIndex+1)) + ", "
				+ " and " + getDueTime(inBuses.get(currentResultIndex+2))					
				+ " time. Would you like to hear about the next set of buses?";
	}
	
	private String getThreeBusString(List<Result> inBuses, int currentResultIndex) {
		return "The next three buses are " 
				+  getDueTime(inBuses.get(currentResultIndex)) + ", " 
				+  getDueTime(inBuses.get(currentResultIndex+1)) + ", "
				+ " and " + getDueTime(inBuses.get(currentResultIndex+2))					
				+ " time. Goodbye";		
	}
	
	private String getTwoBusString(List<Result> inBuses, int currentResultIndex) {
		return "The next two buses are " 
				+  getDueTime(inBuses.get(currentResultIndex)) + ", and " 
				+  getDueTime(inBuses.get(currentResultIndex+1)) + " "
				+ " time. Goodbye";		
	}

	private String getOneBusString(List<Result> inBuses, int currentResultIndex) {
		return "The last bus listed is " +  getDueTime(inBuses.get(currentResultIndex)) + " time. Goodbye";				
	}
	
	private SpeechletResponse getDetailedBusDetailsSpeech(final Session session, int currentResultIndex, List<Result> inBuses) {
		session.setAttribute(MODE, DETAILED_LIST_MODE);
		String speechText;
		if (inBuses != null && inBuses.size() >= 0) {
			if (currentResultIndex < inBuses.size()) {
				Result result = inBuses.get(currentResultIndex);
				speechText = "Route " + result.getRoute() + ". This bus goes from " + result.getOrigin() + " to "
						+ result.getDestination() + ". It's direction is " + result.getDirection()
						+ " and will leave in " + result.getDuetime()
						+ " minutes. Would you like to hear about the next bus?";
				session.setAttribute(CURRENT_RESULT_INDEX, currentResultIndex + 1);
				return newAskResponse(speechText, "Would you like to hear about the next bus?", speechText);
			} else {
				return newTellResponse("No more buses currently listed for stop number " + getStopNumber(session));
			}
		} else {
			return newTellResponse("Could not find any buses");
		}
	}
	
	
	@Override
	public void onSessionEnded(final SessionEndedRequest request, final Session session) throws SpeechletException {
		log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
		// any cleanup logic goes here
	}

	/**
	 * Creates and returns a {@code SpeechletResponse} with a welcome message.
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getWelcomeResponse(final Session session) {
		int stopNumber = getStopNumber(session);

		String speechText = "";
		if (stopNumber != 0) {
			return getNextBus(session);
		} else {
			speechText = "You can get a list of the next buses due to your stop, but first you need to set your stop number. This is usually a four digit number written on your bus stop. You should just need to do this once and I'll remember it for you.";
		}
		
		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Dublin Bus");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		// Create reprompt
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(speech);
		
		resetState(session);
		return SpeechletResponse.newAskResponse(speech, reprompt, card);
	}

	/**
	 * Creates a {@code SpeechletResponse} for the help intent.
	 *
	 * @return SpeechletResponse spoken and visual response for the given intent
	 */
	private SpeechletResponse getHelpResponse(final Session session) {

		int stopNumber = getStopNumber(session);
		String speechText = "";
		int mode = getAttributeSafe(session, MODE);

		if (mode == STOP_NUMBER_MODE) {
			speechText = "You are currently are setting the stop number. Sometimes I can find it hard to recognise the number, so try speaking the number digit by digit. Also I sometimes confuse the word 'to', with the number 'two', so just the numbers. If you are not trying to set the stop number, just say exit and start again.";			
		}
		else if (mode == THREE_BUS_MODE) {
			speechText = "You are currently listening in summary mode. If you want to switch to listen to more detailed list, just say, \"next bus in detail\"";
		}
		else if (mode == DETAILED_LIST_MODE) {
			speechText = "You are currently listening in detail mode. If you want to switch to a summary list just say, \"give me a summary\"";
		}
		else if (stopNumber != 0) {
			speechText = "You can get a list of the next buses due to your stop number " + stopNumber + " by saying \"when are the next buses. If you want to get detailed information, just say \"get detailed information\". You can change the stop number by saying set stop, followed by your bus stop number. For example, set stop 1234.";
		} else {
			speechText = "You can get a list of the next buses due to your stop, but first you need to set your stop number. This is usually a four digit number written on your bus stop. You should just need to do this once and I'll remember it.";
		}

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Dublin Bus");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		// Create reprompt
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(speech);

		return SpeechletResponse.newAskResponse(speech, reprompt, card);
	}

	private void resetState(Session session) {
		session.setAttribute(CURRENT_RESULT_INDEX, 0);
		session.setAttribute(STOP_NUMBER_IN_PROGRESS, 0);
		session.setAttribute(MODE, 0);		
	}
	
	private SpeechletResponse doExit(Session session, String message) {
		resetState(session);
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
		outputSpeech.setText(message);
		return SpeechletResponse.newTellResponse(outputSpeech);
	}

	/**
	 * Returns an ask Speechlet response for a speech and reprompt text.
	 *
	 * @param speechText
	 *            Text for speech output
	 * @param repromptText
	 *            Text for reprompt output
	 * @return ask Speechlet response for a speech and reprompt text
	 */
	private SpeechletResponse getAskSpeechletResponse(String speechText) {
		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Session");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		// Create reprompt
		PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
		repromptSpeech.setText(speechText);
		Reprompt reprompt = new Reprompt();
		reprompt.setOutputSpeech(repromptSpeech);

		return SpeechletResponse.newAskResponse(speech, reprompt, card);
	}
	
	private SpeechletResponse newTellResponse(String speech) {
		PlainTextOutputSpeech opSpeech = new PlainTextOutputSpeech();
		opSpeech.setText(speech);
		SpeechletResponse.newTellResponse(opSpeech);
		return SpeechletResponse.newTellResponse(opSpeech);
	}
	
	private SpeechletResponse newAskResponse(String speechText, String repromptText, String card) {
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);
		
		Reprompt reprompt = new Reprompt();
		PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
		repromptSpeech.setText(repromptText);
		reprompt.setOutputSpeech(repromptSpeech);				
		return SpeechletResponse.newAskResponse(speech, reprompt, getCard(speechText));	
	}	
		
}
