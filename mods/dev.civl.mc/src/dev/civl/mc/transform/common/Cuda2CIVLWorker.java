package dev.civl.mc.transform.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.DeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.EnumeratorDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FieldDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.TypedefDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.label.SwitchLabelNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode.StatementKind;
import dev.civl.abc.ast.node.IF.type.ArrayTypeNode;
import dev.civl.abc.ast.node.IF.type.EnumerationTypeNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode.TypeNodeKind;
import dev.civl.abc.ast.type.IF.EnumerationType;
import dev.civl.abc.ast.type.IF.PointerType;
import dev.civl.abc.ast.type.IF.QualifiedObjectType;
import dev.civl.abc.ast.type.IF.StandardBasicType;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.front.IF.CivlcTokenConstant;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.Formation;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.StringLiteral;
import dev.civl.abc.token.IF.StringToken;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;
import dev.civl.abc.token.IF.CivlcToken.TokenVocabulary;
import dev.civl.mc.model.IF.CIVLSyntaxException;
import dev.civl.mc.transform.IF.Cuda2CIVLTransformer;
import dev.civl.mc.util.IF.Pair;

public class Cuda2CIVLWorker extends BaseWorker {

	private static final String CUDA_HEADER = "cuda.h";
	private int tempVarNum;

	private static final Set<String> builtinCUDAVariables = Set.of("threadIdx", "blockIdx", "gridDim", "blockDim");

	private static final String CUDA_TAG_ENUM_NAME = "$cuda_tag";
	private static final String HOST_COMM_NAME = "$cuda_host_comm";
	private static final String HOST_PLACE_NAME = "$CUDA_PLACE_HOST";
	private static final String DEVICE_PLACE_NAME = "$CUDA_PLACE_DEVICE";
	private static final String DEVICE_GLOB_CONTEXT_NAME = "$cuda_global_context";
	private static final String HOST_MAIN = "$host_main";
	private static final String CUDA_MAIN = "$cuda_main";
	private EnumerationType cudaTagEnumType = null;

	private Map<String, KernelInfo> kernelMap = new HashMap<String, KernelInfo>();
	private Set<ExpressionStatementNode> kernelCalls = new HashSet<ExpressionStatementNode>();

	private Map<String, Function> deviceFunctionMap = new HashMap<String, Function>();

	private int numSyncthreads = 0;

	protected class KernelInfo {

		public Function entity;

		public KernelInfo(Function entity) {
			this.entity = entity;
		}

		public CompoundStatementNode kernelBody = null;

		/**
		 * @return the name of the enum value that is used to signal to the device to
		 *         launch this kernel.
		 */
		public String getTagName() {
			return "$CUDA_TAG_LAUNCH_" + entity.getName();
		}

		public String getParamStructName() {
			return "$cuda_" + entity.getName() + "_data";
		}

		public String getArgRevealFunctionName() {
			return "$cuda_reveal_" + entity.getName() + "_args";
		}

		public String getKernelProcName() {
			return "$cuda_" + entity.getName() + "_proc";
		}

		public String getLaunchFunctionName() {
			return "$cuda_host_launch_" + entity.getName();
		}

		public FunctionDefinitionNode getDefinition() {
			return entity.getDefinition();
		}

		/**
		 * Generates a new struct (wrapped inside of a typedef to avoid needing to
		 * prepend the "struct" keyword before uses of this type) that has a field for
		 * each formal parameter that this kernel takes.
		 * 
		 * This struct is used for passing passing kernel parameters from the host to
		 * the device via communicators.
		 */
		public TypedefDeclarationNode generateParameterStruct() {
			String srcMethod = entity.getName() + ".generateParameterStruct";
			List<Pair<String, String>> contextParams = contextParams(false);
			List<FieldDeclarationNode> fieldList = new ArrayList<FieldDeclarationNode>(
					entity.getType().getNumParameters() + contextParams.size());

			for (Pair<String, String> contextParam : contextParams) {
				fieldList
						.add(nodeDeclField(srcMethod, contextParam.left, nodeTypeNamed(srcMethod, contextParam.right)));
			}

			for (VariableDeclarationNode param : getDefinition().getTypeNode().getParameters()) {
				if (param.getTypeNode().kind() == TypeNodeKind.VOID)
					continue;

				fieldList.add(nodeDeclField(srcMethod, param.getName(), param.getTypeNode().copy()));
			}

			for (FieldDeclarationNode field : fieldList) {
				field.getTypeNode().setConstQualified(false);
			}

			return nodeTypeDefStruct(srcMethod, getParamStructName(), fieldList);
		}

		/**
		 * Generates a function which takes a pointer to the parameter struct associated
		 * to this kernel and $reveal's any pointer parameters.
		 */
		public FunctionDefinitionNode generateArgRevealFunction() {
			String srcMethod = entity.getName() + ".generateArgRevealFunction";

			List<BlockItemNode> bodyList = new LinkedList<>();

			for (VariableDeclarationNode formalDecl : generateFormalParameters(entity.getName(),
					getDefinition().getTypeNode(), false)) {
				ExpressionNode argNode = nodeExprOp(srcMethod, OperatorNode.Operator.ADDRESSOF,
						nodeExprArrow(srcMethod, nodeExprId(srcMethod, "args"), formalDecl.getName()));

				bodyList.add(nodeStmtCall(srcMethod, "$reveal",
						nodeExprCast(srcMethod, nodeTypePointer(srcMethod, voidType()), argNode)));
			}

			return nodeDefnFunction(srcMethod, getArgRevealFunctionName(), voidType(),
					Arrays.asList(nodeDeclVar(srcMethod, "args",
							nodeTypePointer(srcMethod, nodeTypeNamed(srcMethod, getParamStructName())))),
					bodyList);
		}

		private FunctionDefinitionNode generateKernelThreadDefinition() {
			String srcMethod = entity.getName() + ".generateKernelThreadDefinition";

			List<BlockItemNode> bodyList = new LinkedList<>();

			bodyList.add(nodeStmtCall(srcMethod, "$local_start"));
			bodyList.add(nodeDeclVarInit(srcMethod, "$cuda_tid", nodeTypeInt(srcMethod), nodeExprCall(srcMethod,
					"$cuda_dim3_index", nodeExprId(srcMethod, "blockDim"), nodeExprId(srcMethod, "threadIdx"))));
			bodyList.add(nodeDeclVarInit(srcMethod, "$cuda_kid", nodeTypeInt(srcMethod),
					nodeExprCall(srcMethod, "$cuda_kernel_index", nodeExprId(srcMethod, "gridDim"),
							nodeExprId(srcMethod, "blockDim"), nodeExprId(srcMethod, "blockIdx"),
							nodeExprId(srcMethod, "threadIdx"))));
			bodyList.add(nodeDeclVarInit(srcMethod, "$thread", nodeTypeNamed(srcMethod, "$cuda_thread_data_t"),
					nodeExprCall(srcMethod, "$create_cuda_thread_data", nodeExprHere(srcMethod),
							nodeExprId(srcMethod, "$kernel"),
							nodeExprOp(srcMethod, Operator.DIV, nodeExprId(srcMethod, "$cuda_kid"),
									nodeExprOp(srcMethod, Operator.TIMES,
											nodeExprDot(srcMethod, nodeExprId(srcMethod, "blockDim"), "x"),
											nodeExprOp(srcMethod, Operator.TIMES,
													nodeExprDot(srcMethod, nodeExprId(srcMethod, "blockDim"), "y"),
													nodeExprDot(srcMethod, nodeExprId(srcMethod, "blockDim"), "z")))),
							nodeExprOp(srcMethod, Operator.DIV, nodeExprId(srcMethod, "$cuda_tid"),
									nodeExprId(srcMethod, "warpSize")),
							nodeExprOp(srcMethod, Operator.MOD, nodeExprId(srcMethod, "$cuda_tid"),
									nodeExprId(srcMethod, "warpSize")))));

			for (BlockItemNode stmt : kernelBody) {
				if (stmt != null) {
					bodyList.add(stmt.copy());
				}
			}

			bodyList.add(nodeStmtCall(srcMethod, "$destroy_cuda_thread_data", nodeExprId(srcMethod, "$thread")));
			bodyList.add(nodeStmtCall(srcMethod, "$local_end"));

			return nodeDefnFunction(srcMethod, "$cuda_thread", voidType(),
					Arrays.asList(nodeDeclVar(srcMethod, "threadIdx", nodeTypeNamed(srcMethod, "uint3"))), bodyList);
		}

