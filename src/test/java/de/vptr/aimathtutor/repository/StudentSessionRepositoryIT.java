package de.vptr.aimathtutor.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import de.vptr.aimathtutor.entity.ExerciseEntity;
import de.vptr.aimathtutor.entity.StudentSessionEntity;
import de.vptr.aimathtutor.entity.UserEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@QuarkusTest
public class StudentSessionRepositoryIT {

    @Inject
    StudentSessionRepository sessionRepository;
    @Inject
    ExerciseRepository exerciseRepository;
    @Inject
    UserRepository userRepository;

    @Test
    @Transactional
    public void testFindByUserAndDateRange() {
        UserEntity user = new UserEntity();
        user.username = "sessionuser";
        user.password = "pw";
        user.salt = "salt";
        user.email = "session@example.com";
        user.activated = true;
        userRepository.persist(user);

        ExerciseEntity ex = new ExerciseEntity();
        ex.title = "Session Exercise";
        ex.content = "x + 2";
        ex.user = user;
        ex.published = true;
        exerciseRepository.persist(ex);

        StudentSessionEntity session = new StudentSessionEntity();
        session.sessionId = "sess-1";
        session.user = user;
        session.exercise = ex;
        session.startTime = LocalDateTime.now().minusDays(1);
        session.completed = true;
        sessionRepository.persist(session);

        List<StudentSessionEntity> found = sessionRepository.findByUserIdAndDateRange(user.id,
                LocalDateTime.now().minusDays(2), LocalDateTime.now());
        Assertions.assertFalse(found.isEmpty());
        Assertions.assertEquals("sess-1", found.get(0).sessionId);
    }
}
