/*
 * Points-to test (IntraProceduralPointsToTest.compoundInitPtrArrayField):
 * a struct compound initializer whose field is an array of pointers
 * ({{&a, &b}}). Currently a smoke test that the analysis completes without
 * error; element-level assertions (s.arr[0] -> a, s.arr[1] -> b) are pending
 * support for array subscripts in createDesignations.
 */
int a;
int b;

struct S {
    int *arr[2];
};

int main() {
    struct S s = {{&a, &b}};
    return *s.arr[0] + *s.arr[1];
}
