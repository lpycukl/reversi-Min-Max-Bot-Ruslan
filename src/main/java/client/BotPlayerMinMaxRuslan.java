package client;

import logic.Board;
import logic.Cell;
import logic.Move;
import logic.Player;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BotPlayerMinMaxRuslan extends Player {

    public BotPlayerMinMaxRuslan(Cell playerCell) {
        super(playerCell);
    }

    @Override
    public Move makeMove(Board board) {

        List<Move> availableMoves = board.getAllAvailableMoves(playerCell);

        if (availableMoves.size() == 1) {
            Move move = availableMoves.get(0);
            board.placePiece(move.row, move.col, playerCell);
            return move;
        }

        Move zeroMove = new Move(-1, -1);

        long time1 = System.nanoTime();
        Tree father = new Tree(zeroMove, playerCell, playerCell.reverse(), board, 0, playerCell, isCornersEmpty(board));
        long time2 = System.nanoTime();

        try (FileWriter writer = new FileWriter("time on move.txt", true)) {
            float timeLastMoves = (float) (time2 - time1) / 1000000000;
            String result = String.format("%.2f", timeLastMoves);
            if (isCornersEmpty(board) && board.getQuantityOfEmpty() > 10) System.out.print("poisk uglov: ");
            else {
                System.out.print("endspil: ");
            }
            System.out.println(result);
            writer.append(result);
            writer.append('\n');
            writer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Move move = father.getGoldMove();
        board.placePiece(move.row, move.col, playerCell);

        return move;
    }

    private boolean isCornersEmpty(Board board) {
        Cell[][] cells = board.getBoard();
        int[] angleCoordinates = {0, 7};
        for (int cord1 : angleCoordinates
        ) {
            for (int cord2 : angleCoordinates
            ) {
                if (cells[cord1][cord2].equals(Cell.EMPTY)) {
                    return true;
                }

            }

        }
        return false;

    }

    public String getPlayerID() {
        return "Bot1";
    }


    private static class Tree {
        static final int MAX_DEEP = 5;
        static final int EMPTY_LIMIT = 9;
        static final int MAX_DEEP_EMPTY_LIMIT = 8;

        static final long MAX_TIME = 50;

        int deep;
        int value = 0;
        Move move;

        Move goldMove;
        Cell whoWillMakeMove;
        Cell whoMadeMove;

        Cell fatherCell;
        List<Player.Tree> nodes = new ArrayList<>();

        Board board;

        private Tree(Move move, Cell whoWillMakeMove, Cell whoMadeMove, Board board, int deep, Cell fatherCell, boolean isFatherCornersEmpty) {
            this.deep = deep;
            this.move = move;
            this.whoWillMakeMove = whoWillMakeMove;
            this.whoMadeMove = whoMadeMove;
            this.fatherCell = fatherCell;
            this.board = board;
            int maxDeepInThisSituation = MAX_DEEP;
            long timeStart = System.nanoTime();
            if (board.getQuantityOfEmpty() < EMPTY_LIMIT) {
                maxDeepInThisSituation = MAX_DEEP_EMPTY_LIMIT;
            }


            if (deep < maxDeepInThisSituation) {
                List<Move> availableMoves = board.getAllAvailableMoves(this.whoWillMakeMove);
                for (Move thisMove : availableMoves
                ) {
                    long timeEnd = System.nanoTime();
                    if ((timeEnd - timeStart) / 100000000 > MAX_TIME) {
                        System.out.println((timeEnd - timeStart) / 100000000);
                        break;
                    }
                    Board boardAfterMove = board.placePieceAndGetCopy(thisMove.row, thisMove.col, whoWillMakeMove);
                    nodes.add(new Player.Tree(thisMove, whoWillMakeMove.reverse(), whoMadeMove.reverse(), boardAfterMove, deep + 1, fatherCell, isFatherCornersEmpty));
                }
            }
//пробегаться не по всем территориям, а по территориям незанятым у отца
            if (isFatherCornersEmpty && board.getQuantityOfEmpty() > EMPTY_LIMIT) {
                if (deep == maxDeepInThisSituation || Objects.requireNonNull(nodes).isEmpty()) {

                    Cell[][] cells = board.getBoard();
                    int winOrLose = winOrLose(board, fatherCell);
                    if (winOrLose != 0) {
                        value = winOrLose;
                    } else {

//проверяем углы
                        int[] angleCoordinates = {0, 7};
                        for (int cord1 : angleCoordinates
                        ) {
                            for (int cord2 : angleCoordinates
                            ) {
                                if (cells[cord1][cord2].equals(fatherCell)) {
                                    value = value + 25;
                                } else if (cells[cord1][cord2].equals(fatherCell.reverse())) {
                                    value = value - 25;
                                }
                            }
                        }
//проверяем то, что рядом с углами
                        int[][] closeToCornersCoordinates = {{0, 1}, {1, 0}, {1, 1}, {0, 6}, {1, 6}, {1, 7}, {6, 0}, {6, 1}, {7, 1}, {6, 6}, {6, 7}, {7, 6}};
                        for (int[] coord : closeToCornersCoordinates
                        ) {
                            if (cells[coord[0]][coord[1]].equals(fatherCell)) {
                                value = value - 15;
                            } else if (cells[coord[0]][coord[1]].equals(fatherCell.reverse())) {
                                value = value + 15;
                            }
                        }
//проверяем то, что далеко от углов
                        int[][] notCloseToCornersCoordinates = {{0, 2}, {1, 2}, {2, 2}, {2, 1}, {2, 0}, {0, 5}, {1, 5}, {2, 5}, {2, 6}, {2, 7}, {5, 0}, {5, 1}, {5, 2}, {6, 2}, {7, 2}, {5, 5}, {5, 6}, {5, 7}, {6, 5}, {7, 5}};
                        for (int[] coord : notCloseToCornersCoordinates
                        ) {
                            if (cells[coord[0]][coord[1]].equals(fatherCell)) {
                                value = value + 7;
                            } else if (cells[coord[0]][coord[1]].equals(fatherCell.reverse())) {
                                value = value - 7;
                            }
                        }
                    }
                } else {
                    if (whoWillMakeMove.equals(fatherCell)) {
                        value = 0;
                        for (Player.Tree node : nodes
                        ) {
                            if (node.getValue() >= value) {
                                value = node.getValue();
                                if (deep == 0) {
                                    goldMove = node.getMove();
                                }
                            }

                        }
                    } else {
                        value = 1000;
                        for (Player.Tree node : nodes
                        ) {
                            if (node.getValue() <= value) {
                                value = node.getValue();
                            }

                        }

                    }

                }


            } else {
                if (deep == maxDeepInThisSituation || Objects.requireNonNull(nodes).isEmpty()) {
                    int winOrLose = winOrLose(board, fatherCell);
                    if (winOrLose != 0) {
                        value = winOrLose;
                    } else {
                        if (fatherCell.equals(Cell.WHITE)) {
                            value = board.getQuantityOfWhite();
                        }
                        if (fatherCell.equals(Cell.BLACK)) {
                            value = board.getQuantityOfBlack();
                        }
                    }
                } else {
                    if (whoWillMakeMove.equals(fatherCell)) {
                        value = -5000;
                        for (Player.Tree node : nodes
                        ) {
                            if (node.getValue() >= value) {
                                value = node.getValue();
                                if (deep == 0) {
                                    goldMove = node.getMove();
                                }
                            }

                        }
                    } else {
                        value = 5000;
                        for (Player.Tree node : nodes
                        ) {
                            if (node.getValue() <= value) {
                                value = node.getValue();
                            }

                        }

                    }

                }

            }
        }

        private int getValue() {
            return value;
        }

        private Move getGoldMove() {
            return goldMove;
        }

        private Move getMove() {
            return move;
        }

        private int winOrLose(Board board, Cell cell) {
            if (!board.getAllAvailableMoves(Cell.BLACK).isEmpty() || !board.getAllAvailableMoves(Cell.WHITE).isEmpty()) {
                return 0;
            }
            if (board.getQuantityOfBlack() > board.getQuantityOfWhite()) {
                if (cell.equals(Cell.BLACK)) return 5000;
                if (cell.equals(Cell.WHITE)) return -5000;
            }
            if (board.getQuantityOfBlack() < board.getQuantityOfWhite()) {
                if (cell.equals(Cell.BLACK)) return -5000;
                if (cell.equals(Cell.WHITE)) return 5000;
            }
            if (board.getQuantityOfBlack() == board.getQuantityOfWhite()) {
                return -2000;
            }
            return 0;
        }


    }
}



