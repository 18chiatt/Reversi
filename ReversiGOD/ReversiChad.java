import java.awt.*;
import java.util.*;
import java.awt.event.*;
import java.lang.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.math.*;
import java.text.*;


class ValidMoves {
    public final int[] validMoves;
    public final int numValidMoves;

    public ValidMoves(int numValidMoves, int[] validMoves){
        this.validMoves = validMoves;
        this.numValidMoves = numValidMoves;
    }
}

class MoveValue {
    public final int index;
    public final double value;

    public MoveValue(int index, double value){
        this.index = index;
        this.value = value;
    }
}

class ReversiChad {

    public Socket s;
	public BufferedReader sin;
	public PrintWriter sout;
    Random generator = new Random();
    private final int depth = 7;
    double [][] importance = { {10,-1,9,7,7,9,-1,10}, {-1,-4,6,5,5,6,-4,-1},{9,3,2,2,2,2,3,9},{7,4,3,2,2,3,4,7},{7,4,3,2,2,3,4,7},{9,3,2,2,2,2,3,9},{-1,-4,6,5,5,6,-4,-1}, {10,-1,9,7,7,9,-1,10}  };
    private int _me;
    private ArrayList<Integer> explored = new ArrayList<Integer>();
    private int friendlyCornerModifier = 5;
    private int enemyCornerModifier = -4;


    double t1, t2;
    int boardState;
    // int state[][] = new int[8][8]; // state[0][0] is the bottom left corner of the board (on the GUI)
    int turn = -1;
    int round;
    
    int validMoves[] = new int[64];
    int numValidMoves;
    int heuristicStates = 0;
    
    // main function that (1) establishes a connection with the server, and then plays whenever it is this player's turn
    public ReversiChad(int _me, String host) {
        int me = _me;
        this._me = _me;
        initClient(host);

        int myMove;
        double sum = 0;

        for(int i =0; i< 8; i++){
            for(int j = 0; j< 8; j++){
                sum += importance[i][j];
            }
        }

        for(int i =0; i< 8; i++){
            for(int j = 0; j< 8; j++){
                importance[i][j] = importance[i][j] / sum;
            }
        }
        
        while (true) {
            System.out.println("Read");
            int[][] state = readMessage();
            
            if (turn == _me) {
                heuristicStates = 0;
                System.out.println("Move");
                ValidMoves moves = getValidMoves(round, state, _me);
                
                myMove = move(state, me);
                if(myMove >= moves.numValidMoves){
                    System.out.println("wE HAVE An error!");
                }

                //myMove = generator.nextInt(numValidMoves);        // select a move randomly
                explored.add(heuristicStates);
                System.out.println(heuristicStates);
                String sel = moves.validMoves[myMove] / 8 + "\n" + moves.validMoves[myMove] % 8;
                
                System.out.println("Selection: " + moves.validMoves[myMove] / 8 + ", " + moves.validMoves[myMove] % 8);
                
                sout.println(sel);
            }
        }
        //while (turn == me) {
        //    System.out.println("My turn");
            
            //readMessage();
        //}
    }
    
    // You should modify this function
    // validMoves is a list of valid locations that you could place your "stone" on this turn
    // Note that "state" is a global variable 2D list that shows the state of the game
    private int move( int[][] state, int me) {

        int indexToDo = -1;
        if(me == 1){
            MoveValue vals = maxPlayer(state, depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, round);
            System.out.println(vals.value);
            indexToDo = vals.index;
        } else {
            MoveValue vals = minPlayer(state, depth, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, round);
            System.out.println(vals.value);
            indexToDo = vals.index;


        }
        System.out.println(validMoves);
        return indexToDo;
    }

    private int[][] makeState(int[][] startState, int move, int player) {
        int[][] newState = new int[8][8];
        for(int i =0; i< 8; i++){
            for(int j =0; j< 8; j++){
                newState[i][j] = startState[i][j];
            }
        }
        int row = move / 8;
        int column = move % 8;
        newState[row][column] = move;
        return newState;
    }

