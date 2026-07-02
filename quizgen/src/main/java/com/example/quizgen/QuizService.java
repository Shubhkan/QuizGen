package com.example.quizgen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QuizService {

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizAttemptRepository quizAttemptRepository;

    @Autowired
    private GeminiService geminiService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Generates a quiz with 'count' questions, parses it, and saves it.
     */
    @Transactional
    public Quiz generateAndSaveQuiz(String topic, String notes, int count) {
        try {
            // 1. Get raw JSON string from AI passing the dynamic count
            String jsonText = geminiService.generateQuizJson(topic, notes, count);

            // 2. Parse the JSON list into Java Question objects
            List<Question> questions = objectMapper.readValue(jsonText, new TypeReference<List<Question>>() {});

            // 3. Create the Quiz entity
            Quiz quiz = new Quiz();
            quiz.setTitle(topic + " Quiz");
            quiz.setTopic(topic);

            // 4. Map questions to the quiz
            for (Question question : questions) {
                question.setQuiz(quiz);
            }
            quiz.setQuestions(questions);

            // 5. Save the Quiz and its questions
            return quizRepository.save(quiz);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate and save quiz: " + e.getMessage(), e);
        }
    }

    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Quiz not found with ID: " + id));
    }

    @Transactional
    public void deleteQuiz(Long id) {
        if (!quizRepository.existsById(id)) {
            throw new EntityNotFoundException("Quiz not found with ID: " + id);
        }
        quizRepository.deleteById(id);
    }

    @Transactional
    public QuizAttempt submitQuizAttempt(Long quizId, List<Integer> userAnswers) {
        Quiz quiz = getQuizById(quizId);
        List<Question> questions = quiz.getQuestions();

        if (userAnswers.size() != questions.size()) {
            throw new IllegalArgumentException("Incorrect number of answers. Expected: " + questions.size());
        }

        int score = 0;
        for (int i = 0; i < questions.size(); i++) {
            if (questions.get(i).getCorrectOptionIndex() == userAnswers.get(i)) {
                score++;
            }
        }

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setScore(score);
        attempt.setTotalQuestions(questions.size());

        return quizAttemptRepository.save(attempt);
    }

    public List<QuizAttempt> getAttemptHistory() {
        return quizAttemptRepository.findAllByOrderByAttemptedAtDesc();
    }

    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    public Map<String, Object> getAnalyticsSummary() {
        List<QuizAttempt> attempts = quizAttemptRepository.findAll();
        Map<String, Object> analytics = new HashMap<>();

        int totalAttempts = attempts.size();
        analytics.put("totalQuizzesTaken", totalAttempts);

        if (totalAttempts == 0) {
            analytics.put("averagePercentage", 0.0);
            analytics.put("highestScore", 0);
            return analytics;
        }

        double totalPercentage = 0;
        int highestScore = 0;

        for (QuizAttempt attempt : attempts) {
            double percentage = ((double) attempt.getScore() / attempt.getTotalQuestions()) * 100;
            totalPercentage += percentage;
            if (attempt.getScore() > highestScore) {
                highestScore = attempt.getScore();
            }
        }

        analytics.put("averagePercentage", Math.round((totalPercentage / totalAttempts) * 100.0) / 100.0);
        analytics.put("highestScore", highestScore);

        return analytics;
    }
}