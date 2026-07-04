package dev.civl.sarl.prove.common;

import java.io.PrintStream;

import dev.civl.sarl.IF.TheoremProverException;
import dev.civl.sarl.IF.ValidityResult;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.preuniverse.IF.PreUniverse;
import dev.civl.sarl.prove.IF.Prove;
import dev.civl.sarl.prove.IF.TheoremProver;

/**
 * An implementation of {@link TheoremProver} which wraps a sequence of
 * underlying {@link TheoremProver}s. To determine validity of a formula, this
 * prover invokes the underlying provers in sequence, until a conclusive result
 * is obtained.
 *
 * @author Stephen F. Siegel
 */
public class MultiProver implements TheoremProver {

	/** Width of the borders used to "box" a query group in the output. */
	private static final int BORDER_WIDTH = 72;

	/** Heavy border printed above the reason and below the prover queries. */
	private static final String OUTER_BORDER = "=".repeat(BORDER_WIDTH);

	/** Light border printed between the reason and the prover queries. */
	private static final String INNER_BORDER = "-".repeat(BORDER_WIDTH);

	private TheoremProver[] provers;

	/**
	 * The controlling symbolic universe. Used to determine whether prover queries
	 * should be shown and, if so, to obtain the "query explanation": a
	 * program-level description of why the query is being generated.
	 */
	private PreUniverse universe;

	public MultiProver(TheoremProver[] provers, PreUniverse universe) {
		this.provers = provers;
		this.universe = universe;
	}

	/**
	 * If prover queries are being shown, opens a "box" for this query group: a
	 * heavy top border, the "query explanation" describing why this query is being
	 * generated, and a light border separating the reason from the prover queries
	 * that follow. If no explanation was set by the caller, a fallback is printed
	 * so that every shown query is annotated (making it easy to spot sites that
	 * still need a specific explanation). The explanation is then cleared so a
	 * subsequent, un-annotated query does not inherit a stale reason.
	 *
	 * @param queryKind a short description of the kind of query, e.g. "validity"
	 *                  or "satisfiability"
	 * @return {@code true} iff prover queries are being shown (in which case the
	 *         caller must call {@link #endQuery(boolean)} to close the box)
	 */
	private boolean beginQuery(String queryKind) {
		if (!universe.getShowProverQueries())
			return false;

		PrintStream out = universe.getOutputStream();
		String explanation = universe.getQueryExplanation();

		out.println();
		out.println(OUTER_BORDER);
		if (explanation != null)
			out.println("Query reason (" + queryKind + "): " + explanation);
		else
			out.println("Query reason (" + queryKind + "): <unannotated query>");
		out.println(INNER_BORDER);
		out.flush();
		// clear so an un-annotated later query does not inherit a stale reason
		universe.setQueryExplanation(null);
		return true;
	}

	/**
	 * Closes the "box" opened by {@link #beginQuery(String)} with a heavy bottom
	 * border, so the reason and all of its constituent prover queries are visually
	 * grouped together.
	 *
	 * @param show the value returned by the matching {@link #beginQuery(String)}
	 */
	private void endQuery(boolean show) {
		if (!show)
			return;

		PrintStream out = universe.getOutputStream();

		out.println(OUTER_BORDER);
		out.println();
		out.flush();
	}

	@Override
	public ValidityResult valid(BooleanExpression predicate) {
		boolean show = beginQuery("validity");
		ValidityResult result = Prove.RESULT_MAYBE;

		for (TheoremProver prover : provers) {
			ValidityResult r = prover.valid(predicate);

			if (r.getResultType() != ResultType.MAYBE) {
				result = r;
				break;
			}
		}
		endQuery(show);
		return result;
	}

	@Override
	public ValidityResult validOrModel(BooleanExpression predicate) {
		boolean show = beginQuery("validity+model");
		ValidityResult result = Prove.RESULT_MAYBE;

		for (TheoremProver prover : provers) {
			ValidityResult r = prover.validOrModel(predicate);

			if (r.getResultType() != ResultType.MAYBE) {
				result = r;
				break;
			}
		}
		endQuery(show);
		return result;
	}

	@Override
	public ValidityResult unsat(BooleanExpression predicate)
			throws TheoremProverException {
		boolean show = beginQuery("satisfiability");
		ValidityResult result = Prove.RESULT_MAYBE;

		for (TheoremProver prover : provers) {
			ValidityResult r = prover.unsat(predicate);

			if (r.getResultType() != ResultType.MAYBE) {
				result = r;
				break;
			}
		}
		endQuery(show);
		return result;
	}
}
