package com.example.rocnikovka;

import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

public class Rocnikovka extends Application {

    // === HERNÍ PROMĚNNÉ ===
    private String SECRET_WORD;          // Tajné slovo
    private final int MAX_ATTEMPTS = 6;  // Počet pokusů
    private int currentAttempt = 0;      // Aktuální pokus

    // === UI PRVKY ===
    private GridPane grid = new GridPane();
    private Label[][] labels = new Label[MAX_ATTEMPTS][5]; // Mřížka políček 6x5
    private TextField inputField;
    private Button newGameButton;
    private StackPane root;
    private VBox gameContent;
    private Pane confettiPane;   // Vrstva pro konfety (výhra)
    private Pane curtainPane;    // Vrstva pro závěsy (prohra)
    private Rectangle curtainLeft;
    private Rectangle curtainRight;

    private Random random = new Random();

    // === KONSTRUKTOR ===
    public Rocnikovka() {
        SECRET_WORD = loadRandomWord().toUpperCase();
        System.out.println("Tajné slovo: " + SECRET_WORD); // Pro ladění
    }

    // === NAČTENÍ SLOVA ===

    /**
     * Načte náhodné 5písmenné slovo ze souboru words.txt.
     */
    private String loadRandomWord() {
        List<String> words = new ArrayList<>();

        try (InputStream is = getClass().getResourceAsStream("/words.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // Přidáme jen 5písmenná slova
                if (line.trim().length() == 5 && line.trim().matches("\\p{L}+")) {
                    words.add(line.trim());
                }
            }

        } catch (IOException e) {
            System.err.println("Chyba při načítání words.txt: " + e.getMessage());
        }

        return words.get(random.nextInt(words.size()));
    }

    /**
     * Odstraní diakritiku ze vstupního textu, aby šlo porovnávat
     * písmena bez háčků a čárek (např. 'Á' -> 'A').
     */
    private String normalize(String text) {
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        return Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");
    }

    // === SESTAVENÍ UI ===

    @Override
    public void start(Stage stage) {
        root = new StackPane();
        root.setStyle("-fx-background-color: linear-gradient(to bottom right, #e0f2fe, #d2e9fb);");

        gameContent = new VBox(20);
        gameContent.setPadding(new Insets(20));
        gameContent.setAlignment(Pos.TOP_CENTER);

        setupGrid();
        setupInputField();

        gameContent.getChildren().addAll(grid, inputField);
        root.getChildren().add(gameContent);

        setupConfettiPane();
        setupCurtainPane();
        setupNewGameButton();

        Scene scene = new Scene(root, 500, 580);
        stage.setTitle("Frutiger Aero - Wordle");
        stage.setScene(scene);
        stage.show();

        bindCurtainsToScene(scene);
    }

    /** Vytvoří mřížku 6x5 s prázdnými políčky. */
    private void setupGrid() {
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setAlignment(Pos.CENTER);

        for (int row = 0; row < MAX_ATTEMPTS; row++) {
            for (int col = 0; col < 5; col++) {
                Label label = new Label("");
                label.setPrefSize(60, 60);
                label.setAlignment(Pos.CENTER);
                label.setStyle("-fx-border-color: #a0c2e6; -fx-border-width: 2; -fx-background-color: rgba(255,255,255,0.6); " +
                        "-fx-text-fill: #333333; -fx-font-size: 24; -fx-font-weight: bold; " +
                        "-fx-background-radius: 8; -fx-border-radius: 8;");
                labels[row][col] = label;
                grid.add(label, col, row);
            }
        }
    }

    /** Vytvoří textové pole pro zadávání slov. Enter odešle tip. */
    private void setupInputField() {
        inputField = new TextField();
        inputField.setPromptText("Zadej 5místné slovo a stiskni Enter");
        inputField.setMaxWidth(300);
        inputField.setStyle("-fx-background-color: rgba(255,255,255,0.7); -fx-border-color: #a0c2e6; " +
                "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; " +
                "-fx-prompt-text-fill: #666666; -fx-text-fill: #333333;");
        inputField.setOnAction(e -> handleGuess());
    }

    /** Vytvoří průhlednou vrstvu pro konfety (zobrazí se při výhře). */
    private void setupConfettiPane() {
        confettiPane = new Pane();
        confettiPane.setMouseTransparent(true);
        root.getChildren().add(confettiPane);
    }

