package org.jbpm.cmmn.test;

import bitronix.tm.jndi.BitronixContext;
import bitronix.tm.resource.jdbc.PoolingDataSource;
import org.drools.core.ClockType;
import org.drools.core.audit.event.LogEvent;
import org.drools.core.audit.event.RuleFlowNodeLogEvent;
import org.drools.core.marshalling.impl.ClassObjectMarshallingStrategyAcceptor;
import org.drools.core.marshalling.impl.SerializablePlaceholderResolverStrategy;
import org.jbpm.cmmn.casefile.common.CaseFilePersistence;
import org.jbpm.cmmn.casefile.jpa.HibernateSubscriptionManager;
import org.jbpm.cmmn.casefile.jpa.JpaCaseFilePersistence;
import org.jbpm.cmmn.casefile.jpa.JpaCollectionPlaceHolderResolverStrategy;
import org.jbpm.cmmn.casefile.jpa.JpaPlaceHolderResolverStrategy;
import org.jbpm.cmmn.flow.common.impl.PlanItemInstanceFactoryNodeImpl;
import org.jbpm.cmmn.flow.xml.CMMNBuilder;
import org.jbpm.cmmn.instance.CaseInstance;
import org.jbpm.cmmn.instance.PlanElementState;
import org.jbpm.cmmn.instance.PlanItemInstance;
import org.jbpm.cmmn.instance.impl.*;
import org.jbpm.cmmn.instance.subscription.SubscriptionManager;
import org.jbpm.cmmn.service.api.CMMNService;
import org.jbpm.cmmn.service.api.impl.CMMNServiceImpl;
import org.jbpm.cmmn.task.registration.CaseRegisterableItemsFactory;
import org.jbpm.marshalling.impl.ProcessInstanceResolverStrategy;
import org.jbpm.process.instance.event.DefaultSignalManagerFactory;
import org.jbpm.process.instance.impl.DefaultProcessInstanceManagerFactory;
import org.jbpm.runtime.manager.impl.factory.LocalTaskServiceFactory;
import org.jbpm.services.task.identity.JBossUserGroupCallbackImpl;
import org.jbpm.services.task.impl.model.GroupImpl;
import org.jbpm.services.task.impl.model.UserImpl;
import org.jbpm.test.JbpmJUnitBaseTestCase;
import org.jbpm.workflow.instance.NodeInstanceContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.kie.api.event.process.ProcessEventListener;
import org.kie.api.event.rule.AgendaEventListener;
import org.kie.api.io.ResourceType;
import org.kie.api.marshalling.ObjectMarshallingStrategy;
import org.kie.api.runtime.Environment;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.conf.ClockTypeOption;
import org.kie.api.runtime.manager.Context;
import org.kie.api.runtime.manager.RuntimeEngine;
import org.kie.api.runtime.manager.RuntimeEnvironmentBuilder;
import org.kie.api.runtime.manager.RuntimeManager;
import org.kie.api.runtime.manager.audit.NodeInstanceLog;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.task.TaskLifeCycleEventListener;
import org.kie.api.task.TaskService;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.io.ResourceFactory;
import org.kie.internal.task.api.ContentMarshallerContext;
import org.kie.internal.task.api.InternalTaskService;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.transaction.UserTransaction;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.*;

import static org.kie.api.runtime.EnvironmentName.OBJECT_MARSHALLING_STRATEGIES;

public abstract class AbstractCmmnCaseTestCase extends JbpmJUnitBaseTestCase {
	static {
		System.setProperty(InitialContext.INITIAL_CONTEXT_FACTORY, bitronix.tm.jndi.BitronixInitialContextFactory.class.getName());
		System.setProperty(InitialContext.URL_PKG_PREFIXES, "bitronix.tm.jndi");
	}
	protected CaseFilePersistence persistence;
	protected boolean isJpa = false;
	private RuntimeEngine runtimeEngine;
	private UserTransaction transaction;
	private RuntimeManager runtimeManager;
	private CMMNService cmmnService;
	private static EntityManagerFactory emf;
	private static PoolingDataSource ds;
	private String persistenceUnitName;
	protected Stopwatch stopwatch = new Stopwatch(getClass());

	protected EntityManagerFactory getEmf() {
		return emf;
	}

	public AbstractCmmnCaseTestCase() {
		super();
	}

