package dev.civl.mc.model.common;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.civl.abc.ast.conversion.IF.Conversion;
import dev.civl.abc.ast.conversion.IF.Conversion.ConversionKind;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Entity.EntityKind;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.entity.IF.Label;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.acsl.AssignsOrReadsNode;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.acsl.InvariantNode;
import dev.civl.abc.ast.node.IF.compound.CompoundInitializerNode;
import dev.civl.abc.ast.node.IF.compound.CompoundLiteralObject;
import dev.civl.abc.ast.node.IF.compound.LiteralObject;
import dev.civl.abc.ast.node.IF.compound.ScalarLiteralObject;
import dev.civl.abc.ast.node.IF.declaration.AbstractFunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.FieldDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.declaration.OrdinaryDeclarationNode.OrdinaryDeclarationKind;
import dev.civl.abc.ast.node.IF.declaration.TypedefDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ArrayLambdaNode;
import dev.civl.abc.ast.node.IF.expression.ArrowNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.CompoundLiteralNode;
import dev.civl.abc.ast.node.IF.expression.ConstantNode;
import dev.civl.abc.ast.node.IF.expression.ConstantNode.ConstantKind;
import dev.civl.abc.ast.node.IF.expression.DotNode;
import dev.civl.abc.ast.node.IF.expression.EnumerationConstantNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.HereOrRootNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.IntegerConstantNode;
import dev.civl.abc.ast.node.IF.expression.LambdaNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.ast.node.IF.expression.ResultNode;
import dev.civl.abc.ast.node.IF.expression.ScopeOfNode;
import dev.civl.abc.ast.node.IF.expression.SizeableNode;
import dev.civl.abc.ast.node.IF.expression.SizeofNode;
import dev.civl.abc.ast.node.IF.expression.SpawnNode;
import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;
import dev.civl.abc.ast.node.IF.label.LabelNode;
import dev.civl.abc.ast.node.IF.label.OrdinaryLabelNode;
import dev.civl.abc.ast.node.IF.label.SwitchLabelNode;
import dev.civl.abc.ast.node.IF.statement.AtomicNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.ChooseStatementNode;
import dev.civl.abc.ast.node.IF.statement.CivlForNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.DeclarationListNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopInitializerNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopNode;
import dev.civl.abc.ast.node.IF.statement.GotoNode;
import dev.civl.abc.ast.node.IF.statement.IfNode;
import dev.civl.abc.ast.node.IF.statement.JumpNode;
import dev.civl.abc.ast.node.IF.statement.JumpNode.JumpKind;
import dev.civl.abc.ast.node.IF.statement.LabeledStatementNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode;
import dev.civl.abc.ast.node.IF.statement.NullStatementNode;
import dev.civl.abc.ast.node.IF.statement.ReturnNode;
import dev.civl.abc.ast.node.IF.statement.RunNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.SwitchNode;
import dev.civl.abc.ast.node.IF.statement.WhenNode;
import dev.civl.abc.ast.node.IF.type.ArrayTypeNode;
import dev.civl.abc.ast.node.IF.type.EnumerationTypeNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.PointerTypeNode;
import dev.civl.abc.ast.node.IF.type.StructureOrUnionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode.TypeNodeKind;
import dev.civl.abc.ast.type.IF.ArrayType;
import dev.civl.abc.ast.type.IF.DomainType;
import dev.civl.abc.ast.type.IF.EnumerationType;
import dev.civl.abc.ast.type.IF.Enumerator;
import dev.civl.abc.ast.type.IF.Field;
import dev.civl.abc.ast.type.IF.FunctionType;
import dev.civl.abc.ast.type.IF.ObjectType;
import dev.civl.abc.ast.type.IF.PointerType;
import dev.civl.abc.ast.type.IF.QualifiedObjectType;
import dev.civl.abc.ast.type.IF.StandardBasicType;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.StructureOrUnionType;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.ast.value.IF.CharacterValue;
import dev.civl.abc.ast.value.IF.IntegerValue;
import dev.civl.abc.ast.value.IF.RealFloatingValue;
import dev.civl.abc.ast.value.IF.Value;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.StringLiteral;
import dev.civl.gmc.CommandLineException;
import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.model.IF.AbstractFunction;
import dev.civl.mc.model.IF.CIVLException;
import dev.civl.mc.model.IF.CIVLFunction;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLSyntaxException;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.Fragment;
import dev.civl.mc.model.IF.Identifier;
import dev.civl.mc.model.IF.LogicFunction;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.contract.LoopContract;
import dev.civl.mc.model.IF.expression.ArrayLambdaExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression.BINARY_OPERATOR;
import dev.civl.mc.model.IF.expression.CompoundLiteralExpression;
import dev.civl.mc.model.IF.expression.CompoundLiteralExpression.CIVLCompoundLiteralObject;
import dev.civl.mc.model.IF.expression.CompoundLiteralExpression.CIVLLiteralObject;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.FunctionIdentifierExpression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.expression.LambdaExpression;
import dev.civl.mc.model.IF.expression.LiteralExpression;
import dev.civl.mc.model.IF.expression.QuantifiedExpression;
import dev.civl.mc.model.IF.expression.QuantifiedExpression.Quantifier;
import dev.civl.mc.model.IF.expression.UnaryExpression.UNARY_OPERATOR;
import dev.civl.mc.model.IF.expression.VariableExpression;
import dev.civl.mc.model.IF.location.Location;
import dev.civl.mc.model.IF.location.Location.AtomicKind;
import dev.civl.mc.model.IF.statement.AssignStatement;
import dev.civl.mc.model.IF.statement.CallOrSpawnStatement;
import dev.civl.mc.model.IF.statement.CivlParForSpawnStatement;
import dev.civl.mc.model.IF.statement.MallocStatement;
import dev.civl.mc.model.IF.statement.NoopStatement;
import dev.civl.mc.model.IF.statement.ReturnStatement;
import dev.civl.mc.model.IF.statement.Statement;
import dev.civl.mc.model.IF.type.CIVLArrayType;
import dev.civl.mc.model.IF.type.CIVLCompleteArrayType;
import dev.civl.mc.model.IF.type.CIVLCompleteDomainType;
import dev.civl.mc.model.IF.type.CIVLFunctionType;
import dev.civl.mc.model.IF.type.CIVLPointerType;
import dev.civl.mc.model.IF.type.CIVLPrimitiveType;
import dev.civl.mc.model.IF.type.CIVLPrimitiveType.PrimitiveTypeKind;
import dev.civl.mc.model.IF.type.CIVLSetType;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.type.StructOrUnionField;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.model.common.expression.CommonUndefinedProcessExpression;
import dev.civl.mc.model.common.statement.CommonAtomicLockAssignStatement;
import dev.civl.mc.util.IF.Pair;
import dev.civl.mc.util.IF.Singleton;
import dev.civl.mc.util.IF.Triple;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;

/**
 * This class translates an AST node of a function body and completes the
 * resulting function accordingly. The only incomplete translation is the call
 * or spawn statements involved in this function, which dont have the
 * corresponding function of invocation set appropriately.
 * 
 * @author Manchun Zheng (zmanchun)
 * 
 */
public class FunctionTranslator {
	private static final String ARTIFICIAL_VAR_NAME = "_civl_ir";

	private static final String PAR_FUNC_NAME = "_par_proc";

	private static final String RUN_FUNC_NAME = "_run_proc";

	/* ************************** Instance Fields ************************** */

	private int atomicCount = 0;

	/**
	 * Counter for the variable of a counter for $for loop on literal domains.
	 */
	private int literalDomForCounterCount = 0;

	/**
	 * Store temporary information of the function being processed
	 */
	protected FunctionInfo functionInfo;

	/**
	 * The unique model factory to be used in the system.
	 */
	protected ModelFactory modelFactory;

	/**
	 * The unique type factory to be used in the system.
	 */
	private CIVLTypeFactory typeFactory;

	/**
	 * The unique model builder of the system.
	 */
	private ModelBuilderWorker modelBuilder;

	/**
	 * The AST node of the function body, which is to be used for translation.
	 */
	private StatementNode functionBodyNode;

	/**
	 * The CIVL function that is the result of this function translator.
	 */
	private CIVLFunction function;

	private CIVLConfiguration civlConfig;

	/* **************************** Constructors *************************** */

	/**
	 * Constructs new instance of function translator. This constructor will be used
	 * for translating all function nodes except for the system function. See also
	 * {@link #FunctionTranslator(ModelBuilderWorker, ModelFactory, StatementNode, CIVLFunction, CIVLConfiguration)}
	 * .
	 *
	 * @param modelBuilder The model builder worker where this function translator
	 *                     is created.
	 * @param modelFactory The unique model factory used by the system to create new
	 *                     instances of CIVL expressions, statements, etc.
	 * @param bodyNode     The AST node of the function body that this function
	 *                     translator is going to translate.
	 * @param function     The CIVL function that will be the result of this
	 *                     function translator.
	 */
	FunctionTranslator(ModelBuilderWorker modelBuilder, ModelFactory modelFactory, StatementNode bodyNode,
			CIVLFunction function, CIVLConfiguration civlConfig) {
		this.modelBuilder = modelBuilder;
		this.modelFactory = modelFactory;
		this.typeFactory = modelFactory.typeFactory();
		this.functionBodyNode = bodyNode;
		this.setFunction(function);
		this.functionInfo = new FunctionInfo(function);
		this.civlConfig = civlConfig;
	}

	/**
	 * Constructs new instance of function translator. This constructor will be used
	 * only for translating the system function, because initially the model builder
	 * worker doesn't know about the function body node of the system function
	 * (i.e., the body node of the main function). It will have to translate the
	 * root nodes before processing the main function. See also
	 * {@link #translateRootFunction(Scope, ASTNode)}.
	 * 
	 * @param modelBuilder The model builder worker where this function translator
	 *                     is created.
	 * @param modelFactory The unique model factory used by the system to create new
	 *                     instances of CIVL expressions, statements, etc.
	 * @param bodyNode     The AST node of the function body that this function
	 *                     translator is going to translate.
	 * @param function     The CIVL function that will be the result of this
	 *                     function translator.
	 */
	FunctionTranslator(ModelBuilderWorker modelBuilder, ModelFactory modelFactory, CIVLFunction function,
			CIVLConfiguration civlConfig) {
		this.modelBuilder = modelBuilder;
		this.modelFactory = modelFactory;
		this.typeFactory = modelFactory.typeFactory();
		this.setFunction(function);
		this.functionInfo = new FunctionInfo(function);
		this.civlConfig = civlConfig;
	}

	/* *************************** Public Methods ************************** */

	/**
	 * Processes the function body of a function definition node. At least one
	 * function declaration for this function should have been processed already, so
	 * the corresponding CIVL function should already exist.
	 */
	public void translateFunction() {
		Fragment body = this.translateFunctionBody();

		functionInfo.completeFunction(body);
	}

	/**
	 * This method translates the "_CIVL_System" function. The result should be a
	 * function with the following:
	 * <ul>
	 * <li>statements in the global scope, and</li>
	 * <li>statements in the main function body.</li>
	 * </ul>
	 * Initially, the model builder worker have no information about the main
	 * function node. Thus the translation starts at translating the rootNode,
	 * obtaining a list of initialization statements declared in the root scope and
	 * the AST node of the main function.
	 * 
	 * @param systemScope The root scope of the model.
	 * @param rootNode    The root node of the AST for translation.
	 * @throws CIVLSyntaxException if no main function node could be found in the
	 *                             rootNode's children.
	 */
	public void translateRootFunction(Scope systemScope, ASTNode rootNode) {

		Fragment initialization = new CommonFragment();
		Fragment body;

		for (int i = 0; i < rootNode.numChildren(); i++) {
			ASTNode node = rootNode.child(i);
			Fragment fragment;

			if (node != null) {
				fragment = translateASTNode(node, systemScope, null);
				if (fragment != null)
					initialization = initialization.combineWith(fragment);
			}
		}
		if (modelBuilder.mainFunctionNode == null) {
			throw new CIVLSyntaxException("program must have a main function,", modelFactory.sourceOf(rootNode));
		} else {
			Function mainFunction = modelBuilder.mainFunctionNode.getEntity();
			FunctionType functionType = mainFunction.getType();
			FunctionTypeNode functionTypeNode = modelBuilder.mainFunctionNode.getTypeNode();
			SequenceNode<VariableDeclarationNode> abcParameters = functionTypeNode.getParameters();
			int numParameters = abcParameters.numChildren();
			ObjectType abcReturnType = functionType.getReturnType();
			Scope scope = this.function.outerScope();

			if (abcReturnType.kind() != TypeKind.VOID) {
				CIVLType returnType = translateABCTypeNode(
						modelFactory.sourceOf(functionTypeNode.getReturnType().getSource()), scope,
						functionTypeNode.getReturnType());

				this.function.setReturnType(returnType);
			}
			if (numParameters > 0) {
				List<Variable> parameters = new ArrayList<>();
				List<CIVLType> parameterTypes = new ArrayList<>();

				for (int i = 0; i < numParameters; i++) {
					VariableDeclarationNode decl = abcParameters.getSequenceChild(i);

					if (decl.getTypeNode().kind() == TypeNodeKind.VOID)
						continue;
					else {
						CIVLType type = translateABCTypeNode(modelFactory.sourceOf(decl), scope,
								functionTypeNode.getParameters().getSequenceChild(i).getTypeNode());
						CIVLSource source = modelFactory.sourceOf(decl.getIdentifier());
						Identifier variableName = modelFactory.identifier(source, decl.getName());

						parameters.add(modelFactory.variable(source, type, variableName, parameters.size()));
						parameterTypes.add(type);
					}
				}
				this.function.setParameters(parameters);
				this.function.setParameterTypes(parameterTypes.toArray(new CIVLType[parameterTypes.size()]));
			}
			this.functionBodyNode = modelBuilder.mainFunctionNode.getBody();
			body = this.translateFunctionBody();
			body = initialization.combineWith(body);
			functionInfo.completeFunction(body);
		}
	}

	/* *************************** Private Methods ************************* */

	/**
	 * Translate the function body node associated with this function translator.
	 * 
	 * @return The fragment of CIVL locations and statements that represents the
	 *         function body node.
	 */
	private Fragment translateFunctionBody() {
		Fragment body;
		Scope scope = this.function.outerScope();

		body = translateStatementNode(scope, this.functionBodyNode);
		if (!containsReturn(body)) {
			CIVLSource endSource = modelFactory.sourceOfEnd(this.functionBodyNode);
			Location returnLocation = modelFactory.location(endSource, function.outerScope());
			Fragment returnFragment = modelFactory.returnFragment(endSource, returnLocation, null,
					this.functionInfo.function());

			if (body != null)
				body = body.combineWith(returnFragment);
			else
				body = returnFragment;
		}
		return body;
	}

	/*
	 * *********************************************************************
	 * Translate ABC Statement Nodes into CIVL Statements
	 * *********************************************************************
	 */

	/**
	 * Given a StatementNode, return a Fragment representing it. Takes a statement
	 * node where the start location and extra guard are defined elsewhere and
	 * returns the appropriate model statement.
	 * 
	 * @param scope         The scope containing this statement.
	 * @param statementNode The statement node.
	 * @return The fragment representation of this statement.
	 */
	private Fragment translateStatementNode(Scope scope, StatementNode statementNode) {
		Fragment result = null;

		switch (statementNode.statementKind()) {
		// case ASSUME:
		// result = translateAssumeNode(scope, (AssumeNode) statementNode);
		// break;
		// case ASSERT:
		// result = translateAssertNode(scope, (AssertNode) statementNode);
		// break;
		case ATOMIC:
			result = translateAtomicNode(scope, (AtomicNode) statementNode);
			break;
		case CHOOSE:
			result = translateChooseNode(scope, (ChooseStatementNode) statementNode);
			break;
		case CIVL_FOR: {
			CivlForNode forNode = (CivlForNode) statementNode;

			if (forNode.isParallel())
				result = translateParForNode(scope, forNode);
			else
				result = translateCivlForNode(scope, forNode);
			break;
		}
		case COMPOUND:
			result = translateCompoundStatementNode(scope, (CompoundStatementNode) statementNode);
			break;
		case EXPRESSION:
			if (((ExpressionStatementNode) statementNode).getExpression() == null)
				result = new CommonFragment();
			else
				result = translateExpressionStatementNode(scope,
						((ExpressionStatementNode) statementNode).getExpression());
			break;
		// case FOR:
		// result = translateForLoopNode(scope, (ForLoopNode)
		// statementNode);
		// break;
		// case GOTO:
		// result = translateGotoNode(scope, (GotoNode) statementNode);
		// break;
		case IF:
			result = translateIfNode(scope, (IfNode) statementNode);
			break;
		case JUMP:
			result = translateJumpNode(scope, (JumpNode) statementNode);
			break;
		case LABELED:
			result = translateLabelStatementNode(scope, (LabeledStatementNode) statementNode);
			break;
		case LOOP:// either WHILE loop or DO_WHILE loop
			result = translateLoopNode(scope, (LoopNode) statementNode);
			break;
		case NULL:
			result = translateNullStatementNode(scope, (NullStatementNode) statementNode);
			break;
		case RUN:
			result = translateRunStatementNode(scope, (RunNode) statementNode);
			break;
		case SWITCH:
			result = translateSwitchNode(scope, (SwitchNode) statementNode);
			break;
		case WHEN:
			result = translateWhenNode(scope, (WhenNode) statementNode);
			break;
		default:
			throw new CIVLUnimplementedFeatureException(
					"translating statement nodes of type " + statementNode.statementKind(),
					modelFactory.sourceOf(statementNode));
		}
		if (!modelFactory.anonFragment().isEmpty()) {
			result = modelFactory.anonFragment().combineWith(result);
			modelFactory.clearAnonFragment();
		}
		return result;
	}

	private Fragment translateParForNode(Scope scope, CivlForNode civlForNode) {
		DeclarationListNode loopInits = civlForNode.getVariables();
		Triple<Scope, Fragment, List<Variable>> initResults = this.translateForLoopInitializerNode(scope, loopInits);
		CIVLSource source = modelFactory.sourceOf(civlForNode);
		CIVLSource parForBeginSource = modelFactory.sourceOfBeginning(civlForNode);
		CIVLSource parForEndSource = modelFactory.sourceOfEnd(civlForNode);
		Scope parForScope = modelFactory.scope(source, scope, Arrays.asList(), scope.function());
		VariableExpression domSizeVar = modelFactory.domSizeVariable(source, parForScope);
		CIVLArrayType procsType = typeFactory.completeArrayType(typeFactory.processType(), domSizeVar);
		VariableExpression parProcs = modelFactory.parProcsVariable(source, procsType, parForScope);
		StatementNode bodyNode = civlForNode.getBody();
		// FunctionCallNode bodyFuncCall = this.isFunctionCall(bodyNode);
		CIVLFunction procFunc;
		CivlParForSpawnStatement parForEnter;
		Fragment result;
		Location location;
		Expression domain;

		// even when the body is a single function call statement, we still need
		// to introduce a new proc function to wrap that single function call
		// because there is no guarantee that the arguments of the arbitrary
		// function call would fit the iterator variables of the domain nicely.
		CIVLSource procFuncSource = modelFactory.sourceOf(bodyNode);
		CIVLSource procFuncStartSource = modelFactory.sourceOfBeginning(bodyNode);
		List<Variable> loopVars = initResults.third;
		int numOfLoopVars = loopVars.size();
		Scope parameterScope = this.modelFactory.scope(procFuncSource, parForScope, new ArrayList<>(0), null);
		List<Variable> procFuncParameters = new ArrayList<>(numOfLoopVars);

		for (int i = 0; i < numOfLoopVars; i++) {
			Variable loopVar = loopVars.get(i);
			Variable parameter = modelFactory.variable(loopVar.getSource(), loopVar.type(), loopVar.name(), i + 1);

			procFuncParameters.add(parameter);
			parameterScope.addVariable(parameter);
		}
		procFunc = modelFactory.function(procFuncSource, false,
				modelFactory.identifier(procFuncStartSource, PAR_FUNC_NAME + modelBuilder.parProcFunctions.size()),
				parameterScope, procFuncParameters, typeFactory.voidType(), scope, null);
		scope.addFunction(procFunc);
		parameterScope.setFunction(procFunc);
		modelBuilder.parProcFunctions.put(procFunc, bodyNode);
		domain = this.translateExpressionNode(civlForNode.getDomain(), parForScope, true);
		result = new CommonFragment(this.elaborateDomainCall(parForScope, domain));
		location = modelFactory.location(parForBeginSource, parForScope);
		parForEnter = modelFactory.civlParForEnterStatement(parForBeginSource, location, domain, domSizeVar, parProcs,
				procFunc);
		assert procFunc != null;
		parForEnter.setParProcFunction(procFunc);
		result = result.combineWith(new CommonFragment(parForEnter));
		result = result
				.combineWith(parForProcessesTerminationFragment(domSizeVar, parProcs, parForScope, parForEndSource));
		return result;
	}

	/**
	 * <p>
	 * Returns a {@link Fragment} which contains the generated statements of
	 * terminating the processes spawned by a <code>$parfor</code> statement.
	 * </p>
	 * 
	 * <p>
	 * The fragment is described roughly by the following pseudo code: <code>
	 * int _tmp = 0;
	 * 
	 * while (_tmp < domain_size) {
	 *   $wait(procs[_tmp]);
	 *   _tmp++;
	 * }
	 * </code>
	 * </p>
	 * 
	 * @param domSize      The {@link Expression} represents the size of the domain
	 *                     in a <code>$parfor</code> statement.
	 * @param processArray The {@link Expression} represents an array of processes
	 *                     which are spawned by a <code>$parfor</code> statement
	 * @param scope        The {@link Scope} in where the corresponding
	 *                     <code>$parfor</code> statement locates.
	 * @param source       The {@link CIVLSource} associates to a
	 *                     <code>$parfor</code> statement.
	 * @return a {@link Fragment} which contains the generated statements of
	 *         terminating the processes spawned by a <code>$parfor</code>
	 *         statement.
	 */
	private Fragment parForProcessesTerminationFragment(Expression domSize, LHSExpression processArray, Scope scope,
			CIVLSource source) {
		Scope loopConditionScope = modelFactory.scope(source, scope, Arrays.asList(), scope.function());
		Scope loopBodyScope = modelFactory.scope(source, loopConditionScope, Arrays.asList(), scope.function());
		// Use numVariable in scope to identify artificial variables which can
		// guarantee same name will never appear in the same scope:
		String artificiatialVarName = ARTIFICIAL_VAR_NAME + loopConditionScope.numVariables();
		Variable loopIdentifierVar = modelFactory.variable(source, typeFactory.integerType(),
				modelFactory.identifier(source, artificiatialVarName), loopConditionScope.numVariables());
		Location initLocation = modelFactory.location(source, loopConditionScope);
		Location loopLocation = modelFactory.location(source, loopConditionScope);
		Location waitLocation = modelFactory.location(source, loopBodyScope);
		Location incrementLocation = modelFactory.location(source, loopBodyScope);
		LHSExpression loopIdentifier = modelFactory.variableExpression(source, loopIdentifierVar);

		loopConditionScope.addVariable(loopIdentifierVar);

		Statement initStmt, loopEnter, loopExit, increment;
		CallOrSpawnStatement waitStmt;
		Expression loopCondition, terminateCondition, proc;
		Fragment result;

		loopCondition = modelFactory.binaryExpression(domSize.getSource(), BINARY_OPERATOR.LESS_THAN, loopIdentifier,
				domSize);
		terminateCondition = modelFactory.unaryExpression(source, UNARY_OPERATOR.NOT, loopCondition);
		// loop identifier initialization:
		initStmt = modelFactory.assignStatement(source, initLocation, loopIdentifier,
				modelFactory.integerLiteralExpression(source, BigInteger.ZERO), true);
		loopEnter = modelFactory.loopBranchStatement(source, loopLocation, loopCondition, true, null);
		loopExit = modelFactory.loopBranchStatement(source, loopLocation, terminateCondition, false, null);
		// The argument of the $wait: procArray[loopIdentifier]:
		proc = modelFactory.subscriptExpression(processArray.getSource(), processArray, loopIdentifier);
		// the $wait function should have been in the AST and should
		// have been processed already...
		Function waitFun = (Function) modelBuilder.program.getAST().getInternalOrExternalEntity("$wait");
		if (waitFun == null)
			throw new CIVLInternalException("$wait function missing from AST", source);
		CIVLFunction civlWaitFun = modelBuilder.functionMap.get(waitFun);
		if (civlWaitFun == null)
			throw new CIVLInternalException("$wait function missing from CIVL model", source);
		FunctionIdentifierExpression waitExpr = modelFactory.functionIdentifierExpression(source, civlWaitFun);
		waitStmt = modelFactory.callOrSpawnStatement(source, waitLocation, true, waitExpr, Arrays.asList(proc), null,
				false);
		// I thought CIVL can figure out the guard of system functions by itself
		// (at runtime, the older version CIVL did that and changes happened
		// after POR contracts I believe) but it seems not the case. Not
		// setting guard here will cause CIVL to use the default guard "true"
		// which will break things down. Deciding the guard at model building
		// time definitely is better than what I thought. So I just write down
		// this comment to tell who reads this code about this point.
		waitStmt.setGuard(modelFactory.systemGuardExpression(waitStmt));
		increment = modelFactory.assignStatement(source, incrementLocation, loopIdentifier,
				modelFactory.binaryExpression(source, BINARY_OPERATOR.PLUS, loopIdentifier,
						modelFactory.integerLiteralExpression(source, BigInteger.ONE)),
				false);
		result = new CommonFragment(initStmt);
		result.addNewStatement(loopEnter);
		result.addNewStatement(waitStmt);
		result.addNewStatement(increment);
		result.addNewStatement(loopExit);
		return result;
	}

