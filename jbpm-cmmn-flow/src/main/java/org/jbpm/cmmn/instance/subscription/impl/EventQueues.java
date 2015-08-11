package org.jbpm.cmmn.instance.subscription.impl;

import org.drools.core.command.impl.GenericCommand;
import org.drools.core.command.impl.KnowledgeCommandContext;
import org.drools.core.process.instance.WorkItem;
import org.drools.core.process.instance.WorkItemManager;
import org.drools.core.process.instance.impl.WorkItemImpl;
import org.jbpm.cmmn.flow.common.impl.AbstractStandardEventNode;
import org.jbpm.cmmn.instance.CaseFileItemEvent;
import org.jbpm.cmmn.instance.CaseInstance;
import org.kie.api.command.Command;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.internal.command.Context;
import org.kie.internal.runtime.KnowledgeContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class EventQueues {
    private static ThreadLocal<Set<CaseFileItemEventWrapper>> eventQueue = new ThreadLocal<Set<CaseFileItemEventWrapper>>();
    private static ThreadLocal<Set<WorkItem>> workItemQueue = new ThreadLocal<Set<WorkItem>>();
    static Logger logger = LoggerFactory.getLogger(EventQueues.class);

    private static Set<CaseFileItemEventWrapper> getEventQueue() {
        Set<CaseFileItemEventWrapper> set = eventQueue.get();
        if (set == null) {
            eventQueue.set(set = new HashSet<CaseFileItemEventWrapper>());
        }
        return set;
    }

    private static Set<WorkItem> getWorkItemQueue() {
        Set<WorkItem> set = workItemQueue.get();
        if (set == null) {
            workItemQueue.set(set = new HashSet<WorkItem>());
        }
        return set;
    }

    public static boolean dispatchWorkItemQueue(RuntimeEngine re) {
        Set<WorkItem> workItemQueue2 = getWorkItemQueue();
        workItemQueue.set(null);
        for (final WorkItem workItem : workItemQueue2) {
            try {
                CaseInstance ci = (CaseInstance) re.getKieSession().getProcessInstance(workItem.getProcessInstanceId());
                if (ci != null) {
                    String deploymentId = (String) re.getKieSession().getEnvironment().get("deploymentId");
                    workItem.setDeploymentId(deploymentId);
                    re.getKieSession().execute(new GenericCommand<Void>() {
                        @Override
                        public Void execute(Context context) {
                            KnowledgeCommandContext kcc= (KnowledgeCommandContext) context;
                            WorkItemManager wim=(WorkItemManager) kcc.getWorkItemManager();
                            wim.internalExecuteWorkItem(workItem);
                            return null;
                        }
                    });
                }
            } catch (Exception e) {
                logger.error("Error occurred dispatching workitem", e);
            }
        }
        return workItemQueue2.size() > 0;
    }

    public static boolean dispatchCaseFileItemEventQueue(RuntimeEngine engine) {
        Set<CaseFileItemEventWrapper> eq = getEventQueue();
        eventQueue.set(new HashSet<CaseFileItemEventWrapper>());
        if (eq.size() > 0) {
            try {
                for (CaseFileItemEventWrapper w : eq) {
                    CaseFileItemEvent event = w.getEvent();
                    String eventType = AbstractStandardEventNode.getType(event.getCaseFileItemName(), event.getTransition());
                    engine.getKieSession().signalEvent(eventType, event, w.getProcessInstanceId());
                }
            } catch (Exception e) {
                logger.error("Could not dispatch CaseFileItemEvents", e);
            }
            return true;
        } else {
            return false;
        }
    }

    public static void queueWorkItem(WorkItemImpl wi) {
        Set<WorkItem> set = getWorkItemQueue();
        set.add(wi);
    }

    public static void queueEvent(String caseKey, long processId, CaseFileItemEvent caseFileItemEvent) {
        // TODO use deploymentId here
        Set<CaseFileItemEventWrapper> set = getEventQueue();
        set.add(new CaseFileItemEventWrapper(caseFileItemEvent, caseKey, processId));
    }

}