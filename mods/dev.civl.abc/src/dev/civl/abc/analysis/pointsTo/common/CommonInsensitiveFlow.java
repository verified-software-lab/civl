package dev.civl.abc.analysis.pointsTo.common;

import static dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind.CONSTANT;
import static dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind.IDENTIFIER_EXPRESSION;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.ADDRESSOF;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.DEREFERENCE;
import static dev.civl.abc.ast.type.IF.Type.TypeKind.ARRAY;
import static dev.civl.abc.ast.type.IF.Type.TypeKind.POINTER;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.civl.abc.analysis.pointsTo.IF.AssignExprIF;
import dev.civl.abc.analysis.pointsTo.IF.AssignOffsetIF;
import dev.civl.abc.analysis.pointsTo.IF.AssignmentIF;
import dev.civl.abc.analysis.pointsTo.IF.InsensitiveFlow;
import dev.civl.abc.analysis.pointsTo.IF.InsensitiveFlowFactory;
import dev.civl.abc.analysis.pointsTo.IF.InvocationGraphNode;
import dev.civl.abc.analysis.pointsTo.IF.InvocationGraphNodeFactory;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Entity.EntityKind;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.entity.IF.Scope;
import dev.civl.abc.ast.entity.IF.Variable;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.compound.CompoundInitializerNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.declaration.OrdinaryDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.OrdinaryDeclarationNode.OrdinaryDeclarationKind;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ArrayLambdaNode;
import dev.civl.abc.ast.node.IF.expression.ArrowNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.ConstantNode;
import dev.civl.abc.ast.node.IF.expression.ConstantNode.ConstantKind;
import dev.civl.abc.ast.node.IF.expression.DotNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.StatementExpressionNode;
import dev.civl.abc.ast.node.IF.omp.OmpExecutableNode;
import dev.civl.abc.ast.node.IF.omp.OmpNode;
import dev.civl.abc.ast.node.IF.statement.AtomicNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode.BlockItemKind;
import dev.civl.abc.ast.node.IF.statement.CivlForNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.IfNode;
import dev.civl.abc.ast.node.IF.statement.JumpNode;
import dev.civl.abc.ast.node.IF.statement.JumpNode.JumpKind;
import dev.civl.abc.ast.node.IF.statement.LabeledStatementNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode;
import dev.civl.abc.ast.node.IF.statement.ReturnNode;
import dev.civl.abc.ast.node.IF.statement.RunNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode.StatementKind;
import dev.civl.abc.ast.node.IF.statement.SwitchNode;
import dev.civl.abc.ast.node.IF.statement.WhenNode;
import dev.civl.abc.ast.type.IF.ArrayType;
import dev.civl.abc.ast.type.IF.AtomicType;
import dev.civl.abc.ast.type.IF.Field;
import dev.civl.abc.ast.type.IF.ObjectType;
import dev.civl.abc.ast.type.IF.PointerType;
import dev.civl.abc.ast.type.IF.QualifiedObjectType;
import dev.civl.abc.ast.type.IF.SetType;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.StructureOrUnionType;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.ast.type.IF.TypeFactory;
import dev.civl.abc.ast.value.IF.ValueFactory.Answer;
import dev.civl.abc.err.IF.ABCRuntimeException;
import dev.civl.abc.transform.common.StringOrCompoundInitializerTranslateWorker;
import dev.civl.abc.transform.common.StringOrCompoundInitializerTranslateWorker.AccessPathNode;
import dev.civl.abc.util.IF.Pair;

/**
 * <p>
 * an implementation of {@link InsensitiveFlow}.
 * </p>
 * 
 * <p>
 * A CommonInsensitiveFlow is generated from a set of program statements. For
 * any kind of assignment in the given program statements, it will be translated
 * to an {@link AssignmentIF} in the generated CommonInsensitiveFlow if the
 * assignment may impact what a pointer may points-to.
 * </p>
 * 
 * <p>
 * Let <code>trans(expr)</code> denotes the result of the translation of an
 * expression. For a general assignment in program <code>lhs = rhs</code>:
 * <ol>
 * <li>the basic rule is that if both <code>trans(lhs)</code> and
 * <code>trans(rhs)</code> have the form <code>op U</code>, using an auxiliary
 * abstract object for rhs, i.e., <code>
 * aux = op U;
 * </code> and let <code>trans(lhs) = aux;</code></li>
 * 
 * <li><code>trans(expr)</code> may return null if expr has no impact on the
 * points-to set of any pointer. If either lhs or rhs gets translated to null,
 * ignore the assignment.</li>
 * 
 * <li>there is a special rule for an array "e":
 * <ul>
 * <li>For <code>trans(e) := op U</code>, "trans(e)" cannot have array type, it
 * must be converted to pointer to array element type if it initially has array
 * type.</li>
 * <li>For <code>trans(lhs) = trans(e)</code>, "trans(e)" cannot have array
 * type, it must be converted to pointer to array element type if it initially
 * has array type.</li>
 * <li>For an actual argument "arg", "trans(arg)" cannot have array type, it
 * must be converted to pointer to array element type if it initially has array
 * type.</li>
 * <li>For <code>trans(e) + oft</code>, "trans(e)" cannot have array type, it
 * must be converted to pointer to array element type if it initially has array
 * type.</li>
 * </ul>
 * These special rules initiates the BASE case assignment for array objects.
 * Base assignment initiates points-to sets.</li>
 * </ol>
 * </p>
 *
 * @author ziqing
 */
