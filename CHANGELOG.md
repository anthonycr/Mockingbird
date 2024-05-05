# CHANGELOG

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
