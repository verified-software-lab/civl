package dev.civl.abc.ast.node.common.expression;

import java.io.PrintStream;

import dev.civl.abc.ast.IF.ASTException;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.common.CommonASTNode;
import dev.civl.abc.token.IF.Source;

public class CommonFunctionCallNode extends CommonExpressionNode implements FunctionCallNode {

	public CommonFunctionCallNode(Source source, ExpressionNode function, SequenceNode<ExpressionNode> contextArguments,
			SequenceNode<ExpressionNode> arguments) {
		super(source, function, contextArguments, arguments);
	}

	@Override
	public ExpressionNode getFunction() {
		return (ExpressionNode) child(0);
	}

	@Override
	public void setFunction(ExpressionNode function) {
		setChild(0, function);
	}

	@Override
	public int getNumberOfContextArguments() {
		if (child(1) != null)
			return child(1).numChildren();
		return 0;
	}

	@Override
	public int getNumberOfArguments() {
		return child(2).numChildren();
	}

	@Override
	public ExpressionNode getContextArgument(int index) {
		return (ExpressionNode) child(1).child(index);
	}

	@Override
	public ExpressionNode getArgument(int index) {
		return (ExpressionNode) child(2).child(index);
	}

	@Override
	public void setContextArgument(int index, ExpressionNode value) {
		((CommonASTNode) child(1)).setChild(index, value);
	}

	@Override
	public void setArgument(int index, ExpressionNode value) {
		((CommonASTNode) child(2)).setChild(index, value);
	}

	@Override
	public void setContextArguments(SequenceNode<ExpressionNode> arguments) {
		this.setChild(1, arguments);
	}

	@Override
	public void setArguments(SequenceNode<ExpressionNode> arguments) {
		this.setChild(2, arguments);
	}

	@Override
	protected void printBody(PrintStream out) {
		out.print("FunctionCallNode");
	}

	@Override
	public boolean isConstantExpression() {
		return false;
	}

	@Override
	public FunctionCallNode copy() {
		@SuppressWarnings("unchecked")
		SequenceNode<ExpressionNode> contextArguments = (SequenceNode<ExpressionNode>) child(1);
		@SuppressWarnings("unchecked")
		SequenceNode<ExpressionNode> arguments = (SequenceNode<ExpressionNode>) child(2);

		return new CommonFunctionCallNode(getSource(), duplicate(getFunction()), duplicate(contextArguments),
				duplicate(arguments));
	}

	@Override
	public ExpressionKind expressionKind() {
		return ExpressionKind.FUNCTION_CALL;
	}

	@Override
	public boolean isSideEffectFree(boolean errorsAreSideEffects) {
		ExpressionNode functionExpr = getFunction();
		boolean result = true;

		if (functionExpr instanceof IdentifierExpressionNode) {
			IdentifierNode functionIdentifier = ((IdentifierExpressionNode) functionExpr).getIdentifier();

			if (functionIdentifier.getEntity() == null) {
				// FIXME: Why do we need this? Not having this check was
				// causing a failure with ring2.cvl
				return false;
			}

			boolean isAtomicPureFunction = false;
			Entity functionEntity = functionIdentifier.getEntity();

			if (functionEntity instanceof Function) {
				Function function = (Function) functionEntity;

				isAtomicPureFunction = function.isLogic() || function.isAbstract()
						|| (function.isSystemFunction() && (function.isPure() || function.isStateFunction()));
			}
			if (isAtomicPureFunction) {
				for (int i = 0; i < getNumberOfContextArguments(); i++) {
					boolean argSEF = getContextArgument(i).isSideEffectFree(errorsAreSideEffects);

					if (!argSEF)
						return false;
				}
				if (result) {
					for (int i = 0; i < getNumberOfArguments(); i++) {
						boolean argSEF = getArgument(i).isSideEffectFree(errorsAreSideEffects);

						if (!argSEF)
							return false;
					}
				}
			} else {
				result = false;
			}
		} else {
			// Assume this isn't an abstract function.
			result = false;
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public SequenceNode<ExpressionNode> getArguments() {
		return (SequenceNode<ExpressionNode>) child(2);
	}

	@Override
	public ASTNode setChild(int index, ASTNode child) {
		if (index >= 3)
			throw new ASTException("CommonFunctionCallNode has 3 children, but saw index " + index);
		if (index == 0 && !(child == null || child instanceof ExpressionNode))
			throw new ASTException("Child of CommonFunctionCallNode at index " + index
					+ " must be a ExpressionNode, but saw " + child + " with type " + child.nodeKind());
		if (index > 0 && index < 3 && !(child == null || child instanceof SequenceNode))
			throw new ASTException("Child of CommonFunctionCallNode at index " + index
					+ " must be a SequenceNode, but saw " + child + " with type " + child.nodeKind());
		return super.setChild(index, child);
	}
}
