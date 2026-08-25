package dev.civl.mc.model.common;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.program.IF.Program;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SourceFile;
import dev.civl.gmc.CommandLineException;
import dev.civl.gmc.GMCSection;
import dev.civl.mc.analysis.IF.Analysis;
import dev.civl.mc.analysis.IF.CodeAnalyzer;
import dev.civl.mc.config.IF.CIVLConfiguration;
import dev.civl.mc.config.IF.CIVLConstants;
import dev.civl.mc.model.IF.CIVLFunction;
import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLSyntaxException;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.model.IF.Identifier;
import dev.civl.mc.model.IF.LogicFunction;
import dev.civl.mc.model.IF.Model;
import dev.civl.mc.model.IF.ModelConfiguration;
import dev.civl.mc.model.IF.ModelFactory;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.SystemFunction;
import dev.civl.mc.model.IF.contract.CallEvent;
import dev.civl.mc.model.IF.contract.FunctionContract;
import dev.civl.mc.model.IF.expression.BinaryExpression;
import dev.civl.mc.model.IF.expression.BinaryExpression.BINARY_OPERATOR;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.expression.VariableExpression;
import dev.civl.mc.model.IF.location.Location;
import dev.civl.mc.model.IF.statement.AssignStatement;
import dev.civl.mc.model.IF.statement.AtomicLockAssignStatement;
import dev.civl.mc.model.IF.statement.CallOrSpawnStatement;
import dev.civl.mc.model.IF.statement.LoopBranchStatement;
import dev.civl.mc.model.IF.statement.MallocStatement;
import dev.civl.mc.model.IF.statement.ReturnStatement;
import dev.civl.mc.model.IF.statement.Statement;
import dev.civl.mc.model.IF.statement.Statement.StatementKind;
import dev.civl.mc.model.IF.type.CIVLArrayType;
import dev.civl.mc.model.IF.type.CIVLBundleType;
import dev.civl.mc.model.IF.type.CIVLHeapType;
import dev.civl.mc.model.IF.type.CIVLPointerType;
import dev.civl.mc.model.IF.type.CIVLStructOrUnionType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.type.StructOrUnionField;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.model.common.location.CommonLocation;
import dev.civl.mc.model.common.statement.CommonAtomicLockAssignStatement;
import dev.civl.mc.model.common.type.CommonType;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.type.SymbolicArrayType;
import dev.civl.sarl.IF.type.SymbolicType;

/**
 * Does the main work translating a single ABC Program to a model.
 * 
 * @author Stephen F. Siegel (siegel)
 * @author Manchun Zheng (zmanchun)
 * @author Timothy K. Zirkel (zirkel)
 */
public class ModelBuilderWorker {

	/* ************************** Instance Fields ************************** */

	/**
	 * Used to give names to anonymous structs and unions.
	 */
	int anonymousStructCounter = 0;

	/**
	 * Does the source file include time.h? If yes, then the time count variable and
	 * the broken time variable need to be created.
	 */
	boolean timeLibIncluded = false;

	/**
	 * Does the source file contains any function call to $next_time_count? If yes,
	 * then the time count variable needs to be created.
	 */
	boolean hasNextTimeCountCall = false;

	Map<CIVLFunction, StatementNode> parProcFunctions = new HashMap<>();

	/**
	 * A collection for translated functions from $run statements
	 */
	Map<CIVLFunction, StatementNode> runProcFunctions = new HashMap<>();

	/** Used to shortcut checking whether circular types are bundleable. */
	private List<CIVLType> bundleableEncountered = new LinkedList<>();

	/**
	 * The types that may be part of a bundle.
	 */
	private LinkedList<CIVLType> bundleableTypeList = new LinkedList<>();

	/**
	 * The unique type for a bundle.
	 */
	CIVLBundleType bundleType;

	/**
	 * Map whose key set contains all {@link CallOrSpawnStatement} whose callee is
	 * unknown in the model. The value associated to the key is the ABC
	 * {@link Function} entity. This is built up as call/spawn statements are
	 * processed. On a later pass, we iterate over this map and set the function
	 * fields of the call/spawn statements to the corresponding {@link CIVLFunction}
	 * object. Visibility make it package-private since {@link FunctionTranslator}
	 * needs to access it.
	 */
	Map<CallOrSpawnStatement, Function> callStatements;

	/**
	 * Map whose key set contains all {@link CallEvent} whose callee is unknown in
	 * the model. The value associated to the key is the ABC {@link Function}
	 * entity. This is built up as call events are processed. On a later pass, we
	 * iterate over this map and set the function fields of the call event to the
	 * corresponding {@link CIVLFunction} object. Visibility make it package-private
	 * since {@link FunctionContractTranslator} needs to access it.
	 */
	Map<CallEvent, Function> callEvents;

	/**
	 * A collection of all atomic blocks in the model. An atomic block is
	 * represented as an array of two locations: [0]: the entry location and [1]:
	 * exit location.
	 */
	List<Location[]> atomicBlocks = new LinkedList<>();

	// Map<Scope, CIVLFunction> elaborateDomainFunction = new HashMap<>();

	boolean needToAddElaborateDomainFunction = false;

	/**
	 * The unique type for a comm.
	 */
	CIVLType commType;

	/**
	 * The unique type for a gcomm
	 */
	CIVLType gcommType;

	/**
	 * The type __barrier__, which is the base type of the handle $barrier.
	 */
	CIVLType barrierType;

	/**
	 * The type __gbarrier__, which is the base type of the handle $gbarrier.
	 */
	CIVLType gbarrierType;

