package org.bimserver.obj;

import org.bimserver.plugins.serializers.ProgressReporter;
import org.bimserver.plugins.serializers.SerializerException;
import org.bimserver.plugins.serializers.AbstractGeometrySerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;

public class BinaryObjSerializer extends AbstractGeometrySerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(BinaryObjSerializer.class);

    @Override
    protected boolean write(OutputStream outputStream, ProgressReporter progressReporter) throws SerializerException {
        LOGGER.info("Writing OBJ binary serializer to output stream");

        return false;
    }
}

