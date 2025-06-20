#!/usr/bin/env python3
"""
Generates an approximately accurate call graph for our Android and iOS UI components.

Writes into `./component-graphs/`. Will render PNGs if GraphViz is installed, or write .dot source if not.

Usage:
$ bin/component-graph.py RouteCard AlertDetailsPage
$ bin/component-graph.py # for everything
"""

from dataclasses import dataclass
from pathlib import Path
import re
import subprocess
import sys


@dataclass(order=True, frozen=True)
class Component:
    name: str
    path: Path


@dataclass(order=True, frozen=True)
class Edge:
    caller: str
    callee: str


COMPONENT_CALL = re.compile(r"[^\.](\w+)\s*(?:\(|\{)")


def get_android_components() -> set[Component]:
    components = set()

    for file in Path("androidApp/src/main/java").glob("**/*.kt"):
        if (
            re.search(
                rf"@Composable\s+fun (\w+\.)?{file.stem}\(",
                file.read_text(encoding="utf-8"),
            )
            and file.stem[0].isupper()
            and file.stem != "MyApplicationTheme"
        ):
            components.add(Component(file.stem, file))

    return components


def get_ios_components() -> set[Component]:
    components = set()

    for file in Path("iosApp/iosApp").glob("**/*.swift"):
        if re.search(
            rf"struct {file.stem}(<.+>)?: View\b", file.read_text(encoding="utf-8")
        ):
            components.add(Component(file.stem, file))

    return components


def get_edges(components: set[Component]) -> set[Edge]:
    component_names = {component.name for component in components}
    edges = set()
    for component in components:
        for call in COMPONENT_CALL.finditer(component.path.read_text(encoding="utf-8")):
            callee = call.group(1)
            if callee in component_names and callee != component.name:
                edges.add(Edge(component.name, callee))
    return edges


def filter_graph(
    starting_from: str, components: set[str], edges: set[Edge]
) -> tuple[set[str], set[Edge]]:
    reachable = {starting_from}
    frontier = [starting_from]
    while len(frontier) > 0:
        current_node = frontier.pop(0)
        for edge in edges:
            if edge.caller == current_node and edge.callee not in reachable:
                frontier.append(edge.callee)
                reachable.add(edge.callee)

    filtered_components = {c for c in components if c in reachable}
    filtered_edges = {
        e for e in edges if e.caller in reachable and e.callee in reachable
    }
    return (filtered_components, filtered_edges)


def main(roots: list[str]):
    android_components = get_android_components()
    android_edges = get_edges(android_components)

    ios_components = get_ios_components()
    ios_edges = get_edges(ios_components)

    android_components = {c.name for c in android_components}
    ios_components = {c.name for c in ios_components}

    both_components = android_components.intersection(ios_components)
    android_only_components = android_components.difference(both_components)
    ios_only_components = ios_components.difference(both_components)

    both_edges = android_edges.intersection(ios_edges)
    android_only_edges = android_edges.difference(both_edges)
    ios_only_edges = ios_edges.difference(both_edges)

    if len(roots) == 0:
        roots = list(android_components.union(ios_components))

    Path("component-graphs").mkdir(exist_ok=True)
    legend_content = """
digraph {
  BP1 [label="Component on Both Platforms"];
  BP2 [label="Component on Both Platforms"];
  AO [style=dashed, color=green, label="Component on Android Only"];
  IO [style=dashed, color=red, label="Component on iOS Only"];
  BP1 -> BP2 [label="Call on Both Platforms"];
  BP1 -> AO [style=dashed, color=green, label="Call on Android Only"];
  BP1 -> IO [style=dashed, color=red, label="Call on iOS Only"];
  AO -> BP2 [style=dashed, color=green];
}
"""

    try:
        subprocess.run(
            ["dot", "-Tpng", "-ocomponent-graphs/_Legend.png"],
            input=legend_content,
            capture_output=True,
            text=True,
            check=True,
        )
        graphviz_installed = True
    except FileNotFoundError:
        graphviz_installed = False
    except subprocess.CalledProcessError:
        graphviz_installed = False

    if not graphviz_installed:
        print("Could not find Graphviz, writing .dot source instead of rendered .png")
        Path("component-graphs/_Legend.dot").write_text(legend_content)

    for root in sorted(roots):
        print(root)
        filtered_components, filtered_edges = filter_graph(
            root,
            android_components.union(ios_components),
            android_edges.union(ios_edges),
        )

        dot_lines = []
        dot_lines.append("digraph {")
        for component in sorted(both_components):
            if component in filtered_components:
                dot_lines.append(f"  {component};")
        for component in sorted(android_only_components):
            if component in filtered_components:
                dot_lines.append(f"  {component} [style=dashed, color=green];")
        for component in sorted(ios_only_components):
            if component in filtered_components:
                dot_lines.append(f"  {component} [style=dashed, color=red];")
        dot_lines.append("  subgraph pages {")
        dot_lines.append("    rank = same;")
        for component in sorted(android_components.union(ios_components)):
            if (
                component in filtered_components
                and component.endswith("Page")
                and component != "MapAndSheetPage"
            ):
                dot_lines.append(f"    {component};")
        dot_lines.append("  }")
        for edge in sorted(both_edges):
            if edge in filtered_edges:
                dot_lines.append(f"  {edge.caller} -> {edge.callee};")
        for edge in sorted(android_only_edges):
            if edge in filtered_edges:
                dot_lines.append(
                    f"  {edge.caller} -> {edge.callee} [style=dashed, color=green];",
                )
        for edge in sorted(ios_only_edges):
            if edge in filtered_edges:
                dot_lines.append(
                    f"  {edge.caller} -> {edge.callee} [style=dashed, color=red];"
                )
        dot_lines.append("}")

        dot_content = "\n".join(dot_lines)

        if graphviz_installed:
            subprocess.run(
                ["dot", "-Tpng", f"-ocomponent-graphs/{root}.png"],
                input=dot_content,
                text=True,
                check=True,
            )
        else:
            Path(f"component-graphs/{root}.dot").write_text(dot_content)


if __name__ == "__main__":
    roots = []
    if len(sys.argv) > 1:
        roots = sys.argv[1:]
    if roots == ["-h"] or roots == ["--help"]:
        print(__doc__)
        exit(1)
    main(roots)
