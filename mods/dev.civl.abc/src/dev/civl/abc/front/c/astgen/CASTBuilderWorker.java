package dev.civl.abc.front.c.astgen;

import static dev.civl.abc.front.IF.CivlcTokenConstant.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import org.antlr.runtime.Token;
import org.antlr.runtime.tree.CommonTree;

import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.AttributeKey;
import dev.civl.abc.ast.node.IF.GenericAssociationNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.PragmaNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.StaticAssertionNode;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode.ExtendedQuantifier;
import dev.civl.abc.ast.node.IF.acsl.TransformNode;
import dev.civl.abc.ast.node.IF.compound.CompoundInitializerNode;
import dev.civl.abc.ast.node.IF.compound.DesignationNode;
import dev.civl.abc.ast.node.IF.compound.DesignatorNode;
import dev.civl.abc.ast.node.IF.declaration.EnumeratorDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FieldDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ArrayLambdaNode;
import dev.civl.abc.ast.node.IF.expression.CharacterConstantNode;
import dev.civl.abc.ast.node.IF.expression.CompoundLiteralNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FloatingConstantNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.GenericSelectionNode;
import dev.civl.abc.ast.node.IF.expression.IntegerConstantNode;
import dev.civl.abc.ast.node.IF.expression.LambdaNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode.Quantifier;
import dev.civl.abc.ast.node.IF.expression.SizeableNode;
import dev.civl.abc.ast.node.IF.expression.SizeofNode;
import dev.civl.abc.ast.node.IF.expression.StatementExpressionNode;
import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;
import dev.civl.abc.ast.node.IF.label.OrdinaryLabelNode;
import dev.civl.abc.ast.node.IF.label.SwitchLabelNode;
import dev.civl.abc.ast.node.IF.omp.OmpExecutableNode;
import dev.civl.abc.ast.node.IF.omp.OmpForNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode.BlockItemKind;
import dev.civl.abc.ast.node.IF.statement.ChooseStatementNode;
import dev.civl.abc.ast.node.IF.statement.CivlForNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.DeclarationListNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopInitializerNode;
import dev.civl.abc.ast.node.IF.statement.LabeledStatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode.StatementKind;
import dev.civl.abc.ast.node.IF.statement.SwitchNode;
import dev.civl.abc.ast.node.IF.type.ArrayTypeNode;
import dev.civl.abc.ast.node.IF.type.AtomicTypeNode;
import dev.civl.abc.ast.node.IF.type.EnumerationTypeNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.StructureOrUnionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode.TypeNodeKind;
import dev.civl.abc.ast.node.IF.type.TypedefNameNode;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.config.IF.Configuration;
import dev.civl.abc.err.IF.ABCUnsupportedException;
import dev.civl.abc.front.IF.ParseException;
import dev.civl.abc.front.c.astgen.AcslContractWorker.ACSLSpecTranslation;
import dev.civl.abc.front.c.parse.CivlCParser;
import dev.civl.abc.front.c.ptree.CParseTree;
import dev.civl.abc.front.common.astgen.ASTBuilderWorker;
import dev.civl.abc.front.common.astgen.PragmaFactory;
import dev.civl.abc.front.common.astgen.PragmaHandler;
import dev.civl.abc.front.common.astgen.SimpleScope;
import dev.civl.abc.token.IF.CharacterToken;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.CivlcToken.TokenVocabulary;
import dev.civl.abc.token.IF.CivlcTokenSequence;
import dev.civl.abc.token.IF.CivlcTokenSource;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.StringToken;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;

/**
 * Builds an AST from an ANTLR tree.
 * 
 * @author siegel
 */
public class CASTBuilderWorker extends ASTBuilderWorker {

	/*
	 * Typedef names of CIVL-C types that have specific TypeNode and Type being
	 * associated with:
	 */
	/**
	 * The typedef name of the $mem type, which is associated with {@link TypeNode}s
	 * of {@link TypeNodeKind#MEM} and {@link Type}s of {@link TypeKind#MEM}
	 */
	private static String CIVLC_MEM_TYPEDEF_NAME = "$mem";

	private final String PRAGMA_ID_NAME_CIVL = "civl";

	private boolean debug = false;

	/* ************************** Instance Fields ************************* */

	private CParseTree parseTree;

	private ASTFactory astFactory;

	private NodeFactory nodeFactory;

	private TokenFactory tokenFactory;

	private PragmaFactory pragmaFactory;

	private CommonTree rootTree;

	private Map<String, PragmaHandler> pragmaMap = new HashMap<>();

	/**
	 * The configuration of this translation task.
	 */
	private Configuration config;

	/**
	 * The number of anonymous tagged entities (structs, unions, enums) encountered
	 * so far. Used to assign a unique name to each anonymous entity.
	 */
	private int anonymousTagCount = 0;

	private AcslContractHandler acslHandler;

	/**
	 * A simple class that bundles together a scope, along with annotation data
	 * obtained from translateAnnotation.
	 * 
	 * @author awilton
	 */
	public class AnnotationData {
		AnnotationData(SimpleScope scope, SequenceNode<ContractNode> contract, List<TransformNode> transforms) {
			this.scope = scope;
			this.contract = contract;
			this.transforms = transforms;
		}

		SimpleScope scope;
		SequenceNode<ContractNode> contract;
		List<TransformNode> transforms;
	}

	/**
	 * This stack keeps track of all the annotation data being applied to each scope
	 * starting from the current scope and up through each of the scopes parents.
	 * Upon entering a deeper scope, an entry is added and it is removed when
	 * leaving the scope. This is so that we can parse the annotation data before,
	 * say a statement, and then translate that statement and finally add the
	 * annotation data to that completed statement afterwards, all while being able
	 * to handle when there are annotations within the statement being translated.
	 */
	private Stack<AnnotationData> annotationStack = new Stack<>();

	/*
	 * The attribute key representing the expression is a civl omp dependency
	 * source, which shall be depended by a target with a same tag.
	 */
	private AttributeKey attrKeyDependSource = null;

	/*
	 * The attribute key representing the expression is a civl omp dependency
	 * target, which depends on a source with a same tag.
	 */
	private AttributeKey attrKeyDependTarget = null;

	/* *************************** Constructors *************************** */

	/**
	 * Constructs a new ASTBuilder for the given ANTLR tree.
	 * 
	 * @param factory     an ASTFactory to use
	 * @param rootTree    the root of the ANTLR tree
	 * @param tokenSource the CTokenSource used to produce the ANTLR tree
	 * 
	 */
	public CASTBuilderWorker(Configuration config, CParseTree parseTree, ASTFactory astFactory,
			PragmaFactory pragmaFactory) {
		this.parseTree = parseTree;
		this.astFactory = astFactory;
		this.nodeFactory = astFactory.getNodeFactory();
		this.tokenFactory = astFactory.getTokenFactory();
		this.rootTree = parseTree.getRoot();
		this.pragmaFactory = pragmaFactory;
		this.config = config;
		acslHandler = new AcslContractHandler(this.nodeFactory, this.tokenFactory);
		attrKeyDependSource = nodeFactory.getCivlOmpDependKey(true);
		attrKeyDependTarget = nodeFactory.getCivlOmpDependKey(false);
	}

	/* ************************* Private Methods ************************** */

	// Utility methods...

	private SyntaxException error(String message, CommonTree tree) {
		return new SyntaxException(message, newSource(tree));
	}

	private SyntaxException error(String message, ASTNode node) {
		return new SyntaxException(message, node.getSource());
	}

	private Source newSource(CommonTree tree) {
		return parseTree.source(tree);
	}

	private SpecifierAnalysis newSpecifierAnalysis(CommonTree specifiers) throws SyntaxException {
		return new SpecifierAnalysis(specifiers, parseTree, this.config);
	}

	private boolean isFunction(TypeNode type, SimpleScope scope) throws SyntaxException {
		return isFunction(type, scope, new HashSet<String>());
	}

	private boolean isFunction(TypeNode type, SimpleScope scope, Set<String> seenNames) throws SyntaxException {
		TypeNodeKind kind = type.kind();

		switch (kind) {
		case FUNCTION:
			return true;
		case TYPEDEF_NAME: {
			String typeName = ((TypedefNameNode) type).getName().name();
			TypeNode referencedNode = scope.getReferencedType(typeName);

			if (seenNames.contains(typeName))
				throw error("Cycle in typedefs", type);
			while (referencedNode == null) {
				scope = scope.getParent();
				if (scope == null)
					throw error("Could not resolve typedef name", type);
				referencedNode = scope.getReferencedType(typeName);
			}
			seenNames.add(typeName);
			return isFunction(referencedNode, scope, seenNames);
		}
		default:
			return false;
		}
	}

	// Translation of Misc. Primitives...

	private IdentifierNode translateIdentifier(CommonTree identifier) {
		Token idToken = identifier.getToken();
		CivlcToken token;
		Source source;

		if (idToken instanceof CivlcToken)
			token = (CivlcToken) idToken;
		else
			token = tokenFactory.newCivlcToken(idToken, null, TokenVocabulary.CIVLC);

		source = tokenFactory.newSource(token);
		return nodeFactory.newIdentifierNode(source, token.getText());
	}

	// private SequenceNode<VariableDeclarationNode> translateScopeListDef(
	// CommonTree list) {
	// int kind = list.getType();
	//
	// if (kind == ABSENT) {
	// return null;
	// } else {
	// int numChildren = list.getChildCount();
	// LinkedList<VariableDeclarationNode> nodeList = new
	// LinkedList<VariableDeclarationNode>();
	// SequenceNode<VariableDeclarationNode> result;
	// Source source = newSource(list);
	//
	// for (int i = 0; i < numChildren; i++) {
	// CommonTree child = (CommonTree) list.getChild(i);
	// IdentifierNode identifierNode = translateIdentifier(child);
	// Source childSource = identifierNode.getSource();
	// TypeNode typeNode = nodeFactory.newScopeTypeNode(childSource);
	// VariableDeclarationNode declNode = nodeFactory
	// .newVariableDeclarationNode(childSource,
	// identifierNode, typeNode);
	//
	// nodeList.add(declNode);
	// }
	// result = nodeFactory.newSequenceNode(source, "scope list def",
	// nodeList);
	// return result;
	// }
	// }

	// private SequenceNode<ExpressionNode> translateScopeListUse(CommonTree
	// list) {
	// int kind = list.getType();
	//
	// if (kind == ABSENT) {
	// return null;
	// } else {
	// int numChildren = list.getChildCount();
	// LinkedList<ExpressionNode> nodeList = new LinkedList<ExpressionNode>();
	// SequenceNode<ExpressionNode> result;
	// Source source = newSource(list);
	//
	// for (int i = 0; i < numChildren; i++) {
	// CommonTree child = (CommonTree) list.getChild(i);
	// IdentifierNode identifierNode = translateIdentifier(child);
	// Source childSource = identifierNode.getSource();
	// IdentifierExpressionNode exprNode = nodeFactory
	// .newIdentifierExpressionNode(childSource,
	// identifierNode);
	//
	// nodeList.add(exprNode);
	// }
	// result = nodeFactory.newSequenceNode(source, "scope list use",
	// nodeList);
	// return result;
	// }
	// }

	// Translation of Expressions...

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

	// removed from grammar, using macros instead:
	// private ConstantNode translateTrue(Source source) {
	// return nodeFactory.newBooleanConstantNode(source, true);
	// }
	//
	// private ConstantNode translateFalse(Source source) {
	// return nodeFactory.newBooleanConstantNode(source, false);
	// }

	private StringLiteralNode translateStringLiteral(Source source, CommonTree stringLiteral) throws SyntaxException {
		StringToken token = (StringToken) stringLiteral.getToken();

		return nodeFactory.newStringLiteralNode(source, stringLiteral.getText(), token.getStringLiteral());
	}

	private ExpressionNode translateRegularRange(Source source, CommonTree expressionTree, SimpleScope scope)
			throws SyntaxException {
		{// regular range expression lo..hi or lo..hi#step
			ExpressionNode loNode = translateExpression((CommonTree) expressionTree.getChild(0), scope);
			ExpressionNode hiNode = translateExpression((CommonTree) expressionTree.getChild(1), scope);
			if (expressionTree.getChildCount() > 2) {
				CommonTree stepTree = (CommonTree) expressionTree.getChild(2);

				if (stepTree != null && stepTree.getType() != ABSENT) {
					ExpressionNode stepNode = translateExpression(stepTree, scope);

					return nodeFactory.newRegularRangeNode(source, loNode, hiNode, stepNode);
				}
			}
			return nodeFactory.newRegularRangeNode(source, loNode, hiNode);
		}
	}

	private ExpressionNode translateScopeOf(Source source, CommonTree expressionTree, SimpleScope scope)
			throws SyntaxException {
		ExpressionNode expression = this.translateExpression((CommonTree) expressionTree.getChild(0), scope);

		return nodeFactory.newScopeOfNode(source, expression);
	}