// TODO: need more careful handling for determine if a type is a pointer type.
// Currently it compares type kind. But be aware of that a qualified pointer
// type can have QUALIFIED type kind
public class CommonInsensitiveFlow implements InsensitiveFlow {

	/**
	 * The generated set of assignments:
	 */
	private List<AssignmentIF> assigns = new LinkedList<>();

	/**
	 * a reference to {@link InsensitiveFlowFactory}
	 */
	private InsensitiveFlowFactory absFactory;

	/**
	 * a reference to {@link InvocationGraphNodeFactory}
	 */
	private InvocationGraphNodeFactory igFactory;

	/**
	 * the {@link InvocationGraphNode} that is associated with function body
	 * represented by this {@link InsensitiveFlow}
	 */
	private InvocationGraphNode igNode;

	/**
	 * the scope of the function body represented by this
	 * {@link InsensitiveFlow}
	 */
	private Scope functionScope;

	/**
	 * a reference to {@link TypeFactory}
	 */
	private TypeFactory typeFactory;

	/**
	 * caches all translations of expression node:
	 */
	private Map<ExpressionNode, TempExprAbstraction> translationCache;

	CommonInsensitiveFlow(Iterable<BlockItemNode> funcBody, Scope scope,
			InsensitiveFlowFactory factory,
			InvocationGraphNodeFactory igFactory, InvocationGraphNode igNode,
			TypeFactory typeFactory) {
		this.absFactory = factory;
		this.igFactory = igFactory;
		this.igNode = igNode;
		this.functionScope = scope;
		this.assigns = new LinkedList<>();
		this.typeFactory = typeFactory;
		this.translationCache = new HashMap<>();
		for (BlockItemNode node : funcBody)
			processBlockItemNode(node);

	}

	@Override
	public Iterator<AssignmentIF> iterator() {
		return assigns.iterator();
	}

	@Override
	public InsensitiveFlowFactory insensitiveFlowfactory() {
		return this.absFactory;
	}

	/* ************ methods for build ************ */
	/**
	 * process a {@link BlockItemNode}
	 * 
	 * @param node
	 */
	private void processBlockItemNode(BlockItemNode node) {
		if (node == null)
			return;

		BlockItemKind kind = node.blockItemKind();

		switch (kind) {
			case ORDINARY_DECLARATION :
				processOrdDecNode((OrdinaryDeclarationNode) node);
				break;
			case STATEMENT :
				processStatementNode((StatementNode) node);
				break;
			case STRUCT_OR_UNION :
			case TYPEDEF :
				// skip
				break;
			case OMP_DECLARATIVE :
			case PRAGMA :
			case STATIC_ASSERTION :
			default :
				throw new ABCRuntimeException("Unimplemented: convert "
						+ node.prettyRepresentation() + " of kind " + kind
						+ " to a set of assignments for points-to analysis");
		}
	}

	/**
	 * process ordinary declarations
	 * 
	 * @param node
	 */
	private void processOrdDecNode(OrdinaryDeclarationNode node) {
		OrdinaryDeclarationKind kind = node.ordinaryDeclarationKind();

		switch (kind) {
			case VARIABLE_DECLARATION :
				processVarDecNode((VariableDeclarationNode) node);
				break;
			case ABSTRACT_FUNCTION_DEFINITION :
				break;
			case FUNCTION_DECLARATION :
			case FUNCTION_DEFINITION :
			default :
				throw new ABCRuntimeException("Unimplemented: represent "
						+ node.prettyRepresentation() + " of kind " + kind
						+ " in an insensitive flow");
		}
	}

	/**
	 * <p>
	 * translates variable declaration to {@link AssignmentIF}s if it has an
	 * initializer, i.e. <code>var = init-expr</code>
	 * </p>
	 * 
	 * @param varDecl
	 */
	private void processVarDecNode(VariableDeclarationNode varDecl) {
		InitializerNode init = varDecl.getInitializer();
		TempExprAbstraction abs;
		Type varType = varDecl.getEntity().getType();

		if (init == null)
			return;
		if (!containingPtr(varType))
			return;
		if (init.nodeKind() == NodeKind.EXPRESSION) {
			abs = processExpressionNode((ExpressionNode) init);
			if (abs == null) // irrelevant to pointer
				return;
		} else {
			processVarDecNodeWorkerForCompoundinitializer(varDecl, (CompoundInitializerNode) init);
			return;
		}

		boolean deref = abs.op == Operator.DEREFERENCE,
				addrof = abs.op == Operator.ADDRESSOF;

		AssignExprIF lhs = absFactory.assignStoreExpr(varDecl.getEntity());
		AssignExprIF rhs = abs.assignExpr;

		assigns.add(absFactory.assignment(lhs, false, rhs, deref, addrof));
	}

