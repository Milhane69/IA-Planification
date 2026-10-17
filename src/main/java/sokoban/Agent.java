package sokoban;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import fr.uga.pddl4j.parser.DefaultParsedProblem;
import fr.uga.pddl4j.parser.Parser;
import fr.uga.pddl4j.plan.Plan;
import fr.uga.pddl4j.planners.statespace.HSP;
import fr.uga.pddl4j.problem.DefaultProblem;
import fr.uga.pddl4j.problem.Problem;
import fr.uga.pddl4j.problem.operator.Action;

public class Agent {

    private static final int TIMEOUT_SECONDS = 18;
    private static final int[][] DIRS = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}}; // u, d, l, r
    private static final String[] DIR_NAMES = {"u", "d", "l", "r"};

    public static void main(String[] args) {
        System.err.println("--- DEMARRAGE DE L'AGENT ---");

        Scanner in = new Scanner(System.in);
        if (!in.hasNextInt()) {
            System.err.println("ERREUR : aucune donnee recue du jeu.");
            return;
        }

        int width = in.nextInt();
        int height = in.nextInt();
        int boxCount = in.nextInt();

        List<String> grid = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            grid.add(in.next());
        }

        int pusherX = in.nextInt();
        int pusherY = in.nextInt();

        int[][] boxes = new int[boxCount][2];
        for (int i = 0; i < boxCount; i++) {
            boxes[i][0] = in.nextInt();
            boxes[i][1] = in.nextInt();
        }

        String solution = "";
        try {
            solution = solve(width, height, grid, pusherX, pusherY, boxes);
        } catch (Throwable t) {
            System.err.println("ERREUR pendant la resolution : " + t);
        }

        if (solution.isEmpty()) {
            System.err.println("ECHEC : aucun plan. Passage en mode SURVIE (aleatoire).");
            jouerAleatoire(in, boxCount);
            return;
        }

        System.err.println("SUCCES ! Envoi des mouvements : " + solution);
        for (int i = 0; i < solution.length(); i++) {
            System.out.println(solution.charAt(i));
            System.out.flush();

            if (i == solution.length() - 1) break;

            if (in.hasNextInt()) {
                in.nextInt(); in.nextInt();
                for (int j = 0; j < boxCount; j++) {
                    in.nextInt(); in.nextInt();
                }
            } else {
                break;
            }
        }
    }

    // ------------------------------------------------------------------
    // Resolution PDDL
    // ------------------------------------------------------------------
    private static String solve(int width, int height, List<String> grid,
                                int pusherX, int pusherY, int[][] boxes) throws Exception {

        Path problemPath = Files.createTempFile("sokoban-problem", ".pddl");
        Path domainPath = Files.createTempFile("sokoban-domain", ".pddl");
        problemPath.toFile().deleteOnExit();
        domainPath.toFile().deleteOnExit();

        // Domaine : depuis le classpath (jar), sinon depuis le dossier du projet
        try (InputStream is = Agent.class.getResourceAsStream("/pddl/domain.pddl")) {
            if (is != null) {
                Files.copy(is, domainPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(Paths.get("src/main/resources/pddl/domain.pddl"), domainPath,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }

        System.err.println("Generation du probleme PDDL...");
        String problemText = generateProblem(width, height, grid, pusherX, pusherY, boxes);
        Files.write(problemPath, problemText.getBytes(StandardCharsets.UTF_8));

        PrintStream originalOut = System.out;
        Plan plan = null;

        try {
            // Rend PDDL4J muet (stdout sert uniquement a parler au jeu)
            System.setOut(new PrintStream(new OutputStream() {
                @Override public void write(int b) {}
            }));

            System.err.println("Parsing du domaine et du probleme...");
            Parser parser = new Parser();
            DefaultParsedProblem parsed = parser.parse(domainPath.toFile(), problemPath.toFile());
            final Problem problem = new DefaultProblem(parsed);
            problem.instantiate();

            System.err.println("Recherche avec HSP (max " + TIMEOUT_SECONDS + "s)...");
            ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r);
                t.setDaemon(true); // ne bloque pas la fin de la JVM si HSP ne s'arrete pas
                return t;
            });
            Future<Plan> future = executor.submit(new Callable<Plan>() {
                @Override
                public Plan call() throws Exception {
                    HSP planner = new HSP();
                    return planner.solve(problem);
                }
            });

            try {
                plan = future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                System.err.println("TIMEOUT : HSP a mis trop de temps.");
                future.cancel(true);
            }
            executor.shutdownNow();
        } finally {
            System.setOut(originalOut);
        }

        StringBuilder solution = new StringBuilder();
        if (plan != null && plan.actions() != null) {
            for (Object o : plan.actions()) {
                String name = ((Action) o).getName().toLowerCase(); // move-u, push-l, ...
                char d = Character.toUpperCase(name.charAt(name.length() - 1));
                if (d == 'U' || d == 'D' || d == 'L' || d == 'R') solution.append(d);
            }
        }
        return solution.toString();
    }

    // ------------------------------------------------------------------
    // Mode survie
    // ------------------------------------------------------------------
    private static void jouerAleatoire(Scanner in, int boxCount) {
        String[] dirs = {"U", "D", "L", "R"};
        while (true) {
            System.out.println(dirs[(int) (Math.random() * 4)]);
            System.out.flush();
            if (in.hasNextInt()) {
                in.nextInt(); in.nextInt();
                for (int j = 0; j < boxCount; j++) {
                    in.nextInt(); in.nextInt();
                }
            } else {
                break;
            }
        }
    }

    // ------------------------------------------------------------------
    // Generation du probleme PDDL
    // ------------------------------------------------------------------
    private static char at(List<String> g, int x, int y) {
        if (y < 0 || y >= g.size()) return '#';
        String row = g.get(y);
        if (x < 0 || x >= row.length()) return '#';
        return row.charAt(x);
    }

    private static String pos(int x, int y) {
        return "pos-" + x + "-" + y;
    }

    private static String generateProblem(int width, int height, List<String> grid,
                                          int pusherX, int pusherY, int[][] boxes) {

        // 1) Cases atteignables par le pousseur (on ignore l'exterieur du niveau)
        boolean[][] reach = new boolean[width][height];
        Deque<int[]> q = new ArrayDeque<>();
        reach[pusherX][pusherY] = true;
        q.add(new int[]{pusherX, pusherY});
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] d : DIRS) {
                int nx = c[0] + d[0], ny = c[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
                if (reach[nx][ny] || at(grid, nx, ny) == '#') continue;
                reach[nx][ny] = true;
                q.add(new int[]{nx, ny});
            }
        }

        // 2) Cases sures pour une caisse : BFS inverse (tirer les caisses depuis les objectifs)
        boolean[][] safe = new boolean[width][height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (reach[x][y] && at(grid, x, y) == '*') {
                    safe[x][y] = true;
                    q.add(new int[]{x, y});
                }
            }
        }
        while (!q.isEmpty()) {
            int[] c = q.poll();
            for (int[] d : DIRS) {
                int bx = c[0] + d[0], by = c[1] + d[1];         // d'ou la caisse viendrait
                int px = c[0] + 2 * d[0], py = c[1] + 2 * d[1]; // ou se tiendrait le pousseur
                if (bx < 0 || by < 0 || bx >= width || by >= height) continue;
                if (!reach[bx][by] || safe[bx][by]) continue;
                if (px < 0 || py < 0 || px >= width || py >= height || !reach[px][py]) continue;
                safe[bx][by] = true;
                q.add(new int[]{bx, by});
            }
        }

        boolean[][] hasBox = new boolean[width][height];
        for (int[] b : boxes) hasBox[b[0]][b[1]] = true;

        StringBuilder sb = new StringBuilder();
        sb.append("(define (problem soko-prob)\n");
        sb.append("  (:domain sokoban)\n");

        sb.append("  (:objects\n   ");
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (reach[x][y]) sb.append(' ').append(pos(x, y));
        sb.append(" - location\n  )\n");

        sb.append("  (:init\n");
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!reach[x][y]) continue;
                for (int k = 0; k < 4; k++) {
                    int nx = x + DIRS[k][0], ny = y + DIRS[k][1];
                    if (nx < 0 || ny < 0 || nx >= width || ny >= height || !reach[nx][ny]) continue;
                    sb.append("    (adj-").append(DIR_NAMES[k]).append(' ')
                      .append(pos(x, y)).append(' ').append(pos(nx, ny)).append(")\n");
                }
            }
        }

        sb.append("    (at-robot ").append(pos(pusherX, pusherY)).append(")\n");
        for (int[] b : boxes) sb.append("    (at-box ").append(pos(b[0], b[1])).append(")\n");

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!reach[x][y]) continue;
                boolean robot = (x == pusherX && y == pusherY);
                if (!robot && !hasBox[x][y]) sb.append("    (clear ").append(pos(x, y)).append(")\n");
                if (safe[x][y]) sb.append("    (safe-for-box ").append(pos(x, y)).append(")\n");
            }
        }
        sb.append("  )\n");

        sb.append("  (:goal (and\n");
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                if (reach[x][y] && at(grid, x, y) == '*')
                    sb.append("    (at-box ").append(pos(x, y)).append(")\n");
        sb.append("  ))\n)\n");

        return sb.toString();
    }
}
