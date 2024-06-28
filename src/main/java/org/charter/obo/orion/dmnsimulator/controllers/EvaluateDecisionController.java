package org.charter.obo.orion.dmnsimulator.controllers;

import static org.camunda.spin.Spin.*;

import java.util.*;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.charter.obo.orion.dmnsimulator.listerners.SimulatorDecisionTableEvaluationListener;
import org.camunda.spin.json.SpinJsonNode;
import org.charter.obo.orion.dmnsimulator.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class is the controller for the Evaluate Decision API.
 */
@RestController
@RequestMapping("/dmnSimulator")
public class EvaluateDecisionController {

  private static Logger log = Logger.getLogger(EvaluateDecisionController.class);

  @Autowired
  private SimulatorDecisionTableEvaluationListener evaluationListener;
  @Autowired
  private EvaluateDecisionService evaluateDecisionService;
  @Autowired
  private InputNamesService inputNamesService;
  @Autowired
  private OutputNamesService outputNamesService;
  @Autowired
  private DecisionNamesService decisionNamesService;

  @Autowired
  private DmnAllDetailsService dmnAllDetailsService;

  /**
   * This method is used to evaluate a decision based on the request body.
   * @param reqBody The request body as a string, which contains the decision to be evaluated.
   * @param resp    The HTTP response.
   * @return The result of the decision evaluation as a JSON string.
   */
  @SuppressWarnings("unchecked")
  @RequestMapping(value = "/evaluateDecision" , method = RequestMethod.POST)
  public String evaluateDecision(@RequestBody String reqBody, HttpServletResponse resp) {

    SpinJsonNode rootNode = evaluateDecisionService.evaluateDecision(reqBody);
    resp.setHeader("Content-Type", "application/json;charset=UTF-8");
    String json = rootNode.toString();
    log.debug("Result: " + json);
    return json;
  }

  /**
   * This method is used to get the input names of a decision based on the request body.
   * It calls the inputNamesService class  which has logic to get the input names.
   * @param reqBody reqBody The request body as a string, which contains the decision to be evaluated.
   * @return
   */
  @SuppressWarnings("unchecked")
  @RequestMapping(value = "/inputNames", method = RequestMethod.POST)
  public  Map<String, Map<String, String>> getInputNames(@RequestBody String reqBody) {
    Map<String, Map<String, String>> inputNames = inputNamesService.getDecisionInputs(reqBody);
    return inputNames;
  }

  /**
   * This method is used to get the output names of a decision based on the request body.
   * It calls the outputNamesService class  which has logic to get the output names.
   * @param reqBody The request body as a string, which contains the decision to be evaluated.
   * @return The output names of the decision as a list of strings.
   */
  @SuppressWarnings("unchecked")
  @RequestMapping(value = "/outputNames", method = RequestMethod.POST)
  public Map<String, String> getOutputNames(@RequestBody String reqBody) {
    Map<String, String> outputNames = outputNamesService.getDecisionOutputs(reqBody);
    return outputNames;
  }

  /**
   * this method is used to get all the details i.e, table names, inputs and output names of a decision based on the request body.
   * It calls the dmnAllDetailsService class  which has logic to get the details of a decision.
   * @param reqBody
   * @return
   */
  @RequestMapping(value = "/allDetails", method = RequestMethod.POST)
  public String getDmnAllDetails(@RequestBody String reqBody) {
    String details = dmnAllDetailsService.getDecisionDetails(reqBody);
    return details;
  }

  /**
   * This method is used to get the decision names based on the request body.
   * It calls the decisionNamesService class  which has logic to get the decision names.
   * @param reqBody The request body as a string, which contains the decision to be evaluated.
   * @return The decision names as a list of strings.
   */
  @SuppressWarnings("unchecked")
  @RequestMapping(value = "/decisionNames", method = RequestMethod.POST)
  public List<String> getDecisionNames(@RequestBody String reqBody) {
    List<String> decisionNames = decisionNamesService.getDecisionNames(reqBody);
    return decisionNames;
  }

}
