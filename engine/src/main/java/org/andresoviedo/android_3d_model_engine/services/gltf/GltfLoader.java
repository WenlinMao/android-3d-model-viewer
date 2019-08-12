package org.andresoviedo.android_3d_model_engine.services.gltf;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.AccessorModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.BufferModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.BufferViewModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.GltfModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.ImageModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.MeshModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.MeshPrimitiveModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.NodeModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.SceneModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.io.GltfModelReader;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.TextureModel;
import org.andresoviedo.android_3d_model_engine.services.wavefront.WavefrontLoader;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Load glTF model data to inner data structure
 *
 * @author wenlinm
 */
public class GltfLoader {

    private static Map<String, Integer> keyHandleMap = new HashMap<>();
    private static String[] textureKeys = {"baseColorTexture"
                                            , "emissiveTexture"
                                            , "occlusionTexture"
                                            , "normalTexture"};

    // read model data and fill in data in Object3DData structure
    public static Object[] buildAnimatedModel(URI uri) throws IOException{


        GltfModelReader gltfModelReader = new GltfModelReader();
        GltfModel gltfModel = gltfModelReader.read(uri);

        Log.d("GltfLoaderTask", uri.toString());

        //convert all primitives to Object3DData object
        List<Object3DData> ret = new ArrayList<>();
//        bindKeyHandle();

        int index = 1;

        // traverse all scene and for each root node, do dfs
        for (SceneModel scene : gltfModel.getSceneModels()){
            for (NodeModel node : scene.getNodeModels()){
                traverseNode(node, ret, index);
                index++;
            }
        }

        return new Object[]{gltfModel,ret};

    }

    private static void traverseNode(NodeModel node, List<Object3DData> ret, int index){
        int i = 1;
        for (MeshModel mesh : node.getMeshModels()){
            for (MeshPrimitiveModel meshPrimitive : mesh.getMeshPrimitiveModels()){


                // for each mesh primitive initialize animated model object which
                // inherited from Object3DData to hold data
                AnimatedModel data3D = new AnimatedModel();
                Map<String, AccessorModel> attriMap = meshPrimitive.getAttributes();

                // for debug identification
                if (mesh.getName() != null){
                    data3D.setId(mesh.getName() + i);
                    i++;
                } else {
                    data3D.setId("Triangle" + index);
                }

                // TODO: refactor to abstract this part

                // for each mesh primitive, check each keywords and deal with the data
                // correspondingly
                for (String key : attriMap.keySet()) {
                    if (!isKeyValid(key))
                        continue;

                    // get accessor, bufferview, and buffer for vertex buffer, normal buffer
                    // texture coordinate buffer
                    AccessorModel accessor = attriMap.get(key);
                    BufferViewModel bufferView = accessor.getBufferViewModel();
                    BufferModel buffer = bufferView.getBufferModel();
                    int size = accessor.getElementType().getNumComponents();
                    int count = accessor.getCount();
                    int type = accessor.getComponentType();
                    int offset = accessor.getByteOffset() + bufferView.getByteOffset();
                    int stride = accessor.getByteStride();
                    if(bufferView.getByteStride() != null) {
                        stride += bufferView.getByteStride();
                    }
                    int compSize = accessor.getComponentSizeInBytes();
                    int length = accessor.getCount() * size * compSize;
                    ByteBuffer bBuffer = buffer.getBufferData();
                    ByteBuffer tempbuf = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
                    byte[] byte2 = subBytes(bBuffer.array(),offset + 4,length);
                    float[] ver = byteToFloat(byte2);
                    FloatBuffer dataFB = tempbuf.asFloatBuffer();
                    dataFB.put(ver);
                    dataFB.position(0);

                    if (key.equals("POSITION")){
                        transformVertices(data3D, node, dataFB);
                        data3D.setVertexArrayBuffer(dataFB);
                        data3D.setVertices(ver);
                    } else if (key.equals("NORMAL")) {
                        data3D.setVertexNormalsArrayBuffer(dataFB);
                    } else if (key.startsWith("TEXCOORD_")){
                        data3D.addTextureCoords(key, dataFB);
                    } else if (key.startsWith("COLOR_")){
                        data3D.setVertexColorsArrayBuffer(dataFB);
                    }
                }

                // if this mesh primitive describe indexed geometry, store draw order buffer
                AccessorModel indices = meshPrimitive.getIndices();
                if (indices!=null)
                {
                    BufferViewModel indicesBufferViewModel = indices.getBufferViewModel();
                    BufferModel buffer = indicesBufferViewModel.getBufferModel();
                    int size = indices.getElementType().getNumComponents();
                    int type = indices.getComponentType();
                    int offset = indices.getByteOffset() + indicesBufferViewModel.getByteOffset();
                    int compSize = indices.getComponentSizeInBytes();
                    int length = indices.getCount()*size*compSize;
                    ByteBuffer bBuffer = buffer.getBufferData();
                    ByteBuffer tempbuf = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
                    byte[] byte2 = subBytes(bBuffer.array(),offset+4,length);

                    Buffer indexBuffer = null;

                    if (type == GLES20.GL_UNSIGNED_SHORT){
                        short[] ver = bytesToShort(byte2);
                        indexBuffer = tempbuf.asShortBuffer();
                        ((ShortBuffer)indexBuffer).put(ver);
                    } else if (type == GLES20.GL_UNSIGNED_INT) {
                        int[] ver = bytesToInt(byte2);
                        indexBuffer = tempbuf.asIntBuffer();
                        ((IntBuffer)indexBuffer).put(ver);
                    } else if (type == GLES20.GL_UNSIGNED_BYTE){
                        indexBuffer = tempbuf;
                        ((ByteBuffer)indexBuffer).put(byte2);
                    }
                    indexBuffer.position(0);
                    data3D.setDrawOrder(indexBuffer);
                    data3D.setDrawOrderBufferType(type);
                    data3D.setDrawUsingArrays(false);
                }

                // TODO: add technique model to shader for realistic PBR
//                TechniqueModel test2 = meshPrimitive.getMaterialModel().getTechniqueModel();


                data3D.setGltfMaterial(meshPrimitive.getMaterialModel());

                data3D.setDrawMode(meshPrimitive.getMode());
                WavefrontLoader.ModelDimensions modelDimensions = new WavefrontLoader.ModelDimensions();
                data3D.setDimensions(modelDimensions);

                ret.add(data3D);
            }
        }

        if (node.getChildren().isEmpty() == true){
            return;
        } else {
            for (NodeModel child : node.getChildren()){
                traverseNode(child, ret, ++index);
            }
        }
    }


