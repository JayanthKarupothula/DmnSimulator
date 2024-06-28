package org.charter.obo.orion.dmnsimulator.services;

import static org.camunda.spin.Spin.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

import org.camunda.bpm.dmn.engine.*;
import org.camunda.bpm.dmn.engine.delegate.DmnDecisionLogicEvaluationEvent;
import org.camunda.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.camunda.bpm.dmn.engine.delegate.DmnEvaluatedDecisionRule;
import org.camunda.bpm.dmn.engine.delegate.DmnEvaluatedOutput;
import org.camunda.bpm.dmn.engine.impl.*;
import org.charter.obo.orion.dmnsimulator.listerners.SimulatorDecisionTableEvaluationListener;
import org.camunda.bpm.engine.variable.VariableMap;
import org.camunda.bpm.engine.variable.Variables;
import org.camunda.spin.impl.json.jackson.JacksonJsonNode;
import org.camunda.spin.json.SpinJsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

import static org.camunda.spin.Spin.JSON;

/**
 * Service class which has the business logic to get the decisionID or key from the request body
 * using the decision name
 * the decision key or id is used with parseDecision method for parsing xml
 */
@Service
public class DecisionKeyService {
    @Autowired
    private SimulatorDecisionTableEvaluationListener evaluationListener;

    /**
     * Entry Method to get the decision key from the request body
     * @param reqBody
     * @param decisionName
     * @return
     */

    public String getDecisionKey(String reqBody, String decisionName) {
        SpinJsonNode requestNode = JSON(reqBody);
        DmnEngine dmnEngine = buildDecisionEngine();
        InputStream inputStream = new ByteArrayInputStream(requestNode.prop("xml").stringValue().getBytes(StandardCharsets.UTF_8));
        Map<String, String> decisionKeys = parseDecision(dmnEngine, inputStream);
        String decisionToEvaluate = findDecisionKey(decisionKeys, decisionName);
        return decisionToEvaluate;
    }

    /**
     * Method to parse the decision and get the decision keys
     * It parses the xml input stream using parseDecisions method of dmnEngine
     * for each decision it calls addAllDecisionKeys method to get the decision keys of that particular decision
     * @param dmnEngine
     * @param inputStream
     * @return
     */
    private   Map<String, String> parseDecision(DmnEngine dmnEngine, InputStream inputStream) {
        Map<String, String> decisionKeys = new HashMap<>();;
        List<String> completedDecisions = new ArrayList<>();
            List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
            for (DmnDecision dmnDecision : decisions) {
                DmnDecisionLogic decisionLogic = dmnDecision.getDecisionLogic();
                if(decisionLogic instanceof DmnDecisionTableImpl) {
                    addAllDecisionKeys(dmnDecision, decisionKeys, completedDecisions);
                }
            }

        return decisionKeys;
    }

    /**
     * Here we are adding all the decision keys to the decisionKeys map
     * It checks if the decision is already added to the map or not
     * If not it adds the decision name and key(id)to the map and
     * calls the same method for the required decisions, of that particular decision
     * @param decision
     * @param decisionKeys
     * @param completedDecisions
     */
    private void addAllDecisionKeys(DmnDecision decision,  Map<String, String> decisionKeys, List<String> completedDecisions) {

        Collection<DmnDecision> requiredDecisions = decision.getRequiredDecisions();
        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        if(!completedDecisions.contains(decision.getName())) {
            if(decisionLogic instanceof DmnDecisionTableImpl){
                if (requiredDecisions.isEmpty()  ) {
                    decisionKeys.put(decision.getName(), decision.getKey());
                    completedDecisions.add(decision.getName());
                } else  {
                    decisionKeys.put(decision.getName(), decision.getKey());
                    completedDecisions.add(decision.getName());
                    for (DmnDecision requiredDecision : requiredDecisions) {
                        addAllDecisionKeys(requiredDecision, decisionKeys, completedDecisions);
                    }
                }
            }

        }
    }

    /**
     * Method to build the decision engine
     * It creates the default configuration for the dmn engine
     * and adds the custom listener to the configuration
     * @return DmnEngine
     */
    private DmnEngine buildDecisionEngine() {

        DefaultDmnEngineConfiguration engineConfiguration = (DefaultDmnEngineConfiguration) DmnEngineConfiguration
                .createDefaultDmnEngineConfiguration();
        engineConfiguration.getCustomPostDecisionTableEvaluationListeners().add(evaluationListener);
        engineConfiguration.setDefaultOutputEntryExpressionLanguage("feel");
        DmnEngine dmnEngine = engineConfiguration.buildEngine();
        return dmnEngine;
    }

    /**
     * Method to find the decision key from the decisionKeys map
     * It iterates over the decisionKeys map and returns the key for the decision name
     * @param decisionKeys
     * @param decisionName
     * @return
     */
    private String findDecisionKey(Map<String, String> decisionKeys, String decisionName) {
        for (Entry<String, String> entry : decisionKeys.entrySet()) {
            if (entry.getKey().equals(decisionName)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