	/**
	 * The type __collect_record__, which is the type of an entry in a collective
	 * operation checker
	 */
	CIVLType collectRecordType;

	/**
	 * The type __gcollect_checker__, which is the type of the handle
	 * $gcollect_checker
	 */
	CIVLType gcollatorType;

	/**
	 * The type __collect_checker__, which is the type of the handle
	 * $collect_checker
	 */
	CIVLType collatorType;

	/**
	 * The type __int_iter__, which is the base type of the handle $int_iter.
	 */
	CIVLType intIterType;

	/**
	 * The base type of the pointer type $filesystem; a structure type with fields
	 * (0) scope, and (1) files. NULL if there is no IO operation.
	 */
	CIVLStructOrUnionType basedFilesystemType;

	/**
	 * The CIVL struct type $file, defined in stdio. NULL if there is no IO
	 * operation.
	 */
	CIVLStructOrUnionType fileType;

	/**
	 * The CIVL type FILE, defined in stdio. NULL if there is no IO operation.
	 */
	CIVLStructOrUnionType FILEtype;

	/**
	 * The type __omp_gws__, which is the base type of the handle $omp_gws.
	 */
	CIVLType ompGwsType;

	/**
	 * The type __omp_ws__, which is the base type of the handle $omp_ws.
	 */
	CIVLType ompWsType;

	/**
	 * Configuration information for the generic model checker.
	 */
	private GMCSection config;

	private CIVLConfiguration civlConfig;

	private boolean debugging = false;

	private PrintStream debugOut = System.out;

	/**
	 * The factory used to create new Model components.
	 */
	private ModelFactory factory;

	/**
	 * Map from ABC Function entity to corresponding CIVL Function.
	 */
	Map<Function, CIVLFunction> functionMap;

	ArrayList<CIVLType> handledObjectTypes = new ArrayList<>();

	/**
	 * The unique type for a heap.
	 */
	CIVLHeapType heapType;

	/**
	 * Set containing the names of all input variables that were initialized from a
	 * commandline argument. This is used at the end of the building process to
	 * determine if there were any command line arguments that were not used. This
	 * usually indicates an error.
	 */
	Set<String> initializedInputs = new HashSet<>();

	/**
	 * The map formed from parsing the command line for "-input" options that
	 * specifies an initial constant value for some input variables. May be null if
	 * no "-input"s appeared on the command line.
	 */
	Map<String, Object> inputInitMap;

	/**
	 * List of all malloc statements in the program.
	 */
	List<MallocStatement> mallocStatements = new ArrayList<>();

	// List<CallOrSpawnStatement> elaborateDomainCalls = new ArrayList<>();

	/**
	 * The function definition node of the main function
	 */
	FunctionDefinitionNode mainFunctionNode = null;

	/**
	 * The unique type for a message.
	 */
	CIVLType messageType;

	/**
	 * The model being constructed by this worker
	 */
	private Model model;

	/**
	 * The name of the model (i.e., core name of the cvl file)
	 */
	private String modelName;

	/**
	 * The ABC AST being translated by this model builder worker.
	 */
	protected Program program;

	/**
	 * The unique type for a queue.
	 */
	CIVLType queueType;

	/**
	 * The types that may not be part of a bundle.
	 */
	private LinkedList<CIVLType> unbundleableTypeList = new LinkedList<CIVLType>();

	/**
	 * The symbolic universe
	 */
	private SymbolicUniverse universe;

	/**
	 * This field accumulates the AST definition node of every function definition
	 * in the AST.
	 */
	ArrayList<FunctionDefinitionNode> unprocessedFunctions;

	/**
	 * The outermost scope of the model, root of the static scope tree, known as the
	 * "system scope".
	 */
	Scope rootScope;

	CIVLTypeFactory typeFactory;

	/**
	 * Mapping from ABC types to corresponding CIVL types.
	 */
	Map<Type, CIVLType> typeMap = new HashMap<Type, CIVLType>();

	CIVLFunction rootFunction;

	/**
	 * All translated {@link LogicFunction}s during model building:
	 */
	List<LogicFunction> seenLogicFunctions = new LinkedList<>();

	/* **************************** Constructors *************************** */

	/**
	 * Constructs new instance of ModelBuilderWorker.
	 * 
	 * @param config    the GMC configuration
	 * @param factory   the model factory
	 * @param program   the program
	 * @param name      name of the model, i.e. the file name without .cvl extension
	 * @param debugOut
	 * @param debugging
	 */
	public ModelBuilderWorker(GMCSection config, ModelFactory factory, Program program, String name, boolean debugging,
			PrintStream debugOut) {
		this.config = config;
		this.civlConfig = new CIVLConfiguration(config);
		this.inputInitMap = config.getMapValue(CIVLConstants.inputO);
		this.factory = factory;
		this.program = program;
		this.factory.setTokenFactory(program.getTokenFactory());
		typeFactory = factory.typeFactory();
		this.modelName = name;
		this.heapType = typeFactory.heapType(name);
		this.bundleType = typeFactory.initBundleType();
		this.universe = factory.universe();
		((CommonModelFactory) factory).modelBuilder = this;
		this.debugging = debugging;
		this.debugOut = debugOut;
	}

	/* ************************** Protected Methods ************************ */

