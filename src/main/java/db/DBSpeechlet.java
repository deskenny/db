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
	private List<Result> buses = null;
	private String CURRENT_RESULT_INDEX = "CURRENT_RESULT_INDEX";
	private String MODE = "MODE";
	private String STOP_NUMBER = "STOP_NUMBER";
	private String STOP_NUMBER_IN_PROGRESS = "STOP_NUMBER_IN_PROGRESS";
	private int NEXT_BUS_MODE = 1;
	private int STOP_NUMBER_MODE = 2;
	private static final String SLOT_STOP_NUMBER = "StopNumber";
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
		} else if ("NextThreeBusesIntent".equals(intentName)) {
			return getNextThreeBuses(session);
		} else if ("SetStopIntent".equals(intentName)) {
			return doSetStop(session, intent);
		} else if ("AMAZON.HelpIntent".equals(intentName)) {
			return getHelpResponse(session);
		} else if ("AMAZON.StopIntent".equals(intentName)) {
			return doExit(session, "Thank you for using dublin bus today");
		} else if ("AMAZON.CancelIntent".equals(intentName)) {
			return doExit(session, "Thank you for using dublin bus today");
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
		int stopNumber = getStopNumber(session);
		buses = new WsRequest().getNextB(stopNumber);
		return buses;
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
		if (mode == NEXT_BUS_MODE) {
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
		if (mode == NEXT_BUS_MODE) {
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
		return getNextBus(session);
	}

	private SpeechletResponse doNoNextBus(Session session) {
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText("Thank you for using dublin bus today. Seeya!");
		return SpeechletResponse.newTellResponse(speech);
	}

	private SpeechletResponse doSetStop(final Session session, Intent intent) {
		try {
			log.info("slot stop number was " + intent.getSlot(SLOT_STOP_NUMBER));
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
			String speechText = "Great, I've updated your stop number to " + getStopNumber(session) + ". If you would like to hear the time of the next bus, just say next bus. Otherwise just say exit."; 
			speech.setText(speechText);
			session.setAttribute(MODE, NEXT_BUS_MODE);

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
		String speechText = "";
		int currentResultIndex = getAttributeSafe(session, CURRENT_RESULT_INDEX);
		if (currentResultIndex > 0) {
			currentResultIndex = currentResultIndex - 1;
		}

		if (getStopNumber(session) == 0) {
			speechText = "First you need to tell me the stop number by saying \"set stop\" and then the number.";
		} else {
			speechText = getDetailedBusDetailsSpeech(session, currentResultIndex, getBuses(session));
		}
		SimpleCard card = new SimpleCard();
		card.setTitle("Detailed Dublin Bus view");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);
		Reprompt reprompt = new Reprompt();
		PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
		repromptSpeech.setText("Would you like to hear about the next bus?");
		reprompt.setOutputSpeech(repromptSpeech);

		return SpeechletResponse.newAskResponse(speech, reprompt, card);
	}
		
	private String getThreeBusDetailsSpeech(final Session session, int currentResultIndex, List<Result> inBuses) {
		String speechText = "";
		if (inBuses != null && inBuses.size() >= 0) {
			if (currentResultIndex < inBuses.size()) {
				speechText = "The next three buses are " 
						+ inBuses.get(0).getRoute() + " in " + inBuses.get(0).getDuetime()
						+ "minutes, " + inBuses.get(1).getRoute() + " in " + inBuses.get(1).getDuetime()
						+ "minutes and " + inBuses.get(2).getRoute() + " in " + inBuses.get(2).getDuetime()						
						+ " minutes time. Would you like to hear more details, or next bus?";
				session.setAttribute(CURRENT_RESULT_INDEX, 3);
			} else {
				speechText = "There are not 3 buses for stop number " + getStopNumber(session) + " at this time. You can say next bus to hear one.";
			}
		} else {
			speechText = "Could not find any buses";
		}	
		return speechText;
	}

	private String getSimpleBusDetailsSpeech(final Session session, int currentResultIndex, List<Result> inBuses) {
		String speechText = "";
		if (inBuses != null && inBuses.size() >= 0) {
			if (currentResultIndex < inBuses.size()) {
				speechText = "The number " + inBuses.get(currentResultIndex).getRoute() + " bus will arrive in "
						+ inBuses.get(currentResultIndex).getDuetime()
						+ " minutes time. Would you like to hear more details, or next bus?";
				session.setAttribute(CURRENT_RESULT_INDEX, currentResultIndex + 1);
			} else {
				speechText = "No more buses listed for stop number " + getStopNumber(session);
			}
		} else {
			speechText = "Could not find any buses";
		}	
		return speechText;
	}
	
	
	private String getDetailedBusDetailsSpeech(final Session session, int currentResultIndex, List<Result> inBuses) {
		String speechText;
		if (inBuses != null && inBuses.size() >= 0) {
			if (currentResultIndex < inBuses.size()) {
				Result result = inBuses.get(currentResultIndex);
				speechText = "Route " + result.getRoute() + ". This bus goes from " + result.getOrigin() + " to "
						+ result.getDestination() + ". It's direction is " + result.getDirection()
						+ " and will leave in " + result.getDuetime()
						+ " minutes. Would you like to hear about the next bus?";
			} else {
				speechText = "No more buses listed for stop number " + getStopNumber(session);
			}
		} else {
			speechText = "Could not find any buses";
		}
		return speechText;
	}
	
	private SpeechletResponse getNextThreeBuses(final Session session) {
		List<Result> buses = getBuses(session);
		String speechText = "";
		int currentResultIndex = getAttributeSafe(session, CURRENT_RESULT_INDEX);
		session.setAttribute(MODE, NEXT_BUS_MODE);

		if (getStopNumber(session) == 0) {
			speechText = "First you need to tell me the stop number by saying \"set stop\" and then the number.";
		} else {
			speechText = getThreeBusDetailsSpeech(session, currentResultIndex, buses);
		}

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Next Three buses from Dublin Bus");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		Reprompt reprompt = new Reprompt();
		PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
		repromptSpeech.setText("Would you like to hear about the next bus?");
		session.setAttribute(CURRENT_RESULT_INDEX, currentResultIndex+1);
		reprompt.setOutputSpeech(repromptSpeech);

		return SpeechletResponse.newAskResponse(speech, reprompt, card);
		
	}
	
	private SpeechletResponse getNextBus(final Session session) {
		List<Result> buses = getBuses(session);
		String speechText = "";
		int currentResultIndex = getAttributeSafe(session, CURRENT_RESULT_INDEX);
		session.setAttribute(MODE, NEXT_BUS_MODE);

		if (getStopNumber(session) == 0) {
			speechText = "First you need to tell me the stop number by saying \"set stop\" and then the number.";
		} else {
			speechText = getSimpleBusDetailsSpeech(session, currentResultIndex, buses);
		}

		// Create the Simple card content.
		SimpleCard card = new SimpleCard();
		card.setTitle("Next Dublin Bus");
		card.setContent(speechText);

		// Create the plain text output.
		PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
		speech.setText(speechText);

		Reprompt reprompt = new Reprompt();
		PlainTextOutputSpeech repromptSpeech = new PlainTextOutputSpeech();
		repromptSpeech.setText("Would you like to hear about the next bus?");
		session.setAttribute(CURRENT_RESULT_INDEX, currentResultIndex+1);
		reprompt.setOutputSpeech(repromptSpeech);

		return SpeechletResponse.newAskResponse(speech, reprompt, card);
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
			speechText = "You can get a list of the next buses due to your stop number " + stopNumber + " by saying \"get next. You can get the next three bus times by saying, next three. If you ever want to change your stop number, just say stop number followed the the number of your stop.";
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
		if (stopNumber != 0) {
			speechText = "You can get a list of the next buses due to your stop number " + stopNumber + " by saying get next. You can get the next three bus times by saying, next three. You can change the stop number by saying set stop, followed by your bus stop number. For example, set stop 1234.";
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
}
