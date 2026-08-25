parser grammar PreprocessorParser;

/* Author: Stephen F. Siegel, University of Delaware
 * Last modified: July 15, 2016
 *
 * Grammar for C preprocessor.
 * This grammar describes a C source file before preprocessing.
 * It does not execute any preprocessor directives.
 * It simply represents the file in a structured way.
 *
 * See the C11 Standard, Sec. 6.10.
 *
 * This grammar uses the PreprocessorLexer, which has already
 * formed the preprocessor tokens.
 *
 * Extensions from other languages (beyond C11) are included.
 */

// TODO: use things like this:
// bar 	: ID{$ID.setText("HELLO");$ID.setType(0);} WS INT -> ID INT;

options {
   tokenVocab=PreprocessorLexer;
   output=AST;
}

/* "imaginary" tokens that will be used in the tree */
tokens {
	FILE;         // root node
	TEXT_BLOCK;   // a list of tokens
	PARAMLIST;    // x1,x2,x3
	EXPR;         // an expression used in a conditional (#if)
	SEQUENCE;     // true branch of conditional directive
	BODY;         // body of macro definition
	PIF;          // preprocessor if: #if
	PELSE;        // preprocessor else: #else
	PPRAGMA;      // preprocessor pragma: #pragma
	/* C, CIVL, ACSL, and CUDA keywords */
	AUTO;
	ASM;
	BREAK;
	CASE;
	CHAR;
	CONST;
	CONTINUE;
	DEFAULT;
	DO;
	DOUBLE;
	ENUM;
	EXTERN;
	FLOAT;
	FOR;
	GOTO;
	INLINE;
	INT;
	LONG;
	REGISTER;
	RESTRICT;
	RETURN;
	SHORT;
	SIGNED;
	SIZEOF;
	STATIC;
	STRUCT;
	SWITCH;
	TYPEDEF;
	UNION;
	UNSIGNED;
	VOID;
	VOLATILE;
	WHILE;
	ALIGNAS;
	ALIGNOF;
	ATOMIC;
	BOOL;
	COMPLEX;
	GENERIC;
	IMAGINARY;
	NORETURN;
	STATICASSERT;
	THREADLOCAL;
	/* */
	ABSTRACT;
	ASSIGNS;
	BIG_O;
	CALLS;
	CATCH;
	CHOOSE;
	CIVLATOMIC;
	CIVLFOR;
	COLLECTIVE; //dummy
	CONTIN;
	DEPENDS;
	DERIV;
	DIFFERENTIABLE;
	DOMAIN;
	ENSURES;
	EXISTS;
	FORALL;
	FATOMIC;
	GUARD;
	HERE;
	INPUT;
	INVARIANT;
	LAMBDA;
	MEM_TYPE;
	OUTPUT;
	ORIGINAL;
	PARFOR;
	PROCNULL;
	PURE;
	RANGE;
	REAL;
	REQUIRES;
	RESULT;
	RUN;
	SCOPEOF;
	SELF;
	STATE_F;
	READS;
	SPAWN;
	SYSTEM;
	UNIFORM;
	WHEN;
	/* */
	DEVICE;
	GLOBAL;
	SHARED;
	/* */
	TYPEOF;
}

@header
{
package dev.civl.abc.front.c.preproc;
}

@members{
@Override
public void emitErrorMessage(String msg) { // don't try to recover!
    throw new RuntimeException(msg);
}
}
		
/* An item is either a preprocessor directive
 * or a text block.  For compound directives,
 * such as #ifdef ... #endif, all of the text
 * between the opening if and the closing #endif
 * is considered part of the directive.  
 * A textblock is a maximal sequence of plain
 * text lines.
 */
file		: whiteBlock? itemList EOF
		  -> ^(FILE whiteBlock? itemList EOF)
		;

/*
items		: directiveBlock*
		  (textBlock directiveBlock+)*
		  textBlock?
		;
*/

/* starts with non-ws token # or something not # and ends just before
 * non-ws token that does not start a directive block or text block. */
itemList	: directiveBlock itemList
		| textBlock ( directiveBlock itemList | )
		|
		;

whiteBlock	: white+ -> ^(TEXT_BLOCK white+)
		;

textBlock	: textSegment+ -> ^(TEXT_BLOCK textSegment+)
		;

textSegment	: NEWLINE white*
		| ~(HASH|WS|COMMENT|NEWLINE) (~NEWLINE)* NEWLINE white*
		;

directiveBlock	: directive whiteBlock?
		;

directive	: HASH! white!* directiveSuffix
		;

