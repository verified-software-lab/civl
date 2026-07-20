package dev.civl.abc.ast.node.common.acsl;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.acsl.FocusAssertTransformNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode.Quantifier;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;

public class CommonFocusAssertTransformNode extends CommonFocusTransformNode
		implements
			FocusAssertTransformNode {
	
	private List<String> focusTags;
	
	public CommonFocusAssertTransformNode(Source source,
			NodeFactory nodeFactory, TokenFactory tokenFactory,
			List<String> focusTags) {
		super(source, nodeFactory, tokenFactory);
		this.focusTags = focusTags;
	}
	
	@Override
	public List<BlockItemNode> transform(List<BlockItemNode> items)
			throws SyntaxException {
		if (items == null || items.size() != 1) {
			throw new SyntaxException(
					"Focus can only be applied to exactly one BlockItemNode.",
					getSource());
		}
		BlockItemNode root = items.get(0);
		if (!(root instanceof ExpressionStatementNode)) {
			throw new SyntaxException("Focus must be applied to an ExpressionStatementNode.", getSource());
		}
		ExpressionStatementNode assertNode = (ExpressionStatementNode) root;
		ExpressionStatementNode assumeNode = assertNode.copy();
		removeAllFocusTransforms(assumeNode);
		((IdentifierExpressionNode) ((FunctionCallNode) assumeNode
				.getExpression()).getFunction()).getIdentifier()
						.setName("$assume");
		FunctionCallNode funcCallNode = (FunctionCallNode) assertNode
				.getExpression();

		ExpressionNode newForallNode = transformForall(
				(QuantifiedExpressionNode) funcCallNode.getArgument(0));

		newForallNode.remove();
		funcCallNode.setArgument(0, newForallNode);

		List<BlockItemNode> result = new LinkedList<BlockItemNode>();
		result.add(assertNode);
		result.add(assumeNode);
		return result;
	}

	private ExpressionNode transformForall(QuantifiedExpressionNode forallNode)
			throws SyntaxException {
		SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVarList = forallNode
				.boundVariableList();
		HashMap<String, String> replacementMap = new HashMap<String, String>();
		List<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> newBoundVarList = new ArrayList<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>>();
		int i = 0, numTags = focusTags.size();

		ExpressionNode restriction = null;
		
		for (PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode> boundVars : boundVarList) {
			SequenceNode<VariableDeclarationNode> varDecls = boundVars
					.getLeft();
			RegularRangeNode domain = (RegularRangeNode) boundVars.getRight();
			List<VariableDeclarationNode> newVarDecls = new ArrayList<VariableDeclarationNode>();
			for (VariableDeclarationNode varDecl : varDecls) {
				if (i == numTags) {
					newVarDecls.add(varDecl.copy());
				} else {
					String focusVarName = focusData
							.getVarNameFromTag(focusTags.get(i));
					replacementMap.put(varDecl.getName(), focusVarName);
					if (domain != null) {
						ExpressionNode focusVarRestriction = genVarRestriction(
								focusVarName, domain);
						restriction = restriction == null
								? focusVarRestriction
								: nodeFactory.newOperatorNode(newSource("transformForall", "&&"),
										OperatorNode.Operator.LAND, restriction,
										focusVarRestriction);
					}
					i++;
				}
			}
			if (!newVarDecls.isEmpty()) {
				SequenceNode<VariableDeclarationNode> newVarDeclsNode = nodeFactory
						.newSequenceNode(varDecls.getSource(),
								"bounded var decls", newVarDecls);
				newBoundVarList
						.add(nodeFactory.newPairNode(boundVars.getSource(),
								newVarDeclsNode, boundVars.getRight().copy()));
			}
		}
		ExpressionNode newForallExprNode = (ExpressionNode) replaceFocusedVars(
				forallNode.expression().copy(), replacementMap);
		ExpressionNode transformedBody = newBoundVarList.isEmpty()
				? newForallExprNode
				: nodeFactory.newQuantifiedExpressionNode(
						forallNode.getSource(), Quantifier.FORALL,
						nodeFactory.newSequenceNode(boundVarList.getSource(),
								"bound var list", newBoundVarList),
						forallNode.restriction(), newForallExprNode);
		return restriction == null
				? transformedBody
				: nodeFactory.newOperatorNode(forallNode.getSource(),
						OperatorNode.Operator.LOR,
						nodeFactory.newOperatorNode(forallNode.getSource(),
								OperatorNode.Operator.NOT, restriction),
						transformedBody);
	}
	
	private ASTNode replaceFocusedVars(ASTNode node,
			HashMap<String, String> replacementMap) {
		if (node == null) {
			return null;
		}
		if (node instanceof IdentifierNode) {
			IdentifierNode identNode = (IdentifierNode) node;
			String name = identNode.name();
			if (replacementMap.containsKey(name)) {
				return nodeFactory.newIdentifierNode(identNode.getSource(),
						replacementMap.get(name));
			}
		}
		for (int i = 0; i < node.numChildren(); i++) {
			ASTNode child = node.child(i);
			node.removeChild(i);
			node.setChild(i, replaceFocusedVars(child, replacementMap));
		}
		return node;
	}
	
	@Override
	public ContractNode copy() {
		return new CommonFocusAssertTransformNode(this.getSource(), nodeFactory,
				tokenFactory, focusTags);
	}

	@Override
	public List<String> getFocusTags() {
		return focusTags;
	}

	@Override
	public FocusKind getFocusKind() {
		return FocusKind.ASSERT;
	}
	
	@Override
	protected void printBody(PrintStream out) {
		out.print("focusAssert");
	}

}
