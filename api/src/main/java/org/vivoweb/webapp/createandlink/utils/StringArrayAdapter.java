package org.vivoweb.webapp.createandlink.utils;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StringArrayAdapter extends TypeAdapter<String[]> {
    @Override
    public void write(JsonWriter jsonWriter, String[] strings) throws IOException {
    }

    public String[] read(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return null;
        }

        if (reader.peek() == JsonToken.BEGIN_ARRAY) {
            reader.beginArray();
            List<String> list = new ArrayList<String>();
            while (reader.hasNext()) {
                list.add(reader.nextString());
            }
            reader.endArray();
            return list.toArray(new String[list.size()]);
        } else if (reader.peek() == JsonToken.STRING) {
            return new String[] { reader.nextString() };
        }

        return null;
    }
}
