package com.acme.motor.api;

import com.acme.motor.application.evaluation.EvaluationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/avaliacao")
public class EvaluationController {
    private final EvaluationService evaluation;

    public EvaluationController(EvaluationService evaluation) {
        this.evaluation = evaluation;
    }

    @PostMapping("/executar")
    public EvaluationService.EvaluationReport execute() {
        return evaluation.run();
    }
}
