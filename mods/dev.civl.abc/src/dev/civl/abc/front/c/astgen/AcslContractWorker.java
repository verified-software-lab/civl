package dev.civl.abc.front.c.astgen;

import static dev.civl.abc.front.IF.CivlcTokenConstant.EXPR;
import static dev.civl.abc.front.IF.CivlcTokenConstant.TYPE;
import static dev.civl.abc.front.c.parse.AcslParser.ABSENT;
import static dev.civl.abc.front.c.parse.AcslParser.ABSTRACT_DECLARATOR;
import static dev.civl.abc.front.c.parse.AcslParser.ACCESS_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.AMPERSAND;
import static dev.civl.abc.front.c.parse.AcslParser.AND;
import static dev.civl.abc.front.c.parse.AcslParser.ANYACT;
import static dev.civl.abc.front.c.parse.AcslParser.ARRAY_SUFFIX;
import static dev.civl.abc.front.c.parse.AcslParser.ARROW;
import static dev.civl.abc.front.c.parse.AcslParser.ASSIGN;
import static dev.civl.abc.front.c.parse.AcslParser.BEQUIV_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.BIMPLIES_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.BITOR;
import static dev.civl.abc.front.c.parse.AcslParser.BITXOR;
import static dev.civl.abc.front.c.parse.AcslParser.BOOLEAN;
import static dev.civl.abc.front.c.parse.AcslParser.CALL_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.CAST;
import static dev.civl.abc.front.c.parse.AcslParser.CHARACTER_CONSTANT;
import static dev.civl.abc.front.c.parse.AcslParser.COMMA;
import static dev.civl.abc.front.c.parse.AcslParser.C_TYPE;
import static dev.civl.abc.front.c.parse.AcslParser.DIV;
import static dev.civl.abc.front.c.parse.AcslParser.DOT;
import static dev.civl.abc.front.c.parse.AcslParser.DOTDOT;
import static dev.civl.abc.front.c.parse.AcslParser.ELLIPSIS;
import static dev.civl.abc.front.c.parse.AcslParser.EQUALS;
import static dev.civl.abc.front.c.parse.AcslParser.EQUIV_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.EVENT_BASE;
import static dev.civl.abc.front.c.parse.AcslParser.EVENT_INTS;
import static dev.civl.abc.front.c.parse.AcslParser.EVENT_PARENTHESIZED;
import static dev.civl.abc.front.c.parse.AcslParser.EVENT_PLUS;
import static dev.civl.abc.front.c.parse.AcslParser.EVENT_SUB;
import static dev.civl.abc.front.c.parse.AcslParser.FALSE_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.FLOATING_CONSTANT;
import static dev.civl.abc.front.c.parse.AcslParser.FOCUS_ASSERT;
import static dev.civl.abc.front.c.parse.AcslParser.FUNC_CALL;
import static dev.civl.abc.front.c.parse.AcslParser.GT;
import static dev.civl.abc.front.c.parse.AcslParser.GTE;
import static dev.civl.abc.front.c.parse.AcslParser.HASH;
import static dev.civl.abc.front.c.parse.AcslParser.IDENTIFIER;
import static dev.civl.abc.front.c.parse.AcslParser.IMPLIES;
import static dev.civl.abc.front.c.parse.AcslParser.IMPLIES_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.INDEX;
import static dev.civl.abc.front.c.parse.AcslParser.INTEGER;
import static dev.civl.abc.front.c.parse.AcslParser.INTEGER_CONSTANT;
import static dev.civl.abc.front.c.parse.AcslParser.LOGIC_TYPE;
import static dev.civl.abc.front.c.parse.AcslParser.LT;
import static dev.civl.abc.front.c.parse.AcslParser.LTE;
import static dev.civl.abc.front.c.parse.AcslParser.MOD;
import static dev.civl.abc.front.c.parse.AcslParser.NEQ;
import static dev.civl.abc.front.c.parse.AcslParser.NOT;
import static dev.civl.abc.front.c.parse.AcslParser.NOTHING;
import static dev.civl.abc.front.c.parse.AcslParser.OPERATOR;
import static dev.civl.abc.front.c.parse.AcslParser.OR;
import static dev.civl.abc.front.c.parse.AcslParser.PLUS;
import static dev.civl.abc.front.c.parse.AcslParser.QMARK;
import static dev.civl.abc.front.c.parse.AcslParser.QUANTIFIED;
import static dev.civl.abc.front.c.parse.AcslParser.READ_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.REAL_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.RELCHAIN;
import static dev.civl.abc.front.c.parse.AcslParser.RESULT_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.SELF;
import static dev.civl.abc.front.c.parse.AcslParser.SET_BINDERS;
import static dev.civl.abc.front.c.parse.AcslParser.SHIFTLEFT;
import static dev.civl.abc.front.c.parse.AcslParser.SHIFTRIGHT;
import static dev.civl.abc.front.c.parse.AcslParser.SIZEOF;
import static dev.civl.abc.front.c.parse.AcslParser.STAR;
import static dev.civl.abc.front.c.parse.AcslParser.STRING_LITERAL;
import static dev.civl.abc.front.c.parse.AcslParser.SUB;
import static dev.civl.abc.front.c.parse.AcslParser.TERM_PARENTHESIZED;
import static dev.civl.abc.front.c.parse.AcslParser.TILDE;
import static dev.civl.abc.front.c.parse.AcslParser.TRUE_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.TYPE_BUILTIN;
import static dev.civl.abc.front.c.parse.AcslParser.TYPE_ID;
import static dev.civl.abc.front.c.parse.AcslParser.VALID;
import static dev.civl.abc.front.c.parse.AcslParser.VAR_ID;
import static dev.civl.abc.front.c.parse.AcslParser.VAR_ID_BASE;
import static dev.civl.abc.front.c.parse.AcslParser.VAR_ID_SQUARE;
import static dev.civl.abc.front.c.parse.AcslParser.VAR_ID_STAR;
import static dev.civl.abc.front.c.parse.AcslParser.WRITE_ACSL;
import static dev.civl.abc.front.c.parse.AcslParser.XOR_ACSL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.acsl.AllocationNode;
import dev.civl.abc.ast.node.IF.acsl.AssignsOrReadsNode;
import dev.civl.abc.ast.node.IF.acsl.AssumesNode;
import dev.civl.abc.ast.node.IF.acsl.BehaviorNode;
import dev.civl.abc.ast.node.IF.acsl.CompletenessNode;
import dev.civl.abc.ast.node.IF.acsl.CompositeEventNode;
import dev.civl.abc.ast.node.IF.acsl.CompositeEventNode.EventOperator;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.acsl.DependsEventNode;
import dev.civl.abc.ast.node.IF.acsl.DependsNode;
import dev.civl.abc.ast.node.IF.acsl.EnsuresNode;
import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode.ExtendedQuantifier;
import dev.civl.abc.ast.node.IF.acsl.FocusAssertTransformNode;
import dev.civl.abc.ast.node.IF.acsl.FocusLoopTransformNode;
import dev.civl.abc.ast.node.IF.acsl.GuardsNode;
import dev.civl.abc.ast.node.IF.acsl.MemoryEventNode.MemoryEventNodeKind;
import dev.civl.abc.ast.node.IF.acsl.PureNode;
import dev.civl.abc.ast.node.IF.acsl.RequiresNode;
import dev.civl.abc.ast.node.IF.acsl.TransformNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.CharacterConstantNode;
import dev.civl.abc.ast.node.IF.expression.ConstantNode;
import dev.civl.abc.ast.node.IF.expression.EnumerationConstantNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FloatingConstantNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IntegerConstantNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode.Quantifier;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.ast.node.IF.expression.SizeableNode;
import dev.civl.abc.ast.node.IF.expression.SizeofNode;
import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.type.ArrayTypeNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode.TypeNodeKind;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.config.IF.Configuration;
import dev.civl.abc.err.IF.ABCRuntimeException;
import dev.civl.abc.front.c.parse.AcslParser;
import dev.civl.abc.front.c.ptree.CParseTree;
import dev.civl.abc.front.common.astgen.SimpleScope;
import dev.civl.abc.token.IF.CharacterToken;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.CivlcToken.TokenVocabulary;
import dev.civl.abc.token.IF.Formation;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.StringToken;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;

/**
 * This is responsible for translating a CParseTree that represents an ACSL
 * contract specification for a function or a loop into an ordered list of
 * Contract nodes. It serves as a helper for {@link AcslContractHandler}. <br>
 * Precondition: all tokens are preprocessed with the regular program
 * components. <br>
 * Note: there are no separated lexer for the ACSL parser. All keywords are
 * recognized as IDENTIFIER or EXTENDED_IDENTIFIER and semantic predicates are
 * used to match different keywords, like <code>requires</code>,
 * <code>ensures</code>, <code>assumes</code> as IDENTIFIER and
 * <code>\valid</code>, <code>\result</code>, <code>\valid</code> as
 * EXTENDED_IDENTIFIER.
 *
 * @author Manchun Zheng (zmanchun)
 */
public class AcslContractWorker {