directiveSuffix	: macrodef
		| macroundef
		| includeline
		| pragmaline
		| errorline
		| lineline
		| ifdefblock
		| ifblock
		| ifndefblock
		| nondirective
		;

/* A nondirective is any line starting with # that
 * doesn't fall into one of the ordinary directive
 * forms. */
nondirective	: t+=not_directive t+=wpptoken* NEWLINE -> ^(HASH $t+)
		| NEWLINE -> ^(HASH)
		;

/* A function-like or object-like macro definition. */
macrodef	: DEFINE white+ i=identifier
		  ( paramlist macrobody -> ^(DEFINE $i paramlist macrobody)
		  | NEWLINE -> ^(DEFINE $i ^(BODY))
		  | white macrobody -> ^(DEFINE $i macrobody)
		  )
		;

macrobody	: white* 
		  ( t+=pptoken (t+=wpptoken* t+=pptoken)? white* NEWLINE
		    -> ^(BODY $t+)
		  | NEWLINE -> ^(BODY)
		  )
		;

paramlist	: LPAREN white* 
		  ( RPAREN -> ^(PARAMLIST)
		  | ELLIPSIS white* RPAREN -> ^(PARAMLIST ELLIPSIS)
		  | identifier (white* COMMA white* identifier)* white*
		    ( RPAREN -> ^(PARAMLIST identifier+)
		    | COMMA white* ELLIPSIS white* RPAREN
		      -> ^(PARAMLIST identifier+ ELLIPSIS)
		    )
		  )
		;

macroundef	: UNDEF white+ identifier white* NEWLINE
		  -> ^(UNDEF identifier)
		;

includeline	: INCLUDE white* t+=pptoken (t+=wpptoken* t+=pptoken)?
		  white* NEWLINE
		  -> ^(INCLUDE $t+)
		;

pragmaline	: PRAGMA{$PRAGMA.setType(PPRAGMA);} wpptoken* NEWLINE ->
		  ^(PRAGMA wpptoken* NEWLINE)
		;

errorline	: ERROR wpptoken* NEWLINE -> ^(ERROR wpptoken*)
		;

lineline	: LINE wpptoken* NEWLINE -> ^(LINE wpptoken*)
		;

/* #ifdef X ... #elif ... #elif ... #else ... #endif.
 * Tree:
 * (IFDEF identifier ^(SEQUENCE item*)), or
 * (IFDEF identifier ^(SEQUENCE item*) elseblock)
 */
ifdefblock	: IFDEF white* i=identifier white* NEWLINE
		  t=if_section f=if_suffix
		  -> ^(IFDEF $i ^(SEQUENCE $t?) $f?)
		;

/* Exactly like above, except with #ifndef instead of #ifdef */
ifndefblock	: IFNDEF white* i=identifier white* NEWLINE
		  t=if_section f=if_suffix
		  -> ^(IFNDEF $i ^(SEQUENCE $t?) $f?)
		;

/* #if expr ... #elif ... #elif ... #else ... #endif.
 * Very similar to #ifdef, but with an expression in place
 * of an identifier. */
ifblock		: IF{$IF.setType(PIF);}
		  white* e=expr white* NEWLINE
		  t=if_section f=if_suffix
		  -> ^(IF $e ^(SEQUENCE $t?) $f?)	
		;

/* A section of a conditional directive.
 * Begins just after the line containing
 * one of #ifdef, #ifndef, #if, #elif,
 * or #else.
 * Ends with the HASH white*
 * immediately preceding the first matching
 * endif, elif, or else. 
 */
if_section	: whiteBlock? section_body
		;

/* Begins with first non-white token on a line inside a
 * conditional section,
 * ends with the HASH white* immediately preceding the
 * endif, elif, or else closing that section.
 * Tree is just flat
 * list of TEXT_BLOCKs and directives.
 */
section_body	: textBlock? subsection
		;

/* Begins with a # at beginning of a line (after possible
 * white space) inside a conditional directive body.
 * Ends with the HASH white* immediately preceding
 * the closing endif, elif, or else.  Tree is just
 * flat list of TEXT_BLOCKs and directives.
 */
subsection	: HASH! white!*
		  ( directiveSuffix whiteBlock? section_body)?
		;

/* Begins with endif, elif, or else.   Ends with NEWLINE after
 * closing #endif.
 * Tree: one of
 *   1. empty
 *   2. (ELIF (ELIF expr (SEQUENCE items) elseblock?))
 *   3. (ELSE items)
 * respectively.  The reason for #2 is to make the tree
 * for a #elif... look the same as what would be obtained from
 * #else #if ....  The first ELIF
 * should be interpreted as ELSE and the second as IF.
 */