	private GenericSelectionNode translateGenericSelection(Source source, CommonTree genericSelectionTree,
			SimpleScope scope) throws SyntaxException {
		ExpressionNode controllingExpression = translateExpression((CommonTree) genericSelectionTree.getChild(0),
				scope);
		CommonTree genericAssocListTree = (CommonTree) genericSelectionTree.getChild(1);
		ExpressionNode defaultExpression = null;
		List<GenericAssociationNode> genericAssocList = new LinkedList<GenericAssociationNode>();

		for (Object assocObj : genericAssocListTree.getChildren()) {
			CommonTree assocTree = (CommonTree) assocObj;
			CommonTree label = (CommonTree) assocTree.getChild(0);
			ExpressionNode assocExpr = translateExpression((CommonTree) assocTree.getChild(1), scope);

			if (label.getType() == DEFAULT) {
				if (defaultExpression != null) {
					throw new SyntaxException(
							"Generic selection can only contain one default association but found at least 2", source);
				}

				defaultExpression = assocExpr;
			} else {
				TypeNode typeLabel = translateTypeName(label, scope);

				genericAssocList.add(nodeFactory.newGenericAssociationNode(source, typeLabel, assocExpr));
			}
		}

		return nodeFactory.newGenericSelectionNode(source, controllingExpression, defaultExpression,
				nodeFactory.newSequenceNode(source, "GenericAssociationList", genericAssocList));
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
		CommonTree functionTree = (CommonTree) callTree.getChild(1);
		CommonTree contextArgumentListTree = (CommonTree) callTree.getChild(2);
		CommonTree argumentListTree = (CommonTree) callTree.getChild(3);
		ExpressionNode functionNode = translateExpression(functionTree, scope);
		int numContextArgs = contextArgumentListTree.getChildCount();
		int numArgs = argumentListTree.getChildCount();
		List<ExpressionNode> contextArgumentList = new LinkedList<ExpressionNode>();
		List<ExpressionNode> argumentList = new LinkedList<ExpressionNode>();
		// SequenceNode<ExpressionNode> scopeList =
		// translateScopeListUse((CommonTree) callTree
		// .getChild(4));

		for (int i = 0; i < numContextArgs; i++) {
			CommonTree argumentTree = (CommonTree) contextArgumentListTree.getChild(i);
			ExpressionNode contextArgumentNode = translateExpression(argumentTree, scope);

			contextArgumentList.add(contextArgumentNode);
		}

		for (int i = 0; i < numArgs; i++) {
			CommonTree argumentTree = (CommonTree) argumentListTree.getChild(i);
			ExpressionNode argumentNode = translateExpression(argumentTree, scope);

			argumentList.add(argumentNode);
		}
		return nodeFactory.newContextFunctionCallNode(source, functionNode, contextArgumentList, argumentList);
	}

	/**
	 * 
	 * @param compoundLiteralTree
	 * @return
	 * @throws SyntaxException
	 */
	private CompoundLiteralNode translateCompoundLiteral(Source source, CommonTree compoundLiteralTree,
			SimpleScope scope) throws SyntaxException {
		// tree has form:
		// ^(COMPOUND_LITERAL LPAREN typeName initializerList RCURLY)
		TypeNode typeNode = translateTypeName((CommonTree) compoundLiteralTree.getChild(1), scope);
		CompoundInitializerNode initializerList = (CompoundInitializerNode) translateInitializer(
				(CommonTree) compoundLiteralTree.getChild(2), scope);

		return nodeFactory.newCompoundLiteralNode(source, typeNode, initializerList);
	}

	/**
	 * 
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
	 * 
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
		case BIG_O:
			operator = Operator.BIG_O;
			break;
		case BITANDEQ:
			operator = Operator.BITANDEQ;
			break;
		case TILDE:
			operator = Operator.BITCOMPLEMENT;
			break;
		case BITOR:
			operator = Operator.BITOR;
			break;
		case BITOREQ:
			operator = Operator.BITOREQ;
			break;
		case BITXOR:
			operator = Operator.BITXOR;
			break;
		case BITXOREQ:
			operator = Operator.BITXOREQ;
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
		case DIVEQ:
			operator = Operator.DIVEQ;
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
		case AND:
			operator = Operator.LAND;
			break;
		case OR:
			operator = Operator.LOR;
			break;
		case IMPLIES:
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
		case SUBEQ:
			operator = Operator.MINUSEQ;
			break;
		case MOD:
			operator = Operator.MOD;
			break;
		case MODEQ:
			operator = Operator.MODEQ;
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
		case PLUSEQ:
			operator = Operator.PLUSEQ;
			break;
		case POST_DECREMENT:
			operator = Operator.POSTDECREMENT;
			break;
		case POST_INCREMENT:
			operator = Operator.POSTINCREMENT;
			break;
		case PRE_DECREMENT:
			operator = Operator.PREDECREMENT;
			break;
		case PRE_INCREMENT:
			operator = Operator.PREINCREMENT;
			break;
		case SHIFTLEFT:
			operator = Operator.SHIFTLEFT;
			break;
		case SHIFTLEFTEQ:
			operator = Operator.SHIFTLEFTEQ;
			break;
		case SHIFTRIGHT:
			operator = Operator.SHIFTRIGHT;
			break;
		case SHIFTRIGHTEQ:
			operator = Operator.SHIFTRIGHTEQ;
			break;
		case INDEX:
			operator = Operator.SUBSCRIPT;
			break;
		case STAREQ:
			operator = Operator.TIMESEQ;
			break;
		case HASH:
			operator = Operator.HASH;
			break;
		default:
			throw error("Unknown operator :", operatorTree);
		}
		return nodeFactory.newOperatorNode(source, operator, arguments);
	}

	/**
	 * 
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
			sizeable = translateTypeName(child, scope);
		else
			throw error("Unexpected argument to sizeof", expressionTree);
		return nodeFactory.newSizeofNode(source, sizeable);
	}

	/**
	 * 
	 * @param source
	 * @param expressionTree has the format: (, COMPOUND_STATEMENT, )
	 * @param scope
	 * @return
	 * @throws SyntaxException
	 */
	private StatementExpressionNode translateStatementExpression(Source source, CommonTree expressionTree,
			SimpleScope scope) throws SyntaxException {
		CompoundStatementNode statement = this.translateCompoundStatement((CommonTree) expressionTree.getChild(1),
				scope);
		BlockItemNode last = statement.getSequenceChild(statement.numChildren() - 1);

		if (!(last instanceof ExpressionStatementNode)) {
			this.error("the last element of the compound statement for a "
					+ "statement expression should only be an expression", expressionTree);
		}
		return this.nodeFactory.newStatementExpressionNode(source, statement);
	}

	/**
	 * Translates an expression.
	 * 
	 * @param expressionTree any CommonTree node representing an expression
	 * @return an ExpressionNode
	 * @throws SyntaxException
	 */
	private ExpressionNode translateExpression(Source source, CommonTree expressionTree, SimpleScope scope)
			throws SyntaxException {
		int kind = expressionTree.getType();

		switch (kind) {
		case INTEGER_CONSTANT:
			return translateIntegerConstant(source, expressionTree);
		case FLOATING_CONSTANT:
			return translateFloatingConstant(source, expressionTree);
		case ENUMERATION_CONSTANT:
			return nodeFactory.newEnumerationConstantNode(translateIdentifier((CommonTree) expressionTree.getChild(0)));
		case CHARACTER_CONSTANT:
			return translateCharacterConstant(source, expressionTree);
		case STRING_LITERAL:
			return translateStringLiteral(source, expressionTree);
		case IDENTIFIER:
			return nodeFactory.newIdentifierExpressionNode(source, translateIdentifier(expressionTree));
		case LAMBDA:
			return translateArrayLambdaExpression(source, expressionTree, scope);
		case PARENTHESIZED_EXPRESSION:
			return translateExpression(source, (CommonTree) expressionTree.getChild(1), scope);
		case GENERIC:
			return translateGenericSelection(source, expressionTree, scope);
		case CALL:
			return translateCall(source, expressionTree, scope);
		case DOT:
		case ARROW:
			return translateDotOrArrow(source, expressionTree, scope);
		case COMPOUND_LITERAL:
			return translateCompoundLiteral(source, expressionTree, scope);
		case OPERATOR:
			return translateOperatorExpression(source, expressionTree, scope);
		case SIZEOF:
			return translateSizeOf(source, expressionTree, scope);
		case SCOPEOF:
			return translateScopeOf(source, expressionTree, scope);
		case ALIGNOF:
			return nodeFactory.newAlignOfNode(source,
					translateTypeName((CommonTree) expressionTree.getChild(0), scope));
		case CAST:
			return nodeFactory.newCastNode(source, translateTypeName((CommonTree) expressionTree.getChild(0), scope),
					translateExpression((CommonTree) expressionTree.getChild(1), scope));
		case SELF:
			return nodeFactory.newSelfNode(source);
		case PROCNULL:
			return nodeFactory.newProcnullNode(source);
		case HERE:
			return nodeFactory.newHereNode(source);
		case SPAWN:
			return nodeFactory.newSpawnNode(source, translateCall(source, expressionTree, scope));
		case RESULT:
			return nodeFactory.newResultNode(source);
		case QUANTIFIED:
			return translateQuantifiedExpressionNew(source, expressionTree, scope);
		case DOTDOT:
			return translateRegularRange(source, expressionTree, scope);
		case ELLIPSIS:
			return translateWildcard(source, expressionTree, scope);
		case STATEMENT_EXPRESSION:
			return translateStatementExpression(source, expressionTree, scope);
		case SUM:
			return translateSumExpression(source, expressionTree, scope);
		default:
			throw error("Unknown expression kind", expressionTree);
		} // end switch
	}

	private ExtendedQuantifiedExpressionNode translateSumExpression(Source source, CommonTree sumTree,
			SimpleScope scope) throws SyntaxException {
		SimpleScope newScope = new SimpleScope(scope);
		CommonTree boundVarDeclListTree = (CommonTree) sumTree.getChild(0);
		CommonTree bodyTree = (CommonTree) sumTree.getChild(1);

		if (boundVarDeclListTree.getChildCount() != 1)
			throw error("$sum requires exactly one bound variable declaration", sumTree);

		CommonTree boundVarDeclTree = (CommonTree) boundVarDeclListTree.getChild(0);
		CommonTree typeTree = (CommonTree) boundVarDeclTree.getChild(0);
		CommonTree namesTree = (CommonTree) boundVarDeclTree.getChild(1);
		CommonTree domainTree = (CommonTree) boundVarDeclTree.getChild(2);

		if (namesTree.getChildCount() != 1)
			throw error("$sum requires exactly one bound variable", sumTree);
		if (domainTree == null || domainTree.getType() != DOTDOT)
			throw error("$sum requires a range domain (lo .. hi)", sumTree);

		TypeNode type = translateTypeName(typeTree, scope);
		IdentifierNode identNode = translateIdentifier((CommonTree) namesTree.getChild(0));
		VariableDeclarationNode varDecl = nodeFactory.newVariableDeclarationNode(source, identNode, type);
		ExpressionNode lo = translateExpression((CommonTree) domainTree.getChild(0), scope);
		ExpressionNode hi = translateExpression((CommonTree) domainTree.getChild(1), scope);
		ExpressionNode body = translateExpression(bodyTree, newScope);
		LambdaNode lambda = nodeFactory.newLambdaNode(source, varDecl, body);

		return nodeFactory.newExtendedQuantifiedExpressionNode(source, ExtendedQuantifier.SUM, lo, hi, lambda);
	}

	private ArrayLambdaNode translateArrayLambdaExpression(Source source, CommonTree arrayLambdaTree, SimpleScope scope)
			throws SyntaxException {
		SimpleScope newScope = new SimpleScope(scope);
		CommonTree typeTree = (CommonTree) arrayLambdaTree.getChild(0);
		CommonTree boundVariableDeclListTree = (CommonTree) arrayLambdaTree.getChild(1);
		CommonTree bodyTree = (CommonTree) arrayLambdaTree.getChild(2);
		CommonTree restrictionTree = (CommonTree) arrayLambdaTree.getChild(3);
		TypeNode type = this.translateTypeName(typeTree, scope);
		SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableDeclListNode = this
				.translateBoundVariableDeclarationList(source, boundVariableDeclListTree, newScope);
		ExpressionNode restriction = null, expression;

		if (restrictionTree != null)
			restriction = this.translateExpression(restrictionTree, newScope);
		expression = this.translateExpression(bodyTree, newScope);
		return nodeFactory.newArrayLambdaNode(source, type, boundVariableDeclListNode, restriction, expression);
	}

	private SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> translateBoundVariableDeclarationList(
			Source source, CommonTree boundVariableDeclListTree, SimpleScope scope) throws SyntaxException {
		List<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableDeclarationLists = new LinkedList<>();

		for (Object varObj : boundVariableDeclListTree.getChildren()) {
			boundVariableDeclarationLists
					.add(this.translateBoundVariableDeclarationSubList(source, (CommonTree) varObj, scope));
		}
		return nodeFactory.newSequenceNode(source, "bound variable declaration sub-list",
				boundVariableDeclarationLists);
	}

