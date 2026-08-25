package dev.civl.abc.front.c.parse;

import static dev.civl.abc.token.IF.CivlcToken.TokenVocabulary.CIVLC;

import java.util.HashMap;

import org.antlr.runtime.TokenStream;

import dev.civl.abc.front.IF.PP2CivlcTokenConverter;
import dev.civl.abc.front.c.preproc.PreprocessorParser;
import dev.civl.abc.token.IF.CivlcToken;

/**
 * This class converts a pp-token stream to a civl-c token stream by converting
 * all c keywords from tokens with a identifier type to its corresponding type.
 * 
 * @author Wenhao Wu (wuwenhao@udel.edu)
 */
public class PP2CivlcTokenCConverter implements PP2CivlcTokenConverter {

	/**
	 * A map used for mapping token type from its text.
	 */
	private HashMap<String, Integer> cKeywordTokenTypeMap = new HashMap<String, Integer>();

	public PP2CivlcTokenCConverter() {
		initCKeywordMap();
	}

	/**
	 * Initializes {@link #cKeywordTokenTypeMap}
	 */
	private void initCKeywordMap() {
		cKeywordTokenTypeMap.put("auto", CivlCParser.AUTO);
		cKeywordTokenTypeMap.put("asm", CivlCParser.ASM);
		cKeywordTokenTypeMap.put("break", CivlCParser.BREAK);
		cKeywordTokenTypeMap.put("case", CivlCParser.CASE);
		cKeywordTokenTypeMap.put("char", CivlCParser.CHAR);
		cKeywordTokenTypeMap.put("const", CivlCParser.CONST);
		cKeywordTokenTypeMap.put("continue", CivlCParser.CONTINUE);
		cKeywordTokenTypeMap.put("default", CivlCParser.DEFAULT);
		cKeywordTokenTypeMap.put("do", CivlCParser.DO);
		cKeywordTokenTypeMap.put("double", CivlCParser.DOUBLE);
		cKeywordTokenTypeMap.put("enum", CivlCParser.ENUM);
		cKeywordTokenTypeMap.put("extern", CivlCParser.EXTERN);
		cKeywordTokenTypeMap.put("float", CivlCParser.FLOAT);
		cKeywordTokenTypeMap.put("for", CivlCParser.FOR);
		cKeywordTokenTypeMap.put("goto", CivlCParser.GOTO);
		cKeywordTokenTypeMap.put("inline", CivlCParser.INLINE);
		cKeywordTokenTypeMap.put("int", CivlCParser.INT);
		cKeywordTokenTypeMap.put("long", CivlCParser.LONG);
		cKeywordTokenTypeMap.put("register", CivlCParser.REGISTER);
		cKeywordTokenTypeMap.put("restrict", CivlCParser.RESTRICT);
		cKeywordTokenTypeMap.put("return", CivlCParser.RETURN);
		cKeywordTokenTypeMap.put("short", CivlCParser.SHORT);
		cKeywordTokenTypeMap.put("signed", CivlCParser.SIGNED);
		cKeywordTokenTypeMap.put("sizeof", CivlCParser.SIZEOF);
		cKeywordTokenTypeMap.put("static", CivlCParser.STATIC);
		cKeywordTokenTypeMap.put("struct", CivlCParser.STRUCT);
		cKeywordTokenTypeMap.put("switch", CivlCParser.SWITCH);
		cKeywordTokenTypeMap.put("typedef", CivlCParser.TYPEDEF);
		cKeywordTokenTypeMap.put("union", CivlCParser.UNION);
		cKeywordTokenTypeMap.put("unsigned", CivlCParser.UNSIGNED);
		cKeywordTokenTypeMap.put("void", CivlCParser.VOID);
		cKeywordTokenTypeMap.put("volatile", CivlCParser.VOLATILE);
		cKeywordTokenTypeMap.put("while", CivlCParser.WHILE);
		cKeywordTokenTypeMap.put("_Alignas", CivlCParser.ALIGNAS);
		cKeywordTokenTypeMap.put("_Alignof", CivlCParser.ALIGNOF);
		cKeywordTokenTypeMap.put("_Atomic", CivlCParser.ATOMIC);
		cKeywordTokenTypeMap.put("_Bool", CivlCParser.BOOL);
		cKeywordTokenTypeMap.put("_Complex", CivlCParser.COMPLEX);
		cKeywordTokenTypeMap.put("_Generic", CivlCParser.GENERIC);
		cKeywordTokenTypeMap.put("_Imaginary", CivlCParser.IMAGINARY);
		cKeywordTokenTypeMap.put("_Noreturn", CivlCParser.NORETURN);
		cKeywordTokenTypeMap.put("_Static_assert", CivlCParser.STATICASSERT);
		cKeywordTokenTypeMap.put("_Thread_local", CivlCParser.THREADLOCAL);
		cKeywordTokenTypeMap.put("$abstract", CivlCParser.ABSTRACT);
		cKeywordTokenTypeMap.put("$assigns", CivlCParser.ASSIGNS);
		cKeywordTokenTypeMap.put("$O", CivlCParser.BIG_O);
		cKeywordTokenTypeMap.put("$calls", CivlCParser.CALLS);
		cKeywordTokenTypeMap.put("$catch", CivlCParser.CATCH);
		cKeywordTokenTypeMap.put("$choose", CivlCParser.CHOOSE);
		cKeywordTokenTypeMap.put("$atomic", CivlCParser.CIVLATOMIC);
		cKeywordTokenTypeMap.put("$for", CivlCParser.CIVLFOR);
		cKeywordTokenTypeMap.put("$contin", CivlCParser.CONTIN);
		cKeywordTokenTypeMap.put("$depends", CivlCParser.DEPENDS);
		cKeywordTokenTypeMap.put("$D", CivlCParser.DERIV);
		cKeywordTokenTypeMap.put("$differentiable", CivlCParser.DIFFERENTIABLE);
		cKeywordTokenTypeMap.put("$domain", CivlCParser.DOMAIN);
		cKeywordTokenTypeMap.put("$ensures", CivlCParser.ENSURES);
		cKeywordTokenTypeMap.put("$exists", CivlCParser.EXISTS);
		cKeywordTokenTypeMap.put("$forall", CivlCParser.FORALL);
		cKeywordTokenTypeMap.put("$atomic_f", CivlCParser.FATOMIC);
		cKeywordTokenTypeMap.put("$guard", CivlCParser.GUARD);
		cKeywordTokenTypeMap.put("$here", CivlCParser.HERE);
		cKeywordTokenTypeMap.put("$input", CivlCParser.INPUT);
		cKeywordTokenTypeMap.put("$invariant", CivlCParser.INVARIANT);
		cKeywordTokenTypeMap.put("$lambda", CivlCParser.LAMBDA);
		cKeywordTokenTypeMap.put("$mem", CivlCParser.MEM_TYPE);
		cKeywordTokenTypeMap.put("$output", CivlCParser.OUTPUT);
		cKeywordTokenTypeMap.put("$parfor", CivlCParser.PARFOR);
		cKeywordTokenTypeMap.put("$proc_null", CivlCParser.PROCNULL);
		cKeywordTokenTypeMap.put("$pure", CivlCParser.PURE);
		cKeywordTokenTypeMap.put("$range", CivlCParser.RANGE);
		cKeywordTokenTypeMap.put("$real", CivlCParser.REAL);
		cKeywordTokenTypeMap.put("$requires", CivlCParser.REQUIRES);
		cKeywordTokenTypeMap.put("$result", CivlCParser.RESULT);
		cKeywordTokenTypeMap.put("$run", CivlCParser.RUN);
		cKeywordTokenTypeMap.put("$scopeof", CivlCParser.SCOPEOF);
		cKeywordTokenTypeMap.put("$self", CivlCParser.SELF);
		cKeywordTokenTypeMap.put("$state_f", CivlCParser.STATE_F);
		cKeywordTokenTypeMap.put("$reads", CivlCParser.READS);
		cKeywordTokenTypeMap.put("$spawn", CivlCParser.SPAWN);
		cKeywordTokenTypeMap.put("$system", CivlCParser.SYSTEM);
		cKeywordTokenTypeMap.put("$uniform", CivlCParser.UNIFORM);
		cKeywordTokenTypeMap.put("$when", CivlCParser.WHEN);
		cKeywordTokenTypeMap.put("$sum", CivlCParser.SUM);
		cKeywordTokenTypeMap.put("__device__", CivlCParser.DEVICE);
		cKeywordTokenTypeMap.put("__global__", CivlCParser.GLOBAL);
		cKeywordTokenTypeMap.put("__shared__", CivlCParser.SHARED);
		cKeywordTokenTypeMap.put("typeof", CivlCParser.TYPEOF);
	}

	@Override
	public TokenStream convert(TokenStream stream) {
		CivlcToken cur = null;

		// By reading the binary of BufferredTokenStream, we know that
		// "toString" forces synchronization with the PreProcessorParser while
		// "get" doesn't. So we call "toString" to do a synchronization.
		stream.toString();
		cur = (CivlcToken) stream.get(0);
		while (cur != null) {
			if (cur.getType() == PreprocessorParser.IDENTIFIER) {
				Integer typeCode = cKeywordTokenTypeMap.get(cur.getText());

				if (typeCode != null)
					cur.setType(typeCode.intValue());
			}
			cur.setTokenVocab(CIVLC);
			cur = cur.getNext();
		}
		return stream;
	}

}
