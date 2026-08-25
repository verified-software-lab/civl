package dev.civl.mc.transform.analysisIF;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.civl.abc.analysis.pointsTo.IF.AssignExprIF;
import dev.civl.abc.analysis.pointsTo.IF.AssignExprIF.AssignExprKind;
import dev.civl.abc.analysis.pointsTo.IF.AssignOffsetIF;
import dev.civl.abc.analysis.pointsTo.IF.AssignStoreExprIF;
import dev.civl.abc.analysis.pointsTo.IF.AssignSubscriptExprIF;
import dev.civl.abc.analysis.pointsTo.IF.FlowInsensePointsToAnalyzer;
import dev.civl.abc.analysis.pointsTo.IF.InsensitiveFlowFactory;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Entity.EntityKind;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.entity.IF.Scope;
import dev.civl.abc.ast.entity.IF.Variable;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.compound.CompoundInitializerNode;
import dev.civl.abc.ast.node.IF.compound.DesignationNode;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.declaration.OrdinaryDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ArrowNode;
import dev.civl.abc.ast.node.IF.expression.DotNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.statement.DeclarationListNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.type.IF.Field;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.transform.analysis.common.ReadWriteDataStructureImpl.CommonReadWriteDataStructureFactory;
import dev.civl.mc.transform.analysisIF.ReadWriteDataStructures.RWSet;
import dev.civl.mc.transform.analysisIF.ReadWriteDataStructures.RWSetBaseElement;
import dev.civl.mc.transform.analysisIF.ReadWriteDataStructures.RWSetElement;
import dev.civl.mc.transform.analysisIF.ReadWriteDataStructures.RWSetFieldElement;
import dev.civl.mc.transform.analysisIF.ReadWriteDataStructures.RWSetOffsetElement;
import dev.civl.mc.transform.analysisIF.ReadWriteDataStructures.RWSetSubscriptElement;
import dev.civl.mc.transform.analysisIF.ReadWriteDataStructures.ReadWriteDataStructureFactory;

/**
 * inter-procedural flow-insensitive read-write set analyzer
 * 
 * @author ziqing
 *
 */
public class SimpleReadWriteAnalyzer {

	/**
	 * a reference to the factory that produces basic data structures for
	 * read/write analysis
	 */
	private ReadWriteDataStructureFactory rwFactory;
	/**
	 * a reference to a flow-insensitive points-to analyzer
	 */
	private FlowInsensePointsToAnalyzer pointsToAnalyzer;

	/**
	 * a reference to the insensitive flow factory which is used by
	 * {@link #pointsToAnalyzer}
	 */
	private InsensitiveFlowFactory isFactory;

	/**
	 * the {@link Function} where the current analyzing code is in
	 */
	private Function currentFunction;

	/**
	 * the set of entities declared in the given ASTNode of interface:
	 * {@link #collectRWFromStmtDeclExpr(Function, ASTNode, Set)}
	 */
	private Set<Entity> declaredEntities;

	/* ************** full set exception ********/

	/**
	 * full set exception for informing <b>CLIENTS</b> of this Java class
	 * 
	 * @author ziqing
	 */
	public class SimpleFullSetException extends Exception {
		// full set exception for informing clients of this Java file
		private static final long serialVersionUID = 1L;
		// is write set full or read set full ?
		final boolean isWriteSetFull;
		SimpleFullSetException(boolean isWriteSetFull) {
			this.isWriteSetFull = isWriteSetFull;
		}
	}

	/**
	 * <p>
	 * A temporary representation for the result of analyzing the read/write set
	 * of an expression. see also {@link TempRWResult#primary} and
	 * {@link TempRWResult#involved}.
	 * </p>
	 * 
	 * 
	 * <p>
	 * immutable during analysis
	 * </p>
	 * 
	 * @author ziqing
	 */
	private class TempRWResult {
		/**
		 * For an expression denoting an object or a pointer value, the
		 * "primary" field refers to an instance of {@link RWSetElement}
		 * representing the object or the pointer. When an expression DOES NOT
		 * denote an object or a pointer value, such as <code>i + 1</code> where
		 * "i" is an integer, the "primary" field is null.
		 */
		private RWSetElement primary;

		/**
		 * the "involved" field contains (at least) everything involved in the
		 * evaluation of the expression.
		 */
		private RWSet involved;

		TempRWResult(RWSetElement primary, RWSet involved) {
			this.primary = primary;
			this.involved = involved;
		}
	}