	private Fragment translateCivlForNode(Scope scope, CivlForNode civlForNode) {
		DeclarationListNode loopInits = civlForNode.getVariables();
		Fragment nextInDomain, result;
		List<Variable> loopVariables;
		ExpressionNode domainNode = civlForNode.getDomain();
		Expression domain;
		Expression domainGuard;
		Variable literalDomCounter;
		Triple<Scope, Fragment, List<Variable>> initResults = this.translateForLoopInitializerNode(scope, loopInits);
		Location location;
		CIVLSource source = modelFactory.sourceOf(civlForNode);
		int dimension;
		Statement elaborateCall;
		SequenceNode<ContractNode> loopContractNode = civlForNode.loopContracts();
		LoopContract loopContract = loopContractNode == null ? null
				: this.translateLoopInvariants(scope, null, loopContractNode, modelFactory.sourceOf(loopContractNode));

		scope = initResults.first;
		// Create a loop counter variable for the for loop.
		literalDomCounter = modelFactory.variable(source, typeFactory.integerType(),
				modelFactory.getLiteralDomCounterIdentifier(source, this.literalDomForCounterCount),
				scope.numVariables());
		this.literalDomForCounterCount++;
		scope.addVariable(literalDomCounter);
		loopVariables = initResults.third;
		location = modelFactory.location(modelFactory.sourceOfBeginning(civlForNode), scope);
		domain = this.translateExpressionNode(civlForNode.getDomain(), scope, true);
		elaborateCall = this.elaborateDomainCall(scope, domain);
		dimension = ((CIVLCompleteDomainType) domain.getExpressionType()).getDimension();
		if (dimension != loopVariables.size()) {
			throw new CIVLSyntaxException("The number of loop variables for $for does NOT match "
					+ "the dimension of the domain " + domain + ":\n" + "number of loop variables: "
					+ loopVariables.size() + "\n" + "dimension of " + domain + ": " + dimension, source);
		}
		domainGuard = modelFactory.domainGuard(modelFactory.sourceOf(domainNode), loopVariables, literalDomCounter,
				domain);
		location = modelFactory.location(modelFactory.sourceOfBeginning(civlForNode), scope);
		nextInDomain = modelFactory.civlForEnterFragment(modelFactory.sourceOfBeginning(civlForNode), location, domain,
				initResults.third, literalDomCounter);
		result = this.composeLoopFragmentWorker(scope, modelFactory.sourceOfBeginning(domainNode),
				modelFactory.sourceOfEnd(domainNode), domainGuard, nextInDomain, civlForNode.getBody(), null, false,
				loopContract);
		return new CommonFragment(elaborateCall).combineWith(result);
	}

	/**
	 * If the given CIVL expression e has array type, this returns the expression
	 * &e[0]. Otherwise returns e unchanged.<br>
	 * This method should be called on every LHS (lvalue) expression e when it is
	 * converted to a rvalue expression, except in the following cases: (1) e is the
	 * first argument to the SUBSCRIPT operator (i.e., e occurs in the context
	 * e[i]), or (2) e is the argument to the "sizeof" operator.<br>
	 * note: argument to & should never have array type.
	 * 
	 * @param array any CIVL expression e
	 * @return either the original expression or &e[0]
	 */
	protected Expression arrayToPointer(Expression array) {
		CIVLType type = array.getExpressionType();

		if (type.isArrayType()) {
			CIVLSource source = array.getSource();
			Expression zero = modelFactory.integerLiteralExpression(source, BigInteger.ZERO);
			LHSExpression subscript = modelFactory.subscriptExpression(source, (LHSExpression) array, zero);

			return modelFactory.addressOfExpression(source, subscript);
		}
		return array;
	}

	/**
	 * Sometimes an assignment is actually modeled as a fork or function call with
	 * an optional left hand side argument. Catch these cases.
	 * 
	 * @param source        the CIVL source information of the assign node
	 * @param location      The start location for this assign.
	 * @param lhs           Model expression for the left hand side of the
	 *                      assignment.
	 * @param rhsNode       AST expression for the right hand side of the
	 *                      assignment.
	 * @param isInitializer is this assignment part of a variable initializer?
	 * @param scope         The scope containing this assignment.
	 * @return The model representation of the assignment, which might also be a
	 *         fork statement or function call.
	 */
	private Fragment assignStatement(CIVLSource source, LHSExpression lhs, ExpressionNode rhsNode,
			boolean isInitializer, Scope scope) {
		Statement stmt = null;
		Location location;
		Statement assign;

		if (isCompleteMallocExpression(rhsNode)) {
			location = modelFactory.location(lhs.getSource(), scope);
			assign = mallocStatement(source, location, lhs, (CastNode) rhsNode, scope);
			return new CommonFragment(assign);
		} else if (rhsNode instanceof FunctionCallNode || rhsNode instanceof SpawnNode) {
			FunctionCallNode functionCallNode;
			boolean isCall;

			if (rhsNode instanceof FunctionCallNode) {
				functionCallNode = (FunctionCallNode) rhsNode;
				isCall = true;
			} else {
				functionCallNode = ((SpawnNode) rhsNode).getCall();
				isCall = false;
			}
			if (rhsNode.getNumConversions() > 0) {
				Fragment result;
				CIVLType type = this.translateABCType(source, scope, rhsNode.getInitialType());
				Variable tmpVar = this.modelFactory.newAnonymousVariable(source, scope, type);
				LHSExpression tmpLhs = this.modelFactory.variableExpression(source, tmpVar);
				Expression castTmp;

				// the intermediate variable "tmpVar" is just declared here, so
				// this function call must be an initializer:
				stmt = translateFunctionCall(scope, tmpLhs, functionCallNode, isCall, true, source);
				result = new CommonFragment(stmt);
				tmpLhs = this.modelFactory.variableExpression(source, tmpVar);
				castTmp = this.applyConversions(scope, functionCallNode, tmpLhs);
				assign = modelFactory.assignStatement(source, this.modelFactory.location(source, scope), lhs, castTmp,
						isInitializer);
				result.addNewStatement(assign);
				return result;
			} else {
				stmt = translateFunctionCall(scope, lhs, functionCallNode, isCall, isInitializer, source);
				return new CommonFragment(stmt);
			}

		} else {
			Expression rhs;
			CIVLType leftType;

			rhs = translateExpressionNode(rhsNode, scope, true);
			location = modelFactory.location(lhs.getSource(), scope);
			leftType = lhs.getExpressionType();
			/*
			 * When assigning a boolean to an variable with integer type, wrap an cast
			 * expression on the right hand side to explicitly cast the right hand side to
			 * an integer. We need to do that because in c, _Bool is a subtype of integer
			 * and there will be no conversion.
			 */
			if (leftType.isIntegerType() && rhs.getExpressionType().isBoolType())
				rhs = modelFactory.castExpression(rhs.getSource(), leftType, rhs);
			assign = modelFactory.assignStatement(source, location, lhs, rhs, isInitializer);
			this.normalizeAssignment((AssignStatement) assign);
			return new CommonFragment(assign);
		}
	}

	/**
	 * Translate a FunctionCall node into a call or spawn statement
	 * 
	 * @param location         The origin location for this statement. Must be
	 *                         non-null.
	 * @param scope            The scope containing this statement. Must be
	 *                         non-null.
	 * @param callNode         The ABC node representing the function called or
	 *                         spawned. Must be non-null.
	 * @param lhs              The left-hand-side expression, where the value of the
	 *                         function call or process ID resulting from the spawn
	 *                         is stored. May be null.
	 * @param isCall           True when the node is a call node, otherwise the node
	 *                         is a spawn node
	 * @param isInitialization a boolean value indicating if the return value of
	 *                         this call statement will initialize a left-hand side
	 *                         expression
	 * @return the CallOrSpawnStatement
	 */
	private CallOrSpawnStatement callOrSpawnStatement(Scope scope, Location location, FunctionCallNode callNode,
			LHSExpression lhs, List<Expression> arguments, boolean isCall, CIVLSource source,
			boolean isInitialization) {
		ExpressionNode functionExpression = ((FunctionCallNode) callNode).getFunction();
		CallOrSpawnStatement result;
		Function callee;

		if (isMallocCall(callNode))
			throw new CIVLException("$malloc can only occur in a cast expression", modelFactory.sourceOf(callNode));
		if (functionExpression instanceof IdentifierExpressionNode) {
			Entity entity = ((IdentifierExpressionNode) functionExpression).getIdentifier().getEntity();

			switch (entity.getEntityKind()) {
			case FUNCTION:
				callee = (Function) entity;
				result = modelFactory.callOrSpawnStatement(source, location, isCall, null, arguments, null,
						isInitialization);
				break;
			case VARIABLE:
				Expression function = this.translateExpressionNode(functionExpression, scope, true);

				callee = null;
				result = modelFactory.callOrSpawnStatement(source, location, isCall, function, arguments, null,
						isInitialization);
				// added function guard expression since the function could
				// be a
				// system function which has an outstanding guard, only when
				// it
				// is a call statement
				if (isCall)
					result.setGuard(modelFactory.functionGuardExpression(source, function, arguments));
				break;
			default:
				throw new CIVLUnimplementedFeatureException(
						"Function call must use identifier of variables or functions for now: "
								+ functionExpression.getSource());
			}
		} else {
			Expression function = this.translateExpressionNode(functionExpression, scope, true);

			callee = null;
			result = modelFactory.callOrSpawnStatement(source, location, isCall, function, arguments, null,
					isInitialization);
			// added function guard expression since the function could be a
			// system function which has an outstanding guard, only when it
			// is a call statement
			if (isCall)
				result.setGuard(modelFactory.functionGuardExpression(source, function, arguments));
		}
		// throw new CIVLUnimplementedFeatureException(
		// "Function call must use identifier for now: "
		// + functionExpression.getSource());
		result.setLhs(lhs);
		if (callee != null)
			modelBuilder.callStatements.put(result, callee);
		return result;
	}

	/**
	 * Composes a loop fragment.
	 * 
	 * @param loopScope       The scope of the loop
	 * @param condStartSource The beginning source of the loop condition
	 * @param condEndSource   The ending source of the loop condition
	 * @param condition       The loop condition
	 * @param bodyPrefix      The fragment before entering the loop
	 * @param loopBodyNode    The body statement node of the loop
	 * @param incrementer     The incrementer fragment of the loop
	 * @param isDoWhile       If this is a do-while loop
	 * @return
	 */
	private Fragment composeLoopFragmentWorker(Scope loopScope, CIVLSource condStartSource, CIVLSource condEndSource,
			Expression condition, Fragment bodyPrefix, StatementNode loopBodyNode, Fragment incrementer,
			boolean isDoWhile, LoopContract loopContract) {
		Set<Statement> continues, breaks, switchExits;
		Fragment loopEntrance, loopBody, loopExit, result;
		Location loopEntranceLocation, continueLocation;

		try {
			condition = modelFactory.booleanExpression(condition);
		} catch (ModelFactoryException err) {
			throw new CIVLSyntaxException("The condition of the loop statement " + condition + " is of "
					+ condition.getExpressionType() + " type which cannot be converted to boolean type.",
					condition.getSource());
		}
		loopEntranceLocation = modelFactory.location(condition.getSource(), loopScope);
		// incrementer comes after the loop body
		loopEntrance = new CommonFragment(modelFactory.loopBranchStatement(condition.getSource(), loopEntranceLocation,
				condition, true, loopContract));
		// the loop entrance location is the same as the loop exit location
		loopExit = new CommonFragment(modelFactory.loopBranchStatement(condition.getSource(), loopEntranceLocation,
				modelFactory.unaryExpression(condition.getSource(), UNARY_OPERATOR.NOT, condition), false,
				loopContract));
		functionInfo.addContinueSet(new LinkedHashSet<Statement>());
		functionInfo.addBreakSet(new LinkedHashSet<Statement>());
		loopBody = translateStatementNode(loopScope, loopBodyNode);
		if (bodyPrefix != null)
			loopBody = bodyPrefix.combineWith(loopBody);
		continues = functionInfo.popContinueStack();
		// if there is no incrementer statement, continue statements will go to
		// the loop entrance/exit location
		if (incrementer != null) {
			continueLocation = incrementer.startLocation();
		} else
			continueLocation = loopEntrance.startLocation();
		for (Statement s : continues) {
			s.setTarget(continueLocation);
		}
		// loopEntrance.startLocation().setLoopPossible(true);
		if (incrementer != null)
			loopBody = loopBody.combineWith(incrementer);
		// loop entrance comes before the loop body, P.S. loopExit is "combined"
		// implicitly because its start location is the same as loopEntrance
		loopBody = loopBody.combineWith(loopEntrance);
		// initially loop entrance comes before the loopBody. Now we'll have
		// loopBody -> loopEntrance -> loopBody and the loop is formed.
		result = loopEntrance.combineWith(loopBody);
		// for do while, mark the loopbody's start location as the start
		// location of the resulting fragment
		if (isDoWhile)
			result.setStartLocation(loopBody.startLocation());
		// break statements will go out of the loop, and thus is considered as
		// one of the last statement of the fragment
		breaks = functionInfo.popBreakStack();
		switchExits = breaks;
		switchExits.addAll(loopExit.finalStatements());
		result.setFinalStatements(switchExits);
		return result;
	}

	/**
	 * Helper method for translating for-loop and while-loop statement nodes
	 * Translate a loop structure into a fragment of CIVL statements
	 * 
	 * @param loopScope       The scope containing the loop body.
	 * @param conditionNode   The loop condition which is an expression node
	 * @param loopBodyNode    The body of the loop which is a statement node
	 * @param incrementerNode The incrementer which is an expression node, null for
	 *                        while loop
	 * @param isDoWhile       True iff the loop is a do-while loop. Always false for
	 *                        for loop and while loop.
	 * @return the fragment of the loop structure
	 */
	private Fragment composeLoopFragment(Scope loopScope, ExpressionNode conditionNode, StatementNode loopBodyNode,
			ExpressionNode incrementerNode, boolean isDoWhile, LoopContract loopContract) {
		Expression condition;
		Fragment incrementer = null;
		CIVLSource conditionStart, conditionEnd;

		if (incrementerNode != null) {
			incrementer = translateExpressionStatementNode(loopScope, incrementerNode);
		}
		if (conditionNode == null) {
			conditionStart = modelFactory.sourceOfBeginning(loopBodyNode);
			conditionEnd = modelFactory.sourceOfBeginning(loopBodyNode);
			condition = modelFactory.trueExpression(conditionStart);
		} else {
			conditionStart = modelFactory.sourceOfBeginning(conditionNode);
			conditionEnd = modelFactory.sourceOfEnd(conditionNode);
			condition = translateExpressionNode(conditionNode, loopScope, true);
		}
		return this.composeLoopFragmentWorker(loopScope, conditionStart, conditionEnd, condition, null, loopBodyNode,
				incrementer, isDoWhile, loopContract);
	}

	// how to process individual block elements?
	// int x: INTEGER or STRING -> universe.integer
	// real x: INTEGER or DOUBLE or STRING -> universe.real
	// String x: STRING
	// boolean x : BOOLEAN
	// else no can do yet
	// ["55", "55"]
	/**
	 * Translate command line constants into CIVL literal expression
	 * 
	 * @param variable The variable
	 * @param constant The constant value object
	 * @return the literal expression of the constant
	 * @throws CommandLineException
	 */
	private LiteralExpression constant(Variable variable, Object constant) throws CommandLineException {
		CIVLType type = variable.type();
		CIVLSource source = variable.getSource();

		if (type instanceof CIVLPrimitiveType) {
			PrimitiveTypeKind kind = ((CIVLPrimitiveType) type).primitiveTypeKind();

			switch (kind) {
			case BOOL:
				if (constant instanceof Boolean)
					return modelFactory.booleanLiteralExpression(source, (boolean) constant);
				else
					throw new CommandLineException(
							"Expected boolean value for variable " + variable + " but saw " + constant);
			case INT:
				if (constant instanceof BigInteger)
					return modelFactory.integerLiteralExpression(source, (BigInteger) constant);
				if (constant instanceof Integer)
					return modelFactory.integerLiteralExpression(source,
							new BigInteger(((Integer) constant).toString()));
				if (constant instanceof String)
					return modelFactory.integerLiteralExpression(source, new BigInteger((String) constant));
				else
					throw new CommandLineException(
							"Expected integer value for variable " + variable + " but saw " + constant);
			case REAL:
				if (constant instanceof Integer)
					return modelFactory.realLiteralExpression(source, new BigDecimal(((Integer) constant).toString()));
				if (constant instanceof Double)
					return modelFactory.realLiteralExpression(source, new BigDecimal(((Double) constant).toString()));
				if (constant instanceof String)
					return modelFactory.realLiteralExpression(source, new BigDecimal((String) constant));
				else
					throw new CommandLineException(
							"Expected real value for variable " + variable + " but saw " + constant);
			default:
			}
		} else {
			if (type.isArrayType()) {
				CIVLArrayType arrayType = (CIVLArrayType) type;
				CIVLType elementType = arrayType.elementType();

				if (elementType.isCharType()) {

				}

			}
		}
		throw new CIVLUnimplementedFeatureException(
				"Specification of initial value not of integer, real, or boolean type", variable);
	}

	/**
	 * Checks if a given fragment (which is to be used as the function body of some
	 * function) contains a return statement. It returns true iff all possible
	 * executions have return statements.
	 * 
	 * @param functionBody The fragment to be checked.
	 * @return True iff a return statement can be reached back from the last
	 *         statement.
	 */
	private boolean containsReturn(Fragment functionBody) {
		Set<Statement> lastStatements = functionBody.finalStatements();
		Statement uniqueLastStatement;

		if (functionBody == null || functionBody.isEmpty())
			return false;
		if (lastStatements.size() > 1) {
			for (Statement statement : lastStatements) {
				if (!(statement instanceof ReturnStatement))
					return false;
			}
			return true;
		}
		uniqueLastStatement = functionBody.uniqueFinalStatement();
		if (uniqueLastStatement.source().getNumOutgoing() == 1) {
			Location lastLocation = uniqueLastStatement.source();
			Set<Integer> locationIds = new HashSet<Integer>();

			while (lastLocation.atomicKind() == AtomicKind.ATOMIC_EXIT) {
				locationIds.add(lastLocation.id());
				if (lastLocation.getNumIncoming() == 1) {
					lastLocation = lastLocation.getIncoming(0).source();
					if (locationIds.contains(lastLocation.id()))
						return false;
				} else {
					return false;
				}
			}
			if (lastLocation.getNumOutgoing() == 1 && lastLocation.getOutgoing(0) instanceof ReturnStatement) {
				return true;
			}
		}
		return false;
	}

	/**
	 * @param fileName The name of a certain file
	 * @return File name without extension
	 */
	private String fileNameWithoutExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		String libName;

