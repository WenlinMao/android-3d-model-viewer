package org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.animation;

/**
 * Enumeration of interpolator types
 */
public enum InterpolatorType
{
    /**
     * A linear interpolator
     */
    LINEAR,
    
    /**
     * A spherical linear interpolation (SLERP). The input values will 
     * be assumed to consist of 4 elements, which are interpreted as 
     * quaternions for the interpolation
     */
    SLERP,
    
    /**
     * A stepwise interpolation
     */
    STEP
}