model file_saver

import "GraphLink.gaml"
import "GraphVertex.gaml"

/**
 * Helper class to save data output to external file.
 * 
 * @author Hieu Chu (chc116@uowmail.edu.au)
 * @author Damien Vergnet
 */
species file_saver {
  string dir <- "../results/output";
  file result_dir <- folder(dir);
  string node_stats_file <- dir + "/node-stats.tsv";
  string edge_stats_file <- dir + "/edge-stats.tsv";
  string matrix_stats_file <- dir + "/matrix-stats.txt";

  string separator <- "\t";

  /** Writes file header only once. */
  init {
    do clear_file();
    do write_node_header();
    do write_edge_header();
  }

  /** Saves data stats output every cycle. */
  reflex save_output {
    do write_node_output();
    do write_edge_output();
    do write_matrix_output();

    // Reset counts
    ask graph_link.population {
      do reset_counts();
    }
    ask graph_vertex.population {
      do reset_counts();
    }

    write world.time;
  }

  /** Clears file before writing data. */
  action clear_file {
    loop txt_file over: result_dir {
      // Empty file before simulation runs.
      save to: (string(result_dir) + "/" + txt_file) type: "text" rewrite: true;
    }
  }

  /** Writes node stats file header. */
  action write_node_header {
    do _write_file_header(graph_vertex.population, node_stats_file);
  }

  /** Writes edge stats file header. */
  action write_edge_header {
    do _write_file_header(graph_link.population, edge_stats_file);
  }

  /**
   * Writes the header for the given agents list.
   * 
   * @param agents_list The list of agents.
   * @param file_name The name of the file to write to.
   */
  action _write_file_header(list<counter> agents_list, string file_name) {
    string header <- "time" + separator;
    string last_name <- agents_list[length(agents_list) - 1].name;

    ask agents_list {
      header <- header + self.name;
      if (self.name != last_name) {
        header <- header + myself.separator;
      }
    }

    save header to: file_name type: "text";
  }

  /** Writes orig_dest matrix stats. */
  action write_matrix_output {
    string matrix_header <- "Time: " + string(world.time) + "\n";

    loop i from: 0 to: length(world.orig_dest_matrix) - 1 {
      loop j from: 0 to: length(world.orig_dest_matrix[i]) - 1 {
        matrix_header <- matrix_header + world.orig_dest_matrix[i][j];
        if (j != length(world.orig_dest_matrix[i]) - 1) {
          matrix_header <- matrix_header + ",";
        }
      }

      matrix_header <- matrix_header + "\n";
    }

    save matrix_header to: matrix_stats_file type: "text" rewrite: false;
  }

  /** Writes node stats output in a row. */
  action write_node_output {
    do _write_data(graph_vertex.population, node_stats_file);
  }

  /** Writes edge stats output in a row. */
  action write_edge_output {
    do _write_data(graph_link.population, edge_stats_file);
  }

  /**
   * Writes the location and speed of all mobile agents in that interacted
   * with the given counting agents.
   * 
   * @param agents_list The list of agents.
   * @param file_name The name of the file to write to.
   */
  action _write_data(list<counter> agents_list, string file_name) {
    string output <- string(world.time) + separator;
    string last_name <- agents_list[length(agents_list) - 1].name;

    ask agents_list {
      list<string> data <- self.get_data();

      if (!empty(data)) {
        int last_index <- length(data) - 1;
        loop i from: 0 to: last_index {
          output <- output + data[i];
          if (i != last_index) {
            output <- output + "|";
          }
        }
      }

      if (self.name != last_name) {
        output <- output + myself.separator;
      }
    }

    save output to: file_name type: "text" rewrite: false;
  }
}
