package net.rptools.maptool.util.preferences;

import net.rptools.maptool.util.preferences.PreferenceType.*;
import org.junit.jupiter.api.Test;

import java.awt.*;
import java.io.File;
import java.util.Map;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PreferenceTypeRecordTest {

  public record SimpleRecord(String name, int age, boolean isMale, double height, File file) {

  }

  SimpleRecord simpleRecord = new SimpleRecord("John Doe", 30, true, 1.75, new File("/path/to/file"));

  public record RecordWithTrickyType(String name, Color color) {

  }


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
  void testCantInstantiateTrickyTypeWithoutStorer() {
    try {
      RecordType<RecordWithTrickyType> recordType = new RecordType<>(RecordWithTrickyType.class, null, null);
      fail("construction of TrickyType record without a storer should fail");
    } catch (IllegalArgumentException e) {
      assertEquals("No preference type provided for element color of type Color in record RecordWithTrickyType.", e.getMessage());
    }
  }

  @Test
  void testCanInstantiateTrickyTypeWithStorer() {
    RecordType<RecordWithTrickyType> recordType = new RecordType<>(RecordWithTrickyType.class,
        Map.of("color", new ColorType(false)),
        null);
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
  void trickySave() {
    Preferences storage = mock(Preferences.class);
    RecordType<RecordWithTrickyType> recordType = new RecordType<>(RecordWithTrickyType.class,
        Map.of("color", new ColorType(false)),
        null);

    RecordWithTrickyType rtt = new RecordWithTrickyType("John Doe", Color.RED);

    recordType.set(storage, "ABC.DEF", rtt);

    verify(storage, times(1)).put("ABC.DEF.class", "RecordWithTrickyType");
    verify(storage, times(1)).put("ABC.DEF.name", rtt.name());
    verify(storage, times(1)).putInt("ABC.DEF.color", Color.RED.getRGB());
    verifyNoMoreInteractions(storage);
  }


}
