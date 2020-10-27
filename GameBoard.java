package Component;
import LeaderBoard.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Random;

public class GameBoard {
    public static final int LEFT = 0;
    public static final int RIGHT = 1;
    public static final int UP = 2;
    public static final int DOWN = 3;

    public static final int ROWS = 4;
    public static final int COLS = 4;

    private final int startingTiles = 2;
    private Tile[][] board;
    public boolean dead;
    public boolean won;
    private BufferedImage gameBoard;
    private int x;
    private int y;
    private int move;

    private static int SPACING = 10;
    public static int BOARD_WIDTH = (COLS + 1) * SPACING + COLS * Tile.WIDTH;
    public static int BOARD_HEIGHT = (ROWS + 1) * SPACING + ROWS * Tile.HEIGHT;

    public boolean hasStarted;
    public boolean perform;

    private ScoreManager scores;
    public int currentScore;
    private Leaderboards lBoard;
    private int saveCount = 0;

    //for copies
    // public GameBoard(GameBoard curr){
    //     this.board = new Tile[ROWS][COLS];
    //     for (int i = 0; i < ROWS; i++){
    //         for (int j = 0; j < COLS; j++){
    //             this.board[i][j] = new Tile(curr.board[i][j].getValue(),i,j);
    //         }
    //     }
    //     this.dead = curr.dead;
    //     this.won = curr.won;
    //     this.hasStarted = curr.hasStarted;
    // }
    //normal construction
    public GameBoard(int x, int y) {
        this.x = x;
        this.y = y;
        board = new Tile[ROWS][COLS];
        gameBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_RGB);
        createBoardImage();

