package com.example.quizgen;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/quizzes")
@Tag(name = "Quiz Controller", description = "Endpoints for generating, attempting, and analyzing quizzes using plain text.")
public class QuizController {

    @Autowired
    private QuizService quizService;

    @PostMapping(value = "/generate", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Generate a new quiz (Plain Text)", description = "Enter topic, notes, and the number of questions you want (defaults to 3).")
    public ResponseEntity<String> generateQuiz(
            @RequestParam String topic, 
            @RequestParam String notes,
            @RequestParam(defaultValue = "3") int count) { // Dynamic count parameter
        
        Quiz quiz = quizService.generateAndSaveQuiz(topic, notes, count);
        return ResponseEntity.ok(formatQuizAsText(quiz));
    }

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Get a quiz as a test paper", description = "Enter the ID to view the questions.")
    public ResponseEntity<String> getQuizById(@PathVariable Long id) {
        Quiz quiz = quizService.getQuizById(id);
        return ResponseEntity.ok(formatQuizAsText(quiz));
    }

    @GetMapping(value = "", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "List all quizzes", description = "View a list of all generated quizzes and their IDs.")
    public ResponseEntity<String> getAllQuizzes() {
        List<Quiz> quizzes = quizService.getAllQuizzes();
        if (quizzes.isEmpty()) {
            return ResponseEntity.ok("No quizzes found in the database. Use /generate to create one!");
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== ALL GENERATED QUIZZES ===\n\n");
        for (Quiz q : quizzes) {
            sb.append("Quiz ID: ").append(q.getId())
              .append(" | Title: ").append(q.getTitle())
              .append(" | Topic: ").append(q.getTopic())
              .append(" | Total Questions: ").append(q.getQuestions().size())
              .append("\n");
        }
        return ResponseEntity.ok(sb.toString());
    }

    @DeleteMapping(value = "/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Delete a quiz by ID", description = "Deletes a quiz from database.")
    public ResponseEntity<String> deleteQuiz(@PathVariable Long id) {
        quizService.deleteQuiz(id);
        return ResponseEntity.ok("Quiz with ID " + id + " has been successfully deleted.");
    }

    @PostMapping(value = "/{id}/attempt", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Submit quiz answers (using Letters)", description = "Enter answers as comma-separated letters (e.g. A, C, B, D, A). Case-insensitive.")
    public ResponseEntity<String> submitAttempt(
            @PathVariable Long id, 
            @RequestParam List<String> userAnswers) {
        
        // Convert user's letter answers (A, B, C, D) to standard integer indices (0, 1, 2, 3)
        List<Integer> indices = new java.util.ArrayList<>();
        for (String ans : userAnswers) {
            String clean = ans.trim().toUpperCase();
            switch (clean) {
                case "A" -> indices.add(0);
                case "B" -> indices.add(1);
                case "C" -> indices.add(2);
                case "D" -> indices.add(3);
                default -> throw new IllegalArgumentException("Invalid option: '" + ans + "'. Please choose only A, B, C, or D.");
            }
        }

        // Call the service to grade the converted indices
        QuizAttempt attempt = quizService.submitQuizAttempt(id, indices);
        
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append("           QUIZ SUBMISSION RESULT        \n");
        sb.append("=========================================\n");
        sb.append("Quiz ID:       ").append(attempt.getQuiz().getId()).append("\n");
        sb.append("Quiz Title:    ").append(attempt.getQuiz().getTitle()).append("\n");
        sb.append("Your Score:    ").append(attempt.getScore()).append(" / ").append(attempt.getTotalQuestions()).append("\n");
        double percentage = ((double) attempt.getScore() / attempt.getTotalQuestions()) * 100;
        sb.append("Percentage:    ").append(Math.round(percentage * 100.0) / 100.0).append("%\n");
        sb.append("Submitted At:  ").append(attempt.getAttemptedAt()).append("\n");
        sb.append("=========================================\n");
        
        return ResponseEntity.ok(sb.toString());
    }

    @GetMapping(value = "/attempts", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "View all past attempt history", description = "Lists scores of all previous runs.")
    public ResponseEntity<String> getAttemptHistory() {
        List<QuizAttempt> attempts = quizService.getAttemptHistory();
        if (attempts.isEmpty()) {
            return ResponseEntity.ok("No quiz attempts found in history.");
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== QUIZ ATTEMPT HISTORY ===\n\n");
        for (QuizAttempt attempt : attempts) {
            sb.append("ID: ").append(attempt.getId())
              .append(" | Quiz: ").append(attempt.getQuiz().getTitle())
              .append(" | Score: ").append(attempt.getScore()).append("/").append(attempt.getTotalQuestions())
              .append(" | Date: ").append(attempt.getAttemptedAt())
              .append("\n");
        }
        return ResponseEntity.ok(sb.toString());
    }

    @GetMapping(value = "/analytics", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "View statistics report", description = "Calculates average score and highest scores.")
    public ResponseEntity<String> getAnalyticsSummary() {
        Map<String, Object> summary = quizService.getAnalyticsSummary();
        
        StringBuilder sb = new StringBuilder();
        sb.append("=== ANALYTICS REPORT ===\n\n");
        sb.append("Total Quizzes Attempted:  ").append(summary.get("totalQuizzesTaken")).append("\n");
        sb.append("Average Score Percentage: ").append(summary.get("averagePercentage")).append("%\n");
        sb.append("Highest Score Obtained:   ").append(summary.get("highestScore")).append("\n");
        
        return ResponseEntity.ok(sb.toString());
    }

    // Helper method to format a Quiz object into a human-readable text sheet
    private String formatQuizAsText(Quiz quiz) {
        StringBuilder sb = new StringBuilder();
        sb.append("=========================================\n");
        sb.append(" QUIZ ID: ").append(quiz.getId()).append(" - ").append(quiz.getTitle()).append("\n");
        sb.append(" Topic: ").append(quiz.getTopic()).append("\n");
        sb.append("=========================================\n\n");
        
        int qNum = 1;
        for (Question q : quiz.getQuestions()) {
            sb.append("Q").append(qNum++).append(": ").append(q.getQuestionText()).append("\n");
            sb.append("  A) ").append(q.getOptionA()).append("\n");
            sb.append("  B) ").append(q.getOptionB()).append("\n");
            sb.append("  C) ").append(q.getOptionC()).append("\n");
            sb.append("  D) ").append(q.getOptionD()).append("\n\n");
        }
        return sb.toString();
    }
}