	/* ************** constructor **************/
	public SimpleReadWriteAnalyzer(
			FlowInsensePointsToAnalyzer pointsToAnalyzer) {
		this.pointsToAnalyzer = pointsToAnalyzer;
		this.isFactory = this.pointsToAnalyzer.insensitiveFlowFactory();
		// TODO: not import common package!
		this.rwFactory = new CommonReadWriteDataStructureFactory();
	}

	public RWSetElement packVariable(Variable var) {
		return this.rwFactory.baseElement(var.getFirstDeclaration(),
				isFactory.assignStoreExpr(var));
	}

	/**
	 * <p>
	 * collect read/write set from a {@link StatementNode}, an
	 * {@link OrdinaryDeclarationNode} or an {@link ExpressionNode}. If the
	 * input is not a statement, a declaration or an expression, this method
	 * returns null;
	 * </p>
	 * 
	 * <p>
	 * Note that write set is always a subset of the read set.
	 * </p>
	 * 
	 * @param function
	 *            the function where this given ASTNode belongs to
	 * @param stmtDeclExpr
	 *            an instance of {@link StatementNode}, Declaration or
	 *            {@link ExpressionNode}, otherwise this method is a no-op
	 * @param locallyDeclaredEntities
	 *            OUTPUT argument: the set of entities declared in the given
	 *            "stmtDeclExpr" node
	 * @return the read/write set of the given node
	 * @throws SimpleFullSetException
	 */
	public RWSet collectRWFromStmtDeclExpr(Function function,
			ASTNode stmtDeclExpr, Set<Entity> locallyDeclaredEntities)
			throws SimpleFullSetException {
		this.currentFunction = function;
		this.declaredEntities = new HashSet<>();

		RWSet result = collectReadsWritesWorker(stmtDeclExpr);

		locallyDeclaredEntities.addAll(declaredEntities);
		return result;
	}

	// worker method of "collectReadsWrites" :
	private RWSet collectReadsWritesWorker(ASTNode stmtDeclExpr)
			throws SimpleFullSetException {
		if (stmtDeclExpr == null)
			return null;
		if (stmtDeclExpr.nodeKind() == NodeKind.STATEMENT)
			return collectStmtReadsWrites((StatementNode) stmtDeclExpr);
		else if (stmtDeclExpr.nodeKind() == NodeKind.EXPRESSION) {
			List<TempRWResult> tmps = collectExprReadsWrites(
					(ExpressionNode) stmtDeclExpr);
			RWSet rwSet = rwFactory.newRWSet();

			for (TempRWResult tmp : tmps)
				rwSet.add(tmp.involved);
			return rwSet;
		} else if (stmtDeclExpr.nodeKind() == NodeKind.DECLARATION_LIST) {
			DeclarationListNode list = (DeclarationListNode) stmtDeclExpr;
			RWSet rwset = rwFactory.newRWSet();

			for (VariableDeclarationNode varDecl : list)
				rwset.add(collectVarDeclReadsWrites(varDecl));
			return rwset;
		} else if (stmtDeclExpr.nodeKind() == NodeKind.VARIABLE_DECLARATION)
			return collectVarDeclReadsWrites(
					(VariableDeclarationNode) stmtDeclExpr);
		else
			return null;
	}

	// process variable declaration
	private RWSet collectVarDeclReadsWrites(VariableDeclarationNode varDecl)
			throws SimpleFullSetException {
		InitializerNode init = varDecl.getInitializer();
		RWSet result = rwFactory.newRWSet();

		declaredEntities.add(varDecl.getEntity());
		if (init == null)
			return result;
		result.add(collectInitializerReadsWrites(init));

		AssignExprIF varAbstractObj = isFactory
				.assignStoreExpr(varDecl.getEntity());
		RWSetElement writeElement = rwFactory.baseElement(varDecl,
				varAbstractObj);

		result.addWrites(writeElement);
		return result;
	}