	protected void initialization() {
		Identifier rootID = factory.identifier(factory.systemSource(), CIVLConstants.civlSystemFunction);
		CIVLSource rootFunctionSource = factory.sourceOf(program.getAST().getMain().getDefinition());
		// the order of creating the static scope and the root scope does
		// matters
		// always create the static scope first and then the root scope
		// because the static scope has id 0 while the root scope has id 1
		Scope staticScope = this.factory.scope(rootFunctionSource, null, new ArrayList<Variable>(0), null);

		rootScope = this.factory.scope(rootFunctionSource, staticScope, new ArrayList<Variable>(0), null);
		factory.setScopes(rootScope);
		rootFunction = factory.function(factory.sourceOf(program.getAST().getMain().getDefinition()), false, rootID,
				rootScope, new ArrayList<Variable>(), null, null, null);
		rootScope.setFunction(rootFunction);
		callStatements = new LinkedHashMap<>();
		callEvents = new LinkedHashMap<>();
		functionMap = new LinkedHashMap<>();
		unprocessedFunctions = new ArrayList<>();
	}

	/**
	 * Translate the function definition node of unprocessed functions, which are
	 * obtained by translating function declaration node, function call node, and so
	 * on.
	 */
	protected void translateUndefinedFunctions() {
		while (!unprocessedFunctions.isEmpty()) {
			FunctionDefinitionNode functionDefinition = unprocessedFunctions.remove(0);

			translateFunctionDefinitionNode(functionDefinition);
		}
		unprocessedFunctions.clear();
	}

	/* *************************** Private Methods ************************* */

	/**
	 * analysis of AST tree before translation, including: check if time.h is ever
	 * included, if yes, then we need to add _time_count to the root scope.
	 */
	private void preprocess() {
		ASTNode root = program.getAST().getRootNode();

		this.timeLibIncluded = this.hasTimeLibrary(root);
		this.hasNextTimeCountCall = this.hasNextTimeCountCall(root);
	}

	/**
	 * Does the AST node contains any function call node to $next_time_count()? If
	 * yes, then the model will need the time count variable; otherwise, no need to
	 * create that variable.
	 * 
	 * @param node an node from the AST
	 * @return true iff the node contains a function call node to $next_time_count.
	 */
	private boolean hasNextTimeCountCall(ASTNode node) {
		for (ASTNode child : node.children()) {
			if (child == null)
				continue;
			if (child instanceof FunctionCallNode) {
				ExpressionNode functionExpr = ((FunctionCallNode) child).getFunction();

				if (functionExpr instanceof IdentifierExpressionNode) {
					if (((IdentifierExpressionNode) functionExpr).getIdentifier().name()
							.equals(ModelConfiguration.NEXT_TIME_COUNT))
						return true;
				}
			}
			if (hasNextTimeCountCall(child))
				return true;
		}
		return false;
	}

	/**
	 * Does the source file include the time library (time.h)? If yes
	 * 
	 * @param node
	 * @return
	 */
	private boolean hasTimeLibrary(ASTNode node) {
		Source source = node.getSource();
		CivlcToken token = source == null ? null : node.getSource().getFirstToken();
		SourceFile file = token == null ? null : token.getFormation().getLastFile();

		if (file != null && file.getName().equals(ModelConfiguration.TIME_LIB))
			return true;
		else {
			for (ASTNode child : node.children()) {
				if (child != null) {
					boolean childResult = this.hasTimeLibrary(child);

					if (childResult)
						return childResult;
				}
			}
		}
		return false;
	}

	private void completeHeapType() {
		completeHandleObjectTypes();
		this.completeMallocStatements();
		typeFactory.completeHeapType(heapType, mallocStatements);
	}

	private void completeHandleObjectTypes() {
		for (CIVLType handleObjectType : this.handledObjectTypes) {
			int mallocId = mallocStatements.size();

			mallocStatements.add(factory.mallocStatement(null, null, null, handleObjectType, null,
					factory.sizeofTypeExpression(null, handleObjectType), mallocId, null));
			typeFactory.addHeapFieldObjectType(handleObjectType, mallocId);
		}
	}

	/**
	 * Processes the function body of a function definition node. At least one
	 * function declaration for this function should have been processed already, so
	 * the corresponding CIVL function should already exist.
	 * 
	 * @param functionNode The function definition node in the AST to be translated.
	 * @throws CIVLInternalException if no corresponding CIVL function could be
	 *                               found.
	 */
	private void translateFunctionDefinitionNode(FunctionDefinitionNode functionNode) {
		Entity entity = functionNode.getEntity();
		CIVLFunction result;
		FunctionTranslator functionTranslator;

		result = functionMap.get(entity);
		if (result == null)
			throw new CIVLInternalException("Did not process declaration", factory.sourceOf(functionNode));
		functionTranslator = new FunctionTranslator(this, factory, functionNode.getBody(), result, this.civlConfig);
		// no return value because the result will be stored in the variable
		// "result" of CIVLFunction type.
		functionTranslator.translateFunction();
		// if (result.name().name().equals("$elaborate_rectangular_domain")) {
		// // this.elaborateDomainFunction = result;
		// this.elaborateDomainFunction.put(result.containingScope(), result);
		// }
	}

	private void translateParProcFunctions() {
		Set<CIVLFunction> checkedFunctions = new HashSet<>();
		Stack<CIVLFunction> working = new Stack<>();

		for (CIVLFunction func : this.parProcFunctions.keySet()) {
			working.push(func);
		}
		while (!working.isEmpty()) {
			CIVLFunction function = working.pop();
			StatementNode bodyNode = parProcFunctions.get(function);
			FunctionTranslator translator = new FunctionTranslator(this, factory, bodyNode, function, this.civlConfig);

			checkedFunctions.add(function);
			translator.translateFunction();
			for (CIVLFunction func : this.parProcFunctions.keySet()) {
				if (!checkedFunctions.contains(func) && !working.contains(func))
					working.push(func);
			}
		}
	}

