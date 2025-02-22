package io.cosmosoftware.kite.janus;

import io.cosmosoftware.kite.exception.KiteTestException;
import io.cosmosoftware.kite.janus.checks.AllVideoCheck;
import io.cosmosoftware.kite.janus.checks.FirstVideoCheck;
import io.cosmosoftware.kite.janus.pages.JanusPage;
import io.cosmosoftware.kite.janus.steps.GetStatsStep;
import io.cosmosoftware.kite.janus.steps.LeaveDemoStep;
import io.cosmosoftware.kite.janus.steps.StartDemoStep;
import io.cosmosoftware.kite.janus.steps.WaitStep;
import io.cosmosoftware.kite.janus.steps.videoroom.JoinVideoRoomStep;
import io.cosmosoftware.kite.report.Status;
import io.cosmosoftware.kite.steps.ScreenshotStep;
import io.cosmosoftware.kite.steps.WaitForOthersStep;
import io.cosmosoftware.kite.util.TestUtils;
import io.cosmosoftware.kite.steps.TestStep;
import org.webrtc.kite.tests.KiteBaseTest;
import org.webrtc.kite.tests.TestRunner;

import java.util.concurrent.ThreadLocalRandom;

import static org.webrtc.kite.Utils.getStackTrace;

public class JanusVideoRoomTest extends KiteBaseTest {

  protected boolean sfu = false;
  protected int stepTimeout = 1800;
  protected int firstRoomId = 1;
  protected int startIntervalRandomRange = 1000;
  protected int startInterval = 500;
  protected int startIntervalById = 0;

  @Override
  public void populateTestSteps(TestRunner runner) {
    try {
      String userName = "user" + TestUtils.idToString(runner.getId());
      final JanusPage janusPage = new JanusPage(runner);
      String room = "" + (runner.getId() / getMaxUsersPerRoom() + firstRoomId);
      waitRandom(runner);
      runner.addStep(new StartDemoStep(runner, this.url + "?room="+room));
      //find a way to have no more than 6 user per room with the room manager(flag?) or accept the pop up if there is too many users in the room
      runner.addStep(new JoinVideoRoomStep(runner, userName, janusPage));
      if(janusPage.getRegistrationState()){
        runner.addStep(waitForOthersStep(runner, this, runner.getLastStep()));
        waitRandom(runner);

        runner.addStep(new FirstVideoCheck(runner));
        runner.addStep(waitForOthersStep(runner, this, runner.getLastStep()));
        waitRandom(runner);

        runner.addStep(new AllVideoCheck(runner, getMaxUsersPerRoom(), janusPage));
        runner.addStep(waitForOthersStep(runner, this, runner.getLastStep()));
        waitRandom(runner);

        if (this.takeScreenshotForEachTest()) {
          ScreenshotStep screenshotStep = new ScreenshotStep(runner);
          screenshotStep.setName(screenshotStep.getName() + "-before-get-stat");
          runner.addStep(screenshotStep);
          runner.addStep(waitForOthersStep(runner, this, runner.getLastStep()));
          waitRandom(runner);
        }

        if (this.getStats()) {
          runner.addStep(new GetStatsStep(runner, getStatsConfig, sfu, janusPage));
          runner.addStep(waitForOthersStep(runner, this, runner.getLastStep()));
          waitRandom(runner);
        }

        if (this.takeScreenshotForEachTest()) {
          ScreenshotStep screenshotStep = new ScreenshotStep(runner);
          screenshotStep.setName(screenshotStep.getName() + "-after-get-stat");
          runner.addStep(screenshotStep);
          runner.addStep(waitForOthersStep(runner, this, runner.getLastStep()));
          waitRandom(runner);
        }

        runner.addStep(new LeaveDemoStep(runner));
      }else{
        throw new KiteTestException("Videoroom is too crowded", Status.BROKEN);
      }

    } catch (Exception e) {
      logger.error(getStackTrace(e));
    }
  }

  private WaitForOthersStep waitForOthersStep(TestRunner runner, JanusVideoRoomTest stepSynchronizer,
      TestStep stepToWaitFor) {
        WaitForOthersStep w = new WaitForOthersStep(runner, stepSynchronizer, stepToWaitFor);
        w.setTimeout(stepTimeout);
        return w;
  }

  private void waitRandom(TestRunner runner) {
    runner.addStep(new WaitStep(runner, runner.getId() * startIntervalById + startInterval + ThreadLocalRandom.current().nextInt(startIntervalRandomRange)));
  }
  private Integer getTupleSize() {
    return this.testConfig.getTupleSize();
  }

  @Override
  public void payloadHandling () {
    super.payloadHandling();
    sfu = payload.getBoolean("sfu", false);
    stepTimeout = payload.getInt("stepTimeout", 1800);
    firstRoomId = payload.getInt("firstRoomId", 1);
    startIntervalRandomRange = payload.getInt("startIntervalRandomRange", 1000);
    startInterval = payload.getInt("startInterval", 500);
    startIntervalById = payload.getInt("startIntervalById", 0);
  }
}