#!/usr/bin/env -S uv run --script
#
# /// script
# requires-python = ">=3.13"
# dependencies = [
#     "tree-sitter>=0.27.0",
#     "tree-sitter-kotlin",
# ]
#
# [tool.uv.sources]
# tree-sitter-kotlin = { git = "https://github.com/fwcd/tree-sitter-kotlin.git", tag = "0.3.8" }
# ///

from dataclasses import dataclass
from pathlib import Path
import sys

from tree_sitter import Language, Node, Parser, Query, QueryCursor, Tree
import tree_sitter_kotlin as tskotlin


# the tskotlin python interface is slightly behind current best practices
if not sys.warnoptions:
    import warnings

    warnings.filterwarnings(
        action="ignore", message="int argument support is deprecated"
    )

KT_LANGUAGE = Language(tskotlin.language())
parser = Parser(KT_LANGUAGE)

# tree-sitter queries: https://tree-sitter.github.io/tree-sitter/using-parsers/queries/
package_query = Query(KT_LANGUAGE, "(package_header ((identifier) @pkg))")
import_query = Query(
    KT_LANGUAGE,
    """
    (import_header
        ((identifier (simple_identifier) @tail .) @import)
        ; the . anchor means only the last simple_identifier will match
    )
    """,
)
class_type_identifier_query = Query(
    KT_LANGUAGE, "(class_declaration ((type_identifier) @type))"
)
type_query = Query(
    KT_LANGUAGE,
    """
    ((class_declaration
        (modifiers
            ((annotation) @serializable (#eq? @serializable "@Serializable"))
        )?
        ((type_identifier) @type)
        ; inheritance is parsed as delegation_specifier
        (delegation_specifier
            [
                ((user_type) @parent)
                (constructor_invocation ((user_type) @parent))
            ]
        )?
    ) @decl)
    ; interfaces are parsed as class_declarations, but objects are parsed as object_declarations
    ((object_declaration
        (modifiers
            ((annotation) @serializable (#eq? @serializable "@Serializable"))
        )?
        ((type_identifier) @type)
        (delegation_specifier
            [
                ((user_type) @parent)
                (constructor_invocation ((user_type) @parent))
            ]
        )?
    ) @decl)
    """,
)
remember_saveable_query = Query(
    KT_LANGUAGE,
    """
    ((call_expression
        ((simple_identifier) @called (#eq? @called "rememberSaveable"))
        (call_suffix
            (value_arguments
                (value_argument
                    ; argument names are parsed as a simple_identifier directly below a value_argument
                    ((simple_identifier) @saver_arg_name (#eq? @saver_arg_name "saver"))
                    ((_) @saver)
                )
            )
        )?
    ) @call)
    """,
)
mutable_state_of_query = Query(
    KT_LANGUAGE,
    """
    (call_expression
        ((simple_identifier) @called (#eq? @called "mutableStateOf"))
        (call_suffix
            (type_arguments (type_projection ((_) @type)))?
            ; a null literal is parsed as an empty value_argument
            (value_arguments (value_argument ((_) @arg)?))
        )
    )
    """,
)
variable_type_query = Query(
    KT_LANGUAGE,
    """
    (variable_declaration
        (simple_identifier)
        (_) @type
    )
    """,
)


@dataclass(frozen=True)
class DeclaredType:
    name: str
    is_serializable: bool
    parent: str | None
    file: Path
    decl_node: Node


def parse_source_file(path: Path) -> Tree:
    return parser.parse(path.read_bytes())


def dequalify_name(name: str) -> str:
    pieces = name.split(".")
    return ".".join(p for p in pieces if p[0].isupper())


def get_fully_qualified_name(node: Node) -> str:
    if node.type == "nullable_type":
        node = node.named_child(0)
    assert node.type in ("type_identifier", "user_type")
    # we want to turn Foo.Bar<T> into Foo.Bar, but that’s
    # (user_type (type_identifier) (type_identifier) (type_arguments)),
    # so there’s no subtree we can use and we have to just slice the bytes
    if (
        len(node.named_children) > 0
        and node.named_children[-1].type == "type_arguments"
    ):
        arguments_node = node.named_children[-1]
        name = node.text[: -len(arguments_node.text)].decode()
    else:
        name = node.text.decode()
    if name in ("Boolean", "Long", "String"):
        return name
    if node.type == "user_type":
        import_name = node.child(0).text.decode()
    else:
        import_name = name
    # for class Foo { class Bar } we want Foo.Bar but for class Foo: Bar we do not want Foo.Bar
    from_class_body = False
    n = node.parent
    while n is not None:
        if n.type == "class_body":
            from_class_body = True
        if n.type == "class_declaration" and from_class_body:
            from_class_body = False
            q = QueryCursor(class_type_identifier_query)
            parent_type_identifier = q.captures(n)["type"][0]
            if parent_type_identifier.text.decode() != name:
                parent_fqn = get_fully_qualified_name(parent_type_identifier)
                return parent_fqn + "." + name
        if n.type == "source_file":
            for _, match in QueryCursor(import_query).matches(n):
                if match["tail"][0].text.decode() == import_name:
                    return (
                        match["import"][0].text.decode().removesuffix(import_name)
                        + name
                    )
            q = QueryCursor(package_query)
            package = q.captures(n)["pkg"][0].text.decode()
            return package + "." + name
        n = n.parent
    return name


