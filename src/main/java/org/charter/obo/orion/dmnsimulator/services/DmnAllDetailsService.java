package org.charter.obo.orion.dmnsimulator.services;

import static org.camunda.spin.Spin.*;

import java.util.*;

import org.charter.obo.orion.dmnsimulator.listerners.SimulatorDecisionTableEvaluationListener;
import org.camunda.spin.json.SpinJsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.camunda.spin.Spin.JSON;
/**
 * Service class which has logic  to get the decision table names, decision inputs, decision outputs from the DMN XML
 */
@Service
public class DmnAllDetailsService {

    @Autowired
    private SimulatorDecisionTableEvaluationListener evaluationListener;
    @Autowired
    private DecisionNamesService decisionNamesService;
    @Autowired
    private InputNamesService inputNamesService;
    @Autowired
    private OutputNamesService outputNamesService;
    @Autowired
    private InputVariablesService inputVariablesService;
    @Autowired
    private InputExpressionsService inputExpressionsService;

    /**
     * entry method is used to get the decision details from the DMN XML
     * It uses decisionNamesService to get the decision names
     * @param reqBody
     * @return
     */
    public String getDecisionDetails(String reqBody) {
        SpinJsonNode requestNode = JSON(reqBody);
        List<String> decisionNames = decisionNamesService.getDecisionNames(reqBody);
        SpinJsonNode resultNode = generateInputsOutputsNode(requestNode, decisionNames, reqBody);
        return resultNode.toString();
    }

    /**
     * Method to generate the inputs and outputs for a particular decision
     * using inputnamesService and outputNamesService
     * It calls getDecisionInputs and getDecisionOutputs methods  to get the inputs and outputs for a particular decision
     * @param requestNode
     * @param reqBody
     * @return
     */
    private SpinJsonNode generateDecisionElementsNode(SpinJsonNode requestNode, String reqBody) {
        Map<String, Map<String, String>> decisionInputs = inputNamesService.getDecisionInputs(reqBody);
        Map<String, String> decisionOutputs = outputNamesService.getDecisionOutputs(reqBody);
        Map<String, Map<String, String>> decisionInputVariables = inputVariablesService.getDecisionVariables(reqBody);
        Map<String, Map<String, String>> decisionInputExpressions = inputExpressionsService.getDecisionInputExpressions(reqBody);
        SpinJsonNode decisionElementsNode = JSON("{}");
        SpinJsonNode inputsNode = JSON(decisionInputs);
        decisionElementsNode.prop("inputs", inputsNode);
        SpinJsonNode outputsNode = JSON(decisionOutputs);
        decisionElementsNode.prop("outputs", outputsNode);
        SpinJsonNode inputVariablesNode = JSON(decisionInputVariables);
        decisionElementsNode.prop("inputVariables", inputVariablesNode);
        SpinJsonNode inputExpressionsNode = JSON(decisionInputExpressions);
        decisionElementsNode.prop("inputExpressions", inputExpressionsNode);
        return decisionElementsNode;
    }

    /**
     * Method to generate the inputs and outputs for all the decisions
     * first it will get the inputs and outputs for All-tables property
     * It calls generateDecisionElementsNode method to get the inputs and outputs for each decision
     * @param requestNode
     * @param decisionNames
     * @param reqBody
     * @return
     */
    private SpinJsonNode generateInputsOutputsNode(SpinJsonNode requestNode, List<String> decisionNames, String reqBody) {
        SpinJsonNode resultNode = JSON("{}");
        requestNode.prop("decision", "");
        reqBody = requestNode.toString();
        SpinJsonNode decisionElementsNode = generateDecisionElementsNode(requestNode, reqBody);
        resultNode.prop("All-tables", decisionElementsNode);
        for(String decisionName : decisionNames) {
            String decisionToEvaluate = decisionName;
            requestNode.prop("decision", decisionToEvaluate);
            reqBody = requestNode.toString();
            decisionElementsNode = generateDecisionElementsNode(requestNode, reqBody);
            resultNode.prop(decisionName, decisionElementsNode);
        }
        return resultNode;
    }

}
