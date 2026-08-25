package dev.civl.abc.ast.node.IF.expression;

import dev.civl.abc.ast.node.IF.SequenceNode;

/**
 * Node representing a function call. The children node include the expression
 * of function type and the sequence of actual arguments.
 * 
 * <p>
 * Note: the order of the children nodes is unspecified. Use the specific
 * methods (e.g., {@link #setFunction(ExpressionNode)}) to read or change a
 * child node.
 * </p>
 * 
 * @author siegel
 * 
 */
public interface FunctionCallNode extends ExpressionNode {

	/**
	 * The function being called.
	 * 
	 * @return the expression of function type which evaluates to the function being
	 *         called; the most common case is an identifier expression giving the
	 *         name of a function
	 */
	ExpressionNode getFunction();

	/**
	 * Sets the value returned by {@link #getFunction()}.
	 * 
	 * @param function the expression of function type which evaluates to the
	 *                 function being called; the most common case is an identifier
	 *                 expression giving the name of a function
	 */
	void setFunction(ExpressionNode function);

	/**
	 * Returns the number of actual execution context arguments occurring in this
	 * function call.
	 * 
	 * @return the number of actual execution context arguments
	 */
	int getNumberOfContextArguments();

	/**
	 * Returns the number of actual arguments occurring in this function call.
	 * 
	 * @return the number of actual arguments
	 */
	int getNumberOfArguments();

	/**
	 * Returns the index-th execution context argument, indexed from 0.
	 * 
	 * @return the index-th actual execution context argument
	 */
	ExpressionNode getContextArgument(int index);

	/**
	 * Returns the arguments of this function call node, which is a sequence node of
	 * expressions.
	 * 
	 * @return the arguments of this function call node
	 */
	SequenceNode<ExpressionNode> getArguments();

	/**
	 * Returns the index-th argument, indexed from 0.
	 * 
	 * @return the index-th actual argument
	 */
	ExpressionNode getArgument(int index);

	/**
	 * Sets the index-th execution context argument.
	 * 
	 * @param index nonnegative integer
	 * @param value expression node to use as index-th actual execution context
	 *              argument
	 */
	void setContextArgument(int index, ExpressionNode value);

	/**
	 * Sets the index-th argument.
	 * 
	 * @param index nonnegative integer
	 * @param value expression node to use as index-th actual argument
	 */
	void setArgument(int index, ExpressionNode value);

	@Override
	FunctionCallNode copy();

	/**
	 * Updates the actual execution context parameters of the function call node.
	 * 
	 * @param arguments The actual execution context parameters to be used to update
	 *                  the function call node.
	 */
	void setContextArguments(SequenceNode<ExpressionNode> arguments);

	/**
	 * Updates the actual parameters of the function call node.
	 * 
	 * @param arguments The actual parameters to be used to update the function call
	 *                  node.
	 */
	void setArguments(SequenceNode<ExpressionNode> arguments);

}