	/**
	 * Translates anonymous function bodies which are generated from $run
	 * statements. For a translated anonymous function body, it should be appended
	 * with a "return" statement. The "return" statement should be set
	 * {@link ReturnStatement#setFromRunProcFunction(boolean)}.
	 */
	private void translateRunProcFunctions() {
		Set<CIVLFunction> checkedFunctions = new HashSet<>();
		Stack<CIVLFunction> working = new Stack<>();

		for (CIVLFunction func : runProcFunctions.keySet()) {
			working.push(func);
		}
		while (!working.isEmpty()) {
			CIVLFunction function = working.pop();
			StatementNode bodyNode = runProcFunctions.get(function);

			if (bodyNode == null)
				continue;

			FunctionTranslator translator = new FunctionTranslator(this, factory, bodyNode, function, this.civlConfig);

			checkedFunctions.add(function);
			translator.translateFunction();
			for (CIVLFunction func : this.runProcFunctions.keySet()) {
				if (!checkedFunctions.contains(func) && !working.contains(func))
					working.push(func);
			}
		}
	}

	/*
	 * *********************************************************************
	 * Post-translation Methods
	 * *********************************************************************
	 */
	/**
	 * Returns false if a type contains a bundle, void (but void* is ok), or the
	 * type is incomplete.
	 * 
	 * @param type The CIVL type to be checked
	 * @return True of False
	 */
	private boolean bundleableType(CIVLType type) {
		if (type.isVoidType())
			return false;
		if ((type.isStructType() || type.isUnionType()) && !((CIVLStructOrUnionType) type).isComplete())
			return false;
		return bundleableTypeHelper(type);
	}

	private boolean bundleableTypeHelper(CIVLType type) {
		boolean result = true;

		if (bundleableEncountered.contains(type)) {
			// We are in a recursive evaluation that has already encountered
			// this type. E.g. a struct foo with a field of type struct foo,
			// etc. If this type is not bundleable, that will be determined
			// elsewhere.
			return true;
		} else {
			bundleableEncountered.add(type);
		}
		if (type.isBundleType()) {
			result = false;
		} else if (type.isPointerType()) {
			if (((CIVLPointerType) type).baseType().isVoidType()) {
				// void* is bundleable, so catch this before checking base type
				result = true;
			} else {
				result = bundleableType(((CIVLPointerType) type).baseType());
			}
		} else if (type.isVoidType()) {
			result = false;
		} else if (type.isArrayType()) {
			result = bundleableType(((CIVLArrayType) type).elementType());
		} else if (type.isStructType()) {
			CIVLStructOrUnionType structType = (CIVLStructOrUnionType) type;

			if (!structType.isComplete())
				return false;
			for (StructOrUnionField f : ((CIVLStructOrUnionType) type).fields()) {
				result = result && bundleableType(f.type());
				if (!result)
					break;
			}
		}
		// Heaps and primitive types can be bundled.
		bundleableEncountered.remove(type);
		return result;
	}

	/**
	 * Complete the bundle type.
	 */
	protected void completeBundleType() {
		Map<SymbolicType, Integer> dynamicTypeMap = new LinkedHashMap<SymbolicType, Integer>();
		int dynamicTypeCount = 0;

		for (Entry<Type, CIVLType> entry : typeMap.entrySet()) {
			CIVLType thisType = entry.getValue();

			if (bundleableType(thisType)) {
				bundleableTypeList.add(thisType);
			} else {
				unbundleableTypeList.add(thisType);
			}
		}
		for (CIVLType type : bundleableTypeList) {
			SymbolicType dynamicType = type.getDynamicType(universe);
			Integer id = dynamicTypeMap.get(dynamicType);

			if (id == null) {
				id = dynamicTypeCount;
				dynamicTypeMap.put(dynamicType, id);
				dynamicTypeCount++;
			}
			((CommonType) type).setDynamicTypeIndex(id);
		}
		typeFactory.completeBundleType(bundleType, bundleableTypeList, dynamicTypeMap.keySet());
	}

	/**
	 * Complete the model by updating its fields according to the information
	 * obtained by the translation.
	 * 
	 * @param system The system function of the model, i.e. _CIVL_SYSTEM function.
	 */
	protected void completeModel(CIVLFunction system) {
		// boolean hasWaitall = false;

		model = factory.model(system.getSource(), system, this.program);
		model.setMessageType(messageType);
		model.setQueueType(queueType);
		model.setName(modelName);
		// add all functions to model except main:
		for (CIVLFunction f : functionMap.values()) {
			model.addFunction(f);
			// if (f.name().name().equals("$waitall"))
			// hasWaitall = true;
		}
		for (CIVLFunction f : this.parProcFunctions.keySet())
			model.addFunction(f);
		for (CIVLFunction f : this.runProcFunctions.keySet())
			model.addFunction(f);
		// if (this.parProcFunctions.size() > 0 && !hasWaitall) {
		// model.addFunction(factory.waitFunctionPointer().function());
		// }
		// if (this.elaborateDomainFunction.size() > 0
		// && needToAddElaborateDomainFunction) {
		// for (Entry<Scope, CIVLFunction> entry : elaborateDomainFunction
		// .entrySet())
		// model.addFunction(entry.getValue());
		// }
		((CommonModel) model).setMallocStatements(mallocStatements);
		// if (this.civlConfig.isEnableMpiContract()) {
		model.setSleepLocation(new CommonLocation(factory.systemSource(), true));
		// }
		model.setLogicFunctions(seenLogicFunctions);
		model.complete();
		// TODO check scope/proc/pointers of variables.
	}

