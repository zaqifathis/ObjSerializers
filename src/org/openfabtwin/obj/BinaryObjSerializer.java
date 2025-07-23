package org.openfabtwin.obj;

import org.bimserver.geometry.Matrix;
import org.bimserver.models.geometry.GeometryData;
import org.bimserver.models.geometry.GeometryInfo;
import org.bimserver.models.ifc2x3tc1.IfcAnnotation;
import org.bimserver.models.ifc2x3tc1.IfcProduct;
import org.bimserver.plugins.serializers.ProgressReporter;
import org.bimserver.plugins.serializers.SerializerException;
import org.bimserver.plugins.serializers.AbstractGeometrySerializer;
import org.bimserver.utils.GeometryUtils;
import org.bimserver.utils.UTF8PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.nio.*;

public class BinaryObjSerializer extends AbstractGeometrySerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryObjSerializer.class);
    private UTF8PrintWriter writer;

    public BinaryObjSerializer() {
    }

    @Override
    protected boolean write(OutputStream outputStream, ProgressReporter progressReporter) throws SerializerException {
        LOGGER.info("Starting OBJ serialization");

        this.writer = new UTF8PrintWriter(outputStream);
        int totalVertexNum = 0;

        for (IfcProduct ifcProduct : model.getAllWithSubTypes(IfcProduct.class)) {
            if (!checkGeometry(ifcProduct, false)) continue;

            GeometryInfo geometryInfo = ifcProduct.getGeometry();
            GeometryData data = geometryInfo.getData();

            writeLine("g " + ifcProduct.getName());

            //transformation matrix
            ByteBuffer transformationBuffer = ByteBuffer.wrap(ifcProduct.getGeometry().getTransformation()).order(ByteOrder.LITTLE_ENDIAN);
            DoubleBuffer transformationDouble = transformationBuffer.asDoubleBuffer();
            double[] matrix = new double[16];
            for (int i = 0; i < 16; i++) {
                matrix[i] = transformationDouble.get(i);
            }

            // Write vertices
            GeometryUtils.toDoubleArray(data.getVertices().getData());
            ByteBuffer verticesBuffer = ByteBuffer.wrap(data.getVertices().getData()).order(ByteOrder.LITTLE_ENDIAN);
            DoubleBuffer verticesDouble = verticesBuffer.asDoubleBuffer();
            int numVertices = data.getNrVertices();

            for (int i = 0; i < numVertices / 3; i++) {
                double x = verticesDouble.get(i * 3);
                double y = verticesDouble.get(i * 3 + 1);
                double z = verticesDouble.get(i * 3 + 2);

                double[] input = new double[] {x, y, z, 1} ;
                double[] output = new double[4];
                Matrix.multiplyMV(output, 0, matrix, 0, input, 0);

                writeLine("v " + output[0] + " " + output[1] + " " + output[2]);
            }

            // Write normals
            ByteBuffer normalsBuffer = ByteBuffer.wrap(data.getNormals().getData()).order(ByteOrder.LITTLE_ENDIAN);
            FloatBuffer normalsFloat = normalsBuffer.asFloatBuffer();
            int numNormals = data.getNrNormals();

            for (int i = 0; i < numNormals / 3; i++) {
                float x = normalsFloat.get(i * 3);
                float y = normalsFloat.get(i * 3 + 1);
                float z = normalsFloat.get(i * 3 + 2);
                float[] normal = new float[] { x, y, z };

                writeLine("vn " + normal[0] + " " + normal[1] + " " + normal[2]);
            }

            // Write face
            ByteBuffer indicesBuffer = ByteBuffer.wrap(data.getIndices().getData()).order(ByteOrder.LITTLE_ENDIAN);
            IntBuffer indicesInt = indicesBuffer.asIntBuffer();

            for (int i = 0; i < data.getNrIndices() / 3; i++) {
                int index1 = indicesInt.get(i * 3);
                int index2 = indicesInt.get(i * 3 + 1);
                int index3 = indicesInt.get(i * 3 + 2);

                writeLine("f " +
                        (index1 + 1 + totalVertexNum) + "//" + (index1 + 1 + totalVertexNum) + " " +
                        (index2 + 1 + totalVertexNum) + "//" + (index2 + 1 + totalVertexNum) + " " +
                        (index3 + 1 + totalVertexNum) + "//" + (index3 + 1 + totalVertexNum));
            }

            totalVertexNum += numVertices / 3;
            writeLine("");
        }

        writer.flush();
        writer.close();
        return false;
    }

    private void writeLine(String line) {
        writer.println(line);
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