    /** Vytvoří vrstvu pro animaci závěsů (zobrazí se při prohře). */
    private void setupCurtainPane() {
        curtainPane = new Pane();
        curtainPane.setMouseTransparent(false);

        curtainLeft = new Rectangle();
        curtainLeft.setFill(Color.web("#1a2a3a"));
        curtainLeft.setVisible(false);

        curtainRight = new Rectangle();
        curtainRight.setFill(Color.web("#1a2a3a"));
        curtainRight.setVisible(false);

        curtainPane.getChildren().addAll(curtainLeft, curtainRight);
        root.getChildren().add(curtainPane);
    }

    /** Vytvoří tlačítko "Další hra", které se zobrazí po skončení hry. */
    private void setupNewGameButton() {
        newGameButton = new Button("Další hra");
        newGameButton.setStyle("-fx-background-color: linear-gradient(to bottom, #87CEEB, #4682B4); " +
                "-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 20;");
        newGameButton.setVisible(false);
        newGameButton.setOnAction(e -> resetGame());
        StackPane.setAlignment(newGameButton, Pos.CENTER);
        root.getChildren().add(newGameButton);
    }

    // === ZÁVĚSY ===

    /** Naváže závěsy na velikost okna – automaticky se přizpůsobí při změně. */
    private void bindCurtainsToScene(Scene scene) {
        setupCurtains(scene.getWidth(), scene.getHeight());
        scene.widthProperty().addListener((obs, oldVal, newVal) -> setupCurtains(newVal.doubleValue(), scene.getHeight()));
        scene.heightProperty().addListener((obs, oldVal, newVal) -> setupCurtains(scene.getWidth(), newVal.doubleValue()));
    }

    /** Nastaví rozměry a pozice závěsů podle aktuální velikosti okna. */
    private void setupCurtains(double w, double h) {
        double half = w / 2;
        curtainPane.setPrefSize(w, h);
        StackPane.setAlignment(curtainPane, Pos.TOP_LEFT);

        curtainLeft.setWidth(half);
        curtainLeft.setHeight(h);
        curtainRight.setWidth(half);
        curtainRight.setHeight(h);

        if (!curtainLeft.isVisible()) {
            // Skryté – mimo obrazovku
            curtainLeft.setX(-half);
            curtainRight.setX(w);
        } else {
            // Viditelné – uprostřed se setkají
            curtainLeft.setX(0);
            curtainRight.setX(half);
        }
    }

    // === HERNÍ LOGIKA ===

    /** Zpracuje tip hráče – zavolá se po stisku Enter. */
    private void handleGuess() {
        String guess = inputField.getText().toUpperCase();
        if (guess.length() == 5 && currentAttempt < MAX_ATTEMPTS) {
            processGuess(guess);
            inputField.clear();
            checkGameStatus(guess);
        }
    }

