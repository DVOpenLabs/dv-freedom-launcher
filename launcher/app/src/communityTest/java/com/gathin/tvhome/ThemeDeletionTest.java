package com.gathin.tvhome;
import org.junit.Test;
import java.io.File;
import java.nio.file.Files;
import static org.junit.Assert.*;
public class ThemeDeletionTest {
    @Test public void unreadableParentCannotLookLikeAnAbsentFile(){
        File parent=new File("unreadable-parent"){
            @Override public boolean exists(){return true;}
            @Override public String[] list(){return null;}
        };
        File child=new File("hidden-child"){
            @Override public File getParentFile(){return parent;}
            @Override public boolean exists(){return false;}
        };
        assertThrows(IllegalStateException.class,()->ThemeDeletion.remove(child));
    }
    @Test public void deletionFailureCannotLookLikeAnAbsentFile(){
        File denied=new File("unavailable-fixture"){
            @Override public boolean exists(){return true;}
            @Override public boolean delete(){return false;}
        };
        assertThrows(IllegalStateException.class,()->ThemeDeletion.remove(denied));
    }
    @Test public void removingAnAbsentFileIsIdempotent(){
        File absent=new File("absent-fixture"){
            @Override public boolean exists(){return false;}
            @Override public boolean delete(){throw new AssertionError("An absent file does not need deletion");}
        };
        assertEquals(0,ThemeDeletion.remove(absent));
    }
    @Test public void actualLocalFileIsRemovedOnce()throws Exception{
        File file=Files.createTempFile("dv-theme-delete-",".json").toFile();
        try{assertEquals(1,ThemeDeletion.remove(file));assertFalse(file.exists());assertEquals(0,ThemeDeletion.remove(file));}
        finally{Files.deleteIfExists(file.toPath());}
    }
}
