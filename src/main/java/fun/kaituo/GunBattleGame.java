package fun.kaituo;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.PacketContainer;
import fun.kaituo.event.PlayerChangeGameEvent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static fun.kaituo.GameUtils.world;

public class GunBattleGame extends Game implements Listener {
    private static final GunBattleGame instance = new GunBattleGame((GunBattle) Bukkit.getPluginManager().getPlugin("GunBattle"));
    Scoreboard gunBattle;
    Scoreboard scoreboard;
    Team team;
    HashMap<Player, Long> timeMap;
    HashMap<Player, Boolean> isReloaded;
    HashMap<Player, Integer> remainingAmmo;
    HashMap<Player, Long> timeMap2;
    HashMap<Player, Boolean> isReloaded2;
    HashMap<Player, Integer> remainingAmmo2;
    HashMap<Player, Vector> vectorMap;
    HashMap<Player, Vector> tempVector;
    ItemStack arrow;
    ItemStack firework_star;
    ProtocolManager pm;

    private GunBattleGame(GunBattle plugin) {
        this.plugin = plugin;
        players = plugin.players;
        gunBattle = Bukkit.getScoreboardManager().getNewScoreboard();
        scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        team = gunBattle.registerNewTeam("gunBattle");
        team.setAllowFriendlyFire(true);
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        gunBattle.registerNewObjective("gunBattle", "dummy", "枪械乱斗击败榜");
        gunBattle.getObjective("gunBattle").setDisplaySlot(DisplaySlot.SIDEBAR);
        timeMap = new HashMap<>();
        isReloaded = new HashMap<>();
        remainingAmmo = new HashMap<>();
        timeMap2 = new HashMap<>();
        isReloaded2 = new HashMap<>();
        remainingAmmo2 = new HashMap<>();
        vectorMap = new HashMap<>();
        tempVector = new HashMap<>();
        arrow = new ItemStack(Material.ARROW, 1);
        for (Player p : Bukkit.getOnlinePlayers()) {
            timeMap.put(p, 0l);
            isReloaded.put(p, true);
            remainingAmmo.put(p, 30);
            timeMap2.put(p, 0l);
            isReloaded2.put(p, true);
            remainingAmmo2.put(p, 10);
        }
        Bukkit.getScheduler().scheduleAsyncRepeatingTask(plugin, () -> {
            for (Player p : players) {
                Vector currentLoc = p.getLocation().toVector().clone();
                tempVector.putIfAbsent(p, currentLoc);
                Vector previousLoc = tempVector.get(p);
                Vector velocity = currentLoc.clone().subtract(previousLoc);
                vectorMap.put(p, velocity);
                tempVector.put(p, currentLoc);
            }
        }, 1, 1);
        initializeGame(plugin, "GunBattle","§a枪械乱斗", new Location(world, -3,58,1994),
                new BoundingBox(-300, 64, 1700, 300, 88, 2300));
        Bukkit.getScheduler().runTask(plugin, () -> {
            pm = ProtocolLibrary.getProtocolManager();
        });
    }

    public static GunBattleGame getInstance() {
        return instance;
    }

    public static Predicate<Entity> isDesiredTarget(Player p) {
        return e -> !e.equals(p) && e instanceof Player;
    }

