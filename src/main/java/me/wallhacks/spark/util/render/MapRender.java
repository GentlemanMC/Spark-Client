package me.wallhacks.spark.util.render;

import me.wallhacks.spark.Spark;
import me.wallhacks.spark.gui.dvdpanels.GuiPanelBase;
import me.wallhacks.spark.manager.FontManager;
import me.wallhacks.spark.manager.MapManager;
import me.wallhacks.spark.manager.WaypointManager;
import me.wallhacks.spark.systems.clientsetting.clientsettings.ClientConfig;
import me.wallhacks.spark.systems.clientsetting.clientsettings.MapConfig;
import me.wallhacks.spark.util.GuiUtil;
import me.wallhacks.spark.util.MC;
import me.wallhacks.spark.util.maps.SparkMap;
import me.wallhacks.spark.util.objects.*;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;

public class MapRender implements MC {


    private static final ResourceLocation ARROW_ICON = new ResourceLocation("textures/icons/arrowicon.png");


    public enum MapGrid {
        Chunks(16, new Vec2i(200,Integer.MAX_VALUE), new Color(0xFF4D4D57, true)),
        Regions(16*32, new Vec2i(40,450), new Color(0xFF9F9FBD, true)),
        Sector(4096, new Vec2i(0,80), new Color(0xFF6363D3, true));



        final int size;
        final float scaledSize;

        final Vec2i range;

        final Color color;



        MapGrid(int sizeInBlocks, Vec2i range, Color color) {
            this.size = sizeInBlocks;
            this.scaledSize = sizeInBlocks/SparkMap.scale;
            this.range = range;

            this.color = color;

        }
    }


