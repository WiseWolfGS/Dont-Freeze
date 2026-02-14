package net.WWGS.dontfreeze.core.command;

import com.minecolonies.api.colony.ICivilianData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.logging.LogUtils;
import net.WWGS.dontfreeze.core.colony.MineColoniesCitizenQuery;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

public final class DFCommands {
    private DFCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("df_migrate")
                        .requires(src -> src.hasPermission(2)) // OP 레벨 2 이상만
                        .then(Commands.argument("targetColonyId", IntegerArgumentType.integer(1))
                                .then(Commands.argument("citizens", EntityArgument.entities())
                                        .executes(ctx -> {
                                            int targetId = IntegerArgumentType.getInteger(ctx, "targetColonyId");
                                            ServerLevel level = ctx.getSource().getLevel();

                                            IColony targetColony = IColonyManager.getInstance().getColonyByWorld(targetId, level);
                                            if (targetColony == null) {
                                                ctx.getSource().sendFailure(Component.literal("대상 콜로니를 찾을 수 없습니다: " + targetId));
                                                return 0;
                                            }

                                            var entities = EntityArgument.getEntities(ctx, "citizens");

                                            int success = 0;
                                            int fail = 0;

                                            MineColoniesCitizenQuery query = new MineColoniesCitizenQuery();

                                            for (Entity e : entities) {
                                                // MineColonies 시민 엔티티만 처리
                                                if (!(e instanceof com.minecolonies.api.entity.citizen.AbstractCivilianEntity civilianEntity)) {
                                                    fail++;
                                                    continue;
                                                }

                                                ICivilianData data = civilianEntity.getCivilianData();
                                                if (data == null) {
                                                    fail++;
                                                    continue;
                                                }

                                                try {
                                                    query.migrateCitizen(targetColony, data);
                                                    success++;
                                                } catch (Exception ex) {
                                                    fail++;
                                                }
                                            }

                                            int finalSuccess = success;
                                            int finalFail = fail;
                                            ctx.getSource().sendSuccess(() -> Component.literal(
                                                    "이주 완료: 성공 " + finalSuccess + "명 / 실패 " + finalFail + "명"
                                            ), true);

                                            return success;
                                        })
                                )
                        )
        );
    }
}