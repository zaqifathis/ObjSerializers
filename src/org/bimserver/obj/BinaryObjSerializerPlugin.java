package org.bimserver.obj;

import java.util.Set;

import org.bimserver.emf.Schema;
import org.bimserver.plugins.PluginConfiguration;
import org.bimserver.plugins.PluginContext;
import org.bimserver.plugins.serializers.AbstractSerializerPlugin;
import org.bimserver.plugins.serializers.Serializer;
import org.bimserver.shared.exceptions.PluginException;

public class BinaryObjSerializerPlugin extends AbstractSerializerPlugin {

    @Override
    public void init(PluginContext pluginContext, PluginConfiguration pluginConfiguration) throws PluginException {

    }

    public Serializer createSerializer(PluginConfiguration pluginConfiguration) {
        return new BinaryObjSerializer();
    }

    @Override
    public String getDefaultContentType() {
        return "model/obj";
    }

    @Override
    public String getDefaultExtension() {
        return "obj";
    }

    @Override
    public Set<Schema> getSupportedSchemas() {
        return Schema.GEOMETRY.toSet();
    }

    @Override
    public String getOutputFormat(Schema schema) {
        return "GEOMETRY";
    }

}