    public static void RenderWholeMap(int ImageStartX, int ImageStartY, int ImageScaleX, int ImageScaleY, int ImageScale, double TargetX, double TargetZ, double offsetX, double offsetY, int dim, double mouseX, double mouseY, boolean hover,boolean drawGrid, boolean showBiomes) {
        GL11.glPushMatrix();
        GuiUtil.glScissor(ImageStartX, ImageStartY, ImageScaleX, ImageScaleY);
        GL11.glEnable(GL11.GL_SCISSOR_TEST);



        //background
        Gui.drawRect(ImageStartX, ImageStartY, ImageStartX + ImageScaleX, ImageStartY + ImageScaleY, new Color(68, 68, 68, 165).getRGB());
        GlStateManager.color(1, 1, 1, 1);


        float centerX = (float) (SparkMap.get2dMapPosFromWorldPos(TargetX, ImageScale) - ImageScaleX * 0.5 - (offsetX));
        float centerY = (float) (SparkMap.get2dMapPosFromWorldPos(TargetZ, ImageScale) - ImageScaleY * 0.5 - (offsetY));

        Vec2i WholeMapStartPos = SparkMap.getMapPosFrom2dMapPos(centerX, centerY, ImageScale);
        Vec2i WholeMapEndPos = SparkMap.getMapPosFrom2dMapPos(centerX + ImageScaleX, centerY + ImageScaleY, ImageScale);


        float thick = 0.4f;

        if(drawGrid)
        for (int i = 0; i < MapGrid.values().length; i++) {
            MapGrid grid = MapGrid.values()[i];
            if(grid.range.x < ImageScale && ImageScale < grid.range.y)
            {
                float scale = grid.scaledSize/SparkMap.size*ImageScale;

                float conv = grid.size/SparkMap.getWidthAndHeight();

                //this code can be made better in so many ways :(
                float x_start = (float) (ImageStartX+ (Math.floor(WholeMapStartPos.x/conv)*conv)*ImageScale - centerX);
                float y_start = (float) (ImageStartY+ (Math.floor(WholeMapStartPos.y/conv)*conv)*ImageScale - centerY);


                GuiUtil.linePre(grid.color,thick);

                thick*=1.4f;

                for (float xr = x_start; xr < ImageStartX+ImageScaleX+scale; xr+=scale) {
                    GL11.glBegin(2);
                    GL11.glVertex2d(xr, ImageStartY);
                    GL11.glVertex2d(xr, ImageStartY+ImageScaleY);
                    GL11.glEnd();
                }

                for (float yr = y_start; yr < ImageStartY+ImageScaleY+scale; yr+=scale) {
                    GL11.glBegin(2);
                    GL11.glVertex2d(ImageStartX,yr);
                    GL11.glVertex2d(ImageStartX+ImageScaleX,yr);
                    GL11.glEnd();
                }


                GuiUtil.linePost();

            }

        }


        ArrayList<Pair<Vec2i,MCStructures>> structuresHashMap = new ArrayList<Pair<Vec2i, MCStructures>>();


        if(showBiomes && !Spark.mapManager.canShowBiomes(dim))
            showBiomes = false;

        //render map

        for (int x = WholeMapStartPos.x; x <= WholeMapEndPos.x; x++) {
            for (int y = WholeMapStartPos.y; y <= WholeMapEndPos.y; y++) {


                SparkMap map = MapManager.instance.getMap(new Vec2i(x, y), dim);

                float x_ = map.getStartPos().x * (ImageScale / SparkMap.getWidthAndHeight()) - centerX;
                float y_ = map.getStartPos().y * (ImageScale / SparkMap.getWidthAndHeight()) - centerY;

                if(!map.structures.isEmpty())
                    structuresHashMap.addAll(new ArrayList<>(map.structures));



                if(showBiomes){
                    if(!map.isBiomeMapEmpty())
                    {


                        ResourceLocation location = map.getBiomeResourceLocation();

                        if(location != null)
                        {

                            mc.getTextureManager().bindTexture(location);

                            GuiUtil.drawCompleteImage(ImageStartX + x_, ImageStartY + y_, ImageScale, ImageScale);
                        }
                    }
                    else
                        Spark.mapManager.addToGenerateBiomeMap(map);
                } else if(!map.isEmpty())
                {


                    ResourceLocation location = map.getResourceLocation();

                    if(location != null)
                    {

                        mc.getTextureManager().bindTexture(location);

                        GuiUtil.drawCompleteImage(ImageStartX + x_, ImageStartY + y_, ImageScale, ImageScale);
                    }
                }



            }
        }



        if(MapConfig.getInstance().Structures.isOn())
        for (Pair<Vec2i, MCStructures> structuresPair : structuresHashMap) {

            if(!MapConfig.getInstance().StructureList.contains(structuresPair.getValue()))
                continue;

            Vec2d pos = new Vec2d(structuresPair.getKey().x*16,structuresPair.getKey().y*16);
            double x = ImageStartX + ImageScaleX * 0.5 + offsetX + SparkMap.get2dMapPosFromWorldPos(pos.x - TargetX, ImageScale);
            double y = ImageStartY + ImageScaleY * 0.5 + offsetY + SparkMap.get2dMapPosFromWorldPos(pos.y - TargetZ, ImageScale);

            boolean hovered = false;
            if (hover) {
                Double distance = Math.sqrt((y - mouseY) * (y - mouseY) + (x - mouseX) * (x - mouseX));
                if (distance < 3) hovered = true;
            }

            MCStructures structures = structuresPair.getValue();


            double s = Math.min(structures.getSize()*(ImageScale / SparkMap.getWidthAndHeight()),0.6);

            double hideSize = 0.1;

            if(s < hideSize)
                continue;

            GL11.glPushMatrix();
            GlStateManager.translate(x, y, 0);

            float alpha = (float) Math.min(1,Math.abs(s-hideSize)/0.1);


            if(hovered)
                s*=1.3;

            GlStateManager.scale(s,s,s);
            GuiUtil.drawCompleteImageRotated(-6,-6,6*2,6*2,0,structures.getResourceLocation(),new Color(1f,1f,1f,alpha));

            GL11.glPopMatrix();

        }



        FontManager fontManager = Spark.fontManager;

        for (WaypointManager.Waypoint point : Spark.waypointManager.getWayPoints()) {

            if (point.getDim() == dim || (point.getDim() != 1 && dim != 1)) {

                Vec2d pos = point.getLocation2d(point.getDim(), dim);
                double x = (ImageStartX + ImageScaleX * 0.5 + offsetX + SparkMap.get2dMapPosFromWorldPos(pos.x - TargetX, ImageScale));
                double y = (ImageStartY + ImageScaleY * 0.5 + offsetY + SparkMap.get2dMapPosFromWorldPos(pos.y - TargetZ, ImageScale));
                boolean hovered = false;
                if (hover) {
                    Double distance = Math.sqrt((y - mouseY) * (y - mouseY) + (x - mouseX) * (x - mouseX));
                    if (distance < 3) hovered = true;
                }
                GL11.glPushMatrix();
                GlStateManager.translate(x, y, 0);

                RenderUtil.drawFilledCircle(0, 0, hovered ? 4 : 3, new Color(56, 53, 53, 245).getRGB());
                RenderUtil.drawFilledCircle(0, 0, hovered ? 3 : 2, point.getColor().getRGB());
                if (hovered) {

                    GuiPanelBase.drawQuad(0, 0, fontManager.getTextWidth(point.getName() + 2), fontManager.getTextHeight() + 3, new Color(56, 53, 53, 245).getRGB());
                    fontManager.drawString(point.getName(), 2, 2, new Color(239, 224, 224).getRGB());
                }

                GL11.glPopMatrix();


            }


        }



        if (mc.player.dimension == dim) {
            for (Entity e : mc.world.loadedEntityList) {
                if (e instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) e;

                    if (player != mc.player) {
                        double x = ImageStartX + ImageScaleX * 0.5 + offsetX + SparkMap.get2dMapPosFromWorldPos(player.posX - TargetX, ImageScale);
                        double y = ImageStartY + ImageScaleY * 0.5 + offsetY + SparkMap.get2dMapPosFromWorldPos(player.posZ - TargetZ, ImageScale);

                        GL11.glPushMatrix();
                        GlStateManager.translate(x,y,0);
                        GlStateManager.scale(0.3,0.3,0.3);
                        Gui.drawRect(-12,-12,12,12, ClientConfig.getInstance().getMainColor().getRGB());

                        NetworkPlayerInfo info = mc.player.connection.getPlayerInfo(player.getName());
                        if (info != null)
                            GuiUtil.renderPlayerHead(info, -10, -10, 20);
                        GL11.glPopMatrix();
                    }

                }
            }
        }





