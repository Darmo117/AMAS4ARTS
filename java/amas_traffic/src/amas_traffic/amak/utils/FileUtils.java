package amas_traffic.amak.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import amas_traffic.amak.MobileEntitySnapshot;
import amas_traffic.amak.TrafficAmas;
import amas_traffic.amak.TrafficSummary;
import amas_traffic.amak.agents.MobileEntity;
import amas_traffic.amak.agents.network.Edge;
import msi.gama.metamodel.shape.GamaPoint;
import msi.gama.metamodel.shape.ILocation;
import msi.gama.runtime.IScope;
import msi.gama.util.file.GamaCSVFile;
import msi.gama.util.file.GamaFile;
import msi.gama.util.file.GamaGeoJsonFile;
import net.darmo_creations.json.JsonEntity;
import net.darmo_creations.json.JsonObject;
import net.darmo_creations.json.JsonString;
import net.darmo_creations.json.parser.JsonManager;
import net.darmo_creations.json.parser.JsonParseException;

public final class FileUtils {
  public static List<DummyObservationZone> getObservationZones(GamaGeoJsonFile file, IScope scope) {
    List<DummyObservationZone> dummyOZs = new ArrayList<>();
    JsonObject root = JsonManager.parse(fileContents(file, scope).toString());

    for (JsonEntity entity : root.getArray("observation_zones")) {
      JsonObject oz = (JsonObject) entity;
      String name = oz.getString("name");
      String[] nodeIds = oz.getArray("nodes").stream().map(e -> ((JsonString) e).get()).toArray(String[]::new);
      String[] edgeIds = oz.getArray("edges").stream().map(e -> ((JsonString) e).get()).toArray(String[]::new);
      dummyOZs.add(new DummyObservationZone(name, nodeIds, edgeIds));
    }

    return dummyOZs;
  }

  public static TrafficSummary extractTrafficData(GamaCSVFile file, IScope scope) {
    TrafficSummary summary = new TrafficSummary();
    String[] header = null;
    int limit = 0;

    for (String line : fileContents(file, scope).split("\n")) { // lines
      String[] entries = line.split("\t", limit);
      if (header == null) {
        header = entries;
        limit = header.length;
      }
      else {
        double timestamp = Double.parseDouble(entries[0]);
        summary.put(timestamp, new HashMap<>());
        for (int i = 1; i < entries.length; i++) { // columns
          List<MobileEntitySnapshot> snapshots = new ArrayList<>();
          for (String s : entries[i].split("\\|")) { // entities in column
            String[] data = s.split(";");
            if (data.length == 3) {
              snapshots.add(new MobileEntitySnapshot(data[0], extractLocation(data[1]), Double.parseDouble(data[2])));
            }
          }
          summary.get(timestamp).put(header[i], snapshots);
        }
      }
    }

    return summary;
  }

  public static void writeCSV(TrafficAmas amas, String filename) {
    List<String> lines = new ArrayList<>();

    for (Edge e : amas.edges(null)) {
      lines.add(String.format(Locale.ENGLISH, "%s,%f,%d", e.getName(), e.getCongestion(), //
          e.getMobileEntities().size()));
    }
    writeCSVFile("edge,congestion,entities nb", lines, String.format(filename, "edges"));

    lines.clear();
    for (MobileEntity me : amas.mobileEntities(null)) {
      lines.add(String.format("%s,%s", me.getName(), me.getNextStep().getName()));
    }
    writeCSVFile("mobile entity,next node", lines, String.format(filename, "mobile_entities"));
  }

  private static void writeCSVFile(String header, List<String> lines, String filename) {
    lines.add(0, header);
    System.out.println(Paths.get(filename));
    try {
      Files.write(Paths.get(filename), lines, StandardCharsets.UTF_8);
    }
    catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private static final String NUMBER = "(-?\\d+\\.\\d+(?:[Ee][+-]?\\d+)?)";
  private static final Pattern POINT_REGEX = Pattern
      .compile(String.format("^\\{%1$s,%1$s,%1$s\\}$", NUMBER, NUMBER, NUMBER));

  private static ILocation extractLocation(String s) {
    Matcher m = POINT_REGEX.matcher(s);
    if (m.matches()) {
      return new GamaPoint(Double.parseDouble(m.group(1)), Double.parseDouble(m.group(2)),
          Double.parseDouble(m.group(3)));
    }
    throw new RuntimeException("Point format error.");
  }

  private static String fileContents(GamaFile<?, ?> file, IScope scope) {
    StringJoiner contents = new StringJoiner("\n");

    try (BufferedReader br = new BufferedReader(new FileReader(file.getFile(scope)));) {
      String line;
      while ((line = br.readLine()) != null) {
        contents.add(line);
      }
    }
    catch (IOException | JsonParseException e) {
      throw new RuntimeException(e);
    }
    return contents.toString();
  }

  private FileUtils() {
  }
}