    public void reloadAmmo2(Player p) {
        if (!isReloaded2.get(p)) {
            return;
        }
        if (remainingAmmo2.get(p) == 10) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a§l弹匣已满！"));
            return;
        }
        if (!p.getInventory().contains(Material.FIREWORK_STAR)) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c§l弹药用尽！"));
            return;
        }
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§6§l正在装弹"));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 1f, 0.5f);
        }, 16);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 1f, 0.5f);
        }, 52);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 1f, 0.7f);
        }, 74);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_STEP, SoundCategory.PLAYERS, 1f, 0.7f);
        }, 72);
        isReloaded2.put(p, false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int ammo = 0;
            int remain = remainingAmmo2.get(p);
            for (Map.Entry e : p.getInventory().all(Material.FIREWORK_STAR).entrySet()) {
                ammo += ((ItemStack) e.getValue()).getAmount();
            }
            if (ammo + remain < 10) {
                removeItem(p, Material.FIREWORK_STAR, ammo);
                remainingAmmo2.put(p, ammo + remain);
                p.getInventory().getItem(1).setAmount(ammo + remain);

            } else {
                removeItem(p, Material.FIREWORK_STAR, 10 - remain);
                remainingAmmo2.put(p, 10);
                p.getInventory().getItem(1).setAmount(10);
            }
            isReloaded2.put(p, true);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§e§l装弹完成"));
        }, 80);
    }

    public void reloadAmmo(Player p) {
        if (!isReloaded.get(p)) {
            return;
        }
        if (remainingAmmo.get(p) == 30) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§a§l弹匣已满！"));
            return;
        }
        if (!p.getInventory().contains(Material.ARROW)) {
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§c§l弹药用尽！"));
            return;
        }
        p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§6§l正在装弹"));
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 1f, 0.5f);
        }, 6);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 1f, 0.5f);
        }, 32);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_STEP, SoundCategory.PLAYERS, 1f, 0.7f);
        }, 42);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 1f, 0.7f);
        }, 44);
        isReloaded.put(p, false);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            int ammo = 0;
            int remain = remainingAmmo.get(p);
            for (Map.Entry e : p.getInventory().all(Material.ARROW).entrySet()) {
                ammo += ((ItemStack) e.getValue()).getAmount();
            }
            if (ammo + remain < 30) {
                removeItem(p, Material.ARROW, ammo);
                remainingAmmo.put(p, ammo + remain);
                p.getInventory().getItem(0).setAmount(ammo + remain);
            } else {
                removeItem(p, Material.ARROW, 30 - remain);
                remainingAmmo.put(p, 30);
                p.getInventory().getItem(0).setAmount(30);
            }
            isReloaded.put(p, true);
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent("§e§l装弹完成"));
        }, 60);
    }

    public boolean checkSameTeam(Player a, Player b) {

        if (a.equals(b)) {
            return true;
        } else if (scoreboard.getPlayerTeam(a) == null || scoreboard.getPlayerTeam(b) == null) {
            return false;
        } else return scoreboard.getPlayerTeam(a).equals(scoreboard.getPlayerTeam(b));
    }

    public void shootGun2(Player p) {
        if (getTime(world) - timeMap2.get(p) >= 50) {
            if (!isReloaded2.get(p)) {
                return;
            }
            if (remainingAmmo2.get(p) == 0) {
                reloadAmmo2(p);
                return;
            } else if (remainingAmmo2.get(p) == 1) {
                reloadAmmo2(p);
            }
            remainingAmmo2.put(p, remainingAmmo2.get(p) - 1);
            if (remainingAmmo2.get(p) == 0) {
                p.getInventory().getItem(1).setAmount(1);
            } else {
                p.getInventory().getItem(1).setAmount(remainingAmmo2.get(p));
            }
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_STEP, SoundCategory.PLAYERS, 1f, 0.7f);
            }, 30);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                world.playSound(p.getLocation(), Sound.BLOCK_CHAIN_HIT, SoundCategory.PLAYERS, 1f, 0.7f);
            }, 36);
            int remain = remainingAmmo2.get(p);
            String color = "";
            if (remain <= 2) {
                color = "§c";
            } else if (remain <= 4) {
                color = "§6";
            } else if (remain <= 6) {
                color = "§e";
            } else if (remain <= 8) {
                color = "§a";
            } else {
                color = "§2";
            }
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(color + "§l" + remainingAmmo2.get(p) + "§f§l / §6§l10"));
            world.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 2);
            timeMap2.put(p, getTime(world));
            Projectile projectile = p.launchProjectile(Arrow.class, p.getEyeLocation().getDirection());
            projectile.setSilent(true);
            PacketContainer removeArrow = pm.createPacket(PacketType.Play.Server.ENTITY_DESTROY);
            List<Integer> idList = new ArrayList<>();
            idList.add(projectile.getEntityId());
            removeArrow.getIntLists().write(0, idList);
            //removeArrow.getIntegerArrays().write(0,new int[] {projectile.getEntityId()});
            try {
                pm.sendServerPacket(p, removeArrow);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return;
        }
    }

    public void shootGun(Player p) {
        if (getTime(world) - timeMap.get(p) >= 1) {
            if (!isReloaded.get(p)) {
                return;
            }
            if (remainingAmmo.get(p) == 0) {
                reloadAmmo(p);
                return;
            } else if (remainingAmmo.get(p) == 1) {
                reloadAmmo(p);
            }
            remainingAmmo.put(p, remainingAmmo.get(p) - 1);
            if (remainingAmmo.get(p) == 0) {
                p.getInventory().getItem(0).setAmount(1);
            } else {
                p.getInventory().getItem(0).setAmount(remainingAmmo.get(p));
            }
            int remain = remainingAmmo.get(p);
            String color = "";
            if (remain <= 5) {
                color = "§c";
            } else if (remain <= 10) {
                color = "§6";
            } else if (remain <= 15) {
                color = "§e";
            } else if (remain <= 20) {
                color = "§a";
            } else {
                color = "§2";
            }
            p.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(color + "§l" + remain + " §f§l/ §6§l30"));
            world.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.4f, 2);
            timeMap.put(p, getTime(world));

            /*
            PacketContainer changeRotation = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_LOOK);
            changeRotation.getIntegers().writeSafely(0, p.getEntityId());
            changeRotation.getBytes().writeSafely(0, (byte) (p.getEyeLocation().getYaw()*256F / 360F));
            changeRotation.getBytes().writeSafely(1, (byte) (p.getEyeLocation().getPitch()*256F / 360F));
            try {
                pm.sendServerPacket(p,changeRotation);
            } catch (Exception e) {
                e.printStackTrace();
            }
             */

            RayTraceResult result = world.rayTrace(p.getEyeLocation(), p.getEyeLocation().getDirection(), 256,
                    FluidCollisionMode.NEVER,
                    true,
                    0,
                    isDesiredTarget(p));
            if (result == null) {
                return;
            }
            Entity victim = result.getHitEntity();
            if (victim == null) {
                return;
            }
            if (victim instanceof Player) {
                if (!checkSameTeam(p, (Player) victim)) {
                    ((Player) victim).damage(5.5, p);
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.2f, 0);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerChangeGame(PlayerChangeGameEvent pcge) {
        players.remove(pcge.getPlayer());
    }

    @EventHandler
    public void preventSwapHand(PlayerSwapHandItemsEvent pshie) {
        if (players.contains(pshie.getPlayer())) {
            pshie.setCancelled(true);
        }
    }

    @EventHandler
    public void resetAmmoAmount(PlayerRespawnEvent pre) {
        Player p = pre.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!(players.contains(pre.getPlayer()))) {
                return;
            }
            ItemStack item1 = p.getInventory().getItem(0);
            ItemStack item2 = p.getInventory().getItem(1);
            if (item1 != null) {
                if (item1.getType().equals(Material.SALMON)) {
                    item1.setAmount(30);
                }
            }
            if (item2 != null) {
                if (item2.getType().equals(Material.COD)) {
                    item2.setAmount(10);
                }
            }
        }, 1);
    }

    @EventHandler
    public void resetAmmoAmount(PlayerTeleportEvent pte) {
        Player p = pte.getPlayer();
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!(players.contains(pte.getPlayer()))) {
                return;
            }
            ItemStack item1 = p.getInventory().getItem(0);
            ItemStack item2 = p.getInventory().getItem(1);
            if (item1 != null) {
                if (item1.getType().equals(Material.SALMON)) {
                    item1.setAmount(30);
                }
            }
            if (item2 != null) {
                if (item2.getType().equals(Material.COD)) {
                    item2.setAmount(10);
                }
            }
        }, 1);
    }

    @EventHandler
    public void initializeAmmo(PlayerJoinEvent pje) {
        timeMap.put(pje.getPlayer(), 0l);
        isReloaded.put(pje.getPlayer(), true);
        remainingAmmo.put(pje.getPlayer(), 30);
        timeMap2.put(pje.getPlayer(), 0l);
        isReloaded2.put(pje.getPlayer(), true);
        remainingAmmo2.put(pje.getPlayer(), 10);
    }

    @EventHandler
    public void shoot(PlayerInteractEvent pie) {
        Player p = pie.getPlayer();
        if (!pie.getAction().equals(Action.RIGHT_CLICK_BLOCK) && !pie.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }
        if (!(players.contains(pie.getPlayer()))) {
            return;
        }
        if (p.getInventory().getItemInMainHand().getType().equals(Material.SALMON)) {
            shootGun(p);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                shootGun(p);
            }, 0);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                shootGun(p);
            }, 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                shootGun(p);
            }, 2);
        } else if (p.getInventory().getItemInMainHand().getType().equals(Material.COD)) {
            shootGun2(p);
        }
    }

    @EventHandler
    public void resistKnockbackAndRemoveDamageCoolDown(EntityDamageEvent ede) {
        if (!(players.contains(ede.getEntity()))) {
            return;
        }
        if (!ede.getEntity().getType().equals(EntityType.PLAYER)) {
            return;
        }
        if ((((Player) ede.getEntity()).hasPotionEffect(PotionEffectType.DAMAGE_RESISTANCE))) {
            ede.setCancelled(true);
        }
    }

    @EventHandler
    public void preventFriendlyFire(EntityDamageByEntityEvent edbee) {
        if (!(edbee.getEntity() instanceof Player)) {
            return;
        }
        if (!(edbee.getDamager() instanceof Player)) {
            return;
        }
        if (checkSameTeam((Player) edbee.getDamager(), (Player) edbee.getEntity())) {
            edbee.setCancelled(true);
        }
    }

    @EventHandler
    public void reduceAttackRange(EntityDamageByEntityEvent edbee) {
        if (!edbee.getDamager().getType().equals(EntityType.PLAYER)) {
            return;
        }
        if (!(players.contains(edbee.getDamager()))) {
            return;
        }
        if (((Player) edbee.getDamager()).getInventory().getItemInMainHand().getType().equals(Material.IRON_SWORD)) {
            if (edbee.getDamager().getLocation().distance(edbee.getEntity().getLocation()) > 2) {
                edbee.setCancelled(true);
            }
        }

    }

    @EventHandler
    public void reloadAmmo(PlayerDropItemEvent pdie) {
        if (!(players.contains(pdie.getPlayer()))) {
            return;
        }
        pdie.setCancelled(true);
        if (pdie.getItemDrop().getItemStack().getType().equals(Material.SALMON)) {
            reloadAmmo(pdie.getPlayer());
        } else if (pdie.getItemDrop().getItemStack().getType().equals(Material.COD)) {
            reloadAmmo2(pdie.getPlayer());
        }
    }

    @EventHandler
    public void onProjectileLaunch(ProjectileLaunchEvent ple) {
        if (ple.getEntity().getShooter() instanceof Player) {
            Player p = (Player) ple.getEntity().getShooter();
            if (!(players.contains(p))) {
                return;
            }
            if ((ple.getEntity().getType().equals(EntityType.ARROW))) {
                Vector v = ple.getEntity().getVelocity().clone();
                Vector velocity = vectorMap.get(p).clone();
                v.add(velocity.multiply(5));//这里是偏移量
                v.multiply(64);
                ple.getEntity().setVelocity(v);
                world.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.8f, 2);
            } else if ((ple.getEntity().getType().equals(EntityType.SNOWBALL))) {
                ple.getEntity().setVelocity(ple.getEntity().getVelocity().multiply(0.5));
                ple.getEntity().setCustomName(((Player) ple.getEntity().getShooter()).getName());
            }
        }

    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent phe) {
        if (!((phe.getEntity().getShooter()) instanceof Entity)) {
            return;
        }//不是实体
        Entity shooter = (Entity) phe.getEntity().getShooter();
        if (!(players.contains(shooter))) {
            return;
        }//不在里
        if (phe.getHitBlock() != null) {
            if (!phe.getHitBlock().getType().equals(Material.WATER)) {
                phe.getEntity().remove();
            }
        }//消除箭
        Location l;
        if (phe.getHitBlock() != null) {
            l = phe.getHitBlock().getLocation();
            switch (phe.getHitBlockFace()) {
                case UP:
                    l.setX(l.getX() + 0.5);
                    l.setY(l.getY() + 1.5);
                    l.setZ(l.getZ() + 0.5);
                    break;
                case DOWN:
                    l.setX(l.getX() + 0.5);
                    l.setY(l.getY() - 0.5);
                    l.setZ(l.getZ() + 0.5);
                    break;
                case NORTH:
                    l.setX(l.getX() + 0.5);
                    l.setY(l.getY() + 0.5);
                    l.setZ(l.getZ() - 0.5);
                    break;
                case SOUTH:
                    l.setX(l.getX() + 0.5);
                    l.setY(l.getY() + 0.5);
                    l.setZ(l.getZ() + 1.5);
                    break;
                case EAST:
                    l.setX(l.getX() + 1.5);
                    l.setY(l.getY() + 0.5);
                    l.setZ(l.getZ() + 0.5);
                    break;
                case WEST:
                    l.setX(l.getX() - 0.5);
                    l.setY(l.getY() + 0.5);
                    l.setZ(l.getZ() + 0.5);
                    break;
                default:
                    l.setX(l.getX() + 0.5);
                    l.setY(l.getY() + 0.5);
                    l.setZ(l.getZ() + 0.5);
                    break;
            }
        } else {
            l = phe.getHitEntity().getLocation();
            l.setY(l.getY() + 1.8);
        }
        if (l.getY() < 64) {
            return;
        } //大厅里面
        if (phe.getEntity().getType().equals(EntityType.SNOWBALL)) {
            world.createExplosion(l, 3f, false, false, (Entity) phe.getEntity().getShooter());
            world.spawnParticle(Particle.EXPLOSION_LARGE, l, 1);
        }
    }

    @EventHandler
    public void cancelItemMovement(InventoryClickEvent ice) {
        if (!(ice.getWhoClicked() instanceof Player)) {
            return;
        }
        if (ice.getWhoClicked().getGameMode().equals(GameMode.CREATIVE)) {
            return;
        }
        if (players.contains(ice.getWhoClicked())) {
            ice.setCancelled(true);
        }
    }

    @EventHandler
    public void giveItems(PlayerDeathEvent pde) {
        Player p = pde.getEntity();
        if (scoreboard.getTeam("gunBattleR").hasPlayer(p)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + p.getName() + " run function gunbattle:red");
        } else if (scoreboard.getTeam("gunBattleB").hasPlayer(p)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute as " + p.getName() + " run function gunbattle:blue");
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent pde) {
        remainingAmmo.put(pde.getEntity(), 30);
        isReloaded.put(pde.getEntity(), true);
        remainingAmmo2.put(pde.getEntity(), 10);
        isReloaded2.put(pde.getEntity(), true);
        Player player = pde.getEntity();
        Entity killer = pde.getEntity().getKiller();
        if (!(players.contains(player))) {
            return;
        }
        if (killer == null) {
            return;
        }
        if (killer.getType().equals(EntityType.PLAYER)) {
            if (player.equals(killer)) {
                return;
            }
            if (players.contains(killer)) {
                if (scoreboard.getTeam("gunBattleR").hasPlayer((Player) killer)) {
                    Score score = gunBattle.getObjective("gunBattle").getScore("§c" + killer.getName());
                    score.setScore(score.getScore() + 1);//加分
                } else if (scoreboard.getTeam("gunBattleB").hasPlayer((Player) killer)) {
                    Score score = gunBattle.getObjective("gunBattle").getScore("§9" + killer.getName());
                    score.setScore(score.getScore() + 1);//加分
                }
            }
        }
    }

    @EventHandler
    public void clearKills(PlayerInteractEvent pie) {
        if (pie.getClickedBlock() == null) {
            return;
        }
        Location location = pie.getClickedBlock().getLocation();
        long x = location.getBlockX();
        long y = location.getBlockY();
        long z = location.getBlockZ();
        if (x == 1 && y == 60 && z == 1990) {
            gunBattle.getObjective("gunBattle").unregister();
            gunBattle.registerNewObjective("gunBattle", "dummy", "枪械乱斗击败榜");
            gunBattle.getObjective("gunBattle").setDisplaySlot(DisplaySlot.SIDEBAR);
        } else if (x == -3 && y == 58 && z == 1989) {
            players.add(pie.getPlayer());
            pie.getPlayer().setScoreboard(gunBattle);
            team.addPlayer(pie.getPlayer());
            pie.getPlayer().setMaximumNoDamageTicks(0);
        } else if (x == 2 && y == 58 && z == 1994) {
            players.add(pie.getPlayer());
            pie.getPlayer().setScoreboard(gunBattle);
            team.addPlayer(pie.getPlayer());
            pie.getPlayer().setMaximumNoDamageTicks(0);
        }
    }

    public void removeItem(Player p, Material material, int number) {
        p.getInventory().all(material).get(0);
        int totalNumber = 0;
        for (Map.Entry entry : p.getInventory().all(material).entrySet()) {
            ItemStack i = (ItemStack) entry.getValue();
            totalNumber += i.getAmount();
            if (totalNumber < number) {
                i.setAmount(0);
            } else {
                i.setAmount(i.getAmount() - (number - (totalNumber - i.getAmount())));
                break;
            }
        }
    }


    @Override
    protected void initializeGameRunnable() {
        gameRunnable = () -> {
            Bukkit.getPluginManager().registerEvents(this, plugin);
        };
    }

    @Override
    protected void savePlayerQuitData(Player p) throws IOException {
        players.remove(p);
    }

    @Override
    protected void rejoin(Player player) {

    }
}
