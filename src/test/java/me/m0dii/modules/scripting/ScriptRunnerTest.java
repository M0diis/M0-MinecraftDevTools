package me.m0dii.modules.scripting;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ScriptRunnerTest {
    private static Thread testThread;
    private final GroovyScriptManager groovy = new GroovyScriptManager();

    @BeforeAll
    static void useTestThreadAsClientThread() {
        testThread = Thread.currentThread();
        ScriptRunner.useMainThreadForTests(Runnable::run, () -> Thread.currentThread() == testThread);
    }

    private ScriptRunner.Outcome runSync(String source, Map<String, Object> context) {
        List<ScriptRunner.Outcome> outcomes = new ArrayList<>();
        ScriptRunner.run("test", groovy, source, context, false, outcomes::add);
        assertEquals(1, outcomes.size());
        return outcomes.getFirst();
    }

    @Test
    void returnsScriptResult() {
        ScriptRunner.Outcome outcome = runSync("1 + 2", Map.of());
        assertTrue(outcome.success());
        assertEquals(3, outcome.result());
    }

    @Test
    void compileErrorsAreReportedNotSwallowed() {
        ScriptRunner.Outcome outcome = runSync("def x = ", Map.of());
        assertFalse(outcome.success());
        assertTrue(outcome.error().startsWith("Groovy compile error"), outcome.error());
    }

    @Test
    void runtimeErrorsAreReported() {
        ScriptRunner.Outcome outcome = runSync("throw new RuntimeException('boom')", Map.of());
        assertFalse(outcome.success());
        assertTrue(outcome.error().contains("boom"), outcome.error());
    }

    @Test
    void contextAndHelperAreBound() {
        assertEquals("Steve", runSync("player", Map.of("player", "Steve")).result());
        assertEquals(false, runSync("m0.isAsync()", Map.of()).result());
    }

    @Test
    void sleepIsRefusedOnTheClientThread() {
        ScriptRunner.Outcome outcome = runSync("m0.sleep(10)", Map.of());
        assertFalse(outcome.success());
        assertTrue(outcome.error().contains("@async"), outcome.error());
    }

    @Test
    void asyncScriptsRunOffTheClientThread() throws Exception {
        CompletableFuture<ScriptRunner.Outcome> done = new CompletableFuture<>();
        ScriptRunner.run("async", groovy, "m0.sleep(20); m0.isAsync()", Map.of(), true, done::complete);
        ScriptRunner.Outcome outcome = done.get(10, TimeUnit.SECONDS);
        assertTrue(outcome.success(), String.valueOf(outcome.error()));
        assertEquals(true, outcome.result());
    }

    @Test
    void stopCancelsARunawayAsyncScript() throws Exception {
        CompletableFuture<ScriptRunner.Outcome> done = new CompletableFuture<>();
        ScriptRunner.run("loop", groovy, "long i = 0; while (true) { i++ }", Map.of(), true, done::complete);
        Thread.sleep(200);
        assertEquals(1, ScriptRunner.cancelAll());
        ScriptRunner.Outcome outcome = done.get(5, TimeUnit.SECONDS);
        assertFalse(outcome.success());
        assertTrue(outcome.error().contains("was stopped"), outcome.error());

        CompletableFuture<ScriptRunner.Outcome> next = new CompletableFuture<>();
        ScriptRunner.run("next", groovy, "'ok'", Map.of(), true, next::complete);
        assertEquals("ok", next.get(5, TimeUnit.SECONDS).result());
    }

    @Test
    void asyncDirectiveIsReadFromTheHeaderOnly() {
        assertTrue(ScriptDirectives.isAsync("// @async on\nprintln 1"));
        assertFalse(ScriptDirectives.isAsync("// @async off\nprintln 1"));
        assertFalse(ScriptDirectives.isAsync("println 1\n// @async on"));
    }

    @Test
    void stoppedScriptCannotExecuteItsQueuedClientAction() throws Exception {
        LinkedBlockingQueue<Runnable> queued = new LinkedBlockingQueue<>();
        AtomicInteger effects = new AtomicInteger();
        CompletableFuture<ScriptRunner.Outcome> finished = new CompletableFuture<>();
        ScriptRunner.useMainThreadForTests(queued::add, () -> false);
        try {
            ScriptManager manager = new ScriptManager() {
                public String languageName() { return "test"; }
                public Object runScript(String source, Map<String, Object> bindings, ScriptJob job) {
                    ((ScriptApi) bindings.get("m0")).sync(effects::incrementAndGet);
                    return null;
                }
            };
            ScriptRunner.run("queued", manager, "", Map.of(), true, finished::complete);
            Runnable action = queued.poll(5, TimeUnit.SECONDS);
            assertNotNull(action);
            ScriptRunner.cancelAll();
            action.run();
            Runnable callback = queued.poll(5, TimeUnit.SECONDS);
            assertNotNull(callback);
            callback.run();
            assertFalse(finished.get(5, TimeUnit.SECONDS).success());
            assertEquals(0, effects.get());
        } finally {
            ScriptRunner.cancelAll();
            useTestThreadAsClientThread();
        }
    }

    @Test
    void cancelledDirectClientCallIsAlsoRefused() {
        ScriptJob job = new ScriptJob("stopped", false, 5000);
        job.cancel("was stopped");
        assertThrows(ScriptExecutionException.class, () -> new ScriptApi(job).call(() -> "unexpected"));
    }
}
