import 'package:flutter_test/flutter_test.dart';

import 'package:mbta_app/main.dart';

void main() {
  testWidgets("Home should say \"Hello World!\"", (tester) async {
    await tester.pumpWidget(const MainApp());

    expect(find.text("Hello World!"), findsOneWidget);
  });
}
