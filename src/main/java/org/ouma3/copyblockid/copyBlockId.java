package org.ouma3.copyblockid;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.common.NeoForge;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

@Mod("copy_block_id")
public class copyBlockId {
    // シフトキーが押されている間に表示するためのリスト
    private final Set<String> tempBlockIds = new HashSet<>();
    // シフトキーが押されているかどうかを追跡
    private boolean isShiftKeyPressed = false;

    public copyBlockId() {
        // イベントバスに登録
        NeoForge.EVENT_BUS.register(this);
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // クライアント側のプレイヤーであることを確認
        if (event.getLevel().isClientSide()) {
            // プレイヤーが紙を持っているかチェック
            ItemStack heldItem = event.getEntity().getItemInHand(event.getHand());

            if (heldItem.getItem() == Items.PAPER) {
                // ブロックの情報を取得
                Block block = event.getLevel().getBlockState(event.getPos()).getBlock();
                // ブロックのレジストリ名（ID）を取得
                ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(block);
                String blockIdStr = blockId.toString();

                // イベントをキャンセル（オプション - 紙を消費せずにブロックを操作しないようにする場合）
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);

                // シフトキーが押されている場合の処理
                if (event.getEntity().isShiftKeyDown()) {
                    if (!isShiftKeyPressed) {
                        // シフトキーが押されたらテンポラリリストをクリア
                        isShiftKeyPressed = true;
                        tempBlockIds.clear();
                    }

                    // 一時リストにブロックIDを追加（重複は自動的に排除される）
                    tempBlockIds.add(blockIdStr);

                    // カンマ区切りで表示するための準備
                    StringJoiner joiner = new StringJoiner(",");
                    for (String id : tempBlockIds) {
                        joiner.add(id);
                    }
                    Component message = Component.translatable("copy_block_id.message.recording");
                    Component recordingContext = Component.empty().append(message).append(joiner.toString());
                    // 現在の一時リストを表示
                    event.getEntity().displayClientMessage(recordingContext,true);
                } else {
                    if (isShiftKeyPressed) {
                        // シフトキーが離されたときの処理
                        isShiftKeyPressed = false;
                        // ここで必要ならtempBlockIdsを処理
                    }
                }
            }
        }
    }

    // キー入力状態の変化を監視するメソッドを追加
    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public void onKeyInput(net.neoforged.neoforge.client.event.InputEvent.Key event) {
        // シフトキーが離されたかを確認（キーコード16はシフトキー）
        if (event.getKey() == 340 || event.getKey() == 344) { // 左シフトと右シフトのキーコード
            if (event.getAction() == 0 && isShiftKeyPressed) { // 0はキーが離された時
                isShiftKeyPressed = false;

                // シフトキーが離されたときに集めたブロックIDをチャットに表示
                if (!tempBlockIds.isEmpty()) {
                    StringJoiner joiner = new StringJoiner(",");
                    for (String id : tempBlockIds) {
                        joiner.add(id);
                    }

                    final String content = joiner.toString();

                    // GLFWを使ってクリップボードにコピーする
                    copyToClipboardWithGLFW(content);

                    Component message = Component.translatable("copy_block_id.message.copied");
                    Component copiedContext = Component.empty().append(message).append(content);

                    Minecraft.getInstance().player.displayClientMessage(copiedContext, true);

                    tempBlockIds.clear();
                }
            }
        }
    }

    // GLFWを使ってクリップボードにコピーするメソッド
    private void copyToClipboardWithGLFW(String content) {
        try {
            // この操作はゲームスレッド（メインスレッド）で行う必要があります
            // GLFWの関数はメインスレッドからのみ呼び出せます
            com.mojang.blaze3d.platform.Window window = Minecraft.getInstance().getWindow();
            long handle = window.getWindow();

            if (handle != 0) {
                // GLFWのクリップボードにテキストを設定
                org.lwjgl.glfw.GLFW.glfwSetClipboardString(handle, content);
                System.out.println("GLFWを使用してクリップボードにテキストをコピーしました");
            } else {
                System.err.println("GLFWウィンドウハンドルが無効です");
            }
        } catch (Exception e) {
            System.err.println("GLFWクリップボード操作に失敗しました: " + e.getMessage());
            e.printStackTrace();
        }
    }
}