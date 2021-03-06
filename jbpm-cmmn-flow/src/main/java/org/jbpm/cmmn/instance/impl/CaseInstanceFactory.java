/**
 * Copyright 2010 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jbpm.cmmn.instance.impl;

import org.drools.core.common.InternalKnowledgeRuntime;
import org.drools.core.process.instance.WorkItem;
import org.jbpm.cmmn.common.WorkItemParameters;
import org.jbpm.cmmn.flow.core.CaseParameter;
import org.jbpm.cmmn.flow.core.impl.CaseImpl;
import org.jbpm.process.core.ContextContainer;
import org.jbpm.process.core.context.variable.VariableScope;
import org.jbpm.process.instance.AbstractProcessInstanceFactory;
import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.ProcessInstance;
import org.jbpm.process.instance.context.variable.VariableScopeInstance;
import org.kie.api.definition.process.Process;
import org.kie.internal.process.CorrelationKey;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CaseInstanceFactory extends AbstractProcessInstanceFactory implements Externalizable {
	private static final long serialVersionUID = 510L;

	@Override
	public ProcessInstance createProcessInstance() {
		return new CaseInstanceImpl();
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
	}

	@Override
	public ProcessInstance createProcessInstance(Process process, CorrelationKey correlationKey, InternalKnowledgeRuntime kruntime,
			Map<String, Object> parameters) {
		CaseInstanceImpl processInstance = (CaseInstanceImpl) createProcessInstance();
		processInstance.setKnowledgeRuntime(kruntime);
		processInstance.setProcess(process);
		CaseImpl theCase = (CaseImpl) process;
		((InternalProcessRuntime) kruntime.getProcessRuntime()).getProcessInstanceManager().addProcessInstance(processInstance, correlationKey);
		// set variable default values
		VariableScope variableScope = (VariableScope) ((ContextContainer) process).getDefaultContext(VariableScope.VARIABLE_SCOPE);
		VariableScopeInstance variableScopeInstance = (VariableScopeInstance) processInstance.getContextInstance(VariableScope.VARIABLE_SCOPE);
		// set input parameters
		if (parameters != null) {
			if (variableScope != null) {
				Collection<CaseParameter> inputParameters = theCase.getInputParameters();
				for (CaseParameter caseParameter : inputParameters) {
					if(caseParameter.getBoundVariable()!=null) {
						Object var = parameters.get(caseParameter.getName());
						if (caseParameter.getBoundVariable().isCollection() && !(var instanceof Collection)) {
							Set<Object> newVal = new HashSet<Object>();
							if (var != null) {
								newVal.add(var);
							}
							var = newVal;
						}
						variableScopeInstance.setVariable(caseParameter.getBoundVariable().getName(), var);
					}
				}
			} else {
				throw new IllegalArgumentException("This process does not support parameters!");
			}
		}
		WorkItem workItem = (WorkItem) parameters.get(CaseImpl.WORK_ITEM);
		if (workItem != null) {
			processInstance.setVariable("workItemId",workItem.getId());
		}
		String initiator = (String) parameters.get(WorkItemParameters.INITIATOR);
		if (initiator != null) {
			variableScopeInstance.setVariable(WorkItemParameters.INITIATOR, initiator);
		}
		String caseOwner = (String) parameters.get(WorkItemParameters.CASE_OWNER);
		if (caseOwner != null) {
			variableScopeInstance.setVariable(WorkItemParameters.CASE_OWNER, caseOwner);
		}
		if (caseOwner == null && initiator == null) {
			throw new IllegalArgumentException("A case must either have an owner, or initiator, or both");
		}

		return processInstance;
	}

}
