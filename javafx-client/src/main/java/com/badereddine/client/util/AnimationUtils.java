package com.badereddine.client.util;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Utility class for UI animations
 */
public class AnimationUtils {

    private AnimationUtils() {
    }

    /**
     * Fade in animation
     */
    public static void fadeIn(Node node, double durationMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    /**
     * Fade in animation with delay
     */
    public static void fadeIn(Node node, double durationMs, double delayMs) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setDelay(Duration.millis(delayMs));
        ft.play();
    }

    /**
     * Slide in from left animation
     */
    public static void slideInFromLeft(Node node, double durationMs) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromX(-100);
        tt.setToX(0);

        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.play();
    }

    /**
     * Slide in from bottom animation
     */
    public static void slideInFromBottom(Node node, double durationMs) {
        node.setOpacity(0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(durationMs), node);
        tt.setFromY(50);
        tt.setToY(0);

        FadeTransition ft = new FadeTransition(Duration.millis(durationMs), node);
        ft.setFromValue(0);
        ft.setToValue(1);

        ParallelTransition pt = new ParallelTransition(tt, ft);
        pt.play();
    }

    /**
     * Scale animation for button hover
     */
    public static void scaleOnHover(Node node) {
        ScaleTransition stEnter = new ScaleTransition(Duration.millis(100), node);
        stEnter.setToX(1.05);
        stEnter.setToY(1.05);

        ScaleTransition stExit = new ScaleTransition(Duration.millis(100), node);
        stExit.setToX(1);
        stExit.setToY(1);

        node.setOnMouseEntered(e -> stEnter.playFromStart());
        node.setOnMouseExited(e -> stExit.playFromStart());
    }

    /**
     * Pulse animation
     */
    public static void pulse(Node node) {
        ScaleTransition st = new ScaleTransition(Duration.millis(200), node);
        st.setFromX(1);
        st.setFromY(1);
        st.setToX(1.1);
        st.setToY(1.1);
        st.setAutoReverse(true);
        st.setCycleCount(2);
        st.play();
    }

    /**
     * Shake animation for errors
     */
    public static void shake(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setByX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.setOnFinished(e -> node.setTranslateX(0));
        tt.play();
    }

    /**
     * Add glow effect on hover
     */
    public static void glowOnHover(Node node, Color color) {
        DropShadow glow = new DropShadow();
        glow.setColor(color);
        glow.setRadius(15);
        glow.setSpread(0.5);

        node.setOnMouseEntered(e -> node.setEffect(glow));
        node.setOnMouseExited(e -> node.setEffect(null));
    }

    /**
     * Create a loading spinner animation
     */
    public static RotateTransition createSpinner(Node node) {
        RotateTransition rt = new RotateTransition(Duration.millis(1000), node);
        rt.setByAngle(360);
        rt.setCycleCount(Animation.INDEFINITE);
        rt.setInterpolator(Interpolator.LINEAR);
        return rt;
    }
}
