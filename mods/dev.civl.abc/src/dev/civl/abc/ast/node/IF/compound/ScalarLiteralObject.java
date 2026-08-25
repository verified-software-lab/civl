package dev.civl.abc.ast.node.IF.compound;

import dev.civl.abc.ast.node.IF.expression.ExpressionNode;

/**
 * A scalar literal object is defined by an ordinary expression that occurs
 * within a compound initializer. It is a leaf node in the tree that represents
 * the structure of the compound literal value.
 * 
 * @author siegel
 * 
 */
public interface ScalarLiteralObject extends LiteralObject {

	/**
	 * Returns the expression corresponding to this scalar literal object.
	 * 
	 * @return the corresponding expression
	 */
	ExpressionNode getExpression();

}
