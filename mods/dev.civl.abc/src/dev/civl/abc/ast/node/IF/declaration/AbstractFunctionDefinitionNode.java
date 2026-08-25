package dev.civl.abc.ast.node.IF.declaration;

import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;

/**
 * <p>
 * An abstract function definition contains the information for an abstract
 * function (i.e. a function in the mathematical sense, treated as uninterpreted
 * in the code).
 * </p>
 * 
 * <p>
 * An abstract function has an identifier, return type, and parameters.
 * </p>
 * 
 * @author zirkel
 * 
 */
public interface AbstractFunctionDefinitionNode extends FunctionDeclarationNode {

	@Override
	AbstractFunctionDefinitionNode copy();

	/**
	 * @return the StringLiteralNode representing an optional attribute attached to
	 *         the abstract function, if there is an attribute. <code>null</code>,
	 *         otherwise.
	 */
	StringLiteralNode getAttribute();

}
