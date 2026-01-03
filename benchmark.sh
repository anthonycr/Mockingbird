#!/bin/sh

# shellcheck disable=SC2034
under_test="3"
test_count="500"
iterations=10

./gradlew :benchmark:mockingbird:executor:test --dry-run > /dev/null 2>&1
./gradlew :benchmark:mockk-interface:executor:test --dry-run > /dev/null 2>&1
./gradlew :benchmark:mockk-static:executor:test --dry-run > /dev/null 2>&1

elapsed_ms=0
for i in $(seq 1 $iterations);
do
    echo "Run $i / $iterations"
    ./gradlew clean :benchmark:mockingbird:executor:kspTest > /dev/null 2>&1
    start_time=$(date +%s%N)
    ./gradlew :benchmark:mockingbird:executor:test --no-build-cache > /dev/null 2>&1
    end_time=$(date +%s%N)
    duration_ns=$((end_time - start_time))
    duration_ms=$((duration_ns / 1000000))
    elapsed_ms=$((elapsed_ms+duration_ms))
done

echo "Mockingbird execution time in ms: $elapsed_ms"

elapsed_ms=0
for i in $(seq 1 $iterations);
do
    echo "Run $i / $iterations"
    ./gradlew clean :benchmark:mockk-interface:executor:kspTest > /dev/null 2>&1
    start_time=$(date +%s%N)
    ./gradlew :benchmark:mockk-interface:executor:test --no-build-cache > /dev/null 2>&1
    end_time=$(date +%s%N)
    duration_ns=$((end_time - start_time))
    duration_ms=$((duration_ns / 1000000))
    elapsed_ms=$((elapsed_ms+duration_ms))
done

echo "Mockk interface execution time in ms: $elapsed_ms"

elapsed_ms=0
for i in $(seq 1 $iterations);
do
    echo "Run $i / $iterations"
    ./gradlew clean :benchmark:mockk-static:executor:kspTest > /dev/null 2>&1
    start_time=$(date +%s%N)
    ./gradlew :benchmark:mockk-static:executor:test --no-build-cache > /dev/null 2>&1
    end_time=$(date +%s%N)
    duration_ns=$((end_time - start_time))
    duration_ms=$((duration_ns / 1000000))
    elapsed_ms=$((elapsed_ms+duration_ms))
done

echo "Mockk static execution time in ms: $elapsed_ms"
