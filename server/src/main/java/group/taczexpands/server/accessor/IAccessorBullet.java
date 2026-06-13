package group.taczexpands.server.accessor;

import com.tacz.guns.entity.EntityKineticBullet;
import group.taczexpands.server.entity.BulletExtraData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface IAccessorBullet {
    BulletExtraData taczexpands$getBulletExtraData();

    void taczexpands$onDiscard();

    void taczexpands$initCustomData(double x, double y, double z, float speed);

    boolean taczexpands$isGenerated();

    Vec3 taczexpands$getOverridePosition();

    @Nullable
    ItemStack taczexpands$getGunItem();

    int taczexpands$getPenetration();

    static int getPenetration(EntityKineticBullet bullet) {
        return ((IAccessorBullet) bullet).taczexpands$getPenetration();
    }
}
