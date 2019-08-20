model graph_vertex

import "CountingAgent.gaml"

/**
 * Node species on road graph network.
 * 
 * @author Hieu Chu (chc116@uowmail.edu.au)
 * @author Damien Vergnet
 */
species graph_vertex parent: counter {
  /** Node's ID. */
  int node_number <- length(graph_vertex.population) - 1;

  /** Draws a black square with the ID on top. */
  aspect base {
    draw square(50) color: #black;
    if (world.draw_names) {
      draw string(node_number) color: #black font: font('Arial', 12, #plain);
    }
  }
}