		private FunctionDefinitionNode generateKernelBlockDefinition() {
			String srcMethod = entity.getName() + ".generateKernelBlockDefinition";

			List<BlockItemNode> bodyList = new LinkedList<>();

			List<VariableDeclarationNode> sharedVars = extractSharedVariableDeclarations(
					entity.getDefinition().getBody());
			completeSharedExternArrays(sharedVars);
			bodyList.addAll(sharedVars);

			bodyList.add(generateKernelThreadDefinition());

			bodyList.add(nodeStmtCall(srcMethod, "$cuda_run_and_wait_on_procs", nodeExprId(srcMethod, "blockDim"),
					nodeExprId(srcMethod, "$cuda_thread")));

			return nodeDefnFunction(srcMethod, "$cuda_block", voidType(),
					Arrays.asList(nodeDeclVar(srcMethod, "blockIdx", nodeTypeNamed(srcMethod, "uint3"))), bodyList);
		}

		/**
		 * Generates a transformed definition of this kernel to emulate the thread
		 * hierarchy of CUDA kernels as well as to inject data race checks.
		 */
		public FunctionDefinitionNode generateTransformedKernelDefinition() {
			String srcMethod = entity.getName() + ".generateTransformedKernelDefinition";

			kernelBody = entity.getDefinition().getBody();

			// We transform the kernel body first because it also scans the body
			// for information we need ahead of time like the number of
			// __syncthreads() calls.
			transformBodyOfCudaFunction(kernelBody);

			List<BlockItemNode> bodyList = new LinkedList<>();

			// Need to initialize in order to pass in to $cuda_syncthreads
			bodyList.add(nodeDeclVarInit(srcMethod, "$kernel", nodeTypeNamed(srcMethod, "$cuda_kernel_data_t"),
					nodeExprCall(srcMethod, "$create_cuda_kernel_data", nodeExprHere(srcMethod),
							nodeExprId(srcMethod, "gridDim"), nodeExprId(srcMethod, "blockDim"))));

			bodyList.add(generateKernelBlockDefinition());
			bodyList.add(nodeStmtCall(srcMethod, "$cuda_run_and_wait_on_procs", nodeExprId(srcMethod, "gridDim"),
					nodeExprId(srcMethod, "$cuda_block")));

			bodyList.add(nodeStmtCall(srcMethod, "$destroy_cuda_kernel_data", nodeExprId(srcMethod, "$kernel")));

			return nodeDefnFunction(srcMethod, transformCudaFunctionName(entity.getName()), voidType(),
					generateFormalParameters(entity.getName(), getDefinition().getTypeNode(), false), bodyList);
		}

		/**
		 * Generates a transformed declaration of this kernel which uses the new
		 * transformed name and includes the context parameters as regular formal
		 * parameters.
		 */
		public FunctionDeclarationNode generateTransformedKernelDeclaration() {
			String srcMethod = entity.getName() + ".generateTransformedKernelDeclaration";

			return nodeDeclFunction(srcMethod, transformCudaFunctionName(entity.getName()), voidType(),
					generateFormalParameters(entity.getName(), getDefinition().getTypeNode(), false));
		}

		public FunctionDefinitionNode generateKernelProcDefinition() {
			String srcMethod = entity.getName() + ".generateKernelProcDefinition";

			List<BlockItemNode> bodyList = new LinkedList<>();

			bodyList.add(nodeStmtWhen(srcMethod, nodeExprArrow(srcMethod, nodeExprId(srcMethod, "opState"), "start")));
			bodyList.add(nodeDeclVar(srcMethod, "args", nodeTypeNamed(srcMethod, getParamStructName())));
			bodyList.add(nodeStmtCall(srcMethod, "$message_unpack", nodeExprId(srcMethod, "request"),
					nodeExprOp(srcMethod, Operator.ADDRESSOF, nodeExprId(srcMethod, "args")),
					nodeExprSizeof(srcMethod, nodeTypeNamed(srcMethod, getParamStructName()))));
			bodyList.add(nodeStmtCall(srcMethod, getArgRevealFunctionName(),
					nodeExprOp(srcMethod, Operator.ADDRESSOF, nodeExprId(srcMethod, "args"))));

			List<VariableDeclarationNode> formals = generateFormalParameters(entity.getName(),
					getDefinition().getTypeNode(), false);
			ExpressionNode kernelArgs[] = new ExpressionNode[formals.size()];

			int i = 0;
			for (VariableDeclarationNode paramDeclNode : formals) {
				kernelArgs[i] = nodeExprDot(srcMethod, nodeExprId(srcMethod, "args"), paramDeclNode.getName());
				i++;
			}

			bodyList.add(nodeStmtCall(srcMethod, transformCudaFunctionName(entity.getName()), kernelArgs));
			bodyList.add(nodeStmtCall(srcMethod, "$stream_dequeue", nodeExprId(srcMethod, "cudaStream")));

			return nodeDefnFunction(srcMethod, getKernelProcName(), voidType(),
					Arrays.asList(nodeDeclVar(srcMethod, "request", nodeTypeNamed(srcMethod, "$message")),
							nodeDeclVar(srcMethod, "opState", nodeTypeNamed(srcMethod, "$cuda_op_state_t")),
							nodeDeclVar(srcMethod, "cudaStream", nodeTypeNamed(srcMethod, "cudaStream_t"))),
					bodyList);
		}

		/**
		 * Generates a new launch function definition which the host will call in place
		 * of a kernel launch.
		 * 
		 * This function takes the context parameters of the kernel followed by the
		 * kernel's formal parameters. It then puts all of these parameters into an
		 * instance of the kernel's parameter structure. Then it packs this object into
		 * a message and sends it to the device. Then it waits for a response from the
		 * device before moving on.
		 * 
		 * @return
		 */
		public FunctionDefinitionNode generateKernelLaunchFunction() {
			String srcMethod = entity.getName() + ".generateKernelLaunchFunction";
			List<VariableDeclarationNode> formals = generateFormalParameters(entity.getName(),
					getDefinition().getTypeNode(), false);

			List<BlockItemNode> bodyList = new LinkedList<>();

			bodyList.add(nodeDeclVar(srcMethod, "args", nodeTypeNamed(srcMethod, getParamStructName())));

			for (VariableDeclarationNode formal : formals) {
				bodyList.add(nodeStmtAssign(srcMethod,
						nodeExprDot(srcMethod, nodeExprId(srcMethod, "args"), formal.getName()),
						nodeExprId(srcMethod, formal.getName())));
			}

			ExpressionNode messagePackExpr = nodeExprCall(srcMethod, "$message_pack",
					nodeExprId(srcMethod, HOST_PLACE_NAME), nodeExprId(srcMethod, DEVICE_PLACE_NAME),
					nodeFactory.newEnumerationConstantNode(nodeIdent(srcMethod, getTagName())),
					nodeExprOp(srcMethod, Operator.ADDRESSOF, nodeExprId(srcMethod, "args")),
					nodeExprSizeof(srcMethod, nodeTypeNamed(srcMethod, getParamStructName())));

			bodyList.add(
					nodeStmtCall(srcMethod, "$comm_enqueue", nodeExprId(srcMethod, HOST_COMM_NAME), messagePackExpr));

			bodyList.add(nodeStmtCall(srcMethod, "$comm_dequeue", nodeExprId(srcMethod, HOST_COMM_NAME),
					nodeExprId(srcMethod, DEVICE_PLACE_NAME),
					nodeFactory.newEnumerationConstantNode(nodeIdent(srcMethod, getTagName()))));

			return nodeDefnFunction(srcMethod, getLaunchFunctionName(), voidType(), formals, bodyList);
		}
	}

