package dev.civl.abc.ast.node.IF.acsl;

/**
 * Node representing the "pure;" clause in an ACSL contract.
 */
public interface PureNode extends ContractNode {
	@Override
	PureNode copy();
}
