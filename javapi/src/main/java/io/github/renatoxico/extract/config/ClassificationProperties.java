package io.github.renatoxico.extract.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "classification")
public class ClassificationProperties {
    private int batchSize = 50;
    private int propagationBatchSize = 50;
    private int maxAiAttempts = 3;
    private int maxApplyAttempts = 3;
    private Duration leaseDuration = Duration.ofMinutes(10);
    private List<Duration> retryDelays = List.of(
        Duration.ofMinutes(1),
        Duration.ofMinutes(5),
        Duration.ofMinutes(15)
    );

    public Duration retryDelay(int completedAttempts) {
        int index = Math.max(0, Math.min(completedAttempts - 1, retryDelays.size() - 1));
        return retryDelays.get(index);
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public int getPropagationBatchSize() {
        return propagationBatchSize;
    }

    public void setPropagationBatchSize(int propagationBatchSize) {
        this.propagationBatchSize = propagationBatchSize;
    }

    public int getMaxAiAttempts() {
        return maxAiAttempts;
    }

    public void setMaxAiAttempts(int maxAiAttempts) {
        this.maxAiAttempts = maxAiAttempts;
    }

    public int getMaxApplyAttempts() {
        return maxApplyAttempts;
    }

    public void setMaxApplyAttempts(int maxApplyAttempts) {
        this.maxApplyAttempts = maxApplyAttempts;
    }

    public Duration getLeaseDuration() {
        return leaseDuration;
    }

    public void setLeaseDuration(Duration leaseDuration) {
        this.leaseDuration = leaseDuration;
    }

    public List<Duration> getRetryDelays() {
        return retryDelays;
    }

    public void setRetryDelays(List<Duration> retryDelays) {
        if (retryDelays == null || retryDelays.isEmpty()) {
            throw new IllegalArgumentException("classification.retry-delays must not be empty");
        }
        this.retryDelays = List.copyOf(retryDelays);
    }
}
