# Mockk Static Mocking

Adjust the parameters in the `:benchmark:mockk-static:executor` `build.gradle.kts` in order to
see the way the performance changes, or set `under_test` and `test_count` to change the number of
verification classes and tests to generate.

Generally, the performance is bounded both by the number of mocked static classes, with additional
fixed overhead for each additional mock, and the number of tests. This is because `mockkStatic` has
fixed overhead for each call and must be called for each test, assuming `unmockkAll` is correctly
called when the test is torn down. This means that changing the `benchmark.interface_count`
parameter will have a meaningful effect on the performance of the benchmark. Additionally, the
performance also not scales linearly when changing the `benchmark.test_count` parameter. The
performance generally can be modeled around the number of times the `mockkStatic` function is
called, which is a multiple of the number of classes statically mocked and the number of tests that
run using those static mocks.

Given `N` classes that will be statically mocked, `T` tests to be run, `M` fixed overhead for
mocking a class, `S` fixed overhead for calling `mockkStatic`, and assuming negligible test runtime,
the performance of the test suite can be modeled as follows, where `R` is the total runtime in
milliseconds (ms):

```
R = (N * M) + (N * T * S)
```
