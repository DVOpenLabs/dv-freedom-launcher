package com.gathin.tvhome;
import org.junit.Test;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public class CinemaPackTest {
    @Test public void portableWorldRequiresExactlyOneSelfReference()throws Exception{
        Path root=Files.createTempDirectory("portable-world");
        try{
            String world="{\"schema\":1,\"worlds\":[{\"id\":\"hobbit\",\"wallpaper\":\""+CinemaPack.SELF+"\"}]}";
            Files.write(root.resolve("cinema.json"),world.getBytes(StandardCharsets.UTF_8));assertEquals(1,CinemaPack.portable(root.toFile()).worlds.size());
            Files.write(root.resolve("cinema.json"),world.replace(CinemaPack.SELF,"p-11111111-1111-1111-1111-111111111111").getBytes(StandardCharsets.UTF_8));assertThrows(Exception.class,()->CinemaPack.portable(root.toFile()));
            Files.write(root.resolve("cinema.json"),"{\"schema\":1,\"worlds\":[]}".getBytes(StandardCharsets.UTF_8));assertThrows(Exception.class,()->CinemaPack.portable(root.toFile()));
        }finally{Files.deleteIfExists(root.resolve("cinema.json"));Files.deleteIfExists(root);}
    }
}
