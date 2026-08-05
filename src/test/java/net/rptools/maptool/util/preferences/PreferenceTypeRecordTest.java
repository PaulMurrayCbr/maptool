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

import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.prefs.Preferences;
import net.rptools.maptool.util.preferences.PreferenceType.*;
import org.junit.jupiter.api.Test;

public class PreferenceTypeRecordTest {

  public record SimpleRecord(String name, int age, boolean isMale, double height, File file) {}

  SimpleRecord simpleRecord =
      new SimpleRecord("John Doe", 30, true, 1.75, new File("/path/to/file"));

  public record RecordWithObject(String name, Color color) {}

  @Test
  void testRecord() {
    assertNotNull(simpleRecord);
    assertEquals("John Doe", simpleRecord.name());
    assertEquals(30, simpleRecord.age());
    assertEquals(true, simpleRecord.isMale());
    assertEquals(1.75, simpleRecord.height());
    assertEquals(new File("/path/to/file"), simpleRecord.file());
  }

  @Test
  void testCanInstantiate() {
    RecordType<SimpleRecord> recordType = new RecordType<>(SimpleRecord.class, null, null);
    assertNotNull(recordType);
  }

  @Test
  void testCantInstantiateObjectWithoutStorer() {
    try {
      RecordType<RecordWithObject> recordType =
          new RecordType<>(RecordWithObject.class, null, null);
      fail("construction of RecordWithObject without a storer should fail");
    } catch (IllegalArgumentException e) {
      assertEquals(
          "No preference type provided for element color of type Color in record RecordWithObject.",
          e.getMessage());
    }
  }

  @Test
  void testCanInstantiateObjectWithStorer() {
    RecordType<RecordWithObject> recordType =
        new RecordType<>(RecordWithObject.class, Map.of("color", new ColorType(false)), null);
    assertNotNull(recordType);
  }

  @Test
  void saveNull() {
    Preferences storage = mock(Preferences.class);
    RecordType<SimpleRecord> recordType = new RecordType<>(SimpleRecord.class, null, null);

    recordType.set(storage, "ABC.DEF", null);
    verify(storage, times(1)).remove("ABC.DEF.class");
    verifyNoMoreInteractions(storage);
  }

  @Test
  void simpleSave() {
    Preferences storage = mock(Preferences.class);
    RecordType<SimpleRecord> recordType = new RecordType<>(SimpleRecord.class, null, null);

    recordType.set(storage, "ABC.DEF", simpleRecord);

    verify(storage, times(1)).put("ABC.DEF.class", "SimpleRecord");
    verify(storage, times(1)).put("ABC.DEF.name", simpleRecord.name());
    verify(storage, times(1)).putInt("ABC.DEF.age", simpleRecord.age());
    verify(storage, times(1)).putBoolean("ABC.DEF.isMale", simpleRecord.isMale());
    verify(storage, times(1)).putDouble("ABC.DEF.height", simpleRecord.height());
    verify(storage, times(1)).put("ABC.DEF.file", simpleRecord.file().toString());
    verifyNoMoreInteractions(storage);
  }

  @Test
  void objectSave() {
    Preferences storage = mock(Preferences.class);
    RecordType<RecordWithObject> recordType =
        new RecordType<>(RecordWithObject.class, Map.of("color", new ColorType(false)), null);

    RecordWithObject rtt = new RecordWithObject("John Doe", Color.RED);

    recordType.set(storage, "ABC.DEF", rtt);

    verify(storage, times(1)).put("ABC.DEF.class", "RecordWithObject");
    verify(storage, times(1)).put("ABC.DEF.name", rtt.name());
    verify(storage, times(1)).putInt("ABC.DEF.color", Color.RED.getRGB());
    verifyNoMoreInteractions(storage);
  }

  @Test
  void simpleLoad() {
    Preferences storage = mock(Preferences.class);

    RecordType<SimpleRecord> recordType = new RecordType<>(SimpleRecord.class, null, null);

    when(storage.get(eq("ABC.DEF.class"), any())).thenReturn("SimpleRecord");
    when(storage.get(eq("ABC.DEF.name"), any())).thenReturn(simpleRecord.name());
    when(storage.getInt(eq("ABC.DEF.age"), anyInt())).thenReturn(simpleRecord.age());
    when(storage.getBoolean(eq("ABC.DEF.isMale"), anyBoolean())).thenReturn(simpleRecord.isMale());
    when(storage.getDouble(eq("ABC.DEF.height"), anyDouble())).thenReturn(simpleRecord.height());
    when(storage.get(eq("ABC.DEF.file"), any())).thenReturn(simpleRecord.file().toString());

    SimpleRecord result = recordType.get(storage, "ABC.DEF", null);

    verify(storage, times(1)).get(eq("ABC.DEF.class"), any());
    verify(storage, times(1)).get(eq("ABC.DEF.name"), any());
    verify(storage, times(1)).getInt(eq("ABC.DEF.age"), anyInt());
    verify(storage, times(1)).getBoolean(eq("ABC.DEF.isMale"), anyBoolean());
    verify(storage, times(1)).getDouble(eq("ABC.DEF.height"), anyDouble());
    verify(storage, times(1)).get(eq("ABC.DEF.class"), any());
    verify(storage, times(1)).get(eq("ABC.DEF.file"), any());
    verifyNoMoreInteractions(storage);

    assertEquals(simpleRecord, result);
  }

  @Test
  void simpleLoadWithDefault() {
    Preferences storage = mock(Preferences.class);

    when(storage.get(eq("ABC.DEF.class"), any())).thenReturn("SimpleRecord");
    when(storage.get(eq("ABC.DEF.name"), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));

    RecordType<SimpleRecord> recordType =
        new RecordType<>(SimpleRecord.class, null, Map.of("name", () -> "default name"));

    SimpleRecord result = recordType.get(storage, "ABC.DEF", null);

    assertEquals("default name", result.name());
  }

  @Test
  void simpleLoadWithDefaultAndData() {
    Preferences storage = mock(Preferences.class);

    when(storage.get(eq("ABC.DEF.class"), any())).thenReturn("SimpleRecord");
    when(storage.get(eq("ABC.DEF.name"), any())).thenReturn(simpleRecord.name());

    RecordType<SimpleRecord> recordType =
        new RecordType<>(SimpleRecord.class, null, Map.of("name", () -> "default name"));

    SimpleRecord result = recordType.get(storage, "ABC.DEF", null);

    assertEquals(simpleRecord.name(), result.name());
  }
}
