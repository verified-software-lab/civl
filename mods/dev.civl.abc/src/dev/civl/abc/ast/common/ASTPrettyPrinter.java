package dev.civl.abc.ast.common;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.GenericAssociationNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.PragmaNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.StaticAssertionNode;
import dev.civl.abc.ast.node.IF.acsl.AllocationNode;
import dev.civl.abc.ast.node.IF.acsl.AssignsOrReadsNode;
import dev.civl.abc.ast.node.IF.acsl.AssumesNode;
import dev.civl.abc.ast.node.IF.acsl.BehaviorNode;
import dev.civl.abc.ast.node.IF.acsl.CallEventNode;
import dev.civl.abc.ast.node.IF.acsl.CompositeEventNode;
import dev.civl.abc.ast.node.IF.acsl.CompositeEventNode.EventOperator;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.acsl.ContractNode.ContractKind;
import dev.civl.abc.ast.node.IF.acsl.DependsEventNode;
import dev.civl.abc.ast.node.IF.acsl.DependsEventNode.DependsEventNodeKind;
import dev.civl.abc.ast.node.IF.acsl.DependsNode;
import dev.civl.abc.ast.node.IF.acsl.EnsuresNode;
import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.acsl.GuardsNode;
import dev.civl.abc.ast.node.IF.acsl.InvariantNode;
import dev.civl.abc.ast.node.IF.acsl.MemoryEventNode;
import dev.civl.abc.ast.node.IF.acsl.ObjectOrRegionOfNode;
import dev.civl.abc.ast.node.IF.acsl.RequiresNode;
import dev.civl.abc.ast.node.IF.compound.ArrayDesignatorNode;
import dev.civl.abc.ast.node.IF.compound.CompoundInitializerNode;
import dev.civl.abc.ast.node.IF.compound.DesignationNode;
import dev.civl.abc.ast.node.IF.compound.DesignatorNode;
import dev.civl.abc.ast.node.IF.compound.FieldDesignatorNode;
import dev.civl.abc.ast.node.IF.declaration.AbstractFunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.EnumeratorDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FieldDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.declaration.TypedefDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.AlignOfNode;
import dev.civl.abc.ast.node.IF.expression.ArrayLambdaNode;
import dev.civl.abc.ast.node.IF.expression.ArrowNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.CompoundLiteralNode;
import dev.civl.abc.ast.node.IF.expression.ConstantNode;
import dev.civl.abc.ast.node.IF.expression.DotNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.GenericSelectionNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.LambdaNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.ast.node.IF.expression.ScopeOfNode;
import dev.civl.abc.ast.node.IF.expression.SizeableNode;
import dev.civl.abc.ast.node.IF.expression.SizeofNode;
import dev.civl.abc.ast.node.IF.expression.SpawnNode;
import dev.civl.abc.ast.node.IF.expression.StatementExpressionNode;
import dev.civl.abc.ast.node.IF.label.LabelNode;
import dev.civl.abc.ast.node.IF.label.OrdinaryLabelNode;
import dev.civl.abc.ast.node.IF.label.SwitchLabelNode;
import dev.civl.abc.ast.node.IF.omp.OmpDeclarativeNode;
import dev.civl.abc.ast.node.IF.omp.OmpDeclarativeNode.OmpDeclarativeNodeKind;
import dev.civl.abc.ast.node.IF.omp.OmpExecutableNode;
import dev.civl.abc.ast.node.IF.omp.OmpExecutableNode.OmpExecutableKind;
import dev.civl.abc.ast.node.IF.omp.OmpForNode;
import dev.civl.abc.ast.node.IF.omp.OmpForNode.OmpScheduleKind;
import dev.civl.abc.ast.node.IF.omp.OmpFunctionReductionNode;
import dev.civl.abc.ast.node.IF.omp.OmpNode;
import dev.civl.abc.ast.node.IF.omp.OmpNode.OmpNodeKind;
import dev.civl.abc.ast.node.IF.omp.OmpParallelNode;
import dev.civl.abc.ast.node.IF.omp.OmpReductionNode;
import dev.civl.abc.ast.node.IF.omp.OmpSimdNode;
import dev.civl.abc.ast.node.IF.omp.OmpSymbolReductionNode;
import dev.civl.abc.ast.node.IF.omp.OmpSyncNode;
import dev.civl.abc.ast.node.IF.omp.OmpSyncNode.OmpSyncNodeKind;
import dev.civl.abc.ast.node.IF.omp.OmpWorksharingNode;
import dev.civl.abc.ast.node.IF.omp.OmpWorksharingNode.OmpWorksharingNodeKind;
import dev.civl.abc.ast.node.IF.statement.AtomicNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode.BlockItemKind;
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
import dev.civl.abc.ast.node.IF.statement.LoopNode.LoopKind;
import dev.civl.abc.ast.node.IF.statement.ReturnNode;
import dev.civl.abc.ast.node.IF.statement.RunNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode.StatementKind;
import dev.civl.abc.ast.node.IF.statement.SwitchNode;
import dev.civl.abc.ast.node.IF.statement.WhenNode;
import dev.civl.abc.ast.node.IF.type.ArrayTypeNode;
import dev.civl.abc.ast.node.IF.type.BasicTypeNode;
import dev.civl.abc.ast.node.IF.type.DomainTypeNode;
import dev.civl.abc.ast.node.IF.type.EnumerationTypeNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.LambdaTypeNode;
import dev.civl.abc.ast.node.IF.type.PointerTypeNode;
import dev.civl.abc.ast.node.IF.type.StructureOrUnionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode.TypeNodeKind;
import dev.civl.abc.ast.node.IF.type.TypedefNameNode;
import dev.civl.abc.ast.node.IF.type.TypeofNode;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.err.IF.ABCRuntimeException;
import dev.civl.abc.err.IF.ABCUnsupportedException;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.Formation;
import dev.civl.abc.token.IF.Inclusion;
import dev.civl.abc.token.IF.SourceFile;
import dev.civl.abc.util.IF.Pair;

/**
 * This class implements the pretty printing of AST nodes. The purpose is to
 * print an AST in the original programming language and the output is expected
 * to be compiled into an equivalent AST.
 *
 * @author Manchun Zheng
 *
 */
public class ASTPrettyPrinter {

	/* ************************ Private Static Fields ********************** */

	private static String indention = "  ";

	private static int headerLength = 60;

	private static final StringBuffer EMPTY_STRING_BUFFER = new StringBuffer(0);

	/**
	 * Path prefix shared by all CIVL internal resource files — both the ABC header
	 * directory ({@code dev/civl/abc/include/}) and the MC library implementation
	 * directory ({@code dev/civl/mc/src/}). Files whose path starts with this
	 * prefix are CIVL-managed, not user-written code. Uses the OS-specific file
	 * separator so the check works on all platforms.
	 */
	private static final String CIVL_RESOURCE_PREFIX = "dev" + java.io.File.separator + "civl" + java.io.File.separator;

	/**
	 * Path segment identifying CIVL's ABC header directory specifically. Used to
	 * confirm a file is a header (rather than implementation) when deciding whether
	 * to emit an {@code #include} directive for it.
	 */
	private static final String ABC_LIB_PATH = new java.io.File("dev/civl/abc/include").getPath();

	/**
	 * CIVL bootstrap headers that are added automatically and should be omitted
	 * entirely from the pretty-printed output (no {@code #include} emitted).
	 */
	private static final Set<String> OMIT_ENTIRELY = new HashSet<>(
			java.util.Arrays.asList("implicit_defs.h", "gnuc.h"));

	/* ******************* Package-private Static Methods ****************** */

	/**
	 * Returns the pretty representation of the given AST node (and its descendants)
	 * in a form that should be similar to the actual programming language.
	 *
	 * @param node      the given AST node
	 * @param maxLength the maximal length of the string representation of this
	 *                  node; -1 if the length is unlimited
	 * @return the pretty representation of this AST node (and its descendants) in a
	 *         form that should be similar to the actual programming language.
	 */
	@SuppressWarnings("unchecked")
	public static StringBuffer prettyRepresentation(ASTNode node, int maxLength) {
		NodeKind kind = node.nodeKind();

		switch (kind) {
		case DECLARATION_LIST:
			return declarationList2Pretty((DeclarationListNode) node, maxLength);
		case ENUMERATOR_DECLARATION:
			return enumeratorDeclaration2Pretty((EnumeratorDeclarationNode) node, maxLength);
		case EXPRESSION:
			return expression2Pretty((ExpressionNode) node, maxLength);
		case FIELD_DECLARATION:
			return fieldDeclaration2Pretty("", (FieldDeclarationNode) node, maxLength);
		case FUNCTION_DECLARATION:
			return functionDeclaration2Pretty("", (FunctionDeclarationNode) node, maxLength);
		case FUNCTION_DEFINITION:
			return functionDeclaration2Pretty("", (FunctionDeclarationNode) node, maxLength);
		case IDENTIFIER:
			return new StringBuffer(((IdentifierNode) node).name());
		case OMP_NODE:
			return ompNode2Pretty("", (OmpNode) node, maxLength);
		case OMP_REDUCTION_OPERATOR:
			return ompReduction2Pretty((OmpReductionNode) node, maxLength);
		case ORDINARY_LABEL:
		case SWITCH_LABEL:
			return labelNode2Pretty((LabelNode) node, maxLength);
		case PRAGMA:
			return pragma2Pretty("", (PragmaNode) node, maxLength);
		case STATEMENT:
			return statement2Pretty("", (StatementNode) node, false, maxLength);
		case STATIC_ASSERTION:
			return staticAssertion2Pretty("", (StaticAssertionNode) node, maxLength);
		case TYPE:
			return type2Pretty("", (TypeNode) node, true, maxLength);
		case TYPEDEF:
			return typedefDeclaration2Pretty("", (TypedefDeclarationNode) node, maxLength);
		case VARIABLE_DECLARATION:
			return variableDeclaration2Pretty("", (VariableDeclarationNode) node, maxLength);
		case SEQUENCE:
			return sequenceNode2Pretty((SequenceNode<ASTNode>) node, maxLength);
		case PAIR:
			return pairNode2Pretty((PairNode<ASTNode, ASTNode>) node, maxLength);
		case CONTRACT:
			return contractNode2Pretty("", (ContractNode) node, maxLength);
		default:
			throw new ABCUnsupportedException(
					"the pretty printing of AST node of " + kind + " kind is not supported yet.",
					node.getSource().getLocation(false));
		}
	}

