package dev.civl.sarl.config.common;

import static java.io.StreamTokenizer.TT_EOF;
import static java.io.StreamTokenizer.TT_NUMBER;
import static java.io.StreamTokenizer.TT_WORD;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.StreamTokenizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import dev.civl.sarl.IF.SARLException;
import dev.civl.sarl.IF.SARLInternalException;
import dev.civl.sarl.IF.config.ProverInfo;
import dev.civl.sarl.IF.config.ProverInfo.ProverKind;
import dev.civl.sarl.IF.config.SARLConfig;

/**
 * Factory for producing and manipulating SARL configuration files and
 * {@link SARLConfig} objects.
 * 
 * @author siegel
 *
 */
public class ConfigFactory {

	// Fields...

	/**
	 * Nickname for <code>System.err</code>.
	 */
	public final static PrintStream err = System.err;

	/**
	 * Nickname for <code>System.out</code>.
	 */
	public final static PrintStream out = System.out;

	/**
	 * Map from the typical file names of the executable theorem provers to the kind
	 * of theorem prover. Add more entries as needed.
	 */
	private static Map<String, ProverKind> executableMap = new HashMap<>();

	static {
		executableMap.put("z3", ProverKind.Z3);
		executableMap.put("cvc5", ProverKind.CVC5);
		executableMap.put("cvc4", ProverKind.CVC4);
		executableMap.put("alt-ergo", ProverKind.ALT_ERGO);
		executableMap.put("vampire", ProverKind.VAMPIRE);
	}

	// Private methods...

	/**
	 * <p>
	 * Function converting a string representation of the theorem prover kind to an
	 * actual {@link ProverKind} object.
	 * </p>
	 * 
	 * <p>
	 * The {@link ProverKind} enumerated type provides some classification of
	 * theorem provers. There might be multiple entries for one actual prover, for
	 * example, because one entry is needed for the executable version using one
	 * input language, another entry is needed for the executable version using a
	 * different input language, and another version is needed for the API version.
	 * Add entries as needed.
	 * </p>
	 * 
	 * @param kindString the exact string representation of the kind
	 * @return the kind which corresponds exactly to that string, or
	 *         <code>null</code> is there is no such kind
	 */
	private static ProverKind kind(String kindString) {
		switch (kindString) {
		case "CVC4":
			return ProverKind.CVC4;
		case "CVC5":
			return ProverKind.CVC5;
		case "Z3":
			return ProverKind.Z3;
		case "ALT_ERGO":
			return ProverKind.ALT_ERGO;
		case "VAMPIRE":
			return ProverKind.VAMPIRE;
		default:
			return null;
		}
	}

	/**
	 * Returns the command argument(s) for the given kind of theorem prover that
	 * causes that prover to print its version number. Add new entries as needed.
	 * 
	 * @param kind the kind of theorem prover
	 * @return the command line arguments that cause the version number to be
	 *         printed
	 * @throws SARLException if the kind is not of the executable variety
	 */
	private static String versionCommand(ProverKind kind) {
		switch (kind) {
		case CVC4:
		case CVC5:
		case Z3:
		case ALT_ERGO:
		case VAMPIRE:
			return "--version";
		default:
			throw new SARLException("Unknown executable prover kind: " + kind);
		}
	}

	/**
	 * Compares two version strings. Both are assumed to have form
	 * something.something....
	 * 
	 * @param version1 a version string
	 * @param version2 a version string
	 * @return a negative int if version1 is lower than version2; 0 if they are
	 *         equal; a positive int if version1 is higher that version2
	 */
	private static int compareVersions(String version1, String version2) {
		if (version1 == null) { // null lower than everything
			return version2 == null ? 0 : -1;
		}
		if (version2 == null) {
			return 1;
		}

		String[] split1 = version1.split("\\."), split2 = version2.split("\\.");
		int n1 = split1.length, n2 = split2.length;

		for (int i = 0; i < n1; i++) {
			if (i >= n2) { // anything higher than nothing
				return 1;
			}

			String s1 = split1[i], s2 = split2[i];
			Integer x1, x2;
			int result;

			try {
				x1 = Integer.valueOf(s1);
			} catch (NumberFormatException e) {
				x1 = null;
			}
			try {
				x2 = Integer.valueOf(s2);
			} catch (NumberFormatException e) {
				x2 = null;
			}
			if (x1 != null & x2 != null) { // two numbers: numerical order
				result = x1 - x2;
			} else if (x1 != null) { // number higher than non-number
				result = 1;
			} else if (x2 != null) {// number higher than non-number
				result = -1;
			} else { // two non-numbers: alphabetical order
				result = s1.compareTo(s2);
			}
			if (result != 0)
				return result;
		}
		return 0;
	}

