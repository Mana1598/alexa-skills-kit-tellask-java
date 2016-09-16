package io.klerch.alexa.tellask.model;

import com.amazon.speech.speechlet.*;
import io.klerch.alexa.state.handler.AlexaSessionStateHandler;
import io.klerch.alexa.state.utils.AlexaStateException;
import io.klerch.alexa.tellask.schema.AlexaIntentHandler;
import io.klerch.alexa.tellask.util.AlexaIntentHandlerFactory;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import java.io.IOException;

public class AlexaSpeechlet implements Speechlet {
    private final Logger LOG = Logger.getLogger(AlexaSpeechlet.class);

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session) throws SpeechletException {
        LOG.debug("Session has started.");
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session) throws SpeechletException {
        LOG.debug("Session has launched.");
        return null;
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session) throws SpeechletException {
        LOG.debug("Intent appeared.");

        final AlexaInput input = new AlexaInput(request, session);
        try {
            final AlexaIntentHandler handler = AlexaIntentHandlerFactory.createHandler(input).orElse(null);

            Validate.notNull(handler, "Could not find a handler for intent '" + request.getIntent().getName() + "'");

            final AlexaOutput output = handler.handleIntent(input);
            // save state of all models
            output.getModels().stream().forEach(model -> {
                try {
                    // ensure model has a handler. by default choose the session state handler
                    if (model.getHandler() == null) {
                        new AlexaSessionStateHandler(session).writeModel(model.getModel());
                    } else {
                        model.saveState();
                    }
                } catch (final AlexaStateException e) {
                    LOG.error("Error while saving state of a model.", e);
                }
            });
            // generate speechlet response from settings returned by the intent handler and
            // contents of YAML utterance file
            return new AlexaSpeechletResponse(output);
        } catch (final AlexaStateException e) {
            LOG.error("Error while handling an intent.", e);
        }
        return null;
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session) throws SpeechletException {
        LOG.debug("Session has ended.");
    }
}
