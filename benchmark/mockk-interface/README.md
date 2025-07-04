# Mockk Interface Mocking

Adjust the parameters in the `:benchmark:mockk-interface:executor` `build.gradle.kts` in order to
see the way the performance changes, or set `under_test` and `test_count` to change the number of
verification classes and tests to generate.

Generally, the performance is bounded by the number of mocked interfaces, with additional fixed
overhead for each additional mock. This means that changing the `benchmark.interface_count`
parameter will have a meaningful effect on the performance of the benchmark. On the other hand, the
performance does not scale linearly when changing the `benchmark.test_count` parameter. There is
fixed overhead for each interface mocked, but it is incurred the first time the interface is mocked
in the Java process.

Given `N` classes that will be mocked, `T` tests to be run, `M` fixed overhead for mocking a class,
and assuming negligible test runtime, the performance of the test suite can be modeled as follows,
where `R` is the total runtime in milliseconds (ms):

```
R = (N * M)
```
