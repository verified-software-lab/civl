#ifndef _COMPLEX_
#define _COMPLEX_
/* The header complex.h defines macros and declares functions that support complex
 * arithmetic.  CIVL will transform these to use a struct type.
 */

// includes to load libraries needed by ComplexTransformer:
#include <math.h>

/* Macros */
#define complex _Complex
#define _Complex_I 1.0if
#define I _Complex_I

// These are described as macros in the C23 Standard, but we use
// abstract functions that will be replaced with expressions...
$abstract double complex       CMPLX(double x, double y);
$abstract float complex        CMPLXF(float x, float y);
$abstract long double complex  CMPLXL(long double x, long double y);

/* Functions.  Note all are $abstract so can be used in quantified
   expressions. */

// These functions will be replaced with expressions with the precise
// semantics...

$abstract double               cabs(double complex);
$abstract float                cabsf(float complex);
$abstract long double          cabsl(long double complex);
$abstract double               cimag(double complex);
$abstract float                cimagf(float complex);
$abstract long double          cimagl(long double complex);
$abstract double complex       conj(double complex);
$abstract float complex        conjf(float complex);
$abstract long double complex  conjl(long double complex);
$abstract double               creal(double complex);
$abstract float                crealf(float complex);
$abstract long double          creall(long double complex);

// These will be kept uninterpreted...

$abstract double complex       cacos(double complex);
$abstract float complex        cacosf(float complex);
$abstract double complex       cacosh(double complex);
$abstract float complex        cacoshf(float complex);
$abstract long double complex  cacoshl(long double complex);
$abstract long double complex  cacosl(long double complex);
$abstract double               carg(double complex);
$abstract float                cargf(float complex);
$abstract long double          cargl(long double complex);
$abstract double complex       casin(double complex);
$abstract float complex        casinf(float complex);
$abstract double complex       casinh(double complex);
$abstract float complex        casinhf(float complex);
$abstract long double complex  casinhl(long double complex);
$abstract long double complex  casinl(long double complex);
$abstract double complex       catan(double complex);
$abstract float complex        catanf(float complex);
$abstract double complex       catanh(double complex);
$abstract float complex        catanhf(float complex);
$abstract long double complex  catanhl(long double complex);
$abstract long double complex  catanl(long double complex);
$abstract double complex       ccos(double complex);
$abstract float complex        ccosf(float complex);
$abstract double complex       ccosh(double complex);
$abstract float complex        ccoshf(float complex);
$abstract long double complex  ccoshl(long double complex);
$abstract long double complex  ccosl(long double complex);
$abstract double complex       cexp(double complex);
$abstract float complex        cexpf(float complex);
$abstract long double complex  cexpl(long double complex);
$abstract double complex       clog(double complex);
$abstract float complex        clogf(float complex);
$abstract long double complex  clogl(long double complex);
$abstract double complex       cpow(double complex, double complex);
$abstract float complex        cpowf(float complex, float complex);
$abstract long double complex  cpowl(long double complex, long double complex);
$abstract double complex       cproj(double complex);
$abstract float complex        cprojf(float complex);
$abstract long double complex  cprojl(long double complex);
$abstract double complex       csin(double complex);
$abstract float complex        csinf(float complex);
$abstract double complex       csinh(double complex);
$abstract float complex        csinhf(float complex);
$abstract long double complex  csinhl(long double complex);
$abstract long double complex  csinl(long double complex);
$abstract double complex       csqrt(double complex);
$abstract float complex        csqrtf(float complex);
$abstract long double complex  csqrtl(long double complex);
$abstract double complex       ctan(double complex);
$abstract float complex        ctanf(float complex);
$abstract double complex       ctanh(double complex);
$abstract float complex        ctanhf(float complex);
$abstract long double complex  ctanhl(long double complex);
$abstract long double complex  ctanl(long double complex);

#endif