	/**
	 * Worker method of {@link processVarDecNode} that decomposes a compound
	 * initializer to scalar level assignments to sub-objects.
	 */
	private void processVarDecNodeWorkerForCompoundinitializer(VariableDeclarationNode varDecl,
			CompoundInitializerNode cInit) {
		ArrayList<Pair<ArrayList<AccessPathNode>, ExpressionNode>> pairs = StringOrCompoundInitializerTranslateWorker
				.getAsAccessPathExpressionPairs(cInit.getLiteralObject());

		for (var pair : pairs) {
			Type subObjTy = cInit.getLiteralObject().getType();

			// To ignore non-pointer type sub-objects:
			for (var apNode : pair.left) {
				subObjTy = subObjTy.ignoreQualifiersAtomic();
				if (apNode.operator() == AccessPathNode.Operator.DOT) {
					subObjTy = apNode.field().getType();
				} else {
					assert subObjTy.kind() == TypeKind.ARRAY;
					subObjTy = ((ArrayType) subObjTy).getElementType();
				}
			}
			if (!containingPtr(subObjTy))
				continue;

			AssignExprIF lhs = absFactory.assignStoreExpr(varDecl.getEntity());

			// Compute AssignExprIF of the sub-object:
			for (var apNode : pair.left) {
				if (apNode.operator() == AccessPathNode.Operator.DOT)
					lhs = absFactory.assignFieldExpr(lhs, apNode.field());
				else
					lhs = absFactory.assignSubscriptExpr(lhs, absFactory.assignOffset(apNode.arrayIndex()));
			}

			var rhs = processExpressionNode(pair.right);
			if (rhs == null)
				continue;
			boolean deref = rhs.op == Operator.DEREFERENCE,
					addrof = rhs.op == Operator.ADDRESSOF;

			assigns.add(absFactory.assignment(lhs, false, rhs.assignExpr, deref, addrof));
		}
	}

	/**
	 * process a statement
	 * 
	 * @param stmt
	 */
	private void processStatementNode(StatementNode stmt) {
		if (stmt == null)
			return;

		StatementKind kind = stmt.statementKind();

		switch (kind) {
			case ATOMIC :
				processStatementNode(((AtomicNode) stmt).getBody());
				break;
			case CHOOSE : {
				@SuppressWarnings("unchecked")
				SequenceNode<BlockItemNode> seq = (SequenceNode<BlockItemNode>) stmt;

				processBlockItemSequence(seq);
			}
				break;
			case CIVL_FOR : {
				CivlForNode civlForNode = (CivlForNode) stmt;

				processStatementNode(civlForNode.getBody());
				break;
			}
			case COMPOUND :
				processBlockItemSequence((CompoundStatementNode) stmt);
				break;
			case EXPRESSION :
				processExpressionNode(
						((ExpressionStatementNode) stmt).getExpression());
				break;
			case IF :
				processStatementNode(((IfNode) stmt).getTrueBranch());
				processStatementNode(((IfNode) stmt).getFalseBranch());
				processExpressionNode(((IfNode) stmt).getCondition());
				break;
			case LABELED :
				processStatementNode(
						((LabeledStatementNode) stmt).getStatement());
				break;
			case LOOP :
				processStatementNode(((LoopNode) stmt).getBody());
				processExpressionNode(((LoopNode) stmt).getCondition());
				break;
			case RUN :
				processStatementNode(((RunNode) stmt).getStatement());
				break;
			case SWITCH :
				processStatementNode(((SwitchNode) stmt).getBody());
				break;
			case WHEN :
				processStatementNode(((WhenNode) stmt).getBody());
				break;
			case JUMP :
				processJumpNode((JumpNode) stmt);
				break;
			case OMP :
				processOmpNode((OmpNode) stmt);
				break;
			case UPDATE :
			case WITH :
			case PRAGMA :
				throw unimplemented("statement " + stmt.prettyRepresentation()
						+ " of kind " + kind);
			default :
				break;
		}
	}

	/**
	 * process a sequence of block item nodes
	 * 
	 * @param bSeq
	 */
	private void processBlockItemSequence(
			SequenceNode<? extends BlockItemNode> bSeq) {
		for (BlockItemNode b : bSeq)
			processBlockItemNode(b);
	}

	/**
	 * <p>
	 * process a jump statement: any jump statement other than return is a
	 * no-op.
	 * </p>
	 * 
	 * <p>
	 * For a return statement: if <code>
	 * trans(ret-expr) := U
	 * </code>, adding "U" to {@link #igNode} as return value;
	 * 
	 * if <code>
	 * trans(ret-expr) := op U
	 * </code>, then introducing auxiliary abstract object <code>
	 * aux = op U
	 * </code> adding "aux" to {@link #igNode} as return value;
	 * </p>
	 */
	private void processJumpNode(JumpNode jumpNode) {
		if (jumpNode.getKind() != JumpKind.RETURN)
			return;

		ExpressionNode retExpr = ((ReturnNode) jumpNode).getExpression();

		if (retExpr == null)
			return;

		TempExprAbstraction tmpAbs = processExpressionNode(retExpr);

		if (tmpAbs == null)
			return;

		AssignExprIF abs = tmpAbs.assignExpr;

		if (tmpAbs.op != null) {
			// if the return expression abstraction has forms *e or &e,
			// introduce an auxiliary AssignExprIF:
			AssignExprIF aux = absFactory.assignAuxExpr(retExpr.getType());

			assigns.add(processAuxAssignment(aux, abs, tmpAbs.op));
			abs = aux;
		}
		this.igNode.addReturnValue(abs);
	}