	public Cuda2CIVLWorker(ASTFactory astFactory) {
		super(Cuda2CIVLTransformer.LONG_NAME, astFactory);
		identifierPrefix = "_cuda_";
	}

	@Override
	protected AST transformCore(AST ast) throws SyntaxException {
		if (!hasHeader(ast, CUDA_HEADER))
			return ast;

		SequenceNode<BlockItemNode> root = ast.getRootNode();

		ast.release();
		scanTree(root);
		assert cudaTagEnumType != null;
		addEnumTags();
		translateDeviceFunctions();
		executeKernelTransformations();
		executeKernelCallTransformations();

		translateCudaMallocCalls(root);
		if (!has_gen_mainFunction(root)) {
			transformMainFunction(root);
			createNewMainFunction(root);
		}
		translateMainDefinition(root);
		// translateKernelDefinitions(root);
		// translateKernelDeclarations(root);
		AST newAST = astFactory.newAST(root, ast.getSourceFiles(), ast.isWholeProgram());
		// newAST.prettyPrint(System.out, false);
		return newAST;
	}

	/**
	 * Returns a new temporary variable each time it is called.
	 * 
	 * @return A generated temporary variable name
	 */
	protected String newTemporaryVariableName() {
		return identifierPrefix + "tmp" + tempVarNum++;
	}

	/**
	 * Scans the entire AST recursively, collecting nodes and entities of interest.
	 * Also performs some light transformations such as removing all of the built-in
	 * CUDA variables.
	 * 
	 * @param root The root of the AST to be scanned.
	 */
	protected void scanTree(ASTNode root) {
		for (ASTNode child : root.children()) {
			if (child == null)
				continue;

			switch (child.nodeKind()) {
			case TYPE:
				if (cudaTagEnumType == null && ((TypeNode) child).kind() == TypeNodeKind.ENUMERATION) {
					EnumerationTypeNode enumTypeNode = (EnumerationTypeNode) child;

					if (enumTypeNode.getTag().name().equals(CUDA_TAG_ENUM_NAME)) {
						cudaTagEnumType = enumTypeNode.getType();
					}
				}
				break;

			case FUNCTION_DEFINITION:
				FunctionDefinitionNode definition = (FunctionDefinitionNode) child;

				if (definition.hasDeviceFunctionSpecifier() && definition.getTypeNode() instanceof FunctionTypeNode) {
					String funcName = definition.getName();

					if (funcName == null) {
						throw new CIVLSyntaxException("__device__ functions cannot be anonymous",
								definition.getSource());
					}
					addDeviceFunction(funcName, definition.getEntity());
				}
				if (definition.hasGlobalFunctionSpecifier()) {
					String kernelName = definition.getName();

					if (kernelName == null) {
						throw new CIVLSyntaxException("CUDA kernels cannot be anonymous", definition.getSource());
					}
					addKernel(kernelName, definition.getEntity());
				}
				break;
			case FUNCTION_DECLARATION:
				FunctionDeclarationNode declaration = (FunctionDeclarationNode) child;

				if (declaration.hasDeviceFunctionSpecifier() && declaration.getTypeNode() instanceof FunctionTypeNode) {
					String funcName = declaration.getName();

					if (funcName == null) {
						throw new CIVLSyntaxException("__device__ functions cannot be anonymous",
								declaration.getSource());
					}
					addDeviceFunction(funcName, declaration.getEntity());
				}
				if (declaration.hasGlobalFunctionSpecifier() && declaration.getTypeNode() instanceof FunctionTypeNode) {
					String kernelName = declaration.getName();

					if (kernelName == null) {
						throw new CIVLSyntaxException("CUDA kernels cannot be anonymous", declaration.getSource());
					}
					addKernel(kernelName, declaration.getEntity());
				}
				break;
			case VARIABLE_DECLARATION:
				VariableDeclarationNode variableDeclaration = (VariableDeclarationNode) child;

				if (variableDeclaration.getIdentifier() != null
						&& variableDeclaration.getIdentifier().getSource().getFirstToken().getSourceFile().getName()
								.equals("cuda.h")
						&& builtinCUDAVariables.contains(variableDeclaration.getIdentifier().name())) {
					// variableDeclaration.remove();
					continue;
				}
				break;
			case STATEMENT:
				StatementNode statement = (StatementNode) child;

				if (statement.statementKind() == StatementKind.EXPRESSION) {
					ExpressionStatementNode expressionStatement = (ExpressionStatementNode) statement;
					ExpressionNode expression = expressionStatement.getExpression();
					if (expression.expressionKind() == ExpressionKind.FUNCTION_CALL) {
						FunctionCallNode functionCall = (FunctionCallNode) expression;
						if (functionCall.getNumberOfContextArguments() > 0) {
							if (functionCall.getFunction().expressionKind() != ExpressionKind.IDENTIFIER_EXPRESSION) {
								throw new CIVLSyntaxException(
										"CUDA kernel calls must be made with kernel identifier explicitly",
										functionCall.getFunction().getSource());
							}
							kernelCalls.add(expressionStatement);
							break;
						}
					}
				}
			default:
				break;
			}
			scanTree(child);
		}
	}

	/**
	 * Gets the {@link KernelInfo} associated with kernelName if it exists. If no
	 * such KernelInfo exists then a fresh one is created and associated to
	 * kernelName and returned.
	 * 
	 * @param kernelName
	 * @return the (potentially fresh) associated KernelInfo
	 */
	private KernelInfo addKernel(String kernelName, Function kernelEntity) {
		KernelInfo kernelEntry = kernelMap.get(kernelName);

		if (kernelEntry == null) {
			kernelEntry = new KernelInfo(kernelEntity);
			kernelMap.put(kernelName, kernelEntry);
		} else {
			assert kernelEntry.entity == kernelEntity;
		}
		return kernelEntry;
	}

	private Function addDeviceFunction(String funcName, Function entity) {
		return deviceFunctionMap.put(funcName, entity);
	}

	private void addEnumTags() {
		SequenceNode<EnumeratorDeclarationNode> enumValues = ((EnumerationTypeNode) cudaTagEnumType.getDefinition())
				.enumerators();
		for (KernelInfo kernel : kernelMap.values()) {
			enumValues.addSequenceChild(nodeDeclEnumerator("addEnumTags", kernel.getTagName()));
		}
	}

