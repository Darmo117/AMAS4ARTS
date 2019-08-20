model counting_agent

import "MobileEntity.gaml"

/**
 * This base species stores the position and speed of all person agents that interact with it.
 * 
 * @author Damien Vergnet
 */
species counter {
  /** List of names of all mobile agents that interacted with this counter. */
  list<string> names;
  /** List of locations of all mobile agents that interacted with this counter. */
  list<point> locations;
  /** List of speeds of all mobile agents that interacted with this counter. */
  list<float> speeds;

  init reflex {
    do reset_counts();
  }

  /**
   * Adds data from the given mobile agent to the lists.
   * 
   * @param p The mobile entity to extract data from.
   */
  action add_data(mobile_entity p) {
    names <+ p.name;
    locations <+ p.location;
    speeds <+ p.speed;
  }

  /**
   * Returns all data from the lists as a list of strings (one string per agent).
   */
  list<string> get_data {
    list<string> data <- [];

    if (!empty(names)) {
      // All lists should have the same length.
      loop i from: 0 to: length(names) - 1 {
        data <+ names[i] + ";" + string(locations[i]) + ";" + string(speeds[i]);
      }
    }

    return data;
  }

  /**
   * Resets all lists.
   */
  action reset_counts {
    names <- [];
    locations <- [];
    speeds <- [];
  }
}
