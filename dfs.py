graph = {
    'A': ['B', 'C', 'D', 'E'],
    'B': ['A', 'F', 'G'],
    'C': ['A', 'G', 'H', 'I'],
    'D': ['A', 'I', 'J'],
    'E': ['A', 'J', 'K'],
    'F': ['B', 'L', 'M'],
    'G': ['B', 'C', 'M', 'N'],
    'H': ['C', 'N', 'O'],
    'I': ['C', 'D', 'O', 'P'],
    'J': ['D', 'E', 'P', 'Q'],
    'K': ['E', 'Q', 'R'],
    'L': ['F', 'S'],
    'M': ['F', 'G', 'S', 'T'],
    'N': ['G', 'H', 'T', 'U'],
    'O': ['H', 'I', 'U', 'V'],
    'P': ['I', 'J', 'V', 'W'],
    'Q': ['J', 'K', 'W', 'X'],
    'R': ['K', 'X'],
    'S': ['L', 'M', 'Y'],
    'T': ['M', 'N', 'Y', 'Z'],
    'U': ['N', 'O', 'Z'],
    'V': ['O', 'P', 'AA'],
    'W': ['P', 'Q', 'AA', 'AB'],
    'X': ['Q', 'R', 'AB'],
    'Y': ['S', 'T', 'AC'],
    'Z': ['T', 'U', 'AC'],
}


import networkx as nx
import matplotlib.pyplot as plt

G = nx.Graph()

for node, neighbors in graph.items():
    for neighbor in neighbors:
        G.add_edge(node, neighbor)

plt.figure(figsize=(16, 12))

pos = nx.spring_layout(G, seed=42, k=0.8)

nx.draw(
    G,
    pos,
    with_labels=True,
    node_size=900,
    font_size=10,
    font_weight='bold',
    node_color='lightblue',
    edge_color='gray'
)

plt.show()