        //arrow
        if (dim == mc.player.dimension || (dim != 1 && mc.player.dimension != 1)) {

            Vec2d pos = ConvertPos(new Vec2d(mc.player.posX, mc.player.posZ), mc.player.dimension, dim);
            float OffsetXtoPlayer = (float) (ImageStartX + ImageScaleX * 0.5 + offsetX + SparkMap.get2dMapPosFromWorldPos(pos.x - TargetX, ImageScale));
            float OffsetYtoPlayer = (float) (ImageStartY + ImageScaleY * 0.5 + offsetY + SparkMap.get2dMapPosFromWorldPos(pos.y - TargetZ, ImageScale));

            GuiUtil.drawCompleteImageRotated(OffsetXtoPlayer - 2, OffsetYtoPlayer - 2, 4, 4, (int) mc.player.rotationYaw + 90, ARROW_ICON, dim == -1 ? Color.WHITE : Color.RED);
        }


        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        GL11.glPopMatrix();
    }




    public static Vec2d ConvertPos(Vec2d pos, int fromDim, int toDim) {
        if (toDim != fromDim) {
            if (fromDim == 1 || toDim == 1) {
                pos.x = 0;
                pos.y = 0;
            } else {
                if (fromDim == -1) {
                    pos.x *= 8;
                    pos.y *= 8;
                } else {
                    pos.x /= 8;
                    pos.y /= 8;
                }
            }
        }
        return pos;
    }


}
