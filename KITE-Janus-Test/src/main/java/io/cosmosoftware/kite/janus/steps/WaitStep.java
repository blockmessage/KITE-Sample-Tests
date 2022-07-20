package io.cosmosoftware.kite.janus.steps;

import io.cosmosoftware.kite.exception.KiteTestException;
import io.cosmosoftware.kite.interfaces.Runner;
import io.cosmosoftware.kite.janus.pages.JanusPage;
import io.cosmosoftware.kite.steps.TestStep;

import java.util.concurrent.ThreadLocalRandom;

import static io.cosmosoftware.kite.entities.Timeouts.ONE_SECOND_INTERVAL;
import static io.cosmosoftware.kite.util.TestUtils.waitAround;
import static io.cosmosoftware.kite.util.WebDriverUtils.loadPage;

public class WaitStep extends TestStep {

  private final Integer milliSeconds;


  public WaitStep(Runner runner, Integer milliSeconds) {
    super(runner);
    this.milliSeconds = milliSeconds;
  }
  
  @Override
  public String stepDescription() {
    return "Wait " + milliSeconds + " milliSeconds";
  }
  
  @Override
  protected void step() throws KiteTestException {
    waitAround(milliSeconds);
  }
}
