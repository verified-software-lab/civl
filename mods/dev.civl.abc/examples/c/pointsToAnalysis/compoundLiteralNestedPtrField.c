/*
 * Points-to test (IntraProceduralPointsToTest.compoundLiteralNestedPtrField):
 * a nested compound literal used as an expression. Reading the pointer field
 * of the nested aggregate should resolve r to point to `a`.
 */
int a;

struct Inner {
    int *p;
};

struct Outer {
    struct Inner in;
    int x;
};

int main() {
    int *r = (struct Outer){{&a}, 2}.in.p;
    return *r;
}