    private MoveValue maxPlayer(int[][] state, int depth,double a,double b, int round){
        ValidMoves validMoves = getValidMoves(round, state,1);
        if(depth == 0 ){
            return new MoveValue(-1, heuristic(state,2));
        }

        if(validMoves.numValidMoves < 1) {
            ValidMoves opponentMove = getValidMoves(round, state, 2);
            if(opponentMove.numValidMoves == 0){
                if(amWinning(state, 1)){
                    return new MoveValue(-1,999999999);
                } else {
                    return new MoveValue(-1, -999999999);
                }
            }
            return new MoveValue(-1, minPlayer(state, depth, a, b, round).value);
        }
        double value = Double.NEGATIVE_INFINITY;
        int bestIndex = -1;
        for(int i =0; i< validMoves.numValidMoves; i++){
            int move = validMoves.validMoves[i];
            int[][] childState = makeState(state, move, 1);
            MoveValue childValue = minPlayer(childState, depth - 1, a, b, round + 1);
            if( childValue.value > value){
                bestIndex = i;
            }
            value = Math.max(value, childValue.value);
            a = Math.max(a, value);
            if (value >= b){
                break;
            }
        }
        return new MoveValue(bestIndex,value);
    }

    private MoveValue minPlayer(int[][] state, int depth, double a, double b, int round){
        if(depth == 0){
            return new MoveValue(-1, heuristic(state, 2));
        }

        double value = Double.POSITIVE_INFINITY;
        int bestIndex = -1;
        ValidMoves validMoves = getValidMoves(round, state, 2);
        if(validMoves.numValidMoves < 1){
            ValidMoves opponentMove = getValidMoves(round, state, 1);
            if(opponentMove.numValidMoves == 0){
                if(amWinning(state, 2)){
                    return new MoveValue(-1,-999999999);
                } else {
                    return new MoveValue(-1, 999999999);
                }
            }
            return new MoveValue(-1, maxPlayer(state,depth,a,b,round).value);
        }
        for(int i =0; i< validMoves.numValidMoves; i++){
            int move = validMoves.validMoves[i];
            int[][] childState = makeState(state, move, 2);
            MoveValue childValue = maxPlayer(childState, depth - 1, a, b, round + 1);
            if(childValue.value < value){
                bestIndex = i;
            }
            value = Math.min(value, childValue.value);
            b = Math.min(b, value);
            if (value <= a){
                break;
            }
        }
        return new MoveValue(bestIndex,value);
    }

    private boolean amWinning(int[][] state, int player){
        int myCount = 0;
        int enemyCount = 0;
        for(int i =0; i< 8 ;i++){
            for(int j=0; j< 8; j++){
                if(state[i][j] == 0){
                    continue;
                }
                if(state[i][j] == player){
                    myCount++;
                    continue;
                }
                enemyCount++;
            }
        }
        return myCount > enemyCount;
    }

    private double heuristic(int[][] state, int player){
        heuristicStates++;
        int myCount = 0;
        int enemyCount = 0;
        double myValue = 0.0;
        double enemyValue = 0.0;
        double [][] importance = { {10,-1,9,7,7,9,-1,10}, {-1,-4,6,5,5,6,-4,-1},{9,3,2,2,2,2,3,9},{7,4,3,2,2,3,4,7},{7,4,3,2,2,3,4,7},{9,3,2,2,2,2,3,9},{-1,-4,6,5,5,6,-4,-1}, {10,-1,9,7,7,9,-1,10}  };

        if(state[0][0] != 0){
            int modifier = state[0][0] == player ? friendlyCornerModifier : enemyCornerModifier;
            importance[0][1] += modifier;
            importance[1][0] += modifier;
            importance[1][1] += modifier;
        }

        if(state[7][7] != 0){
            int modifier = state[7][7] == player ? friendlyCornerModifier : enemyCornerModifier;
            importance[7][6] += modifier;
            importance[6][7] += modifier;
            importance[6][6] += modifier;
        }

        if(state[0][7] != 0){
            int modifier = state[0][7] == player ? friendlyCornerModifier : enemyCornerModifier;
            importance[0][6] += modifier;
            importance[1][7] += modifier;
            importance[1][6] += modifier;
        }

        if(state[7][0] != 0){
            int modifier = state[7][0] == player ? friendlyCornerModifier : enemyCornerModifier;
            importance[7][1] += modifier;
            importance[6][0] += modifier;
            importance[6][1] += modifier;
        }


        for(int i =0; i< 8; i++){
            for(int j =0 ; j< 8; j++){
                int owned = state[i][j];
                if(owned == 0){
                    continue;
                }
                if(owned == player){
                    myCount++;
                    myValue = myValue + importance[i][j];
                    continue;
                }
                enemyCount++;
                enemyValue = enemyValue + importance[i][j];

            }
        }
        if(myCount + enemyCount == 64){
            if(myCount > enemyCount){
                return player == 1 ? 9999999 : -999999;
            }
            return player == 1 ? -9999999 : 99999999;
        }

        double friendlyNet = myValue - enemyValue;
        if (player == 2){
            friendlyNet *= -1;
        }

        // myVal = .5
        // enemyVal = 1.5


        return friendlyNet;
    }
    