	/**
	 * process an expression
	 * 
	 * @param expr
	 * @return an {@link TempExprAbstraction} of the given expression or null if
	 *         the given expression has no impact on points-to analysis
	 */
	private TempExprAbstraction processExpressionNode(ExpressionNode expr) {
		if (expr == null)
			return null;

		ExpressionKind kind = expr.expressionKind();
		TempExprAbstraction result;

		switch (kind) {
			case ARRAY_LAMBDA : {
				ArrayLambdaNode arrLambda = (ArrayLambdaNode) expr;
				result = processExpressionNode(arrLambda.expression());
				break;
			}
			case ARROW :
				result = processArrow((ArrowNode) expr);
				break;
			case CAST :
				result = processCast((CastNode) expr);
				break;
			case CONSTANT :
				result = processConstant((ConstantNode) expr);
				break;
			case DOT :
				result = processDot((DotNode) expr);
				break;
			case IDENTIFIER_EXPRESSION :
				result = processIdentifierExpression(
						(IdentifierExpressionNode) expr);
				break;
			case OPERATOR :
				result = processRHSOperator((OperatorNode) expr);
				break;
			case STATEMENT_EXPRESSION :
				processStatementNode(((StatementExpressionNode) expr)
						.getCompoundStatement());
				return null;
			case FUNCTION_CALL :
				// first test if it is an allocation:
				TempExprAbstraction alloc = processIfAlloc(
						(FunctionCallNode) expr);
				if (alloc != null)
					result = alloc;
				else
					result = processFunctionCall((FunctionCallNode) expr);
				break;
			case REMOTE_REFERENCE :
			case UPDATE :
			case VALUE_AT :
			case QUANTIFIED_EXPRESSION :
			case LAMBDA :
			case SPAWN :
			case DERIVATIVE_EXPRESSION :
			case COMPOUND_LITERAL :
				throw unimplemented(expr.expressionKind() + " kind expression");
			default :
				return null;
		}
		this.translationCache.put(expr, result);
		return result;
	}

	/**
	 * <p>
	 * translate an arrow expression <code>expr->id</code>: <code>
	 * IF trans(expr) := * U, 
	 * THEN 
	 *   trans(expr->id) := aux2.id WITH aux = trans(expr); aux2 = *aux;
	 * ELSE IF trans(expr) := & U,
	 * THEN
	 *   trans(expr->id) := U.id;
	 * ELSE
	 * THEN
	 *   trans(expr->id) := aux.id WITH aux = *trans(expr); 
	 * </code>
	 * </p>
	 */
	private TempExprAbstraction processArrow(ArrowNode arrowNode) {
		ExpressionNode ptr = arrowNode.getStructurePointer();
		Field field = (Field) arrowNode.getFieldName().getEntity();
		TempExprAbstraction ptrAbs = processExpressionNode(ptr);
		AssignExprIF aux = null, result;

		if (ptrAbs.op == DEREFERENCE) {
			AssignExprIF aux2 = absFactory.assignAuxExpr(ptr.getType());

			// aux2 reps the struct pointer
			assigns.add(
					processAuxAssignment(aux2, ptrAbs.assignExpr, ptrAbs.op));
			// aux reps the struct
			aux = absFactory.assignAuxExpr(
					((PointerType) ptr.getType()).referencedType());
			assigns.add(processAuxAssignment(aux, aux2, DEREFERENCE));
			result = absFactory.assignFieldExpr(aux, field);
		} else if (ptrAbs.op == ADDRESSOF) {
			// if e = &b, return b.id directly
			aux = ptrAbs.assignExpr;
			result = absFactory.assignFieldExpr(aux, field);
		} else {
			// (*e).field
			aux = absFactory.assignAuxExpr(
					((PointerType) ptr.getType()).referencedType());
			assigns.add(
					processAuxAssignment(aux, ptrAbs.assignExpr, DEREFERENCE));
			result = absFactory.assignFieldExpr(aux, field);
		}
		return new TempExprAbstraction(result, null);
	}

	/**
	 * <p>
	 * translate a dot expression<code>e.id</code>, similar but simpler to
	 * {@link #processArrow(ArrowNode)}
	 * <p>
	 */
	private TempExprAbstraction processDot(DotNode dotNode) {
		ExpressionNode structure = dotNode.getStructure();
		Field field = (Field) dotNode.getFieldName().getEntity();
		TempExprAbstraction structureAbs = processExpressionNode(structure);
		AssignExprIF ret;

		// e never will be of the form &b:
		assert structureAbs.op != ADDRESSOF;
		if (structureAbs.op == DEREFERENCE) {
			// aux is the struct
			AssignExprIF aux = absFactory.assignAuxExpr(structure.getType());

			assigns.add(processAuxAssignment(aux, structureAbs.assignExpr,
					structureAbs.op));
			ret = absFactory.assignFieldExpr(aux, field);
		} else
			ret = absFactory.assignFieldExpr(structureAbs.assignExpr, field);
		return new TempExprAbstraction(ret, null);
	}