		libName = fileName.substring(0, dotIndex);
		return libName;
	}

	/**
	 * Is the ABC expression node an expression of the form
	 * <code>(t)$malloc(...)</code>? I.e., a cast expression for which the argument
	 * is a malloc call?
	 * 
	 * @param node an expression node
	 * @return true iff this is a cast of a malloc call
	 */
	private boolean isCompleteMallocExpression(ExpressionNode node) {
		if (node instanceof CastNode) {
			ExpressionNode argumentNode = ((CastNode) node).getArgument();

			return isMallocCall(argumentNode);
		}
		return false;
	}

	/**
	 * Is the ABC expression node a call to the function "$malloc"?
	 * 
	 * @param node The expression node to be checked.
	 * @return true iff node is a function call to node to a function named
	 *         "$malloc"
	 */
	private boolean isMallocCall(ExpressionNode node) {
		if (node instanceof FunctionCallNode) {
			ExpressionNode functionNode = ((FunctionCallNode) node).getFunction();

			if (functionNode instanceof IdentifierExpressionNode) {
				String functionName = ((IdentifierExpressionNode) functionNode).getIdentifier().name();

				if ("$malloc".equals(functionName) || "malloc".equals(functionName))
					return true;
			}
		}
		return false;
	}

	/**
	 * Translate a cast node into a malloc statement
	 * 
	 * @param source   The CIVL source
	 * @param location The location
	 * @param lhs      The left-hand-side expression
	 * @param castNode The node of the malloc statement
	 * @param scope    The scope
	 * @return the malloc statement
	 */
	private MallocStatement mallocStatement(CIVLSource source, Location location, LHSExpression lhs, CastNode castNode,
			Scope scope) {
		TypeNode typeNode = castNode.getCastType();
		CIVLType pointerType = translateABCType(modelFactory.sourceOf(typeNode), scope, typeNode.getType());
		FunctionCallNode callNode = (FunctionCallNode) castNode.getArgument();
		int mallocId = modelBuilder.mallocStatements.size();
		Expression scopeExpression;
		Expression sizeExpression;
		CIVLType elementType;
		MallocStatement result;

		if (!pointerType.isPointerType())
			throw new CIVLException("result of $malloc/malloc not cast to pointer type", source);
		elementType = ((CIVLPointerType) pointerType).baseType();
		if (elementType.isVoidType()) {
			throw new CIVLSyntaxException("missing cast to non-void pointer type around malloc expression: "
					+ "CIVL-C requires that malloc expressions be enclosed in a cast to a pointer to a non-void type, "
					+ "such as (double*)$malloc($here, n*sizeof(double))", source);
		}
		if (callNode.getNumberOfArguments() == 1)
			throw new CIVLInternalException("$malloc only has one argument. Transformers are responsible to cover this",
					source);
		scopeExpression = translateExpressionNode(callNode.getArgument(0), scope, true);
		sizeExpression = translateExpressionNode(callNode.getArgument(1), scope, true);
		result = modelFactory.mallocStatement(source, location, lhs, elementType, scopeExpression, sizeExpression,
				mallocId, null);
		modelBuilder.mallocStatements.add(result);
		return result;
	}

	private void normalizeAssignment(AssignStatement assign) {
		LHSExpression lhs = assign.getLhs();
		Expression rhs = assign.rhs();

		if (rhs instanceof BinaryExpression) {
			BinaryExpression binary = (BinaryExpression) rhs;
			Expression leftOperand = binary.left(), rightOperand = binary.right();

			if (leftOperand.equals(lhs))
				binary.setAssignToLeft(true);
			else if (rightOperand.equals(lhs)) {
				binary.setAssignToLeft(binary.switchOperands());
			}
		}
	}

	/**
	 * Sometimes an assignment is actually modeled as a spawn or function call with
	 * an optional left hand side argument. Catch these cases.
	 * 
	 * Precondition: assignNode.getOperator() == ASSIGN;
	 * 
	 * @param assignNode The assign node to be translated.
	 * @param scope      The scope containing this assignment.
	 * @return The model representation of the assignment, which might also be a
	 *         fork statement or function call.
	 */
	private Fragment translateAssignNode(Scope scope, OperatorNode assignNode) {
		ExpressionNode lhs = assignNode.getArgument(0);
		ExpressionNode rhs = assignNode.getArgument(1);
		Expression leftExpression;

		leftExpression = translateExpressionNode(lhs, scope, true);
		assert assignNode.getOperator() == Operator.ASSIGN;
		if (!(leftExpression instanceof LHSExpression))
			throw new CIVLInternalException("expected LHS expression, not " + leftExpression,
					modelFactory.sourceOf(lhs));
		if (leftExpression instanceof VariableExpression) {
			Variable lhsVariable = ((VariableExpression) leftExpression).variable();

			if (lhsVariable.isInput())
				throw new CIVLSyntaxException("attempt to modify the input variable " + leftExpression,
						modelFactory.sourceOf(lhs));
			if (lhsVariable.isConst())
				throw new CIVLSyntaxException("attempt to modify the constant variable " + leftExpression,
						modelFactory.sourceOf(lhs));
		}

		return assignStatement(modelFactory.sourceOfSpan(lhs, rhs), (LHSExpression) leftExpression, rhs, false, scope);
	}

	// /**
	// * Translate an assume node into a fragment of CIVL statements
	// *
	// * @param scope
	// * The scope containing this statement.
	// * @param assumeNode
	// * The assume node to be translated.
	// * @return the fragment
	// */
	// private Fragment translateAssumeNode(Scope scope, AssumeNode assumeNode)
	// {
	// Expression expression;
	// Location location;
	// Fragment result;
	//
	// expression = translateExpressionNode(assumeNode.getExpression(), scope,
	// true);
	// location = modelFactory.location(
	// modelFactory.sourceOfBeginning(assumeNode), scope);
	// result = modelFactory.assumeFragment(modelFactory.sourceOf(assumeNode),
	// location, expression);
	// result = result.combineWith(accuracyAssumptionBuilder
	// .accuracyAssumptions(expression, scope));
	// return result;
	// }

	// /**
	// *
	// * Translate an assert node into a fragment of CIVL statements
	// *
	// * @param scope
	// * The scope containing this statement.
	// * @param assertNode
	// * The assert node to be translated.
	// * @return the result fragment
	// */
	// private Fragment translateAssertNode(Scope scope, AssertNode assertNode)
	// {
	// Expression expression;
	// Location location;
	// Fragment result;
	// Expression[] explanation = null;
	// SequenceNode<ExpressionNode> explanationNode = assertNode
	// .getExplanation();
	//
	// expression = translateExpressionNode(assertNode.getCondition(), scope,
	// true);
	// try {
	// expression = modelFactory.booleanExpression(expression);
	// } catch (ModelFactoryException e) {
	// throw new CIVLSyntaxException(
	// "The expression of the $assert statement "
	// + expression
	// + " is of "
	// + expression.getExpressionType()
	// + " type which cannot be converted to boolean type.",
	// assertNode.getSource());
	// }
	// location = modelFactory.location(
	// modelFactory.sourceOfBeginning(assertNode), scope);
	// if (explanationNode != null) {
	// int numArgs = explanationNode.numChildren();
	// List<Expression> args = new ArrayList<>(numArgs);
	//
	// explanation = new Expression[numArgs];
	// for (int i = 0; i < numArgs; i++) {
	// Expression arg = translateExpressionNode(
	// explanationNode.getSequenceChild(i), scope, true);
	//
	// arg = this.arrayToPointer(arg);
	// args.add(arg);
	// }
	// args.toArray(explanation);
	// }
	// result = modelFactory.assertFragment(modelFactory.sourceOf(assertNode),
	// location, expression, explanation);
	// return result;
	// }

	/**
	 * @param node     The AST node
	 * @param scope    The scope
	 * @param location The location
	 * @return The fragment of statements translated from the AST node
	 */
	private Fragment translateASTNode(ASTNode node, Scope scope, Location location) {
		Fragment result = null;

		switch (node.nodeKind()) {
		case VARIABLE_DECLARATION:
			try {
				result = translateVariableDeclarationNode(location, scope, (VariableDeclarationNode) node);
				if (!modelFactory.anonFragment().isEmpty()) {
					result = modelFactory.anonFragment().combineWith(result);
					modelFactory.clearAnonFragment();
				}
			} catch (CommandLineException e) {
				throw new CIVLInternalException("Saw input variable outside of root scope",
						modelFactory.sourceOf(node));
			}
			break;
		case PRAGMA:// ignored pragma
			result = new CommonFragment();
			break;
		case TYPEDEF:
			// TypedefDeclarationNode node has to be processed separately
			// from
			// StructureOrUnionTypeNode, because TypedefDeclarationNode is
			// not a
			// sub-type of TypeNode but the one returned by
			// TypedefDeclarationNode.getTypeNode() is.
			result = translateCompoundTypeNode(location, scope, ((TypedefDeclarationNode) node).getTypeNode());
			break;
		case FUNCTION_DEFINITION:
			FunctionDefinitionNode functionDefinitionNode = (FunctionDefinitionNode) node;
			if (functionDefinitionNode.getName().equals("main")) {
				// TODO arguments to main() become arguments to the system
				// function; specified by command line, after the .cvl file
				// name; think about how to initialize them.
				modelBuilder.mainFunctionNode = functionDefinitionNode;
			} else
				translateFunctionDeclarationNode(functionDefinitionNode, scope);
			break;
		case FUNCTION_DECLARATION:
			result = translateFunctionDeclarationNode((FunctionDeclarationNode) node, scope);
			break;
		case STATEMENT:
			result = translateStatementNode(scope, (StatementNode) node);
			break;
		case TYPE:
			TypeNode typeNode = (TypeNode) node;

			switch (typeNode.kind()) {
			case STRUCTURE_OR_UNION:
			case ENUMERATION:
				result = translateCompoundTypeNode(location, scope, (TypeNode) node);
				return result;
			default:
			}
			// if not structure or union type or enumeration type, then
			// execute
			// default
			// case to throw an exception
		default:
			if (scope.id() == modelBuilder.rootScope.id())
				throw new CIVLInternalException("Unsupported declaration type", modelFactory.sourceOf(node));
			else
				throw new CIVLUnimplementedFeatureException("Unsupported block element: " + node.prettyRepresentation(),
						modelFactory.sourceOf(node));
		}
		return result;
	}

	private CIVLType translateABCEnumerationType(CIVLSource source, Scope scope, EnumerationType enumType) {
		String name = enumType.getTag();
		int numOfEnumerators = enumType.getNumEnumerators();
		BigInteger currentValue = BigInteger.ZERO;
		Map<String, BigInteger> valueMap = new LinkedHashMap<>(numOfEnumerators);

		if (name == null) {
			throw new CIVLInternalException("Anonymous enum encountered, which should already " + "been handled by ABC",
					source);
		}
		for (Enumerator enumerator : enumType.getEnumerators()) {
			String member = enumerator.getName();
			Value abcValue = enumerator.getValue();
			BigInteger value;

			if (abcValue != null) {
				if (abcValue instanceof IntegerValue) {
					value = ((IntegerValue) abcValue).getIntegerValue();
				} else if (abcValue instanceof CharacterValue) {
					value = BigInteger.valueOf(((CharacterValue) abcValue).getCharacter().getCharacters()[0]);
				} else
					throw new CIVLSyntaxException("Only integer or char constant can be used in enumerators.", source);
			} else {
				value = currentValue;
			}
			valueMap.put(member, value);
			currentValue = value.add(BigInteger.ONE);
		}
		return typeFactory.enumType(name, valueMap);
	}

	/**
	 * Translate an ABC AtomicNode, which represents an $atomic block
	 * 
	 * @param scope
	 * @param statementNode
	 * @return
	 */
	private Fragment translateAtomicNode(Scope scope, AtomicNode atomicNode) {
		StatementNode bodyNode = atomicNode.getBody();
		Fragment bodyFragment;
		Location start = modelFactory.location(modelFactory.sourceOfBeginning(atomicNode), scope);
		Location end = modelFactory.location(modelFactory.sourceOfEnd(atomicNode), scope);
		Location firstStmtLoc, atomicEnterLoc;
		Iterator<Statement> firstStmtsIter;
		Expression guard = null;

		this.atomicCount++;
		bodyFragment = translateStatementNode(scope, bodyNode);
		firstStmtLoc = bodyFragment.startLocation();
		// translate of the first statement guard:
		// stackTopLoc = modelBuilder.peekChooseGuardLocaton();
		// if (stackTopLoc != null && stackTopLoc.id() == firstStmtLoc.id()) {
		// assert firstStmtLoc.getNumOutgoing() == 1;
		// guard = modelBuilder.popChooseGuard();
		// modelBuilder.clearChooseGuard();
		// } else {
		firstStmtsIter = firstStmtLoc.outgoing().iterator();
		while (firstStmtsIter.hasNext()) {
			Statement currStmt = firstStmtsIter.next();

			guard = (guard == null) ? currStmt.guard()
					: modelFactory.binaryExpression(currStmt.getSource(), BINARY_OPERATOR.AND, guard, currStmt.guard());
		}
		// }
		this.atomicCount--;
		bodyFragment = modelFactory.atomicFragment(bodyFragment, start, end);
		atomicEnterLoc = bodyFragment.startLocation();

		Location atomicBlockModel[] = { atomicEnterLoc, end };

		// Let the ModelBuilderWorker collect the atomic block :
		modelBuilder.atomicBlocks.add(atomicBlockModel);
		assert atomicEnterLoc.getNumOutgoing() == 1
				: "ENTER_ATOMIC location " + "should only have exactly one outgoing statement.";
		assert guard != null;
		atomicEnterLoc.getSoleOutgoing().setGuard(guard);
		return bodyFragment;
	}

	/**
	 * <p>
	 * tests if a {@link CIVLFunction} is one of the system functions:
	 * <code>$local_start()</code> and <code>$local_end()</code>.
	 * </p>
	 *
	 * @param func an instance of {@link CIVLFunction}
	 * @return true iff the given function is either <code>$local_start()</code> and
	 *         <code>$local_end()</code>
	 */
	private boolean isLocalBlockEnterOrExit(CIVLFunction func) {
		String name = func.name().name();

		return name.equals("$local_start") || name.equals("$local_end");
	}

	/**
	 * <p>
	 * Translates system function call <code>$local_start()</code> to ATOMIC_ENTER
	 * and marks the location as {@link Location#isEntryOfLocalBlock()}; Translates
	 * system function call <code>$local_end()</code> to ATOMIC_EXIT.
	 * </p>
	 *
	 * <p>
	 * The translation is based on the fact that the only difference between local
	 * block and atomic block is that entering a local block is a purely local
	 * action while whether entering an atomic block is purely local depends on the
	 * body of the atomic block. Therefore, the location associated with the
	 * <code>$local_start</code> will be labeled as "isEntryOfLocalBlock". During
	 * verification, the AmpleSetWorker can use the label on the location to make
	 * the local block-entering transition an ample set.
	 * </p>
	 *
	 * @param scope    the scope where the given call node is in
	 * @param function the {@link CIVLFunction} called by the call node
	 * @param callNode a {@link FunctionCallNode} to the specific system function
	 * @return the translated {@link Fragment} which contains either an ATOMIC_ENTER
	 *         or ATOMIC_EXIT action.
	 */
	private Statement translateLocalBlockEnterOrExit(Scope scope, CIVLFunction function, FunctionCallNode callNode) {
		CIVLSource nodeSource = modelFactory.sourceOfBeginning(callNode);
		Location location = modelFactory.location(nodeSource, scope);
		String name = function.name().name();

		if (civlConfig.disableLocalBlock())
			return modelFactory.noopStatementTemporary(nodeSource, location);
		if (name.equals("$local_start")) {
			location.setIsEntryOfLocalBlock(true);
			return modelFactory.atomicEnter(location);
		} else
			return modelFactory.atomicExit(location);
	}

	/**
	 * Translate a choose node into a fragment that has multiple outgoing statements
	 * from its start location
	 * 
	 * @param scope               The scope
	 * @param chooseStatementNode The choose statement node
	 * @return the fragment of the choose statements
	 */
	private Fragment translateChooseNode(Scope scope, ChooseStatementNode chooseStatementNode) {
		CIVLSource startSource = modelFactory.sourceOfBeginning(chooseStatementNode);
		Location startLocation = modelFactory.location(startSource, scope);
		int defaultOffset = 0;
		Fragment result = new CommonFragment();
		Expression defaultGuard = null; // guard of default cqse
		Expression wholeGuard = null; // guard of whole statement
		NoopStatement insertedNoop;

		if (chooseStatementNode.getDefaultCase() != null) {
			defaultOffset = 1;
		}
		result.setStartLocation(startLocation);
		for (int i = 0; i < chooseStatementNode.numChildren() - defaultOffset; i++) {
			StatementNode childNode = chooseStatementNode.getSequenceChild(i);
			Fragment caseFragment = translateStatementNode(scope, childNode);
			Expression caseGuard;

			if (this.containsHereConstant(caseFragment.startLocation())) {
				throw new CIVLSyntaxException(
						"the first (recursively) primitive statement " + "of a clause of $choose should not use $here",
						caseFragment.startLocation().getSource());
			}
			caseGuard = this.factorOutGuards(caseFragment.startLocation());
			caseFragment.updateStartLocation(startLocation);
			result.addFinalStatementSet(caseFragment.finalStatements());
			wholeGuard = this.disjunction(wholeGuard, caseGuard);
		}
		if (!modelFactory.isTrue(wholeGuard)) {
			if (chooseStatementNode.getDefaultCase() != null) {
				Fragment defaultFragment = translateStatementNode(scope, chooseStatementNode.getDefaultCase());

				if (this.containsHereConstant(defaultFragment.startLocation())) {
					throw new CIVLSyntaxException(
							"the first (recursively) primitive statement "
									+ "of a clause of $choose should not use $here",
							defaultFragment.startLocation().getSource());
				}
				defaultGuard = modelFactory.unaryExpression(wholeGuard.getSource(), UNARY_OPERATOR.NOT, wholeGuard);
				defaultFragment.addGuardToStartLocation(defaultGuard, modelFactory);
				defaultFragment.updateStartLocation(startLocation);
				result.addFinalStatementSet(defaultFragment.finalStatements());
				wholeGuard = modelFactory.trueExpression(startSource);
				startLocation.setSwitchOrChooseWithDefault();
			}
		} else
			startLocation.setSwitchOrChooseWithDefault();
		assert wholeGuard != null;
		// insert noop at the beginning the fragment so that the guard of the
		// start location will be true;
		result = insertNoopAtBeginning(startSource, scope, result);
		result.startLocation().getSoleOutgoing().setGuard(wholeGuard);
		insertedNoop = (NoopStatement) result.startLocation().getSoleOutgoing();
		insertedNoop.setRemovable();
		return result;
	}

	private Fragment insertNoopAtBeginning(CIVLSource source, Scope scope, Fragment old) {
		Location start = modelFactory.location(source, scope);
		NoopStatement noop;
		Fragment noopFragment;

		noop = modelFactory.noopStatementTemporary(source, start);
		noopFragment = new CommonFragment(noop);
		return noopFragment.combineWith(old);
	}

	/**
	 * checks if any outgoing statement of the given location uses the $here
	 * constant.
	 * 
	 * @param location
	 * @return
	 */
	private boolean containsHereConstant(Location location) {
		for (Statement stmt : location.outgoing()) {
			if (stmt.containsHere())
				return true;
		}
		return false;
	}

	/**
	 * factors out the guards of the outgoing statements of a location in
	 * disjunction.
	 * 
	 * For example, if the location has two outgoing statements: [x>2] s1; [x<6] s2;
	 * then the result is (x>2 || x<6).
	 * 
	 * If the location has exactly one outgoing statement: [x<10] s; then the result
	 * is (x<10).
	 * 
	 * This method serves as a helper function for $choose.
	 * 
	 * @param location
	 * @return
	 */
	private Expression factorOutGuards(Location location) {
		Expression guard = null;
		Iterator<Statement> iter = location.outgoing().iterator();

		while (iter.hasNext()) {
			Expression statementGuard = iter.next().guard();

			guard = this.disjunction(guard, statementGuard);
		}
		return guard;
	}

	/**
	 * Computes the disjunction of two boolean expressions. The left could be NULL
	 * but the right couldn't.
	 * 
	 * @param left
	 * @param right
	 * @return
	 */
	private Expression disjunction(Expression left, Expression right) {
		if (left == null)
			return right;
		if (modelFactory.isTrue(left))
			return left;
		if (modelFactory.isTrue(right))
			return right;
		return modelFactory.binaryExpression(modelFactory.sourceOfSpan(left.getSource(), right.getSource()),
				BINARY_OPERATOR.OR, left, right);
	}

	/**
	 * Translates a compound statement.
	 * <p>
	 * Tagged entities can have state and require special handling.
	 * <p>
	 * When perusing compound statements or external defs, when you come across a
	 * typedef, or complete struct or union def, we might need to create a variable
	 * if the type has some state, as
	 * {@link ModelBuilderWorker#translateCompoundTypeNode}.
	 * <p>
	 * when processing a variable decl: if variable has compound type (array or
	 * struct), insert statement (into beginning of current compound statement)
	 * saying "v = InitialValue[v]". then insert the variable's initializer if
	 * present.
	 * 
	 * @param scope         The scope that contains this compound node
	 * @param statementNode The compound statement node
	 * @return the fragment of the compound statement node
	 */
	private Fragment translateCompoundStatementNode(Scope scope, CompoundStatementNode statementNode) {
		Scope newScope;
		Location location;
		// indicates whether the location field has been used:
		boolean usedLocation = false;
		Fragment result = new CommonFragment();
		boolean newScopeNeeded = this.needsNewScope(statementNode);

		// // In order to eliminate unnecessary scopes, do this loop twice.
		// // The first time, just check if there are any declarations. If there
		// // are, create newScope as usual. Otherwise, let newScope = scope.
		if (newScopeNeeded)
			newScope = modelFactory.scope(modelFactory.sourceOf(statementNode), scope, new ArrayList<>(0),
					functionInfo.function());
		else
			newScope = scope;
		location = modelFactory.location(modelFactory.sourceOfBeginning(statementNode), newScope);
		for (int i = 0; i < statementNode.numChildren(); i++) {
			BlockItemNode node = statementNode.getSequenceChild(i);

			if (node == null)
				continue;

			Fragment fragment = translateASTNode(node, newScope, usedLocation ? null : location);

			if (fragment != null) {
				usedLocation = true;
				result = result.combineWith(fragment);
			}
		}
		if (result.isEmpty())
			result = new CommonFragment(
					modelFactory.noopStatement(modelFactory.sourceOf(statementNode), location, null));
		return result;
	}

	/**
	 * Checks if an AST node contains any $here node in a certain scope.
	 * 
	 * In order to eliminate unnecessary scopes, do this loop twice. The first time,
	 * just check if there are any declarations. If there are, create newScope as
	 * usual. Otherwise, let newScope = scope.
	 * 
	 * @param scope    The scope to be checked.
	 * @param compound The AST node to be checked.
	 * @return True iff a $here node exists in the AST node and is in the given
	 *         scope.
	 */
	private boolean needsNewScope(CompoundStatementNode compound) {
		int numChildren = compound.numChildren();

		for (int i = 0; i < numChildren; i++) {
			BlockItemNode blockItem = compound.getSequenceChild(i);

			if (blockItem instanceof VariableDeclarationNode || blockItem instanceof FunctionDeclarationNode) {
				return true;
			}
			if (blockItem instanceof CompoundStatementNode)
				continue;
			if (blockItem instanceof LabeledStatementNode) {
				StatementNode labeledStatementNode = ((LabeledStatementNode) blockItem).getStatement();
				if (labeledStatementNode instanceof VariableDeclarationNode) {
					return true;
				}
			}
			if (hasHereNodeWork(blockItem))
				return true;
		}
		return false;
	}

	// private boolean containsHereNodeInFirstPrimitiveStatement(
	// StatementNode statementNode) {
	// if (statementNode instanceof CompoundStatementNode) {
	// CompoundStatementNode compound = (CompoundStatementNode) statementNode;
	// int numChildren = compound.numChildren();
	// StatementNode first = null;
	//
	// for (int i = 0; i < numChildren; i++) {
	// BlockItemNode child = compound.getSequenceChild(i);
	//
	// if (child == null)
	// continue;
	// if (child instanceof VariableDeclarationNode)
	// return false;
	// if (!(child instanceof StatementNode))
	// continue;
	// return containsHereNodeInFirstPrimitiveStatement((StatementNode) child);
	// }
	// return false;
	// } else if(statementNode instanceof IfNode) {
	// IfNode if
	// }
	// }

	private boolean hasHereNodeWork(ASTNode node) {
		if (isHereNode(node)) {
			return true;
		}

		int numChildren = node.numChildren();

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = node.child(i);

			if (child == null)
				continue;
			if (child instanceof CompoundStatementNode)
				continue;
			if (hasHereNodeWork(child))
				return true;
		}
		return false;
	}

	private boolean isHereNode(ASTNode node) {
		if (node instanceof HereOrRootNode) {
			return ((HereOrRootNode) node).isHereNode();
		}
		return false;
	}

	/**
	 * Takes an expression statement and converts it to a fragment of that
	 * statement. For spawn, assign, function call, increment and decrement, they
	 * are translated into spawn or call statement, and assignment, respectively.
	 * Any other expressions are translated into a noop statement, and the original
	 * expression becomes one field of the noop statement, which will be evaluated
	 * when executing the noop, and the result of evaluating the expression is
	 * discarded but any side effect error during evaluation will be reported, like
	 * array index out of bound, division by zero, etc.
	 * 
	 * @param scope          The scope containing this statement.
	 * @param expressionNode The expression node to be translated.
	 * @return the fragment representing the expression node.
	 */
	private Fragment translateExpressionStatementNode(Scope scope, ExpressionNode expressionNode) {
		Fragment result;
		Location location = modelFactory.location(modelFactory.sourceOfBeginning(expressionNode), scope);

		switch (expressionNode.expressionKind()) {
		// case CAST: {
		// CastNode castNode = (CastNode) expressionNode;
		// CIVLType castType = translateABCType(
		// modelFactory.sourceOf(castNode.getCastType()), scope,
		// castNode.getCastType().getType());
		//
		// if (castType.isVoidType()) {
		// Statement noopStatement = modelFactory.noopStatement(
		// modelFactory.sourceOf(castNode), location);
		//
		// result = new CommonFragment(noopStatement);
		// } else
		// throw new CIVLUnimplementedFeatureException(
		// "expression statement of a cast expression with the cast type "
		// + castType,
		// modelFactory.sourceOf(expressionNode));
		// break;
		// }
		case OPERATOR: {
			OperatorNode operatorNode = (OperatorNode) expressionNode;

			switch (operatorNode.getOperator()) {
			case ASSIGN:
				result = translateAssignNode(scope, operatorNode);
				break;
			case COMMA: {
				int number = operatorNode.getNumberOfArguments();
				result = new CommonFragment();

				for (int i = 0; i < number; i++) {
					ExpressionNode argument = operatorNode.getArgument(i);
					Fragment current = this.translateExpressionStatementNode(scope, argument);

					result = result.combineWith(current);
				}
				break;
			}
			case POSTINCREMENT:
			case PREINCREMENT:
			case POSTDECREMENT:
			case PREDECREMENT:
				throw new CIVLInternalException("Side-effect not removed: ", modelFactory.sourceOf(operatorNode));
			default: {// since side-effects have been removed,
				// the only expressions remaining with
				// side-effects
				// are assignments. all others are equivalent to
				// no-op
				Expression expression = this.translateExpressionNode(expressionNode, scope, true);
				Statement noopStatement = modelFactory.noopStatement(modelFactory.sourceOf(operatorNode), location,
						expression);

				result = new CommonFragment(noopStatement);
			}
			}
			break;
		}
		case SPAWN:
			result = translateSpawnNode(scope, (SpawnNode) expressionNode);
			break;
		case FUNCTION_CALL:
			result = translateFunctionCallNodeAsExpressionWithnoLHS(scope, (FunctionCallNode) expressionNode,
					modelFactory.sourceOf(expressionNode));
			break;
		case CONSTANT:
			// case IDENTIFIER_EXPRESSION: {
			// Statement noopStatement = modelFactory.noopStatement(
			// modelFactory.sourceOf(expressionNode), location, null);
			//
			// result = new CommonFragment(noopStatement);
			// }
			// break;
		default: {
			Expression expression = this.translateExpressionNode(expressionNode, scope, true);
			Statement noopStatement = modelFactory.noopStatement(modelFactory.sourceOf(expressionNode), location,
					expression);

			result = new CommonFragment(noopStatement);
			// throw new CIVLUnimplementedFeatureException(
			// "expression statement of this kind "
			// + expressionNode.expressionKind(),
			// modelFactory.sourceOf(expressionNode));
		}
		}
		return result;
	}

	/**
	 * Translate a for loop node into a fragment. A for loop has the form
	 * <code> for (init; cond; inc) stmt </code>, where <code>init</code> is a
	 * {@link ForLoopInitializerNode} which either is a variable declaration list or
	 * an expression (the expression could be a comma expression, like
	 * <code>int i = 0, j = 0</code>), <code>cond</code> is a boolean expression
	 * which is side-effect-free, and <code>inc</code> is an expression (also could
	 * be a comma expression, like <code>i=i+1,j=j+1</code>). All side effects
	 * except assignments should have been removed already.
	 * 
	 * @param scope       The scope
	 * @param forLoopNode The for loop node
	 * @return the fragment representing the for loop
	 */
	private Fragment translateForLoopNode(Scope scope, ForLoopNode forLoopNode, LoopContract loopContract) {
		ForLoopInitializerNode initNode = forLoopNode.getInitializer();
		Fragment initFragment = new CommonFragment();
		Fragment result;

		// If the initNode does not have a declaration, don't create a new
		// scope.
		if (initNode != null) {
			Triple<Scope, Fragment, List<Variable>> initData = translateForLoopInitializerNode(scope, initNode);

			scope = initData.first;
			initFragment = initData.second;
		}
		result = composeLoopFragment(scope, forLoopNode.getCondition(), forLoopNode.getBody(),
				forLoopNode.getIncrementer(), false, loopContract);
		result = initFragment.combineWith(result);
		return result;
	}

	private Triple<Scope, Fragment, List<Variable>> translateForLoopInitializerNode(Scope scope,
			ForLoopInitializerNode initNode) {
		Location location;
		Fragment initFragment = new CommonFragment();
		Scope newScope = scope;
		List<Variable> variables = new ArrayList<>();

		switch (initNode.nodeKind()) {
		case EXPRESSION:
			ExpressionNode initExpression = (ExpressionNode) initNode;

			location = modelFactory.location(modelFactory.sourceOfBeginning(initNode), newScope);
			initFragment = translateExpressionStatementNode(newScope, initExpression);
			break;
		case DECLARATION_LIST:
			newScope = modelFactory.scope(modelFactory.sourceOf(initNode), newScope, new ArrayList<>(0),
					functionInfo.function());
			for (int i = 0; i < ((DeclarationListNode) initNode).numChildren(); i++) {
				VariableDeclarationNode declaration = ((DeclarationListNode) initNode).getSequenceChild(i);

				if (declaration == null)
					continue;

				Variable variable = translateVariableDeclarationNode(declaration, newScope).left;
				Fragment fragment;

				variables.add(variable);
				location = modelFactory.location(modelFactory.sourceOfBeginning(initNode), newScope);
				fragment = translateVariableInitializationNode(declaration, variable, location, newScope);
				initFragment = initFragment.combineWith(fragment);
			}
			break;
		default:
			throw new CIVLInternalException("A for loop initializer must be an expression or a declaration list.",
					modelFactory.sourceOf(initNode));
		}
		return new Triple<>(newScope, initFragment, variables);
	}

	protected Pair<Function, CIVLFunction> getFunction(IdentifierExpressionNode ident) {
		Entity entity = ident.getIdentifier().getEntity();

		if (entity.getEntityKind() == EntityKind.FUNCTION) {
			Function function = (Function) entity;

			return new Pair<>(function, modelBuilder.functionMap.get(function));
		}
		return new Pair<>(null, null);
	}

	/**
	 * Translate a function call node into a fragment containing the call statement.
	 * 
	 * @param scope            The scope
	 * @param functionCallNode The function call node
	 * @param isInitialization a boolean value indicating if the returned value of
	 *                         this call statement will initialize a left-hand side
	 *                         expression
	 * @return the translated statement
	 */
	private Statement translateFunctionCall(Scope scope, LHSExpression lhs, FunctionCallNode functionCallNode,
			boolean isCall, boolean isInitialization, CIVLSource source) {
		ArrayList<Expression> arguments = new ArrayList<Expression>();
		Location location;
		CIVLFunction civlFunction = null;
		ExpressionNode functionExpression = functionCallNode.getFunction();
		CallOrSpawnStatement callStmt;
		Statement result;
		CIVLFunctionType functionType = null;
		CIVLType[] types = null;
		int typesLen = 0;
		int numOfArgs = functionCallNode.getNumberOfArguments();
		// For $print_helper we do not apply conversions to arguments
		boolean isPrintHelper = false;

		if (functionExpression instanceof IdentifierExpressionNode) {
			civlFunction = getFunction((IdentifierExpressionNode) functionExpression).right;
			isPrintHelper = civlFunction != null && civlFunction.name().name().equals("$print_helper");
		}
		if (civlFunction != null) {
			// for $local_start or $local_end functions, translate them to
			// ATOMIC_ENTER and ATOMIC_END
			if (isLocalBlockEnterOrExit(civlFunction))
				return translateLocalBlockEnterOrExit(scope, civlFunction, functionCallNode);
			functionType = civlFunction.functionType();
			types = functionType.parameterTypes();
			typesLen = types.length;
		}
		for (int i = 0; i < numOfArgs; i++) {
			Expression actual = translateExpressionNode(functionCallNode.getArgument(i), scope,
					i == 0 || !isPrintHelper);

			/*
			 * for each actual argument of a function call, if the formal type is integer
			 * but the actual type is a boolean, we need to add a cast expression to cast
			 * the boolean into an integer.
			 */
			if (i < typesLen) {
				if (types[i].isIntegerType() && actual.getExpressionType().isBoolType())
					actual = modelFactory.castExpression(actual.getSource(), typeFactory.integerType(), actual);
			}
			if (i == 0 || !isPrintHelper) {
				actual = arrayToPointer(actual);
			}
			arguments.add(actual);
		}
		location = modelFactory.location(modelFactory.sourceOfBeginning(functionCallNode), scope);
		if (civlFunction != null) {
			String functionName = civlFunction.name().name();

			if (functionName.equals("$quotient") || functionName.equals("$remainder")) {
				assert arguments.size() == 2;

				BINARY_OPERATOR op = functionName.equals("$quotient") ? BINARY_OPERATOR.DIVIDE : BINARY_OPERATOR.MODULO;
				Expression binary = modelFactory.binaryExpression(source, op, arguments.get(0), arguments.get(1));

				if (lhs != null)
					result = modelFactory.assignStatement(source, location, lhs, binary, isInitialization);
				else
					result = modelFactory.noopStatement(source, location, binary);
			} else {
				if (civlFunction.isAbstractFunction()) {
					Expression abstractFunctionCall = modelFactory.abstractFunctionCallExpression(
							modelFactory.sourceOf(functionCallNode), (AbstractFunction) civlFunction, arguments);

					if (lhs != null)
						result = modelFactory.assignStatement(source, location, lhs, abstractFunctionCall,
								isInitialization);
					else
						// An abstract function call without left-hand side
						// expression is just a no-op:
						result = modelFactory.noopStatement(source, location, abstractFunctionCall);
					return result;
				}
				callStmt = callOrSpawnStatement(scope, location, functionCallNode, lhs, arguments, isCall, source,
						isInitialization);
				callStmt.setFunction(modelFactory.functionIdentifierExpression(civlFunction.getSource(), civlFunction));
				if (callStmt.isSystemCall())
					callStmt.setGuard(modelFactory.systemGuardExpression(callStmt));
				result = callStmt;
			}
		} else
			// call on a function pointer
			result = callOrSpawnStatement(scope, location, functionCallNode, lhs, arguments, isCall, source,
					isInitialization);
		return result;
	}

	/**
	 * Translate a function call node into a fragment containing the call statement
	 * 
	 * @param scope            The scope
	 * @param functionCallNode The function call node
	 * @return the fragment containing the function call statement
	 */
	private Fragment translateFunctionCallNodeAsExpressionWithnoLHS(Scope scope, FunctionCallNode functionCallNode,
			CIVLSource source) {
		Statement functionCall = translateFunctionCall(scope, null, functionCallNode, true, false, source);

		return new CommonFragment(functionCall);
	}

	/**
	 * Processes a function declaration node (whether or not node is also a
	 * definition node).
	 * 
	 * Let F be the ABC Function Entity corresponding to this function declaration.
	 * 
	 * First, see if there is already a CIVL Function CF corresponding to F. If not,
	 * create one and add it to the model and map(s). This may be an ordinary or a
	 * system function. (It is a system function if F does not have any definition.)
	 * 
	 * Process the contract (if any) and add it to whatever is already in the
	 * contract fields of CF.
	 * 
	 * If F is a function definition, add to lists of unprocessed function
	 * definitions: unprocessedFunctions.add(node); containingScopes.put(node,
	 * scope);. Function bodies will be processed at a later pass.
	 * 
	 * @param node  any ABC function declaration node
	 * @param scope the scope in which the function declaration occurs
	 */
	private Fragment translateFunctionDeclarationNode(FunctionDeclarationNode node, Scope scope) {
		Function entity = node.getEntity();
		SequenceNode<ContractNode> contract = node.getContract();
		CIVLFunction result;
		Fragment fragment = null;
		// Flag: True if and only if the given node represents a regular
		// function definition:
		boolean isRegularDefinition = node.ordinaryDeclarationKind() == OrdinaryDeclarationKind.FUNCTION_DEFINITION;

		if (entity == null)
			throw new CIVLInternalException("Unresolved function declaration", modelFactory.sourceOf(node));
		result = modelBuilder.functionMap.get(entity);

		// TODO: debugging code...
		// System.out.println("*** function: "+entity.getName());
		// if ("$wait".equals(entity.getName())) {
		// System.out.println("*** $wait function is being translated now.");
		// }

		// Create or update the CIVLFunction object in two cases:
		// 1. It is the first time encountering the function declaration or
		// definition.
		// 2. It is a regular function definition, then the CIVLFunction should
		// be updated in case parameter names are different from previous
		// declarations.
		if (result == null || isRegularDefinition) {
			CIVLSource nodeSource = modelFactory.sourceOf(node);
			Scope parameterScope = modelFactory.scope(nodeSource, scope, new ArrayList<>(0), null);
			CIVLSource identifierSource = modelFactory.sourceOf(node.getIdentifier());
			Identifier functionIdentifier = modelFactory.identifier(identifierSource, entity.getName());
			ArrayList<Variable> parameters = new ArrayList<Variable>();
			// type should come from entity, not this type node.
			// if it has a definition node, should probably use that one.
			FunctionType functionType = entity.getType();
			FunctionTypeNode functionTypeNode = (FunctionTypeNode) node.getTypeNode();
			CIVLType returnType = translateABCTypeNode(modelFactory.sourceOf(functionTypeNode.getReturnType()), scope,
					functionTypeNode.getReturnType());
			SequenceNode<VariableDeclarationNode> abcParameters = functionTypeNode.getParameters();
			int numParameters = abcParameters.numChildren();

			for (int i = 0; i < numParameters; i++) {
				VariableDeclarationNode decl = abcParameters.getSequenceChild(i);

				// Don't process void types. Should only happen in the prototype
				// of a function with no parameters.
				if (decl.getTypeNode().kind() != TypeNodeKind.VOID) {
					CIVLType type = translateABCType(modelFactory.sourceOf(decl), parameterScope,
							functionType.getParameterType(i));
					CIVLSource source = modelFactory.sourceOf(decl);
					String varName = decl.getName() == null ? "_arg" + i : decl.getName();
					Identifier variableName = modelFactory.identifier(source, varName);
					Variable parameter = modelFactory.variable(source, type, variableName, parameters.size() + 1);

					if (decl.getTypeNode().isConstQualified())
						parameter.setConst(true);
					parameters.add(parameter);
					parameterScope.addVariable(parameter);
				}
			}
			if (entity.isLogic())
				result = buildLogicFunction(entity, scope, parameterScope, parameters, functionIdentifier, nodeSource);
			else if (entity.getDefinition() != null)
				result = buildRegularCIVLFunction(entity, node, scope, parameterScope, parameters, functionIdentifier,
						functionType, returnType, nodeSource);
			else if (entity.isSystemFunction())
				result = buildSystemCIVLFunction(entity, node, scope, parameterScope, parameters, functionIdentifier,
						functionType, returnType, nodeSource);
			else if (entity.isAbstract())
				result = buildAbstractCIVLFunction(entity, node, scope, parameterScope, parameters, functionIdentifier,
						functionType, returnType, nodeSource);
			else
				throw new CIVLSyntaxException("Function " + entity.getName() + " doesn't have a definition.",
						identifierSource);
			result.setStateFunction(node.hasStatefFunctionSpecifier());
			result.setPureFunction(node.hasPureFunctionSpecifier());
			if (scope.getFunction(result.name().name()) == null)
				scope.addFunction(result);
			parameterScope.setFunction(result);
			modelBuilder.functionMap.put(entity, result);
		}
		if (contract != null) {
			if (result.functionContract() != null) {
				// TODO: eventually, find a way to combine contracts
				/*
				 * System.err .println("Warning: ignoring new contract for function " +
				 * function.name());
				 */
			} else {
				FunctionContractTranslator contractTranslator = new FunctionContractTranslator(modelBuilder,
						modelFactory, typeFactory, result, this.civlConfig);
				contractTranslator.translateFunctionContract(contract);
			}
		}
		return fragment;
	}

	/**
	 * Build logic function. Definitions of logic functions will be translated in
	 * this method.
	 */
	private CIVLFunction buildLogicFunction(Function entity, Scope scope, Scope parameterScope,
			ArrayList<Variable> parameters, Identifier functionIdentifier, CIVLSource functionSource) {
		CIVLFunction result = modelBuilder.functionMap.get(entity);
		FunctionDefinitionNode funcDefinition = entity.getDefinition();

		if (result != null)
			return result;
		assert funcDefinition == null || funcDefinition.isLogicFunction();

		FunctionTypeNode funcTypeNode = (FunctionTypeNode) ((FunctionDeclarationNode) entity.getFirstDeclaration())
				.getTypeNode();
		CIVLSource outputTypeSource = modelFactory.sourceOf(funcTypeNode.getReturnType());
		CIVLType outputType = translateABCTypeNode(outputTypeSource, scope, funcTypeNode.getReturnType());

		if (funcDefinition != null) {
			ExpressionNode defnExpression = funcDefinition.getLogicDefinition();
			Expression definition;

			if (!defnExpression.isSideEffectFree(false))
				throw new CIVLSyntaxException(
						"A logic function (or predicate) " + "definition must be a side-effect free expression.",
						defnExpression.getSource());

			int pointerToArrayMap[] = new int[parameters.size()];
			int i = 0;

			// initialize:
			Arrays.fill(pointerToArrayMap, -1);
			// add dummy heap variable for pointer type formal parameters of
			// this logic function....
			// This is the way how pointers in logic function definitions become
			// state-independent: when evaluating the logic function
			// definitions, pointers will point to those dummy heap variables
			// and those heap variables will have unique arbitrary array values.
			for (Variable param : parameters) {
				if (param.type().isPointerType()) {
					checkSupportedFormalType(param);
					CIVLType arrayType = modelFactory.typeFactory()
							.incompleteArrayType(((CIVLPointerType) param.type()).baseType());
					Identifier ptrHeapName = modelFactory.identifier(param.getSource(),
							LogicFunction.heapVariableName(param));

					pointerToArrayMap[i] = parameterScope.numVariables();
					parameterScope.addVariable(modelFactory.variable(param.getSource(), arrayType, ptrHeapName,
							parameterScope.numVariables()));
				}
				i++;
			}
			definition = translateExpressionNode(defnExpression, parameterScope, true);
			result = modelFactory.logicFunction(functionSource, functionIdentifier, parameterScope, parameters,
					outputType, pointerToArrayMap, scope, definition);
		}
		if (result == null)
			result = modelFactory.logicFunction(functionSource, functionIdentifier, parameterScope, parameters,
					outputType, new int[parameters.size()], scope, null);
		return result;
	}

	/**
	 * Check if logic function formal parameter type is supported. Currently only
	 * scalar types or pointer to
	 * <ul>
	 * <li>1. non-pointer scalar type</li>
	 * <li>ARRAY type</li>
	 * </ul>
	 * are supported.
	 * 
	 * @param formal
	 */
	private void checkSupportedFormalType(Variable formal) {
		CIVLType type = formal.type();

		if (type.isScalar() && !type.isPointerType())
			return;
		if (type.isPointerType()) {
			CIVLType referredType = ((CIVLPointerType) type).baseType();

			if (referredType.isScalar() && !referredType.isPointerType())
				return;
			else if (referredType.isArrayType())
				return;
		}
		// this error is triggered only if the supported type checking in
		// transformer has changed but the model translation doesn't get
		// changed.
		throw new CIVLInternalException("Cannot translate logic function with a formal parameter of type :" + type,
				formal.getSource());
	}

	/**
	 * <p>
	 * Creates or update a {@link CIVLFunction} object for a regular function
	 * entity. The CIVLFunction will be created at the first time the translator
	 * encounters either the declaration or the definition of a function; The
	 * CIVLFunction will be updated with its parameters and parameter scope when the
	 * definition of it is encountered after being created.
	 * </p>
	 * 
	 * <p>
	 * The CIVLFunction will be added to "modelBuilder.unprocessedFunctions" once
	 * its definition is encountered.
	 * </p>
	 * 
	 * @param entity             An entity associates to a regular function.
	 * @param node               An instance of a {@link FunctionDeclarationNode}
	 * @param scope              The {@link Scope} where the function is located
	 * @param parameterScope     The {@link Scope} where the function parameters are
	 *                           located
	 * @param parameters         An {@link ArrayList} of {@link Variable}s which are
	 *                           formal parameters
	 * @param functionIdentifier The {@link Identifier} of the function
	 * @param functionType       The {@link FunctionType} of the function
	 * @param returnType         The function return type
	 * @param functionSource     The {@link CIVLSource} associated to the function
	 *                           declaration (or definition).
	 * @return The created (or updated) CIVLFunction object.
	 */
	private CIVLFunction buildRegularCIVLFunction(Function entity, FunctionDeclarationNode node, Scope scope,
			Scope parameterScope, ArrayList<Variable> parameters, Identifier functionIdentifier,
			FunctionType functionType, CIVLType returnType, CIVLSource functionSource) {
		CIVLFunction result = modelBuilder.functionMap.get(entity);
		boolean isDefinition = node.ordinaryDeclarationKind() == OrdinaryDeclarationKind.FUNCTION_DEFINITION;

		// If it's the first time encountering either the function declaration
		// or definition, create the CIVLFunction object, else if it encounters
		// a function definition, update the parameters:
		if (result == null)
			result = modelFactory.function(functionSource, entity.isAtomic(), functionIdentifier, parameterScope,
					parameters, returnType, scope, null);
		else if (isDefinition) {
			result.setOuterScope(parameterScope);
			result.setParameters(parameters);
		}
		// add to the unprocessedFunctions:
		if (isDefinition)
			modelBuilder.unprocessedFunctions.add(entity.getDefinition());
		return result;
	}

	/**
	 * <p>
	 * Create a {@link CIVLFunction} object for a CIVL system function. This method
	 * should only be called once per entity. The CIVLFunction object should be
	 * created when the first time the translator encounters a declaration.
	 * <p>
	 * 
	 * @param entity             An entity associates to a regular function.
	 * @param node               An instance of a {@link FunctionDeclarationNode}
	 * @param scope              The {@link Scope} where the function is located
	 * @param parameterScope     The {@link Scope} where the function parameters are
	 *                           located
	 * @param parameters         An {@link ArrayList} of {@link Variable}s which are
	 *                           formal parameters
	 * @param functionIdentifier The {@link Identifier} of the function
	 * @param functionType       The {@link FunctionType} of the function
	 * @param returnType         The function return type
	 * @param functionSource     The {@link CIVLSource} associated to the function
	 *                           declaration
	 * @return The created CIVLFunction object.
	 */
	private CIVLFunction buildSystemCIVLFunction(Function entity, FunctionDeclarationNode node, Scope scope,
			Scope parameterScope, ArrayList<Variable> parameters, Identifier functionIdentifier,
			FunctionType functionType, CIVLType returnType, CIVLSource functionSource) {
		Source declSource = node.getIdentifier().getSource();
		CivlcToken token = declSource.getFirstToken();
		File file = token.getSourceFile().getFile();
		String functionName = functionIdentifier.name();
		// fileName will be something like "stdlib.h" or "civlc.h"
		String fileName = file.getName();
		String libName;

		switch (functionIdentifier.name()) {
		case "$assert":
		case "$assume":
		case "$defined":
		case "$havoc":
			libName = "civlc";
			break;
		case "$assert_equals":
		case "$equals":
			libName = "pointer";
			break;
		default: {
			libName = entity.systemLibrary();

			if (libName == null) {
				if (!fileName.contains("."))
					throw new CIVLInternalException(
							"Malformed file name " + fileName + " containing system function " + functionName,
							functionSource);
				libName = fileNameWithoutExtension(fileName);
			}
		}
		}
		return modelFactory.systemFunction(functionSource, functionIdentifier, parameterScope, parameters, returnType,
				scope, libName);
	}

	/**
	 * <p>
	 * Create a {@link CIVLFunction} object for an abstract function. An abstract
	 * function declaration is a function definition as well. So this method should
	 * only be called once per entity. The CIVLFunction object should be created
	 * when the first time the translator encounters a declaration.
	 * </p>
	 * 
	 * @param entity             An entity associates to a regular function.
	 * @param node               An instance of a {@link FunctionDeclarationNode}
	 * @param scope              The {@link Scope} where the function is located
	 * @param parameterScope     The {@link Scope} where the function parameters are
	 *                           located
	 * @param parameters         An {@link ArrayList} of {@link Variable}s which are
	 *                           formal parameters
	 * @param functionIdentifier The {@link Identifier} of the function
	 * @param functionType       The {@link FunctionType} of the function
	 * @param returnType         The function return type
	 * @param functionSource     The {@link CIVLSource} associated to the function
	 *                           declaration
	 * @return The created CIVLFunction object.
	 */
	private CIVLFunction buildAbstractCIVLFunction(Function entity, FunctionDeclarationNode node, Scope scope,
			Scope parameterScope, ArrayList<Variable> parameters, Identifier functionIdentifier,
			FunctionType functionType, CIVLType returnType, CIVLSource functionSource) {
		AbstractFunctionDefinitionNode absFunNode = (AbstractFunctionDefinitionNode) node;
		String attr = null;

		if (absFunNode.getAttribute() != null) {
			attr = absFunNode.getAttribute().getStringRepresentation();
			// trim the escaped double-quotes:
			attr = attr.substring(1, attr.length() - 1);
		}
		if (parameters.isEmpty())
			throw new CIVLSyntaxException(
					"$abstract functions must have at least one input.\n"
							+ "An abstract function with 0 inputs is a constant.\n"
							+ "It can be declared as an unconstrained input variable instead, e.g.\n" + "$input int N;",
					node.getSource());
		return modelFactory.abstractFunction(functionSource, functionIdentifier, parameterScope, parameters, returnType,
				scope, attr);
	}

	/**
	 * callWaitAll = modelFactory.callOrSpawnStatement(parForEndSource, location,
	 * true, modelFactory.waitallFunctionPointer(),
	 * Arrays.asList(this.arrayToPointer(parProcs), domSizeVar), null);
	 */
	private Statement elaborateDomainCall(Scope scope, Expression domain) {
		CIVLSource source = domain.getSource();
		Location location = modelFactory.location(source, scope);
		CallOrSpawnStatement call = this.modelFactory.callOrSpawnStatement(source, location, true,
				modelFactory.elaborateDomainPointer(), Arrays.asList(domain), null, false);

		return call;
	}

	/**
	 * Translate goto statement, since the labeled location might not have been
	 * processed, record the no-op statement and the label to be complete later
	 * 
	 * @param scope    The scope
	 * @param gotoNode The goto node
	 * @return The fragment of the goto statement
	 */
	private Fragment translateGotoNode(Scope scope, GotoNode gotoNode) {
		OrdinaryLabelNode label = ((Label) gotoNode.getLabel().getEntity()).getDefinition();
		Location location = modelFactory.location(modelFactory.sourceOfBeginning(gotoNode), scope);
		Statement noop = modelFactory.gotoBranchStatement(modelFactory.sourceOf(gotoNode), location, label.getName());

		// At this point, the target of the goto may or may not have been
		// encountered. We store the goto in a map from statements to labels.
		// When labeled statements are encountered, we store a map from the
		// label to the corresponding location. When functionInfo.complete() is
		// called, it will get the label for each goto noop from the map and set
		// the target to the corresponding location.
		functionInfo.putToGotoStatement(noop, label);
		return new CommonFragment(noop);
	}

	/**
	 * Translate an IfNode (i.e., an if-else statement) into a fragment.
	 * 
	 * @param scope  The scope of the start location of the resulting fragment.
	 * @param ifNode The if node to be translated.
	 * @return The fragment of the if-else statements.
	 */
	private Fragment translateIfNode(Scope scope, IfNode ifNode) {
		ExpressionNode conditionNode = ifNode.getCondition();
		Expression expression = translateExpressionNode(conditionNode, scope, true);
		Fragment trueBranch, trueBranchBody, falseBranch, falseBranchBody, result;
		Location location = modelFactory.location(modelFactory.sourceOfBeginning(ifNode), scope);
		Fragment anonFragment = null;

		try {
			expression = modelFactory.booleanExpression(expression);
		} catch (ModelFactoryException err) {
			throw new CIVLSyntaxException("The condition of the if statement " + expression + " is of "
					+ expression.getExpressionType() + " type which cannot be converted to boolean type.",
					expression.getSource());
		}
		if (modelFactory.anonFragment() != null) {
			anonFragment = modelFactory.anonFragment();
			modelFactory.clearAnonFragment();
		}
		trueBranch = new CommonFragment(modelFactory.ifElseBranchStatement(
				modelFactory.sourceOfBeginning(ifNode.getTrueBranch()), location, expression, true));
		falseBranch = new CommonFragment(modelFactory.ifElseBranchStatement(modelFactory.sourceOfEnd(ifNode), location,
				modelFactory.unaryExpression(expression.getSource(), UNARY_OPERATOR.NOT, expression), false));
		trueBranchBody = translateStatementNode(scope, ifNode.getTrueBranch());
		trueBranch = trueBranch.combineWith(trueBranchBody);
		if (ifNode.getFalseBranch() != null) {
			falseBranchBody = translateStatementNode(scope, ifNode.getFalseBranch());
			falseBranch = falseBranch.combineWith(falseBranchBody);
		}
		result = trueBranch.parallelCombineWith(falseBranch);
		result = this.insertNoopAtBeginning(modelFactory.sourceOfBeginning(ifNode), scope, result);
		if (anonFragment != null)
			result = anonFragment.combineWith(result);
		return result;
	}

	/**
	 * Translate a jump node (i.e., a break or continue statement) into a fragment.
	 * 
	 * @param scope    The scope of the source location of jump statement.
	 * @param jumpNode The jump node to be translated.
	 * @return The fragment of the break or continue statement
	 */
	private Fragment translateJumpNode(Scope scope, JumpNode jumpNode) {
		Location location = modelFactory.location(modelFactory.sourceOfBeginning(jumpNode), scope);
		Statement result = modelFactory.noopStatement(modelFactory.sourceOf(jumpNode), location, null);
		JumpKind kind = jumpNode.getKind();

		switch (kind) {
		case BREAK:
			functionInfo.peekBreakStack().add(result);
			break;
		case CONTINUE:
			functionInfo.peekContinueStack().add(result);
			break;
		case GOTO:
			return translateGotoNode(scope, (GotoNode) jumpNode);
		default:// RETURN
			return translateReturnNode(scope, (ReturnNode) jumpNode);
		}
		// if (jumpNode.getKind() == JumpKind.CONTINUE) {
		// functionInfo.peekContinueStack().add(result);
		// } else if (jumpNode.getKind() == JumpKind.BREAK) {
		// functionInfo.peekBreakStack().add(result);
		// } else {
		// throw new CIVLInternalException(
		// "Jump nodes other than BREAK and CONTINUE should be handled
		// seperately.",
		// modelFactory.sourceOf(jumpNode.getSource()));
		// }
		return new CommonFragment(result);
	}

	/**
	 * Translate labeled statements
	 * 
	 * @param scope              The scope
	 * @param labelStatementNode The label statement node
	 * @return The fragment of the label statement
	 */
	private Fragment translateLabelStatementNode(Scope scope, LabeledStatementNode labelStatementNode) {
		Fragment result = translateStatementNode(scope, labelStatementNode.getStatement());

		functionInfo.putToLabeledLocations(labelStatementNode.getLabel(), result.startLocation());
		return result;
	}

	/**
	 * Translate a loop node that is a while node or a do-while node into a fragment
	 * of CIVL statements
	 * 
	 * @param scope    The scope
	 * @param loopNode The while loop node
	 * @return the fragment of the while loop
	 */
	private Fragment translateLoopNode(Scope scope, LoopNode loopNode) {
		Fragment result;
		// Translate loop invariants, loop invariants can be used in both
		// contracts system mode and regular CIVL mode:
		SequenceNode<ContractNode> loopContractNode = loopNode.loopContracts();
		LoopContract loopContract = loopContractNode == null ? null
				: translateLoopInvariants(scope, null, loopNode.loopContracts(),
						modelFactory.sourceOf(loopContractNode));

		switch (loopNode.getKind()) {
		case DO_WHILE:
			result = composeLoopFragment(scope, loopNode.getCondition(), loopNode.getBody(), null, true, loopContract);
			break;
		case FOR:
			result = translateForLoopNode(scope, (ForLoopNode) loopNode, loopContract);
			break;
		default:// case WHILE:
			result = composeLoopFragment(scope, loopNode.getCondition(), loopNode.getBody(), null, false, loopContract);
		}
		if (result.startLocation().getNumOutgoing() > 1)
			result = this.insertNoopAtBeginning(modelFactory.sourceOfBeginning(loopNode), scope, result);
		return result;
	}

	private LoopContract translateLoopInvariants(Scope scope, Location loopLocation,
			SequenceNode<ContractNode> loopContractsNode, CIVLSource civlSource) {
		List<Expression> loopInvariants = new LinkedList<>();
		List<LHSExpression> loopAssigns = new LinkedList<>();
		List<Expression> loopVariants = new LinkedList<>();

		for (ContractNode contract : loopContractsNode) {
			switch (contract.contractKind()) {
			case INVARIANT:
				InvariantNode invariant = (InvariantNode) contract;
				Expression invariantExpression = translateExpressionNode(invariant.getExpression(), scope, true);

				if (!invariantExpression.getExpressionType().isBoolType())
					throw new CIVLSyntaxException("Expressions specified by loop invariant must be boolean expressions",
							invariantExpression.getSource());
				loopInvariants.add(invariantExpression);
				break;
			case ASSIGNS_READS:
				AssignsOrReadsNode assigns = (AssignsOrReadsNode) contract;

				assert assigns.isAssigns();
				for (ExpressionNode memoryLoc : assigns.getMemoryList()) {
					Expression memLocExpr = translateExpressionNode(memoryLoc, scope, true);

					assert memLocExpr instanceof LHSExpression;
					loopAssigns.add((LHSExpression) memLocExpr);
				}
				break;
			default:
				throw new CIVLSyntaxException(
						"Non support contract clause for loop statements: " + contract.contractKind());
			}
		}
		return modelFactory.loopContract(civlSource, loopLocation, loopInvariants, loopAssigns, loopVariants);
	}

	/**
	 * Translate a null statement node into a fragment of a no-op statement
	 * 
	 * @param scope             The scope
	 * @param nullStatementNode The null statement node
	 * @return the fragment of the null statement (i.e. no-op statement)
	 */
	private Fragment translateNullStatementNode(Scope scope, NullStatementNode nullStatementNode) {
		Location location = modelFactory.location(modelFactory.sourceOfBeginning(nullStatementNode), scope);

		return new CommonFragment(modelFactory.noopStatement(modelFactory.sourceOf(nullStatementNode), location, null));
	}

	/**
	 * Translate return statements
	 * 
	 * @param scope      The scope
	 * @param returnNode The return node
	 * @return The fragment of the return statement
	 */
	private Fragment translateReturnNode(Scope scope, ReturnNode returnNode) {
		Location location;
		Expression expression;
		CIVLFunction function = this.functionInfo.function();
		Fragment returnFragment, atomicReleaseFragment = new CommonFragment();

		if (returnNode.getExpression() != null) {
			expression = translateExpressionNode(returnNode.getExpression(), scope, true);
			if (function.returnType().isBoolType()) {
				try {
					expression = modelFactory.booleanExpression(expression);
				} catch (ModelFactoryException err) {
					throw new CIVLSyntaxException("The return type of the function " + function.name().name()
							+ " is boolean, but the returned expression " + expression + " is of "
							+ expression.getExpressionType() + " type which cannot be converted to boolean type.",
							expression.getSource());
				}
			}
		} else
			expression = null;
		if (this.atomicCount > 0) {
			Statement leaveAtomic;
			SymbolicUniverse universe = modelFactory.universe();
			// UndefinedProcessExpression has the constant value:
			SymbolicExpression undefinedProcValue = universe.tuple(typeFactory.processSymbolicType(),
					new Singleton<SymbolicExpression>(universe.integer(ModelConfiguration.UNDEFINED_PROC_ID)));

			for (int i = 0; i < this.atomicCount; i++) {
				location = modelFactory.location(modelFactory.sourceOfBeginning(returnNode), scope);
				location.setLeaveAtomic();
				leaveAtomic = new CommonAtomicLockAssignStatement(location.getSource(),
						modelFactory.atomicLockVariableExpression().expressionScope(),
						modelFactory.atomicLockVariableExpression().expressionScope(), location,
						modelFactory.trueExpression(location.getSource()), false,
						modelFactory.atomicLockVariableExpression(), new CommonUndefinedProcessExpression(
								modelFactory.systemSource(), typeFactory.processType(), undefinedProcValue));
				atomicReleaseFragment.addNewStatement(leaveAtomic);
			}
		}
		location = modelFactory.location(modelFactory.sourceOfBeginning(returnNode), scope);
		returnFragment = modelFactory.returnFragment(modelFactory.sourceOf(returnNode), location, expression, function);
		return atomicReleaseFragment.combineWith(returnFragment);
	}

	/**
	 * Translates a ResultNode as an new variable, and adds it into a corresponding
	 * scope. The $result expression can only be translated by
	 * {@link FunctionContractTranslator}.
	 * 
	 * @param resultNode The {@link ResultNode} appears in a contract clause
	 * @param scope      The scope of the contract clause, same as the scope of
	 *                   function arguments
	 * @return
	 */
	protected Expression translateResultNode(ResultNode resultNode, Scope scope) {
		throw new CIVLSyntaxException("$result expression used in a non-contract environment.");
	}

	/**
	 * Translate a spawn node into a fragment containing the spawn statement
	 * 
	 * @param scope     The scope in which this statement occurs. Must be non-null.
	 * @param spawnNode The ABC representation of the spawn, which will be
	 *                  translated to yield a new {@link Fragment}. Must be
	 *                  non-null.
	 * @return The fragment of the spawn statement
	 */
	private Fragment translateSpawnNode(Scope scope, SpawnNode spawnNode) {
		return new CommonFragment(translateFunctionCall(scope, null, spawnNode.getCall(), false, false,
				modelFactory.sourceOf(spawnNode)));
	}

	/**
	 * Translate switch block into a fragment
	 * 
	 * @param scope      The scope
	 * @param switchNode The switch node
	 * @return The fragment of the switch statements
	 */
	private Fragment translateSwitchNode(Scope scope, SwitchNode switchNode) {
		Fragment result = new CommonFragment();
		Iterator<LabeledStatementNode> cases = switchNode.getCases();
		Expression condition = translateExpressionNode(switchNode.getCondition(), scope, true);
		// Collect case guards to determine guard for default case.
		Expression combinedCaseGuards = null;
		Fragment bodyGoto;
		Statement defaultExit = null;
		Set<Statement> breaks;
		Location location = modelFactory.location(modelFactory.sourceOfSpan(modelFactory.sourceOfBeginning(switchNode),
				modelFactory.sourceOfBeginning(switchNode.child(1))), scope);

		functionInfo.addBreakSet(new LinkedHashSet<Statement>());
		// All caseGoto and defaultGoto statements will be updated with the
		// correct target location in the method
		// functionInfo.completeFunction(). So it is not a problem to have it
		// wrong here, because it will finally get corrected.
		while (cases.hasNext()) {
			LabeledStatementNode caseStatement = cases.next();
			SwitchLabelNode label;
			Expression caseGuard;
			Fragment caseGoto;
			Expression labelExpression;

			assert caseStatement.getLabel() instanceof SwitchLabelNode;
			label = (SwitchLabelNode) caseStatement.getLabel();
			labelExpression = translateExpressionNode(label.getExpression(), scope, true);
			caseGuard = modelFactory.binaryExpression(modelFactory.sourceOf(label.getExpression()),
					BINARY_OPERATOR.EQUAL, condition, labelExpression);
			if (combinedCaseGuards == null) {
				combinedCaseGuards = caseGuard;
			} else {
				combinedCaseGuards = modelFactory.binaryExpression(
						modelFactory.sourceOfSpan(caseGuard.getSource(), combinedCaseGuards.getSource()),
						BINARY_OPERATOR.OR, caseGuard, combinedCaseGuards);
			}
			caseGoto = new CommonFragment(modelFactory.switchBranchStatement(modelFactory.sourceOf(caseStatement),
					location, caseGuard, labelExpression));
			result = result.parallelCombineWith(caseGoto);
			for (Statement stmt : caseGoto.finalStatements())
				functionInfo.putToGotoStatement(stmt, label);
		}
		if (switchNode.getDefaultCase() != null) {
			LabelNode label = switchNode.getDefaultCase().getLabel();
			Fragment defaultGoto = new CommonFragment(
					modelFactory.switchBranchStatement(modelFactory.sourceOf(switchNode.getDefaultCase()), location,
							modelFactory.unaryExpression(modelFactory.sourceOfBeginning(switchNode.getDefaultCase()),
									UNARY_OPERATOR.NOT, combinedCaseGuards)));

			result = result.parallelCombineWith(defaultGoto);
			location.setSwitchOrChooseWithDefault();
			for (Statement stmt : defaultGoto.finalStatements())
				functionInfo.putToGotoStatement(stmt, label);
		} else {
			defaultExit = modelFactory.noopStatementWtGuard(modelFactory.sourceOfBeginning(switchNode), location,
					modelFactory.unaryExpression(modelFactory.sourceOfBeginning(switchNode), UNARY_OPERATOR.NOT,
							combinedCaseGuards));
		}
		bodyGoto = translateStatementNode(scope, switchNode.getBody());
		// Although it is not correct to have caseGotos and defaultGoto to go to
		// the start location of the switch body, we have to do it here for the
		// following reason: 1. the fragment before the switch block need to set
		// its last statement to go to the start location of this switch
		// fragment; 2. the fragment after the switch block needs to have its
		// start location set to be the target of all last statements of this
		// switch body. We can't return purely caseGotos or bodyGoto without
		// combining them as one fragment. Moreover, it is not
		// a problem to have them wrong here, because they will finally get
		// corrected when calling functionInfo.completeFunction().
		result = result.combineWith(bodyGoto);
		breaks = functionInfo.popBreakStack();
		if (breaks.size() > 0) {
			for (Statement s : breaks) {
				result.addFinalStatement(s);
			}
		}
		if (defaultExit != null)
			result.addFinalStatement(defaultExit);
		return this.insertNoopAtBeginning(modelFactory.sourceOfBeginning(switchNode), scope, result);
	}

	/**
	 * Translates a variable declaration node. If the given sourceLocation is
	 * non-null, it is used as the source location for the new statement(s).
	 * Otherwise a new location is generated and used. This method may return null
	 * if no statements are generated as a result of processing the declaration.
	 * 
	 * @param sourceLocation location to use as origin of new statements or null
	 * @param scope          CIVL scope in which this declaration appears
	 * @param node           the ABC variable declaration node to translate
	 * @return the pair consisting of the (new or given) start location of the
	 *         generated fragment and the last statement in the sequence of
	 *         statements generated by translating this declaration node, or null if
	 *         no statements are generated
	 * @throws CommandLineException if an initializer for an input variable
	 *                              specified on the command line does not have a
	 *                              type compatible with the variable
	 */
	private Fragment translateVariableDeclarationNode(Location sourceLocation, Scope scope,
			VariableDeclarationNode node) throws CommandLineException {
		Pair<Variable, Boolean> pair = translateVariableDeclarationNode(node, scope);
		Variable variable = pair.left;
		Boolean exists = pair.right;

		CIVLType type = variable.type();
		Fragment result = null, initialization = null;
		IdentifierNode identifier = node.getIdentifier();
		CIVLSource source = modelFactory.sourceOf(node);
		boolean initializerTranslated = false;

		if (sourceLocation == null)
			sourceLocation = modelFactory.location(modelFactory.sourceOfBeginning(node), scope);
		result = new CommonFragment(modelFactory.noopStatementForVariableDeclaration(source, sourceLocation));
		if (variable.isInput() || variable.isStatic() || type instanceof CIVLArrayType
				|| type instanceof CIVLStructOrUnionType || type.isHeapType()) {
			Expression rhs = null;

			if (variable.isInput() && modelBuilder.inputInitMap != null) {
				String name = variable.name().name();
				Object value = modelBuilder.inputInitMap.get(name);

				if (value != null) {
					rhs = constant(variable, value);
					modelBuilder.initializedInputs.add(name);
				}
			}
			if (rhs == null && node.getInitializer() == null && !exists)
				rhs = modelFactory.initialValueExpression(source, variable);
			if (sourceLocation == null)
				sourceLocation = modelFactory.location(modelFactory.sourceOfBeginning(node), scope);
			if (rhs != null) {
				Location location = modelFactory.location(modelFactory.sourceOfEnd(node), scope);

				initializerTranslated = true;
				result = result.combineWith(new CommonFragment(modelFactory.assignStatement(source, location,
						modelFactory.variableExpression(modelFactory.sourceOf(identifier), variable), rhs, true)));
			}
		}
		// for input variables, only use the initialization if there
		// was no command line specification of the input value:
		if (!initializerTranslated || !variable.isInput()) {
			initialization = translateVariableInitializationNode(node, variable, null, scope);
			result = result.combineWith(initialization);
		}
		return result;
	}

	/**
	 * Processes a variable declaration. Adds the new variable to the given scope.
	 * 
	 * @param scope the Model scope in which the variable declaration occurs
	 * @param node  the AST variable declaration node.
	 * @return a pair whose left (pair.left) is variable in the declaration node and
	 *         its right is a boolean value indicates whether the variable already
	 *         exists in this scope.
	 */
	protected Pair<Variable, Boolean> translateVariableDeclarationNode(VariableDeclarationNode node, Scope scope) {
		return translateVariableDeclarationNodeWork(node, scope, false);
	}

	/**
	 * @return a pair whose left (pair.left) is variable in the declaration node and
	 *         its right is a boolean value indicates whether the variable already
	 *         exists in this scope.
	 */
	private Pair<Variable, Boolean> translateVariableDeclarationNodeWork(VariableDeclarationNode node, Scope scope,
			boolean isBound) {
		if (!isBound) {
			dev.civl.abc.ast.entity.IF.Variable varEntity = node.getEntity();
			// node.prettyPrint(System.out);
			// System.out.println();
			if (varEntity.getDefinition() == null)
				throw new CIVLSyntaxException("Can't find the definition for variable " + node.getName(),
						node.getSource());
		}

		TypeNode typeNode = node.getTypeNode();
		CIVLType type = translateABCTypeNode(modelFactory.sourceOf(typeNode), scope, typeNode);
		CIVLSource source = modelFactory.sourceOf(node.getIdentifier());
		Identifier name = modelFactory.identifier(source, node.getName());
		int vid = isBound ? -1 : scope.numVariables();
		Variable variable = modelFactory.variable(source, type, name, vid);

		if (!isBound) {
			if (typeNode.isConstQualified())
				variable.setConst(true);

			// the following looks in scope only (not ancestors):
			Variable searchVar = scope.getMatch(variable);
			if (searchVar != null)
				return new Pair<>(searchVar, Boolean.valueOf(true));

			scope.addVariable(variable);
			if (node.getTypeNode().isInputQualified()) {
				variable.setIsInput(true);
				modelFactory.addInputVariable(variable);
				assert variable.scope().id() == ModelConfiguration.STATIC_ROOT_SCOPE;
			}
			if (node.getTypeNode().isOutputQualified()) {
				variable.setIsOutput(true);
			}
			if (node.hasStaticStorage()
					|| (node.getInitializer() == null && scope.id() == ModelConfiguration.STATIC_ROOT_SCOPE)) {
				variable.setStatic(true);
			}
		}
		return new Pair<>(variable, Boolean.valueOf(false));
	}

	/**
	 * Translate the initializer node of a variable declaration node (if it has one)
	 * into a fragment of an assign statement
	 * 
	 * @param node     The variable declaration node that might contain an
	 *                 initializer node
	 * @param variable The variable
	 * @param location The location
	 * @param scope    The scope containing this variable declaration node
	 * @return The fragment
	 */
	private Fragment translateVariableInitializationNode(VariableDeclarationNode node, Variable variable,
			Location location, Scope scope) {
		Fragment initFragment = null;
		InitializerNode init = node.getInitializer();
		LHSExpression lhs = modelFactory.variableExpression(modelFactory.sourceOf(node), variable);

		if (init != null) {
			if (!(init instanceof ExpressionNode) && !(init instanceof CompoundInitializerNode))
				throw new CIVLUnimplementedFeatureException("Non-expression initializer", modelFactory.sourceOf(init));
			if (location == null)
				location = modelFactory.location(modelFactory.sourceOfBeginning(node), scope);
			if (init instanceof CompoundInitializerNode) {
				Expression rhs = translateCompoundInitializerNode((CompoundInitializerNode) init, scope);

				initFragment = new CommonFragment(
						modelFactory.assignStatement(modelFactory.sourceOf(node), location, lhs, rhs, true));
			} else {
				assert init instanceof ExpressionNode;
				initFragment = this.assignStatement(modelFactory.sourceOf(node), lhs, (ExpressionNode) init, true,
						scope);
			}
			if (!modelFactory.anonFragment().isEmpty()) {
				initFragment = modelFactory.anonFragment().combineWith(initFragment);
				modelFactory.clearAnonFragment();
			}
		}
		return initFragment;
	}

	private Expression translateCompoundLiteralNode(CompoundLiteralNode compoundNode, Scope scope) {
		CIVLType type = translateABCType(modelFactory.sourceOf(compoundNode.getTypeNode()), scope,
				compoundNode.getType());

		if (type.isDomainType()) {
			// TODO: check this. Make sure that users don't need to specify the
			// dimension when using compound literal statement for DomainType.
			return translateDomainLiteral(compoundNode.getInitializerList(), scope, type);
		}
		return translateCompoundInitializerNode(compoundNode.getInitializerList(), scope);
	}

	private Expression translateDomainLiteral(CompoundInitializerNode domainLit, Scope scope, CIVLType type) {
		CIVLSource source = modelFactory.sourceOf(domainLit);
		int size = domainLit.numChildren();
		List<Expression> expressions = new ArrayList<>(size);

		assert type.isDomainType() : "Compound initializer nodes that are not children of"
				+ " $domain literals have been translated away by ABC";
		int dimension;

		if (!(type instanceof CIVLCompleteDomainType))
			throw new CIVLSyntaxException("It is illegal to define a $domain literal without the dimension specified.",
					source);
		dimension = ((CIVLCompleteDomainType) type).getDimension();
		assert size == dimension;
		for (int i = 0; i < size; i++)
			expressions.add(translateInitializerNode(domainLit.getSequenceChild(i).getRight(), scope));
		return modelFactory.recDomainLiteralExpression(source, expressions, type);
	}

	/**
	 * A CompoundInitializerNode initializes a struct/union or an array.
	 * 
	 * ( TODO: check that ABC should translate away other possibilities, e.g.,
	 * `(int*){&x}`
	 */
	private Expression translateCompoundInitializerNode(CompoundInitializerNode compoundInit, Scope scope) {
		CIVLSource source = modelFactory.sourceOf(compoundInit);
		CIVLType type = translateABCType(source, scope, compoundInit.getType());

		if (type.typeKind() == CIVLType.TypeKind.DOMAIN)
			return translateDomainLiteral(compoundInit, scope, type);

		CompoundLiteralExpression result = modelFactory.compoundLiteralExpression(source, scope, type, false);
		CompoundLiteralObject abcCompoundLitObj = compoundInit.getLiteralObject();
		CompoundLiteralExpression.CIVLCompoundLiteralObject CompoundLitObj = translateABCCompoundLiteralObject(
				abcCompoundLitObj, scope, result);

		result.setLiteralObject(CompoundLitObj);
		return result;
	}

	/**
	 * Translates ABC's {@link CompoundLiteralObject} to
	 * {@link CompoundLiteralExpression#CompoundLiteralObj}.
	 * 
	 * @param obj     an ABC {@link CompoundLiteralObject}
	 * @param scope   the {@link Scope} of <code>litExpr</code>
	 * @param litExpr the CompoundLiteralExpression that will own the translated
	 *                result; in this method, <code>litExpr</code> is used to
	 *                provide creator methods for
	 *                {@link CompoundLiteralExpression#LiteralObj}s.
	 * @return
	 */
	private CIVLCompoundLiteralObject translateABCCompoundLiteralObject(CompoundLiteralObject abcObj, Scope scope,
			CompoundLiteralExpression litExpr) {
		List<CIVLLiteralObject> elements = new LinkedList<>();
		Type abcType = abcObj.getType();
		CIVLType type = translateABCType(litExpr.getSource(), scope, abcType);

		if (type.isArrayType()) {
			CIVLType eltType = ((CIVLArrayType) type).elementType();

			for (LiteralObject eltObj : abcObj)
				elements.add(translateABCLiteralObject(eltObj, scope, eltType, litExpr));
			if (type.isIncompleteArrayType()) {
				// If 'type' is 'T[]', its size is determined by the number of
				// elements:
				Expression arraySize = modelFactory.integerLiteralExpression(
						modelFactory.sourceOf(abcObj.getSourceNode()), BigInteger.valueOf(elements.size()));

				type = typeFactory.completeArrayType(eltType, arraySize);
			}
			return litExpr.createCompoundLiteralObject(type, elements);
		} else if (type.isStructType()) {
			CIVLStructOrUnionType structTy = (CIVLStructOrUnionType) type;
			int idx = 0;

			assert structTy.numFields() == abcObj.size();
			for (LiteralObject fieldObj : abcObj) {
				CIVLType fieldTy = structTy.getField(idx).type();

				elements.add(translateABCLiteralObject(fieldObj, scope, fieldTy, litExpr));
				idx++;
			}
			return litExpr.createCompoundLiteralObject(structTy, elements);
		}
		assert type.isUnionType();

		CIVLStructOrUnionType unionTy = (CIVLStructOrUnionType) type;
		int idx = 0;

		assert unionTy.numFields() >= abcObj.size();
		for (LiteralObject fieldObj : abcObj) {
			CIVLType fieldTy = unionTy.getField(idx).type();

			elements.add(translateABCLiteralObject(fieldObj, scope, fieldTy, litExpr));
			idx++;
		}
		return litExpr.createCompoundLiteralObject(unionTy, elements);
	}

	private CIVLLiteralObject translateABCLiteralObject(LiteralObject /* Nullable */ abcLitObj, Scope scope,
			CIVLType abcLitObjTy, CompoundLiteralExpression litExpr) {
		if (abcLitObj == null)
			return litExpr.createScalarLiteralObject(abcLitObjTy, null);
		if (abcLitObj instanceof ScalarLiteralObject) {
			Expression expr = translateExpressionNode(((ScalarLiteralObject) abcLitObj).getExpression(), scope, true);
			return litExpr.createScalarLiteralObject(expr.getExpressionType(), expr);
		}

		return translateABCCompoundLiteralObject(((CompoundLiteralObject) abcLitObj), scope, litExpr);
	}

	private Expression translateInitializerNode(InitializerNode initNode, Scope scope) {
		if (initNode instanceof ExpressionNode)
			return translateExpressionNode((ExpressionNode) initNode, scope, true);

		assert initNode instanceof CompoundInitializerNode;
		return translateCompoundInitializerNode((CompoundInitializerNode) initNode, scope);
	}

	/**
	 * Translate a when node into a fragment of a when statement
	 * 
	 * @param scope    The scope
	 * @param whenNode The when node
	 * @return the fragment of the when statement
	 */
	private Fragment translateWhenNode(Scope scope, WhenNode whenNode) {
		Expression whenGuard = translateExpressionNode(whenNode.getGuard(), scope, true);
		Fragment result;
		// Location whenLocation = modelFactory
		// .location(modelFactory.sourceOfBeginning(whenNode), scope);

		try {
			// Convert numerical type to boolean type.
			whenGuard = modelFactory.booleanExpression(whenGuard);
		} catch (ModelFactoryException err) {
			throw new CIVLSyntaxException("The condition of the when statement " + whenGuard + " is of "
					+ whenGuard.getExpressionType() + "type which cannot be converted to " + "boolean type.",
					whenGuard.getSource());
		}
		result = translateStatementNode(scope, whenNode.getBody());
		if (!modelFactory.isTrue(whenGuard)) {
			// Each outgoing statement from the first location in this
			// fragment should have its guard set to the conjunction of guard
			// and that statement's guard.
			result.addGuardToStartLocation(whenGuard, modelFactory);
		}
		result.startLocation().setCIVLSource(modelFactory.sourceOfBeginning(whenNode));
		// result.updateStartLocation(whenLocation);
		return result;
	}

	/**
	 * Translate a {@link RunNode} to an anonymous function f. Replace the $run
	 * statement with a $spawn expression on f.
	 * 
	 * @param scope   The current scope.
	 * @param runNode The {@link RunNode}
	 * @return A {@link Fragment} with a unique {@link CallOrSpawnStatement} that
	 *         <code>{@link CallOrSpawnStatement#isRun()} == true</code>
	 */
	private Fragment translateRunStatementNode(Scope scope, RunNode runNode) {
		CIVLSource civlsource = modelFactory.sourceOf(runNode);
		CIVLSource startSource = modelFactory.sourceOfBeginning(runNode);
		CIVLFunction anonRunFunc;
		CallOrSpawnStatement stmt;
		Expression functionIdentifier;
		Fragment result;
		Location currentLocation;
		StatementNode bodyNode = runNode.getStatement();
		Scope parameterScope;
		String anonRunFuncName = RUN_FUNC_NAME + modelBuilder.runProcFunctions.size();

		currentLocation = modelFactory.location(startSource, scope);
		// Run statement will be transformed to an anonymous function which has
		// no parameters:
		parameterScope = modelFactory.scope(startSource, scope, Arrays.asList(), null);
		anonRunFunc = modelFactory.function(civlsource, false, modelFactory.identifier(startSource, anonRunFuncName),
				parameterScope, Arrays.asList(), typeFactory.voidType(), scope, null);
		scope.addFunction(anonRunFunc);
		parameterScope.setFunction(anonRunFunc);
		modelBuilder.runProcFunctions.put(anonRunFunc, bodyNode);
		functionIdentifier = modelFactory.functionIdentifierExpression(startSource, anonRunFunc);
		stmt = modelFactory.callOrSpawnStatement(civlsource, currentLocation, false, functionIdentifier,
				Arrays.asList(), null, false);
		// Set the callOrSpawnStatement as a $spawn translated from $run:
		stmt.setAsRun(true);
		result = new CommonFragment(stmt);
		return result;
	}

	/*
	 * *********************************************************************
	 * Translate AST Expression Node into CIVL Expression
	 * *********************************************************************
	 */

	/**
	 * Translate a struct pointer field reference from the CIVL AST to the CIVL
	 * model.
	 * 
	 * @param arrowNode The arrow expression.
	 * @param scope     The (static) scope containing the expression.
	 * @return The model representation of the expression.
	 */
	private Expression translateArrowNode(ArrowNode arrowNode, Scope scope) {
		Expression struct = translateExpressionNode(arrowNode.getStructurePointer(), scope, true);
		CIVLSource source = modelFactory.sourceOf(arrowNode);
		Field[] navseq = arrowNode.getNavigationSequence();
		Expression result = modelFactory.dereferenceExpression(source, struct);

		// s->f really means ((((*s).f0).f1).f2). ... .fn
		for (Field field : navseq)
			result = modelFactory.dotExpression(source, result, field.getMemberIndex());
		return result;
	}

	/**
	 * Translate a cast expression from the CIVL AST to the CIVL model.
	 * 
	 * @param castNode The cast expression.
	 * @param scope    The (static) scope containing the expression.
	 * @return The model representation of the expression.
	 */
	private Expression translateCastNode(CastNode castNode, Scope scope) {
		TypeNode typeNode = castNode.getCastType();
		CIVLType castType = translateABCType(modelFactory.sourceOf(typeNode), scope, typeNode.getType());
		ExpressionNode argumentNode = castNode.getArgument();
		Expression castExpression, result;
		CIVLSource source = modelFactory.sourceOf(castNode);

		castExpression = translateExpressionNode(argumentNode, scope, true);
		castExpression = arrayToPointer(castExpression);
		result = modelFactory.castExpression(source, castType, castExpression);
		return result;
	}

	/**
	 * Translate a ConstantNode into a CIVL literal expression
	 * 
	 * @param constantNode The constant node
	 * 
	 * @return a CIVL literal expression representing the constant node
	 */
	private Expression translateConstantNode(Scope scope, ConstantNode constantNode) {
		CIVLSource source = modelFactory.sourceOf(constantNode);
		Type convertedType = constantNode.getConvertedType();
		Expression result;

		switch (convertedType.kind()) {
		case SCOPE:
			HereOrRootNode scopeConstantNode = (HereOrRootNode) constantNode;

			result = modelFactory.hereOrRootExpression(source, scopeConstantNode.isRootNode());
			break;
		case PROCESS:
			String procValue = constantNode.getStringRepresentation();

			if (procValue.equals("$self"))
				result = modelFactory.selfExpression(source);
			else
				result = modelFactory.procnullExpression(source);
			break;
		case OTHER_INTEGER:
			if (constantNode instanceof EnumerationConstantNode) {
				BigInteger value = ((IntegerValue) ((EnumerationConstantNode) constantNode).getConstantValue())
						.getIntegerValue();

				result = modelFactory.integerLiteralExpression(source, value);
			} else {
				Value value = constantNode.getConstantValue();

				if (value instanceof IntegerValue)
					result = modelFactory.integerLiteralExpression(source, ((IntegerValue) value).getIntegerValue());
				else if (value instanceof RealFloatingValue)
					result = modelFactory.integerLiteralExpression(source,
							((RealFloatingValue) value).getWholePartValue());
				else if (value instanceof CharacterValue)
					result = translateCharacterValue(source, constantNode);
				else
					throw new CIVLSyntaxException("Invalid constant for integers", source);
			}
			break;
		case BASIC: {
			switch (((StandardBasicType) convertedType).getBasicTypeKind()) {
			case SHORT:
			case UNSIGNED_SHORT:
			case INT:
			case UNSIGNED:
			case LONG:
			case UNSIGNED_LONG:
			case LONG_LONG:
			case UNSIGNED_LONG_LONG:
				if (constantNode instanceof EnumerationConstantNode) {
					BigInteger value = ((IntegerValue) ((EnumerationConstantNode) constantNode).getConstantValue())
							.getIntegerValue();

					result = modelFactory.integerLiteralExpression(source, value);
				} else {
					Value value = constantNode.getConstantValue();

					if (value instanceof IntegerValue)
						result = modelFactory.integerLiteralExpression(source,
								((IntegerValue) value).getIntegerValue());
					else if (value instanceof RealFloatingValue)
						result = modelFactory.integerLiteralExpression(source,
								((RealFloatingValue) value).getWholePartValue());
					else
						throw new CIVLSyntaxException("Invalid constant for integers", source);
				}
				break;
			case FLOAT:
			case DOUBLE:
			case LONG_DOUBLE:
				Value constVal = constantNode.getConstantValue();

				if (constVal instanceof IntegerValue) {
					result = modelFactory.realLiteralExpression(source,
							BigDecimal.valueOf(((IntegerValue) constVal).getIntegerValue().doubleValue()));
				} else if (constVal instanceof RealFloatingValue) {
					result = modelFactory.realLiteralExpression(source,
							BigDecimal.valueOf(((RealFloatingValue) constVal).getDoubleValue()));
				} else {
					// Original default solution
					String doubleString = constantNode.getStringRepresentation();

					if (doubleString.endsWith("l") || doubleString.endsWith("L") || doubleString.endsWith("f")
							|| doubleString.endsWith("F")) {
						doubleString = doubleString.substring(0, doubleString.length() - 1);
					}
					result = modelFactory.realLiteralExpression(source,
							BigDecimal.valueOf(Double.parseDouble(doubleString)));
				}
				break;
			case BOOL:
				boolean value;

				if (constantNode instanceof IntegerConstantNode) {
					BigInteger integerValue = ((IntegerConstantNode) constantNode).getConstantValue().getIntegerValue();

					if (integerValue.intValue() == 0) {
						value = false;
					} else {
						value = true;
					}
				} else {
					value = Boolean.parseBoolean(constantNode.getStringRepresentation());
				}
				result = modelFactory.booleanLiteralExpression(source, value);
				break;
			case CHAR:
			case UNSIGNED_CHAR:
				return translateCharacterValue(source, constantNode);
			default:
				throw new CIVLUnimplementedFeatureException("type " + convertedType, source);
			}
			break;
		}
		case ENUMERATION:
			if (constantNode instanceof EnumerationConstantNode) {
				BigInteger value = ((IntegerValue) ((EnumerationConstantNode) constantNode).getConstantValue())
						.getIntegerValue();

				result = modelFactory.integerLiteralExpression(source, value);
			} else
				result = modelFactory.integerLiteralExpression(source,
						BigInteger.valueOf(Long.parseLong(constantNode.getStringRepresentation())));
			break;
		case POINTER:
		case ARRAY:
			boolean isSupportedChar = false;

			if (constantNode.getStringRepresentation().equals("0")) {
				result = modelFactory.nullPointerExpression(typeFactory.pointerType(typeFactory.voidType()), source);
				break;
			} else if (convertedType.kind() == TypeKind.POINTER && constantNode instanceof IntegerConstantNode) {
				result = modelFactory.integerLiteralExpression(source,
						((IntegerConstantNode) constantNode).getConstantValue().getIntegerValue());
				break;
			} else if (constantNode instanceof StringLiteralNode) {
				Type elementType = null;

				if (convertedType.kind() == TypeKind.POINTER) {
					elementType = ((PointerType) convertedType).referencedType();
				} else {// convertedType.kind() == ARRAY
					elementType = ((ArrayType) convertedType).getElementType();
				}
				if (elementType.kind() == TypeKind.QUALIFIED) {
					elementType = ((QualifiedObjectType) elementType).getBaseType();
				}
				if (elementType != null && elementType.kind() == TypeKind.BASIC) {
					if (((StandardBasicType) elementType).getBasicTypeKind() == BasicTypeKind.CHAR)
						isSupportedChar = true;
				}
				if (isSupportedChar) {
					StringLiteralNode stringLiteralNode = (StringLiteralNode) constantNode;
					StringLiteral stringValue = stringLiteralNode.getConstantValue().getLiteral();
					CIVLType type = translateABCType(modelFactory.sourceOf(stringLiteralNode), scope,
							stringLiteralNode.getInitialType());

					assert type.isArrayType() && ((CIVLArrayType) type).isComplete();

					CIVLCompleteArrayType arrTy = (CIVLCompleteArrayType) type;
					SymbolicUniverse universe = modelFactory.universe();

					assert arrTy.hasConstantLength();
					arrTy.extent().calculateConstantValue(universe);

					CompoundLiteralExpression compoundLitExpr = modelFactory.compoundLiteralExpression(source, scope,
							arrTy, true);
					SymbolicExpression constVal = universe.constantArray(universe.characterType(),
							(NumericExpression) arrTy.extent().constantValue(), universe.zeroInt());
					int numChars = stringValue.getNumCharacters();

					for (int i = 0; i < numChars; i++) {
						for (char c : stringValue.getCharacter(i).getCharacters()) {
							constVal = universe.arrayWrite(constVal, universe.integer(i), universe.character(c));
						}
					}
					// TODO denseArrayWrite on concrete arrays
					// should be simplified to the same as writing
					// elements one-by-one.
					compoundLitExpr.setLiteralConstantValue(constVal);
					result = compoundLitExpr;
					break;
				}
			}
		default:
			throw new CIVLUnimplementedFeatureException("type " + convertedType, source);
		}
		return result;
	}

	private Expression translateCharacterValue(CIVLSource source, ConstantNode constantNode) {
		Value constValue = constantNode.getConstantValue();
		Type convertedType = constantNode.getConvertedType();
		ConstantKind constKind = constantNode.constantKind();
		char[] charValues;
		BigInteger intValues;
		Expression result;

		if (constKind.equals(ConstantKind.CHAR)) {
			try {
				charValues = ((CharacterValue) constValue).getCharacter().getCharacters();
				if (charValues.length == 0)
					return modelFactory.charLiteralExpression(source,
							(char) ((CharacterValue) constValue).getCharacter().getCodePoint());
				assert (charValues.length == 1) : constValue + " is not belong to execution characters set\n";
			} catch (ClassCastException e) {
				throw new CIVLInternalException("CHAR Constant value casting failed\n", source);
			}
		} else if (constKind.equals(ConstantKind.INT)) {
			try {
				// TODO: what about signed char which allows assigned by
				// negative int objects ?
				intValues = ((IntegerValue) constValue).getIntegerValue();
				if (intValues.intValue() < 0 || intValues.intValue() > 255)
					throw new CIVLUnimplementedFeatureException(
							"Converting integer whose value is larger than UCHAR_MAX or is less than UCHAR_MIN to char type\n");
				charValues = new char[1];
				charValues[0] = (char) intValues.intValue();
			} catch (ClassCastException e) {
				throw new CIVLInternalException("INT Constant value casting failed\n", source);
			}
		} else
			throw new CIVLSyntaxException(source.getSummary(true) + " to " + convertedType.toString());

		result = modelFactory.charLiteralExpression(source, charValues[0]);
		return result;
	}

	/**
	 * Translate a struct field reference from the CIVL AST to the CIVL model.
	 * 
	 * @param dotNode The dot node to be translated.
	 * @param scope   The (static) scope containing the expression.
	 * @return The model representation of the expression.
	 */
	private Expression translateDotNode(DotNode dotNode, Scope scope) {
		Expression struct = translateExpressionNode(dotNode.getStructure(), scope, true);
		CIVLSource source = modelFactory.sourceOf(dotNode);
		Field[] navseq = dotNode.getNavigationSequence();
		Expression result = struct;

		// s.f really means (((s.f0).f1).f2). ... .fn
		for (Field field : navseq)
			result = modelFactory.dotExpression(source, result, field.getMemberIndex());
		return result;
	}

	/**
	 * Translate an ExpressionNode object in the AST into a CIVL Expression object
	 * 
	 * @param expressionNode       The expression node
	 * @param scope                The scope
	 * @param translateConversions The translation conversions
	 * @return the CIVL Expression object
	 */
	protected Expression translateExpressionNode(ExpressionNode expressionNode, Scope scope,
			boolean translateConversions) {
		Expression result;

		switch (expressionNode.expressionKind()) {
		case ARRAY_LAMBDA:
			result = translateArrayLambdaNode((ArrayLambdaNode) expressionNode, scope);
			break;
		case LAMBDA:
			result = translateLambdaNode((LambdaNode) expressionNode, scope);
			break;
		case ARROW:
			result = translateArrowNode((ArrowNode) expressionNode, scope);
			break;
		case CAST:
			result = translateCastNode((CastNode) expressionNode, scope);
			break;
		case COMPOUND_LITERAL:
			result = translateCompoundLiteralNode((CompoundLiteralNode) expressionNode, scope);
			break;
		case CONSTANT:
			result = translateConstantNode(scope, (ConstantNode) expressionNode);
			break;
		case DOT:
			result = translateDotNode((DotNode) expressionNode, scope);
			break;
		case FUNCTION_CALL:
			result = translateFunctionCallExpression((FunctionCallNode) expressionNode, scope);
			break;
		case IDENTIFIER_EXPRESSION:
			result = translateIdentifierNode((IdentifierExpressionNode) expressionNode, scope);
			break;
		case OPERATOR:
			result = translateOperatorNode((OperatorNode) expressionNode, scope);
			break;
		case QUANTIFIED_EXPRESSION:
			result = translateQuantifiedExpressionNode((QuantifiedExpressionNode) expressionNode, scope);
			break;
		case REGULAR_RANGE:
			result = translateRegularRangeNode((RegularRangeNode) expressionNode, scope);
			break;
		case SCOPEOF:
			result = translateScopeofNode((ScopeOfNode) expressionNode, scope);
			break;
		case SIZEOF:
			result = translateSizeofNode((SizeofNode) expressionNode, scope);
			break;
		case RESULT:
			result = translateResultNode((ResultNode) expressionNode, scope);
			break;
		case NOTHING:
			return this.modelFactory.nothing(modelFactory.sourceOf(expressionNode));
		case WILDCARD: {
			return this.modelFactory.wildcardExpression(modelFactory.sourceOf(expressionNode), this
					.translateABCType(modelFactory.sourceOf(expressionNode), scope, expressionNode.getConvertedType()));
		}
		case EXTENDED_QUANTIFIED:
			result = translateExtendedQuantifiedExpression((ExtendedQuantifiedExpressionNode) expressionNode, scope);
			break;
		default:
			throw new CIVLUnimplementedFeatureException("expressions of kind " + expressionNode.expressionKind(),
					modelFactory.sourceOf(expressionNode));
		}
		if (translateConversions) {
			result = applyConversions(scope, expressionNode, result);
		}
		return result;
	}

	private Expression translateExtendedQuantifiedExpression(ExtendedQuantifiedExpressionNode expressionNode,
			Scope scope) {
		CIVLSource source = modelFactory.sourceOf(expressionNode);
		Expression lower = translateExpressionNode(expressionNode.lower(), scope, true),
				higher = translateExpressionNode(expressionNode.higher(), scope, true),
				function = translateExpressionNode(expressionNode.function(), scope, true);

		assert function.getExpressionType().isFunction();
		return modelFactory.extendedQuantifiedExpression(source,
				((CIVLFunctionType) function.getExpressionType()).returnType(), expressionNode.extQuantifier(), lower,
				higher, function);
	}

	/**
	 * translates the bound variable declaration with (optional) domains in to CIVL
	 * representation.
	 * 
	 * @param boundVariableSeqNode the sequence node of bound variable declarations
	 *                             and domains (optional)
	 * @param scope                the scope of this node
	 * @return the list of variables and their (optional) domains
	 */
	private List<Pair<List<Variable>, Expression>> translateBoundVaraibleSequence(
			SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableSeqNode,
			Scope scope) {
		List<Pair<List<Variable>, Expression>> boundVariableList = new LinkedList<>();

		for (PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode> variableDeclSubList : boundVariableSeqNode) {
			List<Variable> variableSubList = new LinkedList<>();
			Expression domain = null;

			for (VariableDeclarationNode variableNode : variableDeclSubList.getLeft()) {
				Variable variable = this.translateVariableDeclarationNodeWork(variableNode, scope, true).left;

				functionInfo.addBoundVariable(variable);
				variableSubList.add(variable);
			}
			if (variableDeclSubList.getRight() != null)
				domain = this.translateExpressionNode(variableDeclSubList.getRight(), scope, true);
			boundVariableList.add(new Pair<List<Variable>, Expression>(variableSubList, domain));
		}
		return boundVariableList;
	}

	/**
	 * translates an array lambda node into an array lambda expression
	 * 
	 * @param lambdaNode the array lambda node to be translated
	 * @param scope      the current scope of the array lambda node
	 * @return the array lambda expression resulting from the translation of the
	 *         given array lambda node
	 */
	private LambdaExpression translateLambdaNode(LambdaNode lambdaNode, Scope scope) {
		VariableDeclarationNode freeVarDecl = lambdaNode.freeVariable();
		ExpressionNode lambdaFunction = lambdaNode.lambdaFunction();
		CIVLSource civlsource = modelFactory.sourceOf(lambdaNode);
		Scope lambdaScope = modelFactory.scope(civlsource, scope, Arrays.asList(), scope.function());

		functionInfo.addBoundVariableSet();

		Variable freeVar = translateVariableDeclarationNode(freeVarDecl, scope).left;

		functionInfo.addBoundVariable(freeVar);

		Expression lambdaFunctionExpr = translateExpressionNode(lambdaFunction, lambdaScope, true);
		CIVLType freeVarType[] = { freeVar.type() };

		functionInfo.popBoundVariableStackNew();
		return modelFactory.lambdaExpression(civlsource,
				typeFactory.functionType(lambdaFunctionExpr.getExpressionType(), freeVarType), freeVar,
				lambdaFunctionExpr);
	}

	/**
	 * translates an array lambda node into an array lambda expression
	 * 
	 * @param arrayLambdaNode the array lambda node to be translated
	 * @param scope           the current scope of the array lambda node
	 * @return the array lambda expression resulting from the translation of the
	 *         given array lambda node
	 */
	private ArrayLambdaExpression translateArrayLambdaNode(ArrayLambdaNode arrayLambdaNode, Scope scope) {
		ArrayLambdaExpression result;
		TypeNode arrayTypeNode = arrayLambdaNode.type();
		CIVLArrayType arrayType;
		Expression bodyExpression;
		CIVLSource source = modelFactory.sourceOf(arrayLambdaNode.getSource());
		Expression restriction = null;
		List<Pair<List<Variable>, Expression>> boundVariableList;
		CIVLType type = this.translateABCType(modelFactory.sourceOf(arrayTypeNode), scope, arrayTypeNode.getType());

		if (!type.isArrayType()) {
			throw new CIVLInternalException("unreachable: non-array-type array lambdas", source);
		}
		arrayType = (CIVLArrayType) type;
		functionInfo.addBoundVariableSet();
		boundVariableList = translateBoundVaraibleSequence(arrayLambdaNode.boundVariableList(), scope);
		if (arrayLambdaNode.restriction() != null)
			restriction = translateExpressionNode(arrayLambdaNode.restriction(), scope, true);
		else
			restriction = modelFactory.trueExpression(source);
		bodyExpression = translateExpressionNode(arrayLambdaNode.expression(), scope, true);
		result = modelFactory.arrayLambdaExpression(source, arrayType, boundVariableList, restriction, bodyExpression);
		functionInfo.popBoundVariableStackNew();
		return result;
	}

	/**
	 * creates an anonymous and const variable in the root scope for an array
	 * literal or array lambda.
	 * 
	 * @param array the array literal or labmda expression
	 * @return a variable expression wrapping the new anonymous variable
	 */
	private VariableExpression createAnonymousVariableForArrayLiteralOrArrayLambda(Scope scope, Expression array) {
		CIVLSource source = array.getSource();
		CIVLArrayType arrayType = (CIVLArrayType) array.getExpressionType();
		VariableExpression anonVariable;
		boolean isStringLiteral = false;

		if (array instanceof CompoundLiteralExpression)
			isStringLiteral = ((CompoundLiteralExpression) array).isStringLiteral();
		array.calculateConstantValue(modelFactory.universe());
		if (isStringLiteral) {
			assert array.hasConstantValue();
			anonVariable = modelFactory.variableExpression(source,
					modelFactory.newAnonymousVariableForConstantArrayLiteral(source, arrayType, array.constantValue()));
		} else {
			anonVariable = modelFactory.variableExpression(source,
					modelFactory.newAnonymousVariableForArrayLiteral(source, scope, arrayType));

			Statement anonAssign = modelFactory.assignStatement(source, modelFactory.location(source, scope),
					anonVariable, array, true);

			modelFactory.addAnonStatement(anonAssign);
		}
		return anonVariable;
	}

	/**
	 * Applies conversions associated with the given expression node to the given
	 * expression.
	 * 
	 * Precondition: the given expression is the CIVL representation of the given
	 * expression node before conversions.
	 * 
	 * @param scope
	 * @param expressionNode
	 * @param expression
	 * @return
	 */
	private Expression applyConversions(Scope scope, ExpressionNode expressionNode, Expression expression) {
		// apply conversions
		CIVLSource source = expression.getSource();
		int numConversions = expressionNode.getNumConversions();

		for (int i = 0; i < numConversions; i++) {
			Conversion conversion = expressionNode.getConversion(i);
			Type oldType = conversion.getOldType();
			Type newType = conversion.getNewType();
			// Arithmetic, Array, CompatibleStructureOrUnion,
			// Function, Lvalue, NullPointer, PointerBool, VoidPointer
			ConversionKind kind = conversion.conversionKind();

			switch (kind) {
			case ARITHMETIC: {
				CIVLType oldCIVLType = translateABCType(source, scope, oldType);
				CIVLType newCIVLType = translateABCType(source, scope, newType);

				// need equals on Types
				if (oldCIVLType.isIntegerType() && newCIVLType.isIntegerType()
						|| oldCIVLType.isRealType() && newCIVLType.isRealType()) {
					// nothing to do
				} else if (oldCIVLType.isCharType() && newCIVLType.isBoolType()) {
					// "(bool) c" where c has char type can always be
					// converted to "c != NUL", where NUL is the character
					// of value 0 defined in ASCII.
					char NUL = 0;

					expression = modelFactory.binaryExpression(expression.getSource(), BINARY_OPERATOR.NOT_EQUAL,
							expression, modelFactory.charLiteralExpression(expression.getSource(), NUL));
				} else {
					// Sometimes the conversion might have been done during
					// the translating the expression node, for example,
					// when translating a constant node, so only create a
					// cast expression if necessary.
					if (!expression.getExpressionType().equals(newCIVLType))
						expression = modelFactory.castExpression(source, newCIVLType, expression);
				}
				break;
			}
			case ARRAY: {
				Expression.ExpressionKind expressionKind = expression.expressionKind();

				if (expression instanceof LHSExpression) {
					expression = modelFactory.addressOfExpression(source,
							modelFactory.subscriptExpression(source, (LHSExpression) expression,
									modelFactory.integerLiteralExpression(source, BigInteger.ZERO)));
				} else if (expressionKind == Expression.ExpressionKind.COMPOUND_LITERAL
						|| expressionKind == Expression.ExpressionKind.ARRAY_LAMBDA) {
					// creates anonymous variable in the root scope for this
					// literal
					// and return the address to this variable
					VariableExpression anonVariable = createAnonymousVariableForArrayLiteralOrArrayLambda(scope,
							expression);

					expression = arrayToPointer(anonVariable);
					expression.setErrorFree(true);
				}
				break;
			}
			case COMPATIBLE_POINTER:// nothing to do
				break;
			case COMPATIBLE_STRUCT_UNION: {
				// This variable only used in java assertions so that the
				// assertion can save one call to translateABCType.
				CIVLType oldCIVLType;

				assert (oldCIVLType = translateABCType(source, scope, oldType)).equals(
						translateABCType(source, scope, oldType)) && oldCIVLType.equals(expression.getExpressionType());
				// The C11 Section 6.2.7 states following about 2 types have
				// compatible type:
				/*
				 * Two types have compatible type if their types are the same. Moreover, two
				 * structure, union, or enumerated types declared in separate translation units
				 * are compatible if their tags and members satisfy the following requirements:
				 * If one is declared with a tag, the other shall be declared with the same tag.
				 * If both are completed anywhere within their respective translation units,
				 * then the following additional requirements apply: there shall be a one-to-one
				 * correspondence between their members such that each pair of corresponding
				 * members are declared with compatible types; if one member of the pair is
				 * declared with an alignment specifier, the other is declared with an
				 * equivalent alignment specifier; and if one member of the pair is declared
				 * with a name, the other is declared with the same name.
				 */
				// According to above, any case that two types, which have
				// compatible type, have different CIVLTypes ? TODO: I think
				// no (ziqing)
				break;
			}
			case FUNCTION:
				if (expression instanceof LHSExpression) {
					expression = modelFactory.addressOfExpression(source, (LHSExpression) expression);
				}
				break;
			case LVALUE:
				break;
			case MEM:
				expression = modelFactory.castExpression(source, typeFactory.civlMemType(), expression);
				break;
			case NULL_POINTER: {
				// result is a null pointer to new type
				CIVLType tmpType = translateABCType(source, scope, newType);
				CIVLPointerType newCIVLType = (CIVLPointerType) tmpType;

				expression = modelFactory.nullPointerExpression(newCIVLType, source);
				break;
			}
			case POINTER_BOOL: {
				// pointer type to boolean type: p!=NULL
				expression = modelFactory.binaryExpression(source, BINARY_OPERATOR.NOT_EQUAL, expression,
						modelFactory.nullPointerExpression((CIVLPointerType) expression.getExpressionType(), source));
				break;
			}
			case REG_RANGE_DOMAIN: {
				// $range -> $domain(1)
				expression = modelFactory.recDomainLiteralExpression(source, Arrays.asList(expression),
						typeFactory.completeDomainType(expression.getExpressionType(), 1));
				break;
			}
			case POINTER_INTEGER: {
				expression = modelFactory.castExpression(source, this.typeFactory.integerType(), expression);
				break;
			}
			// case INTEGER_POINTER:{
			//
			// }
			case VOID_POINTER:
				// void*->T* or T*->void*
				// ignore, pointer types are all the same
				// all pointer types are using the same symbolic object type
				break;
			case INTEGER_POINTER: {
				expression = modelFactory.castExpression(source,
						this.translateABCType(source, scope, conversion.getNewType()), expression);
				break;
			}
			default:
				throw new CIVLUnimplementedFeatureException("applying " + conversion + " conversion", source);
			}

			// if (conversion instanceof ArithmeticConversion) {
			// CIVLType oldCIVLType = translateABCType(source, scope, oldType);
			// CIVLType newCIVLType = translateABCType(source, scope, newType);
			//
			// // need equals on Types
			// if (oldCIVLType.isIntegerType() && newCIVLType.isIntegerType()
			// || oldCIVLType.isRealType() && newCIVLType.isRealType()) {
			// // nothing to do
			// } else {
			// // Sometimes the conversion might have been done during
			// // the translating the expression node, for example,
			// // when translating a constant node, so only create a
			// // cast expression if necessary.
			// if (!expression.getExpressionType().equals(newCIVLType))
			// expression = modelFactory.castExpression(source,
			// newCIVLType, expression);
			// }
			// } else if (conversion instanceof CompatiblePointerConversion) {
			// // nothing to do
			// } else if (conversion instanceof ArrayConversion) {
			// if (expressionNode.expressionKind() == ExpressionKind.OPERATOR
			// && ((OperatorNode) expressionNode).getOperator() ==
			// Operator.SUBSCRIPT) {
			// // we will ignore this one here because we want
			// // to keep it as array in subscript expressions
			// } else if (expression.expressionKind() ==
			// Expression.ExpressionKind.ADDRESS_OF
			// || expression.expressionKind() ==
			// Expression.ExpressionKind.ARRAY_LITERAL) {
			// // FIXME: Not sure why this needs to be checked...
			// } else {
			// assert expression instanceof LHSExpression;
			// expression = modelFactory.addressOfExpression(source,
			// modelFactory.subscriptExpression(source,
			// (LHSExpression) expression, modelFactory
			// .integerLiteralExpression(source,
			// BigInteger.ZERO)));
			// }
			//
			// } else if (conversion instanceof
			// CompatibleStructureOrUnionConversion) {
			// // think about this
			// throw new CIVLUnimplementedFeatureException(
			// "compatible structure or union conversion", source);
			// } else if (conversion instanceof FunctionConversion) {
			// } else if (conversion instanceof LvalueConversion) {
			// // nothing to do since ignore qualifiers anyway
			// } else if (conversion instanceof NullPointerConversion) {
			// // result is a null pointer to new type
			// CIVLPointerType newCIVLType = (CIVLPointerType) translateABCType(
			// source, scope, newType);
			//
			// expression = modelFactory.nullPointerExpression(newCIVLType,
			// source);
			// } else if (conversion instanceof PointerBoolConversion) {
			// // pointer type to boolean type: p!=NULL
			// expression = modelFactory.binaryExpression(source,
			// BINARY_OPERATOR.NOT_EQUAL, expression, modelFactory
			// .nullPointerExpression(
			// (CIVLPointerType) expression
			// .getExpressionType(), source));
			// } else if (conversion instanceof VoidPointerConversion) {
			// // void*->T* or T*->void*
			// // ignore, pointer types are all the same
			// // all pointer types are using the same symbolic object type
			// } else if (conversion instanceof RegularRangeToDomainConversion)
			// {
			// expression = modelFactory.recDomainLiteralExpression(
			// source,
			// Arrays.asList(expression),
			// typeFactory.completeDomainType(
			// expression.getExpressionType(), 1));
			// } else
			// throw new CIVLInternalException("Unknown conversion: "
			// + conversion, source);
		}
		return expression;
	}

	private Expression translateRegularRangeNode(RegularRangeNode rangeNode, Scope scope) {
		CIVLSource source = modelFactory.sourceOf(rangeNode);
		Expression low = this.translateExpressionNode(rangeNode.getLow(), scope, true);
		Expression high = this.translateExpressionNode(rangeNode.getHigh(), scope, true);
		Expression step;
		ExpressionNode stepNode = rangeNode.getStep();

		if (stepNode != null)
			step = this.translateExpressionNode(stepNode, scope, true);
		else
			step = modelFactory.integerLiteralExpression(source, BigInteger.ONE);
		return modelFactory.regularRangeExpression(source, low, high, step);
	}

	private Expression translateScopeofNode(ScopeOfNode expressionNode, Scope scope) {
		ExpressionNode argumentNode = expressionNode.expression();
		Expression argument = translateExpressionNode(argumentNode, scope, true);
		CIVLSource source = modelFactory.sourceOf(expressionNode);

		if (!(argument instanceof LHSExpression))
			throw new CIVLInternalException("expected LHS expression, not " + argument,
					modelFactory.sourceOf(argumentNode));
		return modelFactory.scopeofExpression(source, (LHSExpression) argument);
	}

	/**
	 * A function call used as an expression. At present, this should only happen
	 * when the function is an abstract function.
	 * 
	 * @param callNode The AST representation of the function call.
	 * @param scope    The scope containing this expression.
	 * @return The model representation of the function call expression.
	 */
	protected Expression translateFunctionCallExpression(FunctionCallNode callNode, Scope scope) {
		Expression result;
		ExpressionNode functionExpression = callNode.getFunction();
		Function callee;
		CIVLFunction civlFunction;
		CIVLSource source = modelFactory.sourceOf(callNode);

		if (functionExpression instanceof IdentifierExpressionNode) {
			callee = (Function) ((IdentifierExpressionNode) functionExpression).getIdentifier().getEntity();
		} else
			throw new CIVLUnimplementedFeatureException(
					"Function call must use identifier for now: " + functionExpression.getSource());
		civlFunction = modelBuilder.functionMap.get(callee);
		assert civlFunction != null;

		// translate actual arguments:
		List<Expression> arguments = new ArrayList<Expression>();

		for (int i = 0; i < callNode.getNumberOfArguments(); i++) {
			Expression actual = translateExpressionNode(callNode.getArgument(i), scope, true);

			actual = arrayToPointer(actual);
			arguments.add(actual);
		}
		if (civlFunction.isAbstractFunction())
			return modelFactory.abstractFunctionCallExpression(source, (AbstractFunction) civlFunction, arguments);
		if (civlFunction.isLogic() || ((civlFunction.isSystemFunction())
				&& (civlFunction.isPureFunction() || civlFunction.isStateFunction()))) {
			Fragment fragment = this.translateFunctionCallNodeAsExpressionWithnoLHS(scope, callNode, source);
			CallOrSpawnStatement callStmt = (CallOrSpawnStatement) fragment.uniqueFinalStatement();

			callStmt.setLhs(null);
			result = modelFactory.functionCallExpression(callStmt);
			return result;
		} else
			throw new CIVLUnimplementedFeatureException("Using a function call as an expression.",
					callNode.getSource());
	}

	/**
	 * Translate an IdentifierExpressionNode object from the AST into a CIVL
	 * VariableExpression object.
	 * 
	 * @param identifierNode The identifier node to be translated.
	 * @param scope          The scope of the identifier.
	 * @return The CIVL VariableExpression object corresponding to the
	 *         IdentifierExpressionNode
	 */
	protected Expression translateIdentifierNode(IdentifierExpressionNode identifierNode, Scope scope) {
		CIVLSource source = modelFactory.sourceOf(identifierNode);
		Identifier name = modelFactory.identifier(source, identifierNode.getIdentifier().name());
		Expression result;
		Variable boundVariable = functionInfo.findBoundVariable(name);

		if (boundVariable != null) {
			result = modelFactory.boundVariableExpression(source, name, boundVariable.type());
		} else if (scope.seekVariable(name) != null) {
			VariableExpression varExpression = modelFactory.variableExpression(source, scope.seekVariable(name));

			result = varExpression;
		} else if (scope.seekFunction(name) != null) {
			result = modelFactory.functionIdentifierExpression(source, scope.seekFunction(name));
		} else {
			throw new CIVLInternalException("Can't find declaration of variable " + name, source);
		}
		return result;
	}

	/**
	 * Translate an operator expression from the CIVL AST to the CIVL model.
	 * 
	 * @param operatorNode The operator expression.
	 * @param scope        The (static) scope containing the expression.
	 * @return The model representation of the expression.
	 */
	protected Expression translateOperatorNode(OperatorNode operatorNode, Scope scope) {
		CIVLSource source = modelFactory.sourceOf(operatorNode);
		Operator operator = operatorNode.getOperator();

		if (operator == Operator.SUBSCRIPT)
			return translateSubscriptNode(operatorNode, scope);
		if (operator == Operator.VALID)
			return translateValidExpression(operatorNode, scope);

		int numArgs = operatorNode.getNumberOfArguments();
		List<Expression> arguments = new ArrayList<Expression>();
		Expression result = null;
		Expression booleanArg0, booleanArg1;

		for (int i = 0; i < numArgs; i++) {
			arguments.add(translateExpressionNode(operatorNode.getArgument(i), scope, true));
		}
		if (numArgs < 1 || numArgs > 3) {
			throw new RuntimeException(
					"Unsupported number of arguments: " + numArgs + " in expression " + operatorNode);
		}
		switch (operatorNode.getOperator()) {
		case ADDRESSOF: {
			Expression operand = arguments.get(0);
			Expression.ExpressionKind operandKind = operand.expressionKind();

			if (operand instanceof FunctionIdentifierExpression)
				result = operand;
			else if (operand instanceof LHSExpression)
				result = modelFactory.addressOfExpression(source, (LHSExpression) operand);
			else if (operandKind == Expression.ExpressionKind.COMPOUND_LITERAL
					|| operandKind == Expression.ExpressionKind.ARRAY_LAMBDA) {
				VariableExpression anonVariable = createAnonymousVariableForArrayLiteralOrArrayLambda(scope, operand);

				result = modelFactory.addressOfExpression(source, anonVariable);
				result.setErrorFree(true);
			}
			break;
		}
		case BIG_O:
			result = modelFactory.unaryExpression(source, UNARY_OPERATOR.BIG_O, arguments.get(0));
			break;
		case BITAND:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.BIT_AND, arguments.get(0), arguments.get(1));
			break;
		case BITCOMPLEMENT:
			result = modelFactory.unaryExpression(source, UNARY_OPERATOR.BIT_NOT, arguments.get(0));
			break;
		case BITOR:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.BIT_OR, arguments.get(0), arguments.get(1));
			break;
		case BITXOR:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.BIT_XOR, arguments.get(0), arguments.get(1));
			break;
		case SHIFTLEFT:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.SHIFTLEFT, arguments.get(0),
					arguments.get(1));
			break;
		case SHIFTRIGHT:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.SHIFTRIGHT, arguments.get(0),
					arguments.get(1));
			break;
		case DEREFERENCE:
			Expression pointer = arguments.get(0);
			CIVLType exprType = pointer.getExpressionType();

			if (!exprType.isPointerType()) {
				pointer = this.arrayToPointer(pointer);
			}
			result = modelFactory.dereferenceExpression(source, pointer);
			break;
		case CONDITIONAL:
			try {
				booleanArg0 = modelFactory.booleanExpression(arguments.get(0));
			} catch (ModelFactoryException err) {
				throw new CIVLSyntaxException("The first argument of the conditional expression " + arguments.get(0)
						+ " is of " + arguments.get(0).getExpressionType() + "type which cannot be converted to "
						+ "boolean type.", arguments.get(0).getSource());
			}
			result = modelFactory.conditionalExpression(source, booleanArg0, arguments.get(1), arguments.get(2));
			break;
		case DIV:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.DIVIDE,
					modelFactory.numericExpression(arguments.get(0)), modelFactory.numericExpression(arguments.get(1)));
			break;
		case GT:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.LESS_THAN,
					modelFactory.comparableExpression(arguments.get(1)),
					modelFactory.comparableExpression(arguments.get(0)));
			break;
		case GTE:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.LESS_THAN_EQUAL,
					modelFactory.comparableExpression(arguments.get(1)),
					modelFactory.comparableExpression(arguments.get(0)));
			break;
		case IMPLIES:
			try {
				booleanArg0 = modelFactory.booleanExpression(arguments.get(0));
			} catch (ModelFactoryException err) {
				throw new CIVLSyntaxException("The first argument of the implies expression " + arguments.get(0)
						+ " is of " + arguments.get(0).getExpressionType() + "type which cannot be converted to "
						+ "boolean type.", arguments.get(0).getSource());
			}
			try {
				booleanArg1 = modelFactory.booleanExpression(arguments.get(1));
			} catch (ModelFactoryException err) {
				throw new CIVLSyntaxException("The second argument of the implies expression " + arguments.get(1)
						+ " is of " + arguments.get(1).getExpressionType() + "type which cannot be converted to "
						+ "boolean type.", arguments.get(1).getSource());
			}
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.IMPLIES, booleanArg0, booleanArg1);
			break;
		case LAND:
			try {
				booleanArg0 = modelFactory.booleanExpression(arguments.get(0));
			} catch (ModelFactoryException err) {
				throw new CIVLSyntaxException("The first argument of the logical and expression " + arguments.get(0)
						+ " is of " + arguments.get(0).getExpressionType() + "type which cannot be converted to "
						+ "boolean type.", arguments.get(0).getSource());
			}
			try {
				booleanArg1 = modelFactory.booleanExpression(arguments.get(1));
			} catch (ModelFactoryException err) {
				throw new CIVLSyntaxException("The first argument of the logical and expression " + arguments.get(1)
						+ " is of " + arguments.get(1).getExpressionType() + "type which cannot be converted to "
						+ "boolean type.", arguments.get(1).getSource());
			}
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.AND, booleanArg0, booleanArg1);
			break;
		case LOR:
			try {
				booleanArg0 = modelFactory.booleanExpression(arguments.get(0));
			} catch (ModelFactoryException err) {
				throw new CIVLSyntaxException("The first argument of the logical or expression " + arguments.get(0)
						+ " is of " + arguments.get(0).getExpressionType() + "type which cannot be converted to "
						+ "boolean type.", arguments.get(0).getSource());
			}
			try {
				booleanArg1 = modelFactory.booleanExpression(arguments.get(1));
			} catch (ModelFactoryException err) {
				throw new CIVLSyntaxException("The first argument of the conditional expression " + arguments.get(1)
						+ " is of " + arguments.get(1).getExpressionType() + "type which cannot be converted to "
						+ "boolean type.", arguments.get(1).getSource());
			}
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.OR, booleanArg0, booleanArg1);
			break;
		case LT:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.LESS_THAN,
					modelFactory.comparableExpression(arguments.get(0)),
					modelFactory.comparableExpression(arguments.get(1)));
			break;
		case LTE:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.LESS_THAN_EQUAL,
					modelFactory.comparableExpression(arguments.get(0)),
					modelFactory.comparableExpression(arguments.get(1)));
			break;
		case MINUS:
			result = translateMinusOperation(source, modelFactory.arithmeticableExpression(arguments.get(0)),
					modelFactory.arithmeticableExpression(arguments.get(1)));
			break;
		case MOD:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.MODULO,
					modelFactory.numericExpression(arguments.get(0)), modelFactory.numericExpression(arguments.get(1)));
			break;
		case EQUALS:
		case NEQ: {
			Expression arg0 = arguments.get(0), arg1 = arguments.get(1);
			CIVLType arg0Type = arg0.getExpressionType(), arg1Type = arg1.getExpressionType();

			if (arg0Type.isNumericType() && arg1Type.isBoolType())
				arg1 = modelFactory.numericExpression(arg1);
			else if (arg0Type.isBoolType() && arg1Type.isNumericType())
				arg0 = modelFactory.numericExpression(arg0);
			result = modelFactory.binaryExpression(source,
					operatorNode.getOperator() == Operator.EQUALS ? BINARY_OPERATOR.EQUAL : BINARY_OPERATOR.NOT_EQUAL,
					arg0, arg1);
			break;
		}
		case NOT: {
			// CIVLType argType = arguments.get(0).getExpressionType();
			try {
				booleanArg0 = modelFactory.booleanExpression(arguments.get(0));
			} catch (ModelFactoryException err) {
				throw new CIVLSyntaxException("The argument of the logical not expression " + arguments.get(0)
						+ " is of " + arguments.get(0).getExpressionType() + "type which cannot be converted to "
						+ "boolean type.", arguments.get(0).getSource());
			}
			result = modelFactory.unaryExpression(source, UNARY_OPERATOR.NOT, booleanArg0);
			// if (argType.isIntegerType()) {
			// result = modelFactory.castExpression(source, argType,
			// result);
			// }
		}
			break;
		case PLUS: {
			result = translatePlusOperation(source, modelFactory.arithmeticableExpression(arguments.get(0)),
					modelFactory.arithmeticableExpression(arguments.get(1)));
			break;
		}
		case SUBSCRIPT:
			throw new CIVLInternalException("unreachable", source);
		case TIMES:
			result = modelFactory.binaryExpression(source, BINARY_OPERATOR.TIMES,
					modelFactory.numericExpression(arguments.get(0)), modelFactory.numericExpression(arguments.get(1)));
			break;
		case UNARYMINUS:
			result = modelFactory.unaryExpression(source, UNARY_OPERATOR.NEGATIVE,
					modelFactory.numericExpression(arguments.get(0)));
			break;
		case UNARYPLUS:
			result = modelFactory.numericExpression(arguments.get(0));
			break;
		default:
			throw new CIVLUnimplementedFeatureException(
					"Unsupported operator: " + operatorNode.getOperator() + " in expression " + operatorNode);
		}
		return result;
	}

	/**
	 * Translate a <code>\valid</code> expression, which is a {@link OperatorNode}
	 * who has {@link Operator#VALID}.
	 * 
	 * @param validExpression
	 * @return The translated CIVL {@link Expression}
	 */
	private Expression translateValidExpression(OperatorNode validExpression, Scope scope) {
		ExpressionNode argNode = validExpression.getArgument(0);
		ExpressionNode ptr, offsets;
		Expression ptrExpr, offsetsExpr;

		// For the argument: currently we can only handle the pattern:
		// pointer +/- (l .. h), where "(l .. h)" is optional ...
		if (argNode instanceof OperatorNode) {
			OperatorNode opNode = (OperatorNode) argNode;

			if (opNode.getOperator() != Operator.PLUS)
				throw new CIVLUnimplementedFeatureException("Translate the argument of \\valid expression:"
						+ argNode.prettyRepresentation() + ". CIVL currently only can deal with the argument"
						+ " in a specific pattern: pointer + (low .. high), " + "where '(low .. high)' is optional.",
						argNode.getSource());

			ptr = opNode.getArgument(0);
			offsets = opNode.getArgument(1);
			offsetsExpr = translateExpressionNode(offsets, scope, true);
		} else {
			ptr = argNode;
			offsetsExpr = modelFactory.integerLiteralExpression(modelFactory.sourceOf(argNode), BigInteger.ZERO);
		}
		ptrExpr = translateExpressionNode(ptr, scope, true);
		if (!ptrExpr.getExpressionType().isPointerType())
			throw new CIVLUnimplementedFeatureException("Translate the argument of \\valid expression:"
					+ argNode.prettyRepresentation() + ". CIVL currently only can deal with the argument"
					+ " in a specific pattern: pointer + (low .. high), " + "where '(low .. high)' is optional.",
					argNode.getSource());
		return modelFactory.binaryExpression(modelFactory.sourceOf(validExpression), BINARY_OPERATOR.VALID, ptrExpr,
				offsetsExpr);
	}

	/**
	 * Translate plus operation into an expression, as a helper method for
	 * {@link #translateOperatorNode(OperatorNode, Scope)}.
	 * 
	 * @param source The CIVL source of the plus operator.
	 * @param arg0   The first argument of the plus operation.
	 * @param arg1   The second argument of the plus operation.
	 * @return The CIVL expression of the plus operation.
	 */
	private Expression translatePlusOperation(CIVLSource source, Expression arg0, Expression arg1) {
		CIVLType type0 = arg0.getExpressionType();
		CIVLType type1 = arg1.getExpressionType();
		boolean isNumeric0 = type0.isNumericType() || type0.isScopeType();
		boolean isNumeric1 = type1.isNumericType() || type1.isScopeType();

		isNumeric0 |= type0.isSetType() && ((CIVLSetType) type0).elementType().isNumericType();
		isNumeric1 |= type1.isSetType() && ((CIVLSetType) type1).elementType().isNumericType();
		if (isNumeric0 && isNumeric1) {
			return modelFactory.binaryExpression(source, BINARY_OPERATOR.PLUS, arg0, arg1);
		} else {
			Expression pointer, offset;

			if (isNumeric1) {
				pointer = arrayToPointer(arg0);
				offset = arg1;
			} else if (isNumeric0) {
				pointer = arrayToPointer(arg1);
				offset = arg0;
			} else
				throw new CIVLInternalException("Expected at least one numeric argument", source);
			assert !pointer.getExpressionType().isSetType()
					|| ((CIVLSetType) pointer.getExpressionType()).elementType().isPointerType()
					: "arrays plus integers is not allowed";
			return modelFactory.binaryExpression(source, BINARY_OPERATOR.POINTER_ADD, pointer, offset);
		}
	}

	/**
	 * Translate plus operation into an expression, as a helper method for
	 * {@link #translateOperatorNode(OperatorNode, Scope)}.
	 * 
	 * @param source The CIVL source of the minus operator.
	 * @param arg0   The first argument of the minus operation.
	 * @param arg1   The second argument of the minus operation.
	 * @return The CIVL expression of the minus operation.
	 */
	private Expression translateMinusOperation(CIVLSource source, Expression arg0, Expression arg1) {
		CIVLType type0 = arg0.getExpressionType();
		CIVLType type1 = arg1.getExpressionType();
		boolean isNumeric0 = type0.isNumericType() || type0.isScopeType();
		boolean isNumeric1 = type1.isNumericType() || type1.isScopeType();

		if (isNumeric0 && isNumeric1) {
			return modelFactory.binaryExpression(source, BINARY_OPERATOR.MINUS, arg0, arg1);
		} else {
			Expression pointer, rightOperand;// , offset;

			pointer = arrayToPointer(arg0);
			rightOperand = arrayToPointer(arg1);
			if (!pointer.getExpressionType().isPointerType())
				throw new CIVLInternalException("Expected expression of pointer type", pointer.getSource());
			return modelFactory.binaryExpression(source, BINARY_OPERATOR.POINTER_SUBTRACT, pointer, rightOperand);
		}
	}

	/**
	 * Translate a QuantifiedExpressionNode from AST into a CIVL Quantified
	 * expression
	 * 
	 * @param quantifiedNode The quantified expression node
	 * @param scope          The scope
	 * @return the CIVL QuantifiedExpression
	 */
	protected Expression translateQuantifiedExpressionNode(QuantifiedExpressionNode quantifiedNode, Scope scope) {
		QuantifiedExpression result;
		Quantifier quantifier;
		Expression bodyExpression;
		CIVLSource source = modelFactory.sourceOf(quantifiedNode.getSource());
		Expression restriction = null;
		List<Pair<List<Variable>, Expression>> boundVariableList;

		functionInfo.addBoundVariableSet();
		boundVariableList = translateBoundVaraibleSequence(quantifiedNode.boundVariableList(), scope);
		switch (quantifiedNode.quantifier()) {
		case EXISTS:
			quantifier = Quantifier.EXISTS;
			break;
		case FORALL:
			quantifier = Quantifier.FORALL;
			break;
		default:
			throw new CIVLUnimplementedFeatureException("quantifier " + quantifiedNode.quantifier(), source);
		}
		if (quantifiedNode.restriction() != null)
			restriction = translateExpressionNode(quantifiedNode.restriction(), scope, true);
		else
			restriction = modelFactory.trueExpression(source);
		bodyExpression = modelFactory
				.booleanExpression(translateExpressionNode(quantifiedNode.expression(), scope, true));
		result = modelFactory.quantifiedExpression(source, quantifier, boundVariableList, restriction, bodyExpression);
		functionInfo.popBoundVariableStackNew();
		return result;
	}

	/**
	 * Translate a SizeofNode object from AST into a CIVL expression object
	 * 
	 * @param sizeofNode The size of node
	 * @param scope      The scope
	 * @return the CIVL Sizeof expression
	 */
	private Expression translateSizeofNode(SizeofNode sizeofNode, Scope scope) {
		SizeableNode argNode = sizeofNode.getArgument();
		CIVLSource source = modelFactory.sourceOf(sizeofNode);
		Expression result;

		switch (argNode.nodeKind()) {
		case TYPE:
			TypeNode typeNode = (TypeNode) argNode;
			CIVLType type = translateABCType(modelFactory.sourceOf(typeNode), scope, typeNode.getType());

			result = modelFactory.sizeofTypeExpression(source, type);
			break;
		case EXPRESSION:
			Expression argument = translateExpressionNode((ExpressionNode) argNode, scope, true);

			result = modelFactory.sizeofExpressionExpression(source, argument);
			break;
		default:
			throw new CIVLInternalException("Unknown kind of SizeofNode: " + sizeofNode, source);
		}
		return result;
	}

	private Expression ensureIntType(Expression expr) {
		CIVLType type = expr.getExpressionType();
		if (type.isIntegerType())
			return expr;
		if (type.isBoolType() || type.isEnumerationType())
			return modelFactory.castExpression(expr.getSource(), typeFactory.integerType(), expr);
		throw new CIVLInternalException("Unable to convert expression to int type", expr.getSource());
	}

	/**
	 * Translates an AST subscript node e1[e2] to a CIVL expression. The result will
	 * either be a CIVL subscript expression (if e1 has array type) or a CIVL
	 * expression of the form *(e1+e2) or *(e2+e1).
	 * 
	 * @param subscriptNode an AST node with operator SUBSCRIPT
	 * @param scope         scope in which this expression occurs
	 * @return the equivalent CIVL expression
	 */
	private Expression translateSubscriptNode(OperatorNode subscriptNode, Scope scope) {
		CIVLSource source = modelFactory.sourceOf(subscriptNode);
		ExpressionNode leftNode = subscriptNode.getArgument(0);
		ExpressionNode rightNode = subscriptNode.getArgument(1);
		Expression lhs = translateExpressionNode(leftNode, scope, false);
		Expression rhs = translateExpressionNode(rightNode, scope, true);
		CIVLType lhsType = lhs.getExpressionType();
		Expression result;

		if (lhsType.isSetType())
			lhsType = ((CIVLSetType) lhsType).elementType();
		if (lhsType.isArrayType()) {
			if (!rhs.getExpressionType().isSetTypeOf(typeFactory.integerType()))
				rhs = ensureIntType(rhs);
			if (!(lhs instanceof LHSExpression)) {
				Expression.ExpressionKind lhsKind = lhs.expressionKind();

				if (lhsKind == Expression.ExpressionKind.COMPOUND_LITERAL
						|| lhsKind == Expression.ExpressionKind.ARRAY_LAMBDA)
					lhs = this.createAnonymousVariableForArrayLiteralOrArrayLambda(scope, lhs);
				else
					throw new CIVLInternalException(
							"Expected expression with array type to be LHS or array literal or array lambda",
							lhs.getSource());
			}
			result = modelFactory.subscriptExpression(source, (LHSExpression) lhs, rhs);
		} else {
			Expression pointerExpr, indexExpr;
			if (lhsType.isPointerType()) {
				if (!rhs.getExpressionType().isSetTypeOf(typeFactory.integerType()))
					rhs = ensureIntType(rhs);
				pointerExpr = lhs;
				indexExpr = rhs;
			} else
				throw new CIVLInternalException("Expected one argument of integer type and one of pointer type",
						source);
			result = modelFactory.dereferenceExpression(source,
					modelFactory.binaryExpression(source, BINARY_OPERATOR.POINTER_ADD, pointerExpr, indexExpr));
		}
		return result;
	}

	/*
	 * *********************************************************************
	 * Translating ABC Type into CIVL Type
	 * *********************************************************************
	 */

	/**
	 * Translate the extent of an array type to an expression
	 * 
	 * @param source    The CIVL source
	 * @param arrayType The array type
	 * @param scope     The scope
	 * @return the expression representing the extent
	 */
	private Expression arrayExtent(CIVLSource source, ArrayType arrayType, Scope scope) {
		Expression result;
		if (arrayType.isComplete()) {
			ExpressionNode variableSize = arrayType.getVariableSize();

			if (variableSize != null) {
				result = translateExpressionNode(variableSize, scope, true);
			} else {
				IntegerValue constantSize = arrayType.getConstantSize();

				if (constantSize != null)
					result = modelFactory.integerLiteralExpression(source, constantSize.getIntegerValue());
				else
					throw new CIVLInternalException(
							"Complete array type has neither constant size nor variable size: " + arrayType, source);
			}
		} else
			result = null;
		return result;
	}

	// /**
	// * Calculate the index of a field in a struct type
	// *
	// * @param fieldIdentifier
	// * The identifier of the field
	// * @return The index of the field
	// */
	// private int getFieldIndex(IdentifierNode fieldIdentifier) {
	// Entity entity = fieldIdentifier.getEntity();
	// EntityKind kind = entity.getEntityKind();
	//
	// if (kind == EntityKind.FIELD) {
	// Field field = (Field) entity;
	//
	// return field.getMemberIndex();
	// } else {
	// throw new CIVLInternalException(
	// "getFieldIndex given identifier that does not correspond to field: ",
	// modelFactory.sourceOf(fieldIdentifier));
	// }
	// }

	/**
	 * Translate ABC basic types into CIVL types
	 * 
	 * @param source    The CIVL source
	 * @param basicType The basic ABC type
	 * @return CIVL type
	 */
	private CIVLType translateABCBasicType(CIVLSource source, StandardBasicType basicType) {
		switch (basicType.getBasicTypeKind()) {
		case SIGNED_CHAR:
		case SHORT:
		case UNSIGNED_SHORT:
		case INT:
		case UNSIGNED:
		case LONG:
		case UNSIGNED_LONG:
		case LONG_LONG:
		case UNSIGNED_LONG_LONG:
			return typeFactory.integerType();
		case FLOAT:
		case DOUBLE:
		case LONG_DOUBLE:
		case REAL:
			return typeFactory.realType();
		case BOOL:
			return typeFactory.booleanType();
		case CHAR:
		case UNSIGNED_CHAR:
			return typeFactory.charType();
		case DOUBLE_COMPLEX:
		case FLOAT_COMPLEX:
		case LONG_DOUBLE_COMPLEX:
		default:
			throw new CIVLUnimplementedFeatureException("types of kind " + basicType.getBasicTypeKind(), source);
		}
	}

	/**
	 * Translate ABC struct or union type into CIVL type
	 * 
	 * @param source      The CIVL source
	 * @param scope       The scope
	 * @param dynamicType The ABC struct or union type
	 * @return CIVL type of struct or union
	 */
	private CIVLType translateABCStructureOrUnionTypeNode(CIVLSource source, Scope scope,
			StructureOrUnionTypeNode typeNode, CIVLType result) {
		StructureOrUnionType type = (StructureOrUnionType) typeNode.getType();
		String tag = type.getTag();
		int numFields;
		StructOrUnionField[] civlFields;
		CIVLStructOrUnionType structType = null;

		assert tag != null;
		if (result == null) {
			result = translateNewABCStructureOrUnionType(source, scope, type);
		}
		if (result instanceof CIVLStructOrUnionType)
			structType = (CIVLStructOrUnionType) result;

		SequenceNode<FieldDeclarationNode> fields = typeNode.getStructDeclList();

		if (fields != null && structType != null) {
			numFields = fields.numChildren();
			civlFields = new StructOrUnionField[numFields];
			for (int i = 0; i < numFields; i++) {
				Field field = type.getField(i);
				CIVLType civlFieldType = translateABCTypeNode(source, scope, fields.getSequenceChild(i).getTypeNode());
				String name = field.getName() == null ? "_f" + i : field.getName();
				Identifier identifier = modelFactory.identifier(modelFactory.sourceOf(field.getDefinition()), name);
				StructOrUnionField civlField = typeFactory.structField(identifier, civlFieldType, i,
						field.isAnonymous(), structType);

				civlFields[i] = civlField;
			}
			structType.complete(civlFields);
		}
		return result;
	}

	private CIVLType translateNewABCStructureOrUnionType(CIVLSource source, Scope scope, StructureOrUnionType type) {
		boolean isSystemType = true;
		CIVLStructOrUnionType structType = null;
		CIVLType result;
		String tag = type.getTag();

		assert tag != null;
		switch (tag) {
		case ModelConfiguration.PROC_TYPE:
			result = typeFactory.processType();
			break;
		case ModelConfiguration.HEAP_TYPE:
			result = modelBuilder.heapType;
			break;
		case ModelConfiguration.DYNAMIC_TYPE:
			result = typeFactory.dynamicType();
			break;
		case ModelConfiguration.BUNDLE_TYPE:
			result = modelBuilder.bundleType;
			break;
		case ModelConfiguration.SCOPE_TYPE:
			result = typeFactory.scopeType();
			break;
		default:
			structType = typeFactory.structOrUnionType(modelFactory.identifier(source, tag), type.isStruct());
			result = structType;
			isSystemType = false;
		}
		modelBuilder.typeMap.put(type, result);
		if (!isSystemType)
			switch (tag) {
			case ModelConfiguration.MESSAGE_TYPE:
				modelBuilder.messageType = result;
				break;
			case ModelConfiguration.QUEUE_TYPE:
				modelBuilder.queueType = result;
				break;
			case ModelConfiguration.PTHREAD_THREAD_TYPE:
				typeFactory.addSystemType(tag, result);
				break;
			case ModelConfiguration.PTHREAD_POOL:
			case ModelConfiguration.PTHREAD_GPOOL:
				structType.setHandleObjectType(true);
				typeFactory.addSystemType(tag, result);
				modelBuilder.handledObjectTypes.add(result);
				break;
			case ModelConfiguration.BARRIER_TYPE:
				structType.setHandleObjectType(true);
				typeFactory.addSystemType(tag, result);
				modelBuilder.barrierType = result;
				modelBuilder.handledObjectTypes.add(result);
				break;
			case ModelConfiguration.GBARRIER_TYPE:
				structType.setHandleObjectType(true);
				typeFactory.addSystemType(tag, result);
				modelBuilder.gbarrierType = result;
				modelBuilder.handledObjectTypes.add(result);
				break;
			case ModelConfiguration.INT_ITER_TYPE:
				typeFactory.addSystemType(tag, result);
				// result.setHandleObjectType(true);
				modelBuilder.intIterType = result;
				modelBuilder.handledObjectTypes.add(result);
				break;
			case ModelConfiguration.COMM_TYPE:
				typeFactory.addSystemType(tag, result);
				structType.setHandleObjectType(true);
				modelBuilder.commType = result;
				modelBuilder.handledObjectTypes.add(result);
				break;
			case ModelConfiguration.GCOMM_TYPE:
				typeFactory.addSystemType(tag, result);
				structType.setHandleObjectType(true);
				modelBuilder.gcommType = result;
				modelBuilder.handledObjectTypes.add(result);
				break;
			case ModelConfiguration.FILE_SYSTEM_TYPE:
				// result.setHandleObjectType(true);
				modelBuilder.basedFilesystemType = structType;
				typeFactory.addSystemType(tag, result);
				modelBuilder.handledObjectTypes.add(result);
				break;
			case ModelConfiguration.REAL_FILE_TYPE:
				modelBuilder.fileType = structType;
				typeFactory.addSystemType(tag, result);
				break;
			case ModelConfiguration.FILE_STREAM_TYPE:
				typeFactory.addSystemType(tag, result);
				modelBuilder.FILEtype = structType;
				modelBuilder.handledObjectTypes.add(result);
				break;
			case ModelConfiguration.TM_TYPE:
				// modelBuilder.handledObjectTypes.add(result);
				typeFactory.addSystemType(tag, result);
				break;
			case ModelConfiguration.FLOAT_COMPLEX_TYPE:
			case ModelConfiguration.DOUBLE_COMPLEX_TYPE:
			case ModelConfiguration.LDOUBLE_COMPLEX_TYPE:
				typeFactory.addSystemType(tag, result);
				break;

			default:
				// TODO: set default case
			}
		return result;
	}

	/**
	 * Translate ABC struct or union type into CIVL type
	 * 
	 * @param source The CIVL source
	 * @param scope  The scope
	 * @param type   The ABC struct or union type
	 * @return CIVL type of struct or union
	 */
	private CIVLType translateABCStructureOrUnionType(CIVLSource source, Scope scope, StructureOrUnionType type) {
		CIVLType result = modelBuilder.typeMap.get(type);

		if (result == null) {
			result = translateNewABCStructureOrUnionType(source, scope, type);
		}
		return result;
	}

	protected CIVLType translateABCTypeNode(CIVLSource source, Scope scope, TypeNode abcTypeNode) {
		Type abcType = abcTypeNode.getType();
		CIVLType result = modelBuilder.typeMap.get(abcType);

		if (result == null) {
			TypeNodeKind kind = abcTypeNode.kind();

			switch (kind) {
			case STRUCTURE_OR_UNION:
				// type already entered into map, so just return:
				return translateABCStructureOrUnionTypeNode(source, scope, (StructureOrUnionTypeNode) abcTypeNode,
						(CIVLStructOrUnionType) result);
			case ENUMERATION:
				return translateABCEnumerationType(source, scope, (EnumerationType) abcType);
			case POINTER: {
				PointerTypeNode pointerTypeNode = (PointerTypeNode) abcTypeNode;
				CIVLType baseType = this.translateABCTypeNode(source, scope, pointerTypeNode.referencedType());

				result = this.typeFactory.pointerType(baseType);
				this.modelBuilder.typeMap.put(abcType, result);
				break;
			}
			case ARRAY:
				ArrayTypeNode arrayTypeNode = (ArrayTypeNode) abcTypeNode;
				CIVLType elementType = translateABCTypeNode(source, scope, arrayTypeNode.getElementType());

				if (arrayTypeNode.getExtent() != null) {
					Expression extent = translateExpressionNode(arrayTypeNode.getExtent(), scope, true);

					result = typeFactory.completeArrayType(elementType, extent);
				} else
					result = typeFactory.incompleteArrayType(elementType);
				// cache
				this.modelBuilder.typeMap.put(abcType, result);
				break;
			case FUNCTION:
			case TYPEDEF_NAME:
			case BASIC:
			case SCOPE:
			case VOID:
			case RANGE:
			case DOMAIN:
			case LAMBDA:
			case MEM:
				return translateABCType(source, scope, abcType);
			case TYPEOF:
			case ATOMIC:
				throw new CIVLUnimplementedFeatureException("Type " + abcType, source);
			default:
				throw new CIVLInternalException("Unknown type: " + abcType, source);
			}
		} else {
			CIVLType.TypeKind typeKind = result.typeKind();

			switch (typeKind) {
			case STRUCT_OR_UNION: {
				if (abcTypeNode instanceof StructureOrUnionTypeNode) {
					CIVLStructOrUnionType structUnionType = (CIVLStructOrUnionType) result;
					StructureOrUnionTypeNode structUnionTypeNode = (StructureOrUnionTypeNode) abcTypeNode;

					if (structUnionType.numFields() < 1 && structUnionTypeNode.getStructDeclList() != null)
						result = this.translateABCStructureOrUnionTypeNode(source, scope, structUnionTypeNode,
								structUnionType);
				}
			}
			default:
			}
		}
		return result;
	}

	/**
	 * Working on replacing process type with this.
	 * 
	 * @param source  The CIVL source
	 * @param scope   The scope
	 * @param abcType The ABC type
	 * @return The CIVL type
	 */
	protected CIVLType translateABCType(CIVLSource source, Scope scope, Type abcType) {
		CIVLType result = modelBuilder.typeMap.get(abcType);

		if (result == null) {
			TypeKind kind = abcType.kind();

			switch (kind) {
			case ARRAY: {
				ArrayType arrayType = (ArrayType) abcType;
				CIVLType elementType = translateABCType(source, scope, arrayType.getElementType());
				Expression extent = arrayExtent(source, arrayType, scope);

				if (extent != null)
					result = typeFactory.completeArrayType(elementType, extent);
				else
					result = typeFactory.incompleteArrayType(elementType);
				break;
			}
			case BASIC:
				result = translateABCBasicType(source, (StandardBasicType) abcType);
				break;
			case HEAP:
				result = typeFactory.heapType();
				break;
			case OTHER_INTEGER:
				result = typeFactory.integerType();
				break;
			case POINTER: {
				PointerType pointerType = (PointerType) abcType;
				Type referencedType = pointerType.referencedType();
				CIVLType baseType = translateABCType(source, scope, referencedType);

				// if (baseType.isFunction())
				// result = baseType;
				// else
				result = typeFactory.pointerType(baseType);
				break;
			}
			case PROCESS:
				result = typeFactory.processType();
				break;
			case SCOPE:
				result = typeFactory.scopeType();
				break;
			case QUALIFIED:
				result = translateABCType(source, scope, ((QualifiedObjectType) abcType).getBaseType());
				break;
			case STRUCTURE_OR_UNION:
				result = translateABCStructureOrUnionType(source, scope, (StructureOrUnionType) abcType);
				// type already entered into map, so just return:
				return result;
			case VOID:
				result = typeFactory.voidType();
				break;
			case ENUMERATION:
				return translateABCEnumerationType(source, scope, (EnumerationType) abcType);
			case FUNCTION:
				return translateABCFunctionType(source, scope, (FunctionType) abcType);
			case RANGE:
				return typeFactory.rangeType();
			case MEM:
				return typeFactory.civlMemType();
			case DOMAIN:
				return translateABCDomainType(source, scope, (DomainType) abcType);
			case ATOMIC:
				throw new CIVLUnimplementedFeatureException("Type " + abcType, source);
			default:
				throw new CIVLInternalException("Unknown type: " + abcType, source);
			}
			modelBuilder.typeMap.put(abcType, result);
		}
		return result;
	}

	private CIVLType translateABCDomainType(CIVLSource source, Scope scope, DomainType domainType) {
		if (domainType.hasDimension())
			return typeFactory.completeDomainType(typeFactory.rangeType(), domainType.getDimension());
		else
			return typeFactory.domainType(typeFactory.rangeType());
	}

	/**
	 * Translates ABC function type.
	 * 
	 * @param source  The source code element to be used for error report.
	 * @param scope   The scope of the function type.
	 * @param abcType The ABC representation of the function type.
	 * @return The CIVL function type translated from the ABC function type.
	 */
	private CIVLType translateABCFunctionType(CIVLSource source, Scope scope, FunctionType abcType) {
		CIVLType returnType = translateABCType(source, scope, abcType.getReturnType());
		int numberOfParameters = abcType.getNumParameters();
		CIVLType[] paraTypes = new CIVLType[numberOfParameters];

		for (int i = 0; i < numberOfParameters; i++) {
			paraTypes[i] = translateABCType(source, scope, abcType.getParameterType(i));
		}
		return typeFactory.functionType(returnType, paraTypes);
	}

	/**
	 * Translate type node that is typedef, struct or union.
	 * <p>
	 * The method {@link CIVLType#hasState} in {@link CIVLType} will return
	 * <code>true</code> for any type which contains an array with extent which is
	 * not constant. We associate to these types a state variable that can be set
	 * and get.
	 * <p>
	 * When you come across a typedef, or complete struct or union def, construct
	 * the CIVL type <code>t</code> as usual. If <code>t.hasState()</code>, insert
	 * into the model at the current scope a variable <code>__struct_foo__</code>,
	 * <code>__union_foo__</code>, or <code>__typedef_foo__</code> of type "CIVL
	 * dynamic type". Set the state variable in <code>t</code> to this variable. At
	 * the point of definition, insert a model assignment statement,
	 * <code>__struct_foo__ = DynamicTypeOf(t)</code> (for example).
	 * 
	 * @param sourceLocation The location
	 * @param scope          The scope
	 * @param typeNode       The type node
	 * @return the fragment
	 */
	private Fragment translateCompoundTypeNode(Location sourceLocation, Scope scope, TypeNode typeNode) {
		Fragment result = null;
		String prefix;
		String tag;
		CIVLType type = translateABCTypeNode(modelFactory.sourceOf(typeNode), scope, typeNode);
		CIVLSource civlSource = modelFactory.sourceOf(typeNode);

		if (typeNode instanceof StructureOrUnionTypeNode) {
			StructureOrUnionTypeNode structOrUnionTypeNode = (StructureOrUnionTypeNode) typeNode;

			if (structOrUnionTypeNode.isStruct())
				prefix = "__struct_";
			else
				prefix = "__union_";
			// This is null if this is a "declaration" but not the
			// "definition".
			if (((StructureOrUnionTypeNode) typeNode).getStructDeclList() == null)
				return result;
			if (!(type instanceof CIVLStructOrUnionType))
				throw new CIVLInternalException("unexpected type: " + type, civlSource);
			else {
				tag = ((CIVLStructOrUnionType) type).name().name();
			}
		} else {
			prefix = "__typedef_";
			if (!(typeNode instanceof EnumerationTypeNode))
				tag = ((TypedefDeclarationNode) typeNode.parent()).getName();
			else
				tag = "";
		}
		if (type.hasState()) {
			Variable variable;
			String name = prefix + tag + "__";
			Identifier identifier = modelFactory.identifier(civlSource, name);
			int vid = scope.numVariables();
			LHSExpression lhs;
			Expression rhs = modelFactory.dynamicTypeOfExpression(civlSource, type);

			variable = modelFactory.variable(civlSource, typeFactory.dynamicType(), identifier, vid);
			lhs = modelFactory.variableExpression(civlSource, variable);
			scope.addVariable(variable);
			type.setStateVariable(variable);
			if (sourceLocation == null)
				sourceLocation = modelFactory.location(modelFactory.sourceOfBeginning(typeNode), scope);
			result = new CommonFragment(modelFactory.assignStatement(civlSource, sourceLocation, lhs, rhs, true));
		}
		return result;
	}

	private void setFunction(CIVLFunction function) {
		this.function = function;
	}
}
