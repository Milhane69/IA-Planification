package sokoban;

import com.codingame.gameengine.runner.SoloGameRunner;

public class SokobanMain {
    public static void main(String[] args) {
        SoloGameRunner gameRunner = new SoloGameRunner();

        // Execution externe pour contourner le bug CodinGame (Javassist / Thread.stop)
        gameRunner.setAgent("java --add-opens java.base/java.lang=ALL-UNNAMED -cp target/sokoban-1.0-SNAPSHOT-jar-with-dependencies.jar sokoban.Agent");

        gameRunner.setTestCase("test26.json");
        gameRunner.start();
    }
}
