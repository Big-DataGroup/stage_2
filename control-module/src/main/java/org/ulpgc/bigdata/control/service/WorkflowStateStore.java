package org.ulpgc.bigdata.control.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class WorkflowStateStore {
    private final Path path;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Map<Integer, String> states = new HashMap<>();

    public WorkflowStateStore() throws IOException {
        this(Path.of(System.getenv().getOrDefault(
                "CONTROL_STATE_PATH", "data/control/workflow-state.json")));
    }

    public WorkflowStateStore(Path path) throws IOException {
        this.path = path;
        if (Files.exists(path)) {
            Type type = new TypeToken<Map<Integer, String>>() {}.getType();
            Map<Integer, String> loaded = gson.fromJson(Files.readString(path), type);
            if (loaded != null) states.putAll(loaded);
        }
    }

    public synchronized void mark(int bookId, String state) throws IOException {
        states.put(bookId, state);
        if (path.getParent() != null) Files.createDirectories(path.getParent());
        Files.writeString(path, gson.toJson(states));
    }

    public synchronized String state(int bookId) {
        return states.getOrDefault(bookId, "UNKNOWN");
    }
}
