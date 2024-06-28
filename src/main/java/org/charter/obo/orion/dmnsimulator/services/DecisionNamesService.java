package org.charter.obo.orion.dmnsimulator.services;

import static org.camunda.spin.Spin.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.camunda.bpm.dmn.engine.*;
import org.camunda.bpm.dmn.engine.impl.*;
import org.charter.obo.orion.dmnsimulator.listerners.SimulatorDecisionTableEvaluationListener;
import org.camunda.spin.json.SpinJsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.camunda.spin.Spin.JSON;

/**
 * Service class to get the decision names from the DMN XML
 */
@Service
public class DecisionNamesService {
    @Autowired
    private SimulatorDecisionTableEvaluationListener evaluationListener;

    /**
     * Entry Method to get the decision names from the DMN XML
     * @param reqBody
     * @return List of decision names
     */
    public List<String> getDecisionNames(String reqBody) {
        SpinJsonNode requestNode = JSON(reqBody);
        DmnEngine dmnEngine = buildDecisionEngine();
        InputStream inputStream = new ByteArrayInputStream(requestNode.prop("xml").stringValue().getBytes(StandardCharsets.UTF_8));
        List<String> decisionNames = parseDecision(dmnEngine, inputStream);
        return decisionNames;
    }

    /**
     * Method to parse the decisions from the DMN XML
     * and for each decision, it will call addAllDecisionNames method
     * @param dmnEngine
     * @param inputStream
     * @return List of decision names
     */
    private   List<String> parseDecision(DmnEngine dmnEngine, InputStream inputStream) {
        List<String> decisionNames = new ArrayList<>();
        List<String> completedDecisions = new ArrayList<>();
            List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
            for (DmnDecision dmnDecision : decisions) {
                addAllDecisionNames(dmnDecision, decisionNames, completedDecisions);
            }
        return decisionNames;
    }

    /**
     * Here we are adding all the decision names to the decisionNames list
     * First we check if the decision is already added to the completedDecisions list
     * If not, we add the decision name to the decisionNames list
     * If the decision has required decisions, we add the required decisions to the decisionNames list
     * @param decision
     * @param decisionNames
     * @param completedDecisions
     */
    private void addAllDecisionNames(DmnDecision decision, List<String> decisionNames, List<String> completedDecisions) {
        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        Collection<DmnDecision> requiredDecisions = decision.getRequiredDecisions();
        if(!completedDecisions.contains(decision.getName())) {
            if(decisionLogic instanceof DmnDecisionTableImpl){
                if (requiredDecisions.isEmpty()  ) {
                    String decisionName = (decision.getName() == null) ? "undefined" : decision.getName();
                    decisionNames.add(decisionName);
                    completedDecisions.add(decisionName);
                } else  {
                    String decisionName = (decision.getName() == null) ? "undefined" : decision.getName();
                    decisionNames.add(decisionName);
                    completedDecisions.add(decisionName);
                    for (DmnDecision requiredDecision : requiredDecisions) {
                        addAllDecisionNames(requiredDecision, decisionNames, completedDecisions);
                    }
                }
            }
        }
    }

    /**
     * Method to build the DMN Engine
     * @return DMN Engine
     */
    private DmnEngine buildDecisionEngine() {
        DefaultDmnEngineConfiguration engineConfiguration = (DefaultDmnEngineConfiguration) DmnEngineConfiguration
                .createDefaultDmnEngineConfiguration();
        engineConfiguration.getCustomPostDecisionTableEvaluationListeners().add(evaluationListener);
        engineConfiguration.setDefaultOutputEntryExpressionLanguage("feel");
        DmnEngine dmnEngine = engineConfiguration.buildEngine();
        return dmnEngine;
    }
}
