package dev.civl.mc;

import static dev.civl.mc.TestConstants.QUIET;
import static dev.civl.mc.TestConstants.VERIFY;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import dev.civl.abc.err.IF.ABCException;
import dev.civl.mc.run.IF.UserInterface;
public class TicketsFixTest {
	@SuppressWarnings("exports")
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);

	/* *************************** Static Fields *************************** */

	private static File rootDir = new File("examples/tickets/");

	private static UserInterface ui = new UserInterface();

	/* *************************** Helper Methods *************************** */

	private static String filename(String name) {
		return new File(rootDir, name).getPath();
	}

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	/* **************************** Test Methods *************************** */

	/**
	 * See: https://vsl.cis.udel.edu/trac/civl/ticket/913#comment:4
	 *
	 * @author Wenhao Wu (wuwenhao@udel.edu)
	 * @throws ABCException
	 */
	@Test
	public void ticket_913_inaccurate_array_index_error_message_simple()
			throws ABCException {
		assertFalse(ui.run(VERIFY, QUIET, filename(
				"ticket_913_inaccurate_array_index_error_message_simple.cvl")));
	}

	/**
	 * See: https://vsl.cis.udel.edu/trac/civl/ticket/919 </br>
	 * This test executes the example mentioned in the ticket.
	 * 
	 * @author Wenhao Wu (wuwenhao@udel.edu)
	 * @throws ABCException
	 */
	@Test
	public void ticket_919_char_to_int_cast1() throws ABCException {
		assertFalse(ui.run(VERIFY, QUIET,
				filename("ticket_919_char_to_int_cast1.cvl")));
	}

	/**
	 * See: https://vsl.cis.udel.edu/trac/civl/ticket/919#comment:8 </br>
	 * This test executes an additional example to verify the correctness of the
	 * solution.
	 * 
	 * @author Wenhao Wu (wuwenhao@udel.edu)
	 * @throws ABCException
	 */
	@Test
	public void ticket_919_char_to_int_cast2() throws ABCException {
		assertTrue(ui.run(VERIFY, QUIET,
				filename("ticket_919_char_to_int_cast2.cvl")));
	}

	/**
	 * See: https://vsl.cis.udel.edu/trac/civl/ticket/853 </br>
	 * 
	 * @author Wenhao Wu (wuwenhao@udel.edu)
	 * @throws ABCException
	 */
	@Test
	public void ticket_853_valid_arg_for_pow() throws ABCException {
		assertFalse(ui.run(VERIFY, QUIET,
				filename("ticket_853_valid_arg_for_pow.cvl")));
	}

	/**
	 * See: https://vsl.cis.udel.edu/trac/civl/ticket/954 </br>
	 * 
	 * @author Wenhao Wu (wuwenhao@udel.edu)
	 * @throws ABCException
	 */
	@Test
	public void ticket_954_MPI_handle_struct_disabled_in_equ_expr()
			throws ABCException {
		assertTrue(ui.run(VERIFY, QUIET,
				// "-showAmpleSet", "-showTransitions",
				"-input_mpi_nprocs=3", filename(
						"ticket_954_MPI_handle_struct_disabled_in_equ_expr.cvl")));
	}

	/**
	 * See: https://github.com/verified-software-lab/civl/issues/17 </br>
	 * Decimal literals were converted to the wrong exact real, because the
	 * double value of the literal was used to construct the real.
	 */
	@Test
	public void issue_17_imprecise_real_literals() throws ABCException {
		assertTrue(ui.run(VERIFY, QUIET, filename("issue_17_imprecise_real_literals.cvl")));
	}

	/**
	 * See: https://vsl.cis.udel.edu/trac/civl/ticket/943 </br>
	 *
	 * @author Alex Wilton (awilton@udel.edu)
	 * @throws ABCException
	 */
	@Test
	public void ticket_943_short_circuit() throws ABCException {
		assertTrue(ui.run(VERIFY, QUIET,
				filename("ticket_943_short_circuit.cvl")));
	}
}
