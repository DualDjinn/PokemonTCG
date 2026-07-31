package ar.edu.utn.frc.tup.piii.judge.store;

import ar.edu.utn.frc.tup.piii.judge.events.DamageResolvedEvent;
import ar.edu.utn.frc.tup.piii.judge.events.JudgeEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

@Component
public class JudgeEventStore {

    private final ConcurrentHashMap<String, CopyOnWriteArrayList<JudgeEvent>> logs =
            new ConcurrentHashMap<>();

    public void record(JudgeEvent event) {
        if (event == null) {
            return;
        }
        logs.computeIfAbsent(event.getEngineMatchId(), k -> new CopyOnWriteArrayList<>())
                .add(event);
    }

    public List<JudgeEvent> getLog(String engineMatchId) {
        List<JudgeEvent> list = logs.get(engineMatchId);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public <T extends JudgeEvent> List<T> getEventsOfType(String engineMatchId, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (JudgeEvent event : getLog(engineMatchId)) {
            if (type.isInstance(event)) {
                result.add(type.cast(event));
            }
        }
        return Collections.unmodifiableList(result);
    }

    public Optional<DamageResolvedEvent> getLastDamageEvent(String engineMatchId) {
        List<DamageResolvedEvent> events = getEventsOfType(engineMatchId, DamageResolvedEvent.class);
        if (events.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(events.get(events.size() - 1));
    }

    public void clearLog(String engineMatchId) {
        logs.remove(engineMatchId);
    }
}
