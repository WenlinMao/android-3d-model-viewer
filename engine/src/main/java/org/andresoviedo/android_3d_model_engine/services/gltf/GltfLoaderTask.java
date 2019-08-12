package org.andresoviedo.android_3d_model_engine.services.gltf;

import android.app.Activity;
import android.net.Uri;

import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.LoaderTask;
import org.andresoviedo.android_3d_model_engine.services.gltf.jgltf_model.GltfModel;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;

import org.andresoviedo.android_3d_model_engine.services.gltf.GltfLoader;

public class GltfLoaderTask extends LoaderTask {

    GltfModel modelData;

    public GltfLoaderTask(Activity parent, Uri uri, Callback callback) {
        super(parent, uri, callback);
    }

    @Override
    protected List<Object3DData> build() throws IOException, URISyntaxException {

        Object[] ret = GltfLoader.buildAnimatedModel(new URI(uri.toString()));
        List<Object3DData> datas = (List<Object3DData>) ret[1];
        modelData = (GltfModel) ret[0];

        return datas;
    }

    @Override
    protected void build(List<Object3DData> datas) throws Exception {
        GltfLoader.populateAnimatedModel(new URL(uri.toString()), datas, modelData);
        if (datas.size() == 1) {
            datas.get(0).centerAndScale(5, new float[]{0, 0, 0});
        } else {
            Object3DData.centerAndScale(datas, 5, new float[]{0, 0, 0});
        }
    }

}
