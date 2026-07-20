package dev.civl.abc.ast.node.IF.type;

import dev.civl.abc.ast.node.IF.IdentifierNode;

public interface TypedefNameNode extends TypeNode {

	IdentifierNode getName();

	void setName(IdentifierNode name);

	@Override
	TypedefNameNode copy();

}
