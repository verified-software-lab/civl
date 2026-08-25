/**
 * 
 */
package dev.civl.abc.ast.node.IF.expression;

import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;

/**
 * A CIVL-C quantified expression, including three components, bound variable
 * declaration list, (optional) restriction and expression. It has the following
 * syntax:<br>
 * 
 * <pre>
 * quantified: 
 *   quantifier ( variable-decl-list | restrict? ) expression ;
 * 
 * variable-decl-list:
 *   variable-decl-sub-list (; variable-decl-sub-list)* ;
 *   
 * variable-decl-sub-list:
 *   type ID (, ID)* (: domain)?
 *   
 * quantifier: $forall | $exists
 * </pre>
 * 
 * e.g.,
 * 
 * <pre>
 * $forall (int x, y: dom; double z | x > 0 && z<5.9} x*z > -1
 * </pre>
 * 
 * @author zirkel, Manchun Zheng (zmanchun)
 * 
 */
public interface QuantifiedExpressionNode extends ExpressionNode {

	/**
	 * An enumerated type for the different quantifiers.
	 * 
	 * @author siegel
	 * 
	 */
	public enum Quantifier {
		/**
		 * The universal quantifier "for all".
		 */
		FORALL,
		/**
		 * The existential quantifier "there exists".
		 */
		EXISTS
	}

	/**
	 * Returns the quantifier.
	 * 
	 * @return The quantifier used by this quantifier expression.
	 */
	Quantifier quantifier();

	/**
	 * the bound variable declaration list, which is a sequence node of pairs of
	 * variable declaration list and an optional expression that has domain
	 * type. The dimension of the domain expression, if present, should agree
	 * with the number of variable declarations in the same pair. e.g.,
	 * <code>$forall(int i,j: dom1; double x, y | x&lt;y) a[i]*x &lt;= b[j]*y </code>
	 * This will have the bound variable list as: <code>{{{int i, int j}, dom1},
	 * {{double x, double y}, NULL}}</code>.
	 * 
	 * @return the bound variable declaration list
	 */
	SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableList();

	/**
	 * Returns the predicate which specifies the restriction on the domain of
	 * the bound variables.
	 * 
	 * @return the boolean expression involving the bound variable which
	 *         restricts the domain of that variable, or <code>null</code> if no
	 *         restriction is present.
	 */
	ExpressionNode restriction();

	/**
	 * The quantified expression.
	 * 
	 * @return The quantified expression.
	 */
	ExpressionNode expression();

}
