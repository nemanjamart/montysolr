package org.apache.lucene.queryParser.aqp.parser;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.apache.lucene.messages.Message;
import org.apache.lucene.messages.MessageImpl;
import org.apache.lucene.queryParser.core.QueryNodeException;
import org.apache.lucene.queryParser.core.QueryNodeParseException;
import org.apache.lucene.queryParser.core.messages.QueryParserMessages;
import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.parser.SyntaxParser;

import org.apache.lucene.queryParser.aqp.parser.StandardLuceneGrammarLexer;
import org.apache.lucene.queryParser.aqp.parser.StandardLuceneGrammarParser;
import org.apache.lucene.queryParser.aqp.processors.ASTConvertProcessor;

public class ANTLRSyntaxParser implements SyntaxParser {

	@Override
	public QueryNode parse(CharSequence query, CharSequence field)
			throws QueryNodeParseException {

		ANTLRStringStream in = new ANTLRStringStream(query.toString());
		StandardLuceneGrammarLexer lexer = new StandardLuceneGrammarLexer(in);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		StandardLuceneGrammarParser parser = new StandardLuceneGrammarParser(
				tokens);
		StandardLuceneGrammarParser.mainQ_return returnValue;
		try {
			returnValue = parser.mainQ();
		} catch (RecognitionException e) {
			throw new QueryNodeParseException(new MessageImpl(e.getMessage()));
		} catch (Error e) {
			Message message = new MessageImpl(
					QueryParserMessages.INVALID_SYNTAX_CANNOT_PARSE, query,
					e.getMessage());
			QueryNodeParseException ee = new QueryNodeParseException(e);
			ee.setQuery(query);
			ee.setNonLocalizedMessage(message);
			throw ee;
		}

		// GET the AST tree
		CommonTree astTree = (CommonTree) returnValue.getTree();


		// convert it to QueryNodes
		ASTConvertProcessor convertor = new ASTConvertProcessor();
		return convertor.processAST(astTree);
		/*
		try {
			return convertor.processAST(astTree);
		} catch (QueryNodeException e) {
			// TODO Auto-generated catch block
			throw new QueryNodeParseException(new MessageImpl("Error converting AST query tree", e.getMessage()));
		}
		*/
	}

}