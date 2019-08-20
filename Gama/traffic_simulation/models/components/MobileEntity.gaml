model mobile_entity

import "GraphLink.gaml"

/** 
 * People species definition with smart re-routing strategy function.
 * 
 * @author Hieu Chu (chc116@uowmail.edu.au)
 * @author Damien Vergnet
 */
species mobile_entity skills: [moving] { // people
  rgb _color;
  /** Destination node. */
  point dest; // Do not rename as 'destination'!
  bool has_target;
  path shortest_path;
  /** The list of links to browse to get to the destination. */
  list<graph_link> fixed_edges;
  /** Number or nodes in the shortest path. */
  int num_nodes_to_complete;
  /** Index of the current link in the path. */
  int current_link_index;
  bool is_on_last_link -> {current_link_index = num_nodes_to_complete - 1};
  point next_node;
  /** Indicates whether people agent is on a node. */
  bool is_on_node;
  /** If true, the shortest path will be shown. */
  bool clicked;
  /** Ratio between 2D gama graph and real world link length distance. */
  float ratio;
  /** Real world distance to the next node. */
  float real_dist_to_next_node;
  graph_link current_link;
  graph_vertex current_node;
  /** Indicates whether the next link is at full capacity. */
  bool is_link_jammed;
  /** Indicates whether this agent is in a blocked link. */
  bool is_in_blocked_link;
  /** Indicates whether this agent cannot find a path to its destination. */
  bool cant_find_path;
  /** Indicates whether this agent has a radio (will be notified of link block changes globally). */
  bool has_radio;
  /** Graph used if moving_in_blocked_link is true. */
  graph mini_graph;
  /** Graph for people who use re-route strategy. */
  graph modified_graph;
  /** List of edges that mobile entities who use re-route strategy will avoid (congested). */
  list<graph_link> avoided_links;
  /** Indicates whether this agent is in blocked link but can still move to its destination because it is on the same link. */
  bool moving_in_blocked_link;
  /** Indicates whether this agent has a smart reroute strategy. */
  bool has_smart_strategy;
  /** Alpha weights for reroute strategy. */
  float alpha;
  /** Theta weights for reroute strategy. */
  float theta;
  /** Real time spent in the simulation. */
  float real_time_spent;
  /** Free flow time needed to travel to next node. */
  float free_flow_time_needed;
  /** Free flow time spent in the simulation. */
  float free_flow_time_spent;

  init {
    current_link_index <- 0;
    is_on_node <- true;
    clicked <- false;
    ratio <- 1.0;
    is_link_jammed <- true;
    is_in_blocked_link <- false;
    cant_find_path <- false;
    has_radio <- flip(world.radio_prob);
    avoided_links <- [];
    moving_in_blocked_link <- false;
    has_smart_strategy <- flip(world.smart_strategy_prob);
    real_time_spent <- 0.0;
    free_flow_time_needed <- 0.0;
    free_flow_time_spent <- 0.0;
    has_target <- false;
  }

  /**
   * Reflex function to attempt to re-route when stuck on a node and can't find path.
   */
  reflex reroute_attempt when: cant_find_path and is_on_node {
    try {
      shortest_path <- world.generate_path(self);
    }
    catch {
      speed <- 0 °m / °s;
      return;
    }

    if (shortest_path_is_wrong()) {
      speed <- 0 °m / °s;
    }
    else {
      do _update_edges(shortest_path);
      cant_find_path <- false;
    }
  }

  reflex update {
    if (is_on_node) {
      ask graph_vertex(next_node) {
        do add_data(myself);
      }
    }
    else {
      ask current_link {
        do add_data(myself);
      }
    }
  }

  /**
   * Main reflex function that enables selected people based on criteria to travel in the simulation.
   */
  reflex smart_move when: (!cant_find_path or cant_find_path and !is_on_node)
                          and !is_link_jammed
                          and (!is_in_blocked_link or is_in_blocked_link and is_on_last_link) {
    float epsilon <- 1e-5; // epsilon is used when the distance calculation with multiple decimals might cause error.

    if (name = "mobile_entity41") { // XXX debug
      write "move";
    }

    do follow path: shortest_path; // Move according to the shortest path.
    is_on_node <- false;
    // Get current agent's graph network to calculate distance.
    graph current_graph <- world.road_graph;

    if (mini_graph != nil) {
      current_graph <- mini_graph;
    }
    else if (modified_graph != nil) {
      current_graph <- modified_graph;
    }

    // Handle epsilon (where calculation between float values with multiples decimals might cause error).
    float distance_to_next_node;

    using topology(current_graph) {
      distance_to_next_node <- self distance_to next_node;
    }

    real_dist_to_next_node <- distance_to_next_node * ratio;
    if (real_dist_to_next_node < epsilon or (self distance_to next_node) * ratio < epsilon) {
      location <- next_node.location;
    }

    // Check if after moving, the people agent ends up on the node.
    if ((self overlaps next_node) or (self overlaps dest)) {
      current_link.current_volume <- current_link.current_volume - 1;
      // Accumulate previous free flow time calculated from previous node.
      free_flow_time_spent <- free_flow_time_spent + free_flow_time_needed;

      if (has_target) {
        // Original code. Executed if this entity is on an edge or has a final target.
        if (is_on_last_link or (self overlaps dest)) {
          world.nb_trips_completed <- world.nb_trips_completed + 1;
          do die();
        }
        else {
          is_on_node <- true;
          is_link_jammed <- true;
          current_link_index <- current_link_index + 1;
          current_link <- fixed_edges[current_link_index];

          if (cant_find_path) {
            do _stop_cant_find_path();
          }
          else {
            if (current_link.blocked) {
              do generate_new_path();
            }
            if (has_smart_strategy and is_on_node) {
              do _will_reroute(
                real_time_spent / free_flow_time_spent,
                current_link.current_volume / current_link.max_capacity
              );
            }
          }
        }
      }
//      else {
//        is_on_node <- true;
//        list target <- get_target_from_amas(name);
//        has_target <- bool(target[1]);
//        dest <- one_of(graph_vertex.population where (each.name = target[0])).location;
//        if (name = "mobile_entity41") { // XXX debug
//          write "set dest: " + target[0] + " (" + dest + ")";
//        }
//        do _update_edges(shortest_path);
//      }
    }
  }

  /**
   * Tries to re-route this agent by generating a new path.
   */
  action generate_new_path {
    try {
      shortest_path <- world.generate_path(self);
    }
    catch {
      do _stop_cant_find_path();
      return;
    }

    if (shortest_path_is_wrong()) {
      do _stop_cant_find_path();
    }
    else {
      do _update_edges(shortest_path);
    }
  }

  /**
   * Function to compute the output whether the people agent will reroute (choose different path),
   * based on current local conditions passed through parameters
   * 
   * @params normalized_time_spent it's the real time spent divided by free flow time spent
   * @params next_link_saturation it's the current volume divided by max_capacity
   */
  action _will_reroute(float normalized_time_spent, float next_link_saturation) {
    float epsilon <- 1e-5;

    if (normalized_time_spent < 1 and 1 - normalized_time_spent <= epsilon) {
      normalized_time_spent <- 1.0;
    }

    // Re-route strategy formula (Heavistep function).
    float value <- cos(alpha °to_deg) * normalized_time_spent + sin(alpha °to_deg) * next_link_saturation - theta;

    // If output = 1, will attempt to avoid congested link.
    if (value >= 0) {
      world.total_reroute_count <- world.total_reroute_count + 1;
      graph new_graph <- directed(
        as_edge_graph(graph_link.population where (!each.hidden and !each.blocked and each != current_link))
        with_weights (graph_link.population as_map (each :: each.real_length))
      );

      // Try to compute new shortest path avoiding current link if possible.
      path new_shortest_path;
      try {
        new_shortest_path <- path_between(new_graph, location, dest);
      }
      catch {
        return;
      }

      bool path_is_wrong <-
        new_shortest_path != nil and new_shortest_path.shape != nil and (
          new_shortest_path.shape.points[0] != location or
          new_shortest_path.shape.points[length(new_shortest_path.shape.points) - 1] != dest
        ) or new_shortest_path = nil or empty(new_shortest_path.edges);

      if (!path_is_wrong) {
        do _update_edges(new_shortest_path);
        cant_find_path <- false;
        add current_link to: avoided_links;
        shortest_path <- new_shortest_path;
        modified_graph <- new_graph;
      }
    }
  }

  /**
   * Increments the global "num_people_cant_find_path" variable, sets the cant_find_path
   * variable to true and sets the speed to 0.
   */
  action _stop_cant_find_path {
    world.num_mobile_entities_cant_find_path <- world.num_mobile_entities_cant_find_path + 1;
    cant_find_path <- true;
    speed <- 0 #m / #s;
  }

  /**
   * Updates the edges and current link from the given path.
   * 
   * @param a_path The path to extract the data from (default: shortest_path).
   */
  action _update_edges(path a_path) {
    fixed_edges <- a_path.edges collect graph_link(each);
    num_nodes_to_complete <- length(fixed_edges);
    current_link_index <- 0;
    current_link <- fixed_edges[current_link_index];
  }

  /**
   * Tells whether the current shortest path is wrong.
   */
  bool shortest_path_is_wrong {
    return shortest_path != nil and shortest_path.shape != nil and (
             shortest_path.shape.points[0] != location or
             shortest_path.shape.points[length(shortest_path.shape.points) - 1] != dest
           ) or shortest_path = nil or empty(shortest_path.edges);
  }

  action update_current_node {
    current_node <- one_of(graph_vertex.population where (self overlaps each));
    if (name = "mobile_entity41") { // XXX debug
      write self;
      write current_node;
    }
  }

  aspect base {
    if (is_in_blocked_link and is_on_last_link) {
      if (clicked) {
        draw circle(2) at: location color: #yellow;
        draw circle(2) at: dest color: #cyan;
        draw polyline([location, dest]) color: #orchid width: 5;
      }
    }
    else if (clicked and !cant_find_path) {
      path new_path <- world.generate_path(self);
      draw circle(2) at: point(new_path.source) color: #yellow;
      draw circle(2) at: point(new_path.target) color: #cyan;
      draw new_path.shape color: #orchid width: 5;
    }

    if (has_radio and has_smart_strategy) {
      _color <- #lightcoral;
    }
    else if (has_radio) {
      _color <- #darkviolet;
    }
    else if (has_smart_strategy) {
      _color <- #saddlebrown;
    }
    else {
      _color <- #dimgray;
    }

    geometry g <- is_in_blocked_link ? triangle(65) : circle(50);

    draw g color: _color;
    if (world.draw_names) {
      draw name color: _color font: font('Arial', 10, #plain);
    }
  }
}
