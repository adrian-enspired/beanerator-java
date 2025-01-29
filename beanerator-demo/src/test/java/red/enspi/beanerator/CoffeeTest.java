/*
 * author     Adrian <adrian@enspi.red>
 * copyright  2025
 * license    GPL-3.0 (only)
 *
 *  This program is free software: you can redistribute it and/or modify it
 *  under the terms of the GNU General public final License, version 3.
 *  The right to apply the terms of later versions of the GPL is RESERVED.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU General public final License for more details.
 *
 *  You should have received a copy of the GNU General public final License along with this program.
 *  If not, see <http://www.gnu.org/licenses/gpl-3.0.txt>.
 */
package red.enspi.beanerator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import red.enspi.beanerator.demo.Coffee;
import red.enspi.beanerator.demo.CoffeeBean;
import red.enspi.beanerator.demo.CoffeeTaste;

/**
 * Extend this class to test that your records are beanerated correctly.
 *
 * <p>{@code T} MUST be a record type and MUST be annotated with {@code @Beanerate}.
 */
public class CoffeeTest extends BeaneratedTest<Coffee, CoffeeBean> {

  private static Coffee coffee() {
    return new Coffee(
      Coffee.Variety.Arabica,
      Coffee.Origin.Brazil,
      Coffee.Roast.Medium,
      new CoffeeTaste(CoffeeTaste.Flavor.Nutty, CoffeeTaste.Acidity.Low, CoffeeTaste.Body.Full));
  }

  public static Stream<Arguments> coffee_source() {
    return Stream.of(
      Arguments.of(CoffeeTest.coffee())
    );
  }

  @ParameterizedTest
  @MethodSource("coffee_source")
  void construct(Coffee record) {
    var actual = CoffeeBean.fromRecord(record);
    assertTrue(actual instanceof CoffeeBean);
    assertEquals(record, actual.toRecord());
  }

  public static Stream<Arguments> toString_source() {
    return Stream.of(
      Arguments.of(
        CoffeeBean.fromRecord(CoffeeTest.coffee()),
        "CoffeeBean[variety=Arabica, origin=Brazil, roast=Medium, taste=CoffeeTasteBean[flavor=Nutty, acidity=Low, body=Full]]")
    );
  }

  @ParameterizedTest
  @MethodSource("toString_source")
  void toString(CoffeeBean beanerated, String expected) {}
}
