package gov.cms.ab2d.eventclient.events;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Objects;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MetricsEvent extends LoggableEvent {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MetricsEvent)) return false;
        if (!super.equals(o)) return false;
        MetricsEvent that = (MetricsEvent) o;
        return service == that.service && timeOfEvent.equals(that.timeOfEvent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), service, timeOfEvent);
    }

    @Builder
    public MetricsEvent(String service, @NotNull OffsetDateTime timeOfEvent) {
        super();
        this.service = Services.from(service);
        this.timeOfEvent = timeOfEvent;
    }

    @Override
    public String asMessage() {
        return String.format("(%s) %s", service, timeOfEvent);
    }

    public enum Services {
        API_EFS("api_efs"),
        WORKER_EFS("worker_efs"),
        API_DATABASE("api_database"),
        WORKER_DATABASE("worker_database"),
        API("api"),
        WORKER("worker");

        @JsonValue
        private final String value;

        Services(String value) {
            this.value = value;
        }


        public String getValue() {
            return value;
        }

        public static Services from(String value) {
            return Arrays.stream(Services.values())
                    .filter(services -> services.value.equals(value))
                    .findFirst()
                    .orElse(null);
        }
    }

    @NonNull
    private Services service;

    @NonNull
    private OffsetDateTime timeOfEvent;
}