	// collect read/write set from a statement
	private RWSet collectStmtReadsWrites(StatementNode stmt)
			throws SimpleFullSetException {
		switch (stmt.statementKind()) {
			case EXPRESSION : {
				List<TempRWResult> tmps = collectExprReadsWrites(
						((ExpressionStatementNode) stmt).getExpression());
				RWSet rwSet = rwFactory.newRWSet();

				for (TempRWResult tmp : tmps)
					rwSet.add(tmp.involved);
				return rwSet;
			}
			case ATOMIC :
			case CHOOSE :
			case CIVL_FOR :
			case COMPOUND :
			case IF :
			case JUMP :
			case LABELED :
			case LOOP :
			case NULL :
			case PRAGMA :
			case RUN :
			case SWITCH :
			case WHEN :
				RWSet result = rwFactory.newRWSet();

				for (ASTNode child : stmt.children()) {
					RWSet tmp = collectReadsWritesWorker(child);

					if (tmp != null)
						result.add(tmp);
				}
				return result;
			case OMP :
			default :
				throw new CIVLInternalException(
						"Unexpected statement kind for read/write analysis "
								+ stmt.statementKind(),
						stmt.getSource());
		}
	}

	// collect reads/writes in initializers:
	private RWSet collectInitializerReadsWrites(InitializerNode initNode)
			throws SimpleFullSetException {
		if (initNode.nodeKind() == NodeKind.EXPRESSION) {
			List<TempRWResult> tmps = collectExprReadsWrites(
					(ExpressionNode) initNode);
			RWSet rwSet = rwFactory.newRWSet();

			for (TempRWResult tmp : tmps)
				rwSet.add(tmp.involved);
			return rwSet;
		} else {
			CompoundInitializerNode compInitNode = (CompoundInitializerNode) initNode;
			RWSet result = rwFactory.newRWSet();

			for (PairNode<DesignationNode, InitializerNode> pair : compInitNode)
				result.add(collectInitializerReadsWrites(pair.getRight()));
			return result;
		}
	}

	// collect read/write set from an ExpressionNode
	private List<TempRWResult> collectExprReadsWrites(ExpressionNode expr)
			throws SimpleFullSetException {
		ExpressionKind kind = expr.expressionKind();

		switch (kind) {
			case ARROW :
				return collectArrowReadsWrites((ArrowNode) expr, false);
			case DOT :
				return collectDotReadsWrites((DotNode) expr, false);
			case FUNCTION_CALL :
				return collectCallReadsWrites((FunctionCallNode) expr);
			case IDENTIFIER_EXPRESSION :
				return collectIdentifierExpression(
						(IdentifierExpressionNode) expr, false);
			case OPERATOR :
				return collectOperatorReadsWrites((OperatorNode) expr);
			case CONSTANT :
				return new LinkedList<>();
			default :
				List<TempRWResult> results = new LinkedList<>();
				RWSet involved = rwFactory.newRWSet();

				for (ASTNode child : expr.children()) {
					RWSet tmp = collectReadsWritesWorker(child);

					if (tmp != null)
						involved.add(tmp);
				}
				results.add(new TempRWResult(
						rwFactory.arbitraryElement(expr, expr.getType()),
						involved));
				return results;
		}
	}

	// collect read/write set from an OperatorNode:
	private List<TempRWResult> collectOperatorReadsWrites(OperatorNode opNode)
			throws SimpleFullSetException {
		ExpressionNode lhs;
		List<ExpressionNode> rhs = new LinkedList<>();

		switch (opNode.getOperator()) {
			case SUBSCRIPT :
				return collectSubscriptNode(opNode, false);
			case DEREFERENCE :
				return collectDereferenceReadsWrites(opNode, false);
			// all kinds of assignments:
			case ASSIGN :
				lhs = opNode.getArgument(0);
				rhs.add(opNode.getArgument(1));
				break;
			case SHIFTLEFTEQ :
			case SHIFTRIGHTEQ :
			case TIMESEQ :
			case DIVEQ :
			case MINUSEQ :
			case MODEQ :
			case PLUSEQ :
				lhs = opNode.getArgument(0);
				rhs.add(opNode.getArgument(1));
				rhs.add(lhs);
				break;
			case POSTDECREMENT :
			case POSTINCREMENT :
			case PREDECREMENT :
			case PREINCREMENT :
				lhs = opNode.getArgument(0);
				rhs.add(lhs);
				break;
			case PLUS :
				boolean oftPositive = true;
			case MINUS :
				oftPositive = false;
				if (opNode.getType().kind() == TypeKind.POINTER)
					return collectPointerAddition(opNode, oftPositive);
			default :
				List<TempRWResult> result = new LinkedList<>();
				RWSet involved = rwFactory.newRWSet();
				int numArgs = opNode.getNumberOfArguments();

				for (int i = 0; i < numArgs; i++)
					for (TempRWResult tmp : collectExprReadsWrites(
							opNode.getArgument(i)))
						involved.add(tmp.involved);
				result.add(new TempRWResult(
						rwFactory.arbitraryElement(opNode, opNode.getType()),
						involved));
				return result;
		}

		// collect for assignments:
		List<TempRWResult> lhsResults = collectLHSExprReadsWrites(lhs);

		for (TempRWResult lhsResult : lhsResults)
			for (ExpressionNode rhsExpr : rhs)
				for (TempRWResult rhsResult : collectExprReadsWrites(rhsExpr))
					lhsResult.involved.add(rhsResult.involved);
		return lhsResults;
	}

