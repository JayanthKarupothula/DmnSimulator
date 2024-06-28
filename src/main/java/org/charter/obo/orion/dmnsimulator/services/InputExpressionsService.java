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
 * Service class to get the input Expressions of all the inputs from the DMN XML
 * evaluatedecision method uses input Expresstions to evaluate the decision
 */
@Service
public class InputExpressionsService {
    @Autowired
    private SimulatorDecisionTableEvaluationListener evaluationListener;
    @Autowired
    private DecisionKeyService decisionKeyService;

    /**
     * Entry Method to get the decision input expressions from the DMN XML
     * It uses decision service to find the decision key based on the decision name
     * It will call parseDecision method to parse the DMN XML based on decisionToEvaluate name
     * @param reqBody
     * @return Map of decision input expressions
     */
    public  Map<String, Map<String, String>> getDecisionInputExpressions(String reqBody) {
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
        Map<String, Map<String, String>> decisionInputExpressions = parseDecision(decisionToEvaluate, dmnEngine, inputStream);
        return decisionInputExpressions;
    }

    /**
     * Method to parse the decision and get the input variables
     * It will call getAllInputExpressions method to get all the input expressions
     * @param decisionToEvaluate
     * @param dmnEngine
     * @param inputStream
     * @return
     */
    private  Map<String, Map<String, String>> parseDecision(String decisionToEvaluate, DmnEngine dmnEngine, InputStream inputStream) {
        DmnDecision decision;
        Map<String, Map<String, String>> decisionInputExpressions = new HashMap<>();
        List<String> completedDecisions = new ArrayList<>();
        if (decisionToEvaluate != null && !decisionToEvaluate.trim().equals("")) {
            decision = dmnEngine.parseDecision(decisionToEvaluate, inputStream);
            decisionInputExpressions = getAllInputExpressions(decision, completedDecisions);
        } else {
            List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
            for (DmnDecision dmnDecision : decisions) {
                decisionInputExpressions.putAll(getAllInputExpressions(dmnDecision, completedDecisions));
            }
        }
        return decisionInputExpressions;
    }

    /**
     * Helper method to get all the input expressions from the decision
     * It will call getAllInputExpressionsRecursive method to get the input expressions
     * @param decision
     * @param completedDecisions
     * @return
     */
    private  Map<String, Map<String, String>> getAllInputExpressions(DmnDecision decision, List<String> completedDecisions) {
        Map<String, Map<String, String>> decisionInputExpressions = new HashMap<>();
        getAllInputExpressionsRecursive(decision, decisionInputExpressions, completedDecisions);
        return decisionInputExpressions;
    }

    /**
     * Recursive method to get all the input expressions from the decision
     * It will call getExpressions method to get the input expressions
     * first it will check if the decision is already evaluated
     * If not, it will add the input variables to the map
     * If the decision has required decisions, it will call the same method for the required decisions
     * @param decision
     * @param decisionInputExpressions
     * @param completedDecisions
     */
    private void getAllInputExpressionsRecursive(DmnDecision decision,  Map<String, Map<String, String>> decisionInputExpressions, List<String> completedDecisions) {
        Map<String, String> names = getExpressions(decision);
        Map<String, String> inputExpressions = new HashMap<>();
        Collection<DmnDecision> requiredDecisions = decision.getRequiredDecisions();
        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        if (!completedDecisions.contains(decision.getName())) {
            if (decisionLogic instanceof DmnDecisionTableImpl) {
                for (String name : names.keySet()) {
                    if(!requiredDecisions.isEmpty()){
                        for (DmnDecision requiredDecision : requiredDecisions) {
                            if (!requiredDecision.getName().equals(name)) {
                                inputExpressions.put(name, names.get(name));
                            }
                            else {
                                getAllInputExpressionsRecursive(requiredDecision, decisionInputExpressions, completedDecisions);
                            }
                        }
                    }
                    else{
                        inputExpressions.put(name, names.get(name));
                    }
                }
                decisionInputExpressions.put(decision.getName(), inputExpressions);
                completedDecisions.add(decision.getName());
            }
        }
    }

    /**
     * Method to get all the input expressions from the decision
     * It will get the input expressions from the decision table
     * for each input, it will get the name and the expression
     * @param decision
     * @return
     */
    private Map<String, String> getExpressions(DmnDecision decision) {
        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        Map<String, String> inputExpressions = new HashMap<>();
        if(decisionLogic instanceof DmnDecisionTableImpl){
            DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionLogic;
            List<DmnDecisionTableInputImpl> inputs = decisionTable.getInputs();
            for (DmnDecisionTableInputImpl input : inputs) {
                String name = (input.getName() == null) ? "undefined" : input.getName();
                String expression = (input.getExpression().getExpression() == null) ? "undefined" : input.getExpression().getExpression();
                inputExpressions.put(name, expression);
            }
        }
        return inputExpressions;
    }

    /**
     * Method to build the decision engine
     * @return
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
