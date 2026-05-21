package model;

import boardifier.model.ContainerElement;
import boardifier.model.GameElement;
import boardifier.model.GameStageModel;

/**
 * Plateau du jeu de la Mérelle.
 *
 * Le plateau réel est un graphe de 24 nœuds (positions où poser un pion),
 * mais boardifier ne gère que des grilles rectangulaires. On mappe donc les
 * 24 positions sur une grille interne 7x7 : les 25 cases restantes sont vides.
 *
 * Système de coordonnées utilisateur : lettre A-G (ligne) + chiffre 1-7 (colonne).
 *
 * Visualisation du plateau et correspondance coordonnée <-> position logique :
 *
 *        col: 1      2      3      4      5      6      7
 *   lig A(0): A1=0   .      .     D1=1   .      .     G1=2
 *   lig B(1): .      B2=3   .     D2=4   .     F2=5    .
 *   lig C(2): .      .     C3=6  D3=7  E3=8    .       .
 *   lig D(3): A4=9  B4=10 C4=11   .   E4=12 F4=13  G4=14
 *   lig E(4): .      .    C5=15  D5=16 E5=17   .       .
 *   lig F(5): .     B6=18   .    D6=19   .    F6=20    .
 *   lig G(6): A7=21  .      .    D7=22   .      .    G7=23
 *
 * Moulins (exemples) :
 *   A1-D1-G1 = {0,1,2}   A1-A4-A7 = {0,9,21}   B2-D2-F2 = {3,4,5}
 */
public class MerelleBoard extends ContainerElement {

    /**
     * Table principale : position logique (0-23) -> [row, col] dans la grille 7x7.
     *
     * Chaque ligne = une position valide du plateau avec ses coordonnées grille.
     * Utilisée pour lire/écrire dans la grille boardifier et pour convertir
     * une coordonnée utilisateur en case de la grille.
     *
     * Équivalences complètes :
     *   pos  0 = A1 = grille[0][0]    pos  1 = D1 = grille[0][3]    pos  2 = G1 = grille[0][6]
     *   pos  3 = B2 = grille[1][1]    pos  4 = D2 = grille[1][3]    pos  5 = F2 = grille[1][5]
     *   pos  6 = C3 = grille[2][2]    pos  7 = D3 = grille[2][3]    pos  8 = E3 = grille[2][4]
     *   pos  9 = A4 = grille[3][0]    pos 10 = B4 = grille[3][1]    pos 11 = C4 = grille[3][2]
     *   pos 12 = E4 = grille[3][4]    pos 13 = F4 = grille[3][5]    pos 14 = G4 = grille[3][6]
     *   pos 15 = C5 = grille[4][2]    pos 16 = D5 = grille[4][3]    pos 17 = E5 = grille[4][4]
     *   pos 18 = B6 = grille[5][1]    pos 19 = D6 = grille[5][3]    pos 20 = F6 = grille[5][5]
     *   pos 21 = A7 = grille[6][0]    pos 22 = D7 = grille[6][3]    pos 23 = G7 = grille[6][6]
     */
    public static final int[][] POS_TO_GRID = {
        {0,0},{0,3},{0,6},   // pos  0= A1,  1= D1,  2= G1  (carré extérieur haut)
        {1,1},{1,3},{1,5},   // pos  3= B2,  4= D2,  5= F2  (carré moyen haut)
        {2,2},{2,3},{2,4},   // pos  6= C3,  7= D3,  8= E3  (carré intérieur haut)
        {3,0},{3,1},{3,2},   // pos  9= A4, 10= B4, 11= C4  (milieu gauche des 3 carrés)
        {3,4},{3,5},{3,6},   // pos 12= E4, 13= F4, 14= G4  (milieu droite des 3 carrés)
        {4,2},{4,3},{4,4},   // pos 15= C5, 16= D5, 17= E5  (carré intérieur bas)
        {5,1},{5,3},{5,5},   // pos 18= B6, 19= D6, 20= F6  (carré moyen bas)
        {6,0},{6,3},{6,6}    // pos 21= A7, 22= D7, 23= G7  (carré extérieur bas)
    };