	// add to read/write set if this identifier refers to a variable:
	private List<TempRWResult> collectIdentifierExpression(
			IdentifierExpressionNode id, boolean isWrite) {
		Entity entity = id.getIdentifier().getEntity();
		RWSet result = rwFactory.newRWSet();
		List<TempRWResult> ret = new LinkedList<>();

		if (entity.getEntityKind() == EntityKind.VARIABLE) {
			AssignExprIF varAbstractObj = this.isFactory
					.assignStoreExpr((Variable) entity);
			RWSetElement varElement = rwFactory.baseElement(id, varAbstractObj);

			if (isWrite)
				result.addWrites(varElement);
			result.addReads(varElement);
			ret.add(new TempRWResult(varElement, result));
		}
		return ret;
	}

	// collect read/write set for subscript expression
	private List<TempRWResult> collectSubscriptNode(OperatorNode opNode,
			boolean isLhs) throws SimpleFullSetException {
		/*
		 * the invariant of the RWSet.arraySubscript field must be satisfied: if
		 * the base array "x" of the subscript expression "x[i][...][j]" is a
		 * pointer, the read/write set will includes all "y[i][...][j]" where
		 * "y" is an element in the points-to set of "x".
		 */
		List<ExpressionNode> indices = new LinkedList<>();
		ExpressionNode baseArray = indices(opNode, indices);
		List<TempRWResult> arrayResults = collectExprReadsWrites(baseArray);
		ExpressionNode[] indicesArray = new ExpressionNode[indices.size()];

		indices.toArray(indicesArray);

		List<TempRWResult> result;
		RWSet idxInvolved = rwFactory.newRWSet();

		if (baseArray.getInitialType().kind() == TypeKind.ARRAY) {
			collectSubscriptNodeArrayWorker(opNode, arrayResults, indicesArray,
					isLhs);
			result = arrayResults;
		} else {
			// dereference pointers:
			result = collectSubscriptNodePointerWorker(opNode, arrayResults,
					indicesArray, isLhs);
		}
		for (ExpressionNode idx : indicesArray)
			for (TempRWResult r : collectExprReadsWrites(idx))
				idxInvolved.add(r.involved);
		for (TempRWResult tmp : result)
			tmp.involved.add(idxInvolved);
		return result;
	}

	private List<TempRWResult> collectPointerAddition(OperatorNode opNode,
			boolean isPositive) throws SimpleFullSetException {
		ExpressionNode ptr = opNode.getArgument(0);
		ExpressionNode oft = opNode.getArgument(1);

		if (ptr.getType().kind() != TypeKind.POINTER) {
			// exchange:
			ExpressionNode tmp = ptr;
			ptr = oft;
			oft = tmp;
		}

		RWSet oftInvolved = rwFactory.newRWSet();

		for (TempRWResult oftRet : collectExprReadsWrites(oft))
			oftInvolved.add(oftRet.involved);

		List<TempRWResult> results = collectExprReadsWrites(ptr);

		for (TempRWResult ptrRet : results) {
			ptrRet.involved.add(oftInvolved);
			ptrRet.primary = rwFactory.offsetElement(opNode, ptrRet.primary,
					oft, isPositive);
		}
		return results;
	}

