package org.example.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import org.example.MainApp;

/**
 * Home Controller with smooth animations
 * Landing page with options to Login or Register
 */
public class HomeController {

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    @FXML
    private Button exitButton;

    @FXML
    private StackPane appIconContainer;

    @FXML
    private Label appTitleLabel;

    @FXML
    private VBox homeCard;

    @FXML
    public void initialize() {
        setupAnimations();
        setupButtonHoverEffects();
        startIdleAnimations();
    }

    /**
     * Setup entrance animations
     */
    private void setupAnimations() {
        if (homeCard != null) {
            // Fade in animation
            FadeTransition fadeIn = new FadeTransition(Duration.millis(800), homeCard);
            fadeIn.setFromValue(0.0);
            fadeIn.setToValue(1.0);
            fadeIn.play();

            // Slide up animation
            TranslateTransition slideUp = new TranslateTransition(Duration.millis(800), homeCard);
            slideUp.setFromY(30);
            slideUp.setToY(0);
            slideUp.setInterpolator(Interpolator.EASE_OUT);
            slideUp.play();
        }
    }

    /**
     * Setup sophisticated button hover effects
     */
    private void setupButtonHoverEffects() {
        setupButtonAnimation(loginButton, Color.rgb(92, 107, 192, 0.35), 1.03);
        setupButtonAnimation(registerButton, Color.rgb(255, 255, 255, 0.25), 1.02);
        setupButtonAnimation(exitButton, Color.rgb(255, 100, 100, 0.2), 1.015);
    }

    /**
     * Setup individual button animation
     */
    private void setupButtonAnimation(Button button, Color glowColor, double hoverScale) {
        if (button == null) return;

        DropShadow glow = new DropShadow();
        glow.setColor(glowColor);
        glow.setRadius(15);
        glow.setSpread(0.3);

        button.setOnMouseEntered(e -> {
            // Scale animation
            ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), button);
            scaleUp.setToX(hoverScale);
            scaleUp.setToY(hoverScale);
            scaleUp.setInterpolator(Interpolator.EASE_OUT);
            scaleUp.play();

            // Glow animation
            Timeline glowIn = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(glow.radiusProperty(), 10)),
                new KeyFrame(Duration.millis(200),
                    new KeyValue(glow.radiusProperty(), 20, Interpolator.EASE_OUT))
            );
            glowIn.play();
            button.setEffect(glow);
        });

        button.setOnMouseExited(e -> {
            // Scale back animation
            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), button);
            scaleDown.setToX(1.0);
            scaleDown.setToY(1.0);
            scaleDown.setInterpolator(Interpolator.EASE_IN);
            scaleDown.play();

            // Glow out animation
            Timeline glowOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                    new KeyValue(glow.radiusProperty(), 20)),
                new KeyFrame(Duration.millis(200),
                    new KeyValue(glow.radiusProperty(), 10, Interpolator.EASE_IN))
            );
            glowOut.play();
        });

        button.setOnMousePressed(e -> {
            ScaleTransition press = new ScaleTransition(Duration.millis(100), button);
            press.setToX(hoverScale * 0.97);
            press.setToY(hoverScale * 0.97);
            press.play();
        });

        button.setOnMouseReleased(e -> {
            ScaleTransition release = new ScaleTransition(Duration.millis(100), button);
            release.setToX(hoverScale);
            release.setToY(hoverScale);
            release.play();
        });
    }

    /**
     * Start subtle idle animations
     */
    private void startIdleAnimations() {
        if (appIconContainer != null) {
            // Gentle floating animation
            TranslateTransition float1 = new TranslateTransition(Duration.seconds(3), appIconContainer);
            float1.setFromY(0);
            float1.setToY(-8);
            float1.setCycleCount(Animation.INDEFINITE);
            float1.setAutoReverse(true);
            float1.setInterpolator(Interpolator.EASE_BOTH);
            float1.play();

            // Subtle rotation animation
            RotateTransition rotate = new RotateTransition(Duration.seconds(20), appIconContainer);
            rotate.setFromAngle(-2);
            rotate.setToAngle(2);
            rotate.setCycleCount(Animation.INDEFINITE);
            rotate.setAutoReverse(true);
            rotate.setInterpolator(Interpolator.EASE_BOTH);
            rotate.play();
        }

        if (appTitleLabel != null) {
            // Subtle fade pulse for title
            FadeTransition pulse = new FadeTransition(Duration.seconds(4), appTitleLabel);
            pulse.setFromValue(0.95);
            pulse.setToValue(1.0);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.setAutoReverse(true);
            pulse.setInterpolator(Interpolator.EASE_BOTH);
            pulse.play();
        }
    }

    @FXML
    private void handleLogin() {
        playExitAnimation(() -> MainApp.loadLogin());
    }

    @FXML
    private void handleRegister() {
        playExitAnimation(() -> MainApp.loadRegister());
    }

    @FXML
    private void handleExit() {
        playExitAnimation(() -> System.exit(0));
    }

    /**
     * Play smooth exit animation before scene change
     */
    private void playExitAnimation(Runnable action) {
        if (homeCard != null) {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), homeCard);
            fadeOut.setToValue(0.0);

            ScaleTransition scaleDown = new ScaleTransition(Duration.millis(300), homeCard);
            scaleDown.setToX(0.95);
            scaleDown.setToY(0.95);

            ParallelTransition exit = new ParallelTransition(fadeOut, scaleDown);
            exit.setOnFinished(e -> action.run());
            exit.play();
        } else {
            action.run();
        }
    }
}

