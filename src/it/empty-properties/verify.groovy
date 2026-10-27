// the csproj writes the values of the custom properties it received to target/properties.txt
List<String> lines = new File(basedir, "target/properties.txt").readLines()

assert lines.contains("Control=[set]"): "the custom properties were not passed at all: $lines"

List<String> emptyProperties = ["EmptyPair", "SelfClosing", "Whitespace", "Comment", "Cdata", "EmptyExpression"]
List<String> wrong = emptyProperties.findAll { !lines.contains(it + "=[]") }

assert wrong.isEmpty(): "properties $wrong were not set to an empty string: $lines"

String buildLog = new File(basedir, "build.log").text

assert !(buildLog =~ /-p:\w+=null\b/): "a custom property was passed as 'null'"
