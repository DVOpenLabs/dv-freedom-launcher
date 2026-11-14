package com.gathin.tvhome;
import org.junit.Test;
import static org.junit.Assert.*;
public class ProblemHelpTest {
    @Test public void allTopicsHaveReadOnlyAdvice(){for(int i=0;i<ProblemHelp.TOPICS.length;i++){String advice=ProblemHelp.explain(i);assertTrue(advice.length()>100);assertFalse(advice.contains("adb shell"));assertFalse(advice.contains("192.168."));}assertThrows(IllegalArgumentException.class,()->ProblemHelp.explain(-1));}
}