if_suffix	: ENDIF white* NEWLINE
 		  -> 
		| c=ELIF white* expr white* NEWLINE if_section if_suffix
		  -> ^($c ^($c expr ^(SEQUENCE if_section?) if_suffix?))
		| ELSE{$ELSE.setType(PELSE);}
		  white* NEWLINE if_section ENDIF white* NEWLINE
		  -> ^(ELSE if_section?)
		;

/* A space, tab, or comment */
white		: WS | COMMENT ;

/* A preprocessor token or white space token (but not NEWLINE). */
wpptoken	: pptoken | white ;

/* An expression that can be used with #if or #elif.
 * This grammar will accept just about anything here. */
expr		: ppdExpr (white* ppdExpr)* -> ^(EXPR ppdExpr+) ;

definedExpr	: DEFINED white!*
		  ( identifier
		  | LPAREN! white!* identifier white!* RPAREN!
		  )
		;

/* A preprocessor token or defined expressions.  These are the
 * things that can occur in an #if or #elif directive: */	
ppdExpr		: (DEFINED)=> definedExpr
		| pptoken
		;

/* A "preprocessor token" as defined in the C11 Standard.
 * This rule includes all of the extensions from the other
 * languages too.  We got rid of header names because
 * those are composed of smaller tokens in our lexer. */
pptoken		:	identifier
		|	pp_number
		|	CHARACTER_CONSTANT
		|	STRING_LITERAL
		|	punctuator
		|	OTHER
		;

/* Any token that is not a preprocessor keyword */
not_directive	:	pp_number
		|	CHARACTER_CONSTANT
		|	STRING_LITERAL
		|	punctuator
		|	OTHER
		|	IDENTIFIER
		|	EXTENDED_IDENTIFIER
		;
	
/* An "identifier" for the preprocessor is an IDENTIFIER
 * or any of the reserved words from any of the languages
 */
identifier	:	IDENTIFIER
		|	EXTENDED_IDENTIFIER
		|	pp_keyword
		;

/* C and preprocessor keywords: */


/* Words that are used in both C and the preprocessor */
c_pp_keyword	:	IF
		|	ELSE
		;

/* Words used in preprocessor but not in C */
pp_notc_keyword	:	DEFINE
		|	DEFINED
		|	ELIF
		|	ENDIF
		|	ERROR
		|	IFDEF
		|	IFNDEF
		|	INCLUDE
		|	LINE
		|	PRAGMA
		|	UNDEF
		;

/* Words used in preprocessor */
pp_keyword	:	pp_notc_keyword | c_pp_keyword
		;

/* a "pp_number" is any PP_NUMBER, INTEGER_CONSTANT, or FLOATING_CONSTANT */
pp_number	:	INTEGER_CONSTANT
		|	FLOATING_CONSTANT
		|	PP_NUMBER
		;
		
/* The punctuators are the symbols which are not words.
 * These are punctuators from all languages: */
punctuator	:	c_punctuator
		|	civl_punctuator
		|	cuda_punctuator
		;

/* C punctuators:  */
c_punctuator	:	AMPERSAND
		|	AND
		|	ARROW
		|	ASSIGN
		|	BITANDEQ
		|	BITOR
		|	BITOREQ
		|	BITXOR
		|	BITXOREQ
		|	COLON
		|	COMMA
		|	DIV
		|	DIVEQ
		|	ELLIPSIS
		|	DOTDOT
		|	DOT
		|	EQUALS
		|	GT
		|	GTE
		|	HASH
		|	HASHHASH
		|	LCURLY
		|	LPAREN
		|	LSQUARE
		|	LT
		|	LTE
		|	MINUSMINUS
		|	MOD
		|	MODEQ
		|	NEQ
		|	NOT
		|	OR
		|	PLUS
		|	PLUSEQ
		|	PLUSPLUS
		|	QMARK
		|	RCURLY
		|	RPAREN
		|	RSQUARE
		|	SEMI
		|	SHIFTLEFT
		|	SHIFTLEFTEQ
		|	SHIFTRIGHT
		|	SHIFTRIGHTEQ
		|	STAR
		|	STAREQ
		|	SUB
		|	SUBEQ
		|	TILDE
		;

civl_punctuator	:	ANNOTATION_END
		|	ANNOTATION_START
		|	AT
		|	EQUIV_ACSL
		|	IMPLIES
		|	IMPLIES_ACSL
		|	INLINE_ANNOTATION_START
		|	LSLIST
		|	RSLIST
		|	XOR_ACSL
		;

cuda_punctuator	:	LEXCON
		|	REXCON
		;
