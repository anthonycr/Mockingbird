#!/bin/sh

# shellcheck disable=SC2034
under_test="3"
test_count="500"

iterations=10


./gradlew clean :mockingbird:processor:build > /dev/null 2>&1
./gradlew :benchmark:mockingbird:executor:test --dry-run > /dev/null 2>&1
./gradlew :benchmark:mockk-interface:executor:test --dry-run > /dev/null 2>&1
./gradlew :benchmark:mockk-static:executor:test --dry-run > /dev/null 2>&1

start_time=$(date +%s%N)

for i in $(seq 1 $iterations);
do
    echo "Run $i / $iterations"
    ./gradlew :benchmark:mockingbird:executor:test --rerun-tasks > /dev/null 2>&1
done

end_time=$(date +%s%N)
duration_ns=$((end_time - start_time))
duration_ms=$((duration_ns / 1000000))

echo "Mockingbird execution time in ms: $duration_ms"

start_time=$(date +%s%N)

for i in $(seq 1 $iterations);
do
    echo "Run $i / $iterations"
    ./gradlew :benchmark:mockk-interface:executor:test --rerun-tasks > /dev/null 2>&1
done

end_time=$(date +%s%N)
duration_ns=$((end_time - start_time))
duration_ms=$((duration_ns / 1000000))

echo "Mockk interface execution time in ms: $duration_ms"

start_time=$(date +%s%N)

for i in $(seq 1 $iterations);
do
    echo "Run $i / $iterations"
    ./gradlew :benchmark:mockk-static:executor:test --rerun-tasks > /dev/null 2>&1
done

end_time=$(date +%s%N)
duration_ns=$((end_time - start_time))
duration_ms=$((duration_ns / 1000000))

echo "Mockk static execution time in ms: $duration_ms"
