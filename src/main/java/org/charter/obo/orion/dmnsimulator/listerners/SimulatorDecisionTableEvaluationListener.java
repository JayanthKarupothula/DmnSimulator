/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.charter.obo.orion.dmnsimulator.listerners;

import java.util.ArrayList;
import java.util.List;

import org.camunda.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationEvent;
import org.camunda.bpm.dmn.engine.delegate.DmnDecisionTableEvaluationListener;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * This class listens to DMN decision table evaluation events and stores them for later retrieval.
 * It is annotated with @Component to indicate that it is a Spring Bean and with @RequestScope to specify that a new instance should be created for each HTTP request.
 */
@Component
@RequestScope
public class SimulatorDecisionTableEvaluationListener implements DmnDecisionTableEvaluationListener {

  // The last decision table evaluation event that occurred
  protected DmnDecisionTableEvaluationEvent lastEvent;

  // A list of all decision table evaluation events that occurred
  protected List<DmnDecisionTableEvaluationEvent> lastEvents = new ArrayList<DmnDecisionTableEvaluationEvent>();

  /**
   * This method is called when a decision table evaluation event occurs.
   * It stores the event in the lastEvent field and adds it to the lastEvents list.
   *
   * @param dmnDecisionTableEvaluationEvent The decision table evaluation event.
   */
  public void notify(DmnDecisionTableEvaluationEvent dmnDecisionTableEvaluationEvent) {
    lastEvent = dmnDecisionTableEvaluationEvent;
    lastEvents.add(dmnDecisionTableEvaluationEvent);
  }

  /**
   * This method returns the last decision table evaluation event that occurred.
   *
   * @return The last decision table evaluation event.
   */
  public DmnDecisionTableEvaluationEvent getLastEvent() {
    return lastEvent;
  }

  /**
   * This method returns a list of all decision table evaluation events that occurred.
   *
   * @return A list of all decision table evaluation events.
   */
  public List<DmnDecisionTableEvaluationEvent> getLastEvents() {
    return new ArrayList<>(lastEvents);
  }

}
