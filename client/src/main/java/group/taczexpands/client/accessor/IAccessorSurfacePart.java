package group.taczexpands.client.accessor;

/** Per-model binding state published together with the loaded model. */
public interface IAccessorSurfacePart {
    void taczexpands$setSurfaceDynamic(boolean dynamic);

    boolean taczexpands$isSurfaceDynamic();
}
