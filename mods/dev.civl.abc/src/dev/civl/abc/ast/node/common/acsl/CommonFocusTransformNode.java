package dev.civl.abc.ast.node.common.acsl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import dev.civl.abc.analysis.IF.FocusAnalysisData;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.acsl.FocusTransformNode;
import dev.civl.abc.ast.node.IF.acsl.TransformNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.front.IF.CivlcTokenConstant;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.CivlcToken.TokenVocabulary;
import dev.civl.abc.token.IF.Formation;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.StringToken;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;

public abstract class CommonFocusTransformNode extends CommonTransformNode
		implements FocusTransformNode {
	protected NodeFactory nodeFactory;
	protected TokenFactory tokenFactory;
	protected FocusAnalysisData focusData = null;

	public CommonFocusTransformNode(Source source, NodeFactory nodeFactory,
			TokenFactory tokenFactory, Iterable<? extends ASTNode> children) {
		super(source, children);
		this.nodeFactory = nodeFactory;
		this.tokenFactory = tokenFactory;
	}
	
	public CommonFocusTransformNode(Source source, NodeFactory nodeFactory,
			TokenFactory tokenFactory, ASTNode child0) {
		this(source, nodeFactory, tokenFactory, Arrays.asList(child0));
	}
	
	public CommonFocusTransformNode(Source source, NodeFactory nodeFactory,
			TokenFactory tokenFactory) {
		this(source, nodeFactory, tokenFactory, Arrays.asList(new ASTNode[] {}));
	}
	
	public void setFocusAnalysisData(FocusAnalysisData focusData) {
		this.focusData = focusData;
	}
	
	protected boolean removeAllFocusTransforms(ASTNode node) {
		boolean hadFocusTransforms = false;
		List<TransformNode> transforms = node.transformAnnotations();
		for (int i = 0; i < transforms.size(); i++) {
			TransformNode transform = transforms.get(i);
			if (transform instanceof FocusTransformNode) {
				hadFocusTransforms = true;
				node.removeTransformAnnotation(i);
				i--;
			}
		}
		return hadFocusTransforms;
	}
	
	protected ExpressionNode replaceIdent(ExpressionNode expr, String origName,
			String newName) {
		return replaceIdent(expr, origName, identifierExpression(newName));
	}
	
	protected ExpressionNode replaceIdent(ExpressionNode expr, String origName, ExpressionNode replacementExpr) {
		// If the top-level expression is itself the identifier to replace,
		// return a copy of the replacement directly — the copied root has no
		// parent, so replaceIdentHelper would NPE trying to call parent.removeChild().
		if (expr.expressionKind() == ExpressionNode.ExpressionKind.IDENTIFIER_EXPRESSION) {
			IdentifierNode ident = ((IdentifierExpressionNode) expr).getIdentifier();
			if (ident.name().equals(origName))
				return replacementExpr.copy();
		}
		ExpressionNode result = expr.copy();
		replaceIdentHelper(result, origName, replacementExpr);
		return result;
	}

	private void replaceIdentHelper(ASTNode node, String identName,
			ExpressionNode replacementNode) {
		if (node == null)
			return;

		if (node.nodeKind() == ASTNode.NodeKind.EXPRESSION) {
			ExpressionNode expr = (ExpressionNode) node;
			if (expr.expressionKind() == ExpressionNode.ExpressionKind.IDENTIFIER_EXPRESSION) {
				IdentifierNode ident = ((IdentifierExpressionNode) expr)
						.getIdentifier();
				if (ident.name().equals(identName)) {
					int childIndex = expr.childIndex();
					ASTNode parent = expr.parent();
					parent.removeChild(childIndex);
					parent.setChild(childIndex, replacementNode.copy());
				}

				return;
			}
		}
		for (ASTNode child : node.children()) {
			replaceIdentHelper(child, identName, replacementNode);
		}
	}
	
	protected ExpressionNode genVarRestriction(String varName, RegularRangeNode range) {
		String methodName = "genVarRestriction";
		Source varSource = newSource(methodName, varName);
		IdentifierExpressionNode varNode = nodeFactory
				.newIdentifierExpressionNode(varSource,
						nodeFactory.newIdentifierNode(varSource, varName));
		ExpressionNode loBoundNode = nodeFactory.newOperatorNode(
				range.getLow().getSource(), OperatorNode.Operator.LTE,
				range.getLow().copy(), varNode);
		ExpressionNode hiBoundNode = nodeFactory.newOperatorNode(
				range.getHigh().getSource(), OperatorNode.Operator.LTE,
				varNode.copy(), range.getHigh().copy());
		return nodeFactory.newOperatorNode(range.getSource(), OperatorNode.Operator.LAND, loBoundNode, hiBoundNode);
	}
	
	protected FunctionCallNode functionCall(String name,
			List<ExpressionNode> args) {
		return nodeFactory.newFunctionCallNode(newSource("functionCall", name),
				identifierExpression(name), args);
	}

	protected FunctionCallNode functionCall(String name) {
		return functionCall(name, new LinkedList<>());
	}

	protected IdentifierExpressionNode identifierExpression(String name) {
		Source source = newSource("identifierExpression", name);
		return nodeFactory.newIdentifierExpressionNode(source,
				identifier(name));
	}

	protected IdentifierNode identifier(String name) {
		return nodeFactory.newIdentifierNode(newSource("identifier", name),
				name);
	}

	protected StringLiteralNode stringLiteralExpression(String value)
			throws SyntaxException {
		// value should use C escape sequences (e.g., "\\n" for newline)
		String representation = "\"" + value + "\"";
		Formation formation = tokenFactory.newTransformFormation(
				"CommonFocusTransformNode", "stringLiteralExpression");
		CivlcToken token = tokenFactory.newCivlcToken(
				CivlcTokenConstant.STRING_LITERAL, representation, formation,
				TokenVocabulary.DUMMY);
		StringToken stringToken = tokenFactory.newStringToken(token);
		Source source = tokenFactory.newSource(stringToken);
		return nodeFactory.newStringLiteralNode(source, representation,
				stringToken.getStringLiteral());
	}
	
	protected ExpressionNode addIntExpr(ExpressionNode expr, int offset) {
		String thisFuncName = "addInt";
		Source thisSource = newSource(thisFuncName,
				expr.toString() + "+" + Integer.toString(offset));
		return nodeFactory.newOperatorNode(thisSource, Operator.PLUS, expr,
				nodeFactory.newIntConstantNode(
						newSource(thisFuncName, Integer.toString(offset)),
						offset));
	}
	
	protected ExpressionNode andExpr(ExpressionNode... args) {
		Source source = newSource("andExpr", "&&");
		if (args.length == 0)
			return nodeFactory.newBooleanConstantNode(source, true);

		ExpressionNode result = args[0];
		for (int i = 1; i < args.length; i++) {
			result = nodeFactory.newOperatorNode(source,
					OperatorNode.Operator.LAND, result, args[i]);
		}
		return result;
	}
	
	protected ExpressionNode implies(ExpressionNode contextNode,
			ExpressionNode resultNode) {
		String thisFuncName = "implies";
		Source thisSource = newSource(thisFuncName,
				contextNode.toString() + " ==> " + resultNode.toString());
		return nodeFactory.newOperatorNode(thisSource, Operator.LOR, nodeFactory
				.newOperatorNode(thisSource, Operator.NOT, contextNode),
				resultNode);
	}
	
	protected ExpressionNode minExpression(ExpressionNode expr1, ExpressionNode expr2) {
		String thisFuncName = "minExpression";
		Source maxSource = newSource(thisFuncName,
				"min(" + expr1.toString() + ", " + expr2.toString() + ")");
		return nodeFactory.newOperatorNode(maxSource, Operator.CONDITIONAL,
				nodeFactory.newOperatorNode(maxSource,
						Operator.LTE, expr1, expr2), expr1.copy(), expr2.copy());
	}
	
	protected ExpressionNode maxExpression(ExpressionNode expr1, ExpressionNode expr2) {
		String thisFuncName = "maxExpression";
		Source maxSource = newSource(thisFuncName,
				"max(" + expr1.toString() + ", " + expr2.toString() + ")");
		return nodeFactory.newOperatorNode(maxSource, Operator.CONDITIONAL,
				nodeFactory.newOperatorNode(maxSource,
						Operator.GTE, expr1, expr2), expr1.copy(), expr2.copy());
	}

	protected Source newSource(String callingFuncName, String sourceText) {
		Formation formation = tokenFactory.newTransformFormation(
				"CommonFocusTransformNode", callingFuncName);
		CivlcToken token = tokenFactory.newCivlcToken(
				CivlcTokenConstant.DECLARATION, sourceText, formation,
				TokenVocabulary.DUMMY);
		return tokenFactory.newSource(token);
	}
}