	private void calculateConstantValue() {
		for (CIVLFunction f : model.functions()) {
			FunctionContract contract = f.functionContract();

			if (contract != null) {
				Expression guard = contract.guard();

				if (guard != null)
					guard.calculateConstantValue(universe);
			}
			for (Statement statement : f.statements()) {
				statement.calculateConstantValue(this.universe);
			}
		}
	}

	/**
	 * Perform static analysis, including: dereferences, purely local statements,
	 * etc.
	 */
	protected void staticAnalysis() {
		Set<Variable> addressedOfVariables = new HashSet<>();
		MemoryUnitExpressionAnalyzer memUnitAnalyzer = new MemoryUnitExpressionAnalyzer(this.factory);
		List<CodeAnalyzer> analyzers = factory.codeAnalyzers();

		for (CIVLFunction f : model.functions()) {
			// identify all purely local variables
			f.purelyLocalAnalysisForVariables();
			f.setModel(model);
			for (Statement s : f.statements()) {
				Set<Variable> statementResult = s.variableAddressedOf();

				if (statementResult != null) {
					addressedOfVariables.addAll(statementResult);
				}
				s.setModel(model);
				s.calculateDerefs();
				Analysis.staticAnalysis(s, analyzers);
			}
		}
		if (debugging) {
			debugOut.println("Variable addressed of:");
			for (Variable variable : addressedOfVariables) {
				debugOut.print(variable.name() + "-" + variable.scope().id() + ", ");
			}
			debugOut.println();
		}
		if (debugging) {
			debugOut.println("static analysis of locations...");
		}
		// model.print(System.out, false);
		for (CIVLFunction f : model.functions()) {
			// purely local statements can only be
			// identified after ALL variables have been
			// checked for being purely local or not
			for (Location loc : f.locations()) {
				loc.staticAnalysis();
				loc.computeWritableVariables(addressedOfVariables);
			}
			f.purelyLocalAnalysis();
		}
		if (debugging) {
			debugOut.println("loop analysis of locations...");
		}
		for (CIVLFunction f : model.functions()) {
			boolean noUnsafeloop = true;

			// purely local locations that enters an atomic block needs future
			// statements to be checked, thus it can only be
			// identified after ALL statements have been
			// checked for being purely local or not
			for (Location loc : f.locations()) {
				boolean isLoop = loopAnalysis(loc, addressedOfVariables);

				// loc.purelyLocalAnalysis();
				loc.loopAnalysis();
				factory.computeImpactScopeOfLocation(loc);
				noUnsafeloop &= (!isLoop || loc.isSafeLoop()) && !loc.isInNoopLoop();
			}
			f.setFreeOfUnsafeloop(noUnsafeloop);
		}
		// Atomic block termination analysis: attempt to determine if an atomic
		// block will terminate:
		for (Location[] atomic : atomicBlocks)
			atomicBlockTerminationAnalysis(atomic[0], atomic[1]);
		memUnitAnalyzer.memoryUnitAnalysis(model);
		// Compute the variables used in every atomic block:
		for (CIVLFunction f : model.functions()) {
			for (Statement stmt : f.statements()) {
				if (stmt instanceof AtomicLockAssignStatement) {
					AtomicLockAssignStatement lockStmt = (AtomicLockAssignStatement) stmt;

					if (lockStmt.enterAtomic()) {
						((CommonAtomicLockAssignStatement) lockStmt).setVariables(computeAccessesAtomicBlock(lockStmt));
					}
				}
			}
		}
		// Compute the variables used in every atomic function:
		for (CIVLFunction f : model.functions()) {
			if (f.isAtomicFunction() && f.startLocation() != null)
				f.setAccessesAtomicFunction(computeAccessesAtomicFunction(f));
		}
	}

