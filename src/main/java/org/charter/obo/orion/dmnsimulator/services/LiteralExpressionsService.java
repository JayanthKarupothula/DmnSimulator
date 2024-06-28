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
 * Service class to get the outputs variables Names of all Dmn Literal Expressions from the DMN XML
 */
@Service
public class LiteralExpressionsService {
    @Autowired
    private SimulatorDecisionTableEvaluationListener evaluationListener;

    /**
     * Entry Method to get the decision names from the DMN XML
     * @param reqBody
     * @return List of decision names
     */
    public List<String> getVariableNames(String reqBody) {
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
        List<String> variableNames = new ArrayList<>();
        List<String> completedDecisions = new ArrayList<>();
        List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
        for (DmnDecision dmnDecision : decisions) {
            addAllVariableNames(dmnDecision, variableNames, completedDecisions);
        }
        return variableNames;
    }

    /**
     * Here we are adding all the variable Names to the variableNames list
     * first we check if decision is a literal expression or not
     * then we check if variable is already added to the completedVariables list
     * If not, we add the decision name to the variableNames list
     * If the decision has required decisions, we add the required decisions to the variableNames list
     * @param decision
     * @param variableNames
     * @param completedVariables
     */
    private void addAllVariableNames(DmnDecision decision, List<String> variableNames, List<String> completedVariables) {
        DmnDecisionLogic decisionLogic = decision.getDecisionLogic();
        Collection<DmnDecision> requiredDecisions = decision.getRequiredDecisions();
        if(decisionLogic instanceof DmnDecisionLiteralExpressionImpl) {
            DmnDecisionLiteralExpressionImpl literalExpression = (DmnDecisionLiteralExpressionImpl)decision.getDecisionLogic();
            if( !completedVariables.contains(literalExpression.getVariable().getName())){
                if (requiredDecisions.isEmpty()  ) {
                    String variableName = literalExpression.getVariable().getName() == null ? "undefined" : literalExpression.getVariable().getName();
                    variableNames.add(variableName);
                    completedVariables.add(variableName);
                } else  {
                    String variableName = literalExpression.getVariable().getName() == null ? "undefined" : literalExpression.getVariable().getName();
                    variableNames.add(variableName);
                    completedVariables.add(variableName);
                    for (DmnDecision requiredDecision : requiredDecisions) {
                        addAllVariableNames(requiredDecision, variableNames, completedVariables);
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