    // generates the set of valid moves for the player; returns a list of valid moves (validMoves)
    private ValidMoves getValidMoves(int round, int state[][], int me) {
        int i, j;
        int[] validMoves = new int[64];
        int numValidMoves = 0;
        numValidMoves = 0;
        if (round < 4) {
            if (state[3][3] == 0) {
                validMoves[numValidMoves] = 3*8 + 3;
                numValidMoves ++;
            }
            if (state[3][4] == 0) {
                validMoves[numValidMoves] = 3*8 + 4;
                numValidMoves ++;
            }
            if (state[4][3] == 0) {
                validMoves[numValidMoves] = 4*8 + 3;
                numValidMoves ++;
            }
            if (state[4][4] == 0) {
                validMoves[numValidMoves] = 4*8 + 4;
                numValidMoves ++;
            }

        }
        else {
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    if (state[i][j] == 0) {
                        if (couldBe(state, i, j, me)) {
                            validMoves[numValidMoves] = i*8 + j;
                            numValidMoves ++;
                        }
                    }
                }
            }
        }
        
        return new ValidMoves(numValidMoves,validMoves);
    }
    
    private boolean checkDirection(int state[][], int row, int col, int incx, int incy, int me) {
        int sequence[] = new int[7];
        int seqLen;
        int i, r, c;
        
        seqLen = 0;
        for (i = 1; i < 8; i++) {
            r = row+incy*i;
            c = col+incx*i;
        
            if ((r < 0) || (r > 7) || (c < 0) || (c > 7))
                break;
        
            sequence[seqLen] = state[r][c];
            seqLen++;
        }
        
        int count = 0;
        for (i = 0; i < seqLen; i++) {
            if (me == 1) {
                if (sequence[i] == 2)
                    count ++;
                else {
                    if ((sequence[i] == 1) && (count > 0))
                        return true;
                    break;
                }
            }
            else {
                if (sequence[i] == 1)
                    count ++;
                else {
                    if ((sequence[i] == 2) && (count > 0))
                        return true;
                    break;
                }
            }
        }
        
        return false;
    }
    
    private boolean couldBe(int state[][], int row, int col, int me) {
        int incx, incy;
        
        for (incx = -1; incx < 2; incx++) {
            for (incy = -1; incy < 2; incy++) {
                if ((incx == 0) && (incy == 0))
                    continue;
            
                if (checkDirection(state, row, col, incx, incy, me))
                    return true;
            }
        }
        
        return false;
    }
    
    public int[][] readMessage() {
        int i, j;
        String status;
        int[][] state = new int[8][8];
        try {
            //System.out.println("Ready to read again");
            turn = Integer.parseInt(sin.readLine());
            
            if (turn == -999) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
                
                System.exit(1);
            }
            
            //System.out.println("Turn: " + turn);
            round = Integer.parseInt(sin.readLine());
            t1 = Double.parseDouble(sin.readLine());
            System.out.println(t1);
            t2 = Double.parseDouble(sin.readLine());
            System.out.println(t2);
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    state[i][j] = Integer.parseInt(sin.readLine());
                }
            }
            sin.readLine();
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
        
        System.out.println("Turn: " + turn);
        System.out.println("Round: " + round);
        for (i = 7; i >= 0; i--) {
            for (j = 0; j < 8; j++) {
                System.out.print(state[i][j]);
            }
            System.out.println();
        }
        System.out.println();
        return state;
    }
    
    public void initClient(String host) {
        int portNumber = 3333+_me;
        
        try {
			s = new Socket(host, portNumber);
            sout = new PrintWriter(s.getOutputStream(), true);
			sin = new BufferedReader(new InputStreamReader(s.getInputStream()));
            
            String info = sin.readLine();
            System.out.println(info);
        } catch (IOException e) {
            System.err.println("Caught IOException: " + e.getMessage());
        }
    }

    
    // compile on your machine: javac *.java
    // call: java RandomGuy [ipaddress] [player_number]
    //   ipaddress is the ipaddress on the computer the server was launched on.  Enter "localhost" if it is on the same computer
    //   player_number is 1 (for the black player) and 2 (for the white player)
    public static void main(String args[]) {
        new ReversiChad(Integer.parseInt(args[1]), args[0]);
    }
    
}
