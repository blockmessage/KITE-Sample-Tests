package io.cosmosoftware.kite.simulcast.steps;

import io.cosmosoftware.kite.exception.KiteTestException;
import io.cosmosoftware.kite.report.Reporter;
import io.cosmosoftware.kite.report.Status;
import io.cosmosoftware.kite.steps.TestStep;
import org.openqa.selenium.WebDriver;

import javax.json.JsonArray;
import javax.json.JsonObject;

import static org.webrtc.kite.stats.StatsUtils.getPCStatOvertime;

public class GetStatsStep extends TestStep {

  private final JsonObject getStatsConfig;

  public GetStatsStep(WebDriver webDriver, JsonObject getStatsConfig) {
    super(webDriver);
    this.getStatsConfig = getStatsConfig;
  }

  
  
  @Override
  public String stepDescription() {
    return "GetStats";
  }
  
  @Override
  protected void step() throws KiteTestException {
    try {
      JsonObject stats = getPCStatOvertime(webDriver, getStatsConfig).get(0);
      Reporter.getInstance().jsonAttachment(report, "getStatsRaw", stats);
    } catch (Exception e) {
      e.printStackTrace();
      throw new KiteTestException("Failed to getStats", Status.BROKEN, e);
    }
  }
}