	/**
	 * Translates an interval of real numbers expressed as [a,b], where a and b are
	 * expressions.
	 * 
	 * @param intervalTree the ANTLR tree node for the interval, which has 2
	 *                     children, one for a and one for b
	 * @param scope        the scope in which this interval occurs
	 * @return a new {@link PairNode} consisting of an expression node for the left
	 *         endpoint and an expression node for the right end point of the
	 *         interval
	 * @throws SyntaxException if either or both expressions contain syntax errors
	 */
	private PairNode<ExpressionNode, ExpressionNode> translateInterval(CommonTree intervalTree, SimpleScope scope)
			throws SyntaxException {
		ExpressionNode left = translateExpression((CommonTree) intervalTree.getChild(0), scope);
		ExpressionNode right = translateExpression((CommonTree) intervalTree.getChild(1), scope);

		return nodeFactory.newPairNode(newSource(intervalTree), left, right);
	}

	/**
	 * Translates an ANTLR tree representing a sequence of real intervals: [a1,b1]
	 * [a2,b2] ... [an,bn]. These are used in the $uniform expression.
	 * 
	 * @param intervalSequenceTree ANTLR tree representing interval sequence
	 * @param scope                the scope in which the $uniform expression occurs
	 * @return a new sequence node consisting of the result of translating the
	 *         intervals
	 * @throws SyntaxException if any interval contains a syntax error
	 */
	private SequenceNode<PairNode<ExpressionNode, ExpressionNode>> translateIntervalSequence(
			CommonTree intervalSequenceTree, SimpleScope scope) throws SyntaxException {
		int n = intervalSequenceTree.getChildCount();
		List<PairNode<ExpressionNode, ExpressionNode>> intervalList = new LinkedList<>();

		for (int i = 0; i < n; i++) {
			CommonTree intervalTree = (CommonTree) intervalSequenceTree.getChild(i);
			PairNode<ExpressionNode, ExpressionNode> interval = translateInterval(intervalTree, scope);

			intervalList.add(interval);
		}
		return nodeFactory.newSequenceNode(newSource(intervalSequenceTree), "IntervalSequence", intervalList);
	}

	/**
	 * Translates an ANTLR tree representing a quantified expression, i.e., an
	 * expression beginning with one of $exists, $forall, or $uniform.
	 * 
	 * @param source         the source for this expression
	 * @param quantifiedTree the ANTLR tree representing the entire quantified
	 *                       expression
	 * @param scope          the scope in which the expression occurs
	 * @return the new node which roots the new AST tree
	 * @throws SyntaxException if any syntax error occurs in the tree
	 */
	private QuantifiedExpressionNode translateQuantifiedExpressionNew(Source source, CommonTree quantifiedTree,
			SimpleScope scope) throws SyntaxException {
		SimpleScope newScope = new SimpleScope(scope);
		CommonTree quantifierTree = (CommonTree) quantifiedTree.getChild(0);
		CommonTree boundVariableDeclListTree = (CommonTree) quantifiedTree.getChild(1);
		CommonTree bodyTree = (CommonTree) quantifiedTree.getChild(2);
		CommonTree restrictionTree = (CommonTree) quantifiedTree.getChild(3);
		Quantifier quantifier = translateQuantifier(quantifierTree);
		ExpressionNode restrict = null, body;
		SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableDeclListNode = this
				.translateBoundVariableDeclarationList(source, boundVariableDeclListTree, newScope);

		if (restrictionTree != null && restrictionTree.getToken().getType() != ABSENT)
			restrict = this.translateExpression(source, restrictionTree, newScope);
		body = this.translateExpression(bodyTree, newScope);
		return nodeFactory.newQuantifiedExpressionNode(source, quantifier, boundVariableDeclListNode, restrict, body);
	}

	private PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode> translateBoundVariableDeclarationSubList(
			Source source, CommonTree boundVariableListTree, SimpleScope scope) throws SyntaxException {
		CommonTree typeTree = (CommonTree) boundVariableListTree.getChild(0);
		CommonTree namesTree = (CommonTree) boundVariableListTree.getChild(1);
		CommonTree domainTree = (CommonTree) boundVariableListTree.getChild(2);
		List<VariableDeclarationNode> variableList = new LinkedList<>();
		SequenceNode<VariableDeclarationNode> variablesNode;
		ExpressionNode domainNode = null;
		TypeNode type = translateTypeName(typeTree, scope);

		for (Object varObj : namesTree.getChildren()) {
			IdentifierNode variableName = this.translateIdentifier((CommonTree) varObj);

			variableList.add(this.nodeFactory.newVariableDeclarationNode(source, variableName, type.copy()));
		}
		variablesNode = this.nodeFactory.newSequenceNode(source, "bound variable declarations", variableList);
		if (domainTree != null)
			domainNode = this.translateExpression(domainTree, scope);
		return this.nodeFactory.newPairNode(source, variablesNode, domainNode);
	}

	private Quantifier translateQuantifier(CommonTree quantifierTree) throws SyntaxException {
		switch (quantifierTree.getType()) {
		case FORALL:
			return Quantifier.FORALL;
		case EXISTS:
			return Quantifier.EXISTS;
		default:
			throw this.error("unknown quantifier", quantifierTree);
		}
	}

	private ExpressionNode translateWildcard(Source source, CommonTree expressionTree, SimpleScope scope)
			throws SyntaxException {
		return nodeFactory.newWildcardNode(source);
	}

	/**
	 * If typeNode is a struct, union, or enumeration type node, make it in
	 * complete, i.e., delete the "body" (list of fields, or enumerators) if it is
	 * present. Otherwise, a no-op.
	 * 
	 * @param typeNode any type node
	 */
	private TypeNode makeIncomplete(TypeNode typeNode) {
		if (typeNode instanceof StructureOrUnionTypeNode) {
			((StructureOrUnionTypeNode) typeNode).makeIncomplete();
		} else if (typeNode instanceof EnumerationTypeNode) {
			((EnumerationTypeNode) typeNode).makeIncomplete();
		}
		return typeNode;
	}

	// private VariableDeclarationNode translateScopeDeclaration(
	// CommonTree scopeDeclTree, SimpleScope scope) throws SyntaxException {
	// CommonTree identifierTree = (CommonTree) scopeDeclTree.getChild(0);
	// IdentifierNode identifierNode = translateIdentifier(identifierTree);
	// Source source = newSource(scopeDeclTree);
	// TypeNode scopeType = nodeFactory.newScopeTypeNode(source);
	// VariableDeclarationNode result = nodeFactory
	// .newVariableDeclarationNode(source, identifierNode, scopeType);
	//
	// return result;
	// }

	/**
	 * Returns a list consisting of the following kinds of external definitions:
	 * 
	 * <ul>
	 * <li>VariableDeclarationNode</li>
	 * <li>FunctionDeclarationNode</li> (includes FunctionDefinitionNode)
	 * <li>StructureOrUnionTypeNode</li>
	 * <li>EnumerationTypeNode</li>
	 * <li>TypedefDeclarationNode</li>
	 * </ul>
	 * 
	 * @param declarationTree CommonTree node of type DECLARATION (not static
	 *                        assertions)
	 * @return list of external definitions
	 * @throws SyntaxException if the declaration does not conform to the C11
	 *                         Standard
	 */
	private List<BlockItemNode> translateDeclaration(CommonTree declarationTree, SimpleScope scope)
			throws SyntaxException {
		CommonTree declarationSpecifiers = (CommonTree) declarationTree.getChild(0);
		CommonTree initDeclaratorList = (CommonTree) declarationTree.getChild(1);
		SpecifierAnalysis analysis = newSpecifierAnalysis(declarationSpecifiers);
		int numDeclarators = initDeclaratorList.getChildCount();
		ArrayList<BlockItemNode> definitionList = new ArrayList<BlockItemNode>();
		Source source = newSource(declarationTree);

		if (numDeclarators == 0) {
			TypeNode baseType;
			BlockItemNode definition;

			// C11 Sec. 6.7 Constraint 2:
			// "A declaration other than a static_assert declaration shall
			// declare at least a declarator (other than the parameters of a
			// function or the members of a structure or union), a tag, or the
			// members of an enumeration."

			// error if $input or $output occur here:
			if (analysis.inputQualifier || analysis.outputQualifier)
				throw error("Use of $input or $output without variable", declarationTree);
			baseType = newSpecifierType(analysis, scope);
			if (baseType instanceof EnumerationTypeNode)
				definition = (EnumerationTypeNode) baseType;
			else if (baseType instanceof StructureOrUnionTypeNode)
				definition = (StructureOrUnionTypeNode) baseType;
			else
				throw error("Declaration missing declarator", declarationTree);
			definitionList.add(definition);
			return definitionList;
		}
		for (int i = 0; i < numDeclarators; i++) {
			CommonTree initDeclarator = (CommonTree) initDeclaratorList.getChild(i);
			CommonTree declaratorTree = (CommonTree) initDeclarator.getChild(0);
			CommonTree initializerTree = (CommonTree) initDeclarator.getChild(1);
			InitializerNode initializer = translateInitializer(initializerTree, scope);
			TypeNode baseType = i == 0 ? newSpecifierType(analysis, scope)
					: makeIncomplete(newSpecifierType(analysis, scope));
			DeclaratorData data = processDeclarator(declaratorTree, baseType, scope);
			BlockItemNode definition;

			// special handling of $input and $output qualifiers required
			// these must not go in base type but must be pulled all the
			// way out to modify the final declarator.
			// So remove them from the base type, and add them back at end.
			// $input int x, y;
			// $input const double a[n];

			if (analysis.typedefCount > 0) {
				TypeNode typeNode = data.type;
				String name;

				definition = nodeFactory.newTypedefDeclarationNode(source, data.identifier, typeNode);
				if (data.identifier == null)
					throw error("Missing identifier in typedef", declaratorTree);
				name = data.identifier.name();
				scope.putMapping(name, data.type);
			} else if (isFunction(data.type, scope)) {
				TypeNode typeNode = data.type;
				FunctionDeclarationNode declaration;

				if (analysis.abstractSpecifier) {
					int degree;
					SequenceNode<PairNode<ExpressionNode, ExpressionNode>> intervals;
					StringLiteralNode attr = null;

					if (analysis.differentiableNode != null) {
						degree = analysis.differentiableDegree;
						intervals = translateIntervalSequence((CommonTree) analysis.differentiableNode.getChild(1),
								scope);
					} else {
						degree = 0;
						intervals = null;
					}
					if (analysis.abstractAttributeNode != null)
						attr = translateStringLiteral(newSource(analysis.abstractAttributeNode),
								analysis.abstractAttributeNode);
					declaration = nodeFactory.newAbstractFunctionDefinitionNode(source, data.identifier, typeNode,
							getContract(), degree, intervals, attr);

				} else {
					declaration = nodeFactory.newFunctionDeclarationNode(source, data.identifier, typeNode,
							getContract());
					if (analysis.differentiableNode != null)
						throw error("$differentiable specifier used with non-abstract function",
								analysis.differentiableNode);
				}
				if (debug)
					System.out.println("processing function " + data.identifier.name());
				// declaration.setContract(getContract());
				setFunctionSpecifiers(declaration, analysis);
				setStorageSpecifiers(declaration, analysis);
				if (initializer != null)
					throw error("Initializer used in function declaration", initializerTree);
				checkAlignmentSpecifiers(declaration, analysis);
				definition = declaration;
			} else {
				VariableDeclarationNode declaration;

				if (analysis.inputQualifier)
					data.type.setInputQualified(true);
				if (analysis.outputQualifier)
					data.type.setOutputQualified(true);
				declaration = nodeFactory.newVariableDeclarationNode(source, data.identifier, data.type);
				if (initializer != null)
					declaration.setInitializer(initializer);
				setStorageSpecifiers(declaration, analysis, scope);
				setAlignmentSpecifiers(declaration, analysis, scope);
				checkFunctionSpecifiers(declaration, analysis);
				definition = declaration;
			}
			definitionList.add(definition);
		}
		return definitionList;
	}