	/**
	 * Performs analysis on an atomic block to determine if it will terminate. If an
	 * atomic block satisfies following conditions, they will be determined as "will
	 * terminate":
	 * <li>No un-safe loop in the atomic block. An unsafe loop can be determined if
	 * {@link Location#isSafeLoop()} returns false, where location is the entry
	 * location of the loop.</li>
	 * <li>No regular function call in the atomic block. Regular function calls are
	 * function calls on non-system functions. Termination of system functions is
	 * guaranteed</li>
	 * 
	 * @param startLocation The entry location of the atomic block, it must has a
	 *                      sole outgoing statement ATOMIC_ENTER
	 * @param endLocation   The exit location of the atomic block, an atomic block
	 *                      must have exact one exit location. //TODO: does GOTO
	 *                      allowed in atomic block ?
	 */
	private void atomicBlockTerminationAnalysis(Location startLocation, Location endLocation) {
		Set<Integer> seenLocations = new HashSet<>();
		Stack<Location> stack = new Stack<>();
		Location location = startLocation;
		boolean terminate = true;

		// About the traverse each location in the atomic block. The traverse is
		// based on
		// the fact that there is exact one entry and exit location of the
		// atomic block. For special statements: RETURN and GOTO, RETURN has no
		// target location, so it will not affect the termination of this
		// traverse; We assume there is no GOTO allowed in atomic blocks.
		stack.push(location);
		do {
			location = stack.pop();
			// If location is an entry of a loop ...
			if (location.getNumOutgoing() == 2) {
				Statement outgoing0 = location.getOutgoing(0), outgoing1 = location.getOutgoing(1);

				if (outgoing0 instanceof LoopBranchStatement && outgoing1 instanceof LoopBranchStatement)
					if (!location.isSafeLoop()) {
						terminate = false;
						break;
					}
			}
			// Proceed along with all outgoing statements ...
			for (Statement outgoing : location.outgoing()) {
				if (outgoing.statementKind() == StatementKind.CALL_OR_SPAWN) {
					CallOrSpawnStatement call = (CallOrSpawnStatement) outgoing;

					// If there is any non-system function call, it may not
					// terminate
					if (call.isCall() && !call.isSystemCall()) {
						CIVLFunction function = call.function();

						if (function != null) {
							// If the calling function has no more calls inside
							// it and itself is free of unsafe loops, the call
							// statement will terminate.
							terminate &= function.isFreeOfUnsafeloop() && noCallInFunction(function);
						} else
							// call by function pointer, give up ...
							terminate = false;
						if (!terminate)
							break;
					}
				}
				if (outgoing.target() != null) {
					Location target = outgoing.target();
					int targetID = target.id();

					if (!seenLocations.contains(targetID) && target != endLocation) {
						seenLocations.add(targetID);
						stack.push(target);
					}
				}
			}
		} while (!stack.isEmpty());
		startLocation.setEntryOfUnsafeAtomic(!terminate);
	}

	/**
	 * Test if there is any function call inside the function body by traversing all
	 * locations ({@link CIVLFunction#locations()}).
	 * 
	 * @param function The given function will be tested if it has any function call
	 *                 in its body.
	 * @return True iff there is at least one function call statement in the
	 *         function body.
	 */
	private boolean noCallInFunction(CIVLFunction function) {
		for (Location loc : function.locations()) {
			for (Statement stmt : loc.outgoing()) {
				if (stmt.statementKind() == StatementKind.CALL_OR_SPAWN && ((CallOrSpawnStatement) stmt).isCall())
					return false;
			}
		}
		return true;
	}

	/**
	 * Identifies loops that satisfy the following conditions:
	 * <ol>
	 * <li>has one iteration variable</li>
	 * <li>the iteration variable is only modified by the last statement
	 * (incremental)</li>
	 * <li>the condition has the form <code>i < N</code> (or <code>i > N</code>)
	 * </li>
	 * <li>the loop has finite iterations (can be decided statically)</li>
	 * </ol>
	 * 
	 * @param location
	 * @param addressedOfVariables
	 * @return True iff this location is an entry of a loop
	 */
	private boolean loopAnalysis(Location location, Set<Variable> addressedOfVariables) {
		boolean isLoopEntry = false;

		if (location.getNumOutgoing() == 2) {
			Statement outgoing0 = location.getOutgoing(0), outgoing1 = location.getOutgoing(1);

			// loop detected
			if ((outgoing0 instanceof LoopBranchStatement) && (outgoing1 instanceof LoopBranchStatement)) {
				LoopBranchStatement loopEnter, loopExit;
				Expression condition;
				Statement increment;
				LHSExpression iterVar;

				isLoopEntry = true;
				if (((LoopBranchStatement) outgoing0).isEnter()) {
					loopEnter = (LoopBranchStatement) outgoing0;
					loopExit = (LoopBranchStatement) outgoing1;
				} else {
					loopEnter = (LoopBranchStatement) outgoing1;
					loopExit = (LoopBranchStatement) outgoing0;
				}
				condition = loopEnter.guard();
				if (condition.hasConstantValue() && condition.constantValue().isTrue())
					return isLoopEntry;
				increment = this.getIncrement(location, loopExit);
				if (increment instanceof AssignStatement) {
					AssignStatement assign = (AssignStatement) increment;
					Expression incrExpr = assign.rhs();

					iterVar = assign.getLhs();
					// The loop body modifies the iteration variable
					if (iterVar instanceof VariableExpression) {
						Variable var = ((VariableExpression) iterVar).variable();

						// iteration variable could be modified through
						// pointers.
						if (addressedOfVariables.contains(var))
							return isLoopEntry;
					}
					if (modifiesIterVarInBody(loopEnter.target(), iterVar, increment.source(), loopExit.target()))
						return isLoopEntry;
					if (condition instanceof BinaryExpression) {
						BinaryExpression binary = (BinaryExpression) condition;
						Expression condLeft = binary.left(), condRight = binary.right();
						BINARY_OPERATOR condOp = binary.operator();

						if (condOp != BINARY_OPERATOR.LESS_THAN && condOp != BINARY_OPERATOR.LESS_THAN_EQUAL)
							return isLoopEntry;
						if (incrExpr instanceof BinaryExpression) {
							BinaryExpression incrementExpr = (BinaryExpression) incrExpr;
							BINARY_OPERATOR incrOp = incrementExpr.operator();
							Expression incrLeft = incrementExpr.left(), incrRight = incrementExpr.right();

							if (condLeft.equals(iterVar)) {
								// i < K, then the increment should be i = i + x
								// or i = x + i;
								if (incrOp != BINARY_OPERATOR.PLUS)
									return isLoopEntry;
								if (incrLeft.equals(iterVar) || incrRight.equals(iterVar)) {
									location.setSafeLoop(true);
								}
							} else if (condRight.equals(iterVar)) {
								// K < i, then the increment should be i = i - x
								if (incrOp != BINARY_OPERATOR.MINUS)
									return isLoopEntry;
								if (incrLeft.equals(iterVar))
									location.setSafeLoop(true);
							}
						}
					}

				}

			}
		}
		return isLoopEntry;
	}

