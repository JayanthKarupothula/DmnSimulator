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
 * Service class which has business logic to get the input names from the DMN XML
 */
@Service
public class InputNamesService {

    @Autowired
    private SimulatorDecisionTableEvaluationListener evaluationListener;

    @Autowired
    private DecisionKeyService decisionKeyService;

    @Autowired
    private LiteralExpressionsService literalExpressionsService;

    /**
     * Entry Method to get the input names from the DMN XML
     * It uses decision service to find the decision key based on the decision name
     * It will call parseDecision method to parse the DMN XML based on decision to evaluate name
     * @param reqBody
     * @return
     */
    public  Map<String, Map<String, String>> getDecisionInputs(String reqBody) {
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
        List<String> variableNames = literalExpressionsService.getVariableNames(reqBody);
        Map<String, Map<String, String>> decisionInputs = parseDecision(decisionToEvaluate, dmnEngine, inputStream, variableNames);

        return decisionInputs;
    }

    /**
     * Method to parse the decision and get the input names
     * It will call getAllInputExpressions method to get the input names
     * @param decisionToEvaluate
     * @param dmnEngine
     * @param inputStream
     * @return
     */
    private  Map<String, Map<String, String>> parseDecision(String decisionToEvaluate, DmnEngine dmnEngine, InputStream inputStream, List<String> variableNames) {
        DmnDecision decision;
        Map<String, Map<String, String>> decisionInputs = new HashMap<>();
        List<String> completedDecisions = new ArrayList<>();
        if (decisionToEvaluate != null && !decisionToEvaluate.trim().equals("")) {
            decision = dmnEngine.parseDecision(decisionToEvaluate, inputStream);
             decisionInputs = getAllInputExpressions(decision, completedDecisions, variableNames);
        } else {
            List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
            for (DmnDecision dmnDecision : decisions) {
                decisionInputs.putAll(getAllInputExpressions(dmnDecision, completedDecisions, variableNames));
            }

        }
        return decisionInputs;
    }

    /**
     * Helper method to call the getAllInputExpressionsRecursive method to get the input names
     * @param decision
     * @param completedDecisions
     * @return
     */
    private  Map<String, Map<String, String>> getAllInputExpressions(DmnDecision decision, List<String> completedDecisions, List<String> variableNames) {

        Map<String, Map<String, String>> decisionInputs = new HashMap<>();
        getAllInputExpressionsRecursive(decision, decisionInputs, completedDecisions, variableNames);
        return decisionInputs;
    }

    /**
     * Recursive method to get the input names from the DMN XML
     * It will call getAllInput method to get the input names for a particular decision
     * it checks if the decision is already added to the map or not
     * If the decision has required decisions, it will call this method recursively
     * @param decision
     * @param decisionInputs
     * @param completedDecisions
     */
    private void getAllInputExpressionsRecursive(DmnDecision decision,  Map<String, Map<String, String>> decisionInputs, List<String> completedDecisions, List<String> variableNames) {
        Map<String, String> names = getAllInput(decision, variableNames);
        Map<String, String> inputs = new HashMap<>();
        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        Collection<DmnDecision> requiredDecisions = decision.getRequiredDecisions();
        if (!completedDecisions.contains(decision.getName())) {
            if(decisionLogic instanceof DmnDecisionTableImpl){
                for (String name : names.keySet()) {
                    if(!requiredDecisions.isEmpty()){
                        for (DmnDecision requiredDecision : requiredDecisions) {
                            if (!requiredDecision.getName().equals(name)) {
                                inputs.put(name, names.get(name));
                            }
                            else {
                                getAllInputExpressionsRecursive(requiredDecision, decisionInputs, completedDecisions, variableNames);
                            }
                        }
                    }
                    else{
                        inputs.put(name, names.get(name));
                    }
                }
                decisionInputs.put(decision.getName(), inputs);
                completedDecisions.add(decision.getName());
            }
        }

    }

    /**
     * Method to get the input names of a particular decision
     * @param decision
     * @return
     */
    private Map<String, String> getAllInput(DmnDecision decision, List<String> variableNames) {
        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        Map<String, String> names = new HashMap<>();
        if(decisionLogic instanceof DmnDecisionTableImpl) {
            DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionLogic;
            List<DmnDecisionTableInputImpl> inputs = decisionTable.getInputs();
            for (DmnDecisionTableInputImpl input : inputs) {
                String expressionName = input.getExpression().getExpression();
                String variable = input.getInputVariable();
                if (!(variableNames.contains(expressionName) || variableNames.contains(variable))) {
                    String name = (input.getName() == null) ? "undefined" : input.getName();
                    String expression = (input.getExpression().getExpression() == null) ? "undefined" : input.getExpression().getExpression();
                    String type = (name.equals("undefined")) ? expression : input.getExpression().getTypeDefinition().getTypeName();
                    names.put(name, type);
                }
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