    /**
     * Les 16 moulins possibles, chacun défini par 3 positions logiques.
     * Un moulin est formé quand les 3 positions appartiennent toutes au même joueur.
     *
     * Carré extérieur (4 côtés) :
     *   {0,1,2}   = A1-D1-G1  (côté haut)
     *   {0,9,21}  = A1-A4-A7  (côté gauche)
     *   {2,14,23} = G1-G4-G7  (côté droit)
     *   {21,22,23}= A7-D7-G7  (côté bas)
     * Carré moyen (4 côtés) :
     *   {3,4,5}   = B2-D2-F2  (côté haut)
     *   {3,10,18} = B2-B4-B6  (côté gauche)
     *   {5,13,20} = F2-F4-F6  (côté droit)
     *   {18,19,20}= B6-D6-F6  (côté bas)
     * Carré intérieur (4 côtés) :
     *   {6,7,8}   = C3-D3-E3  (côté haut)
     *   {6,11,15} = C3-C4-C5  (côté gauche)
     *   {8,12,17} = E3-E4-E5  (côté droit)
     *   {15,16,17}= C5-D5-E5  (côté bas)
     * Liaisons verticales entre carrés (4 lignes centrales) :
     *   {1,4,7}   = D1-D2-D3  (centre haut)
     *   {9,10,11} = A4-B4-C4  (centre gauche)
     *   {12,13,14}= E4-F4-G4  (centre droite)
     *   {16,19,22}= D5-D6-D7  (centre bas)
     */
    public static final int[][] MILLS = {
        {0,1,2}, {0,9,21}, {2,14,23}, {21,22,23},   // carré extérieur
        {3,4,5}, {3,10,18}, {5,13,20}, {18,19,20},   // carré moyen
        {6,7,8}, {6,11,15}, {8,12,17}, {15,16,17},   // carré intérieur
        {1,4,7}, {9,10,11}, {12,13,14}, {16,19,22}   // liaisons entre carrés
    };

    /**
     * Adjacences : pour chaque position logique, les positions directement
     * accessibles par un déplacement (reliées par une ligne du plateau).
     * Utilisé pour valider les déplacements en phase 2 et détecter les blocages.
     *
     *   pos  0 (A1) : D1(1), A4(9)
     *   pos  1 (D1) : A1(0), G1(2), D2(4)
     *   pos  2 (G1) : D1(1), G4(14)
     *   pos  3 (B2) : D2(4), B4(10)
     *   pos  4 (D2) : D1(1), B2(3), F2(5), D3(7)
     *   pos  5 (F2) : D2(4), F4(13)
     *   pos  6 (C3) : D3(7), C4(11)
     *   pos  7 (D3) : D2(4), C3(6), E3(8)
     *   pos  8 (E3) : D3(7), E4(12)
     *   pos  9 (A4) : A1(0), B4(10), A7(21)
     *   pos 10 (B4) : B2(3), A4(9), C4(11), B6(18)
     *   pos 11 (C4) : C3(6), B4(10), C5(15)
     *   pos 12 (E4) : E3(8), F4(13), E5(17)
     *   pos 13 (F4) : F2(5), E4(12), G4(14), F6(20)
     *   pos 14 (G4) : G1(2), F4(13), G7(23)
     *   pos 15 (C5) : C4(11), D5(16)
     *   pos 16 (D5) : C5(15), E5(17), D6(19)
     *   pos 17 (E5) : E4(12), D5(16)
     *   pos 18 (B6) : B4(10), D6(19)
     *   pos 19 (D6) : D5(16), B6(18), F6(20), D7(22)
     *   pos 20 (F6) : F4(13), D6(19)
     *   pos 21 (A7) : A4(9), D7(22)
     *   pos 22 (D7) : D6(19), A7(21), G7(23)
     *   pos 23 (G7) : G4(14), D7(22)
     */
    public static final int[][] ADJACENCY = {
        {1,9},         // 0  = A1
        {0,2,4},       // 1  = D1
        {1,14},        // 2  = G1
        {4,10},        // 3  = B2
        {1,3,5,7},     // 4  = D2
        {4,13},        // 5  = F2
        {7,11},        // 6  = C3
        {4,6,8},       // 7  = D3
        {7,12},        // 8  = E3
        {0,10,21},     // 9  = A4
        {3,9,11,18},   // 10 = B4
        {6,10,15},     // 11 = C4
        {8,13,17},     // 12 = E4
        {5,12,14,20},  // 13 = F4
        {2,13,23},     // 14 = G4
        {11,16},       // 15 = C5
        {15,17,19},    // 16 = D5
        {12,16},       // 17 = E5
        {10,19},       // 18 = B6
        {16,18,20,22}, // 19 = D6
        {13,19},       // 20 = F6
        {9,22},        // 21 = A7
        {19,21,23},    // 22 = D7
        {14,22}        // 23 = G7
    };

