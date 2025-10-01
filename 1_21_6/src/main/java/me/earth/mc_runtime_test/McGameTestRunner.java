package me.earth.mc_runtime_test;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Similar to running the "/test runall" command.
 */
public class McGameTestRunner {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Basically what happens in {@link TestCommand} when "runall" is used.
     * We just exit with an error code if a test fails.
     *
     * @param playerUUID the uuid of the player.
     * @param server the server to run the tests on.
     */
    public static @Nullable MultipleTestTracker runGameTests(UUID playerUUID, MinecraftServer server) throws ExecutionException, InterruptedException, TimeoutException {
        return server.submit(() -> {
            Player player = Objects.requireNonNull(server.getPlayerList().getPlayer(playerUUID));
            ServerLevel level = (ServerLevel) player.level();
            GameTestRunner.clearMarkers(level);
            Registry<GameTestInstance> registry = level.registryAccess().lookupOrThrow(Registries.TEST_INSTANCE);
            GameTestBatchFactory.TestDecorator testDecorator = GameTestBatchFactory.DIRECT;
            Collection<Holder.Reference<GameTestInstance>> testFunctions =
                registry.listElements().filter((reference) -> !reference.value().manualOnly()).toList();
            LOGGER.info("TestFunctions: {}", testFunctions);
            if (testFunctions.size() < McRuntimeTest.MIN_GAME_TESTS_TO_FIND) {
                LOGGER.error("Failed to find the minimum amount of gametests, expected " + McRuntimeTest.MIN_GAME_TESTS_TO_FIND + ", but found " + testFunctions.size());
                throw new IllegalStateException("Failed to find the minimum amount of gametests, expected " + McRuntimeTest.MIN_GAME_TESTS_TO_FIND + ", but found " + testFunctions.size());
            } else if (testFunctions.isEmpty()) {
                return null;
            }

            FailedTestTracker.forgetFailedTests();

            Collection<GameTestBatch> batches = GameTestBatchFactory.divideIntoBatches(testFunctions, testDecorator, level);
            BlockPos blockPos = new BlockPos(level.random.nextIntBetweenInclusive(-14999992, 14999992), -59, level.random.nextIntBetweenInclusive(-14999992, 14999992));
            level.setDefaultSpawnPos(blockPos, 0.0F);
            GameTestRunner gameTestRunner = GameTestRunner.Builder.fromBatches(batches, level).newStructureSpawner(new StructureGridSpawner(blockPos, 8, false)).build();
            gameTestRunner.start();

            MultipleTestTracker multipleTestTracker = new MultipleTestTracker(gameTestRunner.getTestInfos());
            multipleTestTracker.addFailureListener(gameTestInfo -> {
                LOGGER.error("Test failed: " + gameTestInfo);
                if (gameTestInfo.getError() != null) {
                    LOGGER.error(String.valueOf(gameTestInfo), gameTestInfo.getError());
                }

                if (!gameTestInfo.isOptional() || McRuntimeTest.GAME_TESTS_FAIL_ON_OPTIONAL) {
                    System.exit(-1);
                }
            });

            return multipleTestTracker;
        }).get(60, TimeUnit.SECONDS);
    }

}
