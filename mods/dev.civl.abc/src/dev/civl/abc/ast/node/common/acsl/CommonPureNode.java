package dev.civl.abc.ast.node.common.acsl;

import java.io.PrintStream;

import dev.civl.abc.ast.IF.ASTException;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.acsl.PureNode;
import dev.civl.abc.token.IF.Source;

public class CommonPureNode extends CommonContractNode implements PureNode {

	public CommonPureNode(Source source) {
		super(source);
	}

	@Override
	public PureNode copy() {
		return new CommonPureNode(this.getSource());
	}

	@Override
	protected void printBody(PrintStream out) {
		out.println("PureNode");
	}

	@Override
	public ASTNode setChild(int index, ASTNode child) {
		throw new ASTException("CommonPureNode has no child, but saw index " + index);
	}

	@Override
	public ContractKind contractKind() {
		return ContractKind.PURE;
	}
}
