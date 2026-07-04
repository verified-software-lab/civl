/*******************************************************************************
 * Copyright (c) 2013 Stephen F. Siegel, University of Delaware.
 * 
 * This file is part of SARL.
 * 
 * SARL is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * SARL is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with SARL. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package dev.civl.sarl.prove.cvc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;

import dev.civl.sarl.IF.SARLException;
import dev.civl.sarl.IF.TheoremProverException;
import dev.civl.sarl.IF.ValidityResult;
import dev.civl.sarl.IF.config.ProverInfo;
import dev.civl.sarl.IF.config.ProverInfo.ProverKind;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.preuniverse.IF.PreUniverse;
import dev.civl.sarl.prove.IF.Prove;
import dev.civl.sarl.prove.IF.ProverFunctionInterpretation;
import dev.civl.sarl.prove.IF.TheoremProver;
import dev.civl.sarl.util.FastList;
import dev.civl.sarl.util.ProcessControl;

/**
 * An implementation of {@link TheoremProver} using one of the automated theorem
 * provers CVC3 or CVC4. Transforms a theorem proving query into the language of
 * CVC, invokes CVC through its command line interface in a new process, and
 * interprets the output.
 * 
 * @author siegel
 */
public class RobustCVCTheoremProver implements TheoremProver {

	// ************************** Static Fields *************************** //

	/**
	 * Nick-name for <code>stderr</code>, where warnings and error messages will be
	 * sent.
	 */
	public static final PrintStream err = System.err;
	// ****************************** Fields ****************************** //

	/**
	 * Info object on underlying theorem prover, which will have {@link ProverKind}
	 * either {@link ProverKind#CVC3} or {@link ProverKind#CVC4}
	 */
	private ProverInfo info;

	/**
	 * Java object for producing new OS-level processes executing a specified
	 * command.
	 */
	private ProcessBuilder processBuilder;

	/**
	 * The symbolic universe used for managing symbolic expressions. Initialized by
	 * constructor and never changes.
	 */
	private PreUniverse universe;

	/**
	 * The translation of the given context to a CVC3 expression. Created once
	 * during instantiation and never modified.
	 */
	private CVCTranslator assumptionTranslator;

	// *************************** Constructors *************************** //

	/**
	 * Constructs new CVC theorem prover for the given context.
	 * 
	 * @param universe       the controlling symbolic universe
	 * @param context        the assumption(s) the prover will use for queries
	 * @param ProverInfo     information object on the underlying theorem prover,
	 *                       which must have {@link ProverKind} either
	 *                       {@link ProverKind#CVC3} or {@link ProverKind#CVC4}
	 * @param logicFunctions a list of {@link ProverFunctionInterpretation}s which
	 *                       are the logic function definitions
	 * @throws TheoremProverException if the context contains something CVC just
	 *                                can't handle
	 */
	RobustCVCTheoremProver(PreUniverse universe, BooleanExpression context, ProverInfo info,
			ProverFunctionInterpretation[] logicFunctions) throws TheoremProverException {
		LinkedList<String> command = new LinkedList<>();

		assert universe != null;
		assert context != null;
		assert info != null;
		this.universe = universe;
		this.info = info;
		// The following is apparently necessary since the same bound symbolic
		// constant can be used in different scopes in the context; CVC*
		// requires that these map to distinct variables.
		// The CVC translator will screw up if there is a bound variable and a
		// free variable with the same name because it is caching translation
		// Also, this translator requires this assumption too now.
		context = (BooleanExpression) universe.cleanBoundVariables(context);
		this.assumptionTranslator = new CVCTranslator(universe, context, logicFunctions);
		command.add(info.getPath().getAbsolutePath());
		command.addAll(info.getOptions());
		command.add("--quiet");
		command.add("--lang=cvc4");
		command.add("--no-interactive");
		// command.add("--rewrite-divk"); // this appears to be gone in v1.8
		// solve non_linear
		// command.add("--force-logic=AUFNIRA");
		// also try "--use-theory=idl", which can sometimes solve non-linear
		// queries
		this.processBuilder = new ProcessBuilder(command);
	}

	private ValidityResult readCVCOutput(BufferedReader cvcOut, BufferedReader cvcErr) {
		try {
			String line = cvcOut.readLine();

			if (line == null) {
				if (info.getShowErrors() || info.getShowInconclusives()) {
					try {
						if (cvcErr.ready()) {
							PrintStream exp = new PrintStream(new File(universe.getErrFile()));

							printProverUnexpectedException(cvcErr, exp);
							exp.close();
						}
					} catch (IOException e) {
						printProverUnexpectedException(cvcErr, err);
					}
				}
				return Prove.RESULT_MAYBE;
			}
			line = line.trim();
			if (("valid").equals(line) || ("entailed").equals(line))
				return Prove.RESULT_YES;
			if (("invalid").equals(line) || ("not_entailed").equals(line))
				return Prove.RESULT_NO;
			if (info.getShowInconclusives()) {
				err.println(info.getFirstAlias() + " inconclusive with message: " + line);
				for (line = cvcOut.readLine(); line != null; line = cvcOut.readLine()) {
					err.println(line);
				}
			}
			if (line.startsWith("unknown"))
				return Prove.RESULT_MAYBE;
			throw new SARLException("Unexpected cvc4 output: " + line);
		} catch (IOException e) {
			if (info.getShowErrors())
				err.println("I/O error reading " + info.getFirstAlias() + " output: " + e.getMessage());
			return Prove.RESULT_MAYBE;
		}
	}