	/**
	 * Creates a new type node based on the result of analyzing a set of type
	 * specifiers.
	 * 
	 * Input and output specifiers are ignored, since these require special
	 * handling: they must be pulled all the way up to the final type node for the
	 * variable being declared.
	 * 
	 * @param analysis
	 * @param scope
	 * @return
	 * @throws SyntaxException
	 */
	private TypeNode newSpecifierType(SpecifierAnalysis analysis, SimpleScope scope) throws SyntaxException {
		TypeNode result;

		switch (analysis.typeNameKind) {
		case VOID:
			result = nodeFactory.newVoidTypeNode(newSource(analysis.typeSpecifierNode));
			break;
		case BASIC: {
			Source source;

			if (analysis.specifierListNode.getChildCount() == 0)
				source = this.tokenFactory
						.newSource(tokenFactory.newCivlcToken(IDENTIFIER, analysis.basicTypeKind.toString(),
								tokenFactory.newSystemFormation("system"), TokenVocabulary.CIVLC));
			else
				source = newSource(analysis.specifierListNode);
			result = nodeFactory.newBasicTypeNode(source, analysis.basicTypeKind);
			break;
		}
		case TYPEDEF_NAME: {
			CommonTree typedefNameTree = (CommonTree) analysis.typeSpecifierNode;
			CommonTree identifierTree = (CommonTree) typedefNameTree.getChild(0);
			IdentifierNode identifierNode = translateIdentifier(identifierTree);

			result = nodeFactory.newTypedefNameNode(identifierNode);
			// special handling for CIVL-C built-in types that are defined
			// using typedefs, i.e. $state, $mem:
			if (((TypedefNameNode) result).getName().name().equals(CIVLC_MEM_TYPEDEF_NAME))
				result = nodeFactory.newMemTypeNode(identifierNode.getSource());
			break;
		}
		case TYPEOF: {
			CommonTree typeofTree = (CommonTree) analysis.typeSpecifierNode;
			CommonTree operandTree = (CommonTree) typeofTree.getChild(1);

			if (typeofTree.getType() == TYPEOF_TYPE) {
				result = this.translateTypeName(operandTree, scope);
			} else {
				Source source;
				ExpressionNode expression = this.translateExpression(operandTree, scope);

				if (analysis.specifierListNode.getChildCount() == 0)
					source = this.tokenFactory
							.newSource(tokenFactory.newCivlcToken(IDENTIFIER, analysis.basicTypeKind.toString(),
									tokenFactory.newSystemFormation("system"), TokenVocabulary.CIVLC));
				else
					source = newSource(analysis.specifierListNode);
				result = this.nodeFactory.newTypeofNode(source, expression);
			}
			break;
		}
		case STRUCTURE_OR_UNION:
			result = translateStructOrUnionType(analysis.typeSpecifierNode, scope);
			break;
		case ENUMERATION:
			result = translateEnumerationType(analysis.typeSpecifierNode, scope);
			break;
		case ATOMIC:
			result = translateAtomicType(analysis.typeSpecifierNode, scope);
			break;
		case DOMAIN: {
			CommonTree node = analysis.typeSpecifierNode;
			Source source = newSource(node);

			if (node.getChildCount() != 0) {
				CommonTree child = (CommonTree) node.getChild(0);

				if (child.getToken().getType() != ABSENT) {
					ExpressionNode dimensionNode = translateExpression(child, scope);

					result = nodeFactory.newDomainTypeNode(source, dimensionNode);
					break;
				}
			}
			result = nodeFactory.newDomainTypeNode(source);
			break;
		}
		case RANGE:
			result = nodeFactory.newRangeTypeNode(newSource(analysis.typeSpecifierNode));
			break;
		case MEM:
			result = nodeFactory.newMemTypeNode(newSource(analysis.typeSpecifierNode));
			break;
		default:
			throw new RuntimeException("Should not happen.");
		}
		if (analysis.constQualifier)
			result.setConstQualified(true);
		if (analysis.volatileQualifier)
			result.setVolatileQualified(true);
		if (analysis.restrictQualifier)
			result.setRestrictQualified(true);
		if (analysis.atomicQualifier)
			result.setAtomicQualified(true);
		return result;
	}

	/**
	 * 
	 * @param structTree CommonTree of type STRUCT or UNION
	 * @return
	 * @throws SyntaxException
	 */
	private StructureOrUnionTypeNode translateStructOrUnionType(CommonTree structTree, SimpleScope scope)
			throws SyntaxException {
		Source wholeSource = newSource(structTree);
		int kind = structTree.getType();
		boolean isStruct = kind == STRUCT;
		CommonTree tagTree = (CommonTree) structTree.getChild(0);
		CommonTree declListTree = (CommonTree) structTree.getChild(1);
		IdentifierNode tag;
		SequenceNode<FieldDeclarationNode> structDeclList;

		if (tagTree.getType() == ABSENT) {
			tag = nodeFactory.newIdentifierNode(wholeSource,
					"$anon_" + (isStruct ? "struct" : "union") + "_" + anonymousTagCount);
			anonymousTagCount++;
		} else {
			tag = translateIdentifier(tagTree);
		}
		if (declListTree.getType() == ABSENT) {
			structDeclList = null;
		} else {
			int numFields = declListTree.getChildCount();

			if (numFields > 0) {
				List<FieldDeclarationNode> fieldDecls = new LinkedList<FieldDeclarationNode>();

				for (int i = 0; i < numFields; i++) {
					CommonTree declTree = (CommonTree) declListTree.getChild(i);
					List<FieldDeclarationNode> fieldDeclarations = translateFieldDeclaration(declTree, scope);

					fieldDecls.addAll(fieldDeclarations);
				}
				structDeclList = nodeFactory.newSequenceNode(newSource(declListTree), "FieldDeclarations", fieldDecls);
			} else {
				throw this.error("empty struct is not allowed", structTree);
			}
		}

		StructureOrUnionTypeNode result = nodeFactory.newStructOrUnionTypeNode(wholeSource, isStruct, tag,
				structDeclList);

		return result;
	}

	private List<FieldDeclarationNode> translateFieldDeclaration(CommonTree declarationTree, SimpleScope scope)
			throws SyntaxException {
		CommonTree declarationSpecifiers = (CommonTree) declarationTree.getChild(0); // may be ABSENT
		CommonTree structDeclaratorList = (CommonTree) declarationTree.getChild(1); // may be ABSENT
		SpecifierAnalysis analysis = newSpecifierAnalysis(declarationSpecifiers);
		int numDeclarators = structDeclaratorList.getChildCount();
		List<FieldDeclarationNode> result = new LinkedList<FieldDeclarationNode>();
		Source source = newSource(declarationTree);

		if (numDeclarators == 0) {
			// this can happen if the specifier is an anonymous struct or union
			TypeNode baseType = newSpecifierType(analysis, scope);

			// TODO: consider given a name to this anonymous field
			// and then updating DOT and ARROW expressions which refer to it
			result.add(nodeFactory.newFieldDeclarationNode(source, null, baseType, null));
		} else {
			for (int i = 0; i < numDeclarators; i++) {
				TypeNode baseType = i == 0 ? newSpecifierType(analysis, scope)
						: makeIncomplete(newSpecifierType(analysis, scope));
				CommonTree structDeclarator = (CommonTree) structDeclaratorList.getChild(i);
				CommonTree declaratorTree = (CommonTree) structDeclarator.getChild(0); // could be ABSENT
				CommonTree bitFieldTree = (CommonTree) structDeclarator.getChild(1); // could be ABSENT
				ExpressionNode bitFieldWidth = translateExpression(bitFieldTree, scope);
				DeclaratorData data = processDeclarator(declaratorTree, baseType, scope);
				FieldDeclarationNode declaration;

				if (bitFieldWidth == null)
					declaration = nodeFactory.newFieldDeclarationNode(source, data.identifier, data.type);
				else
					declaration = nodeFactory.newFieldDeclarationNode(source, data.identifier, data.type,
							bitFieldWidth);

				result.add(declaration);
			}
		}
		return result;
	}

	/**
	 * 
	 * @param enumerationTree
	 * @return
	 * @throws SyntaxException
	 */
	private EnumerationTypeNode translateEnumerationType(CommonTree enumerationTree, SimpleScope scope)
			throws SyntaxException {
		Source wholeSource = newSource(enumerationTree);
		// tagTree may be ABSENT:
		CommonTree tagTree = (CommonTree) enumerationTree.getChild(0);
		// enumeratorListTree may be ABSENT:
		CommonTree enumeratorListTree = (CommonTree) enumerationTree.getChild(1);
		IdentifierNode tag;
		SequenceNode<EnumeratorDeclarationNode> enumerators;

		if (tagTree.getType() == ABSENT) {
			tag = nodeFactory.newIdentifierNode(wholeSource, "$anon_enum_" + anonymousTagCount);
			anonymousTagCount++;
		} else {
			tag = translateIdentifier(tagTree);
		}
		if (enumeratorListTree.getType() == ABSENT) {
			enumerators = null;
		} else {
			int numEnumerators = enumeratorListTree.getChildCount();
			List<EnumeratorDeclarationNode> enumeratorList = new LinkedList<EnumeratorDeclarationNode>();

			for (int i = 0; i < numEnumerators; i++) {
				CommonTree enumeratorTree = (CommonTree) enumeratorListTree.getChild(i);
				CommonTree enumeratorNameTree = (CommonTree) enumeratorTree.getChild(0);
				IdentifierNode enumeratorName = translateIdentifier(enumeratorNameTree);
				CommonTree constantTree = (CommonTree) enumeratorTree.getChild(1);
				ExpressionNode constant = translateExpression(constantTree, scope);
				EnumeratorDeclarationNode decl = nodeFactory.newEnumeratorDeclarationNode(newSource(enumeratorTree),
						enumeratorName, constant);

				scope.addEnumerationConstant(enumeratorName.name());
				enumeratorList.add(decl);
			}
			enumerators = nodeFactory.newSequenceNode(newSource(enumeratorListTree), "EnumeratorList", enumeratorList);
		}
		return nodeFactory.newEnumerationTypeNode(wholeSource, tag, enumerators);
	}

	/**
	 * 
	 * @param atomicTree
	 * @return
	 * @throws SyntaxException
	 */
	private AtomicTypeNode translateAtomicType(CommonTree atomicTree, SimpleScope scope) throws SyntaxException {
		TypeNode type = translateTypeName((CommonTree) atomicTree.getChild(0), scope);

		return nodeFactory.newAtomicTypeNode(newSource(atomicTree), type);
	}

	/**
	 * 
	 * @param declaration
	 * @param analysis
	 */
	private void setFunctionSpecifiers(FunctionDeclarationNode declaration, SpecifierAnalysis analysis) {
		if (analysis.inlineSpecifier)
			declaration.setInlineFunctionSpecifier(true);
		if (analysis.noreturnSpecifier)
			declaration.setNoreturnFunctionSpecifier(true);
		if (analysis.globalSpecifier)
			declaration.setGlobalFunctionSpecifier(true);
		if (analysis.deviceSpecifier)
			declaration.setDeviceFunctionSpecifier(true);
		if (analysis.pureSpecifier)
			declaration.setPureFunctionSpecifier(true);
		if (analysis.fatomicSpecifier)
			declaration.setAtomicFunctionSpecifier(true);
		if (analysis.statefSpecifier)
			declaration.setStatefFunctionSpecifier(true);
		if (analysis.systemSpecifier) {
			declaration.setSystemFunctionSpecifier(true);
			if (analysis.systemLibrary != null)
				declaration.setSystemLibrary(analysis.systemLibrary);
		}
	}

	private void checkFunctionSpecifiers(VariableDeclarationNode declaration, SpecifierAnalysis analysis)
			throws SyntaxException {
		if (analysis.inlineSpecifier)
			throw error("Use of inline specifier in object delcaration", declaration);
		if (analysis.noreturnSpecifier)
			throw error("Use of _Noreturn specifier in object delcaration", declaration);
	}

	/**
	 * 
	 * @param declaration
	 * @param analysis
	 * @throws SyntaxException
	 */
	private void setAlignmentSpecifiers(VariableDeclarationNode declaration, SpecifierAnalysis analysis,
			SimpleScope scope) throws SyntaxException {
		if (!analysis.alignmentTypeNodes.isEmpty()) {
			List<TypeNode> typeAlignmentSpecifiers = new ArrayList<TypeNode>();

			for (CommonTree node : analysis.alignmentTypeNodes)
				typeAlignmentSpecifiers.add(translateTypeName(node, scope));
			declaration.setTypeAlignmentSpecifiers(nodeFactory.newSequenceNode(newSource(analysis.specifierListNode),
					"TypeAlignmentSpecifiers", typeAlignmentSpecifiers));
		}
		if (!analysis.alignmentExpressionNodes.isEmpty()) {
			List<ExpressionNode> constantAlignmentSpecifiers = new ArrayList<ExpressionNode>();

			for (CommonTree node : analysis.alignmentExpressionNodes)
				constantAlignmentSpecifiers.add(translateExpression(node, scope));

			declaration.setConstantAlignmentSpecifiers(nodeFactory.newSequenceNode(
					newSource(analysis.specifierListNode), "ConstantAlignmentSpecifiers", constantAlignmentSpecifiers));
		}
	}

	private void checkAlignmentSpecifiers(FunctionDeclarationNode declaration, SpecifierAnalysis analysis)
			throws SyntaxException {
		if (!analysis.alignmentTypeNodes.isEmpty() || !analysis.alignmentExpressionNodes.isEmpty())
			throw error("Use of alignment specifiers in function declaration", declaration);
	}

