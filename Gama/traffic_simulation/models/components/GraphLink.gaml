model graph_link

import "GraphVertex.gaml"
import "CountingAgent.gaml"
import "../Global.gaml"

/**
 * Edge species definition with interactive blocking and unblocking.
 * 
 * @author Hieu Chu (chc116@uowmail.edu.au)
 * @author Damien Vergnet
 */
species graph_link parent: counter {
  float real_length;
  float free_flow_speed;

  int max_capacity;
  int current_volume;
  /** Percentage of this link's fullness to determine its status. */
  float _link_full_percentage -> {current_volume / max_capacity};
  string status <- "low" among: ["low", "moderate", "high", "extreme", "traffic_jam"];
  bool is_jammed -> {current_volume = max_capacity};

  bool blocked;
  bool hidden;

  rgb _color;

  // User command section.
  user_command "Block" action: block;
  user_command "Unblock" action: unblock;
  user_command "View Direction" action: view_direction;

  init {
    current_volume <- 0;
    blocked <- false;
    hidden <- false;
  }

  /**
   * Blocks or unblocks this link depending on the argument.
   * 
   * @param block If true, this link will be blocked.
   */
  action _block_link(bool block) {
    blocked <- block;
    // Generate new graph with or without the edge just selected.
    ask world {
      do generate_road_graph();
    }

    // Global switch toggle.
    world.change_graph_action <- true;
    ask mobile_entity.population {
      // Handle mobile entities with smart re-route strategy,
      // their link graph will be updated to discard blocked link too.
      if (self.modified_graph != nil and empty(self.avoided_links)) {
        self.modified_graph <- directed(
          as_edge_graph(graph_link.population where (!(self.avoided_links contains each) and !each.hidden and !each.blocked))
          with_weights (graph_link.population as_map (each :: each.real_length))
        );
      }

      if (!block or !self.cant_find_path) {
        bool next_link_is_blocked <- false;

        if (self.current_link != nil and self.current_link.shape = myself.shape) {
          // Mobile entities that are stuck in middle of the link.
          if (self.is_on_node) {
            next_link_is_blocked <- true;
          } // Mobile entities that have their next link blocked.
          else {
            self.is_in_blocked_link <- block;
          }
        }

        // Only re-route mobile entities who have a radio or have their next link blocked.
        if (!self.is_in_blocked_link and (self.has_radio or next_link_is_blocked)) {
          path new_shortest_path;

          try {
            new_shortest_path <- world.generate_path(self);
          }
          catch {
            self.cant_find_path <- true;
            if (!block and self.is_on_node) {
              self.speed <- 0 °m / °s;
            }
          }

          if (new_shortest_path != nil and new_shortest_path.shape != nil and
              new_shortest_path.shape.points[length(new_shortest_path.shape.points) - 1] != self.dest) {
            self.cant_find_path <- true;
          }

          // Update shortest path related variables to travel correctly.
          if (!self.cant_find_path) {
            if (new_shortest_path = nil or empty(new_shortest_path.edges)) {
              self.cant_find_path <- true;
            }
            else {
              self.fixed_edges <- new_shortest_path.edges collect (graph_link(each));
              self.num_nodes_to_complete <- length(self.fixed_edges);
              self.current_link_index <- 0;
              if (!block) {
                self.cant_find_path <- false;
              }
              self.current_link <- self.fixed_edges[self.current_link_index];
              self.shortest_path <- new_shortest_path;
            }
          }

          // Handle number of people who cannot find shortest path
          if (self.cant_find_path and self.is_on_node) {
            world.num_mobile_entities_cant_find_path <- world.num_mobile_entities_cant_find_path + 1;
            self.speed <- 0 #m / #s;
          }
        }
      }
    }
  }

  /**
   * Interactive block function (Right click selected link -> Apply Block function).
   */
  action block {
    do _block_link(true);

    // Ask mobile entities in blocked link that have their destination inside the blocked link.
    ask mobile_entity.population where (each.is_in_blocked_link and each.is_on_last_link and !each.moving_in_blocked_link) {
      self.moving_in_blocked_link <- true;
      create graph_link {
        shape <- curve(myself.shape.points[0], myself.shape.points[length(myself.shape.points) - 1], world.curve_width_eff);
        real_length <- myself.current_link.real_length;
        free_flow_speed <- myself.current_link.free_flow_speed;
        max_capacity <- myself.current_link.max_capacity;
        hidden <- true;
      }
      // Since the global graph has the blocked link removed, in order for these people to move in their last link
      // we will assign them a virtual graph with only that link, after finishing this link they will finish their trip.
      self.mini_graph <- directed(
        as_edge_graph([graph_link.population[length(graph_link.population) - 1]])
        with_weights ([graph_link.population[length(graph_link.population) - 1]] as_map (each :: each.real_length))
      );
    }

    // Ask mobile entities in blocked link that have their destination outside of the blocked link.
    // Correctly update speed for data stats.
    ask mobile_entity.population where (each.is_in_blocked_link and !each.is_on_last_link) {
      self.speed <- 0 #m / #s;
    }
  }

  /**
   * Interactive unblock function (Right click selected link -> Apply Unblock function).
   */
  action unblock {
    do _block_link(false);
  }

  /**
   * Helper function to view the direction of a link.
   */
  action view_direction {
    int len <- length(shape.points);
    write "source: " + graph_vertex(shape.points[0]).node_number
          + ", dest: " + graph_vertex(shape.points[len - 1]).node_number;
  }

  aspect base {
    if (_link_full_percentage < 0.25) {
      _color <- #lime;
      status <- "low";
    }
    else if (_link_full_percentage < 0.5) {
      _color <- #blue;
      status <- "moderate";
    }
    else if (_link_full_percentage < 0.75) {
      _color <- #yellow;
      status <- "high";
    }
    else if (_link_full_percentage < 1) {
      _color <- #orange;
      status <- "extreme";
    }
    else {
      _color <- #red;
      status <- "traffic_jam";
    }

    if (blocked) {
      _color <- #purple;
    }

    if (!hidden) {
      draw shape color: _color width: 3;
      if (world.draw_names) {
        draw name color: _color font: font('Arial', 12, #plain);
      }
    }
  }
}
