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
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

import static org.camunda.spin.Spin.JSON;

@Service
public class EvaluateDecisionService {

    @Autowired
    private SimulatorDecisionTableEvaluationListener evaluationListener;
    @Autowired
    private DecisionKeyService decisionKeyService;

    /**
     * Entry Method to evaluate the decision
     * It parses the decision from the request, evaluates it using the DMN engine,
     * and returns the result as a JSON string.
     * @param reqBody
     * @return JSON response
     */
    public SpinJsonNode evaluateDecision(String reqBody) {
        SpinJsonNode rootNode = JSON("{}");
        try {
            SpinJsonNode requestNode = JSON(reqBody);
            String decisionName = requestNode.hasProp("decision") ? requestNode.prop("decision").stringValue() : null;
            String decisionToEvaluate ;
            if (decisionName == null || decisionName.isEmpty()) {
                decisionToEvaluate = decisionName;
            } else {
                decisionToEvaluate = decisionKeyService.getDecisionKey(reqBody, decisionName);
            }
            VariableMap variables = getVariables(requestNode);
            DmnEngine dmnEngine = buildDecisionEngine();
            InputStream inputStream = new ByteArrayInputStream(requestNode.prop("xml").stringValue().getBytes(StandardCharsets.UTF_8));
            DmnDecision decision = parseDecision(decisionToEvaluate, dmnEngine, inputStream);
            DmnDecisionResult result = dmnEngine.evaluateDecision(decision, variables);
            SpinJsonNode decisionNode = JSON("{}");
            SpinJsonNode resultValues = JSON(result.getResultList());
            decisionNode.prop("results", resultValues.toString());
            rootNode.prop(decision.getName(), decisionNode);
            getEvaluatedRules(rootNode, variables);
        } catch (Exception e) {
            rootNode.prop("error", e.getMessage());
        }

        return rootNode;
    }

    /**
     * This method is used to convert the variables from the request node into a VariableMap.
     * It first maps the variables from the request node into a HashMap.
     * Then, it iterates over each entry in the HashMap, and depending on the type of the value, it adds the entry to the VariableMap.
     * If the value is an array, it is mapped to an ArrayList and added to the VariableMap.
     * If the value is not an array, it is added to the VariableMap based on its type.
     * If the type is not specified, the value is added as an untyped value.
     *
     * @param requestNode The request node from which the variables are extracted.
     * @return The VariableMap containing the variables from the request node.
     * @throws Exception If an error occurs while processing the variables.
     */
    public VariableMap getVariables(SpinJsonNode requestNode) throws Exception {

        @SuppressWarnings({ "unchecked" })
        HashMap<String, String> mappedVariables = (HashMap<String, String>) requestNode.prop("variables")
                .mapTo(java.util.HashMap.class);
        VariableMap variables = Variables.createVariables();

        for (Entry<String, String> variable : mappedVariables.entrySet()) {

            SpinJsonNode valueJson = JSON(variable.getValue());
            SpinJsonNode valueJsonValueNode = valueJson.prop("value");

            if (valueJsonValueNode.isArray()) {
                JacksonJsonNode o = (JacksonJsonNode) valueJsonValueNode;
                ArrayList<Object> myVariable = o.mapTo(ArrayList.class);
                variables.putValue(variable.getKey(), myVariable);
            } else {
                Object valueObj = valueJsonValueNode.value();
                if (valueJson.hasProp("type")) {
                    String type = valueJson.prop("type").stringValue();

                    if (type.equalsIgnoreCase("String")) {
                        variables.putValueTyped(variable.getKey(), Variables.stringValue((String) valueObj));
                    } else if (type.equalsIgnoreCase("Boolean")) {
                        variables.putValueTyped(variable.getKey(), Variables.booleanValue((Boolean) valueObj));
                    } else if (type.equalsIgnoreCase("Integer")) {
                        variables.putValueTyped(variable.getKey(), Variables.integerValue((Integer) valueObj));
                    } else if (type.equalsIgnoreCase("Double")) {
                        variables.putValueTyped(variable.getKey(), Variables.doubleValue((Double) valueObj));
                    } else if (type.equalsIgnoreCase("Long")) {
                        variables.putValueTyped(variable.getKey(), Variables.longValue((Long) valueObj));
                    } else if (type.contains("Date")) {
                        Date date = getDateObject((String) valueObj);
                        if (date == null) {
                            throw new RuntimeException("Could not parse Date from String: " + (String) valueObj);
                        }
                        variables.putValueTyped(variable.getKey(), Variables.dateValue(date));
                    } else {
                        variables.putValue(variable.getKey(), valueObj);
                    }
                } else {
                    variables.putValue(variable.getKey(), valueObj);
                }
            }
        }
        return variables;
    }

    /**
     * This method is used to parse a decision from an input stream.
     * If the decision to evaluate is not null or empty, it uses the DMN engine to parse the decision from the input stream.
     * Otherwise, it gets the top-level decision from the input stream.
     *
     * @param decisionToEvaluate The decision to be evaluated.
     * @param dmnEngine The DMN engine used to parse the decision.
     * @param inputStream The input stream from which the decision is parsed.
     * @return The parsed decision.
     */
    public DmnDecision parseDecision(String decisionToEvaluate, DmnEngine dmnEngine, InputStream inputStream) {
        DmnDecision decision;
        if (decisionToEvaluate != null && !decisionToEvaluate.trim().equals("")) {
            decision = dmnEngine.parseDecision(decisionToEvaluate, inputStream);
        } else {
            List<DmnDecision> decisions = dmnEngine.parseDecisions(inputStream);
            decision = getRootDecision(decisions);
        }
        return decision;
    }

