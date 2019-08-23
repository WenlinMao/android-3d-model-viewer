/*
 * www.javagl.de - JglTF
 *
 * Copyright 2015-2017 Marco Hutter - http://www.javagl.de
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.impl;

import android.opengl.GLES20;

import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.AccessorData;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.AccessorDatas;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.AccessorModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.Accessors;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.BufferModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.BufferViewModel;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.ElementType;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

/**
 * Implementation of an {@link AccessorModel}
 */
public final class  DefaultAccessorModel extends AbstractNamedModelElement
    implements AccessorModel
{
    /**
     * The component type, as a GL constant
     */
    private final int componentType;
    
    /**
     * The offset in bytes, referring to the buffer view
     */
    private int byteOffset;
    
    /**
     * The {@link BufferViewModel} for this model
     */
    private BufferViewModel bufferViewModel;
    
    /**
     * The {@link ElementType} of this accessor
     */
    private final ElementType elementType;
    
    /**
     * The number of elements
     */
    private final int count;
    
    /**
     * The stride between the start of one element and the next
     */
    private int byteStride;
    
    /**
     * The {@link AccessorData}
     */
    private AccessorData accessorData;
    
    /**
     * The minimum components
     */
    private Number[] max;
    
    /**
     * The maximum components
     */
    private Number[] min;
    
    /**
     * Creates a new instance
     * 
     * @param componentType The component type GL constant
     * @param count The number of elements
     * @param elementType The element type
     */
    public DefaultAccessorModel(
        int componentType,
        int count, 
        ElementType elementType)
    {
        this.componentType = componentType;
        this.count = count;
        this.elementType = elementType;
    }
    
    /**
     * Set the {@link BufferViewModel} for this model
     * 
     * @param bufferViewModel The {@link BufferViewModel}
     */
    public void setBufferViewModel(BufferViewModel bufferViewModel)
    {
        this.bufferViewModel = bufferViewModel;
    }
    
    /**
     * Set the byte offset, referring to the {@link BufferViewModel}
     * 
     * @param byteOffset The byte offset
     */
    public void setByteOffset(int byteOffset)
    {
        this.byteOffset = byteOffset;
    }
    
    /**
     * Set the byte stride, indicating the number of bytes between the start
     * of one element and the start of the next element
     * 
     * @param byteStride The byte stride
     */
    public void setByteStride(int byteStride)
    {
        this.byteStride = byteStride;
    }

    @Override
    public BufferViewModel getBufferViewModel()
    {
        return bufferViewModel;
    }
    
    @Override
    public int getComponentType()
    {
        return componentType;
    }
    
    @Override
    public Class<?> getComponentDataType()
    {
        return Accessors.getDataTypeForAccessorComponentType(
            getComponentType());
    }
    
    @Override
    public int getComponentSizeInBytes()
    {
        return Accessors.getNumBytesForAccessorComponentType(componentType);
    }
    
    @Override
    public int getElementSizeInBytes()
    {
        return elementType.getNumComponents() * getComponentSizeInBytes();
    }
    
    @Override
    public int getByteOffset()
    {
        return byteOffset;
    }
    
    @Override
    public int getCount()
    {
        return count;
    }
    
    @Override
    public ElementType getElementType()
    {
        return elementType;
    }
    
    @Override
    public int getByteStride()
    {
        return byteStride;
    }
    
    @Override
    public AccessorData getAccessorData()
    {
        if (accessorData == null)
        {
            accessorData = AccessorDatas.create(this);
        }
        return accessorData;
    }
    
    
    @Override
    public Number[] getMin()
    {
        if (min == null)
        {
            min = AccessorDatas.computeMin(getAccessorData());
        }
        return min.clone();
    }
    
    @Override
    public Number[] getMax()
    {
        if (max == null)
        {
            max = AccessorDatas.computeMax(getAccessorData());
        }
        return max.clone();
    }

    @Override
    public Buffer getCorrBufferData(){
        BufferViewModel bufferView = this.getBufferViewModel();
        BufferModel buffer = bufferView.getBufferModel();
        int size = this.getElementType().getNumComponents();
        int count = this.getCount();
        int type = this.getComponentType();
        int offset = this.getByteOffset() + bufferView.getByteOffset();
        int stride = this.getByteStride();
        if(bufferView.getByteStride() != null) {
            stride += bufferView.getByteStride();
        }
        int compSize = this.getComponentSizeInBytes();
        int length = this.getCount() * size * compSize;
        ByteBuffer bBuffer = buffer.getBufferData();
        ByteBuffer tempbuf = ByteBuffer.allocateDirect(length).order(ByteOrder.nativeOrder());
        byte[] byte2 = subBytes(bBuffer.array(),offset + 4,length);

        Buffer retBuffer = null;

        if (type == GLES20.GL_UNSIGNED_SHORT){
            short[] ver = bytesToShort(byte2);
            retBuffer = tempbuf.asShortBuffer();
            ((ShortBuffer)retBuffer).put(ver);
//            tempbuf.asShortBuffer().put(ver);
        } else if (type == GLES20.GL_UNSIGNED_INT) {
            int[] ver = bytesToInt(byte2);
            retBuffer = tempbuf.asIntBuffer();
            ((IntBuffer)retBuffer).put(ver);
//            tempbuf.asIntBuffer().put(ver);
        } else if (type == GLES20.GL_UNSIGNED_BYTE){
            retBuffer = tempbuf;
            ((ByteBuffer)retBuffer).put(byte2);
//            tempbuf.put(byte2);
        } else if (type == GLES20.GL_FLOAT){
            float[] ver = byteToFloat(byte2);
            retBuffer = tempbuf.asFloatBuffer();
            ((FloatBuffer)retBuffer).put(ver);
//            float[] ver = byteToFloat(byte2);
//            tempbuf.asFloatBuffer().put(ver);
        }
//        tempbuf.position(0);
        retBuffer.position(0);

        return retBuffer;
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