	/**
	 * worker method for {@link #collectSubscriptNode(OperatorNode, boolean)}
	 * when the "array" part in an subscript expression is actually a array (not
	 * a pointer).
	 * 
	 * @param arrayResultsINOUT
	 * @param indices
	 * @param isLhs
	 */
	private void collectSubscriptNodeArrayWorker(ExpressionNode subscriptNode,
			List<TempRWResult> arrayResultsINOUT, ExpressionNode[] indices,
			boolean isLhs) {
		RWSet involved = rwFactory.newRWSet();

		for (TempRWResult arrayResult : arrayResultsINOUT) {
			RWSetElement subscript = rwFactory.subscriptElement(subscriptNode,
					arrayResult.primary, isFactory.assignOffsetZero(), indices);

			arrayResult.primary = subscript;
			involved.add(arrayResult.involved);

			if (subscript.type().isScalar()) {
				involved.addReads(subscript);
				if (isLhs)
					involved.addWrites(subscript);
			}
		}
		for (TempRWResult arrayResult : arrayResultsINOUT)
			arrayResult.involved = involved;
	}

	/**
	 * worker method for {@link #collectSubscriptNode(OperatorNode, boolean)}
	 * when the "array" part in an subscript expression is actually a pointer.
	 * 
	 * @param ptrResults
	 * @param indices
	 * @param isLhs
	 * @return
	 * @throws SimpleFullSetException
	 */
	private List<TempRWResult> collectSubscriptNodePointerWorker(
			ExpressionNode subscriptNode, List<TempRWResult> ptrResults,
			ExpressionNode[] indices, boolean isLhs)
			throws SimpleFullSetException {
		RWSet involved = rwFactory.newRWSet();
		List<TempRWResult> results = new LinkedList<>();
		List<RWSetElement> primaries = new LinkedList<>();

		for (TempRWResult ptrResult : ptrResults) {
			for (AssignExprIF pt : pointsTo(toAbstractObj(ptrResult.primary),
					isLhs)) {
				/*
				 * Explain the assertion:
				 * 
				 * For p[x] s.t. p is a pointer, an object that is pointed-to by
				 * p must be an element of an array, this is guaranteed by the
				 * points-to analyzer:
				 */
				assert pt.kind() == AssignExprKind.SUBSCRIPT;
				AssignSubscriptExprIF subscriptPt = (AssignSubscriptExprIF) pt;
				RWSetElement ptElement = rwFactory
						.baseElement(source(subscriptPt), subscriptPt.array());

				ptElement = rwFactory.subscriptElement(subscriptNode, ptElement,
						subscriptPt.index(), indices);
				primaries.add(ptElement);
			}
			involved.add(ptrResult.involved);
		}
		for (RWSetElement primary : primaries) {
			if (primary.type().isScalar()) {
				involved.addReads(primary);
				if (isLhs)
					involved.addWrites(primary);
			}
			results.add(new TempRWResult(primary, involved));
		}
		return results;
	}

	/*
	 * collect reads/writes in LHS expression.
	 */
	private List<TempRWResult> collectLHSExprReadsWrites(ExpressionNode lhs)
			throws SimpleFullSetException {
		ExpressionKind kind = lhs.expressionKind();

		switch (kind) {
			case ARROW :
				return collectArrowReadsWrites((ArrowNode) lhs, true);
			case DOT :
				return collectDotReadsWrites((DotNode) lhs, true);
			case IDENTIFIER_EXPRESSION :
				return collectIdentifierExpression(
						(IdentifierExpressionNode) lhs, true);
			case OPERATOR :
				OperatorNode opNode = (OperatorNode) lhs;

				switch (opNode.getOperator()) {
					case DEREFERENCE :
						return collectDereferenceReadsWrites(opNode, true);
					case SUBSCRIPT :
						return collectSubscriptNode(opNode, true);
					default :
				}
			default :
				throw new CIVLInternalException(
						"unexpected left-hand side expression "
								+ lhs.prettyRepresentation(),
						lhs.getSource());
		}
	}

	private List<TempRWResult> collectDereferenceReadsWrites(OperatorNode node,
			boolean isLhs) throws SimpleFullSetException {
		ExpressionNode ptr = node.getArgument(0);
		List<TempRWResult> result = collectExprReadsWrites(ptr);
		RWSet involved = rwFactory.newRWSet();
		List<RWSetElement> pts = new LinkedList<>();

		for (TempRWResult t : result) {
			for (AssignExprIF pt : pointsTo(toAbstractObj(t.primary), isLhs))
				pts.add(rwFactory.baseElement(source(pt), pt));
			involved.add(t.involved);
		}
		result.clear();
		for (RWSetElement pt : pts)
			result.add(new TempRWResult(pt, involved));
		return result;
	}