    public static void populateAnimatedModel(URL url, List<Object3DData> datas, GltfModel modelData){

        for (int i=0; i<datas.size(); i++) {
            Object3DData data = datas.get(i);

            // Parse all facets...
//            double[] normal = new double[3];
//            double[][] vertices = new double[3][3];
//            int normalCounter = 0, vertexCounter = 0;

            FloatBuffer normalsBuffer = data.getVertexNormalsArrayBuffer();
            FloatBuffer vertexBuffer = data.getVertexArrayBuffer();
            Buffer indexBuffer = data.getDrawOrderBuffer();

//            vertexBuffer.put(data.getVertices());
//            normalsBuffer.put(data.getNormals());

            WavefrontLoader.ModelDimensions modelDimensions = data.getDimensions();

            boolean first = true;
            for (int counter = 0; counter < data.getVertices().length - 3; counter += 3) {

                // update model dimensions
                if (first) {
                    modelDimensions.set(data.getVertices()[counter], data.getVertices()[counter + 1], data.getVertices()[counter + 2]);
                    first = false;
                }
                modelDimensions.update(data.getVertices()[counter], data.getVertices()[counter + 1], data.getVertices()[counter + 2]);

            }

            bindTexture(data, modelData);
//            Log.i("ColladaLoaderTask", "Building 3D object '"+meshData.getId()+"'...");
//            data.setId(meshData.getId());
//            vertexBuffer.put(meshData.getVertices());
//            normalsBuffer.put(meshData.getNormals());

//            indexBuffer.put(meshData.getIndices());
            data.setFaces(new WavefrontLoader.Faces(vertexBuffer.capacity() / 3));
            data.setDrawOrder(indexBuffer);

//            data.setIsDoubleSided((Integer)1);

            // Load skeleton and animation
//            AnimatedModel data3D = (AnimatedModel) data;
//            try {
//
//                // load skeleton
//                SkeletonData skeletonData = modelData.getJointsData();
//                Joint headJoint = createJoints(skeletonData.headJoint);
//                data3D.setRootJoint(headJoint, skeletonData.jointCount, skeletonData.boneCount, false);
//
//                // load animation
//                Animation animation = loadAnimation(url.openStream());
//                data3D.doAnimation(animation);
//
//            } catch (Exception e) {
//                Log.e("ColladaLoader", "Problem loading model animation' " + e.getMessage(), e);
//                data3D.doAnimation(null);
//            }
        }
    }

//    // TODO: consider replace function
//    // different action for different attribute in mesh
//    private static void bindKeyHandle()
//    {
//        // TODO: lambda function not working, research on how two parameter lambda function works
////        keyHandleMap.put("POSITION", (buffer, obj) -> obj.setVertexBuffer(buffer));
////        keyHandleMap.put("NORMAL", (buffer, obj) -> obj.setVertexNormalsArrayBuffer(buffer));
////        keyHandleMap.put("TEXCOORD_0", (buffer, obj) -> obj.setTextureCoordsArrayBuffer(buffer));
//
//        keyHandleMap.put("POSITION", 1);
//        keyHandleMap.put("NORMAL", 2);
//        keyHandleMap.put("TEXCOORD_0", 3);
//    }