	protected RuntimeManager getRuntimeManager() {
		return runtimeManager;
	}

	public AbstractCmmnCaseTestCase(boolean setupDataSource, boolean sessionPersistence) {
		super(setupDataSource, sessionPersistence);
	}

	public CMMNService getCmmnService() {
		return cmmnService;
	}

	@Before
	public void setUp() throws Exception {

		stopwatch.start();
		if (setupDataSource && (ds == null || emf == null)) {
			ds = setupPoolingDataSource();
			stopwatch.lap("setupPoolingDataSource");
			emf = Persistence.createEntityManagerFactory(persistenceUnitName);
			stopwatch.lap("createEntityManagerFactory");
		}
		cleanupSingletonSessionId();
		stopwatch.lap("cleanupSingletonSessionId");
	}

	public AbstractCmmnCaseTestCase(boolean setupDataSource, boolean sessionPersistence, String persistenceUnitName) {
		super(setupDataSource, sessionPersistence, persistenceUnitName);
		this.persistenceUnitName = persistenceUnitName;
	}

	@Override
	public void assertNodeActive(long processInstanceId, KieSession ksession, String... name) {
		super.assertNodeActive(processInstanceId, ksession, name);
	}

	protected CaseInstance reloadCaseInstance(CaseInstance caseInstance2) {
		return (CaseInstance) getRuntimeEngine().getKieSession().getProcessInstance(caseInstance2.getId());
	}

	protected void assertNodeNotTriggered(long processInstanceId, String... nodeNames) {
		getPersistence().start();
		List<String> names = removeNodesTriggered(processInstanceId, nodeNames);
		if (names.isEmpty()) {
			String s = names.get(0);
			for (int i = 1; i < names.size(); i++) {
				s += ", " + names.get(i);
			}
			fail("Node(s) executed: " + s);
		}
		getPersistence().commit();
	}

	protected InternalTaskService getTaskService() {
		return (InternalTaskService) getRuntimeEngine().getTaskService();
	}

	@Override
	public void assertNodeTriggered(long processInstanceId, String... nodeNames) {
		getPersistence().start();
		List<String> names = removeNodesTriggered(processInstanceId, nodeNames);
		if (!names.isEmpty()) {
			String s = names.get(0);
			for (int i = 1; i < names.size(); i++) {
				s += ", " + names.get(i);
			}
			fail("Node(s) not executed: " + s);
		}
		getPersistence().commit();
	}

	private List<String> removeNodesTriggered(long processInstanceId, String... nodeNames) {
		List<String> names = new ArrayList<String>();
		for (String nodeName : nodeNames) {
			names.add(nodeName);
		}
		if (sessionPersistence) {
			List<? extends NodeInstanceLog> logs = getLogService().findNodeInstances(processInstanceId);
			if (logs != null) {
				for (NodeInstanceLog l : logs) {
					String nodeName = l.getNodeName();
					if ((l.getType() == NodeInstanceLog.TYPE_ENTER || l.getType() == NodeInstanceLog.TYPE_EXIT) && names.contains(nodeName)) {
						names.remove(nodeName);
					}
				}
			}
		} else {
			for (LogEvent event : getInMemoryLogger().getLogEvents()) {
				if (event instanceof RuleFlowNodeLogEvent) {
					String nodeName = ((RuleFlowNodeLogEvent) event).getNodeName();
					if (names.contains(nodeName)) {
						names.remove(nodeName);
					}
				}
			}
		}
		return names;
	}

	class StateResult {
		int count = 0;
		String foundState = "";
	}

	public void assertPlanItemInState(long processInstanceId, String planItemName, PlanElementState s, int... numberOfTimes) {
		getPersistence().start();
		NodeInstanceContainer ci = (NodeInstanceContainer) getRuntimeEngine().getKieSession().getProcessInstance(processInstanceId);

		StateResult sr = new StateResult();
		countItemInState(planItemName, s, ci, sr);
		getPersistence().commit();
		if (numberOfTimes.length == 0) {
			if (sr.count == 0) {
				assertTrue(planItemName + " should be in state " + s.name() + " but was in " + sr.foundState, sr.count > 0);
			}
		} else {
			assertEquals(planItemName + " should be in state " + s.name() + "  " + numberOfTimes[0] + " times, but was foudn in state "
					+ sr.count + " times", numberOfTimes[0], sr.count);
		}
	}

