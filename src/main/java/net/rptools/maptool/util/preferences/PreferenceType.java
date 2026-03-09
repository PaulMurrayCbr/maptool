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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.File;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.Map;
import java.util.function.Supplier;
import java.util.prefs.Preferences;

/**
 * Defines how preferences with a value of type {@code T} can be read and written.
 *
 * <p>For numeric types, also defines how to constrain the value to its bounds.
 *
 * @param <T> The preference value type, as for {@link Preference}.
 */
interface PreferenceType<T> {
  static Logger log = LoggerFactory.getLogger(PreferenceType.class);

  Class<T> getValueClass();

  /**
   * Serializes and writes {@code value} to {@code storage}, associating it with {@code key}.
   *
   * @param storage The preferences node to write to.
   * @param key The key to write to.
   * @param value The value to associate with {@code key}.
   */
  void set(Preferences storage, String key, T value);

  /**
   * Reads and deserialized the value associated with {@code key} in {@code storage}.
   *
   * @param storage The preferences node to read from.
   * @param key The key to read.
   * @param defaultValue If the key is not found, use {@code defaultValue.get()} instead.
   * @return The deserialized value, or {@code defaultValue} if not found.
   */
  T get(Preferences storage, String key, Supplier<T> defaultValue);

  /** Refines {@link PreferenceType} to allow clamping a numeric value to some bounds. */
  interface Numeric<T extends Number> extends PreferenceType<T> {
    /**
     * Constrains {@code value} to lie between {@code minValue} and {@code maxValue}, inclusive.
     *
     * @param value The value to constrain.
     * @param minValue The least value that can be returned.
     * @param maxValue The greatest value that can be returned.
     * @return If {@code value} is less than {@code minValue}, then {@code minValue}. If {@code
     *     value} is greater than {@code maxValue}, then {@code maxValue}. Otherwise, {@code value}.
     */
    T clamp(T value, T minValue, T maxValue);
  }

  /**
   * Reads and writes {@code boolean} values.
   *
   * <p>Values are read using {@link Preferences#getBoolean(String, boolean)} and written using
   * {@link Preferences#putBoolean(String, boolean)}.
   */
  final class BooleanType implements PreferenceType<Boolean> {
    @Override
    public Class<Boolean> getValueClass() {
      return Boolean.class;
    }

    @Override
    public void set(Preferences storage, String key, Boolean value) {
      storage.putBoolean(key, value);
    }

    @Override
    public Boolean get(Preferences storage, String key, Supplier<Boolean> defaultValue) {
      return storage.getBoolean(key, defaultValue.get());
    }
  }

  /**
   * Reads and writes {@code int} values.
   *
   * <p>Values are read using {@link Preferences#getInt(String, int)} and written using {@link
   * Preferences#putInt(String, int)}.
   */
  final class IntegerType implements Numeric<Integer> {
    @Override
    public Class<Integer> getValueClass() {
      return Integer.class;
    }

    @Override
    public Integer clamp(Integer value, Integer minValue, Integer maxValue) {
      return Math.clamp(value, minValue, maxValue);
    }

    @Override
    public void set(Preferences storage, String key, Integer value) {
      storage.putInt(key, value);
    }

    @Override
    public Integer get(Preferences storage, String key, Supplier<Integer> defaultValue) {
      return storage.getInt(key, defaultValue.get());
    }
  }

  /**
   * Reads and writes {@code double} values.
   *
   * <p>Values are read using {@link Preferences#getDouble(String, double)} and written using {@link
   * Preferences#putDouble(String, double)}.
   */
  final class DoubleType implements Numeric<Double> {
    @Override
    public Class<Double> getValueClass() {
      return Double.class;
    }

    @Override
    public Double clamp(Double value, Double minValue, Double maxValue) {
      return Math.clamp(value, minValue, maxValue);
    }

    @Override
    public void set(Preferences storage, String key, Double value) {
      storage.putDouble(key, value);
    }

    @Override
    public Double get(Preferences storage, String key, Supplier<Double> defaultValue) {
      return storage.getDouble(key, defaultValue.get());
    }
  }

  /**
   * Reads and writes {@code double} values.
   *
   * <p>Values are read using {@link Preferences#get(String, String)} and written using {@link
   * Preferences#put(String, String)}.
   */
  final class StringType implements PreferenceType<String> {
    @Override
    public Class<String> getValueClass() {
      return String.class;
    }

    @Override
    public void set(Preferences storage, String key, String value) {
      storage.put(key, value);
    }

    @Override
    public String get(Preferences storage, String key, Supplier<String> defaultValue) {
      return storage.get(key, defaultValue.get());
    }
  }

