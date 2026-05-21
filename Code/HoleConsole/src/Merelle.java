import boardifier.control.Logger;
import boardifier.control.StageFactory;
import boardifier.model.GameException;
import boardifier.model.Model;
import boardifier.view.View;
import control.MerelleController;
import control.MerelleDecider;
import model.MerellePawn;
import model.MerelleStageFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Merelle {

    public static void main(String[] args)
    {
        Logger.setLevel(Logger.LOGGER_NONE);

        // --- Lecture du mode de jeu ---
        Scanner sc = new Scanner(System.in);
        int mode;

        if (args.length >= 1)
        {
            mode = parseMode(args[0]);

            if (mode < 0)
            {
                System.err.println("Mode invalide : '" + args[0] + "' (doit être 0, 1 ou 2)");
                System.err.println("0 = Humain vs Humain");
                System.err.println("1 = Humain vs IA");
                System.err.println("2 = IA vs IA");
                System.exit(1);
            }
        }
        else
        {
            System.out.println("Choisissez un mode de jeu :");
            System.out.println("0 = Humain vs Humain");
            System.out.println("1 = Humain vs IA");
            System.out.println("2 = IA vs IA");
            System.out.print("> ");

            mode = sc.nextInt();

            if (mode < 0 || mode > 2)
            {
                System.err.println("Mode invalide.");
                System.exit(1);
            }
        }

        System.out.println("La Merelle");
        System.out.println();

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        // --- Choix des couleurs selon le mode ---
        int colorJ1;
        int colorJ2;

        // --- Choix des difficulté pour l'IA ---
        if (mode == 1 || mode == 2) {
            int difficulty = chooseDifficulty(br);
            MerelleDecider.aiDifficulty = difficulty;
        }

        if (mode == 0) {
            System.out.println("Mode : Humain vs Humain");
            MerellePawn.printColorMenu();
            colorJ1 = chooseColor(br, "Joueur 1, choisissez votre couleur : ", -1);
            colorJ2 = chooseColor(br, "Joueur 2, choisissez votre couleur : ", colorJ1);

        } else if (mode == 1) {
            System.out.println("Mode : Humain vs IA");
            MerellePawn.printColorMenu();
            colorJ1 = chooseColor(br, "Joueur 1, choisissez votre couleur : ", -1);
            colorJ2 = pickColorForIA(colorJ1);
            System.out.println("L'IA jouera avec la couleur : " + MerellePawn.getColorName(colorJ2));

        } else {
            System.out.println("Mode : IA vs IA");
            colorJ1 = MerellePawn.PAWN_BLACK;
            colorJ2 = MerellePawn.PAWN_RED;
            System.out.println("IA 1 : " + MerellePawn.getColorName(colorJ1)
                    + " | IA 2 : " + MerellePawn.getColorName(colorJ2));
        }

        // Transmet les couleurs à la factory avant la création du stage
        MerelleStageFactory.setColors(colorJ1, colorJ2);

        // --- Création du modèle et ajout des joueurs ---
        Model model = new Model();
        if (mode == 0) {
            model.addHumanPlayer("Joueur 1");
            model.addHumanPlayer("Joueur 2");
        } else if (mode == 1) {
            model.addHumanPlayer("Joueur 1");
            model.addComputerPlayer("Ordinateur");
        } else {
            model.addComputerPlayer("Ordi-1");
            model.addComputerPlayer("Ordi-2");
        }

        // --- Enregistrement du stage ---
        StageFactory.registerModelAndView( "merelle", "model.MerelleStageModel", "view.MerelleStageView" );

        // --- Création de la vue et du contrôleur ---
        View view = new View(model);
        MerelleController control = new MerelleController(model, view);
        control.setFirstStageName("merelle");

        // --- Lancement ---
        System.out.println();
        System.out.println("Coordonnées : ligne A-G (haut→bas) + colonne 1-7 (gauche→droite)");
        System.out.println("Positions valides : A1 A4 A7 | B2 B4 B6 | C3 C4 C5 | D1 D2 D3 D5 D6 D7 | etc.");
        System.out.println();

        // --- Joueur qui commence selon le mode ---
        int startingPlayer = new java.util.Random().nextInt(2);
        System.out.println("Tirage au sort : c'est "
                    + model.getPlayers().get(startingPlayer).getName()
                    + " qui commence !");
        model.setIdPlayer(startingPlayer);
        System.out.println();

        GameException error = startGame(control);
        if (error != null)
        {
            System.out.println("Impossible de démarrer le jeu : " + error.getMessage());
            System.exit(1);
        }
        control.stageLoop();
    }

    // MÉTHODES UTILITAIRES

    /**
     * Parse le mode de jeu depuis une chaîne.
     * Retourne -1 si la chaîne n'est pas un entier valide ou hors plage 0-2.
     */
    private static int parseMode(String s) {
        if (s == null || s.isEmpty()) return -1;
        // Vérifie que tous les caractères sont des chiffres (et éventuellement un signe -)
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (!Character.isDigit(c)) return -1;
        }
        int val = Integer.parseInt(s);
        if (val < 0 || val > 2) return -1;
        return val;
    }

    /**
     * Lance la partie et retourne l'exception si elle échoue, null sinon.
     * Permet d'éviter un try/catch dans main().
     */
    private static GameException startGame(MerelleController control) {
        try {
            control.startGame();
            return null;
        } catch (GameException e) {
            return e;
        }
    }

    /**
     * Demande à l'utilisateur de choisir une couleur.
     * Refuse la couleur "forbidden" (déjà prise par l'autre joueur).
     * En cas d'EOF (fichier d'entrée), retourne une couleur par défaut.
     */
    private static int chooseColor(BufferedReader br, String prompt, int forbidden) {
        int choice = -1;
        boolean valid = false;

        while (!valid) {
            System.out.print(prompt);

            String line = readLine(br);

            // EOF ou erreur de lecture → couleur par défaut
            if (line == null) {
                choice = (forbidden == MerellePawn.PAWN_BLACK) ? MerellePawn.PAWN_RED : MerellePawn.PAWN_BLACK;
                valid = true;
                continue;
            }

            line = line.trim();
            choice = parseIntOrMinus1(line);

            if (choice == -1) {
                System.out.println("Entrez un chiffre valide.");
                continue;
            }
            if (!MerellePawn.isValidColor(choice)) {
                System.out.println("Choix invalide (0 à " + (MerellePawn.NB_COLORS - 1) + ").");
                continue;
            }
            if (choice == forbidden) {
                System.out.println("Cette couleur est déjà prise, choisissez-en une autre.");
                continue;
            }
            valid = true;
        }
        return choice;
    }

    /**
     * Lit une ligne depuis le BufferedReader.
     * Retourne null en cas d'erreur ou de fin de fichier, sans lever d'exception.
     */
    private static String readLine(BufferedReader br) {
        try {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parse un entier depuis une chaîne.
     * Retourne -1 si la chaîne ne représente pas un entier, sans lever d'exception.
     */
    private static int parseIntOrMinus1(String s) {
        if (s == null || s.isEmpty()) return -1;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return -1;
        }
        return Integer.parseInt(s);
    }

    /**
     * Choisit automatiquement une couleur pour l'IA,
     * différente de celle du joueur humain.
     */
    private static int pickColorForIA(int humanColor) {
        for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
            if (i != humanColor) return i;
        }
        return MerellePawn.PAWN_RED;
    }

    /**
     * Affiche le menu de difficulté de l'IA.
     */
    private static void printDifficultyMenu() {
        System.out.println("Choisissez la difficulté de l'IA :");
        System.out.println("1 - MiniMax");
        System.out.println("2 - Alpha-Beta");
        System.out.println("3 - MCTS (Monte Carlo Tree Search)");
    }

    /**
     * Demande à l'utilisateur de choisir la difficulté de l'IA.
     * Retourne la constante correspondante de MerelleDecider.
     */
    private static int chooseDifficulty(BufferedReader br) {
        printDifficultyMenu();

        int choice = -1;
        boolean valid = false;

        while (!valid) {
            System.out.print("> ");
            String line = readLine(br);

            // EOF → difficulté par défaut
            if (line == null) {
                choice = MerelleDecider.DIFFICULTY_MINIMAX;
                valid = true;
                continue;
            }

            line = line.trim();
            int val = parseIntOrMinus1(line);

            if (val < 1 || val > 3) {
                System.out.println("Choix invalide (entrez 1, 2 ou 3).");
                continue;
            }

            choice = val;
            valid = true;
        }

        String[] names = { "", "MiniMax", "Alpha-Beta", "Monte Carlo" };
        System.out.println("Difficulté choisie : " + names[choice]);
        return choice;
    }
}
