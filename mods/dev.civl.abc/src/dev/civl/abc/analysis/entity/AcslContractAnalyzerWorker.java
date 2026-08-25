package dev.civl.abc.analysis.entity;

import dev.civl.abc.ast.conversion.IF.ConversionFactory;
import dev.civl.abc.ast.entity.IF.BehaviorEntity;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.acsl.*;
import dev.civl.abc.ast.node.IF.acsl.ContractNode.ContractKind;
import dev.civl.abc.ast.node.IF.acsl.DependsEventNode.DependsEventNodeKind;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.UnsourcedException;

import java.util.HashMap;
import java.util.Map;

public class AcslContractAnalyzerWorker {
	/**
	 * The entity analyzer controlling this declaration analyzer.
	 */
	private EntityAnalyzer entityAnalyzer;

	private Map<String, BehaviorEntity> definedBehaviors = new HashMap<>();

	private ExpressionAnalyzer expressionAnalyzer;

	AcslContractAnalyzerWorker(EntityAnalyzer entityAnalyzer, ConversionFactory conversionFactory) {
		this.entityAnalyzer = entityAnalyzer;
		this.expressionAnalyzer = this.entityAnalyzer.expressionAnalyzer;
	}

	/**
	 * Do entity analysis on a whole contract block of a function.
	 * 
	 * @param contract
	 * @param result
	 * @throws SyntaxException
	 */
	void processContractNodes(SequenceNode<ContractNode> contract, Function result) throws SyntaxException {
		for (ContractNode contractClause : contract) {
			processContractNode(contractClause);
			result.addContract(contractClause);
		}
	}

	/**
	 * <p>
	 * Check if the given expression is a (set of) lvalue expression(s). An
	 * expression that is a (set of) lvalue expression(s) satisfies:
	 * <ol>
	 * <li>{@link ExpressionNode#isLvalue()} returns true and the expression has
	 * scalar type or set of scalar type;</li>
	 * <li>or is a NOTHING expression;</li>
	 * <li>or is a MPI_REGION expression</li>
	 * </ol>
	 * </p>
	 * 
	 * <p>
	 * Note that if the given expression is an argument of a READ clause, it is
	 * allowed to have non-scalar type or mem type with element of non-scalar type.
	 * </p>
	 * 
	 * @param expr    an expression node
	 * @param isReads true iff the given <code>expr</code> is an argument of a READS
	 *                clause
	 * @return true iff the given <code>expr</code> is an lvalue expression or a set
	 *         of lvalue expressions
	 * @throws SyntaxException when the given expression has mixed mem type
	 */
	private boolean isLvalueOfMemorySet(ExpressionNode expr, boolean isReads) throws SyntaxException {
		if (isReads) {
			// it this expression is an argument of a READS clause, it either is
			// an lvalue expression or is a memory location set expression:
			if (expr.isLvalue() || SetTypeAnalyzer.isMemoryLocationSet(expressionAnalyzer, expr))
				return true;
		} else if (SetTypeAnalyzer.isMemoryLocationSet(expressionAnalyzer, expr))
			// it this expression is an argument of a ASSIGNS clause, it must be
			// a memory location set expression:
			return true;

		// both READS and ASSIGNS accepts \nothing:
		if (expr.expressionKind() == ExpressionKind.NOTHING)
			// or the expression is \nothing:
			return true;
		return false;
	}

	// TODO: Get rid of MPI contract code (no longer supported)

