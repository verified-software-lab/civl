package dev.civl.abc.ast.node.common.acsl;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.acsl.FocusOrderedTransformNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.IntegerConstantNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode.Quantifier;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.type.BasicTypeNode;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;

public class CommonFocusOrderedTransformNode extends CommonFocusTransformNode
		implements
			FocusOrderedTransformNode {
	private String focusTag;

	public CommonFocusOrderedTransformNode(Source source,
			NodeFactory nodeFactory, TokenFactory tokenFactory, String focusTag,
			OperatorNode operator, RegularRangeNode range,
			ExpressionNode expr) {
		super(source, nodeFactory, tokenFactory,
				Arrays.asList(operator, range, expr));
		this.focusTag = focusTag;
	}

	@Override
	public FocusKind getFocusKind() {
		return FocusKind.ORDERED;
	}

	@Override
	public List<BlockItemNode> transform(List<BlockItemNode> items)
			throws SyntaxException {
		String focusVarName = focusData.getVarNameFromTag(focusTag);
//		String altFocusVarName = focusData.getAltVarNameFromTag(focusTag);
		List<BlockItemNode> result = new LinkedList<>();
		result.add(
				nodeFactory.newExpressionStatementNode(functionCall("$assert",
						Arrays.asList(implies(
								genOrderContext(focusVarName, 0, 1),
								genOrderExpression(focusVarName, 0, 1))))));
//		SortedSet<Integer> offsets = computeOrderedOffsets();
//		int firstOffset = offsets.first();
//		int lastOffset = firstOffset - 1;
//		for (Integer offset : offsets) {
//			if (lastOffset != firstOffset-1) {
//				result.add(nodeFactory.newExpressionStatementNode(functionCall(
//						"$assume",
//						Arrays.asList(implies(
//								genOrderContext(focusVarName, lastOffset,
//										offset),
//								genOrderExpression(focusVarName, lastOffset,
//										offset))))));
//				result.add(nodeFactory.newExpressionStatementNode(functionCall(
//						"$assume",
//						Arrays.asList(implies(
//								genOrderContext(altFocusVarName, lastOffset,
//										offset),
//								genOrderExpression(altFocusVarName, lastOffset,
//										offset))))));
//			}
//
//			lastOffset = offset;
//		}
//		result.add(nodeFactory.newExpressionStatementNode(functionCall(
//				"$assume", Arrays
//						.asList(implies(
//								genOrderContext(getRangeLow().copy(),
//										genVarWithOffset(focusVarName,
//												firstOffset),
//										true),
//								genOrderExpression(getRangeLow().copy(),
//										genVarWithOffset(focusVarName,
//												firstOffset)))))));
//		result.add(
//				nodeFactory.newExpressionStatementNode(functionCall("$assume",
//						Arrays.asList(implies(
//								genOrderContext(
//										genVarWithOffset(focusVarName,
//												lastOffset),
//										getRangeHigh().copy(), true),
//								genOrderExpression(
//										genVarWithOffset(focusVarName,
//												lastOffset),
//										getRangeHigh().copy()))))));
//		result.add(nodeFactory.newExpressionStatementNode(functionCall(
//				"$assume", Arrays
//						.asList(implies(
//								genOrderContext(getRangeLow().copy(),
//										genVarWithOffset(altFocusVarName,
//												firstOffset),
//										true),
//								genOrderExpression(getRangeLow().copy(),
//										genVarWithOffset(altFocusVarName,
//												firstOffset)))))));
//		result.add(
//				nodeFactory.newExpressionStatementNode(functionCall("$assume",
//						Arrays.asList(implies(
//								genOrderContext(
//										genVarWithOffset(altFocusVarName,
//												lastOffset),
//										getRangeHigh().copy(), true),
//								genOrderExpression(
//										genVarWithOffset(altFocusVarName,
//												lastOffset),
//										getRangeHigh().copy()))))));
//		result.add(nodeFactory.newExpressionStatementNode(functionCall(
//				"$assume",
//				Arrays.asList(implies(genOrderContext(
//						genVarWithOffset(altFocusVarName, lastOffset),
//						genVarWithOffset(focusVarName, firstOffset), true),
//						genOrderExpression(
//								genVarWithOffset(altFocusVarName, lastOffset),
//								genVarWithOffset(focusVarName,
//										firstOffset)))))));
//		result.add(nodeFactory.newExpressionStatementNode(
//				functionCall("$assume", Arrays.asList(implies(genOrderContext(
//						genVarWithOffset(focusVarName, lastOffset),
//						genVarWithOffset(altFocusVarName, firstOffset), true),
//						genOrderExpression(
//								genVarWithOffset(focusVarName, lastOffset),
//								genVarWithOffset(altFocusVarName,
//										firstOffset)))))));

		// Build a single $assume($forall (int i: lo..hi; int j: lo..hi)
		//   !(i op j) || (expr[i] op expr[j]))
		// where op is the user-specified ordering operator and lo..hi is the
		// range from the ordered annotation.
		Source forallSource = newSource("transform", "$forall assumption");
		String iName = "i";
		String jName = "j";

		// int type nodes for each bound variable
		BasicTypeNode intTypeI = nodeFactory.newBasicTypeNode(forallSource,
				BasicTypeKind.INT);
		BasicTypeNode intTypeJ = nodeFactory.newBasicTypeNode(forallSource,
				BasicTypeKind.INT);

		// Variable declarations: int i, int j
		VariableDeclarationNode iDecl = nodeFactory.newVariableDeclarationNode(
				forallSource, identifier(iName), intTypeI);
		VariableDeclarationNode jDecl = nodeFactory.newVariableDeclarationNode(
				forallSource, identifier(jName), intTypeJ);

		// Wrap each declaration in a singleton sequence
		SequenceNode<VariableDeclarationNode> iDeclSeq = nodeFactory
				.newSequenceNode(forallSource, "i decl", Arrays.asList(iDecl));
		SequenceNode<VariableDeclarationNode> jDeclSeq = nodeFactory
				.newSequenceNode(forallSource, "j decl", Arrays.asList(jDecl));

		// Domain ranges lo..hi for each bound variable
		RegularRangeNode iDomain = nodeFactory.newRegularRangeNode(forallSource,
				getRangeLow().copy(), getRangeHigh().copy());
		RegularRangeNode jDomain = nodeFactory.newRegularRangeNode(forallSource,
				getRangeLow().copy(), getRangeHigh().copy());

		// Pair (decl list, domain) for each group
		PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode> iPair =
				nodeFactory.newPairNode(forallSource, iDeclSeq, iDomain);
		PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode> jPair =
				nodeFactory.newPairNode(forallSource, jDeclSeq, jDomain);

		// Bound variable list: (int i: lo..hi; int j: lo..hi)
		SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVarList =
				nodeFactory.newSequenceNode(forallSource, "bound var list",
						Arrays.asList(iPair, jPair));

		// Body: implies(i < j, expr[i] op expr[j])
		// The antecedent always uses strict less-than (i < j) to capture the
		// ordering of indices; the consequent uses the user's chosen operator.
		ExpressionNode iForContext = identifierExpression(iName);
		ExpressionNode jForContext = identifierExpression(jName);
		ExpressionNode orderContext = nodeFactory.newOperatorNode(forallSource,
				Operator.LT, iForContext, jForContext);
		ExpressionNode forallBody = implies(orderContext,
				genOrderExpression(identifierExpression(iName),
						identifierExpression(jName)));

		// $forall (int i: lo..hi; int j: lo..hi) !(i op j) || (expr[i] op expr[j])
		QuantifiedExpressionNode forallExpr = nodeFactory
				.newQuantifiedExpressionNode(forallSource, Quantifier.FORALL,
						boundVarList, null, forallBody);

		result.add(nodeFactory.newExpressionStatementNode(
				functionCall("$assume", Arrays.asList((ExpressionNode) forallExpr))));

		return result;
	}
	
	private ExpressionNode genOrderContext(String focusVarName, int offset1,
			int offset2) {
		return genOrderContext(genVarWithOffset(focusVarName, offset1),
				genVarWithOffset(focusVarName, offset2), false);
	}
	
	private ExpressionNode genOrderContext(ExpressionNode lowExprNode, ExpressionNode highExprNode, boolean orderExprs) {
		String thisFuncName = "genOrderContext";
		Source thisSource = newSource(thisFuncName,
				lowExprNode.toString()+" and "+highExprNode.toString());
		ExpressionNode lowerBoundNode = nodeFactory.newOperatorNode(thisSource,
				Operator.LTE, getRangeLow().copy(), lowExprNode);
		ExpressionNode upperBoundNode = nodeFactory.newOperatorNode(thisSource,
				Operator.LTE, highExprNode,
				getRangeHigh().copy());

		return orderExprs
				? andExpr(lowerBoundNode,
						nodeFactory.newOperatorNode(thisSource, Operator.LT,
								lowExprNode.copy(), highExprNode.copy()),
						upperBoundNode)
				: andExpr(lowerBoundNode, upperBoundNode);
	}

	private ExpressionNode genOrderExpression(String focusVarName, int offset1,
			int offset2) {
		return genOrderExpression(genVarWithOffset(focusVarName, offset1),
				genVarWithOffset(focusVarName, offset2));
	}

	private ExpressionNode genOrderExpression(
			ExpressionNode lowReplacement,
			ExpressionNode highReplacement) {
		String thisFuncName = "genOrderExpression";
		Source thisSource = newSource(thisFuncName,
				"(" + lowReplacement.toString() + ", "
						+ highReplacement.toString() + ") ordered result");
		return nodeFactory.newOperatorNode(thisSource, getOperator(),
				replaceIdent(getOrderedExpression(), focusTag, lowReplacement),
				replaceIdent(getOrderedExpression(), focusTag,
						highReplacement));
	}
	
	private ExpressionNode genVarWithOffset(String focusVarName, int offset) {
		return addIntExpr(identifierExpression(focusVarName), offset);
	}

	/**
	 * Computes the set of offsets to use for generating ordering assumptions.
	 * If the ordered expression is an array subscript whose last index is
	 * exactly the focus tag (e.g. {@code A[0][T]}), this method attempts to
	 * derive offsets by matching the expression's base against each protected
	 * memory expression stored in {@code focusData}. A protected memory
	 * expression of the form {@code A[0][T+C .. T+D]} contributes both
	 * {@code C} and {@code D}; one of the form {@code A[0][T+C]} contributes
	 * just {@code C}. If no matches are found, or if the ordered expression
	 * does not have the required form, falls back to
	 * {@link FocusAnalysisData#getFocusOffsets}.
	 */
	@SuppressWarnings("unused")
	private SortedSet<Integer> computeOrderedOffsets() {
		ExpressionNode orderedExpr = getOrderedExpression();
		if (!isArrayAccessByTag(orderedExpr))
			return focusData.getFocusOffsets(focusTag);

		ExpressionNode orderedBase = ((OperatorNode) orderedExpr).getArgument(0);
		SortedSet<Integer> offsets = new TreeSet<>();
		for (ExpressionNode protExpr : focusData.getProtectedMemExprs(focusTag)) {
			if (protExpr.expressionKind() != ExpressionKind.OPERATOR)
				continue;
			OperatorNode subscript = (OperatorNode) protExpr;
			if (subscript.getOperator() != Operator.SUBSCRIPT)
				continue;
			ExpressionNode protBase = subscript.getArgument(0);
			ExpressionNode protIndex = subscript.getArgument(1);
			if (!orderedBase.equiv(protBase))
				continue;
			if (protIndex.expressionKind() == ExpressionKind.REGULAR_RANGE) {
				RegularRangeNode range = (RegularRangeNode) protIndex;
				Integer low = extractTagOffset(range.getLow());
				Integer high = extractTagOffset(range.getHigh());
				if (low != null)
					offsets.add(low);
				if (high != null)
					offsets.add(high);
			} else {
				Integer offset = extractTagOffset(protIndex);
				if (offset != null)
					offsets.add(offset);
			}
		}
		return offsets.isEmpty() ? focusData.getFocusOffsets(focusTag) : offsets;
	}

	/**
	 * Returns {@code true} iff {@code expr} is a SUBSCRIPT operator whose
	 * index argument is exactly the identifier for the focus tag (e.g.
	 * {@code A[0][T]} where {@code T} is the focus tag).
	 */
	private boolean isArrayAccessByTag(ExpressionNode expr) {
		if (expr.expressionKind() != ExpressionKind.OPERATOR)
			return false;
		OperatorNode op = (OperatorNode) expr;
		if (op.getOperator() != Operator.SUBSCRIPT)
			return false;
		ExpressionNode index = op.getArgument(1);
		return index.expressionKind() == ExpressionKind.IDENTIFIER_EXPRESSION
				&& ((IdentifierExpressionNode) index).getIdentifier().name()
						.equals(focusTag);
	}

	/**
	 * If {@code expr} has the form {@code T}, {@code T+C}, or {@code T-C}
	 * (where {@code T} is the focus tag and {@code C} is an integer constant),
	 * returns the integer offset {@code C} (possibly 0 or negative). Returns
	 * {@code null} if the expression does not match any of these patterns.
	 */
	private Integer extractTagOffset(ExpressionNode expr) {
		if (expr.expressionKind() == ExpressionKind.IDENTIFIER_EXPRESSION) {
			if (((IdentifierExpressionNode) expr).getIdentifier().name()
					.equals(focusTag))
				return 0;
			return null;
		}
		if (expr.expressionKind() != ExpressionKind.OPERATOR)
			return null;
		OperatorNode op = (OperatorNode) expr;
		ExpressionNode arg0 = op.getArgument(0);
		ExpressionNode arg1 = op.getArgument(1);
		boolean arg0IsTag = arg0.expressionKind() == ExpressionKind.IDENTIFIER_EXPRESSION
				&& ((IdentifierExpressionNode) arg0).getIdentifier().name()
						.equals(focusTag);
		if (op.getOperator() == Operator.PLUS && arg0IsTag) {
			if (arg1 instanceof IntegerConstantNode)
				return ((IntegerConstantNode) arg1).getConstantValue()
						.getIntegerValue().intValue();
			// T + (-C) represented as T + UNARYMINUS(C)
			if (arg1.expressionKind() == ExpressionKind.OPERATOR) {
				OperatorNode unary = (OperatorNode) arg1;
				if (unary.getOperator() == Operator.UNARYMINUS
						&& unary.getArgument(0) instanceof IntegerConstantNode)
					return -((IntegerConstantNode) unary.getArgument(0))
							.getConstantValue().getIntegerValue().intValue();
			}
		}
		if (op.getOperator() == Operator.MINUS && arg0IsTag
				&& arg1 instanceof IntegerConstantNode)
			return -((IntegerConstantNode) arg1).getConstantValue()
					.getIntegerValue().intValue();
		return null;
	}

	public Operator getOperator() {
		return ((OperatorNode) child(0)).getOperator();
	}
	
	public ExpressionNode getRangeLow() {
		return getRange().getLow();
	}
	
	public ExpressionNode getRangeHigh() {
		return getRange().getHigh();
	}
	
	private RegularRangeNode getRange() {
		return (RegularRangeNode) child(1);
	}
	
	public ExpressionNode getOrderedExpression() {
		return (ExpressionNode) child(2);
	}

	@Override
	public ContractNode copy() {
		return new CommonFocusOrderedTransformNode(this.getSource(),
				nodeFactory, tokenFactory, focusTag,
				(OperatorNode) child(0).copy(),
				(RegularRangeNode) child(1).copy(),
				(ExpressionNode) child(2).copy());
	}

	@Override
	protected void printBody(PrintStream out) {
		out.print("focus ordered");
	}

}
