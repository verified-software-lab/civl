package dev.civl.mc.model.common;

import static dev.civl.mc.model.IF.AbstractFunction.SpecialRelationKind.LINEAR_ORDER;
import static dev.civl.mc.model.IF.AbstractFunction.SpecialRelationKind.NONE;
import static dev.civl.mc.model.IF.AbstractFunction.SpecialRelationKind.PARTIAL_ORDER;
import static dev.civl.mc.model.IF.AbstractFunction.SpecialRelationKind.PIECEWISE_LINEAR_ORDER;
import static dev.civl.mc.model.IF.AbstractFunction.SpecialRelationKind.TREE_ORDER;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;

import dev.civl.mc.model.IF.AbstractFunction;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.Identifier;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.variable.Variable;

public class CommonAbstractFunction extends CommonFunction implements AbstractFunction {

	private SpecialRelationKind attribute = null;

	/**
	 * An abstract function.
	 * 
	 * @param source          The CIVL source of the function
	 * @param name            The name of this function.
	 * @param parameters      The list of parameters.
	 * @param returnType      The return type of this function.
	 * @param containingScope The scope containing this function.
	 * @param attribute       The nullable attribute string
	 */
	public CommonAbstractFunction(CIVLSource source, Identifier name, Scope parameterScope, List<Variable> parameters,
			CIVLType returnType, Scope containingScope, int fid, String attr) {
		super(source, true, name, parameterScope, parameters, returnType, containingScope, fid, null);
		if (attr != null) {
			switch (attr) {
			case "partial-order":
				this.attribute = PARTIAL_ORDER;
				break;
			case "tree-order":
				this.attribute = TREE_ORDER;
				break;
			case "linear-order":
				this.attribute = LINEAR_ORDER;
				break;
			case "piecewise-linear-order":
				this.attribute = PIECEWISE_LINEAR_ORDER;
				break;
			default:
				this.attribute = NONE;
			}
		} else
			this.attribute = NONE;
	}

	/* ********************** Methods from CIVLFunction ******************** */

	@Override
	public void print(String prefix, PrintStream out, boolean isDebug) {
		Iterator<Variable> iter;
		String attr = attribute.toString();

		if (attribute == NONE)
			out.println(prefix + "abstract function " + this.name());
		else
			out.println(prefix + "abstract(\"" + attr + "\") function " + name());
		out.println(prefix + "| formal parameters");
		iter = this.parameters().iterator();
		while (iter.hasNext()) {
			out.println(prefix + "| | " + iter.next().name());
		}
		this.outerScope().print(prefix + "| ", out, isDebug);
		out.flush();
	}

	@Override
	public boolean isAbstractFunction() {
		return true;
	}

	@Override
	public boolean isNormalFunction() {
		return false;
	}

	@Override
	public SpecialRelationKind getAttribute() {
		return attribute;
	}
}