	/**
	 * A collection of translated nodes. The collection is the result of any ACSL
	 * contract translation. The collection includes two groups:
	 * <li>a set of {@link ContractNode}s</li>
	 * <li>a set of {@link BlockItemNode}s that can be directly put at the position
	 * of the translated ACSL contract.</li>
	 *
	 * @author ziqingluo
	 */
	public class ACSLSpecTranslation {
		/**
		 * A set of {@link ContractNode}s which are the translation results of the ACSL
		 * contract annotations.
		 */
		final SequenceNode<ContractNode> contractNodes;

		/**
		 * A set of {@link TransformNode}s which are the translation results of a
		 * transform block.
		 */
		final List<TransformNode> transformNodes;
		/**
		 * A set of {@link BlockItemNode}s which are the translation results of some
		 * ACSL contract annotations that can be directly mapped to existing ABC nodes.
		 */
		final List<BlockItemNode> blockItemNodes;

		ACSLSpecTranslation(Source acslSpecSource, List<ContractNode> contractNodes, List<TransformNode> transformNodes,
				List<BlockItemNode> blockItemNodes) {
			assert contractNodes != null;
			assert blockItemNodes != null;
			this.contractNodes = nodeFactory.newSequenceNode(acslSpecSource, "ACSL spec", contractNodes);
			this.transformNodes = transformNodes;
			this.blockItemNodes = blockItemNodes;
		}
	}

	/**
	 * the parse tree to be translated
	 */
	private CParseTree parseTree;

	/**
	 * the node factory to be used for creating AST nodes
	 */
	private NodeFactory nodeFactory;

	/**
	 * the token factory
	 */
	private TokenFactory tokenFactory;

	/**
	 * the formation to be used for transforming ANTLR tokens into CTokens
	 */
	private Formation formation;

	/**
	 * the configuration of this translation task
	 */
	private Configuration config;

	/* ******************** Constants ******************* */

	private final String CIVL_ASSERT = "$assert";

	/**
	 * creates a new instance of AcslContractWorker
	 *
	 * @param factory      the node factory to be used
	 * @param tokenFactory the token factory to be used
	 * @param parseTree    the parse tree to be translated
	 */
	public AcslContractWorker(NodeFactory factory, TokenFactory tokenFactory, CParseTree parseTree,
			Configuration config) {
		this.nodeFactory = factory;
		this.tokenFactory = tokenFactory;
		this.parseTree = parseTree;
		this.config = config;
		formation = tokenFactory.newTransformFormation("ACSL", "contract");
	}

	/**
	 * translates the parse tree to a list of contract nodes. Currently, two kinds
	 * of contracts are supported, one is function contract, the other is loop
	 * annotation.
	 *
	 * @param scope the scope of the contract
	 * @return the list of contract nodes which is the result of translating the
	 *         parse tree
	 * @throws SyntaxException if there are syntax errors during the translation
	 */
	public ACSLSpecTranslation generateContractNodes(SimpleScope scope) throws SyntaxException {
		CommonTree contractTree = parseTree.getRoot();
		List<ContractNode> translatedContractNodes = new LinkedList<>();
		List<TransformNode> translatedTransformNodes = new LinkedList<>();
		List<BlockItemNode> translatedBlockItems = new LinkedList<>();

		if (contractTree.getTokenStartIndex() < 0 && contractTree.getTokenStopIndex() < 0) {
			// empty contract. we have no source. fix to include /*@ */ in tree.
			return new ACSLSpecTranslation(null, translatedContractNodes, translatedTransformNodes,
					translatedBlockItems);
		}
		switch (contractTree.getType()) {
		case AcslParser.FUNC_CONTRACT:
			translatedContractNodes
					.addAll(translateFunctionContractBlock((CommonTree) contractTree.getChild(0), scope));
			if (contractTree.getChildCount() >= 2) {
				CommonTree tree1 = (CommonTree) contractTree.getChild(1);
				int type1 = tree1.getType();
				if (type1 == AcslParser.PURE) {
					PureNode pureNode = nodeFactory.newPureNode(newSource(tree1));
					translatedContractNodes.add(pureNode);
				} else {
					throw error("Unknown kind of function contract child", tree1);
				}
				if (contractTree.getChildCount() >= 3)
					throw error("Extra children in function contract tree", contractTree);
			}
			break;
		case AcslParser.LOOP_CONTRACT:
			for (ContractNode node : translateLoopContractBlock((CommonTree) contractTree.getChild(0), scope)) {
				if (node instanceof TransformNode)
					translatedTransformNodes.add((TransformNode) node);
				else
					translatedContractNodes.add(node);
			}
			break;
		case AcslParser.LOGIC_FUNCTIONS:
			translatedBlockItems.addAll(translateLogicFunctions(contractTree, scope));
			break;
		case AcslParser.ASSERT_ACSL:
			translatedBlockItems.add(translateACSLAssertion(contractTree, scope));
			break;
		case AcslParser.FOCUS_ORDERED_STATEMENT:
			translatedTransformNodes.add(translateFocusOrderedStatement(contractTree, scope));
			translatedBlockItems.add(nodeFactory.newNullStatementNode(parseTree.source(contractTree)));
			break;
		case AcslParser.TRANSFORM_CONTRACT:
			translatedTransformNodes.addAll(translateTransformContract((CommonTree) contractTree.getChild(0), scope));
			break;
		default:
			throw this.error("unknown kind of contract", contractTree);
		}
		return new ACSLSpecTranslation(parseTree.source(contractTree), translatedContractNodes,
				translatedTransformNodes, translatedBlockItems);
	}

	private List<TransformNode> translateTransformContract(CommonTree tree, SimpleScope scope) throws SyntaxException {
		assert tree.getType() == AcslParser.TRANSFORM_CONTRACT_BLOCK;

		int numChildren = tree.getChildCount();
		List<TransformNode> result = new ArrayList<>();

		for (int i = 0; i < numChildren; i++) {
			result.add(translateTransform((CommonTree) tree.getChild(i), scope));
		}
		return result;
	}

	private TransformNode translateTransform(CommonTree tree, SimpleScope scope) throws SyntaxException {
		assert tree.getType() == AcslParser.TRANSFORM;
		Source source = newSource(tree);
		CommonTree specTree = (CommonTree) tree.getChild(0);

		switch (specTree.getType()) {
		case FOCUS_ASSERT:
			return translateFocusAssert(specTree, source);
		default:
			throw this.error("Unknown transform type", specTree);
		}
	}

	private FocusAssertTransformNode translateFocusAssert(CommonTree tree, Source source) throws SyntaxException {
		List<String> focusTags = new ArrayList<String>();
		for (int i = 0; i < tree.getChildCount(); i++) {
			String childText = ((CommonTree) tree.getChild(i)).getToken().getText();
			for (String focusTag : focusTags) {
				if (focusTag == childText) {
					throw this.error("focus cannot be invoked with duplicate focus tags. Tag \"" + focusTag
							+ "\" is used more than once.", tree);
				}
			}
			focusTags.add(childText);
		}
		return this.nodeFactory.newFocusAssertNode(source, tokenFactory, focusTags);
	}

	/**
	 * translates the contract for a loop.
	 *
	 * @param tree  the tree to be translated, which represented the contracts of
	 *              the loop
	 * @param scope the current scope
	 * @return the list of contracts associated with a loop
	 * @throws SyntaxException if there are syntax errors
	 */
	private List<ContractNode> translateLoopContractBlock(CommonTree tree, SimpleScope scope) throws SyntaxException {
		int numChildren = tree.getChildCount();
		List<ContractNode> result = new ArrayList<>();

		assert tree.getType() == AcslParser.LOOP_CONTRACT_BLOCK;
		for (int i = 0; i < numChildren; i++) {
			CommonTree loopItem = (CommonTree) tree.getChild(i);
			int loopItemKind = loopItem.getType();

			switch (loopItemKind) {
			case AcslParser.LOOP_CLAUSE:
				result.add(this.translateLoopClause((CommonTree) loopItem.getChild(0), scope));
				break;
			case AcslParser.FOCUS_LOOP:
				result.add(translateFocusLoop(loopItem, newSource(loopItem), scope));
				break;
			case AcslParser.LOOP_VARIANT:
				System.err.println("loop variants are not supported hence ignored.");
				break;
			case AcslParser.LOOP_BEHAVIOR:
			default:
				throw this.error("unknown kind of loop contract", loopItem);
			}
		}
		return result;
	}

	private FocusLoopTransformNode translateFocusLoop(CommonTree tree, Source source, SimpleScope scope)
			throws SyntaxException {
		CommonTree focusHeaderTree = (CommonTree) tree.getChild(0);
		String focusTag = ((CommonTree) focusHeaderTree.getChild(0)).getToken().getText();
		SequenceNode<ExpressionNode> tagWindow = focusHeaderTree.getChildCount() > 1
				? translateLoopFocusWindow((CommonTree) focusHeaderTree.getChild(1), scope)
				: null;
		SequenceNode<ExpressionNode> memoryList = translateArgumentList((CommonTree) tree.getChild(1), scope);
		return this.nodeFactory.newFocusLoopNode(source, tokenFactory, focusTag, tagWindow, memoryList);
	}