        lBoard = Leaderboards.getInstance();
        lBoard.loadScores();
        scores = new ScoreManager(this);
        scores.loadGame();
        currentScore = scores.getCurrentScore();
        scores.setCurrentTopScore(lBoard.getHighScore());
        if(scores.newGame()){
            start();
            scores.saveGame();
            currentScore = scores.getCurrentScore();
        }
        else{
            for(int i = 0; i < scores.getBoard().length; i++){
                if(scores.getBoard()[i] == 0) continue;
                spawn(i / ROWS, i % COLS, scores.getBoard()[i]);
            }
            // not calling setDead because we don't want to save anything
            dead = checkDead();
            // not coalling setWon because we don't want to save the time
            won = checkWon();
            currentScore = scores.getCurrentScore();
        }
        perform = true;
    }

    public void reset(){
        board = new Tile[ROWS][COLS];
        start();
        scores.saveGame();
        dead = false;
        won = false;
        hasStarted = false;
        saveCount = 0;
    }

    private void start() {
        for (int i = 0; i < startingTiles; i++) {
            spawnRandom();
        }
        move = 0;
    }

    /** Debug method */
    private void spawn(int row, int col, int value) {
        board[row][col] = new Tile(value, getTileX(col), getTileY(row));
    }

    private void createBoardImage() {
        Graphics2D g = (Graphics2D) gameBoard.getGraphics();
        g.setColor(Color.darkGray);
        g.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        g.setColor(Color.lightGray);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                int x = SPACING + SPACING * col + Tile.WIDTH * col;
                int y = SPACING + SPACING * row + Tile.HEIGHT * row;
                g.fillRoundRect(x, y, Tile.WIDTH, Tile.HEIGHT, Tile.ARC_WIDTH, Tile.ARC_HEIGHT);
            }
        }
    }

    public void update() {
        saveCount++;
        if (saveCount >= 1000) {
            saveCount = 0;
            scores.saveGame();
        }

        checkKeys();

        if (scores.getCurrentScore() > scores.getCurrentTopScore()) {
            scores.setCurrentTopScore(scores.getCurrentScore());
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Tile current = board[row][col];
                if (current == null) continue;
                current.update();
                resetPosition(current, row, col);
                if (current.getValue() == 2048) {
                    setWon(true);
                }
            }
        }
    }
    public int getMove(){
        return move;
    }

    public void render(Graphics2D g) {
        BufferedImage finalBoard = new BufferedImage(BOARD_WIDTH, BOARD_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = (Graphics2D) finalBoard.getGraphics();
        g2d.setColor(new Color(0, 0, 0, 0));
        g2d.fillRect(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        g2d.drawImage(gameBoard, 0, 0, null);

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Tile current = board[row][col];
                if (current == null) continue;
                current.render(g2d);
            }
        }

        g.drawImage(finalBoard, x, y, null);
        g2d.dispose();


    }

    private void resetPosition(Tile tile, int row, int col) {
        if (tile == null) return;

        int x = getTileX(col);
        int y = getTileY(row);

        int distX = tile.getX() - x;
        int distY = tile.getY() - y;

        if (Math.abs(distX) < Tile.SLIDE_SPEED) {
            tile.setX(tile.getX() - distX);
        }

        if (Math.abs(distY) < Tile.SLIDE_SPEED) {
            tile.setY(tile.getY() - distY);
        }

        if (distX < 0) {
            tile.setX(tile.getX() + Tile.SLIDE_SPEED);
        }
        if (distY < 0) {
            tile.setY(tile.getY() + Tile.SLIDE_SPEED);
        }
        if (distX > 0) {
            tile.setX(tile.getX() - Tile.SLIDE_SPEED);
        }
        if (distY > 0) {
            tile.setY(tile.getY() - Tile.SLIDE_SPEED);
        }
    }

    public int getTileX(int col) {
        return SPACING + col * Tile.WIDTH + col * SPACING;
    }

    public int getTileY(int row) {
        return SPACING + row * Tile.HEIGHT + row * SPACING;
    }

    private boolean checkOutOfBounds(int direction, int row, int col) {
        if (direction == LEFT) {
            return col < 0;
        }
        else if (direction == RIGHT) {
            return col > COLS - 1;
        }
        else if (direction == UP) {
            return row < 0;
        }
        else if (direction == DOWN) {
            return row > ROWS - 1;
        }
        return false;
    }

    private boolean move(int row, int col, int horizontalDirection, int verticalDirection, int direction) {
        boolean canMove = false;
        Tile current = board[row][col];
        if (current == null) return false;
        boolean move = true;
        int newCol = col;
        int newRow = row;
        while (move) {
            newCol += horizontalDirection;
            newRow += verticalDirection;
            if (checkOutOfBounds(direction, newRow, newCol)) break;
            if (board[newRow][newCol] == null) {
                board[newRow][newCol] = current;
                canMove = true;
                board[newRow - verticalDirection][newCol - horizontalDirection] = null;
                board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
            }
            else if (board[newRow][newCol].getValue() == current.getValue() && board[newRow][newCol].canCombine()) {
                board[newRow][newCol].setCanCombine(false);
                board[newRow][newCol].setValue(board[newRow][newCol].getValue() * 2);
                canMove = true;
                board[newRow - verticalDirection][newCol - horizontalDirection] = null;
                board[newRow][newCol].setSlideTo(new Point(newRow, newCol));
                board[newRow][newCol].setCombineAnimation(true);
                scores.setCurrentScore(scores.getCurrentScore() + board[newRow][newCol].getValue());
                currentScore = scores.getCurrentScore();
            }
            else {
                move = false;
            }
        }
        return canMove;
    }

    public void moveTiles(int direction) {
        boolean canMove = false;
        int horizontalDirection = 0;
        int verticalDirection = 0;

        if (direction == LEFT) {
            if (!checkDead()) move++;
            horizontalDirection = -1;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    if (!canMove)
                        canMove = move(row, col, horizontalDirection, verticalDirection, direction);
                    else {
                        move(row, col, horizontalDirection, verticalDirection, direction);
                    }
                }
            }

        }
        else if (direction == RIGHT) {
            if (!checkDead()) move++;
            horizontalDirection = 1;
            for (int row = 0; row < ROWS; row++) {
                for (int col = COLS - 1; col >= 0; col--) {
                    if (!canMove)
                        canMove = move(row, col, horizontalDirection, verticalDirection, direction);
                    else {
                        move(row, col, horizontalDirection, verticalDirection, direction);
                    }
                }
            }
        }
        else if (direction == UP) {
            if (!checkDead()) move++;
            verticalDirection = -1;
            for (int row = 0; row < ROWS; row++) {
                for (int col = 0; col < COLS; col++) {
                    if (!canMove)
                        canMove = move(row, col, horizontalDirection, verticalDirection, direction);
                    else {
                        move(row, col, horizontalDirection, verticalDirection, direction);
                    }
                }
            }
        }
        else if (direction == DOWN) {
            if (!checkDead()) move++;
            verticalDirection = 1;
            for (int row = ROWS - 1; row >= 0; row--) {
                for (int col = 0; col < COLS; col++) {
                    if (!canMove)
                        canMove = move(row, col, horizontalDirection, verticalDirection, direction);
                    else {
                        move(row, col, horizontalDirection, verticalDirection, direction);
                    }
                }
            }
        }
        else {
            System.out.println(direction + " is not a valid direction.");
        }

        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                Tile current = board[row][col];
                if (current == null) continue;
                current.setCanCombine(true);
            }
        }

        if (canMove) {
            spawnRandom();
            setDead(checkDead());
        }
    }

    // MODIFIED
    private boolean checkDead() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if (board[row][col] == null) return false;
                boolean canCombine = checkSurroundingTiles(row, col, board[row][col]);
                if (canCombine) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean checkWon() {
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLS; col++) {
                if(board[row][col] == null) continue;
                if(board[row][col].getValue() >= 2048) return true;
            }
        }
        return false;
    }

    private boolean checkSurroundingTiles(int row, int col, Tile tile) {
        if (row > 0) {
            Tile check = board[row - 1][col];
            if (check == null) return true;
            if (tile.getValue() == check.getValue()) return true;
        }
        if (row < ROWS - 1) {
            Tile check = board[row + 1][col];
            if (check == null) return true;
            if (tile.getValue() == check.getValue()) return true;
        }
        if (col > 0) {
            Tile check = board[row][col - 1];
            if (check == null) return true;
            if (tile.getValue() == check.getValue()) return true;
        }
        if (col < COLS - 1) {
            Tile check = board[row][col + 1];
            if (check == null) return true;
            if (tile.getValue() == check.getValue()) return true;
        }
        return false;
    }

    private void spawnRandom() {
        Random random = new Random();
        boolean notValid = true;

        while (notValid) {
            int location = random.nextInt(16);
            int row = location / ROWS;
            int col = location % COLS;
            Tile current = board[row][col];
            if (current == null) {
                int value = random.nextInt(10) < 8 ? 2 : 4;
                Tile tile = new Tile(value, getTileX(col), getTileY(row));
                board[row][col] = tile;
                notValid = false;
            }
        }
    }

    private void checkKeys() {
        if ((!Keys.pressed[KeyEvent.VK_LEFT] && Keys.prev[KeyEvent.VK_LEFT]) || (!Keys.pressed[KeyEvent.VK_A] && Keys.prev[KeyEvent.VK_A])) {
            moveTiles(LEFT);
            if (!hasStarted) hasStarted = !dead;
        }
        if ((!Keys.pressed[KeyEvent.VK_RIGHT] && Keys.prev[KeyEvent.VK_RIGHT]) || (!Keys.pressed[KeyEvent.VK_D] && Keys.prev[KeyEvent.VK_D])) {
            moveTiles(RIGHT);
            if (!hasStarted) hasStarted = !dead;
        }
        if ((!Keys.pressed[KeyEvent.VK_UP] && Keys.prev[KeyEvent.VK_UP]) || (!Keys.pressed[KeyEvent.VK_W] && Keys.prev[KeyEvent.VK_W])){
            moveTiles(UP);
            if (!hasStarted) hasStarted = !dead;
        }
        if ((!Keys.pressed[KeyEvent.VK_DOWN] && Keys.prev[KeyEvent.VK_DOWN])|| (!Keys.pressed[KeyEvent.VK_S] && Keys.prev[KeyEvent.VK_S])) {
            moveTiles(DOWN);
            if (!hasStarted) hasStarted = !dead;
        }
    }

    public int getHighestTileValue(){
        int value = 2;
        for(int row = 0; row < ROWS; row++){
            for(int col = 0; col < COLS; col++){
                if(board[row][col] == null) continue;
                if(board[row][col].getValue() > value) value = board[row][col].getValue();
            }
        }
        return value;
    }

    public boolean isDead() {
        return dead;
    }

    public void setDead(boolean dead) {
        if(!this.dead && dead){
            lBoard.addTile(getHighestTileValue());
            lBoard.addScore(scores.getCurrentScore());
            lBoard.saveScores();
        }
        this.dead = dead;
    }

    public Tile[][] getBoard() {
        return board;
    }

    public void setBoard(Tile[][] board) {
        this.board = board;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isWon() {
        return won;
    }

    public void setWon(boolean won) {
        if(!this.won && won && !dead){
            lBoard.saveScores();
        }
        this.won = won;
    }

    public ScoreManager getScores(){
        return scores;
    }
}