	/**
	 * Returns an exception object in the case where a SARL configuration file has
	 * incorrect syntax.
	 * 
	 * @param file the SARL configuration file being parsed
	 * @param st   the stream tokenizer being used to parse the configuration file
	 * @param msg  an error message
	 * @return a {@link SARLException} with a nice presentation of the error
	 *         message, file name, and line number
	 */
	private static SARLException parseErr(File file, StreamTokenizer st, String msg) {
		return new SARLException(file + " " + st.toString() + " " + msg);
	}

	/**
	 * Determines whether a file with a specified name exists in a specified
	 * directory. The file must be a regular file, not a directory.
	 * 
	 * @param dir      directory in which to look
	 * @param filename name of file
	 * @return if the directory exists and the file exists and is a regular file in
	 *         that directory: the {@link File} object corresponding to the file;
	 *         otherwise <code>null</code>
	 */
	private static File getFile(File dir, String filename) {
		if (dir.isDirectory()) {
			File file = new File(dir, filename);

			if (file.isFile())
				return file;
		}
		return null;
	}

	/**
	 * Checks that an executable prover actually can be executed and creates a
	 * corresponding {@link ProverInfo} object for it.
	 * 
	 * @param kind           the kind of theorem prover
	 * @param alias          the alias string to use in the configuration entry
	 * @param executableFile the executable theorem prover
	 * @return a new {@link ProverInfo} object corresponding to the specified
	 *         prover, or <code>null</code> if the prover could not be executed
	 *         correctly
	 */
	private static ProverInfo processExecutableProver(ProverKind kind, String alias, File executableFile) {
		String fullPath = executableFile.getAbsolutePath();
		ProcessBuilder pb = new ProcessBuilder(fullPath, versionCommand(kind));
		Process process = null;
		String line;

		try {
			process = pb.start();

			BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));

