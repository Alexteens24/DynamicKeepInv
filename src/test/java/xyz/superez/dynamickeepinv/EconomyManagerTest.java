package xyz.superez.dynamickeepinv;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EconomyManagerTest {

    @Mock private DynamicKeepInvPlugin plugin;
    @Mock private org.bukkit.Server server;
    @Mock private org.bukkit.plugin.PluginManager pluginManager;
    @Mock private ServicesManager servicesManager;
    @Mock private RegisteredServiceProvider<Economy> rsp;
    @Mock private Economy economy;
    @Mock private Player player;
    @Mock private Logger logger;

    private EconomyManager manager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(plugin.getServer()).thenReturn(server);
        when(plugin.getLogger()).thenReturn(logger);
        when(server.getPluginManager()).thenReturn(pluginManager);
        when(server.getServicesManager()).thenReturn(servicesManager);
        manager = new EconomyManager(plugin);
    }

    @Test
    @DisplayName("No Vault plugin found → setup returns false, isEnabled false")
    void testNoVault() {
        when(pluginManager.getPlugin("Vault")).thenReturn(null);

        boolean result = manager.setupEconomy();

        assertFalse(result);
        assertFalse(manager.isEnabled());
    }

    @Test
    @DisplayName("Vault found but no Economy provider → setup returns false")
    void testVaultNoProvider() {
        org.bukkit.plugin.Plugin vaultPlugin = mock(org.bukkit.plugin.Plugin.class);
        when(pluginManager.getPlugin("Vault")).thenReturn(vaultPlugin);
        when(servicesManager.getRegistration(Economy.class)).thenReturn(null);

        boolean result = manager.setupEconomy();

        assertFalse(result);
        assertFalse(manager.isEnabled());
    }

    @Test
    @DisplayName("Vault + Economy provider present → setup returns true, isEnabled true")
    void testSetupSuccess() {
        org.bukkit.plugin.Plugin vaultPlugin = mock(org.bukkit.plugin.Plugin.class);
        when(pluginManager.getPlugin("Vault")).thenReturn(vaultPlugin);
        when(servicesManager.getRegistration(Economy.class)).thenReturn(rsp);
        when(rsp.getProvider()).thenReturn(economy);
        when(economy.getName()).thenReturn("TestEcon");

        boolean result = manager.setupEconomy();

        assertTrue(result);
        assertTrue(manager.isEnabled());
    }

    @Test
    @DisplayName("When disabled: hasEnough always returns true (permissive)")
    void testDisabledHasEnough() {
        // Not set up → disabled
        assertTrue(manager.hasEnough(player, 100.0));
    }

    @Test
    @DisplayName("When disabled: withdraw always returns true (permissive)")
    void testDisabledWithdraw() {
        assertTrue(manager.withdraw(player, 50.0));
    }

    @Test
    @DisplayName("When disabled: deposit always returns true (permissive)")
    void testDisabledDeposit() {
        assertTrue(manager.deposit(player, 50.0));
    }

    @Test
    @DisplayName("hasEnough delegates to economy provider when enabled")
    void testHasEnoughDelegates() throws Exception {
        setupEconomyEnabled();
        when(economy.has(player, 100.0)).thenReturn(true);

        assertTrue(manager.hasEnough(player, 100.0));

        when(economy.has(player, 999.0)).thenReturn(false);
        assertFalse(manager.hasEnough(player, 999.0));
    }

    @Test
    @DisplayName("withdraw returns true only on transactionSuccess")
    void testWithdrawSuccess() throws Exception {
        setupEconomyEnabled();
        EconomyResponse successResponse = new EconomyResponse(50.0, 950.0, EconomyResponse.ResponseType.SUCCESS, "");
        when(economy.withdrawPlayer(player, 50.0)).thenReturn(successResponse);

        assertTrue(manager.withdraw(player, 50.0));
    }

    @Test
    @DisplayName("withdraw returns false on failure response")
    void testWithdrawFailure() throws Exception {
        setupEconomyEnabled();
        EconomyResponse failResponse = new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "not enough");
        when(economy.withdrawPlayer(player, 50.0)).thenReturn(failResponse);

        assertFalse(manager.withdraw(player, 50.0));
    }

    @Test
    @DisplayName("getBalance delegates to economy provider")
    void testGetBalanceDelegates() throws Exception {
        setupEconomyEnabled();
        when(economy.getBalance(player)).thenReturn(1234.56);

        assertEquals(1234.56, manager.getBalance(player));
    }

    private void setupEconomyEnabled() {
        org.bukkit.plugin.Plugin vaultPlugin = mock(org.bukkit.plugin.Plugin.class);
        when(pluginManager.getPlugin("Vault")).thenReturn(vaultPlugin);
        when(servicesManager.getRegistration(Economy.class)).thenReturn(rsp);
        when(rsp.getProvider()).thenReturn(economy);
        when(economy.getName()).thenReturn("TestEcon");
        manager.setupEconomy();
    }
}
