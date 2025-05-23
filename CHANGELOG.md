# CHANGELOG

## 2.1.0 *(23 May 2025)*
- Updated KotlinPoet to 2.2.0
- Updated Kotlin to 2.1.21

## 2.0.0 *(15 April 2025)*
- Simplified support for parameter verification that no longer requires function references and instead allows matchers to be passed as arguments.

## 1.7.0 *(26 March 2025)*
- Improve error message when `fake` function is used without properly applying the `kspTest` dependency.

## 1.6.0 *(17 March 2025)*
- Add support to verifying abstract functions in abstract classes with zero argument constructors.

## 1.5.0 *(11 March 2025)*
- Fix issues with incremental compilation not triggering reprocessing.

## 1.4.0 *(2 March 2025)*
- Improve error logging to include the file location in the logs.

## 1.3.1 *(22 December 2024)*
- Lower the Kotlin language version from 2.1 to 2.0 to permit compatibility with older projects.

## 1.3.0 *(17 December 2024)*
- Improve usability by allowing import of `fake` function before it's generated.
- Fix issue where usage of the `fake` function like `val fake = fake<Type>()` would not compile.
- Fix issue where usage of the `fake` function like `val fake = fake(Type::class.java)` would not compile.

## 1.2.0 *(15 September 2024)*
- Add support for `verifyPartial` to allow for verifying only some invocations.

## 1.1.0 *(7 September 2024)*
- Fix issue where lambdas could not be faked.

## 1.0.0 *(5 May 2024)*
- First stable release.
- Removed `times` function since it can be replaced with Kotlin's `repeat` function.
- Added documentation to all public verification functions.

## 0.0.6 *(24 April 2024)*
- Target JVM 11 specifically for the core library to allow for better compatibility.

## 0.0.5 *(24 April 2024)*
- Target JVM 17 toolchain to support inlined bytecode for most consumers.

## 0.0.4 *(24 April 2024)*
- Generate stubbed implementations of non-`Unit` returning functions that just throw exceptions.
- Support generating and verifying `suspend` functions that return `Unit`.

## 0.0.3 *(22 April 2024)*
- Fix issue where interfaces that used `android.util.Pair` would not be verified.

## 0.0.2 *(21 April 2024)*
- Redesign `verifyParams` API to support type safe parameter comparison.

## 0.0.1 *(20 April 2024)*
- Initial version.
