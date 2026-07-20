package dev.civl.abc.front.IF;

import dev.civl.abc.front.c.parse.CivlCParser;

/**
 * This class exports the constants for token types from CivlCParser. These are
 * needed currently for transformers when they create new AST nodes and need to
 * make up source object for them.
 * 
 * Ultimately, this class will be got rid of when the transformers create
 * intermediate files and generate source object from them.
 * 
 * The token constants are just extracted from the ANTLR-generated parser using
 * the following Perl script:
 * 
 * <pre>
 * open(FILE, "fields.txt") || die "could not open fields.txt";
 * while (defined($line=<FILE>)) {
 *   chomp($line);
 *   ($pre, $name, $num) = ($line =~ /^(\s*public\s*static\s*final\s*int\s*)(.*)=(.*)\;$/);
 *   # print "$pre $name $num\n";
 *   print "$pre","$name = CivlCParser.$name;\n";
 * }
 * close(FILE);
 * </pre>
 * 
 * I thought this approach would be better than exporting the entire
 * ANTLR-generated parser; I don't want other code in ABC or elsewhere tied so
 * totally to the generated code. On the other hand, other code does need to
 * access these constants.
 * 
 * @author Manchun Zheng
 * 
 */
public interface CivlcTokenConstant {

	// constants defined in the ANTLR-generated parser
	public static final int EOF = CivlCParser.EOF;
	public static final int ABSTRACT = CivlCParser.ABSTRACT;
	public static final int ALIGNAS = CivlCParser.ALIGNAS;
	public static final int ALIGNOF = CivlCParser.ALIGNOF;
	public static final int AMPERSAND = CivlCParser.AMPERSAND;
	public static final int AND = CivlCParser.AND;
	public static final int ANNOTATION = CivlCParser.ANNOTATION;
	public static final int ARROW = CivlCParser.ARROW;
	public static final int ASM = CivlCParser.ASM;
	public static final int ASSIGN = CivlCParser.ASSIGN;
	public static final int ASSIGNS = CivlCParser.ASSIGNS;
	public static final int AT = CivlCParser.AT;
	public static final int ATOMIC = CivlCParser.ATOMIC;
	public static final int AUTO = CivlCParser.AUTO;
	public static final int BIG_O = CivlCParser.BIG_O;
	public static final int BITANDEQ = CivlCParser.BITANDEQ;
	public static final int BITOR = CivlCParser.BITOR;
	public static final int BITOREQ = CivlCParser.BITOREQ;
	public static final int BITXOR = CivlCParser.BITXOR;
	public static final int BITXOREQ = CivlCParser.BITXOREQ;
	public static final int BOOL = CivlCParser.BOOL;
	public static final int BREAK = CivlCParser.BREAK;
	public static final int BinaryExponentPart = CivlCParser.BinaryExponentPart;
	public static final int CALLS = CivlCParser.CALLS;
	public static final int CASE = CivlCParser.CASE;
	public static final int CChar = CivlCParser.CChar;
	public static final int CHAR = CivlCParser.CHAR;
	public static final int CHARACTER_CONSTANT = CivlCParser.CHARACTER_CONSTANT;
	public static final int CHOOSE = CivlCParser.CHOOSE;
	public static final int CIVLATOMIC = CivlCParser.CIVLATOMIC;
	public static final int CIVLFOR = CivlCParser.CIVLFOR;
	public static final int COLLECTIVE = CivlCParser.COLLECTIVE;
	public static final int COLON = CivlCParser.COLON;
	public static final int COMMA = CivlCParser.COMMA;
	public static final int COMMENT = CivlCParser.COMMENT;
	public static final int COMPLEX = CivlCParser.COMPLEX;
	public static final int CONST = CivlCParser.CONST;
	public static final int CONTIN = CivlCParser.CONTIN;
	public static final int CONTINUE = CivlCParser.CONTINUE;
	public static final int DEFAULT = CivlCParser.DEFAULT;
	public static final int DEFINED = CivlCParser.DEFINED;
	public static final int DEPENDS = CivlCParser.DEPENDS;
	public static final int DERIV = CivlCParser.DERIV;
	public static final int DEVICE = CivlCParser.DEVICE;
	public static final int DIV = CivlCParser.DIV;
	public static final int DIVEQ = CivlCParser.DIVEQ;
	public static final int DO = CivlCParser.DO;
	public static final int DOMAIN = CivlCParser.DOMAIN;
	public static final int DOT = CivlCParser.DOT;
	public static final int DOTDOT = CivlCParser.DOTDOT;
	public static final int DOUBLE = CivlCParser.DOUBLE;
	public static final int DecimalConstant = CivlCParser.DecimalConstant;
	public static final int DecimalFloatingConstant = CivlCParser.DecimalFloatingConstant;
	public static final int Digit = CivlCParser.Digit;
	public static final int ELLIPSIS = CivlCParser.ELLIPSIS;
	public static final int ELSE = CivlCParser.ELSE;
	public static final int ENSURES = CivlCParser.ENSURES;
	public static final int ENUM = CivlCParser.ENUM;
	public static final int EQUALS = CivlCParser.EQUALS;
	public static final int EXISTS = CivlCParser.EXISTS;
	public static final int EXTERN = CivlCParser.EXTERN;
	public static final int EscapeSequence = CivlCParser.EscapeSequence;
	public static final int ExponentPart = CivlCParser.ExponentPart;
	// public static final int FALSE = CivlCParser.FALSE;
	public static final int FATOMIC = CivlCParser.FATOMIC;
	public static final int FLOAT = CivlCParser.FLOAT;
	public static final int FLOATING_CONSTANT = CivlCParser.FLOATING_CONSTANT;
	public static final int FOR = CivlCParser.FOR;
	public static final int FORALL = CivlCParser.FORALL;
	public static final int FloatingSuffix = CivlCParser.FloatingSuffix;
	public static final int FractionalConstant = CivlCParser.FractionalConstant;
	public static final int GENERIC = CivlCParser.GENERIC;
	public static final int GOTO = CivlCParser.GOTO;
	public static final int GLOBAL = CivlCParser.GLOBAL;
	public static final int GT = CivlCParser.GT;
	public static final int GTE = CivlCParser.GTE;
	public static final int GUARD = CivlCParser.GUARD;
	public static final int HASH = CivlCParser.HASH;
	public static final int HASHHASH = CivlCParser.HASHHASH;
	public static final int HERE = CivlCParser.HERE;
	public static final int HexEscape = CivlCParser.HexEscape;
	public static final int HexFractionalConstant = CivlCParser.HexFractionalConstant;
	public static final int HexPrefix = CivlCParser.HexPrefix;
	public static final int HexQuad = CivlCParser.HexQuad;
	public static final int HexadecimalConstant = CivlCParser.HexadecimalConstant;
	public static final int HexadecimalDigit = CivlCParser.HexadecimalDigit;
	public static final int HexadecimalFloatingConstant = CivlCParser.HexadecimalFloatingConstant;
	public static final int IDENTIFIER = CivlCParser.IDENTIFIER;
	public static final int IF = CivlCParser.IF;
	public static final int IMAGINARY = CivlCParser.IMAGINARY;
	public static final int IMPLIES = CivlCParser.IMPLIES;
	public static final int INLINE = CivlCParser.INLINE;
	public static final int INPUT = CivlCParser.INPUT;
	public static final int INT = CivlCParser.INT;
	public static final int INTEGER_CONSTANT = CivlCParser.INTEGER_CONSTANT;
	public static final int INVARIANT = CivlCParser.INVARIANT;
	public static final int IdentifierNonDigit = CivlCParser.IdentifierNonDigit;
	public static final int IntegerSuffix = CivlCParser.IntegerSuffix;
	public static final int LAMBDA = CivlCParser.LAMBDA;
	public static final int LCURLY = CivlCParser.LCURLY;
	public static final int LEXCON = CivlCParser.LEXCON;
	public static final int LIB_NAME = CivlCParser.LIB_NAME;
	public static final int LONG = CivlCParser.LONG;
	public static final int LPAREN = CivlCParser.LPAREN;
	public static final int LSLIST = CivlCParser.LSLIST;
	public static final int LSQUARE = CivlCParser.LSQUARE;
	public static final int LT = CivlCParser.LT;
	public static final int LTE = CivlCParser.LTE;
	public static final int LongLongSuffix = CivlCParser.LongLongSuffix;
	public static final int LongSuffix = CivlCParser.LongSuffix;
	public static final int MINUSMINUS = CivlCParser.MINUSMINUS;
	public static final int MOD = CivlCParser.MOD;
	public static final int MODEQ = CivlCParser.MODEQ;
	public static final int NEQ = CivlCParser.NEQ;
	public static final int NEWLINE = CivlCParser.NEWLINE;
	public static final int NORETURN = CivlCParser.NORETURN;
	public static final int NOT = CivlCParser.NOT;
	public static final int NonDigit = CivlCParser.NonDigit;
	public static final int NonZeroDigit = CivlCParser.NonZeroDigit;
	public static final int OR = CivlCParser.OR;
	public static final int OTHER = CivlCParser.OTHER;
	public static final int OUTPUT = CivlCParser.OUTPUT;
	public static final int OctalConstant = CivlCParser.OctalConstant;
	public static final int OctalDigit = CivlCParser.OctalDigit;
	public static final int OctalEscape = CivlCParser.OctalEscape;
	public static final int PARFOR = CivlCParser.PARFOR;
	public static final int DEFINE = CivlCParser.DEFINE;
	public static final int ELIF = CivlCParser.ELIF;
	public static final int ENDIF = CivlCParser.ENDIF;
	public static final int ERROR = CivlCParser.ERROR;
	public static final int IFDEF = CivlCParser.IFDEF;
	public static final int IFNDEF = CivlCParser.IFNDEF;
	public static final int INCLUDE = CivlCParser.INCLUDE;
	public static final int ORIGINAL = CivlCParser.ORIGINAL;
	public static final int PLINE = CivlCParser.LINE;
	public static final int PLUS = CivlCParser.PLUS;
	public static final int PLUSEQ = CivlCParser.PLUSEQ;
	public static final int PLUSPLUS = CivlCParser.PLUSPLUS;
	public static final int PP_NUMBER = CivlCParser.PP_NUMBER;
	public static final int PPRAGMA = CivlCParser.PPRAGMA;
	public static final int PROCNULL = CivlCParser.PROCNULL;
	public static final int PROGRAM = CivlCParser.PROGRAM;
	public static final int PURE = CivlCParser.PURE;
	public static final int UNDEF = CivlCParser.UNDEF;
	public static final int QMARK = CivlCParser.QMARK;
	public static final int QUANTIFIED = CivlCParser.QUANTIFIED;
	public static final int RCURLY = CivlCParser.RCURLY;
	public static final int READS = CivlCParser.READS;
	public static final int REXCON = CivlCParser.REXCON;
	public static final int RANGE = CivlCParser.RANGE;
	public static final int REAL = CivlCParser.REAL;
	public static final int REGISTER = CivlCParser.REGISTER;
	public static final int REQUIRES = CivlCParser.REQUIRES;
	public static final int RESTRICT = CivlCParser.RESTRICT;
	public static final int RESULT = CivlCParser.RESULT;
	public static final int RETURN = CivlCParser.RETURN;
	public static final int RPAREN = CivlCParser.RPAREN;
	public static final int RUN = CivlCParser.RUN;
	public static final int RSLIST = CivlCParser.RSLIST;
	public static final int RSQUARE = CivlCParser.RSQUARE;
	public static final int SCOPEOF = CivlCParser.SCOPEOF;
	public static final int SChar = CivlCParser.SChar;
	public static final int SELF = CivlCParser.SELF;
	public static final int SEMI = CivlCParser.SEMI;
	public static final int SHARED = CivlCParser.SHARED;
	public static final int SHIFTLEFT = CivlCParser.SHIFTLEFT;
	public static final int SHIFTLEFTEQ = CivlCParser.SHIFTLEFTEQ;
	public static final int SHIFTRIGHT = CivlCParser.SHIFTRIGHT;
	public static final int SHIFTRIGHTEQ = CivlCParser.SHIFTRIGHTEQ;
	public static final int SHORT = CivlCParser.SHORT;
	public static final int SIGNED = CivlCParser.SIGNED;
	public static final int SIZEOF = CivlCParser.SIZEOF;
	public static final int SPAWN = CivlCParser.SPAWN;
	public static final int STAR = CivlCParser.STAR;
	public static final int STAREQ = CivlCParser.STAREQ;
	public static final int STATEMENT = CivlCParser.STATEMENT;
	public static final int STATEMENT_EXPRESSION = CivlCParser.STATEMENT_EXPRESSION;
	public static final int STATIC = CivlCParser.STATIC;
	public static final int STATICASSERT = CivlCParser.STATICASSERT;
	public static final int STRING_LITERAL = CivlCParser.STRING_LITERAL;
	public static final int STRUCT = CivlCParser.STRUCT;
	public static final int SUB = CivlCParser.SUB;
	public static final int SUBEQ = CivlCParser.SUBEQ;
	public static final int SWITCH = CivlCParser.SWITCH;
	public static final int STATE_F = CivlCParser.STATE_F;
	public static final int SUM = CivlCParser.SUM;
	public static final int SYSTEM = CivlCParser.SYSTEM;
	public static final int THREADLOCAL = CivlCParser.THREADLOCAL;
	public static final int TILDE = CivlCParser.TILDE;
	// public static final int TRUE = CivlCParser.TRUE;
	public static final int TYPEDEF = CivlCParser.TYPEDEF;
	public static final int TYPEOF_EXPRESSION = CivlCParser.TYPEOF_EXPRESSION;
	public static final int TYPEOF_TYPE = CivlCParser.TYPEOF_TYPE;
	public static final int UNIFORM = CivlCParser.UNIFORM;
	public static final int UNION = CivlCParser.UNION;
	public static final int UNSIGNED = CivlCParser.UNSIGNED;
	public static final int UniversalCharacterName = CivlCParser.UniversalCharacterName;
	public static final int UnsignedSuffix = CivlCParser.UnsignedSuffix;
	public static final int VOID = CivlCParser.VOID;
	public static final int VOLATILE = CivlCParser.VOLATILE;
	public static final int WHEN = CivlCParser.WHEN;
	public static final int WHILE = CivlCParser.WHILE;
	public static final int WS = CivlCParser.WS;
	public static final int Zero = CivlCParser.Zero;
	public static final int BODY = CivlCParser.BODY;
	public static final int EXPR = CivlCParser.EXPR;
	public static final int FILE = CivlCParser.FILE;
	public static final int PARAMLIST = CivlCParser.PARAMLIST;
	public static final int SEQUENCE = CivlCParser.SEQUENCE;
	public static final int TEXT_BLOCK = CivlCParser.TEXT_BLOCK;
	public static final int ABSENT = CivlCParser.ABSENT;
	public static final int ABSTRACT_DECLARATOR = CivlCParser.ABSTRACT_DECLARATOR;
	public static final int ARGUMENT_LIST = CivlCParser.ARGUMENT_LIST;
	public static final int ARRAY_ELEMENT_DESIGNATOR = CivlCParser.ARRAY_ELEMENT_DESIGNATOR;
	public static final int ARRAY_SUFFIX = CivlCParser.ARRAY_SUFFIX;
	public static final int BLOCK_ITEM_LIST = CivlCParser.BLOCK_ITEM_LIST;
	public static final int CALL = CivlCParser.CALL;
	public static final int CASE_LABELED_STATEMENT = CivlCParser.CASE_LABELED_STATEMENT;
	public static final int CAST = CivlCParser.CAST;
	public static final int COMPOUND_LITERAL = CivlCParser.COMPOUND_LITERAL;
	public static final int COMPOUND_STATEMENT = CivlCParser.COMPOUND_STATEMENT;
	public static final int CONTRACT = CivlCParser.CONTRACT;
	public static final int DECLARATION = CivlCParser.DECLARATION;
	public static final int DECLARATION_LIST = CivlCParser.DECLARATION_LIST;
	public static final int DECLARATION_SPECIFIERS = CivlCParser.DECLARATION_SPECIFIERS;
	public static final int DECLARATOR = CivlCParser.DECLARATOR;
	public static final int DEFAULT_LABELED_STATEMENT = CivlCParser.DEFAULT_LABELED_STATEMENT;
	public static final int DESIGNATED_INITIALIZER = CivlCParser.DESIGNATED_INITIALIZER;
	public static final int DESIGNATION = CivlCParser.DESIGNATION;
	public static final int DIRECT_ABSTRACT_DECLARATOR = CivlCParser.DIRECT_ABSTRACT_DECLARATOR;
	public static final int DIRECT_DECLARATOR = CivlCParser.DIRECT_DECLARATOR;
	public static final int ENUMERATION_CONSTANT = CivlCParser.ENUMERATION_CONSTANT;
	public static final int ENUMERATOR = CivlCParser.ENUMERATOR;
	public static final int ENUMERATOR_LIST = CivlCParser.ENUMERATOR_LIST;
	public static final int EXPRESSION_STATEMENT = CivlCParser.EXPRESSION_STATEMENT;
	public static final int FIELD_DESIGNATOR = CivlCParser.FIELD_DESIGNATOR;
	public static final int FUNCTION_DEFINITION = CivlCParser.FUNCTION_DEFINITION;
	public static final int FUNCTION_SUFFIX = CivlCParser.FUNCTION_SUFFIX;
	public static final int GENERIC_ASSOCIATION = CivlCParser.GENERIC_ASSOCIATION;
	public static final int GENERIC_ASSOC_LIST = CivlCParser.GENERIC_ASSOC_LIST;
	public static final int IDENTIFIER_LABELED_STATEMENT = CivlCParser.IDENTIFIER_LABELED_STATEMENT;
	public static final int IDENTIFIER_LIST = CivlCParser.IDENTIFIER_LIST;
	public static final int INDEX = CivlCParser.INDEX;
	public static final int INITIALIZER_LIST = CivlCParser.INITIALIZER_LIST;
	public static final int INIT_DECLARATOR = CivlCParser.INIT_DECLARATOR;
	public static final int INIT_DECLARATOR_LIST = CivlCParser.INIT_DECLARATOR_LIST;
	public static final int MEM_TYPE = CivlCParser.MEM_TYPE;
	public static final int OPERATOR = CivlCParser.OPERATOR;
	public static final int PARAMETER_DECLARATION = CivlCParser.PARAMETER_DECLARATION;
	public static final int PARAMETER_LIST = CivlCParser.PARAMETER_LIST;
	public static final int PARAMETER_TYPE_LIST = CivlCParser.PARAMETER_TYPE_LIST;
	public static final int PARENTHESIZED_EXPRESSION = CivlCParser.PARENTHESIZED_EXPRESSION;
	public static final int POINTER = CivlCParser.POINTER;
	public static final int POST_DECREMENT = CivlCParser.POST_DECREMENT;
	public static final int POST_INCREMENT = CivlCParser.POST_INCREMENT;
	public static final int PRE_DECREMENT = CivlCParser.PRE_DECREMENT;
	public static final int PRE_INCREMENT = CivlCParser.PRE_INCREMENT;
	public static final int SCALAR_INITIALIZER = CivlCParser.SCALAR_INITIALIZER;
	public static final int SPECIFIER_QUALIFIER_LIST = CivlCParser.SPECIFIER_QUALIFIER_LIST;
	public static final int STRUCT_DECLARATION = CivlCParser.STRUCT_DECLARATION;
	public static final int STRUCT_DECLARATION_LIST = CivlCParser.STRUCT_DECLARATION_LIST;
	public static final int STRUCT_DECLARATOR = CivlCParser.STRUCT_DECLARATOR;
	public static final int STRUCT_DECLARATOR_LIST = CivlCParser.STRUCT_DECLARATOR_LIST;
	public static final int TOKEN_LIST = CivlCParser.TOKEN_LIST;
	public static final int TRANSLATION_UNIT = CivlCParser.TRANSLATION_UNIT;
	public static final int TYPE = CivlCParser.TYPE;
	public static final int TYPEDEF_NAME = CivlCParser.TYPEDEF_NAME;
	public static final int TYPEDOF = CivlCParser.TYPEOF;
	public static final int TYPE_NAME = CivlCParser.TYPE_NAME;
	public static final int TYPE_QUALIFIER_LIST = CivlCParser.TYPE_QUALIFIER_LIST;

}
