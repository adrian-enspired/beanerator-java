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

import org.junit.jupiter.api.Test;

/**
 * Extend this class to test that your records are beanerated correctly.
 *
 * <p>{@code T} MUST be a record type and MUST be annotated with {@code @Beanerate}.
 */
abstract public class BeaneratedTest<R, B> {}
