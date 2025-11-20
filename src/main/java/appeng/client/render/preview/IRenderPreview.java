package appeng.client.render.preview;

import java.util.List;

public interface IRenderPreview {

    void renderPreview();

    List<Class<?>> validItemClass();
}
