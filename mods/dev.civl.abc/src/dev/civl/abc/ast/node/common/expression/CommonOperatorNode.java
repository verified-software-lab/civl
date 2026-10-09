package dev.civl.abc.ast.node.common.expression;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import dev.civl.abc.ast.IF.ASTException;
import dev.civl.abc.ast.IF.DifferenceObject;
import dev.civl.abc.ast.conversion.IF.Conversion;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.token.IF.Source;

public class CommonOperatorNode extends CommonExpressionNode
		implements
			OperatorNode {

	private Operator operator;

	public CommonOperatorNode(Source source, Operator operator,
			List<ExpressionNode> arguments) {
		super(source, arguments);
		this.operator = operator;
	}

	@Override
	public Operator getOperator() {
		return operator;
	}

	@Override
	public void setOperator(Operator operator) {
		this.operator = operator;
	}

	@Override
	public int getNumberOfArguments() {
		return this.numChildren();
	}

	@Override
	public ExpressionNode getArgument(int index) {
		return (ExpressionNode) child(index);
	}

	@Override
	public void setArgument(int index, ExpressionNode value) {
		setChild(index, value);
	}

	@Override
	protected void printBody(PrintStream out) {
		out.print("OperatorNode[operator=" + operator + "]");
	}

	private boolean hasConstantOperator() {
		switch (operator) {
			case ASSIGN :
			case BITANDEQ :
			case BITOREQ :
			case BITXOREQ :
			case DIVEQ :
			case MINUSEQ :
			case MODEQ :
			case PLUSEQ :
			case POSTDECREMENT :
			case POSTINCREMENT :
			case PREDECREMENT :
			case PREINCREMENT :
			case SHIFTLEFTEQ :
			case SHIFTRIGHTEQ :
			case TIMESEQ :
				return false;
			default :
				return true;
		}
	}

	@Override
	public boolean isConstantExpression() {
		int numArgs = getNumberOfArguments();

		if (!hasConstantOperator())
			return false;
		for (int i = 0; i < numArgs; i++) {
			if (!getArgument(i).isConstantExpression())
				return false;
		}
		return true;
	}

	@Override
	public OperatorNode copy() {
		List<ExpressionNode> arguments = new LinkedList<ExpressionNode>();
		int numArgs = getNumberOfArguments();

		for (int i = 0; i < numArgs; i++)
			arguments.add(duplicate(getArgument(i)));
		return new CommonOperatorNode(getSource(), getOperator(), arguments);
	}

	@Override
	public ExpressionKind expressionKind() {
		return ExpressionKind.OPERATOR;
	}

	@Override
	public boolean isSideEffectFree(boolean errorsAreSideEffects) {
		switch (getOperator()) {
			// always have side effects:
			case ASSIGN :
			case MINUSEQ :
			case PLUSEQ :
			case POSTDECREMENT :
			case POSTINCREMENT :
			case PREDECREMENT :
			case PREINCREMENT :
			case DIVEQ :
			case MODEQ :
			case TIMESEQ :
			case BITANDEQ :
			case BITOREQ :
			case BITXOREQ :
			case SHIFTLEFTEQ :
			case SHIFTRIGHTEQ :
				return false;
			// possible numeric arithmetic error only:
			case DIV :
			case MOD :
			case TIMES :
			case UNARYMINUS :
				if (errorsAreSideEffects) {
					// perhaps check if expressions are constants that are not
					// 0,
					// for example. But overflow could also be a problem.
					return false;
				}
				break;
			// possible pointer or numeric arithmetic error:
			case MINUS :
			case PLUS :
				// If the PLUS or MINUS is part of the pointer addition
				// operation, then it may have error side-effects. Else, it is
				// numerical operation which will not have error side-effects:
				if (errorsAreSideEffects)
					if (getArgument(0).getType().kind() == TypeKind.POINTER
							|| getArgument(1).getType()
									.kind() == TypeKind.POINTER)
						return false;
				break;
			// always a problem:
			case DEREFERENCE :
			case SUBSCRIPT :
				if (errorsAreSideEffects) {
					return false;
				}
				break;
			// innocuous unless operands have side effects...
			case ADDRESSOF :
			case BIG_O :
			case BITAND :
			case BITCOMPLEMENT :
			case BITOR :
			case BITXOR :
			case COMMA :
			case CONDITIONAL :
			case EQUALS :
			case GT :
			case GTE :
			case IMPLIES :
			case LAND :
			case LOR :
			case LT :
			case LTE :
			case NEQ :
			case NOT :
			case SHIFTLEFT :
			case SHIFTRIGHT :
			case UNARYPLUS :
			case HASH :
				break;
			default :
				break;
		} // end of switch
			// default case: check operands...
		for (int i = 0; i < getNumberOfArguments(); i++) {
			if (!getArgument(i).isSideEffectFree(errorsAreSideEffects))
				return false;
		}
		return true;
	}

	@Override
	protected DifferenceObject diffWork(ASTNode that) {
		if (that instanceof OperatorNode)
			if (this.operator == ((OperatorNode) that).getOperator())
				return null;
		return new DifferenceObject(this, that);
	}

	@Override
	public ASTNode setChild(int index, ASTNode child) {
		if (!(child == null || child instanceof ExpressionNode))
			throw new ASTException("Child of CommonOperatorNode at index "
					+ index + " must be a ExpressionNode, but saw " + child
					+ " with type " + child.nodeKind());
		return super.setChild(index, child);
	}

	@Override
	public void addConversion(Conversion conversion) {
		Type lastType = getConvertedType();

		if (lastType == null)
			throw new RuntimeException(
					"Internal error: adding conversion before initial type defined");
		if (!lastType.equals(conversion.getOldType()))
			throw new IllegalArgumentException(
					"Old type of conversion is not last type:\n"
							+ conversion.getOldType() + "\n" + lastType);
		conversions.add(conversion);
	}
}