	/**
	 * <p>
	 * for <code>
	 * (T)q;
	 * </code>
	 * <li>if both (T)q and q are pointers, return trans(q) typed by T</li>
	 * <li>if only (T)q is pointer, return FULL (unless the case of
	 * (void*)0)</li>
	 * <li>otherwise, return null for ignore</li>
	 * </p>
	 */
	private TempExprAbstraction processCast(CastNode expr) {
		ExpressionNode arg = ((CastNode) expr).getArgument();

		if (expr.getType().kind() == TypeKind.POINTER)
			if (arg.getType().kind() != TypeKind.POINTER) {
				if (arg.isConstantExpression()) {
					ConstantNode constNode = (ConstantNode) arg;
					// if the cast is (T *)0, return null for ignore

					if (constNode.getConstantValue().isZero() == Answer.YES)
						return null;
				}
				return new TempExprAbstraction(absFactory.full(), ADDRESSOF);
			}
		return processExpressionNode(arg);
	}

	/**
	 * <p>
	 * If the constant is a string literal "str":<code>
	 * aux = & string-literal;
	 * trans(str) := aux;
	 * </code> where "aux" is typed by pointer to char. Otherwise, ignore it.
	 * </p>
	 */
	private TempExprAbstraction processConstant(ConstantNode constant) {
		if (constant.constantKind() != ConstantKind.STRING)
			return null;
		AssignExprIF string = absFactory.assignStoreExpr(constant);
		AssignExprIF ptrAux = absFactory.assignAuxExpr(typeFactory
				.pointerType(typeFactory.basicType(BasicTypeKind.CHAR)));

		assigns.add(processAuxAssignment(ptrAux, string, ADDRESSOF));
		return new TempExprAbstraction(ptrAux, null);
	}

	/**
	 * <p>
	 * For every seen identifier "id", first test if it is a global variable, if
	 * it is, adds it as a global variable to {@link #igNode}. Then translates
	 * the identifier to an AssignExprIF of STORE kind.
	 * </p>
	 */
	private TempExprAbstraction processIdentifierExpression(
			IdentifierExpressionNode expr) {
		Entity entity = ((IdentifierExpressionNode) expr).getIdentifier()
				.getEntity();
		// func scope -> param scope -> parent (func) scope
		Scope parent = functionScope.getParentScope().getParentScope();
		Entity sameNameEntity = parent.getLexicalOrdinaryEntity(false,
				entity.getName());
		AssignExprIF exprAbs;

		assert entity.getEntityKind() == EntityKind.VARIABLE;
		Variable var = (Variable) entity;

		exprAbs = absFactory.assignStoreExpr((Variable) var);
		if (entity == sameNameEntity) {
			// If this entity is visible from lexical parent scope of
			// the function, this entity is not declared locally:
			this.igNode.addGlobalAccess(exprAbs);
		}
		return new TempExprAbstraction(exprAbs, null);
	}

	/**
	 * If the given call <code>call</code> is an allocation: <code>
	 * aux = & alloc;
	 * trans(call) := aux;
	 * </code> where "aux" is typed by the type of "call"
	 */
	private TempExprAbstraction processIfAlloc(FunctionCallNode call) {
		ExpressionNode func = call.getFunction();

		if (func.expressionKind() != ExpressionKind.IDENTIFIER_EXPRESSION)
			return null;

		IdentifierNode funcID = ((IdentifierExpressionNode) func)
				.getIdentifier();

		if (!funcID.name().endsWith("alloc"))
			return null;

		PointerType ptrType = (PointerType) call.getType();
		Type referredType = ptrType.referencedType();

		if (!(referredType instanceof ObjectType))
			throw unimplemented(" allocation of pointer to non-object type");

		AssignExprIF alloc = absFactory.assignStoreExpr(call);
		AssignExprIF ptr = absFactory.assignAuxExpr(call.getType());

		assigns.add(processAuxAssignment(ptr, absFactory.assignSubscriptExpr(
				alloc, absFactory.assignOffsetZero()), ADDRESSOF));
		return new TempExprAbstraction(ptr, null);
	}

	/**
	 * <p>
	 * translates a function call that is call by a function identifier (TODO:
	 * function pointer is not supported yet): <code>
	 * aux = call();
	 * </code>. In addition, a {@link InvocationGraphNode} will be created to
	 * represent this lexical function call instance. For every actual argument
	 * "arg" of this call, "trans(arg)" will be saved into the created
	 * invocation graph node.
	 * </p>
	 */
	private TempExprAbstraction processFunctionCall(FunctionCallNode funcCall) {
		ExpressionNode funcNode = funcCall.getFunction();
		boolean funcPtr = funcNode
				.expressionKind() != ExpressionKind.IDENTIFIER_EXPRESSION;
		Function func = null;

		// currently cannot deal with function pointers:
		if (!funcPtr) {
			Entity entity = ((IdentifierExpressionNode) funcNode)
					.getIdentifier().getEntity();

			if (entity.getEntityKind() != EntityKind.FUNCTION)
				funcPtr = true;
			else
				func = (Function) entity;
		}
		assert func != null;
		if (funcPtr)
			throw unimplemented("call by function pointer");
		if (func.getDefinition() == null)
			return processSystemFunctionCall(funcCall);

		AssignExprIF callExprAbs = absFactory.assignAuxExpr(funcCall.getType());
		SequenceNode<ExpressionNode> actualArgSeq = funcCall.getArguments();
		int numArgs = actualArgSeq.numChildren();
		AssignExprIF[] actualArgAbs = new AssignExprIF[numArgs];

		for (int i = 0; i < numArgs; i++) {
			ExpressionNode actualArg = funcCall.getArgument(i);

			actualArgAbs[i] = processFunctionCallArgument(actualArg);
		}
		igFactory.newNode(func, igNode, callExprAbs, actualArgAbs);
		return new TempExprAbstraction(callExprAbs, null);
	}

