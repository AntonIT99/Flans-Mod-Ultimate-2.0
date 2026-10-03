package net.irisshaders.iris.api.v0;

/**
 * Test stand-in for the public API that Oculus and Iris share, limited to the queries Flan's Mod makes.
 * The real interface has more members; the reflective binding only relies on these.
 */
public interface IrisApi
{
    static IrisApi getInstance()
    {
        return Stub.INSTANCE;
    }

    boolean isShaderPackInUse();

    boolean isRenderingShadowPass();

    final class Stub implements IrisApi
    {
        public static final Stub INSTANCE = new Stub();
        public boolean shaderPackInUse;
        public boolean renderingShadowPass;

        @Override
        public boolean isShaderPackInUse()
        {
            return shaderPackInUse;
        }

        @Override
        public boolean isRenderingShadowPass()
        {
            return renderingShadowPass;
        }
    }
}
