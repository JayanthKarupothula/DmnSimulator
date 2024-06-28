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
 * Service class to get the input variables of all the inputs from the DMN XML
 * evaluatedecision method uses input variables to evaluate the decision
 */
@Service
public class InputVariablesService {
    @Autowired
    private SimulatorDecisionTableEvaluationListener evaluationListener;
    @Autowired
    private DecisionKeyService decisionKeyService;

    /**
     * Entry Method to get the decision variables from the DMN XML
     * It uses decision service to find the decision key based on the decision name
     * It will call parseDecision method to parse the DMN XML based on decision to evaluate name
     * @param reqBody
     * @return Map of decision variables
     */
    public  Map<String, Map<String, String>> getDecisionVariables(String reqBody) {
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
        Map<String, Map<String, String>> decisionInputVariables = parseDecision(decisionToEvaluate, dmnEngine, inputStream);
        return decisionInputVariables;
    }

    /**
     * Method to parse the decision and get the input variables
     * It will call getAllInputExpressions method to get all the input variables
     * @param decisionToEvaluate
     * @param dmnEngine
     * @param inputStream
     * @return
     */
    private  Map<String, Map<String, String>> parseDecision(String decisionToEvaluate, DmnEngine dmnEngine, InputStream inputStream) {
        DmnDecision decision;
        Map<String, Map<String, String>> decisionInputVariables = new HashMap<>();
        List<String> completedDecisions = new ArrayList<>();
        if (decisionToEvaluate != null && !decisionToEvaluate.trim().equals("")) {
            decision = dmnEngine.parseDecision(decisionToEvaluate, inputStream);
            decisionInputVariables = getAllInputExpressions(decision, completedDecisions);
        } else {
            List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
            for (DmnDecision dmnDecision : decisions) {
                decisionInputVariables.putAll(getAllInputExpressions(dmnDecision, completedDecisions));
            }
        }
        return decisionInputVariables;
    }

    /**
     * Helper method to get all the input variables from the decision
     * It will call getAllInputExpressionsRecursive method to get the input variables
     * @param decision
     * @param completedDecisions
     * @return
     */
    private  Map<String, Map<String, String>> getAllInputExpressions(DmnDecision decision, List<String> completedDecisions) {
        Map<String, Map<String, String>> decisionInputVariables = new HashMap<>();
        getAllInputExpressionsRecursive(decision, decisionInputVariables, completedDecisions);
        return decisionInputVariables;
    }

    /**
     * Recursive method to get all the input variables from the decision
     * It will call getAllInputVariables method to get the input variables
     * first it will check if the decision is already evaluated
     * If not, it will add the input variables to the map
     * If the decision has required decisions, it will call the same method for the required decisions
     * @param decision
     * @param decisionInputVariables
     * @param completedDecisions
     */
    private void getAllInputExpressionsRecursive(DmnDecision decision,  Map<String, Map<String, String>> decisionInputVariables, List<String> completedDecisions) {
        Map<String, String> names = getAllInputVariables(decision);
        Map<String, String> inputVariables = new HashMap<>();
        Collection<DmnDecision> requiredDecisions = decision.getRequiredDecisions();
        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        if (!completedDecisions.contains(decision.getName())) {
            if (decisionLogic instanceof DmnDecisionTableImpl) {
                for (String name : names.keySet()) {
                    if(!requiredDecisions.isEmpty()){
                        for (DmnDecision requiredDecision : requiredDecisions) {
                            if (!requiredDecision.getName().equals(name)) {
                                inputVariables.put(name, names.get(name));
                            }
                            else {
                                getAllInputExpressionsRecursive(requiredDecision, decisionInputVariables, completedDecisions);
                            }
                        }
                    }
                    else{
                        inputVariables.put(name, names.get(name));
                    }
                }
                decisionInputVariables.put(decision.getName(), inputVariables);
                completedDecisions.add(decision.getName());
            }
        }
    }

    /**
     * Method to get all the input variables from the decision
     * It will get the input variables from the decision table
     * for each input, it will get the name and the variable
     * @param decision
     * @return
     */
    private Map<String, String> getAllInputVariables(DmnDecision decision) {
      //  DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decision.getDecisionLogic();
        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        Map<String, String> inputVariables = new HashMap<>();
        if(decisionLogic instanceof DmnDecisionTableImpl){
            DmnDecisionTableImpl decisionTable = (DmnDecisionTableImpl) decisionLogic;
            List<DmnDecisionTableInputImpl> inputs = decisionTable.getInputs();
            for (DmnDecisionTableInputImpl input : inputs) {
                String name = (input.getName() == null) ? "undefined" : input.getName();
                String variable = (input.getInputVariable() == null) ? "undefined" : input.getInputVariable();
                inputVariables.put(name, variable);
            }
        }
        return inputVariables;
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