    private static void bindTexture(Object3DData data, GltfModel gltfModel){
        List<TextureModel> textures = gltfModel.getTextureModels();
        Map<String,Object> materialValueMap = data.getGltfMaterial().getValues();

//        for (String key : textureKeys){
//            if (materialValueMap.get(key) != null){
//                Integer index = (Integer)materialValueMap.get(key);
//                String texCordKey = (String)materialValueMap.get(key);
//            }
//        }

        // Default Texture
        if (materialValueMap.get("baseColorTexture") != null){
            Integer index = (Integer)materialValueMap.get("baseColorTexture");
            String texCordKey = (String)materialValueMap.get("baseColorTexCoord");
            data.setTextureCoordsArrayBuffer(data.getTextureCoords(texCordKey));
            TextureModel baseColorTexture = textures.get(index);
            ImageModel image = baseColorTexture.getImageModel();
            // Faster way
            data.setTextureFile(null);
            ByteBuffer imageByteBuffer = image.getImageData();
            byte[] imageByte = byteBufferToByte(imageByteBuffer);
            data.setTextureData(imageByte);
            data.setFilter(baseColorTexture.getMinFilter(), baseColorTexture.getMagFilter());
            data.setTextureWrap(baseColorTexture.getWrapS(), baseColorTexture.getWrapT());
        }

        // TODO: finish emissive texture logic
        // Emissive Texture
        if (materialValueMap.get("emissiveTexture") != null){
            Integer index = (Integer)materialValueMap.get("emissiveTexture");
            String texCordKey = (String)materialValueMap.get("emissiveTexCoord");
            data.setEmissiveTextureCoordsArrayBuffer(data.getTextureCoords(texCordKey));
            TextureModel emissiveTexture = textures.get(index);
            ImageModel image = emissiveTexture.getImageModel();
            // Faster way
            ByteBuffer imageByteBuffer = image.getImageData();
            byte[] imageByte = byteBufferToByte(imageByteBuffer);
            data.setEmissiveTextureData(imageByte);
            data.setEmissiveFilter(emissiveTexture.getMinFilter(), emissiveTexture.getMagFilter());
            data.setEmissiveTextureWrap(emissiveTexture.getWrapS(), emissiveTexture.getWrapT());
        }

        if (materialValueMap.get("occlusionTexture") != null){

        }

        if (materialValueMap.get("normalTexture") != null){

        }
        // TODO: set overall material color
        data.setColor((float[])materialValueMap.get("baseColorFactor"));
        data.setIsDoubleSided((Integer)materialValueMap.get("isDoubleSided"));
    }

    private static FloatBuffer transformVertices(Object3DData obj, NodeModel node, FloatBuffer dataFB){

        // model matrix of the node
        obj.setModelMatrix(node.getMatrix());

        // if model matrix is not assigned, use translation, rotation, and scale attribute
        obj.setScale(node.getScale());
        obj.setRotation(node.getRotation());
        obj.setPosition(node.getTranslation());

        float[] modelMatrix = obj.getModelMatrix();

        for (int i = 0; i < dataFB.capacity(); i += 3) {
            float[] ver = {dataFB.get(i), dataFB.get(i + 1), dataFB.get(i + 2), 1};
            Matrix.multiplyMV(ver, 0, modelMatrix, 0, ver, 0);
            dataFB.put(i, ver[0]);
            dataFB.put(i + 1, ver[1]);
            dataFB.put(i + 2, ver[2]);
        }

        return dataFB;
    }

    private static boolean isKeyValid(String key){
        return key.equals("POSITION") || key.equals("NORMAL")
                || key.startsWith("TEXCOORD_") || key.startsWith("COLOR_");
    }

    private static float byte2float(byte[] b, int index) {
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long) b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long) b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long) b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }

    private static short[] bytesToShort(byte[] bytes) {
        if(bytes==null){
            return null;
        }
        short[] shorts = new short[bytes.length/2];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);
        return shorts;
    }

    private static int[] bytesToInt(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        int[] ints = new int[bytes.length / 4];
        ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(ints);
        return ints;
    }

    private static byte[] byteBufferToByte(ByteBuffer byteBuffer){
        if (byteBuffer == null){
            return null;
        }
        byte[] arr = new byte[byteBuffer.remaining()];
        byteBuffer.get(arr);

        return arr;
    }

    private static float[] byteToFloat(byte[] input) {
        float[] ret = new float[input.length/4];
        for (int x = 0; x < input.length; x+=4) {
//            ret[x/4] = ByteBuffer.wrap(input, x, 4).getFloat();
            ret[x/4] = byte2float(input,x);
        }
        return ret;
    }

    private static byte[] subBytes(byte[] src, int offset, int count) {
        byte[] bs = new byte[count];
        for (int i=0;i<count; i++) bs[i] = src[i+offset];
        return bs;
    }


}