			line = stdout.readLine().trim();
			process.destroy();
		} catch (Throwable e) {
			if (process != null)
				process.destroy();
			out.println("Failed to execute " + fullPath);
			return null;
		}
		// try to find version string following usual conventions.
		// Vampire just prints "Vampire 5.0.1 ..."
		String version = null;
		int pos = line.indexOf("Vampire ");
		if (pos >= 0) {
			int pos1 = pos + "Vampire ".length();
			int pos2 = line.indexOf(' ', pos1);
			if (pos2 < 0)
				pos2 = line.length();
			version = line.substring(pos1, pos2);
		} else {
			pos = line.lastIndexOf("version ");
			if (pos >= 0) {
				version = line.substring(pos + "version ".length());
			} else {
				pos = line.indexOf('v');
				if (pos >= 0) {
					version = line.substring(pos + 1);
				} else {
					out.println("Unexpected output from " + fullPath);
					return null;
				}
			}
		}

		ProverInfo info = new CommonProverInfo();
		info.addAlias(alias);
		info.setKind(kind);
		info.setPath(executableFile);
		info.setVersion(version);
		info.setTimeout(5.0); // default timeout
		info.setShowQueries(false);
		info.setShowInconclusives(false);
		info.setShowErrors(true);
		if (kind == ProverKind.CVC4) {

		} else if (kind == ProverKind.CVC5) {
			info.addOption("--lang=smt2"); // language is SMT-LIB2
			// info.addOption("--arrays-exp"); // experimental array features sometimes help
		} else if (kind == ProverKind.Z3) {
			info.addOption("-smt2"); // language is SMT-LIB2
		} else if (kind == ProverKind.ALT_ERGO) {
			info.addOption("-i");
			info.addOption("smtlib2"); // input language is SMT-LIB2
			info.addOption("-o");
			info.addOption("smtlib2"); // output language is SMT-LIB2
		} else if (kind == ProverKind.VAMPIRE) {
			info.addOption("--input_syntax");
			info.addOption("smtlib2");
			info.addOption("--output_mode");
			info.addOption("smtcomp");
		}
		return info;
	}

	// Public methods...

	/**
	 * Parses a SARL configuration file.
	 * 
	 * @param configFile the configuration file
	 * @return the {@link SARLConfig} object resulting from parsing that file
	 * @throws IOException   if anything goes wrong reading the file
	 * @throws SARLException if the configuration file does not conform to the
	 *                       proper syntax
	 */
	public static SARLConfig fromFile(File configFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(configFile));
		StreamTokenizer st = new StreamTokenizer(reader);
		LinkedList<ProverInfo> proverInfos = new LinkedList<>();
		Set<String> allAliases = new HashSet<>();

		st.commentChar('#');
		st.quoteChar('"');
		st.quoteChar('\'');
		st.parseNumbers();
		st.eolIsSignificant(false);
		st.slashSlashComments(true);
		st.slashStarComments(true);
		st.lowerCaseMode(false);
		st.wordChars('_', '_');
		for (int token = st.nextToken(); token != TT_EOF; token = st.nextToken()) {
			if (token != TT_WORD || !"prover".equalsIgnoreCase(st.sval))
				throw parseErr(configFile, st, "expected \"prover\"");
			token = st.nextToken();
			if (token != '{')
				throw parseErr(configFile, st, "expected '{'");

			ProverInfo info = new CommonProverInfo();

			for (token = st.nextToken(); token != '}'; token = st.nextToken()) {
				String keyword;

				if (token != TT_WORD)
					throw parseErr(configFile, st, "expected a key-word");
				keyword = st.sval;
				token = st.nextToken();
				if (token != '=')
					throw parseErr(configFile, st, "expected '='");
				switch (keyword) {
				case "aliases": {
					int numAliases = 0;

					for (token = st.nextToken(); token != ';'; token = st.nextToken()) {
						if (numAliases > 0) {
							if (token != ',')
								throw parseErr(configFile, st, "expected ','");
							token = st.nextToken();
						}
						if (token != TT_WORD)
							throw parseErr(configFile, st, "expected a plain word to be used as alias");
						if (!allAliases.add(st.sval))
							throw parseErr(configFile, st, "alias used more than once");
						info.addAlias(st.sval);
						numAliases++;
					}
					if (numAliases == 0)
						throw parseErr(configFile, st, "expected at least one alias");
					break;
				}
				case "path": {
					token = st.nextToken();
					if (token != '"' && token != '\'')
						throw parseErr(configFile, st, "expected quoted string");
					if (info.getPath() != null)
						throw parseErr(configFile, st, "more than one path");
					info.setPath(new File(st.sval));
					token = st.nextToken();
					if (token != ';')
						throw parseErr(configFile, st, "expected ';'");
					break;
				}
				case "options": {
					boolean first = true;

					for (token = st.nextToken(); token != ';'; token = st.nextToken()) {
						if (first)
							first = false;
						else {
							if (token != ',')
								throw parseErr(configFile, st, "expected ','");
							token = st.nextToken();
						}
						if (token != '"' && token != '\'')
							throw parseErr(configFile, st, "expected a quoted string");
						info.addOption(st.sval);
					}
					break;
				}
				case "version": {
					token = st.nextToken();
					if (token != '"' && token != '\'')
						throw parseErr(configFile, st, "expected quoted string");
					if (info.getVersion() != null)
						throw parseErr(configFile, st, "more than one version");
					info.setVersion(st.sval);
					token = st.nextToken();
					if (token != ';')
						throw parseErr(configFile, st, "expected ';'");
					break;
				}
				case "kind": {
					token = st.nextToken();
					if (token != TT_WORD)
						throw parseErr(configFile, st, "expected word");
					if (info.getKind() != null)
						throw parseErr(configFile, st, "more than one kind");

					ProverKind kind = kind(st.sval);

					if (kind == null)
						throw parseErr(configFile, st, "unknown prover kind");
					info.setKind(kind);
					token = st.nextToken();
					if (token != ';')
						throw parseErr(configFile, st, "expected ';'");
					break;
				}
				case "showQueries":
				case "showInconclusives":
				case "showErrors": {
					token = st.nextToken();
					if (token != TT_WORD)
						throw parseErr(configFile, st, "expected word");

					boolean value;

					switch (st.sval) {
					case "true":
						value = true;
						break;
					case "false":
						value = false;
						break;
					default:
						throw parseErr(configFile, st, "expected true or false");
					}
					switch (keyword) {
					case "showQueries":
						info.setShowQueries(value);
						break;
					case "showInconclusives":
						info.setShowInconclusives(value);
						break;
					case "showErrors":
						info.setShowErrors(value);
						break;
					default:
						throw new SARLInternalException("Unreachable");
					}
					token = st.nextToken();
					if (token != ';')
						throw parseErr(configFile, st, "expected ';'");
					break;
				}
				case "timeout": {
					token = st.nextToken();
					if (token != TT_NUMBER)
						throw parseErr(configFile, st, "expected number (time in seconds)");
					info.setTimeout(st.nval);
					token = st.nextToken();
					if (token != ';')
						throw parseErr(configFile, st, "expected ';'");
					break;
				}
				default:
					throw parseErr(configFile, st, "unknown keyword: " + keyword);
				} // end of switch
			} // end of for
			proverInfos.add(info);
		} // end of for
		reader.close();
		CommonSARLConfig config = new CommonSARLConfig(proverInfos);
		return config;
	}

	/**
	 * Looks for a SARL configuration file by first looking in the current working
	 * directory for a file named <code>.sarl</code>. If no file by that name is
	 * found, it looks in the user's home directory for <code>.sarl</code>. If that
	 * is not found, it looks in the current working directory for a file named
	 * <code>.sarl_default</code>. If that is not found, it looks in the home
	 * directory for <code>.sarl_default</code>.
	 * 
	 * @return the SARL configuration file, or <code>null</code> if not found
	 */
	public static File findSARLConfigFile() {
		File workingDir = new File(System.getProperty("user.dir"));
		File result = getFile(workingDir, ".sarl");

		if (result != null)
			return result;

		File homeDir = getHomeDir();

		result = getFile(homeDir, ".sarl");
		if (result != null)
			return result;
		result = getFile(workingDir, ".sarl_default");
		if (result != null)
			return result;
		result = getFile(homeDir, ".sarl_default");
		return result;
	}

	/**
	 * gets the user home directory:
	 * 
	 * if the vm argument -Duser.home=$HOME, the value of the environment variable
	 * needs to be figured out.
	 * 
	 * @return
	 */
	private static File getHomeDir() {
		// when -Duser.home=$HOME is used, System.getProperty("user.home")
		// returns "$HOME" (HOME could be any identifier)
		String userHome = System.getProperty("user.home");

		if (userHome.startsWith("$")) {
			// getting the value of "HOME" (HOME could be any identifier)
			userHome = System.getenv(userHome.substring(1));
		}
		return new File(userHome);
	}

	/**
	 * Checks that a {@link SARLConfig} object corresponds to the actual system on
	 * which we are running.
	 * 
	 * @param config a {@link SARLConfig} object
	 * @throws SARLException if a theorem prover specified in the config is not
	 *                       found, or cannot be executed/loaded, or does not have
	 *                       the version specified
	 */
	public static void checkConfig(SARLConfig config) throws SARLException {
		for (ProverInfo info : config.getProvers()) {
			ProverInfo reread = processExecutableProver(info.getKind(), info.getFirstAlias(), info.getPath());
			if (reread == null)
				throw new SARLException("no theorem prover " + info.getFirstAlias() + " at " + info.getPath());
			if (!reread.getVersion().equals(info.getVersion()))
				throw new SARLException("expected version " + info.getVersion() + " for " + info.getFirstAlias()
						+ " but found " + reread.getVersion());
		}
	}

	/**
	 * Finds and parses the SARL configuration file.
	 * 
	 * @return {@link SARLConfig} object resulting from parsing the configuration
	 *         file, or <code>null</code> if a configuration file was not found
	 * @throws IOException   if anything goes wrong reading the configuration file
	 * @throws SARLException if the configuration file does not conform to the
	 *                       proper syntax
	 */
	public static SARLConfig findConfig() throws IOException {
		File configFile = findSARLConfigFile();

		if (configFile == null)
			return null;
		return fromFile(configFile);
	}

	/**
	 * Searches for theorem provers on the user's system and creates a SARL
	 * configuration file <code>.sarl</code> in the user's home directory.
	 * 
	 * @throws FileNotFoundException if the configuration file cannot be created in
	 *                               the home directory
	 */
	public static void makeConfigFile() throws FileNotFoundException {
		File userHomeDir = getHomeDir();
		File configFile = new File(userHomeDir, ".sarl");

		if (configFile.exists()) {
			if (configFile.isFile()) {
				File newLocation = new File(userHomeDir, ".sarl.old");

				out.println("Moving existing SARL configuration file to " + newLocation.getAbsolutePath());
				out.flush();
				configFile.renameTo(newLocation);
			} else {
				System.err.println("Remove the non-ordinary-file " + configFile.getAbsolutePath() + " and try again.");
				System.err.flush();
				System.exit(2);
			}
		}

		String path = System.getenv("PATH");
		String[] pathDirs = path.split(File.pathSeparator);
		PrintStream stream = new PrintStream(new FileOutputStream(configFile));
		Map<ProverKind, ProverInfo> executableFoundMap = new HashMap<>();
		ArrayList<ProverInfo> provers = new ArrayList<>();
		HashSet<String> seenPaths = new HashSet<>();

		out.println("Creating SARL configuration file in " + configFile);
		out.flush();
		stream.println("/* This is a SARL configuration file.");
		stream.println(" * It contains one entry for each theorem prover SARL may use.");
		stream.println(" * To resolve a query, by default, SARL will use the first prover here.");
		stream.println(" * If that result in inconclusive, it will try the second prover.");
		stream.println(" * And so on, until a conclusive result has been reached, or all provers");
		stream.println(" * have been exhausted.");
		stream.println(" * ");
		stream.println(" * SARL looks for a configuration file by looking in the following");
		stream.println(" * places in order:");
		stream.println(" * 1. .sarl in current working directory");
		stream.println(" * 2. .sarl in user's home directory");
		stream.println(" * 3. .sarl_default in current working directory");
		stream.println(" * 4. .sarl_default in user's home directory");
		stream.println(" */");
		stream.println();
		stream.flush();

		for (String dirName : pathDirs) {
			File dir = new File(dirName);
			String fullDirName = dir.getAbsolutePath();

			if (dir.isDirectory()) {
				if (!seenPaths.add(fullDirName))
					continue;

				File[] files = dir.listFiles();

				for (File file : files) {
					if (!file.canExecute())
						continue;

					String name = file.getName();
					ProverKind kind = executableMap.get(name);

					if (kind != null) {
						String alias = kind.toString().toLowerCase();
						ProverInfo prover = processExecutableProver(kind, alias, file);

						if (prover != null) {
							String msg = kind + " version " + prover.getVersion() + " at " + prover.getPath();
							ProverInfo oldProver = executableFoundMap.get(kind);

							if (oldProver != null) {
								int compare = compareVersions(prover.getVersion(), oldProver.getVersion());

								if (compare == 0) {
									out.println("Ignoring equivalent " + msg);
									out.flush();
									continue;
								}
								if (compare < 0) {
									out.println("Ignoring older " + msg);
									out.flush();
									continue;
								}
								out.println("Replacing previous " + kind + " with " + msg);
								out.flush();
							} else {
								out.println("Adding " + msg);
							}
							executableFoundMap.put(kind, prover);
						}
					}
				}
			}
		}
		provers.addAll(executableFoundMap.values());
		Collections.sort(provers);
		for (ProverInfo prover : provers) {
			prover.print(stream);
			stream.println();
		}
		stream.close();
		if (provers.isEmpty()) {
			err.println("No appropriate theorem provers were found in your PATH.");
			err.println("SARL's theorem proving capability will be very limited.");
			err.println("Consider installing at least one of Alt-Ergo, CVC4, CVC5, Vampire, or Z3.");
			err.flush();
		}
		out.println("SARL configuration file created successfully in " + configFile.getAbsolutePath());
		out.println("By default, SARL will use all provers listed in the configuration file,");
		out.println("in order, until a conclusive result is obtained.");
		out.println("Edit the file as necessary to remove or change the order of provers.");
		out.flush();
	}
}