    /**
     * This method is used to get the evaluated rules from a DMN (Decision Model and Notation) decision logic evaluation event.
     * If the event is a decision table evaluation event, it iterates over each matched rule, creates a JSON node for each rule,
     * adds the rule ID and outputs to the node, and adds the node to a list.
     * The list of rule nodes is then returned.
     *
     * @param rootNode The root node from which the evaluated rules are extracted.
     * @param variables The variables used to evaluate the rules.
     * @return A list of JSON nodes representing the evaluated rules and results.
     */
    public void getEvaluatedRules(SpinJsonNode rootNode, VariableMap variables){
        List<DmnDecisionTableEvaluationEvent> evaluationEvents = evaluationListener.getLastEvents();
        DmnEngine dmnEngine = buildDecisionEngine();
        try{
            for (DmnDecisionTableEvaluationEvent evaluationEvent : evaluationEvents) {
                String currentDecisionKey = evaluationEvent.getDecision().getName();
                List<Object> evaluatedRules = getEvaluatedRules(evaluationEvent);
                if (rootNode.hasProp(currentDecisionKey)) {
                    SpinJsonNode currentDecisionNode = rootNode.prop(currentDecisionKey);
                    currentDecisionNode.prop("rules", evaluatedRules);
                } else {
                    SpinJsonNode currentDecisionNode = JSON("{}");
                    DmnDecisionResult result = dmnEngine.evaluateDecision(evaluationEvent.getDecision(),variables);
                    SpinJsonNode resultValues = JSON(result.getResultList());
                    currentDecisionNode.prop("results", resultValues.toString());
                    currentDecisionNode.prop("rules", evaluatedRules);
                    rootNode.prop(currentDecisionKey, currentDecisionNode);
                }
            }
        }
        catch(Exception e){
            rootNode.prop("error", e.getMessage());
        }
    }

    /**
     * This method is used to convert a string into a Date object.
     * It tries to parse the string using several different date formats.
     * If the string can be successfully parsed into a Date object using one of the formats, the Date object is returned.
     * If none of the formats can successfully parse the string, null is returned.
     *
     * @param dateString The string to be converted into a Date object.
     * @return The Date object resulting from the conversion, or null if the conversion was not successful.
     */
    private Date getDateObject(String dateString) {
        Date date = null;
        // @formatter:off
        String[] formats = new String[] {
                "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
                "yyyy-MM-dd'T'HH:mm:ssZ",
                "yyyy-MM-dd'T'HH:mm:ss.SSS",
                "yyyy-MM-dd'T'HH:mm:ss",
                "yyyy-MM-dd"
        };
        // @formatter:on
        for (String format : formats) {
            DateFormat df = new SimpleDateFormat(format);
            try {
                date = df.parse(dateString);
                break;
            } catch (ParseException e) {
                // ignore and try the next possible format
            }
        }
        return date;
    }

    /**
     * This method is used to get the root decision from a list of decisions.
     * It creates a set of all required decisions, and then removes these from the list of decisions.
     * The remaining decision in the list is the root decision, which is not required by any other decision.
     *
     * @param decisions The list of decisions from which the root decision is extracted.
     * @return The root decision.
     */
    private DmnDecision getRootDecision(List<DmnDecision> decisions) {
        Set<DmnDecision> allRequiredDecisions = new TreeSet<DmnDecision>(new Comparator<DmnDecision>() {
            @Override
            public int compare(DmnDecision dec1, DmnDecision dec2) {
                return dec1.getKey().compareTo(dec2.getKey());
            }
        });
        for (DmnDecision dmnDecision : decisions) {
            Collection<DmnDecision> requiredDecisions = dmnDecision.getRequiredDecisions();
            allRequiredDecisions.addAll(requiredDecisions);
        }
        decisions.removeAll(allRequiredDecisions);
        return decisions.get(0);
    }

    /**
     * This method is used to build the DMN engine.
     * It creates the default configuration for the DMN engine and builds the engine.
     * @return The DMN engine.
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
     * This method is used to get the evaluated rules from a DMN (Decision Model and Notation) decision logic evaluation event.
     * If the event is a decision table evaluation event, it iterates over each matched rule, creates a JSON node for each rule,
     * adds the rule ID and outputs to the node, and adds the node to a list.
     * The list of rule nodes is then returned.
     * @param dmnDecisionLogicEvaluationEvent The DMN decision logic evaluation event from which the evaluated rules are extracted.
     * @return A list of JSON nodes representing the evaluated rules.
     */
    private List getEvaluatedRules(DmnDecisionLogicEvaluationEvent dmnDecisionLogicEvaluationEvent) {
        List rulesList = new LinkedList<SpinJsonNode>();
        if (dmnDecisionLogicEvaluationEvent instanceof DmnDecisionTableEvaluationEvent) {
            DmnDecisionTableEvaluationEvent dmnTableEvent = (DmnDecisionTableEvaluationEvent) dmnDecisionLogicEvaluationEvent;
            for (DmnEvaluatedDecisionRule matchedRule : dmnTableEvent.getMatchingRules()) {
                SpinJsonNode rulesNode = JSON("{}");
                rulesNode.prop("ruleId", matchedRule.getId());
                List outputList = new LinkedList();
                for (DmnEvaluatedOutput output : matchedRule.getOutputEntries().values()) {
                    SpinJsonNode outputProp = JSON("{}");
                    outputProp.prop(output.getId(), output.getValue().getValue().toString());
                    outputList.add(outputProp);
                }
                rulesNode.prop("outputs", outputList);
                rulesList.add(rulesNode);
            }
        }
        return rulesList;
    }

}
