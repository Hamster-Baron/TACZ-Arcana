package group.taczexpands.common.data;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AttachmentTypeAdapter<T extends Enum<T>> extends TypeAdapter<T> {
    private final Map<String, T> nameToConstant = new HashMap<>();
    private final Map<String, T> stringToConstant = new HashMap<>();
    private final Map<T, String> constantToName = new HashMap<>();

    public AttachmentTypeAdapter(final Class<T> classOfT) {
        try {
            Field[] constantFields = AccessController.doPrivileged(new PrivilegedAction<Field[]>() {
                @Override public Field[] run() {
                    Field[] fields = classOfT.getDeclaredFields();
                    ArrayList<Field> constantFieldsList = new ArrayList<>(fields.length);
                    for (Field f : fields) {
                        if (f.isEnumConstant()) {
                            constantFieldsList.add(f);
                        }
                    }

                    Field[] constantFields = constantFieldsList.toArray(new Field[0]);
                    AccessibleObject.setAccessible(constantFields, true);
                    return constantFields;
                }
            });
            for (Field constantField : constantFields) {
                @SuppressWarnings("unchecked")
                T constant = (T)(constantField.get(null));
                String name = constant.name();
                String toStringVal = constant.toString();

                SerializedName annotation = constantField.getAnnotation(SerializedName.class);
                if (annotation != null) {
                    name = annotation.value();
                    for (String alternate : annotation.alternate()) {
                        nameToConstant.put(alternate, constant);
                    }
                }
                nameToConstant.put(name, constant);
                stringToConstant.put(toStringVal, constant);
                constantToName.put(constant, name);
            }
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }
    @Override public T read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return null;
        }
        String key = in.nextString();
        T constant = nameToConstant.get(key);
        return (constant == null) ? stringToConstant.get(key) : constant;
    }

    @Override public void write(JsonWriter out, T value) throws IOException {
        out.value(value == null ? null : constantToName.get(value));
    }
}