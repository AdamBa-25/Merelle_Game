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

public class Merelle {

    /**
     * Contient la configuration choisie par les joueurs avant une partie.
     * Regrouper ces valeurs dans un objet permet de les retourner depuis
     * configureGame() et de les réutiliser à chaque rejeu.
     */
    private static class GameConfig {
        int mode;
        int colorJ1;
        int colorJ2;
    }

    public static void main(String[] args) {
        Logger.setLevel(Logger.LOGGER_NONE);

        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("=== La Merelle ===");
        System.out.println();
        System.out.println("Coordonnees : ligne A-G (haut vers bas) + colonne 1-7 (gauche vers droite)");
        System.out.println("Positions valides : A1 A4 A7 | B2 B4 B6 | C3 C4 C5 | D1 D2 D3 D5 D6 D7 | etc.");
        System.out.println("Commandes speciales : \"stop\" = quitter le programme | \"return\" = abandonner la partie en cours");

        // Enregistrement du stage une seule fois, avant toute boucle de rejeu
        StageFactory.registerModelAndView("merelle", "model.MerelleStageModel", "view.MerelleStageView");

        // Mode passé en argument de ligne de commande (optionnel)
        Integer forcedMode = null;
        if (args.length >= 1) {
            int parsed = parseMode(args[0]);
            if (parsed < 0) {
                System.err.println("Mode invalide : '" + args[0] + "' (doit etre 0, 1 ou 2)");
                System.err.println("0 = Humain vs Humain | 1 = Humain vs IA | 2 = IA vs IA");
                System.exit(1);
            }
            forcedMode = parsed;
        }

        java.util.Random rng = new java.util.Random();
        boolean playAgain = true;

        while (playAgain) {
            System.out.println();

            // Configuration complète à chaque nouvelle partie
            GameConfig cfg = configureGame(br, forcedMode);

            MerelleStageFactory.setColors(cfg.colorJ1, cfg.colorJ2);

            // Crée un nouveau modele et de nouveaux joueurs a chaque partie
            Model model = new Model();
            if (cfg.mode == 0) {
                model.addHumanPlayer("Joueur 1");
                model.addHumanPlayer("Joueur 2");
            } else if (cfg.mode == 1) {
                model.addHumanPlayer("Joueur 1");
                model.addComputerPlayer("Ordinateur");
            } else {
                model.addComputerPlayer("Ordi-1");
                model.addComputerPlayer("Ordi-2");
            }

            View view = new View(model);
            MerelleController control = new MerelleController(model, view);
            control.setFirstStageName("merelle");

            // Tirage au sort du joueur qui commence
            int startingPlayer = rng.nextInt(2);
            System.out.println();
            System.out.println("Tirage au sort : c'est "
                    + model.getPlayers().get(startingPlayer).getName()
                    + " qui commence !");
            model.setIdPlayer(startingPlayer);
            System.out.println();

            GameException error = startGame(control);
            if (error != null) {
                System.out.println("Impossible de demarrer le jeu : " + error.getMessage());
                System.exit(1);
            }
            control.stageLoop();

            // Propose de rejouer (sauf IA vs IA ou EOF)
            playAgain = askPlayAgain(br, cfg.mode);
        }

        System.out.println("Merci d'avoir joue ! A bientot.");
    }

    // =========================================================================
    // Configuration d'une partie (mode, difficulte, couleurs)
    // =========================================================================

    /**
     * Demande aux joueurs de configurer une nouvelle partie :
     * mode de jeu, difficulte IA (si applicable), couleurs.
     * Si forcedMode != null, le mode n'est pas redemande.
     *
     * @param br         lecteur sur l'entree standard
     * @param forcedMode mode impose par argument CLI, ou null pour le demander
     * @return la configuration choisie
     */
    private static GameConfig configureGame(BufferedReader br, Integer forcedMode) {
        GameConfig cfg = new GameConfig();

        // Choix du mode
        if (forcedMode != null) {
            cfg.mode = forcedMode;
        } else {
            System.out.println("Choisissez un mode de jeu :");
            System.out.println("0 = Humain vs Humain");
            System.out.println("1 = Humain vs IA");
            System.out.println("2 = IA vs IA");
            cfg.mode = readIntInRange(br, 0, 2);
        }

        // Difficulte IA
        if (cfg.mode == 1 || cfg.mode == 2) {
            int difficulty = chooseDifficulty(br);
            MerelleDecider.aiDifficulty = difficulty;
        }

        // Couleurs
        if (cfg.mode == 0) {
            System.out.println("Mode : Humain vs Humain");
            MerellePawn.printColorMenu();
            cfg.colorJ1 = chooseColor(br, "Joueur 1, choisissez votre couleur : ", -1);
            cfg.colorJ2 = chooseColor(br, "Joueur 2, choisissez votre couleur : ", cfg.colorJ1);
        } else if (cfg.mode == 1) {
            System.out.println("Mode : Humain vs IA");
            MerellePawn.printColorMenu();
            cfg.colorJ1 = chooseColor(br, "Joueur 1, choisissez votre couleur : ", -1);
            cfg.colorJ2 = pickColorForIA(cfg.colorJ1);
            System.out.println("L'IA jouera avec la couleur : " + MerellePawn.getColorName(cfg.colorJ2));
        } else {
            System.out.println("Mode : IA vs IA");
            cfg.colorJ1 = MerellePawn.PAWN_BLACK;
            cfg.colorJ2 = MerellePawn.PAWN_RED;
            System.out.println("IA 1 : " + MerellePawn.getColorName(cfg.colorJ1)
                    + " | IA 2 : " + MerellePawn.getColorName(cfg.colorJ2));
        }

        return cfg;
    }