  /**
   * Reads and writes {@code File} objects.
   *
   * <p>Files are represented as their path strings when stored. The path strings are stored as with
   * {@link StringType}.
   */
  final class FileType implements PreferenceType<File> {
    @Override
    public Class<File> getValueClass() {
      return File.class;
    }

    @Override
    public void set(Preferences storage, String key, File value) {
      storage.put(key, value.toString());
    }

    @Override
    public File get(Preferences storage, String key, Supplier<File> defaultValue) {
      String filePath = storage.get(key, null);
      if (filePath != null) {
        return new File(filePath);
      }

      return defaultValue.get();
    }
  }

  /**
   * Reads and writes arbitrary {@code Enum<T>} objects.
   *
   * <p>The enum values are represented as their names. The names are stored as with {@link
   * StringType}.
   */
  final class EnumType<T extends Enum<T>> implements PreferenceType<T> {
    private final Class<T> class_;

    public EnumType(Class<T> class_) {
      this.class_ = class_;
    }

    @Override
    public Class<T> getValueClass() {
      return class_;
    }

    @Override
    public void set(Preferences storage, String key, T value) {
      storage.put(key, value.name());
    }

    @Override
    public T get(Preferences storage, String key, Supplier<T> defaultValue) {
      var stored = storage.get(key, null);
      if (stored == null) {
        return defaultValue.get();
      }

      try {
        return Enum.valueOf(class_, stored);
      } catch (Exception e) {
        return defaultValue.get();
      }
    }
  }

  /**
   * Reads and writes {@code Color} objects.
   *
   * <p>The colors are represented by their 32-bit ARGB values. The values are stored as with {@link
   * IntegerType}.
   */
  final class ColorType implements PreferenceType<Color> {
    private final boolean hasAlpha;

    public ColorType(boolean hasAlpha) {
      this.hasAlpha = hasAlpha;
    }

    @Override
    public Class<Color> getValueClass() {
      return Color.class;
    }

    @Override
    public void set(Preferences storage, String key, Color value) {
      storage.putInt(key, value.getRGB());
    }

    @Override
    public Color get(Preferences storage, String key, Supplier<Color> defaultValue) {
      return new Color(storage.getInt(key, defaultValue.get().getRGB()), hasAlpha);
    }
  }

  /**
   * Reads and writes arrays of {@code T}.
   * <p>
   * Arrays are stored using a series of preferences<br>
   * <tt>&lt;<i>key</i>&gt;.length</tt><br>
   * <tt>&lt;<i>key</i>&gt;.0</tt><br>
   * <tt>&lt;<i>key</i>&gt;.1</tt><br>
   * <tt>&lt;<i>key</i>&gt;.2</tt><br>
   * <tt>&lt;<i>key</i>&gt;. &hellip;</tt><br>
   * <tt>&lt;<i>key</i>&gt;.&lt;<i>length-1</i>&gt;</tt>
   * </p>
   * <p>
   * The signature for get() does not permit us to supply a default value for array elements,
   * so when reading an array, if a  preference is not found for an array element, null is used.
   * </p>
   *
   * @param <T> the type of the array elements.
   */

  class ArrayType<T> implements PreferenceType<T[]> {

    private final PreferenceType<T> elementType;
    private final Class<T[]> valueClass;

    public ArrayType(PreferenceType<T> elementType) {
      this.elementType = elementType;
      this.valueClass = (Class<T[]>) java.lang.reflect.Array
          .newInstance(elementType.getValueClass(), 0)
          .getClass();
    }

    @Override
    public Class<T[]> getValueClass() {
      return valueClass;
    }

    @Override
    public void set(Preferences storage, String key, T[] value) {
      if(value == null) {
        // this will leave garbage lying about, but I don't know what the
        // preferencetype setters might have done in the preferences,
        // and preferenceType has no 'clear()' method.
        // all I can do is remove my own key
        storage.remove(key + ".length");
        return;
      }

      storage.putInt(key + ".length", value.length);
      for (int i = 0; i < value.length; i++) {
        elementType.set(storage, key + "." + i, value[i]);
      }
    }

    @Override
    public T[] get(Preferences storage, String key, Supplier<T[]> defaultValue) {
      int length = storage.getInt(key + ".length", -1);

      if (length < 0) {
        if(defaultValue == null) {
          return null;
        }
        else {
          return defaultValue.get();
        }
      }

      T[] value = (T[]) Array.newInstance(elementType.getValueClass(), length);
      for (int i = 0; i < value.length; i++) {
        value[i] = elementType.get(storage, key + "." + i, () -> null);
      }

      return value;
    }
  }