	/**
	 * <p>
	 * worker method for {@link #processFunctionCall(FunctionCallNode)} that
	 * translates actual arguments.
	 * </p>
	 */
	private AssignExprIF processFunctionCallArgument(ExpressionNode arg) {
		TempExprAbstraction abs = processExpressionNode(arg);
		AssignExprIF result;

		if (abs == null)
			// if the acutal argument is irrelevant ...
			result = absFactory.assignAuxExpr(arg.getType());
		else if (abs.op != null) {
			// if trans(arg) := op U
			AssignExprIF aux = absFactory.assignAuxExpr(arg.getType());

			result = aux;
			assigns.add(processAuxAssignment(aux, abs.assignExpr, abs.op));
		} else {
			result = abs.assignExpr;
			// if trans(arg) has array type, the special translation rule
			// (mentioned in the doc of this class) for array type abstract
			// object must be applied:
			if (result.type().kind() == TypeKind.ARRAY)
				result = arrayToPointerConversion(result);
		}
		return result;
	}

	// TODO: use POR contract for system functions
	// currently, just ignore it:
	private TempExprAbstraction processSystemFunctionCall(
			FunctionCallNode funcCall) {
		return new TempExprAbstraction(
				absFactory.assignAuxExpr(funcCall.getType()), null);
	}

	/**
	 * 
	 * @param opNode
	 *            a node represents operation
	 * 
	 * @return an {@link TempExprAbstraction} of this operation or null if this
	 *         operation is irrelevant to points-to analysis
	 */
	private TempExprAbstraction processRHSOperator(OperatorNode opNode) {
		Operator op = opNode.getOperator();

		switch (op) {
			case ADDRESSOF :
				return processAddressofOperator(opNode);
			case ASSIGN :
				processAssignment(opNode);
				return null;
			case COMMA :
				processExpressionNode(opNode.getArgument(0));
				return processExpressionNode(opNode.getArgument(1));
			case DEREFERENCE :
				return processDereferenceOperator(opNode);
			case SUBSCRIPT :
				return processSubscriptOperator(opNode);
			case PLUS :
			case MINUS :
				if (opNode.getType().kind() == TypeKind.POINTER)
					return processPointerAddition(opNode);
			default :
				// translate general operation : op(e0, e1, ...) to
				// aux = trans(e0);
				// aux = trans(e1);
				// ...
				// trans(op(e0, e1, ...)) := aux
				if (!containingPtr(opNode.getType()))
					return null;

				int numArgs = opNode.getNumberOfArguments();
				AssignExprIF auxLhs = absFactory
						.assignAuxExpr(opNode.getType());

				for (int i = 0; i < numArgs; i++) {
					ExpressionNode arg = opNode.getArgument(i);

					if (isIrrelaventOperand(arg))
						continue;

					TempExprAbstraction exprAbs = processExpressionNode(arg);
					AssignmentIF assignIF;

					if (exprAbs == null)
						continue;
					assignIF = processAuxAssignment(auxLhs, exprAbs.assignExpr,
							exprAbs.op);
					assigns.add(assignIF);
				}
				return new TempExprAbstraction(auxLhs, null);
		}
	}

	/**
	 * translates ADDRESSOF operation <code>& expr</code>, pretty
	 * straightforward.
	 */
	private TempExprAbstraction processAddressofOperator(OperatorNode opNode) {
		TempExprAbstraction exprAbs = processExpressionNode(
				opNode.getArgument(0));

		if (exprAbs.op == DEREFERENCE) {
			exprAbs.op = null;
			return exprAbs;
		}
		// &(&e) is illegal:
		assert exprAbs.op != ADDRESSOF;
		exprAbs.op = ADDRESSOF;
		return exprAbs;
	}

	/**
	 * translates DEREFERENCE operation <code>& expr</code>, pretty
	 * straightforward.
	 */
	private TempExprAbstraction processDereferenceOperator(
			OperatorNode opNode) {
		ExpressionNode ptr = opNode.getArgument(0);
		TempExprAbstraction exprAbs = processExpressionNode(ptr);

		if (exprAbs.op == DEREFERENCE) {
			// aux represents the pointer:
			AssignExprIF aux = absFactory.assignAuxExpr(ptr.getType());

			assigns.add(
					processAuxAssignment(aux, exprAbs.assignExpr, exprAbs.op));
			exprAbs = new TempExprAbstraction(aux, DEREFERENCE);
		} else if (exprAbs.op == ADDRESSOF) {
			// if the case *&p, directly return p
			exprAbs.op = null;
		} else
			exprAbs.op = DEREFERENCE;
		return exprAbs;
	}

