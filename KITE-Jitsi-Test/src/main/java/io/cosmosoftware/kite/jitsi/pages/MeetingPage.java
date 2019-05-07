package io.cosmosoftware.kite.jitsi.pages;

import io.cosmosoftware.kite.pages.BasePage;
import org.apache.log4j.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.webrtc.kite.stats.RTCIceCandidatePairStats;
import org.webrtc.kite.stats.RTCRTPStreamStats;
import org.webrtc.kite.stats.RTCStatObject;

import javax.json.*;
import java.io.StringReader;
import java.util.*;

import static io.cosmosoftware.kite.stats.GetStatsUtils.getStatsOnce;
import static io.cosmosoftware.kite.util.TestUtils.executeJsScript;
import static io.cosmosoftware.kite.util.TestUtils.waitAround;

public class MeetingPage extends BasePage {
  public MeetingPage(WebDriver webDriver, Logger logger) {
    super(webDriver, logger);
  }

  public int numberOfParticipants =
      Integer.parseInt(executeJsScript(webDriver, getNumberOfParticipantScript()).toString()) + 1;

  @FindBy(id = "largeVideo")
  public WebElement mainVideo;

  @FindBy(tagName = "video")
  public List<WebElement> videos;

  @FindBy(xpath = "//*[@id=\"new-toolbox\"]/div[2]/div[3]/div[1]/div/div")
  public WebElement manyTilesVideoToggle;

  public String getNumberOfParticipantScript() {
    return "return APP.conference.getNumberOfParticipantsWithTracks();";
  }

  public JsonObject getPCStatOvertime1(
      WebDriver webDriver, int durationInSeconds, int intervalInSeconds, JsonArray selectedStats)
      throws Exception {
    String statString;
    HashMap<String, HashMap<String, JsonArrayBuilder>> allStatsMap = new HashMap<>();
    HashMap<String, JsonArrayBuilder> videoBweMap = new HashMap<>();
    HashMap<String, JsonArrayBuilder> ssrcMap = new HashMap<>();
    HashMap<String, JsonArrayBuilder> localCandidateMap = new HashMap<>();
    HashMap<String, JsonArrayBuilder> remoteCandidateMap = new HashMap<>();
    HashMap<String, JsonArrayBuilder> googTrackMap = new HashMap<>();
    HashMap<String, JsonArrayBuilder> googComponentMap = new HashMap<>();
    HashMap<String, JsonArrayBuilder> googCandidatePairMap = new HashMap<>();
    HashMap<String, JsonArrayBuilder> googLibjingleSessionMap = new HashMap<>();
    HashMap<String, JsonArrayBuilder> googCertificateMap = new HashMap<>();
    allStatsMap.put("VideoBwe", videoBweMap);
    allStatsMap.put("ssrc", ssrcMap);
    allStatsMap.put("localcandidate", localCandidateMap);
    allStatsMap.put("remotecandidate", remoteCandidateMap);
    allStatsMap.put("googTrack", googTrackMap);
    allStatsMap.put("googComponent", googComponentMap);
    allStatsMap.put("googCandidatePair", googCandidatePairMap);
    allStatsMap.put("googLibjingleSession", googLibjingleSessionMap);
    allStatsMap.put("googCertificate", googCertificateMap);
    for (int timer = 0; timer < durationInSeconds; timer += intervalInSeconds) {
      statString =
          formatToJsonString(getStatsOnce("jitsi", webDriver).toString().replaceAll("=", ":"));
      JsonArray jArr = stringToJsonArray(statString).getJsonArray(0);
      for (JsonValue j : jArr) {
        JsonObject data = j.asJsonObject();
        for (String key : data.keySet()) {
          if (key.equals("type")) {
            String type = data.getString(key);
            for (JsonValue value : selectedStats) {
              String statName = value.toString();
              statName = statName.substring(1, statName.length() - 1);
              String mapDataKey = data.getString("id");
              if (type.equalsIgnoreCase(statName)) {
                switch (type) {
                  case "VideoBwe":
                    updateHashMap(videoBweMap, "videoBwe", data);
                    break;
                  case "ssrc":
                    updateHashMap(ssrcMap, mapDataKey, data);
                    break;
                  case "localcandidate":
                    updateHashMap(localCandidateMap, mapDataKey, data);
                    break;
                  case "remotecandidate":
                    updateHashMap(remoteCandidateMap, mapDataKey, data);
                    break;
                  case "googTrack":
                    updateHashMap(googTrackMap, mapDataKey, data);
                    break;
                  case "googComponent":
                    updateHashMap(googComponentMap, mapDataKey, data);
                    break;
                  case "googCandidatePair":
                    updateHashMap(googCandidatePairMap, mapDataKey, data);
                    break;
                  case "googLibjingleSession":
                    updateHashMap(googLibjingleSessionMap, mapDataKey, data);
                    break;
                  case "googCertificate":
                    updateHashMap(googCertificateMap, mapDataKey, data);
                    break;
                }
              }
            }
          }
        }
      }
      waitAround(intervalInSeconds * 1000);
    }
    return buildFinalStats(allStatsMap);
  }