    /**
     * Table inverse : gridToPos[row][col] -> position logique (-1 si case invalide).
     * Construite une seule fois dans buildGridToPos() à partir de POS_TO_GRID.
     * Utilisée par getLogicalPos() pour convertir des coordonnées grille en position.
     */
    private int[][] gridToPos;

    /**
     * Crée le plateau de la Mérelle.
     * Appelle le constructeur de ContainerElement avec une grille 7x7,
     * puis construit la table inverse gridToPos.
     *
     * @param x              position x du plateau dans l'espace virtuel boardifier
     * @param y              position y du plateau dans l'espace virtuel boardifier
     * @param gameStageModel le stage propriétaire de cet élément
     */
    public MerelleBoard(int x, int y, GameStageModel gameStageModel) {
        super("merelleboard", x, y, 7, 7, gameStageModel);
        buildGridToPos();
    }

    /**
     * Construit la table inverse : part de -1 partout, puis pour chaque position
     * logique (0-23) inscrit son index dans la case grille correspondante.
     */
    private void buildGridToPos() {
        gridToPos = new int[7][7];
        for (int r = 0; r < 7; r++)
            for (int c = 0; c < 7; c++)
                gridToPos[r][c] = -1; // case invalide par défaut
        for (int pos = 0; pos < 24; pos++)
            gridToPos[POS_TO_GRID[pos][0]][POS_TO_GRID[pos][1]] = pos;
    }

    /**
     * Retourne la position logique (0-23) pour une case de la grille,
     * ou -1 si la case n'est pas une position valide du plateau.
     * Ex : getLogicalPos(0,0) = 0 (A1), getLogicalPos(0,1) = -1 (case vide).
     */
    public int getLogicalPos(int row, int col) {
        if (row < 0 || row >= 7 || col < 0 || col >= 7) return -1;
        return gridToPos[row][col];
    }

    /**
     * Retourne le pion à la position logique donnée, ou null si vide.
     * Lit la case grille via POS_TO_GRID puis caste le GameElement en MerellePawn.
     */
    public MerellePawn getPawnAt(int pos) {
        if (pos < 0 || pos >= 24) return null;
        GameElement el = getElement(POS_TO_GRID[pos][0], POS_TO_GRID[pos][1]);
        return (el instanceof MerellePawn) ? (MerellePawn) el : null;
    }

    /**
     * Place un pion à la position logique donnée.
     * Traduit la position en [row,col] grille et appelle addElement() de boardifier.
     */
    public void placePawnAt(MerellePawn pawn, int pos) {
        addElement(pawn, POS_TO_GRID[pos][0], POS_TO_GRID[pos][1]);
    }

    /**
     * Retire le pion à la position logique donnée.
     * Le pion est retiré de la grille (container = null) mais reste dans le stage.
     */
    public void removePawnAt(int pos) {
        MerellePawn pawn = getPawnAt(pos);
        if (pawn != null) removeElement(pawn);
    }

    /** Retourne true si aucun pion n'occupe la position logique donnée. */
    public boolean isFreeAt(int pos) {
        return getPawnAt(pos) == null;
    }

    /** Retourne true si pos est un index de position valide (0 à 23). */
    public boolean isValidPos(int pos) {
        return pos >= 0 && pos < 24;
    }

    /**
     * Retourne true si src et dest sont reliés par une ligne du plateau.
     * Parcourt ADJACENCY[src] et cherche dest dedans.
     * Utilisé pour valider un déplacement en phase 2.
     */
    public boolean isAdjacent(int src, int dest) {
        if (!isValidPos(src) || !isValidPos(dest)) return false;
        for (int adj : ADJACENCY[src])
            if (adj == dest) return true;
        return false;
    }