	/**
	 * Checks if the iteration variable is modified anywhere in the loop body except
	 * the increment.
	 * 
	 * @param loopStart
	 * @param iterVar
	 * @param increment
	 * @return
	 */
	private boolean modifiesIterVarInBody(Location loopStart, LHSExpression iterVar, Location increment,
			Location loopExit) {
		Stack<Location> working = new Stack<>();
		Set<Location> visited = new HashSet<>();
		int incrementId = increment.id(), exitId = loopExit.id();

		working.add(loopStart);
		visited.add(loopStart);
		while (!working.isEmpty()) {
			Location current = working.pop();

			for (Statement statement : current.outgoing()) {
				if (modifiesVariable(iterVar, statement))
					return true;
				Location target = statement.target();

				if (target != null && !visited.contains(target) && target.id() != incrementId
						&& target.id() != exitId) {
					working.add(target);
					visited.add(target);
				}
			}
		}
		return false;
	}

	private boolean modifiesVariable(LHSExpression iterVar, Statement statement) {
		if (statement instanceof AssignStatement) {
			LHSExpression lhs = ((AssignStatement) statement).getLhs();

			return lhs.equals(iterVar);
		}
		return false;
	}

	/**
	 * Finds the increment statement of a loop, which is the last statement in the
	 * loop body.
	 * 
	 * @param loopStart the start location of the loop
	 * @param loopExit  the exit statement of the loop
	 * @return
	 */
	private Statement getIncrement(Location loopStart, Statement loopExit) {
		Set<Integer> seenLocations = new HashSet<>();
		Stack<Location> stack = new Stack<>();
		Location nextOfLoop = loopExit.target();

		stack.push(loopStart);
		do {
			Location location = stack.pop();

			for (Statement stmt : location.outgoing()) {
				Location target = stmt.target();

				if (target == loopStart)
					// If the target is the loop start location, the statement
					// is the last statement of the loop body. Considering that
					// a safe loop should always have an increment at the end
					// of loop body, we don't need explore all locations inside
					// the body which bring the control back to the start
					// location.
					return stmt;
				else if (target != null && target != nextOfLoop) {
					// If target is not null and target is not the next location
					// of the loop (outside of the loop), add it to stack.
					// TODO: goto statement still will bring the traverse out of
					// loop, need some special handling. e.g. keep track of
					// gotos in FunctionTranslator...
					if (!seenLocations.contains(target.id())) {
						seenLocations.add(target.id());
						stack.push(target);
					}
				}
			}
		} while (!stack.isEmpty());
		return null;
		// Statement current = loopEnter;
		// int startId = loopStart.id();
		//
		// if (current.target() == null)
		// return null;
		// do {
		// Location next = current.target();
		//
		// current = next.getOutgoing(0);
		// if (current instanceof LoopBranchStatement) {
		// if (((LoopBranchStatement) current).isEnter())
		// current = next.getOutgoing(1);
		// }
		// if (current.target() == null)
		// return null;
		// } while (current.target().id() != startId);
		// if (current != null)
		// System.out.println("Get incremental :" + current.getSource());
		// return current;
	}

	/* *************************** Public Methods ************************** */

	/**
	 * Build the CIVL model from the AST
	 * 
	 * @throws CommandLineException
	 */
	void buildModel() throws CommandLineException {
		ASTNode rootNode = program.getAST().getRootNode();
		FunctionTranslator rootFunctionTranslator;

		preprocess();
		initialization();
		rootFunctionTranslator = new FunctionTranslator(this, factory, rootFunction, this.civlConfig);
		rootFunctionTranslator.translateRootFunction(rootScope, rootNode);
		if (inputInitMap != null) {
			// if commandline specified input variables that do not
			// exist, throw exception...
			Set<String> commandLineVars = new HashSet<>(inputInitMap.keySet());
			commandLineVars.removeAll(initializedInputs);
		}
		// translate main function, using system as the CIVL function object,
		// and combining initialization statements with its body
		// translateFunctionDefinitionNode(mainFunctionNode, system,
		// initialization);
		translateUndefinedFunctions();
		translateParProcFunctions();
		translateRunProcFunctions();
		translateUndefinedFunctions();
		// TODO when the function is a function pointer, we are unable to
		// identify if it is a system call.
		completeBundleType();
		completeHeapType();
		completeTimeVar();
		completeCallEvents();
		completeModel(rootFunction);
		// this.model.print(System.out, false);
		this.calculateConstantValue();
		this.factory.setCodeAnalyzers(Analysis.getAnalyzers(civlConfig, universe));
		this.staticAnalysis();
	}

	private void completeMallocStatements() {
		for (MallocStatement malloc : this.mallocStatements) {
			CIVLType staticElementType = malloc.getStaticElementType();
			SymbolicType dynamicElementType = staticElementType.getDynamicType(universe);
			SymbolicArrayType dynamicObjectType = universe.arrayType(dynamicElementType);

			malloc.complete(dynamicElementType, dynamicObjectType);
		}
	}

	private void completeCallEvents() {
		for (Map.Entry<CallEvent, Function> callEventPair : this.callEvents.entrySet()) {
			callEventPair.getKey().setFunction(this.functionMap.get(callEventPair.getValue()));
		}
	}