def get_types(tree: Tree, file: Path):
    q = QueryCursor(type_query)
    for _, match in q.matches(tree.root_node):
        name = get_fully_qualified_name(match["type"][0])
        is_serializable = "serializable" in match
        if "parent" in match:
            parent = get_fully_qualified_name(match["parent"][0])
        else:
            parent = None
        decl_node = match["decl"][0]
        yield DeclaredType(name, is_serializable, parent, file, decl_node)


def check_serializable_subclass(known_types: dict[str, DeclaredType]):
    for t in known_types.values():
        if t.parent is None:
            continue
        parent = known_types.get(t.parent)
        if parent is None:
            continue
        if parent.is_serializable and not t.is_serializable:
            location = f"{t.file}:{t.decl_node.start_point.row + 1}:{t.decl_node.start_point.column + 1}"
            yield f"{location}: {dequalify_name(t.name)} is a non-@Serializable subclass of @Serializable {dequalify_name(parent.name)}"


def get_remember_saveable_type(node: Node) -> str | None:
    # check mutableStateOf() for explicit type or arg with obvious type
    q = QueryCursor(mutable_state_of_query)
    for _, match in q.matches(node):
        if "type" in match:
            type = match["type"][0]
            return get_fully_qualified_name(type)
        if "arg" in match:
            arg = match["arg"][0]
            if arg.type == "boolean_literal":
                return "Boolean"
            if arg.type == "string_literal":
                return "String"

    if "mutableIntStateOf" in node.text.decode():
        return "Int"

    # check variable declaration for type
    if node.parent.type == "property_delegate":
        parent = node.parent.parent
        q = QueryCursor(variable_type_query)
        for _, match in q.matches(parent):
            type = match["type"][0]
            return get_fully_qualified_name(type)


def get_remember_saveable_issues(tree: Tree, file: Path, known_types: dict[str, DeclaredType]):
    q = QueryCursor(remember_saveable_query)
    for _, match in q.matches(tree.root_node):
        call = match["call"][0]
        # rememberSaveable(foo) { bar } is parsed as a nested call_expression
        if call.parent.type == "call_expression":
            call = call.parent
        if "saver" in match and match["saver"][0].text != b"stateJsonSaver()":
            # using a different saver, so probably safe
            continue
        saveable_type = get_remember_saveable_type(call)
        if saveable_type in ("Boolean", "Int", "Long", "String"):
            continue
        # parser does not handle explicit constructors on nested classes correctly
        if saveable_type in ("com.mbta.tid.mbta_app.model.Route.Id"):
            continue
        location = f"{file}:{call.start_point.row + 1}:{call.start_point.column + 1}"
        if saveable_type is None:
            message = "rememberSaveable needs explicit type annotation"
        elif "saver" not in match:
            message = f"rememberSaveable with type {saveable_type} needs explicit saver"
        elif saveable_type not in known_types:
            message = f"could not find type {saveable_type}"
        elif not known_types[saveable_type].is_serializable:
            message = f"type {saveable_type} is not @Serializable"
        else:
            continue
        yield f"{location}: {message}"


def main():
    project_dir = Path(__file__).parent.parent
    known_types = dict()
    for kotlin_file in project_dir.glob("*/src/**/*.kt"):
        relative_kotlin_file = kotlin_file.relative_to(project_dir)
        tree = parse_source_file(kotlin_file)
        known_types.update((t.name, t) for t in get_types(tree, relative_kotlin_file))
    errors = list(check_serializable_subclass(known_types))
    for kotlin_file in project_dir.glob("androidApp/src/**/*.kt"):
        relative_kotlin_file = kotlin_file.relative_to(project_dir)
        tree = parse_source_file(kotlin_file)
        errors.extend(get_remember_saveable_issues(tree, relative_kotlin_file, known_types))
    if len(errors) > 0:
        for error in errors:
            print(error)
        sys.exit(1)


if __name__ == "__main__":
    main()
