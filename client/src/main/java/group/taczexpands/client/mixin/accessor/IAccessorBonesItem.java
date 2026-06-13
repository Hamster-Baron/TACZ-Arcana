package group.taczexpands.client.mixin.accessor;

import com.tacz.guns.client.resource.pojo.model.BonesItem;
import com.tacz.guns.client.resource.pojo.model.CubesItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = BonesItem.class, remap = false)
public interface IAccessorBonesItem {
    @Accessor("cubes")
    void setCubes(List<CubesItem> cubes);

    @Accessor("name")
    void setName(String name);

    @Accessor("pivot")
    void setPivot(List<Float> pivot);

    @Accessor("rotation")
    void setRotation(List<Float> rotation);

    @Accessor("parent")
    void setParent(String parent);

    @Accessor("mirror")
    void setMirror(boolean mirror);
}