	private void translateDeviceFunctions() {
		for (Function devFunc : deviceFunctionMap.values()) {
			FunctionDefinitionNode definition = devFunc.getDefinition();

			for (DeclarationNode decl : devFunc.getDeclarations()) {
				FunctionDeclarationNode funcDecl = (FunctionDeclarationNode) decl;
				if (funcDecl == definition)
					continue;

				ASTNode declParent = funcDecl.parent();
				int index = funcDecl.childIndex();
				funcDecl.remove();
				declParent.setChild(index, transformDeviceFunctionDeclaration(funcDecl, definition.getTypeNode()));
			}

			ASTNode parentNode = definition.parent();
			int index = definition.childIndex();
			definition.remove();
			parentNode.setChild(index, transformDeviceFunctionDefinition(definition));
		}
	}

	private DeclarationNode transformDeviceFunctionDeclaration(FunctionDeclarationNode declNode,
			FunctionTypeNode funcTypeNode) {
		String srcMethod = declNode.getName() + ".transformDeviceFunctionDeclaration";
		return nodeDeclFunction(srcMethod, transformCudaFunctionName(declNode.getName()),
				funcTypeNode.getReturnType().copy(), generateFormalParameters(declNode.getName(), funcTypeNode, true));
	}

	// Each pair is param name followed by named type
	private List<Pair<String, String>> contextParams(boolean includeDeviceParams) {
		List<Pair<String, String>> params = new LinkedList<>();
		params.addAll(Arrays.asList(new Pair<String, String>("gridDim", "dim3"),
				new Pair<String, String>("blockDim", "dim3"), new Pair<String, String>("_cuda_mem_size", "size_t"),
				new Pair<String, String>("_cuda_stream", "cudaStream_t")));
		if (includeDeviceParams) {
			params.addAll(Arrays.asList(new Pair<String, String>("blockIdx", "dim3"),
					new Pair<String, String>("threadIdx", "dim3"),
					new Pair<String, String>("$thread", "$cuda_thread_data_t")));
		}
		return params;
	}

	/**
	 * Generates a list of {@link VariableDeclarationNode}s which are the full list
	 * of formal parameters to the kernel. This includes the implicit context
	 * parameters that all CUDA kernels have.
	 * 
	 * @param srcMethod The name of the method generating these parameters. Used for
	 *                  source generation purposes.
	 * @return
	 */
	public List<VariableDeclarationNode> generateFormalParameters(String funcName, FunctionTypeNode funcTypeNode,
			boolean isDevice) {
		String srcMethod = funcName + ".generateFormalParameters";

		List<Pair<String, String>> contextParams = contextParams(isDevice);
		List<VariableDeclarationNode> formals = new LinkedList<VariableDeclarationNode>();

		for (Pair<String, String> contextParam : contextParams) {
			formals.add(nodeDeclVar(srcMethod, contextParam.left, nodeTypeNamed(srcMethod, contextParam.right)));
		}

		for (VariableDeclarationNode param : funcTypeNode.getParameters()) {
			if (param.getTypeNode().kind() == TypeNodeKind.VOID)
				continue;

			formals.add(param.copy());
		}

		return formals;
	}

	private FunctionDefinitionNode transformDeviceFunctionDefinition(FunctionDefinitionNode defNode) {
		String funcName = defNode.getName();
		String srcMethod = funcName + ".transformDeviceFunctionDefinition";

		CompoundStatementNode body = defNode.getBody();
		transformBodyOfCudaFunction(body);

		List<BlockItemNode> bodyList = new LinkedList<>();

		for (BlockItemNode stmt : body) {
			if (stmt != null) {
				bodyList.add(stmt.copy());
			}
		}

		return nodeDefnFunction(srcMethod, transformCudaFunctionName(funcName),
				defNode.getTypeNode().getReturnType().copy(),
				generateFormalParameters(funcName, defNode.getTypeNode(), true), bodyList);
	}

	private String transformCudaFunctionName(String funcName) {
		return "$cuda_" + funcName;
	}

	private void transformBodyOfCudaFunction(ASTNode node) {
		String srcMethod = "transformBodyOfCudaFunction";

		for (ASTNode child : node.children()) {
			if (child == null)
				continue;

			if (child instanceof ExpressionNode) {
				ExpressionNode exprNode = (ExpressionNode) child;
				if (exprNode instanceof FunctionCallNode) {
					FunctionCallNode funcCallNode = (FunctionCallNode) exprNode;
					ExpressionNode function = funcCallNode.getFunction();
					if (function instanceof IdentifierExpressionNode) {
						String functionName = ((IdentifierExpressionNode) function).getIdentifier().name();
						if (functionName.startsWith("__syncthreads")) {
							List<ExpressionNode> args = new LinkedList<>();
							for (ExpressionNode arg : funcCallNode.getArguments()) {
								args.add(arg.copy());
							}
							args.add(nodeExprId(srcMethod, "$thread"));
							args.add(nodeExprInt(srcMethod, numSyncthreads));

							FunctionCallNode newSyncthreads = nodeExprCall(srcMethod, "$cuda" + functionName,
									args.toArray(new ExpressionNode[0]));
							numSyncthreads++;

							int index = funcCallNode.childIndex();
							funcCallNode.remove();
							node.setChild(index, newSyncthreads);
							continue;
						}

						Function devFunction = deviceFunctionMap.get(functionName);
						if (devFunction != null) {
							List<ExpressionNode> args = new LinkedList<>();
							for (Pair<String, String> contextParam : contextParams(true)) {
								args.add(nodeExprId(srcMethod, contextParam.left));
							}
							for (ExpressionNode arg : funcCallNode.getArguments()) {
								args.add(arg.copy());
							}

							int index = funcCallNode.childIndex();
							funcCallNode.remove();
							node.setChild(index, nodeExprCall(srcMethod, transformCudaFunctionName(functionName),
									args.toArray(new ExpressionNode[0])));
						}
					}
				}
			}
			transformBodyOfCudaFunction(child);
		}
	}

	/**
	 * Performs all transformations related to a kernel entity. This includes:
	 * <ul>
	 * <li>Generating a parameter struct definition associated to the kernel;
	 * <li>Generating a launch function that the host will use to launch instances
	 * of this kernel;
	 * <li>Generating a "process" function which the device will $spawn a $proc with
	 * which represents the CUDA op that executes the kernel
	 * <li>Transforming the declaration(s) of this kernel to use the new (mangled)
	 * name;
	 * <li>Transforming the definition of this kernel to emulate the hierarchical
	 * thread structure of CUDA and inject data race checks
	 * </ul>
	 */
	private void executeKernelTransformations() {
		for (KernelInfo kernel : kernelMap.values()) {
			FunctionDefinitionNode kernelDefinition = kernel.getDefinition();

			boolean firstDecl = true;
			for (DeclarationNode decl : kernel.entity.getDeclarations()) {
				if (firstDecl) {
					@SuppressWarnings("unchecked")
					SequenceNode<BlockItemNode> firstDeclParentNode = (SequenceNode<BlockItemNode>) decl.parent();
					firstDeclParentNode.insertChildren(decl.childIndex(),
							Arrays.asList(kernel.generateParameterStruct(), kernel.generateArgRevealFunction(),
									kernel.generateKernelLaunchFunction()));
					firstDeclParentNode.insertChildren(decl.childIndex() + 1,
							Arrays.asList(kernel.generateKernelProcDefinition()));
					firstDecl = false;
				}
				if (decl == kernelDefinition)
					continue;

				ASTNode declParent = decl.parent();
				int index = decl.childIndex();
				decl.remove();
				declParent.setChild(index, kernel.generateTransformedKernelDeclaration());
			}
			@SuppressWarnings("unchecked")
			SequenceNode<BlockItemNode> parentNode = (SequenceNode<BlockItemNode>) kernelDefinition.parent();
			int index = kernelDefinition.childIndex();

			kernelDefinition.remove();
			parentNode.setChild(index, kernel.generateTransformedKernelDefinition());
		}
	}