	/**
	 * Given an ArrowNode <code>a->id</code>, return the reads/writes set <code>
	 * read set:                       collect(a) U pts(collect(a)).id 
	 * write set (if isLhs == true):   pts(collect(a)).id 
	 * </code>
	 * 
	 * @param arrowNode
	 * @param isLhs
	 *            true iff this expression appears at Left-hand side
	 * @return
	 * @throws SimpleFullSetException
	 */
	private List<TempRWResult> collectArrowReadsWrites(ArrowNode arrowNode,
			boolean isLhs) throws SimpleFullSetException {
		// collect(a):
		List<TempRWResult> ptrs = collectExprReadsWrites(
				arrowNode.getStructurePointer());
		Field field = (Field) arrowNode.getFieldName().getEntity();
		// pts(collect(a)).id :
		List<RWSetElement> pointsToFiledAccesses = new LinkedList<>();
		// every r/w involved:
		RWSet involved = rwFactory.newRWSet();

		for (TempRWResult ptr : ptrs) {
			for (AssignExprIF pt : pointsTo(toAbstractObj(ptr.primary),
					isLhs)) {
				RWSetElement ptElement = rwFactory.baseElement(source(pt), pt);

				pointsToFiledAccesses.add(
						rwFactory.fieldElement(arrowNode, ptElement, field));
			}
			involved.add(ptr.involved);
		}

		List<TempRWResult> results = new LinkedList<>();

		for (RWSetElement primary : pointsToFiledAccesses) {
			if (primary.type().isScalar()) {
				involved.addReads(primary);
				if (isLhs)
					involved.addWrites(primary);
			}
			results.add(new TempRWResult(primary, involved));
		}
		return results;
	}

	/**
	 * 
	 * collect DotNode
	 * 
	 * @throws SimpleFullSetException
	 */
	private List<TempRWResult> collectDotReadsWrites(DotNode dotNode,
			boolean isLhs) throws SimpleFullSetException {
		Field field = (Field) dotNode.getFieldName().getEntity();
		List<TempRWResult> structs = collectExprReadsWrites(
				dotNode.getStructure());
		RWSet involved = rwFactory.newRWSet();

		for (TempRWResult struct : structs) {
			RWSetElement fieldAccess = rwFactory.fieldElement(dotNode,
					struct.primary, field);

			struct.primary = fieldAccess;
			involved.add(struct.involved);
			if (fieldAccess.type().isScalar()) {
				involved.addReads(fieldAccess);
				if (isLhs)
					involved.addWrites(fieldAccess);
			}
		}
		for (TempRWResult struct : structs)
			struct.involved = involved;
		return structs;
	}

	// collect function body but ignore local access
	private List<TempRWResult> collectCallReadsWrites(FunctionCallNode call)
			throws SimpleFullSetException {
		List<TempRWResult> results = new LinkedList<>();
		RWSet involved = rwFactory.newRWSet();
		ExpressionNode funcExpr = call.getFunction();

		if (funcExpr.expressionKind() != ExpressionKind.IDENTIFIER_EXPRESSION)
			throw new CIVLUnimplementedFeatureException(
					"analyzer function call through function pointer: "
							+ call.prettyRepresentation());

		Function func = (Function) ((IdentifierExpressionNode) funcExpr)
				.getIdentifier().getEntity();

		if (func.getDefinition() == null)
			involved.add(visibleVariables(call.getScope()));
		else {
			Set<Entity> locallyDefinedInFunc = new HashSet<>();
			RWSet bodyResult = collectRWFromStmtDeclExpr(func,
					func.getDefinition().getBody(), locallyDefinedInFunc);

			for (RWSetElement e : bodyResult.reads())
				if (!locallyDefinedInFunc.contains(containedBy(e)))
					involved.addReads(e);
			for (RWSetElement e : bodyResult.writes())
				if (!locallyDefinedInFunc.contains(containedBy(e)))
					involved.addWrites(e);
		}
		// TODO: actual parameters
		results.add(new TempRWResult(
				rwFactory.arbitraryElement(call, call.getType()), involved));
		return results;
	}

	/**
	 * the points-to set of the pointer expression.
	 * 
	 * @param ptr
	 * @return points-to set of ptr
	 * @throws SimpleFullSetException
	 *             if there is a FULL in its points-to set
	 */
	private List<AssignExprIF> pointsTo(AssignExprIF ptr, boolean isLhs)
			throws SimpleFullSetException {
		List<AssignExprIF> result = pointsToAnalyzer
				.mayPointsTo(currentFunction, ptr);

		if (result.contains(isFactory.full()))
			throw new SimpleFullSetException(isLhs);
		return result;
	}

