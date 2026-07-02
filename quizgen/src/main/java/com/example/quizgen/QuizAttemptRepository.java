package com.example.quizgen;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizAttemptRepository extends JpaRepository<QuizAttempt, Long> {
    
    // Custom query: JPA automatically creates the SQL select query to fetch 
    // attempts ordered by timestamp descending (newest first).
    List<QuizAttempt> findAllByOrderByAttemptedAtDesc();
}