	private SequenceNode<ExpressionNode> translateLoopFocusWindow(CommonTree tree, SimpleScope scope)
			throws SyntaxException {
		CommonTree child = (CommonTree) tree.getChild(0);
		ExpressionNode windowExpr = translateExpression(child, scope);
		ExpressionNode windowLower = null, windowUpper = null;
		switch (tree.getType()) {
		case AcslParser.LOOP_FOCUS_POS_SINGLETON:
			windowLower = windowExpr;
			break;
		case AcslParser.LOOP_FOCUS_NEG_SINGLETON:
			windowLower = nodeFactory.newOperatorNode(newSource(child), OperatorNode.Operator.UNARYMINUS, windowExpr);
			break;
		case AcslParser.LOOP_FOCUS_RANGE:
			if (!(windowExpr instanceof RegularRangeNode))
				throw this.error("Loop focus window requires a range when using curly braces", tree);
			RegularRangeNode rangeNode = (RegularRangeNode) windowExpr;
			if (rangeNode.getStep() != null)
				throw this.error("Loop focus window range cannot be declared with a step", tree);
			windowLower = rangeNode.getLow().copy();
			windowUpper = rangeNode.getHigh().copy();
			break;
		default:
			throw this.error("unknown kind of loop focus window", tree);
		}

		boolean simpleWindow = checkIsSimpleInteger(windowLower);
		if (windowUpper != null)
			simpleWindow &= checkIsSimpleInteger(windowUpper);
		if (!simpleWindow)
			throw error("Loop focus window requires use of integer constants only", child);

		return nodeFactory.newSequenceNode(newSource(tree), "loop focus window",
				Arrays.asList(windowLower, windowUpper));
	}

	private boolean checkIsSimpleInteger(ExpressionNode node) {
		if (node instanceof IntegerConstantNode)
			return true;
		if (node instanceof OperatorNode) {
			OperatorNode opNode = (OperatorNode) node;
			return opNode.getOperator() == OperatorNode.Operator.UNARYMINUS
					&& checkIsSimpleInteger((ExpressionNode) node.child(0));
		}
		return false;
	}

	/**
	 * translate the LOGIC_FUNCTIONS list, which is a list of mixed items, each of
	 * which is either a LOGIC_FUNCTION_CLAUSE or PREDICATE_CLAUSE:
	 */
	private List<BlockItemNode> translateLogicFunctions(CommonTree logicFunctions, SimpleScope scope)
			throws SyntaxException {
		int numChild = logicFunctions.getChildCount();
		List<BlockItemNode> programNodes = new LinkedList<>();

		for (int i = 0; i < numChild; i++) {
			CommonTree clause = (CommonTree) logicFunctions.getChild(i);
			BlockItemNode translatedClause;

			if (clause.getType() == AcslParser.PREDICATE_CLAUSE)
				translatedClause = translatePredicateClause(clause, scope);
			else {
				assert clause.getType() == AcslParser.LOGIC_FUNCTION_CLAUSE;
				translatedClause = translateLogicFunctionClause(clause, scope);
			}
			programNodes.add(translatedClause);
		}
		return programNodes;
	}

