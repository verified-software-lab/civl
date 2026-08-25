package dev.civl.abc.ast.node.common.expression;

import java.io.PrintStream;

import dev.civl.abc.ast.IF.ASTException;
import dev.civl.abc.ast.IF.DifferenceObject;
import dev.civl.abc.ast.IF.DifferenceObject.DiffKind;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode;
import dev.civl.abc.token.IF.Source;

/**
 * A quantified expression consists of a quantifier, a variable bound by the
 * quantifier, an expression restricting the values of the quantified variable,
 * and a quantified expression. e.g. forall {int x | x > 1} x > 0;
 * 
 * @author zirkel
 * 
 */
public class CommonQuantifiedExpressionNode extends CommonExpressionNode implements QuantifiedExpressionNode {

	private Quantifier quantifier;

	/**
	 * @param source       The source code information for this expression.
	 * @param quantifier   The quantifier for this expression. One of {FORALL,
	 *                     EXISTS, UNIFORM}.
	 * @param variableList The list of bound variable declarations.
	 * @param restriction  Boolean-valued expression
	 * @param expression   the expression that is quantified
	 */
	public CommonQuantifiedExpressionNode(Source source, Quantifier quantifier,
			SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> variableList,
			ExpressionNode restriction, ExpressionNode expression) {
		super(source, variableList, restriction, expression);
		this.quantifier = quantifier;
	}

	@Override
	public boolean isConstantExpression() {
		return false;
	}

	@Override
	public ExpressionNode copy() {
		return new CommonQuantifiedExpressionNode(this.getSource(), quantifier, duplicate(boundVariableList()),
				duplicate(restriction()), duplicate(expression()));
	}

	@Override
	public Quantifier quantifier() {
		return quantifier;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableList() {
		return (SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>>) this.child(0);
	}

	@Override
	public ExpressionNode restriction() {
		return (ExpressionNode) this.child(1);
	}

	@Override
	public ExpressionNode expression() {
		return (ExpressionNode) this.child(2);
	}

	@Override
	protected void printBody(PrintStream out) {
		String output = "";

		switch (quantifier) {
		case FORALL:
			output = "forall";
			break;
		case EXISTS:
			output = "exists";
			break;
		}
		out.print(output);
	}

	@Override
	public ExpressionKind expressionKind() {
		return ExpressionKind.QUANTIFIED_EXPRESSION;
	}

	@Override
	public boolean isSideEffectFree(boolean errorsAreSideEffects) {
		boolean result = expression().isSideEffectFree(errorsAreSideEffects);

		if (restriction() != null)
			result = result && this.restriction().isSideEffectFree(errorsAreSideEffects);
		return result;
	}

	@Override
	protected DifferenceObject diffWork(ASTNode that) {
		if (that instanceof QuantifiedExpressionNode) {
			QuantifiedExpressionNode thatQuan = (QuantifiedExpressionNode) that;

			if (this.quantifier == thatQuan.quantifier())
				return null;
			else
				return new DifferenceObject(this, that, DiffKind.OTHER, "different quantifier");
		}
		return new DifferenceObject(this, that);
	}

	@Override
	public ASTNode setChild(int index, ASTNode child) {
		if (index >= 4)
			throw new ASTException("CommonQuantifiedExpressionNode has four children, but saw index " + index);
		switch (index) {
		case 0:
			if (!(child == null || child instanceof SequenceNode))
				throw new ASTException("Child of CommonQuantifiedExpressionNode at index " + index
						+ " must be a SequenceNode, but saw " + child + " with type " + child.nodeKind());
			break;
		case 1:
			if (!(child == null || child instanceof ExpressionNode))
				throw new ASTException("Child of CommonQuantifiedExpressionNode at index " + index
						+ " must be a ExpressionNode, but saw " + child + " with type " + child.nodeKind());
			break;
		case 2:
			if (!(child == null || child instanceof ExpressionNode))
				throw new ASTException("Child of CommonQuantifiedExpressionNode at index " + index
						+ " must be a ExpressionNode, but saw " + child + " with type " + child.nodeKind());
			break;
		default:
			break;
		}
		return super.setChild(index, child);
	}
}
