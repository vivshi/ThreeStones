package threestonesclient;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Scanner;
import threestonesdatacomm.ThreeStonesPacket;

/**
 * Class that creates a Client to enter a session of ThreeStones with a server.
 *
 * @authors Philippe Langlois-Pedroso and Kevin Bui
 * @version 1.0
 */
public class ThreeStonesClient {

    public static final int PORTNUMBER = 50000;
    private final String address;
    private Socket socket;
    private Scanner reader;
    private boolean playAgain = true;
    private InputStream in;
    private OutputStream out;
    private String answer;
    private Cell[][] board;
    private int playerScore = 0;
    private int compScore = 0;
    private int row;
    private int column;
    private int playerWins = 0;
    private int computerWins = 0;
    private int stonesLeft;
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    
    public enum Cell {
        BLOCK, EMPTY, WHITE, BLACK
    };

    /**
     * Default constructor that takes a string to represent the server's IP
     * address.
     *
     * @param address
     */
    public ThreeStonesClient(String address) {
        this.address = address;
    }

    /**
     * Method that will try to create a connection with the ThreeStones server.
     */
    public void makeConnection() {
        try {
            // Instantiate the socket object with it's streams.
            socket = new Socket(address, PORTNUMBER);
            in = socket.getInputStream();
            out = socket.getOutputStream();
            System.out.println("Connected to server.");
            playSession();
        } catch (IOException e) {
            System.out.println("There was a problem connectiong to the server");
        }
    }

    /**
     * Method that will attempt to initiate a session with the server, depending
     * on if the client would like to start a session.
     *
     * @throws IOException
     */
    public void playSession() throws IOException {
        reader = new Scanner(System.in);
        byte[] values = new byte[5];
        while (playAgain) {
            ThreeStonesPacket packet;
            System.out.println("You have won: " + playerWins + " time(s).");
            System.out.println("You have lost: " + computerWins + " time(s).");
            do {
                System.out.println("Would you like to play a game (y/n)");
                answer = reader.next();
            } while (!answer.equals("y") && !answer.equals("n"));
            if (answer.equals("y")) {
                // Tell the server that the client would like to play a game
                packet = new ThreeStonesPacket(1, 0, 0, 0, 0);
                packet.sendPacket(out);
                values = packet.receivePacket(in);
                if ((int) values[0] == 3) { // Server instantiated the game
                    playGame();
                }
            } else {
                // tell the server that the client would not like to play a game
                packet = new ThreeStonesPacket(2, 0, 0, 0, 0);
                playAgain = false;
                endSession(packet);
            }
        }
    }

    /**
     * Method that when called will send a packet to the server to tell that the
     * client no longer wishes to play and the session will close.
     */
    private void endSession(ThreeStonesPacket endPacket) {
        endPacket.sendPacket(out);
        try {
            socket.close();
        } catch (IOException e) {
            System.out.println("There was a plroblem closing the socket "
                    + "in the client.");
            System.exit(1);
        }
    }

    /**
     * Method that when called will play a single game of ThreeStones with the
     * server and will tally the victor.
     */
    private void playGame() {
        playerScore = 0;
        compScore = 0;
        stonesLeft = 15;
        byte[] values = new byte[5];
        instantiateBoard();
        ThreeStonesPacket packet;
        // Gameplay loop
        while (stonesLeft > 0) {
            printBoardAndResult(); // Display board to the user
            // Get user input
            do {
                System.out.println("Select your Row");
                while (!reader.hasNextInt()) {
                    System.out.println("That's not a number, please enter a number!");
                    System.out.println("Select your Row");
                    reader.next();
                }
                row = reader.nextInt();
            } while (row < 1 || row > 11);
            do {
                System.out.println("Select your Column");
                while (!reader.hasNextInt()) {
                    System.out.println("That's not a number, please enter a number!");
                    System.out.println("Select your Column");
                    reader.next();
                }
                column = reader.nextInt();
            } while (column < 1 || column > 11);
            // Create the packet to be sent
            packet = new ThreeStonesPacket(4, row - 1, column - 1, playerScore, compScore);
            packet.sendPacket(out); // Send the packet to the server
            values = packet.receivePacket(in); // Receive packet from the server
            // loop for receiving an invalid move opcode
            while (values[0] == (int) 6) { // 6 is the opCode for invalid move
                System.out.println("That was an invalid move, try again.");
                do {
                    System.out.println("Select your Row");
                    while (!reader.hasNextInt()) {
                        System.out.println("That's not a number, please enter a number!");
                        reader.next();
                    }
                    row = reader.nextInt();
                } while (row < 1 || row > 11);
                do {
                    System.out.println("Select your Column");
                    while (!reader.hasNextInt()) {
                        System.out.println("That's not a number, please enter a number!");
                        reader.next();
                    }
                    column = reader.nextInt();
                } while (column < 1 || column > 11);
                packet = new ThreeStonesPacket(4, row - 1, column - 1, playerScore, compScore);
                packet.sendPacket(out);
                values = packet.receivePacket(in);
            }
            // the move was valid, update the board
            board[row - 1][column - 1] = Cell.WHITE;
            board[(int) values[1]][(int) values[2]] = Cell.BLACK;
            System.out.println("white placed at: " + Integer.toString(row) + ", " + Integer.toString(column));
            System.out.println("black placed at: " + Integer.toString((int) values[1] + 1)
                    + ", " + Integer.toString((int) values[2] + 1));
            playerScore = (int) values[3];
            compScore = (int) values[4];
            stonesLeft--;
        }
        // Tall who won the game
        if (playerScore >= compScore) {
            playerWins++;
            System.out.println("\n YOU WIN");
        } else {
            computerWins++;
            System.out.println("\n YOU LOSE");
        }
    }