	public void countItemInState(String planItemName, PlanElementState s, NodeInstanceContainer ci, StateResult sr) {
		for (NodeInstance ni : ci.getNodeInstances()) {
			if (ni instanceof PlanItemInstanceFactoryNodeInstance) {
				PlanItemInstanceFactoryNodeImpl node = (PlanItemInstanceFactoryNodeImpl) ni.getNode();
				if (node.getItemToInstantiate().getName().equals(planItemName)) {
					PlanItemInstanceFactoryNodeInstance<?> piil = (PlanItemInstanceFactoryNodeInstance<?>) ni;
					if (piil.isPlanItemInstanceStillRequired() && s == PlanElementState.AVAILABLE) {
						sr.count++;
					} else if (piil.getPlanElementState() == s) {
						sr.count++;
					} else {
						sr.foundState = piil.getPlanElementState().name();
					}
				}
			} else if (ni instanceof StageInstance) {
				countItemInState(planItemName, s, (StageInstance) ni, sr);
			}
		}
		if (sr.count == 0) {
			for (NodeInstance ni : ci.getNodeInstances()) {
				if (ni instanceof PlanItemInstance && ni.getNodeName().equals(planItemName)) {
					if (((PlanItemInstance<?>) ni).getPlanElementState() == s) {
						sr.count++;
					} else {
						sr.foundState = ((PlanItemInstance<?>) ni).getPlanElementState().name();
					}
				} else if (ni instanceof StageInstance) {
					countItemInState(planItemName, s, (StageInstance) ni, sr);
				}

			}
		}
	}

	public UserTransaction getTransaction() throws NamingException {
		if (transaction == null) {
			transaction = (UserTransaction) new BitronixContext().lookup("java:comp/UserTransaction");
		}
		return transaction;
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
		if (emf != null) {
			emf.close();
			emf = null;
		}
		if (ds != null) {
			ds.close();
			ds = null;
		}
	}

	@After
	public void tearDown() throws Exception {
		stopwatch.start();
		try {
			getTransaction().rollback();
		} catch (Exception e) {
		}
		Connection c = ds.getConnection();
		try {
			c.createStatement().execute("SET REFERENTIAL_INTEGRITY FALSE");
			ResultSet rst = c.createStatement().executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = SCHEMA()");
			while (rst.next()) {
				c.createStatement().execute("TRUNCATE TABLE " + rst.getString(1));
			}
			c.createStatement().execute("SET REFERENTIAL_INTEGRITY TRUE");
		} catch (Exception e) {

		} finally {
			c.close();
		}
		transaction = null;
		if (isJpa) {
			getPersistence().close();
		}
		clearHistory();
		disposeRuntimeManager();
		runtimeEngine = null;

		persistence = null;

		stopwatch.lap("tearDown");
	}

	@Override
	protected RuntimeEngine getRuntimeEngine(Context<?> context) {
		return super.getRuntimeEngine(context);
	}

	protected void assertTaskTypeCreated(List<TaskSummary> list, String expected, int... numberOfTimes) {
		int count = 0;
		for (TaskSummary taskSummary : list) {
			if (taskSummary.getName().equals(expected)) {
				count++;
			}
		}
		if (numberOfTimes.length == 1) {
			assertEquals("Task not created the correct number of times", numberOfTimes[0], count);
		} else if (count == 0) {
			fail("Task not created: " + expected);
		}
	}