    /**
     * Vyhodnotí zadané slovo
     */
    private void processGuess(String guess) {
        String secretNorm = normalize(SECRET_WORD);
        String guessNorm  = normalize(guess);

        List<Animation> jumpAnimations = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            char guessChar  = guess.charAt(i);
            char secretChar = SECRET_WORD.charAt(i);
            Label label = labels[currentAttempt][i];
            label.setText(String.valueOf(guessChar));

            if (guessChar == secretChar) {
                // Správné místo – zelená
                label.setStyle("-fx-background-color: linear-gradient(to bottom, #8aff8a, #4caf50); -fx-border-color: #4caf50; " +
                        "-fx-text-fill: white; -fx-font-size: 24; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
                jumpAnimations.add(createJumpAnimation(label));

            } else if (secretNorm.contains(String.valueOf(guessNorm.charAt(i)))) {
                // Špatné místo – žlutá
                label.setStyle("-fx-background-color: linear-gradient(to bottom, #ffeb8a, #ffc107); -fx-border-color: #ffc107; " +
                        "-fx-text-fill: white; -fx-font-size: 24; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");

            } else {
                // Není ve slově – modrá
                label.setStyle("-fx-background-color: linear-gradient(to bottom, #b0c4de, #87a9c8); -fx-border-color: #87a2c8; " +
                        "-fx-text-fill: white; -fx-font-size: 24; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
            }
        }

        currentAttempt++;

        // Pokud hráč uhodl, políčka poskočí
        if (guess.equals(SECRET_WORD)) {
            new ParallelTransition(jumpAnimations.toArray(new Animation[0])).play();
        }
    }

    /** Animace poskočení políčka při správném tipu. */
    private Animation createJumpAnimation(Label label) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), label);
        tt.setByY(-20);
        tt.setCycleCount(2);
        tt.setAutoReverse(true);
        return tt;
    }

    /** Zkontroluje, jestli hráč vyhrál nebo prohrál, a spustí příslušnou animaci. */
    private void checkGameStatus(String guess) {
        if (guess.equals(SECRET_WORD)) {
            inputField.setDisable(true);
            newGameButton.setVisible(true);
            startConfettiAnimation();
        } else if (currentAttempt == MAX_ATTEMPTS) {
            System.out.println("Konec hry. Slovo bylo: " + SECRET_WORD);
            inputField.setDisable(true);
            newGameButton.setVisible(true);
            startCurtainAnimation();
        }
    }

    // === ANIMACE ===

    /** Spustí konfety při výhře – barevné obdélníčky padají z hora dolů. */
    private void startConfettiAnimation() {
        confettiPane.getChildren().clear();

        for (int i = 0; i < 60; i++) {
            Rectangle confetti = new Rectangle(10, 10, randomColor());
            confetti.setX(random.nextDouble() * root.getWidth());
            confetti.setY(-20 - random.nextDouble() * 200);
            confetti.setRotate(random.nextDouble() * 360);
            confettiPane.getChildren().add(confetti);

            TranslateTransition fall = new TranslateTransition(Duration.seconds(random.nextDouble() * 3 + 2), confetti);
            fall.setByY(root.getHeight() + 50);

            FadeTransition fade = new FadeTransition(Duration.seconds(1.5), confetti);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setDelay(Duration.seconds(random.nextDouble() * 2 + 1));

            RotateTransition rotate = new RotateTransition(Duration.seconds(random.nextDouble() * 3 + 2), confetti);
            rotate.setByAngle(random.nextDouble() * 720 + 360);
            rotate.setCycleCount(Timeline.INDEFINITE);

            ParallelTransition pt = new ParallelTransition(confetti, fall, fade, rotate);
            pt.setOnFinished(e -> confettiPane.getChildren().remove(confetti));
            pt.play();
        }
    }

    /** Vrátí náhodnou barvu pro konfety. */
    private Color randomColor() {
        return Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    /** Spustí animaci závěsů při prohře – dva panely se zavřou jako opona. */
    private void startCurtainAnimation() {
        double w    = root.getWidth();
        double h    = root.getHeight();
        double half = w / 2;

        curtainLeft.setWidth(half);
        curtainLeft.setHeight(h);
        curtainRight.setWidth(half);
        curtainRight.setHeight(h);

        curtainLeft.setX(-half);
        curtainRight.setX(w);
        curtainLeft.setVisible(true);
        curtainRight.setVisible(true);

        // Levý závěs jede doprava na x=0, pravý doleva na x=half
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(curtainLeft.xProperty(), -half),
                        new KeyValue(curtainRight.xProperty(), w)),
                new KeyFrame(Duration.seconds(1.5),
                        new KeyValue(curtainLeft.xProperty(), 0, Interpolator.EASE_BOTH),
                        new KeyValue(curtainRight.xProperty(), half, Interpolator.EASE_BOTH))
        );
        timeline.play();
    }

    // RESET HRY

    /** Resetuje hru do počátečního stavu a načte nové tajné slovo. */
    private void resetGame() {
        currentAttempt = 0;
        SECRET_WORD = loadRandomWord().toUpperCase();
        System.out.println("Tajné slovo: " + SECRET_WORD);

        inputField.setDisable(false);
        newGameButton.setVisible(false);
        confettiPane.getChildren().clear();

        // Skrýt závěsy zpět mimo obrazovku
        curtainLeft.setVisible(false);
        curtainRight.setVisible(false);
        curtainLeft.setX(-curtainLeft.getWidth());
        curtainRight.setX(root.getWidth());

        // Vyčistit všechna políčka mřížky
        for (int row = 0; row < MAX_ATTEMPTS; row++) {
            for (int col = 0; col < 5; col++) {
                labels[row][col].setText("");
                labels[row][col].setStyle("-fx-border-color: #a0c2e6; -fx-border-width: 2; " +
                        "-fx-background-color: rgba(255,255,255,0.6); -fx-text-fill: #333333; " +
                        "-fx-font-size: 24; -fx-font-weight: bold; -fx-background-radius: 8; -fx-border-radius: 8;");
            }
        }
    }
}