    /**
     * Method that when called will instantiate a new board for the user. By
     * filling the 2D array will their appropriate starting cells.
     */
    private void instantiateBoard() {
        board = new Cell[11][11];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                if (i == 0 || i == 1 || i == 9 || i == 10) {
                    board[i][j] = Cell.BLOCK;
                }
                if (i == 2) {
                    if (j == 4 || j == 5 || j == 6) {
                        board[i][j] = Cell.EMPTY;
                    } else {
                        board[i][j] = Cell.BLOCK;
                    }
                }
                if (i == 3) {
                    if (j == 3 || j == 4 || j == 5 || j == 6 || j == 7) {
                        board[i][j] = Cell.EMPTY;
                    } else {
                        board[i][j] = Cell.BLOCK;
                    }
                }
                if (i == 4) {
                    if (j == 2 || j == 3 || j == 4 || j == 5 || j == 6 || j == 7 || j == 8) {
                        board[i][j] = Cell.EMPTY;
                    } else {
                        board[i][j] = Cell.BLOCK;
                    }
                }
                if (i == 5) {
                    if (j == 2 || j == 3 || j == 4 || j == 6 || j == 7 || j == 8) {
                        board[i][j] = Cell.EMPTY;
                    } else {
                        board[i][j] = Cell.BLOCK;
                    }
                }
                if (i == 6) {
                    if (j == 2 || j == 3 || j == 4 || j == 5 || j == 6 || j == 7 || j == 8) {
                        board[i][j] = Cell.EMPTY;
                    } else {
                        board[i][j] = Cell.BLOCK;
                    }
                }
                if (i == 7) {
                    if (j == 3 || j == 4 || j == 5 || j == 6 || j == 7) {
                        board[i][j] = Cell.EMPTY;
                    } else {
                        board[i][j] = Cell.BLOCK;
                    }
                }
                if (i == 8) {
                    if (j == 4 || j == 5 || j == 6) {
                        board[i][j] = Cell.EMPTY;
                    } else {
                        board[i][j] = Cell.BLOCK;
                    }
                }
            }
        }
    }

    /**
     * Print the board and score to show the user the current state of a game.
     */
    private void printBoardAndResult() {
        int counter = 1;
        int counterRow = 1;
        System.out.println("---------------------------------------------------"
                + "---------------------------------------");
        String result = "";
        for (int i = 0; i < board.length; i++) {
            if (counterRow < 10) {
                result += "       " + counterRow;
            } else if (counterRow >= 10) {
                result += "      " + counterRow;
            }
            counterRow++;
        }
        result += "\n";
        for (ThreeStonesClient.Cell[] row : board) {
            if (counter < 10) {
                result += counter + "  ";
            } else if (counter >= 10) {
                result += counter + " ";
            }
            for (ThreeStonesClient.Cell c : row) {                
                if (c == Cell.WHITE) {
                    result += "| " + ANSI_BLUE + c + ANSI_RESET + " ";
                } else if (c == Cell.BLACK) {
                    result += "| " + ANSI_RED + c + ANSI_RESET + " ";
                }
                else {
                    result += "| " + c + " ";
                }
            }
            counter++;
            result += "\n";
        }
        System.out.println(result);
        System.out.println("---------------------------------------------------"
                + "---------------------------------------");
        System.out.println("Stones Left: " + stonesLeft);
        System.out.println("Player Score: " + playerScore);
        System.out.println("Comp Score: " + compScore);
    }

}