	private void completeTimeVar() {
		Variable brokenTimeVar = this.factory.brokenTimeVariable();

		if (brokenTimeVar != null) {
			CIVLType tmType = this.typeFactory.systemType(ModelConfiguration.TM_TYPE);

			if (tmType != null)// tmType may be null because of the pruner
				brokenTimeVar.setType(tmType);
		}
	}

	// TODO: need to do the same thing for $atomic_f functions.
	// factor out general method.
	// Interested in the variables that exist outside of the function scope.
	//

	/**
	 * Computes the variables that are accessed within an atomic code section. The
	 * code section could be an atomic block or the body of an atomic function.
	 * 
	 * Only variables that existed prior to entering the atomic section are
	 * considered. I.e., only variables declared in originalScope or a scope
	 * containing originalScope are considered. Other variables are ignored.
	 * 
	 * @param originalScope the scope containing the atomic section
	 * @param start         the start location of the atomic function, or the target
	 *                      location of an atomic-enter statement
	 * @return an over-approximation of the set of previously existing variables
	 *         accessed in the atomic section, or {@code null} if no approximation
	 *         could be computed
	 */
	private Set<Variable> computeAccesses(Scope originalScope, Location start, CIVLFunction theFunction) {
		class Node {
			Location location;
			int atomicDepth;
			int statementIndex;

			Node(Location location, int atomicDepth, int statementIndex) {
				this.location = location;
				this.atomicDepth = atomicDepth;
				this.statementIndex = statementIndex;
			}
		}

		Map<Location, Node> seenLocations = new HashMap<>();
		Stack<Node> stack = new Stack<>();
		Set<Variable> result = new HashSet<>();
		Set<CIVLFunction> seenFunctions = new HashSet<>();
		Node node0 = new Node(start, 1, 0);

		if (theFunction != null)
			seenFunctions.add(theFunction);
		seenLocations.put(start, node0);
		stack.push(node0);
		while (!stack.empty()) {
			Node top = stack.peek();

			if (top.statementIndex >= top.location.getNumOutgoing()) {
				stack.pop();
				continue;
			}

			Statement newStatement = top.location.getOutgoing(top.statementIndex);

			top.statementIndex++;
			for (Variable var : newStatement.freeVariables())
				if (originalScope.isDescendantOf(var.scope()))
					result.add(var);
			if (newStatement.statementKind() == StatementKind.CALL_OR_SPAWN) {
				CallOrSpawnStatement call = (CallOrSpawnStatement) newStatement;
				CIVLFunction function = call.function();

				if (function == null)
					return null; // nothing known about called function
				seenFunctions.add(function);
			}

			int newDepth = top.atomicDepth;

			if (newStatement instanceof AtomicLockAssignStatement) {
				if (((AtomicLockAssignStatement) newStatement).enterAtomic()) {
					newDepth++;
				} else {
					newDepth--;
					if (newDepth == 0)
						continue;
				}
			}

			Location newLocation = newStatement.target();

			// a return statement has null target
			if (newLocation != null) {
				Node newNode = seenLocations.get(newLocation);

				if (newNode != null) {
					if (newNode.atomicDepth != newDepth)
						throw new CIVLSyntaxException("Possible branch into atomic block: atomic depths "
								+ newNode.atomicDepth + " and " + newDepth, newLocation);
					continue;
				}
				newNode = new Node(newLocation, newDepth, 0);
				seenLocations.put(newLocation, newNode);
				stack.push(newNode);
			}
		}

		List<CIVLFunction> workList = new LinkedList<>(seenFunctions);

		while (!workList.isEmpty()) {
			CIVLFunction function = workList.remove(0);

			if (function instanceof SystemFunction)
				return null; // or look at contract? (TODO)
			for (Statement stmt : function.statements()) {
				if (stmt.statementKind() == StatementKind.CALL_OR_SPAWN) {
					CallOrSpawnStatement call = (CallOrSpawnStatement) stmt;
					CIVLFunction function2 = call.function();

					if (function2 == null)
						return null;
					if (seenFunctions.add(function2))
						workList.add(function2);
				}
				for (Variable var : stmt.freeVariables())
					if (originalScope.isDescendantOf(var.scope()))
						result.add(var);
			}
		}
		return result;
	}

	private Set<Variable> computeAccessesAtomicFunction(CIVLFunction function) {
		assert function.isAtomicFunction();
		assert !function.isSystemFunction();
		Scope originalScope = function.outerScope();
		// note: this was function.containingScope(), but that's wrong because a
		// variable in the containingScope (usually that's the file scope) is external
		// to the function. The outerScope is correct because it includes all the local
		// variables (including the formal parameters), so anything strictly above that
		// scope counts as external.
		return computeAccesses(originalScope, function.startLocation(), function);

	}

	private Set<Variable> computeAccessesAtomicBlock(AtomicLockAssignStatement atomicEnterStmt) {
		assert atomicEnterStmt.enterAtomic();
		return computeAccesses(atomicEnterStmt.source().scope(), atomicEnterStmt.target(), null);
	}

	/**
	 * @return The model factory used by this model builder.
	 */
	public ModelFactory factory() {
		return factory;
	}

	/**
	 * @return the configuration
	 */
	public GMCSection getConfiguration() {
		return config;
	}

	/**
	 * @return the CIVL configuration
	 */
	public CIVLConfiguration getCIVLConfiguration() {
		return civlConfig;
	}

	/**
	 * @return the model
	 */
	public Model getModel() {
		return model;
	}
}