	/**
	 * Run cvc3/cvc4 to reason about the predicate
	 * 
	 * @param predicate  a boolean expression representing the predicate
	 * @param id         the ID number of the prover call
	 * @param show       true to print the CVC script
	 * @param out        the output stream
	 * @param checkUNSAT true for testing if the predicate and the context is
	 *                   unsatisfiable; false for testing if the context entails the
	 *                   predicate
	 * @return
	 */
	private ValidityResult runCVC(BooleanExpression predicate, boolean checkUNSAT, int id, boolean show,
			PrintStream out) throws TheoremProverException {
		Process process = null;
		ValidityResult result = null;

		try {
			process = processBuilder.start();
		} catch (IOException e) {
			if (info.getShowErrors())
				err.println("I/O exception reading " + info.getFirstAlias() + " output: " + e.getMessage());
			result = Prove.RESULT_MAYBE;
		}
		try {
			if (result == null) {
				PrintStream stdin = new PrintStream(process.getOutputStream());
				BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
				BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
				FastList<String> assumptionDecls = assumptionTranslator.getDeclarations();
				FastList<String> assumptionText = assumptionTranslator.getTranslation();

				predicate = (BooleanExpression) universe.cleanBoundVariables(predicate);

				CVCTranslator translator = new CVCTranslator(assumptionTranslator, predicate);
				FastList<String> predicateDecls = translator.getDeclarations();
				FastList<String> predicateText = translator.getTranslation();

				if (show) {
					String queryKind = checkUNSAT ? "check-unsat" : "";

					out.print("\n" + info.getFirstAlias() + queryKind + " predicate   " + id + ":\n");
					predicateDecls.print(out);
					predicateText.print(out);
					out.println();
					out.println();
					out.flush();
				}
				if (checkUNSAT) {
					FastList<String> unsatQuery = new FastList<>("NOT(");

					unsatQuery.append(assumptionText);
					unsatQuery.add(" AND ");
					unsatQuery.append(predicateText);
					unsatQuery.add(")");
					assumptionDecls.print(stdin);
					predicateDecls.print(stdin);
					stdin.print("QUERY ");
					unsatQuery.print(stdin);
					stdin.println(";\n");
				} else {
					assumptionDecls.print(stdin);
					stdin.print("ASSERT ");
					assumptionText.print(stdin);
					stdin.println(";");
					predicateDecls.print(stdin);
					stdin.print("QUERY ");
					predicateText.print(stdin);
					stdin.println(";\n");
				}
				stdin.flush();
				stdin.close();
				if (info.getTimeout() > 0 && !ProcessControl.waitForProcess(process, info.getTimeout())) {
					if (info.getShowErrors() || info.getShowInconclusives())
						err.println(info.getFirstAlias() + " query       " + id + ": time out");
					result = Prove.RESULT_MAYBE;
				} else {
					result = readCVCOutput(stdout, stderr);
				}
			}
		} catch (Exception e) {
			if (process != null)
				process.destroyForcibly();
			process = null;
			throw e;
		}
		if (process != null)
			process.destroyForcibly();
		return result;
	}

	@Override
	public ValidityResult valid(BooleanExpression predicate) {
		PrintStream out = universe.getOutputStream();
		int id = universe.numProverValidCalls();
		FastList<String> assumptionDecls = assumptionTranslator.getDeclarations();
		FastList<String> assumptionText = assumptionTranslator.getTranslation();
		boolean show = universe.getShowProverQueries() || info.getShowQueries();

		universe.incrementProverValidCount();
		if (show) {
			out.println();
			out.print(info.getFirstAlias() + " assumptions " + id + ":\n");
			assumptionDecls.print(out);
			assumptionText.print(out);
			out.println();
			out.flush();
		}

		ValidityResult result;

		try {
			result = runCVC(predicate, false, id, show, out);
		} catch (TheoremProverException e) {
			if (show)
				err.println("Warning: " + e.getMessage());
			result = Prove.RESULT_MAYBE;
		}
		if (show) {
			out.println(info.getFirstAlias() + " result      " + id + ": " + result);
			out.flush();
		}
		return result;
	}

	@Override
	public ValidityResult validOrModel(BooleanExpression predicate) {
		return Prove.RESULT_MAYBE;
	}

	@Override
	public String toString() {
		return "RobustCVCTheoremProver[" + info.getFirstAlias() + "]";
	}

	void printProverUnexpectedException(BufferedReader proverErr, PrintStream exp) throws IOException {
		String errline;

		do {
			errline = proverErr.readLine();
			if (errline == null)
				break;
			exp.append(errline);
		} while (errline != null);
	}

	@Override
	public ValidityResult unsat(BooleanExpression predicate) throws TheoremProverException {
		PrintStream out = universe.getOutputStream();
		int id = universe.numProverValidCalls();
		FastList<String> assumptionDecls = assumptionTranslator.getDeclarations();
		FastList<String> assumptionText = assumptionTranslator.getTranslation();
		boolean show = universe.getShowProverQueries() || info.getShowQueries();

		universe.incrementProverValidCount();
		if (show) {
			out.println();
			out.print(info.getFirstAlias() + " assumptions " + id + ":\n");
			assumptionDecls.print(out);
			assumptionText.print(out);
			out.println();
			out.flush();
		}

		ValidityResult result;

		try {
			result = runCVC(predicate, true, id, show, out);
		} catch (TheoremProverException e) {
			if (show)
				err.println("Warning: " + e.getMessage());
			result = Prove.RESULT_MAYBE;
		}
		if (show) {
			out.println(info.getFirstAlias() + " result      " + id + ": " + result);
			out.flush();
		}
		return result;
	}
}
