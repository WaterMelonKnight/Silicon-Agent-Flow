package com.silicon.agentflow.util;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 外部进程执行工具类
 * 用于执行 Docker 命令或其他外部程序
 */
@Slf4j
public class ProcessExecutor {

    /**
     * 执行结果
     */
    public static class ExecutionResult {
        private final int exitCode;
        private final String stdout;
        private final String stderr;
        private final boolean success;

        public ExecutionResult(int exitCode, String stdout, String stderr) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.success = (exitCode == 0);
        }

        public int getExitCode() {
            return exitCode;
        }

        public String getStdout() {
            return stdout;
        }

        public String getStderr() {
            return stderr;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getCombinedOutput() {
            StringBuilder sb = new StringBuilder();
            if (stdout != null && !stdout.isEmpty()) {
                sb.append("=== STDOUT ===\n").append(stdout).append("\n");
            }
            if (stderr != null && !stderr.isEmpty()) {
                sb.append("=== STDERR ===\n").append(stderr).append("\n");
            }
            return sb.toString();
        }
    }

    /**
     * 执行外部命令
     *
     * @param command 命令及参数列表
     * @param timeoutMinutes 超时时间（分钟）
     * @return 执行结果
     */
    public static ExecutionResult execute(List<String> command, long timeoutMinutes) {
        log.info("Executing command: {}", String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(false);

        try {
            Process process = processBuilder.start();

            // 读取标准输出
            StringBuilder stdout = new StringBuilder();
            StringBuilder stderr = new StringBuilder();

            Thread stdoutThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stdout.append(line).append("\n");
                        log.debug("[STDOUT] {}", line);
                    }
                } catch (IOException e) {
                    log.error("Error reading stdout", e);
                }
            });

            Thread stderrThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        stderr.append(line).append("\n");
                        log.debug("[STDERR] {}", line);
                    }
                } catch (IOException e) {
                    log.error("Error reading stderr", e);
                }
            });

            stdoutThread.start();
            stderrThread.start();

            // 等待进程完成
            boolean finished = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);

            if (!finished) {
                process.destroyForcibly();
                log.error("Process timed out after {} minutes", timeoutMinutes);
                return new ExecutionResult(-1, stdout.toString(),
                    stderr.toString() + "\nProcess timed out");
            }

            stdoutThread.join();
            stderrThread.join();

            int exitCode = process.exitValue();
            log.info("Process completed with exit code: {}", exitCode);

            return new ExecutionResult(exitCode, stdout.toString(), stderr.toString());

        } catch (IOException e) {
            log.error("Failed to start process", e);
            return new ExecutionResult(-1, "", "Failed to start process: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Process interrupted", e);
            return new ExecutionResult(-1, "", "Process interrupted: " + e.getMessage());
        }
    }

    /**
     * 执行外部命令（默认 30 分钟超时）
     */
    public static ExecutionResult execute(List<String> command) {
        return execute(command, 30);
    }
}
