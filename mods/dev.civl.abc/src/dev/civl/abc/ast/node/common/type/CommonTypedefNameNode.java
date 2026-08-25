package dev.civl.abc.ast.node.common.type;

import java.io.PrintStream;

import dev.civl.abc.ast.IF.ASTException;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.type.TypedefNameNode;
import dev.civl.abc.token.IF.Source;

public class CommonTypedefNameNode extends CommonTypeNode implements TypedefNameNode {

	public CommonTypedefNameNode(Source source, IdentifierNode name) {
		super(source, TypeNodeKind.TYPEDEF_NAME, name);
	}

	@Override
	public IdentifierNode getName() {
		return (IdentifierNode) child(0);
	}

	@Override
	public void setName(IdentifierNode name) {
		setChild(0, name);
	}

	@Override
	protected void printBody(PrintStream out) {
		String qualifiers = qualifierString();

		out.print("TypedefName");
		if (!qualifiers.isEmpty())
			out.print("[" + qualifiers + "]");
	}

	@Override
	public TypedefNameNode copy() {
		CommonTypedefNameNode result = new CommonTypedefNameNode(getSource(), duplicate(getName()));

		result.copyData(result);
		return result;
	}

	@Override
	public ASTNode setChild(int index, ASTNode child) {
		if (index >= 1)
			throw new ASTException("CommonTypedefNameNode has two child, but saw index " + index);
		if (!(child == null || child instanceof IdentifierNode))
			throw new ASTException("Child of CommonTypedefNameNode at index " + index
					+ " must be a IdentifierNode, but saw " + child + " with type " + child.nodeKind());
		return super.setChild(index, child);
	}
}
