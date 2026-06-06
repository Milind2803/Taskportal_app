package com.milind.taskportal.repository;

import com.milind.taskportal.model.Task;
import com.milind.taskportal.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByAssignedToOrderByDeadlineAsc(User user);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.status = :status")
    long countByStatus(Task.Status status);
}
