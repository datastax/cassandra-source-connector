package com.datastax.oss.pulsar.source.converters;

import com.datastax.cassandra.cdc.converter.Converter;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.metadata.schema.ColumnMetadata;
import com.datastax.oss.driver.api.core.metadata.schema.TableMetadata;
import com.datastax.oss.protocol.internal.ProtocolConstants;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.schema.*;
import org.apache.pulsar.client.impl.schema.generic.GenericSchemaImpl;
import org.apache.pulsar.common.schema.SchemaInfo;
import org.apache.pulsar.common.schema.SchemaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public abstract class AbstractGenericConverter implements Converter<GenericRecord, Row, Object[]> {

    GenericSchema<GenericRecord> schema;
    TableMetadata tableMetadata;
    Set<String> columns;

    public AbstractGenericConverter(TableMetadata tableMetadata, Set<String> columns, SchemaType schemaType) {
        RecordSchemaBuilder recordSchemaBuilder =
                SchemaBuilder.record(tableMetadata.getKeyspace()+"."+tableMetadata.getName());
        for(ColumnMetadata cm : tableMetadata.getColumns().values()) {
            if (columns.contains(cm.getName().toString())) {
                FieldSchemaBuilder fieldSchemaBuilder = recordSchemaBuilder.field(cm.getName().toString());
                switch(cm.getType().getProtocolCode()) {
                    case ProtocolConstants.DataType.ASCII:
                    case ProtocolConstants.DataType.VARCHAR:
                        fieldSchemaBuilder.type(SchemaType.STRING);
                        break;
                    case ProtocolConstants.DataType.INT:
                        fieldSchemaBuilder.type(SchemaType.INT32);
                        break;
                    case ProtocolConstants.DataType.BIGINT:
                        fieldSchemaBuilder.type(SchemaType.INT64);
                        break;
                    case ProtocolConstants.DataType.BOOLEAN:
                        fieldSchemaBuilder.type(SchemaType.BOOLEAN);
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported DataType=" + cm.getType().getProtocolCode());
                }
            }
        }
        SchemaInfo schemaInfo = recordSchemaBuilder.build(schemaType);
        this.schema = GenericSchemaImpl.of(schemaInfo);
        this.tableMetadata = tableMetadata;
        this.columns = columns;
    }

    @Override
    public Schema<GenericRecord> getSchema() {
        return this.schema;
    }

    @Override
    public GenericRecord toConnectData(Row row) {
        GenericRecordBuilder genericRecordBuilder = schema.newRecordBuilder();
        for(ColumnDefinition cm : row.getColumnDefinitions()) {
            if (columns.contains(cm.getName().toString())) {
                switch(cm.getType().getProtocolCode()) {
                    case ProtocolConstants.DataType.ASCII:
                    case ProtocolConstants.DataType.VARCHAR:
                        genericRecordBuilder.set(cm.getName().toString(), row.getString(cm.getName()));
                        break;
                    case ProtocolConstants.DataType.INT:
                        genericRecordBuilder.set(cm.getName().toString(), row.getInt(cm.getName()));
                        break;
                    case ProtocolConstants.DataType.BIGINT:
                        genericRecordBuilder.set(cm.getName().toString(), row.getLong(cm.getName()));
                        break;
                    case ProtocolConstants.DataType.BOOLEAN:
                        genericRecordBuilder.set(cm.getName().toString(), row.getBoolean(cm.getName()));
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported DataType=" + cm.getType().getProtocolCode());
                }
            }
        }
        return genericRecordBuilder.build();
    }

    /**
     * Convert GenericRecord to primary key column values.
     * @param genericRecord
     * @return
     */
    @Override
    public Object[] fromConnectData(GenericRecord genericRecord) {
        List<Object> pk = new ArrayList<>();
        for(Field field : genericRecord.getFields()) {
            pk.add(genericRecord.getField(field.getName()));
        }
        return pk.toArray(new Object[pk.size()]);
    }

}