	/**
	 * Recursively process entity analysis for a {@link ContractNode}.
	 *
	 * @param contractClause a contract clause
	 * @throws SyntaxException when a contract expression has syntax errors
	 */
	private void processContractNode(ContractNode contractClause) throws SyntaxException {
		ContractKind contractKind = contractClause.contractKind();

		switch (contractKind) {
		case ALLOCATES_OR_FREES: {
			AllocationNode allocation = (AllocationNode) contractClause;
			SequenceNode<ExpressionNode> memoryList = allocation.memoryList();

			for (ExpressionNode memory : memoryList) {
				expressionAnalyzer.processExpression(memory);
				if (!expressionAnalyzer.typeFactory.isPointerType(memory.getConvertedType()))
					throw this.error(
							"The expression " + memory.prettyRepresentation() + " doesn't have pointer type and thus "
									+ "can't be used as the operand of allocates/frees",
							allocation);
			}
			break;
		}
		case ASSIGNS_READS: {
			AssignsOrReadsNode assignsOrReads = (AssignsOrReadsNode) contractClause;
			SequenceNode<ExpressionNode> expressionList = assignsOrReads.getMemoryList();
			int numExpressions = expressionList.numChildren();

			for (int i = 0; i < numExpressions; i++) {
				ExpressionNode expression = expressionList.getSequenceChild(i);

				expressionAnalyzer.processExpression(expression);
				if (!isLvalueOfMemorySet(expression, assignsOrReads.isReads())) {
					throw error("The expression " + expression.prettyRepresentation()
							+ " doesn't designate an object or a set of memory locations and thus "
							+ "can't be used as the left argument of assigns/reads", assignsOrReads);
				}
			}
			break;
		}
		case DEPENDS: {
			DependsNode depends = (DependsNode) contractClause;
			// ExpressionNode condition = depends.getCondition();
			SequenceNode<DependsEventNode> eventList = depends.getEventList();

			for (DependsEventNode event : eventList) {
				processDependsEvent(event);
			}
			break;
			// int numEvents = eventList.numChildren();
			//
			// if (condition != null)
			// entityAnalyzer.expressionAnalyzer
			// .processExpression(condition);
			// for (int i = 0; i < numEvents; i++) {
			// ExpressionNode event = eventList.getSequenceChild(i);
			//
			// entityAnalyzer.expressionAnalyzer
			// .processExpression(event);
			// }
		}
		case GUARDS: {
			ExpressionNode expression = ((GuardsNode) contractClause).getExpression();

			expressionAnalyzer.processExpression(expression);
			break;
		}
		/* *********************************************************** */
		/* ** Contracts stored in functions without categorization: ** */
		case ASSUMES: {
			AssumesNode assumesNode = (AssumesNode) contractClause;
			ExpressionNode expression = assumesNode.getPredicate();

			expressionAnalyzer.processExpression(expression);
			break;
		}
		case BEHAVIOR: {
			BehaviorNode behavior = (BehaviorNode) contractClause;
			String name = behavior.getName().name();
			SequenceNode<ContractNode> body = behavior.getBody();

			if (definedBehaviors.containsKey(name))
				throw this.error(
						"re-definition of behavior named as " + name + ": the previous definition was at "
								+ definedBehaviors.get(name).getBehavior().getSource().getSummary(false, true),
						contractClause);
			else
				this.definedBehaviors.put(name, entityAnalyzer.entityFactory.newBehavior(name, behavior));
			for (ContractNode subClause : body) {
				processContractNode(subClause);
			}
			break;
		}
		case COMPLETENESS: {
			CompletenessNode completeNode = (CompletenessNode) contractClause;
			SequenceNode<IdentifierNode> idList = completeNode.getIDList();

			if (idList != null) {
				for (IdentifierNode id : idList) {
					BehaviorEntity behavior = this.definedBehaviors.get(id.name());

					if (behavior == null)
						throw this.error("undefined behavior " + id.name(), id);
					id.setEntity(behavior);
				}
			}
			break;
		}
		case REQUIRES: {
			ExpressionNode expression = ((RequiresNode) contractClause).getExpression();
			expressionAnalyzer.processExpression(expression);
			break;
		}
		case ENSURES: {
			ExpressionNode expression = ((EnsuresNode) contractClause).getExpression();
			expressionAnalyzer.processExpression(expression);
			break;
		}
		case PURE: {
			// nothing to analyze for now
			break;
		}
		default:
			throw error("Unknown kind of contract clause", contractClause);
		}
	}

	private boolean isCompatibleWithMemoryType(Type type) {
		if (type.kind() == TypeKind.MEM)
			return true;
		return expressionAnalyzer.typeFactory.isPointerType(type);
	}

	private void processDependsEvent(DependsEventNode event) throws SyntaxException {
		DependsEventNodeKind kind = event.getEventKind();

		switch (kind) {
		case MEMORY: {
			MemoryEventNode rwEvent = (MemoryEventNode) event;
			SequenceNode<ExpressionNode> memoryList = rwEvent.getMemoryList();

			for (ExpressionNode memory : memoryList) {
				this.expressionAnalyzer.processExpression(memory);
				if (!isCompatibleWithMemoryType(memory.getConvertedType()))
					throw this.error("the operand of \\write/\\read/\\access doesn't have valid memory type", memory);
				// memory.addConversion(this.conversionFactory.memoryConversion(memory.getConvertedType()));
			}
			break;
		}
		case ANYACT:
		case NOACT:
			break;
		case CALL: {
			CallEventNode call = (CallEventNode) event;
			SequenceNode<ExpressionNode> arguments = call.arguments();

			this.expressionAnalyzer.processIdentifierExpression(call.getFunction(), true, true);

			if (arguments != null) {
				for (ExpressionNode arg : arguments) {
					this.expressionAnalyzer.processExpression(arg);
				}
			}
			break;
		}
		case COMPOSITE: {
			CompositeEventNode composite = (CompositeEventNode) event;

			this.processDependsEvent(composite.getLeft());
			this.processDependsEvent(composite.getRight());
			break;
		}
		default:
			throw error("Unknown kind of depends event", event);
		}

	}

	private SyntaxException error(String message, ASTNode node) {
		return entityAnalyzer.error(message, node);
	}

	@SuppressWarnings("unused")
	private SyntaxException error(UnsourcedException e, ASTNode node) {
		return entityAnalyzer.error(e, node);
	}

	void processLoopContractNodes(SequenceNode<ContractNode> loopContracts) throws SyntaxException {
		for (ContractNode clause : loopContracts) {
			switch (clause.contractKind()) {
			case INVARIANT:
				InvariantNode loopInvari = ((InvariantNode) clause);

				expressionAnalyzer.processExpression(loopInvari.getExpression());
				break;
			case ASSIGNS_READS:
				AssignsOrReadsNode assignsNode = (AssignsOrReadsNode) clause;

				if (assignsNode.isReads())
					throw error("Unexpected loop contract clause: " + clause, clause);
				for (ExpressionNode mem : assignsNode.getMemoryList())
					expressionAnalyzer.processExpression(mem);
				break;
			case TRANSFORM:
				break;
			default:
				throw error("Unknown kind of loop contracts: " + clause.contractKind(), clause);
			// Check expression types
			}
		}
	}
}