  /**
   * Reads and writes records.
   * Records are stored using a series of preferences<br>
   * <tt>&lt;<i>key</i>&gt;.class</tt><br>
   * <tt>&lt;<i>key</i>&gt;.&lt;<i>Element 0</i>&gt;</tt>
   * <tt>&lt;<i>key</i>&gt;.&lt;<i>Element 1</i>&gt;</tt>
   * <tt>&lt;<i>key</i>&gt;.&lt;<i>Element 2</i>&gt;</tt>
   * <tt>&lt;<i>key</i>&gt;.&hellip;</tt><br>
   * </p>
   * <p>
   * As a record will never have an element named 'class', <tt>&lt;<i>key</i>&gt;.class</tt>
   * is used to check whether or not the preference exists.
   * </p>
   *
   * @param <T> the record type.
   */

  class RecordType<T extends Record> implements PreferenceType<T> {

    private final Class<T> valueClass;
    private final int nElements;
    private final String[] names;
    private final MethodHandle[] accessors;
    private final Class<?>[] parameterTypes;

    // Using raw types, because correctly declaring the generics is difficult and adds no value
    private final PreferenceType[] storers;
    private final Supplier[] defaultValues;

    private final Constructor<T> constructor;

    /**
     *
     * <p>
     * The elements of the record are stored using a map of PreferenceType objects, keyed by element name.
     * If the type of the element is Boolean, Integer, Double String, or File, then a default storer will be used.
     * If there is no storer for an element, then an IllegalArgumentException is thrown.
     * If there is no defaultValue for an element, then ()-&gt;null is used. This may cause issues for
     * primitive types.
     * </p>
     * <p>
     * The storers and default values are declared as raw types, because correctly declaring the generics is difficult and adds no value.
     * </p>
     *
     * @param valueClass
     * @param storerMap
     */
    public RecordType(Class<T> valueClass,
                      Map<String, PreferenceType> storerMap,
                      Map<String, Supplier> defaultValueMap) {
      this.valueClass = valueClass;

      if (storerMap == null) {
        storerMap = Map.of();
      }

      RecordComponent[] components = valueClass.getRecordComponents();

      this.nElements = components.length;
      this.names = new String[nElements];
      this.accessors = new MethodHandle[nElements];
      this.parameterTypes = new Class<?>[nElements];
      this.storers = new PreferenceType<?>[nElements];
      this.defaultValues = new Supplier[nElements];

      try {
        this.constructor = valueClass.getDeclaredConstructor(parameterTypes);
      } catch (NoSuchMethodException e) {
        throw new RuntimeException(e);
      }

      final MethodHandles.Lookup lookup = MethodHandles.lookup();

      for (int i = 0; i < nElements; i++) {
        final RecordComponent component = valueClass.getRecordComponents()[i];
        final String name = component.getName();

        PreferenceType<?> storer = storerMap.get(name);

        if (storer == null) {
          if (Boolean.TYPE.equals(component.getType())) {
            storer = new BooleanType();
          } else if (Integer.TYPE.equals(component.getType())) {
            storer = new IntegerType();
          } else if (Double.TYPE.equals(component.getType())) {
            storer = new DoubleType();
          } else if (String.class.equals(component.getType())) {
            storer = new StringType();
          } else if (File.class.equals(component.getType())) {
            storer = new FileType();
          } else {
            throw new IllegalArgumentException("No preference type provided for element " + name + " of type " + component.getType().getSimpleName() + " in record " + valueClass.getSimpleName() + ".");
          }
        }

        names[i] = name;
        parameterTypes[i] = components[i].getType();
        try {
          accessors[i] = lookup.unreflect(components[i].getAccessor());
        } catch (IllegalAccessException e) {
          throw new RuntimeException(e);
        }
        storers[i] = storer;
        defaultValues[i] = defaultValueMap.getOrDefault(name, () -> null);
      }
    }

    @Override
    public Class<T> getValueClass() {
      return valueClass;
    }

    @Override
    public void set(Preferences storage, String key, T value) {
      if(value == null) {
        // this will leave garbage lying about, but I don't know what the
        // preferencetype setters might have done in the preferences,
        // and preferenceType has no 'clear()' method.
        // all I can do is remove my own key
        storage.remove(key + ".class");
        return;
      }

      storage.put(key + ".class", valueClass.getSimpleName());
      for (int i = 0; i < nElements; i++) {
        try {
          storers[i].set(storage, key + "." + names[i], accessors[i].invoke(value));
        } catch (RuntimeException e) {
          throw e;
        } catch (Throwable e) {
          throw new RuntimeException(e);
        }
      }
    }

    @Override
    public T get(Preferences storage, String key, Supplier<T> defaultValue) {
      String className = storage.get(key + ".class", null);
      if (className == null) {
        return defaultValue.get();
      }

      Object[] args = new Object[nElements];

      for (int i = 0; i < nElements; i++) {
        args[i] = storers[i].get(storage, key + "." + names[i], defaultValues[i]);
      }

      try {
        return constructor.newInstance(args);
      } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    }
  }

}