	/**
	 * Replaces kernel launches (that use the `kernel<<< context args >>>(regular
	 * args)` syntax) with calls to the corresponding launch function, generated
	 * earlier.
	 */
	private void executeKernelCallTransformations() {
		String srcMethod = "executeKernelCallTransformations";

		for (ExpressionStatementNode kernelCallStatement : kernelCalls) {
			ASTNode parent = kernelCallStatement.parent();
			FunctionCallNode kernelCallNode = (FunctionCallNode) kernelCallStatement.getExpression();
			String kernelName = ((IdentifierExpressionNode) kernelCallNode.getFunction()).getIdentifier().name();
			KernelInfo kernel = kernelMap.get(kernelName);

			List<ExpressionNode> launchArgList = getContextArgList(srcMethod, kernelCallNode);

			for (ExpressionNode argument : kernelCallNode.getArguments()) {
				launchArgList.add(argument.copy());
			}

			parent.setChild(kernelCallStatement.childIndex(), nodeStmtCall(srcMethod, kernel.getLaunchFunctionName(),
					launchArgList.toArray(new ExpressionNode[0])));
		}
	}

	private List<ExpressionNode> getContextArgList(String srcMethod, FunctionCallNode kernelCall) {
		List<ExpressionNode> contextArgs = new ArrayList<>();

		for (int i = 0; i < 2; i++) {
			ExpressionNode arg = kernelCall.getContextArgument(i);
			Type argType = arg.getConvertedType();

			if (argType.kind() == TypeKind.QUALIFIED) {
				argType = ((QualifiedObjectType) argType).getBaseType();
			}

			if (argType.kind() == TypeKind.BASIC
					&& ((StandardBasicType) argType).getBasicTypeKind() == BasicTypeKind.INT) {
				contextArgs.add(nodeExprCall(srcMethod, "$cuda_to_dim3", arg.copy()));
			} else {
				contextArgs.add(arg.copy());
			}
		}

		int numContextArgs = kernelCall.getNumberOfContextArguments();

		contextArgs.add(numContextArgs < 3 ? nodeExprInt(srcMethod, 0) : kernelCall.getContextArgument(2).copy());
		contextArgs.add(numContextArgs < 4 ? nodeExprNullPointer(srcMethod) : kernelCall.getContextArgument(3).copy());

		return contextArgs;
	}

	/**
	 * Alters a body of code by removing any variable declaration with the
	 * "__shared__" tag and returning a new list of those removed declarations
	 * without the "__shared__" tag.
	 * 
	 * @param statements a CompountStatementNode that is any section of code
	 * @return The list of removed variable declarations
	 */
	private List<VariableDeclarationNode> extractSharedVariableDeclarations(CompoundStatementNode statements) {
		List<VariableDeclarationNode> declarations = new ArrayList<>();
		for (BlockItemNode item : statements) {
			if (item instanceof VariableDeclarationNode) {
				VariableDeclarationNode variableDeclaration = (VariableDeclarationNode) item.copy();
				if (variableDeclaration.hasSharedStorage()) {
					statements.removeChild(item.childIndex());
					variableDeclaration.setSharedStorage(false);
					declarations.add(variableDeclaration);
				}
			}
		}
		return declarations;
	}

	/**
	 * 
	 * @param sharedVars a list of VariableDeclarationNodes
	 */
	protected void completeSharedExternArrays(List<VariableDeclarationNode> sharedVars) {
		for (VariableDeclarationNode node : sharedVars) {
			if (node.hasExternStorage() && node.getTypeNode().kind() == TypeNodeKind.ARRAY) {
				ArrayTypeNode arrayType = (ArrayTypeNode) node.getTypeNode();
				if (arrayType.getExtent() == null) {
					arrayType.setExtent(identifierExpression("_cuda_mem_size"));
					node.setExternStorage(false);
				}
			}
		}
	}

	/**
	 * Transforms every cuda malloc function call using
	 * {@link Cuda2CIVLWorker#cudaMallocTransform(FunctionCallNode)}. Cuda malloc
	 * calls are found by recursively searching through the AST for a matching
	 * function call.
	 * 
	 * @param root the root node of an Abstract Syntax Tree
	 */
	private void translateCudaMallocCalls(ASTNode root) {
		for (ASTNode child : root.children()) {
			if (child == null)
				continue;

			if (child.nodeKind() == NodeKind.EXPRESSION) {
				ExpressionNode expression = (ExpressionNode) child;

				if (expression.expressionKind() == ExpressionKind.FUNCTION_CALL) {
					FunctionCallNode functionCall = (FunctionCallNode) expression;

					if (functionCall.getFunction().expressionKind() == ExpressionKind.IDENTIFIER_EXPRESSION) {
						IdentifierExpressionNode identifierExpression = (IdentifierExpressionNode) functionCall
								.getFunction();

						if (identifierExpression.getIdentifier().name().equals("cudaMalloc")) {
							int index = functionCall.childIndex();
							/*
							 * ASTNode parent = root; while(parent.nodeKind() != NodeKind.STATEMENT) {
							 * parent = parent.parent(); } StatementNode parentStatement =
							 * (StatementNode)parent;
							 */

							root.setChild(index, cudaMallocTransform(functionCall));
							continue;
						}
					}
				}
			}

			translateCudaMallocCalls(child);
		}
	}

	/**
	 * Translates "cudaMalloc( (void**) ptrPtr, size)" to "*ptrPtr =
	 * (type)$malloc($root, size), cudaSuccess" where "type" is the type of *ptrPtr
	 * 
	 * @param cudaMallocCall a FunctionCallNode which is a call to cuda malloc
	 * @return The translated cuda malloc call as an expression node
	 */
	private ExpressionNode cudaMallocTransform(FunctionCallNode cudaMallocCall) {
		Source source = cudaMallocCall.getSource();

		/*
		 * TypeNode scope = nodeFactory.newScopeTypeNode(source);
		 * VariableDeclarationNode deviceScopeDeclaration =
		 * nodeFactory.newVariableDeclarationNode(source, identifier("deviceScope"),
		 * scope); deviceScopeDeclaration.setInitializer(nodeFactory.newFunctionCallNode
		 * (source, identifierExpression("$cuda_host_request_device_scope"), null,
		 * null));
		 */
		FunctionCallNode request_device = nodeFactory.newFunctionCallNode(source,
				identifierExpression("$cuda_host_request_device_scope"), Collections.<ExpressionNode>emptyList());

		// find the pointer
		ExpressionNode ptrPtr = cudaMallocCall.getArgument(0);
		while (ptrPtr instanceof CastNode) {
			ptrPtr = ((CastNode) ptrPtr).getArgument();
		}
		ExpressionNode size = cudaMallocCall.getArgument(1);
		// build lhs expression
		ExpressionNode assignLhs = nodeFactory.newOperatorNode(cudaMallocCall.getSource(), Operator.DEREFERENCE,
				Arrays.asList(ptrPtr.copy()));
		Type lhsType;
		if (ptrPtr.getInitialType().kind() == TypeKind.POINTER) {
			PointerType ptrType = (PointerType) ptrPtr.getInitialType();
			lhsType = ptrType.referencedType();
		} else {
			lhsType = ptrPtr.getInitialType();
		}
		// build rhs expression
		FunctionCallNode mallocCall = nodeFactory.newFunctionCallNode(source, identifierExpression(source, "$malloc"),
				Arrays.asList(request_device, size.copy()));
		CastNode mallocCast = nodeFactory.newCastNode(source, typeNode(source, lhsType), mallocCall);

		// create assign node
		OperatorNode assignment = nodeFactory.newOperatorNode(cudaMallocCall.getSource(), Operator.ASSIGN,
				Arrays.asList(assignLhs, nodeFactory.newFunctionCallNode(source, identifierExpression("$hide"),
						Arrays.asList(mallocCast))));

		/*
		 * List<BlockItemNode> transformedItems = new ArrayList<BlockItemNode>();
		 * transformedItems.add(deviceScopeDeclaration);
		 * transformedItems.add(nodeFactory.newExpressionStatementNode( assignment));
		 */

		// CompoundStatementNode newStatements =
		// nodeFactory.newCompoundStatementNode(source, transformedItems);
		// create comma node
		ExpressionNode finalExpression = nodeFactory.newOperatorNode(source, Operator.COMMA, Arrays.asList(assignment,
				nodeFactory.newEnumerationConstantNode(nodeFactory.newIdentifierNode(source, "cudaSuccess"))));

		// mallocStatement.parent().setChild(mallocStatement.childIndex(),
		// newStatements);
		// Need to figure out how to put this compound statement node above the
		// statement with the cudaMalloc expression
		// Maybe I have to copy all the statements like with the atomics

		// Return (cuda_C =
		// $hide((float*)$malloc($cuda_host_request_device_scope(),
		// size)), cudaSuccess)
		return finalExpression;
	}

