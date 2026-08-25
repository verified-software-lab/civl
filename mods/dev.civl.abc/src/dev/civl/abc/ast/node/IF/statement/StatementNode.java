package dev.civl.abc.ast.node.IF.statement;

import dev.civl.abc.ast.node.IF.PragmaNode;
import dev.civl.abc.ast.node.IF.omp.OmpNode;

public interface StatementNode extends BlockItemNode {

	public enum StatementKind {
		/**
		 * A CIVL-C <code>$atomic</code> statement. Can be cast to {@link AtomicNode}
		 */
		ATOMIC,
		/**
		 * A CIVL-C <code>$choose</code> statement. Can be cast to
		 * {@link ChooseStatementNode}
		 */
		CHOOSE,
		/**
		 * A CIVL-C <code>$for</code> or <code>$parfor</code> statement. Can be cast to
		 * {@link CivlForNode}
		 */
		CIVL_FOR,
		/**
		 * A compound statement, which is wrapped by a pair of <code>{}</code>. Can be
		 * cast to {@link CompoundStatementNode}
		 */
		COMPOUND,
		/**
		 * An expression statement. Can be cast to {@link ExpressionStatementNode}
		 */
		EXPRESSION,
		/**
		 * An <code>if-else</code> (where <code>else</code> part is optional). Can be
		 * cast to {@link IfNode}
		 */
		IF,
		/**
		 * A jump statement, which could be one of <code>break</code>,
		 * <code>continue</code>, <code>go to</code> and <code>return </code>. Can be
		 * cast to {@link JumpNode}
		 */
		JUMP,
		/**
		 * A labeled statement. Can be cast to {@link LabeledStatementNode}
		 */
		LABELED,
		/**
		 * A loop statement, which could be a <code>for</code>, <code>while</code> or
		 * <code>do-while</code> loop. Can be cast to {@link LoopNode}.
		 */
		LOOP,
		/**
		 * A null statement. Can be cast to {@link NullStatementNode}
		 */
		NULL,
		/**
		 * A statement which has OpenMP pragmas. Can be cast to {@link OmpNode}
		 */
		OMP,
		/**
		 * A statement node which has unknown pragmas. Can be cast to {@link PragmaNode}
		 */
		PRAGMA,
		/**
		 * A CIVL-C <code>$run</code> statement. Can be cast to {@link RunNode}.
		 */
		RUN,
		/**
		 * A <code>switch</code> statement. Can be cast to {@link SwitchNode}
		 */
		SWITCH,
		/**
		 * A CIVL-C guarded statement (<code>$when</code>). Can be cast to
		 * {@link WhenNOde}
		 */
		WHEN
	}

	@Override
	StatementNode copy();

	/**
	 * Different statement nodes have different statement kind. For example, a while
	 * statement node has the statement kind WHILE, an if statement node has the
	 * kind IF, etc.
	 * 
	 * @return The statement kind defined as an enum element
	 */
	StatementKind statementKind();
}
