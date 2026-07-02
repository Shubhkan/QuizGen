package com.example.quizgen;

import org.springframework.data.jpa.repository.JpaRepository;

public interface QuizRepository extends JpaRepository<Quiz, Long> {
    // Standard JPA Repository gives us save(), findById(), deleteById(), etc.
}