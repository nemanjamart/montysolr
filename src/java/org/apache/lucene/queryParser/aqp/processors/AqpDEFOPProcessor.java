package org.apache.lucene.queryParser.aqp.processors;

import java.util.List;

import org.apache.lucene.messages.MessageImpl;
import org.apache.lucene.queryParser.aqp.AqpQueryParser;
import org.apache.lucene.queryParser.aqp.config.DefaultFieldAttribute;
import org.apache.lucene.queryParser.aqp.nodes.AqpANTLRNode;
import org.apache.lucene.queryParser.aqp.util.AqpUtils;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.config.QueryConfigHandler;
import org.apache.lucene.queryParser.core.messages.QueryParserMessages;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessor;
import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
import org.apache.lucene.queryParser.standard.config.DefaultOperatorAttribute;
import org.apache.lucene.queryParser.standard.config.DefaultOperatorAttribute.Operator;

/**
 * Finds the {@link AqpANTLRNode} with tokenLabel <pre>DEFOP</pre> and 
 * sets their @{code tokenInput} to be the name of the default
 * operator.
 * 
 * @see DefaultOperatorAttribute
 * @see AqpQueryParser#setDefaultOperator(org.apache.lucene.queryParser.standard.config.DefaultOperatorAttribute.Operator)
 * 
 */
public class AqpDEFOPProcessor extends QueryNodeProcessorImpl implements
		QueryNodeProcessor {

	@Override
	protected QueryNode preProcessNode(QueryNode node)
			throws QueryNodeException {
		
		if (node instanceof AqpANTLRNode && ((AqpANTLRNode) node).getTokenLabel().equals("DEFOP")) {
			AqpANTLRNode n = (AqpANTLRNode) node;
			Operator op = getDefaultOperator();
			// TODO: we can allow many more operators as default operators (but will have to use our
			// own enum class to harvest them)
			n.setTokenLabel(op==Operator.AND ? "AND" : "OR");
			
			List<QueryNode> children = node.getChildren();
			if (children!=null && children.size()==1) {
				AqpANTLRNode child = (AqpANTLRNode) children.get(0);
				if (child.getTokenName().equals("OPERATOR")
						|| child.getTokenLabel().equals("CLAUSE")
						|| child.getTokenLabel().equals("ATOM")) {
					return child;
				}
			}
			else if(children!=null && children.size() > 1) {
				
				String thisOp = n.getTokenLabel();
				String last = ((AqpANTLRNode)children.get(0)).getTokenLabel();
				boolean rewriteSafe = true;
				
				
				for (int i=1;i<children.size();i++) {
					AqpANTLRNode t = (AqpANTLRNode) children.get(i);
					String tt = t.getTokenLabel();
					if (!(tt.equals(last) && t.getTokenLabel().equals(thisOp))) {
						rewriteSafe = false;
						break;
					}
				}
				
				if (rewriteSafe==true) {
					QueryNode firstChild = children.get(0);
					List<QueryNode> childrenList = firstChild.getChildren();
					
					for (int i=1;i<children.size();i++) {
						QueryNode otherChild = children.get(i);
						for (QueryNode nod: otherChild.getChildren()) {
							childrenList.add(nod);
						}
					}
					
					children.clear();
					n.set(childrenList);
				}
			}
		}
		return node;
	}

	@Override
	protected QueryNode postProcessNode(QueryNode node)
			throws QueryNodeException {
		return node;
	}

	@Override
	protected List<QueryNode> setChildrenOrder(List<QueryNode> children)
			throws QueryNodeException {
		return children;
	}
	
	private Operator getDefaultOperator() throws QueryNodeException {
		QueryConfigHandler queryConfig = getQueryConfigHandler();
		
		if (queryConfig != null) {
			
			if (queryConfig.hasAttribute(DefaultOperatorAttribute.class)) {
				return queryConfig.getAttribute(
						DefaultOperatorAttribute.class).getOperator();
			}
		}
		throw new QueryNodeException(new MessageImpl(
                QueryParserMessages.LUCENE_QUERY_CONVERSION_ERROR,
                "Configuration error: " + DefaultFieldAttribute.class.toString() + " is missing"));
	}

}