	/**
	 * 
	 * @param declaration
	 * @param analysis
	 */
	private void setStorageSpecifiers(VariableDeclarationNode declaration, SpecifierAnalysis analysis,
			SimpleScope scope) {

		if (analysis.externCount > 0)
			declaration.setExternStorage(true);
		if (analysis.staticCount > 0)
			declaration.setStaticStorage(true);
		if (analysis.threadLocalCount > 0)
			declaration.setThreadLocalStorage(true);
		if (analysis.autoCount > 0)
			declaration.setAutoStorage(true);
		if (analysis.registerCount > 0)
			declaration.setRegisterStorage(true);
		if (analysis.sharedCount > 0)
			declaration.setSharedStorage(true);
	}

	private void setStorageSpecifiers(FunctionDeclarationNode declaration, SpecifierAnalysis analysis)
			throws SyntaxException {
		if (analysis.externCount > 0)
			declaration.setExternStorage(true);
		if (analysis.staticCount > 0)
			declaration.setStaticStorage(true);
		if (analysis.threadLocalCount > 0)
			throw new SyntaxException("Use of _Thread_local in function declaration", declaration.getSource());
		if (analysis.autoCount > 0)
			throw new SyntaxException("Use of auto in function declaration", declaration.getSource());
		if (analysis.registerCount > 0)
			throw new SyntaxException("Use of register in function declaration", declaration.getSource());
		if (analysis.sharedCount > 0)
			throw new SyntaxException("Use of __shared__ in function declaration", declaration.getSource());
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
	 * 
	 * @return new DeclaratorData with type derived from given type and identifier
	 * 
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
			case IDENTIFIER:
				return new DeclaratorData(type, translateIdentifier(prefix));
			case DECLARATOR:
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
	 * 
	 * @param initializerTree
	 * @return
	 * @throws SyntaxException
	 */
	private InitializerNode translateInitializer(CommonTree initializerTree, SimpleScope scope) throws SyntaxException {
		int kind = initializerTree.getType();

		if (kind == ABSENT)
			return null;
		if (kind == SCALAR_INITIALIZER) {
			return translateExpression((CommonTree) initializerTree.getChild(0), scope);
		}
		if (kind == INITIALIZER_LIST) {
			int numInits = initializerTree.getChildCount();
			List<PairNode<DesignationNode, InitializerNode>> initList = new LinkedList<PairNode<DesignationNode, InitializerNode>>();

			for (int i = 0; i < numInits; i++) {
				CommonTree designatedInitializer = (CommonTree) initializerTree.getChild(i);
				CommonTree designation = (CommonTree) designatedInitializer.getChild(0);
				CommonTree initializer = (CommonTree) designatedInitializer.getChild(1);
				InitializerNode initializerNode = translateInitializer(initializer, scope);
				DesignationNode designationNode;

				if (designation.getType() == ABSENT) {
					designationNode = null;
				} else {
					int numDesignators = designation.getChildCount();
					List<DesignatorNode> designators = new LinkedList<DesignatorNode>();

					for (int j = 0; j < numDesignators; j++) {
						CommonTree designator = (CommonTree) designation.getChild(j);
						CommonTree designatorChild = (CommonTree) designator.getChild(0);
						int designatorKind = designator.getType();
						DesignatorNode designatorNode;
						Source designatorSource = newSource(designator);

						if (designatorKind == ARRAY_ELEMENT_DESIGNATOR) {
							designatorNode = nodeFactory.newArrayDesignatorNode(designatorSource,
									translateExpression(designatorChild, scope));
						} else if (designatorKind == FIELD_DESIGNATOR) {
							designatorNode = nodeFactory.newFieldDesignatorNode(designatorSource,
									translateIdentifier(designatorChild));
						} else {
							throw error("Unknown kind of designator", designator);
						}
						designators.add(designatorNode);
					}
					designationNode = nodeFactory.newDesignationNode(newSource(designation), designators);
				}
				initList.add(
						nodeFactory.newPairNode(newSource(designatedInitializer), designationNode, initializerNode));
			}
			return nodeFactory.newCompoundInitializerNode(newSource(initializerTree), initList);
		} else {
			throw error("Unrecognized kind of initializer", initializerTree);
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
			CommonTree qualifiers = (CommonTree) starNode.getChild(0);

			source = tokenFactory.join(source, newSource(starNode));
			type = nodeFactory.newPointerTypeNode(source, type);
			applyQualifiers(qualifiers, type);
		}
		return type;
	}

	/**
	 * Given a base type and a declarator suffix, returns the new derived type.
	 * Example: base type is "int", suffix is "[10]", returns the type "array of int
	 * of length 10".
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
		else if (kind == FUNCTION_SUFFIX)
			return translateFunctionSuffix(suffix, baseType, scope);
		else
			throw error("Unknown declarator suffix", suffix);
	}

	/**
	 * Applies the qualifiers in the given qualifier list to the given type.
	 * Modifies the type accordingly.
	 * 
	 * @param qualifierList CommonTree node which is root of list of qualifier
	 *                      nodes, or ABSENT
	 * @param type          the type to modify by applying qualifiers
	 * @throws SyntaxException if a child of the qualifierList is not a type
	 *                         qualifier
	 */
	private void applyQualifiers(CommonTree qualifierList, TypeNode type) throws SyntaxException {
		int numQualifiers = qualifierList.getChildCount();

		for (int i = 0; i < numQualifiers; i++) {
			CommonTree qualifier = (CommonTree) qualifierList.getChild(i);

			switch (qualifier.getType()) {
			case CONST:
				type.setConstQualified(true);
				break;
			case VOLATILE:
				type.setVolatileQualified(true);
				break;
			case RESTRICT:
				type.setRestrictQualified(true);
				break;
			case ATOMIC:
				type.setAtomicQualified(true);
				break;
			default:
				throw error("Unknown type qualifier", qualifier);
			}
		}
	}

	private void applyArrayQualifiers(CommonTree qualifierList, ArrayTypeNode type) throws SyntaxException {
		int numQualifiers = qualifierList.getChildCount();

		for (int i = 0; i < numQualifiers; i++) {
			CommonTree qualifier = (CommonTree) qualifierList.getChild(i);

			switch (qualifier.getType()) {
			case CONST:
				type.setConstInBrackets(true);
				break;
			case VOLATILE:
				type.setVolatileInBrackets(true);
				break;
			case RESTRICT:
				type.setRestrictInBrackets(true);
				break;
			case ATOMIC:
				type.setAtomicInBrackets(true);
				break;
			default:
				throw error("Unknown type qualifier", qualifier);
			}
		}
	}

	/**
	 * 
	 * @param suffix
	 * @param baseType
	 * @return
	 * @throws SyntaxException
	 */
	private ArrayTypeNode translateArraySuffix(CommonTree suffix, TypeNode baseType, SimpleScope scope)
			throws SyntaxException {
		CommonTree staticNode = (CommonTree) suffix.getChild(1);
		CommonTree qualifiers = (CommonTree) suffix.getChild(2);
		CommonTree extentNode = (CommonTree) suffix.getChild(3);
		int extentNodeType = extentNode.getType();
		boolean unspecifiedVariableLength = false;
		ExpressionNode extent = null;
		ArrayTypeNode result;
		Source source = tokenFactory.join(baseType.getSource(), newSource(suffix));

		switch (extentNodeType) {
		case ABSENT:
			break;
		case STAR:
			unspecifiedVariableLength = true;
			break;
		default:
			extent = translateExpression(extentNode, scope);
		}
		result = nodeFactory.newArrayTypeNode(source, baseType, extent);
		if (unspecifiedVariableLength)
			result.setUnspecifiedVariableLength(true);
		if (staticNode.getType() == STATIC)
			result.setStaticExtent(true);
		applyArrayQualifiers(qualifiers, result);
		return result;
	}

	/**
	 * 
	 * @param suffix
	 * @param baseType
	 * @return
	 * @throws SyntaxException
	 */
	private FunctionTypeNode translateFunctionSuffix(CommonTree suffix, TypeNode baseType, SimpleScope scope)
			throws SyntaxException {
		CommonTree child = (CommonTree) suffix.getChild(1);
		int childKind = child.getType();
		FunctionTypeNode result;
		Source source = tokenFactory.join(baseType.getSource(), newSource(suffix));

		if (!scope.isFunctionScope()) {
			// this is not a function definition.
			// need a "function prototype" scope...
			scope = new SimpleScope(scope);
		}
		if (childKind == PARAMETER_TYPE_LIST) {
			CommonTree parameterListNode = (CommonTree) child.getChild(0);
			CommonTree ellipsisNode = (CommonTree) child.getChild(1);
			int numParameters = parameterListNode.getChildCount();
			List<VariableDeclarationNode> parameterDeclarations = new ArrayList<VariableDeclarationNode>(numParameters);

			for (int i = 0; i < numParameters; i++) {
				CommonTree parameterDeclarationNode = (CommonTree) parameterListNode.getChild(i);
				Source parameterDeclarationSource = newSource(parameterDeclarationNode);
				CommonTree specifiers = (CommonTree) parameterDeclarationNode.getChild(0);
				SpecifierAnalysis analysis = newSpecifierAnalysis(specifiers);
				TypeNode parameterBaseType = newSpecifierType(analysis, scope);
				CommonTree declarator = (CommonTree) parameterDeclarationNode.getChild(1);
				int declaratorKind = declarator.getType();
				VariableDeclarationNode declaration;
				// TODO: do adjustments here?

				if (declaratorKind == ABSENT) {
					declaration = nodeFactory.newVariableDeclarationNode(parameterDeclarationSource, null,
							parameterBaseType);
				} else if (declaratorKind == DECLARATOR || declaratorKind == ABSTRACT_DECLARATOR) {
					DeclaratorData data = processDeclarator(declarator, parameterBaseType, scope);

					declaration = nodeFactory.newVariableDeclarationNode(parameterDeclarationSource, data.identifier,
							data.type);
				} else {
					throw error("Unknown kind of declarator", declarator);
				}
				// TODO: C11 6.7.6.3(2):
				// "The only storage-class specifier that shall occur in a
				// parameter declaration is register."
				// setFunctionSpecifiers(declaration, analysis);
				setAlignmentSpecifiers(declaration, analysis, scope);
				setStorageSpecifiers(declaration, analysis, scope);
				parameterDeclarations.add(declaration);
			}
			result = nodeFactory.newFunctionTypeNode(source, baseType, nodeFactory.newSequenceNode(
					newSource(parameterListNode), "FormalParameterList", parameterDeclarations), false);
			if (ellipsisNode.getType() == ELLIPSIS)
				result.setVariableArgs(true);
		} else if (childKind == IDENTIFIER_LIST || childKind == ABSENT) {
			int numParameters = child.getChildCount();
			List<VariableDeclarationNode> parameterDeclarations = new ArrayList<VariableDeclarationNode>(numParameters);

			for (int i = 0; i < numParameters; i++) {
				CommonTree identifierNode = (CommonTree) child.getChild(i);
				IdentifierNode identifier = translateIdentifier(identifierNode);
				Source identifierSource = newSource(identifierNode);
				VariableDeclarationNode declaration = nodeFactory.newVariableDeclarationNode(identifierSource,
						identifier, null);

				parameterDeclarations.add(declaration);
			}
			result = nodeFactory.newFunctionTypeNode(source, baseType,
					nodeFactory.newSequenceNode(source, "IdentifierList", parameterDeclarations), true);
		} else {
			throw error("Unexpected kind of function suffix", child);
		}
		return result;
	}

	/**
	 * 
	 * @param typeNameTree
	 * @return
	 * @throws SyntaxException
	 */
	private TypeNode translateTypeName(CommonTree typeNameTree, SimpleScope scope) throws SyntaxException {
		CommonTree specifiers = (CommonTree) typeNameTree.getChild(0);
		CommonTree abstractDeclarator = (CommonTree) typeNameTree.getChild(1);
		SpecifierAnalysis analysis = newSpecifierAnalysis(specifiers);
		TypeNode baseType = newSpecifierType(analysis, scope);
		DeclaratorData data = processDeclarator(abstractDeclarator, baseType, scope);

		return data.type;
	}

	// Translation of Statements...

	private LabeledStatementNode translateIdentifierLabeledStatement(CommonTree statementTree, SimpleScope scope)
			throws SyntaxException {
		Source statementSource = newSource(statementTree);
		IdentifierNode labelName = translateIdentifier((CommonTree) statementTree.getChild(0));
		StatementNode statement = translateStatement((CommonTree) statementTree.getChild(1), scope);
		OrdinaryLabelNode labelDecl = nodeFactory.newStandardLabelDeclarationNode(labelName.getSource(), labelName,
				statement);

		return nodeFactory.newLabeledStatementNode(statementSource, labelDecl, statement);
	}

	private LabeledStatementNode translateCaseLabeledStatement(CommonTree statementTree, SimpleScope scope)
			throws SyntaxException {
		Source statementSource = newSource(statementTree);
		CivlcToken caseToken = (CivlcToken) ((CommonTree) statementTree.getChild(0)).getToken();
		CommonTree expression = (CommonTree) statementTree.getChild(1);
		ExpressionNode expressionNode = translateExpression(expression, scope);
		StatementNode statement = translateStatement((CommonTree) statementTree.getChild(2), scope);
		Source expressionSource = newSource(expression);
		Source labelSource = tokenFactory.join(expressionSource, caseToken);
		SwitchLabelNode labelDecl = nodeFactory.newCaseLabelDeclarationNode(labelSource, expressionNode, statement);

		return nodeFactory.newLabeledStatementNode(statementSource, labelDecl, statement);
	}

	private LabeledStatementNode translateDefaultLabeledStatement(CommonTree statementTree, SimpleScope scope)
			throws SyntaxException {
		Source statementSource = newSource(statementTree);
		CivlcToken defaultToken = (CivlcToken) ((CommonTree) statementTree.getChild(0)).getToken();
		Source labelSource = tokenFactory.newSource(defaultToken);
		StatementNode statement = translateStatement((CommonTree) statementTree.getChild(1), scope);
		SwitchLabelNode labelDecl = nodeFactory.newDefaultLabelDeclarationNode(labelSource, statement);

		return nodeFactory.newLabeledStatementNode(statementSource, labelDecl, statement);
	}

	private StatementNode translateAtomic(CommonTree statementTree, SimpleScope scope) throws SyntaxException {
		StatementNode body = translateStatement((CommonTree) statementTree.getChild(0), new SimpleScope(scope));

		return nodeFactory.newAtomicStatementNode(newSource(statementTree), body);
	}

	private StatementNode translateGoto(CommonTree statementTree) {
		Source statementSource = newSource(statementTree);

		return nodeFactory.newGotoNode(statementSource, translateIdentifier((CommonTree) statementTree.getChild(0)));
	}

	private CivlForNode translateCivlFor(CommonTree tree, SimpleScope scope) throws SyntaxException {
		Source statementSource = newSource(tree);
		SimpleScope loopScope = getNewScope(scope);
		CommonTree typeNameTree = (CommonTree) tree.getChild(0);
		CommonTree identifierListTree = (CommonTree) tree.getChild(1);
		CommonTree domainTree = (CommonTree) tree.getChild(2);
		CommonTree bodyTree = (CommonTree) tree.getChild(3);
		// CommonTree invariantTree = (CommonTree) tree.getChild(4);
		int numVars = identifierListTree.getChildCount();
		DeclarationListNode declListNode;
		ExpressionNode domainNode = translateExpression(domainTree, loopScope);
		StatementNode bodyNode = translateStatement(bodyTree, loopScope);
		CivlForNode result;

		if (typeNameTree.getType() != ABSENT) {
			List<VariableDeclarationNode> declList = new LinkedList<>();

			for (int i = 0; i < numVars; i++) {
				CommonTree identifierTree = (CommonTree) identifierListTree.getChild(i);
				IdentifierNode identifierNode = translateIdentifier(identifierTree);
				TypeNode typeNode = translateTypeName(typeNameTree, loopScope);
				VariableDeclarationNode declNode = nodeFactory.newVariableDeclarationNode(identifierNode.getSource(),
						identifierNode, typeNode);

				declList.add(declNode);
			}
			declListNode = nodeFactory.newForLoopInitializerNode(newSource(identifierListTree), declList);
		} else
			throw error("Missing int typename in declaration.\n"
					+ "Both $for and $parfor require loop variables to be declared with integer type, as in:\n"
					+ "$for (int i1, i2, ... : domain) ...", tree);
		result = nodeFactory.newCivlForNode(statementSource, tree.getType() == PARFOR, declListNode, domainNode,
				bodyNode, getContract());
		return result;
	}

	private SimpleScope getNewScope(SimpleScope scope) {
		if (!this.annotationStack.isEmpty())
			if (this.annotationStack.peek().scope != null)
				return annotationStack.peek().scope;
		return new SimpleScope(scope);
	}

	private SequenceNode<ContractNode> getContract() {
		if (!this.annotationStack.isEmpty())
			if (this.annotationStack.peek().contract != null)
				return annotationStack.peek().contract;
		return null;
	}

	private List<TransformNode> getTransforms() {
		if (!this.annotationStack.isEmpty())
			if (this.annotationStack.peek().transforms != null)
				return annotationStack.peek().transforms;
		return null;
	}

	private void clearCurrentAnnotations() {
		if (!this.annotationStack.isEmpty()) {
			this.annotationStack.peek().scope = null;
			this.annotationStack.peek().contract = null;
			this.annotationStack.peek().transforms = null;
		}
	}

	private StatementNode translateFor(CommonTree statementTree, SimpleScope scope) throws SyntaxException {
		Source statementSource = newSource(statementTree);
		SimpleScope loopScope = getNewScope(scope);
		CommonTree initializerTree = (CommonTree) statementTree.getChild(0);
		ForLoopInitializerNode initializerNode;

		if (initializerTree.getType() == DECLARATION) {
			List<BlockItemNode> definitions = translateDeclaration(initializerTree, loopScope);
			List<VariableDeclarationNode> declarations = new LinkedList<VariableDeclarationNode>();

			for (BlockItemNode definition : definitions) {
				if (!(definition instanceof VariableDeclarationNode))
					throw error("For-loop initializer declaration "
							+ "\"shall only declare identifiers for objects having storage class auto or register.\"",
							initializerTree);
				declarations.add((VariableDeclarationNode) definition);
			}
			initializerNode = nodeFactory.newForLoopInitializerNode(statementSource, declarations);
		} else {
			initializerNode = translateExpression(initializerTree, loopScope);
		}
		return nodeFactory.newForLoopNode(statementSource, initializerNode,
				translateExpression((CommonTree) statementTree.getChild(1), loopScope),
				translateExpression((CommonTree) statementTree.getChild(2), loopScope),
				translateStatement((CommonTree) statementTree.getChild(3), new SimpleScope(loopScope)), getContract());
	}

	private StatementNode translateDo(CommonTree statementTree, SimpleScope scope) throws SyntaxException {
		Source statementSource = newSource(statementTree);
		SimpleScope loopScope = getNewScope(scope);

		return nodeFactory.newDoLoopNode(statementSource,
				translateExpression((CommonTree) statementTree.getChild(1), loopScope),
				translateStatement((CommonTree) statementTree.getChild(0), new SimpleScope(loopScope)), getContract());
	}

	private StatementNode translateWhile(CommonTree statementTree, SimpleScope scope) throws SyntaxException {
		Source statementSource = newSource(statementTree);
		SimpleScope loopScope = getNewScope(scope);

		return nodeFactory.newWhileLoopNode(statementSource,
				translateExpression((CommonTree) statementTree.getChild(0), loopScope),
				translateStatement((CommonTree) statementTree.getChild(1), new SimpleScope(loopScope)), getContract());
	}

	private StatementNode translateSwitch(CommonTree statementTree, SimpleScope scope) throws SyntaxException {
		Source statementSource = newSource(statementTree);
		CommonTree expressionTree = (CommonTree) statementTree.getChild(0);
		CommonTree bodyTree = (CommonTree) statementTree.getChild(1);
		SimpleScope switchScope = new SimpleScope(scope);
		SimpleScope bodyScope = new SimpleScope(switchScope);
		ExpressionNode expressionNode = translateExpression(expressionTree, switchScope);
		StatementNode statementNode = translateStatement(bodyTree, bodyScope);
		SwitchNode switchNode = nodeFactory.newSwitchNode(statementSource, expressionNode, statementNode);

		return switchNode;
	}

	private StatementNode translateIf(CommonTree statementTree, SimpleScope scope) throws SyntaxException {
		Source statementSource = newSource(statementTree);
		SimpleScope ifScope = new SimpleScope(scope);
		ExpressionNode condition = translateExpression((CommonTree) statementTree.getChild(0), ifScope);
		StatementNode trueBranch = translateStatement((CommonTree) statementTree.getChild(1), new SimpleScope(ifScope));
		StatementNode falseBranch = translateStatement((CommonTree) statementTree.getChild(2),
				new SimpleScope(ifScope));

		if (falseBranch == null)
			return nodeFactory.newIfNode(statementSource, condition, trueBranch);
		else
			return nodeFactory.newIfNode(statementSource, condition, trueBranch, falseBranch);
	}

	private StatementNode translateExpressionStatement(CommonTree statementTree, SimpleScope scope)
			throws SyntaxException {
		Source statementSource = newSource(statementTree);
		CommonTree expression = (CommonTree) statementTree.getChild(0);
		ExpressionNode expressionNode = translateExpression(expression, scope);

		if (expressionNode == null)
			return nodeFactory.newNullStatementNode(statementSource);
		else
			return nodeFactory.newExpressionStatementNode(expressionNode);
	}

	@SuppressWarnings("unused")
	private ExpressionNode getInvariant(CommonTree invariantTree, SimpleScope scope) throws SyntaxException {
		if (invariantTree == null)
			return null;
		if (invariantTree.getType() == ABSENT)
			return null;
		else {
			CommonTree exprTree = (CommonTree) invariantTree.getChild(0);

			return translateExpression(exprTree, scope);
		}
	}

	private PragmaHandler getPragmaHandler(String code) {
		PragmaHandler result = pragmaMap.get(code);

		if (result == null) {
			result = pragmaFactory.newHandler(code, parseTree);
			pragmaMap.put(code, result);
		}
		return result;
	}

	private ASTNode translatePragma(CommonTree pragmaTree, SimpleScope scope) throws SyntaxException {
		Source source = newSource(pragmaTree);
		CommonTree identifierTree = (CommonTree) pragmaTree.getChild(0);
		IdentifierNode identifier = translateIdentifier(identifierTree);
		String code = identifier.name();
		CommonTree bodyTree = (CommonTree) pragmaTree.getChild(1);
		CommonTree newlineTree = (CommonTree) pragmaTree.getChild(2);
		CivlcToken newlineToken = (CivlcToken) newlineTree.getToken();
		CivlcTokenSequence producer = parseTree.getTokenSourceProducer(bodyTree);
		PragmaNode pragmaNode = nodeFactory.newPragmaNode(source, identifier, producer, newlineToken);
		PragmaHandler handler;
		ASTNode result = null;

		// If the pragma is "CIVL ACSL", returns null:
		if (code.equals("CIVL")) {
			CommonTree commandTree = (CommonTree) pragmaTree.getChild(1);

			if (commandTree.getChild(0).getText().equals("ACSL"))
				return null;
		}
		handler = getPragmaHandler(code);
		identifier.setEntity(handler);
		try {
			result = handler.processPragmaNode(pragmaNode, scope);
		} catch (ParseException e) {
			this.error(e.getMessage(), pragmaTree);
		}
		return result;
	}

	/**
	 * Is any node in the tree rooted at <code>tree</code> an identifier node whose
	 * name is <code>name</code>?
	 * 
	 * @param tree a non-null parse tree node
	 * @param name a non-null string
	 * @return <code>true</code> iff such a node exists
	 */
	private boolean containsIdentifier(CommonTree tree, String name) {
		if (tree.getType() == CivlCParser.IDENTIFIER) {
			if (name.equals(tree.getText()))
				return true;
		} else {
			int n = tree.getChildCount();
			for (int i = 0; i < n; i++) {
				CommonTree child = (CommonTree) tree.getChild(i);
				if (containsIdentifier(child, name))
					return true;
			}
		}
		return false;
	}

	/**
	 * 
	 * @param compoundStatementTree
	 * @return
	 * @throws SyntaxException
	 */
	// @SuppressWarnings("unchecked")
	private CompoundStatementNode translateCompoundStatement(CommonTree compoundStatementTree, SimpleScope scope)
			throws SyntaxException {
		SimpleScope newScope = new SimpleScope(scope);
		Source source = newSource(compoundStatementTree);
		CommonTree blockItems = (CommonTree) compoundStatementTree.getChild(1);
		int numChildren = blockItems.getChildCount();
		List<BlockItemNode> items = new LinkedList<BlockItemNode>();
		List<BlockItemNode> civlPragmaNodes = new LinkedList<BlockItemNode>();
		OmpExecutableNode ompStatementNode = null;

		annotationStack.push(new AnnotationData(null, null, null));
		for (int i = 0; i < numChildren; i++) {
			CommonTree childTree = (CommonTree) blockItems.getChild(i);
			List<BlockItemNode> newBlockItems = this.translateBlockItemNode(childTree, newScope, false);

			if (newBlockItems.size() == 1) {
				BlockItemNode itemNode = newBlockItems.get(0);

				if (itemNode.blockItemKind() == BlockItemKind.STATEMENT) {
					StatementNode statementNode = (StatementNode) newBlockItems.get(0);

					if (civlPragmaNodes.size() > 0) {
						for (BlockItemNode node : civlPragmaNodes) {
							Object pragmaDSAttrVal = node.getAttribute(attrKeyDependSource);
							Object pragmaDTAttrVal = node.getAttribute(attrKeyDependTarget);

							if (pragmaDSAttrVal != null) {
								Object stmtDSAttrVal = statementNode.getAttribute(attrKeyDependSource);

								if (stmtDSAttrVal == null) {
									statementNode.setAttribute(attrKeyDependSource, pragmaDSAttrVal);
								} else {
									@SuppressWarnings("unchecked")
									Set<String> set1 = (Set<String>) stmtDSAttrVal;
									@SuppressWarnings("unchecked")
									Set<String> set2 = (Set<String>) pragmaDSAttrVal;
									set1.addAll(set2);
								}
							}
							if (pragmaDTAttrVal != null) {
								Object stmtDTAttrVal = statementNode.getAttribute(attrKeyDependTarget);

								if (stmtDTAttrVal == null) {
									statementNode.setAttribute(attrKeyDependTarget, pragmaDTAttrVal);
								} else {
									@SuppressWarnings("unchecked")
									Set<String> set1 = (Set<String>) stmtDTAttrVal;
									@SuppressWarnings("unchecked")
									Set<String> set2 = (Set<String>) pragmaDTAttrVal;
									set1.addAll(set2);
								}
							}
						}
						civlPragmaNodes.clear();
					}
					if (ompStatementNode != null) {
						ompStatementNode.setStatementNode(statementNode);
						ompStatementNode = null;
					} else {
						items.add(statementNode);
						if (statementNode.statementKind() == StatementKind.OMP) {
							ompStatementNode = (OmpExecutableNode) statementNode;
							if (ompStatementNode.isComplete())
								ompStatementNode = null;
						}
					}
					continue;
				} else if (itemNode.blockItemKind() == BlockItemKind.PRAGMA) {
					PragmaNode pragmaNode = (PragmaNode) itemNode;

					if (pragmaNode.getPragmaIdentifier().name().equals(PRAGMA_ID_NAME_CIVL)) {
						civlPragmaNodes.add(pragmaNode);
						continue;
					}
				}
			}
			items.addAll(newBlockItems);
		}
		annotationStack.pop();

		int numItems = items.size();
		boolean changed = false;

		for (int i = 0; i < numItems; i++) {
			ASTNode child = items.get(i);

			if (child != null && child instanceof OmpExecutableNode) {
				OmpExecutableNode ompStmt = (OmpExecutableNode) child;

				if (!ompStmt.isComplete()) {
					changed = true;
					if (ompStmt instanceof OmpForNode) {
						OmpForNode ompForNode = (OmpForNode) ompStmt;
						int collapse = ompForNode.collapse();

						if (collapse == 1) {
							StatementNode forStatement = (StatementNode) items.get(i + 1);

							items.set(i + 1, null);
							ompForNode.setStatementNode(forStatement);
						} else {
							List<BlockItemNode> forList = new ArrayList<>(collapse);
							CompoundStatementNode compoundStatement;

							source = items.get(i + 1).getSource();

							for (int k = 1; k <= collapse; k++) {
								StatementNode forStatement = (StatementNode) items.get(i + k);

								items.set(i + k, null);
								forList.add(forStatement);
							}
							compoundStatement = nodeFactory.newCompoundStatementNode(source, forList);
							ompForNode.setStatementNode(compoundStatement);
						}
						items.set(i, ompForNode);
					} else {
						StatementNode statementNode = (StatementNode) items.get(i + 1);

						items.set(i + 1, null);
						ompStmt.setStatementNode(statementNode);
						items.set(i, ompStmt);
					}
				}
			}
		}
		if (changed) {
			List<BlockItemNode> newItems = new LinkedList<>();
			for (int i = 0; i < numItems; i++) {
				BlockItemNode item = items.get(i);
				if (item != null)
					newItems.add(item);
			}
			items = newItems;
		}
		return nodeFactory.newCompoundStatementNode(source, items);
	}

	private ChooseStatementNode translateChooseStatement(CommonTree chooseStatementTree, SimpleScope scope)
			throws SyntaxException {
		int numChildren = chooseStatementTree.getChildCount();
		List<StatementNode> statements = new LinkedList<StatementNode>();

		for (int i = 0; i < numChildren; i++) {
			CommonTree statementTree = (CommonTree) chooseStatementTree.getChild(i);
			StatementNode statement = translateStatement(statementTree, scope);

			statements.add(statement);
		}
		return nodeFactory.newChooseStatementNode(newSource(chooseStatementTree), statements);
	}

	/**
	 * 
	 * @param statementTree
	 * @return
	 * @throws SyntaxException
	 */
	private StatementNode translateStatement(CommonTree tree, SimpleScope scope) throws SyntaxException {
		int kind = tree.getType();

		if (kind == ABSENT)
			return null;

		CommonTree statementTree = (CommonTree) tree.getChild(0);

		kind = statementTree.getType();
		switch (kind) {
		case ASM:
			// for now, all assembly code is a no-op
			return nodeFactory.newNullStatementNode(newSource(statementTree));
		case BREAK:
			return nodeFactory.newBreakNode(newSource(statementTree));
		case CASE_LABELED_STATEMENT:
			return translateCaseLabeledStatement(statementTree, scope);
		case CHOOSE:
			return translateChooseStatement(statementTree, scope);
		case CIVLATOMIC:
			return translateAtomic(statementTree, scope);
		case CIVLFOR:
		case PARFOR:
			return translateCivlFor(statementTree, scope);
		case COMPOUND_STATEMENT: {
			CompoundStatementNode compound = translateCompoundStatement(statementTree, scope);
			return compound;
		}
		case CONTINUE:
			return nodeFactory.newContinueNode(newSource(statementTree));
		case DEFAULT_LABELED_STATEMENT:
			return translateDefaultLabeledStatement(statementTree, scope);
		case DO:
			return translateDo(statementTree, scope);
		case EXPRESSION_STATEMENT:
			return translateExpressionStatement(statementTree, scope);
		case FOR:
			return translateFor(statementTree, scope);
		case GOTO:
			return translateGoto(statementTree);
		case IDENTIFIER_LABELED_STATEMENT:
			return translateIdentifierLabeledStatement(statementTree, scope);
		case IF:
			return translateIf(statementTree, scope);
		case PPRAGMA: {
			ASTNode newNode = translatePragma(statementTree, scope);

			if (newNode instanceof StatementNode)
				return (StatementNode) newNode;
			else
				throw error("This pragma cannot be used as a statement", newNode);
		}
		case RETURN:
			return nodeFactory.newReturnNode(newSource(statementTree),
					translateExpression((CommonTree) statementTree.getChild(0), scope));
		case RUN:
			return nodeFactory.newRunNode(newSource(statementTree),
					translateStatement((CommonTree) statementTree.getChild(0), scope));
		case SWITCH:
			return translateSwitch(statementTree, scope);
		case WHEN:
			return nodeFactory.newWhenNode(newSource(statementTree),
					translateExpression((CommonTree) statementTree.getChild(0), scope),
					translateStatement((CommonTree) statementTree.getChild(1), scope));
		case WHILE:
			return translateWhile(statementTree, scope);
		default:
			throw error("Unknown statement type " + kind, statementTree);
		}
	}

	// Translation of Static Assertions and External Definitions...

	/**
	 * 
	 * @param staticAssertTree
	 * @return
	 * @throws SyntaxException
	 */
	private StaticAssertionNode translateStaticAssertion(CommonTree staticAssertTree, SimpleScope scope)
			throws SyntaxException {
		CommonTree stringLiteral = (CommonTree) staticAssertTree.getChild(1);
		Source stringLiteralSource = newSource(stringLiteral);

		return nodeFactory.newStaticAssertionNode(newSource(staticAssertTree),
				translateExpression((CommonTree) staticAssertTree.getChild(0), scope),
				translateStringLiteral(stringLiteralSource, (CommonTree) staticAssertTree.getChild(1)));
	}

	/**
	 * 
	 * @param functionDefinitionTree
	 * @return
	 * @throws SyntaxException
	 */
	private FunctionDefinitionNode translateFunctionDefinition(CommonTree functionDefinitionTree, SimpleScope scope)
			throws SyntaxException {
		// two different ways of declaring parameters:
		// (1) parameter-type list and no declarations
		// (2) identifier list and declarations
		SimpleScope newScope = getNewScope(scope);
		CommonTree specifiers = (CommonTree) functionDefinitionTree.getChild(0);
		CommonTree declarator = (CommonTree) functionDefinitionTree.getChild(1);
		CommonTree declarationList = (CommonTree) functionDefinitionTree.getChild(2);
		CommonTree compoundStatementTree = (CommonTree) functionDefinitionTree.getChild(3);
		SpecifierAnalysis analysis = newSpecifierAnalysis(specifiers);
		TypeNode baseType = newSpecifierType(analysis, newScope);
		DeclaratorData data = processDeclarator(declarator, baseType, newScope);
		FunctionTypeNode functionType = (FunctionTypeNode) data.type;

		if (functionType.hasIdentifierList()) {
			SequenceNode<VariableDeclarationNode> formalSequenceNode = functionType.getParameters();
			int numFormals = formalSequenceNode.numChildren();
			int numDeclarations = declarationList.getChildCount();

			if (numFormals == 0) {
				if (numDeclarations != 0)
					throw error("Function with empty identifier list has parameter declarations", declarationList);
			} else {
				SequenceNode<VariableDeclarationNode> newFormalSequenceNode;
				List<VariableDeclarationNode> newFormalList = new LinkedList<VariableDeclarationNode>();
				Map<String, VariableDeclarationNode> declMap = new HashMap<String, VariableDeclarationNode>();

				for (int i = 0; i < numDeclarations; i++) {
					CommonTree declarationTree = (CommonTree) declarationList.getChild(i);
					List<BlockItemNode> declNodes = translateDeclaration(declarationTree, newScope);

					for (BlockItemNode definition : declNodes) {
						String parameterName;
						VariableDeclarationNode oldDeclaration;

						if (!(definition instanceof VariableDeclarationNode))
							throw error("Illegal parameter declaration", declarationTree);
						parameterName = ((VariableDeclarationNode) definition).getIdentifier().name();
						if (parameterName == null)
							throw error("Illegal parameter declaration", declarationTree);
						oldDeclaration = declMap.get(parameterName);
						if (oldDeclaration != null)
							throw error("Re-declaration of parameter. Old declaration was at " + oldDeclaration,
									declarationTree);
						declMap.put(parameterName, (VariableDeclarationNode) definition);
					}
				}
				for (VariableDeclarationNode formal : formalSequenceNode) {
					String parameterName = formal.getIdentifier().name();
					VariableDeclarationNode newDeclaration;

					if (parameterName == null)
						throw error("Formal parameter declaration missing name: " + formal, declarator);
					newDeclaration = declMap.get(parameterName);
					if (newDeclaration == null)
						throw error("Missing declaration for parameter " + parameterName, formal);
					newFormalList.add(newDeclaration);
					declMap.remove(parameterName);
				}
				if (!declMap.isEmpty())
					throw error("Function contains declarations for variables that are not parameters",
							declarationList);
				newFormalSequenceNode = nodeFactory.newSequenceNode(newSource(declarationList),
						"FormalParameterDeclarations", newFormalList);
				functionType.setParameters(newFormalSequenceNode);
			}
		}
		CompoundStatementNode body = translateCompoundStatement(compoundStatementTree, newScope);
		// According to the C Spec, the implicitly identifier __func__
		// is defined in every function...
		boolean containsFunc = containsIdentifier(compoundStatementTree, "__func__");
		// System.out.println("Contains __func__: " + containsFunc);
		if (containsFunc) {
			CommonTree lcurlyNode = (CommonTree) compoundStatementTree.getChild(0);
			Source lcurlySource = newSource(lcurlyNode);
			IdentifierNode id__func__ = nodeFactory.newIdentifierNode(lcurlySource, "__func__");
			TypeNode charTypeNode = nodeFactory.newBasicTypeNode(lcurlySource, BasicTypeKind.CHAR);
			TypeNode stringTypeNode = nodeFactory.newArrayTypeNode(lcurlySource, charTypeNode, null);
			InitializerNode initNode = astFactory.newStringLiteralNode(
					tokenFactory.newSystemFormation("Predefined identifiers"), "\"" + data.identifier.name() + "\"");
			VariableDeclarationNode funcDecl = nodeFactory.newVariableDeclarationNode(lcurlySource, id__func__,
					stringTypeNode, initNode);
			body.insertChildren(0, Arrays.asList(funcDecl));
		}
		FunctionDefinitionNode result = nodeFactory.newFunctionDefinitionNode(newSource(functionDefinitionTree),
				data.identifier, (FunctionTypeNode) data.type, getContract(), body);
		setFunctionSpecifiers((FunctionDefinitionNode) result, analysis);
		setStorageSpecifiers((FunctionDefinitionNode) result, analysis);
		return result;
	}

	@SuppressWarnings("unused")
	private SequenceNode<ContractNode> getContract(CommonTree contractTree, SimpleScope scope) throws SyntaxException {
		SequenceNode<ContractNode> contract;

		if (contractTree == null)
			contract = null;
		else {
			int kind = contractTree.getType();

			if (kind == ABSENT)
				contract = null;
			else {
				int numItems = contractTree.getChildCount();
				List<ContractNode> items = new LinkedList<ContractNode>();

				if (numItems == 0) {
					contract = null;
				} else {
					for (int i = 0; i < numItems; i++) {
						CommonTree itemTree = (CommonTree) contractTree.getChild(i);
						int itemKind = itemTree.getType();
						ContractNode contractNode;
						Source source = newSource(itemTree);

						if (itemKind == ASSIGNS || itemKind == READS || itemKind == DEPENDS) {
							List<ExpressionNode> argumentList = new ArrayList<>();
							int expressionCount = itemTree.getChildCount();
							// CommonTree conditionTree = null;
							CommonTree listTree;
							int listKind;
							// ExpressionNode condition = null;
							// SequenceNode<ExpressionNode> list;

							if (expressionCount == 2) {
								// conditionTree = (CommonTree) itemTree
								// .getChild(0);
								listTree = (CommonTree) itemTree.getChild(1);
							} else {
								listTree = (CommonTree) itemTree.getChild(0);
							}
							listKind = listTree.getType();
							// if (conditionTree != null) {
							// condition = translateExpression(conditionTree,
							// scope);
							// }
							if (listKind == ARGUMENT_LIST) {
								int numArgs = listTree.getChildCount();

								for (int j = 0; j < numArgs; j++) {
									CommonTree argumentTree = (CommonTree) listTree.getChild(j);
									ExpressionNode argumentNode = translateExpression(argumentTree, scope);

									argumentList.add(argumentNode);
								}
								// list = nodeFactory.newSequenceNode(
								// newSource(listTree),
								// "$assigns/$reads/$depends arguments",
								// argumentList);
								if (itemKind == ASSIGNS)
									contractNode = null;// nodeFactory.newAssignsNode(
								// source, condition, list);
								else if (itemKind == READS)
									contractNode = null;// nodeFactory.newReadsNode(
								// source, condition, list);
								else
									contractNode = nodeFactory.newDependsNode(source, null, null);
							} else {
								throw new SyntaxException("Invalid arguments for $assigns/$reads clause", source);
							}
						} else {
							CommonTree exprTree = (CommonTree) itemTree.getChild(0);
							ExpressionNode expr = translateExpression(exprTree, scope);

							switch (itemKind) {
							case ENSURES:
								contractNode = nodeFactory.newEnsuresNode(source, expr);
								break;
							case REQUIRES:
								contractNode = nodeFactory.newRequiresNode(source, expr);
								break;
							case GUARD:
								contractNode = nodeFactory.newGuardNode(source, expr);
								break;
							default:
								throw error("Unknown kind of contract item: " + itemTree, itemTree);
							}
						}
						items.add(contractNode);
					}
					contract = nodeFactory.newSequenceNode(newSource(contractTree), "Contract", items);
				}
			}
		}
		return contract;
	}

	// Translation of Translation Unit...

	/**
	 * @param translationUnit
	 * @return
	 * @throws SyntaxException
	 */
	private SequenceNode<BlockItemNode> translateTranslationUnit(CommonTree translationUnit) throws SyntaxException {
		int numChildren = translationUnit.getChildCount();
		ArrayList<BlockItemNode> definitions = new ArrayList<BlockItemNode>();
		SimpleScope scope = new SimpleScope(null);

		if (numChildren == 0) {
			String msg = "C11 6.9 requires that a translation unit contain at least one external declaration";
			CivlcTokenSource ts = parseTree.getCivlcTokenSource();
			int numTokens = ts.getNumTokens();

			if (numTokens > 0) {
				CivlcToken first = ts.getToken(0), last = ts.getToken(numTokens - 1);
				Source source = tokenFactory.newSource(first, last);

				throw new SyntaxException(msg, source);
			} else {
				throw new SyntaxException(ts.getSourceName() + ": " + msg, null);
			}
		}
		annotationStack.push(new AnnotationData(null, null, null));
		for (int i = 0; i < numChildren; i++) {
			// TODO need to know what's the language and decide whether the
			// external definition node type needs to be checked, because C
			// doesn't allow statement in the file scope
			definitions.addAll(this.translateBlockItemNode((CommonTree) translationUnit.getChild(i), scope, false));
		}
		annotationStack.pop();

		// TODO: maybe find a better way to handle this (e.g. only when Cuda
		// flag specified so we don't have to rely on automatically detecting
		// Cuda programs
		for (BlockItemNode defNode : definitions) {
			if (defNode instanceof FunctionDeclarationNode) {
				if (((FunctionDeclarationNode) defNode).hasGlobalFunctionSpecifier()) {
					// assume that the presence of __global__ means that
					// this is a Cuda program -> act as if cuda.cvh header
					// file has been included. other ways to check include
					// cheacking for the presence of a __shared__ variable
					// declaration, or a function call with an execution context
					// <<<...>>>

					break;
				}
			}
		}
		return nodeFactory.newTranslationUnitNode(newSource(translationUnit), definitions);
	}

	private void attachAttributeToPragmaNode(PragmaNode pragmaNode) throws SyntaxException {
		final String PRAGMA_CIVL_KEYWORD_DEPEND = "depend";
		String pragmaIdStr = pragmaNode.getPragmaIdentifier().name();

		if (pragmaIdStr.equals(PRAGMA_ID_NAME_CIVL)) {
			// Convert `#pragma civl xxx` as attribute
			int tokenIndex = 0;
			while (tokenIndex < pragmaNode.getNumTokens()
					&& pragmaNode.getToken(tokenIndex).getType() == CivlCParser.WS) {
				tokenIndex++;
			}
			if (tokenIndex >= pragmaNode.getNumTokens())
				throw new SyntaxException("pragma 'civl' has no keyword", null);
			switch (pragmaNode.getToken(tokenIndex++).getText().toLowerCase()) {
			case PRAGMA_CIVL_KEYWORD_DEPEND:
				final String PRAGMA_CIVL_DEPEND_TYPE_SOURCE = "source";
				final String PRAGMA_CIVL_DEPEND_TYPE_TARGET = "target";

				while (tokenIndex < pragmaNode.getNumTokens()
						&& pragmaNode.getToken(tokenIndex).getType() == CivlCParser.WS) {
					tokenIndex++;
				}
				if (tokenIndex >= pragmaNode.getNumTokens())
					throw new SyntaxException("pragma 'civl depend' has no type", null);
				String civlDependTypeName = pragmaNode.getToken(tokenIndex++).getText().toLowerCase();
				AttributeKey attrKey = null;
				Set<String> attrVal = null;
				int nLP = 0;
				int nRP = 0;

				if (civlDependTypeName.equals(PRAGMA_CIVL_DEPEND_TYPE_SOURCE))
					attrKey = attrKeyDependSource;
				else if (civlDependTypeName.equals(PRAGMA_CIVL_DEPEND_TYPE_TARGET))
					attrKey = attrKeyDependTarget;
				for (; tokenIndex < pragmaNode.getNumTokens(); tokenIndex++) {

					switch (pragmaNode.getToken(tokenIndex).getType()) {
					case CivlCParser.LPAREN:
						nLP++;
					case CivlCParser.WS:
						continue;
					case CivlCParser.RPAREN:
						nRP++;
						tokenIndex = pragmaNode.getNumTokens();
						break;
					case CivlCParser.IDENTIFIER:
						if (nLP == 1) {
							Set<String> tagSet = new HashSet<String>();

							tagSet.add(pragmaNode.getToken(tokenIndex).getText());
							attrVal = tagSet;
							continue;
						}
					default:
					}
				}
				if (nLP == nRP && attrKey != null && attrVal != null) {
					// The syntax is correct
					pragmaNode.setAttribute(attrKey, attrVal);
					return;
				} else
					throw new SyntaxException(
							"'civl' pragma with 'depend' type has invalid format, please use: #pragma civl depend source(TAG) or #pragma civl depend target(TAG)",
							null);
			default:
				throw new SyntaxException("'civl' pragma has an unknown keyword", null);
			}
		}
	}

	/**
	 * Translates a block item node.
	 * 
	 * @param blockItemTree
	 * @param scope
	 * @return
	 * @throws SyntaxException
	 */
	private List<BlockItemNode> translateBlockItemNode(CommonTree blockItemTree, SimpleScope scope,
			boolean checkCExternalDefs) throws SyntaxException {
		int kind = blockItemTree.getType();
		List<BlockItemNode> items = new LinkedList<BlockItemNode>();

		switch (kind) {
		case DECLARATION:
			for (BlockItemNode declaration : translateDeclaration(blockItemTree, scope))
				items.add(declaration);
			break;
		case FUNCTION_DEFINITION:
			items.add(translateFunctionDefinition(blockItemTree, scope));
			break;
		case PPRAGMA:
			ASTNode pragmaNode = translatePragma(blockItemTree, scope);

			if (pragmaNode != null) {
				if (pragmaNode instanceof PragmaNode) {
					attachAttributeToPragmaNode((PragmaNode) pragmaNode);
				}
				items.add((BlockItemNode) pragmaNode);
			}
			break;
		case STATEMENT:
			if (checkCExternalDefs) {
				throw new SyntaxException("statement is not allowed in file scope", null);
			}
			items.add((BlockItemNode) this.translateStatement(blockItemTree, scope));
			break;
		case STATICASSERT:
			items.add(translateStaticAssertion(blockItemTree, scope));
			break;
		case ANNOTATION:
			items.addAll(translateAnnotation(blockItemTree, scope));
			break;
		default:
			throw new ABCUnsupportedException("translating block item node " + blockItemTree.toString());
		}

		List<TransformNode> transforms = getTransforms();

		if (transforms != null) {
			for (BlockItemNode item : items) {
				item.addAllTransformAnnotations(transforms);
			}
		}
		if (!items.isEmpty())
			clearCurrentAnnotations();

		return items;
	}

	/* ********************* ASTBuilderWorker Methods ********************* */

	/**
	 * Translates a code annotation, which is a comment that begins with an "@"
	 * character. If the configuration's acsl flag is false, this will do nothing.
	 * Else will interpret it as an ACSL annotation.
	 * 
	 * @param annotationTree the ANTLR tree node representing the annotation; it is
	 *                       a flat list of tokens
	 * @param scope          the scope in which this construct occurs
	 * @throws SyntaxException if something goes wrong in translating the annotation
	 */
	private List<BlockItemNode> translateAnnotation(CommonTree annotationTree, SimpleScope scope)
			throws SyntaxException {
		CommonTree bodyTree = (CommonTree) annotationTree.getChild(1);
		CivlcTokenSource tokenSource = parseTree.getTokenSourceProducer(bodyTree).newSource();
		Source source = this.newSource(annotationTree);
		SimpleScope newScope = new SimpleScope(scope);

		this.annotationStack.peek().scope = newScope;

		ACSLSpecTranslation acslSpec = acslHandler.translateAcslAnnotation(source, tokenSource, newScope, config);

		if (acslSpec.contractNodes.numChildren() > 0 || annotationStack.peek().contract == null)
			this.annotationStack.peek().contract = acslSpec.contractNodes;
		if (!acslSpec.transformNodes.isEmpty() || annotationStack.peek().transforms == null)
			this.annotationStack.peek().transforms = acslSpec.transformNodes;
		return acslSpec.blockItemNodes;
	}

	/**
	 * The main method: given an ANTLR tree, produces a TranslationUnit.
	 * 
	 * @param tree an ANTLR syntax tree
	 * @return a TranslationUnit representing the given syntax tree
	 * @throws SyntaxException if there is something in the tree that does not
	 *                         conform to the C11 standard
	 */
	@Override
	public SequenceNode<BlockItemNode> translateRoot() throws SyntaxException {
		return translateTranslationUnit(rootTree);
	}

	@Override
	public ExpressionNode translateExpression(CommonTree expressionTree, SimpleScope scope) throws SyntaxException {
		int kind = expressionTree.getType();

		if (kind == ABSENT)
			return null;
		return translateExpression(newSource(expressionTree), expressionTree, scope);
	}

	@Override
	public List<BlockItemNode> translateBlockItem(CommonTree blockItemTree, SimpleScope scope) throws SyntaxException {
		return translateBlockItemNode(blockItemTree, scope, false);
	}
}

/**
 * Helper class which wraps a type node and identifier.
 * 
 * @author siegel
 * 
 */
class DeclaratorData {
	TypeNode type;
	IdentifierNode identifier;

	DeclaratorData(TypeNode type, IdentifierNode identifier) {
		this.type = type;
		this.identifier = identifier;
	}
}
