/*
 * Points-to test (IntraProceduralPointsToTest.compoundInitNestedPtrField):
 * a nested struct compound initializer ({{&a}, 2}). The analysis should
 * descend into the nested aggregate and resolve o.in.p to point to `a`.
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
    struct Outer o = {{&a}, 2};
    return *o.in.p;
}
