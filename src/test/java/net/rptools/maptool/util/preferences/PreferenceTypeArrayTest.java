/*
 * This software Copyright by the RPTools.net development team, and
 * licensed under the Affero GPL Version 3 or, at your option, any later
 * version.
 *
 * MapTool Source Code is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * You should have received a copy of the GNU Affero General Public
 * License * along with this source Code.  If not, please visit
 * <http://www.gnu.org/licenses/> and specifically the Affero license
 * text at <http://www.gnu.org/licenses/agpl.html>.
 */
package net.rptools.maptool.util.preferences;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.function.Supplier;
import java.util.prefs.Preferences;
import net.rptools.maptool.util.preferences.PreferenceType.ArrayType;
import net.rptools.maptool.util.preferences.PreferenceType.StringType;
import org.junit.jupiter.api.Test;

public class PreferenceTypeArrayTest {

  @Test
  void testSaveNull() throws Throwable {
    Preferences storage = mock(Preferences.class);

    ArrayType<String> prefType = new PreferenceType.ArrayType<>(new StringType());

    prefType.set(storage, "ABC.DEF", null);

    verify(storage, times(1)).remove("ABC.DEF.length");
    verifyNoMoreInteractions(storage);
  }

  @Test
  void testSaveEmpty() throws Throwable {
    Preferences storage = mock(Preferences.class);

    ArrayType<String> prefType = new PreferenceType.ArrayType<>(new StringType());

    prefType.set(storage, "ABC.DEF", new String[] {});

    verify(storage, times(1)).putInt("ABC.DEF.length", 0);
    verifyNoMoreInteractions(storage);
  }

  @Test
  void testSaveThree() throws Throwable {
    Preferences storage = mock(Preferences.class);

    ArrayType<String> prefType = new PreferenceType.ArrayType<>(new StringType());

    prefType.set(storage, "ABC.DEF", new String[] {"A", "B", "C"});

    verify(storage, times(1)).putInt("ABC.DEF.length", 3);
    verify(storage, times(1)).put("ABC.DEF.0", "A");
    verify(storage, times(1)).put("ABC.DEF.1", "B");
    verify(storage, times(1)).put("ABC.DEF.2", "C");
    verifyNoMoreInteractions(storage);
  }

  @Test
  void testSaveThreeWithNulls() throws Throwable {
    Preferences storage = mock(Preferences.class);

    ArrayType<String> prefType = new PreferenceType.ArrayType<>(new StringType());

    prefType.set(storage, "ABC.DEF", new String[] {"A", null, "C"});

    verify(storage, times(1)).putInt("ABC.DEF.length", 3);
    verify(storage, times(1)).put("ABC.DEF.0", "A");
    verify(storage, times(1)).put("ABC.DEF.1", null);
    verify(storage, times(1)).put("ABC.DEF.2", "C");
    verifyNoMoreInteractions(storage);
  }

  @Test
  void testLoadUnknown() throws Throwable {
    Preferences storage = mock(Preferences.class);
    ArrayType<String> prefType = new PreferenceType.ArrayType<>(new StringType());

    when(storage.getInt("ABC.DEF.length", -1)).thenReturn(-1);

    String[] result = prefType.get(storage, "ABC.DEF", null);

    verify(storage, times(1)).getInt("ABC.DEF.length", -1);
    verifyNoMoreInteractions(storage);

    assertNull(result);
  }

  @Test
  void testLoadUnknownWithDefault() throws Throwable {
    Preferences storage = mock(Preferences.class);
    ArrayType<String> prefType = new PreferenceType.ArrayType<>(new StringType());

    when(storage.getInt("ABC.DEF.length", -1)).thenReturn(-1);
    String[] defaultResult = new String[10];

    Supplier<String[]> defaultSupplier = mock(Supplier.class);
    when(defaultSupplier.get()).thenReturn(defaultResult);

    String[] result = prefType.get(storage, "ABC.DEF", defaultSupplier);

    verify(storage, times(1)).getInt("ABC.DEF.length", -1);
    verifyNoMoreInteractions(storage);

    verify(defaultSupplier, times(1)).get();
    verifyNoMoreInteractions(defaultSupplier);

    assertSame(defaultResult, result);
  }

  @Test
  void testLoadEmpty() throws Throwable {
    Preferences storage = mock(Preferences.class);
    ArrayType<String> prefType = new PreferenceType.ArrayType<>(new StringType());

    when(storage.getInt("ABC.DEF.length", -1)).thenReturn(0);

    String[] result = prefType.get(storage, "ABC.DEF", null);

    verify(storage, times(1)).getInt("ABC.DEF.length", -1);
    verifyNoMoreInteractions(storage);

    assertNotNull(result);
    assertEquals(0, result.length);
  }

  @Test
  void testLoadEmptyWithDefault() throws Throwable {
    Preferences storage = mock(Preferences.class);
    ArrayType<String> prefType = new PreferenceType.ArrayType<>(new StringType());

    String[] defaultResult = new String[10];
    when(storage.getInt("ABC.DEF.length", -1)).thenReturn(0);

    Supplier<String[]> defaultSupplier = mock(Supplier.class);
    when(defaultSupplier.get()).thenReturn(defaultResult);

    String[] result = prefType.get(storage, "ABC.DEF", defaultSupplier);

    verify(storage, times(1)).getInt("ABC.DEF.length", -1);
    verifyNoMoreInteractions(storage);

    verify(defaultSupplier, never()).get();
    verifyNoMoreInteractions(defaultSupplier);

    assertNotNull(result);
    assertNotSame(defaultResult, result);
    assertEquals(0, result.length);
  }

  @Test
  void testLoadThreeWithNulls() throws Throwable {
    Preferences storage = mock(Preferences.class);
    ArrayType<String> prefType = new PreferenceType.ArrayType<>(new StringType());

    when(storage.getInt("ABC.DEF.length", -1)).thenReturn(3);
    when(storage.get(eq("ABC.DEF.0"), any())).thenReturn("A");
    when(storage.get(eq("ABC.DEF.1"), any())).thenReturn(null);
    when(storage.get(eq("ABC.DEF.2"), any())).thenReturn("C");

    String[] result = prefType.get(storage, "ABC.DEF", null);

    verify(storage, times(1)).getInt("ABC.DEF.length", -1);
    verify(storage, times(1)).get(eq("ABC.DEF.0"), any());
    verify(storage, times(1)).get(eq("ABC.DEF.1"), any());
    verify(storage, times(1)).get(eq("ABC.DEF.2"), any());
    verifyNoMoreInteractions(storage);

    assertNotNull(result);
    assertEquals(3, result.length);
    assertEquals("A", result[0]);
    assertNull(result[1]);
    assertEquals("C", result[2]);
  }
}
