package org.jbpm.cmmn.flow.xml;

import org.drools.core.xml.ExtensibleXmlParser;
import org.jbpm.cmmn.flow.planning.ApplicabilityRule;
import org.jbpm.cmmn.flow.planning.DiscretionaryItem;
import org.jbpm.cmmn.flow.planning.PlanningTable;
import org.jbpm.cmmn.flow.planning.impl.DiscretionaryItemImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

public class DiscretionaryItemHandler extends AbstractTableItemHandler  {
	public DiscretionaryItemHandler() {
		super();
		super.validParents.add(null);
		super.validParents.add(PlanningTable.class);
		super.validPeers.add(PlanningTable.class);
		super.validPeers.add(DiscretionaryItem.class);
		super.validPeers.add(ApplicabilityRule.class);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object start(String uri, String localName, Attributes attrs, ExtensibleXmlParser parser) throws SAXException {
		parser.startElementBuilder(localName, attrs);
		DiscretionaryItemImpl item = new DiscretionaryItemImpl();
		item.setDefinitionRef(IdGenerator.toXmlId(attrs.getValue("definitionRef")));
		populateCommonItems(attrs, item);
		String entry = attrs.getValue("entryCriteriaRefs");
		if (entry != null) {
			for (String string : entry.split("\\ ")) {
				item.putEntryCriterion(string, null);
			}
		}
		String exit = attrs.getValue("exitCriteriaRefs");
		if (exit != null) {
			for (String string : exit.split("\\ ")) {
				item.putExitCriterion(string, null);
			}
		}

		item.setId(IdGenerator.getIdAsUniqueAsUuid(parser, item));
		PlanningTable parent = (PlanningTable) parser.getParent();
		parent.addTableItem(item);
		return item;
	}

	@Override
	public Object end(String uri, String localName, ExtensibleXmlParser parser) throws SAXException {
		parser.endElementBuilder();
		return parser.getCurrent();
	}

	@Override
	public Class<?> generateNodeFor() {
		return DiscretionaryItemImpl.class;
	}

}