  public JsonObject getPCStatOvertime(
      WebDriver webDriver, int durationInSeconds, int intervalInSeconds, JsonArray selectedStats) {
    Map<String, Object> statMap = new HashMap<>();
    try {
      for (int timer = 0; timer < durationInSeconds; timer += intervalInSeconds) {
        waitAround(intervalInSeconds);
        Object stats = getStatsOnce("jitsi", webDriver);
        stats = stats.toString().substring(1, stats.toString().length() - 1);
        if (timer == 0) {
          statMap.put("stats", new ArrayList<>());
          ((List<Object>) statMap.get("stats")).add(stats);
        } else if (timer + intervalInSeconds == durationInSeconds) {
          ((List<Object>) statMap.get("stats")).add(stats);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return buildPCStatObject(statMap, selectedStats).build();
  }

  private JsonObjectBuilder buildPCStatObject(
      Map<String, Object> statMap, JsonArray selectedStats) {
    try {
      JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
      List<Object> clientStatArray = (ArrayList) statMap.get("stats");
      JsonArrayBuilder jsonClientStatArray = Json.createArrayBuilder();
      for (Object stats : clientStatArray) {
        JsonObjectBuilder jsonRTCStatObjectBuilder = buildSingleRTCStatObject(stats, selectedStats);
        jsonClientStatArray.add(jsonRTCStatObjectBuilder);
      }
      jsonObjectBuilder.add("statsArray", jsonClientStatArray);
      return jsonObjectBuilder;

    } catch (ClassCastException e) {
      e.printStackTrace();
      return Json.createObjectBuilder();
    }
  }

  public static JsonObjectBuilder buildSingleRTCStatObject(
      Object statArray, JsonArray statsSelection) {
    JsonObjectBuilder jsonObjectBuilder = Json.createObjectBuilder();
    Map<String, List<RTCStatObject>> statObjectMap = new HashMap<>();
    if (statArray != null) {
      statArray = stringToArr(statArray.toString());
      for (Object map : (ArrayList) statArray) {
        if (map != null) {
          Map<Object, Object> statMap = stringToMap(map.toString());
          System.out.println(statMap);
          String type = (String) statMap.get("type");
          System.out.println(type);
          if (statsSelection == null || statsSelection.toString().contains(type)) {
            RTCStatObject statObject = null;
            switch (type) {
              case "ssrc":
                {
                  String id = (String) statMap.get("id");
                  if (id.contains("recv")) {
                    type = "inbound-rtp";
                    statObject = new RTCRTPStreamStats(statMap, true);
                  } else {
                    type = "outbound-rtp";
                    statObject = new RTCRTPStreamStats(statMap, false);
                  }
                  break;
                }

              case "googCandidatePair":
                {
                  type = "candidate-pair";
                  statObject = new RTCIceCandidatePairStats(statMap);
                  break;
                }
                //              case "remotecandidate":
                //                {
                //
                //                  statObject = new RTCIceCandidateStats(statMap);
                //                  break;
                //                }
                //              case "localcandidate":
                //                {
                //                  statObject = new RTCIceCandidateStats(statMap);
                //                  break;
                //                }
            }
            if (statObject != null) {
              if (statObjectMap.get(type) == null) {
                statObjectMap.put(type, new ArrayList<RTCStatObject>());
              }
              statObjectMap.get(type).add(statObject);
            }
          }
        }
      }
    }
    if (!statObjectMap.isEmpty()) {
      for (String type : statObjectMap.keySet()) {
        //        JsonArrayBuilder tmp = Json.createArrayBuilder();
        JsonObjectBuilder tmp = Json.createObjectBuilder();
        for (RTCStatObject stat : statObjectMap.get(type)) {
          tmp.add(stat.getId(), stat.getJsonObjectBuilder());
          //          tmp.add(/*stat.getId(),*/ stat.getJsonObjectBuilder());
        }
        jsonObjectBuilder.add(type, tmp);
      }
    }
    return jsonObjectBuilder;
  }

  private static ArrayList<String> stringToArr(String value) {
    value = value.substring(1, value.length() - 1);
    ArrayList<String> arr = new ArrayList<>();
    String[] elements = value.split(",");
    for (int i = 0; i < elements.length; i++) {
      String r = "";
      if (elements[i].trim().charAt(0) == '{') {
        while (!elements[i].endsWith("}") && i < elements.length) {
          r += elements[i].trim() + ",";
          i++;
        }
        r += elements[i];
        arr.add(r);
      }
    }
    return arr;
  }

  private static Map<Object, Object> stringToMap(String value) {
    value = value.substring(1, value.length() - 1);
    String[] keyValuePairs = value.split(",");
    Map<Object, Object> map = new HashMap<>();

    for (String pair : keyValuePairs) {
      if (pair.contains("=")) {
        String[] entry = pair.split("=");
        if (entry.length == 2) {
          map.put(entry[0].trim(), entry[1].trim());
        }

      }
    }
    return map;
  }

  public JsonObject buildStatSummary(JsonObject rawStats) {
    JsonObjectBuilder mainBuilder = Json.createObjectBuilder();
    for (String type : rawStats.keySet()) {
      JsonArray arr = rawStats.getJsonArray(type);
      switch (type) {
        case "VideoBwe":
          mainBuilder.add(
              "videoBwe",
              extractData(
                  arr,
                  "googActualEncBitrate",
                  "googAvailableSendBandwidth",
                  "googTargetEncBitrate",
                  "googTransmitBitrate"));
          break;
        case "ssrc":
          JsonArray data =
              extractData(
                  arr,
                  "id",
                  "ssrc",
                  "mediaType",
                  "bytesSent",
                  "framesEncoded",
                  "googFrameRateInput",
                  "googFrameRateSent",
                  "googRtt",
                  "packetsSent",
                  "packetsReceived",
                  "packetsLost",
                  "qpSum",
                  "elapsedTime",
                  "audioInputLevel",
                  "bytesReceived",
                  "googFrameRateReceived",
                  "googJitterReceived",
                  "elapsedTime");
          JsonObjectBuilder outerBuilder = Json.createObjectBuilder();
          for (int i = 0; i < data.size(); i++) {
            JsonObject json = data.getJsonObject(i);
            JsonObjectBuilder innerBuilder = Json.createObjectBuilder();
            long elapsedTime = Long.parseLong(json.getString("elapsedTime"));
            int packetsLost = Integer.parseInt(json.getString("packetsLost"));
            for (String key : json.keySet()) {
              String value = json.getString(key);
              if (key.equals("bytesSent") || key.equals("bytesReceived")) {
                long bytes = Long.parseLong(value);
                innerBuilder.add(key, value);
                innerBuilder.add("bitrate", Long.toString(bytes * 8000 / elapsedTime));
              } else if (key.equals("packetsSent") || key.equals("packetsReceived")) {
                int packets = Integer.parseInt(value);
                innerBuilder.add(key, value);
                if (packets != 0) {
                  innerBuilder.add(
                      "packetLoss", Float.toString((float) packetsLost / packets) + '%');
                }
              } else {
                innerBuilder.add(key, value);
              }
            }
            JsonObject innerJson = innerBuilder.build();
            if (innerJson.getString("id").contains("recv")) {
              outerBuilder.add("remoteTrack[" + i + "]", innerJson);
            } else {
              outerBuilder.add("localTrack", innerJson);
            }
          }
          mainBuilder.add("ssrc", outerBuilder.build());
          break;
      }
    }
    return mainBuilder.build();
  }

  private JsonArray extractData(JsonArray data, String... desiredKeys) {
    JsonArrayBuilder arrayBuilder = Json.createArrayBuilder();
    for (int i = 0; i < data.size(); i++) {
      JsonObject json = data.getJsonObject(i);
      JsonObjectBuilder builder = Json.createObjectBuilder();
      for (String desiredKey : desiredKeys) {
        for (String key : json.keySet()) {
          if (desiredKey.equals(key)) {
            builder.add(key, json.getString(key));
          }
        }
      }
      arrayBuilder.add(builder.build());
    }
    return arrayBuilder.build();
  }

  private void printMap(HashMap<String, HashMap<String, JsonArrayBuilder>> map) {
    for (String a : map.keySet()) {
      HashMap<String, JsonArrayBuilder> innerMap = map.get(a);
      if (innerMap != null) {
        for (String k : innerMap.keySet()) {
          System.out.println(k + ": " + innerMap.get(k).build());
        }
      }
    }
  }

  private JsonObject buildFinalStats(
      HashMap<String, HashMap<String, JsonArrayBuilder>> allStatsMap) {
    JsonObjectBuilder finalBuilder = Json.createObjectBuilder();
    for (String mapKey : allStatsMap.keySet()) {
      HashMap<String, JsonArrayBuilder> map = allStatsMap.get(mapKey);
      JsonArrayBuilder innerBuilder = Json.createArrayBuilder();
      if (!map.isEmpty()) {
        HashMap<String, JsonObject> avgMap = avgValue(map);
        for (String key : avgMap.keySet()) {
          innerBuilder.add(avgMap.get(key));
        }
        finalBuilder.add(mapKey, innerBuilder.build());
      }
    }
    return finalBuilder.build();
  }

  private HashMap<String, JsonArrayBuilder> updateHashMap(
      HashMap<String, JsonArrayBuilder> hashMap, String key, JsonObject data) {
    JsonArrayBuilder arrayBuilder =
        hashMap.get(key) == null ? Json.createArrayBuilder() : hashMap.get(key);
    arrayBuilder.add(data);
    hashMap.put(key, arrayBuilder);
    return hashMap;
  }

  private HashMap<String, JsonObject> avgValue(Map<String, JsonArrayBuilder> map) {
    HashMap<String, JsonObject> newMap = new HashMap<>();
    for (String key : map.keySet()) {
      JsonArray arr = map.get(key).build();
      JsonObjectBuilder builder = Json.createObjectBuilder();
      long startTime = 0;
      long endTime;
      for (String k : arr.getJsonObject(0).keySet()) {
        long sum = 0;
        int count = 0;
        for (int i = 0; i < arr.size(); i++) {
          String value = arr.getJsonObject(i).getString(k);
          if (value.matches("\\d+") && value != null && !k.equals("timestamp")) {
            if ((k.contains("Received") || k.contains("Sent")) && !k.contains("Rate")) {
              sum = Long.parseLong(value);
              if (i == arr.size() - 1) {
                builder.add(k, Long.toString(sum));
              }

            } else {
              sum += Long.parseLong(value);
              count++;
              if (i == arr.size() - 1) {
                builder.add(k, Long.toString(sum / count));
              }
            }
          } else if (k.equals("timestamp")) {
            if (i == 0) {
              startTime = Long.parseLong(value);
            }
            if (i == arr.size() - 1) {
              endTime = Long.parseLong(value);
              if (endTime == startTime) {
                builder.add("timestamp", startTime);
              } else {
                builder.add("startTime", value);
                builder.add("endTime", value);
                builder.add("elapsedTime", Long.toString(endTime - startTime));
              }
            }
          } else {
            if (i == arr.size() - 1) {
              builder.add(k, value);
            }
          }
        }
      }
      JsonObject avgStatObject = builder.build();
      newMap.put(key, avgStatObject);
    }

    return newMap;
  }

  public String formatToJsonString(String s) {
    String r = "";
    int start;
    int end;
    boolean needSpecialFormat = false;
    for (int i = 0; i < s.length(); i++) {
      String toBeAppended = "";
      char c = s.charAt(i);
      char nextChar = (i != s.length() - 1 ? s.charAt(i + 1) : '\0');
      if (c == '{' || (c == ' ' && nextChar != '{' && nextChar != '[')) {
        start = i + 1;
        end = start + 1;
        while (end < s.length()) {
          if (s.charAt(end) == ':') {
            i = end - 1;
            break;
          }
          end++;
        }
        String key = s.substring(start, end);
        if (key.equals("googTimingFrameInfo")) needSpecialFormat = true;
        toBeAppended = '"' + key + '"';
      } else if (c == ':') {
        start = i + 1;
        end = start + 1;
        if (!needSpecialFormat) {
          while (end < s.length()) {
            if (s.charAt(end) == '}' || s.charAt(end) == ',') {
              i = end - 1;
              break;
            }
            end++;
          }
        } else {
          needSpecialFormat = false;
          while (end < s.length()) {
            if (s.charAt(end) == ' ') {
              i = end - 2;
              end = end - 1;
              break;
            }
            end++;
          }
        }
        String value = s.substring(start, end);

        toBeAppended = '"' + value + '"';
      }
      r += c + toBeAppended;
    }
    return r;
  }

  private JsonObject stringToJsonObject(String s) {
    JsonReader r = Json.createReader(new StringReader(s));
    return r.readObject();
  }

  private JsonArray stringToJsonArray(String s) {
    JsonReader r = Json.createReader(new StringReader(s));
    return r.readArray();
  }
}
