package bletch.tektopiathief.entities.render;

import bletch.tektopiathief.core.ModDetails;
import bletch.tektopiathief.entities.EntityThief;
import com.leviathanstudio.craftstudio.client.model.ModelCraftStudio;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.tangotek.tektopia.client.LayerVillagerHeldItem;

public class RenderThief<T extends EntityThief> extends RenderLiving<T> {
    public static final RenderThief.Factory<EntityThief> FACTORY;

    protected final String textureName;
    protected final ModelCraftStudio maleModel;
    protected final ModelCraftStudio femaleModel;
    protected ResourceLocation[] maleTextures;
    protected ResourceLocation[] femaleTextures;

    public RenderThief(RenderManager manager) {
        this(manager, EntityThief.MODEL_NAME, false, 64, 64, EntityThief.MODEL_NAME, 0.4F);
    }

    public RenderThief(RenderManager manager, String modelName, boolean hasGenderModels, int textureWidth, int textureHeight, String textureName, float shadowSize) {
        super(manager, new ModelCraftStudio(ModDetails.MOD_ID, modelName + "_m", textureWidth, textureHeight), shadowSize);

        this.addLayer(new LayerVillagerHeldItem(this));
        this.textureName = textureName;
        this.maleModel = (ModelCraftStudio) this.mainModel;
        if (hasGenderModels) {
            this.femaleModel = new ModelCraftStudio(ModDetails.MOD_ID, modelName + "_f", textureWidth, textureHeight);
        } else {
            this.femaleModel = null;
        }

        this.setupTextures();
    }

    protected void setupTextures() {
        this.maleTextures = new ResourceLocation[]{new ResourceLocation(ModDetails.MOD_ID, "textures/entity/" + this.textureName + "_m.png")};
        this.femaleTextures = new ResourceLocation[]{new ResourceLocation(ModDetails.MOD_ID, "textures/entity/" + this.textureName + "_f.png")};
    }

    public void doRender(T entity, double x, double y, double z, float entityYaw, float partialTicks) {
        if (!entity.isMale() && this.femaleModel != null) {
            this.mainModel = this.femaleModel;
        } else {
            this.mainModel = this.maleModel;
        }

        super.doRender(entity, x, y, z, entityYaw, partialTicks);
    }

    @Override
    protected ResourceLocation getEntityTexture(T entity) {
        return entity.isMale() ? this.maleTextures[0] : this.femaleTextures[0];
    }

    public static class Factory<T extends EntityThief> implements IRenderFactory<T> {
        public Render<? super T> createRenderFor(RenderManager manager) {
            return new RenderThief<EntityThief>(manager);
        }
    }

    static {
        FACTORY = new RenderThief.Factory<>();
    }

}
