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
 * Service class  which has business logic to get the decision output names from the DMN XML
 */
@Service
public class OutputNamesService {

    @Autowired
    private SimulatorDecisionTableEvaluationListener evaluationListener;

    @Autowired
    private DecisionKeyService decisionKeyService;

    /**
     * Entry Method to get the decision output names from the DMN XML
     * It uses decision service to find the decision key based on the decision name
     * It will call parseDecision method to parse the DMN XML based on decision to evaluate name
     * @param reqBody
     * @return
     */
    public Map<String, String> getDecisionOutputs(String reqBody) {

        SpinJsonNode requestNode = JSON(reqBody);
        String decisionName = requestNode.hasProp("decision") ? requestNode.prop("decision").stringValue() : null;
        String decisionToEvaluate ;
        if (decisionName == null || decisionName.isEmpty()) {
            decisionToEvaluate = decisionName;
        } else {
            decisionToEvaluate = decisionKeyService.getDecisionKey(reqBody, decisionName);
        }
        DmnEngine dmnEngine = buildDecisionEngine();
        InputStream inputStream = new ByteArrayInputStream(requestNode.prop("xml").stringValue().getBytes(StandardCharsets.UTF_8));
        Map<String, String> decisionOutputs = parseDecision(decisionToEvaluate, dmnEngine, inputStream);
        return decisionOutputs;
    }

    /**
     * Method to parse the decision and get the output names
     * it calls getOutputNames method to get the output names
     * @param decisionToEvaluate
     * @param dmnEngine
     * @param inputStream
     * @return List of output names
     */
    private    Map<String, String> parseDecision(String decisionToEvaluate, DmnEngine dmnEngine, InputStream inputStream) {

        DmnDecision decision;
        Map<String, String> outputNames = new HashMap<>();
        List<String> completedDecisions = new ArrayList<>();
        if (decisionToEvaluate != null && !decisionToEvaluate.trim().equals("")) {
            decision = dmnEngine.parseDecision(decisionToEvaluate, inputStream);
             outputNames = getOutputNames(decision,completedDecisions);
        } else {
            List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
            for (DmnDecision dmnDecision : decisions) {
                outputNames.putAll(getOutputNames(dmnDecision,completedDecisions));
            }
        }
        return outputNames;
    }

    /**
     * Method to get the output names from the decision
     * It will check if the decision is already added to the completedDecisions list
     * If not, it will add the output names to the names list
     * If the decision has required decisions, it will add the required decisions output names to the names list
     * @param decision
     * @param completedDecisions
     * @return
     */
    public Map<String, String> getOutputNames(DmnDecision decision,List<String> completedDecisions) {

        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        Map<String, String> names = new HashMap<>();
        if(!completedDecisions.contains(decision.getName())) {
            if (decisionLogic instanceof DmnDecisionTableImpl) {
                DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionLogic;
                List<DmnDecisionTableOutputImpl> outputs = decisionTable.getOutputs();
                for (DmnDecisionTableOutputImpl output : outputs) {
                    String name = (output.getName() == null) ? "undefined" : output.getName();
                    String outputName = (output.getOutputName() == null) ? "undefined" : output.getOutputName();
                    names.put(name,outputName);
                }
                completedDecisions.add(decision.getName());
                Collection<DmnDecision> requiredDecisions = decision.getRequiredDecisions();
                if(!requiredDecisions.isEmpty()) {
                    for (DmnDecision requiredDecision : requiredDecisions) {
                        names.putAll(getOutputNames(requiredDecision,completedDecisions));
                    }
                }
                return names;
            }

        }
        return names;

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
