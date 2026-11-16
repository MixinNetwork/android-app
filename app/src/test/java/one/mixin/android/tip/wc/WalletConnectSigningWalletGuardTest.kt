package one.mixin.android.tip.wc

import com.reown.walletkit.client.Wallet
import kotlinx.coroutines.runBlocking
import one.mixin.android.Constants
import one.mixin.android.db.web3.vo.WalletItem
import one.mixin.android.db.web3.vo.Web3Address
import one.mixin.android.db.web3.vo.Web3Wallet
import one.mixin.android.db.web3.vo.isSigningEligible
import one.mixin.android.db.web3.vo.isWalletConnectSigningEligible
import one.mixin.android.tip.wc.internal.Chain
import one.mixin.android.tip.wc.internal.WalletConnectAddresses
import one.mixin.android.ui.tip.wc.sessionproposal.PeerUI
import one.mixin.android.ui.wallet.components.WalletDestination
import one.mixin.android.ui.wallet.components.WalletSignerTarget
import one.mixin.android.ui.wallet.components.walletSelectionTarget
import one.mixin.android.vo.WalletCategory
import one.mixin.android.web3.js.Web3Signer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WalletConnectSigningWalletGuardTest {
    private val wcPairUrl = "wc:topic-test@2?relay-protocol=irn&symKey=symkey-test"

    @Before
    fun setUp() {
        WalletConnect.resetTestHooks()
        Web3Signer.resetTestHooks()
        WalletConnectTIP.clear()
        Web3Signer.walletConnectSyncHandler = { _, _, _ -> }
        WalletConnect.localPrivateKeyPresenceProvider = { false }
        runBlocking {
            Web3Signer.applyWalletForTest("", WalletCategory.CLASSIC.value, false, emptyList())
        }
    }

    @After
    fun tearDown() {
        WalletConnectTIP.clear()
        runBlocking {
            Web3Signer.applyWalletForTest("", WalletCategory.CLASSIC.value, false, emptyList())
        }
        WalletConnect.resetTestHooks()
        Web3Signer.resetTestHooks()
    }

    private fun sampleAddresses(
        walletId: String,
        evm: String = "evm-addr-1",
        solana: String = "sol-addr-1",
        bitcoin: String = "btc-addr-1",
    ): List<Web3Address> =
        buildList {
            if (evm.isNotBlank()) {
                add(
                    Web3Address(
                        addressId = "$walletId-evm",
                        walletId = walletId,
                        chainId = Constants.ChainId.ETHEREUM_CHAIN_ID,
                        destination = evm,
                        path = null,
                        createdAt = "2026-01-01T00:00:00Z",
                    ),
                )
            }
            if (solana.isNotBlank()) {
                add(
                    Web3Address(
                        addressId = "$walletId-sol",
                        walletId = walletId,
                        chainId = Constants.ChainId.SOLANA_CHAIN_ID,
                        destination = solana,
                        path = null,
                        createdAt = "2026-01-01T00:00:00Z",
                    ),
                )
            }
            if (bitcoin.isNotBlank()) {
                add(
                    Web3Address(
                        addressId = "$walletId-btc",
                        walletId = walletId,
                        chainId = Constants.ChainId.BITCOIN_CHAIN_ID,
                        destination = bitcoin,
                        path = null,
                        createdAt = "2026-01-01T00:00:00Z",
                    ),
                )
            }
        }

    private fun sampleWallet(
        id: String,
        category: String,
        hasLocalPrivateKey: Boolean,
    ): Web3Wallet =
        Web3Wallet(
            id = id,
            category = category,
            name = "wallet",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
        ).apply {
            this.hasLocalPrivateKey = hasLocalPrivateKey
        }

    private fun sampleWalletItem(
        id: String,
        category: String,
        hasLocalPrivateKey: Boolean,
    ): WalletItem =
        WalletItem(
            id = id,
            category = category,
            name = "wallet",
            createdAt = "2026-01-01T00:00:00Z",
            updatedAt = "2026-01-01T00:00:00Z",
            safeRole = null,
            safeChainId = null,
            safeAddress = null,
            safeUrl = null,
        ).apply {
            this.hasLocalPrivateKey = hasLocalPrivateKey
        }

    private fun installConnectDispatcher(
        pairedUrls: MutableList<String>,
        errors: MutableList<WCErrorEvent>,
    ) {
        WalletConnect.connectDispatcher =
            WalletConnect.ConnectDispatcher(
                pairV2 = { url -> pairedUrls.add(url) },
                publishEvent = { },
                publishError = { err -> errors.add(err) },
                contextProvider = { null },
            )
    }

    @Test
    fun classicWalletIsSigningEligibleAndAllowsConnect() {
        assertTrue(isWalletConnectSigningEligible(WalletCategory.CLASSIC.value, false))
        assertTrue(isWalletConnectSigningEligible(WalletCategory.CLASSIC.value, true))
        assertTrue(sampleWallet("classic-1", WalletCategory.CLASSIC.value, false).isSigningEligible())
        assertTrue(sampleWalletItem("classic-1", WalletCategory.CLASSIC.value, false).isSigningEligible())

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "classic-1",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-1"),
            )
        }

        val pairedUrls = mutableListOf<String>()
        val errors = mutableListOf<WCErrorEvent>()
        installConnectDispatcher(pairedUrls, errors)

        val precheck = WalletConnect.checkCurrentSigningWallet()
        assertTrue(precheck is SigningWalletPrecheck.Eligible)
        assertEquals("classic-1", (precheck as SigningWalletPrecheck.Eligible).walletId)

        WalletConnect.connect(wcPairUrl)
        assertEquals(listOf(wcPairUrl), pairedUrls)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun importedWalletWithLocalPrivateKeyIsEligibleAndAllowsConnect() {
        assertTrue(isWalletConnectSigningEligible(WalletCategory.IMPORTED_MNEMONIC.value, true))
        assertTrue(isWalletConnectSigningEligible(WalletCategory.IMPORTED_PRIVATE_KEY.value, true))
        assertTrue(sampleWallet("imported-1", WalletCategory.IMPORTED_PRIVATE_KEY.value, true).isSigningEligible())
        assertTrue(sampleWalletItem("imported-1", WalletCategory.IMPORTED_MNEMONIC.value, true).isSigningEligible())

        WalletConnect.localPrivateKeyPresenceProvider = { walletId -> walletId == "imported-1" }
        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "imported-1",
                category = WalletCategory.IMPORTED_PRIVATE_KEY.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("imported-1"),
            )
        }

        val pairedUrls = mutableListOf<String>()
        val errors = mutableListOf<WCErrorEvent>()
        installConnectDispatcher(pairedUrls, errors)

        val precheck = WalletConnect.checkCurrentSigningWallet()
        assertTrue(precheck is SigningWalletPrecheck.Eligible)

        WalletConnect.connect(wcPairUrl)
        assertEquals(listOf(wcPairUrl), pairedUrls)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun importedWalletsWithoutLocalPrivateKeyAreBlockedBeforePair() {
        val noKeyCategories =
            listOf(
                WalletCategory.IMPORTED_MNEMONIC.value,
                WalletCategory.IMPORTED_PRIVATE_KEY.value,
            )

        noKeyCategories.forEachIndexed { index, category ->
            val walletId = "imported-nokey-$index"
            assertFalse(isWalletConnectSigningEligible(category, false))
            assertFalse(sampleWallet(walletId, category, false).isSigningEligible())
            assertFalse(sampleWalletItem(walletId, category, false).isSigningEligible())

            runBlocking {
                Web3Signer.applyWalletForTest(
                    walletId = walletId,
                    category = category,
                    hasLocalPrivateKey = false,
                    addresses = sampleAddresses(walletId),
                )
            }

            assertFalse(Web3Signer.isSigningEligible)
            assertEquals("", Web3Signer.evmAddress)
            assertEquals("", Web3Signer.solanaAddress)
            assertEquals("", Web3Signer.btcAddress)

            val pairedUrls = mutableListOf<String>()
            val errors = mutableListOf<WCErrorEvent>()
            installConnectDispatcher(pairedUrls, errors)

            WalletConnect.connect(wcPairUrl)
            assertTrue("pairV2 must not be invoked when local private key is absent", pairedUrls.isEmpty())
            assertEquals(1, errors.size)
            val blocked = errors.single().error.throwable as WalletConnectSigningBlockedException
            assertEquals(SigningBlockedReason.MISSING_PRIVATE_KEY, blocked.reason)
            assertEquals(WalletConnect.CODE_SIGNING_BLOCKED, blocked.code)
        }
    }

    @Test
    fun watchOnlyWalletIsBlockedBeforePairEvenWhenFlaggedTrue() {
        assertFalse(isWalletConnectSigningEligible(WalletCategory.WATCH_ADDRESS.value, false))
        assertFalse(isWalletConnectSigningEligible(WalletCategory.WATCH_ADDRESS.value, true))
        assertFalse(sampleWallet("watch-1", WalletCategory.WATCH_ADDRESS.value, false).isSigningEligible())

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "watch-1",
                category = WalletCategory.WATCH_ADDRESS.value,
                hasLocalPrivateKey = false,
                addresses = sampleAddresses("watch-1"),
            )
        }

        val pairedUrls = mutableListOf<String>()
        val errors = mutableListOf<WCErrorEvent>()
        installConnectDispatcher(pairedUrls, errors)

        WalletConnect.connect(wcPairUrl)
        assertTrue(pairedUrls.isEmpty())
        assertEquals(1, errors.size)
        val blocked = errors.single().error.throwable as WalletConnectSigningBlockedException
        assertEquals(SigningBlockedReason.WATCH_ONLY, blocked.reason)
        assertEquals(WalletConnect.CODE_SIGNING_BLOCKED, blocked.code)
    }

    @Test
    fun safeWalletIsBlockedAndSelectionDoesNotFallBackToPreviousSigningWallet() {
        assertFalse(isWalletConnectSigningEligible(WalletCategory.MIXIN_SAFE.value, false))
        assertFalse(isWalletConnectSigningEligible(WalletCategory.MIXIN_SAFE.value, true))
        assertEquals(
            WalletSignerTarget("safe-1", WalletCategory.MIXIN_SAFE.value),
            WalletDestination.Safe("safe-1", isOwner = true, chainId = null, url = null).walletSelectionTarget(),
        )

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "classic-1",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-1"),
            )
            Web3Signer.applyWalletForTest(
                walletId = "safe-1",
                category = WalletCategory.MIXIN_SAFE.value,
                hasLocalPrivateKey = false,
                addresses = emptyList(),
            )
        }

        assertEquals("safe-1", Web3Signer.currentWalletId)
        assertEquals(WalletCategory.MIXIN_SAFE.value, Web3Signer.currentWalletCategory)
        assertFalse(Web3Signer.isSigningEligible)
        assertEquals("", Web3Signer.address)
        assertEquals("", Web3Signer.evmAddress)
        assertEquals("", Web3Signer.solanaAddress)
        assertEquals("", Web3Signer.btcAddress)

        val pairedUrls = mutableListOf<String>()
        val errors = mutableListOf<WCErrorEvent>()
        installConnectDispatcher(pairedUrls, errors)

        WalletConnect.connect(wcPairUrl)
        assertTrue(pairedUrls.isEmpty())
        assertEquals(1, errors.size)
        val blocked = errors.single().error.throwable as WalletConnectSigningBlockedException
        assertEquals(SigningBlockedReason.UNSUPPORTED_WALLET, blocked.reason)
        assertEquals(WalletConnect.CODE_SIGNING_BLOCKED, blocked.code)
    }

    @Test
    fun switchingFromEligibleToIneligibleWalletDisconnectsSessionsAndClearsAddresses() {
        val syncEvents = mutableListOf<Pair<Boolean, WalletConnectAddresses>>()
        Web3Signer.walletConnectSyncHandler = { eligible, _, addresses ->
            syncEvents.add(eligible to addresses)
        }

        val persistedBindings = mutableMapOf<String, String>()
        val guard =
            WalletConnectSessionGuard(
                loadBindings = { persistedBindings.toMap() },
                saveBindings = { updated ->
                    persistedBindings.clear()
                    persistedBindings.putAll(updated)
                },
            )
        guard.bindSession("session-1", "classic-1")

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "classic-1",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-1"),
            )
            Web3Signer.applyWalletForTest(
                walletId = "watch-1",
                category = WalletCategory.WATCH_ADDRESS.value,
                hasLocalPrivateKey = false,
                addresses = sampleAddresses("watch-1"),
            )
        }

        assertEquals(2, syncEvents.size)
        assertTrue(syncEvents[0].first)
        assertFalse(syncEvents[1].first)
        assertEquals(WalletConnectAddresses("", "", ""), syncEvents[1].second)
        assertEquals(WalletConnectAddresses("", "", ""), Web3Signer.currentWalletConnectAddresses())
        assertEquals("", Web3Signer.address)

        val disconnectedTopics = mutableListOf<String>()
        var updatedCount = 0
        val activeSessions =
            listOf(
                "session-1" to
                    mapOf(
                        "eip155" to
                            Wallet.Model.Namespace.Session(
                                chains = listOf(Chain.Ethereum.chainId),
                                accounts = listOf("${Chain.Ethereum.chainId}:evm-addr-1"),
                                methods = emptyList(),
                                events = emptyList(),
                            ),
                    ),
            )

        guard.handleWalletSwitch(
            precheck = WalletConnect.checkCurrentSigningWallet(),
            activeSessions = activeSessions,
            disconnectSession = { topic -> disconnectedTopics.add(topic) },
            updateSession = { _, _ ->
                updatedCount++
                true
            },
        )

        assertEquals(listOf("session-1"), disconnectedTopics)
        assertEquals(0, updatedCount)
        assertNull(guard.getSessionWalletId("session-1"))
        assertTrue(persistedBindings.isEmpty())
    }

    @Test
    fun switchingFromIneligibleToEligibleWalletUpdatesAddressesAndRebindsSession() {
        val syncWalletIds = mutableListOf<Pair<Boolean, String>>()
        Web3Signer.walletConnectSyncHandler = { eligible, walletId, _ ->
            syncWalletIds.add(eligible to walletId)
        }

        val guard = WalletConnectSessionGuard()
        guard.bindSession("session-1", "classic-1")

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "watch-1",
                category = WalletCategory.WATCH_ADDRESS.value,
                hasLocalPrivateKey = false,
                addresses = sampleAddresses("watch-1"),
            )
            Web3Signer.applyWalletForTest(
                walletId = "classic-2",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-2", evm = "evm-addr-2", solana = "sol-addr-2", bitcoin = "btc-addr-2"),
            )
        }

        assertEquals(listOf(false to "watch-1", true to "classic-2"), syncWalletIds)
        assertTrue(Web3Signer.isSigningEligible)
        assertEquals("evm-addr-2", Web3Signer.evmAddress)
        assertEquals("sol-addr-2", Web3Signer.solanaAddress)
        assertEquals("btc-addr-2", Web3Signer.btcAddress)

        val updatedTopics = mutableListOf<String>()
        val updatedAccounts = mutableListOf<List<String>>()
        val activeSessions =
            listOf(
                "session-1" to
                    mapOf(
                        "eip155" to
                            Wallet.Model.Namespace.Session(
                                chains = listOf(Chain.Ethereum.chainId),
                                accounts = listOf("${Chain.Ethereum.chainId}:evm-addr-1"),
                                methods = emptyList(),
                                events = emptyList(),
                            ),
                    ),
            )

        guard.handleWalletSwitch(
            precheck = WalletConnect.checkCurrentSigningWallet(),
            activeSessions = activeSessions,
            disconnectSession = { },
            updateSession = { topic, updated ->
                updatedTopics.add(topic)
                updatedAccounts.add(updated.getValue("eip155").accounts)
                true
            },
        )

        assertEquals(listOf("session-1"), updatedTopics)
        assertEquals(listOf(listOf("${Chain.Ethereum.chainId}:evm-addr-2")), updatedAccounts)
        assertEquals("classic-2", guard.getSessionWalletId("session-1"))
    }

    @Test
    fun walletSwitchOrKeyRemovalBetweenProposalAndApprovalPreventsApproval() {
        val guard = WalletConnectSessionGuard()
        var hasImportedKey = true
        WalletConnect.localPrivateKeyPresenceProvider = { walletId -> walletId == "imported-1" && hasImportedKey }

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "classic-1",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-1"),
            )
        }
        guard.bindProposal("pair-1", "classic-1")
        assertTrue(guard.canApproveProposal("pair-1", WalletConnect.checkCurrentSigningWallet()))

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "classic-2",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-2", evm = "evm-addr-2"),
            )
        }
        assertFalse(guard.canApproveProposal("pair-1", WalletConnect.checkCurrentSigningWallet()))

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "imported-1",
                category = WalletCategory.IMPORTED_MNEMONIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("imported-1"),
            )
        }
        guard.bindProposal("pair-2", "imported-1")
        assertTrue(guard.canApproveProposal("pair-2", WalletConnect.checkCurrentSigningWallet()))

        hasImportedKey = false
        val afterRemovalPrecheck = WalletConnect.checkCurrentSigningWallet()
        assertTrue(afterRemovalPrecheck is SigningWalletPrecheck.Blocked)
        assertEquals(SigningBlockedReason.MISSING_PRIVATE_KEY, (afterRemovalPrecheck as SigningWalletPrecheck.Blocked).reason)
        assertFalse(guard.canApproveProposal("pair-2", afterRemovalPrecheck))
    }

    @Test
    fun staleIneligibleOrMissingChainRequestIsRejectedWithoutInvokingSigner() {
        val guard = WalletConnectSessionGuard()
        var signerInvoked = false
        val invokeSignerIfAllowed: (SessionRequestGuardResult) -> Unit = { result ->
            if (result is SessionRequestGuardResult.Allowed) {
                signerInvoked = true
            }
        }

        val sessionNamespaces =
            mapOf(
                "eip155" to
                    Wallet.Model.Namespace.Session(
                        chains = listOf(Chain.Ethereum.chainId),
                        accounts = listOf("${Chain.Ethereum.chainId}:evm-addr-1"),
                        methods = emptyList(),
                        events = emptyList(),
                    ),
            )

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "classic-1",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-1", evm = "Evm-Addr-1", solana = "", bitcoin = ""),
            )
        }
        guard.bindSession("session-1", "classic-1")

        // Valid request with case-insensitive EVM match succeeds
        val validResult =
            guard.validateRequest(
                topic = "session-1",
                chainId = Chain.Ethereum.chainId,
                precheck = WalletConnect.checkCurrentSigningWallet(),
                sessionNamespaces = sessionNamespaces,
            )
        assertTrue(validResult is SessionRequestGuardResult.Allowed)

        // Missing chain address for Solana on EVM-only wallet must be rejected without invoking signer
        val missingChainResult =
            guard.validateRequest(
                topic = "session-1",
                chainId = Chain.Solana.chainId,
                precheck = WalletConnect.checkCurrentSigningWallet(),
                sessionNamespaces = sessionNamespaces,
            )
        invokeSignerIfAllowed(missingChainResult)
        assertFalse(signerInvoked)
        assertEquals(
            SessionRequestGuardFailure.MISSING_CHAIN_ADDRESS,
            (missingChainResult as SessionRequestGuardResult.Rejected).failure,
        )

        // Bound wallet ID mismatch (session bound to another wallet) must be rejected without invoking signer
        guard.bindSession("session-other", "classic-other")
        val staleWalletResult =
            guard.validateRequest(
                topic = "session-other",
                chainId = Chain.Ethereum.chainId,
                precheck = WalletConnect.checkCurrentSigningWallet(),
                sessionNamespaces = sessionNamespaces,
            )
        invokeSignerIfAllowed(staleWalletResult)
        assertFalse(signerInvoked)
        assertEquals(
            SessionRequestGuardFailure.STALE_WALLET_SESSION,
            (staleWalletResult as SessionRequestGuardResult.Rejected).failure,
        )

        // Namespace account mismatch on current wallet must also be rejected
        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "classic-2",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-2", evm = "evm-addr-2"),
            )
        }
        guard.bindSession("session-namespace-mismatch", "classic-2")
        val namespaceMismatchResult =
            guard.validateRequest(
                topic = "session-namespace-mismatch",
                chainId = Chain.Ethereum.chainId,
                precheck = WalletConnect.checkCurrentSigningWallet(),
                sessionNamespaces = sessionNamespaces,
            )
        invokeSignerIfAllowed(namespaceMismatchResult)
        assertFalse(signerInvoked)
        assertEquals(
            SessionRequestGuardFailure.STALE_WALLET_SESSION,
            (namespaceMismatchResult as SessionRequestGuardResult.Rejected).failure,
        )

        // Ineligible wallet must be rejected with unauthorized code 4100 without invoking signer
        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "watch-1",
                category = WalletCategory.WATCH_ADDRESS.value,
                hasLocalPrivateKey = false,
                addresses = sampleAddresses("watch-1"),
            )
        }
        val ineligiblePrecheck = WalletConnect.checkCurrentSigningWallet()
        val ineligibleResult =
            guard.validateRequest(
                topic = "session-1",
                chainId = Chain.Ethereum.chainId,
                precheck = ineligiblePrecheck,
                sessionNamespaces = sessionNamespaces,
            )
        invokeSignerIfAllowed(ineligibleResult)
        assertFalse(signerInvoked)
        assertEquals(
            SessionRequestGuardFailure.INELIGIBLE_WALLET,
            (ineligibleResult as SessionRequestGuardResult.Rejected).failure,
        )
        assertEquals(
            WalletConnect.CODE_SIGNING_BLOCKED,
            (ineligiblePrecheck as SigningWalletPrecheck.Blocked).toException(null).code,
        )
    }

    @Test
    fun entryPointsRejectIneligibleAndMismatchedWalletsUniformly() {
        val samplePeer =
            PeerUI(
                uri = "https://example.invalid",
                name = "Test dApp",
                icon = "",
                desc = "",
                account = "",
            )
        val guard = WalletConnectSessionGuard()

        val blockedCases =
            listOf(
                Triple("watch-1", WalletCategory.WATCH_ADDRESS.value, false),
                Triple("imported-nokey", WalletCategory.IMPORTED_PRIVATE_KEY.value, false),
                Triple("safe-1", WalletCategory.MIXIN_SAFE.value, false),
            )

        blockedCases.forEach { (walletId, category, hasKey) ->
            runBlocking {
                Web3Signer.applyWalletForTest(
                    walletId = walletId,
                    category = category,
                    hasLocalPrivateKey = hasKey,
                    addresses = sampleAddresses(walletId),
                )
            }

            // 1. V2 connect preflight blocks
            val pairedUrls = mutableListOf<String>()
            val errors = mutableListOf<WCErrorEvent>()
            installConnectDispatcher(pairedUrls, errors)
            WalletConnect.connect(wcPairUrl)
            assertTrue(pairedUrls.isEmpty())
            assertEquals(1, errors.size)
            assertTrue(errors.single().error.throwable is WalletConnectSigningBlockedException)

            // 2. TIP entry point blocks proposal & sign request UI
            WalletConnectTIP.clear()
            WalletConnectTIP.peer = samplePeer
            WalletConnectTIP.signData = WalletConnect.WCSignData.TIPSignData("test-sign-data")
            assertNull(WalletConnectTIP.getSessionProposalUI())
            assertNull(WalletConnectTIP.getSessionRequestUI())

            // 3. Session guard rejects approval & requests
            guard.bindProposal("pair-$walletId", walletId)
            assertFalse(guard.canApproveProposal("pair-$walletId", WalletConnect.checkCurrentSigningWallet()))
            assertTrue(
                guard.validateRequest(
                    topic = "session-$walletId",
                    chainId = Chain.Ethereum.chainId,
                    precheck = WalletConnect.checkCurrentSigningWallet(),
                ) is SessionRequestGuardResult.Rejected,
            )
        }

        // TIP entry point also blocks stale wallet ID even when switching to another eligible wallet
        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "classic-1",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-1"),
            )
        }
        WalletConnectTIP.clear()
        WalletConnectTIP.peer = samplePeer
        WalletConnectTIP.signData = WalletConnect.WCSignData.TIPSignData("test-sign-data")
        WalletConnectTIP.walletId = "classic-1"
        assertNotNull(WalletConnectTIP.getSessionProposalUI())
        assertNotNull(WalletConnectTIP.getSessionRequestUI())

        runBlocking {
            Web3Signer.applyWalletForTest(
                walletId = "classic-2",
                category = WalletCategory.CLASSIC.value,
                hasLocalPrivateKey = true,
                addresses = sampleAddresses("classic-2", evm = "evm-addr-2"),
            )
        }
        assertNull(WalletConnectTIP.getSessionProposalUI())
        assertNull(WalletConnectTIP.getSessionRequestUI())
    }
}
