package net.rptools.maptool.util.preferences;

import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

/**
 * test class for storing arrays of records in preferences.
 * This is what the new PrefrenceType objects are meant to accomplish, they were written
 * to support display preferences for multiple display setups.
 * This is just a simple smoke test.
 */

public class PreferenceTypeArrayOfRecordTest {

  record Names(String name, String surname) {
  }

  Names name1 = new Names("Bill", "Bailey");
  Names name2 = new Names("David", "Mitchell");
  Names[] names = {name1, name2};


  @Test
  void testSaveArrayOfRecord() {
    Preferences storage = mock(Preferences.class);

    PreferenceType.RecordType<Names> recordType = new PreferenceType.RecordType<>(Names.class, null, null);
    PreferenceType.ArrayType<Names> arrayType = new PreferenceType.ArrayType<>(recordType);

    arrayType.set(storage, "ABC.DEF", names);

    verify(storage, times(1)).putInt("ABC.DEF.length", 2);
    verify(storage, times(1)).put("ABC.DEF.0.class", "Names");
    verify(storage, times(1)).put("ABC.DEF.0.name", "Bill");
    verify(storage, times(1)).put("ABC.DEF.0.surname", "Bailey");
    verify(storage, times(1)).put("ABC.DEF.1.class", "Names");
    verify(storage, times(1)).put("ABC.DEF.1.name", "David");
    verify(storage, times(1)).put("ABC.DEF.1.surname", "Mitchell");
    verifyNoMoreInteractions(storage);

  }

  @Test
  void testLoadArrayOfRecord() {

    Preferences storage = mock(Preferences.class);

    when(storage.getInt(eq("ABC.DEF.length"), anyInt())).thenReturn(2);
    when(storage.get(eq("ABC.DEF.0.class"), any())).thenReturn("Names");
    when(storage.get(eq("ABC.DEF.0.name"), any())).thenReturn(name1.name());
    when(storage.get(eq("ABC.DEF.0.surname"), any())).thenReturn(name1.surname());
    when(storage.get(eq("ABC.DEF.1.class"), any())).thenReturn("Names");
    when(storage.get(eq("ABC.DEF.1.name"), any())).thenReturn(name2.name());
    when(storage.get(eq("ABC.DEF.1.surname"), any())).thenReturn(name2.surname());

    PreferenceType.RecordType<Names> recordType = new PreferenceType.RecordType<>(Names.class, null, null);
    PreferenceType.ArrayType<Names> arrayType = new PreferenceType.ArrayType<>(recordType);

    Names[] result = arrayType.get(storage, "ABC.DEF", null);

    assertNotNull(result);
    assertEquals(2, result.length);
    assertEquals(name1, result[0]);
    assertEquals(name2, result[1]);

  }
}
