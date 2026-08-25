/**
 * 
 */
package dev.civl.mc.model.IF;

/**
 * An abstract function is an uninterpreted mathematical function. It is used in
 * assumptions and assertions to relate values in code to the actual
 * mathematical functions they represent.
 * 
 * @author zirkel
 * 
 */
public interface AbstractFunction extends CIVLFunction {

	/**
	 * The kind of a special relation represented by this abstract function
	 * 
	 * @author ziqingluo
	 *
	 */
	public static enum SpecialRelationKind {
		/**
		 * this abstract function is not representing a special relation
		 */
		NONE,
		/**
		 * this abstract function represents a partial order binary relation
		 */
		PARTIAL_ORDER,
		/**
		 * this abstract function represents a tree order binary relation
		 */
		TREE_ORDER,
		/**
		 * this abstract function represents a linear order binary relation
		 */
		LINEAR_ORDER,
		/**
		 * this abstract function represents a piecewise linear order binary
		 * relation
		 */
		PIECEWISE_LINEAR_ORDER
	}

	/**
	 * @return {@link SpecialRelationKind} of a special relation (or NONE)
	 *         represented by this abstract function
	 */
	SpecialRelationKind getAttribute();
}
