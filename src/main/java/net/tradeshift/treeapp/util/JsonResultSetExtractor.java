package net.tradeshift.treeapp.util;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.SQLException;

public class JsonResultSetExtractor implements ResultSetExtractor<Void> {

    private final OutputStream outputStream;

    public JsonResultSetExtractor(final OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    private static void writeResultSetToJson(final ResultSet resultSet, final JsonGenerator jsonGenerator)
            throws SQLException, IOException {
        final var resultSetMetaData = resultSet.getMetaData();
        final var columnCount = resultSetMetaData.getColumnCount();
        jsonGenerator.writeStartArray();
        while (resultSet.next()) {
            jsonGenerator.writeStartObject();
            for (var i = 1; i <= columnCount; i++) {
                jsonGenerator.writeObjectField(resultSetMetaData.getColumnName(i), resultSet.getObject(i));
            }
            jsonGenerator.writeEndObject();
        }
        jsonGenerator.writeEndArray();
    }

    @SneakyThrows
    @Override
    public Void extractData(ResultSet resultSet) {
        final var objectMapper = new ObjectMapper();
        try (var jsonGenerator = objectMapper.getFactory().createGenerator(outputStream, JsonEncoding.UTF8)) {

            writeResultSetToJson(resultSet, jsonGenerator);
            jsonGenerator.flush();

        }
        return null;
    }
}