	/**
	 * <p>
	 * translates subscript expression <code>a[x]</code>: if
	 * <code>trans(a)</code> has array type, then it must have the form
	 * <code>trans(a) := U</code> according to our special translation rule for
	 * array described in the doc of this class. In such case, <code>
	 * trans(a[x]) := trans(a)[x]
	 * </code>
	 * </p>
	 * 
	 * <p>
	 * Otherwise, <code>trans(a)</code> will be treated as a pointer <code>
	 *   IF trans(a) := op U;
	 *   THEN
	 *     trans(a[x]) := * (aux + x) with aux = op U;
	 *   ELSE THEN
	 *     trans(a[x]) := * (trans(a) + x);
	 * </code>
	 * </p>
	 * 
	 */
	private TempExprAbstraction processSubscriptOperator(OperatorNode opNode) {
		ExpressionNode arrayNode = opNode.getArgument(0);
		ExpressionNode indexNode = opNode.getArgument(1);
		TempExprAbstraction arrAbs = processExpressionNode(arrayNode);
		AssignExprIF ptrAbs = arrAbs.assignExpr;
		AssignOffsetIF offset = absFactory.assignOffset(indexNode, true);

		if (arrAbs.op != null) {
			assert arrAbs.assignExpr.type().kind() == TypeKind.POINTER;

			Type type = arrayNode.getType();

			if (type.kind() == TypeKind.ARRAY)
				type = typeFactory
						.pointerType(((ArrayType) type).getElementType());

			// aux represents a pointer to the first element of the array:
			AssignExprIF aux = absFactory.assignAuxExpr(type);

			assigns.add(
					processAuxAssignment(aux, arrAbs.assignExpr, arrAbs.op));
			ptrAbs = absFactory.assignOffsetExpr(aux, offset);
			return new TempExprAbstraction(ptrAbs, DEREFERENCE);
		} else if (ptrAbs.type().kind() == TypeKind.ARRAY) {
			ptrAbs = absFactory.assignSubscriptExpr(ptrAbs,
					absFactory.assignOffset(indexNode, true));
			return new TempExprAbstraction(ptrAbs, null);
		} else {
			ptrAbs = absFactory.assignOffsetExpr(ptrAbs, offset);
			return new TempExprAbstraction(ptrAbs, DEREFERENCE);
		}
	}

	/**
	 * <p>
	 * translates pointer arithmetic operation: <code>ptr + oft</code>. The only
	 * case that is worth to mention is that when "ptr" initially has array
	 * type. In such case, it must be converted to "&ptr[0]".
	 * </p>
	 */
	private TempExprAbstraction processPointerAddition(OperatorNode opNode) {
		boolean positive = opNode.getOperator() == Operator.PLUS;
		ExpressionNode arg0 = opNode.getArgument(0);
		ExpressionNode arg1 = opNode.getArgument(1);
		TempExprAbstraction ptrTmpAbs;
		ExpressionNode ptr, offset;

		if (arg0.getType().kind() == TypeKind.POINTER) {
			ptr = arg0;
			offset = arg1;
		} else {
			ptr = arg1;
			offset = arg0;
		}
		ptrTmpAbs = processExpressionNode(ptr);

		AssignExprIF ptrAbs = ptrTmpAbs.assignExpr;

		if (ptrTmpAbs.op != null) {
			assert ptr.getType().kind() == TypeKind.POINTER;

			// aux represents the pointer:
			AssignExprIF aux = absFactory.assignAuxExpr(ptr.getType());

			assigns.add(processAuxAssignment(aux, ptrAbs, ptrTmpAbs.op));
			ptrAbs = aux;
		} else if (ptrTmpAbs.assignExpr.type().kind() == TypeKind.ARRAY)
			ptrAbs = arrayToPointerConversion(ptrTmpAbs.assignExpr);

		ptrAbs = absFactory.assignOffsetExpr(ptrAbs,
				absFactory.assignOffset(offset, positive));
		return new TempExprAbstraction(ptrAbs, null);
	}

	/**
	 * process OpenMP node: ignore OpenMP pragma and process statement as usual
	 * 
	 * @param stmt
	 */
	private void processOmpNode(OmpNode stmt) {
		switch (stmt.ompNodeKind()) {
			case EXECUTABLE : {
				OmpExecutableNode execNode = (OmpExecutableNode) stmt;

				processStatementNode(execNode.statementNode());
				break;
			}
			default :
				throw unimplemented("represent OpenMP node");
		}
	}

	/**
	 * Make the ASSIGN operation an {@link AssignmentIF}. The case that is worth
	 * to mention is that if the translation of the right-hand side has array
	 * type according to the special rules for array, it shall be converted to a
	 * pointer to the first element of the array.
	 */
	private void processAssignment(OperatorNode assignNode) {
		ExpressionNode lhs = assignNode.getArgument(0);
		ExpressionNode rhs = assignNode.getArgument(1);
		TempExprAbstraction rhsAbs = processExpressionNode(rhs);
		TempExprAbstraction lhsAbs = processExpressionNode(lhs);

		if (rhsAbs == null || lhsAbs == null)
			return; // not pointer

		boolean lhsDeref = lhsAbs.op == Operator.DEREFERENCE;
		boolean rhsDeref = rhsAbs.op == Operator.DEREFERENCE;
		boolean rhsAddrof = rhsAbs.op == Operator.ADDRESSOF;
		AssignmentIF assignment;

		if (rhsAbs.op == null
				&& rhsAbs.assignExpr.type().kind() == TypeKind.ARRAY)
			rhsAbs = new TempExprAbstraction(
					arrayToPointerConversion(rhsAbs.assignExpr), null);

		// if it's the case of "*e = *r", convert it to
		// "v = *r"
		// "*e = v"
		if (lhsDeref == true && (rhsDeref == true || rhsAddrof == true)) {
			AssignExprIF aux = absFactory.assignAuxExpr(rhs.getType());

			assignment = absFactory.assignment(aux, false, rhsAbs.assignExpr,
					rhsDeref, rhsAddrof);
			assigns.add(assignment);
			assignment = absFactory.assignment(lhsAbs.assignExpr, lhsDeref, aux,
					false, false);
		} else
			assignment = absFactory.assignment(lhsAbs.assignExpr, lhsDeref,
					rhsAbs.assignExpr, rhsDeref, rhsAddrof);
		assigns.add(assignment);
	}

