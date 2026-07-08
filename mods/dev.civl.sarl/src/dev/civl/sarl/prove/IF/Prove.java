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
package dev.civl.sarl.prove.IF;

import java.nio.file.Path;
import java.util.Map;

import dev.civl.sarl.IF.ModelResult;
import dev.civl.sarl.IF.SARLInternalException;
import dev.civl.sarl.IF.ValidityResult;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.config.ProverInfo;
import dev.civl.sarl.IF.config.SARLConfig;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.preuniverse.IF.PreUniverse;
import dev.civl.sarl.prove.common.CommonModelResult;
import dev.civl.sarl.prove.common.CommonValidityResult;
import dev.civl.sarl.prove.common.MultiProverFactory;
import dev.civl.sarl.prove.common.TrivialProverFactory;
import dev.civl.sarl.prove.cvc.RobustCVCTheoremProverFactory;
import dev.civl.sarl.prove.smt.SMTProverFactory;

/**
 * This is the entry point for module prove. It provides:
 * <ul>
 * <li>constants of type {@link ValidityResult} corresponding to the three
 * different kinds of validity results: {@link #RESULT_YES}, {@link #RESULT_NO},
 * and {@link #RESULT_MAYBE}</li>
 * <li>methods for producing new {@link TheoremProverFactory} instances.</li>
 * <li>various other methods dealing with prover results</li>
 * </ul>
 * 
 * @author Stephen F. Siegel
 */
public class Prove {

	/**
	 * A constant of type {@link ValidityResult} which has {@link ResultType}
	 * {@link ResultType.YES}.
	 */
	public final static ValidityResult RESULT_YES = new CommonValidityResult(ResultType.YES);

	/**
	 * A constant of type {@link ValidityResult} which has {@link ResultType}
	 * {@link ResultType.NO}.
	 */
	public final static ValidityResult RESULT_NO = new CommonValidityResult(ResultType.NO);

	/**
	 * A constant of type {@link ValidityResult} which has {@link ResultType}
	 * {@link ResultType.MAYBE}.
	 */
	public final static ValidityResult RESULT_MAYBE = new CommonValidityResult(ResultType.MAYBE);

	private final static TrivialProverFactory trivialProverFactory = new TrivialProverFactory();

	/**
	 * Constructs a new theorem prover factory based on the given configuration. A
	 * resulting prover resolves a query as follows: it starts by using the first
	 * external prover in the given config. If that result is inconclusive, it goes
	 * to the next, and so on.
	 * 
	 * @param universe the symbolic universe used to manage and produce symbolic
	 *                 expressions
	 * @param config   a SARL configuration object specifying some sequence of
	 *                 theorem provers which are available
	 * @return a new theorem prover factory which may use all of the provers
	 *         specified in the config, in order, until a conclusive result is
	 *         reached or all provers have been exhausted
	 */
	public static TheoremProverFactory newMultiProverFactory(PreUniverse universe, SARLConfig config,
			Path workingDirectory) {
		int numProvers = config.getNumProvers();
		TheoremProverFactory[] factories = new TheoremProverFactory[numProvers];
		int count = 0;

		for (ProverInfo prover : config.getProvers()) {
			factories[count] = newProverFactory(universe, prover, workingDirectory);
			count++;
		}
		return new MultiProverFactory(factories, workingDirectory, universe);
	}

	/**
	 * Constructs a new theorem prover factory based on a single underlying theorem
	 * prover.
	 * 
	 * @param universe the symbolic universe used to produce and manipulate symbolic
	 *                 expressions
	 * @param prover   a {@link ProverInfo} object providing information on the
	 *                 specific underlying theorem prover which will be used
	 * @return the new theorem prover factory based on the given prover
	 */
	public static TheoremProverFactory newProverFactory(PreUniverse universe, ProverInfo prover,
			Path workingDirectory) {
		switch (prover.getKind()) {
		case CVC4:
			return new RobustCVCTheoremProverFactory(universe, prover, workingDirectory);
		case CVC5:
			return new SMTProverFactory(universe, prover, workingDirectory);
		case Z3:
			return new SMTProverFactory(universe, prover, workingDirectory);
		case ALT_ERGO:
			return new SMTProverFactory(universe, prover, workingDirectory);
		default:
			throw new SARLInternalException("Unknown kind of theorem prover: " + prover.getKind());
		}
	}

	public static TheoremProverFactory trivialProverFactory() {
		return trivialProverFactory;
	}

	/**
	 * Returns one of the constants {@link #RESULT_YES}, {@link #RESULT_NO},
	 * {@link #RESULT_MAYBE}, corresponding to the given type.
	 * 
	 * @param type a non-null {@link ResultType}
	 * @return either {@link #RESULT_YES}, {@link #RESULT_NO}, or
	 *         {@link #RESULT_MAYBE}, depending on whether <code>type</code> is
	 *         {@link ResultType#YES}, {@link ResultType#NO}, or
	 *         {@link ResultType#MAYBE}, respectively.
	 */
	public static ValidityResult validityResult(ResultType type) {
		switch (type) {
		case YES:
			return RESULT_YES;
		case NO:
			return RESULT_NO;
		case MAYBE:
			return RESULT_MAYBE;
		default:
			throw new SARLInternalException("unreachable");
		}
	}

	/**
	 * Constructs a new {@link ModelResult} wrapping the given mapping from symbolic
	 * constants to symbolic expressions. The represents the case where a validity
	 * result is {@link ResultType#NO} and, in addition, a specific counter example
	 * has been found. The counterexample specifies a concrete value for each
	 * symbolic constant which was used in the query, in such a way that the queried
	 * predicate evaluates to <code>false</code> and the queried assumption
	 * evaluates to <code>true</code>.
	 * 
	 * @param model mapping giving concrete value to each symbolic constant
	 *              occurring in the query
	 * @return new instance of {@link ModelResult} wrapping the given
	 *         <code>mode</code>.
	 */
	public static ModelResult modelResult(Map<SymbolicConstant, SymbolicExpression> model) {
		return new CommonModelResult(model);
	}

}
