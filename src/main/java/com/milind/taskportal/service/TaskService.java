package com.milind.taskportal.service;

import com.milind.taskportal.model.Task;
import com.milind.taskportal.model.User;
import com.milind.taskportal.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;

    public Task save(Task task) { return taskRepository.save(task); }
    public List<Task> findAll() { return taskRepository.findAll(); }

    public List<Task> findByUser(User user) {
        return taskRepository.findByAssignedToOrderByDeadlineAsc(user);
    }

    public Task findById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));
    }

    public void delete(Long id) { taskRepository.deleteById(id); }

    public Task updateStatus(Long id, Task.Status status) {
        Task task = findById(id);
        task.setStatus(status);
        return taskRepository.save(task);
    }

    public Map<String, Long> getStats() {
        return Map.of(
            "Total",      taskRepository.count(),
            "Todo",       taskRepository.countByStatus(Task.Status.TODO),
            "In Progress",taskRepository.countByStatus(Task.Status.IN_PROGRESS),
            "Review",     taskRepository.countByStatus(Task.Status.REVIEW),
            "Done",       taskRepository.countByStatus(Task.Status.DONE)
        );
    }
}
