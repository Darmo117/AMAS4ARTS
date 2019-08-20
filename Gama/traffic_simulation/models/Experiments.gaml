model Experiments

import "Global.gaml"

/**
 * GAMA experiment, includes inspect variables view that user can dyanmically change the value during simulation
 * and data visualization charts, live monitoring of variables.
 */
experiment traffic_simulation type: gui {
  parameter "Road network capacity scale down" var: capacity_scale category: "Road network";
  parameter "Road network shape file" var: _shape_file_roads category: "File";

  output {
    display main_display type: java2D background: #white {
      species graph_link aspect: base;
      species graph_vertex aspect: base;
      species mobile_entity aspect: base;
      event [mouse_down] action: show_shortest_path;
    }

    display traffic_density_chart {
      chart "Traffic density count series"
          type: series
          size: {1, 0.5}
          position: {0, 0}
          x_label: "Cycle"
          y_label: "Count" {
        data "Low" value: _low_count color: #lime style: line;
        data "Moderate" value: _moderate_count color: #blue style: line;
        data "High" value: _high_count color: #yellow style: line;
        data "Extreme" value: _extreme_count color: #orange style: line;
        data "Traffic jam" value: _traffic_jam_count color: #red style: line;
      }

      chart "Traffic density pie chart"
          type: pie
          style: exploded
          size: {0.5, 0.5}
          position: {0, 0.5} {
        data "Low" value: _low_count color: #lime;
        data "Moderate" value: _moderate_count color: #blue;
        data "High" value: _high_count color: #yellow;
        data "Extreme" value: _extreme_count color: #orange;
        data "Traffic jam" value: _traffic_jam_count color: #red;
      }

      chart "Traffic density bar chart"
          type: histogram
          size: {0.5, 0.5}
          position: {0.5, 0.5}
          x_label: "Traffic density category"
          y_label: "Count" {
        data "Low" value: _low_count color: #lime;
        data "Moderate" value: _moderate_count color: #blue;
        data "High" value: _high_count color: #yellow;
        data "Extreme" value: _extreme_count color: #orange;
        data "Traffic jam" value: _traffic_jam_count color: #red;
      }
    }

    display speed_chart {
      chart "Average speed series"
          type: series
          size: {1, 0.5}
          position: {0, 0.25}
          x_label: "Cycle"
          y_label: "Average speed (m/s)" {
        data "Average speed" value: avg_speed color: #deepskyblue;
      }
    }

    monitor "Low density road count" value: _low_count color: #lime;
    monitor "Moderate density road count" value: _moderate_count color: #blue;
    monitor "High density road count" value: _high_count color: #yellow;
    monitor "Extreme density road count" value: _extreme_count color: #orange;
    monitor "Traffic jam count" value: _traffic_jam_count color: #red;
    monitor "Current number of people" value: nb_mobile_entities;
    monitor "Number of trips completed" value: nb_trips_completed;
    monitor "Average speed" value: avg_speed with_precision 2 color: #deepskyblue;
    monitor "Accumulated number of people who can't find path" value: num_mobile_entities_cant_find_path color: #brown;
  }
}
