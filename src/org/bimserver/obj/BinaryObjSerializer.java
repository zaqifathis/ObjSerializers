package org.bimserver.obj;

import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcAnnotation;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.plugins.renderengine.RenderEngineException;
import org.bimserver.plugins.serializers.Extends;
import org.bimserver.plugins.serializers.ProgressReporter;
import org.bimserver.plugins.serializers.SerializerException;
import org.bimserver.plugins.serializers.AbstractGeometrySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BinaryObjSerializer extends AbstractGeometrySerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryObjSerializer.class);
    private OutputStream outputStream;

    public BinaryObjSerializer() {
    }

    @Override
    protected boolean write(OutputStream outputStream, ProgressReporter progressReporter) throws SerializerException {
        LOGGER.info("Starting OBJ serialization");
        this.outputStream = outputStream;

        for (IfcProduct ifcProduct : model.getAllWithSubTypes(IfcProduct.class)) {
            if (!checkGeometry(ifcProduct, false)) continue;

            GeometryInfo geometryInfo = ifcProduct.getGeometry();
            GeometryData data = geometryInfo.getData();

            if (!"IfcFooting".equals(ifcProduct.eClass().getName())) continue;

            // Write metadata and group name
            writeLine("# _t: " + ifcProduct.eClass().getName());
            writeLine("# _guid: " + ifcProduct.getGlobalId());
            writeLine("g " + ifcProduct.getName());

            // Write vertices
            ByteBuffer verticesBuffer = ByteBuffer.wrap(data.getVertices().getData()).order(ByteOrder.LITTLE_ENDIAN);
            DoubleBuffer verticesDouble = verticesBuffer.asDoubleBuffer();
            int numVertices = verticesDouble.capacity() / 3;

            for (int i = 0; i < numVertices; i++) {
                double x = verticesDouble.get(i * 3);
                double y = verticesDouble.get(i * 3 + 1);
                double z = verticesDouble.get(i * 3 + 2);
                writeLine("v " + x + " " + y + " " + z);
            }

            // Write normals
            ByteBuffer normalsBuffer = ByteBuffer.wrap(data.getNormals().getData()).order(ByteOrder.LITTLE_ENDIAN);
            FloatBuffer normalsFloat = normalsBuffer.asFloatBuffer();
            int numNormals = normalsFloat.capacity() / 3;

            for (int i = 0; i < numNormals; i++) {
                float x = normalsFloat.get(i * 3);
                float y = normalsFloat.get(i * 3 + 1);
                float z = normalsFloat.get(i * 3 + 2);
                writeLine("vn " + x + " " + y + " " + z);
            }

            // Write face definitions (assuming vertex index == normal index)
            ByteBuffer indicesBuffer = ByteBuffer.wrap(data.getIndices().getData()).order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer indicesInt = indicesBuffer.asIntBuffer();

            for (int i = 0; i < indicesInt.capacity() / 3; i++) {
                int index1 = indicesInt.get(i * 3);
                int index2 = indicesInt.get(i * 3 + 1);
                int index3 = indicesInt.get(i * 3 + 2);

                writeLine("f " +
                        (index1 + 1) + "//" + (index1 + 1) + " " +
                        (index2 + 1) + "//" + (index2 + 1) + " " +
                        (index3 + 1) + "//" + (index3 + 1));
            }

            writeLine(""); // blank line between objects
        }

        return false;
    }

    private void writeLine(String line) {
        try {
            print(line + "\n");
        } catch (IOException e) {
            throw new RuntimeException("Failed to write line: " + line, e);
        }
    }

    private void print(String line) throws IOException {
        byte[] bytes = line.getBytes(StandardCharsets.UTF_8);
        this.outputStream.write(bytes, 0, bytes.length);
    }

    private boolean checkGeometry(IfcProduct ifcProduct, boolean print) {
        String name = ifcProduct.eClass().getName();
        if (name.equals("IfcOpeningElement") || name.equals("IfcBuildingStorey") || name.equals("IfcBuilding")) {
            return false;
        }
        GeometryInfo geometryInfo = ifcProduct.getGeometry();
        if (geometryInfo == null) {
            if (ifcProduct instanceof IfcAnnotation) {
                return false;
            }
            if (print) {
                LOGGER.info("No GeometryInfo for " + name);
            }
            return false;
        }
        GeometryData geometryData = geometryInfo.getData();
        if (geometryData == null) {
            if (print) {
                LOGGER.info("No GeometryData for " + name);
            }
            return false;
        }
        if (geometryData.getVertices() == null) {
            if (print) {
                LOGGER.info("No Vertices for " + name);
            }
            return false;
        }
        return true;
    }
}