	private FunctionDefinitionNode createCudaMain() {
		String srcMethod = "createCudaMain";

		List<BlockItemNode> cudaMainBody = new ArrayList<BlockItemNode>();

		createCudaMainGlobalVariables(cudaMainBody);
		createDefaultStreamIfNullFunc(cudaMainBody);
		createCudaMainWhileLoop(cudaMainBody);

		return nodeDefnFunction(srcMethod, CUDA_MAIN, voidType(), Arrays.asList(), cudaMainBody);
	}

	private void createCudaMainGlobalVariables(List<BlockItemNode> body) {
		String srcMethod = "createCudaMainGlobalVariables";

		body.add(nodeDeclVarInit(srcMethod, "$cuda_scope", nodeTypeScope(srcMethod), nodeExprHere(srcMethod)));

		body.add(nodeDeclVarInit(srcMethod, "$cuda_device_comm", nodeTypeNamed(srcMethod, "$comm"),
				nodeExprCall(srcMethod, "$comm_create", nodeExprId(srcMethod, "$cuda_scope"),
						nodeExprId(srcMethod, "$cuda_gcomm"), nodeExprInt(srcMethod, 1))));

		body.add(nodeDeclVar(srcMethod, DEVICE_GLOB_CONTEXT_NAME, nodeTypeNamed(srcMethod, "$cuda_context")));

		body.add(nodeDeclVar(srcMethod, "$cuda_default_stream", nodeTypeNamed(srcMethod, "cudaStream_t")));

		body.add(nodeDeclVarInit(srcMethod, "defaultStreamNode", nodeTypeNamed(srcMethod, "$cuda_stream_node_t"),
				nodeExprCall(srcMethod, "$create_new_stream_node", nodeExprId(srcMethod, "$cuda_scope"))));

		body.add(nodeStmtAssign(CUDA_MAIN, nodeExprId(srcMethod, "$cuda_default_stream"),
				nodeExprArrow(srcMethod, nodeExprId(srcMethod, "defaultStreamNode"), "stream")));

		body.add(nodeStmtAssign(CUDA_MAIN,
				nodeExprDot(srcMethod, nodeExprId(srcMethod, DEVICE_GLOB_CONTEXT_NAME), "headNode"),
				nodeExprId(srcMethod, "defaultStreamNode")));

		body.add(nodeStmtAssign(CUDA_MAIN,
				nodeExprDot(srcMethod, nodeExprId(srcMethod, DEVICE_GLOB_CONTEXT_NAME), "numStreams"),
				nodeExprInt(srcMethod, 1)));
	}

	private void createDefaultStreamIfNullFunc(List<BlockItemNode> body) {
		String srcMethod = "createDefaultStreamIfNullFunc";

		List<BlockItemNode> defaultStreamIfNullBody = new ArrayList<BlockItemNode>();

		defaultStreamIfNullBody.add(nodeFactory.newReturnNode(newSource(srcMethod, CivlcTokenConstant.RETURN),
				nodeExprOp(srcMethod, Operator.CONDITIONAL,
						nodeExprOp(srcMethod, Operator.EQUALS, nodeExprId(srcMethod, "stream"),
								nodeExprNullPointer(srcMethod)),
						nodeExprId(srcMethod, "$cuda_default_stream"), nodeExprId(srcMethod, "stream"))));

		body.add(nodeDefnFunction(srcMethod, "$default_stream_if_null", nodeTypeNamed(srcMethod, "cudaStream_t"),
				Arrays.asList(nodeDeclVar(srcMethod, "stream", nodeTypeNamed(srcMethod, "cudaStream_t"))),
				defaultStreamIfNullBody));
	}

	private void createCudaMainWhileLoop(List<BlockItemNode> body) {
		String srcMethod = "createCudaMainWhileLoop";

		List<BlockItemNode> loopBody = new ArrayList<BlockItemNode>();

		// TODO: Fix COMM_ANY_TAG issue, for now I will just use the int -2
		loopBody.add(nodeDeclVarInit(srcMethod, "request", nodeTypeNamed(srcMethod, "$message"),
				nodeExprCall(srcMethod, "$comm_dequeue", nodeExprId(srcMethod, "$cuda_device_comm"),
						nodeExprId(srcMethod, "$CUDA_PLACE_HOST"), nodeExprInt(srcMethod, -2))));

		loopBody.add(nodeDeclVar(srcMethod, "response", nodeTypeNamed(srcMethod, "$message")));

		loopBody.add(nodeDeclVarInit(srcMethod, "tag", nodeTypeInt(srcMethod),
				nodeExprCall(srcMethod, "$message_tag", nodeExprId(srcMethod, "request"))));

		createCudaMainSwitchStatement(loopBody);

		loopBody.add(nodeStmtCall(srcMethod, "$comm_enqueue", nodeExprId(srcMethod, "$cuda_device_comm"),
				nodeExprId(srcMethod, "response")));

		body.add(nodeFactory.newWhileLoopNode(newSource(srcMethod, CivlcTokenConstant.WHILE), booleanConstant(true),
				nodeFactory.newCompoundStatementNode(newSource(srcMethod, CivlcTokenConstant.COMPOUND_STATEMENT),
						loopBody),
				null));
	}

