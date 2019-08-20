model smart_traffic_model

import "components/GraphVertex.gaml"
import "components/GraphLink.gaml"
import "components/MobileEntity.gaml"
import "components/FileSaver.gaml"

/**
 * Global species that determine the environment for the simulation
 * 
 * @author Hieu Chu (chc116@uowmail.edu.au)
 * @author Damien Vergnet
 */
global {
  /** Seed for the random numbers generator. */
  float seed <- 1.0;
  /** If true, the names of all agents will be drawn. */
  bool draw_names <- true;

  /** Road network graph. */
  graph road_graph;
  /** Global action (block or unblock has been applied). */
  bool change_graph_action <- false;
  /** Width of the curve for road representation. */
  float curve_width_eff <- 0.25;
  /** Shape file for generating the road network. */
  file _shape_file_roads <- file("../input_data/network_links.shp");
  geometry shape <- envelope(_shape_file_roads);

  /** Initial number of people. */
  int start_nb_people <- 200 min: 0 max: 500;
  /** Probability that a people agent will have global radio. */
  float radio_prob <- 0.5 min: 0.0 max: 1.0;
  /** Probability that a people agent will have a smart re-route strategy. */
  float smart_strategy_prob <- 0.51 min: 0.0 max: 1.0;

  int _low_count -> {length(graph_link where (each.status = "low"))};
  int _moderate_count -> {length(graph_link where (each.status = "moderate"))};
  int _high_count -> {length(graph_link where (each.status = "high"))};
  int _extreme_count -> {length(graph_link where (each.status = "extreme"))};
  int _traffic_jam_count -> {length(graph_link where (each.status = "traffic_jam"))};

  /** Scale down the max capacity of roads in road network. */
  int capacity_scale <- 300 min: 1;

  /** Current number of mobile entities. */
  int nb_mobile_entities -> {length(mobile_entity.population)};
  /** Number of trips completed. */
  int nb_trips_completed <- 0;
  /** For speed chart. */
  list<float> _speed_list -> {mobile_entity.population collect each.speed};
  /** Average speed stats. */
  float avg_speed -> {mean(_speed_list)};
  /** Total number of times a re-route strategy has been successfully applied. */
  int total_reroute_count <- 0;

  /** Acumulated number of people who can't find a path during the simulation. */
  int num_mobile_entities_cant_find_path <- 0;

  /** 2D array for storing origin/destination matrix. */
  list<list<int>> orig_dest_matrix;
  bool write_output <- true;

  init {
    // Load road network from shape file.
    create graph_link from: _shape_file_roads with: [
      name :: "edge" + read("ID"),
      real_length :: float(read("length")),
      free_flow_speed :: float(read("freespeed")),
      // Scale down capacity of edges based on global capacity scale ratio.
      max_capacity :: int(read("capacity")) / capacity_scale
    ];

    ask graph_link.population {
      // Change shape of road to curve to select block and unblock individually for bidirectional road.
      self.shape <- curve(
        self.shape.points[0], self.shape.points[length(self.shape.points) - 1],
        myself.curve_width_eff
      );

      // Create nodes between roads (non-duplicate check).
      point start <- self.shape.points[0];
      point end <- self.shape.points[length(self.shape.points) - 1];

      if (empty(graph_vertex.population overlapping start)) {
        create graph_vertex {
          location <- start;
        }
      }

      if (empty(graph_vertex.population overlapping end)) {
        create graph_vertex {
          location <- end;
        }
      }
    }

    list<float> alpha_arr;
    list<float> theta_arr;
    list<list<float>> res <- _extract_wheights();
    alpha_arr <- res[0];
    theta_arr <- res[1];

    // Populate origin/destination matrix based on number of nodes.
    int max_len <- length(graph_vertex.population);
    orig_dest_matrix <- list_with(max_len, list_with(max_len, 0));

    do generate_road_graph();
    do _batch_create_mobile_entities(start_nb_people, alpha_arr, theta_arr);

    if (write_output) {
      create file_saver number: 1;
    }

    init_amas road_graph: road_graph
              nodes: graph_vertex.population
              edges: graph_link.population
              oz_file: file("../input_data/observation_zones.json")
              traffic_file: file("../input_data/edge_stats.tsv");

    ask mobile_entity.population {
      do update_current_node();
    }
    launch_amas_resolving mobile_entities: mobile_entity.population;
    ask mobile_entity.population { // Initialize all mobile entities next steps.
      list target <- get_target_from_amas(name);
      write name + ": " + target[0]; // XXX debug
      self.has_target <- bool(target[1]);
      self.dest <- one_of(graph_vertex.population where (each.name = target[0])).location;
      do generate_new_path();
    }
  }

  /**
   * Loads alpha and theta values from the strategy file so, when new mobile entities are created,
   * they will have a random strategy from the input file.
   * 
   * @return The list of alpha coefficients for smart re-route
   *         and the list of theta coefficients for smart re-route.
   */
  list<list<float>> _extract_wheights {
    list<float> alpha_arr <- [];
    list<float> theta_arr <- [];
    bool header <- true;

    loop line over: text_file("../input_data/strategies.txt") {
      if (!header) {
        list<string> split_line <- line split_with ",";
        add float(split_line[0]) to: alpha_arr;
        add float(split_line[1]) to: theta_arr;
      }

      header <- false;
    }
    return [alpha_arr, theta_arr];
  }

  /**
   * Function to batch generate mobile agents with valid origin and destination 
   * (ensure that a shortest path is found).
   * 
   * @param number Number of mobile agents to be created.
   * @param alpha_arr List of alpha coefficients for smart re-route.
   * @param theta_arr List of theta coefficients for smart re-route.
   */
  action _batch_create_mobile_entities(int number, list<float> alpha_arr, list<float> theta_arr) {
    if (number > 0) {
      loop i from: 0 to: number - 1 {
        loop while: !_create_mobile_entity(alpha_arr, theta_arr) {}
      }
    }
  }

  /**
   * Private helper function to create a single mobile agent with random origin and destination
   * and generate the shortest path, increment the value in origin - destination matrix,
   * feed weights to mobile agents who have a smart strategy.
   * 
   * @param alpha_arr List of alpha coefficients for smart re-route.
   * @param theta_arr List of theta coefficients for smart re-route.
   * @return True if the mobile agent can find a shortest path, false otherwise.
   */
  bool _create_mobile_entity(list<float> alpha_arr, list<float> theta_arr) {
    bool result <- true;

    create mobile_entity {
      // Generate random origin, destination and shortest path between them.
      int random_origin_index <- rnd(length(graph_vertex.population) - 1);
      int random_dest_index <- rnd(length(graph_vertex.population) - 1);

      loop while: random_origin_index = random_dest_index {
        random_dest_index <- rnd(length(graph_vertex.population) - 1);
      }

      self.location <- graph_vertex.population[random_origin_index].location;
      self.dest <- nil;

      if (self.has_smart_strategy) {
        int random_strat_index <- rnd(length(alpha_arr) - 1);
        self.alpha <- alpha_arr[random_strat_index];
        self.theta <- theta_arr[random_strat_index];
      }

      self.speed <- 0 °m / °s;
      ask graph_vertex(self.location) {
      	do add_data(myself);
      }
    }

    return result;
  }

  reflex run_amak {
    write "run_amak"; // XXX debug
    ask mobile_entity.population {
      do update_current_node();
    }
    launch_amas_resolving mobile_entities: mobile_entity.population;
    ask mobile_entity.population where (!each.has_target and (each overlaps each.dest)) {
      self.is_on_node <- true;
      list target <- get_target_from_amas(name);
      write "target: " + target;
      if (target != nil) {
        self.has_target <- bool(target[1]);
        self.dest <- one_of(graph_vertex.population where (each.name = target[0])).location;
        if (self.name = "mobile_entity41") { // XXX debug
          write "set dest: " + target[0] + " (" + dest + ")";
        }
        do _update_edges(self.shortest_path);
      }
    }
  }

  /**
   * Reflex function that will select the minimum time of all mobile agents to reach their immediate
   * next node and set the global time equal to that. This preprocessing function will ensure that 
   * mobile agents variable are set correctly before they are able to travel.
   */
  reflex update_min_time {
    write "update_min_time"; // XXX debug
    if (nb_mobile_entities = 0) {
      return;
    }

    // List of times to arrive to next node.
    list<float> time_list <- [];
    /*
     * Selected mobile entities criteria:
     * (
     *  Can find the shortest path
     *  OR
     *  Can't find the shortest path but is in the middle of the road
     *  (agent will move to the nearest next node before getting stuck)
     * )
     * AND
     * (
     *  Is not in the blocked road
     *  OR
     *  Is in the blocked road but the destination is on the same road
     *  (other end of the road)
     * )
     */
    list<mobile_entity> selected_entities <- mobile_entity.population where (
      (!each.cant_find_path or each.cant_find_path and !each.is_on_node) and
      (!each.is_in_blocked_link or each.is_in_blocked_link and each.is_on_last_link)
    );

    ask selected_entities {
      if (name = "mobile_entity41") { // XXX debug
        write self overlaps self.dest;
        write self.has_target;
      }
      if (self overlaps self.dest) {
        myself.nb_trips_completed <- myself.nb_trips_completed + 1;
        do die();
      }

      self.current_link <- self.fixed_edges[self.current_link_index];
      int len <- length(self.current_link.shape.points);
      self.next_node <- self.current_link.shape.points[len - 1];
      float true_link_length <- self.current_link.real_length;
      float distance_to_next_node;
      graph current_graph;

      if (name = "mobile_entity41") { // XXX debug
        write self.current_node.location;
        write self.next_node;
      }

      if (self.is_in_blocked_link and self.is_on_last_link) {
        current_graph <- self.mini_graph;
      }
      else if (self.modified_graph != nil) {
        current_graph <- self.modified_graph;
      }
      else {
        current_graph <- myself.road_graph;
      }

      // Set topology differently based on the graph of each people agent
      // to calculate distance correctly
      using topology(current_graph) {
        // Gama distance (2D graph).
        distance_to_next_node <- self distance_to self.next_node;
      }

      // Update if next road on the shortest path is jammed.
      if (self.is_link_jammed) {
        self.is_link_jammed <- self.current_link.is_jammed;
      }

      // Mobile agent can only travel if link is not at full capacity.
      if (!self.is_link_jammed) {
        // Find initial ratio + speed at start of each link.
        if (self.is_on_node or myself.change_graph_action) {
          // Handle special road blockage, re-calculate things.
          // Ratio is to convert 2D distance in GAMA graph to the link
          // length specified (real world distance).
          if (myself.change_graph_action) {
            self.ratio <- true_link_length / self.current_link.shape.perimeter;
          }
          else {
            if (distance_to_next_node != 0) { // XXX test
              self.ratio <- true_link_length / distance_to_next_node;
            }
            else {
              self.ratio <- 0.0;
            }
          }

          self.speed <- myself.get_equi_speed(
            self.current_link.free_flow_speed,
            self.current_link.current_volume,
            self.current_link.max_capacity
          );
          if (!self.is_in_blocked_link and self.is_on_node) {
            self.current_link.current_volume <- self.current_link.current_volume + 1;
          }

          // Calculate free flow time needed to reach the next node.
          self.free_flow_time_needed <- distance_to_next_node * self.ratio / self.current_link.free_flow_speed;
        }

        if (name = "mobile_entity41") { // XXX debug
          write self.speed;
        }

        self.real_dist_to_next_node <- distance_to_next_node * self.ratio;
        float travel_time <- self.real_dist_to_next_node / self.speed;
        if (travel_time > 0) {
          time_list <+ travel_time;
        }
      }
    }

    // Set global minimum time.
    float min_time <- min(time_list);
    step <- min_time;

    ask mobile_entity.population {
      self.real_time_spent <- self.real_time_spent + min_time;
    }

    if (change_graph_action) {
      change_graph_action <- false;
    }
  }

  /**
   * GUI function that enables clicking mobile entity agents in the simulation display
   * to show their shortest path on the graph (in orchid color).
   */
  action show_shortest_path {
    list<mobile_entity> clicked_entities <- mobile_entity.population inside circle(55, #user_location);

    if (!empty(clicked_entities)) {
      mobile_entity clicked_entity <- clicked_entities[0];
      clicked_entity.clicked <- !clicked_entity.clicked;
    }
  }

  /**
   * Generates the directed road graph, with weights equal to road length.
   */
  action generate_road_graph {
    road_graph <- directed(
      as_edge_graph(graph_link.population where (!each.hidden and !each.blocked))
      with_weights (graph_link.population as_map (each :: each.real_length))
    );
  }

  /**
   * Generates a path between the origin and destination nodes of the given mobile entity.
   * 
   * @param e The mobile entity.
   * @return The path.
   */
  path generate_path(mobile_entity e) {
    return path_between(road_graph, e.location, e.dest);
  }

  /**
   * Returns the equilibrium speed so that people that arrive later
   * on the road (while there are already people on the road), will have
   * lower speed, so that they cannot overtake each other in 1 lane road.
   * 
   * @params free_speed Free flow speed of the road.
   * @params current_volume Current volume of the road.
   * @params max_capacity Maximum capacity of the road.
   * @return The true speed value of people agent on the road.
   */
  float get_equi_speed(float free_speed, int current_volume, int max_capacity) {
    return free_speed / (1 + 0.15 * (current_volume / max_capacity) ^ 4);
  }
}