    // =========================================================================
    // Methodes utilitaires
    // =========================================================================

    /**
     * Demande si les joueurs veulent faire une nouvelle partie.
     * En mode IA vs IA, retourne false directement (pas d'interaction).
     * En cas d'EOF, retourne false proprement.
     */
    private static boolean askPlayAgain(BufferedReader br, int mode) {
        if (mode == 2) return false;
        System.out.println();
        System.out.print("Nouvelle partie ? (o/n) > ");
        String line = readLine(br);
        if (line == null) return false;
        return line.trim().toLowerCase().startsWith("o");
    }

    /**
     * Lit un entier entre min et max inclus depuis l'entree standard.
     * Reboucle tant que la saisie est invalide. EOF retourne min.
     */
    private static int readIntInRange(BufferedReader br, int min, int max) {
        while (true) {
            System.out.print("> ");
            String line = readLine(br);
            if (line == null) return min; // EOF -> valeur par defaut
            int val = parseIntOrMinus1(line.trim());
            if (val >= min && val <= max) return val;
            System.out.println("Choix invalide (entrez un nombre entre " + min + " et " + max + ").");
        }
    }

    /**
     * Parse le mode de jeu depuis une chaine.
     * Retourne -1 si invalide ou hors plage 0-2.
     */
    private static int parseMode(String s) {
        if (s == null || s.isEmpty()) return -1;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return -1;
        }
        int val = Integer.parseInt(s);
        if (val < 0 || val > 2) return -1;
        return val;
    }

    /**
     * Lance la partie et retourne l'exception si elle echoue, null sinon.
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
     * Demande a l'utilisateur de choisir une couleur.
     * Refuse la couleur "forbidden" (deja prise par l'autre joueur).
     * En cas d'EOF, retourne une couleur par defaut.
     */
    private static int chooseColor(BufferedReader br, String prompt, int forbidden) {
        while (true) {
            System.out.print(prompt);
            String line = readLine(br);
            if (line == null) {
                return (forbidden == MerellePawn.PAWN_BLACK) ? MerellePawn.PAWN_RED : MerellePawn.PAWN_BLACK;
            }
            int choice = parseIntOrMinus1(line.trim());
            if (choice == -1) {
                System.out.println("Entrez un chiffre valide.");
                continue;
            }
            if (!MerellePawn.isValidColor(choice)) {
                System.out.println("Choix invalide (0 a " + (MerellePawn.NB_COLORS - 1) + ").");
                continue;
            }
            if (choice == forbidden) {
                System.out.println("Cette couleur est deja prise, choisissez-en une autre.");
                continue;
            }
            return choice;
        }
    }

    /**
     * Lit une ligne depuis le BufferedReader.
     * Retourne null en cas d'erreur ou de fin de fichier.
     */
    private static String readLine(BufferedReader br) {
        try {
            return br.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Parse un entier depuis une chaine.
     * Retourne -1 si invalide.
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
     * differente de celle du joueur humain.
     */
    private static int pickColorForIA(int humanColor) {
        for (int i = 0; i < MerellePawn.NB_COLORS; i++) {
            if (i != humanColor) return i;
        }
        return MerellePawn.PAWN_RED;
    }

    /**
     * Affiche le menu de difficulte de l'IA.
     */
    private static void printDifficultyMenu() {
        System.out.println("Choisissez la difficulte de l'IA :");
        System.out.println("1 - MiniMax");
        System.out.println("2 - Alpha-Beta");
        System.out.println("3 - MCTS (Monte Carlo Tree Search)");
    }

    /**
     * Demande a l'utilisateur de choisir la difficulte de l'IA.
     */
    private static int chooseDifficulty(BufferedReader br) {
        printDifficultyMenu();
        while (true) {
            System.out.print("> ");
            String line = readLine(br);
            if (line == null) return MerelleDecider.DIFFICULTY_MINIMAX; // EOF -> defaut
            int val = parseIntOrMinus1(line.trim());
            if (val >= 1 && val <= 3) {
                String[] names = { "", "MiniMax", "Alpha-Beta", "Monte Carlo" };
                System.out.println("Difficulte choisie : " + names[val]);
                return val;
            }
            System.out.println("Choix invalide (entrez 1, 2 ou 3).");
        }
    }
}