	private ASTNode source(AssignExprIF e) {
		AssignStoreExprIF root = (AssignStoreExprIF) e.root();

		if (root.isAllocation())
			return root.store();
		else
			return root.variable().getFirstDeclaration();
	}

	private RWSet visibleVariables(Scope scope) {
		RWSet result = rwFactory.newRWSet();

		for (Variable var : scope.getVariables()) {
			RWSetElement e = rwFactory.baseElement(var.getFirstDeclaration(),
					isFactory.assignStoreExpr(var));

			result.addReads(e);
			result.addWrites(e);
		}
		if (scope.getParentScope() != null)
			result.add(visibleVariables(scope.getParentScope()));
		return result;
	}

	/**
	 * 
	 * @param subscriptNode
	 *            a expression node of the form: <code>e[x][y][...]</code>
	 * @param indicesOutput
	 *            OUTPUT arguments: the indices of the subscript expression
	 * @return the base array of the subscript expression
	 */
	private ExpressionNode indices(OperatorNode subscriptNode,
			List<ExpressionNode> indicesOutput) {
		ExpressionNode ret = subscriptNode;

		if (subscriptNode.getOperator() == Operator.SUBSCRIPT) {
			ExpressionNode idx = subscriptNode.getArgument(1);
			ExpressionNode arr = subscriptNode.getArgument(0);

			if (arr.expressionKind() == ExpressionKind.OPERATOR)
				ret = indices((OperatorNode) arr, indicesOutput);
			else
				ret = arr;
			indicesOutput.add(idx);
		}
		return ret;
	}

	/**
	 * converts a {@link RWSetElement} to an abstract object
	 * {@link AssignExprIF}
	 */
	private AssignExprIF toAbstractObj(RWSetElement e) {
		switch (e.kind()) {
			case BASE :
				return ((RWSetBaseElement) e).base();
			case FIELD : {
				RWSetFieldElement fieldEle = (RWSetFieldElement) e;
				AssignExprIF struct = toAbstractObj(fieldEle.struct());

				return isFactory.assignFieldExpr(struct, fieldEle.field());
			}
			case OFFSET : {
				RWSetOffsetElement offsetEle = (RWSetOffsetElement) e;
				AssignExprIF base = toAbstractObj(offsetEle.base());

				return isFactory.assignOffsetExpr(base, isFactory.assignOffset(
						offsetEle.offset(), offsetEle.isPositive()));
			}
			case SUBSCRIPT : {
				RWSetSubscriptElement subscriptEle = (RWSetSubscriptElement) e;
				AssignExprIF array = this.toAbstractObj(subscriptEle.array());
				AssignExprIF ret = array;
				ExpressionNode indices[] = subscriptEle.indices();

				AssignOffsetIF oft = subscriptEle.offset();
				AssignOffsetIF firstDimIdx = isFactory.assignOffset(indices[0],
						true);

				ret = isFactory.assignSubscriptExpr(ret,
						addOffsets(oft, firstDimIdx));
				for (int i = 1; i < indices.length; i++)
					ret = isFactory.assignSubscriptExpr(ret,
							isFactory.assignOffset(indices[i], true));
				return ret;
			}
			default :
				throw new CIVLInternalException(
						"Read/write analyzer cannot process " + e.toString(),
						e.source().getSource());
		}
	}

	/**
	 * sums up two {@link AssignOffsetIF}s
	 */
	private AssignOffsetIF addOffsets(AssignOffsetIF oft0,
			AssignOffsetIF oft1) {
		if (oft0.hasConstantValue() && oft1.hasConstantValue()) {
			int i = oft0.constantValue().intValue()
					+ oft1.constantValue().intValue();

			return isFactory.assignOffset(i);
		}
		return oft0.hasConstantValue() ? oft1 : oft0;
	}

	/**
	 * Attempt to return the {@link Variable} that contains the object
	 * represented by the given {@link RWSetElement}. If the object is not a
	 * part of any variable, returns null.
	 */
	private Variable containedBy(RWSetElement e) {
		AssignExprIF ao = e.root();

		if (ao == null || ao.kind() != AssignExprKind.STORE)
			return null;

		AssignStoreExprIF store = (AssignStoreExprIF) ao;

		return store.variable();
	}
}
