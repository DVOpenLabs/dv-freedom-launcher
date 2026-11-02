package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class FreedomChessTest {
    private void move(FreedomChess c,int from,int to){for(FreedomChess.Move m:c.legal())if(m.from==from&&m.to==to){c.apply(m);return;}fail("Illegal move "+from+" "+to);}
    @Test public void standardPerft(){FreedomChess c=new FreedomChess();assertEquals(20,c.perft(1));assertEquals(400,c.perft(2));assertEquals(8902,c.perft(3));}
    @Test public void foolsMate(){FreedomChess c=new FreedomChess();move(c,53,45);move(c,12,28);move(c,54,38);move(c,3,39);assertTrue(c.check(true));assertTrue(c.legal().isEmpty());}
    @Test public void enPassantRemovesCorrectPawn(){FreedomChess c=new FreedomChess();move(c,52,36);move(c,8,16);move(c,36,28);move(c,11,27);move(c,28,19);assertEquals(0,c.board[27]);assertEquals(1,c.board[19]);}
    @Test public void savePreservesPositionAndCastleRights(){FreedomChess c=new FreedomChess();move(c,52,36);FreedomChess d=FreedomChess.load(c.save());assertArrayEquals(c.board,d.board);assertEquals(c.rights,d.rights);assertEquals(c.ep,d.ep);assertEquals(c.white,d.white);}
    @Test public void kingCannotMoveIntoCheck(){FreedomChess c=new FreedomChess();c.board=new int[64];c.board[60]=6;c.board[4]=-6;c.board[12]=-4;c.white=true;c.rights=0;for(FreedomChess.Move m:c.legal())assertNotEquals(52,m.to);}
    @Test public void castleMovesRook(){FreedomChess c=new FreedomChess();c.board[61]=c.board[62]=0;c.board[5]=c.board[6]=0;FreedomChess.Move castle=null;for(FreedomChess.Move m:c.legal())if(m.from==60&&m.to==62)castle=m;assertNotNull(castle);c.apply(castle);assertEquals(4,c.board[61]);assertEquals(0,c.board[63]);}
}