	/**
	 * Translate ACSL assert to CIVL $assert.
	 */
	private BlockItemNode translateACSLAssertion(CommonTree assertTree, SimpleScope scope) throws SyntaxException {
		CommonTree predicate = (CommonTree) assertTree.getChild(0);
		Source source = parseTree.source(assertTree);
		ExpressionNode assertCall = nodeFactory.newFunctionCallNode(source,
				nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, CIVL_ASSERT)),
				Arrays.asList(translateExpression(predicate, scope)));

		return nodeFactory.newExpressionStatementNode(assertCall);
	}

	private TransformNode translateFocusOrderedStatement(CommonTree ordTree, SimpleScope scope) throws SyntaxException {
		OperatorNode relOpNode = translateRelOp((CommonTree) ordTree.getChild(0));
		String focusTag = ((CommonTree) ordTree.getChild(1)).getToken().getText();
		CommonTree rangeTree = (CommonTree) ordTree.getChild(2);
		ExpressionNode rangeExprNode = translateExpression(rangeTree, scope);
		if (!(rangeExprNode instanceof RegularRangeNode))
			throw error("Focus ordered statement requires a range", rangeTree);
		RegularRangeNode rangeNode = (RegularRangeNode) rangeExprNode;
		if (rangeNode.getStep() != null)
			throw error("Focus ordered statement only supports ranges without a step", rangeTree);
		CommonTree exprTree = (CommonTree) ordTree.getChild(3);
		ExpressionNode exprNode = translateExpression(exprTree, scope);
		return nodeFactory.newFocusOrderedNode(parseTree.source(ordTree), tokenFactory, focusTag, relOpNode, rangeNode,
				exprNode);
	}

	private OperatorNode translateRelOp(CommonTree relOpTree) throws SyntaxException {
		OperatorNode.Operator op;
		switch (relOpTree.getType()) {
		case LTE:
			op = Operator.LTE;
			break;
		case LT:
			op = Operator.LT;
			break;
		case GTE:
			op = Operator.GTE;
			break;
		case GT:
			op = Operator.GT;
			break;
		case EQUALS:
			op = Operator.EQUALS;
			break;
		default:
			throw error("Invalid relation operator used in focus ordered statement", relOpTree);
		}
		return nodeFactory.newOperatorNode(parseTree.source(relOpTree), op, Arrays.asList());
	}

	/**
	 * Translate ACSL predicate clause
	 * <code>predicate id binders (opt) = definition (opt) </code>
	 *
	 * @param predicate
	 * @param scope
	 * @return
	 * @throws SyntaxException
	 */
	private BlockItemNode translatePredicateClause(CommonTree predicateClause, SimpleScope scope)
			throws SyntaxException {
		CommonTree identifierTree = (CommonTree) predicateClause.getChild(0);
		CommonTree definitionTree = (CommonTree) predicateClause.getChild(1);
		FunctionDeclarationNode result = translateLogicFunctionBody(identifierTree, definitionTree,
				nodeFactory.newBasicTypeNode(parseTree.source(identifierTree), BasicTypeKind.BOOL), scope);

		result.setIsLogicFunction(true);
		return result;
	}

	/**
	 * Translate ACSL predicate clause
	 * <code>logic type id binders (opt) = definition (opt) </code>
	 *
	 * @param logicFunctionClause
	 * @param scope
	 * @return
	 * @throws SyntaxException
	 */
	private BlockItemNode translateLogicFunctionClause(CommonTree logicFunctionClause, SimpleScope scope)
			throws SyntaxException {
		CommonTree typeTree = (CommonTree) logicFunctionClause.getChild(0);
		CommonTree identifierTree = (CommonTree) logicFunctionClause.getChild(1);
		CommonTree definitionTree = (CommonTree) logicFunctionClause.getChild(2);
		TypeNode returnType = translateTypeExpr(typeTree, scope);
		FunctionDeclarationNode result = translateLogicFunctionBody(identifierTree, definitionTree, returnType, scope);

		result.setIsLogicFunction(true);
		return result;
	}

	private FunctionDeclarationNode translateLogicFunctionBody(CommonTree identifierTree, CommonTree definitionTree,
			TypeNode outputType, SimpleScope scope) throws SyntaxException {
		CommonTree bindersTree = (CommonTree) definitionTree.getChild(0);
		CommonTree bodyTree = (CommonTree) definitionTree.getChild(1);
		SimpleScope defiScope = new SimpleScope(scope);
		SequenceNode<VariableDeclarationNode> binders;
		Source source = parseTree.source(definitionTree);
		ExpressionNode definition = null;
		IdentifierNode identifier = translateIdentifier(identifierTree);

		if (bindersTree.getType() != AcslParser.ABSENT)
			binders = translateBinders(bindersTree, parseTree.source(bindersTree), defiScope);
		else
			binders = nodeFactory.newSequenceNode(source, "ABSENT", Arrays.asList());

		FunctionTypeNode funcTypeNode = nodeFactory.newFunctionTypeNode(source, outputType, binders, false);
		FunctionDeclarationNode result;

		if (bodyTree.getType() != AcslParser.ABSENT) {
			definition = translateExpression(bodyTree, defiScope);

			StatementNode expressionBody = nodeFactory.newReturnNode(definition.getSource(), definition);
			CompoundStatementNode returnExpr = nodeFactory.newCompoundStatementNode(expressionBody.getSource(),
					Arrays.asList(expressionBody));

			result = nodeFactory.newFunctionDefinitionNode(source, identifier, funcTypeNode, null, returnExpr);
		} else
			result = nodeFactory.newAbstractFunctionDefinitionNode(source, identifier, funcTypeNode, null, 0, null,
					null);
		return result;
	}

	/**
	 * translates a loop clause, which could be a loop invariant, an assigns clause,
	 * an allocate clause, a free clause, or a transform clause. Currently, allocate
	 * and free clause are not supported.
	 *
	 * @param tree  the tree represented a loop contract clause
	 * @param scope the current scope
	 * @return the contract node represented a loop clause
	 * @throws SyntaxException if there are some syntax errors
	 */
	private ContractNode translateLoopClause(CommonTree tree, SimpleScope scope) throws SyntaxException {
		int loopClauseKind = tree.getType();
		Source source = this.newSource(tree);

		switch (loopClauseKind) {
		case AcslParser.LOOP_INVARIANT: {
			CommonTree exprTree = (CommonTree) tree.getChild(0);
			ExpressionNode expression = this.translateExpression(exprTree, scope);

			return this.nodeFactory.newInvariantNode(source, true, expression);
		}
		case AcslParser.LOOP_ASSIGNS:
			return translateReadsOrAssigns(tree, scope, false);
		case AcslParser.LOOP_ALLOC:
		case AcslParser.LOOP_FREE:
		default:
			throw this.error("unknown kind of loop contract clause", tree);
		}
	}

	/**
	 * translates a contract block associated with a function
	 *
	 * @param tree  the tree representing the contract block
	 * @param scope the current scope, which is the scope of the function parameter
	 * @return the list of contract nodes after translation
	 * @throws SyntaxException if there are syntax errors
	 */
	private List<ContractNode> translateFunctionContractBlock(CommonTree tree, SimpleScope scope)
			throws SyntaxException {
		int numChildren = tree.getChildCount();
		List<ContractNode> result = new ArrayList<>();

		assert tree.getType() == AcslParser.FUNC_CONTRACT_BLOCK;
		for (int i = 0; i < numChildren; i++) {
			CommonTree child = (CommonTree) tree.getChild(i);
			int childKind = child.getType();

			switch (childKind) {
			case AcslParser.CLAUSE_NORMAL:
				result.add(this.translateContractClause((CommonTree) child.getChild(0), scope));
				break;
			case AcslParser.CLAUSE_BEHAVIOR:
				result.add(this.translateBehavior((CommonTree) child.getChild(0), scope));
				break;
			case AcslParser.CLAUSE_COMPLETE:
				result.add(this.translateCompleteness((CommonTree) child.getChild(0), scope));
				break;
			default:
				throw this.error("Unknown contract kind", tree);
			}
		}
		return result;
	}

	/**
	 * translates the ACSL completeness clause for behavior, which could be COMPLETE
	 * or DISJOINT.
	 *
	 * @param tree  the tree representing the completeness clause
	 * @param scope the current scope
	 * @return the completeness node which is the result of translating the given
	 *         tree
	 * @throws SyntaxException if there are some syntax errors
	 */
	private CompletenessNode translateCompleteness(CommonTree tree, SimpleScope scope) throws SyntaxException {
		int kind = tree.getType();
		boolean isComplete = false;
		SequenceNode<IdentifierNode> idList = this.translateIdList((CommonTree) tree.getChild(2), scope);
		Source source = this.parseTree.source(tree);

		switch (kind) {
		case AcslParser.BEHAVIOR_COMPLETE:
			isComplete = true;
			break;
		case AcslParser.BEHAVIOR_DISJOINT:
			break;
		default:
			throw this.error("Unknown kind of completeness clause", tree);
		}
		return this.nodeFactory.newCompletenessNode(source, isComplete, idList);
	}

	/**
	 * translates a list of identifiers
	 *
	 * @param tree  a tree that represents a list of identifiers
	 * @param scope the current scope
	 * @return a sequence of identifier node
	 */
	private SequenceNode<IdentifierNode> translateIdList(CommonTree tree, SimpleScope scope) {
		int numChildren = tree.getChildCount();
		List<IdentifierNode> list = new ArrayList<>();
		Source source = this.parseTree.source(tree);

		for (int i = 0; i < numChildren; i++) {
			CommonTree idTree = (CommonTree) tree.getChild(i);

			list.add(this.translateIdentifier(idTree));
		}
		return this.nodeFactory.newSequenceNode(source, "ID list", list);
	}

	/**
	 * translates an ACSL behavior block
	 *
	 * @param tree  the tree that represents a behavior block
	 * @param scope the current scope
	 * @return the behavior node which is the result of translating the given
	 *         behavior block
	 * @throws SyntaxException if there are any syntax errors.
	 */
	private BehaviorNode translateBehavior(CommonTree tree, SimpleScope scope) throws SyntaxException {
		CommonTree idTree = (CommonTree) tree.getChild(1);
		CommonTree bodyTree = (CommonTree) tree.getChild(2);
		IdentifierNode id = this.translateIdentifier(idTree);
		SequenceNode<ContractNode> body = this.translateBehaviorBody(bodyTree, scope);

		return this.nodeFactory.newBehaviorNode(this.parseTree.source(tree), id, body);
	}

	/**
	 * translates the body of a behavior block.
	 *
	 * @param tree  the tree that represents the body of a behavior block
	 * @param scope the current scope
	 * @return a sequence of contract nodes which is the result of the translation
	 * @throws SyntaxException if there are any syntax errors
	 */
	private SequenceNode<ContractNode> translateBehaviorBody(CommonTree tree, SimpleScope scope)
			throws SyntaxException {
		Source source = this.parseTree.source(tree);
		int numChildren = tree.getChildCount();
		List<ContractNode> clauses = new ArrayList<>();

		for (int i = 0; i < numChildren; i++) {
			CommonTree clause = (CommonTree) tree.getChild(i);

			clauses.add(this.translateContractClause(clause, scope));
		}
		return this.nodeFactory.newSequenceNode(source, "behavior body", clauses);
	}

	/**
	 * translates a contract clause.
	 *
	 * @param tree  the tree that representing a contract clause
	 * @param scope the current scope
	 * @return the contract node which is the result of the translation
	 * @throws SyntaxException if there are any syntax errors
	 */
	private ContractNode translateContractClause(CommonTree tree, SimpleScope scope) throws SyntaxException {
		int kind = tree.getType();

		switch (kind) {
		case AcslParser.ALLOC:
			return this.translateAllocation(tree, scope, true);
		case AcslParser.FREES:
			return this.translateAllocation(tree, scope, false);
		case AcslParser.REQUIRES_ACSL:
			return this.translateRequires(tree, scope);
		case AcslParser.ENSURES_ACSL:
			return this.translateEnsures(tree, scope);
		case AcslParser.ASSIGNS_ACSL:
			return this.translateReadsOrAssigns(tree, scope, false);
		case AcslParser.ASSUMES_ACSL:
			return this.translateAssumes(tree, scope);
		case AcslParser.READS_ACSL:
			return this.translateReadsOrAssigns(tree, scope, true);
		case AcslParser.DEPENDSON:
			return this.translateDepends(tree, scope);
		case AcslParser.EXECUTES_WHEN:
			return this.translateGuards(tree, scope);
		default:
			throw this.error("Unknown contract clause kind", tree);
		}
	}

	private AllocationNode translateAllocation(CommonTree tree, SimpleScope scope, boolean isAllocates)
			throws SyntaxException {
		SequenceNode<ExpressionNode> memoryList = this.translateArgumentList((CommonTree) tree.getChild(1), scope);

		return this.nodeFactory.newAllocationNode(this.newSource(tree), isAllocates, memoryList);
	}

	/**
	 * translates a guard clause, which has the syntax
	 * <code>executes_when expr</code>.
	 *
	 * @param tree  the tree that represents the guard clause
	 * @param scope the current scope
	 * @return the guard node that is the result of the translation
	 * @throws SyntaxException if there are some syntax errors.
	 */
	private GuardsNode translateGuards(CommonTree tree, SimpleScope scope) throws SyntaxException {
		CommonTree expressionTree = (CommonTree) tree.getChild(1);

		return this.nodeFactory.newGuardNode(this.newSource(tree), this.translateExpression(expressionTree, scope));
	}

	/**
	 * translates an assume clause, which has the syntax <code>assumes expr</code>.
	 *
	 * @param tree  the tree that represents an assumes clause
	 * @param scope the current scope
	 * @return the Assumes node
	 * @throws SyntaxException if there are any syntax errors.
	 */
	private AssumesNode translateAssumes(CommonTree tree, SimpleScope scope) throws SyntaxException {
		CommonTree exprTree = (CommonTree) tree.getChild(1);
		ExpressionNode predicate = this.translateExpression(exprTree, scope);
		Source source = this.parseTree.source(tree);

		return this.nodeFactory.newAssumesNode(source, predicate);
	}

	private AssignsOrReadsNode translateReadsOrAssigns(CommonTree tree, SimpleScope scope, boolean isRead)
			throws SyntaxException {
		Source source = this.parseTree.source(tree);
		SequenceNode<ExpressionNode> memoryList = translateArgumentList((CommonTree) tree.getChild(1), scope);

		if (isRead)
			return this.nodeFactory.newReadsNode(source, memoryList);
		else
			return this.nodeFactory.newAssignsNode(source, memoryList);
	}

	private DependsNode translateDepends(CommonTree tree, SimpleScope scope) throws SyntaxException {
		Source source = this.parseTree.source(tree);
		CommonTree argumentTree = (CommonTree) tree.getChild(1);
		SequenceNode<DependsEventNode> argumentList = this.translateDependsEventList(argumentTree, scope);

		return this.nodeFactory.newDependsNode(source, null, argumentList);
	}

	private SequenceNode<ExpressionNode> translateArgumentList(CommonTree tree, SimpleScope scope)
			throws SyntaxException {
		int numChildren = tree.getChildCount();
		List<ExpressionNode> list = new ArrayList<>();

		for (int i = 0; i < numChildren; i++) {
			CommonTree arg = (CommonTree) tree.getChild(i);

			list.add(this.translateExpression(arg, scope));
		}
		return this.nodeFactory.newSequenceNode(this.parseTree.source(tree), "argument list", list);
	}

	private SequenceNode<DependsEventNode> translateDependsEventList(CommonTree tree, SimpleScope scope)
			throws SyntaxException {
		int numChildren = tree.getChildCount();
		List<DependsEventNode> events = new ArrayList<>();
		Source source = this.parseTree.source(tree);

		for (int i = 0; i < numChildren; i++) {
			CommonTree event = (CommonTree) tree.getChild(i);

			events.add(this.translateDependsEvent(event, scope));
		}
		return this.nodeFactory.newSequenceNode(source, "depends event list", events);
	}

	private DependsEventNode translateDependsEvent(CommonTree tree, SimpleScope scope) throws SyntaxException {
		int kind = tree.getType();

		switch (kind) {
		case EVENT_PLUS:
			EventOperator operator = EventOperator.UNION;
			return translateOperatorEvent(tree, operator, scope);
		case EVENT_SUB:
			operator = EventOperator.DIFFERENCE;
			return translateOperatorEvent(tree, operator, scope);
		case EVENT_INTS:
			operator = EventOperator.INTERSECT;
			return translateOperatorEvent(tree, operator, scope);
		case EVENT_BASE:
			return translateDependsEventBase((CommonTree) tree.getChild(0), scope);
		default:
			throw this.error("unknown kind of operator for depends events", tree);
		}
	}

	private CompositeEventNode translateOperatorEvent(CommonTree tree, EventOperator op, SimpleScope scope)
			throws SyntaxException {
		Source source = this.parseTree.source(tree);
		CommonTree leftTree = (CommonTree) tree.getChild(0), rightTree = (CommonTree) tree.getChild(1);
		DependsEventNode left = this.translateDependsEventBase(leftTree, scope),
				right = this.translateDependsEventBase(rightTree, scope);

		return this.nodeFactory.newOperatorEventNode(source, op, left, right);
	}

	private DependsEventNode translateDependsEventBase(CommonTree tree, SimpleScope scope) throws SyntaxException {
		int kind = tree.getType();
		Source source = this.parseTree.source(tree);

		switch (kind) {
		case READ_ACSL: {
			SequenceNode<ExpressionNode> memList = this.translateArgumentList((CommonTree) tree.getChild(1), scope);

			return nodeFactory.newMemoryEventNode(source, MemoryEventNodeKind.READ, memList);
		}
		case WRITE_ACSL: {
			SequenceNode<ExpressionNode> memList = this.translateArgumentList((CommonTree) tree.getChild(1), scope);

			return nodeFactory.newMemoryEventNode(source, MemoryEventNodeKind.WRITE, memList);
		}
		case ACCESS_ACSL: {
			SequenceNode<ExpressionNode> memList = this.translateArgumentList((CommonTree) tree.getChild(1), scope);

			return nodeFactory.newMemoryEventNode(source, MemoryEventNodeKind.REACH, memList);

		}
		case CALL_ACSL: {
			IdentifierNode function = this.translateIdentifier((CommonTree) tree.getChild(1));
			SequenceNode<ExpressionNode> args = this.translateArgumentList((CommonTree) tree.getChild(1), scope);

			return nodeFactory.newCallEventNode(source,
					this.nodeFactory.newIdentifierExpressionNode(function.getSource(), function), args);
		}
		case NOTHING:
			return nodeFactory.newNoactNode(source);
		case ANYACT:
			return nodeFactory.newAnyactNode(source);
		case EVENT_PARENTHESIZED:
			return translateDependsEvent((CommonTree) tree.getChild(0), scope);
		default:
			throw this.error("unknown kind of nodes for depends events", tree);
		}
	}

	private RequiresNode translateRequires(CommonTree tree, SimpleScope scope) throws SyntaxException {
		CommonTree expressionTree = (CommonTree) tree.getChild(1);
		Source source = this.newSource(tree);
		ExpressionNode expression = this.translateExpression(expressionTree, scope);

		return nodeFactory.newRequiresNode(source, expression);
	}

	private EnsuresNode translateEnsures(CommonTree tree, SimpleScope scope) throws SyntaxException {
		Source source = this.newSource(tree);
		CommonTree expressionTree = (CommonTree) tree.getChild(1);
		ExpressionNode expression = this.translateExpression(expressionTree, scope);

		return nodeFactory.newEnsuresNode(source, expression);
	}

	/**
	 * Translates an expression.
	 *
	 * @param expressionTree any CommonTree node representing an expression
	 * @return an ExpressionNode
	 * @throws SyntaxException
	 */
	private ExpressionNode translateExpression(CommonTree expressionTree, SimpleScope scope) throws SyntaxException {
		Source source = this.newSource(expressionTree);
		int kind = expressionTree.getType();

		switch (kind) {
		case INTEGER_CONSTANT:
			return translateIntegerConstant(source, expressionTree);
		case FLOATING_CONSTANT:
			return translateFloatingConstant(source, expressionTree);
		case CHARACTER_CONSTANT:
			return translateCharacterConstant(source, expressionTree);
		case STRING_LITERAL:
			return translateStringLiteral(source, expressionTree);
		case IDENTIFIER: {
			IdentifierNode identifier = translateIdentifier(expressionTree);
			ExpressionNode enumerationConsant = translateEnumerationConstant(identifier, scope);

			return enumerationConsant != null ? enumerationConsant
					: nodeFactory.newIdentifierExpressionNode(source, identifier);
		}
		case TERM_PARENTHESIZED:
			return translateExpression((CommonTree) expressionTree.getChild(0), scope);
		case DOT:
		case ARROW:
			return translateDotOrArrow(source, expressionTree, scope);
		case OPERATOR:
			return translateOperatorExpression(source, expressionTree, scope);
		case RELCHAIN:
			return translateRelationalChain(source, expressionTree, scope);
		case TRUE_ACSL:
			return translateTrue(source);
		case FALSE_ACSL:
			return translateFalse(source);
		case RESULT_ACSL:
			return nodeFactory.newResultNode(source);
		case SELF:
			return nodeFactory.newSelfNode(source);
		case DOTDOT:
			return translateRegularRange(source, expressionTree, scope);
		case WRITE_ACSL:
			return translateWriteEvent(source, expressionTree, scope);
		case NOTHING:
			return this.nodeFactory.newNothingNode(source);
		case ELLIPSIS:
			return this.nodeFactory.newWildcardNode(source);
		case VALID:
			return this.translateValidNode(expressionTree, source, scope);
		case QUANTIFIED:
			return translateQuantifiedExpression(expressionTree, source, scope);
		case FUNC_CALL:
			return translateCall(source, expressionTree, scope);
		case AcslParser.OBJECT_OF:
			return translateObjectOf(source, expressionTree, scope);
		case AcslParser.QUANTIFIED_EXT:
			return translateExtendedQuantification(source, (CommonTree) expressionTree.getChild(0), scope);
		case AcslParser.LAMBDA_ACSL:
			return translateLambda(source, expressionTree, scope);
		case AcslParser.OLD:
			return translateOld(source, expressionTree, scope);
		case SIZEOF:
			return translateSizeOf(source, expressionTree, scope);
		case CAST:
			return nodeFactory.newCastNode(source, translateTypeExpr((CommonTree) expressionTree.getChild(0), scope),
					translateExpression((CommonTree) expressionTree.getChild(1), scope));
		case SET_BINDERS:
			throw error("Unsupported expression kind", expressionTree);
		default:
			throw error("Unknown expression kind", expressionTree);
		} // end switch
	}

	/**
	 * @param expressionTree
	 * @return
	 * @throws SyntaxException
	 */
	private SizeofNode translateSizeOf(Source source, CommonTree expressionTree, SimpleScope scope)
			throws SyntaxException {
		int kind = expressionTree.getChild(0).getType();
		CommonTree child = (CommonTree) expressionTree.getChild(1);
		SizeableNode sizeable;

		if (kind == EXPR)
			sizeable = translateExpression(child, scope);
		else if (kind == TYPE)
			sizeable = this.translateTypeExpr(child, scope);
		else
			throw error("Unexpected argument to sizeof", expressionTree);
		return nodeFactory.newSizeofNode(source, sizeable);
	}

	private ExpressionNode translateOld(Source source, CommonTree old, SimpleScope scope) throws SyntaxException {
		ExpressionNode expr = this.translateExpression((CommonTree) old.getChild(1), scope);

		return nodeFactory.newOperatorNode(source, Operator.OLD, expr);
	}

	private ExpressionNode translateLambda(Source source, CommonTree lambda, SimpleScope scope) throws SyntaxException {
		SimpleScope newScope = new SimpleScope(scope);
		SequenceNode<VariableDeclarationNode> variableList = this.translateBinders((CommonTree) lambda.getChild(1),
				this.newSource((CommonTree) lambda.getChild(1)), newScope);
		ExpressionNode expression = this.translateExpression((CommonTree) lambda.getChild(2), newScope);

		assert variableList.numChildren() == 1;
		return nodeFactory.newLambdaNode(source, variableList.getSequenceChild(0).copy(), expression);
	}

	private ExpressionNode translateExtendedQuantification(Source source, CommonTree extQuant, SimpleScope scope)
			throws SyntaxException {
		int quant = extQuant.getType();
		ExtendedQuantifier quantifier = null;
		ExpressionNode lo = this.translateExpression((CommonTree) extQuant.getChild(1), scope),
				hi = this.translateExpression((CommonTree) extQuant.getChild(2), scope),
				function = this.translateExpression((CommonTree) extQuant.getChild(3), scope);

		switch (quant) {
		case AcslParser.MAX:
			quantifier = ExtendedQuantifier.MAX;
			break;
		case AcslParser.MIN:
			quantifier = ExtendedQuantifier.MIN;
			break;
		case AcslParser.SUM:
			quantifier = ExtendedQuantifier.SUM;
			break;
		case AcslParser.PROD:
			quantifier = ExtendedQuantifier.PROD;
			break;
		case AcslParser.NUMOF:
			quantifier = ExtendedQuantifier.NUMOF;
			break;
		default:
			throw this.error("unknown kind of extended quantifier ", extQuant);
		}
		return nodeFactory.newExtendedQuantifiedExpressionNode(source, quantifier, lo, hi, function);
	}

	private ExpressionNode translateObjectOf(Source source, CommonTree tree, SimpleScope scope) throws SyntaxException {
		CommonTree operandTree = (CommonTree) tree.getChild(2);
		ExpressionNode operand = this.translateExpression(operandTree, scope);

		return nodeFactory.newObjectofNode(source, operand);
	}

	/**
	 * translate a quantified expression. e.g. \\forall | \\exists type_name
	 * identifier; predicate
	 *
	 * @param expressionTree
	 * @param source
	 * @param scope
	 * @return
	 * @throws SyntaxException
	 */
	private ExpressionNode translateQuantifiedExpression(CommonTree expressionTree, Source source, SimpleScope scope)
			throws SyntaxException {
		SimpleScope newScope = new SimpleScope(scope);
		CommonTree quantifierTree = (CommonTree) expressionTree.getChild(0);
		// The children of the quantifierTree are as follows:
		// arg0: the keyword "\forall", "\exists", or "\lambda"
		// arg1: the binders tree
		// arg2: the formula
		CommonTree bindersTree = (CommonTree) expressionTree.getChild(1);
		CommonTree predTree = (CommonTree) expressionTree.getChild(2);
		ExpressionNode restrict, pred;
		SequenceNode<VariableDeclarationNode> binders;
		Quantifier quantifier;
		// If the quantified expression has more than one binder, it will be
		// translated into several quantifiedExpressions each of which has exact
		// one binder:
		// boolean firstQuantifiedExpr = true;
		// ExpressionNode result = null;
		List<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableList = new LinkedList<>();

		if (quantifierTree.getType() == AcslParser.FORALL_ACSL) {
			quantifier = Quantifier.FORALL;
			restrict = null;
			// if the expression has the form "forall ... ; p==>q",
			// the type of predicate will be OPERATOR, arg0 will be IMPLIES
			// or IMPLIES_ACSL, and arg1 will be an ARGUMENT_LIST with 2
			// children, p and q.
			if (predTree.getType() == OPERATOR) {
				Tree predOperatorTree = predTree.getChild(0);
				int predOperatorType = predOperatorTree.getType();

				if (predOperatorType == IMPLIES_ACSL || predOperatorType == IMPLIES) {
					Tree predArgTree = predTree.getChild(1);

					assert predArgTree.getChildCount() == 2;

					CommonTree restrictTree = (CommonTree) predArgTree.getChild(0);

					predTree = (CommonTree) predArgTree.getChild(1);
					restrict = translateExpression(restrictTree, newScope);
				}
			}
			pred = translateExpression(predTree, newScope);
		} else if (quantifierTree.getType() == AcslParser.EXISTS_ACSL) {
			quantifier = Quantifier.EXISTS;
			pred = translateExpression(predTree, newScope);
			restrict = null;
		} else {
			throw error("Unexpected quantifier " + quantifierTree.getType(), quantifierTree);
		}
		binders = translateBinders(bindersTree, source, newScope);
		boundVariableList.add(nodeFactory.newPairNode(source, binders, null));
		return nodeFactory.newQuantifiedExpressionNode(source, quantifier,
				nodeFactory.newSequenceNode(source, "bound variable declaration list", boundVariableList), restrict,
				pred);
	}

	private SequenceNode<VariableDeclarationNode> translateBinders(CommonTree tree, Source source, SimpleScope scope)
			throws SyntaxException {
		int count = tree.getChildCount();
		List<VariableDeclarationNode> vars = new LinkedList<>();

		for (int i = 0; i < count; i++) {
			CommonTree binder = (CommonTree) tree.getChild(i);

			vars.addAll(this.translateBinder(binder, scope));
		}
		return this.nodeFactory.newSequenceNode(source, "Binder List", vars);
	}

	private List<VariableDeclarationNode> translateBinder(CommonTree tree, SimpleScope scope) throws SyntaxException {
		CommonTree typeTree = (CommonTree) tree.getChild(0);
		int numChild = tree.getChildCount();
		TypeNode type = this.translateTypeExpr(typeTree, scope);
		List<VariableDeclarationNode> result = new LinkedList<>();

		for (int i = 1; i < numChild; i++) {
			CommonTree varIdent = (CommonTree) tree.getChild(i);

			result.add(this.translateVariableIdent(varIdent, scope, type.copy()));
		}
		return result;
	}

	private TypeNode translateTypeExpr(CommonTree tree, SimpleScope scope) throws SyntaxException {
		int kind = tree.getType();

		switch (kind) {
		case LOGIC_TYPE:
			return translateLogicType((CommonTree) tree.getChild(0), scope);
		case C_TYPE:
			return translateCType((CommonTree) tree.getChild(0), (CommonTree) tree.getChild(1), scope);
		default:
			throw this.error("unkown kind of tyep expression", tree);
		}
	}

	/**
	 * ^(C_TYPE specifierList abstractDeclarator)
	 *
	 * @param specifierList Type specifier tree
	 * @param declarators   Abstract declarator tree
	 * @param scope
	 * @return
	 * @throws SyntaxException
	 */
	private TypeNode translateCType(CommonTree specifierList, CommonTree declarators, SimpleScope scope)
			throws SyntaxException {
		Source specifierSource = newSource(specifierList);
		SpecifierAnalysis specifierAnalyzer;
		TypeNode result;
		DeclaratorData declaratorData;

		specifierAnalyzer = new SpecifierAnalysis(specifierList, parseTree, config);
		if (specifierAnalyzer.typeNameKind == TypeNodeKind.BASIC)
			result = nodeFactory.newBasicTypeNode(specifierSource, specifierAnalyzer.getBasicTypeKind());
		else if (specifierAnalyzer.typeNameKind == TypeNodeKind.VOID)
			result = nodeFactory.newVoidTypeNode(specifierSource);
		else
			throw new RuntimeException(
					"Translation of C type of kind : " + specifierAnalyzer.typeNameKind + " has not been implemented.");
		if (declarators.getType() != ABSENT) {
			declaratorData = processDeclarator(declarators, result, scope);
			result = declaratorData.type;
		}
		return result;
	}

	/**
	 * Creates a new DeclaratorData based on given direct declarator tree node and
	 * base type. The direct declarator may be abstract.
	 *
	 * @param directDeclarator CommonTree node of type DIRECT_DECLARATOR,
	 *                         DIRECT_ABSTRACT_DECLARATOR, or ABSENT
	 * @param type             base type
	 * @return new DeclaratorData with derived type and identifier
	 * @throws SyntaxException
	 */
	private DeclaratorData processDirectDeclarator(CommonTree directDeclarator, TypeNode type, SimpleScope scope)
			throws SyntaxException {
		if (directDeclarator.getType() == ABSENT) {
			return new DeclaratorData(type, null);
		} else {
			int numChildren = directDeclarator.getChildCount();
			CommonTree prefix = (CommonTree) directDeclarator.getChild(0);

			// need to peel off right-most suffix first. Example:
			// T prefix [](); : (array of function returning T) prefix;
			for (int i = numChildren - 1; i >= 1; i--)
				type = translateDeclaratorSuffix((CommonTree) directDeclarator.getChild(i), type, scope);
			switch (prefix.getType()) {
			case ABSTRACT_DECLARATOR:
				return processDeclarator(prefix, type, scope);
			case ABSENT:
				return new DeclaratorData(type, null);
			default:
				throw error("Unexpected node for direct declarator prefix", prefix);
			}
		}
	}

	/**
	 * Creates new DeclaratorData based on given declarator tree node and base type.
	 * The declarator may be abstract. The data gives the new type formed by
	 * applying the type derivation operations of the declarator to the base type.
	 * The data also gives the identifier being declared, though this may be null in
	 * the case of an abstract declarator.
	 *
	 * @param declarator CommonTree node of type DECLARATOR, ABSTRACT_DECLARATOR, or
	 *                   ABSENT
	 * @param type       the start type before applying declarator operations
	 * @return new DeclaratorData with type derived from given type and identifier
	 * @throws SyntaxException
	 */
	private DeclaratorData processDeclarator(CommonTree declarator, TypeNode type, SimpleScope scope)
			throws SyntaxException {
		if (declarator.getType() == ABSENT) {
			return new DeclaratorData(type, null);
		} else {
			CommonTree pointerTree = (CommonTree) declarator.getChild(0);
			CommonTree directDeclarator = (CommonTree) declarator.getChild(1);
			type = translatePointers(pointerTree, type, scope);

			return processDirectDeclarator(directDeclarator, type, scope);
		}
	}

	/**
	 * Returns the new type obtained by taking the given type and applying the
	 * pointer operations to it. For example, if the old type is "int" and the
	 * pointerTree is "*", the result is the type "pointer to int".
	 *
	 * @param pointerTree CommonTree node of type POINTER or ABSENT
	 * @param type        base type
	 * @return modified type
	 * @throws SyntaxException if an unknown kind of type qualifier appears
	 */
	private TypeNode translatePointers(CommonTree pointerTree, TypeNode type, SimpleScope scope)
			throws SyntaxException {
		int numChildren = pointerTree.getChildCount();
		Source source = type.getSource();

		for (int i = 0; i < numChildren; i++) {
			CommonTree starNode = (CommonTree) pointerTree.getChild(i);

			source = tokenFactory.join(source, newSource(starNode));
			type = nodeFactory.newPointerTypeNode(source, type);
		}
		return type;
	}

	/**
	 * Process declarator suffix, currently it only supports ARRAY_SUFFIX
	 *
	 * @param suffix a CommonTree node of type ARRAY_SUFFIX or FUNCTION_SUFFIX
	 * @param type
	 * @return new type
	 * @throws SyntaxException if the kind of suffix is not function or array
	 */
	private TypeNode translateDeclaratorSuffix(CommonTree suffix, TypeNode baseType, SimpleScope scope)
			throws SyntaxException {
		int kind = suffix.getType();

		if (kind == ARRAY_SUFFIX)
			return translateArraySuffix(suffix, baseType, scope);
		else
			throw error("Unknown declarator suffix", suffix);
	}

	/**
	 * process ARRAY_SUFFIX tree, currently it only supports two subscript forms.
	 * The form is either specified with extent or not: <code>[ ] or [extent]</code>
	 *
	 * @param suffix
	 * @param baseType
	 * @return
	 * @throws SyntaxException
	 */
	private ArrayTypeNode translateArraySuffix(CommonTree suffix, TypeNode baseType, SimpleScope scope)
			throws SyntaxException {
		CommonTree extentNode = (CommonTree) suffix.getChild(1);
		int extentNodeType = extentNode.getType();
		ExpressionNode extent = null;
		Source source = tokenFactory.join(baseType.getSource(), newSource(suffix));

		switch (extentNodeType) {
		case ABSENT:
			break;
		default:
			extent = translateExpression(extentNode, scope);
		}
		return nodeFactory.newArrayTypeNode(source, baseType, extent);
	}

	private TypeNode translateLogicType(CommonTree tree, SimpleScope scope) throws SyntaxException {
		int kind = tree.getType();
		Source source = this.newSource(tree);

		switch (kind) {
		case TYPE_BUILTIN: {
			int typeKind = tree.getChild(0).getType();

			switch (typeKind) {
			case BOOLEAN:
				return this.nodeFactory.newBasicTypeNode(source, BasicTypeKind.BOOL);
			case INTEGER:
				return this.nodeFactory.newBasicTypeNode(source, BasicTypeKind.INT);
			case REAL_ACSL:
				return this.nodeFactory.newBasicTypeNode(source, BasicTypeKind.REAL);
			default:
				throw this.error("unknown built-in logic type", tree);
			}
		}
		case TYPE_ID:
			return this.nodeFactory.newTypedefNameNode(this.translateIdentifier((CommonTree) tree.getChild(0)));
		default:
			throw this.error("unknown kind of logic type", tree);
		}
	}

	private VariableDeclarationNode translateVariableIdent(CommonTree tree, SimpleScope scope, TypeNode baseType)
			throws SyntaxException {
		int kind = tree.getType();
		Source source = this.newSource(tree);

		switch (kind) {
		case VAR_ID_STAR: {
			VariableDeclarationNode baseVar = this.translateVariableIdentBase((CommonTree) tree.getChild(0), source,
					scope, baseType);
			TypeNode baseVarType, type;

			baseVarType = baseVar.getTypeNode();
			baseVarType.remove();
			type = this.nodeFactory.newPointerTypeNode(source, baseVarType);
			baseVar.setTypeNode(type);
			return baseVar;
		}
		case VAR_ID_SQUARE: {
			VariableDeclarationNode baseVar = this.translateVariableIdentBase((CommonTree) tree.getChild(0), source,
					scope, baseType);
			TypeNode baseVarType, type;

			baseVarType = baseVar.getTypeNode();
			baseVarType.remove();
			type = this.nodeFactory.newArrayTypeNode(source, baseVarType, null);
			baseVar.setTypeNode(type);
			return baseVar;
		}
		case VAR_ID:
			return this.translateVariableIdentBase((CommonTree) tree.getChild(0), source, scope, baseType);
		default:
			throw this.error("unknown kind of variable identity", tree);
		}
	}

	private VariableDeclarationNode translateVariableIdentBase(CommonTree tree, Source source, SimpleScope scope,
			TypeNode baseType) throws SyntaxException {
		int kind = tree.getType();

		switch (kind) {
		case IDENTIFIER: {
			IdentifierNode identifier = this.translateIdentifier(tree);

			return this.nodeFactory.newVariableDeclarationNode(identifier.getSource(), identifier, baseType);
		}
		case VAR_ID_BASE:
			return this.translateVariableIdent((CommonTree) tree.getChild(0), scope, baseType);
		default:
			throw this.error("unknown kind of variable identity base", tree);
		}
	}

	// ////////////////////////////////////
	private ExpressionNode translateValidNode(CommonTree tree, Source source, SimpleScope scope)
			throws SyntaxException {
		CommonTree argumentTree = (CommonTree) tree.getChild(1);
		ExpressionNode argument;

		argument = translateExpression(argumentTree, scope);
		return nodeFactory.newOperatorNode(source, Operator.VALID, argument);
	}

	private ExpressionNode translateWriteEvent(Source source, CommonTree expressionTree, SimpleScope scope) {
		// TODO Auto-generated method stub
		return null;
	}

	private IntegerConstantNode translateIntegerConstant(Source source, CommonTree integerConstant)
			throws SyntaxException {
		return nodeFactory.newIntegerConstantNode(source, integerConstant.getText());
	}

	private FloatingConstantNode translateFloatingConstant(Source source, CommonTree floatingConstant)
			throws SyntaxException {
		return nodeFactory.newFloatingConstantNode(source, floatingConstant.getText());
	}

	private CharacterConstantNode translateCharacterConstant(Source source, CommonTree characterConstant)
			throws SyntaxException {
		CharacterToken token = (CharacterToken) characterConstant.getToken();

		return nodeFactory.newCharacterConstantNode(source, characterConstant.getText(), token.getExecutionCharacter());
	}

	private ConstantNode translateTrue(Source source) {
		return nodeFactory.newBooleanConstantNode(source, true);
	}

	private ConstantNode translateFalse(Source source) {
		return nodeFactory.newBooleanConstantNode(source, false);
	}

	private StringLiteralNode translateStringLiteral(Source source, CommonTree stringLiteral) throws SyntaxException {
		StringToken token = (StringToken) stringLiteral.getToken();

		return nodeFactory.newStringLiteralNode(source, stringLiteral.getText(), token.getStringLiteral());
	}

	private ExpressionNode translateRegularRange(Source source, CommonTree expressionTree, SimpleScope scope)
			throws SyntaxException {
		// regular range expression lo..hi or lo..hi#step
		ExpressionNode loNode = translateExpression((CommonTree) expressionTree.getChild(0), scope);
		ExpressionNode hiNode = translateExpression((CommonTree) expressionTree.getChild(1), scope);
		if (expressionTree.getChildCount() > 2) {
			CommonTree stepTree = (CommonTree) expressionTree.getChild(2);

			if (stepTree != null /* && stepTree.getType() != ABSENT */) {
				ExpressionNode stepNode = translateExpression(stepTree, scope);

				return nodeFactory.newRegularRangeNode(source, loNode, hiNode, stepNode);
			}
		}
		return nodeFactory.newRegularRangeNode(source, loNode, hiNode);

	}

	/**
	 * Translates a function call expression.
	 *
	 * @param callTree CommonTree node of type CALL, representing a function call
	 * @return a FunctionCallNode corresponding to the ANTLR tree
	 * @throws SyntaxException
	 */
	private FunctionCallNode translateCall(Source source, CommonTree callTree, SimpleScope scope)
			throws SyntaxException {
		CommonTree functionTree = (CommonTree) callTree.getChild(0);
		// CommonTree contextArgumentListTree = (CommonTree)
		// callTree.getChild(2);
		CommonTree argumentListTree = (CommonTree) callTree.getChild(1);
		ExpressionNode functionNode = translateExpression(functionTree, scope);
		// int numContextArgs = contextArgumentListTree.getChildCount();
		int numArgs = argumentListTree.getChildCount();
		// List<ExpressionNode> contextArgumentList = new
		// LinkedList<ExpressionNode>();
		List<ExpressionNode> argumentList = new LinkedList<ExpressionNode>();

		for (int i = 0; i < numArgs; i++) {
			CommonTree argumentTree = (CommonTree) argumentListTree.getChild(i);
			ExpressionNode argumentNode = translateExpression(argumentTree, scope);

			argumentList.add(argumentNode);
		}
		return nodeFactory.newFunctionCallNode(source, functionNode, argumentList);
	}

	/**
	 * @param expressionTree
	 * @return
	 * @throws SyntaxException
	 */
	private ExpressionNode translateDotOrArrow(Source source, CommonTree expressionTree, SimpleScope scope)
			throws SyntaxException {
		int kind = expressionTree.getType();
		CommonTree argumentNode = (CommonTree) expressionTree.getChild(0);
		CommonTree fieldNode = (CommonTree) expressionTree.getChild(1);
		ExpressionNode argument = translateExpression(argumentNode, scope);
		IdentifierNode fieldName = translateIdentifier(fieldNode);

		if (kind == DOT)
			return nodeFactory.newDotNode(source, argument, fieldName);
		else
			return nodeFactory.newArrowNode(source, argument, fieldName);
	}

	/**
	 * Translates an ACSL relational chain expression. An example is "x < y < z" ,
	 * which is ACSL short-hand for "x<y && y<z". Here are some notes from the ACSL
	 * spec:
	 *
	 * <pre>
	 * The construct t1 relop1 t2 relop2 t3 · · · tk
	 * with several consecutive comparison operators is a shortcut
	 * (t1 relop1 t2) && (t2 relop2 t3) && ···.
	 * It is required that the relopi operators must be in the same "direction",
	 * i.e. they must all belong either to {<, <=, ==} or to {>,>=,==}.
	 * Expressions such as x < y > z or x != y != z are not allowed.
	 * </pre>
	 *
	 * @param source         source for the token sequence which makes up the
	 *                       expression
	 * @param expressionTree the parse tree resulting from parsing the expression
	 *                       token sequence
	 * @param scope          the scope in which the expression occurs
	 * @return the root of an AST tree representing this expression
	 * @throws SyntaxException if the operators are not all in the same "direction"
	 */
	private OperatorNode translateRelationalChain(Source source, CommonTree tree, SimpleScope scope)
			throws SyntaxException {
		assert tree.getType() == RELCHAIN;

		int numChildren = tree.getChildCount();

		if (numChildren < 3)
			throw new SyntaxException("relational chain has fewer than 3 arguments", source);
		if (numChildren % 2 == 0)
			throw new SyntaxException("relational chain has even number of arguments", source);

		// direction: 0=equality or unknown, 1=increasing, -1=decreasing,
		// -2=inequality
		int direction = 0;
		CommonTree arg1 = (CommonTree) tree.getChild(0), arg2;
		Source source1 = newSource(arg1), source2;
		int i = 1;
		OperatorNode result = null;
		Source resultSource = null;

		while (i < numChildren) {
			CommonTree rel = (CommonTree) tree.getChild(i);
			Operator operator;

			if (i > 1 && direction == -2)
				throw error("'!=' prohibited in a relational chain", rel);
			i++;
			switch (rel.getType()) {
			case LT:
				operator = Operator.LT;
				if (direction == 0)
					direction = 1;
				else if (direction < 0)
					throw error("'<' prohibited in a decreasing relational chain", rel);
				break;
			case LTE:
				operator = Operator.LTE;
				if (direction == 0)
					direction = 1;
				else if (direction < 0)
					throw error("'<=' prohibited in a decreasing relational chain", rel);
				break;
			case EQUALS:
				operator = Operator.EQUALS;
				break;
			case GTE:
				operator = Operator.GTE;
				if (direction == 0)
					direction = -1;
				else if (direction > 0)
					throw error("'>=' prohibited in an increasing relational chain", rel);
				break;
			case GT:
				operator = Operator.GT;
				if (direction == 0)
					direction = -1;
				else if (direction > 0)
					throw error("'>' prohibited in an increasing relational chain", rel);
				break;
			case NEQ:
				if (i > 2)
					throw error("'!=' prohibited in a relational chain", rel);
				operator = Operator.NEQ;
				direction = -2;
				break;
			default:
				throw new ABCRuntimeException("unknown ACSL relational operator: " + rel);
			}
			arg2 = (CommonTree) tree.getChild(i);
			i++;

			ExpressionNode node1 = translateExpression(arg1, scope);
			ExpressionNode node2 = translateExpression(arg2, scope);

			source2 = node2.getSource();

			Source clauseSource = tokenFactory.join(source1, source2);
			OperatorNode clause = nodeFactory.newOperatorNode(clauseSource, operator, node1, node2);

			if (result == null) {
				resultSource = clauseSource;
				result = clause;
			} else {
				resultSource = tokenFactory.join(resultSource, source2);
				result = nodeFactory.newOperatorNode(resultSource, Operator.LAND, result, clause);
			}
			arg1 = arg2;
			source1 = source2;
		}
		return result;
	}

	/**
	 * @param expressionTree
	 * @return
	 * @throws SyntaxException
	 */
	private OperatorNode translateOperatorExpression(Source source, CommonTree expressionTree, SimpleScope scope)
			throws SyntaxException {
		CommonTree operatorTree = (CommonTree) expressionTree.getChild(0);
		int operatorKind = operatorTree.getType();
		CommonTree argumentList = (CommonTree) expressionTree.getChild(1);
		int numArgs = argumentList.getChildCount();
		List<ExpressionNode> arguments = new LinkedList<ExpressionNode>();
		Operator operator;

		for (int i = 0; i < numArgs; i++) {
			ExpressionNode argument = translateExpression((CommonTree) argumentList.getChild(i), scope);

			arguments.add(argument);
		}
		switch (operatorKind) {
		case AMPERSAND:
			operator = numArgs == 1 ? Operator.ADDRESSOF : Operator.BITAND;
			break;
		case ASSIGN:
			operator = Operator.ASSIGN;
			break;
		case TILDE:
			operator = Operator.BITCOMPLEMENT;
			break;
		case BITOR:
			operator = Operator.BITOR;
			break;
		case BITXOR:
			operator = Operator.BITXOR;
			break;
		case COMMA:
			operator = Operator.COMMA;
			break;
		case QMARK:
			operator = Operator.CONDITIONAL;
			break;
		case STAR:
			operator = numArgs == 1 ? Operator.DEREFERENCE : Operator.TIMES;
			break;
		case DIV:
			operator = Operator.DIV;
			break;
		case EQUALS:
			operator = Operator.EQUALS;
			break;
		case GT:
			operator = Operator.GT;
			break;
		case GTE:
			operator = Operator.GTE;
			break;
		case HASH:
			operator = Operator.HASH;
			break;
		case AND:
			operator = Operator.LAND;
			break;
		case OR:
			operator = Operator.LOR;
			break;
		case IMPLIES_ACSL:
			operator = Operator.IMPLIES;
			break;
		case LT:
			operator = Operator.LT;
			break;
		case LTE:
			operator = Operator.LTE;
			break;
		case SUB:
			operator = numArgs == 1 ? Operator.UNARYMINUS : Operator.MINUS;
			break;
		case MOD:
			operator = Operator.MOD;
			break;
		case NEQ:
			operator = Operator.NEQ;
			break;
		case NOT:
			operator = Operator.NOT;
			break;
		case PLUS:
			operator = numArgs == 1 ? Operator.UNARYPLUS : Operator.PLUS;
			break;
		case SHIFTLEFT:
			operator = Operator.SHIFTLEFT;
			break;
		case SHIFTRIGHT:
			operator = Operator.SHIFTRIGHT;
			break;
		case INDEX:
			operator = Operator.SUBSCRIPT;
			break;
		case XOR_ACSL:
			operator = Operator.LXOR;
			break;
		case BEQUIV_ACSL:
			operator = Operator.BITEQUIV;
			break;
		case BIMPLIES_ACSL:
			operator = Operator.BITIMPLIES;
			break;
		case EQUIV_ACSL:
			operator = Operator.LEQ; // TODO: Huh???? Do this right.
			break;
		default:
			throw error("Unknown operator : " + operatorTree.getText(), operatorTree);
		}
		return nodeFactory.newOperatorNode(source, operator, arguments);
	}

	/**
	 * tries to translate the given identifier node into an enumeration node
	 * according to the scope. If the identifer's name has NOT been declared as an
	 * enumeration constant in the scope, then return null.
	 *
	 * @param identifier the identifier node to be translated
	 * @param scope      the current scope
	 * @return an enumeration constant node if the identifer's name has been
	 *         declared as an enumeration in the scope otherwise return null.
	 */
	private EnumerationConstantNode translateEnumerationConstant(IdentifierNode identifier, SimpleScope scope) {
		String name = identifier.name();

		if (scope.isEnumerationConstant(name))
			return this.nodeFactory.newEnumerationConstantNode(identifier);
		return null;
	}

	private IdentifierNode translateIdentifier(CommonTree identifier) {
		Token idToken = identifier.getToken();
		CivlcToken token;
		Source source;

		if (idToken instanceof CivlcToken)
			token = (CivlcToken) idToken;
		else
			token = tokenFactory.newCivlcToken(idToken, formation, TokenVocabulary.CIVLC);
		source = tokenFactory.newSource(token);
		return nodeFactory.newIdentifierNode(source, token.getText());
	}

	private SyntaxException error(String message, CommonTree tree) {
		return new SyntaxException(message, newSource(tree));
	}

	private Source newSource(CommonTree tree) {
		Source result = parseTree.source(tree);

		return result;
	}

}
