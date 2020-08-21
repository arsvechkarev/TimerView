package timerx.format;

import static org.junit.Assert.assertSame;
import static timerx.TestHelper.updateFormatIfNecessary;

import org.junit.Test;
import timerx.TimeUnit;
import timerx.exceptions.IllegalSymbolsCombinationException;
import timerx.exceptions.NoNecessarySymbolsException;
import timerx.exceptions.NonContiguousFormatSymbolsException;

public class AnalyzerTest {

  public TimeUnit smallestAvailableUnitOf(String format) {
    return analyze(format).smallestAvailableUnit;
  }

  public Semantic analyze(String format) {
    return Analyzer.analyze(updateFormatIfNecessary(format));
  }

  @Test
  public void positiveTest1() {
    assertSame(TimeUnit.R_MILLISECONDS, smallestAvailableUnitOf("HH:MM:SS.LLL"));
  }

  @Test
  public void positiveTest2() {
    assertSame(TimeUnit.SECONDS, smallestAvailableUnitOf("HhSMM"));
  }

  @Test
  public void positiveTest3() {
    assertSame(TimeUnit.R_MILLISECONDS, smallestAvailableUnitOf("ssMMmSSmLLl"));
  }

  @Test
  public void positiveTest4() {
    assertSame(TimeUnit.MINUTES, smallestAvailableUnitOf("H/\\*)MMMM"));
  }

  @Test
  public void positiveTest5() {
    assertSame(TimeUnit.SECONDS, smallestAvailableUnitOf("MMMM%^:SS#$&*"));
  }

  @Test
  public void positiveTest6() {
    assertSame(TimeUnit.R_MILLISECONDS, smallestAvailableUnitOf(":SS::LL::#$&*"));
  }

  @Test
  public void positiveTest7() {
    assertSame(TimeUnit.SECONDS, smallestAvailableUnitOf("SSS"));
  }

  @Test
  public void positiveTestWithEscaping1() {
    assertSame(TimeUnit.R_MILLISECONDS,
        smallestAvailableUnitOf("H#HMM#M:SS#S:LL#E#Ls##"));
  }

  @Test
  public void positiveTestWithEscaping2() {
    assertSame(TimeUnit.SECONDS, smallestAvailableUnitOf("HH#H - MM#MSS@#S"));
  }

  @Test
  public void positiveTestWithEscaping3() {
    assertSame(TimeUnit.R_MILLISECONDS, smallestAvailableUnitOf("ssM#MmmSS#Sh#LL#h"));
  }

  @Test
  public void positiveTestWithEscaping4() {
    assertSame(TimeUnit.MINUTES, smallestAvailableUnitOf("H####H/#M#M#MM"));
  }

  @Test
  public void positiveTestWithEscaping5() {
    assertSame(TimeUnit.SECONDS, smallestAvailableUnitOf("MM#####M#M%^:SS#$&*"));
  }

  @Test
  public void positiveTestWithEscaping6() {
    assertSame(TimeUnit.R_MILLISECONDS, smallestAvailableUnitOf(":SS#S==:##LL::#$&*"));
  }

  @Test
  public void positiveTestWithEscaping7() {
    assertSame(TimeUnit.SECONDS, smallestAvailableUnitOf("#S#SS#S#S"));
  }

  @Test(expected = NoNecessarySymbolsException.class)
  public void negativeTestWithNoElements() {
    analyze("qwerty lol! ###");
  }

  @Test(expected = NoNecessarySymbolsException.class)
  public void negativeTestWithAllCommentedElements() {
    analyze("#H#Hs#S#L");
  }

  @Test(expected = NonContiguousFormatSymbolsException.class)
  public void negativeTestWithIncorrectPositions1() {
    analyze("H#HH");
  }

  @Test(expected = NonContiguousFormatSymbolsException.class)
  public void negativeTestWithIncorrectPositions2() {
    analyze("HH:MM:SSqwertyH");
  }

  @Test(expected = NonContiguousFormatSymbolsException.class)
  public void negativeTestWithIncorrectPositions3() {
    analyze("HH#HSSS %^&*sS");
  }

  @Test(expected = NonContiguousFormatSymbolsException.class)
  public void negativeTestWithIncorrectPositions4() {
    analyze("LLasfdLH^&sdHasdL");
  }

  @Test(expected = NonContiguousFormatSymbolsException.class)
  public void negativeTestWithIncorrectPositions5() {
    analyze("M#M#H#H098/M");
  }

  @Test(expected = IllegalSymbolsCombinationException.class)
  public void negativeTestWithIncorrectCombination1() {
    analyze("HH:MM:L");
  }

  @Test(expected = IllegalSymbolsCombinationException.class)
  public void negativeTestWithIncorrectCombination2() {
    analyze("HH:SS:L");
  }

  @Test(expected = IllegalSymbolsCombinationException.class)
  public void negativeTestWithIncorrectCombination3() {
    analyze("HH:SS");
  }

  @Test(expected = IllegalSymbolsCombinationException.class)
  public void negativeTestWithIncorrectCombination4() {
    analyze("LLLL:H");
  }

  @Test(expected = IllegalSymbolsCombinationException.class)
  public void negativeTestWithIncorrectCombination5() {
    analyze(":M#ME#::LL");
  }
}