	private void createCudaMainSwitchStatement(List<BlockItemNode> body) {
		String srcMethod = "createCudaMainSwitchStatement";

		ArrayList<BlockItemNode> switchBody = new ArrayList<BlockItemNode>();

		//// $CUDA_TAG_SCOPE_REQUEST ////

		List<BlockItemNode> cudaTagScopeRequestBody = new ArrayList<BlockItemNode>();

		StatementNode scopeRequestAssignment = nodeStmtAssign(srcMethod, nodeExprId(srcMethod, "response"),
				nodeExprCall(srcMethod, "$message_pack", nodeExprId(srcMethod, DEVICE_PLACE_NAME),
						nodeExprId(srcMethod, HOST_PLACE_NAME),
						nodeFactory.newEnumerationConstantNode(identifier("$CUDA_TAG_SCOPE_REQUEST")),
						nodeExprOp(srcMethod, Operator.ADDRESSOF, nodeExprId(srcMethod, "$cuda_scope")),
						nodeExprSizeof(srcMethod, nodeTypeScope(srcMethod))));

		cudaTagScopeRequestBody.add(scopeRequestAssignment);
		cudaTagScopeRequestBody.add(nodeBreak(srcMethod));

		StatementNode cudaTagScopeRequestLabel = nodeSwitchLabeledStmt(srcMethod, "$CUDA_TAG_SCOPE_REQUEST",
				cudaTagScopeRequestBody);

		switchBody.add(cudaTagScopeRequestLabel);

		//// $CUDA_TAG_cudaFree ////

		List<BlockItemNode> cudaTagCudaFreeBody = new ArrayList<BlockItemNode>();

		StatementNode cudaFreeAssignment = nodeStmtAssign(srcMethod, nodeExprId(srcMethod, "response"),
				nodeExprCall(srcMethod, "$cuda_free", nodeExprId(srcMethod, "request")));

		cudaTagCudaFreeBody.add(cudaFreeAssignment);
		cudaTagCudaFreeBody.add(nodeBreak(srcMethod));

		StatementNode cudaTagCudaFreeLabel = nodeSwitchLabeledStmt(srcMethod, "$CUDA_TAG_cudaFree",
				cudaTagCudaFreeBody);

		switchBody.add(cudaTagCudaFreeLabel);

		//// $CUDA_TAG_cudaMemcpy ////

		List<BlockItemNode> cudaTagCudaMemcpyBody = new ArrayList<BlockItemNode>();

		StatementNode cudaMemcpyAssignment = nodeStmtAssign(srcMethod, nodeExprId(srcMethod, "response"),
				nodeExprCall(srcMethod, "$cuda_memcpy", nodeExprId(srcMethod, "$cuda_scope"),
						nodeExprId(srcMethod, "$cuda_default_stream"), nodeExprId(srcMethod, "request"),
						booleanConstant(false)));

		cudaTagCudaMemcpyBody.add(cudaMemcpyAssignment);
		cudaTagCudaMemcpyBody.add(nodeBreak(srcMethod));

		StatementNode cudaTagCudaMemcpyLabel = nodeSwitchLabeledStmt(srcMethod, "$CUDA_TAG_cudaMemcpy",
				cudaTagCudaMemcpyBody);

		switchBody.add(cudaTagCudaMemcpyLabel);

		//// $CUDA_TAG_cudaMemcpyAsync ////

		List<BlockItemNode> cudaTagCudaMemcpyAsyncBody = new ArrayList<BlockItemNode>();

		StatementNode cudaMemcpyAsyncAssignment = nodeStmtAssign(srcMethod, nodeExprId(srcMethod, "response"),
				nodeExprCall(srcMethod, "$cuda_memcpy", nodeExprId(srcMethod, "$cuda_scope"),
						nodeExprId(srcMethod, "$cuda_default_stream"), nodeExprId(srcMethod, "request"),
						booleanConstant(true)));

		cudaTagCudaMemcpyAsyncBody.add(cudaMemcpyAsyncAssignment);
		cudaTagCudaMemcpyAsyncBody.add(nodeBreak(srcMethod));

		StatementNode cudaTagCudaMemcpyAsyncLabel = nodeSwitchLabeledStmt(srcMethod, "$CUDA_TAG_cudaMemcpyAsync",
				cudaTagCudaMemcpyAsyncBody);

		switchBody.add(cudaTagCudaMemcpyAsyncLabel);

		//// $CUDA_TAG_cudaDeviceSynchronize ////

		List<BlockItemNode> cudaTagCudaDeviceSynchronizeBody = new ArrayList<BlockItemNode>();

		cudaTagCudaDeviceSynchronizeBody.add(nodeStmtAssign(srcMethod, nodeExprId(srcMethod, "response"),
				nodeExprCall(srcMethod, "$cuda_device_synchronize",
						nodeExprOp(srcMethod, Operator.ADDRESSOF, nodeExprId(srcMethod, DEVICE_GLOB_CONTEXT_NAME)))));

		cudaTagCudaDeviceSynchronizeBody.add(nodeBreak(srcMethod));

		StatementNode cudaTagCudaDeviceSynchronizeLabel = nodeSwitchLabeledStmt(srcMethod,
				"$CUDA_TAG_cudaDeviceSynchronize", cudaTagCudaDeviceSynchronizeBody);

		switchBody.add(cudaTagCudaDeviceSynchronizeLabel);

		//// $CUDA_TAG_LAUNCH_Kernel_X ////

		for (KernelInfo kernel : kernelMap.values()) {
			StatementNode cudaTagLaunchKernelLabel = generateCudaTagLaunchLabel(kernel.getTagName(),
					kernel.getKernelProcName());
			switchBody.add(cudaTagLaunchKernelLabel);
		}

		//// $CUDA_TAG_TEARDOWN ////

		List<BlockItemNode> cudaTagCudaTeardownBody = new ArrayList<BlockItemNode>();

		FunctionCallNode streamDestroyCall = nodeExprCall(srcMethod, "$destroy_stream_node",
				nodeExprArrow(srcMethod, nodeExprId(srcMethod, "$cuda_default_stream"), "containingNode"));

		VariableDeclarationNode teardownProcDeclaration = nodeDeclVar(srcMethod, "destructor",
				nodeTypeNamed(srcMethod, "$proc"));

		teardownProcDeclaration.setInitializer(streamDestroyCall);
		cudaTagCudaTeardownBody.add(teardownProcDeclaration);

		StatementNode waitCall = nodeFactory
				.newExpressionStatementNode(nodeExprCall(srcMethod, "$wait", nodeExprId(srcMethod, "destructor")));

		cudaTagCudaTeardownBody.add(waitCall);

		StatementNode commDestroyCall = nodeFactory.newExpressionStatementNode(
				nodeExprCall(srcMethod, "$comm_destroy", nodeExprId(srcMethod, "$cuda_device_comm")));

		cudaTagCudaTeardownBody.add(commDestroyCall);
		cudaTagCudaTeardownBody.add(nodeFactory.newReturnNode(newSource(srcMethod, CivlcTokenConstant.RETURN), null));

		StatementNode cudaTagCudaTeardownLabel = nodeSwitchLabeledStmt(srcMethod, "$CUDA_TAG_TEARDOWN",
				cudaTagCudaTeardownBody);

		switchBody.add(cudaTagCudaTeardownLabel);

		//// default ////

		List<BlockItemNode> cudaTagDefaultBody = new ArrayList<BlockItemNode>();

		String string = "\"" + "Unknown CUDA request" + "\"";

		TokenFactory tokenFactory = astFactory.getTokenFactory();
		Formation formation = tokenFactory.newTransformFormation(transformerName, "stringLiteral");
		CivlcToken ctoke = tokenFactory.newCivlcToken(CivlcTokenConstant.STRING_LITERAL, string, formation,
				TokenVocabulary.DUMMY);
		StringToken stringToken;
		StringLiteral literal = null;
		try {
			stringToken = tokenFactory.newStringToken(ctoke);
			literal = stringToken.getStringLiteral();
		} catch (SyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StatementNode assertionStatement = nodeFactory.newExpressionStatementNode(
				nodeExprCall(srcMethod, "$assert", booleanConstant(false), nodeFactory.newStringLiteralNode(
						newSource(srcMethod, CivlcTokenConstant.STRING_LITERAL), string, literal)));

		cudaTagDefaultBody.add(assertionStatement);

		StatementNode cudaTagDefaultLabel = nodeSwitchLabeledStmt(srcMethod, "default", cudaTagDefaultBody);

		switchBody.add(cudaTagDefaultLabel);

		//// Switch Statement ////

		StatementNode switchStatement = nodeFactory.newSwitchNode(newSource(srcMethod, CivlcTokenConstant.SWITCH),
				nodeExprId(srcMethod, "tag"), nodeBlock(srcMethod, switchBody));
		body.add(switchStatement);
	}

	private StatementNode nodeSwitchLabeledStmt(String srcMethod, String caseName, List<BlockItemNode> body) {
		Source labeledStmtSource = newSource(srcMethod, CivlcTokenConstant.CASE_LABELED_STATEMENT);
		SwitchLabelNode labelDecl;

		if (caseName.equals("default")) {
			labelDecl = nodeFactory.newDefaultLabelDeclarationNode(labeledStmtSource, null);
		} else {
			labelDecl = nodeFactory.newCaseLabelDeclarationNode(labeledStmtSource,
					nodeFactory.newEnumerationConstantNode(identifier(caseName)), null);
		}

		StatementNode label = nodeFactory.newLabeledStatementNode(labeledStmtSource, labelDecl,
				nodeBlock(srcMethod, body));
		return label;
	}

	private StatementNode generateCudaTagLaunchLabel(String caseName, String procName) {
		String srcMethod = "generateCudaTagLaunchLabel";
		List<BlockItemNode> cudaTagLaunchBody = new ArrayList<BlockItemNode>();

		StatementNode streamEnqueueCall = nodeFactory.newExpressionStatementNode(nodeExprCall(srcMethod,
				"$stream_enqueue", nodeExprId(srcMethod, "$cuda_scope"), nodeExprId(srcMethod, "$cuda_default_stream"),
				nodeExprId(srcMethod, "request"), nodeExprId(srcMethod, procName)));

		cudaTagLaunchBody.add(streamEnqueueCall);

		StatementNode responseAssignment = nodeStmtAssign(srcMethod, nodeExprId(srcMethod, "response"),
				nodeExprCall(srcMethod, "$message_pack", nodeExprId(srcMethod, DEVICE_PLACE_NAME),
						nodeExprId(srcMethod, HOST_PLACE_NAME), nodeExprId(srcMethod, "tag"), nullArgument(srcMethod),
						nodeExprInt(srcMethod, 0)));

		cudaTagLaunchBody.add(responseAssignment);
		cudaTagLaunchBody.add(nodeBreak(srcMethod));

		StatementNode cudaTagLaunchLabel = nodeSwitchLabeledStmt(srcMethod, caseName, cudaTagLaunchBody);

		return cudaTagLaunchLabel;
	}

	/**
	 * Finds the main function definition node underneath root and calls
	 * {@link Cuda2CIVLWorker#transformMainFunctionDefinition(FunctionDefinitionNode)}
	 * on it
	 * 
	 * @param root the root node of an Abstract Syntax Tree
	 */
	private void translateMainDefinition(SequenceNode<BlockItemNode> root) {
		String srcMethod = "translateMainDefinition";
		for (ASTNode child : root.children()) {
			if (child == null)
				continue;

			if (child.nodeKind() == NodeKind.FUNCTION_DEFINITION) {
				FunctionDefinitionNode definition = (FunctionDefinitionNode) child;

				if (definition.getName() != null && definition.getName().equals(GEN_MAIN)) {
					FunctionDefinitionNode cudaDefinition = createCudaMain();
					FunctionTypeNode cudaMainType = cudaDefinition.getTypeNode();
					List<VariableDeclarationNode> cudaMainParams = new LinkedList<>();
					for (VariableDeclarationNode param : cudaMainType.getParameters()) {
						cudaMainParams.add(param.copy());
					}
					FunctionDeclarationNode cudaMainDecl = nodeDeclFunction(srcMethod, cudaDefinition.getName(),
							cudaMainType.getReturnType().copy(), cudaMainParams);
					FunctionDefinitionNode hostDefinition = definition.copy();
					hostDefinition.setIdentifier(nodeIdent(srcMethod, HOST_MAIN));
					root.insertChildren(definition.childIndex(), Arrays.asList(cudaMainDecl, hostDefinition));
					root.addSequenceChild(cudaDefinition);
					transformMainFunctionDefinition(definition);
					return;
				}
			}
		}
	}

	/**
	 * Spawns a host $proc on the "new" main function and a device $proc on the
	 * "cuda" main function. Waits for these processes and then destroys the $gcomm
	 * that these two processes communicate with.
	 * 
	 * @param mainFunction the function definition node for the main function
	 */
	private void transformMainFunctionDefinition(FunctionDefinitionNode mainFunction) {
		String srcMethod = "transformMainFunctionDefinition";
		String hostProcName = "$host_proc" + newTemporaryVariableName();
		String deviceProcName = "$cuda_proc" + newTemporaryVariableName();
		List<BlockItemNode> newBody = new LinkedList<BlockItemNode>();

		List<ExpressionNode> hostParams = new LinkedList<>();
		for (VariableDeclarationNode mainParam : mainFunction.getTypeNode().getParameters()) {
			if (mainParam.getTypeNode().kind() == TypeNodeKind.VOID)
				continue;

			hostParams.add(nodeExprId(srcMethod, mainParam.getName()));
		}
		newBody.add(nodeDeclVarInit(srcMethod, hostProcName, nodeTypeNamed(srcMethod, "$proc"),
				nodeFactory.newSpawnNode(newSource(srcMethod, CivlcTokenConstant.SPAWN),
						nodeExprCall(srcMethod, HOST_MAIN, hostParams.toArray(new ExpressionNode[] {})))));
		newBody.add(nodeDeclVarInit(srcMethod, deviceProcName, nodeTypeNamed(srcMethod, "$proc"), nodeFactory
				.newSpawnNode(newSource(srcMethod, CivlcTokenConstant.SPAWN), nodeExprCall(srcMethod, CUDA_MAIN))));
		newBody.add(nodeStmtCall(srcMethod, "$wait", nodeExprId(srcMethod, hostProcName)));

		FunctionCallNode messagePackCall = this.nodeExprCall(srcMethod, "$message_pack",
				this.nodeExprId(srcMethod, HOST_PLACE_NAME), this.nodeExprId(srcMethod, DEVICE_PLACE_NAME),
				nodeFactory.newEnumerationConstantNode(this.identifier("$CUDA_TAG_TEARDOWN")),
				this.nullArgument(srcMethod), this.nodeExprInt(srcMethod, 0));

		FunctionCallNode commEnqueueCall = this.nodeExprCall(srcMethod, "$comm_enqueue",
				this.nodeExprId(srcMethod, HOST_COMM_NAME), messagePackCall);

		FunctionCallNode commDestroyCall = this.nodeExprCall(srcMethod, "$comm_destroy",
				this.nodeExprId(srcMethod, HOST_COMM_NAME));

		newBody.add(nodeFactory.newExpressionStatementNode(commEnqueueCall));
		newBody.add(nodeFactory.newExpressionStatementNode(commDestroyCall));

		newBody.add(nodeStmtCall(srcMethod, "$wait", nodeExprId(srcMethod, deviceProcName)));
		newBody.add(nodeStmtCall(srcMethod, "$gcomm_destroy", nodeExprId(srcMethod, "$cuda_gcomm"),
				nodeExprNullPointer(srcMethod)));

		mainFunction.setBody(nodeFactory
				.newCompoundStatementNode(newSource(srcMethod, CivlcTokenConstant.COMPOUND_STATEMENT), newBody));
	}

	private CastNode nullArgument(String srcMethod) {
		CastNode NULL = nodeExprCast(srcMethod, nodeTypePointer(srcMethod, voidType()), nodeExprInt(srcMethod, 0));
		return NULL;
	}

}
