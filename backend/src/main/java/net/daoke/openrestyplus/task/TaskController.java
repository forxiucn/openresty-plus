package net.daoke.openrestyplus.task;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Tasks")
public class TaskController {
    private final TaskStream stream;
    public TaskController(TaskStream stream) { this.stream = stream; }
    @PostMapping @ResponseStatus(HttpStatus.ACCEPTED) @Operation(summary = "Queue an asynchronous task")
    public TaskAccepted enqueue(@Valid @RequestBody TaskRequest request) {
        var taskId = UUID.randomUUID().toString();
        var messageId = stream.enqueue(taskId, request.type(), request.centerId(), request.artifactId());
        return new TaskAccepted(taskId, messageId);
    }
    public record TaskRequest(@NotBlank String type, @NotBlank String centerId, @NotBlank String artifactId) {}
    public record TaskAccepted(String taskId, String messageId) {}
}
