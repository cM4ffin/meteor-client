/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import baritone.api.BaritoneAPI;
import baritone.api.pathing.goals.Goal;
import baritone.api.pathing.goals.GoalXZ;
import me.zero.alpine.listener.EventHandler;
import me.zero.alpine.listener.Listener;
import minegame159.meteorclient.events.entity.TookDamageEvent;
import minegame159.meteorclient.gui.widgets.WButton;
import minegame159.meteorclient.gui.widgets.WLabel;
import minegame159.meteorclient.gui.widgets.WTable;
import minegame159.meteorclient.gui.widgets.WWidget;
import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.ToggleModule;
import minegame159.meteorclient.settings.BoolSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.utils.Chat;
import minegame159.meteorclient.utils.Utils;
import minegame159.meteorclient.waypoints.Waypoint;
import minegame159.meteorclient.waypoints.Waypoints;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DeathPosition extends ToggleModule {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> createWaypoint = sgGeneral.add(new BoolSetting.Builder()
            .name("create-waypoint")
            .description("Creates a waypoint when you die.")
            .defaultValue(true)
            .build()
    );

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    private final WLabel label = new WLabel("No latest death.");

    public DeathPosition() {
        super(Category.Player, "death-position", "Sends you the exact position where you have died in chat.");
    }

    private final Map<String, Double> deathPos = new HashMap<>();

    @EventHandler
    private final Listener<TookDamageEvent> onTookDamage = new Listener<>(event -> {
        if (event.entity.getUuid() != null && event.entity.getUuid().equals(mc.player.getUuid()) && event.entity.getHealth() <= 0) {
            deathPos.put("x", mc.player.getX());
            deathPos.put("z", mc.player.getZ());
            label.setText(String.format("Latest death: %.1f, %.1f, %.1f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));

            String time = dateFormat.format(new Date());
            Chat.info(this, "Died at (highlight)%.0f(default), (highlight)%.0f(default), (highlight)%.0f (default)on (highlight)%s(default).", mc.player.getX(), mc.player.getY(), mc.player.getZ(), time);

            // Create waypoint
            if (createWaypoint.get()) {
                Waypoint waypoint = new Waypoint();
                waypoint.name = "Death " + time;

                waypoint.x = (int) mc.player.getX();
                waypoint.y = (int) mc.player.getY() + 2;
                waypoint.z = (int) mc.player.getZ();
                waypoint.maxVisibleDistance = Integer.MAX_VALUE;

                switch (Utils.getDimension()) {
                    case Overworld:
                        waypoint.overworld = true;
                        break;
                    case Nether:
                        waypoint.nether = true;
                        break;
                    case End:
                        waypoint.end = true;
                        break;
                }

                Waypoints.INSTANCE.add(waypoint);
            }
        }
    });

    @Override
    public WWidget getWidget() {
        WTable table = new WTable();
        table.add(label);
        WButton path = new WButton("Path");
        table.add(path);
        path.action = this::path;
        return table;
    }

    private void path() {
        if (deathPos.isEmpty() && mc.player != null) {
            Chat.info("No last known death position.");
        } else {
            double x = deathPos.get("x"), z = deathPos.get("z");
            if (BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing())
                BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().cancelEverything();
            BaritoneAPI.getProvider().getPrimaryBaritone().getCustomGoalProcess().setGoalAndPath(new GoalXZ((int) x, (int) z));
        }
    }
}
