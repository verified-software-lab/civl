package dev.civl.mc.transform;

import static dev.civl.mc.TestConstants.NO_PRINTF;
import static dev.civl.mc.TestConstants.QUIET;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import dev.civl.mc.run.IF.UserInterface;

public class MPICollectivePart1Test {
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);

	/* *************************** Static Fields *************************** */

	private static File rootDir = new File(new File(new File("examples"), "mpi"), "collective");

	private static UserInterface ui = new UserInterface();

	/* *************************** Helper Methods ************************** */

	private static String filename(String name) {
		return new File(rootDir, name).getPath();
	}

	/* **************************** Test Methods *************************** */

	@Test
	public void vectorSum() {
		assertTrue(ui.run("verify -input_mpi_nprocs=5", QUIET, NO_PRINTF, "-showAmpleSetWtStates",
				filename("vectorSum.c")));
	}

	@Test
	public void vectorSum_bad() {
		assertFalse(ui.run("verify -input_mpi_nprocs=5", QUIET, NO_PRINTF, filename("vectorSum_bad.c")));
	}

	@Test
	public void bcast_bad() {
		assertFalse(ui.run("verify -input_mpi_nprocs=6 ", QUIET, filename("bcast_bad.c")));
	}

	@Test
	public void bcast_bad_but_ok() {
		assertTrue(ui.run("verify -input_mpi_nprocs=4 ", QUIET, filename("bcast_bad.c")));
	}

	@Test
	public void bcast_good() {
		assertFalse(ui.run("verify -DFASSERT -input_mpi_nprocs=3 ", QUIET, NO_PRINTF, filename("bcast_good.c")));
		assertTrue(ui.run("verify -input_mpi_nprocs=3", QUIET, NO_PRINTF, filename("bcast_good.c")));
	}

	@Test
	public void BcastReduce() {
		assertTrue(ui.run("verify -input_mpi_nprocs=5 ", QUIET, filename("BcastReduce.c")));
	}

	@Test
	public void BcastReduce_bad() {
		assertFalse(ui.run("verify -input_mpi_nprocs=10 ", QUIET, filename("BcastReduce_bad.c")));
	}

	@Test
	public void BcastReduce2() {
		assertTrue(ui.run("verify -input_mpi_nprocs=5 ", QUIET, filename("BcastReduce2.c")));
	}

	@Test
	public void BcastReduce2_bad() {
		assertFalse(ui.run("verify -input_mpi_nprocs=6 ", QUIET, filename("BcastReduce2.c")));
	}

	@Test
	public void gather_order() {
		assertFalse(ui.run("verify -input_mpi_nprocs=6 ", QUIET, filename("gather.c")));
	}

	@Test
	public void gather_type() {
		assertFalse(ui.run("verify -DTYPE -input_mpi_nprocs=6 ", QUIET, filename("gather.c")));
	}

	@Test
	public void gather_root() {
		assertFalse(ui.run("verify -DROOT -input_mpi_nprocs=6 ", QUIET, filename("gather.c")));
	}

	@Test
	public void gather_good() {
		assertTrue(ui.run("verify -input_mpi_nprocs=2 ", QUIET, filename("gather.c")));
	}

	/**
	 * The following is only correct when called with 2 processes (intentionally).
	 */
	@Test
	public void scatter_good() {
		assertTrue(ui.run("verify -input_mpi_nprocs=2", QUIET, filename("scatter.c")));
	}

	@Test
	public void scatter_order() {
		assertFalse(ui.run("verify -input_mpi_nprocs=6 ", QUIET, filename("scatter.c")));
	}

	@Test
	public void scatter_type() {
		assertFalse(ui.run("verify -DTYPE -input_mpi_nprocs=6 ", QUIET, filename("scatter.c")));
	}

	@Test
	public void scatter_root() {
		assertFalse(ui.run("verify -DROOT -input_mpi_nprocs=6 ", QUIET, filename("scatter.c")));
	}

	@Test
	public void scatter2() {
		assertFalse(ui.run("verify -input_mpi_nprocs=6 ", QUIET, filename("scatter2.c")));
	}

	@Test
	public void scatterAllgather_bad() {
		assertFalse(ui.run("verify -input_mpi_nprocs=3 ", QUIET, filename("scatterAllgather_bad.c")));
	}

	@Test
	public void scatterAllgather() {
		assertTrue(ui.run("verify -input_mpi_nprocs=3 ", QUIET, NO_PRINTF, filename("scatterAllgather.c")));
	}

	@Test
	public void reduce_maxloc_real() {
		assertTrue(ui.run("verify -input_mpi_nprocs=4 ", QUIET, NO_PRINTF, filename("reduce_maxloc_real.c")));
	}
	
	@Test
	public void reduce_maxloc() {
		assertTrue(ui.run("verify -input_mpi_nprocs=4 ", QUIET, NO_PRINTF, filename("reduce_maxloc.c")));
	}
	
	@Test
	public void reduce_minloc() {
		assertTrue(ui.run("verify -input_mpi_nprocs=4 ", QUIET, NO_PRINTF, filename("reduce_minloc.c")));
	}
	
	@Test
	public void inplace() {
		assertTrue(ui.run("verify -input_mpi_nprocs=4 ", QUIET, NO_PRINTF, filename("inplace.c")));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ui = null;
		rootDir = null;
	}
}
