# Mockingbird Fake Generation

Adjust the parameters in the `:benchmark:mockingbird:executor` `build.gradle.kts` in order to
see the way the performance changes, or set `under_test` and `test_count` to change the number of
verification classes and tests to generate.

Generally, the performance is bounded by the number of verification fakes that must be generated.
The test runs are negligible, equivalent to hand written fakes, but each class that must be
generated suffers from the slowness of KSP needing to run.

Given `N` classes that will be generated, `T` tests to be run, `K` fixed overhead for running aKSP,
`G` fixed overhead for processing input and generating a class, and assuming negligible test
runtime, the performance of the test suite can be modeled as follows, where `R` is the total runtime
in milliseconds (ms):

```
R = (N * G)
```