	public CaseFilePersistence getPersistence() {
		try {
			if (persistence == null) {
				persistence = new JpaCaseFilePersistence(emf, getRuntimeManager());
			}
			return persistence;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected PoolingDataSource setupPoolingDataSource() {
		PoolingDataSource pds = new PoolingDataSource();
		if (isJpa) {
			// fake XA
			pds.setUniqueName("jdbc/jbpm-ds");
			pds.setClassName("bitronix.tm.resource.jdbc.lrc.LrcXADataSource");
			pds.setMaxPoolSize(5);
			pds.setAllowLocalTransactions(true);
			pds.setIgnoreRecoveryFailures(true);
			pds.getDriverProperties().put("user", "sa");
			pds.getDriverProperties().put("password", "");
			pds.getDriverProperties().put("url", "jdbc:h2:mem:jbpm-db;MVCC=true");
			pds.getDriverProperties().put("driverClassName", "org.h2.Driver");
		} else {
			pds.setClassName("org.h2.jdbcx.JdbcDataSource");
			pds.setUniqueName("jdbc/jbpm-ds");
			pds.setMaxPoolSize(5);
			pds.setAllowLocalTransactions(true);
			pds.getDriverProperties().put("user", "sa");
			pds.setApplyTransactionTimeout(false);
			pds.setIgnoreRecoveryFailures(true);
			pds.getDriverProperties().put("password", "");
			pds.getDriverProperties().put("URL", "jdbc:h2:mem:jbpm-db;MVCC=true");
		}
		pds.init();
		return pds;
	}

	@Override
	protected RuntimeEngine getRuntimeEngine() {
		if (this.runtimeEngine == null) {
			this.runtimeEngine = super.getRuntimeEngine();
		}
		return this.runtimeEngine;
	}

	@Override
	protected RuntimeManager createRuntimeManager(Strategy strategy, String identifier, String... process) {
		Map<String, ResourceType> resources = new HashMap<String, ResourceType>();
		for (String p : process) {
			if (p.endsWith(".cmmn")) {
				resources.put(p, CMMNBuilder.CMMN_RESOURCE_TYPE);
			} else if (p.endsWith(".bpmn")) {
				resources.put(p, ResourceType.BPMN2);
			}
		}
		return createRuntimeManager(strategy, resources, identifier);
	}


    protected RuntimeManager createRuntimeManager(Strategy strategy, Map<String, ResourceType> resources, String identifier) {
        if (manager != null) {
            throw new IllegalStateException("There is already one RuntimeManager active");
        }
        
        RuntimeEnvironmentBuilder builder = null;
        if (!setupDataSource){
            builder = RuntimeEnvironmentBuilder.Factory.get()
        			.newEmptyBuilder()
            .addConfiguration("drools.processSignalManagerFactory", DefaultSignalManagerFactory.class.getName())
            .addConfiguration("drools.processInstanceManagerFactory", DefaultProcessInstanceManagerFactory.class.getName())
            .registerableItemsFactory(new MyCaseRegisterableItemsFactory());
            
        } else if (sessionPersistence) {
            builder = RuntimeEnvironmentBuilder.Factory.get()
        			.newDefaultBuilder()
            .entityManagerFactory(emf)
            .registerableItemsFactory(new MyCaseRegisterableItemsFactory() );
        } else {
            builder = RuntimeEnvironmentBuilder.Factory.get()
        			.newDefaultInMemoryBuilder()
        			.registerableItemsFactory(new MyCaseRegisterableItemsFactory());
        }
		builder.addConfiguration(ClockTypeOption.PROPERTY_NAME, ClockType.PSEUDO_CLOCK.getId());
        builder.userGroupCallback(new JBossUserGroupCallbackImpl("classpath:/usergroups.properties"));
        
        for (Map.Entry<String, ResourceType> entry : resources.entrySet()) {            
            builder.addAsset(ResourceFactory.newClassPathResource(entry.getKey()), entry.getValue());
        }
        
        return createRuntimeManager(strategy, resources, builder.get(), identifier);
    }

	@Override
	protected RuntimeManager createRuntimeManager(String... processFile) {
		writeProcessFiles(processFile);
		RuntimeManager rm = super.createRuntimeManager(processFile);
		this.runtimeManager = rm;
		RuntimeEngine runtimeEngine = getRuntimeEngine();
//		fixPersistenceStrategy(runtimeEngine);
		Environment env = runtimeEngine.getKieSession().getEnvironment();
		prepareEnvironment(env);
		TaskService ts = runtimeEngine.getTaskService();
		if (ts instanceof InternalTaskService) {
			InternalTaskService its = (InternalTaskService) ts;

			its.addMarshallerContext(rm.getIdentifier(), new ContentMarshallerContext(env, getClass().getClassLoader()));
			// its.setUserInfo(new PropertyUserInfoImpl(new Properties()));
		}
		this.cmmnService=new CMMNServiceImpl(runtimeEngine);
		// for some reason the task service does not persist the users and
		// groups ???
		populateUsers();
		return rm;
	}

	private void writeProcessFiles(String... processFile) throws TransformerFactoryConfigurationError {
		try {
			for (String string : processFile) {
				TransformerFactory transformerFactory = TransformerFactory.newInstance();
				Transformer transformer = transformerFactory.newTransformer();
				DOMSource source = new DOMSource(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File("./src/test/resources/" + string)));
				File outputFile = new File("./target/test-classes/" + string);
				outputFile.getParentFile().mkdirs();
				StreamResult result = new StreamResult(outputFile);
				transformer.transform(source, result);
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	protected void prepareEnvironment(Environment env) {
		env.set(OBJECT_MARSHALLING_STRATEGIES, getPlaceholdStrategies(env));
		SubscriptionManager<?> subscriptionManager = getSubscriptionManager();
		if (subscriptionManager != null) {
			env.set(SubscriptionManager.ENV_NAME, subscriptionManager);
		}
		if (isJpa) {
			env.set(JpaCaseFilePersistence.ENV_NAME, getPersistence());
		}
		env.set("org.kie.internal.runtime.manager.TaskServiceFactory", LocalTaskServiceFactory.class.getName());
	}

	protected void populateUsers() {
		JBossUserGroupCallbackImpl users = new JBossUserGroupCallbackImpl("classpath:/usergroups.properties");
		Properties props = buildUserGroupProperties();
		for (Object userId : props.keySet()) {
			getPersistence().start();
			EntityManager em = emf.createEntityManager();
			GroupImpl group = em.find(GroupImpl.class, userId);
			if (group == null) {
				UserImpl builder = em.find(UserImpl.class, userId);
				if (builder == null) {
					em.persist(new UserImpl((String) userId));
					em.flush();
				}
				for (String g : users.getGroupsForUser((String) userId, null, null)) {
					group = em.find(GroupImpl.class, g);
					if (group == null) {
						em.persist(new GroupImpl(g));
						em.flush();
					}
				}
			}
			getPersistence().commit();
		}
	}

	private Properties buildUserGroupProperties() {
		Properties props = new Properties();
		try {
			props.load(getClass().getClassLoader().getResourceAsStream("usergroups.properties"));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return props;
	}

	protected ObjectMarshallingStrategy[] getPlaceholdStrategies(Environment env) {
		if (isJpa) {
			return new ObjectMarshallingStrategy[] { new ProcessInstanceResolverStrategy(), new JpaPlaceHolderResolverStrategy(env),
					new JpaCollectionPlaceHolderResolverStrategy(env),
					new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT) };
		} else {
			return new ObjectMarshallingStrategy[] { new ProcessInstanceResolverStrategy(),
					new JpaPlaceHolderResolverStrategy(env),
					new JpaCollectionPlaceHolderResolverStrategy(env),
					new SerializablePlaceholderResolverStrategy(ClassObjectMarshallingStrategyAcceptor.DEFAULT) };

		}
	}

	protected SubscriptionManager<?> getSubscriptionManager() {
		if (isJpa) {
			return new HibernateSubscriptionManager((JpaCaseFilePersistence) getPersistence());
		}
		return null;
	}




	protected void clearHistory() {
		if (sessionPersistence && getRuntimeManager() != null && getRuntimeEngine() != null && getRuntimeEngine().getAuditService() != null) {
			getRuntimeEngine().getAuditService().clear();
		}
	}

	@SuppressWarnings("rawtypes")
	protected abstract Class[] getClasses();

	private class MyCaseRegisterableItemsFactory extends CaseRegisterableItemsFactory {

		@Override
        public Map<String, WorkItemHandler> getWorkItemHandlers(RuntimeEngine runtime) {
            Map<String, WorkItemHandler> handlers = new HashMap<String, WorkItemHandler>();
            handlers.putAll(super.getWorkItemHandlers(runtime));
            handlers.putAll(customHandlers);
            return handlers;
        }

		@Override
        public List<ProcessEventListener> getProcessEventListeners(RuntimeEngine runtime) {
            List<ProcessEventListener> listeners = super.getProcessEventListeners(runtime);
            listeners.addAll(customProcessListeners);
            return listeners;
        }

		@Override
        public List<AgendaEventListener> getAgendaEventListeners( RuntimeEngine runtime) {
            List<AgendaEventListener> listeners = super.getAgendaEventListeners(runtime);
            listeners.addAll(customAgendaListeners);
            return listeners;
        }

		@Override
        public List<TaskLifeCycleEventListener> getTaskListeners() {
            List<TaskLifeCycleEventListener> listeners = super.getTaskListeners();
            listeners.addAll(customTaskListeners);
            return listeners;
        }

	}
}