    /**
     * Retourne true si la position pos fait partie d'un moulin complet de la couleur playerColor.
     *
     * IMPORTANT : playerColor est une couleur de pion (MerellePawn.PAWN_BLACK, PAWN_RED, etc.),
     * pas un index de joueur (0 ou 1). Les deux coïncident quand on joue Noir/Rouge par défaut,
     * mais divergent dès que les joueurs choisissent d'autres couleurs.
     *
     * @param pos         position logique (0-23) à vérifier
     * @param playerColor couleur du joueur (constante MerellePawn.PAWN_*)
     */
    public boolean isInMill(int pos, int playerColor) {
        for (int[] mill : MILLS) {
            // Vérifie si pos appartient à ce moulin
            boolean posInMill = false;
            for (int p : mill) if (p == pos) { posInMill = true; break; }
            if (!posInMill) continue;
            // Vérifie si les 3 cases sont toutes occupées par la même couleur
            boolean allSame = true;
            for (int p : mill) {
                MerellePawn pw = getPawnAt(p);
                if (pw == null || pw.getColor() != playerColor) { allSame = false; break; }
            }
            if (allSame) return true;
        }
        return false;
    }

    // checkMillFormed() supprimé : c'était un alias exact d'isInMill().
    // Utiliser directement isInMill(pos, playerColor) dans le contrôleur.

    /**
     * Compte et retourne le nombre de pions de la couleur playerColor sur le plateau.
     * Utilisé pour détecter la condition de défaite (moins de 3 pions).
     *
     * @param playerColor couleur du joueur (constante MerellePawn.PAWN_*)
     */
    public int countPawns(int playerColor) {
        int count = 0;
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = getPawnAt(pos);
            if (pw != null && pw.getColor() == playerColor) count++;
        }
        return count;
    }

    /**
     * Retourne true si le joueur de couleur playerColor ne peut plus bouger aucun pion.
     * Pour chaque pion de cette couleur, vérifie si au moins une case adjacente est libre.
     * Dès qu'une case libre est trouvée, retourne false (pas bloqué).
     * Retourne false si le joueur n'a aucun pion : la fin de partie est alors gérée
     * par la condition "moins de 3 pions", pas par le blocage.
     *
     * @param playerColor couleur du joueur (constante MerellePawn.PAWN_*)
     */
    public boolean isBlocked(int playerColor) {
        boolean hasPawn = false;
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = getPawnAt(pos);
            if (pw != null && pw.getColor() == playerColor) {
                hasPawn = true;
                for (int adj : ADJACENCY[pos])
                    if (isFreeAt(adj)) return false; // ce pion peut bouger → pas bloqué
            }
        }
        // Aucun pion sur le plateau → défaite par "moins de 3 pions", pas par blocage
        if (!hasPawn) return false;
        return true; // des pions existent mais aucun ne peut bouger
    }

    /**
     * Retourne true si tous les pions de la couleur playerColor sont dans des moulins.
     * Cas spécial : si c'est vrai, les règles autorisent à capturer un pion en moulin.
     * Retourne false si le joueur n'a aucun pion (pas de pions = pas "tous en moulin").
     *
     * @param playerColor couleur du joueur (constante MerellePawn.PAWN_*)
     */
    public boolean allPawnsInMills(int playerColor) {
        boolean found = false;
        for (int pos = 0; pos < 24; pos++) {
            MerellePawn pw = getPawnAt(pos);
            if (pw != null && pw.getColor() == playerColor) {
                found = true;
                // Si un pion de cette couleur est hors moulin → pas tous en moulin
                if (!isInMill(pos, playerColor)) return false;
            }
        }
        // Si aucun pion trouvé, retourner false (cas dégénéré, ne devrait pas arriver en jeu normal)
        return found;
    }

    /**
     * Retourne le premier moulin complet de playerColor contenant la position pos,
     * sous forme d'un tableau de 3 positions logiques.
     * Retourne null si pos ne fait partie d'aucun moulin complet de cette couleur.
     * Utilisé par le contrôleur pour mémoriser le moulin formé (règle "même moulin interdit 2 tours").
     *
     * @param pos         position logique (0-23) à vérifier
     * @param playerColor couleur du joueur (constante MerellePawn.PAWN_*)
     * @return tableau de 3 positions ou null
     */
    public int[] getMillContaining(int pos, int playerColor) {
        for (int[] mill : MILLS) {
            boolean posInMill = false;
            for (int p : mill) if (p == pos) { posInMill = true; break; }
            if (!posInMill) continue;
            boolean allSame = true;
            for (int p : mill) {
                MerellePawn pw = getPawnAt(p);
                if (pw == null || pw.getColor() != playerColor) { allSame = false; break; }
            }
            if (allSame) return mill.clone();
        }
        return null;
    }
}
