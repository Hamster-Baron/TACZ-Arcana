package group.taczexpands.client.accessor;

public interface IAccessorBedrockModelPOJO {
    boolean taczexpands$isPatched();
    void taczexpands$setPatched(boolean value);

    /** Marks that depth separation has run once on this shared POJO; returns false if it already had. */
    boolean taczexpands$markDepthSeparated();
}
