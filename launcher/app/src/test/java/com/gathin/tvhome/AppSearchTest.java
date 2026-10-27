package com.gathin.tvhome;

import org.junit.Test;
import static org.junit.Assert.*;

public class AppSearchTest {
    @Test public void emptyQueryKeepsCatalog(){assertTrue(AppSearch.matches("Netflix","com.netflix.tv","  "));}
    @Test public void caseAndAccentsAreIgnored(){assertTrue(AppSearch.matches("Télévision","org.example.tv","TELE"));}
    @Test public void everyWordMustMatch(){assertFalse(AppSearch.matches("Prime Video","com.amazon.tv","prime music"));}
    @Test public void matchesPackageAndLabel(){assertTrue(AppSearch.matches("Prime Video","com.amazon.tv","amazon video"));}
    @Test public void punctuationIsLiteral(){assertFalse(AppSearch.matches("Netflix","com.netflix.tv",".*"));}
    @Test public void nonLatinNamesWork(){assertTrue(AppSearch.matches("テレビ","org.example.tv","テレビ"));}
}