	/**
	 * <p>
	 * given "auxLhs", "rhs" and the operator (i.e. dereference or address-of)
	 * of "rhs", return an {@link AssignmentIF} representing
	 * <code>auxLhs = op rhs</code>
	 * </p>
	 * 
	 * <p>
	 * if "rhs == null", create "lhs = FULL"; if "op == null", no operator.
	 * </p>
	 * 
	 * @param var
	 * @return assignment
	 */
	private AssignmentIF processAuxAssignment(AssignExprIF auxLhs,
			AssignExprIF rhs, Operator op) {
		boolean deref = op == Operator.DEREFERENCE;
		boolean addrf = op == Operator.ADDRESSOF;
		if (rhs == null) {
			assert !addrf && !deref;
			rhs = absFactory.full();
			addrf = true;
		}
		return absFactory.assignment(auxLhs, false, rhs, deref, addrf);
	}

	/**
	 * Given an abstract object <code>U</code> of array type, return
	 * <code>aux</code> with <code>aux = &U[0];</code> being added to the
	 * insensitive flow representation.
	 */
	private AssignExprIF arrayToPointerConversion(AssignExprIF array) {
		ArrayType type = (ArrayType) array.type();

		array = absFactory.assignSubscriptExpr(array,
				absFactory.assignOffsetZero());

		AssignExprIF aux = absFactory
				.assignAuxExpr(typeFactory.pointerType(type.getElementType()));

		assigns.add(processAuxAssignment(aux, array, ADDRESSOF));
		return aux;
	}

	/* *************** Util classes & methods ******************/
	/**
	 * <p>
	 * A temporary class-internal representation <code>op U</code> where "op"
	 * can be absent, dereference or addressof operator, "U" is an abstract
	 * object.
	 * </p>
	 * 
	 * <p>
	 * This temporary representation is used for keeping track of operators
	 * (DEREFERENCE or ADDRESS_OF) over {@link AssignExprIF}s until
	 * {@link AssignmentIF}s are generated.
	 * </p>
	 * 
	 * @author ziqing
	 *
	 */
	private class TempExprAbstraction {
		/**
		 * the abstract representation:
		 */
		final AssignExprIF assignExpr;
		/**
		 * dereference, or address-of, or null
		 */
		Operator op;

		TempExprAbstraction(AssignExprIF expr, Operator op) {
			assert expr != null;
			this.assignExpr = expr;
			this.op = op;
		}
	}

	/**
	 * 
	 * @param expr
	 * @return true iff the given expression is NOT a pointer or an array and is
	 *         a trivial expression (i.e. id or constant)
	 */
	private boolean isIrrelaventOperand(ExpressionNode expr) {
		TypeKind kind = expr.getType().kind();
		ExpressionKind exprKind = expr.expressionKind();
		boolean nonStringConst = exprKind == CONSTANT
				&& ((ConstantNode) expr).constantKind() != ConstantKind.STRING;

		return kind != POINTER && kind != ARRAY
				&& (exprKind == IDENTIFIER_EXPRESSION || nonStringConst);
	}

	private boolean containingPtr(Type type) {
		if (type.kind() == TypeKind.POINTER)
			return true;
		switch (type.kind()) {
			case ARRAY :
				return containingPtr(((ArrayType) type).getElementType());
			case QUALIFIED :
				return containingPtr(
						((QualifiedObjectType) type).getBaseType());
			case SET :
				return containingPtr(((SetType) type).elementType());
			case STRUCTURE_OR_UNION : {
				StructureOrUnionType structOrUnionType = (StructureOrUnionType) type;

				if (structOrUnionType.getFields() != null)
					for (Field field : structOrUnionType.getFields())
						if (containingPtr(field.getType()))
							return true;
				return false;
			}
			case HEAP :
			case LAMBDA :
				throw new ABCRuntimeException(
						"unimplemented: points-to analyzing LAMBDA or HEAP type expressionsS");
			default :
				return false;
		}
	}

	private ABCRuntimeException unimplemented(String str) {
		return new ABCRuntimeException("Unimplemented program construct " + str
				+ " for insensitive flow representation.");
	}

	/* *************** Printing ******************/
	public String toString() {
		StringBuffer sb = new StringBuffer();

		for (AssignmentIF assign : assigns)
			sb.append(assign.toString() + "\n");
		return sb.toString();
	}
}