	/**
	 * Pretty print an AST node to the given output stream in a user-friendly way.
	 *
	 * @param node the node to be printed
	 * @param out  the output stream
	 */
	public static void prettyPrint(ASTNode node, PrintStream out) {
		out.print(prettyRepresentation(node, -1));
	}

	private static StringBuffer pairNode2Pretty(PairNode<ASTNode, ASTNode> pair, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		ASTNode left = pair.getLeft(), right = pair.getRight();

		result.append("(");
		if (left != null)
			result.append(prettyRepresentation(left, vacantLength(maxLength, result)));
		else
			result.append("NULL");
		result.append(",");
		if (right != null)
			result.append(prettyRepresentation(right, vacantLength(maxLength, result)));
		else
			result.append("NULL");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer sequenceNode2Pretty(SequenceNode<? extends ASTNode> sequence, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		int numChildren = sequence.numChildren();

		for (int i = 0; i < numChildren; i++) {
			ASTNode node = sequence.getSequenceChild(i);

			if (i != 0)
				result.append(", ");
			if (node != null)
				result.append(prettyRepresentation(node, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	/**
	 * Determines whether a root-level node should be replaced by an
	 * {@code #include} directive, skipped entirely, or printed as-is.
	 *
	 * @param firstToken the first token of the root-level node
	 * @return the {@code #include} directive string (e.g.
	 *         {@code "#include <stdio.h>"}) if one should be emitted; {@code null}
	 *         if the node should be skipped with no output; or the sentinel
	 *         {@code ""} if the node's content should be printed normally.
	 */
	private static String getIncludeDirective(CivlcToken firstToken) {
		SourceFile sf = firstToken.getSourceFile();
		String filename = sf.getName();
		String path = sf.getPath();

		// All CIVL internal resource files: dev/civl/abc/include/* (headers)
		// and dev/civl/mc/src/* (library implementations).
		if (path.startsWith(CIVL_RESOURCE_PREFIX)) {
			if (OMIT_ENTIRELY.contains(filename) || filename.endsWith(".cvl"))
				return null; // skip entirely
			// Headers (.h, .cvh) from the ABC include dir get an #include
			if (path.contains(ABC_LIB_PATH))
				return "#include <" + filename + ">";
			// Other CIVL resource files that aren't headers — skip
			return null;
		}

		// Check how this node's file got into the AST
		Formation formation = firstToken.getFormation();
		if (!(formation instanceof Inclusion))
			return ""; // user source file — print normally

		Inclusion inc = (Inclusion) formation;
		CivlcToken includeToken = inc.getIncludeToken();

		if (includeToken == null)
			return ""; // user source file (top-level, no #include parent)

		String includeText = includeToken.getText();

		if (includeText.startsWith("\""))
			return ""; // user include with quotes — expand inline

		// Angle-bracket include: determine if direct (emit) or transitive (skip)
		SourceFile parentSF = includeToken.getSourceFile();

		// Parent is a CIVL internal file → transitive from CIVL header → skip
		if (parentSF.getPath().startsWith(CIVL_RESOURCE_PREFIX))
			return null;

		// Parent was itself angle-bracket included → transitive system include
		Formation parentFormation = includeToken.getFormation();
		if (parentFormation instanceof Inclusion) {
			CivlcToken parentIncludeToken = ((Inclusion) parentFormation).getIncludeToken();
			if (parentIncludeToken != null && parentIncludeToken.getText().startsWith("<"))
				return null; // transitive — skip, no #include
		}

		// Direct angle-bracket include from user code (or CIVL top level)
		if (filename.endsWith(".cvl"))
			return null; // .cvl implementation files — skip entirely
		return "#include " + includeText;
	}

	static void prettyPrint(AST ast, PrintStream out, boolean ignoreStdLibs) {
		SequenceNode<BlockItemNode> root = ast.getRootNode();
		int numChildren = root.numChildren();

		// Pass 1: collect #include directives and the set of library source files
		// whose content should be suppressed.
		LinkedHashSet<String> includeDirectives = new LinkedHashSet<>();
		Set<SourceFile> librarySourceFiles = new HashSet<>();

		for (int i = 0; i < numChildren; i++) {
			BlockItemNode child = root.getSequenceChild(i);

			if (child == null)
				continue;
			CivlcToken firstToken = child.getSource().getFirstToken();
			String directive = getIncludeDirective(firstToken);

			if (directive == null) {
				// Skip entirely — no include emitted, content suppressed
				librarySourceFiles.add(firstToken.getSourceFile());
			} else if (!directive.isEmpty()) {
				// Emit an #include directive and suppress content
				includeDirectives.add(directive);
				librarySourceFiles.add(firstToken.getSourceFile());
			}
			// directive == "" means print content normally
		}

		// Emit collected #include directives
		for (String directive : includeDirectives) {
			out.println(directive);
		}
		if (!includeDirectives.isEmpty())
			out.println();

		// Pass 2: print non-library content
		String currentFile = null;

		for (int i = 0; i < numChildren; i++) {
			BlockItemNode child = root.getSequenceChild(i);

			if (child == null)
				continue;
			CivlcToken firstToken = child.getSource().getFirstToken();

			if (librarySourceFiles.contains(firstToken.getSourceFile()))
				continue;

			String sourceFile = firstToken.getSourceFile().getName();

			if (currentFile == null || !currentFile.equals(sourceFile)) {
				int fileLength = sourceFile.length();
				int leftBarLength, rightBarLength;

				rightBarLength = (headerLength - fileLength - 4) / 2;
				leftBarLength = headerLength - fileLength - 4 - rightBarLength;
				out.print("//");
				printBar(leftBarLength, '=', out);
				out.print(" ");
				out.print(sourceFile);
				out.print(" ");
				printBar(rightBarLength, '=', out);
				out.print("\n");
				currentFile = sourceFile;
			}
			int childLine = firstToken.getLine();
			if (childLine > 0) {
				out.print("// ");
				out.print(sourceFile);
				out.print(":");
				out.println(childLine);
			}
			out.print(blockItem2Pretty("", child, -1));
			out.println();
		}
	}

	private static void printBar(int length, char symbol, PrintStream out) {
		for (int i = 0; i < length; i++)
			out.print(symbol);
	}

	/**
	 * Returns the source filename (basename) of a node's first token, or
	 * {@code ""}.
	 */
	private static String sourceFileName(ASTNode node) {
		try {
			CivlcToken tok = node.getSource().getFirstToken();
			return tok != null ? tok.getSourceFile().getName() : "";
		} catch (Exception e) {
			return "";
		}
	}

	/** Returns the line number of a node's first token, or 0 if unavailable. */
	private static int sourceStartLine(ASTNode node) {
		try {
			CivlcToken tok = node.getSource().getFirstToken();
			return tok != null ? tok.getLine() : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	/** Returns the line number of a node's last token, or 0 if unavailable. */
	private static int sourceEndLine(ASTNode node) {
		try {
			CivlcToken tok = node.getSource().getLastToken();
			return tok != null ? tok.getLine() : 0;
		} catch (Exception e) {
			return 0;
		}
	}

	/* *************************** Private Methods ************************* */

	private static StringBuffer ompNode2Pretty(String prefix, OmpNode ompNode, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		OmpNodeKind kind = ompNode.ompNodeKind();

		switch (kind) {
		case DECLARATIVE:
			result.append(ompDeclarative2Pretty(prefix, (OmpDeclarativeNode) ompNode, vacantLength(maxLength, result)));
		default:// EXECUTABLE
			result.append(ompStatement2Pretty(prefix, (OmpExecutableNode) ompNode, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer structOrUnion2Pretty(String prefix, StructureOrUnionTypeNode strOrUnion,
			boolean isTypeDeclaration, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		String myIndent = prefix + indention;
		SequenceNode<FieldDeclarationNode> fields = strOrUnion.getStructDeclList();

		result.append(prefix);
		if (strOrUnion.isStruct())
			result.append("struct ");
		else
			result.append("union ");
		if (strOrUnion.getName() != null)
			result.append(strOrUnion.getName());
		if (/* isTypeDeclaration && */fields != null) {
			int numFields = fields.numChildren();

			result.append("{");
			for (int i = 0; i < numFields; i++) {
				FieldDeclarationNode field = fields.getSequenceChild(i);

				result.append("\n");
				// if (!(field.getTypeNode() instanceof
				// StructureOrUnionTypeNode))
				// result.append(myIndent);
				result.append(fieldDeclaration2Pretty(myIndent, field, vacantLength(maxLength, result)));
				result.append(";");
			}
			result.append("\n");
			result.append(prefix);
			result.append("}");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer fieldDeclaration2Pretty(String prefix, FieldDeclarationNode field, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		String type;
		StringBuffer result = new StringBuffer();
		String fieldName = field.getName();

		type = type2Pretty(prefix, field.getTypeNode(), true, maxLength).toString();
		if (type.endsWith("]")) {
			Pair<String, String> typeResult = processArrayType(type);

			result.append(typeResult.left);
			result.append(" ");
			if (fieldName != null) {
				result.append(" ");
				result.append(fieldName);
			}
			result.append(typeResult.right);
		} else {
			result.append(type);
			if (fieldName != null) {
				result.append(" ");
				result.append(field.getName());
			}
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer staticAssertion2Pretty(String prefix, StaticAssertionNode assertion, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append(prefix);
		result.append("(");
		result.append(expression2Pretty(assertion.getExpression(), vacantLength(maxLength, result)));
		result.append(", \"");
		result.append(assertion.getMessage().getStringRepresentation());
		result.append("\")");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer pragma2Pretty(String prefix, PragmaNode pragma, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		Iterable<CivlcToken> tokens = pragma.getTokens();

		result.append(prefix);
		result.append("#pragma ");
		result.append(pragma.getPragmaIdentifier().name());

		for (CivlcToken token : tokens) {
			result.append(" ");
			result.append(token.getText());
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer enumType2Pretty(String prefix, EnumerationTypeNode enumeration, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		IdentifierNode tag = enumeration.getTag();
		SequenceNode<EnumeratorDeclarationNode> enumerators = enumeration.enumerators();
		String myIndent = prefix + indention;

		result.append(prefix);
		result.append("enum ");
		if (tag != null)
			result.append(tag.name());
		if (enumerators != null) {
			int num = enumerators.numChildren();

			result.append("{");
			for (int i = 0; i < num; i++) {
				EnumeratorDeclarationNode enumerator = enumerators.getSequenceChild(i);

				if (i != 0)
					result.append(",");
				result.append("\n");
				result.append(myIndent);
				result.append(enumeratorDeclaration2Pretty(enumerator, vacantLength(maxLength, result)));
			}
			result.append("\n");
			result.append(prefix);
			result.append("}");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer enumeratorDeclaration2Pretty(EnumeratorDeclarationNode enumerator, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append(enumerator.getName());
		if (enumerator.getValue() != null) {
			result.append("=");
			result.append(expression2Pretty(enumerator.getValue(), maxLength - result.length()));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer trimStringBuffer(StringBuffer input, int length) {
		if (length > 0 && input.length() > length)
			return new StringBuffer(input.substring(0, length));
		return input;
	}

	private static StringBuffer ompDeclarative2Pretty(String prefix, OmpDeclarativeNode ompDeclarative, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		OmpDeclarativeNodeKind kind = ompDeclarative.ompDeclarativeNodeKind();

		result.append("#pragma omp ");
		switch (kind) {
		case REDUCTION:
			result.append("reduction");
			break;
		case THREADPRIVATE:
			result.append("threadprivate");
			break;
		default:
			throw new ABCUnsupportedException("The OpenMP declarative directive " + kind + " is not supported yet.",
					ompDeclarative.getSource().getLocation(false));
		}
		result.append("(");
		result.append(sequenceExpression2Pretty(ompDeclarative.variables(), vacantLength(maxLength, result)));
		result.append(")");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer functionDeclaration2Pretty(String prefix, FunctionDeclarationNode function,
			int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		SequenceNode<ContractNode> contracts = function.getContract();

		if (contracts != null && contracts.numChildren() > 0) {
			result.append(contracts2Pretty(prefix, contracts, vacantLength(maxLength, result)));
		}
		result.append(prefix);
		if (function instanceof AbstractFunctionDefinitionNode) {
			result.append("$abstract ");
		}
		if (function.hasGlobalFunctionSpecifier())
			result.append("__global__ ");
		if (function.hasAtomicFunctionSpecifier())
			result.append("$atomic_f ");
		if (function.hasSystemFunctionSpecifier()) {
			String fileName = function.getSource().getFirstToken().getSourceFile().getName();
			int dotIndex = fileName.lastIndexOf(".");

			result.append("$system");
			if (dotIndex >= 0) {
				String systemLib = function.getSystemLibrary();

				result.append("[");
				if (systemLib != null)
					result.append(systemLib);
				else
					result.append(fileName.substring(0, fileName.lastIndexOf(".")));
				result.append("] ");
			}
		}
		if (function.hasStatefFunctionSpecifier())
			result.append("$state_f ");
		if (function.hasPureFunctionSpecifier())
			result.append("$pure ");
		if (function.hasInlineFunctionSpecifier())
			result.append("inline ");
		if (function.hasNoreturnFunctionSpecifier())
			result.append("_Noreturn ");

		TypeNode typeNode = function.getTypeNode();

		if (typeNode instanceof FunctionTypeNode) {
			FunctionTypeNode functionTypeNode = (FunctionTypeNode) typeNode;
			TypeNode returnType = functionTypeNode.getReturnType();
			SequenceNode<VariableDeclarationNode> paras = functionTypeNode.getParameters();
			int numOfParas = paras.numChildren();

			result.append(type2Pretty("", returnType, false, vacantLength(maxLength, result)));
			result.append(" ");
			result.append(function.getName());
			result.append("(");
			for (int i = 0; i < numOfParas; i++) {
				if (i != 0)
					result.append(", ");
				result.append(
						variableDeclaration2Pretty("", paras.getSequenceChild(i), vacantLength(maxLength, result)));
			}
			if (functionTypeNode.hasVariableArgs())
				result.append(", ...");
			result.append(")");
		} else {
			result.append(type2Pretty("", typeNode, false, vacantLength(maxLength, result)));
			result.append(" ");
			result.append(function.getName());
		}
		if (function instanceof FunctionDefinitionNode) {
			CompoundStatementNode body = ((FunctionDefinitionNode) function).getBody();

			result.append("\n");
			result.append(compoundStatement2Pretty(prefix, body, false, vacantLength(maxLength, result)));
		} else {
			result.append(";");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer contracts2Pretty(String prefix, SequenceNode<ContractNode> contracts, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		String newLinePrefix = "\n" + prefix + "  @ ";
		int numContracts = contracts.numChildren();
		boolean isFirst = true;

		result.append(prefix);
		result.append("/*@ ");
		for (int i = 0; i < numContracts; i++) {
			ContractNode contract = contracts.getSequenceChild(i);

			if (isFirst)
				isFirst = false;
			else
				result.append(newLinePrefix);
			result.append(contractNode2Pretty(newLinePrefix, contract, vacantLength(maxLength, result)));
		}
		result.append("\n");
		result.append(prefix);
		result.append("  @*/\n");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer contractNode2Pretty(String prefix, ContractNode contract, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		ContractKind kind = contract.contractKind();

		switch (kind) {
		case ALLOCATES_OR_FREES: {
			AllocationNode allocation = (AllocationNode) contract;

			if (allocation.isAllocates())
				result.append("allocates ");
			else
				result.append("frees ");
			result.append(sequenceExpression2Pretty(allocation.memoryList(), vacantLength(maxLength, result)));
			break;
		}
		case ASSUMES: {
			AssumesNode assumes = (AssumesNode) contract;

			result.append("assumes ");
			result.append(expression2Pretty(assumes.getPredicate(), vacantLength(maxLength, result)));
			result.append(";");
			break;
		}
		case ASSIGNS_READS: {
			AssignsOrReadsNode assignsOrReads = (AssignsOrReadsNode) contract;
			// ExpressionNode condition = assignsOrReads.getCondition();

			if (assignsOrReads.isAssigns())
				result.append("assigns ");
			else
				result.append("reads ");
			result.append(sequenceNode2Pretty(assignsOrReads.getMemoryList(), vacantLength(maxLength, result)));
			result.append(";");
			break;
		}
		case DEPENDS: {
			DependsNode depends = (DependsNode) contract;

			result.append("depends_on ");
			result.append(sequenceDependsEvent2Pretty(depends.getEventList(), vacantLength(maxLength, result)));
			result.append(";");
			break;
		}
		case ENSURES: {
			EnsuresNode ensures = (EnsuresNode) contract;

			result.append("ensures ");
			result.append(expression2Pretty(ensures.getExpression(), vacantLength(maxLength, result)));
			result.append(";");
			break;
		}
		case GUARDS: {
			GuardsNode guard = (GuardsNode) contract;

			result.append("executes_when ");
			result.append(expression2Pretty(guard.getExpression(), vacantLength(maxLength, result)));
			result.append(";");
			break;
		}
		case REQUIRES: {
			RequiresNode requires = (RequiresNode) contract;

			result.append("requires ");
			result.append(expression2Pretty(requires.getExpression(), vacantLength(maxLength, result)));
			result.append(";");
			break;
		}
		case BEHAVIOR: {
			BehaviorNode behavior = (BehaviorNode) contract;
			SequenceNode<ContractNode> body = behavior.getBody();
			String indentedNewLinePrefix = prefix + "  ";

			result.append("behavior ");
			result.append(behavior.getName().name());
			result.append(":");
			for (ContractNode clause : body) {
				// result.append("\n");
				result.append(indentedNewLinePrefix);
				result.append(contractNode2Pretty(indentedNewLinePrefix, clause, vacantLength(maxLength, result)));
			}
			break;
		}
		case INVARIANT: {
			InvariantNode invariant = (InvariantNode) contract;

			if (invariant.isLoopInvariant())
				result.append("loop ");
			result.append("invariant ");
			result.append(expression2Pretty(invariant.getExpression(), vacantLength(maxLength, result)));
			result.append(";");
			break;
		}
		case PURE: {
			result.append("pure;");
			break;
		}
		default:
			throw new ABCUnsupportedException("pretty printing contract node of " + kind + " kind");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer sequenceDependsEvent2Pretty(SequenceNode<DependsEventNode> eventList, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		boolean isFirst = true;

		for (DependsEventNode event : eventList) {
			if (isFirst)
				isFirst = false;
			else
				result.append(", ");
			result.append(dependsEvent2Pretty(event, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer dependsEvent2Pretty(DependsEventNode event, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		DependsEventNodeKind kind = event.getEventKind();
		StringBuffer result = new StringBuffer();

		switch (kind) {
		case MEMORY: {
			MemoryEventNode rwEvent = (MemoryEventNode) event;

			if (rwEvent.isRead())
				result.append("\\read");
			else if (rwEvent.isWrite())
				result.append("\\write");
			else
				result.append("\\access");
			result.append("(");
			result.append(sequenceExpression2Pretty(rwEvent.getMemoryList(), vacantLength(maxLength, result)));
			result.append(")");
			break;
		}
		case COMPOSITE: {
			CompositeEventNode opEvent = (CompositeEventNode) event;
			EventOperator op = opEvent.eventOperator();

			result.append("(");
			result.append(dependsEvent2Pretty(opEvent.getLeft(), vacantLength(maxLength, result)));
			result.append(")");
			switch (op) {
			case UNION:
				result.append(" + ");
				break;
			case DIFFERENCE:
				result.append(" - ");
				break;
			case INTERSECT:
				result.append(" & ");
				break;
			default:
				throw new ABCUnsupportedException("pretty printing depends event node with " + kind + " operator");
			}
			result.append("(");
			result.append(dependsEvent2Pretty(opEvent.getRight(), vacantLength(maxLength, result)));
			result.append(")");
			break;
		}
		case CALL: {
			CallEventNode callEvent = (CallEventNode) event;
			SequenceNode<ExpressionNode> args = callEvent.arguments();

			result.append("\\call(");
			result.append(callEvent.getFunction().getIdentifier().name());
			if (args.numChildren() > 0)
				result.append(", ");
			result.append(sequenceExpression2Pretty(callEvent.arguments(), vacantLength(maxLength, result)));
			result.append(")");
			break;
		}
		case NOACT:
			result.append("\\nothing");
			break;
		case ANYACT:
			result.append("\\anyact");
			break;
		default:
			throw new ABCUnsupportedException("pretty printing depends event node of " + kind + " kind");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer compoundStatement2Pretty(String prefix, CompoundStatementNode compound,
			boolean isSwitchBody, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		int numChildren = compound.numChildren();
		String myIndent = prefix + indention;

		result.append(prefix);
		result.append("{\n");

		int i = 0;
		boolean hasPrinted = false;
		while (i < numChildren) {
			BlockItemNode child = compound.getSequenceChild(i);
			if (child == null) {
				i++;
				continue;
			}

			String file = sourceFileName(child);
			int startLine = sourceStartLine(child);

			// Extend the group as long as consecutive children share the same
			// source file and have a valid location.
			int groupEnd = i;
			int endLine = sourceEndLine(child);
			if (endLine <= 0)
				endLine = startLine;

			while (groupEnd + 1 < numChildren) {
				BlockItemNode next = compound.getSequenceChild(groupEnd + 1);
				if (next == null)
					break;
				String nextFile = sourceFileName(next);
				int nextLine = sourceStartLine(next);
				if (!file.equals(nextFile) || nextLine < endLine)
					break;
				int nextEnd = sourceEndLine(next);
				endLine = nextEnd > 0 ? nextEnd : nextLine;
				groupEnd++;
			}

			// Blank separator line before a group (not the first).
			if (hasPrinted)
				result.append("\n");

			String indent = (isSwitchBody && !(child instanceof LabeledStatementNode)) ? myIndent + indention
					: myIndent;
			result.append(indent);
			if (!file.isEmpty() && startLine > 0) {
				result.append("// ");
				result.append(file);
				result.append(":");
				if (endLine > startLine) {
					result.append(startLine);
					result.append("-");
					result.append(endLine);
				} else {
					result.append(startLine);
				}
				result.append("\n");
			} else {
				result.append("// Missing source\n");
			}

			// Print every item in the group.
			for (int j = i; j <= groupEnd; j++) {
				BlockItemNode item = compound.getSequenceChild(j);
				if (item != null) {
					String jIndent = (isSwitchBody && !(item instanceof LabeledStatementNode)) ? myIndent + indention
							: myIndent;
					result.append(blockItem2Pretty(jIndent, item, vacantLength(maxLength, result)));
					result.append("\n");
				}
			}

			hasPrinted = true;
			i = groupEnd + 1;
		}
		result.append(prefix);
		result.append("}");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer blockItem2Pretty(String prefix, BlockItemNode block, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		BlockItemKind kind = block.blockItemKind();

		switch (kind) {
		case STATEMENT:
			return statement2Pretty(prefix, (StatementNode) block, false, maxLength);
		case ORDINARY_DECLARATION:
			if (block instanceof VariableDeclarationNode) {
				result.append(variableDeclaration2Pretty(prefix, (VariableDeclarationNode) block,
						vacantLength(maxLength, result)));
				result.append(";");
			} else if (block instanceof FunctionDeclarationNode)
				return functionDeclaration2Pretty(prefix, (FunctionDeclarationNode) block, maxLength);
			break;
		case TYPEDEF:
			result.append(typedefDeclaration2Pretty(prefix, (TypedefDeclarationNode) block, maxLength));
			result.append(";");
			break;
		case ENUMERATION:
			result.append(enumType2Pretty(prefix, (EnumerationTypeNode) block, maxLength));
			result.append(";");
			break;
		case OMP_DECLARATIVE:
			return ompDeclarative2Pretty(prefix, (OmpDeclarativeNode) block, maxLength);
		case PRAGMA:
			return pragma2Pretty(prefix, (PragmaNode) block, maxLength);
		case STRUCT_OR_UNION:
			result.append(structOrUnion2Pretty(prefix, (StructureOrUnionTypeNode) block, true, maxLength));
			result.append(";");
			break;
		default:
			throw new ABCUnsupportedException("pretty print of block item node of " + kind + " kind");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer typedefDeclaration2Pretty(String prefix, TypedefDeclarationNode typedef,
			int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append(prefix);
		result.append("typedef ");
		result.append(type2Pretty(prefix, typedef.getTypeNode(), true, vacantLength(maxLength, result)));
		result.append(" ");
		result.append(typedef.getName());
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer statement2Pretty(String prefix, StatementNode statement, boolean isSwitchBody,
			int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StatementKind kind = statement.statementKind();

		switch (kind) {
		case ATOMIC:
			return atomic2Pretty(prefix, (AtomicNode) statement, maxLength);
		case COMPOUND:
			return compoundStatement2Pretty(prefix, (CompoundStatementNode) statement, isSwitchBody, maxLength);
		case EXPRESSION:
			return expressionStatement2Pretty(prefix, (ExpressionStatementNode) statement, maxLength);
		case CHOOSE:
			return chooseStatement2Pretty(prefix, (ChooseStatementNode) statement, maxLength);
		case CIVL_FOR:
			return civlForStatement2Pretty(prefix, (CivlForNode) statement, maxLength);
		case IF:
			return if2Pretty(prefix, (IfNode) statement, maxLength);
		case JUMP:
			return jump2Pretty(prefix, (JumpNode) statement, maxLength);
		case LABELED:
			return labeled2Pretty(prefix, (LabeledStatementNode) statement, maxLength);
		case LOOP:
			return loop2Pretty(prefix, (LoopNode) statement, maxLength);
		case NULL: {
			StringBuffer result = new StringBuffer();

			result.append(prefix);
			result.append(";");
			return trimStringBuffer(result, maxLength);
		}
		case OMP:
			return ompStatement2Pretty(prefix, (OmpExecutableNode) statement, maxLength);
		case RUN:
			return run2Pretty(prefix, (RunNode) statement, maxLength);
		case SWITCH:
			return switch2Pretty(prefix, (SwitchNode) statement, maxLength);
		case WHEN:
			return when2Pretty(prefix, (WhenNode) statement, maxLength);
		default:
			throw new ABCUnsupportedException("pretty print of statement node of " + kind + " kind");
		}
	}

	private static StringBuffer bodyStatement2Pretty(String prefix, StatementNode body, boolean isSwitchBody,
			int maxLength) {
		if (body.statementKind() == StatementKind.COMPOUND)
			return statement2Pretty(prefix, body, isSwitchBody, maxLength);
		return statement2Pretty(prefix + indention, body, isSwitchBody, maxLength);
	}

	private static StringBuffer chooseStatement2Pretty(String prefix, ChooseStatementNode choose, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		int numChildren = choose.numChildren();
		String myIndent = prefix + indention;

		result.append(prefix);
		result.append("$choose{\n");

		for (int i = 0; i < numChildren; i++) {
			StatementNode statement = choose.getSequenceChild(i);

			result.append(statement2Pretty(myIndent, statement, true, vacantLength(maxLength, result)));
			result.append("\n");
		}
		result.append(prefix);
		result.append("}");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer ompStatement2Pretty(String prefix, OmpExecutableNode ompStmt, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		OmpExecutableKind kind = ompStmt.ompExecutableKind();
		SequenceNode<IdentifierExpressionNode> privateList = ompStmt.privateList(),
				firstPrivateList = ompStmt.firstprivateList(), sharedList = ompStmt.sharedList(),
				copyinList = ompStmt.copyinList(), copyPrivateList = ompStmt.copyprivateList(),
				lastPrivateList = ompStmt.lastprivateList();
		SequenceNode<OmpReductionNode> reductionList = ompStmt.reductionList();
		boolean nowait = ompStmt.nowait();
		// String myIndent = prefix + indention;
		StatementNode block = ompStmt.statementNode();

		result.append(prefix);
		result.append("#pragma omp ");
		switch (kind) {
		case PARALLEL:
			result.append(ompParallel2Pretty(prefix, (OmpParallelNode) ompStmt, vacantLength(maxLength, result)));
			break;
		case SYNCHRONIZATION:
			result.append(ompSync2Pretty(prefix, (OmpSyncNode) ompStmt, vacantLength(maxLength, result)));
			break;
		case SIMD:
			result.append(ompSimd2Pretty(prefix, (OmpSimdNode) ompStmt, -1));
			break;
		default: // case WORKSHARING:
			result.append(ompWorksharing2Pretty(prefix, (OmpWorksharingNode) ompStmt, vacantLength(maxLength, result)));
			break;
		}
		if (nowait)
			result.append("nowait");
		if (privateList != null) {
			result.append("private(");
			result.append(sequenceExpression2Pretty(privateList, vacantLength(maxLength, result)));
			result.append(") ");
		}
		if (firstPrivateList != null) {
			result.append("firstprivate(");
			result.append(sequenceExpression2Pretty(firstPrivateList, vacantLength(maxLength, result)));
			result.append(") ");
		}
		if (sharedList != null) {
			result.append("shared(");
			result.append(sequenceExpression2Pretty(sharedList, vacantLength(maxLength, result)));
			result.append(") ");
		}
		if (copyinList != null) {
			result.append("copyin(");
			result.append(sequenceExpression2Pretty(copyinList, vacantLength(maxLength, result)));
			result.append(") ");
		}
		if (copyPrivateList != null) {
			result.append("copyprivate(");
			result.append(sequenceExpression2Pretty(copyPrivateList, vacantLength(maxLength, result)));
			result.append(") ");
		}
		if (lastPrivateList != null) {
			result.append("lastprivate(");
			result.append(sequenceExpression2Pretty(lastPrivateList, vacantLength(maxLength, result)));
			result.append(") ");
		}
		if (reductionList != null) {
			result.append(sequenceReduction2Pretty(reductionList, vacantLength(maxLength, result)));
		}
		if (block != null) {
			result.append("\n");
			result.append(statement2Pretty(prefix, block, false, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer ompWorksharing2Pretty(String prefix, OmpWorksharingNode ompWs, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		OmpWorksharingNodeKind kind = ompWs.ompWorkshareNodeKind();

		switch (kind) {
		case FOR: {
			OmpForNode forNode = (OmpForNode) ompWs;
			int collapse = forNode.collapse();
			OmpScheduleKind schedule = forNode.schedule();

			result.append("for ");
			if (schedule != OmpScheduleKind.NONE) {
				result.append("schedule(");
				switch (forNode.schedule()) {
				case AUTO:
					result.append("auto");
					break;
				case DYNAMIC:
					result.append("dynamic");
					break;
				case GUIDED:
					result.append("guided");
					break;
				case RUNTIME:
					result.append("runtime");
					break;
				default:// STATIC
					result.append("static");
					break;
				}
				if (forNode.chunkSize() != null) {
					result.append(", ");
					result.append(expression2Pretty(forNode.chunkSize(), vacantLength(maxLength, result)));
				}
				result.append(") ");
			}
			if (collapse > 1) {
				result.append("collapse(");
				result.append(collapse);
				result.append(") ");
			}
			if (forNode.isOrdered()) {
				result.append("ordered ");
				if (forNode.ordered() > 1)
					result.append("(" + forNode.ordered() + ") ");
			}
			break;
		}
		case SECTIONS:
			result.append("sections ");
			break;
		case SINGLE:
			result.append("single ");
			break;
		default: // case SECTION:
			result.append("section ");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer ompSync2Pretty(String prefix, OmpSyncNode ompSync, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		OmpSyncNodeKind kind = ompSync.ompSyncNodeKind();

		switch (kind) {
		case MASTER:
			result.append("master ");
			break;
		case CRITICAL:
			result.append("critical");
			if (ompSync.criticalName() != null) {
				result.append("(");
				result.append(ompSync.criticalName().name());
				result.append(")");
			}
			result.append(" ");
			break;
		case BARRIER:
			result.append("barrier ");
			break;
		case FLUSH:
			result.append("flush ");
			if (ompSync.flushedList() != null) {
				result.append("(");
				result.append(sequenceExpression2Pretty(ompSync.flushedList(), vacantLength(maxLength, result)));
				result.append(")");
			}
			break;
		case OMPATOMIC:
			result.append("atomic ");
			break;
		default:// ORDERED
			result.append("ordered ");
		}
		return trimStringBuffer(result, maxLength);
	}

	/**
	 * <p>
	 * print: <code>simd safelen(c) simdlen(c)</code> where safelen and simdlen
	 * maybe absent
	 * </p>
	 *
	 * TODO: cannot understand the arguments: "prefix" and "maxLength". This method
	 * is created by mimic-ing ompParallel2Pretty where the use of "prefix" and
	 * "maxLength" is confusing. Need better doc.
	 */
	private static StringBuffer ompSimd2Pretty(String prefix, OmpSimdNode ompSimd, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append("simd ");
		if (ompSimd.safeLen() != null)
			result.append("safelen(" + expression2Pretty(ompSimd.safeLen(), vacantLength(maxLength, result)) + ") ");
		if (ompSimd.simdLen() != null)
			result.append("simdlen(" + expression2Pretty(ompSimd.simdLen(), vacantLength(maxLength, result)) + ") ");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer ompParallel2Pretty(String prefix, OmpParallelNode para, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		ExpressionNode ifClause = para.ifClause(), numThreads = para.numThreads();
		boolean isDefaultShared = para.isDefaultShared();

		result.append("parallel ");
		if (ifClause != null) {
			result.append("if(");
			result.append(expression2Pretty(ifClause, vacantLength(maxLength, result)));
			result.append(") ");
		}
		if (numThreads != null) {
			result.append("num_threads(");
			result.append(expression2Pretty(numThreads, vacantLength(maxLength, result)));
			result.append(") ");
		}
		if (isDefaultShared)
			result.append("default(shared) ");
		else
			result.append("default(none) ");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer sequenceReduction2Pretty(SequenceNode<OmpReductionNode> sequence, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		int num = sequence.numChildren();

		for (int i = 0; i < num; i++) {
			OmpReductionNode reduction = sequence.getSequenceChild(i);

			result.append(ompReduction2Pretty(reduction, vacantLength(maxLength, result)));
			if (i < num - 1)
				result.append(" ");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer ompReduction2Pretty(OmpReductionNode reduction, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append("reduction(");
		switch (reduction.ompReductionOperatorNodeKind()) {
		case FUNCTION: {
			OmpFunctionReductionNode funcNode = (OmpFunctionReductionNode) reduction;

			result.append(expression2Pretty(funcNode.function(), maxLength));
			break;
		}
		default: // operator
		{
			OmpSymbolReductionNode symbol = (OmpSymbolReductionNode) reduction;

			switch (symbol.operator()) {
			case SUM:
				result.append("+");
				break;
			case MINUS:
				result.append("-");
				break;
			case PROD:
				result.append("*");
				break;
			case BAND:
				result.append("&");
				break;
			case BOR:
				result.append("|");
				break;
			case BXOR:
				result.append("^");
				break;
			case LAND:
				result.append("&&");
				break;
			case LOR:
				result.append("||");
				break;
			case EQV:
				result.append("==");
				break;
			case NEQ:
				result.append("!=");
				break;
			default:
				throw new ABCRuntimeException("Invalid operator for OpenMP reduction: " + symbol.operator(),
						reduction.getSource().getLocation(false));
			}
		}
		}
		result.append(": ");
		result.append(sequenceExpression2Pretty(reduction.variables(), vacantLength(maxLength, result)));
		result.append(")");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer sequenceExpression2Pretty(SequenceNode<? extends ExpressionNode> sequence,
			int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		int numExpressions = sequence.numChildren();

		for (int i = 0; i < numExpressions; i++) {
			ExpressionNode expression = sequence.getSequenceChild(i);

			if (i != 0)
				result.append(", ");
			result.append(expression2Pretty(expression, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer civlForStatement2Pretty(String prefix, CivlForNode civlFor, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		DeclarationListNode vars = civlFor.getVariables();
		int numVars = vars.numChildren();
		// StatementNode body = civlFor.getBody();

		result.append(prefix);
		if (civlFor.isParallel())
			result.append("$parfor");
		else
			result.append("$for");
		result.append(" (int ");
		for (int i = 0; i < numVars; i++) {
			if (i != 0)
				result.append(", ");
			result.append(vars.getSequenceChild(i).getName());
		}
		result.append(": ");
		result.append(expression2Pretty(civlFor.getDomain(), vacantLength(maxLength, result)));
		result.append(")");
		result.append("\n");
		result.append(bodyStatement2Pretty(prefix, civlFor.getBody(), false, vacantLength(maxLength, result)));
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer loop2Pretty(String prefix, LoopNode loop, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		LoopKind loopKind = loop.getKind();
		StringBuffer condition = new StringBuffer();
		StatementNode bodyNode = loop.getBody();
		SequenceNode<ContractNode> contracts = loop.loopContracts();

		if (contracts != null)
			result.append(contracts2Pretty(prefix, contracts, maxLength));
		if (loop.getCondition() != null)
			condition = expression2Pretty(loop.getCondition(), vacantLength(maxLength, result));
		switch (loopKind) {
		case WHILE:
			result.append(prefix);
			result.append("while (");
			result.append(condition);
			result.append(")");
			if (bodyNode == null)
				result.append(";");
			else {
				result.append("\n");
				result.append(bodyStatement2Pretty(prefix, bodyNode, false, vacantLength(maxLength, result)));
			}
			break;
		case DO_WHILE:
			result.append(prefix);
			result.append("do");
			if (bodyNode == null)
				result.append(";");
			else {
				result.append("\n");
				result.append(bodyStatement2Pretty(prefix, bodyNode, false, vacantLength(maxLength, result)));
			}
			if (bodyNode != null && !(bodyNode instanceof CompoundStatementNode)) {
				result.append("\n");
				result.append(prefix);
			}
			result.append("while (");
			result.append(condition);
			result.append(");");
			break;
		default: // case FOR:
			result.append(for2Pretty(prefix, (ForLoopNode) loop, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer atomic2Pretty(String prefix, AtomicNode atomicNode, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		result.append(prefix);
		result.append("$atomic\n");
		result.append(bodyStatement2Pretty(prefix, atomicNode.getBody(), false, vacantLength(maxLength, result)));
		return trimStringBuffer(result, maxLength);

	}

	private static StringBuffer goto2Pretty(String prefix, GotoNode go2, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		result.append(prefix);
		result.append("goto ");
		result.append(go2.getLabel().name());
		result.append(";");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer labeled2Pretty(String prefix, LabeledStatementNode labeled, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		LabelNode label = labeled.getLabel();
		StatementNode statement = labeled.getStatement();

		result.append(prefix);
		result.append(labelNode2Pretty(label, maxLength));
		result.append("\n");
		result.append(bodyStatement2Pretty(prefix, statement, false, vacantLength(maxLength, result)));
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer labelNode2Pretty(LabelNode label, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		if (label instanceof OrdinaryLabelNode) {
			OrdinaryLabelNode ordinary = (OrdinaryLabelNode) label;
			result.append(ordinary.getName());
			result.append(":");
		} else {
			// switch label
			SwitchLabelNode switchLabel = (SwitchLabelNode) label;
			boolean isDefault = switchLabel.isDefault();

			if (isDefault)
				result.append("default:");
			else {
				result.append("case ");
				result.append(expression2Pretty(switchLabel.getExpression(), vacantLength(maxLength, result)));
				result.append(":");
			}
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer switch2Pretty(String prefix, SwitchNode switchNode, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append(prefix);
		result.append("switch (");
		result.append(expression2Pretty(switchNode.getCondition(), vacantLength(maxLength, result)));
		result.append(")\n");
		result.append(bodyStatement2Pretty(prefix, switchNode.getBody(), true, vacantLength(maxLength, result)));
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer jump2Pretty(String prefix, JumpNode jump, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		JumpKind kind = jump.getKind();

		switch (kind) {
		case GOTO:
			return goto2Pretty(prefix, (GotoNode) jump, maxLength);
		case CONTINUE:
			result.append(prefix);
			result.append("continue;");
			break;
		case BREAK:
			result.append(prefix);
			result.append("break;");
			break;
		default: // case RETURN:
			return return2Pretty(prefix, (ReturnNode) jump, maxLength);
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer return2Pretty(String prefix, ReturnNode returnNode, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		ExpressionNode expr = returnNode.getExpression();

		result.append(prefix);
		result.append("return");
		if (expr != null) {
			result.append(" ");
			result.append(expression2Pretty(expr, vacantLength(maxLength, result)));
		}
		result.append(";");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer if2Pretty(String prefix, IfNode ifNode, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		ExpressionNode condition = ifNode.getCondition();
		StatementNode trueBranch = ifNode.getTrueBranch();
		StatementNode falseBranch = ifNode.getFalseBranch();

		result.append(prefix);
		result.append("if (");
		if (condition != null)
			result.append(expression2Pretty(condition, vacantLength(maxLength, result)));
		result.append(")");

		if (trueBranch == null)
			result.append(";");
		else {
			result.append("\n");
			result.append(bodyStatement2Pretty(prefix, trueBranch, false, vacantLength(maxLength, result)));
		}
		if (falseBranch != null) {
			result.append("\n");
			result.append(prefix);
			result.append("else\n");
			result.append(bodyStatement2Pretty(prefix, falseBranch, false, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer for2Pretty(String prefix, ForLoopNode loop, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		ForLoopInitializerNode init = loop.getInitializer();
		ExpressionNode condition = loop.getCondition();
		ExpressionNode incrementer = loop.getIncrementer();
		StatementNode body = loop.getBody();
		SequenceNode<ContractNode> contracts = loop.loopContracts();

		if (contracts != null)
			result.append(contracts2Pretty(prefix, contracts, maxLength));
		result.append(prefix);
		result.append("for (");
		if (init != null) {
			if (init instanceof ExpressionNode)
				result.append(expression2Pretty((ExpressionNode) init, vacantLength(maxLength, result)));
			else if (init instanceof DeclarationListNode)
				result.append(declarationList2Pretty((DeclarationListNode) init, vacantLength(maxLength, result)));
		}
		result.append("; ");
		if (condition != null) {
			result.append(expression2Pretty(condition, vacantLength(maxLength, result)));
		}
		result.append("; ");
		if (incrementer != null) {
			result.append(expression2Pretty(incrementer, vacantLength(maxLength, result)));
		}
		result.append(")");
		if (body == null)
			result.append(";");
		else {
			result.append("\n");
			result.append(bodyStatement2Pretty(prefix, body, false, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer declarationList2Pretty(DeclarationListNode list, int maxLength) {
		int num = list.numChildren();
		StringBuffer result = new StringBuffer();

		for (int i = 0; i < num; i++) {
			VariableDeclarationNode var = list.getSequenceChild(i);

			if (var == null)
				continue;
			if (i != 0)
				result.append(", ");
			result.append(variableDeclaration2Pretty("", var, maxLength - result.length()));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer expressionStatement2Pretty(String prefix, ExpressionStatementNode expr, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		result.append(prefix);
		result.append(expression2Pretty(expr.getExpression(), vacantLength(maxLength, result)));
		result.append(";");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer when2Pretty(String prefix, WhenNode when, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append(prefix);
		result.append("$when (");
		result.append(expression2Pretty(when.getGuard(), vacantLength(maxLength, result)));
		result.append(")\n");
		result.append(bodyStatement2Pretty(prefix, when.getBody(), false, vacantLength(maxLength, result)));
		return trimStringBuffer(result, maxLength);

	}

	/**
	 * Pretty printing for {@link RunNode}
	 *
	 * @param prefix    Anything will be printed before the content of the runNode
	 *                  (e.g. white spaces)
	 * @param runNode   The {@link RunNode} that will be printed
	 * @param maxLength The maximum length of a printed line
	 * @return
	 */
	private static StringBuffer run2Pretty(String prefix, RunNode runNode, int maxLength) {
		StringBuffer result = new StringBuffer();

		result.append(prefix);
		result.append("$run\n");
		result.append(bodyStatement2Pretty(prefix, runNode.getStatement(), false, vacantLength(maxLength, result)));
		return result;
	}

	static private StringBuffer variableDeclaration2Pretty(String prefix, VariableDeclarationNode variable,
			int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		InitializerNode init = variable.getInitializer();
		TypeNode typeNode = variable.getTypeNode();
		String type;
		String varName = variable.getName();

		result.append(prefix);
		if (variable.hasExternStorage())
			result.append("extern ");
		if (variable.hasAutoStorage())
			result.append("auto ");
		if (variable.hasRegisterStorage())
			result.append("register ");
		if (variable.hasStaticStorage())
			result.append("static ");
		if (variable.hasThreadLocalStorage())
			result.append("_Thread_local ");
		if (variable.hasSharedStorage())
			result.append("__shared__ ");
		type = type2Pretty("", typeNode, false, vacantLength(maxLength, result)).toString();
		if (type.endsWith("]")) {
			Pair<String, String> typeResult = processArrayType(type);

			result.append(typeResult.left);
			result.append(" ");
			if (typeNode.isRestrictQualified())
				result.append("restrict ");
			if (varName != null) {
				result.append(" ");
				result.append(varName);
			}
			result.append(typeResult.right);
		} else {
			result.append(type);
			if (typeNode.isRestrictQualified())
				result.append(" restrict");
			if (varName != null) {
				result.append(" ");
				result.append(varName);
			}
		}
		if (init != null) {
			result.append(" = ");
			result.append(initializer2Pretty(init, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static Pair<String, String> processArrayType(String type) {
		int start = type.indexOf('[');
		return new Pair<>(type.substring(0, start), type.substring(start));
	}

	private static StringBuffer initializer2Pretty(InitializerNode init, int maxLength) {
		if (init instanceof CompoundInitializerNode) {
			return compoundInitializer2Pretty((CompoundInitializerNode) init, maxLength);
		} else if (init instanceof ExpressionNode)
			return expression2Pretty((ExpressionNode) init, maxLength);
		else
			throw new ABCRuntimeException("Invalid initializer: " + init.toString());
	}

	private static StringBuffer compoundInitializer2Pretty(CompoundInitializerNode compound, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		int numPairs = compound.numChildren();

		result.append("{");
		for (int i = 0; i < numPairs; i++) {
			PairNode<DesignationNode, InitializerNode> pair = compound.getSequenceChild(i);
			DesignationNode left = pair.getLeft();
			InitializerNode right = pair.getRight();
			int numDesig = left == null ? 0 : left.numChildren();

			if (i != 0)
				result.append(", ");
			if (numDesig > 0) {
				for (int j = 0; j < numDesig; j++) {
					result.append(designator2Pretty(left.getSequenceChild(j), vacantLength(maxLength, result)));
				}
				result.append("=");
			}
			result.append(initializer2Pretty(right, vacantLength(maxLength, result)));
		}
		result.append("}");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer designator2Pretty(DesignatorNode designator, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		if (designator instanceof ArrayDesignatorNode) {
			result.append("[");
			result.append(
					expression2Pretty(((ArrayDesignatorNode) designator).getIndex(), vacantLength(maxLength, result)));
			result.append("]");
		} else {// FieldDesignatorNode
			result.append(".");
			result.append(((FieldDesignatorNode) designator).getField().name());
		}
		return trimStringBuffer(result, maxLength);
	}

	// private static void pPrintAssume(PrintStream out, String prefix,
	// AssumeNode assume) {
	// out.print(prefix);
	// out.print("$assume ");
	// out.print(expression2Pretty(assume.getExpression()));
	// out.print(";");
	// }

	private static StringBuffer expression2Pretty(ExpressionNode expression, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		if (expression == null)
			return result;

		ExpressionKind kind = expression.expressionKind();

		switch (kind) {
		case ALIGNOF: {
			AlignOfNode align = (AlignOfNode) expression;

			result.append("_Alignof(");
			result.append(type2Pretty("", align.getArgument(), false, vacantLength(maxLength, result)));
			result.append(")");
			break;
		}
		case ARRAY_LAMBDA:
			result.append(arrayLambda2Pretty((ArrayLambdaNode) expression, maxLength));
			break;
		case ARROW: {
			ArrowNode arrow = (ArrowNode) expression;

			result.append("(");
			result.append(expression2Pretty(arrow.getStructurePointer(), vacantLength(maxLength, result)));
			result.append(")");
			result.append("->");
			result.append(arrow.getFieldName().name());
			break;
		}
		case CAST: {
			CastNode cast = (CastNode) expression;
			ExpressionNode arg = cast.getArgument();
			ExpressionKind argKind = arg.expressionKind();
			boolean parenNeeded = true;

			result.append("(");
			result.append(type2Pretty("", cast.getCastType(), false, vacantLength(maxLength, result)));
			result.append(")");
			if (argKind == ExpressionKind.IDENTIFIER_EXPRESSION || argKind == ExpressionKind.CONSTANT
					|| argKind == ExpressionKind.COMPOUND_LITERAL)
				parenNeeded = false;
			if (parenNeeded)
				result.append("(");
			result.append(expression2Pretty(arg, vacantLength(maxLength, result)));
			if (parenNeeded)
				result.append(")");
			break;
		}
		case COMPOUND_LITERAL:
			result.append(compoundLiteral2Pretty((CompoundLiteralNode) expression, maxLength));
			break;
		case CONSTANT: {
			String constant = ((ConstantNode) expression).getStringRepresentation();

			if (constant.equals("\\false"))
				constant = "$false";
			else if (constant.equals("\\true"))
				constant = "$true";
			result.append(constant);
			break;
		}
		case DOT: {
			DotNode dot = (DotNode) expression;

			result.append(expression2Pretty(dot.getStructure(), maxLength));
			result.append(".");
			result.append(dot.getFieldName().name());
			break;
		}
		case FUNCTION_CALL:
			result.append(functionCall2Pretty((FunctionCallNode) expression, maxLength));
			break;
		case GENERIC_SELECTION:
			result.append(genericSelection2Pretty((GenericSelectionNode) expression, maxLength));
			break;
		case IDENTIFIER_EXPRESSION:
			result.append(((IdentifierExpressionNode) expression).getIdentifier().name());
			break;
		case OPERATOR:
			result.append(operator2Pretty((OperatorNode) expression, maxLength));
			break;
		case QUANTIFIED_EXPRESSION:
			result.append(quantifiedExpression2Pretty((QuantifiedExpressionNode) expression, maxLength));
			break;
		case REGULAR_RANGE:
			result.append(regularRange2Pretty((RegularRangeNode) expression, maxLength));
			break;
		case SCOPEOF:
			result.append("$scopeof(");
			result.append(expression2Pretty(((ScopeOfNode) expression).expression(), vacantLength(maxLength, result)));
			result.append(")");
			break;
		case SIZEOF:
			result.append("sizeof(");
			result.append(sizeable2Pretty(((SizeofNode) expression).getArgument(), vacantLength(maxLength, result)));
			result.append(")");
			break;
		case SPAWN:
			result.append("$spawn ");
			result.append(functionCall2Pretty(((SpawnNode) expression).getCall(), vacantLength(maxLength, result)));
			break;
		case RESULT:
			result.append("\\result");
			break;
		case STATEMENT_EXPRESSION:
			result.append(statementExpression2Pretty((StatementExpressionNode) expression, maxLength));
			break;
		case NOTHING:
			result.append("\\nothing");
			break;
		case WILDCARD:
			result.append("...");
			break;
		case OBJECT_OR_REGION_OF: {
			ObjectOrRegionOfNode objectRegion = (ObjectOrRegionOfNode) expression;

			if (objectRegion.isObjectOf())
				result.append("$object_of");
			else
				result.append("$region_of");
			result.append("(");
			result.append(expression2Pretty(objectRegion.operand(), vacantLength(maxLength, result)));
			result.append(")");
			break;
		}
		case EXTENDED_QUANTIFIED:
			result.append(
					extendedQuantifiedExpression2Pretty((ExtendedQuantifiedExpressionNode) expression, maxLength));
			break;
		case LAMBDA:
			result.append(lambda2Pretty((LambdaNode) expression, maxLength));
			break;
		default:
			throw new ABCUnsupportedException("pretty print of expression node of " + kind + " kind");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer lambda2Pretty(LambdaNode lambda, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append("$lambda");
		result.append(" (");
		result.append(variableDeclaration2Pretty("", lambda.freeVariable(), vacantLength(maxLength, result)));
		result.append(") ");
		result.append(expression2Pretty(lambda.lambdaFunction(), vacantLength(maxLength, result)));
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer extendedQuantifiedExpression2Pretty(ExtendedQuantifiedExpressionNode extQuantified,
			int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append(extQuantified.extQuantifier());
		result.append("(");
		result.append(expression2Pretty(extQuantified.lower(), vacantLength(maxLength, result)));
		result.append(", ");
		result.append(expression2Pretty(extQuantified.higher(), vacantLength(maxLength, result)));
		result.append(", ");
		result.append(expression2Pretty(extQuantified.function(), vacantLength(maxLength, result)));
		result.append(")");
		return result;
	}

	private static StringBuffer statementExpression2Pretty(StatementExpressionNode statementExpression, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		CompoundStatementNode compound = statementExpression.getCompoundStatement();

		result.append("(");
		result.append(compoundStatement2Pretty("", compound, false, maxLength));
		result.append(")");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer quantifiedExpression2Pretty(QuantifiedExpressionNode quantified, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		String quantifier;

		switch (quantified.quantifier()) {
		case FORALL:
			quantifier = "$forall";
			break;
		case EXISTS:
			quantifier = "$exists";
			break;
		default:// UNIFORM
			quantifier = "$uniform";
		}
		result.append("(");
		result.append(quantifier);
		result.append(" (");
		result.append(boundVariableList2Pretty(quantified.boundVariableList(), vacantLength(maxLength, result)));
		result.append(") ");
		if (quantified.restriction() != null) {
			result.append("!(");
			result.append(expression2Pretty(quantified.restriction(), vacantLength(maxLength, result)));
			result.append(") || ");
		}
		result.append("(");
		result.append(expression2Pretty(quantified.expression(), vacantLength(maxLength, result)));
		result.append("))");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer arrayLambda2Pretty(ArrayLambdaNode quantified, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append("(");
		result.append("(");
		result.append(type2Pretty("", quantified.type(), false, vacantLength(maxLength, result)));
		result.append(") ");
		result.append("$lambda");
		result.append(" (");
		result.append(boundVariableList2Pretty(quantified.boundVariableList(), vacantLength(maxLength, result)));
		if (quantified.restriction() != null) {
			result.append(" | ");
			result.append(expression2Pretty(quantified.restriction(), vacantLength(maxLength, result)));
		}
		result.append(") ");
		result.append(expression2Pretty(quantified.expression(), vacantLength(maxLength, result)));
		result.append(")");
		return trimStringBuffer(result, maxLength);
	}

	private static int vacantLength(int total, StringBuffer buf) {
		if (total == -1)
			return -1;

		int result = total - buf.length();

		return result > 0 ? result : 0;
	}

	private static StringBuffer boundVariableList2Pretty(
			SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableList,
			int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		boolean isFirstBoundVarSubList = true;

		for (PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode> boundVariableSubList : boundVariableList) {
			boolean isFirstVariable = true;

			if (isFirstBoundVarSubList)
				isFirstBoundVarSubList = false;
			else
				result.append("; ");
			for (VariableDeclarationNode variable : boundVariableSubList.getLeft()) {
				if (isFirstVariable) {
					result.append(variableDeclaration2Pretty("", variable, vacantLength(maxLength, result)));
					isFirstVariable = false;
				} else
					result.append(", " + variable.getName());
			}
			if (boundVariableSubList.getRight() != null) {
				result.append(": ");
				result.append(expression2Pretty(boundVariableSubList.getRight(), vacantLength(maxLength, result)));
			}
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer compoundLiteral2Pretty(CompoundLiteralNode compound, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		CompoundInitializerNode list = compound.getInitializerList();
		// int numPairs = list.numChildren();
		TypeNode typeNode = compound.getTypeNode();

		if (typeNode != null) {
			result.append("(");
			result.append(type2Pretty("", compound.getTypeNode(), false, vacantLength(maxLength, result)));
			result.append(")");
		}
		result.append(compoundInitializer2Pretty(list, vacantLength(maxLength, result)));
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer regularRange2Pretty(RegularRangeNode range, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		ExpressionNode step = range.getStep();

		result.append(expression2Pretty(range.getLow(), vacantLength(maxLength, result)));
		result.append(" .. ");
		result.append(expression2Pretty(range.getHigh(), vacantLength(maxLength, result)));
		if (step != null) {
			result.append(" # ");
			result.append(expression2Pretty(step, vacantLength(maxLength, result)));
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer sizeable2Pretty(SizeableNode argument, int maxLength) {
		if (argument instanceof ExpressionNode)
			return expression2Pretty((ExpressionNode) argument, maxLength);
		return type2Pretty("", (TypeNode) argument, false, maxLength);
	}

	private static StringBuffer functionCall2Pretty(FunctionCallNode call, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		int argNum = call.getNumberOfArguments();
		StringBuffer result = new StringBuffer();

		result.append(expression2Pretty(call.getFunction(), vacantLength(maxLength, result)));
		if (call.getNumberOfContextArguments() > 0) {
			result.append("<<<");
			for (int i = 0; i < call.getNumberOfContextArguments(); i++) {
				if (i > 0)
					result.append(", ");
				result.append(expression2Pretty(call.getContextArgument(i), vacantLength(maxLength, result)));
			}
			result.append(">>>");
		}
		result.append("(");
		for (int i = 0; i < argNum; i++) {
			if (i > 0)
				result.append(", ");
			result.append(expression2Pretty(call.getArgument(i), vacantLength(maxLength, result)));
		}
		result.append(")");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer genericSelection2Pretty(GenericSelectionNode genericSelect, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();

		result.append("_Generic(");
		result.append(expression2Pretty(genericSelect.getControllingExpression(), vacantLength(maxLength, result)));
		for (GenericAssociationNode assoc : genericSelect.getAssociationList()) {
			result.append(", ");
			result.append(type2Pretty("", assoc.getTypeNode(), false, vacantLength(maxLength, result)));
			result.append(": ");
			result.append(expression2Pretty(assoc.getExpressionNode(), vacantLength(maxLength, result)));
		}
		ExpressionNode defaultExpr = genericSelect.getDefaultAssociation();
		if (defaultExpr != null) {
			result.append(", default: ");
			result.append(expression2Pretty(defaultExpr, vacantLength(maxLength, result)));
		}
		result.append(")");
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer operator2Pretty(OperatorNode operator, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		Operator op = operator.getOperator();
		ExpressionNode argNode0 = operator.getArgument(0);
		ExpressionNode argNode1 = operator.numChildren() > 1 ? operator.getArgument(1) : null;
		StringBuffer arg0Buf = expression2Pretty(argNode0, maxLength);
		String arg0 = arg0Buf.toString();
		String arg1 = argNode1 != null ? expression2Pretty(argNode1, vacantLength(maxLength, arg0Buf)).toString()
				: null;
		String argWtP0 = arg0, argWtP1 = arg1;

		if (argNode0.expressionKind() == ExpressionKind.OPERATOR)
			argWtP0 = "(" + arg0 + ")";
		if (argNode1 != null && argNode1.expressionKind() == ExpressionKind.OPERATOR)
			argWtP1 = "(" + arg1 + ")";
		switch (op) {
		case ADDRESSOF:
			result.append("&(");
			result.append(arg0);
			result.append(")");
			break;
		case APPLY:
			result.append("(");
			result.append(arg0);
			result.append(")");
			result.append("(");
			result.append(arg1);
			result.append(")");
			break;
		case ASSIGN:
			result.append(arg0);
			result.append(" = ");
			result.append(arg1);
			break;
		case HASH:
			result.append(arg0);
			result.append("#");
			result.append(arg1);
			break;
		case BIG_O:
			result.append("$O(");
			result.append(arg0);
			result.append(")");
			break;
		case BITAND:
			result.append(argWtP0);
			result.append(" & ");
			result.append(argWtP1);
			break;
		case BITANDEQ:
			result.append(argWtP0);
			result.append(" &= ");
			result.append(argWtP1);
			break;
		case BITCOMPLEMENT:
			result.append("~");
			result.append(argWtP0);
			break;
		case BITEQUIV:
			result.append(argWtP0);
			result.append("<-->");
			result.append(argWtP1);
			break;
		case BITIMPLIES:
			result.append(argWtP0);
			result.append("-->");
			result.append(argWtP1);
			break;
		case BITOR:
			result.append(argWtP0);
			result.append(" | ");
			result.append(argWtP1);
			break;
		case BITOREQ:
			result.append(argWtP0);
			result.append(" |= ");
			result.append(argWtP1);
			break;
		case BITXOR:
			result.append(argWtP0);
			result.append(" ^ ");
			result.append(argWtP1);
			break;
		case BITXOREQ:
			result.append(argWtP0);
			result.append(" ^= ");
			result.append(argWtP1);
			break;
		case COMMA:
			result.append(arg0);
			result.append(", ");
			result.append(arg1);
			break;
		case CONDITIONAL:
			result.append(arg0);
			result.append(" ? ");
			result.append(arg1);
			result.append(" : ");
			result.append(expression2Pretty(operator.getArgument(2), vacantLength(maxLength, result)));
			break;
		case DEREFERENCE:
			result.append("*");
			result.append(arg0);
			break;
		case DIV:
			result.append(argWtP0);
			result.append(" / ");
			result.append(argWtP1);
			break;
		case DIVEQ:
			result.append(argWtP0);
			result.append(" /= ");
			result.append(argWtP1);
			break;
		case EQUALS:
			result.append(argWtP0);
			result.append(" == ");
			result.append(argWtP1);
			break;
		case GT:
			result.append(argWtP0);
			result.append(" > ");
			result.append(argWtP1);
			break;
		case GTE:
			result.append(argWtP0);
			result.append(" >= ");
			result.append(argWtP1);
			break;
		case IMPLIES:
			result.append(argWtP0);
			result.append(" => ");
			result.append(argWtP1);
			break;
		case LAND:
			result.append(argWtP0);
			result.append(" && ");
			result.append(argWtP1);
			break;
		case LOR:
			result.append(argWtP0);
			result.append(" || ");
			result.append(argWtP1);
			break;
		case LXOR:
			result.append(argWtP0);
			result.append(" ^^ ");
			result.append(argWtP1);
			break;
		case LEQ:
			result.append(argWtP0);
			result.append(" <==> ");
			result.append(argWtP1);
			break;
		case LT:
			result.append(argWtP0);
			result.append(" < ");
			result.append(argWtP1);
			break;
		case LTE:
			result.append(argWtP0);
			result.append(" <= ");
			result.append(argWtP1);
			break;
		case MINUS:
			result.append(argWtP0);
			result.append(" - ");
			result.append(argWtP1);
			break;
		case MINUSEQ:
			result.append(argWtP0);
			result.append(" -= ");
			result.append(argWtP1);
			break;
		case MOD:
			result.append(argWtP0);
			result.append(" % ");
			result.append(argWtP1);
			break;
		case MODEQ:
			result.append(argWtP0);
			result.append(" %= ");
			result.append(argWtP1);
			break;
		case NEQ:
			result.append(argWtP0);
			result.append(" != ");
			result.append(argWtP1);
			break;
		case NOT:
			result.append("!");
			result.append(argWtP0);
			break;
		case OLD:
			result.append("\\old(");
			result.append(arg0);
			result.append(")");
			break;
		case PLUS:
			result.append(argWtP0);
			result.append(" + ");
			result.append(argWtP1);
			break;
		case PLUSEQ:
			result.append(arg0);
			result.append(" += ");
			result.append(arg1);
			break;
		case POSTDECREMENT:
			result.append(arg0);
			result.append("--");
			break;
		case POSTINCREMENT:
			result.append(arg0);
			result.append("++");
			break;
		case PREDECREMENT:
			result.append("--");
			result.append(arg0);
			break;
		case PREINCREMENT:
			result.append("++");
			result.append(arg0);
			break;
		case SHIFTLEFT:
			result.append(argWtP0);
			result.append(" << ");
			result.append(argWtP1);
			break;
		case SHIFTLEFTEQ:
			result.append(argWtP0);
			result.append(" <<= ");
			result.append(argWtP1);
			break;
		case SHIFTRIGHT:
			result.append(argWtP0);
			result.append(" >> ");
			result.append(argWtP1);
			break;
		case SHIFTRIGHTEQ:
			result.append(argWtP0);
			result.append(" >>= ");
			result.append(argWtP1);
			break;
		case SUBSCRIPT:
			result.append(arg0);
			result.append("[");
			result.append(arg1);
			result.append("]");
			break;
		case TIMES:
			result.append(argWtP0);
			result.append(" * ");
			result.append(argWtP1);
			break;
		case TIMESEQ:
			result.append(argWtP0);
			result.append(" -= ");
			result.append(argWtP1);
			break;
		case UNARYMINUS:
			result.append("-");
			result.append(argWtP0);
			break;
		case UNARYPLUS:
			result.append("+");
			result.append(argWtP0);
			break;
		case VALID:
			result.append("\\valid(");
			result.append(arg0);
			result.append(")");
			break;
		default:
			throw new ABCUnsupportedException("pretty print of operator node of " + op + " kind");
		}
		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer type2Pretty(String prefix, TypeNode type, boolean isTypeDeclaration, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		TypeNodeKind kind = type.kind();

		result.append(prefix);

		if (type.isInputQualified())
			result.append("$input ");
		if (type.isOutputQualified())
			result.append("$output ");
		if (type.isAtomicQualified())
			result.append("_Atomic ");
		if (type.isVolatileQualified())
			result.append("volatile ");

		switch (kind) {
		case ARRAY: {
			StringBuffer extentBuffer = new StringBuffer();
			while (type.kind() == TypeNodeKind.ARRAY) {
				ArrayTypeNode arrayType = (ArrayTypeNode) type;
				ExpressionNode extent = arrayType.getExtent();
				extentBuffer.append("[");
				if (extent != null)
					extentBuffer.append(expression2Pretty(extent, vacantLength(maxLength, result)));
				extentBuffer.append("]");
				type = arrayType.getElementType();
			}
			// result.append("(");
			result.append(type2Pretty("", type, isTypeDeclaration, maxLength));
			result.append(extentBuffer);
			// result.append(")");

		}
			break;
		case DOMAIN: {
			DomainTypeNode domainType = (DomainTypeNode) type;
			ExpressionNode dim = domainType.getDimension();

			result.append("$domain");
			if (dim != null) {
				result.append("(");
				result.append(expression2Pretty(dim, vacantLength(maxLength, result)));
				result.append(")");
			}
			break;
		}
		case MEM:
			result.append(prefix + "$mem");
			break;
		case LAMBDA:
			LambdaTypeNode lambdaType = (LambdaTypeNode) type;
			result.append(prefix + "$lambda(");
			result.append(type2Pretty("", lambdaType.freeVariableType(), false, vacantLength(maxLength, result)));
			result.append(":");
			result.append(type2Pretty("", lambdaType.lambdaFunctionType(), false, vacantLength(maxLength, result)));
			result.append(")");
			break;
		case VOID:
			result.append("void");
			break;
		case BASIC:
			result.append(basicType2Pretty((BasicTypeNode) type, maxLength));
			break;
		case ENUMERATION:
			EnumerationTypeNode enumType = (EnumerationTypeNode) type;
			return enumType2Pretty(prefix, enumType, maxLength);
		case STRUCTURE_OR_UNION: {
			StructureOrUnionTypeNode strOrUnion = (StructureOrUnionTypeNode) type;

			return structOrUnion2Pretty(prefix, strOrUnion, isTypeDeclaration, maxLength);
		}
		case POINTER:
			PointerTypeNode ptrType = ((PointerTypeNode) type);
			result.append(type2Pretty("", ptrType.referencedType(), isTypeDeclaration, maxLength));
			result.append("*");
			break;
		case TYPEDEF_NAME:
			result.append(((TypedefNameNode) type).getName().name());
			break;
		case SCOPE:
			result.append("$scope");
			break;
		case FUNCTION: {
			FunctionTypeNode funcType = (FunctionTypeNode) type;
			SequenceNode<VariableDeclarationNode> paras = funcType.getParameters();
			int i = 0;

			result.append(" (");
			result.append(type2Pretty(prefix, funcType.getReturnType(), false, maxLength - 1));
			result.append(" (");
			for (VariableDeclarationNode para : paras) {
				if (i != 0)
					result.append(", ");
				result.append(variableDeclaration2Pretty("", para, vacantLength(maxLength, result)));
				i++;
			}
			result.append(")");
			result.append(")");
			break;
		}
		case RANGE:
			result.append("$range");
			break;
		case TYPEOF:
			result.append("typeof(");
			result.append(
					expression2Pretty(((TypeofNode) type).getExpressionOperand(), vacantLength(maxLength, result)));
			result.append(")");
			break;
		default:
			throw new ABCUnsupportedException("pretty print of type node of " + kind + " kind");
		}

		if (type.isConstQualified())
			result.append(" const");

		return trimStringBuffer(result, maxLength);
	}

	private static StringBuffer basicType2Pretty(BasicTypeNode type, int maxLength) {
		if (maxLength == 0)
			return EMPTY_STRING_BUFFER;

		StringBuffer result = new StringBuffer();
		BasicTypeKind basicKind = type.getBasicTypeKind();

		switch (basicKind) {
		case BOOL:
			result.append("_Bool");
			break;
		case CHAR:
			result.append("char");
			break;
		case DOUBLE:
			result.append("double");
			break;
		case DOUBLE_COMPLEX:
			result.append("double _Complex");
			break;
		case FLOAT:
			result.append("float");
			break;
		case FLOAT_COMPLEX:
			result.append("float _Complex");
			break;
		case INT:
			result.append("int");
			break;
		case LONG:
			result.append("long");
			break;
		case LONG_DOUBLE:
			result.append("long double");
			break;
		case LONG_DOUBLE_COMPLEX:
			result.append("long double _Complex");
			break;
		case LONG_LONG:
			result.append("long long");
			break;
		case REAL:
			result.append("$real");
			break;
		case SHORT:
			result.append("short");
			break;
		case SIGNED_CHAR:
			result.append("signed char");
			break;
		case UNSIGNED:
			result.append("unsigned");
			break;
		case UNSIGNED_CHAR:
			result.append("unsigned char");
			break;
		case UNSIGNED_LONG:
			result.append("unsigned long");
			break;
		case UNSIGNED_LONG_LONG:
			result.append("unsigned long long");
			break;
		case UNSIGNED_SHORT:
			result.append("unsigned short");
			break;
		default:
			throw new ABCUnsupportedException("pretty print of basic type node of " + basicKind + " kind");
		}
		return trimStringBuffer(result, maxLength);
	}
}
